package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.TypeFactMiner.GlobalContext;
import gr.uom.java.xmi.TypeFactMiner.TypFct;
import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.TypeChangeInstance;
import gr.uom.java.xmi.TypeFactMiner.ExtractSyntacticTypeChange;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import io.vavr.Tuple2;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.api.TypeRelatedRefactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.t2r.common.models.refactorings.ElementKindOuterClass.ElementKind.Return;
import static java.util.stream.Collectors.toList;


public class ChangeReturnTypeRefactoring implements Refactoring, TypeRelatedRefactoring {
	private UMLType originalType;
	private UMLType changedType;
	private UMLOperation operationBefore;
	private UMLOperation operationAfter;
	private Set<AbstractCodeMapping> returnReferences;
	private TypFct typeB4;
	private TypFct typeAfter;
	private List<Tuple2<TypeGraph, TypeGraph>> realTypeChanges;

	@Override
	public TypeChangeInstance getTypeChangeInstance() {
		return typeChangeInstance;
	}

	private TypeChangeInstance typeChangeInstance;

	public ChangeReturnTypeRefactoring(UMLType originalType, UMLType changedType,
			UMLOperation operationBefore, UMLOperation operationAfter, Set<AbstractCodeMapping> returnReferences) {
		this.originalType = originalType;
		this.changedType = changedType;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.returnReferences = returnReferences;
		this.typeB4 = operationBefore.variableTypeGrMap().get("return");
		this.typeAfter = operationAfter.variableTypeGrMap().get("return");
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
	public void updateTypeB4(GlobalContext gc) {
		typeB4.qualify(gc);
	}

	@Override
	public void updateTypeAfter(GlobalContext gc) {
		typeAfter.qualify(gc);
	}



	@Override
	public void extractRealTypeChange() {
		typeChangeInstance = TypeChangeInstance.newBuilder()
				.setNameB4(operationBefore.getName()).setNameAfter(operationAfter.getName())
				.setB4(typeB4.getType()).setAftr(typeAfter.getType())
				.setCompilationUnit(operationBefore.getClassName()).setElementKindAffected(Return)
				.setVisibility(operationBefore.getVisibility())
				.addAllCodeMapping(returnReferences.stream().map(this::getCodeMapping).collect(toList()))
				.setSyntacticUpdate(new ExtractSyntacticTypeChange().extract(typeB4, typeAfter))
				.build();
		realTypeChanges = extractRealTypeChange(typeChangeInstance.getSyntacticUpdate());
	}

	public List<Tuple2<TypeGraph, TypeGraph>> getRealTypeChanges() {
		return realTypeChanges;
	}

	@Override
	public boolean isTypeRelatedChange(){
		return true;
	}
}
