package org.refactoringminer.astDiff.matchers.vanilla;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.utils.SequenceAlgorithms;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 * @since   2022-12-27 8:46 p.m.
 */
public class CustomBottomUpMatcher implements Matcher {
	public CustomBottomUpMatcher() {
	}

	@Override
	public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
		for (Tree t : src.postOrder()) {
			if (t.isRoot()) {
				if (t.getType().name.equals(dst.getType().name))
				{
					if (!mappings.isSrcMapped(t) && !mappings.isDstMapped(dst))
						mappings.addMapping(t, dst);
				}
				lastChanceMatch(mappings, t, dst);
				break;
			}
			else if (!(mappings.isSrcMapped(t) || t.isLeaf())) {
				List<Tree> candidates = getDstCandidates(mappings, t);
				Tree best = null;
				var max = -1D;
				var tSize = t.getDescendants().size();

				for (var candidate : candidates) {
					var threshold = 1D / (1D + Math.log(candidate.getDescendants().size() + tSize));
					var sim = SimilarityMetrics.chawatheSimilarity(t, candidate, mappings);
					if (sim > max && sim >= threshold) {
						max = sim;
						best = candidate;
					}
				}

				if (best != null) {
					lastChanceMatch(mappings, t, best);
					boolean checkOperatorOfInfixExpression = checkInfixExpression(mappings, t, best);
					if (checkOperatorOfInfixExpression)
						mappings.addMapping(t, best);
				}
			}
			else if (mappings.isSrcMapped(t) && mappings.hasUnmappedSrcChildren(t)
					&& mappings.hasUnmappedDstChildren(mappings.getDstForSrc(t)))
				lastChanceMatch(mappings, t, mappings.getDstForSrc(t));
		}
		return mappings;
	}

	private static boolean checkInfixExpression(MappingStore mappings, Tree t, Tree best) {
		boolean checkOperatorOfInfixExpression = true;
		if (t.getType().name.equals(Constants.INFIX_EXPRESSION))
		{
			checkOperatorOfInfixExpression = false;
			Tree a = TreeUtilFunctions.findChildByType(t, Constants.INFIX_EXPRESSION_OPERATOR);
			Tree b = TreeUtilFunctions.findChildByType(best, Constants.INFIX_EXPRESSION_OPERATOR);
			if (mappings.getDstForSrc(a) == b) checkOperatorOfInfixExpression = true;
		}
		return checkOperatorOfInfixExpression;
	}

	protected List<Tree> getDstCandidates(MappingStore mappings, Tree src) {
		List<Tree> seeds = new ArrayList<>();
		for (Tree c : src.getDescendants()) {
			Tree m = mappings.getDstForSrc(c);
			if (m != null)
				seeds.add(m);
		}
		List<Tree> candidates = new ArrayList<>();
		Set<Tree> visited = new HashSet<>();
		for (var seed : seeds) {
			while (seed.getParent() != null) {
				var parent = seed.getParent();
				if (visited.contains(parent))
					break;
				visited.add(parent);
				if (parent.getType() == src.getType() && !mappings.isDstMapped(parent) && !parent.isRoot())
					candidates.add(parent);
				seed = parent;
			}
		}
		return candidates;
	}

	protected void lastChanceMatch(MappingStore mappings, Tree src, Tree dst) {
		lcsEqualMatching(mappings, src, dst);
		lcsStructureMatching(mappings, src, dst);
		histogramMatching(mappings, src, dst);
	}

	protected void lcsEqualMatching(MappingStore mappings, Tree src, Tree dst) {
		List<Tree> unmappedSrcChildren = new ArrayList<>();
		for (Tree c : src.getChildren())
			if (!mappings.isSrcMapped(c))
				unmappedSrcChildren.add(c);

		List<Tree> unmappedDstChildren = new ArrayList<>();
		for (Tree c : dst.getChildren())
			if (!mappings.isDstMapped(c))
				unmappedDstChildren.add(c);

		List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsomorphism(
				unmappedSrcChildren, unmappedDstChildren);
		for (int[] x : lcs) {
			var t1 = unmappedSrcChildren.get(x[0]);
			var t2 = unmappedDstChildren.get(x[1]);
			if (mappings.areSrcsUnmapped(TreeUtils.preOrder(t1)) && mappings.areDstsUnmapped(
					TreeUtils.preOrder(t2)))
				mappings.addMappingRecursively(t1, t2);
		}
	}

	protected void lcsStructureMatching(MappingStore mappings, Tree src, Tree dst) {
		List<Tree> unmappedSrcChildren = new ArrayList<>();
		for (Tree c : src.getChildren())
			if (!mappings.isSrcMapped(c))
				unmappedSrcChildren.add(c);

		List<Tree> unmappedDstChildren = new ArrayList<>();
		for (Tree c : dst.getChildren())
			if (!mappings.isDstMapped(c))
				unmappedDstChildren.add(c);

		List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsostructure(
				unmappedSrcChildren, unmappedDstChildren);
		for (int[] x : lcs) {
			var t1 = unmappedSrcChildren.get(x[0]);
			var t2 = unmappedDstChildren.get(x[1]);
			if (mappings.areSrcsUnmapped(
					TreeUtils.preOrder(t1)) && mappings.areDstsUnmapped(TreeUtils.preOrder(t2)))
				mappings.addMappingRecursively(t1, t2);
		}
	}

	protected void histogramMatching(MappingStore mappings, Tree src, Tree dst) {
		Map<Type, List<Tree>> srcHistogram = new HashMap<>();
		for (var c :  src.getChildren()) {
			if (mappings.isSrcMapped(c))
				continue;
			srcHistogram.putIfAbsent(c.getType(), new ArrayList<>());
			srcHistogram.get(c.getType()).add(c);
		}

		Map<Type, List<Tree>> dstHistogram = new HashMap<>();
		for (var c : dst.getChildren()) {
			if (mappings.isDstMapped(c))
				continue;
			dstHistogram.putIfAbsent(c.getType(), new ArrayList<>());
			dstHistogram.get(c.getType()).add(c);
		}

		for (Type t : srcHistogram.keySet()) {
			if (dstHistogram.containsKey(t) && srcHistogram.get(t).size() == 1 && dstHistogram.get(t).size() == 1) {
				var srcChild = srcHistogram.get(t).get(0);
				var dstChild = dstHistogram.get(t).get(0);
				mappings.addMapping(srcChild, dstChild);
				lastChanceMatch(mappings, srcChild, dstChild);
			}
		}
	}
}
