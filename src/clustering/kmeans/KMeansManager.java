/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package clustering.kmeans;

/**
 *
 * @author valerio
 */
import ij.process.ByteProcessor;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import util.ColorManager;
import util.ColorSpaceConversion;
import vectorLib.VectorProcessor;


public class KMeansManager {

   private int randomizationSeed = 48;
   private double tolerance = 0.0001;
   private int numberOfClusters = 4;
   private boolean grayScaleVisualization = true;
   private boolean randomRGBVisualization = false;
   private boolean binaryStackVisualization = false;
   private boolean clusterCenterColorsVisualization = false;
   private static final String[] initModes = {"random (Forgy)", "K-Means++"};
   private String initializationMode = "K-Means++";
   private static final String[] colorSpaces = {"None", "XYZ", "L*a*b*", "HSB"};
   private String colorSpace = "None";
   private int imageType;

   public int getRandomizationSeed() {
        return randomizationSeed;
   }

   public void setRandomizationSeed(final int randomizationSeed) {
        this.randomizationSeed = randomizationSeed;
   }
   
   public int getNumberOfClusters() {
       return numberOfClusters;
   }

   public void setNumberOfClusters(final int numberOfClusters) {
       this.numberOfClusters = numberOfClusters;
   }

    /**
     * Return tolerance used to determine cluster centroid distance. This tolerance is used to
     * determine if a centroid changed location between iterations.
     *
     * @return cluster centroid location tolerance.
     */
   public double getTolerance() {
       return tolerance;
   }

   public void setTolerance(final float tolerance) {
       this.tolerance = tolerance;
   }

   public boolean getGrayScaleVisualization(){
       return grayScaleVisualization;
   }

   public void setGrayScaleVisualization(boolean visualization){
       grayScaleVisualization = visualization;
   }

   public boolean getRandomRGBVisualization(){
       return randomRGBVisualization;
   }

   public void setRandomRGBVisualization(boolean visualization){
       randomRGBVisualization = visualization;
   }

   public boolean getBinaryStackVisualization(){
       return binaryStackVisualization;
   }

   public void setBinaryStackVisualization(boolean visualization){
       binaryStackVisualization = visualization;
   }

   public boolean getClusterCenterColorsVisualization(){
       return clusterCenterColorsVisualization;
   }

   public void setClusterCenterColorsVisualization(boolean visualization){
       clusterCenterColorsVisualization = visualization;
   }

   public String getInizializationMode(){
       return initializationMode;
   }

   public void setInitializationMode(String initMode){
       initializationMode = initMode;
   }

   public String[] getInitModes(){
       return initModes;
   }

   public String[] getColorSpaces(){
       return colorSpaces;
   }

   public String getColorSpace(){
       return colorSpace;
   }

   public void setColorSpace(String space){
       colorSpace = space;
   }

