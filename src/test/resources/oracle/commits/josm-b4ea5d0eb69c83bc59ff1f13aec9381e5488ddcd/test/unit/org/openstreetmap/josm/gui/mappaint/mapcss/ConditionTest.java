// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.TagCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.Op;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.SimpleKeyValueCondition;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * This test universally tests all {@link Condition}s.
 * @author Michael Zangl
 */
// We need prefs for nodes
@BasicPreferences
class ConditionTest {
    private OsmPrimitive node0;
    private OsmPrimitive node1;
    private OsmPrimitive node2;
    private OsmPrimitive node3;
    private OsmPrimitive node4;

    /**
     * Set up some useful test data.
     */
    @BeforeEach
    public void setUp() {
        node0 = OsmUtils.createPrimitive("n");
        node1 = OsmUtils.createPrimitive("n k1=v1 k2=v1 f1=0.2 r1=ababx c1=xya one=a;b");
        node2 = OsmUtils.createPrimitive("n k1=v1 k2=v1a f1=0.8 r1=abxabxab c1=xy one=a;;x");
        node3 = OsmUtils.createPrimitive("n k1=v1 f1=-100 c1=axy one=x;y;z");
        node4 = OsmUtils.createPrimitive("n k1=v2a k2=v3 f1=x r1=abab c1=axya one=x;a;y");
    }

    /**
     * Test {@link Op#EQ}.
     */
    @Test
    void testKeyValueEq() {
        Condition op = ConditionFactory.createKeyValueCondition("k1", "v1", Op.EQ, Context.PRIMITIVE, false);
        assertFalse(op.applies(genEnv(node0)));
        assertTrue(op.applies(genEnv(node1)));
        assertTrue(op.applies(genEnv(node2)));
        assertTrue(op.applies(genEnv(node3)));
        assertFalse(op.applies(genEnv(node4)));

        assertTrue(op instanceof SimpleKeyValueCondition);
        assertEquals("[k1=v1]", op.toString());
        assertEquals("k1", ((TagCondition) op).asTag(null).getKey());
        assertEquals("v1", ((TagCondition) op).asTag(null).getValue());
    }

    /**
     * Test {@link Op#EQ} and interpretation as key
     */
    @Test
    void testKeyValueEqAsKey() {
        Condition op = ConditionFactory.createKeyValueCondition("k1", "k2", Op.EQ, Context.PRIMITIVE, true);
        assertFalse(op.applies(genEnv(node0)));
        assertTrue(op.applies(genEnv(node1)));
        assertFalse(op.applies(genEnv(node2)));
        assertFalse(op.applies(genEnv(node3)));
        assertFalse(op.applies(genEnv(node4)));

        assertEquals("k1", ((TagCondition) op).asTag(null).getKey());
        assertEquals("k2", ((TagCondition) op).asTag(null).getValue());
    }

    /**
     * Test {@link Op#NEQ}
     */
    @Test
    void testKeyValueNeq() {
        Condition op = ConditionFactory.createKeyValueCondition("k1", "v1", Op.NEQ, Context.PRIMITIVE, false);
        assertTrue(op.applies(genEnv(node0)));
        assertFalse(op.applies(genEnv(node1)));
        assertFalse(op.applies(genEnv(node2)));
        assertFalse(op.applies(genEnv(node3)));
        assertTrue(op.applies(genEnv(node4)));
    }

    /**
     * Test {@link Op#GREATER_OR_EQUAL}
     */
    @Test
    void testKeyValueGreatherEq() {
        Condition op = ConditionFactory.createKeyValueCondition("f1", "0.2", Op.GREATER_OR_EQUAL, Context.PRIMITIVE, false);
        assertFalse(op.applies(genEnv(node0)));
        assertTrue(op.applies(genEnv(node1)));
        assertTrue(op.applies(genEnv(node2)));
        assertFalse(op.applies(genEnv(node3)));
        assertFalse(op.applies(genEnv(node4)));
    }

    /**
     * Test {@link Op#GREATER}
     */
    @Test
    void testKeyValueGreather() {
        Condition op = ConditionFactory.createKeyValueCondition("f1", "0.2", Op.GREATER, Context.PRIMITIVE, false);
        assertFalse(op.applies(genEnv(node0)));
        assertFalse(op.applies(genEnv(node1)));
        assertTrue(op.applies(genEnv(node2)));
        assertFalse(op.applies(genEnv(node3)));
        assertFalse(op.applies(genEnv(node4)));
    }

    /**
     * Test {@link Op#LESS_OR_EQUAL}
     */
    @Test
    void testKeyValueLessEq() {
        Condition op = ConditionFactory.createKeyValueCondition("f1", "0.2", Op.LESS_OR_EQUAL, Context.PRIMITIVE, false);
        assertFalse(op.applies(genEnv(node0)));
        assertTrue(op.applies(genEnv(node1)));
        assertFalse(op.applies(genEnv(node2)));
        assertTrue(op.applies(genEnv(node3)));
        assertFalse(op.applies(genEnv(node4)));
    }

