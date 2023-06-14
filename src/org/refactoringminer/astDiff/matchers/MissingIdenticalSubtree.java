package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.heuristic.gt.DefaultPriorityTreeQueue;
import com.github.gumtreediff.matchers.heuristic.gt.MappingComparators;
import com.github.gumtreediff.matchers.heuristic.gt.PriorityTreeQueue;
import com.github.gumtreediff.tree.Tree;

import java.util.*;
import java.util.function.Function;

/* Created by pourya on 2023-06-14 2:10 p.m. */
public class MissingIdenticalSubtree implements TreeMatcher {
    private static final int DEFAULT_MIN_PRIORITY = 1;
    protected int minPriority = DEFAULT_MIN_PRIORITY;

    private static final String DEFAULT_PRIORITY_CALCULATOR = "height";
    protected Function<Tree, Integer> priorityCalculator = PriorityTreeQueue
            .getPriorityCalculator(DEFAULT_PRIORITY_CALCULATOR);

    protected Tree src;
    protected Tree dst;
    protected ExtendedMultiMappingStore mappings;


    @Override
    public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappingStore;

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
                            if (!mappingStore.isSrcMapped(currentSrc) && !mappingStore.isDstMapped(currentDst))
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
    }
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

//            if (!(ignored.contains(src) || isMappingUnique)) {
//                var adsts = multiMappings.getDsts(src);
//                var asrcs = multiMappings.getSrcs(multiMappings.getDsts(src).iterator().next());
//                for (Tree asrc : asrcs)
//                    for (Tree adst : adsts)
//                        ambiguousList.add(new Mapping(asrc, adst));
//                ignored.addAll(asrcs);
//            }
        }
//
//        // Rank the mappings by score.
//        Set<Tree> srcIgnored = new HashSet<>();
//        Set<Tree> dstIgnored = new HashSet<>();
//        Collections.sort(ambiguousList, new MappingComparators.FullMappingComparator(mappings));
//
//        // Select the best ambiguous mappings
//        retainBestMapping(ambiguousList, srcIgnored, dstIgnored);
    }
    protected void retainBestMapping(List<Mapping> mappingList, Set<Tree> srcIgnored, Set<Tree> dstIgnored) {
        while (mappingList.size() > 0) {
            var mapping = mappingList.remove(0);
            if (!(srcIgnored.contains(mapping.first) || dstIgnored.contains(mapping.second))) {
                mappings.addMappingRecursively(mapping.first, mapping.second);
                srcIgnored.add(mapping.first);
                srcIgnored.addAll(mapping.first.getDescendants());
                dstIgnored.add(mapping.second);
                dstIgnored.addAll(mapping.second.getDescendants());
            }
        }
    }
}
