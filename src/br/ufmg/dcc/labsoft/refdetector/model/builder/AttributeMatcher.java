package br.ufmg.dcc.labsoft.refdetector.model.builder;

import br.ufmg.dcc.labsoft.refdetector.model.SDAttribute;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;

public class AttributeMatcher extends EntityMatcher<SDAttribute> {

    @Override
    protected double similarity(SDModel m, SDAttribute before, SDAttribute after) {
//        double simAssignment = before.assignment().similarity(after.assignment());
        double simClient = before.clientCode().similarity(after.clientCode());
//        if (before.fullName().endsWith("edgeStore") && after.fullName().endsWith("edgeStore")) {
//            System.out.println();
//        }
        return simClient;
    }
}
