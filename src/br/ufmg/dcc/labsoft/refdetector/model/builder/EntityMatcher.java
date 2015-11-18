package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.ArrayList;
import java.util.Collections;

import br.ufmg.dcc.labsoft.refdetector.model.SDEntity;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;

public class EntityMatcher<T extends SDEntity> {

    private final SDModel m;
    
    public EntityMatcher(SDModel m) {
        this.m = m;
    }

    public final void match(Iterable<T> unmatchedBefore, Iterable<T> unmatchedAfter, double threshold) {
        ArrayList<MatchCandidate<T>> candidates = new ArrayList<MatchCandidate<T>>(); 
        for (T eBefore : unmatchedBefore) {
            for (T eAfter : unmatchedAfter) {
                if (canMatch(m, eBefore, eAfter)) {
                    double sim = eBefore.sourceCode().similarity(eAfter.sourceCode());
                    if (sim >= threshold) {
                        candidates.add(new MatchCandidate<T>(eBefore, eAfter, sim));
                    }
                }
            }
        }
        Collections.sort(candidates);
        for (MatchCandidate<T> candidate : candidates) {
            T typeBefore = candidate.getBefore();
            T typeAfter = candidate.getAfter();
            if (!m.isMatched(typeBefore) || !m.isMatched(typeAfter)) {
                m.matchEntities(typeBefore, typeAfter);
                onMatch(m, typeBefore, typeAfter);
            }
        }
    }

    protected void onMatch(SDModel m, T entityBefore, T entityAfter) {
        // override
    }

    protected boolean canMatch(SDModel m, T entityBefore, T entityAfter) {
        return true;
    }
    
}
