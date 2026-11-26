/* 
 *  Copyright (c) 2009, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   
package org.jaitools.media.jai.maskedconvolve;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.util.Properties;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.registry.RenderedRegistryMode;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Unit tests for the MaskedConvolve operator
 * 
 * @author Michael Bedward
 */
public class TestMaskedConvolve {

    // round off tolerance for float comparisons
    private static final float FTOL = 1.0e-4f;

    public TestMaskedConvolve() {
        ensureRegistered();
        fixForOSX();
    }

    /**
     * Compare the results of JAI's standard Convolve
     * with MaskedConvolve for an symmetric kernel with
     * masking disabled.
     */
    @Test
    public void testSymmetric() {
        System.out.println("   testing symmetric kernel");

        int tileW = 128;
        JAI.setDefaultTileSize(new Dimension(tileW, tileW));
        RenderedImage testImg = getBorderTestImage(2*tileW, tileW);

        KernelJAI kernel = new KernelJAI(3, 3,
                new float[]{
                    1f, 1f, 1f,
                    1f, 0f, 1f,
                    1f, 1f, 1f});

        compareStandardToMasked(testImg, kernel);
    }

    /**
     * Compare the results of JAI's standard Convolve
     * with MaskedConvolve for an asymmetric kernel with
     * masking disabled.
     */
    @Test
    public void testAsymmetric() {
        System.out.println("   testing asymmetric kernel");

        int tileW = 128;
        JAI.setDefaultTileSize(new Dimension(tileW, tileW));
        RenderedImage testImg = getBorderTestImage(2*tileW, tileW);

        KernelJAI kernel = new KernelJAI(3, 3, 0, 0, new float[]{
                    0f, 1f, 1f,
                    1f, 1f, 1f,
                    1f, 1f, 1f
                });

        compareStandardToMasked(testImg, kernel);
    }

    /**
     * Compare the results of MaskedConvolve with source masking
     * enabled to those generated "by hand" using standard JAI
     * operators
     */
    @Test
    public void testMaskSource() {
        System.out.println("   testing source masking");

        int tileW = 128;
        JAI.setDefaultTileSize(new Dimension(tileW, tileW));
        RenderedImage testImg = getRandomTestImage(1.0f, 5.0f, 2*tileW, tileW);

        KernelJAI kernel = new KernelJAI(3, 3,
                new float[]{
                    0.5f, 1.0f, 0.5f,
                    1.0f, 1.0f, 1.0f,
                    0.5f, 1.0f, 0.5f}
        );

        /*
         * Create an ROI that includes all pixels in the test image
         * with values >= 3
         */
        ROI roi = new ROI(testImg, 3);

        ParameterBlockJAI pb = new ParameterBlockJAI("maskedconvolve");
        pb.setSource("source0", testImg);
        pb.setParameter("kernel", kernel);
        pb.setParameter("roi", roi);
        pb.setParameter("masksource", true);
        pb.setParameter("maskdest", false);

        RenderingHints hints = new RenderingHints(
                JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO));

        RenderedOp maskedConvImg = JAI.create("maskedconvolve", pb, hints);

        /*
         * Now we repeat the masked convolve as we would with JAI's standard
         * operators - note the amount of additional code required :)
         */

        // binarize on the same threshold used above
        pb = new ParameterBlockJAI("binarize");
        pb.setSource("source0", testImg);
        pb.setParameter("threshold", 3.0);
        RenderedOp bin3Img = JAI.create("binarize", pb);

        // convert the binary image to TYPE_INT with values 0 or 1
        // (otherwise we get (0/255)
        pb = new ParameterBlockJAI("lookup");
        pb.setSource("source0", bin3Img);
        pb.setParameter("table", new LookupTableJAI(new int[]{0, 1}));
        RenderedOp bin301Img = JAI.create("lookup", pb);

        // multiply the images to get a masked source image
        pb = new ParameterBlockJAI("multiply");
        pb.setSource("source0", testImg);
        pb.setSource("source1", bin301Img);
        RenderedOp multImg = JAI.create("multiply", pb);

        // perform convolution on the masked source image
        pb = new ParameterBlockJAI("convolve");
        pb.setSource("source0", multImg);
        pb.setParameter("kernel", kernel);
        RenderedOp stdConvImg = JAI.create("convolve", pb, hints);

