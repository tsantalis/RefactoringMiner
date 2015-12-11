package br.ufmg.dcc.labsoft.refdetector.model.builder;

import br.ufmg.dcc.labsoft.refdetector.model.SDAttribute;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;

public class AttributeMatcher extends EntityMatcher<SDAttribute> {

    @Override
    protected double similarity(SDModel m, SDAttribute before, SDAttribute after) {
        return before.type().equals(after.type()) ? 1.0 : 0.0;
    }
}
