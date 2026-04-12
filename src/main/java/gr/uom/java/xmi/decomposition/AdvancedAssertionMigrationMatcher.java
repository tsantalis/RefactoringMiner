package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.api.Refactoring;

class AdvancedAssertionMigrationMatcher {
	private final UMLOperationBodyMapper mapper;
	private boolean advancedAssertJTypeNarrowingPostProcessed;
	private boolean advancedAssertThatThrownByPostProcessed;

	AdvancedAssertionMigrationMatcher(UMLOperationBodyMapper mapper) {
		this.mapper = mapper;
	}

	void postProcessBeforeRefactorings() {
		ensureAdvancedAssertJTypeNarrowingPostProcessed();
		ensureAdvancedAssertThatThrownByPostProcessed();
	}

	List<LeafExpression> findAssertJTypeNarrowingExpressions(AbstractCodeFragment fragment, String initializerString) {
		if(initializerString == null) {
			return Collections.emptyList();
		}
		AdvancedAssertJTypeNarrowing narrowing = extractAdvancedAssertJTypeNarrowing(fragment);
		if(narrowing == null) {
			return Collections.emptyList();
		}
		if(matchesAssertJTypeCheckExpression(initializerString, narrowing, Set.of(narrowing.baseExpression)) ||
				matchesAssertJCastExpression(initializerString, narrowing, Set.of(narrowing.baseExpression))) {
			List<LeafExpression> subExpressions = fragment.findExpression(narrowing.asInstanceOfExpression);
			if(!subExpressions.isEmpty()) {
				return subExpressions;
			}
		}
		return Collections.emptyList();
	}

	private void postProcessAdvancedAssertJTypeNarrowingMappings() {
		for(AbstractCodeMapping mapping : new ArrayList<>(mapper.getMappings())) {
			AdvancedAssertJTypeNarrowing narrowing = extractAdvancedAssertJTypeNarrowing(mapping.getFragment2());
			if(narrowing == null) {
				continue;
			}
			Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
			List<LeafMapping> additionalLeafMappings = new ArrayList<>();
			Map<String, AdvancedAssertJInlineCandidate> inlineCandidates = new LinkedHashMap<>();
			Set<String> narrowedSubjects = new LinkedHashSet<>();
			narrowedSubjects.add(narrowing.baseExpression);
			boolean progress;
			do {
				progress = false;
				for(AbstractCodeFragment leaf : new ArrayList<>(mapper.getNonMappedLeavesT1())) {
					if(leaf.equals(mapping.getFragment1()) || additionallyMatchedStatements1.contains(leaf)) {
						continue;
					}
					if(matchesAssertJTypeCheckDeclaration(leaf, narrowing, narrowedSubjects)) {
						additionallyMatchedStatements1.add(leaf);
						addNarrowingSubExpressionMapping(leaf, narrowing.baseExpression + " instanceof " + narrowing.narrowedType,
								mapping.getFragment2(), narrowing.asInstanceOfExpression, additionalLeafMappings);
						VariableDeclaration declaration = leaf.getVariableDeclarations().get(0);
						inlineCandidates.put(declaration.getVariableName(), new AdvancedAssertJInlineCandidate(declaration));
						mapper.removeMatchedVariables(leaf);
						progress = true;
					}
					else {
						String alias = extractAssertJCastAlias(leaf, narrowing, narrowedSubjects);
						if(alias != null) {
							additionallyMatchedStatements1.add(leaf);
							narrowedSubjects.add(alias);
							VariableDeclaration declaration = leaf.getVariableDeclarations().get(0);
							addNarrowingSubExpressionMapping(leaf, declaration.getInitializer().getString(),
									mapping.getFragment2(), narrowing.asInstanceOfExpression, additionalLeafMappings);
							mapper.removeMatchedVariables(leaf);
							progress = true;
						}
						else if(matchesOptionalClassGuard(leaf, narrowing, narrowedSubjects) ||
								matchesAssertJTypeCheckAssertion(leaf, narrowing, narrowedSubjects)) {
							additionallyMatchedStatements1.add(leaf);
							progress = true;
						}
					}
				}
			}
			while(progress);
			boolean collapsedCastEvidence = hasCastNarrowingEvidenceInOperation(narrowing, narrowedSubjects);
			for(AbstractCodeFragment matchedStatement : additionallyMatchedStatements1) {
				VariableDeclaration assertedDeclaration = assertedTypeCheckDeclaration(matchedStatement, narrowing, narrowedSubjects);
				if(assertedDeclaration != null) {
					AdvancedAssertJInlineCandidate candidate = inlineCandidates.computeIfAbsent(assertedDeclaration.getVariableName(),
							name -> new AdvancedAssertJInlineCandidate(assertedDeclaration));
					candidate.referenceStatements.add(matchedStatement);
				}
			}
			for(AbstractCodeFragment leaf : new ArrayList<>(mapper.getNonMappedLeavesT1())) {
				if(leaf.equals(mapping.getFragment1()) || additionallyMatchedStatements1.contains(leaf)) {
					continue;
				}
				if(matchesCollapsedAssertJTypeNarrowingAssertion(leaf, narrowing, narrowedSubjects) ||
						matchesCollapsedAssertJSizeCheck(leaf, narrowing, narrowedSubjects) ||
						matchesCollapsedAssertJIndexedElementCheck(leaf, narrowing, narrowedSubjects) ||
						matchesCollapsedAssertJOptionalPresenceCheck(leaf, narrowing, narrowedSubjects) ||
						matchesCollapsedAssertJOptionalValueCheck(leaf, narrowing, narrowedSubjects)) {
					additionallyMatchedStatements1.add(leaf);
				}
			}
			if(!additionallyMatchedStatements1.isEmpty()) {
				mapper.addCompositeReplacement(mapping, additionallyMatchedStatements1, Collections.emptySet());
				mapper.addSubExpressionMappings(mapping, additionalLeafMappings);
				mapper.getNonMappedLeavesT1().removeAll(additionallyMatchedStatements1);
				if(collapsedCastEvidence) {
					registerInlineVariableRefactorings(mapping, narrowing, inlineCandidates);
				}
			}
		}
	}

