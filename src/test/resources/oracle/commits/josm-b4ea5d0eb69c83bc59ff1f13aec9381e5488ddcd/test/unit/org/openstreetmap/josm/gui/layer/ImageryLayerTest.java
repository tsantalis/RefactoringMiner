// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ImageryLayer} class.
 */
@BasicPreferences
class ImageryLayerTest {
    /**
     * Unit test of {@link ImageryLayer#getFilterSettings()}
     */
    @Test
    void testHasSettings() {
        ImageryLayer layer = TMSLayerTest.createTmsLayer();
        ImageryFilterSettings settings = layer.getFilterSettings();
        assertNotNull(settings);
        assertSame(settings, layer.getFilterSettings());
    }
}
