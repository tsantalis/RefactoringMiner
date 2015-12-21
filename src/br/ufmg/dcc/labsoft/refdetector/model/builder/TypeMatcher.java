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
        double sim = 0.5 * (before.sourceCode().similarity(after.sourceCode()) + before.membersRepresentation().similarity(after.membersRepresentation()));
//        if (before.fullName().endsWith("DiskBackedPartitionStore") && after.fullName().endsWith("DiskBackedPartitionStore")) {
//            sim = sim + 0.0;
//        }
        return sim;
    }
}
