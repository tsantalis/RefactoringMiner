// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of "Duplicate relation" validation test.
 */
class DuplicateRelationTest {

    /**
     * Setup test by initializing JOSM preferences and projection.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    static class ExpectedResult {
        final int code;
        final boolean fixable;
        ExpectedResult(int code, boolean fixable) {
            this.code = code;
            this.fixable = fixable;
        }
    }

    private void doTest(String tags1, String tags2, ExpectedResult... expectations) {
        performTest(buildDataSet(tags1, tags2), expectations);
    }

    private void performTest(DataSet ds, ExpectedResult... expectations) {
        final DuplicateRelation TEST = new DuplicateRelation();
        TEST.startTest(NullProgressMonitor.INSTANCE);
        TEST.visit(ds.allPrimitives());
        TEST.endTest();

        assertEquals(expectations.length, TEST.getErrors().size());
        int i = 0;
        for (TestError error : TEST.getErrors()) {
            ExpectedResult expected = expectations[i++];
            assertEquals(expected.code, error.getCode());
            assertEquals(expected.fixable, error.isFixable());
        }
    }

    private static DataSet buildDataSet(String tags1, String tags2) {
        DataSet ds = new DataSet();

        Node a = new Node(new LatLon(10.0, 5.0));
        ds.addPrimitive(a);
        ds.addPrimitive(TestUtils.newRelation(tags1, new RelationMember(null, a)));
        ds.addPrimitive(TestUtils.newRelation(tags2, new RelationMember(null, a)));
        return ds;
    }

    /**
     * Test of "Duplicate relation" validation test - no tags.
     */
    @Test
    void testDuplicateRelationNoTags() {
        doTest("", "",
                new ExpectedResult(DuplicateRelation.DUPLICATE_RELATION, true),
                new ExpectedResult(DuplicateRelation.SAME_RELATION, false));
    }

    /**
     * Test of "Duplicate relation" validation test - same tags.
     */
    @Test
    void testDuplicateRelationSameTags() {
        doTest("type=boundary", "type=boundary",
                new ExpectedResult(DuplicateRelation.DUPLICATE_RELATION, true),
                new ExpectedResult(DuplicateRelation.SAME_RELATION, false));
    }

    /**
     * Test of "Duplicate relation" validation test - different tags.
     */
    @Test
    void testDuplicateRelationDifferentTags() {
        doTest("type=boundary", "type=multipolygon",
                new ExpectedResult(DuplicateRelation.SAME_RELATION, false));
    }
}
