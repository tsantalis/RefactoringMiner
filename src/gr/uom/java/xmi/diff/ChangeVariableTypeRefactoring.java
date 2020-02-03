package gr.uom.java.xmi.diff;

import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import com.t2r.common.models.refactorings.ElementKindOuterClass;
import com.t2r.common.models.refactorings.TypeChangeAnalysisOuterClass.TypeChangeAnalysis.TypeChangeInstance;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.TypeFactMiner.ExtractSyntacticTypeChange;
import gr.uom.java.xmi.TypeFactMiner.GlobalContext;
import gr.uom.java.xmi.TypeFactMiner.TypFct;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.api.TypeRelatedRefactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.t2r.common.models.refactorings.ElementKindOuterClass.ElementKind.LocalVariable;


public class ChangeVariableTypeRefactoring implements Refactoring, TypeRelatedRefactoring {
	private VariableDeclaration originalVariable;
	private VariableDeclaration changedTypeVariable;
	private UMLOperation operationBefore;
	private UMLOperation operationAfter;
	private Set<AbstractCodeMapping> variableReferences;
	private final TypFct typeB4;
	private final TypFct typeAfter;
	private List<Tuple2<TypeGraph, TypeGraph>> realTypeChanges;

	@Override
	public TypeChangeInstance getTypeChangeInstance() {
		return typeChangeInstance;
	}

	@Override
	public Set<AbstractCodeMapping> getReferences() {
		return getVariableReferences();
	}

	private TypeChangeInstance typeChangeInstance;

	public ChangeVariableTypeRefactoring(VariableDeclaration originalVariable, VariableDeclaration changedTypeVariable,
			UMLOperation operationBefore, UMLOperation operationAfter, Set<AbstractCodeMapping> variableReferences) {
		this.originalVariable = originalVariable;
		this.changedTypeVariable = changedTypeVariable;
		this.operationBefore = operationBefore;
		this.operationAfter = operationAfter;
		this.variableReferences = variableReferences;
		this.typeB4 = operationBefore.variableTypeGrMap().get(originalVariable.getVariableName());
		this.typeAfter = operationAfter.variableTypeGrMap().get(changedTypeVariable.getVariableName());
	}

	public RefactoringType getRefactoringType() {
		if(originalVariable.isParameter() && changedTypeVariable.isParameter())
			return RefactoringType.CHANGE_PARAMETER_TYPE;
		return RefactoringType.CHANGE_VARIABLE_TYPE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public VariableDeclaration getOriginalVariable() {
		return originalVariable;
	}

	public VariableDeclaration getChangedTypeVariable() {
		return changedTypeVariable;
	}

	public UMLOperation getOperationBefore() {
		return operationBefore;
	}

	public UMLOperation getOperationAfter() {
		return operationAfter;
	}

	public Set<AbstractCodeMapping> getVariableReferences() {
		return variableReferences;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean qualified = originalVariable.getType().equals(changedTypeVariable.getType()) && !originalVariable.getType().equalsQualified(changedTypeVariable.getType());
		sb.append(getName()).append("\t");
		sb.append(qualified ? originalVariable.toQualifiedString() : originalVariable.toString());
		sb.append(" to ");
		sb.append(qualified ? changedTypeVariable.toQualifiedString() : changedTypeVariable.toString());
		sb.append(" in method ");
		sb.append(qualified ? operationAfter.toQualifiedString() : operationAfter.toString());
		sb.append(" in class ").append(operationAfter.getClassName());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changedTypeVariable == null) ? 0 : changedTypeVariable.hashCode());
		result = prime * result + ((operationAfter == null) ? 0 : operationAfter.hashCode());
		result = prime * result + ((operationBefore == null) ? 0 : operationBefore.hashCode());
		result = prime * result + ((originalVariable == null) ? 0 : originalVariable.hashCode());
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
		ChangeVariableTypeRefactoring other = (ChangeVariableTypeRefactoring) obj;
		if (changedTypeVariable == null) {
			if (other.changedTypeVariable != null)
				return false;
		} else if (!changedTypeVariable.equals(other.changedTypeVariable))
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
		if (originalVariable == null) {
			if (other.originalVariable != null)
				return false;
		} else if (!originalVariable.equals(other.originalVariable))
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
		ranges.add(originalVariable.codeRange()
				.setDescription("original variable declaration")
				.setCodeElement(originalVariable.toString()));
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(changedTypeVariable.codeRange()
				.setDescription("changed-type variable declaration")
				.setCodeElement(changedTypeVariable.toString()));
		return ranges;
	}

	@Override
	public boolean isResolved() {
		if(getTypeAfter() == null || getTypeB4() == null )
			System.out.println();
		return getTypeB4().isResolved()  &&  getTypeAfter().isResolved();
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
		TypeChangeInstance.Builder typeChangeInstanceBldr = TypeChangeInstance.newBuilder()
				.setNameB4(originalVariable.getVariableName())
				.setNameAfter(changedTypeVariable.getVariableName())
				.setB4(typeB4.getType()).setAftr(typeAfter.getType())
//				.addAllCodeMapping(variableReferences.stream().map(this::getCodeMapping).collect(toList()))
				.setCompilationUnit(operationBefore.getClassName());
		if(originalVariable.isParameter() && changedTypeVariable.isParameter()){
			typeChangeInstanceBldr.setElementKindAffected(ElementKindOuterClass.ElementKind.Parameter)
					.setVisibility(operationBefore.getVisibility())
					.setSyntacticUpdate(new ExtractSyntacticTypeChange().extract(typeB4, typeAfter))
					.build();
		}else{
			typeChangeInstanceBldr.setElementKindAffected(LocalVariable)
					.setVisibility("Block")
					.setSyntacticUpdate(new ExtractSyntacticTypeChange().extract(typeB4, typeAfter))
					.build();
		}
		typeChangeInstance = typeChangeInstanceBldr.build();
		realTypeChanges = extractRealTypeChange(typeChangeInstance.getSyntacticUpdate());
	}

	public List<Tuple2<TypeGraph, TypeGraph>> getRealTypeChanges() {
		return realTypeChanges;
	}


	@Override
	public void updateTypeChangeInstance(Function<TypeChangeInstance, TypeChangeInstance> fn) {
		typeChangeInstance = fn.apply(typeChangeInstance);
	}



	public Tuple2<LocationInfo, LocationInfo> getLocationOfType(){
		return Tuple.of(originalVariable.getType().getLocationInfo(), changedTypeVariable.getType().getLocationInfo());
	}

	@Override
	public boolean isTypeRelatedChange(){
		return true;
	}
}
