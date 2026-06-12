package org.refactoringminer.astDiff.matchers.vanilla;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.heuristic.gt.*;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

public class CustomTopDownMatcher extends GreedySubtreeMatcher {
	private final Constants LANG1;
	private final Constants LANG2;
	public CustomTopDownMatcher(int minP, Constants LANG1, Constants LANG2) {
		super();
		this.LANG1 = LANG1;
		this.LANG2 = LANG2;
		setMinPriority(minP);
	}

	@Override
	protected void retainBestMapping(List<Mapping> mappingList, Set<Tree> srcIgnored, Set<Tree> dstIgnored) {
		List<Mapping> verifiedList = new ArrayList<>();
		for (Mapping mapping : mappingList) {
			if (TreeUtilFunctions.areBothFromThisType(mapping, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME) || TreeUtilFunctions.areBothFromThisType(mapping, LANG1.QUALIFIED_NAME, LANG2.QUALIFIED_NAME)) {
				if (isAcceptableMatch(mapping))
					verifiedList.add(mapping);
			}
			else verifiedList.add(mapping);
		}
		super.retainBestMapping(verifiedList, srcIgnored, dstIgnored);
	}

	private boolean isAcceptableMatch(Mapping mapping) {
		Tree first = mapping.first;
		Tree second = mapping.second;
		return isAcceptableMatch(first, second);
	}

	private boolean isAcceptableMatch(Tree first, Tree second) {
		if (isPartOfConditional(first, LANG1) && isPartOfConditional(second, LANG2))
			if (!isSamePositionInConditional(first,second))
				return false;
		if (isPartOfConditional(first, LANG1) && getIndexInConditional(first, LANG1) == 0 && !isPartOfConditional(second, LANG2))
			return false;
		if (isPartOfConditional(second, LANG2) && getIndexInConditional(second, LANG2) == 0 && !isPartOfConditional(first, LANG1))
			return false;
		return (!first.getParent().getType().name.equals(LANG1.METHOD_INVOCATION)
				||
				second.getParent().getType().name.equals(LANG2.METHOD_INVOCATION)) &&
				(!second.getParent().getType().name.equals(LANG2.METHOD_INVOCATION)
						||
						first.getParent().getType().name.equals(LANG1.METHOD_INVOCATION));
	}

	private boolean isOnlyOneMethodInvocation(Tree input1, Tree input2) {
		if (input1 == null || input2 == null) return false;
		if (input1.getParent() == null || input2.getParent() == null) return false;
		if (input1.getParent().getType().name.equals(LANG1.METHOD_INVOCATION) && !(input2.getParent().getType().name.equals(LANG2.METHOD_INVOCATION) || input2.getParent().getType().name.equals(LANG2.EXPRESSION_METHOD_REFERENCE)))
			return true;
		else if (!(input1.getParent().getType().name.equals(LANG1.METHOD_INVOCATION) || input1.getParent().getType().name.equals(LANG1.EXPRESSION_METHOD_REFERENCE)) && input2.getParent().getType().name.equals(LANG2.METHOD_INVOCATION))
			return true;
		return false;

	}

	private static boolean isPartOfConditional(Tree input, Constants LANG) {
		if (input.getType().name.equals(LANG.CONDITIONAL_EXPRESSION))
			return true;
		else {
			Tree parent = input.getParent();
			if (parent == null) return false;
			if (TreeUtilFunctions.isStatement(parent.getType().name, LANG)) return false;
			return isPartOfConditional(parent, LANG);
		}
	}

	private boolean isSamePositionInConditional(Tree input1, Tree input2) {
		int input1Index = getIndexInConditional(input1, LANG1);
		int input2Index = getIndexInConditional(input2, LANG2);
		return input1Index == input2Index;
	}

	private static int getIndexInConditional(Tree input, Constants LANG) {
		int inputIndex = 0;
		while (!input.getParent().getType().name.equals(LANG.CONDITIONAL_EXPRESSION))
			input = input.getParent();
		inputIndex = input.positionInParent();
		return inputIndex;
	}

