package gr.uom.java.xmi.decomposition;

import static gr.uom.java.xmi.Constants.JAVA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.replacement.ClassInstanceCreationWithMethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.IntersectionReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationWithClassInstanceCreationReplacement;
import gr.uom.java.xmi.decomposition.replacement.ObjectCreationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation.Direction;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.LeafMappingProvider;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;

public abstract class AbstractCodeMapping implements LeafMappingProvider {

	private AbstractCodeFragment fragment1;
	private AbstractCodeFragment fragment2;
	private VariableDeclarationContainer operation1;
	private VariableDeclarationContainer operation2;
	private Set<Replacement> replacements;
	private List<LeafMapping> subExpressionMappings;
	private boolean identicalWithExtractedVariable;
	private boolean identicalWithInlinedVariable;
	private Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
	private int matchingArgumentsWithOperationInvocation;
	private boolean matchedWithNullReplacements;
	private Set<UMLAnonymousClassDiff> anonymousClassDiffs = new LinkedHashSet<UMLAnonymousClassDiff>();
	
	public AbstractCodeMapping(AbstractCodeFragment fragment1, AbstractCodeFragment fragment2,
			VariableDeclarationContainer operation1, VariableDeclarationContainer operation2) {
		this.fragment1 = fragment1;
		this.fragment2 = fragment2;
		this.operation1 = operation1;
		this.operation2 = operation2;
		this.replacements = new LinkedHashSet<Replacement>();
		this.subExpressionMappings = new ArrayList<LeafMapping>();
	}

	public abstract double editDistance();

	public boolean equalContainer() {
		return operation1.equals(operation2);
	}

	public AbstractCodeFragment getFragment1() {
		return fragment1;
	}

	public AbstractCodeFragment getFragment2() {
		return fragment2;
	}

	public VariableDeclarationContainer getOperation1() {
		return operation1;
	}

	public VariableDeclarationContainer getOperation2() {
		return operation2;
	}

	public boolean isIdenticalWithExtractedVariable() {
		return identicalWithExtractedVariable;
	}

	public boolean isIdenticalWithInlinedVariable() {
		return identicalWithInlinedVariable;
	}

	public void addRefactoring(Refactoring r) {
		refactorings.add(r);
	}

	public Set<Refactoring> getRefactorings() {
		return refactorings;
	}

	public Set<UMLAnonymousClassDiff> getAnonymousClassDiffs() {
		return anonymousClassDiffs;
	}

	public boolean containsRefactoringOfType(RefactoringType type) {
		for(Refactoring r : refactorings) {
			if(r.getRefactoringType().equals(type)) {
				return true;
			}
		}
		return false;
	}

	public int getMatchingArgumentsWithOperationInvocation() {
		return matchingArgumentsWithOperationInvocation;
	}

	public void setMatchingArgumentsWithOperationInvocation(int matchingArgumentsWithOperationInvocation) {
		this.matchingArgumentsWithOperationInvocation = matchingArgumentsWithOperationInvocation;
	}

	public boolean isExact() {
		return (fragment1.getArgumentizedString().equals(fragment2.getArgumentizedString()) || argumentizedStringExactAfterTypeReplacement() ||
				fragment1.getString().equals(fragment2.getString()) || isExactAfterAbstraction() || containsIdenticalOrCompositeReplacement()) && !fragment1.isKeyword();
	}

