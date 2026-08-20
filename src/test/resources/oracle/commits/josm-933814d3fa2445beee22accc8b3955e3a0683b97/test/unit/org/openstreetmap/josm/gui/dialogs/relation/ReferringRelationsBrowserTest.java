// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ReferringRelationsBrowser} class.
 */
class ReferringRelationsBrowserTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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
