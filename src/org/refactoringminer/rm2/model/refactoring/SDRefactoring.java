package org.refactoringminer.rm2.model.refactoring;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm2.model.SDEntity;

public class SDRefactoring implements Refactoring {

	protected RefactoringType type;
	protected SDEntity mainEntity;

	public SDRefactoring(RefactoringType type, SDEntity mainEntity) {
		this.type = type;
		this.mainEntity = mainEntity;
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
//		if (this.details != null && !this.details.isEmpty()) {
//			sb.append(' ');
//			sb.append(this.details);
//		}
		return sb.toString();
	}
}