    /**
     * Test {@link Op#LESS}
     */
    @Test
    void testKeyValueLess() {
        Condition op = ConditionFactory.createKeyValueCondition("f1", "0.2", Op.LESS, Context.PRIMITIVE, false);
        assertFalse(op.applies(genEnv(node0)));
        assertFalse(op.applies(genEnv(node1)));
        assertFalse(op.applies(genEnv(node2)));
        assertTrue(op.applies(genEnv(node3)));
        assertFalse(op.applies(genEnv(node4)));
    }

    /**
     * Test {@link Op#REGEX}
     */
    @Test
    void testKeyValueRegex() {
        Condition op = ConditionFactory.createKeyValueCondition("r1", "(ab){2}", Op.REGEX, Context.PRIMITIVE, false);
        assertFalse(op.applies(genEnv(node0)));
        assertTrue(op.applies(genEnv(node1)));
        assertFalse(op.applies(genEnv(node2)));
        assertFalse(op.applies(genEnv(node3)));
        assertTrue(op.applies(genEnv(node4)));
    }

    /**
     * Test {@link Op#NREGEX}
     */
    @Test
    void testKeyValueNregex() {
        Condition op = ConditionFactory.createKeyValueCondition("r1", "(ab){2}", Op.NREGEX, Context.PRIMITIVE, false);
        assertTrue(op.applies(genEnv(node0)));
        assertFalse(op.applies(genEnv(node1)));
        assertTrue(op.applies(genEnv(node2)));
        assertTrue(op.applies(genEnv(node3)));
        assertFalse(op.applies(genEnv(node4)));
    }

    /**
     * Test {@link Op#ONE_OF}
     */
    @Test
    void testKeyValueOneOf() {
        Condition op = ConditionFactory.createKeyValueCondition("one", "a", Op.ONE_OF, Context.PRIMITIVE, false);
        assertFalse(op.applies(genEnv(node0)));
        assertTrue(op.applies(genEnv(node1)));
        assertTrue(op.applies(genEnv(node2)));
        assertFalse(op.applies(genEnv(node3)));
        assertTrue(op.applies(genEnv(node4)));
    }

    /**
     * Test {@link Op#BEGINS_WITH}
     */
    @Test
    void testKeyValueBeginsWith() {
        Condition op = ConditionFactory.createKeyValueCondition("c1", "xy", Op.BEGINS_WITH, Context.PRIMITIVE, false);
        assertFalse(op.applies(genEnv(node0)));
        assertTrue(op.applies(genEnv(node1)));
        assertTrue(op.applies(genEnv(node2)));
        assertFalse(op.applies(genEnv(node3)));
        assertFalse(op.applies(genEnv(node4)));
    }

    /**
     * Test {@link Op#ENDS_WITH}
     */
    @Test
    void testKeyValueEndsWith() {
        Condition op = ConditionFactory.createKeyValueCondition("c1", "xy", Op.ENDS_WITH, Context.PRIMITIVE, false);
        assertFalse(op.applies(genEnv(node0)));
        assertFalse(op.applies(genEnv(node1)));
        assertTrue(op.applies(genEnv(node2)));
        assertTrue(op.applies(genEnv(node3)));
        assertFalse(op.applies(genEnv(node4)));
    }

    /**
     * Test {@link Op#CONTAINS}
     */
    @Test
    void testKeyValueContains() {
        Condition op = ConditionFactory.createKeyValueCondition("c1", "xy", Op.CONTAINS, Context.PRIMITIVE, false);
        assertFalse(op.applies(genEnv(node0)));
        assertTrue(op.applies(genEnv(node1)));
        assertTrue(op.applies(genEnv(node2)));
        assertTrue(op.applies(genEnv(node3)));
        assertTrue(op.applies(genEnv(node4)));
    }

    /**
     * Test of {@link ConditionFactory#createRegexpKeyRegexpValueCondition(String, String, Op)}
     */
    @Test
    void testRegexpKeyValueRegexpCondition() {
        Condition op = ConditionFactory.createRegexpKeyRegexpValueCondition("^k", "\\da", Op.REGEX);
        assertFalse(op.applies(genEnv(node0)));
        assertFalse(op.applies(genEnv(node1)));
        assertTrue(op.applies(genEnv(node2)));
        assertFalse(op.applies(genEnv(node3)));
        assertTrue(op.applies(genEnv(node4)));

        Condition notOp = ConditionFactory.createRegexpKeyRegexpValueCondition("^k", "\\da", Op.NREGEX);
        assertTrue(notOp.applies(genEnv(node0)));
        assertTrue(notOp.applies(genEnv(node1)));
        assertFalse(notOp.applies(genEnv(node2)));
        assertTrue(notOp.applies(genEnv(node3)));
        assertFalse(notOp.applies(genEnv(node4)));
    }

    private Environment genEnv(OsmPrimitive primitive) {
        return new Environment(primitive);
    }
}