        /*
         * Now compare the results of the two convolutions
         */
        compareImages(stdConvImg, maskedConvImg);
    }

    /**
     * Compare the results of MaskedConvolve with destination masking
     * enabled to those generated "by hand" using standard JAI
     * operators
     */
    @Test
    public void testMaskDest() {
        System.out.println("   testing destination masking");

        int tileW = 128;
        JAI.setDefaultTileSize(new Dimension(tileW, tileW));
        RenderedImage testImg = getRandomTestImage(1.0f, 5.0f, 2*tileW, tileW);

        KernelJAI kernel = new KernelJAI(3, 3,
                new float[]{
                    0.5f, 1.0f, 0.5f,
                    1.0f, 1.0f, 1.0f,
                    0.5f, 1.0f, 0.5f}
        );

        /*
         * Create an ROI that includes all pixels in the test image
         * with values >= 3. When we peform MaskedConvolve operation
         * with destination masking this will give us a result image
         * with value 0 in all of the destination pixels corresponding
         * to source pixel values < 3
         */
        ROI roi = new ROI(testImg, 3);

        ParameterBlockJAI pb = new ParameterBlockJAI("maskedconvolve");
        pb.setSource("source0", testImg);
        pb.setParameter("kernel", kernel);
        pb.setParameter("roi", roi);
        pb.setParameter("masksource", false);
        pb.setParameter("maskdest", true);

        RenderingHints hints = new RenderingHints(
                JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO));

        RenderedOp maskedConvImg = JAI.create("maskedconvolve", pb, hints);

        /*
         * Now we repeat the masked convolve as we would with JAI's standard
         * operators - note the amount of additional code required :)
         */

        // binarize on the same threshold used above
        pb = new ParameterBlockJAI("binarize");
        pb.setSource("source0", testImg);
        pb.setParameter("threshold", 3.0);
        RenderedOp bin3Img = JAI.create("binarize", pb);

        // convert the binary image to TYPE_INT with values 0 or 1
        // (otherwise we get (0/255)
        pb = new ParameterBlockJAI("lookup");
        pb.setSource("source0", bin3Img);
        pb.setParameter("table", new LookupTableJAI(new int[]{0, 1}));
        RenderedOp bin301Img = JAI.create("lookup", pb);

        // perform convolution on the test image image
        pb = new ParameterBlockJAI("convolve");
        pb.setSource("source0", testImg);
        pb.setParameter("kernel", kernel);
        RenderedOp convImg = JAI.create("convolve", pb, hints);

        // multiply the result of the convolution by the int mask image
        pb = new ParameterBlockJAI("multiply");
        pb.setSource("source0", convImg);
        pb.setSource("source1", bin301Img);
        RenderedOp multImg = JAI.create("multiply", pb);

        /*
         * Now compare the results of the two convolutions
         */
        compareImages(multImg, maskedConvImg);
    }

    /**
     * Compare the results of MaskedConvolve with both source and
     * destination masking enabled to those generated "by hand" using
     * standard JAI operators
     */
    @Test
    public void testMaskSourceAndDest() {
        System.out.println("   testing source and destination masking");

        int tileW = 128;
        JAI.setDefaultTileSize(new Dimension(tileW, tileW));
        RenderedImage testImg = getRandomTestImage(1.0f, 5.0f, 2*tileW, tileW);

        KernelJAI kernel = new KernelJAI(3, 3,
                new float[]{
                    0.5f, 1.0f, 0.5f,
                    1.0f, 1.0f, 1.0f,
                    0.5f, 1.0f, 0.5f}
        );

        /*
         * Create an ROI that includes all pixels in the test image
         * with values >= 3. When we peform MaskedConvolve operation
         * with destination masking this will give us a result image
         * with value 0 in all of the destination pixels corresponding
         * to source pixel values < 3
         */
        ROI roi = new ROI(testImg, 3);

        ParameterBlockJAI pb = new ParameterBlockJAI("maskedconvolve");
        pb.setSource("source0", testImg);
        pb.setParameter("kernel", kernel);
        pb.setParameter("roi", roi);
        pb.setParameter("masksource", true);
        pb.setParameter("maskdest", true);

        RenderingHints hints = new RenderingHints(
                JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO));

        RenderedOp maskedConvImg = JAI.create("maskedconvolve", pb, hints);

        /*
         * Now we repeat the masked convolve as we would with JAI's standard
         * operators - note the amount of additional code required :)
         */

        // binarize on the same threshold used above
        pb = new ParameterBlockJAI("binarize");
        pb.setSource("source0", testImg);
        pb.setParameter("threshold", 3.0);
        RenderedOp bin3Img = JAI.create("binarize", pb);

        // convert the binary image to TYPE_INT with values 0 or 1
        // (otherwise we get (0/255)
        pb = new ParameterBlockJAI("lookup");
        pb.setSource("source0", bin3Img);
        pb.setParameter("table", new LookupTableJAI(new int[]{0, 1}));
        RenderedOp bin301Img = JAI.create("lookup", pb);

        // multiply the test image with the int mask image
        pb = new ParameterBlockJAI("multiply");
        pb.setSource("source0", testImg);
        pb.setSource("source1", bin301Img);
        RenderedOp maskedTestImg = JAI.create("multiply", pb);

        // perform convolution on the masked test image image
        pb = new ParameterBlockJAI("convolve");
        pb.setSource("source0", maskedTestImg);
        pb.setParameter("kernel", kernel);
        RenderedOp convImg = JAI.create("convolve", pb, hints);

        // multiply the result of the convolution by the int mask image
        pb = new ParameterBlockJAI("multiply");
        pb.setSource("source0", convImg);
        pb.setSource("source1", bin301Img);
        RenderedOp finalImg = JAI.create("multiply", pb);

        /*
         * Now compare the results of the two convolutions
         */
        compareImages(finalImg, maskedConvImg);
    }


    /**
     * Run a comparison of results between standard convolution and
     * mssked convolution with masking disabled
     */
    private void compareStandardToMasked(RenderedImage testImg, KernelJAI kernel) {
        ParameterBlockJAI pb = new ParameterBlockJAI("convolve");
        pb.setSource("source0", testImg);
        pb.setParameter("kernel", kernel);
        RenderingHints hints = new RenderingHints(
                JAI.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
        RenderedOp stdConvImg = JAI.create("convolve", pb, hints);

        pb = new ParameterBlockJAI("maskedconvolve");
        pb.setSource("source0", testImg);
        pb.setParameter("kernel", kernel);
        ROI roi = new ROI(testImg, -1);
        pb.setParameter("roi", roi);
        RenderedOp maskedConvImg = JAI.create("maskedconvolve", pb, hints);
        
        compareImages(stdConvImg, maskedConvImg);
    }

    private void compareImages(RenderedImage img1, RenderedImage img2) {
        RectIter iter1 = RectIterFactory.create(img1, null);
        RectIter iter2 = RectIterFactory.create(img2, null);

        boolean iter1Done, iter2Done;
        int x = 0, y = 0;
        do {
            do {
                float val1 = iter1.getSampleFloat();
                float val2 = iter2.getSampleFloat();
                assertTrue(
                        String.format("images differ at %d:%d values: %f %f",
                                         x, y, val1, val2),
                        Math.abs(val1 - val2) < FTOL);

                iter1Done = iter1.nextPixelDone();
                iter2Done = iter2.nextPixelDone();
                x++ ;
            } while (!iter1Done && !iter2Done);

            if (!(iter1Done && iter2Done)) {
                // images out of sync
                fail("images out of sync");
            }

            iter1.startPixels();
            iter2.startPixels();
            x = 0;
            iter1Done = iter1.nextLineDone();
            iter2Done = iter2.nextLineDone();
            y++ ;

        } while (!iter1Done && !iter2Done);

        if (!(iter1Done && iter2Done)) {
            // images out of sync
            fail("images out of sync");
        }
    }

    /**
     * Create a test image two tiles wide, one tile high, with edge
     * pixels having value 0 and inner pixels having value 1
     */
    private RenderedImage getBorderTestImage(int width, int height) {
        ParameterBlockJAI pb = new ParameterBlockJAI("constant");
        pb.setParameter("width", (float) width);
        pb.setParameter("height", (float) height);
        pb.setParameter("bandvalues", new Integer[]{0});
        RenderedOp zeroImg = JAI.create("constant", pb);

        pb = new ParameterBlockJAI("constant");
        pb.setParameter("width", width - 2f);
        pb.setParameter("height", height - 2f);
        pb.setParameter("bandvalues", new Integer[]{1});
        RenderedOp oneImg = JAI.create("constant", pb);

        pb = new ParameterBlockJAI("translate");
        pb.setSource("source0", oneImg);
        pb.setParameter("xtrans", 1.0f);
        pb.setParameter("ytrans", 1.0f);
        RenderedOp oneImgT = JAI.create("translate", pb);

        pb = new ParameterBlockJAI("overlay");
        pb.setSource("source0", zeroImg);
        pb.setSource("source1", oneImgT);
        RenderedImage overlayImg = JAI.create("overlay", pb);

        return overlayImg;
    }

    /*
     * Get a test image with pixel values randomly chosen between
     * minValue and maxValue
     */
    private RenderedImage getRandomTestImage(float minValue, float maxValue, int width, int height) {
        ParameterBlockJAI pb = new ParameterBlockJAI("imagefunction");
        pb.setParameter("function", new RandomImageFunction(minValue, maxValue));
        pb.setParameter("width", width);
        pb.setParameter("height", height);
        RenderedOp op = JAI.create("imagefunction", pb);

        return op.getAsBufferedImage();
    }

    /**
     * Register the operator with JAI if it is not already registered
     */
    private void ensureRegistered() {
        OperationRegistry reg = JAI.getDefaultInstance().getOperationRegistry();
        String[] names = reg.getDescriptorNames(RenderedRegistryMode.MODE_NAME);
        MaskedConvolveDescriptor desc = new MaskedConvolveDescriptor();
        String descName = desc.getName();
        for (String name : names) {
            if (descName.equalsIgnoreCase(name)) {
                return;
            }
        }

        MaskedConvolveSpi spi = new MaskedConvolveSpi();
        spi.updateRegistry(reg);
    }

    /**
     * If we are running on OSX, turn off native acceleration
     * for JAI operations so that operators work properly
     * with float and double data rasters.
     */
    private void fixForOSX() {
        Properties sys = new Properties(System.getProperties());
        if (sys.getProperty("os.name").compareToIgnoreCase("mac os x") == 0) {
            sys.put("com.sun.media.jai.disableMediaLib", "true");
        }
        System.setProperties(sys);
    }
    
}
