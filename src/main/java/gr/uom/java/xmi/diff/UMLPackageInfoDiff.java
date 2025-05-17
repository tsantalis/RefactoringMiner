package gr.uom.java.xmi.diff;

import java.util.Optional;

import gr.uom.java.xmi.UMLPackageInfo;

public class UMLPackageInfoDiff {
	private UMLPackageInfo originalPackageInfo;
	private UMLPackageInfo nextPackageInfo;
	private Optional<UMLJavadocDiff> packageDeclarationJavadocDiff;
	private UMLImportListDiff importDiffList;
	private UMLAnnotationListDiff annotationListDiff;
	private UMLCommentListDiff commentListDiff;

	public UMLPackageInfoDiff(UMLPackageInfo originalPackageInfo, UMLPackageInfo nextPackageInfo) {
		this.originalPackageInfo = originalPackageInfo;
		this.nextPackageInfo = nextPackageInfo;
		this.commentListDiff = new UMLCommentListDiff(originalPackageInfo.getComments(), nextPackageInfo.getComments());
		this.annotationListDiff = new UMLAnnotationListDiff(originalPackageInfo.getAnnotations(), nextPackageInfo.getAnnotations());
		this.importDiffList = new UMLImportListDiff(originalPackageInfo.getImportedTypes(), nextPackageInfo.getImportedTypes());
		if(originalPackageInfo.getPackageDoc() != null && nextPackageInfo.getPackageDoc() != null) {
			UMLJavadocDiff diff = new UMLJavadocDiff(originalPackageInfo.getPackageDoc(), nextPackageInfo.getPackageDoc());
			this.packageDeclarationJavadocDiff = Optional.of(diff);
		}
		else {
			this.packageDeclarationJavadocDiff = Optional.empty();
		}
	}

	public UMLPackageInfo getOriginalPackageInfo() {
		return originalPackageInfo;
	}

	public UMLPackageInfo getNextPackageInfo() {
		return nextPackageInfo;
	}

	public Optional<UMLJavadocDiff> getPackageDeclarationJavadocDiff() {
		return packageDeclarationJavadocDiff;
	}

	public UMLImportListDiff getImportDiffList() {
		return importDiffList;
	}

	public UMLAnnotationListDiff getAnnotationListDiff() {
		return annotationListDiff;
	}

	public UMLCommentListDiff getCommentListDiff() {
		return commentListDiff;
	}
}
