package org.refactoringminer.astDiff.tests;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeMetricComputer;
import com.github.gumtreediff.tree.TreeVisitor;
import com.github.gumtreediff.tree.TypeSet;
import org.junit.jupiter.api.Test;
import org.refactoringminer.astDiff.matchers.vanilla.CustomTopDownMatcher;
import org.refactoringminer.astDiff.utils.Constants;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class CustomTopDownMatcherPerformanceTest {
	private static final int REPEATED_SUBTREE_COUNT = 300;
	private static final int ORDERING_EQUIVALENCE_SUBTREE_COUNT = 60;

	@Test
	public void retainsMappingsForPathologicallyAmbiguousSubtreeGroups() {
		List<Tree> srcRepeatedSubtrees = new ArrayList<>();
		List<Tree> dstRepeatedSubtrees = new ArrayList<>();
		Tree src = repeatedTree("src-root", "src-marker", srcRepeatedSubtrees);
		Tree dst = repeatedTree("dst-root", "dst-marker", dstRepeatedSubtrees);
		Constants javaConstants = new Constants(".java");

		MappingStore mappings = assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
				new CustomTopDownMatcher(0, javaConstants, javaConstants).match(src, dst));

		assertSame(dstRepeatedSubtrees.get(0), mappings.getDstForSrc(srcRepeatedSubtrees.get(0)));
		assertSame(dstRepeatedSubtrees.get(REPEATED_SUBTREE_COUNT - 1),
				mappings.getDstForSrc(srcRepeatedSubtrees.get(REPEATED_SUBTREE_COUNT - 1)));
	}

	@Test
	public void optimizedRankingMatchesOriginalComparatorRanking() {
		List<Tree> srcRepeatedSubtrees = new ArrayList<>();
		List<Tree> dstRepeatedSubtrees = new ArrayList<>();
		Tree src = repeatedTree("src-root", "src-marker", srcRepeatedSubtrees, ORDERING_EQUIVALENCE_SUBTREE_COUNT);
		Tree dst = repeatedTree("dst-root", "dst-marker", dstRepeatedSubtrees, ORDERING_EQUIVALENCE_SUBTREE_COUNT);
		Constants javaConstants = new Constants(".java");

		MappingStore optimized = new CustomTopDownMatcher(0, javaConstants, javaConstants).match(src, dst);
		MappingStore reference = new ReferenceCustomTopDownMatcher(0, javaConstants, javaConstants).match(src, dst);

		assertEquals(reference.size(), optimized.size());
		for (Mapping mapping : reference) {
			assertSame(mapping.second, optimized.getDstForSrc(mapping.first));
		}
	}

	private static Tree repeatedTree(String rootLabel, String markerLabel, List<Tree> repeatedSubtrees) {
		return repeatedTree(rootLabel, markerLabel, repeatedSubtrees, REPEATED_SUBTREE_COUNT);
	}

	private static Tree repeatedTree(String rootLabel, String markerLabel, List<Tree> repeatedSubtrees,
									 int repeatedSubtreeCount) {
		Tree root = tree("root", rootLabel, 0);
		int position = 1;
		for (int i = 0; i < repeatedSubtreeCount; i++) {
			Tree repeated = repeatedSubtree(position);
			root.addChild(repeated);
			repeatedSubtrees.add(repeated);
			position += 3;
		}
		root.addChild(markerSubtree(markerLabel, position));
		TreeVisitor.visitTree(root, new TreeMetricComputer());
		return root;
	}

	private static Tree repeatedSubtree(int position) {
		Tree call = tree("call", "expect", position);
		call.addChild(tree("identifier", "expect", position + 1));
		call.addChild(tree("identifier", "env", position + 2));
		return call;
	}

	private static Tree markerSubtree(String label, int position) {
		Tree marker = tree("call", label, position);
		marker.addChild(tree("identifier", label, position + 1));
		return marker;
	}

	private static Tree tree(String type, String label, int position) {
		DefaultTree tree = new DefaultTree(TypeSet.type(type), label);
		tree.setPos(position);
		tree.setLength(1);
		return tree;
	}
}
