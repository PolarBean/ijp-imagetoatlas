package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import ch.epfl.biop.atlastoimg2d.multislice.SlicingAdjusterPanel;
import ch.epfl.biop.sourceandconverter.transform.SourceMosaicZSlicer;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesAdderCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvWindowCreatorCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import static net.imglib2.cache.img.DiskCachedCellImgOptions.options;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Position Multiple Slices")
public class SacMultiSacsPositionerCommand implements Command {

    @Parameter(choices = {"coronal", "sagittal", "horizontal", "free"})
    String slicingMode;

    @Parameter
    public BiopAtlas ba;

    AffineTransform3D slicingTransfom;

    @Parameter
    CommandService cs;

    @Parameter
    Context context;

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvMultiSlicer;

    @Parameter
    ObjectService os;

    @Parameter(type = ItemIO.OUTPUT)
    MultiSlicePositioner mp;

    @Override
    public void run() {

        slicingTransfom = new AffineTransform3D();

        switch(slicingMode) {
            case "free" :
                throw new UnsupportedOperationException();
            case "coronal" :
                // No Change
                break;
            case "sagittal" :
                slicingTransfom.rotate(1,-Math.PI/2);
                break;
            case "horizontal" :
                slicingTransfom.rotate(0,Math.PI/2);
                break;
        }

        // No let's check for bounds along the z axis
        // Pick the first SourceAndConverter
        SourceAndConverter sacForBoundsTesting = ba.map.getStructuralImages()[0];

        // Gets level 0 (and timepoint 0) and source transform
        AffineTransform3D sacTransform = new AffineTransform3D();
        sacForBoundsTesting.getSpimSource().getSourceTransform(0,0, sacTransform);

        RandomAccessibleInterval rai = sacForBoundsTesting.getSpimSource().getSource(0,0);

        double minZAxis = Double.MAX_VALUE;
        double maxZAxis = -Double.MAX_VALUE;

        double minXAxis = Double.MAX_VALUE;
        double maxXAxis = -Double.MAX_VALUE;

        double minYAxis = Double.MAX_VALUE;
        double maxYAxis = -Double.MAX_VALUE;

        // Project all corners on slicing coordinate system and find min / max
        for (int x=0;x<2;x++)
        for (int y=0;y<2;y++)
        for (int z=0;z<2;z++){
            RealPoint pt = new RealPoint(3);
            pt.setPosition(new long[] {
                    x*rai.dimension(0),
                    y*rai.dimension(1),
                    z*rai.dimension(2),
            });
            RealPoint ptRealSpace = new RealPoint(3);
            sacTransform.apply(pt,ptRealSpace);

            double projectedPointOnSlicingAxis =
                    ptRealSpace.getDoublePosition(0)*slicingTransfom.get(2,0)+
                            ptRealSpace.getDoublePosition(1)*slicingTransfom.get(2,1)+
                            ptRealSpace.getDoublePosition(2)*slicingTransfom.get(2,2);
            if (projectedPointOnSlicingAxis<minZAxis)
                minZAxis = projectedPointOnSlicingAxis;
            if (projectedPointOnSlicingAxis>maxZAxis)
                maxZAxis = projectedPointOnSlicingAxis;

            double projectedPointOnSlicingXAxis =
                    ptRealSpace.getDoublePosition(0)*slicingTransfom.get(0,0)+
                            ptRealSpace.getDoublePosition(1)*slicingTransfom.get(0,1)+
                            ptRealSpace.getDoublePosition(2)*slicingTransfom.get(0,2);
            if (projectedPointOnSlicingXAxis<minXAxis)
                minXAxis = projectedPointOnSlicingXAxis;
            if (projectedPointOnSlicingXAxis>maxXAxis)
                maxXAxis = projectedPointOnSlicingXAxis;

            double projectedPointOnSlicingYAxis =
                    ptRealSpace.getDoublePosition(0)*slicingTransfom.get(1,0)+
                            ptRealSpace.getDoublePosition(1)*slicingTransfom.get(1,1)+
                            ptRealSpace.getDoublePosition(2)*slicingTransfom.get(1,2);
            if (projectedPointOnSlicingYAxis<minYAxis)
                minYAxis = projectedPointOnSlicingYAxis;
            if (projectedPointOnSlicingYAxis>maxYAxis)
                maxYAxis = projectedPointOnSlicingYAxis;

        }

        // Adds a margin of 10 % for tilt correction

        minZAxis*=1.1;
        maxZAxis*=1.1;

        // Gets slicing resolution
        // TODO : check null pointer exception if getvoxel not present
        double slicingResolution = 0.01;
                //sacForBoundsTesting.getSpimSource().getVoxelDimensions().dimension(0);

        // Dummy ImageFactory
        final int[] cellDimensions = new int[] { 32, 32, 32 };

        // Cached Image Factory Options
        final DiskCachedCellImgOptions factoryOptions = options()
                .cellDimensions( cellDimensions )
                .cacheType( DiskCachedCellImgOptions.CacheType.BOUNDED )
                .maxCacheSize( 1 );

        // Creates cached image factory of Type UnsignedShort
        final DiskCachedCellImgFactory<UnsignedShortType> factory = new DiskCachedCellImgFactory<>( new UnsignedShortType(), factoryOptions );

        slicingTransfom.scale(slicingResolution);

        long nPixX = (long)((maxXAxis-minXAxis)/slicingResolution);
        long nPixY = (long)((maxYAxis-minYAxis)/slicingResolution);
        long nPixZ = (long)((maxZAxis-minZAxis)/slicingResolution);

        adjustShiftSlicingTransform(slicingTransfom, nPixX, nPixY, nPixZ);

        SourceAndConverter nonWrappedSlicingModel = new EmptySourceAndConverterCreator("SlicingModel",new AffineTransform3D(),
                nPixX,
                nPixY,
                nPixZ,
                factory
                ).get();

        // Wrapped as TransformedSource to adjust slicing
        SourceAndConverter slicingModel = new SourceAffineTransformer(nonWrappedSlicingModel, slicingTransfom).getSourceOut();

        SourceAndConverterServices.getSourceAndConverterService().register(slicingModel);
        //SourceAndConverterServices.getSourceAndConverterDisplayService().show(slicingModel);

        //MultiSlicePositioner.ZStepSetter zSetter = new MultiSlicePositioner.ZStepSetter();
        try {
            // creates new bdv window
            bdvMultiSlicer = (BdvHandle) cs.run(BdvWindowCreatorCommand.class, true,
                    "is2D", false, //true,
                    "windowTitle", "Multi Slice Positioner " + ba.toString(),
                    "interpolate", false,
                    "nTimepoints", 1,
                    "projector", Projection.SUM_PROJECTOR)
                    .get().getOutput("bdvh");


            SourceAndConverter[] nonExtendedSlicedSources = new SourceAndConverter[ba.map.getStructuralImages().length];
            SourceAndConverter[] extendedSlicedSources = new SourceAndConverter[ba.map.getStructuralImages().length];

            SourceMosaicZSlicer mosaic = new SourceMosaicZSlicer(null, slicingModel, true, false, false, () -> {
                if (mp == null) {
                    return (long) 1;
                } else {
                    return mp.getzStepSetter().getStep();
                }});

            // Recenter around center of source in xy
            SourceAndConverter sourceUsedForRecenteringXY = ba.map.getStructuralImages()[0];

            //sourceUsedForRecenteringXY.getSpimSource().getSource(0,0).dimensions(dims);
            AffineTransform3D centerTransform = null;
            for (int index = 0; index<ba.map.getStructuralImages().length;index++) {
                SourceAndConverter sac = ba.map.getStructuralImages()[index];
                nonExtendedSlicedSources[index] = sac;
                SourceAndConverter reslicedSac = mosaic.apply(sac);
                if (centerTransform == null) {
                    centerTransform = new AffineTransform3D();
                    reslicedSac.getSpimSource().getSourceTransform(0,0,centerTransform);
                    RealPoint ptCenterGlobal = new RealPoint(3);

                    long[] dims = new long[3];
                    reslicedSac.getSpimSource().getSource(0,0).dimensions(dims);
                    dims[0] = dims[0]/dims[2];
                    RealPoint ptCenterPixel = new RealPoint((dims[0]-1.0)/2.0,(dims[1]-1.0)/2.0, 0);//(dims[2]-1.0)/2.0);

                    centerTransform.apply(ptCenterPixel, ptCenterGlobal);
                    centerTransform.identity();
                    centerTransform.translate(-ptCenterGlobal.getDoublePosition(0),
                            -ptCenterGlobal.getDoublePosition(1),
                            0
                            );

                }

                reslicedSac = new SourceAffineTransformer(null, centerTransform).apply(reslicedSac);
                SourceAndConverterServices.getSourceAndConverterService()
                        .register(reslicedSac);
                extendedSlicedSources[index] = reslicedSac;
            }

            mp = new MultiSlicePositioner(bdvMultiSlicer, slicingModel, extendedSlicedSources, nonExtendedSlicedSources, context);

            bdvMultiSlicer.getCardPanel().addCard("Slicing Adjustement",
                    new SlicingAdjusterPanel(mp,context).getPanel("modelSlicing", slicingModel,
                            "slicedSources", extendedSlicedSources,
                            "sliceSetter", mp.getzStepSetter(),
                            "originalAffineTransform3D", slicingTransfom), true);

            os.addObject(mp);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static  void adjustShiftSlicingTransform(AffineTransform3D slicingTransfom, long nX, long nY, long nZ) {
        AffineTransform3D notShifted = new AffineTransform3D();
        notShifted.set(slicingTransfom);
        notShifted.set(0,0,3);
        notShifted.set(0,1,3);
        notShifted.set(0,2,3);

        RealPoint pt = new RealPoint(nX, nY, nZ);

        RealPoint ptRealSpace = new RealPoint(3);

        notShifted.apply(pt, ptRealSpace);

        slicingTransfom.set(-ptRealSpace.getDoublePosition(0)/2.0, 0,3);
        slicingTransfom.set(-ptRealSpace.getDoublePosition(1)/2.0, 1,3);
        slicingTransfom.set(-ptRealSpace.getDoublePosition(2)/2.0, 2,3);
    }
}
