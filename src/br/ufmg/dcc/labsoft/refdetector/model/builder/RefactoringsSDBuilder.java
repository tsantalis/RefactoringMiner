package br.ufmg.dcc.labsoft.refdetector.model.builder;

import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.Refactoring;

import java.util.List;

import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;

public class RefactoringsSDBuilder {
	
	public void analyze(SDModel model, List<Refactoring> refactorings) {
		for (Refactoring ref : refactorings) {
			if (ref instanceof ExtractOperationRefactoring) {
				this.onExtractMethod(model, (ExtractOperationRefactoring) ref);
			}
		}
	}

	private void onExtractMethod(SDModel model, ExtractOperationRefactoring ref) {
		String keyExtracted = ref.getExtractedOperation().getKey();
		SDMethod extracted = model.after().find(SDMethod.class, keyExtracted);
		if (extracted == null) {
			throw new RuntimeException("Method not found: " + keyExtracted);
		}
		
		String keyFrom = ref.getExtractedFromOperation().getKey();
		SDMethod from = model.before().find(SDMethod.class, keyFrom);
		
		model.reportExtractedMethod(extracted, from);
	}

}