   public ImageStack[] run(ImagePlus img)
    {
        ImageStack[] imgArray = null;
        imageType = img.getType();

//        final ImagePlus stack = convertToFloatStack(img);
//        ImageStack ims = stack.getStack();
//
//        System.out.println("Stack slices "+ ims.getSize());
//        VectorProcessor vp = new VectorProcessor(ims);

        VectorProcessor vp = convertToColorSpace(img, colorSpace);

        float [][] imageData = vp.getPixels();

        int initMode = computeInitializationMode();
        
        /* Calling the clustering algorithm */
        final long startTime = System.currentTimeMillis();
        Object[] resultMatrixes = KMeans.run(imageData, numberOfClusters, tolerance,
                                             randomizationSeed, initMode);
        final long endTime = System.currentTimeMillis();
        System.out.println("Clustering completed in " + (endTime - startTime) + " ms.");
        
        int [][] clusterMemberships = (int[][]) resultMatrixes[0];
        float [][] clusterCenters = (float[][]) resultMatrixes[1];

        Object[] stacks = new Object[4];
        //imgArray = new ImageStack[4];
        int visualizationModes = 0;

        if (grayScaleVisualization)
        {
            ImageStack imgsbp = new ImageStack(vp.getWidth(), vp.getHeight());
            imgsbp.addSlice("GrayScale", encodeClusteredImageInGray(vp, clusterMemberships, clusterCenters));
            stacks[visualizationModes] = imgsbp;
            visualizationModes++;
        }

        if (randomRGBVisualization)
        {
            ImageStack imgsbp2 = new ImageStack(vp.getWidth(), vp.getHeight());
            imgsbp2.addSlice("RGB", encodeClusteredImageInRGB(vp, clusterMemberships, clusterCenters, null, randomizationSeed));
            stacks[visualizationModes] = imgsbp2;
            visualizationModes++;
        }

        if (binaryStackVisualization)
        {
            stacks[visualizationModes] = encodeClusteredImageBinaryStack(vp, clusterMemberships, clusterCenters);
            visualizationModes++;
        }

        if (clusterCenterColorsVisualization)
        {
            float [][] computedClusterCenters = convertMatrixColorSpace(clusterCenters, colorSpace);
            stacks[visualizationModes] = encodeClusteredImageWithClusterCenterColors(vp, clusterMemberships, computedClusterCenters);
            visualizationModes++;
        }

        imgArray = new ImageStack[visualizationModes];
        for (int i = 0; i < visualizationModes; i++)
        {
            imgArray[i] = (ImageStack)stacks[i];
        }

        return imgArray;
    }

   
   private VectorProcessor convertToColorSpace(ImagePlus imp, String colorSpace){

       VectorProcessor vp = null;

       if (colorSpace.equals("XYZ"))
       {
            vp = ColorSpaceConversion.rgbToXYZVectorProcessor((ColorProcessor)imp.getProcessor());
       }
       else if (colorSpace.equals("L*a*b*"))
       {
            vp = ColorSpaceConversion.rgbToLabVectorProcessor((ColorProcessor)imp.getProcessor());
       }
       else if (colorSpace.equals("HSB"))
       {
//            final ImagePlus stack = convertToHSBFloatStack(imp);
//           ColorProcessor cp = (ColorProcessor)imp.getProcessor();
//
//           ImageStack ims = cp.getHSBStack();
//           final ImagePlus stack = convertToHSBFloatStack(imp);
//            ImageStack imss = stack.getStack();
//           //ImageStack stack = convertToHSBFloatStack(ims);
//            vp = new VectorProcessor(imss);
            final ImagePlus stack = convertToHSBFloatStack(imp);
            ImageStack ims = stack.getStack();
            System.out.println("Stack slices "+ ims.getSize());
            vp = new VectorProcessor(ims);
            float [] hsb = vp.get(3, 44);

            System.out.println("HS "+ hsb[0] + " " + hsb[1] + " " + hsb[2]);
       }
//       else if (colorSpace.equals("YCrCb"))
//       {
//            final ByteProcessor[] bps = ColorSpaceConversion.rgbToYCbCr((ColorProcessor)imp.getProcessor());
//            final ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight());
//            stack.addSlice("Y", bps[0]);
//            stack.addSlice("Cb", bps[1]);
//            stack.addSlice("Cr", bps[2]);
//            vp = new VectorProcessor(stack);
//       }
       else // if (colorSpace.equals("None"))
       {
            final ImagePlus stack = convertToFloatStack(imp);
            ImageStack ims = stack.getStack();

            System.out.println("Stack slices "+ ims.getSize());
            vp = new VectorProcessor(ims);
       }

       return vp;
   }


    private int computeInitializationMode(){

        int initMode = -1;

        for (int i = 0; i < initModes.length; i++)
        {
            if (initializationMode.equals(initModes[i]))
            {
                initMode = i;
            }
        }

        if (initMode == -1){
            throw new IllegalArgumentException("Invalid Initialization Mode");
        }
        return initMode;
    }

    private ByteProcessor encodeClusteredImageInGray(VectorProcessor vp, int [][] clusterMemberships, float [][] clusterCenters){

        final ByteProcessor dest = new ByteProcessor(vp.getWidth(), vp.getHeight());
        final VectorProcessor.PixelIterator iterator = vp.pixelIterator();
        final int nClusters = clusterCenters.length;
        while (iterator.hasNext())
        {
            final float[] v = iterator.next();
            for (int j = 0; j < nClusters; j++)
            {
                if (clusterMemberships[iterator.getOffset()][j] == 1)
                {
                    dest.putPixel(iterator.getX(), iterator.getY(), j);
                }
            }
        }
        return dest;
    }

   private ImageStack encodeClusteredImageBinaryStack(VectorProcessor vp, int [][] clusterMemberships, float [][] clusterCenters){

        final ImageStack dest = new ImageStack(vp.getWidth(), vp.getHeight());
        final VectorProcessor.PixelIterator iterator = vp.pixelIterator();
        final int nClusters = clusterCenters.length;

        ByteProcessor[] binaryClusters = new ByteProcessor[nClusters];
        for (int i = 0; i < nClusters; i++)
        {
            binaryClusters[i] = new ByteProcessor(vp.getWidth(), vp.getHeight());
        }

        while (iterator.hasNext())
        {
            final float[] v = iterator.next();
            for (int j = 0; j < nClusters; j++)
            {
                if (clusterMemberships[iterator.getOffset()][j] == 1)
                {
                    binaryClusters[j].putPixel(iterator.getX(), iterator.getY(), 0);
                }
                else
                {
                    binaryClusters[j].putPixel(iterator.getX(), iterator.getY(), 255);
                }
            }
        }
        
        for (int i = 0; i < nClusters; i++)
        {
            dest.addSlice("", binaryClusters[i]);
        }

        return dest;
    }

