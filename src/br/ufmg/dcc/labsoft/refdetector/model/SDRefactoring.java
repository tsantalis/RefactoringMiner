package br.ufmg.dcc.labsoft.refdetector.model;

import gr.uom.java.xmi.diff.Refactoring;
import gr.uom.java.xmi.diff.RefactoringType;

public class SDRefactoring implements Refactoring {

	private RefactoringType type;
	private SDEntity mainEntity;
	private String details;

	public SDRefactoring(RefactoringType type, SDEntity mainEntity, String details) {
		this.type = type;
		this.mainEntity = mainEntity;
		this.details = details;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return type;
	}

	@Override
	public String getName() {
		return type.getDisplayName();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName());
		sb.append(' ');
		sb.append(this.mainEntity);
		if (this.details != null && !this.details.isEmpty()) {
			sb.append(' ');
			sb.append(this.details);
		}
		return sb.toString();
	}
}
