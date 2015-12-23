package br.ufmg.dcc.labsoft.refdetector.model.builder;

import br.ufmg.dcc.labsoft.refdetector.model.SDAttribute;

public class AttributeMatcher extends EntityMatcher<SDAttribute> {

    public AttributeMatcher() {
        using(SimilarityIndex.CLIENT_CODE);
    }

}
