package org.refactoringminer.model.builder;

import org.refactoringminer.model.SDModel;
import org.refactoringminer.model.SDType;

public class TypeMatcher extends EntityMatcher<SDType> {

    public TypeMatcher() {
        using(SimilarityIndex.SOURCE_CODE);
        //using(SimilarityIndex.MEMBERS);
    }
    
    @Override
    public int getPriority(SDModel m, SDType entityBefore, SDType entityAfter) {
        return Math.max(entityBefore.nestingLevel(), entityAfter.nestingLevel());
    }
 
}
