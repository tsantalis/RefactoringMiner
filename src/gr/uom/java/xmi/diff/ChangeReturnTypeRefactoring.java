package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.t2r.common.models.ast.GlobalContext;
import com.t2r.common.models.ast.TypFct;
import com.t2r.common.models.ast.TypeGraphOuterClass;
import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import io.vavr.Tuple3;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import org.refactoringminer.api.TypeRelatedRefactoring;

import static gr.uom.java.xmi.TypeFactMiner.ExtractAndAnalyseTypeChange.realTypeChange;

public class ChangeReturnTypeRefactoring implements Refactoring, TypeRelatedRefactoring {
	private UMLType originalType;
	private UMLType changedType;
	private UMLOperation operationBefore;
	private UMLOperation operationAfter;
	private Set<AbstractCodeMapping> returnReferences;
	private TypFct typeB4;
	private TypFct typeAfter;
	private List<Tuple3<TypeGraph, TypeGraph, List<String>>> realTypeChanges;

	public ChangeReturnTypeRefactoring(UMLType originalType, UMLType changedType,
			UMLOperation operationBefore, UMLOperation operationAfter, Set<AbstractCodeMapping> returnReferences) {
		this.originalType = originalType;
		this.changedType = changedType;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.returnReferences = returnReferences;
		this.typeB4 = getVariableTypeMapB4().get("return");
		this.typeAfter = getVariableTypeMapAfter().get("return");
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.CHANGE_RETURN_TYPE;
	}

	public String getName() {
		return getRefactoringType().getDisplayName();
	}

	public UMLType getOriginalType() {
		return originalType;
	}

	public UMLType getChangedType() {
		return changedType;
	}

	public UMLOperation getOperationBefore() {
		return operationBefore;
	}

	public UMLOperation getOperationAfter() {
		return operationAfter;
	}

	public Set<AbstractCodeMapping> getReturnReferences() {
		return returnReferences;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean qualified = originalType.equals(changedType) && !originalType.equalsQualified(changedType);
		sb.append(getName()).append("\t");
		sb.append(qualified ? originalType.toQualifiedString() : originalType.toString());
		sb.append(" to ");
		sb.append(qualified ? changedType.toQualifiedString() : changedType.toString());
		sb.append(" in method ");
		sb.append(qualified ? operationAfter.toQualifiedString() : operationAfter.toString());
		sb.append(" in class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changedType == null) ? 0 : changedType.hashCode());
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
		result = prime * result + ((originalType == null) ? 0 : originalType.hashCode());
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
		ChangeReturnTypeRefactoring other = (ChangeReturnTypeRefactoring) obj;
		if (changedType == null) {
			if (other.changedType != null)
				return false;
		} else if (!changedType.equals(other.changedType))
			return false;
		if (operationAfter == null) {
			if (other.operationAfter != null)
				return false;
		} else if (!operationAfter.equals(other.operationAfter))
			return false;
		if (operationBefore == null) {
			if (other.operationBefore != null)
				return false;
		} else if (!operationBefore.equals(other.operationBefore))
			return false;
		if (originalType == null) {
			if (other.originalType != null)
				return false;
		} else if (!originalType.equals(other.originalType))
			return false;
		return true;
	}

	public List<String> getInvolvedClassesBeforeRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(operationBefore.getClassName());
		return classNames;
	}

	public List<String> getInvolvedClassesAfterRefactoring() {
		List<String> classNames = new ArrayList<String>();
		classNames.add(operationAfter.getClassName());
		return classNames;
	}


	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(originalType.codeRange()
				.setDescription("original return type")
				.setCodeElement(originalType.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(changedType.codeRange()
				.setDescription("changed return type")
				.setCodeElement(changedType.toString()));
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
	@Override
	public TypFct getTypeAfter() {
		return typeAfter;
	}
	@Override
	public Map<String, TypFct> getFieldTypeMapB4(){
		return operationBefore.getFieldTypeMap();
	}
	@Override
	public Map<String, TypFct> getFieldTypeMapAfter(){
		return operationAfter.getFieldTypeMap();
	}
	@Override
	public Map<String, TypFct> getVariableTypeMapB4(){
		return operationBefore.variableTypeGrMap();
	}
	@Override
	public Map<String, TypFct> getVariableTypeMapAfter(){
		return operationAfter.variableTypeGrMap();
	}

	@Override
	public void updateTypeB4(GlobalContext gc) {
		typeB4.qualify(gc);
	}

	@Override
	public void updateTypeAfter(GlobalContext gc) {
		typeAfter.qualify(gc);
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
