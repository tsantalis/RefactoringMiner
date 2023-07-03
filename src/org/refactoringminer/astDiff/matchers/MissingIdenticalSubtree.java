package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.matchers.heuristic.gt.DefaultPriorityTreeQueue;
import com.github.gumtreediff.matchers.heuristic.gt.PriorityTreeQueue;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;

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
                        if (TreeUtilFunctions.isIsomorphicTo(currentSrc, currentDst)) {
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
        List<Mapping> ambiguousList = new ArrayList<>();
        Set<Tree> ignored = new HashSet<>();
        for (var src : multiMappings.allMappedSrcs()) {
            if (multiMappings.isSrcUnique(src)) {
                var dst = multiMappings.getDsts(src).stream().findAny().get();
                if (multiMappings.isDstUnique(dst)) {
                    if (TreeUtilFunctions.isStatement(src.getType().name))
                        mappings.addMappingRecursively(src, dst);
                    else if (TreeUtilFunctions.isPartOfJavadoc(src))
                        mappings.addMappingRecursively(src, dst);
                    else if (src.getType().name.equals(Constants.METHOD_INVOCATION))
                    {
                        mappings.addMappingRecursively(src, dst);
                    }

                }
            }
        }
    }
}
