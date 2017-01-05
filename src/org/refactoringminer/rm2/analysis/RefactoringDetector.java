package org.refactoringminer.rm2.analysis;

import org.refactoringminer.rm2.analysis.EntityMatcher.Criterion;
import org.refactoringminer.rm2.model.Filter;
import org.refactoringminer.rm2.model.MembersRepresentation;
import org.refactoringminer.rm2.model.RelationshipType;
import org.refactoringminer.rm2.model.SDAttribute;
import org.refactoringminer.rm2.model.SDMethod;
import org.refactoringminer.rm2.model.SDModel;
import org.refactoringminer.rm2.model.SDType;
import org.refactoringminer.rm2.model.SourceRepresentation;
import org.refactoringminer.rm2.model.refactoring.SDExtractMethod;
import org.refactoringminer.rm2.model.refactoring.SDExtractSupertype;
import org.refactoringminer.rm2.model.refactoring.SDInlineMethod;
import org.refactoringminer.rm2.model.refactoring.SDMoveAndRenameClass;
import org.refactoringminer.rm2.model.refactoring.SDMoveAttribute;
import org.refactoringminer.rm2.model.refactoring.SDMoveClass;
import org.refactoringminer.rm2.model.refactoring.SDMoveMethod;
import org.refactoringminer.rm2.model.refactoring.SDPullUpAttribute;
import org.refactoringminer.rm2.model.refactoring.SDPullUpMethod;
import org.refactoringminer.rm2.model.refactoring.SDPushDownAttribute;
import org.refactoringminer.rm2.model.refactoring.SDPushDownMethod;
import org.refactoringminer.rm2.model.refactoring.SDRenameClass;
import org.refactoringminer.rm2.model.refactoring.SDRenameMethod;

public class RefactoringDetector {

    private final RefactoringDetectorConfig config;

    public RefactoringDetector(RefactoringDetectorConfig config) {
        this.config = config;
    }

    public void analyze(SDModel model) {
        identifyMatchingTypes(model);
        identifyExtractTypes(model);
        identifyMatchingMethods(model);
        identifyExtractMethod(model);
        identifyInlineMethod(model);
        identifyMatchingAttributes(model);
    }

