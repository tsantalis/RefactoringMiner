package br.ufmg.dcc.labsoft.refdetector.model.builder;

import br.ufmg.dcc.labsoft.refdetector.model.SDAttribute;
import br.ufmg.dcc.labsoft.refdetector.model.SDEntity;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;

public abstract class SimilarityIndex<T extends SDEntity> {

    public abstract double similarity(T e1, T e2);

    public static final SimilarityIndex<SDEntity> SOURCE_CODE = new SimilarityIndex<SDEntity>() {
        public double similarity(SDEntity e1, SDEntity e2) {
            return e1.sourceCode().similarity(e2.sourceCode());
        }
    };

    public static final SimilarityIndex<SDType> MEMBERS = new SimilarityIndex<SDType>() {
        public double similarity(SDType e1, SDType e2) {
            return e1.membersRepresentation().similarity(e2.membersRepresentation());
        }
    };

    public static final SimilarityIndex<SDAttribute> CLIENT_CODE = new SimilarityIndex<SDAttribute>() {
        public double similarity(SDAttribute e1, SDAttribute e2) {
            return e1.clientCode().similarity(e2.clientCode());
        }
    };

}
