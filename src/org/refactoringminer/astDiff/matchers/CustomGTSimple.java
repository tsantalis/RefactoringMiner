package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.heuristic.gt.*;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;

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
						String SimpleNameType = "SimpleName";
						if (mapping.first.getType().name.equals(SimpleNameType) &&
								mapping.second.getType().name.equals(SimpleNameType)) {
							if (
									!mapping.first.getParent().getType().name.equals(mapping.second.getParent().getType().name)
									&&
									(
											((mapping.first.getParent().getType().name.equals("MethodInvocation")) && !mapping.second.getParent().getType().name.equals("MethodInvocation"))
											||
											((mapping.second.getParent().getType().name.equals("MethodInvocation")) && !mapping.first.getParent().getType().name.equals("MethodInvocation"))
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
