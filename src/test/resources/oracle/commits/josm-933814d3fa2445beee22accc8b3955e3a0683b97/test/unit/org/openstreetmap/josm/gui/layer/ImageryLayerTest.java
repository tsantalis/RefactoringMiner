// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ImageryLayer} class.
 */
class ImageryLayerTest {

    /**
     * For creating layers
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
