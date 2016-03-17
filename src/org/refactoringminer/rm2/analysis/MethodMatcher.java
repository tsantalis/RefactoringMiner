package org.refactoringminer.rm2.analysis;

import org.refactoringminer.rm2.model.SDMethod;

public class MethodMatcher extends EntityMatcher<SDMethod> {

    public MethodMatcher() {
        using(SimilarityIndex.SOURCE_CODE);
    }
 
}
