package org.refactoringminer.model.builder;

import org.refactoringminer.model.SDMethod;

public class MethodMatcher extends EntityMatcher<SDMethod> {

    public MethodMatcher() {
        using(SimilarityIndex.SOURCE_CODE);
    }
 
}
