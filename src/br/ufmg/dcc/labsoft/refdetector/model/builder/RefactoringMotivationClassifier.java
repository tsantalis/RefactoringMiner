package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.EnumSet;
import java.util.Set;

import br.ufmg.dcc.labsoft.refdetector.model.EntitySet;
import br.ufmg.dcc.labsoft.refdetector.model.EntitySet.Filter;
import br.ufmg.dcc.labsoft.refdetector.model.SDMethod;
import br.ufmg.dcc.labsoft.refdetector.model.SDModel;

public class RefactoringMotivationClassifier {

	SDModel sdModel;
	
	public RefactoringMotivationClassifier(SDModel sdModel) {
		this.sdModel = sdModel;
	}

	public enum Motivation {
		EM_REUSE,
		EM_INTRODUCE_ALTERNATIVE_SIGNATURE,
		EM_REMOVE_DUPLICATION,
		EM_PRESERVE_BACKWARD_COMPATIBILITY,
		EM_IMPROVE_TESTABILITY,
		EM_ENABLE_OVERRIDING,
		EM_ENABLE_RECURSION;
	}

	public Set<Motivation> classifyExtractMethod(SDMethod extractedMethod) {
		Set<Motivation> tags = EnumSet.noneOf(Motivation.class);
		EntitySet<SDMethod> from = extractedMethod.origins();
		
		if (from.size() == 1) {
			SDMethod fromMethodBefore = from.getFirst();
			SDMethod fromMethod = sdModel.after(fromMethodBefore);
			if (fromMethod.delegatesTo(extractedMethod)) {
				if (fromMethod.isDeprecated()) {
					tags.add(Motivation.EM_PRESERVE_BACKWARD_COMPATIBILITY);
				} else {
					tags.add(Motivation.EM_INTRODUCE_ALTERNATIVE_SIGNATURE);
				}
			}
		}

		if (from.size() > 1) {
			tags.add(Motivation.EM_REMOVE_DUPLICATION);
		}
		
		if (!tags.contains(Motivation.EM_PRESERVE_BACKWARD_COMPATIBILITY) && !tags.contains(Motivation.EM_INTRODUCE_ALTERNATIVE_SIGNATURE)) {
			final boolean isTest = extractedMethod.isTestCode();
			if (extractedMethod.callers().suchThat(testCodeEquals(isTest)).minus(from).size() > 0) {
				tags.add(Motivation.EM_REUSE);
			}
		}

		if (!tags.contains(Motivation.EM_PRESERVE_BACKWARD_COMPATIBILITY)) {
			if (!extractedMethod.isTestCode() && extractedMethod.callers().suchThat(testCodeEquals(true)).minus(from).size() > 0) {
				tags.add(Motivation.EM_IMPROVE_TESTABILITY);
			}
		}

		if (extractedMethod.isOverriden()) {
			tags.add(Motivation.EM_ENABLE_OVERRIDING);
		}

		if (extractedMethod.isRecursive()) {
			tags.add(Motivation.EM_ENABLE_RECURSION);
		}
		
		return tags;
	}

	private Filter<SDMethod> testCodeEquals(final boolean value) {
		return new Filter<SDMethod>() {
			@Override
			public boolean accept(SDMethod method) {
				return method.isTestCode() == value;
			}
		};
	}
}