    private ColorProcessor encodeClusteredImageInRGB(VectorProcessor vp, int [][] clusterMemberships,
                                                     float [][] clusterCenters, int [] mixingColor,
                                                     int randomizationSeed){

        final ColorProcessor dest = new ColorProcessor(vp.getWidth(), vp.getHeight());
        final VectorProcessor.PixelIterator iterator = vp.pixelIterator();
        final int nClusters = clusterCenters.length;

        /* Creating an array of random RGB colors, one for each cluster */
        int [][] clusterColors = new int [nClusters][4];
        for (int i = 0; i < nClusters; i++)
        {
            clusterColors[i] = ColorManager.randomMixedRGBColor(mixingColor);
            System.out.println("C" + i + " " + clusterColors[i][0] + " " + clusterColors[i][1] + " " + clusterColors[i][2]);
        }

        while (iterator.hasNext())
        {
            final float[] v = iterator.next();
            for (int j = 0; j < nClusters; j++)
            {
                if (clusterMemberships[iterator.getOffset()][j] == 1)
                {
                    //System.out.println("C" + j + " " + clusterColors[j][0] + " " + clusterColors[j][1] + " " + clusterColors[j][2]);
                    dest.putPixel(iterator.getX(), iterator.getY(), clusterColors[j]);
                    //dest.putPixel(iterator.getX(), iterator.getY(), clusterColors[j][0]*clusterColors[j][1]*clusterColors[j][2]);
                }
            }
        }

        System.out.println(dest.getColor(4, 33));
        return dest;
    }

    private ImageStack encodeClusteredImageWithClusterCenterColors(VectorProcessor vp, int [][] clusterMemberships, float [][] clusterCenters){

        final ImageStack dest = new ImageStack(vp.getWidth(), vp.getHeight());
        final VectorProcessor.PixelIterator iterator = vp.pixelIterator();
        final int nClusters = clusterCenters.length;

        for (int i = 0; i < vp.getNumberOfValues(); i++)
        {
            dest.addSlice("Band i", new FloatProcessor(vp.getWidth(), vp.getHeight()));
        }

        final Object[] pixels = dest.getImageArray();

        while (iterator.hasNext())
        {
            final float[] v = iterator.next();
            for (int j = 0; j < nClusters; j++)
            {
                if (clusterMemberships[iterator.getOffset()][j] == 1)
                {
                    for (int k = 0; k < vp.getNumberOfValues(); k++)
                    {
                        ((float[]) pixels[k])[iterator.getOffset()] = clusterCenters[j][k];
                    }
                }
            }
        }

        return dest;
    }

    private float [][] convertMatrixColorSpace(float [][] dataFeatureMatrix, String colorSpace){

        float [][] convertedMatrix = null;

        if (colorSpace.equals("L*a*b*"))
        {
            convertedMatrix = ColorSpaceConversion.labToRGBMatrix(dataFeatureMatrix);
        }
        else if (colorSpace.equals("XYZ"))
        {
            convertedMatrix = ColorSpaceConversion.xyzToRGBMatrix(dataFeatureMatrix);
        }
        else if (colorSpace.equals("HSB"))
        {
            convertedMatrix = ColorSpaceConversion.hsbToRGBMatrix(dataFeatureMatrix);
        }
//        else if (colorSpace.equals("YCrCb"))
//        {
//        }
        else // if (colorSpace.equals("None"))
        {
            convertedMatrix  = dataFeatureMatrix;
        }

        return convertedMatrix;
    }

    /**
     * Convert image to a stack of FloatProcessors.
     *
     * @param src image to convert.
     * @return float stack.
     */
    private static ImagePlus convertToFloatStack(final ImagePlus src) {

        final ImagePlus dest = new Duplicator().run(src);

        // Remember scaling setup
        final boolean doScaling = ImageConverter.getDoScaling();

        try {
            // Disable scaling
            ImageConverter.setDoScaling(false);

            if (src.getType() == ImagePlus.COLOR_RGB)
            {
                if (src.getStackSize() > 1)
                {
                    throw new IllegalArgumentException("Unsupported image type: RGB with more than one slice.");
                }

                final ImageConverter converter = new ImageConverter(dest);
                converter.convertToRGBStack();
            }

            if (dest.getStackSize() > 1)
            {
                final StackConverter converter = new StackConverter(dest);
                converter.convertToGray32();

            } 
            else
            {
                final ImageConverter converter = new ImageConverter(dest);
                converter.convertToGray32();
            }

            return dest;
        } 
        finally
        {
            // Restore original scaling option
            ImageConverter.setDoScaling(doScaling);
        }
    }

    private static ImagePlus convertToHSBFloatStack(final ImagePlus src) {

        final ImagePlus dest = new Duplicator().run(src);

        // Remember scaling setup
        final boolean doScaling = ImageConverter.getDoScaling();

        try {
            // Disable scaling
            ImageConverter.setDoScaling(false);

            if (src.getType() == ImagePlus.COLOR_RGB)
            {
                if (src.getStackSize() > 1)
                {
                    throw new IllegalArgumentException("Unsupported image type: RGB with more than one slice.");
                }

                final ImageConverter converter = new ImageConverter(dest);
                converter.convertToHSB();
            }

            if (dest.getStackSize() > 1)
            {
                final StackConverter converter = new StackConverter(dest);
                converter.convertToGray32();

            }
            else
            {
                final ImageConverter converter = new ImageConverter(dest);
                converter.convertToGray32();
            }

            return dest;
        }
        finally
        {
            // Restore original scaling option
            ImageConverter.setDoScaling(doScaling);
        }
    }
}