	private boolean argumentizedStringExactAfterTypeReplacement() {
		String s1 = fragment1.getArgumentizedString();
		String s2 = fragment2.getArgumentizedString();
		for(Replacement r : replacements) {
			if(r.getType().equals(ReplacementType.TYPE)) {
				if(s1.startsWith(r.getBefore()) && s2.startsWith(r.getAfter())) {
					String temp = s2.replace(r.getAfter(), r.getBefore());
					if(s1.equals(temp) || (s1 + JAVA.STATEMENT_TERMINATION).equals(temp)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isExactAfterAbstraction() {
		AbstractCall invocation1 = fragment1.invocationCoveringEntireFragment();
		AbstractCall invocation2 = fragment2.invocationCoveringEntireFragment();
		if(invocation1 != null && invocation2 != null) {
			return invocation1.actualString().equals(invocation2.actualString());
		}
		ObjectCreation creation1 = fragment1.creationCoveringEntireFragment();
		ObjectCreation creation2 = fragment2.creationCoveringEntireFragment();
		if(creation1 != null && creation2 != null) {
			return creation1.actualString().equals(creation2.actualString());
		}
		return false;
	}

	public void addSubExpressionMapping(LeafMapping newLeafMapping) {
		boolean alreadyPresent = false; 
		for(LeafMapping oldLeafMapping : subExpressionMappings) { 
			if(oldLeafMapping.getFragment1().getLocationInfo().equals(newLeafMapping.getFragment1().getLocationInfo()) && 
					oldLeafMapping.getFragment2().getLocationInfo().equals(newLeafMapping.getFragment2().getLocationInfo())) { 
				alreadyPresent = true; 
				break; 
			} 
		} 
		if(!alreadyPresent) { 
			subExpressionMappings.add(newLeafMapping); 
		}
	}

	public void addSubExpressionMappings(List<LeafMapping> leafMappings) {
		for(LeafMapping leafMapping : leafMappings) {
			addSubExpressionMapping(leafMapping);
		}
	}

	public List<LeafMapping> getSubExpressionMappings() {
		return subExpressionMappings;
	}

	private boolean containsIdenticalOrCompositeReplacement() {
		for(Replacement r : replacements) {
			if(r.getType().equals(ReplacementType.ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS) &&
					r.getBefore().equals(r.getAfter())) {
				return true;
			}
			else if(r.getType().equals(ReplacementType.COMPOSITE)) {
				return true;
			}
		}
		return false;
	}

	public CompositeReplacement containsCompositeReplacement() {
		for(Replacement r : replacements) {
			if(r.getType().equals(ReplacementType.COMPOSITE)) {
				return (CompositeReplacement)r;
			}
		}
		return null;
	}

	public void addReplacement(Replacement replacement) {
		this.replacements.add(replacement);
	}

	public void addReplacements(Set<Replacement> replacements) {
		if(replacements != null) {
			this.replacements.addAll(replacements);
		}
		else {
			matchedWithNullReplacements = true;
		}
	}

	public boolean isMatchedWithNullReplacements() {
		return matchedWithNullReplacements;
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}

	public boolean containsReplacement(ReplacementType type) {
		for(Replacement replacement : replacements) {
			if(replacement.getType().equals(type)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsOnlyReplacement(ReplacementType type) {
		for(Replacement replacement : replacements) {
			if(!replacement.getType().equals(type)) {
				return false;
			}
		}
		return replacements.size() > 0;
	}

	public Set<ReplacementType> getReplacementTypes() {
		Set<ReplacementType> types = new LinkedHashSet<ReplacementType>();
		for(Replacement replacement : replacements) {
			types.add(replacement.getType());
		}
		return types;
	}

	public String toString() {
		return fragment1.toString() + fragment2.toString();
	}

	public boolean isFieldAssignmentWithParameter() {
		boolean fieldAssignmentWithParameter1 = false;
		for(String parameterName : operation1.getParameterNameList()) {
			if(fragment1.getString().equals(JAVA.THIS_DOT + parameterName + JAVA.ASSIGNMENT + parameterName + JAVA.STATEMENT_TERMINATION)) {
				fieldAssignmentWithParameter1 = true;
				break;
			}
		}
		boolean fieldAssignmentWithParameter2 = false;
		for(String parameterName : operation2.getParameterNameList()) {
			if(fragment2.getString().equals(JAVA.THIS_DOT + parameterName + JAVA.ASSIGNMENT + parameterName + JAVA.STATEMENT_TERMINATION)) {
				fieldAssignmentWithParameter2 = true;
				break;
			}
		}
		return fieldAssignmentWithParameter1 && fieldAssignmentWithParameter2;
	}

	public boolean isFieldAssignmentWithParameterHavingSameType() {
		boolean fieldAssignmentWithParameter1 = false;
		UMLType type1 = null;
		for(UMLParameter parameter : operation1.getParametersWithoutReturnType()) {
			String parameterName = parameter.getName();
			if(fragment1.getString().equals(JAVA.THIS_DOT + parameterName + JAVA.ASSIGNMENT + parameterName + JAVA.STATEMENT_TERMINATION)) {
				fieldAssignmentWithParameter1 = true;
				type1 = parameter.getType();
				break;
			}
		}
		boolean fieldAssignmentWithParameter2 = false;
		UMLType type2 = null;
		for(UMLParameter parameter : operation2.getParametersWithoutReturnType()) {
			String parameterName = parameter.getName();
			if(fragment2.getString().equals(JAVA.THIS_DOT + parameterName + JAVA.ASSIGNMENT + parameterName + JAVA.STATEMENT_TERMINATION)) {
				fieldAssignmentWithParameter2 = true;
				type2 = parameter.getType();
				break;
			}
		}
		return fieldAssignmentWithParameter1 && fieldAssignmentWithParameter2 && type1.equals(type2);
	}

	public void temporaryVariableAssignment(Set<Refactoring> refactorings, List<? extends AbstractCodeFragment> nonMappedLeavesT2, boolean insideExtractedOrInlinedMethod) {
		if(this instanceof LeafMapping && getFragment1() instanceof AbstractExpression
				&& getFragment2() instanceof StatementObject) {
			StatementObject statement = (StatementObject) getFragment2();
			List<VariableDeclaration> variableDeclarations = statement.getVariableDeclarations();
			boolean validReplacements = true;
			for(Replacement replacement : getReplacements()) {
				if(replacement instanceof MethodInvocationReplacement || replacement instanceof ObjectCreationReplacement) {
					validReplacements = false;
					break;
				}
			}
			if(getFragment1().getVariableDeclarations().size() == 0 && variableDeclarations.size() == 1 && validReplacements) {
				VariableDeclaration variableDeclaration = variableDeclarations.get(0);
				ExtractVariableRefactoring ref = new ExtractVariableRefactoring(variableDeclaration, operation1, operation2, insideExtractedOrInlinedMethod);
				LeafMapping leafMapping = new LeafMapping(getFragment1(), variableDeclaration.getInitializer(), operation1, operation2);
				ref.addSubExpressionMapping(leafMapping);
				processExtractVariableRefactoring(ref, refactorings);
				checkForNestedExtractVariable(ref, refactorings, nonMappedLeavesT2, insideExtractedOrInlinedMethod);
				identicalWithExtractedVariable = true;
			}
		}
	}

	private void checkForNestedExtractVariable(ExtractVariableRefactoring parentRefactoring, Set<Refactoring> refactorings, List<? extends AbstractCodeFragment> nonMappedLeavesT2, boolean insideExtractedOrInlinedMethod) {
		for(AbstractCodeFragment leaf2 : nonMappedLeavesT2) {
			List<VariableDeclaration> variableDeclarations = leaf2.getVariableDeclarations();
			if(variableDeclarations.size() == 1) {
				VariableDeclaration variableDeclaration = variableDeclarations.get(0);
				if(variableDeclaration.getInitializer() != null) {
					List<LeafExpression> leafExpressions1 = getFragment1().findExpression(variableDeclaration.getInitializer().getString());
					if(leafExpressions1.isEmpty() && !leaf2.equals(getFragment2())) {
						for(AbstractCall invocation : getFragment1().getMethodInvocations()) {
							if(variableDeclaration.getInitializer().getString().startsWith(invocation.actualString()) || variableDeclaration.getInitializer().getString().endsWith(invocation.actualString())) {
								leafExpressions1 = getFragment1().findExpression(invocation.actualString());
							}
						}
					}
					if(leafExpressions1.size() > 0 && isVariableReferenced(parentRefactoring, variableDeclaration)) {
						ExtractVariableRefactoring ref2 = new ExtractVariableRefactoring(variableDeclaration, operation1, operation2, insideExtractedOrInlinedMethod);
						if(!ref2.equals(parentRefactoring)) {
							for(LeafExpression subExpression : leafExpressions1) {
								LeafMapping leafMapping2 = new LeafMapping(subExpression, variableDeclaration.getInitializer(), operation1, operation2);
								ref2.addSubExpressionMapping(leafMapping2);
							}
							processExtractVariableRefactoring(ref2, refactorings);
						}
					}
				}
			}
		}
	}

	private boolean isVariableReferenced(ExtractVariableRefactoring parentRefactoring, VariableDeclaration variableDeclaration) {
		if(parentRefactoring.getVariableDeclaration().getInitializer() != null && parentRefactoring.getVariableDeclaration().getInitializer().findExpression(variableDeclaration.getVariableName()).size() > 0) {
			return true;
		}
		if(parentRefactoring.getVariableDeclaration().isAttribute()) {
			for(LeafMapping mapping : parentRefactoring.getSubExpressionMappings()) {
				if(ReplacementUtil.contains(mapping.getFragment2().getString(), variableDeclaration.getVariableName())) {
					return true;
				}
			}
		}
		for(AbstractCodeMapping mapping : parentRefactoring.getReferences()) {
			if(variableDeclaration.getScope().subsumes(mapping.getFragment2().getLocationInfo()) && mapping.getFragment2().findExpression(variableDeclaration.getVariableName()).size() > 0 &&
					mapping.getFragment1().findExpression(variableDeclaration.getVariableName()).size() == 0) {
				return true;
			}
		}
		return false;
	}

	private void checkForAliasedVariable(AbstractExpression initializer, Replacement replacement,
			List<? extends AbstractCodeFragment> nonMappedLeavesT2, UMLAbstractClassDiff classDiff, boolean insideExtractedOrInlinedMethod) {
		VariableDeclaration aliasedWithVariable = operation2.getVariableDeclaration(initializer.getString());
		if(aliasedWithVariable != null && aliasedWithVariable.getInitializer() != null) {
			String rightHandSide = aliasedWithVariable.getInitializer().getString();
			if(replacement instanceof VariableReplacementWithMethodInvocation) {
				VariableReplacementWithMethodInvocation r = (VariableReplacementWithMethodInvocation)replacement;
				for(AbstractCall call : aliasedWithVariable.getInitializer().getMethodInvocations()) {
					if(call.equals(r.getInvokedOperation())) {
						ExtractVariableRefactoring ref = new ExtractVariableRefactoring(aliasedWithVariable, operation1, operation2, insideExtractedOrInlinedMethod);
						LeafMapping leafMapping = new LeafMapping(r.getInvokedOperation(), call, operation1, operation2);
						ref.addSubExpressionMapping(leafMapping);
						processExtractVariableRefactoring(ref, refactorings);
						checkForNestedExtractVariable(ref, refactorings, nonMappedLeavesT2, insideExtractedOrInlinedMethod);
						if(identical()) {
							identicalWithExtractedVariable = true;
						}
						break;
					}
				}
			}
			else if(replacement.getBefore().equals(rightHandSide)) {
				ExtractVariableRefactoring ref = new ExtractVariableRefactoring(aliasedWithVariable.getVariableDeclaration(), operation1, operation2, insideExtractedOrInlinedMethod);
				List<LeafExpression> leafExpressions1 = getFragment1().findExpression(rightHandSide);
				List<LeafExpression> leafExpressions2 = aliasedWithVariable.getInitializer().findExpression(rightHandSide);
				if(leafExpressions1.size() == leafExpressions2.size()) {
					for(int i=0; i<leafExpressions1.size(); i++) {
						LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(i), leafExpressions2.get(i), operation1, operation2);
						ref.addSubExpressionMapping(leafMapping);
					}
				}
				processExtractVariableRefactoring(ref, refactorings);
				checkForNestedExtractVariable(ref, refactorings, nonMappedLeavesT2, insideExtractedOrInlinedMethod);
				if(identical()) {
					identicalWithExtractedVariable = true;
				}
			}
		}
		else if(classDiff != null) {
			UMLAttribute aliasedWithAttribute = null;
			if(classDiff.getNextClass().containsAttributeWithName(initializer.getString())) {
				for(UMLAttribute attribute : classDiff.getNextClass().getAttributes()) {
					if(attribute.getName().equals(initializer.getString())) {
						aliasedWithAttribute = attribute;
						break;
					}
				}
			}
			if (classDiff.getModelDiff() != null) {
				UMLClass addedClass = classDiff.getModelDiff().getAddedClass(operation2.getClassName());
				if(addedClass != null && addedClass.containsAttributeWithName(initializer.getString())) {
					for(UMLAttribute attribute : addedClass.getAttributes()) {
						if(attribute.getName().equals(initializer.getString())) {
							aliasedWithAttribute = attribute;
							break;
						}
					}
				}
			}
			if(aliasedWithAttribute != null) {
				for(AbstractCodeFragment leaf2 : nonMappedLeavesT2) {
					if(leaf2.getString().startsWith(initializer.getString() + JAVA.ASSIGNMENT)) {
						String rightHandSide = null;
						if(leaf2.getString().endsWith(JAVA.STATEMENT_TERMINATION)) {
							rightHandSide = leaf2.getString().substring(leaf2.getString().indexOf(JAVA.ASSIGNMENT)+1, leaf2.getString().length()-JAVA.STATEMENT_TERMINATION.length());
						}
						else {
							rightHandSide = leaf2.getString().substring(leaf2.getString().indexOf(JAVA.ASSIGNMENT)+1, leaf2.getString().length());
						}
						if(replacement instanceof VariableReplacementWithMethodInvocation) {
							VariableReplacementWithMethodInvocation r = (VariableReplacementWithMethodInvocation)replacement;
							for(AbstractCall call : leaf2.getMethodInvocations()) {
								if(call.equals(r.getInvokedOperation())) {
									ExtractVariableRefactoring ref = new ExtractVariableRefactoring(aliasedWithAttribute.getVariableDeclaration(), operation1, operation2, insideExtractedOrInlinedMethod);
									LeafMapping leafMapping = new LeafMapping(r.getInvokedOperation(), call, operation1, operation2);
									ref.addSubExpressionMapping(leafMapping);
									processExtractVariableRefactoring(ref, refactorings);
									checkForNestedExtractVariable(ref, refactorings, nonMappedLeavesT2, insideExtractedOrInlinedMethod);
									if(identical()) {
										identicalWithExtractedVariable = true;
									}
									break;
								}
							}
						}
						else if(replacement.getBefore().equals(rightHandSide)) {
							ExtractVariableRefactoring ref = new ExtractVariableRefactoring(aliasedWithAttribute.getVariableDeclaration(), operation1, operation2, insideExtractedOrInlinedMethod);
							List<LeafExpression> leafExpressions1 = getFragment1().findExpression(rightHandSide);
							List<LeafExpression> leafExpressions2 = leaf2.findExpression(rightHandSide);
							if(leafExpressions1.size() == leafExpressions2.size()) {
								for(int i=0; i<leafExpressions1.size(); i++) {
									LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(i), leafExpressions2.get(i), operation1, operation2);
									ref.addSubExpressionMapping(leafMapping);
								}
							}
							processExtractVariableRefactoring(ref, refactorings);
							checkForNestedExtractVariable(ref, refactorings, nonMappedLeavesT2, insideExtractedOrInlinedMethod);
							if(identical()) {
								identicalWithExtractedVariable = true;
							}
						}
						break;
					}
				}
			}
		}
	}

	public void temporaryVariableAssignment(AbstractCodeFragment statement,
			List<? extends AbstractCodeFragment> nonMappedLeavesT2, UMLAbstractClassDiff classDiff, boolean insideExtractedOrInlinedMethod, Set<AbstractCodeMapping> currentMappings) throws RefactoringMinerTimedOutException {
		for(VariableDeclaration declaration : statement.getVariableDeclarations()) {
			String variableName = declaration.getVariableName();
			AbstractExpression initializer = declaration.getInitializer();
			for(Replacement replacement : getReplacements()) {
				String after = replacement.getAfter();
				String before = replacement.getBefore();
				if(replacement.getType().equals(ReplacementType.PARENTHESIZED_EXPRESSION) ||
						replacement.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_PARENTHESIZED_EXPRESSION)) {
					if(after.startsWith("(") && after.endsWith(")")) {
						after = after.substring(1, after.length()-1);
					}
					if(before.startsWith("(") && before.endsWith(")")) {
						before = before.substring(1, before.length()-1);
					}
				}
				if(replacement.getType().equals(ReplacementType.CAST_EXPRESSION)) {
					String cast1 = null;
					String cast2 = null;
					if(after.startsWith("(") && after.contains(")")) {
						cast1 = after.substring(0, after.indexOf(")")+1);
					}
					if(before.startsWith("(") && before.contains(")")) {
						cast2 = before.substring(0, before.indexOf(")")+1);
					}
					if(cast1 != null && cast2 != null && cast1.equals(cast2)) {
						after = after.substring(after.indexOf(")")+1, after.length());
						before = before.substring(before.indexOf(")")+1, before.length());
					}
				}
				if(replacement instanceof MethodInvocationReplacement) {
					MethodInvocationReplacement r = (MethodInvocationReplacement)replacement;
					AbstractCall callBefore = r.getInvokedOperationBefore();
					AbstractCall callAfter = r.getInvokedOperationAfter();
					int indexOfArgument2 = callAfter.arguments().indexOf(variableName);
					if(indexOfArgument2 != -1 && callBefore.arguments().size() == callAfter.arguments().size()) {
						after = variableName;
						before = callBefore.arguments().get(indexOfArgument2);
					}
				}
				if(after.startsWith(variableName + ".")) {
					String suffixAfter = after.substring(variableName.length(), after.length());
					if(before.endsWith(suffixAfter) || before.contains(suffixAfter)) {
						String prefixBefore = before.substring(0, before.indexOf(suffixAfter));
						if(initializer != null) {
							if(initializer.toString().equals(prefixBefore) ||
									overlappingExtractVariable(initializer, prefixBefore, nonMappedLeavesT2, insideExtractedOrInlinedMethod, refactorings)) {
								ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
								List<LeafExpression> subExpressions = getFragment1().findExpression(prefixBefore);
								for(LeafExpression subExpression : subExpressions) {
									LeafMapping leafMapping = new LeafMapping(subExpression, initializer, operation1, operation2);
									ref.addSubExpressionMapping(leafMapping);
								}
								processExtractVariableRefactoring(ref, refactorings);
								checkForNestedExtractVariable(ref, refactorings, nonMappedLeavesT2, insideExtractedOrInlinedMethod);
								if(identical()) {
									identicalWithExtractedVariable = true;
								}
								return;
							}
						}
					}
				}
				else if(after.startsWith("()" + JAVA.LAMBDA_ARROW + variableName + ".")) {
					int extraLength = "()".length() + JAVA.LAMBDA_ARROW.length();
					String suffixAfter = after.substring(extraLength + variableName.length(), after.length());
					if(before.endsWith(suffixAfter) || before.contains(suffixAfter)) {
						String prefixBefore = before.substring(0, before.indexOf(suffixAfter));
						if(prefixBefore.startsWith("()" + JAVA.LAMBDA_ARROW)) {
							prefixBefore = prefixBefore.substring(extraLength);
						}
						if(initializer != null) {
							if(initializer.toString().equals(prefixBefore)) {
								ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
								List<LeafExpression> subExpressions = getFragment1().findExpression(prefixBefore);
								for(LeafExpression subExpression : subExpressions) {
									LeafMapping leafMapping = new LeafMapping(subExpression, initializer, operation1, operation2);
									ref.addSubExpressionMapping(leafMapping);
								}
								processExtractVariableRefactoring(ref, refactorings);
								checkForNestedExtractVariable(ref, refactorings, nonMappedLeavesT2, insideExtractedOrInlinedMethod);
								if(identical()) {
									identicalWithExtractedVariable = true;
								}
								return;
							}
						}
					}
				}
				if(variableName.equals(after) && initializer != null) {
					checkForAliasedVariable(initializer, replacement, nonMappedLeavesT2, classDiff, insideExtractedOrInlinedMethod);
					if(initializer.toString().equals(before) ||
							initializer.toString().equals(JAVA.THIS_DOT + before) ||
							overlappingExtractVariable(initializer, before, nonMappedLeavesT2, insideExtractedOrInlinedMethod, refactorings) ||
							(initializer.toString().equals("(" + declaration.getType() + ")" + before) && !containsVariableNameReplacement(variableName)) ||
							ternaryMatch(initializer, before) ||
							infixOperandMatch(initializer, before) ||
							wrappedAsArgument(initializer, before) ||
							stringConcatMatch(initializer, before) ||
							reservedTokenMatch(initializer, replacement, before) ||
							anonymousWithMethodSignatureChange(initializer, before, classDiff)) {
						ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
						List<LeafExpression> subExpressions = getFragment1().findExpression(before);
						for(LeafExpression subExpression : subExpressions) {
							LeafMapping leafMapping = new LeafMapping(subExpression, initializer, operation1, operation2);
							ref.addSubExpressionMapping(leafMapping);
						}
						if(infixOperandMatch(initializer, before)) {
							List<LeafExpression> infixExpressions = initializer.getInfixExpressions();
							for(LeafExpression infixExpression : infixExpressions) {
								if(infixExpression.getString().contains(JAVA.STRING_CONCATENATION)) {
									List<String> operands = Arrays.asList(StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN.split(infixExpression.getString()));
									for(String operand : operands) {
										List<LeafExpression> leafExpressions2 = initializer.findExpression(operand);
										List<LeafExpression> leafExpressions1 = fragment1.findExpression(operand);
										if(leafExpressions1.size() == leafExpressions2.size()) {
											for(int i=0; i<leafExpressions1.size(); i++) {
												LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(i), leafExpressions2.get(i), operation1, operation2);
												ref.addSubExpressionMapping(leafMapping);
											}
										}
									}
								}
							}
						}
						else if(stringConcatMatch(initializer, before)) {
							if(initializer.getString().contains(JAVA.STRING_CONCATENATION) && !before.contains(JAVA.STRING_CONCATENATION)) {
								List<String> operands = Arrays.asList(StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN.split(initializer.getString()));
								List<LeafMapping> leafMappings = new ArrayList<LeafMapping>();
								StringBuilder concatenated = new StringBuilder();
								for(String operand : operands) {
									List<LeafExpression> leafExpressions2 = initializer.findExpression(operand);
									List<LeafExpression> leafExpressions1 = fragment1.findExpression(before);
									if(leafExpressions1.size() == leafExpressions2.size() && leafExpressions1.size() == 1) {
										LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(0), leafExpressions2.get(0), operation1, operation2);
										leafMappings.add(leafMapping);
									}
									if(operand.startsWith("\"") && operand.endsWith("\"")) {
										concatenated.append(operand.substring(1, operand.length()-1));
									}
									else {
										concatenated.append(operand);
									}
								}
								if(before.contains(concatenated)) {
									for(LeafMapping leafMapping : leafMappings) {
										ref.addSubExpressionMapping(leafMapping);
									}
								}
							}
						}
						processExtractVariableRefactoring(ref, refactorings);
						checkForNestedExtractVariable(ref, refactorings, nonMappedLeavesT2, insideExtractedOrInlinedMethod);
						if(identical()) {
							identicalWithExtractedVariable = true;
						}
						return;
					}
				}
			}
			if(classDiff != null && initializer != null) {
				AbstractCall invocation = initializer.invocationCoveringEntireFragment();
				if(invocation != null) {
					for(Refactoring refactoring : classDiff.getRefactoringsBeforePostProcessing()) {
						if(refactoring instanceof RenameOperationRefactoring) {
							RenameOperationRefactoring rename = (RenameOperationRefactoring)refactoring;
							if(invocation.getName().equals(rename.getRenamedOperation().getName())) {
								String initializerBeforeRename = initializer.getString().replace(rename.getRenamedOperation().getName(), rename.getOriginalOperation().getName());
								if(getFragment1().getString().contains(initializerBeforeRename) && getFragment2().getString().contains(variableName)) {
									ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
									List<LeafExpression> subExpressions = getFragment1().findExpression(initializerBeforeRename);
									for(LeafExpression subExpression : subExpressions) {
										LeafMapping leafMapping = new LeafMapping(subExpression, initializer, operation1, operation2);
										ref.addSubExpressionMapping(leafMapping);
									}
									processExtractVariableRefactoring(ref, refactorings);
									checkForNestedExtractVariable(ref, refactorings, nonMappedLeavesT2, insideExtractedOrInlinedMethod);
									return;
								}
							}
						}
					}
				}
			}
			if(classDiff != null && getFragment1().getVariableDeclarations().size() > 0 && initializer != null && getFragment1().getVariableDeclarations().toString().equals(getFragment2().getVariableDeclarations().toString())) {
				VariableDeclaration variableDeclaration1 = getFragment1().getVariableDeclarations().get(0);
				if(variableDeclaration1.getInitializer() != null && variableDeclaration1.getInitializer().toString().contains(initializer.toString()) && !isDefaultValue(variableDeclaration1.getInitializer().toString())) {
					boolean callToAddedOperation = false;
					boolean callToDeletedOperation = false;
					AbstractCall invocationCoveringTheEntireStatement1 = getFragment1().invocationCoveringEntireFragment();
					if(invocationCoveringTheEntireStatement1 != null) {
						callToDeletedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getRemovedOperations(), operation1) != null;
					}
					AbstractCall invocationCoveringTheEntireStatement2 = getFragment2().invocationCoveringEntireFragment();
					if(invocationCoveringTheEntireStatement2 != null) {
						callToAddedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getAddedOperations(), operation2) != null;
					}
					boolean equalInvocations = invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
							(invocationCoveringTheEntireStatement1.equals(invocationCoveringTheEntireStatement2) || containsOnlyReplacement(ReplacementType.METHOD_INVOCATION_NAME));
					if(callToAddedOperation != callToDeletedOperation && !equalInvocations) {
						ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
						List<LeafExpression> subExpressions = getFragment1().findExpression(initializer.toString());
						for(LeafExpression subExpression : subExpressions) {
							LeafMapping leafMapping = new LeafMapping(subExpression, initializer, operation1, operation2);
							ref.addSubExpressionMapping(leafMapping);
						}
						processExtractVariableRefactoring(ref, refactorings);
						checkForNestedExtractVariable(ref, refactorings, nonMappedLeavesT2, insideExtractedOrInlinedMethod);
						return;
					}
				}
			}
		}
		if(getFragment2().getVariableDeclarations().size() > 0 && getFragment2().getVariableDeclarations().get(0).getInitializer() != null && replacements.size() == 1) {
			checkForAliasedVariable(getFragment2().getVariableDeclarations().get(0).getInitializer(), replacements.iterator().next(), nonMappedLeavesT2, classDiff, insideExtractedOrInlinedMethod);
		}
		String argumentizedString = statement.getArgumentizedString();
		if(argumentizedString.contains(JAVA.ASSIGNMENT) && (statement.getLocationInfo().before(fragment2.getLocationInfo()) || fragment2.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT))) {
			String beforeAssignment = argumentizedString.substring(0, argumentizedString.indexOf(JAVA.ASSIGNMENT));
			String[] tokens = beforeAssignment.split("\\s");
			String variable = tokens[tokens.length-1];
			String initializer = null;
			if(argumentizedString.endsWith(JAVA.STATEMENT_TERMINATION)) {
				initializer = argumentizedString.substring(argumentizedString.indexOf(JAVA.ASSIGNMENT)+1, argumentizedString.length()-JAVA.STATEMENT_TERMINATION.length());
			}
			else {
				initializer = argumentizedString.substring(argumentizedString.indexOf(JAVA.ASSIGNMENT)+1, argumentizedString.length());
			}
			for(Replacement replacement : getReplacements()) {
				if(variable.endsWith(replacement.getAfter()) &&	(initializer.equals(replacement.getBefore()) ||
						initializer.contains(": " + replacement.getBefore()) || initializer.contains("? " + replacement.getBefore()))) {
					List<VariableDeclaration> variableDeclarations = operation2.getVariableDeclarationsInScope(fragment2.getLocationInfo());
					for(VariableDeclaration declaration : variableDeclarations) {
						if(declaration.getVariableName().equals(variable)) {
							boolean declarationMappingFound = false;
							for(AbstractCodeMapping currentMapping : currentMappings) {
								if(currentMapping.getFragment2().getVariableDeclarations().contains(declaration) &&
										currentMapping.getFragment1().getVariableDeclaration(variable) != null) {
									declarationMappingFound = true;
									break;
								}
							}
							if(!declarationMappingFound) {
								ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
								List<LeafExpression> subExpressions = getFragment1().findExpression(replacement.getBefore());
								for(LeafExpression subExpression : subExpressions) {
									List<LeafExpression> initializerSubExpressions = statement.findExpression(initializer);
									if(initializerSubExpressions.size() > 0) {
										LeafMapping leafMapping = new LeafMapping(subExpression, initializerSubExpressions.get(0), operation1, operation2);
										ref.addSubExpressionMapping(leafMapping);
									}
								}
								processExtractVariableRefactoring(ref, refactorings);
								checkForNestedExtractVariable(ref, refactorings, nonMappedLeavesT2, insideExtractedOrInlinedMethod);
								if(identical()) {
									identicalWithExtractedVariable = true;
								}
								return;
							}
						}
					}
				}
			}
		}
	}

