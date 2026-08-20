// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TagMergeModel} class.
 */
@SuppressWarnings("unchecked")
class TagMergeModelTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    protected Set<PropertyChangeListener> getListeners(TagMergeModel model) throws ReflectiveOperationException {
        return (Set<PropertyChangeListener>) TestUtils.getPrivateField(model, "listeners");
    }

    protected List<TagMergeItem> getTagMergeItems(TagMergeModel model) throws ReflectiveOperationException {
        return (List<TagMergeItem>) TestUtils.getPrivateField(model, "tagMergeItems");
    }

    @Test
    void testAddPropertyChangeListener() throws ReflectiveOperationException {
        TagMergeModel model = new TagMergeModel();
        PropertyChangeListener listener = evt -> {
        };
        model.addPropertyChangeListener(listener);

        Set<?> list = getListeners(model);

        assertEquals(1, list.size());
        assertEquals(listener, list.iterator().next());
    }

    @Test
    void testRemovePropertyChangeListener() throws ReflectiveOperationException {
        TagMergeModel model = new TagMergeModel();
        PropertyChangeListener listener = evt -> {
        };
        model.addPropertyChangeListener(listener);
        model.removePropertyChangeListener(listener);

        Set<?> list = getListeners(model);

        assertEquals(0, list.size());
    }

    @Test
    void testPopulateNoConflichts() throws ReflectiveOperationException {
        Node my = new Node(1);
        Node their = new Node(1);
        TagMergeModel model = new TagMergeModel();
        model.populate(my, their);

        List<TagMergeItem> list = getTagMergeItems(model);

        assertEquals(0, list.size());
    }

    @Test
    void testPopulateNoConflicts1() throws ReflectiveOperationException {
        Node my = new Node(1);
        my.put("key", "value");
        Node their = new Node(1);
        their.put("key", "value");
        TagMergeModel model = new TagMergeModel();
        model.populate(my, their);

        List<TagMergeItem> list = getTagMergeItems(model);

        assertEquals(0, list.size());
    }

    @Test
    void testPopulateMissingKeyMine() throws ReflectiveOperationException {
        Node my = new Node(1);
        Node their = new Node(1);
        their.put("key", "value");
        TagMergeModel model = new TagMergeModel();
        model.populate(my, their);

        List<TagMergeItem> list = getTagMergeItems(model);

        assertEquals(1, list.size());
        TagMergeItem item = list.get(0);
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
        assertEquals("key", item.getKey());
        assertNull(item.getMyTagValue());
        assertEquals("value", item.getTheirTagValue());
    }

    @Test
    void testPopulateMissingKeyTheir() throws ReflectiveOperationException {
        Node my = new Node(1);
        my.put("key", "value");
        Node their = new Node(1);
        TagMergeModel model = new TagMergeModel();
        model.populate(my, their);

        List<TagMergeItem> list = getTagMergeItems(model);

        assertEquals(1, list.size());
        TagMergeItem item = list.get(0);
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
        assertEquals("key", item.getKey());
        assertNull(item.getTheirTagValue());
        assertEquals("value", item.getMyTagValue());
    }

    @Test
    void testPopulateConflictingValues() throws ReflectiveOperationException {
        Node my = new Node(1);
        my.put("key", "myvalue");
        Node their = new Node(1);
        their.put("key", "theirvalue");
        TagMergeModel model = new TagMergeModel();
        model.populate(my, their);

        List<TagMergeItem> list = getTagMergeItems(model);

        assertEquals(1, list.size());
        TagMergeItem item = list.get(0);
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
        assertEquals("key", item.getKey());
        assertEquals("myvalue", item.getMyTagValue());
        assertEquals("theirvalue", item.getTheirTagValue());
    }

    @Test
    void testAddItem() throws ReflectiveOperationException {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        TagMergeModel model = new TagMergeModel();
        model.addItem(item);

        List<TagMergeItem> list = getTagMergeItems(model);

        assertEquals(1, list.size());
        item = list.get(0);
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
        assertEquals("key", item.getKey());
        assertEquals("myvalue", item.getMyTagValue());
        assertEquals("theirvalue", item.getTheirTagValue());
    }

    @Test
    void testDecide() throws ReflectiveOperationException {
        TagMergeItem item = new TagMergeItem("key", "myvalue", "theirvalue");
        TagMergeModel model = new TagMergeModel();
        model.addItem(item);

        List<TagMergeItem> list = getTagMergeItems(model);

        model.decide(0, MergeDecisionType.KEEP_MINE);
        assertEquals(1, list.size());
        item = list.get(0);
        assertEquals(MergeDecisionType.KEEP_MINE, item.getMergeDecision());

        model.decide(0, MergeDecisionType.KEEP_THEIR);
        assertEquals(1, list.size());
        item = list.get(0);
        assertEquals(MergeDecisionType.KEEP_THEIR, item.getMergeDecision());

        model.decide(0, MergeDecisionType.UNDECIDED);
        assertEquals(1, list.size());
        item = list.get(0);
        assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
    }

    @Test
    void testDecideMultiple() throws ReflectiveOperationException {

        TagMergeModel model = new TagMergeModel();
        for (int i = 0; i < 10; i++) {
            model.addItem(new TagMergeItem("key-" + i, "myvalue-" + i, "theirvalue-" +i));
        }

        List<TagMergeItem> list = getTagMergeItems(model);

        assertEquals(10, list.size());

        model.decide(new int[] {0, 3, 5}, MergeDecisionType.KEEP_MINE);
        for (int i = 0; i < 10; i++) {
            TagMergeItem item = list.get(i);
            if (i == 0 || i == 3 || i == 5) {
                assertEquals(MergeDecisionType.KEEP_MINE, item.getMergeDecision());
            } else {
                assertEquals(MergeDecisionType.UNDECIDED, item.getMergeDecision());
            }
        }
    }
}
