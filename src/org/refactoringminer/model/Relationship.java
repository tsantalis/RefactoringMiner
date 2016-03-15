package org.refactoringminer.model;

public class Relationship {

    private final RelationshipType type;
    private final boolean secondary;
    private final SDEntity entityBefore;
    private final SDEntity entityAfter;
    private final int multiplicity;
    
    public Relationship(RelationshipType type, boolean secondary, SDEntity entityBefore, SDEntity entityAfter, int multiplicity) {
        super();
        this.type = type;
        this.secondary = secondary;
        this.entityBefore = entityBefore;
        this.entityAfter = entityAfter;
        this.multiplicity = multiplicity;
    }

    public RelationshipType getType() {
        return type;
    }

    public boolean isSecondary() {
        return secondary;
    }

    public SDEntity getEntityBefore() {
        return entityBefore;
    }

    public SDEntity getEntityAfter() {
        return entityAfter;
    }

    public int getMultiplicity() {
        return multiplicity;
    }
    
}
