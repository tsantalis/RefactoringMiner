package br.ufmg.dcc.labsoft.refdetector.model.builder;

import br.ufmg.dcc.labsoft.refdetector.model.SDEntity;

public class MatchCandidate<T extends SDEntity> implements Comparable<MatchCandidate<T>> {

    private final T before;
    private final T after;
    private final double similarity;
    
    public MatchCandidate(T before, T after, double similarity) {
        this.before = before;
        this.after = after;
        this.similarity = similarity;
    }

    public T getBefore() {
        return before;
    }

    public T getAfter() {
        return after;
    }

    public double getSimilarity() {
        return similarity;
    }

    @Override
    public int compareTo(MatchCandidate<T> o) {
        return -Double.compare(similarity, o.similarity);
    }
    
}
