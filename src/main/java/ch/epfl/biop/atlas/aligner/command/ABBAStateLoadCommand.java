package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>File>ABBA - Load State [Experimental]",
        description = "Loads a previous registration state into ABBA")
public class ABBAStateLoadCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(style = "open")
    File state_file;

    @Override
    public void run() {
        mp.loadState(state_file);
    }
}