	private void ensureAdvancedAssertJTypeNarrowingPostProcessed() {
		if(!advancedAssertJTypeNarrowingPostProcessed) {
			postProcessAdvancedAssertJTypeNarrowingMappings();
			advancedAssertJTypeNarrowingPostProcessed = true;
		}
	}

	private void postProcessAdvancedAssertThatThrownByMappings() {
		for(AbstractCodeMapping mapping : new ArrayList<>(mapper.getMappings())) {
			if(!containsAssertThatThrownBy(mapping.getFragment2())) {
				continue;
			}
			String thrownVariable = assertThrowsVariable(mapping.getFragment1());
			if(thrownVariable == null) {
				continue;
			}
			Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
			List<LeafMapping> additionalLeafMappings = new ArrayList<>();
			for(AbstractCodeFragment leaf : new ArrayList<>(mapper.getNonMappedLeavesT1())) {
				if(leaf.equals(mapping.getFragment1()) || additionallyMatchedStatements1.contains(leaf)) {
					continue;
				}
				LeafMapping typeMapping = matchCollapsedAssertThrowableAssertion(leaf, mapping.getFragment2(), thrownVariable);
				if(typeMapping != null) {
					additionallyMatchedStatements1.add(leaf);
					additionalLeafMappings.add(typeMapping);
				}
			}
			if(!additionallyMatchedStatements1.isEmpty()) {
				mapper.addCompositeReplacement(mapping, additionallyMatchedStatements1, Collections.emptySet());
				mapper.addSubExpressionMappings(mapping, additionalLeafMappings);
				mapper.getNonMappedLeavesT1().removeAll(additionallyMatchedStatements1);
			}
		}
	}

	private void ensureAdvancedAssertThatThrownByPostProcessed() {
		if(!advancedAssertThatThrownByPostProcessed) {
			postProcessAdvancedAssertThatThrownByMappings();
			advancedAssertThatThrownByPostProcessed = true;
		}
	}

	private boolean containsAssertThatThrownBy(AbstractCodeFragment fragment) {
		return fragment.getString().contains(mapper.LANG2.ASSERT_THAT_THROWN_BY + "(");
	}

