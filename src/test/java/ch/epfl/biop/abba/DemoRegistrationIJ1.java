package ch.epfl.biop.abba;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixAffineCommand;
import ch.epfl.biop.atlas.allen.adultmousebrain.AllenBrainAdultMouseAtlasCCF2017Command;
import ch.epfl.biop.atlas.aligner.commands.SacMultiSacsPositionerCommand;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.bdv.command.importer.SourceFromImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

import java.io.File;

public class DemoRegistrationIJ1 {

	public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ImagePlus demoSlice = IJ.openImage("src/test/resources/demoSlice.tif");
        demoSlice.show();

        ij.command().run(AllenBrainAdultMouseAtlasCCF2017Command.class, true).get();

        ij.command().run(SourceFromImagePlusCommand.class, true, "imagePlus", demoSlice).get();

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command().run(SacMultiSacsPositionerCommand.class, true).get().getOutput("mp"));

        SourceAndConverter[] sac = ij.convert().convert(demoSlice.getTitle(), SourceAndConverter[].class);

        mp.createSlice(sac,4.5);

        mp.waitForTasks();

        SliceSources slice = mp.getSortedSlices().get(0);

        mp.centerBdvViewOn(slice);
        mp.selectSlice(slice);

        ij.command().run(RegistrationElastixAffineCommand.class, true,
                "mp", mp,
                        "showImagePlusRegistrationResult", true,
                        "background_offset_value_moving", 0,
                        "atlasImageChannel",0,
                        "sliceImageChannel",0
                ).get();

        /*ij.command().run(RegistrationElastixSplineCommand.class, true,
                    "mp", mp,
                    "nbControlPointsX", 10,
                    "showImagePlusRegistrationResult", true,
                    "background_offset_value_moving", 0,
                    "atlasImageChannel",0,
                    "sliceImageChannel",0
            ).get();*/

        System.out.println("Waiting for registration tasks to be finished...");
        mp.waitForTasks();
        System.out.println("Saving...");
        mp.saveState(new File("src/test/resources/output/reg_demoregistrationij1.json"), true);
        System.out.println("Done");

	}

}