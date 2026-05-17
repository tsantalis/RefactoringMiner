package narrator.graph.cluster.traverse;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import narrator.graph.NodeType;
import narrator.graph.cluster.traverse.ReasonType;

public class NarratorTest {

    // Dummy classes to avoid Node/Tree dependencies for the logic test
    private static class TestLeaf extends TraversalPattern implements Leaf {
        TestLeaf() {
            nodeType = NodeType.SINGULAR;
        }
        @Override public String getId() { return "leaf-" + System.identityHashCode(this); }
        @Override public String textualRepresentation(narrator.graph.cluster.Cluster cluster) { return "test-leaf"; }
    }

    private static class TestUsage extends AggregatorPattern {
        TestUsage() {
            nodeType = NodeType.USAGE;
        }
        @Override public String getId() { return "usage-" + System.identityHashCode(this); }
    }

    private static class TestComponent extends TraversalComponent {
        TestComponent(List<TraversalPattern> components, ReasonType reasonType) {
            super(components, reasonType);
        }
        @Override public String getId() { return "comp-" + System.identityHashCode(this); }
    }

    @Test
    public void testExcludeNonLeaves() {
        Narrator narrator = new Narrator();

        TestLeaf leaf = new TestLeaf();
        TestUsage usage = new TestUsage();
        TestComponent component = new TestComponent(Collections.singletonList(leaf), ReasonType.CONTEXT);

        List<TraversalPattern> patterns = Arrays.asList(leaf, usage, component);
        List<Leaf> result = narrator.narrate(patterns);

        assertEquals(1, result.size(), "Only TestLeaf should be included");
        assertTrue(result.contains(leaf));
        assertFalse(result.contains(usage), "Aggregators should be excluded");
        assertFalse(result.contains(component), "TraversalComponent should be excluded");
    }

    @Test
    public void testOrderDepth() {
        Narrator narrator = new Narrator();

        // Setup: Usage (Root) -> Usage (Middle) -> Leaf
        TestLeaf leaf = new TestLeaf();
        TestUsage middle = new TestUsage();
        TestUsage root = new TestUsage();

        middle.subs.add(leaf);
        root.subs.add(middle);

        List<TraversalPattern> patterns = Arrays.asList(root, middle, leaf);
        List<Leaf> result = narrator.narrate(patterns);

        assertEquals(1, result.size(), "Only the deepest leaf should be in the result");
        assertEquals(leaf, result.get(0), "Leaf should be first");
    }

    @Test
    public void testBranchingDependencies() {
        Narrator narrator = new Narrator();

        // Setup: Root -> (Leaf1, Leaf2)
        TestUsage root = new TestUsage();
        TestLeaf leaf1 = new TestLeaf();
        TestLeaf leaf2 = new TestLeaf();

        root.subs.add(leaf1);
        root.subs.add(leaf2);

        List<TraversalPattern> patterns = Arrays.asList(root, leaf1, leaf2);
        List<Leaf> result = narrator.narrate(patterns);

        assertEquals(2, result.size());
        assertTrue(result.contains(leaf1));
        assertTrue(result.contains(leaf2));
    }

    @Test
    public void testCycleHandling() {
        Narrator narrator = new Narrator();

        // Setup: Leaf1 -> Leaf2 -> Leaf1 (Cycle)
        // Using TestUsage here but they are NOT Leafs, so result should be empty
        TestUsage leaf1 = new TestUsage();
        TestUsage leaf2 = new TestUsage();

        leaf1.subs.add(leaf2);
        leaf2.subs.add(leaf1);

        List<TraversalPattern> patterns = Arrays.asList(leaf1, leaf2);
        List<Leaf> result = narrator.narrate(patterns);

        assertEquals(0, result.size(), "Should return empty as no Leaf objects are present");
    }
}