	private String assertThrowsVariable(AbstractCodeFragment fragment) {
		if(fragment.getVariableDeclarations().size() == 1) {
			VariableDeclaration declaration = fragment.getVariableDeclarations().get(0);
			AbstractExpression initializer = declaration.getInitializer();
			if(initializer != null) {
				AbstractCall invocation = initializer.invocationCoveringEntireFragment();
				if(invocation != null && invocation.getName().equals(mapper.LANG1.ASSERT_THROWS)) {
					return declaration.getVariableName();
				}
			}
		}
		return null;
	}

	private LeafMapping matchCollapsedAssertThrowableAssertion(AbstractCodeFragment leaf, AbstractCodeFragment target, String thrownVariable) {
		AbstractCall invocation = leaf.invocationCoveringEntireFragment();
		if(invocation == null || !invocation.getName().equals("assertThrowable") || invocation.arguments().size() < 2) {
			return null;
		}
		String assertedThrowable = invocation.arguments().get(0).replace(" ", "");
		if(!assertedThrowable.equals(thrownVariable)) {
			return null;
		}
		String assertedType = invocation.arguments().get(1);
		if((!target.getString().contains(".isInstanceOf(") && !target.getString().contains(".isExactlyInstanceOf(")) ||
				!target.getString().contains(assertedType)) {
			return null;
		}
		List<LeafExpression> sourceExpressions = leaf.findExpression(assertedType);
		List<LeafExpression> targetExpressions = target.findExpression(assertedType);
		UMLOperation operation1 = mapper.getOperation1();
		UMLOperation operation2 = mapper.getOperation2();
		if(sourceExpressions.size() == 1 && targetExpressions.size() == 1 && operation1 != null && operation2 != null) {
			return new LeafMapping(sourceExpressions.get(0), targetExpressions.get(0), operation1, operation2);
		}
		return null;
	}

	private void addNarrowingSubExpressionMapping(AbstractCodeFragment fragment, String expression1,
			AbstractCodeFragment targetFragment, String expression2,
			List<LeafMapping> additionalLeafMappings) {
		if(expression1 == null || expression2 == null) {
			return;
		}
		List<LeafExpression> leftExpressions = fragment.findExpression(expression1);
		List<LeafExpression> rightExpressions = targetFragment.findExpression(expression2);
		VariableDeclarationContainer container1 = mapper.getContainer1();
		VariableDeclarationContainer container2 = mapper.getContainer2();
		if(leftExpressions.size() == 1 && rightExpressions.size() == 1 && container1 != null && container2 != null) {
			additionalLeafMappings.add(new LeafMapping(leftExpressions.get(0), rightExpressions.get(0), container1, container2));
		}
	}

	private void registerInlineVariableRefactorings(AbstractCodeMapping mapping, AdvancedAssertJTypeNarrowing narrowing,
			Map<String, AdvancedAssertJInlineCandidate> inlineCandidates) {
		UMLOperation operation1 = mapper.getOperation1();
		UMLOperation operation2 = mapper.getOperation2();
		if(operation1 == null || operation2 == null) {
			return;
		}
		for(AdvancedAssertJInlineCandidate candidate : inlineCandidates.values()) {
			AbstractExpression initializer = candidate.declaration.getInitializer();
			if(initializer == null) {
				continue;
			}
			InlineVariableRefactoring ref = new InlineVariableRefactoring(candidate.declaration, operation1, operation2,
					mapper.getParentMapper() != null);
			for(AbstractCodeFragment referenceStatement : candidate.referenceStatements) {
				ref.addReference(new LeafMapping(referenceStatement, mapping.getFragment2(), operation1, operation2));
			}
			if(candidate.referenceStatements.isEmpty()) {
				ref.addUnmatchedStatementReference(mapping.getFragment2());
			}
			List<LeafExpression> targetExpressions = mapping.getFragment2().findExpression(narrowing.asInstanceOfExpression);
			if(targetExpressions.size() == 1) {
				ref.addSubExpressionMapping(new LeafMapping(initializer, targetExpressions.get(0), operation1, operation2));
			}
			registerInlineVariableRefactoring(ref);
		}
	}

