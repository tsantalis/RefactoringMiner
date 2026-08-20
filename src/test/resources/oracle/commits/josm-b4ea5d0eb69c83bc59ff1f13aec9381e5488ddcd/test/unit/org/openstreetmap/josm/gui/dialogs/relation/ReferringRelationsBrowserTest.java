// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link ReferringRelationsBrowser} class.
 */
@BasicPreferences
class ReferringRelationsBrowserTest {
    /**
     * Unit test of {@link ReferringRelationsBrowser#ReferringRelationsBrowser}.
     */
    @Test
    void testReferringRelationsBrowser() {
        DataSet ds = new DataSet();
        Relation r = new Relation();
        ds.addPrimitive(r);
        new ReferringRelationsBrowser(new OsmDataLayer(ds, "", null), new ReferringRelationsBrowserModel(r)).init();
    }
}
