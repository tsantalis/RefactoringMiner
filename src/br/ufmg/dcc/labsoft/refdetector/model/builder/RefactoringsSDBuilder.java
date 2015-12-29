package br.ufmg.dcc.labsoft.refdetector.model.builder;

import br.ufmg.dcc.labsoft.refdetector.model.Filter;
import br.ufmg.dcc.labsoft.refdetector.model.MembersRepresentation;
import br.ufmg.dcc.labsoft.refdetector.model.RelationshipType;
import br.ufmg.dcc.labsoft.refdetector.model.SDAttribute;
import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;
import br.ufmg.dcc.labsoft.refdetector.model.SourceRepresentation;
import br.ufmg.dcc.labsoft.refdetector.model.builder.EntityMatcher.Criterion;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDExtractMethod;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDExtractSupertype;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDInlineMethod;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDMoveAndRenameClass;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDMoveAttribute;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDMoveClass;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDMoveMethod;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDPullUpAttribute;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDPullUpMethod;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDPushDownAttribute;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDPushDownMethod;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDRenameClass;
import br.ufmg.dcc.labsoft.refdetector.model.refactoring.SDRenameMethod;

public class RefactoringsSDBuilder {
	
	private double moveTypeThreshold = 0.4;
	private double renameTypeThreshold = 0.4;
	private double moveAndRenameTypeThreshold = 0.5;
	private double extractSupertypeThreshold = 0.5;
	
	private double renameMethodThreshold = 0.5;
	private double moveMethodThreshold = 0.5;
	private double pullUpMethodThreshold = 0.5;
	private double pushDownMethodThreshold = 0.5;
	private double extractMethodThreshold = 0.5;
	private double inlineMethodThreshold = 0.5;
	