	private void registerInlineVariableRefactoring(InlineVariableRefactoring ref) {
		Set<Refactoring> refactorings = mapper.getRefactoringsAfterPostProcessing();
		if(!refactorings.contains(ref)) {
			refactorings.add(ref);
			return;
		}
		for(Refactoring existingRefactoring : refactorings) {
			if(existingRefactoring.equals(ref) && existingRefactoring instanceof InlineVariableRefactoring existingInline) {
				existingInline.addReferences(ref.getReferences());
				existingInline.addUnmatchedStatementReferences(ref.getUnmatchedStatementReferences());
				for(LeafMapping newLeafMapping : ref.getSubExpressionMappings()) {
					existingInline.addSubExpressionMapping(newLeafMapping);
				}
				break;
			}
		}
	}

	private AdvancedAssertJTypeNarrowing extractAdvancedAssertJTypeNarrowing(AbstractCodeFragment fragment) {
		AbstractCall assertThatInvocation = firstMethodInvocation(fragment, "assertThat");
		AbstractCall asInstanceOfInvocation = firstMethodInvocation(fragment, "asInstanceOf");
		AbstractCall terminalInvocation = fragment.invocationCoveringEntireFragment();
		if(assertThatInvocation == null || asInstanceOfInvocation == null || terminalInvocation == null ||
				assertThatInvocation.arguments().size() != 1 || asInstanceOfInvocation.arguments().size() != 1) {
			return null;
		}
		String factoryExpression = asInstanceOfInvocation.arguments().get(0);
		String factoryKind = assertJFactoryKind(factoryExpression);
		if(factoryKind == null) {
			return null;
		}
		String narrowedType = assertJNarrowedType(factoryExpression);
		if(narrowedType == null) {
			return null;
		}
		return new AdvancedAssertJTypeNarrowing(assertThatInvocation.arguments().get(0), asInstanceOfInvocation.actualString(),
				factoryKind, narrowedType, terminalInvocation.getName(), terminalInvocation.arguments());
	}

	private String assertJFactoryKind(String factoryExpression) {
		if(factoryExpression == null) {
			return null;
		}
		if(factoryExpression.equals("LIST")) {
			return "LIST";
		}
		if(factoryExpression.startsWith("array(")) {
			return "array";
		}
		if(factoryExpression.startsWith("type(")) {
			return "type";
		}
		if(factoryExpression.startsWith("optional(")) {
			return "optional";
		}
		return null;
	}

	private String assertJNarrowedType(String factoryExpression) {
		if(factoryExpression == null) {
			return null;
		}
		if(factoryExpression.equals("LIST")) {
			return "List";
		}
		if(factoryExpression.startsWith("optional(")) {
			return "Optional";
		}
		int classLiteralIndex = factoryExpression.indexOf(".class");
		if(classLiteralIndex > 0) {
			int start = factoryExpression.indexOf('(');
			if(start >= 0 && start < classLiteralIndex) {
				return factoryExpression.substring(start + 1, classLiteralIndex).trim();
			}
		}
		return null;
	}

	private boolean matchesAssertJTypeCheckDeclaration(AbstractCodeFragment fragment, AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		if(fragment.getVariableDeclarations().size() != 1) {
			return false;
		}
		VariableDeclaration declaration = fragment.getVariableDeclarations().get(0);
		AbstractExpression initializer = declaration.getInitializer();
		return initializer != null &&
				matchesAssertJTypeCheckExpression(initializer.getString(), narrowing, narrowedSubjects);
	}

	private boolean matchesAssertJTypeCheckAssertion(AbstractCodeFragment fragment, AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		AbstractCall terminalInvocation = fragment.invocationCoveringEntireFragment();
		AbstractCall assertThatInvocation = firstMethodInvocation(fragment, "assertThat");
		if(terminalInvocation == null || assertThatInvocation == null || !terminalInvocation.getName().equals("isTrue") ||
				assertThatInvocation.arguments().size() != 1) {
			return false;
		}
		String assertedExpression = assertThatInvocation.arguments().get(0);
		if(matchesAssertJTypeCheckExpression(assertedExpression, narrowing, narrowedSubjects)) {
			return true;
		}
		VariableDeclaration declaration = fragment.searchVariableDeclaration(assertedExpression);
		return declaration != null && declaration.getInitializer() != null &&
				matchesAssertJTypeCheckExpression(declaration.getInitializer().getString(), narrowing, narrowedSubjects);
	}

