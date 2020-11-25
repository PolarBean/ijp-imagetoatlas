package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.DisplaySettingsCommand;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import spimdata.util.Displaysettings;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DisplayPanel implements MultiSlicePositioner.SliceChangeListener, ListSelectionListener {

    final JPanel paneDisplay;

    final MultiSlicePositioner mp;

    final JTable table;

    final JTable tableSelectionControl;

    final SliceDisplayTableModel model;

    final SelectedSliceDisplayTableModel modelSelect;

    Consumer<String> log = (str) -> System.out.println(DisplayPanel.class+":"+str);

    int maxChannels = 0;

    int nSlices = 0;

    boolean globalFlagVisible = true;

    List<Boolean> globalFlagPerChannel = new ArrayList<>();

    List<Displaysettings> globalDisplaySettingsPerChannel = new ArrayList<>();

    public DisplayPanel(MultiSlicePositioner mp) {
        this.mp = mp;
        paneDisplay = new JPanel(new BorderLayout());

        JButton toggleDisplayMode = new JButton("Multi/Single Slice");
        toggleDisplayMode.addActionListener(e -> {
            mp.changeSliceDisplayMode();
        });

        mp.addSliceListener(this);

        model = new SliceDisplayTableModel();
        modelSelect = new SelectedSliceDisplayTableModel();
        table = new JTable(model);
        tableSelectionControl = new JTable();

        tableSelectionControl.setModel( modelSelect );

        tableSelectionControl.setShowGrid(false);
        table.setShowGrid( false );

        table.setModel( model );

        table.getSelectionModel().addListSelectionListener(this);

        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Displaysettings.class, new DisplaySettingsRenderer(true));
        table.setDefaultRenderer(Boolean.class, new VisibilityRenderer(true));

        tableSelectionControl.setFillsViewportHeight(true);
        tableSelectionControl.setShowHorizontalLines(true);
        tableSelectionControl.setDefaultRenderer(Displaysettings.class, new DisplaySettingsRenderer(true));
        tableSelectionControl.setDefaultRenderer(Boolean.class, new VisibilityRenderer(true));

        // table.setDefaultEditor(Displaysettings.class, new DisplaysettingsEditor());
        // tableSelectionControl.setDefaultEditor(Displaysettings.class, new DisplaysettingsEditor());

        JScrollPane scPane = new JScrollPane(tableSelectionControl);
        Dimension d = new Dimension(tableSelectionControl.getPreferredSize());

        //d.height*=2;
        tableSelectionControl.setPreferredScrollableViewportSize(d);//new Dimension(400,500));


        paneDisplay.add(scPane, BorderLayout.NORTH);
        paneDisplay.add(table, BorderLayout.CENTER);


        tableSelectionControl.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = tableSelectionControl.rowAtPoint(evt.getPoint());
                int col = tableSelectionControl.columnAtPoint(evt.getPoint());
                if (row >= 0 && col >= 0) {
                    if (row==0) {
                        if ((col>1)&&(col%2 == 1)) {
                            int iChannel = (col-3)/2;

                            SourceAndConverter[] sacs = getSelectedIndices().stream()
                                    .map(idx -> sortedSlices.get(idx))
                                    .filter(slice -> slice.nChannels>iChannel)
                                    .map(slice -> slice.getGUIState().getCurrentSources()[iChannel])
                                    .collect(Collectors.toList()).toArray(new SourceAndConverter[0]);

                            if (sacs.length>0) {
                                mp.scijavaCtx
                                        .getService(CommandService.class)
                                        .run(DisplaySettingsCommand.class, true, "sacs", sacs);
                            } else {
                                mp.log.accept("Please select a slice with a valid channel in the tab.");
                            }


                        }
                    }
                }
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                int col = table.columnAtPoint(evt.getPoint());
                if (row >= 0 && col >= 0) {
                    if ((col>1)&&(col%2 == 1)) {
                        int iChannel = (col-3)/2;

                        SourceAndConverter[] sacs = new SourceAndConverter[1];

                        if (sortedSlices.get(row).nChannels>iChannel) {
                            sacs[0] = sortedSlices.get(row).getGUIState().getCurrentSources()[iChannel];

                            mp.scijavaCtx
                                    .getService(CommandService.class)
                                    .run(DisplaySettingsCommand.class, true, "sacs", sacs);
                        } else {
                            mp.log.accept("This slice has no channel indexed "+iChannel);
                        }
                    }
                }
            }
        });
    }

    public JPanel getPanel() {
        return paneDisplay;
    }

    @Override
    public synchronized void sliceDeleted(SliceSources slice) {
        //log.accept(slice+" deleted");
        nSlices--;
        sortSlices();
        List<SliceSources> slices = sortedSlices;//mp.getSortedSlices();
        int index = slices.indexOf(slice);
        slices.remove(slice);
        model.fireTableRowsDeleted(index, index); // Which thread ?

        // What happened to the number of channels ?
        if (slice.nChannels==maxChannels) {
            // Maybe it's the last one with this number of channels...
            int newMaxChannels = slices.stream()
                    .map(s -> s.nChannels)
                    .reduce(Math::max).get();
            if (newMaxChannels < maxChannels) {
                // The number of channels diminished... full update
                maxChannels = newMaxChannels;
                model.fireTableStructureChanged();

                globalFlagPerChannel.remove(globalFlagPerChannel.size()-1);
                globalDisplaySettingsPerChannel.remove(globalDisplaySettingsPerChannel.size()-1);
                modelSelect.fireTableStructureChanged();
            }

        } else {
            // No need to worry
        }
    }

    @Override
    public synchronized void sliceCreated(SliceSources slice) {
        //log.accept(slice+" created");
        nSlices++;
        sortSlices();
        int index = sortedSlices.indexOf(slice);
        model.fireTableRowsInserted(index, index); // Which thread ?
        if (slice.nChannels>maxChannels) {
            for (int i=maxChannels;i<slice.nChannels;i++) {
                globalFlagPerChannel.add(new Boolean(true));
                globalDisplaySettingsPerChannel.add(new Displaysettings(-1));
            }
            maxChannels = slice.nChannels;
            model.fireTableStructureChanged(); // All changed!
            modelSelect.fireTableStructureChanged();
        }
    }

    @Override
    public void sliceZPositionChanged(SliceSources slice) {
        log.accept(slice+" display changed");
        sortSlices();
        model.fireTableDataChanged();
    }

    @Override
    public void sliceVisibilityChanged(SliceSources slice) {
        int index = sortedSlices.indexOf(slice);
        model.fireTableRowsUpdated(index,index);
    }

    List<Integer> currentlySelectedIndices = new ArrayList<>();

    synchronized List<Integer> getSelectedIndices() {
        return new ArrayList<>(currentlySelectedIndices);
    }

    synchronized void setCurrentlySelectedIndices(List<Integer> selectedIndices) {
        currentlySelectedIndices = new ArrayList<>(selectedIndices);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        ListSelectionModel lsm = (ListSelectionModel)e.getSource();

        List<Integer> currentSelection = new ArrayList<>();

        if (lsm.isSelectionEmpty()) {
            //output.append(" <none>");
        } else {

            // Find out which indexes are selected.
            int minIndex = lsm.getMinSelectionIndex();
            int maxIndex = lsm.getMaxSelectionIndex();
            for (int i = minIndex; i <= maxIndex; i++) {
                if (lsm.isSelectedIndex(i)) {
                    currentSelection.add(i);
                }
            }
        }

        setCurrentlySelectedIndices(currentSelection);
    }

    List<SliceSources> sortedSlices = new ArrayList<>();

    public void sortSlices() {
        sortedSlices = mp.getSortedSlices();
    }

    class SliceDisplayTableModel extends AbstractTableModel {

        public String getColumnName(int columnIndex) {
            if ((columnIndex) == 0) {
                return "#";
            } else if ((columnIndex) == 1) {
                return "Vis.";
            } else if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-3)/2;
                return "Ch_"+iChannel;
            } else {
                return "";
            }
        }

        @Override
        public int getRowCount() {
            return nSlices;
        }

        @Override
        public int getColumnCount() {
            return maxChannels*2+2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SliceSources slice =  sortedSlices.get(rowIndex); // Not efficient
            if ((columnIndex == 0)) {
                return new Integer(rowIndex).toString();
            } else if ((columnIndex) == 1) {
                return new Boolean(slice.getGUIState().isSliceVisible());
            } else if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-2)/2;
                if (slice.nChannels>iChannel) {
                    return new Boolean(slice.getGUIState().channelVisible[iChannel]);
                } else {
                    return new Boolean(false);
                }
            } else {
                int iChannel = (columnIndex-3)/2;
                if (slice.nChannels>iChannel) {
                    return slice.getGUIState().getDisplaysettings()[iChannel];
                } else {
                    return new Displaysettings(-1,"-");
                }
            }
        }

        /**
         *
         *  @param  aValue   value to assign to cell
         *  @param  rowIndex   row of cell
         *  @param  columnIndex  column of cell
         */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            SliceSources slice =  sortedSlices.get(rowIndex);
            if ((columnIndex) == 0) {
                // Nothing
            } else if ((columnIndex) == 1) {
                Boolean flag = (Boolean) aValue;
                if (flag) {
                    if (!slice.getGUIState().isSliceVisible())
                        slice.getGUIState().setSliceVisible();
                } else {
                    if (slice.getGUIState().isSliceVisible()) {
                        slice.getGUIState().setSliceInvisible();
                    }
                }
            } else if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-2)/2;
                if (slice.nChannels>iChannel) {
                    Boolean flag = (Boolean) aValue;
                    if (slice.getGUIState().isChannelVisible(iChannel)) {
                        if (!flag) {
                            slice.getGUIState().setChannelVisibility(iChannel, false);
                        }
                    } else {
                        if (flag) {
                            slice.getGUIState().setChannelVisibility(iChannel, true);
                        }
                    }
                } else {
                    // Channel not available for this slice
                }
            } else {
                int iChannel = (columnIndex-3)/2;
                if (slice.nChannels>iChannel) {
                    //return slice.getGUIState().getDisplaysettings()[iChannel];
                } else {
                    //return new Displaysettings(-1,"-");
                }
            }
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return String.class;
            if (columnIndex == 1) return Boolean.class;
            if (columnIndex%2 == 0) {
                return Boolean.class;
            } else {
                return Displaysettings.class;
            }
        }

        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            return col>0;
        }

    }

    class SelectedSliceDisplayTableModel extends SliceDisplayTableModel {

        /**
         *
         *  @param  aValue   value to assign to cell
         *  @param  rowIndex   row of cell
         *  @param  columnIndex  column of cell
         */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if ((columnIndex) == 0) {

            } else if ((columnIndex) == 1) {
                Boolean flag = (Boolean) aValue;
                globalFlagVisible = flag;
                getSelectedIndices().forEach(idx -> {
                SliceSources cSlice = sortedSlices.get(idx);
                    if (flag) {
                        if (!cSlice.getGUIState().isSliceVisible())
                            cSlice.getGUIState().setSliceVisible();
                    } else {
                        if (cSlice.getGUIState().isSliceVisible()) {
                            cSlice.getGUIState().setSliceInvisible();
                        }
                    }
                });
            } else if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-2)/2;
                Boolean flag = (Boolean) aValue;
                globalFlagPerChannel.set(iChannel, flag);
                getSelectedIndices().forEach(idx -> {
                    SliceSources cSlice = sortedSlices.get(idx);
                    if (cSlice.nChannels>iChannel) {
                        if (cSlice.getGUIState().isChannelVisible(iChannel)) {
                            if (!flag) {
                                cSlice.getGUIState().setChannelVisibility(iChannel, false);
                            }
                        } else {
                            if (flag) {
                                cSlice.getGUIState().setChannelVisibility(iChannel, true);
                            }
                        }
                    } else {
                        // Channel not available for this slice
                    }
                });
            } else {
                int iChannel = (columnIndex-3)/2;
                /*if (cSlice.nChannels>iChannel) {
                    //return slice.getGUIState().getDisplaysettings()[iChannel];
                } else {
                    //return new Displaysettings(-1,"-");
                }*/
            }

        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if ((columnIndex) == 0) {
                return ".";
            } else if ((columnIndex) == 1) {
                return new Boolean(globalFlagVisible);
            } if (columnIndex%2 == 0) {
                int iChannel = (columnIndex-2)/2;
                if (sortedSlices.size()>0) {
                    return new Boolean(globalFlagPerChannel.get(iChannel));
                } else {
                    return new Boolean(false);
                }
            } else {
                int iChannel = (columnIndex-3)/2;
                if (sortedSlices.size()>0) {
                    return globalDisplaySettingsPerChannel.get(iChannel);
                } else {
                    return new Displaysettings(-1,"-");
                }
            }
        }

        @Override
        public int getRowCount() {
            return 1;
        }
    }

    public static class DisplaySettingsRenderer extends JLabel
            implements TableCellRenderer {
        Border unselectedBorder = null;
        Border selectedBorder = null;
        boolean isBordered = true;

        public DisplaySettingsRenderer(boolean isBordered) {
            this.isBordered = isBordered;
            setOpaque(true); //MUST do this for background to show up.
        }

        public Component getTableCellRendererComponent(
                JTable table, Object displaysettings,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            Displaysettings ds = (Displaysettings) displaysettings;

            if (ds.getName().equals("-")) {

            } else {

                Color newColor = new Color(ds.color[0], ds.color[1], ds.color[2]);
                setBackground(newColor);
                setForeground(new Color(255 - ds.color[0], 255 - ds.color[1], 255 - ds.color[2]));
                setText((int) ds.min + ":" + (int) ds.max);
                if (isBordered) {
                    if (isSelected) {
                        if (selectedBorder == null) {
                            selectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                                    table.getSelectionBackground());
                        }
                        setBorder(selectedBorder);
                    } else {
                        if (unselectedBorder == null) {
                            unselectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                                    table.getBackground());
                        }
                        setBorder(unselectedBorder);
                    }
                }

                setToolTipText("RGB value: " + newColor.getRed() + ", "
                        + newColor.getGreen() + ", "
                        + newColor.getBlue());
            }
            return this;
        }
    }

    public static class VisibilityRenderer extends JLabel implements TableCellRenderer {

        static ImageIcon visibleIcon;
        static ImageIcon invisibleIcon;

        static {
            URL iconURL;
            iconURL = DisplayPanel.class.getResource("/graphics/Visible.png");

            visibleIcon = new ImageIcon(iconURL);
            Image image = visibleIcon.getImage(); // transform it
            Image newimg = image.getScaledInstance(15, 15,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
            visibleIcon = new ImageIcon(newimg);  // transform it back


            iconURL = DisplayPanel.class.getResource("/graphics/InvisibleL.png");
            invisibleIcon = new ImageIcon(iconURL);
            image = invisibleIcon.getImage(); // transform it
            newimg = image.getScaledInstance(15, 15,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
            invisibleIcon = new ImageIcon(newimg);  // transform it back
        }

        Border unselectedBorder = null;
        Border selectedBorder = null;
        boolean isBordered = true;

        public VisibilityRenderer(boolean isBordered) {
            this.isBordered = isBordered;
            setOpaque(true); //MUST do this for background to show up.
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object v,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            Boolean visible = (Boolean) v;

            if (isBordered) {
                if (isSelected) {
                    if (selectedBorder == null) {
                        selectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                table.getSelectionBackground());
                    }
                    setBorder(selectedBorder);
                } else {
                    if (unselectedBorder == null) {
                        unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                table.getBackground());
                    }
                    setBorder(unselectedBorder);
                }
            }

            /*setToolTipText("RGB value: " + newColor.getRed() + ", "
                    + newColor.getGreen() + ", "
                    + newColor.getBlue());*/
            if (visible) {
                setIcon(visibleIcon);
            } else {
                setIcon(invisibleIcon);
            }
            return this;
        }
    }


    public class DisplaysettingsEditor extends AbstractCellEditor
            implements TableCellEditor,
            ActionListener {
        Displaysettings currentDisplaySettings = new Displaysettings(-1);
        JButton button;
        //JColorChooser colorChooser;
        //JDialog dialog;
        protected static final String EDIT = "edit";
        volatile Future<CommandModule> cm;

        public DisplaysettingsEditor() {
            //Set up the editor (from the table's point of view),
            //which is a button.
            //This button brings up the color chooser dialog,
            //which is the editor from the user's point of view.
            button = new JButton();
            button.setActionCommand(EDIT);
            button.addActionListener(this);
            button.setBorderPainted(false);

            //Set up the dialog that the button brings up.
            //colorChooser = new JColorChooser();
            /*dialog = JColorChooser.createDialog(button,
                    "Pick a Color",
                    true,  //modal
                    colorChooser,
                    this,  //OK button handler
                    null); //no CANCEL button handler*/
        }

        /**
         * Handles events from the editor button and from
         * the dialog's OK button.
         */
        public void actionPerformed(ActionEvent e) {
            if (EDIT.equals(e.getActionCommand())) {
                log.accept("Inside action");

                cm = null;
                cm = mp.scijavaCtx
                        .getService(CommandService.class)
                        .run(DisplaySettingsCommand.class, false);
                //fireEditingStopped();
                try {
                    Thread.sleep(3000);
                } catch (Exception exception) {

                }

            } else { //User pressed dialog's "OK" button.
              //   currentColor = colorChooser.getColor();
                log.accept("Other stuff");
            }
        }

        //Implement the one CellEditor method that AbstractCellEditor doesn't.
        public Object getCellEditorValue() {
            try {
                cm.get();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            System.out.println("Getting Cell Editor Value NOW");
            return currentDisplaySettings;
        }

        //Implement the one method defined by TableCellEditor.
        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            currentDisplaySettings = (Displaysettings) value;
            return button;
        }
    }

}