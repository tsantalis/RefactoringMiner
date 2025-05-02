package gr.uom.java.xmi.diff;

import java.util.Optional;

import gr.uom.java.xmi.UMLModule;

public class UMLModuleDiff {
	private UMLModule originalModule;
	private UMLModule nextModule;
	private Optional<UMLJavadocDiff> javadocDiff;
	private UMLAnnotationListDiff annotationListDiff;
	private boolean nameChanged;
	private UMLModuleDirectiveListDiff directiveListDiff;
	private UMLCommentListDiff commentListDiff;

	public UMLModuleDiff(UMLModule originalModule, UMLModule nextModule) {
		this.originalModule = originalModule;
		this.nextModule = nextModule;
		if(!originalModule.getName().equals(nextModule.getName())) {
			nameChanged = true;
		}
		if(originalModule.getJavadoc() != null && nextModule.getJavadoc() != null) {
			UMLJavadocDiff diff = new UMLJavadocDiff(originalModule.getJavadoc(), nextModule.getJavadoc());
			this.javadocDiff = Optional.of(diff);
		}
		else {
			this.javadocDiff = Optional.empty();
		}
		this.annotationListDiff = new UMLAnnotationListDiff(originalModule.getAnnotations(), nextModule.getAnnotations());
		this.directiveListDiff = new UMLModuleDirectiveListDiff(originalModule.getDirectives(), nextModule.getDirectives());
		this.commentListDiff = new UMLCommentListDiff(originalModule.getComments(), nextModule.getComments());
	}

	public UMLModule getOriginalModule() {
		return originalModule;
	}

	public UMLModule getNextModule() {
		return nextModule;
	}

	public Optional<UMLJavadocDiff> getJavadocDiff() {
		return javadocDiff;
	}

	public UMLAnnotationListDiff getAnnotationListDiff() {
		return annotationListDiff;
	}

	public boolean isNameChanged() {
		return nameChanged;
	}

	public UMLModuleDirectiveListDiff getDirectiveListDiff() {
		return directiveListDiff;
	}

	public UMLCommentListDiff getCommentListDiff() {
		return commentListDiff;
	}
}