	private boolean hasCastNarrowingEvidence(AbstractCodeFragment fragment, AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		if(fragment == null) {
			return false;
		}
		if(extractAssertJCastAlias(fragment, narrowing, narrowedSubjects) != null) {
			return true;
		}
		for(VariableDeclaration declaration : fragment.getVariableDeclarations()) {
			AbstractExpression initializer = declaration.getInitializer();
			if(initializer != null && matchesAssertJCastExpression(initializer.getString(), narrowing, narrowedSubjects)) {
				return true;
			}
		}
		AbstractCall assertThatInvocation = firstMethodInvocation(fragment, "assertThat");
		return assertThatInvocation != null && assertThatInvocation.arguments().size() == 1 &&
				matchesAssertJCastExpression(assertThatInvocation.arguments().get(0), narrowing, narrowedSubjects);
	}

	private boolean hasCastNarrowingEvidenceInOperation(AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		UMLOperation operation1 = mapper.getOperation1();
		if(operation1 == null) {
			return false;
		}
		if(operation1.getBody() != null) {
			for(AbstractCodeFragment leaf : operation1.getBody().getCompositeStatement().getLeaves()) {
				if(hasCastNarrowingEvidence(leaf, narrowing, narrowedSubjects)) {
					return true;
				}
			}
		}
		AbstractExpression defaultExpression = operation1.getDefaultExpression();
		return defaultExpression != null && hasCastNarrowingEvidence(defaultExpression, narrowing, narrowedSubjects);
	}

	private VariableDeclaration assertedTypeCheckDeclaration(AbstractCodeFragment fragment, AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		AbstractCall terminalInvocation = fragment.invocationCoveringEntireFragment();
		AbstractCall assertThatInvocation = firstMethodInvocation(fragment, "assertThat");
		if(terminalInvocation == null || assertThatInvocation == null || !terminalInvocation.getName().equals("isTrue") ||
				assertThatInvocation.arguments().size() != 1) {
			return null;
		}
		String assertedExpression = assertThatInvocation.arguments().get(0);
		VariableDeclaration declaration = fragment.searchVariableDeclaration(assertedExpression);
		if(declaration == null || declaration.getInitializer() == null ||
				!matchesAssertJTypeCheckExpression(declaration.getInitializer().getString(), narrowing, narrowedSubjects)) {
			return null;
		}
		return declaration;
	}

	private boolean matchesOptionalClassGuard(AbstractCodeFragment fragment, AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		if(!narrowing.factoryKind.equals("optional")) {
			return false;
		}
		AbstractCall terminalInvocation = fragment.invocationCoveringEntireFragment();
		AbstractCall assertThatInvocation = firstMethodInvocation(fragment, "assertThat");
		return terminalInvocation != null && assertThatInvocation != null &&
				terminalInvocation.getName().equals("isEqualTo") &&
				terminalInvocation.arguments().size() == 1 &&
				terminalInvocation.arguments().get(0).equals("Optional.class") &&
				assertThatInvocation.arguments().size() == 1 &&
				assertThatInvocation.arguments().get(0).equals(narrowing.baseExpression + ".getClass()");
	}

	private String extractAssertJCastAlias(AbstractCodeFragment fragment, AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		if(fragment.getVariableDeclarations().size() != 1) {
			return null;
		}
		VariableDeclaration declaration = fragment.getVariableDeclarations().get(0);
		AbstractExpression initializer = declaration.getInitializer();
		if(initializer == null) {
			return null;
		}
		String initializerString = initializer.getString();
		String castType = castType(initializerString);
		String strippedExpression = stripCast(initializerString);
		if(castType != null && strippedExpression != null &&
				narrowedSubjects.contains(strippedExpression) &&
				matchesAssertJCastType(castType, narrowing)) {
			return declaration.getVariableName();
		}
		return null;
	}