	private double moveAttributeThreshold = 0.2;
	private double pullUpAttributeThreshold = 0.2;
	private double pushDownAttributeThreshold = 0.2;

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
	    .addCriterion(new Criterion<SDType>(RelationshipType.MOVE_TYPE, moveTypeThreshold) {
            protected boolean canMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                return entityBefore.simpleName().equals(entityAfter.simpleName());
            }
            protected void onMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                m.addRefactoring(new SDMoveClass(entityBefore, entityAfter));
            }
        })
        .addCriterion(new Criterion<SDType>(RelationshipType.RENAME_TYPE, renameTypeThreshold) {
            protected boolean canMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                return m.entitiesMatch(entityBefore.container(), entityAfter.container());
            }
            protected void onMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                m.addRefactoring(new SDRenameClass(entityBefore, entityAfter));
            }
        })
        .addCriterion(new Criterion<SDType>(RelationshipType.MOVE_AND_RENAME_TYPE, moveAndRenameTypeThreshold){
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
            for (SDType subtype : typeAfter.subtypes().suchThat(m.<SDType>isMatched())) {
                MembersRepresentation subtypeMembersBefore = m.before(subtype).membersRepresentation();
                double sim = supertypeMembers.partialSimilarity(subtypeMembersBefore);
                if (sim >= extractSupertypeThreshold) {
                    // found an extracted supertype
                    typeAfter.addOrigin(m.before(subtype), 1);
                    m.addRelationship(RelationshipType.EXTRACT, m.before(subtype), typeAfter, 1);
                }
            }
            if (typeAfter.origins().size() > 0) {
                m.addRefactoring(new SDExtractSupertype(typeAfter));
            }
        }
    }
	
	private void identifyMatchingMethods(SDModel m) {
        new MethodMatcher()
        .addCriterion(new Criterion<SDMethod>(RelationshipType.CHANGE_METHOD_SIGNATURE, renameMethodThreshold){
            protected boolean canMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                return methodBefore.identifier().equals(methodAfter.identifier()) && 
                    !methodBefore.isAbstract() && !methodAfter.isAbstract() &&
                    m.entitiesMatch(methodBefore.container(), methodAfter.container());
            }
            protected void onMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                // change signature
            }
        })
        .addCriterion(new Criterion<SDMethod>(RelationshipType.RENAME_MEMBER, renameMethodThreshold){
            protected boolean canMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                return !methodBefore.identifier().equals(methodAfter.identifier()) && 
                    !methodBefore.isAbstract() && !methodAfter.isAbstract() && 
                    m.entitiesMatch(methodBefore.container(), methodAfter.container());
            }
            protected void onMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                m.addRefactoring(new SDRenameMethod(methodBefore, methodAfter));
            }
        })
        .addCriterion(new Criterion<SDMethod>(RelationshipType.PULL_UP_MEMBER, pullUpMethodThreshold){
            protected boolean canMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                return methodBefore.identifier().equals(methodAfter.identifier()) &&
                    !methodBefore.isAbstract() && !methodAfter.isAbstract() &&
                    !methodBefore.isConstructor() && !methodAfter.isConstructor() &&
                    m.existsAfter(methodBefore.container()) &&
                    m.after(methodBefore.container()).isSubtypeOf(methodAfter.container());
            }
            protected void onMatch(SDModel m, SDMethod methodBefore, SDMethod methodAfter) {
                // pull up method
                m.addRefactoring(new SDPullUpMethod(methodBefore, methodAfter));
            }
        })
        .addCriterion(new Criterion<SDMethod>(RelationshipType.PUSH_DOWN_MEMBER, pushDownMethodThreshold){
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
        .addCriterion(new Criterion<SDMethod>(RelationshipType.MOVE_MEMBER, moveMethodThreshold){
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
        .addCriterion(new Criterion<SDAttribute>(RelationshipType.PULL_UP_MEMBER, pullUpAttributeThreshold){
            protected boolean canMatch(SDModel m, SDAttribute attributeBefore, SDAttribute attributeAfter) {
                return attributeBefore.simpleName().equals(attributeAfter.simpleName()) &&
                    attributeBefore.type().equals(attributeAfter.type()) && 
                    m.existsAfter(attributeBefore.container()) &&
                    m.after(attributeBefore.container()).isSubtypeOf(attributeAfter.container());
            }
            protected void onMatch(SDModel m, SDAttribute attributeBefore, SDAttribute attributeAfter) {
                m.addRefactoring(new SDPullUpAttribute(attributeBefore, attributeAfter));
            }
        })
        .addCriterion(new Criterion<SDAttribute>(RelationshipType.PUSH_DOWN_MEMBER, pushDownAttributeThreshold){
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
        .addCriterion(new Criterion<SDAttribute>(RelationshipType.MOVE_MEMBER, moveAttributeThreshold){
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
			for (SDMethod caller : method.callers().suchThat(Filter.isNotEqual(method).and(m.<SDMethod>isMatched()))) {
				SDMethod origin = m.before(caller);
                SourceRepresentation callerBodyBefore = origin.sourceCode();
				SourceRepresentation callerBodyAfter = caller.sourceCode();
				SourceRepresentation removedCode = callerBodyBefore.minus(callerBodyAfter);
				SourceRepresentation methodBody = method.sourceCode();
				double sim = methodBody.partialSimilarity(removedCode);
                if (sim >= extractMethodThreshold) {
					// found an extracted method
					// now find how many times the body of the extracted method was duplicated at the origin 
					int invocations = method.invocationsCount(caller);
					double currentSim = methodBody.similarity(callerBodyBefore);
					int copies = 1;
					for (int i = 2; i <= invocations; i++) {
						double newSim = methodBody.similarity(callerBodyBefore, i);
						if (newSim > currentSim) {
							copies = i;
							currentSim = newSim;
						} else {
							break;
						}
					}
					method.addOrigin(origin, copies);
					m.addRelationship(RelationshipType.EXTRACT, origin, method, copies);
					m.addRefactoring(new SDExtractMethod(method, origin));
				}
			}
//			if (method.origins().size() > 0) {
//				m.addRefactoring(new SDExtractMethod(method));
//			}
		}
	}

	private void identifyInlineMethod(SDModel m) {
	    for (SDMethod method : m.before().getUnmatchedMethods()) {
	        for (SDMethod caller : method.callers().suchThat(Filter.isNotEqual(method).and(m.<SDMethod>isMatched()))) {
	            SourceRepresentation callerBodyBefore = caller.sourceCode();
	            SDMethod dest = m.after(caller);
                SourceRepresentation callerBodyAfter = dest.sourceCode();
	            SourceRepresentation addedCode = callerBodyAfter.minus(callerBodyBefore);
	            SourceRepresentation methodBody = method.sourceCode();
	            double sim = methodBody.partialSimilarity(addedCode);
                if (sim >= inlineMethodThreshold) {
	                // found an inline method
                    method.addInlinedTo(dest, 1);
                    
                    m.addRelationship(RelationshipType.INLINE, method, dest, 1);
                    m.addRefactoring(new SDInlineMethod(method, dest));
	            }
	        }
//	        if (method.inlinedTo().size() > 0) {
//                m.addRefactoring(new SDInlineMethod(method));
//            }
	    }
	}

}
