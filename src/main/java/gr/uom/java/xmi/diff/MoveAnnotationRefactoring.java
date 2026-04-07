package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.VariableDeclarationContainer;

public class MoveAnnotationRefactoring implements Refactoring, ModifyAnnotationRefactoring {
	public static final class DeclarationDescriptor {
		private final String description;
		private final String displayName;
		private final String codeElement;
		private final LocationInfo locationInfo;
		private final String filePath;
		private final String className;

		private DeclarationDescriptor(String description, String displayName, String codeElement,
				LocationInfo locationInfo, String filePath, String className) {
			this.description = description;
			this.displayName = displayName;
			this.codeElement = codeElement;
			this.locationInfo = locationInfo;
			this.filePath = filePath;
			this.className = className;
		}

		public static DeclarationDescriptor of(Object declaration) {
			if(declaration instanceof VariableDeclarationContainer container) {
				return new DeclarationDescriptor("method declaration", "method " + container.toQualifiedString(),
						container.toString(), container.getLocationInfo(), container.getLocationInfo().getFilePath(),
						container.getClassName());
			}
			if(declaration instanceof UMLAttribute attribute) {
				String elementType = attribute instanceof UMLEnumConstant ? "enum constant" : "attribute";
				return new DeclarationDescriptor(elementType + " declaration", elementType + " " + attribute,
						attribute.toString(), attribute.getLocationInfo(), attribute.getLocationInfo().getFilePath(),
						attribute.getClassName());
			}
			if(declaration instanceof UMLAbstractClass umlClass) {
				return new DeclarationDescriptor("class declaration", "class " + umlClass.getName(), umlClass.toString(),
						umlClass.getLocationInfo(), umlClass.getLocationInfo().getFilePath(), umlClass.getName());
			}
			throw new IllegalArgumentException("Unsupported annotation declaration type: " + declaration.getClass());
		}

		public String getDescription() {
			return description;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getCodeElement() {
			return codeElement;
		}

		public LocationInfo getLocationInfo() {
			return locationInfo;
		}

		public CodeRange codeRange() {
			return locationInfo.codeRange();
		}

		public String getFilePath() {
			return filePath;
		}

		public String getClassName() {
			return className;
		}

		@Override
		public String toString() {
			if(displayName.startsWith("class ")) {
				return className;
			}
			return codeElement;
		}

		@Override
		public int hashCode() {
			return Objects.hash(className, codeElement, description, displayName, filePath, locationInfo);
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null || getClass() != obj.getClass())
				return false;
			DeclarationDescriptor other = (DeclarationDescriptor)obj;
			return Objects.equals(className, other.className) &&
					Objects.equals(codeElement, other.codeElement) &&
					Objects.equals(description, other.description) &&
					Objects.equals(displayName, other.displayName) &&
					Objects.equals(filePath, other.filePath) &&
					Objects.equals(locationInfo, other.locationInfo);
		}
	}

	private final UMLAnnotation annotationBefore;
	private final UMLAnnotation annotationAfter;
	private final DeclarationDescriptor sourceDeclaration;
	private final DeclarationDescriptor targetDeclaration;

	public MoveAnnotationRefactoring(UMLAnnotation annotationBefore, UMLAnnotation annotationAfter,
			Object sourceDeclaration, Object targetDeclaration) {
		this.annotationBefore = annotationBefore;
		this.annotationAfter = annotationAfter;
		this.sourceDeclaration = DeclarationDescriptor.of(sourceDeclaration);
		this.targetDeclaration = DeclarationDescriptor.of(targetDeclaration);
	}

	@Override
	public UMLAnnotation getAnnotationBefore() {
		return annotationBefore;
	}

	@Override
	public UMLAnnotation getAnnotationAfter() {
		return annotationAfter;
	}

	public DeclarationDescriptor getSourceDeclaration() {
		return sourceDeclaration;
	}

	public DeclarationDescriptor getTargetDeclaration() {
		return targetDeclaration;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(annotationBefore.codeRange()
				.setDescription("annotation before move")
				.setCodeElement(annotationBefore.toString()));
		ranges.add(sourceDeclaration.codeRange()
				.setDescription("source " + sourceDeclaration.getDescription())
				.setCodeElement(sourceDeclaration.getCodeElement()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(annotationAfter.codeRange()
				.setDescription("annotation after move")
				.setCodeElement(annotationAfter.toString()));
		ranges.add(targetDeclaration.codeRange()
				.setDescription("target " + targetDeclaration.getDescription())
				.setCodeElement(targetDeclaration.getCodeElement()));
		return ranges;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.MOVE_ANNOTATION;
	}

	@Override
	public String getName() {
		return getRefactoringType().getDisplayName();
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(sourceDeclaration.getFilePath(), sourceDeclaration.getClassName()));
		return pairs;
	}

	@Override
	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(targetDeclaration.getFilePath(), targetDeclaration.getClassName()));
		return pairs;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(annotationAfter);
		sb.append(" from ");
		sb.append(sourceDeclaration.getDisplayName());
		sb.append(" to ");
		sb.append(targetDeclaration.getDisplayName());
		sb.append(" in class ");
		sb.append(getClassName());
		return sb.toString();
	}

	private String getClassName() {
		return sourceDeclaration.getClassName().equals(targetDeclaration.getClassName()) ?
				sourceDeclaration.getClassName() : targetDeclaration.getClassName();
	}

	@Override
	public int hashCode() {
		return Objects.hash(annotationAfter, annotationBefore, sourceDeclaration, targetDeclaration);
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;
		MoveAnnotationRefactoring other = (MoveAnnotationRefactoring)obj;
		return Objects.equals(annotationAfter, other.annotationAfter) &&
				Objects.equals(annotationBefore, other.annotationBefore) &&
				Objects.equals(sourceDeclaration, other.sourceDeclaration) &&
				Objects.equals(targetDeclaration, other.targetDeclaration);
	}
}
