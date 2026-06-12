package org.refactoringminer.astDiff.tests;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MultiMappingStore;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.matchers.vanilla.CustomTopDownMatcher;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ReferenceCustomTopDownMatcher extends CustomTopDownMatcher {
	ReferenceCustomTopDownMatcher(int minP, Constants LANG1, Constants LANG2) {
		super(minP, LANG1, LANG2);
	}

	@Override
	public void filterMappings(MultiMappingStore multiMappings) {
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
				for (Tree asrc : asrcs) {
					for (Tree adst : adsts) {
						ambiguousList.add(new Mapping(asrc, adst));
					}
				}
				ignored.addAll(asrcs);
			}
		}

		Set<Tree> srcIgnored = new HashSet<>();
		Set<Tree> dstIgnored = new HashSet<>();
		ambiguousList.sort(new ExtendedFullMappingComparator(mappings));
		// The synthetic tree avoids literal leaves, so this isolates the original comparator ranking.
		retainBestMapping(ambiguousList, srcIgnored, dstIgnored);
	}
}
