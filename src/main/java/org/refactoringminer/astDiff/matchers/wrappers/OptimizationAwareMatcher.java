package org.refactoringminer.astDiff.matchers.wrappers;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.matchers.TreeMatcher;

/* Created by pourya on 2024-05-22*/
/* This category of matchers, report the result of the matching process,
 but they also affect the optimizationData, so in case you want to use them,
 you should consider checking the optimizationData after using them
 */
public abstract class OptimizationAwareMatcher implements TreeMatcher {
    protected OptimizationData optimizationData;

    /**
     * Only use this constructor if the optimization data is not accessible during the object creation.
     * Don't invoke the match function before setting the optimization data via {@link #setOptimizationData(OptimizationData)} method.
     * This constructor is useful when you want to use the matcher in a pipeline of matchers.
     */
    public OptimizationAwareMatcher() {

    }

    public OptimizationAwareMatcher(OptimizationData optimizationData) {
        this.optimizationData = optimizationData;
    }

    public void setOptimizationData(OptimizationData optimizationData) {
        this.optimizationData = optimizationData;
    }

    @Override
    public final void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        if (optimizationData == null) {
            throw new IllegalStateException("Optimization data is not set. Please set the optimization data before invoking the match function.");
        }
        matchAndUpdateOptimizationStore(src, dst, mappingStore);
    }
    abstract void matchAndUpdateOptimizationStore(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore);

}
