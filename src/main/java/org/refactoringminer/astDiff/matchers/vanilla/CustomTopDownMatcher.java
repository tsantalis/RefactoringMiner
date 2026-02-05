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
		if (isPartOfConditional(first, LANG1) && isPartOfConditional(second, LANG2))
			if (!isSamePositionInConditional(first,second))
				return false;
		if (isPartOfConditional(first, LANG1) && getIndexInConditional(first, LANG1) == 0 && !isPartOfConditional(second, LANG2))
			return false;
		if (isPartOfConditional(second, LANG2) && getIndexInConditional(second, LANG2) == 0 && !isPartOfConditional(first, LANG1))
			return false;
		return (!mapping.first.getParent().getType().name.equals(LANG1.METHOD_INVOCATION)
				||
				mapping.second.getParent().getType().name.equals(LANG2.METHOD_INVOCATION)) &&
				(!mapping.second.getParent().getType().name.equals(LANG2.METHOD_INVOCATION)
						||
						mapping.first.getParent().getType().name.equals(LANG1.METHOD_INVOCATION));
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
		List<Mapping> ambiguousList = new ArrayList<>();
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
				for (Tree asrc : asrcs)
					for (Tree adst : adsts) {
						ambiguousList.add(new Mapping(asrc, adst));
					}
				ignored.addAll(asrcs);
			}
		}

		// Rank the mappings by score.
		Set<Tree> srcIgnored = new HashSet<>();
		Set<Tree> dstIgnored = new HashSet<>();
		Collections.sort(ambiguousList, new ExtendedFullMappingComparator(mappings));
		if (ambiguousList.size() > 1)
			ambiguousLeafModification(ambiguousList,srcIgnored,dstIgnored);
		// Select the best ambiguous mappings
		retainBestMapping(ambiguousList, srcIgnored, dstIgnored);
	}

	private void ambiguousLeafModification(List<Mapping> ambiguousList, Set<Tree> srcIgnored, Set<Tree> dstIgnored) {
		List<Mapping> ret = new ArrayList<>();
		for (Mapping mapping : ambiguousList) {
			Tree asrc = mapping.first;
			Tree adst = mapping.second;
			if (
					(asrc.getType().name.equals(LANG1.STRING_LITERAL) && adst.getType().name.equals(LANG2.STRING_LITERAL))
					 || (asrc.getType().name.equals(LANG1.NUMBER_LITERAL) && adst.getType().name.equals(LANG2.NUMBER_LITERAL))
					 || (asrc.getType().name.equals(LANG1.PREFIX_EXPRESSION) && adst.getType().name.equals(LANG2.PREFIX_EXPRESSION))
					 || (asrc.getType().name.equals(LANG1.BOOLEAN_LITERAL) && adst.getType().name.equals(LANG2.BOOLEAN_LITERAL))
				) {
				if (asrc.getParent() != null && adst.getParent() != null) {
					int i = asrc.positionInParent();
					int j = adst.positionInParent();
					if (i == j && asrc.getParent().getType().name.equals(adst.getParent().getType().name))
					{
						ret.add(mapping);
						srcIgnored.add(asrc);
						dstIgnored.add(adst);
					}
					else if (i > 0 && j > 0) {
						Tree srcYoungerSibling = asrc.getParent().getChild(i - 1);
						Tree dstYoungerSibling = adst.getParent().getChild(j - 1);
						if (mappings.getSrcForDst(dstYoungerSibling) != null)
							if (mappings.getSrcForDst(dstYoungerSibling).equals(srcYoungerSibling)) {
								ret.add(mapping);
								srcIgnored.add(asrc);
								dstIgnored.add(adst);
							}
					}
				}
			} else {
				break;
			}
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
