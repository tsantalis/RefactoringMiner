package br.ufmg.dcc.labsoft.refdetector.model.builder;

import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;

public class MethodMatcher extends EntityMatcher<SDMethod> {

    public MethodMatcher() {
        using(SimilarityIndex.SOURCE_CODE);
    }
 
}
