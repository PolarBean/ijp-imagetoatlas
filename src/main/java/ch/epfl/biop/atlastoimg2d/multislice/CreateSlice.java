package ch.epfl.biop.atlastoimg2d.multislice;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

import static ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner.POSITIONING_MODE;
import static ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner.REGISTRATION_MODE;

/**
 * Creates a new Slice source
 */
public class CreateSlice extends CancelableAction {

    private List<SourceAndConverter<?>> sacs;
    private SliceSources sliceSource;
    private double slicingAxisPosition;

    public CreateSlice(MultiSlicePositioner mp, List<SourceAndConverter<?>> sacs, double slicingAxisPosition) {
        super(mp);
        this.sacs = sacs;
        this.slicingAxisPosition = slicingAxisPosition;
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    @Override
    public boolean run() {
        boolean sacAlreadyPresent = false;
        for (SourceAndConverter sac : sacs) {
            for (SliceSources slice : mp.slices) {
                for (SourceAndConverter test : slice.original_sacs) {
                    if (test.equals(sac)) {
                        sacAlreadyPresent = true;
                    }
                }
            }
        }

        if (sacAlreadyPresent) {
            SliceSources zeSlice = null;

            // A source is already included :
            // If all sources match exactly what's in a single SliceSources -> that's a move operation

            boolean exactMatch = false;
            for (SliceSources ss : mp.slices) {
                if (ss.exactMatch(sacs)) {
                    exactMatch = true;
                    zeSlice = ss;
                }
            }

            if (!exactMatch) {
                System.err.println("A source is already used in the positioner : slice not created.");
                mp.log.accept("A source is already used in the positioner : slice not created.");
                return false;
            } else {
                // Move action:
                new MoveSlice(mp, zeSlice, slicingAxisPosition).runRequest();
                return false;
            }
        }

        if (sliceSource == null) // for proper redo function
            sliceSource = new SliceSources(sacs.toArray(new SourceAndConverter[sacs.size()]),
                    slicingAxisPosition,mp);

        mp.slices.add(sliceSource);

        mp.updateDisplay();

        if (mp.currentMode.equals(POSITIONING_MODE)) {
            SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .show(mp.bdvh, sliceSource.relocated_sacs_positioning_mode);
            sliceSource.enableGraphicalHandles();
        } else if (mp.currentMode.equals(REGISTRATION_MODE)) {
            SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .show(mp.bdvh, sliceSource.registered_sacs);
            sliceSource.disableGraphicalHandles();
        }

        mp.log.accept("Slice added");

        return true;
    }

    public SliceSources getSlice() {
        return sliceSource;
    }

    public String toString() {
        return "Slice Created " + new DecimalFormat("###.##").format(this.slicingAxisPosition);
    }

    @Override
    public boolean cancel() {
        System.out.println("Cancelling source creation");
        mp.slices.remove(sliceSource);
        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .remove(mp.bdvh, sliceSource.relocated_sacs_positioning_mode);
        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .remove(mp.bdvh, sliceSource.registered_sacs);
        mp.log.accept("Slice removed");
        return true;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.drawString("C", (int) px - 5, (int) py + 5);
    }
}
