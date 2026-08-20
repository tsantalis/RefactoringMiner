// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link GeoImageLayer} class.
 */
// Basic preferences are needed for OSM primitives
@BasicPreferences
class GeoImageLayerTest {

    /**
     * Test that {@link GeoImageLayer#mergeFrom} throws IAE for invalid arguments
     */
    @Test
    void testMergeFromIAE() {
        assertThrows(IllegalArgumentException.class, () -> new GeoImageLayer(null, null).mergeFrom(new OsmDataLayer(new DataSet(), "", null)));
    }
}
