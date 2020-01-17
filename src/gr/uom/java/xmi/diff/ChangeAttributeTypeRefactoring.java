package gr.uom.java.xmi.diff;

import java.util.*;

import com.t2r.common.models.ast.GlobalContext;
import com.t2r.common.models.ast.TypFct;
import com.t2r.common.models.ast.TypeGraphOuterClass;
import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import io.vavr.Tuple3;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.refactoringminer.api.TypeRelatedRefactoring;

import static gr.uom.java.xmi.TypeFactMiner.ExtractAndAnalyseTypeChange.realTypeChange;

public class ChangeAttributeTypeRefactoring implements Refactoring, TypeRelatedRefactoring {
	private VariableDeclaration originalAttribute;
	private VariableDeclaration changedTypeAttribute;
	private String classNameBefore;
	private String classNameAfter;
	private Set<AbstractCodeMapping> attributeReferences;
	private final TypFct typeB4;
	private final TypFct typeAfter;
	private Map<String, TypFct> fieldMapB4;
	private Map<String, TypFct> fieldMapAfter;
	private List<Tuple3<TypeGraph, TypeGraph, List<String>>> realTypeChanges;


	public ChangeAttributeTypeRefactoring(VariableDeclaration originalAttribute,
										  VariableDeclaration changedTypeAttribute, String classNameBefore, String classNameAfter, Set<AbstractCodeMapping> attributeReferences
			, Map<String, TypFct> fieldMapB4, Map<String, TypFct> fieldMapAfter) {
		this.originalAttribute = originalAttribute;
		this.changedTypeAttribute = changedTypeAttribute;
		this.classNameBefore = classNameBefore;
		this.classNameAfter = classNameAfter;
		this.attributeReferences = attributeReferences;
		this.fieldMapB4 = fieldMapB4;
		this.fieldMapAfter = fieldMapAfter;
		this.typeB4 = fieldMapB4.get(originalAttribute.getVariableName());
		this.typeAfter = fieldMapAfter.get(changedTypeAttribute.getVariableName());
	}

	public VariableDeclaration getOriginalAttribute() {
		return originalAttribute;
	}

	public VariableDeclaration getChangedTypeAttribute() {
		return changedTypeAttribute;
	}

	public String getClassNameBefore() {
		return classNameBefore;
	}

	public String getClassNameAfter() {
		return classNameAfter;
	}

	public Set<AbstractCodeMapping> getAttributeReferences() {
		return attributeReferences;
	}

	@Override
	public RefactoringType getRefactoringType() {
		return RefactoringType.CHANGE_ATTRIBUTE_TYPE;
	}

	@Override
	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean qualified = originalAttribute.getType().equals(changedTypeAttribute.getType()) && !originalAttribute.getType().equalsQualified(changedTypeAttribute.getType());
		sb.append(getName()).append("\t");
		sb.append(qualified ? originalAttribute.toQualifiedString() : originalAttribute.toString());
		sb.append(" to ");
		sb.append(qualified ? changedTypeAttribute.toQualifiedString() : changedTypeAttribute.toString());
		sb.append(" in class ").append(classNameAfter);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changedTypeAttribute == null) ? 0 : changedTypeAttribute.hashCode());
		result = prime * result + ((classNameAfter == null) ? 0 : classNameAfter.hashCode());
		result = prime * result + ((classNameBefore == null) ? 0 : classNameBefore.hashCode());
		result = prime * result + ((originalAttribute == null) ? 0 : originalAttribute.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChangeAttributeTypeRefactoring other = (ChangeAttributeTypeRefactoring) obj;
		if (changedTypeAttribute == null) {
			if (other.changedTypeAttribute != null)
				return false;
		} else if (!changedTypeAttribute.equals(other.changedTypeAttribute))
			return false;
		if (classNameAfter == null) {
			if (other.classNameAfter != null)
				return false;
		} else if (!classNameAfter.equals(other.classNameAfter))
			return false;
		if (classNameBefore == null) {
			if (other.classNameBefore != null)
				return false;
		} else if (!classNameBefore.equals(other.classNameBefore))
			return false;
		if (originalAttribute == null) {
			if (other.originalAttribute != null)
				return false;
		} else if (!originalAttribute.equals(other.originalAttribute))
			return false;
		return true;
	}

	@Override
	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(classNameBefore);
		return classNames;
	}

	@Override
	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(classNameAfter);
		return classNames;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalAttribute.codeRange()
				.setDescription("original attribute declaration")
				.setCodeElement(originalAttribute.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(changedTypeAttribute.codeRange()
				.setDescription("changed-type attribute declaration")
				.setCodeElement(changedTypeAttribute.toString()));
		return ranges;
	}

	@Override
	public boolean isResolved() {
		return getTypeB4().isResolved() && getTypeAfter().isResolved();
	}

	@Override
	public TypFct getTypeB4() {
		return typeB4;
	}

	public void updateTypeB4(GlobalContext gc){
		typeB4.qualify(gc);
	}

	public void updateTypeAfter(GlobalContext gc){
		typeAfter.qualify(gc);
	}

	@Override
	public TypFct getTypeAfter() {
		return typeAfter;
	}

	@Override
	public Map<String, TypFct> getFieldTypeMapB4() {
		return fieldMapB4;
	}

	@Override
	public Map<String, TypFct> getFieldTypeMapAfter() {
		return fieldMapAfter;
	}

	@Override
	public Map<String, TypFct> getVariableTypeMapB4() {
		return new HashMap<>();
	}

	@Override
	public Map<String, TypFct> getVariableTypeMapAfter() {
		return new HashMap<>();
	}

	@Override
	public void updateTypeNameSpaceBefore(GlobalContext gc) {
		typeB4.searchNameSpace(gc);
	}

	@Override
	public void updateTypeNameSpaceAfter(GlobalContext gc) {
		typeAfter.searchNameSpace(gc);
	}

	@Override
	public void extractRealTypeChange(GlobalContext gc) {
		realTypeChanges = realTypeChange(typeB4, typeAfter, gc);
	}

	public List<Tuple3<TypeGraph, TypeGraph, List<String>>> getRealTypeChanges() {
		return realTypeChanges;
	}
	@Override
	public boolean isTypeRelatedChange(){
		return true;
	}

}
