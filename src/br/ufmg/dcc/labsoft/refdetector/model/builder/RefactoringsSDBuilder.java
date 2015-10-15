package br.ufmg.dcc.labsoft.refdetector.model.builder;

import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.MoveClassRefactoring;
import gr.uom.java.xmi.diff.Refactoring;

import java.util.List;

import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;

public class RefactoringsSDBuilder {
	
	public void analyze(SDModel model, List<Refactoring> refactorings) {
		for (Refactoring ref : refactorings) {
			if (ref instanceof MoveClassRefactoring) {
				this.onMoveClassRefactoring(model, (MoveClassRefactoring) ref);
			}
			if (ref instanceof ExtractOperationRefactoring) {
				this.onExtractMethod(model, (ExtractOperationRefactoring) ref);
			}
		}
	}

	private void onMoveClassRefactoring(SDModel model, MoveClassRefactoring ref) {
		SDType moved = model.after().find(SDType.class, ref.getMovedClassName());
		assert moved != null;
		
		SDType original = model.before().find(SDType.class, ref.getOriginalClassName());
		assert original != null;
		
		model.reportMovedClass(original, moved);
	}

	private void onExtractMethod(SDModel model, ExtractOperationRefactoring ref) {
		String keyExtracted = ref.getExtractedOperation().getKey();
		SDMethod extracted = model.after().find(SDMethod.class, keyExtracted);
		assert extracted != null;
		
		String keyFrom = ref.getExtractedFromOperation().getKey();
		SDMethod from = model.before().find(SDMethod.class, keyFrom);
		
		model.reportExtractedMethod(extracted, from);
	}

}
