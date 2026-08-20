// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.layer.geoimage.ImageDisplay.VisRect;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ImageDisplay} class.
 */
@BasicPreferences
class ImageDisplayTest {
    /**
     * Unit test of {@link ImageDisplay#calculateDrawImageRectangle}.
     */
    @Test
    void testCalculateDrawImageRectangle() {
        assertEquals(new Rectangle(),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(), new Dimension()));
        assertEquals(new Rectangle(0, 0, 10, 5),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(10, 5)));
        assertEquals(new Rectangle(0, 0, 10, 5),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 20, 10), new Dimension(10, 5)));
        assertEquals(new Rectangle(0, 0, 20, 10),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(20, 10)));
        assertEquals(new Rectangle(5, 0, 24, 12),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(35, 12)));
        assertEquals(new Rectangle(0, 1, 8, 4),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(8, 6)));
    }

    /**
     * Performance test for {@link ImageDisplay.LoadImageRunnable}
     * @throws Exception if any error occurs
     */
    @Test
    @Disabled("Set working directory to image folder and run manually")
    void testLoadImageRunnablePerformance() throws Exception {
        ImageDisplay imageDisplay = new ImageDisplay(new ImageryFilterSettings());
        imageDisplay.setSize(640, 480);
        Graphics2D graphics = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB).createGraphics();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(".").toAbsolutePath(), "*.{JPG,jpg}")) {
            for (Path p : stream) {
                Runnable loadImage = imageDisplay.setImage0(new ImageEntry(p.toFile()));
                loadImage.run();
                imageDisplay.paintComponent(graphics);
            }
        }
    }
}
