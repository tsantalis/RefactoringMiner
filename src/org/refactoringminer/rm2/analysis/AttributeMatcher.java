package org.refactoringminer.rm2.analysis;

import org.refactoringminer.rm2.model.SDAttribute;

public class AttributeMatcher extends EntityMatcher<SDAttribute> {

    public AttributeMatcher() {
        using(SimilarityIndex.CLIENT_CODE);
    }

}
