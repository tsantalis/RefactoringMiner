package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.utils.SequenceAlgorithms;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

import java.util.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class BasicTreeMatcher implements TreeMatcher {

	@Override
	public void match(Tree src, Tree dst, AbstractCodeMapping abstractCodeMapping, ExtendedMultiMappingStore mappingStore) {
		basicMatcher(src, dst, mappingStore);
	}

	public void basicMatcher(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
		//TODO: Migration fix
		MappingStore match = new CustomGreedy(0, false).match(src, dst);
		mappingStore.add(match);
		greedyMatcher(src, dst,mappingStore);
	}

	public void greedyMatcher(Tree src, Tree dst, ExtendedMultiMappingStore mappings) {
		double simThreshold = 0.5;
		for (Tree t : src.postOrder()) {
			boolean _flag = true;
			if (mappings.isSrcMapped(t)) {
				ArrayList<Tree> temp = new ArrayList<>(mappings.getDsts(t));
				for (Tree tree : temp) {
					if (TreeUtils.preOrder(dst).contains(tree)) {
						_flag = false;
						break;
					}
				}
			}
			if (t.isRoot()) {
				mappings.addMapping(t, dst);
				lastChanceMatch(t,dst,mappings);
				break;
			} else if (!(mappings.isSrcMapped(t) || t.isLeaf()) || (!t.isLeaf() && _flag)) {
				List<Tree> candidates = getDstCandidates(mappings, t, dst);
				Tree best = null;
				double max = -1D;
				for (Tree cand : candidates) {
					double sim = diceSimilarity(t, cand, mappings);
					if (sim > max && sim >= simThreshold) {
						max = sim;
						best = cand;
					}
				}

				if (best != null) {
					if (!mappings.isDstMapped(best)) {
						mappings.addMapping(t, best);
						lastChanceMatch(t, best, mappings);
					}
					else
					{
						Set<Tree> srcForDst = mappings.getSrcs(best);
						boolean _check = true;
						for (Tree srcMapped : srcForDst)
							if (src.getDescendants().contains(srcMapped))
							{
								_check = false;
								break;
							}
						if (_check) {
							mappings.addMapping(t, best);
							lastChanceMatch(t, best, mappings);
						}
					}
					// TODO: 8/2/2022 Might be mapped from other trees but it must be discarded
				}
			}
		}
	}

	protected List<Tree> getDstCandidates(ExtendedMultiMappingStore mappings, Tree src, Tree checkingInsideDst) {
		List<Tree> seeds = new ArrayList<>();
		for (Tree c : src.getDescendants()) {
			if (mappings.isSrcMapped(c)) {
				for (Tree t : mappings.getDsts(c)) {
					if (TreeUtils.preOrder(checkingInsideDst).contains(t))
						seeds.add(t);
				}
			}
		}
		List<Tree> candidates = new ArrayList<>();
		Set<Tree> visited = new HashSet<>();
		for (Tree seed : seeds) {
			while (seed.getParent() != null) {
				Tree parent = seed.getParent();
				if (visited.contains(parent))
					break;
				visited.add(parent);
				if (parent.getType() == src.getType() && !parent.isRoot())
					if (!(mappings.isDstMapped(parent)))
						candidates.add(parent);
					else {
						List<Tree> mappedParent = new ArrayList<>(mappings.getSrcs(parent));
						boolean flag = true;
						for (Tree mp : mappedParent) {
							if (TreeUtils.preOrder(checkingInsideDst).contains(mp)) {
								flag = false;
								break;
							}
						}
						if (flag)
							candidates.add(parent);
					}
				seed = parent;
			}
		}
		return candidates;
	}

	protected void lastChanceMatch(Tree src, Tree dst,ExtendedMultiMappingStore mappings) {
		lcsEqualMatching(mappings, src, dst);
		lcsStructureMatching(mappings, src, dst);
		histogramMatching(src, dst, mappings);
	}

	protected void lcsEqualMatching(ExtendedMultiMappingStore mappings, Tree src, Tree dst) {
		List<Tree> unmappedSrcChildren = new ArrayList<>();
		List<Tree> unmappedDstChildren = new ArrayList<>();
		for (Tree c : src.getChildren()) {
			if (mappings.isSrcMapped(c)) {
				boolean _flag = false;
				Set<Tree> dstForSrc = mappings.getDsts(c);
				for (Tree dstMapped : dstForSrc) {
					if (TreeUtils.preOrder(dst).contains(dstMapped)) {
						_flag = true;
						break;
					}
				}
				if (_flag)
					continue;
			}
			unmappedSrcChildren.add(c);
		}

		for (Tree c : dst.getChildren()) {
			if (mappings.isDstMapped(c)) {
				boolean _flag = false;
				Set<Tree> srcForDst = mappings.getSrcs(c);
				for (Tree srcMapped : srcForDst) {
					if (TreeUtils.preOrder(src).contains(srcMapped)) {
						_flag = true;
						break;
					}
				}
				if (_flag)
					continue;
			}
			unmappedDstChildren.add(c);
		}

		List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsomorphism(
				unmappedSrcChildren, unmappedDstChildren);
		for (int[] x : lcs) {
			var t1 = unmappedSrcChildren.get(x[0]);
			var t2 = unmappedDstChildren.get(x[1]);
			//TODO:
			if (mappings.areSrcsUnmapped(TreeUtils.preOrder(t1),dst) && mappings.areDstsUnmapped(
					TreeUtils.preOrder(t2),src))
				mappings.addMappingRecursively(t1, t2);
		}
	}

	protected void lcsStructureMatching(ExtendedMultiMappingStore mappings, Tree src, Tree dst) {
		List<Tree> unmappedSrcChildren = new ArrayList<>();
		List<Tree> unmappedDstChildren = new ArrayList<>();
		for (Tree c : src.getChildren()) {
			if (mappings.isSrcMapped(c)) {
				boolean _flag = false;
				Set<Tree> dstForSrc = mappings.getDsts(c);
				for (Tree dstMapped : dstForSrc) {
					if (TreeUtils.preOrder(dst).contains(dstMapped)) {
						_flag = true;
						break;
					}
				}
				if (_flag)
					continue;
			}
			unmappedSrcChildren.add(c);
		}

		for (Tree c : dst.getChildren()) {
			if (mappings.isDstMapped(c)) {
				boolean _flag = false;
				Set<Tree> srcForDst = mappings.getSrcs(c);
				for (Tree srcMapped : srcForDst) {
					if (TreeUtils.preOrder(src).contains(srcMapped)) {
						_flag = true;
						break;
					}
				}
				if (_flag)
					continue;
			}
			unmappedDstChildren.add(c);
		}

		List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsostructure(
				unmappedSrcChildren, unmappedDstChildren);
		for (int[] x : lcs) {
			var t1 = unmappedSrcChildren.get(x[0]);
			var t2 = unmappedDstChildren.get(x[1]);
			if (mappings.areSrcsUnmapped(
					TreeUtils.preOrder(t1),dst) && mappings.areDstsUnmapped(TreeUtils.preOrder(t2),src))
				mappings.addMappingRecursively(t1, t2);
		}
	}

	protected void histogramMatching(Tree src, Tree dst, ExtendedMultiMappingStore mappings) {
		Map<Type, List<Tree>> srcHistogram = new HashMap<>();
		for (var c :  src.getChildren()) {
			if (mappings.isSrcMapped(c)) {
				boolean _flag = false;
				Set<Tree> dstForSrc = mappings.getDsts(c);
				for (Tree dstMapped : dstForSrc) {
					if (TreeUtils.preOrder(dst).contains(dstMapped))
					{
						_flag = true;
						break;
					}
				}
				if (_flag)
					continue;
			}
			srcHistogram.putIfAbsent(c.getType(), new ArrayList<>());
			srcHistogram.get(c.getType()).add(c);
		}

		Map<Type, List<Tree>> dstHistogram = new HashMap<>();
		for (var c : dst.getChildren()) {
			if (mappings.isDstMapped(c)) {
				boolean _flag = false;
				Set<Tree> srcForDst = mappings.getSrcs(c);
				for (Tree mappedSrc : srcForDst) {
					if (TreeUtils.preOrder(src).contains(mappedSrc))
					{
						_flag = true;
						break;
					}
				}
				if (_flag)
					continue;
			}
			dstHistogram.putIfAbsent(c.getType(), new ArrayList<>());
			dstHistogram.get(c.getType()).add(c);
		}

		for (Type t : srcHistogram.keySet()) {
			if (dstHistogram.containsKey(t) && srcHistogram.get(t).size() == 1 && dstHistogram.get(t).size() == 1) {
				var srcChild = srcHistogram.get(t).get(0);
				var dstChild = dstHistogram.get(t).get(0);
				mappings.addMapping(srcChild, dstChild);
				lastChanceMatch(srcChild, dstChild,mappings);
			}
		}
	}

	public static double diceSimilarity(Tree src, Tree dst, ExtendedMultiMappingStore mappings) {
		return SimilarityMetrics.diceCoefficient(numberOfMappedDescendants(src, dst, mappings),
				src.getDescendants().size(), dst.getDescendants().size());
	}

	private static int numberOfMappedDescendants(Tree src, Tree dst, ExtendedMultiMappingStore mappings) {
		//Set<Tree> dstDescendants = new HashSet<>(dst.getCustomDescendants("Block"));
		Set<Tree> dstDescendants = new HashSet<>(dst.getDescendants());
		int mappedDescendants = 0;

		for (var srcDescendant : src.getDescendants()) {
			if (mappings.isSrcMapped(srcDescendant)) {
				Set<Tree> dstForSrcDescendantList = mappings.getDsts(srcDescendant);

				Tree dstDescendantsTree;
				if (dstForSrcDescendantList.size() > 1)
				{
					for(Tree d : dstForSrcDescendantList)
						if (dstDescendants.contains(d)) {
							mappedDescendants++;
							break;
						}
				}
				dstDescendantsTree = mappings.getDsts(srcDescendant).iterator().next();
				if (dstDescendants.contains(dstDescendantsTree))
					mappedDescendants++;
			}
		}
		return mappedDescendants;
	}
}