	private boolean anonymousWithMethodSignatureChange(AbstractExpression initializer, String before, UMLAbstractClassDiff classDiff) throws RefactoringMinerTimedOutException {
		if(initializer.getAnonymousClassDeclarations().size() > 0) {
			for(AnonymousClassDeclarationObject anonymousDeclaration : initializer.getAnonymousClassDeclarations()) {
				UMLAnonymousClass anonymousClass = operation1.findAnonymousClass(anonymousDeclaration);
				if(anonymousClass == null) {
					anonymousClass = operation2.findAnonymousClass(anonymousDeclaration);
					//check if operation1 has anonymous with equal signature
					if(anonymousClass != null) {
						for(UMLAnonymousClass otherAnonymousClass : operation1.getAnonymousClassList()) {
							UMLAnonymousClassDiff anonymousClassDiff = new UMLAnonymousClassDiff(otherAnonymousClass, anonymousClass, classDiff, classDiff != null ? classDiff.getModelDiff() : null);
							anonymousClassDiff.process();
							List<UMLOperationBodyMapper> matchedOperationMappers = anonymousClassDiff.getOperationBodyMapperList();
							if(matchedOperationMappers.size() > 0) {
								anonymousClassDiffs.add(anonymousClassDiff);
								return true;
							}
						}
					}
				}
				else {
					//check if operation2 has anonymous with equal signature
					for(UMLAnonymousClass otherAnonymousClass : operation2.getAnonymousClassList()) {
						UMLAnonymousClassDiff anonymousClassDiff = new UMLAnonymousClassDiff(anonymousClass, otherAnonymousClass, classDiff, classDiff != null ? classDiff.getModelDiff() : null);
						anonymousClassDiff.process();
						List<UMLOperationBodyMapper> matchedOperationMappers = anonymousClassDiff.getOperationBodyMapperList();
						if(matchedOperationMappers.size() > 0) {
							anonymousClassDiffs.add(anonymousClassDiff);
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean isDefaultValue(String argument) {
		return argument.equals("null") || argument.equals("0") || argument.equals("1") || argument.equals("false") || argument.equals("true");
	}

	public void inlinedVariableAssignment(AbstractCodeFragment statement,
			List<? extends AbstractCodeFragment> nonMappedLeavesT2, UMLAbstractClassDiff classDiff, boolean insideExtractedOrInlinedMethod) throws RefactoringMinerTimedOutException {
		for(VariableDeclaration declaration : statement.getVariableDeclarations()) {
			AbstractExpression initializer = declaration.getInitializer();
			String variableName = declaration.getVariableName();
			for(Replacement replacement : getReplacements()) {
				String after = replacement.getAfter();
				String before = replacement.getBefore();
				if(replacement.getType().equals(ReplacementType.PARENTHESIZED_EXPRESSION) ||
						replacement.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_PARENTHESIZED_EXPRESSION)) {
					if(after.startsWith("(") && after.endsWith(")")) {
						after = after.substring(1, after.length()-1);
					}
					if(before.startsWith("(") && before.endsWith(")")) {
						before = before.substring(1, before.length()-1);
					}
				}
				if(replacement.getType().equals(ReplacementType.CAST_EXPRESSION)) {
					String cast1 = null;
					String cast2 = null;
					if(after.startsWith("(") && after.contains(")")) {
						cast1 = after.substring(0, after.indexOf(")")+1);
					}
					if(before.startsWith("(") && before.contains(")")) {
						cast2 = before.substring(0, before.indexOf(")")+1);
					}
					if(cast1 != null && cast2 != null && cast1.equals(cast2)) {
						after = after.substring(after.indexOf(")")+1, after.length());
						before = before.substring(before.indexOf(")")+1, before.length());
					}
				}
				if(before.startsWith(variableName + ".")) {
					String suffixBefore = before.substring(variableName.length(), before.length());
					if(after.endsWith(suffixBefore) || after.contains(suffixBefore)) {
						String prefixAfter = after.substring(0, after.indexOf(suffixBefore));
						if(initializer != null) {
							if(initializer.toString().equals(prefixAfter) ||
									overlappingExtractVariable(initializer, prefixAfter, nonMappedLeavesT2, insideExtractedOrInlinedMethod, refactorings)) {
								InlineVariableRefactoring ref = new InlineVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
								List<LeafExpression> subExpressions = getFragment2().findExpression(prefixAfter);
								for(LeafExpression subExpression : subExpressions) {
									LeafMapping leafMapping = new LeafMapping(initializer, subExpression, operation1, operation2);
									ref.addSubExpressionMapping(leafMapping);
								}
								processInlineVariableRefactoring(ref, refactorings);
								if(identical()) {
									identicalWithInlinedVariable = true;
								}
								return;
							}
						}
					}
				}
				else if(before.startsWith("()" + JAVA.LAMBDA_ARROW + variableName + ".")) {
					int extraLength = "()".length() + JAVA.LAMBDA_ARROW.length();
					String suffixBefore = before.substring(extraLength + variableName.length(), before.length());
					if(after.endsWith(suffixBefore) || after.contains(suffixBefore)) {
						String prefixAfter = after.substring(0, after.indexOf(suffixBefore));
						if(prefixAfter.startsWith("()" + JAVA.LAMBDA_ARROW)) {
							prefixAfter = prefixAfter.substring(extraLength);
						}
						if(initializer != null) {
							if(initializer.toString().equals(prefixAfter)) {
								InlineVariableRefactoring ref = new InlineVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
								List<LeafExpression> subExpressions = getFragment2().findExpression(prefixAfter);
								for(LeafExpression subExpression : subExpressions) {
									LeafMapping leafMapping = new LeafMapping(initializer, subExpression, operation1, operation2);
									ref.addSubExpressionMapping(leafMapping);
								}
								processInlineVariableRefactoring(ref, refactorings);
								if(identical()) {
									identicalWithInlinedVariable = true;
								}
								return;
							}
						}
					}
				}
				if(variableName.equals(before) && initializer != null) {
					if(initializer.toString().equals(after) ||
							initializer.toString().equals(JAVA.THIS_DOT + after) ||
							overlappingExtractVariable(initializer, after, nonMappedLeavesT2, insideExtractedOrInlinedMethod, refactorings) ||
							(initializer.toString().equals("(" + declaration.getType() + ")" + after) && !containsVariableNameReplacement(variableName)) ||
							ternaryMatch(initializer, after) ||
							infixOperandMatch(initializer, after) ||
							wrappedAsArgument(initializer, after) ||
							stringConcatMatch(initializer, after) ||
							reservedTokenMatch(initializer, replacement, after) ||
							anonymousWithMethodSignatureChange(initializer, after, classDiff)) {
						InlineVariableRefactoring ref = new InlineVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
						List<LeafExpression> subExpressions = getFragment2().findExpression(after);
						for(LeafExpression subExpression : subExpressions) {
							LeafMapping leafMapping = new LeafMapping(initializer, subExpression, operation1, operation2);
							ref.addSubExpressionMapping(leafMapping);
						}
						if(infixOperandMatch(initializer, after)) {
							List<LeafExpression> infixExpressions = initializer.getInfixExpressions();
							for(LeafExpression infixExpression : infixExpressions) {
								if(infixExpression.getString().contains(JAVA.STRING_CONCATENATION)) {
									List<String> operands = Arrays.asList(StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN.split(infixExpression.getString()));
									for(String operand : operands) {
										List<LeafExpression> leafExpressions1 = initializer.findExpression(operand);
										List<LeafExpression> leafExpressions2 = fragment2.findExpression(operand);
										if(leafExpressions1.size() == leafExpressions2.size()) {
											for(int i=0; i<leafExpressions1.size(); i++) {
												LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(i), leafExpressions2.get(i), operation1, operation2);
												ref.addSubExpressionMapping(leafMapping);
											}
										}
									}
								}
							}
						}
						else if(stringConcatMatch(initializer, after)) {
							if(initializer.getString().contains(JAVA.STRING_CONCATENATION) && !after.contains(JAVA.STRING_CONCATENATION)) {
								List<String> operands = Arrays.asList(StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN.split(initializer.getString()));
								List<LeafMapping> leafMappings = new ArrayList<LeafMapping>();
								StringBuilder concatenated = new StringBuilder();
								for(String operand : operands) {
									List<LeafExpression> leafExpressions1 = initializer.findExpression(operand);
									List<LeafExpression> leafExpressions2 = fragment2.findExpression(after);
									if(leafExpressions1.size() == leafExpressions2.size() && leafExpressions1.size() == 1) {
										LeafMapping leafMapping = new LeafMapping(leafExpressions1.get(0), leafExpressions2.get(0), operation1, operation2);
										leafMappings.add(leafMapping);
									}
									if(operand.startsWith("\"") && operand.endsWith("\"")) {
										concatenated.append(operand.substring(1, operand.length()-1));
									}
									else {
										concatenated.append(operand);
									}
								}
								if(after.contains(concatenated)) {
									for(LeafMapping leafMapping : leafMappings) {
										ref.addSubExpressionMapping(leafMapping);
									}
								}
							}
						}
						processInlineVariableRefactoring(ref, refactorings);
						if(identical()) {
							identicalWithInlinedVariable = true;
						}
						return;
					}
				}
			}
			if(classDiff != null && getFragment1().getVariableDeclarations().size() > 0 && initializer != null && getFragment1().getVariableDeclarations().toString().equals(getFragment2().getVariableDeclarations().toString())) {
				VariableDeclaration variableDeclaration2 = getFragment2().getVariableDeclarations().get(0);
				if(variableDeclaration2.getInitializer() != null && variableDeclaration2.getInitializer().toString().contains(initializer.toString())) {
					boolean callToAddedOperation = false;
					boolean callToDeletedOperation = false;
					AbstractCall invocationCoveringTheEntireStatement1 = getFragment1().invocationCoveringEntireFragment();
					if(invocationCoveringTheEntireStatement1 != null) {
						callToDeletedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement1, classDiff.getRemovedOperations(), operation1) != null;
					}
					AbstractCall invocationCoveringTheEntireStatement2 = getFragment2().invocationCoveringEntireFragment();
					if(invocationCoveringTheEntireStatement2 != null) {
						callToAddedOperation = classDiff.matchesOperation(invocationCoveringTheEntireStatement2, classDiff.getAddedOperations(), operation2) != null;
					}
					boolean equalInvocations = invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
							(invocationCoveringTheEntireStatement1.equals(invocationCoveringTheEntireStatement2) || containsOnlyReplacement(ReplacementType.METHOD_INVOCATION_NAME));
					if(callToAddedOperation != callToDeletedOperation && !equalInvocations) {
						InlineVariableRefactoring ref = new InlineVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
						List<LeafExpression> subExpressions = getFragment2().findExpression(initializer.toString());
						for(LeafExpression subExpression : subExpressions) {
							LeafMapping leafMapping = new LeafMapping(initializer, subExpression, operation1, operation2);
							ref.addSubExpressionMapping(leafMapping);
						}
						processInlineVariableRefactoring(ref, refactorings);
						return;
					}
				}
			}
		}
		String argumentizedString = statement.getArgumentizedString();
		if(argumentizedString.contains(JAVA.ASSIGNMENT) && (statement.getLocationInfo().before(fragment1.getLocationInfo()) || fragment1.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT))) {
			String beforeAssignment = argumentizedString.substring(0, argumentizedString.indexOf(JAVA.ASSIGNMENT));
			String[] tokens = beforeAssignment.split("\\s");
			String variable = tokens[tokens.length-1];
			String initializer = null;
			if(argumentizedString.endsWith(JAVA.STATEMENT_TERMINATION)) {
				initializer = argumentizedString.substring(argumentizedString.indexOf(JAVA.ASSIGNMENT)+1, argumentizedString.length()-JAVA.STATEMENT_TERMINATION.length());
			}
			else {
				initializer = argumentizedString.substring(argumentizedString.indexOf(JAVA.ASSIGNMENT)+1, argumentizedString.length());
			}
			for(Replacement replacement : getReplacements()) {
				if(variable.endsWith(replacement.getBefore()) && initializer.equals(replacement.getAfter())) {
					List<VariableDeclaration> variableDeclarations = operation1.getVariableDeclarationsInScope(fragment1.getLocationInfo());
					for(VariableDeclaration declaration : variableDeclarations) {
						if(declaration.getVariableName().equals(variable)) {
							InlineVariableRefactoring ref = new InlineVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
							List<LeafExpression> subExpressions = getFragment2().findExpression(replacement.getAfter());
							for(LeafExpression subExpression : subExpressions) {
								List<LeafExpression> initializerSubExpressions = statement.findExpression(initializer);
								if(initializerSubExpressions.size() > 0) {
									LeafMapping leafMapping = new LeafMapping(initializerSubExpressions.get(0), subExpression, operation1, operation2);
									ref.addSubExpressionMapping(leafMapping);
								}
							}
							processInlineVariableRefactoring(ref, refactorings);
							if(identical()) {
								identicalWithInlinedVariable = true;
							}
							return;
						}
					}
				}
			}
		}
	}

	private boolean identical() {
		if(getReplacements().size() == 1 && fragment1.getVariableDeclarations().size() == fragment2.getVariableDeclarations().size()) {
			return true;
		}
		if(fragment1.getVariableDeclarations().size() == fragment2.getVariableDeclarations().size() && fragment2.getTernaryOperatorExpressions().size() > 0) {
			TernaryOperatorExpression ternary = fragment2.getTernaryOperatorExpressions().get(0);
			AbstractExpression thenExpression = ternary.getThenExpression();
			AbstractCodeFragment elseExpression = ternary.getElseExpression();
			String temp = new String(fragment1.getString());
			if(replacements.size() > 0) {
				Replacement r = replacements.iterator().next();
				for(Refactoring ref : refactorings) {
					if(ref instanceof ExtractVariableRefactoring) {
						ExtractVariableRefactoring extract = (ExtractVariableRefactoring)ref;
						if(extract.getVariableDeclaration().getInitializer() != null &&
								extract.getVariableDeclaration().getInitializer().getString().equals(r.getBefore())) {
							temp = ReplacementUtil.performReplacement(temp, r.getBefore(), r.getAfter());
							if(temp.endsWith(elseExpression.getString() + JAVA.STATEMENT_TERMINATION)) {
								return true;
							}
							if(temp.endsWith(thenExpression.getString() + JAVA.STATEMENT_TERMINATION)) {
								return true;
							}
						}
					}
				}
			}
		}
		int stringLiteralReplacents = 0;
		for(Replacement r : replacements) {
			if((r.getBefore().startsWith("\"") && r.getBefore().endsWith("\"")) || (r.getAfter().startsWith("\"") && r.getAfter().endsWith("\""))) {
				stringLiteralReplacents++;
			}
		}
		if(stringLiteralReplacents == replacements.size()) {
			return true;
		}
		if(refactorings.size() > 1) {
			String temp = new String(fragment1.getString());
			for(Refactoring ref : refactorings) {
				if(ref instanceof InlineVariableRefactoring) {
					InlineVariableRefactoring inline = (InlineVariableRefactoring)ref;
					for(Replacement r : replacements) {
						if(inline.getVariableDeclaration().getInitializer() != null &&
								inline.getVariableDeclaration().getInitializer().getString().equals(r.getAfter())) {
							if(r.getBefore().equals(inline.getVariableDeclaration().getVariableName())) {
								temp = ReplacementUtil.performReplacement(temp, inline.getVariableDeclaration().getVariableName(), r.getAfter());
							}
							else {
								temp = ReplacementUtil.performReplacement(temp, inline.getVariableDeclaration().getVariableName(), r.getBefore());
							}
						}
					}
				}
			}
			if(temp.equals(fragment2.getString())) {
				return true;
			}
		}
		if(getReplacements().size() == 2 && fragment1.getVariableDeclarations().size() == fragment2.getVariableDeclarations().size()) {
			boolean listToArrayConversion = false;
			boolean identicalCallWithExtraArguments = false;
			for(Replacement r : replacements) {
				if(r instanceof VariableReplacementWithMethodInvocation) {
					VariableReplacementWithMethodInvocation replacement = (VariableReplacementWithMethodInvocation)r;
					AbstractCall call = replacement.getInvokedOperation();
					if(call.getName().equals("toArray") && call.getExpression() != null) {
						if(replacement.getDirection().equals(Direction.VARIABLE_TO_INVOCATION) && call.getExpression().equals(r.getBefore())) {
							listToArrayConversion = true;
						}
						else if(replacement.getDirection().equals(Direction.INVOCATION_TO_VARIABLE) && call.getExpression().equals(r.getAfter())) {
							listToArrayConversion = true;
						}
					}
				}
				else if(r instanceof MethodInvocationReplacement) {
					MethodInvocationReplacement replacement = (MethodInvocationReplacement)r;
					AbstractCall before = replacement.getInvokedOperationBefore();
					AbstractCall after = replacement.getInvokedOperationAfter();
					if(before.identicalName(after) && before.argumentIntersection(after).size() == Math.min(before.arguments().size(), after.arguments().size())) {
						identicalCallWithExtraArguments = true;
					}
				}
			}
			if(listToArrayConversion || identicalCallWithExtraArguments) {
				return true;
			}
		}
		return false;
	}

	private boolean wrappedAsArgument(AbstractExpression initializer, String replacedExpression) {
		int replacementCount = 0;
		for(Replacement r : replacements) {
			if(r.getBefore().equals(replacedExpression) || r.getAfter().equals(replacedExpression)) {
				replacementCount++;
			}
		}
		if(replacementCount > 1) {
			return false;
		}
		AbstractCall invocation = initializer.invocationCoveringEntireFragment();
		if(invocation != null) {
			if(invocation.arguments().contains(replacedExpression)) {
				return true;
			}
			String expression = invocation.getExpression();
			if(expression != null && (expression.equals(replacedExpression) || ReplacementUtil.contains(expression, replacedExpression))) {
				boolean subExpressionIsCallToSameMethod = false;
				if(invocation instanceof OperationInvocation) {
					String subExpression = ((OperationInvocation)invocation).subExpressionIsCallToSameMethod();
					if(subExpression != null && ReplacementUtil.contains(subExpression, replacedExpression)) {
						subExpressionIsCallToSameMethod = true;
					}
				}
				if(!subExpressionIsCallToSameMethod) {
					return true;
				}
			}
		}
		ObjectCreation creation = initializer.creationCoveringEntireFragment();
		if(creation != null) {
			if(creation.arguments().contains(replacedExpression)) {
				return true;
			}
		}
		return false;
	}

	private boolean infixOperandMatch(AbstractExpression initializer, String replacedExpression) {
		List<LeafExpression> infixExpressions = initializer.getInfixExpressions();
		for(LeafExpression infixExpression : infixExpressions) {
			String infix = infixExpression.getString();
			if(infix.startsWith(replacedExpression) || infix.endsWith(replacedExpression)) {
				return true;
			}
		}
		return false;
	}

	private boolean ternaryMatch(AbstractExpression initializer, String replacedExpression) {
		List<TernaryOperatorExpression> ternaryList = initializer.getTernaryOperatorExpressions();
		for(TernaryOperatorExpression ternary : ternaryList) {
			if(ternary.getThenExpression().toString().equals(replacedExpression) || ternary.getElseExpression().toString().equals(replacedExpression)) {
				return true;
			}
		}
		return false;
	}

	private boolean stringConcatMatch(AbstractExpression initializer, String replacedExpression) {
		String s1 = initializer.getString();
		String s2 = replacedExpression;
		if(s1.contains(JAVA.STRING_CONCATENATION) && s2.contains(JAVA.STRING_CONCATENATION)) {
			Set<String> tokens1 = new LinkedHashSet<String>(Arrays.asList(StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN.split(s1)));
			Set<String> tokens2 = new LinkedHashSet<String>(Arrays.asList(StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN.split(s2)));
			Set<String> intersection = new LinkedHashSet<String>(tokens1);
			intersection.retainAll(tokens2);
			return intersection.size() == Math.min(tokens1.size(), tokens2.size());
		}
		else if(s1.contains(JAVA.STRING_CONCATENATION) && !s2.contains(JAVA.STRING_CONCATENATION)) {
			List<String> tokens1 = Arrays.asList(StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN.split(s1));
			StringBuilder concatenated = new StringBuilder();
			for(int i=0; i<tokens1.size(); i++) {
				String token = tokens1.get(i);
				if(token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
					concatenated.append(token.substring(1, token.length()-1));
				}
				else {
					concatenated.append(token);
				}
			}
			if(s2.contains(concatenated)) {
				return true;
			}
		}
		else if(!s1.contains(JAVA.STRING_CONCATENATION) && s2.contains(JAVA.STRING_CONCATENATION)) {
			List<String> tokens2 = Arrays.asList(StringBasedHeuristics.SPLIT_CONCAT_STRING_PATTERN.split(s2));
			StringBuilder concatenated = new StringBuilder();
			for(int i=0; i<tokens2.size(); i++) {
				String token = tokens2.get(i);
				if(token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
					concatenated.append(token.substring(1, token.length()-1));
				}
				else {
					concatenated.append(token);
				}
			}
			if(s1.contains(concatenated)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsVariableNameReplacement(String variableName) {
		for(Replacement replacement : getReplacements()) {
			if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
				if(replacement.getBefore().equals(variableName) || replacement.getAfter().equals(variableName)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean reservedTokenMatch(AbstractExpression initializer, Replacement replacement, String replacedExpression) {
		AbstractCall initializerInvocation = initializer.invocationCoveringEntireFragment();
		AbstractCall replacementInvocation = replacement instanceof VariableReplacementWithMethodInvocation ? ((VariableReplacementWithMethodInvocation)replacement).getInvokedOperation() : null;
		boolean methodInvocationMatch = true;
		if(initializerInvocation != null && replacementInvocation != null) {
			if(!initializerInvocation.getName().equals(replacementInvocation.getName())) {
				methodInvocationMatch = false;
			}
			if(initializerInvocation.identicalName(replacementInvocation) && initializerInvocation.identicalExpression(replacementInvocation)) {
				MethodInvocationReplacement r = new MethodInvocationReplacement(replacementInvocation.actualString(), initializerInvocation.actualString(), replacementInvocation, initializerInvocation, ReplacementType.METHOD_INVOCATION_ARGUMENT);
				this.replacements.add(r);
				return true;
			}
		}
		else if(initializerInvocation != null && replacementInvocation == null) {
			methodInvocationMatch = false;
		}
		else if(initializerInvocation == null && replacementInvocation != null) {
			methodInvocationMatch = false;
		}
		AbstractCall initializerCreation = initializer.creationCoveringEntireFragment();
		String replacementCreation = replacement.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_CLASS_INSTANCE_CREATION) ? replacement.getAfter() : null;
		if(initializerCreation != null && replacementCreation != null) {
			if(replacementCreation.startsWith("new " + initializerCreation.getName())) {
				List<String> arguments = initializerCreation.arguments();
				int matchingArguments = 0;
				for(String argument : arguments) {
					if(replacementCreation.contains(argument)) {
						matchingArguments++;
					}
				}
				if(matchingArguments == arguments.size()) {
					return true;
				}
			}
		}
		String initializerReservedTokens = ReplacementUtil.keepReservedTokens(initializer.toString());
		String replacementReservedTokens = ReplacementUtil.keepReservedTokens(replacedExpression);
		return methodInvocationMatch && !initializerReservedTokens.isEmpty() && !initializerReservedTokens.equals("[]") && !initializerReservedTokens.equals(".()") && !initializerReservedTokens.equals(" ()") && initializerReservedTokens.equals(replacementReservedTokens);
	}

	private void processInlineVariableRefactoring(InlineVariableRefactoring ref, Set<Refactoring> refactorings) {
		if(!refactorings.contains(ref)) {
			ref.addReference(this);
			refactorings.add(ref);
		}
		else {
			for(Refactoring refactoring : refactorings) {
				if(refactoring.equals(ref)) {
					InlineVariableRefactoring inlineVariableRefactoring = (InlineVariableRefactoring)refactoring;
					inlineVariableRefactoring.addReference(this);
					for(LeafMapping newLeafMapping : ref.getSubExpressionMappings()) {
						inlineVariableRefactoring.addSubExpressionMapping(newLeafMapping);
					}
					break;
				}
			}
		}
	}

	private void processExtractVariableRefactoring(ExtractVariableRefactoring ref, Set<Refactoring> refactorings) {
		if(!refactorings.contains(ref)) {
			ref.addReference(this);
			refactorings.add(ref);
		}
		else {
			for(Refactoring refactoring : refactorings) {
				if(refactoring.equals(ref)) {
					ExtractVariableRefactoring extractVariableRefactoring = (ExtractVariableRefactoring)refactoring;
					extractVariableRefactoring.addReference(this);
					for(LeafMapping newLeafMapping : ref.getSubExpressionMappings()) {
						extractVariableRefactoring.addSubExpressionMapping(newLeafMapping);
					}
					break;
				}
			}
		}
	}

	private boolean overlappingExtractVariable(AbstractExpression initializer, String input, List<? extends AbstractCodeFragment> nonMappedLeavesT2,
			boolean insideExtractedOrInlinedMethod, Set<Refactoring> refactorings) {
		String output = input;
		for(Refactoring ref : refactorings) {
			if(ref instanceof ExtractVariableRefactoring) {
				ExtractVariableRefactoring extractVariable = (ExtractVariableRefactoring)ref;
				VariableDeclaration declaration = extractVariable.getVariableDeclaration();
				if(declaration.getInitializer() != null && input.contains(declaration.getInitializer().toString())) {
					output = output.replace(declaration.getInitializer().toString(), declaration.getVariableName());
				}
			}
		}
		if(initializer.toString().equals(output)) {
			return true;
		}
		String longestCommonSuffix = PrefixSuffixUtils.longestCommonSuffix(initializer.toString(), input);
		if(!longestCommonSuffix.isEmpty() && longestCommonSuffix.startsWith(".")) {
			String prefix1 = initializer.toString().substring(0, initializer.toString().indexOf(longestCommonSuffix));
			String prefix2 = input.substring(0, input.indexOf(longestCommonSuffix));
			//skip static variable prefixes
			if(prefix1.equals(prefix2) || (!prefix1.toUpperCase().equals(prefix1) && !prefix2.toUpperCase().equals(prefix2))) {
				return true;
			}
		}
		String longestCommonPrefix = PrefixSuffixUtils.longestCommonPrefix(initializer.toString(), input);
		if(!longestCommonSuffix.isEmpty() && !longestCommonPrefix.isEmpty() &&
				!longestCommonPrefix.equals(initializer.toString()) && !longestCommonPrefix.equals(input) &&
				!longestCommonSuffix.equals(initializer.toString()) && !longestCommonSuffix.equals(input) &&
				longestCommonPrefix.length() + longestCommonSuffix.length() < input.length() &&
				longestCommonPrefix.length() + longestCommonSuffix.length() < initializer.toString().length()) {
			String s1 = input.substring(longestCommonPrefix.length(), input.lastIndexOf(longestCommonSuffix));
			String s2 = initializer.toString().substring(longestCommonPrefix.length(), initializer.toString().lastIndexOf(longestCommonSuffix));
			for(AbstractCodeFragment statement : nonMappedLeavesT2) {
				VariableDeclaration variable = statement.getVariableDeclaration(s2);
				if(variable != null) {
					if(variable.getInitializer() != null && variable.getInitializer().toString().equals(s1)) {
						ExtractVariableRefactoring ref = new ExtractVariableRefactoring(variable, operation1, operation2, insideExtractedOrInlinedMethod);
						List<LeafExpression> subExpressions = getFragment1().findExpression(s1);
						for(LeafExpression subExpression : subExpressions) {
							LeafMapping leafMapping = new LeafMapping(subExpression, variable.getInitializer(), operation1, operation2);
							ref.addSubExpressionMapping(leafMapping);
						}
						processExtractVariableRefactoring(ref, refactorings);
						return true;
					}
					List<TernaryOperatorExpression> ternaryOperators = statement.getTernaryOperatorExpressions();
					for(TernaryOperatorExpression ternaryOperator : ternaryOperators) {
						if(ternaryOperator.getThenExpression().toString().equals(s1) ||
								ternaryOperator.getElseExpression().toString().equals(s1)) {
							ExtractVariableRefactoring ref = new ExtractVariableRefactoring(variable, operation1, operation2, insideExtractedOrInlinedMethod);
							List<LeafExpression> subExpressions = getFragment1().findExpression(s1);
							for(LeafExpression subExpression : subExpressions) {
								AbstractCodeFragment initializerSubExpression =
										ternaryOperator.getThenExpression().toString().equals(s1) ?
										ternaryOperator.getThenExpression() : ternaryOperator.getElseExpression();
								LeafMapping leafMapping = new LeafMapping(subExpression, initializerSubExpression, operation1, operation2);
								ref.addSubExpressionMapping(leafMapping);
							}
							processExtractVariableRefactoring(ref, refactorings);
							return true;
						}
					}
				}
			}
		}
		for(AbstractCodeFragment leaf2 : nonMappedLeavesT2) {
			List<VariableDeclaration> variableDeclarations = leaf2.getVariableDeclarations();
			if(variableDeclarations.size() == 1) {
				VariableDeclaration variableDeclaration = variableDeclarations.get(0);
				if(variableDeclaration.getInitializer() != null && initializer.findExpression(variableDeclaration.getVariableName()).size() > 0) {
					List<LeafExpression> leafExpressions1 = getFragment1().findExpression(input);
					if(leafExpressions1.size() > 0 && variableDeclaration.getInitializer().findExpression(input).size() > 0) {
						ExtractVariableRefactoring ref = new ExtractVariableRefactoring(variableDeclaration, operation1, operation2, insideExtractedOrInlinedMethod);
						for(LeafExpression subExpression : leafExpressions1) {
							LeafMapping leafMapping = new LeafMapping(subExpression, variableDeclaration.getInitializer(), operation1, operation2);
							ref.addSubExpressionMapping(leafMapping);
						}
						processExtractVariableRefactoring(ref, refactorings);
						return true;
					}
				}
			}
		}
		return false;
	}

	public Set<Replacement> commonReplacements(AbstractCodeMapping other) {
		Set<Replacement> intersection = new LinkedHashSet<Replacement>(this.replacements);
		intersection.retainAll(other.replacements);
		return intersection;
	}

	public Set<Replacement> getReplacementsInvolvingMethodInvocation() {
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		for(Replacement replacement : getReplacements()) {
			if(involvesMethodInvocation(replacement)) {
				replacements.add(replacement);
			}
		}
		return replacements;
	}

	public Pair<CompositeStatementObject, CompositeStatementObject> nestedUnderCatchBlock() {
		CompositeStatementObject parent1 = fragment1.getParent();
		CompositeStatementObject parent2 = fragment2.getParent();
		while(parent1 != null && parent2 != null) {
			if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
					parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
				return Pair.of(parent1, parent2);
			}
			else if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) &&
					parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK)) {
				return Pair.of(parent1, parent2);
			}
			parent1 = parent1.getParent();
			parent2 = parent2.getParent();
		}
		return null;
	}

	private static boolean involvesMethodInvocation(Replacement replacement) {
		return replacement instanceof MethodInvocationReplacement ||
				replacement instanceof VariableReplacementWithMethodInvocation ||
				replacement instanceof ClassInstanceCreationWithMethodInvocationReplacement ||
				replacement instanceof MethodInvocationWithClassInstanceCreationReplacement ||
				replacement.getType().equals(ReplacementType.ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION) ||
				replacement.getType().equals(ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION) ||
				replacement.getType().equals(ReplacementType.ARGUMENT_REPLACED_WITH_METHOD_INVOCATION) ||
				replacement.getType().equals(ReplacementType.METHOD_INVOCATION_REPLACED_WITH_CONDITIONAL_EXPRESSION) ||
				replacement instanceof IntersectionReplacement ||
				replacement.getType().equals(ReplacementType.ANONYMOUS_CLASS_DECLARATION) ||
				replacement.getType().equals(ReplacementType.LAMBDA_WITH_BODY_REPLACED_LAMBDA_WITH_EXPRESSION);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fragment1 == null) ? 0 : fragment1.hashCode());
		result = prime * result + ((fragment2 == null) ? 0 : fragment2.hashCode());
		result = prime * result + ((operation1 == null) ? 0 : operation1.hashCode());
		result = prime * result + ((operation2 == null) ? 0 : operation2.hashCode());
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
		AbstractCodeMapping other = (AbstractCodeMapping) obj;
		if (fragment1 == null) {
			if (other.fragment1 != null)
				return false;
		} else if (!fragment1.equals(other.fragment1))
			return false;
		if (fragment2 == null) {
			if (other.fragment2 != null)
				return false;
		} else if (!fragment2.equals(other.fragment2))
			return false;
		if (operation1 == null) {
			if (other.operation1 != null)
				return false;
		} else if (!operation1.equals(other.operation1))
			return false;
		if (operation2 == null) {
			if (other.operation2 != null)
				return false;
		} else if (!operation2.equals(other.operation2))
			return false;
		return true;
	}
}