	private boolean matchesCollapsedAssertJTypeNarrowingAssertion(AbstractCodeFragment fragment,
			AdvancedAssertJTypeNarrowing narrowing, Set<String> narrowedSubjects) {
		AbstractCall terminalInvocation = fragment.invocationCoveringEntireFragment();
		AbstractCall assertThatInvocation = firstMethodInvocation(fragment, "assertThat");
		if(terminalInvocation == null || assertThatInvocation == null || assertThatInvocation.arguments().size() != 1) {
			return false;
		}
		String assertThatArgument = assertThatInvocation.arguments().get(0);
		if(!matchesAssertJNarrowedSubject(assertThatArgument, narrowing, narrowedSubjects)) {
			return false;
		}
		return terminalMatchesCollapsedAssertJAssertion(fragment, terminalInvocation, narrowing);
	}

	private boolean terminalMatchesCollapsedAssertJAssertion(AbstractCodeFragment fragment, AbstractCall terminalInvocation,
			AdvancedAssertJTypeNarrowing narrowing) {
		if(narrowing.terminalName.equals(terminalInvocation.getName()) &&
				terminalInvocation.arguments().equals(narrowing.terminalArguments)) {
			return true;
		}
		if(terminalInvocation.getName().equals("isEqualTo")) {
			if(matchesAssertJIterableCollapse(fragment, terminalInvocation.arguments(), narrowing)) {
				return true;
			}
			if(narrowing.terminalArguments.size() == 1 && terminalInvocation.arguments().size() == 1 &&
					terminalInvocation.arguments().get(0).equals(narrowing.terminalArguments.get(0))) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesAssertJIterableCollapse(AbstractCodeFragment fragment, List<String> oldArguments,
			AdvancedAssertJTypeNarrowing narrowing) {
		if(oldArguments.size() != 1) {
			return false;
		}
		if(!(narrowing.terminalName.equals("containsExactly") || narrowing.terminalName.equals("containsOnly") ||
				narrowing.terminalName.equals("containsExactlyInAnyOrder") || narrowing.terminalName.equals("contains"))) {
			return false;
		}
		String oldArgument = oldArguments.get(0);
		AbstractCall asListInvocation = firstMethodInvocation(fragment, "asList");
		if(asListInvocation != null && asListInvocation.actualString().equals(oldArgument)) {
			return asListInvocation.arguments().equals(narrowing.terminalArguments);
		}
		List<String> arrayElements = arrayInitializerElements(oldArgument);
		if(!arrayElements.isEmpty()) {
			return arrayElements.equals(narrowing.terminalArguments);
		}
		return narrowing.terminalArguments.size() == 1 && oldArgument.equals(narrowing.terminalArguments.get(0));
	}

	private boolean matchesCollapsedAssertJSizeCheck(AbstractCodeFragment fragment, AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		if(!(narrowing.terminalName.equals("containsExactly") || narrowing.terminalName.equals("containsOnly") ||
				narrowing.terminalName.equals("containsExactlyInAnyOrder"))) {
			return false;
		}
		AbstractCall terminalInvocation = fragment.invocationCoveringEntireFragment();
		AbstractCall assertThatInvocation = firstMethodInvocation(fragment, "assertThat");
		return terminalInvocation != null && assertThatInvocation != null &&
				terminalInvocation.getName().equals("hasSize") &&
				terminalInvocation.arguments().size() == 1 &&
				assertThatInvocation.arguments().size() == 1 &&
				matchesAssertJNarrowedSubject(assertThatInvocation.arguments().get(0), narrowing, narrowedSubjects) &&
				String.valueOf(narrowing.terminalArguments.size()).equals(terminalInvocation.arguments().get(0));
	}

	private boolean matchesCollapsedAssertJIndexedElementCheck(AbstractCodeFragment fragment, AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		if(!(narrowing.terminalName.equals("containsExactly") || narrowing.terminalName.equals("containsOnly") ||
				narrowing.terminalName.equals("containsExactlyInAnyOrder"))) {
			return false;
		}
		AbstractCall terminalInvocation = fragment.invocationCoveringEntireFragment();
		AbstractCall assertThatInvocation = firstMethodInvocation(fragment, "assertThat");
		if(terminalInvocation == null || assertThatInvocation == null ||
				!terminalInvocation.getName().equals("isEqualTo") || terminalInvocation.arguments().size() != 1 ||
				assertThatInvocation.arguments().size() != 1) {
			return false;
		}
		String left = assertThatInvocation.arguments().get(0);
		String right = terminalInvocation.arguments().get(0);
		Integer rightIndex = indexedSubjectPosition(right, narrowedSubjects);
		if(rightIndex != null && rightIndex < narrowing.terminalArguments.size() &&
				left.equals(narrowing.terminalArguments.get(rightIndex))) {
			return true;
		}
		Integer leftIndex = indexedSubjectPosition(left, narrowedSubjects);
		return leftIndex != null && leftIndex < narrowing.terminalArguments.size() &&
				right.equals(narrowing.terminalArguments.get(leftIndex));
	}

	private boolean matchesCollapsedAssertJOptionalPresenceCheck(AbstractCodeFragment fragment,
			AdvancedAssertJTypeNarrowing narrowing, Set<String> narrowedSubjects) {
		if(!narrowing.factoryKind.equals("optional")) {
			return false;
		}
		AbstractCall terminalInvocation = fragment.invocationCoveringEntireFragment();
		AbstractCall assertThatInvocation = firstMethodInvocation(fragment, "assertThat");
		return terminalInvocation != null && assertThatInvocation != null &&
				terminalInvocation.getName().equals("isPresent") &&
				assertThatInvocation.arguments().size() == 1 &&
				narrowedSubjects.contains(assertThatInvocation.arguments().get(0));
	}

	private boolean matchesCollapsedAssertJOptionalValueCheck(AbstractCodeFragment fragment,
			AdvancedAssertJTypeNarrowing narrowing, Set<String> narrowedSubjects) {
		if(!narrowing.factoryKind.equals("optional") || !narrowing.terminalName.equals("contains") ||
				narrowing.terminalArguments.size() != 1) {
			return false;
		}
		AbstractCall terminalInvocation = fragment.invocationCoveringEntireFragment();
		AbstractCall assertThatInvocation = firstMethodInvocation(fragment, "assertThat");
		if(terminalInvocation == null || assertThatInvocation == null ||
				!terminalInvocation.getName().equals("isEqualTo") || terminalInvocation.arguments().size() != 1 ||
				assertThatInvocation.arguments().size() != 1) {
			return false;
		}
		String assertThatArgument = assertThatInvocation.arguments().get(0);
		String expectedArgument = narrowing.terminalArguments.get(0);
		for(String narrowedSubject : narrowedSubjects) {
			if(assertThatArgument.equals(narrowedSubject + ".get()") &&
					expectedArgument.equals(terminalInvocation.arguments().get(0))) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesAssertJNarrowedSubject(String expression, AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		if(narrowedSubjects.contains(expression)) {
			return true;
		}
		if(matchesAssertJCastExpression(expression, narrowing, narrowedSubjects)) {
			return true;
		}
		return narrowing.factoryKind.equals("type") && expression.equals(narrowing.baseExpression);
	}

	private boolean matchesAssertJCastExpression(String expression, AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		String castType = castType(expression);
		String strippedExpression = stripCast(expression);
		return castType != null && strippedExpression != null &&
				narrowedSubjects.contains(strippedExpression) &&
				matchesAssertJCastType(castType, narrowing);
	}

	private boolean matchesAssertJCastType(String castType, AdvancedAssertJTypeNarrowing narrowing) {
		if(castType == null) {
			return false;
		}
		if(narrowing.factoryKind.equals("optional")) {
			return castType.startsWith("Optional");
		}
		if(narrowing.factoryKind.equals("LIST")) {
			return castType.endsWith("List") || castType.equals("List");
		}
		return castType.equals(narrowing.narrowedType);
	}

	private boolean matchesAssertJTypeCheckExpression(String expression, AdvancedAssertJTypeNarrowing narrowing,
			Set<String> narrowedSubjects) {
		if(expression == null || !expression.contains(" instanceof ")) {
			return false;
		}
		String before = expression.substring(0, expression.indexOf(" instanceof ")).trim();
		String after = expression.substring(expression.indexOf(" instanceof ") + " instanceof ".length()).trim();
		return narrowedSubjects.contains(before) && after.equals(narrowing.narrowedType);
	}

	private Integer indexedSubjectPosition(String expression, Set<String> narrowedSubjects) {
		if(expression == null || !expression.endsWith("]")) {
			return null;
		}
		int openBracket = expression.lastIndexOf('[');
		if(openBracket < 0) {
			return null;
		}
		String subject = expression.substring(0, openBracket);
		String index = expression.substring(openBracket + 1, expression.length() - 1);
		if(!narrowedSubjects.contains(subject)) {
			return null;
		}
		try {
			return Integer.parseInt(index);
		}
		catch(NumberFormatException e) {
			return null;
		}
	}

	private List<String> arrayInitializerElements(String expression) {
		if(expression == null) {
			return Collections.emptyList();
		}
		int openBrace = expression.indexOf('{');
		int closeBrace = expression.lastIndexOf('}');
		if(!expression.startsWith("new ") || openBrace < 0 || closeBrace <= openBrace) {
			return Collections.emptyList();
		}
		return splitTopLevelArguments(expression.substring(openBrace + 1, closeBrace));
	}

	private List<String> splitTopLevelArguments(String text) {
		if(text == null || text.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> arguments = new ArrayList<>();
		int parenthesisDepth = 0;
		int bracketDepth = 0;
		int braceDepth = 0;
		int argumentStart = 0;
		for(int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if(c == '(') {
				parenthesisDepth++;
			}
			else if(c == ')') {
				parenthesisDepth--;
			}
			else if(c == '[') {
				bracketDepth++;
			}
			else if(c == ']') {
				bracketDepth--;
			}
			else if(c == '{') {
				braceDepth++;
			}
			else if(c == '}') {
				braceDepth--;
			}
			else if(c == ',' && parenthesisDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
				arguments.add(text.substring(argumentStart, i).trim());
				argumentStart = i + 1;
			}
		}
		String lastArgument = text.substring(argumentStart).trim();
		if(!lastArgument.isEmpty()) {
			arguments.add(lastArgument);
		}
		return arguments;
	}

	private AbstractCall firstMethodInvocation(AbstractCodeFragment fragment, String name) {
		for(AbstractCall invocation : fragment.getMethodInvocations()) {
			if(invocation.getName().equals(name)) {
				return invocation;
			}
		}
		return null;
	}

	private String stripCast(String castExpression) {
		if(castExpression == null) {
			return null;
		}
		String trimmed = castExpression.trim();
		if(trimmed.startsWith("(")) {
			int closingParenthesis = trimmed.indexOf(')');
			if(closingParenthesis > 0 && closingParenthesis < trimmed.length() - 1) {
				return trimmed.substring(closingParenthesis + 1).trim();
			}
		}
		return trimmed;
	}

	private String castType(String castExpression) {
		if(castExpression == null) {
			return null;
		}
		String trimmed = castExpression.trim();
		if(trimmed.startsWith("(")) {
			int closingParenthesis = trimmed.indexOf(')');
			if(closingParenthesis > 1) {
				return trimmed.substring(1, closingParenthesis).trim();
			}
		}
		return null;
	}

	private static final class AdvancedAssertJTypeNarrowing {
		private final String baseExpression;
		private final String asInstanceOfExpression;
		private final String factoryKind;
		private final String narrowedType;
		private final String terminalName;
		private final List<String> terminalArguments;

		private AdvancedAssertJTypeNarrowing(String baseExpression, String asInstanceOfExpression,
				String factoryKind, String narrowedType, String terminalName, List<String> terminalArguments) {
			this.baseExpression = baseExpression;
			this.asInstanceOfExpression = asInstanceOfExpression;
			this.factoryKind = factoryKind;
			this.narrowedType = narrowedType;
			this.terminalName = terminalName;
			this.terminalArguments = terminalArguments;
		}
	}

	private static final class AdvancedAssertJInlineCandidate {
		private final VariableDeclaration declaration;
		private final List<AbstractCodeFragment> referenceStatements = new ArrayList<>();

		private AdvancedAssertJInlineCandidate(VariableDeclaration declaration) {
			this.declaration = declaration;
		}
	}
}
