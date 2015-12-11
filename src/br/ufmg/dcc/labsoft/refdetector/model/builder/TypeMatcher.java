package br.ufmg.dcc.labsoft.refdetector.model.builder;

import br.ufmg.dcc.labsoft.refdetector.model.SDModel;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;

public class TypeMatcher extends EntityMatcher<SDType> {

    @Override
    public int getPriority(SDModel m, SDType entityBefore, SDType entityAfter) {
        return Math.max(entityBefore.nestingLevel(), entityAfter.nestingLevel());
    }
 
    @Override
    protected double similarity(SDModel m, SDType before, SDType after) {
        return 0.5 * (before.sourceCode().similarity(after.sourceCode()) + before.membersRepresentation().similarity(after.membersRepresentation()));
    }
}
