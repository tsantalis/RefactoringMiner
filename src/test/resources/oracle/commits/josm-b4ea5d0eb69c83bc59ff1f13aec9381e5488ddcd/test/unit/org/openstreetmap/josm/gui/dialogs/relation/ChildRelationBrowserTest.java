// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link ChildRelationBrowser} class.
 */
@BasicPreferences
class ChildRelationBrowserTest {
    /**
     * Unit test of {@link ChildRelationBrowser#ChildRelationBrowser}.
     */
    @Test
    void testChildRelationBrowser() {
        DataSet ds = new DataSet();
        Relation r = new Relation();
        ds.addPrimitive(r);
        assertNotNull(new ChildRelationBrowser(new OsmDataLayer(ds, "", null), r));
    }
}