	@Override
	public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
		this.src = src;
		this.dst = dst;
		this.mappings = mappings;

		var multiMappings = new MultiMappingStore();
		PriorityTreeQueue srcTrees = new DefaultPriorityTreeQueue(src, this.minPriority, this.priorityCalculator);
		PriorityTreeQueue dstTrees = new DefaultPriorityTreeQueue(dst, this.minPriority, this.priorityCalculator);

		while (!(srcTrees.isEmpty() || dstTrees.isEmpty())) {
			PriorityTreeQueue.synchronize(srcTrees, dstTrees);
			if (srcTrees.isEmpty() || dstTrees.isEmpty())
				break;

			var currentPrioritySrcTrees = srcTrees.pop();
			var currentPriorityDstTrees = dstTrees.pop();

			for (var currentSrc : currentPrioritySrcTrees)
				for (var currentDst : currentPriorityDstTrees)
					if (currentSrc.getMetrics().hash == currentDst.getMetrics().hash) {
						if (TreeUtilFunctions.isIsomorphicTo(currentSrc,currentDst)) {
							if (!isOnlyOneMethodInvocation(currentSrc, currentDst))
								multiMappings.addMapping(currentSrc, currentDst);
						}
					}
					else {
						if (currentSrc.getLabel().equals(currentDst.getLabel()))
						{
//							//TODO: Its definitely better to avoid this
//							if (currentSrc.getType().name.contains("_EXPRESSION_OPERATOR")
//									&&
//									currentDst.getType().name.contains("_EXPRESSION_OPERATOR"))
//								multiMappings.addMapping(currentSrc, currentDst);
						}
					}



			for (var t : currentPrioritySrcTrees)
				if (!multiMappings.hasSrc(t))
					srcTrees.open(t);
			for (var t : currentPriorityDstTrees)
				if (!multiMappings.hasDst(t))
					dstTrees.open(t);
		}

