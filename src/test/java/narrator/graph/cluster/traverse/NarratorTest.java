package narrator.graph.cluster.traverse;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class NarratorTest {

    // Dummy classes to avoid Node/Tree dependencies for the logic test
    private static class TestLeaf extends TraversalPattern {
        TestLeaf() {
            nodeType = NodeType.SINGULAR;
        }
        @Override public String getId() { return "leaf-" + System.identityHashCode(this); }
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
        TestComponent component = new TestComponent(Collections.singletonList(leaf), ReasonType.SINK);

        List<TraversalPattern> patterns = Arrays.asList(leaf, usage, component);
        List<TraversalPattern> result = narrator.narrate(patterns);

        assertEquals(2, result.size(), " traversal component should be excluded");
        assertFalse(result.contains(component), "Result should not contain TraversalComponent");
        assertTrue(result.contains(leaf));
        assertTrue(result.contains(usage));
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
        List<TraversalPattern> result = narrator.narrate(patterns);

        assertEquals(3, result.size());
        assertEquals(leaf, result.get(0), "Deepest leaf should be first");
        assertEquals(middle, result.get(1), "Middle should be second");
        assertEquals(root, result.get(2), "Root should be third");
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
        List<TraversalPattern> result = narrator.narrate(patterns);

        assertEquals(3, result.size());
        assertEquals(root, result.get(2), "Root should be last");
        assertTrue(result.indexOf(leaf1) < 2);
        assertTrue(result.indexOf(leaf2) < 2);
    }

    @Test
    public void testCycleHandling() {
        Narrator narrator = new Narrator();

        // Setup: Leaf1 -> Leaf2 -> Leaf1 (Cycle)
        TestUsage leaf1 = new TestUsage();
        TestUsage leaf2 = new TestUsage();

        leaf1.subs.add(leaf2);
        leaf2.subs.add(leaf1);

        List<TraversalPattern> patterns = Arrays.asList(leaf1, leaf2);
        List<TraversalPattern> result = narrator.narrate(patterns);

        assertEquals(2, result.size(), "Should still return all leaves despite cycle");
        assertTrue(result.contains(leaf1));
        assertTrue(result.contains(leaf2));
    }
}
