// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TagMergeItem} class.
 */
class TagMergeItemTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    @Test
    void testTagMergeItem() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        assertEquals("key", item.getKey());
        assertEquals("myvalue", item.getMyTagValue());
        assertEquals("theirvalue", item.getTheirTagValue());
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
    }

    @Test
    void testTagMergeItem2() {
        Node n1 = new Node(1);
        Node n2 = new Node(1);
        n1.put("key", "myvalue");
        n2.put("key", "theirvalue");

        TagMergeItem item = new TagMergeItem("key", n1, n2);
        assertEquals("key", item.getKey());
        assertEquals("myvalue", item.getMyTagValue());
        assertEquals("theirvalue", item.getTheirTagValue());
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
    }

    @Test
    void testTagMergeItem3() {
        Node n1 = new Node(1);
        Node n2 = new Node(1);
        n1.put("key", "myvalue");
        // n2 does not have this key

        TagMergeItem item = new TagMergeItem("key", n1, n2);
        assertEquals("key", item.getKey());
        assertEquals("myvalue", item.getMyTagValue());
        assertNull(item.getTheirTagValue());
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
    }

    @Test
    void testTagMergeItem4() {
        Node n1 = new Node(1);
        Node n2 = new Node(1);
        // n1 does not have this key
        // n1.put("key", "myvalue");
        n2.put("key", "theirvalue");

        TagMergeItem item = new TagMergeItem("key", n1, n2);
        assertEquals("key", item.getKey());
        assertNull(item.getMyTagValue());
        assertEquals("theirvalue", item.getTheirTagValue());
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
    }

    @Test
    void testDecide() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        item.decide(MergeDecisionType.KEEP_MINE);
        assertEquals(MergeDecisionType.KEEP_MINE, item.getMergeDecision());
    }

    @Test
    void testDecide1() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        try {
            item.decide(null);
            fail("expected IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            // OK
            Logging.trace(e);
        }
    }

    @Test
    void testApplyToMyPrimitive() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        item.decide(MergeDecisionType.KEEP_MINE);

        Node n1 = new Node(1);
        n1.put("key", "oldvalue");
        item.applyToMyPrimitive(n1);
        assertEquals("myvalue", n1.get("key"));

        n1 = new Node(1);
        item.applyToMyPrimitive(n1);
        assertEquals("myvalue", n1.get("key"));
    }

    @Test
    void testApplyToMyPrimitive2() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        item.decide(MergeDecisionType.KEEP_THEIR);

        Node n1 = new Node(1);
        n1.put("key", "oldvalue");
        item.applyToMyPrimitive(n1);
        assertEquals("theirvalue", n1.get("key"));

        n1 = new Node(1);
        item.applyToMyPrimitive(n1);
        assertEquals("theirvalue", n1.get("key"));
    }

    @Test
    void testApplyToMyPrimitive3() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        // item is undecided
        // item.decide(MergeDecisionType.KEEP_THEIR);

        Node n1 = new Node(1);
        n1.put("key", "oldvalue");
        try {
            item.applyToMyPrimitive(n1);
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) {
            // OK
            Logging.trace(e);
        }
    }

    @Test
    void testApplyToMyPrimitive4() {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");

        try {
            item.applyToMyPrimitive(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Logging.trace(e);
        }
    }
}
