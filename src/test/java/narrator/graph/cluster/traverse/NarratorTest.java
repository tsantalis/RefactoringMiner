package narrator.graph.cluster.traverse;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class NarratorTest {

    // Dummy classes to avoid Node/Tree dependencies for the logic test
    private static class TestPattern extends TraversalPattern {
        TestPattern() {
            // We cannot call super because the Node constructor is too complex for this test
        }
        @Override public String getId() { return "test-" + System.identityHashCode(this); }
    }

    private static class TestAggregator extends AggregatorPattern {
        TestAggregator() {
        }
    }

    @Test
    public void testOrderDepth() {
        Narrator narrator = new Narrator();

        // Setup: Root -> Middle -> Leaf
        TestAggregator root = new TestAggregator();
        TestAggregator middle = new TestAggregator();
        TestPattern leaf = new TestPattern();

        middle.subs.add(leaf);
        root.subs.add(middle);

        List<TraversalPattern> patterns = Arrays.asList(root, middle, leaf);
        List<TraversalPattern> result = narrator.narrate(patterns);

        assertEquals(3, result.size());
        assertEquals(leaf, result.get(0), "Leaf should be first");
        assertEquals(middle, result.get(1), "Middle should be second");
        assertEquals(root, result.get(2), "Root should be third");
    }

    @Test
    public void testBranchingDependencies() {
        Narrator narrator = new Narrator();

        // Setup: Root -> (Leaf1, Leaf2)
        TestAggregator root = new TestAggregator();
        TestPattern leaf1 = new TestPattern();
        TestPattern leaf2 = new TestPattern();

        root.subs.add(leaf1);
        root.subs.add(leaf2);

        List<TraversalPattern> patterns = Arrays.asList(root, leaf1, leaf2);
        List<TraversalPattern> result = narrator.narrate(patterns);

        assertEquals(3, result.size());
        assertEquals(root, result.get(2), "Root should be last");
        assertTrue(result.indexOf(leaf1) < 2);
        assertTrue(result.indexOf(leaf2) < 2);
    }
}
