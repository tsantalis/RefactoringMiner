package br.ufmg.dcc.labsoft.refdetector.model.builder;

import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.MoveClassRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.Refactoring;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;

import java.util.List;

import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;
import br.ufmg.dcc.labsoft.refdetector.model.SDType;

public class RefactoringsSDBuilder {
	
	public void analyze(SDModel model, List<Refactoring> refactorings) {
		for (Refactoring ref : refactorings) {
			if (ref instanceof RenameClassRefactoring) {
				this.onRenameClassRefactoring(model, (RenameClassRefactoring) ref);
			}
			if (ref instanceof MoveClassRefactoring) {
				this.onMoveClassRefactoring(model, (MoveClassRefactoring) ref);
			}
			if (ref instanceof RenameOperationRefactoring) {
				this.onRenameMethod(model, (RenameOperationRefactoring) ref);
			}
			if (ref instanceof MoveOperationRefactoring) {
				this.onMoveMethod(model, (MoveOperationRefactoring) ref);
			}
			if (ref instanceof ExtractOperationRefactoring) {
				this.onExtractMethod(model, (ExtractOperationRefactoring) ref);
			}
		}
	}

	private void onRenameClassRefactoring(SDModel model, RenameClassRefactoring ref) {
		SDType renamed = model.after().find(SDType.class, ref.getRenamedClassName());
		assert renamed != null;
		
		SDType original = model.before().find(SDType.class, ref.getOriginalClassName());
		assert original != null;
		
		model.reportRenamedOrMovedClass(original, renamed);
	}

	private void onMoveClassRefactoring(SDModel model, MoveClassRefactoring ref) {
		SDType moved = model.after().find(SDType.class, ref.getMovedClassName());
		assert moved != null;
		
		SDType original = model.before().find(SDType.class, ref.getOriginalClassName());
		assert original != null;
		
		model.reportRenamedOrMovedClass(original, moved);
	}

	private void onRenameMethod(SDModel model, RenameOperationRefactoring ref) {
		SDMethod renamed = model.after().find(SDMethod.class, ref.getRenamedOperation().getKey());
		assert renamed != null;
		
		SDMethod original = model.before().find(SDMethod.class, ref.getOriginalOperation().getKey());
		assert original != null;
		
		model.reportRenamedMethod(original, renamed);
	}

	private void onMoveMethod(SDModel model, MoveOperationRefactoring ref) {
		SDMethod moved = model.after().find(SDMethod.class, ref.getMovedOperation().getKey());
		assert moved != null;
		
		SDMethod original = model.before().find(SDMethod.class, ref.getOriginalOperation().getKey());
		assert original != null;
		
		model.reportMovedMethod(original, moved);
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