		filterMappings(multiMappings);
		return this.mappings;
	}

	@Override
	public void filterMappings(MultiMappingStore multiMappings) {
		// Select unique mappings first and extract ambiguous mappings.
		List<AmbiguousGroup> ambiguousGroups = new ArrayList<>();
		long ambiguousMappingCount = 0;
		Set<Tree> ignored = new HashSet<>();
		for (var src : multiMappings.allMappedSrcs()) {
			var isMappingUnique = false;
			if (multiMappings.isSrcUnique(src)) {
				var dst = multiMappings.getDsts(src).stream().findAny().get();
				if (multiMappings.isDstUnique(dst)) {
					mappings.addMappingRecursively(src, dst);
					isMappingUnique = true;
				}
			}

			if (!(ignored.contains(src) || isMappingUnique)) {
				var adsts = multiMappings.getDsts(src);
				var asrcs = multiMappings.getSrcs(multiMappings.getDsts(src).iterator().next());
				ambiguousGroups.add(new AmbiguousGroup(asrcs, adsts));
				ambiguousMappingCount += (long) asrcs.size() * adsts.size();
				ignored.addAll(asrcs);
			}
		}

		// Rank the mappings by score.
		ScoredMappingFactory scoredMappingFactory = new ScoredMappingFactory(mappings);
		scoredMappingFactory.prepare(ambiguousGroups);
		List<ScoredMapping> ambiguousList = new ArrayList<>(initialCapacity(ambiguousMappingCount));
		long order = 0;
		for (AmbiguousGroup ambiguousGroup : ambiguousGroups) {
			for (Tree asrc : ambiguousGroup.srcs)
				for (Tree adst : ambiguousGroup.dsts)
					ambiguousList.add(scoredMappingFactory.score(asrc, adst, order++));
		}
		Set<Tree> srcIgnored = new HashSet<>();
		Set<Tree> dstIgnored = new HashSet<>();
		Collections.sort(ambiguousList);
		if (ambiguousList.size() > 1)
			ambiguousLeafModification(ambiguousList,srcIgnored,dstIgnored);
		// Select the best ambiguous mappings
		retainBestScoredMapping(ambiguousList, srcIgnored, dstIgnored);
	}

	private int initialCapacity(long mappingCount) {
		return mappingCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) mappingCount;
	}

	private void ambiguousLeafModification(List<ScoredMapping> ambiguousList, Set<Tree> srcIgnored, Set<Tree> dstIgnored) {
		for (ScoredMapping scoredMapping : ambiguousList) {
			Tree asrc = scoredMapping.first;
			Tree adst = scoredMapping.second;

			if (!isAmbiguousLeafCandidate(asrc, adst)) {
				break;
			}

			if (asrc.getParent() == null || adst.getParent() == null) {
				continue;
			}

			int i = asrc.positionInParent();
			int j = adst.positionInParent();
			if (i == j && asrc.getParent().getType().name.equals(adst.getParent().getType().name)) {
				ignoreMapping(asrc, adst, srcIgnored, dstIgnored);
			}
			else if (i > 0 && j > 0) {
				Tree srcYoungerSibling = asrc.getParent().getChild(i - 1);
				Tree dstYoungerSibling = adst.getParent().getChild(j - 1);
				Tree mappedSrcYoungerSibling = mappings.getSrcForDst(dstYoungerSibling);
				if (srcYoungerSibling.equals(mappedSrcYoungerSibling)) {
					ignoreMapping(asrc, adst, srcIgnored, dstIgnored);
				}
			}
		}
	}

	private boolean isAmbiguousLeafCandidate(Tree asrc, Tree adst) {
		return (asrc.getType().name.equals(LANG1.STRING_LITERAL) && adst.getType().name.equals(LANG2.STRING_LITERAL))
				|| (asrc.getType().name.equals(LANG1.NUMBER_LITERAL) && adst.getType().name.equals(LANG2.NUMBER_LITERAL))
				|| (asrc.getType().name.equals(LANG1.PREFIX_EXPRESSION) && adst.getType().name.equals(LANG2.PREFIX_EXPRESSION))
				|| (asrc.getType().name.equals(LANG1.BOOLEAN_LITERAL) && adst.getType().name.equals(LANG2.BOOLEAN_LITERAL));
	}

	private void ignoreMapping(Tree asrc, Tree adst, Set<Tree> srcIgnored, Set<Tree> dstIgnored) {
		srcIgnored.add(asrc);
		dstIgnored.add(adst);
	}

	private void retainBestScoredMapping(List<ScoredMapping> mappingList, Set<Tree> srcIgnored, Set<Tree> dstIgnored) {
		for (ScoredMapping scoredMapping : mappingList) {
			Tree first = scoredMapping.first;
			Tree second = scoredMapping.second;
			if (isVerifiedMapping(first, second) && !(srcIgnored.contains(first) || dstIgnored.contains(second))) {
				mappings.addMappingRecursively(first, second);
				srcIgnored.add(first);
				srcIgnored.addAll(first.getDescendants());
				dstIgnored.add(second);
				dstIgnored.addAll(second.getDescendants());
			}
		}
	}

	private boolean isVerifiedMapping(Tree first, Tree second) {
		if (TreeUtilFunctions.areBothFromThisType(first, second, LANG1.SIMPLE_NAME, LANG2.SIMPLE_NAME) || TreeUtilFunctions.areBothFromThisType(first, second, LANG1.QUALIFIED_NAME, LANG2.QUALIFIED_NAME)) {
			return isAcceptableMatch(first, second);
		}
		return true;
	}

	private static class AmbiguousGroup {
		private final Set<Tree> srcs;
		private final Set<Tree> dsts;

		private AmbiguousGroup(Set<Tree> srcs, Set<Tree> dsts) {
			this.srcs = srcs;
			this.dsts = dsts;
		}
	}

	private static class ScoredMapping implements Comparable<ScoredMapping> {
		private final Tree first;
		private final Tree second;
		private final double siblingsSimilarity;
		private final double parentsSimilarity;
		private final double parentPositionDistance;
		private final int absolutePositionDistance;
		private final int srcPosition;
		private final int dstPosition;
		private final long order;

		private ScoredMapping(Tree first, Tree second, double siblingsSimilarity, double parentsSimilarity,
							  double parentPositionDistance, int absolutePositionDistance, long order) {
			this.first = first;
			this.second = second;
			this.siblingsSimilarity = siblingsSimilarity;
			this.parentsSimilarity = parentsSimilarity;
			this.parentPositionDistance = parentPositionDistance;
			this.absolutePositionDistance = absolutePositionDistance;
			this.srcPosition = first.getPos();
			this.dstPosition = second.getPos();
			this.order = order;
		}

		@Override
		public int compareTo(ScoredMapping other) {
			int result = Double.compare(other.siblingsSimilarity, siblingsSimilarity);
			if (result != 0) return result;
			result = Double.compare(other.parentsSimilarity, parentsSimilarity);
			if (result != 0) return result;
			result = Double.compare(parentPositionDistance, other.parentPositionDistance);
			if (result != 0) return result;
			result = Integer.compare(absolutePositionDistance, other.absolutePositionDistance);
			if (result != 0) return result;
			result = Integer.compare(srcPosition, other.srcPosition);
			if (result != 0) return result;
			result = Integer.compare(dstPosition, other.dstPosition);
			if (result != 0) return result;
			return Long.compare(order, other.order);
		}
	}

	private static class ScoredMappingFactory {
		private final MappingStore mappings;
		private final Map<Tree, Set<Tree>> srcDescendants = new HashMap<>();
		private final Map<Tree, Set<Tree>> dstDescendants = new HashMap<>();
		private final Map<Tree, double[]> positionVectors = new HashMap<>();
		private final Map<Tree, Map<Tree, Double>> siblingsSimilarities = new HashMap<>();
		private final Map<Tree, Map<Tree, Double>> parentsSimilarities = new HashMap<>();
		private final Map<Tree, Map<Tree, Integer>> commonParents = new HashMap<>();
		private final Map<Tree, Integer> parentDepths = new HashMap<>();

		private ScoredMappingFactory(MappingStore mappings) {
			this.mappings = mappings;
		}

		private void prepare(List<AmbiguousGroup> ambiguousGroups) {
			Map<Tree, Set<Tree>> candidateDstParentsBySrcParent = new HashMap<>();
			Set<Tree> relevantDstParents = new HashSet<>();
			for (AmbiguousGroup ambiguousGroup : ambiguousGroups) {
				for (Tree asrc : ambiguousGroup.srcs) {
					Tree srcParent = asrc.getParent();
					Set<Tree> dstParents = candidateDstParentsBySrcParent.computeIfAbsent(srcParent, ignored -> new HashSet<>());
					for (Tree adst : ambiguousGroup.dsts) {
						Tree dstParent = adst.getParent();
						dstParents.add(dstParent);
						relevantDstParents.add(dstParent);
					}
				}
			}
			Map<Tree, Map<Tree, Integer>> commonDescendants = new HashMap<>();
			for (Mapping mapping : mappings) {
				List<Tree> srcParents = relevantParents(mapping.first, candidateDstParentsBySrcParent.keySet());
				if (srcParents.isEmpty()) {
					continue;
				}
				List<Tree> dstParents = relevantParents(mapping.second, relevantDstParents);
				if (dstParents.isEmpty()) {
					continue;
				}
				for (Tree srcParent : srcParents) {
					Set<Tree> candidateDstParents = candidateDstParentsBySrcParent.get(srcParent);
					Map<Tree, Integer> countsByDstParent = commonDescendants.computeIfAbsent(srcParent, ignored -> new HashMap<>());
					for (Tree dstParent : dstParents) {
						if (candidateDstParents.contains(dstParent)) {
							countsByDstParent.merge(dstParent, 1, Integer::sum);
						}
					}
				}
			}
			for (Map.Entry<Tree, Set<Tree>> entry : candidateDstParentsBySrcParent.entrySet()) {
				Tree srcParent = entry.getKey();
				Map<Tree, Integer> countsByDstParent = commonDescendants.getOrDefault(srcParent, Collections.emptyMap());
				Map<Tree, Double> similaritiesByDstParent = siblingsSimilarities.computeIfAbsent(srcParent, ignored -> new HashMap<>());
				int srcDescendants = descendantSize(srcParent);
				for (Tree dstParent : entry.getValue()) {
					int common = countsByDstParent.getOrDefault(dstParent, 0);
					int dstDescendants = descendantSize(dstParent);
					similaritiesByDstParent.put(dstParent, SimilarityMetrics.diceCoefficient(common, srcDescendants, dstDescendants));
				}
			}
		}

		private List<Tree> relevantParents(Tree tree, Set<Tree> relevantParents) {
			List<Tree> parents = new ArrayList<>();
			Tree parent = tree.getParent();
			while (parent != null) {
				if (relevantParents.contains(parent)) {
					parents.add(parent);
				}
				parent = parent.getParent();
			}
			return parents;
		}

		private int descendantSize(Tree tree) {
			if (tree == null) {
				return 0;
			}
			return tree.getMetrics().size - 1;
		}

		private ScoredMapping score(Tree src, Tree dst, long order) {
			return new ScoredMapping(src, dst,
					siblingsSimilarity(src.getParent(), dst.getParent()),
					parentsSimilarity(src, dst),
					parentPositionDistance(src, dst),
					Math.abs(src.getMetrics().position - dst.getMetrics().position),
					order);
		}

		private double siblingsSimilarity(Tree srcParent, Tree dstParent) {
			Map<Tree, Double> similaritiesByDstParent = siblingsSimilarities.computeIfAbsent(srcParent, ignored -> new HashMap<>());
			return similaritiesByDstParent.computeIfAbsent(dstParent, ignored -> {
				Set<Tree> srcDescendants = srcDescendants(srcParent);
				Set<Tree> dstDescendants = dstDescendants(dstParent);
				int commonDescendants = 0;
				for (Tree srcDescendant : srcDescendants) {
					Tree dstDescendant = mappings.getDstForSrc(srcDescendant);
					if (dstDescendant != null && dstDescendants.contains(dstDescendant)) {
						commonDescendants++;
					}
				}
				return SimilarityMetrics.diceCoefficient(commonDescendants, srcDescendants.size(), dstDescendants.size());
			});
		}

		private Set<Tree> srcDescendants(Tree tree) {
			if (tree == null) {
				return Collections.emptySet();
			}
			return srcDescendants.computeIfAbsent(tree, ignored -> new HashSet<>(tree.getDescendants()));
		}

		private Set<Tree> dstDescendants(Tree tree) {
			if (tree == null) {
				return Collections.emptySet();
			}
			return dstDescendants.computeIfAbsent(tree, ignored -> new HashSet<>(tree.getDescendants()));
		}

		private double parentsSimilarity(Tree src, Tree dst) {
			Tree srcParent = src.getParent();
			Tree dstParent = dst.getParent();
			Map<Tree, Double> similaritiesByDstParent = parentsSimilarities.computeIfAbsent(srcParent, ignored -> new HashMap<>());
			return similaritiesByDstParent.computeIfAbsent(dstParent, ignored -> {
				int commonParents = commonParents(srcParent, dstParent);
				return SimilarityMetrics.diceCoefficient(commonParents, parentDepth(srcParent), parentDepth(dstParent));
			});
		}

		private int commonParents(Tree srcParent, Tree dstParent) {
			if (srcParent == null || dstParent == null) {
				return 0;
			}
			Integer common = cachedCommonParents(srcParent, dstParent);
			if (common == null) {
				List<Tree> srcParents = parentChain(srcParent);
				List<Tree> dstParents = parentChain(dstParent);
				for (int i = srcParents.size() - 1; i >= 0; i--) {
					Tree currentSrcParent = srcParents.get(i);
					for (int j = dstParents.size() - 1; j >= 0; j--) {
						Tree currentDstParent = dstParents.get(j);
						if (cachedCommonParents(currentSrcParent, currentDstParent) != null) {
							continue;
						}
						int currentCommon;
						if (currentSrcParent.hasSameTypeAndLabel(currentDstParent)) {
							currentCommon = 1 + commonParentsValue(currentSrcParent.getParent(), currentDstParent.getParent());
						}
						else {
							currentCommon = Math.max(
									commonParentsValue(currentSrcParent.getParent(), currentDstParent),
									commonParentsValue(currentSrcParent, currentDstParent.getParent()));
						}
						cacheCommonParents(currentSrcParent, currentDstParent, currentCommon);
					}
				}
				common = cachedCommonParents(srcParent, dstParent);
			}
			return common;
		}

		private List<Tree> parentChain(Tree tree) {
			List<Tree> parents = new ArrayList<>();
			Tree current = tree;
			while (current != null) {
				parents.add(current);
				current = current.getParent();
			}
			return parents;
		}

		private Integer cachedCommonParents(Tree srcParent, Tree dstParent) {
			Map<Tree, Integer> byDstParent = commonParents.get(srcParent);
			return byDstParent != null ? byDstParent.get(dstParent) : null;
		}

		private int commonParentsValue(Tree srcParent, Tree dstParent) {
			if (srcParent == null || dstParent == null) {
				return 0;
			}
			Integer common = cachedCommonParents(srcParent, dstParent);
			if (common == null) {
				throw new IllegalStateException("Parent LCS state was not initialized");
			}
			return common;
		}

		private void cacheCommonParents(Tree srcParent, Tree dstParent, int common) {
			commonParents.computeIfAbsent(srcParent, ignored -> new HashMap<>()).put(dstParent, common);
		}

		private int parentDepth(Tree tree) {
			if (tree == null) {
				return 0;
			}
			Integer depth = parentDepths.get(tree);
			if (depth == null) {
				List<Tree> parentChain = parentChain(tree);
				depth = 0;
				for (int i = parentChain.size() - 1; i >= 0; i--) {
					depth++;
					parentDepths.put(parentChain.get(i), depth);
				}
			}
			return depth;
		}

		private double parentPositionDistance(Tree src, Tree dst) {
			double[] srcVector = positionVector(src);
			double[] dstVector = positionVector(dst);
			double distance = 0;
			for (int i = 0; i < Math.min(srcVector.length, dstVector.length); i++) {
				double difference = srcVector[i] - dstVector[i];
				distance += difference * difference;
			}
			return Math.sqrt(distance);
		}

		private double[] positionVector(Tree tree) {
			return positionVectors.computeIfAbsent(tree, ignored -> {
				List<Double> positions = new ArrayList<>();
				Tree current = tree;
				while (current != null && current.getParent() != null) {
					Tree parent = current.getParent();
					positions.add(parent.getChildPosition(current) / (double) parent.getChildren().size());
					current = parent;
				}
				double[] vector = new double[positions.size()];
				for (int i = 0; i < positions.size(); i++) {
					vector[i] = positions.get(i);
				}
				return vector;
			});
		}
	}

	public static class ExtendedFullMappingComparator implements Comparator<Mapping> {
		private final MappingComparators.FullMappingComparator fullMappingComparator;

		public ExtendedFullMappingComparator(MappingStore ms) {
			fullMappingComparator = new MappingComparators.FullMappingComparator(ms);
		}

		@Override
		public int compare(Mapping m1, Mapping m2) {
			int result = fullMappingComparator.compare(m1,m2);
			if (result != 0)
				return result;
			return new AbsoluteOffsetComparator().compare(m1,m2);
		}
	}
	public static class AbsoluteOffsetComparator implements Comparator<Mapping> {
		@Override
		public int compare(Mapping m1, Mapping m2) {
			int result =  Integer.compare(m1.first.getPos(), m2.first.getPos());
			if (result != 0) return result;
			result =  Integer.compare(m1.second.getPos(), m2.second.getPos());
			return result;
		}
	}
}
