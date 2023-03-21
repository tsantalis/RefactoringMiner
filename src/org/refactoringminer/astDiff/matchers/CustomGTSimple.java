package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.heuristic.gt.*;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 * @since   2022-12-26 8:13 p.m.
 */
public class CustomGTSimple extends CompositeMatchers.CompositeMatcher {
	public CustomGTSimple() {
		super(new CustomGreedy(0,false), new SimpleBottomUpMatcher());
	}
}

class CustomGreedy extends GreedySubtreeMatcher {
	private final boolean original;

	public CustomGreedy(int minP, boolean original) {
		super();
		setMinPriority(minP);
		this.original = original;
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

	public void handleAmbiguousMappings(List<Pair<Set<Tree>, Set<Tree>>> ambiguousMappings) {
		MappingComparators.FullMappingComparator comparator = new MappingComparators.FullMappingComparator(mappings);
		ambiguousMappings.sort(new AmbiguousMappingsComparator());
		ambiguousMappings.forEach((pair) -> {
			List<Mapping> candidates = convertToMappings(pair);
			candidates.sort(comparator);
			candidates.forEach(mapping -> {
				if (mappings.areBothUnmapped(mapping.first, mapping.second)) {
					if (original)
						mappings.addMappingRecursively(mapping.first, mapping.second);
					else {
						String SimpleNameType = Constants.SIMPLE_NAME;
						if (mapping.first.getType().name.equals(SimpleNameType) &&
								mapping.second.getType().name.equals(SimpleNameType)) {
							if (
									!mapping.first.getParent().getType().name.equals(mapping.second.getParent().getType().name)
									&&
									(
											((mapping.first.getParent().getType().name.equals(Constants.METHOD_INVOCATION)) && !mapping.second.getParent().getType().name.equals(Constants.METHOD_INVOCATION))
											||
											((mapping.second.getParent().getType().name.equals(Constants.METHOD_INVOCATION)) && !mapping.first.getParent().getType().name.equals(Constants.METHOD_INVOCATION))
											)
									) {
								//pass
							} else {
								mappings.addMappingRecursively(mapping.first, mapping.second);
							}

						}
					}
				}
			});
		});
	}

	public static class AmbiguousMappingsComparator implements Comparator<Pair<Set<Tree>, Set<Tree>>> {
		@Override
		public int compare(Pair<Set<Tree>, Set<Tree>> m1, Pair<Set<Tree>, Set<Tree>> m2) {
			int s1 = m1.first.stream().max(Comparator.comparingInt(t -> t.getMetrics().size)).get().getMetrics().size;
			int s2 = m1.first.stream().max(Comparator.comparingInt(t -> t.getMetrics().size)).get().getMetrics().size;
			return Integer.compare(s2, s1);
		}
	}

	public static final List<Mapping> convertToMappings(Pair<Set<Tree>, Set<Tree>> ambiguousMapping) {
		List<Mapping> mappings = new ArrayList<>();
		for (Tree src : ambiguousMapping.first)
			for (Tree dst : ambiguousMapping.second)
				mappings.add(new Mapping(src, dst));
		return mappings;
	}
}
