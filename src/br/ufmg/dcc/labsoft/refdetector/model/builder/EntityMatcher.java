package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.ArrayList;
import java.util.Collections;

import br.ufmg.dcc.labsoft.refdetector.model.RelationshipType;
import br.ufmg.dcc.labsoft.refdetector.model.SDEntity;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;

public class EntityMatcher<T extends SDEntity> {

    private final ArrayList<Criterion<T>> criteria = new ArrayList<Criterion<T>>();
    private final ArrayList<SimilarityIndex<? super T>> similarityIndexes = new ArrayList<SimilarityIndex<? super T>>();

    public EntityMatcher<T> addCriterion(Criterion<T> criterion) {
        this.criteria.add(criterion);
        return this;
    }

    public EntityMatcher<T> using(SimilarityIndex<? super T> similarity) {
        this.similarityIndexes.add(similarity);
        return this;
    }

    protected int getPriority(SDModel m, T entityBefore, T entityAfter) {
        return 0;
    }
    
//    protected double similarity(SDModel m, T entityBefore, T entityAfter) {
//        return entityBefore.sourceCode().similarity(entityAfter.sourceCode());
//    }
    
    public void match(SDModel m, Iterable<T> unmatchedBefore, Iterable<T> unmatchedAfter) {
        ArrayList<MatchCandidate<T>> candidates = new ArrayList<MatchCandidate<T>>(); 
        for (T eBefore : unmatchedBefore) {
            for (T eAfter : unmatchedAfter) {
                for (int i = 0; i < criteria.size(); i++) {
                    Criterion<T> matcher = criteria.get(i);
                    if (matcher.canMatch(m, eBefore, eAfter)) {
                        double maxSim = 0.0;
                        double averageSim = 0.0;
                        for (SimilarityIndex<? super T> index : similarityIndexes) {
                            double sim = index.similarity(eBefore, eAfter);
                            averageSim += sim;
                            maxSim = Math.max(sim, maxSim);
                        }
                        averageSim = averageSim / similarityIndexes.size();
                        if (maxSim >= matcher.threshold) {
                            candidates.add(new MatchCandidate<T>(eBefore, eAfter, matcher, getPriority(m, eBefore, eAfter), i, averageSim));
                        }
                        break;
                    }
                }
                
            }
        }
        Collections.sort(candidates);
        for (MatchCandidate<T> candidate : candidates) {
            T entityBefore = candidate.before;
            T entityAfter = candidate.after;
            RelationshipType relationshipType = candidate.criterion.relationshipType;
            if (m.addRelationship(relationshipType, entityBefore, entityAfter, 1)) {
                candidate.criterion.onMatch(m, entityBefore, entityAfter);
            }
//          if (beforeMatch == null && afterMatch == null) {
//            } else {
//                if (beforeMatch == null && relationshipType.isMultisource() && afterMatch.getType() == relationshipType) {
//                    m.addRelationship(relationshipType, true, typeBefore, typeAfter, 1);
//                }
//                if (afterMatch == null && relationshipType.isMultitarget() && beforeMatch.getType() == relationshipType) {
//                    m.addRelationship(relationshipType, true, typeBefore, typeAfter, 1);
//                }
//            }
//            if (!m.isMatched(typeBefore) && !m.isMatched(typeAfter)) {
//                m.matchEntities(typeBefore, typeAfter);
//                
//          }
        }
    }

    public static class Criterion<T extends SDEntity> {
        private final double threshold;
        private final RelationshipType relationshipType;
        
        public Criterion(RelationshipType relType, double threshold) {
            this.relationshipType = relType;
            this.threshold = threshold;
        }
        
        public RelationshipType getRelationshipType() {
            return relationshipType;
        }

        protected boolean canMatch(SDModel m, T entityBefore, T entityAfter) {
            return true;
        }
        
        protected void onMatch(SDModel m, T entityBefore, T entityAfter) {
            // override
        }
    }
    
    private static class MatchCandidate<T extends SDEntity> implements Comparable<MatchCandidate<T>> {
        
        private final T before;
        private final T after;
        private final Criterion<T> criterion;
        private final int mainPriority;
        private final int matcherPriority;
        private final double contentSimilarity;
        
        public MatchCandidate(T before, T after, Criterion<T> criterion, int mainPriority, int matcherPriority, double similarity) {
            this.before = before;
            this.after = after;
            this.criterion = criterion;
            this.mainPriority = mainPriority;
            this.matcherPriority = matcherPriority;
            this.contentSimilarity = similarity;
        }
        
        @Override
        public int compareTo(MatchCandidate<T> o) {
            int c = Integer.compare(mainPriority, o.mainPriority);
            if (c != 0) return c;
            
            c = Integer.compare(matcherPriority, o.matcherPriority);
            if (c != 0) return c;
            
            return -Double.compare(contentSimilarity, o.contentSimilarity);
        }
        
        @Override
        public String toString() {
            return "b: " + before + ", a:" + after + ", sim:" + contentSimilarity;
        }
    }
}
