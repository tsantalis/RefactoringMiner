package br.ufmg.dcc.labsoft.refdetector.model.builder;

import gr.uom.java.xmi.diff.RefactoringType;
import br.ufmg.dcc.labsoft.refdetector.model.Filter;
import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;
import br.ufmg.dcc.labsoft.refdetector.model.SDRefactoring;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;

public class RefactoringsSDBuilder {
	
	private double moveTypeThreshold = 0.5;
	private double renameTypeThreshold = 0.5;
	private double moveAndRenameTypeThreshold = 0.5;
	private double renameMethodThreshold = 0.5;
	private double moveMethodThreshold = 0.5;
	private double extractMethodThreshold = 0.5;

	public void analyze(SDModel model) {
		identifyMoveType(model);
		identifyRenameType(model);
		identifyMoveAndRenameType(model);
		identifyRenameMethod(model);
		identifyMoveMethod(model);
		identifyExtractMethod(model);
	}

	private void identifyMoveType(SDModel m) {
	    new EntityMatcher<SDType>(m){
	        protected boolean canMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
	            return entityBefore.simpleName().equals(entityAfter.simpleName());
	        }
	        protected void onMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
	            m.addRefactoring(new SDRefactoring(RefactoringType.MOVE_CLASS, entityBefore, "to " + entityAfter));
	        }
	    }.match(m.before().getUnmatchedTypes(), m.after().getUnmatchedTypes(), moveTypeThreshold);
    }

	private void identifyRenameType(SDModel m) {
	    new EntityMatcher<SDType>(m){
	        protected boolean canMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
	            return entityBefore.container().matches(entityAfter.container());
	        }
	        protected void onMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
	            m.addRefactoring(new SDRefactoring(RefactoringType.RENAME_CLASS, entityBefore, "to " + entityAfter));
	        }
	    }.match(m.before().getUnmatchedTypes(), m.after().getUnmatchedTypes(), renameTypeThreshold);
	}

	private void identifyMoveAndRenameType(SDModel m) {
        new EntityMatcher<SDType>(m){
            protected boolean canMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                return !entityBefore.simpleName().equals(entityAfter.simpleName()) &&
                    !entityBefore.container().matches(entityAfter.container());
            }
            protected void onMatch(SDModel m, SDType entityBefore, SDType entityAfter) {
                m.addRefactoring(new SDRefactoring(RefactoringType.MOVE_CLASS, entityBefore, "to " + entityAfter));
            }
        }.match(m.before().getUnmatchedTypes(), m.after().getUnmatchedTypes(), moveAndRenameTypeThreshold);
    }
	
	private void identifyRenameMethod(SDModel m) {
        new EntityMatcher<SDMethod>(m){
            protected boolean canMatch(SDModel m, SDMethod entityBefore, SDMethod entityAfter) {
                return entityBefore.container().matches(entityAfter.container());
            }
            protected void onMatch(SDModel m, SDMethod entityBefore, SDMethod entityAfter) {
                m.addRefactoring(new SDRefactoring(RefactoringType.RENAME_METHOD, entityBefore, "to " + entityAfter));
            }
        }.match(m.before().getUnmatchedMethods(), m.after().getUnmatchedMethods(), renameMethodThreshold);
    }
	
	private void identifyMoveMethod(SDModel m) {
        new EntityMatcher<SDMethod>(m){
            protected boolean canMatch(SDModel m, SDMethod entityBefore, SDMethod entityAfter) {
                return !entityBefore.container().matches(entityAfter.container()) && entityBefore.identifier().equals(entityAfter.identifier());
            }
            protected void onMatch(SDModel m, SDMethod entityBefore, SDMethod entityAfter) {
                m.addRefactoring(new SDRefactoring(RefactoringType.MOVE_OPERATION, entityBefore, "to " + entityAfter));
            }
        }.match(m.before().getUnmatchedMethods(), m.after().getUnmatchedMethods(), moveMethodThreshold);
    }
	
	private void identifyExtractMethod(SDModel m) {
		for (SDMethod method : m.after().getUnmatchedMethods()) {
			for (SDMethod caller : method.callers().suchThat(Filter.isNotEqual(method).and(m.<SDMethod>isMatched()))) {
				SourceRepresentation callerBodyBefore = m.before(caller).sourceCode();
				SourceRepresentation methodBody = method.sourceCode();
				if (methodBody.partialSimilarity(callerBodyBefore) >= extractMethodThreshold) {
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
					method.addOrigin(m.before(caller), copies);
				}
			}
			if (method.origins().size() > 0) {
				m.addRefactoring(new SDRefactoring(RefactoringType.EXTRACT_OPERATION, method, "from " + method.origins()));
			}
		}
	}

}