    private void identifyMatchingTypes(SDModel m) {
        new TypeMatcher()
            .addCriterion(new Criterion<SDType>(RelationshipType.MOVE_TYPE, config.getMoveTypeThreshold()) {
                protected boolean canMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                    return entityBefore.simpleName().equals(entityAfter.simpleName());
                }

                protected void onMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                    m.addRefactoring(new SDMoveClass(entityBefore, entityAfter));
                }
            })
            .addCriterion(new Criterion<SDType>(RelationshipType.RENAME_TYPE, config.getRenameTypeThreshold()) {
                protected boolean canMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                    return m.entitiesMatch(entityBefore.container(), entityAfter.container());
                }

                protected void onMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                    m.addRefactoring(new SDRenameClass(entityBefore, entityAfter));
                }
            })
            .addCriterion(new Criterion<SDType>(RelationshipType.MOVE_AND_RENAME_TYPE, config.getMoveAndRenameTypeThreshold()) {
                protected boolean canMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                    return !entityBefore.simpleName().equals(entityAfter.simpleName()) &&
                        !m.entitiesMatch(entityBefore.container(), entityAfter.container());
                }

                protected void onMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                    m.addRefactoring(new SDMoveAndRenameClass(entityBefore, entityAfter));
                }
            })
            .match(m, m.before().getUnmatchedTypes(), m.after().getUnmatchedTypes());
    }

    private void identifyExtractTypes(SDModel m) {
        for (SDType typeAfter : m.after().getUnmatchedTypes()) {
            MembersRepresentation supertypeMembers = typeAfter.membersRepresentation();
            for (SDType subtype : typeAfter.subtypes().suchThat(m.<SDType> isMatched())) {
                MembersRepresentation subtypeMembersBefore = m.before(subtype).membersRepresentation();
                double sim = supertypeMembers.partialSimilarity(subtypeMembersBefore);
                if (sim >= config.getExtractSupertypeThreshold()) {
                    // found an extracted supertype
                    typeAfter.addOrigin(m.before(subtype), 1);
                    m.addRelationship(RelationshipType.EXTRACT_SUPERTYPE, m.before(subtype), typeAfter, 1);
                    m.addRefactoring(new SDExtractSupertype(typeAfter, subtype));
                }
            }
            if (typeAfter.origins().size() > 0) {
                // m.addRefactoring(new SDExtractSupertype(typeAfter));
            }
        }
    }

    private void identifyMatchingMethods(SDModel m) {
        new MethodMatcher()
            .addCriterion(new Criterion<SDMethod>(RelationshipType.CHANGE_METHOD_SIGNATURE, config.getRenameMethodThreshold()) {
                protected boolean canMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                    return methodBefore.identifier().equals(methodAfter.identifier()) &&
                        !methodBefore.isAbstract() && !methodAfter.isAbstract() &&
                        m.entitiesMatch(methodBefore.container(), methodAfter.container());
                }

                protected void onMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                    // change signature
                }
            })
            .addCriterion(new Criterion<SDMethod>(RelationshipType.RENAME_METHOD, config.getRenameMethodThreshold()) {
                protected boolean canMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                    return !methodBefore.identifier().equals(methodAfter.identifier()) &&
                        !methodBefore.isAbstract() && !methodAfter.isAbstract() &&
                        m.entitiesMatch(methodBefore.container(), methodAfter.container());
                }

                protected void onMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                    m.addRefactoring(new SDRenameMethod(methodBefore, methodAfter));
                }
            })
            .addCriterion(new Criterion<SDMethod>(RelationshipType.PULL_UP_METHOD, config.getPullUpMethodThreshold()) {
                protected boolean canMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                    return methodBefore.identifier().equals(methodAfter.identifier()) &&
                        !methodBefore.isAbstract() && !methodAfter.isAbstract() &&
                        !methodBefore.isConstructor() && !methodAfter.isConstructor() &&
                        m.existsAfter(methodBefore.container()) &&
                        m.after(methodBefore.container()).isSubtypeOf(methodAfter.container());
                }

                protected void onMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                    // pull up method
                    if (!m.hasRelationship(RelationshipType.EXTRACT_SUPERTYPE, methodBefore.container(), methodAfter.container())) {
                        m.addRefactoring(new SDPullUpMethod(methodBefore, methodAfter));
                    }
                }
            })
            .addCriterion(new Criterion<SDMethod>(RelationshipType.PUSH_DOWN_METHOD, config.getPushDownMethodThreshold()) {
                protected boolean canMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                    return methodBefore.identifier().equals(methodAfter.identifier()) &&
                        !methodBefore.isAbstract() && !methodAfter.isAbstract() &&
                        !methodBefore.isConstructor() && !methodAfter.isConstructor() &&
                        m.existsAfter(methodBefore.container()) &&
                        methodAfter.container().isSubtypeOf(m.after(methodBefore.container()));
                }

                protected void onMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                    // pull up method
                    m.addRefactoring(new SDPushDownMethod(methodBefore, methodAfter));
                }
            })
            .addCriterion(new Criterion<SDMethod>(RelationshipType.MOVE_METHOD, config.getMoveMethodThreshold()) {
                protected boolean canMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                    return methodBefore.identifier().equals(methodAfter.identifier()) &&
                        !methodBefore.isAbstract() && !methodAfter.isAbstract() &&
                        !methodBefore.isConstructor() && !methodAfter.isConstructor() &&
                        !m.entitiesMatch(methodBefore.container(), methodAfter.container());
                }

                protected void onMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                    // move method, possibly with a new signature
                    m.addRefactoring(new SDMoveMethod(methodBefore, methodAfter));
                }
            })
            .match(m, m.before().getUnmatchedMethods(), m.after().getUnmatchedMethods());
    }

    private void identifyMatchingAttributes(SDModel m) {
        new AttributeMatcher()
            .addCriterion(new Criterion<SDAttribute>(RelationshipType.PULL_UP_FIELD, config.getPullUpAttributeThreshold()) {
                protected boolean canMatch(SDModel m, SDAttribute attributeBefore, SDAttribute attributeAfter) {
                    return attributeBefore.simpleName().equals(attributeAfter.simpleName()) &&
                        attributeBefore.type().equals(attributeAfter.type()) &&
                        m.existsAfter(attributeBefore.container()) &&
                        m.after(attributeBefore.container()).isSubtypeOf(attributeAfter.container());
                }

                protected void onMatch(SDModel m, SDAttribute attributeBefore, SDAttribute attributeAfter) {
                    if (!m.hasRelationship(RelationshipType.EXTRACT_SUPERTYPE, attributeBefore.container(), attributeAfter.container())) {
                        m.addRefactoring(new SDPullUpAttribute(attributeBefore, attributeAfter));
                    }
                }
            })
            .addCriterion(new Criterion<SDAttribute>(RelationshipType.PUSH_DOWN_FIELD, config.getPushDownAttributeThreshold()) {
                protected boolean canMatch(SDModel m, SDAttribute attributeBefore, SDAttribute attributeAfter) {
                    return attributeBefore.simpleName().equals(attributeAfter.simpleName()) &&
                        attributeBefore.type().equals(attributeAfter.type()) &&
                        m.existsAfter(attributeBefore.container()) &&
                        attributeAfter.container().isSubtypeOf(m.after(attributeBefore.container()));
                }

                protected void onMatch(SDModel m, SDAttribute attributeBefore, SDAttribute attributeAfter) {
                    m.addRefactoring(new SDPushDownAttribute(attributeBefore, attributeAfter));
                }
            })
            .addCriterion(new Criterion<SDAttribute>(RelationshipType.MOVE_FIELD, config.getMoveAttributeThreshold()) {
                protected boolean canMatch(SDModel m, SDAttribute attributeBefore, SDAttribute attributeAfter) {
                    return attributeBefore.simpleName().equals(attributeAfter.simpleName()) &&
                        attributeBefore.type().equals(attributeAfter.type());
                }

                protected void onMatch(SDModel m, SDAttribute attributeBefore, SDAttribute attributeAfter) {
                    m.addRefactoring(new SDMoveAttribute(attributeBefore, attributeAfter));
                }
            })
            .match(m, m.before().getUnmatchedAttributes(), m.after().getUnmatchedAttributes());
    }

    private void identifyExtractMethod(SDModel m) {
        for (SDMethod method : m.after().getUnmatchedMethods()) {
            for (SDMethod caller : method.callers().suchThat(Filter.isNotEqual(method).and(m.<SDMethod> isMatched()))) {
                SDMethod origin = m.before(caller);
                SourceRepresentation callerBodyBefore = origin.sourceCode();
                SourceRepresentation callerBodyAfter = caller.sourceCode();
                SourceRepresentation removedCode = callerBodyBefore.minus(callerBodyAfter);
                SourceRepresentation methodBody = method.sourceCode();
                double sim = methodBody.partialSimilarity(removedCode);
                if (sim >= config.getExtractMethodThreshold()) {
                    // found an extracted method
                    // now find how many times the body of the extracted method
                    // was duplicated at the origin
                    int invocations = method.invocationsCount(caller);
                    double currentSim = methodBody.similarity(callerBodyBefore);
                    int copies = 1;
//                    for (int i = 2; i <= invocations; i++) {
//                        double newSim = methodBody.similarity(callerBodyBefore, i);
//                        if (newSim > currentSim) {
//                            copies = i;
//                            currentSim = newSim;
//                        } else {
//                            break;
//                        }
//                    }
                    method.addOrigin(origin, copies);
                    m.addRelationship(RelationshipType.EXTRACT_METHOD, origin, method, copies);
                    if (!method.isSetter() && !method.isGetter()) {
                        m.addRefactoring(new SDExtractMethod(method, origin));
                    }
                }
            }
            // if (method.origins().size() > 0) {
            // m.addRefactoring(new SDExtractMethod(method));
            // }
        }
    }

    private void identifyInlineMethod(SDModel m) {
        for (SDMethod method : m.before().getUnmatchedMethods()) {
            for (SDMethod caller : method.callers().suchThat(Filter.isNotEqual(method).and(m.<SDMethod> isMatched()))) {
                SourceRepresentation callerBodyBefore = caller.sourceCode();
                SDMethod dest = m.after(caller);
                SourceRepresentation callerBodyAfter = dest.sourceCode();
                SourceRepresentation addedCode = callerBodyAfter.minus(callerBodyBefore);
                SourceRepresentation methodBody = method.sourceCode();
                double sim = methodBody.partialSimilarity(addedCode);
                if (sim >= config.getInlineMethodThreshold()) {
                    // found an inline method
                    method.addInlinedTo(dest, 1);

                    m.addRelationship(RelationshipType.INLINE_METHOD, method, dest, 1);
                    m.addRefactoring(new SDInlineMethod(method, dest));
                }
            }
            // if (method.inlinedTo().size() > 0) {
            // m.addRefactoring(new SDInlineMethod(method));
            // }
        }
    }

}
