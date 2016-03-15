package org.refactoringminer.model.builder;

import org.refactoringminer.model.SDAttribute;

public class AttributeMatcher extends EntityMatcher<SDAttribute> {

    public AttributeMatcher() {
        using(SimilarityIndex.CLIENT_CODE);
    }

}
