package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.heuristic.gt.*;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 * @since   2022-12-26 8:13 p.m.
 */
public class CustomGTSimple extends CompositeMatchers.CompositeMatcher {
	public CustomGTSimple() {
		super(new CustomGreedy(0), new SimpleBottomUpMatcher());
	}
}

class CustomGreedy extends GreedySubtreeMatcher {
	public CustomGreedy(int minP) {
		super();
		setMinPriority(minP);
	}

	@Override
	protected void retainBestMapping(List<Mapping> mappingList, Set<Tree> srcIgnored, Set<Tree> dstIgnored) {
		List<Mapping> verifiedList = new ArrayList<>();
		for (Mapping mapping : mappingList) {
			if (mapping.first.getType().name.equals(Constants.SIMPLE_NAME)
			&& mapping.second.getType().name.equals(Constants.SIMPLE_NAME)) {
				if (isAcceptableMatch(mapping))
					verifiedList.add(mapping);
			}
			else verifiedList.add(mapping);
		}
		super.retainBestMapping(verifiedList, srcIgnored, dstIgnored);
	}

	private static boolean isAcceptableMatch(Mapping mapping) {
		Tree first = mapping.first;
		Tree second = mapping.second;
		if (isPartOfConditional(first) && isPartOfConditional(second))
			if (!isSamePositionInConditional(first,second))
				return false;

		return (!mapping.first.getParent().getType().name.equals(Constants.METHOD_INVOCATION)
				||
				mapping.second.getParent().getType().name.equals(Constants.METHOD_INVOCATION)) &&
				(!mapping.second.getParent().getType().name.equals(Constants.METHOD_INVOCATION)
						||
						mapping.first.getParent().getType().name.equals(Constants.METHOD_INVOCATION));
	}

	private static boolean isOnlyOneMethodInvocation(Tree input1, Tree input2)
	{
		if (input1 == null || input2 == null) return false;
		if (input1.getParent() == null || input2.getParent() == null) return false;
		if (input1.getParent().getType().name.equals(Constants.METHOD_INVOCATION) && !input2.getParent().getType().name.equals(Constants.METHOD_INVOCATION))
			return true;
		else if (!input1.getParent().getType().name.equals(Constants.METHOD_INVOCATION) && input2.getParent().getType().name.equals(Constants.METHOD_INVOCATION))
			return true;
		return false;

	}
	private static boolean isPartOfConditional(Tree input) {
		if (input.getType().name.equals(Constants.CONDITIONAL_EXPRESSION))
			return true;
		else {
			Tree parent = input.getParent();
			if (parent == null) return false;
			if (TreeUtilFunctions.isStatement(parent.getType().name)) return false;
			return isPartOfConditional(parent);
		}
	}
	private static boolean isSamePositionInConditional(Tree input1, Tree input2)
	{
		int input1Index = 0;
		int input2Index = 0;
		while (!input1.getParent().getType().name.equals(Constants.CONDITIONAL_EXPRESSION))
			input1 = input1.getParent();
		while (!input2.getParent().getType().name.equals(Constants.CONDITIONAL_EXPRESSION))
			input2 = input2.getParent();
		input1Index = input1.positionInParent();
		input2Index = input2.positionInParent();
		return input1Index == input2Index;
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
					if (currentSrc.getMetrics().hash == currentDst.getMetrics().hash)
						if (currentSrc.isIsomorphicTo(currentDst)) {
							if (!isOnlyOneMethodInvocation(currentSrc,currentDst))
								multiMappings.addMapping(currentSrc, currentDst);
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

		Collections.sort(ambiguousList, new MappingComparators.FullMappingComparator(mappings));
		if (ambiguousList.size() > 1)
			ambiguousStringLiteralModification(ambiguousList,srcIgnored,dstIgnored);
		// Select the best ambiguous mappings
		retainBestMapping(ambiguousList, srcIgnored, dstIgnored);
	}

	private void ambiguousStringLiteralModification(List<Mapping> ambiguousList, Set<Tree> srcIgnored, Set<Tree> dstIgnored) {
		List<Mapping> ret = new ArrayList<>();
		for (Mapping mapping : ambiguousList) {
			Tree asrc = mapping.first;
			Tree adst = mapping.second;
			if (asrc.getType().name.equals(Constants.STRING_LITERAL) && adst.getType().name.equals(Constants.STRING_LITERAL)) {
				if (asrc.getParent() != null && adst.getParent() != null) {
					int i = asrc.positionInParent();
					int j = adst.positionInParent();
					if (i > 0 && j > 0) {
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
			}
			else {
				break;
			}
		}
		if (ret.size() > 0)
			retainBestMapping(ret,new HashSet<>(),new HashSet<>());
	}
}
