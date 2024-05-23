package org.refactoringminer.astDiff.matchers.wrappers;

import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.matchers.TreeMatcher;

/* Created by pourya on 2024-05-22*/
/* This category of matchers, report the result of the matching process,
 but they also affect the optimizationData, so in case you want to use them,
 you should consider checking the optimizationData after using them
 */
public abstract class OptimizationAwareMatcher implements TreeMatcher {
    protected final OptimizationData optimizationData;

    public OptimizationAwareMatcher(OptimizationData optimizationData) {
        this.optimizationData = optimizationData;
    }
}
