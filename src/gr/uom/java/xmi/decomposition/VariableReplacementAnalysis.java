package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.decomposition.replacement.ConsistentReplacementDetector;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.VariableDeclarationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.CandidateAttributeRefactoring;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;

public class VariableReplacementAnalysis {
	private List<AbstractCodeMapping> mappings;
	private UMLOperation operation1;
	private UMLOperation operation2;
	private List<UMLOperationBodyMapper> additionalMappers;
	private Set<Refactoring> refactorings;
	private Set<RenameVariableRefactoring> variableRenames = new LinkedHashSet<RenameVariableRefactoring>();
	private Set<CandidateAttributeRefactoring> candidateAttributeRenames = new LinkedHashSet<CandidateAttributeRefactoring>();

	public VariableReplacementAnalysis(List<AbstractCodeMapping> mappings, UMLOperation operation1, UMLOperation operation2,
			List<UMLOperationBodyMapper> additionalMappers, Set<Refactoring> refactorings) {
		this.mappings = mappings;
		this.operation1 = operation1;
		this.operation2 = operation2;
		this.additionalMappers = additionalMappers;
		this.refactorings = refactorings;
		findConsistentVariableRenames();
	}

	public Set<RenameVariableRefactoring> getVariableRenames() {
		return variableRenames;
	}

	public Set<CandidateAttributeRefactoring> getCandidateAttributeRenames() {
		return candidateAttributeRenames;
	}

	private void findConsistentVariableRenames() {
		Map<Replacement, List<AbstractCodeMapping>> variableDeclarationReplacementOccurrenceMap =	getVariableDeclarationReplacementOccurrenceMap();
		Set<Replacement> allConsistentVariableDeclarationRenames = allConsistentRenames(variableDeclarationReplacementOccurrenceMap);
		Map<Replacement, List<AbstractCodeMapping>> replacementOccurrenceMap = getReplacementOccurrenceMap(ReplacementType.VARIABLE_NAME);
		Set<Replacement> allConsistentRenames = allConsistentRenames(replacementOccurrenceMap);
		Map<Replacement, List<AbstractCodeMapping>> finalConsistentRenames = new LinkedHashMap<Replacement, List<AbstractCodeMapping>>();
		for(Replacement replacement : allConsistentRenames) {
			VariableDeclaration v1 = getVariableDeclaration1(replacement);
			VariableDeclaration v2 = getVariableDeclaration2(replacement);
			List<AbstractCodeMapping> list = replacementOccurrenceMap.get(replacement);
			if((list.size() > 1 && consistencyCheck(v1, v2)) ||
					potentialParameterRename(replacement) ||
					v1 == null || v2 == null ||
					(list.size() == 1 && replacementInLocalVariableDeclaration(replacement))) {
				finalConsistentRenames.put(replacement, list);
			}
			if(v1 != null && !v1.isParameter() && v2 != null && v2.isParameter() && consistencyCheck(v1, v2) &&
					!operation1.getParameterNameList().contains(v2.getVariableName())) {
				finalConsistentRenames.put(replacement, list);
			}
		}
		for(Replacement replacement : allConsistentVariableDeclarationRenames) {
			VariableDeclarationReplacement vdReplacement = (VariableDeclarationReplacement)replacement;
			Replacement variableNameReplacement = vdReplacement.getVariableNameReplacement();
			List<AbstractCodeMapping> list = variableDeclarationReplacementOccurrenceMap.get(vdReplacement);
			if((list.size() > 1 && consistencyCheck(vdReplacement.getVariableDeclaration1(), vdReplacement.getVariableDeclaration2())) ||
					(list.size() == 1 && replacementInLocalVariableDeclaration(variableNameReplacement))) {
				finalConsistentRenames.put(variableNameReplacement, list);
			}
		}
		for(Replacement replacement : finalConsistentRenames.keySet()) {
			VariableDeclaration v1 = getVariableDeclaration1(replacement);
			VariableDeclaration v2 = getVariableDeclaration2(replacement);
			if(v1 != null && v2 != null) {
				RenameVariableRefactoring ref = new RenameVariableRefactoring(v1, v2, operation1, operation2, finalConsistentRenames.get(replacement));
				if(!existsConflictingExtractVariableRefactoring(ref)) {
					variableRenames.add(ref);
				}
			}
			else if(!PrefixSuffixUtils.normalize(replacement.getBefore()).equals(PrefixSuffixUtils.normalize(replacement.getAfter())) &&
					(!operation1.getAllVariables().contains(replacement.getAfter()) || cyclicRename(finalConsistentRenames.keySet(), replacement)) &&
					(!operation2.getAllVariables().contains(replacement.getBefore()) || cyclicRename(finalConsistentRenames.keySet(), replacement))) {
				CandidateAttributeRefactoring candidate = new CandidateAttributeRefactoring(
						replacement.getBefore(), replacement.getAfter(), operation1, operation2,
						replacementOccurrenceMap.get(replacement));
				if(v1 != null)
					candidate.setOriginalVariableDeclaration(v1);
				if(v2 != null)
					candidate.setRenamedVariableDeclaration(v2);
				this.candidateAttributeRenames.add(candidate);
			}
		}
	}

	private Map<Replacement, List<AbstractCodeMapping>> getReplacementOccurrenceMap(ReplacementType type) {
		Map<Replacement, List<AbstractCodeMapping>> map = new LinkedHashMap<Replacement, List<AbstractCodeMapping>>();
		for(AbstractCodeMapping mapping : mappings) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(type) && !returnVariableMapping(mapping, replacement) &&
						!containsMethodInvocationReplacementWithDifferentExpressionNameAndArguments(mapping.getReplacements()) &&
						replacementNotInsideMethodSignatureOfAnonymousClass(mapping, replacement)) {
					if(map.containsKey(replacement)) {
						map.get(replacement).add(mapping);
					}
					else {
						List<AbstractCodeMapping> list = new ArrayList<AbstractCodeMapping>();
						list.add(mapping);
						map.put(replacement, list);
					}
				}
			}
		}
		return map;
	}

	private Map<Replacement, List<AbstractCodeMapping>> getVariableDeclarationReplacementOccurrenceMap() {
		Map<Replacement, List<AbstractCodeMapping>> map = new LinkedHashMap<Replacement, List<AbstractCodeMapping>>();
		for(AbstractCodeMapping mapping : mappings) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.VARIABLE_NAME) && !returnVariableMapping(mapping, replacement) &&
						!containsMethodInvocationReplacementWithDifferentExpressionNameAndArguments(mapping.getReplacements()) &&
						replacementNotInsideMethodSignatureOfAnonymousClass(mapping, replacement)) {
					VariableDeclaration v1 = getVariableDeclaration1(replacement);
					VariableDeclaration v2 = getVariableDeclaration2(replacement);
					if(v1 != null && v2 != null) {
						VariableDeclarationReplacement r = new VariableDeclarationReplacement(v1, v2);
						if(map.containsKey(r)) {
							map.get(r).add(mapping);
						}
						else {
							List<AbstractCodeMapping> list = new ArrayList<AbstractCodeMapping>();
							list.add(mapping);
							map.put(r, list);
						}
					}
				}
			}
		}
		return map;
	}

	private static boolean returnVariableMapping(AbstractCodeMapping mapping, Replacement replacement) {
		return mapping.getFragment1().getString().equals("return " + replacement.getBefore() + ";\n") &&
				mapping.getFragment2().getString().equals("return " + replacement.getAfter() + ";\n");
	}

	private boolean containsMethodInvocationReplacementWithDifferentExpressionNameAndArguments(Set<Replacement> replacements) {
		for(Replacement replacement : replacements) {
			if(replacement instanceof MethodInvocationReplacement) {
				MethodInvocationReplacement r = (MethodInvocationReplacement)replacement;
				if(r.differentExpressionNameAndArguments())
					return true;
			}
		}
		return false;
	}

	private boolean replacementNotInsideMethodSignatureOfAnonymousClass(AbstractCodeMapping mapping, Replacement replacement) {
		AbstractCodeFragment fragment1 = mapping.getFragment1();
		AbstractCodeFragment fragment2 = mapping.getFragment2();
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations1 = fragment1.getAnonymousClassDeclarations();
		List<AnonymousClassDeclarationObject> anonymousClassDeclarations2 = fragment2.getAnonymousClassDeclarations();
		if(anonymousClassDeclarations1.size() > 0 && anonymousClassDeclarations2.size() > 0) {
			boolean replacementBeforeNotFoundInMethodSignature = false;
			String[] lines1 = fragment1.getString().split("\\n");
			for(String line : lines1) {
				line = prepareLine(line);
				if(!Visitor.METHOD_SIGNATURE_PATTERN.matcher(line).matches() &&
						ReplacementUtil.contains(line, replacement.getBefore())) {
					replacementBeforeNotFoundInMethodSignature = true;
					break;
				}
			}
			boolean replacementAfterNotFoundInMethodSignature = false;
			String[] lines2 = fragment2.getString().split("\\n");
			for(String line : lines2) {
				line = prepareLine(line);
				if(!Visitor.METHOD_SIGNATURE_PATTERN.matcher(line).matches() &&
						ReplacementUtil.contains(line, replacement.getAfter())) {
					replacementAfterNotFoundInMethodSignature = true;
					break;
				}
			}
			return replacementBeforeNotFoundInMethodSignature && replacementAfterNotFoundInMethodSignature;
		}
		return true;
	}

	public static String prepareLine(String line) {
		line = line.trim();
		if(line.startsWith("@Nullable")) {
			line = line.substring(9, line.length());
			line = line.trim();
		}
		if(line.startsWith("@Override")) {
			line = line.substring(9, line.length());
			line = line.trim();
		}
		if(line.contains("throws ")) {
			line = line.substring(0, line.indexOf("throws "));
		}
		return line;
	}

	private static boolean cyclicRename(Set<Replacement> finalConsistentRenames, Replacement replacement) {
		for(Replacement r : finalConsistentRenames) {
			if(replacement.getAfter().equals(r.getBefore()))
				return true;
			if(replacement.getBefore().equals(r.getAfter()))
				return true;
		}
		return false;
	}

	private Set<Replacement> allConsistentRenames(Map<Replacement, List<AbstractCodeMapping>> replacementOccurrenceMap) {
		Set<Replacement> renames = replacementOccurrenceMap.keySet();
		Set<Replacement> allConsistentRenames = new LinkedHashSet<Replacement>();
		Set<Replacement> allInconsistentRenames = new LinkedHashSet<Replacement>();
		ConsistentReplacementDetector.updateRenames(allConsistentRenames, allInconsistentRenames, renames);
		allConsistentRenames.removeAll(allInconsistentRenames);
		return allConsistentRenames;
	}

	private boolean replacementInLocalVariableDeclaration(Replacement replacement) {
		VariableDeclaration v1 = null;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				v1 = mapping.getFragment1().searchVariableDeclaration(replacement.getBefore());
				break;
			}
		}
		VariableDeclaration v2 = null;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				v2 = mapping.getFragment2().searchVariableDeclaration(replacement.getAfter());
				break;
			}
		}
		return v1 != null && v2 != null &&
				v1.equalVariableDeclarationType(v2) &&
				!containsVariableDeclarationWithName(operation1.getAllVariableDeclarations(), replacement.getAfter()) &&
				!containsVariableDeclarationWithName(operation2.getAllVariableDeclarations(), replacement.getBefore()) &&
				consistencyCheck(v1, v2);
	}

	private boolean consistencyCheck(VariableDeclaration v1, VariableDeclaration v2) {
		return !variableAppearsInExtractedMethod(v1, v2) &&
				!inconsistentVariableMapping(v1, v2);
	}

	private boolean inconsistentVariableMapping(VariableDeclaration v1, VariableDeclaration v2) {
		if(v1 != null && v2 != null) {
			for(AbstractCodeMapping mapping : mappings) {
				List<VariableDeclaration> variableDeclarations1 = mapping.getFragment1().getVariableDeclarations();
				List<VariableDeclaration> variableDeclarations2 = mapping.getFragment2().getVariableDeclarations();
				if(variableDeclarations1.contains(v1) &&
						variableDeclarations2.size() > 0 &&
						!variableDeclarations2.contains(v2)) {
					return true;
				}
				if(variableDeclarations2.contains(v2) &&
						variableDeclarations1.size() > 0 &&
						!variableDeclarations1.contains(v1)) {
					return true;
				}
				if(mapping.isExact() && (bothFragmentsUseVariable(v1, mapping) || bothFragmentsUseVariable(v2, mapping))) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean bothFragmentsUseVariable(VariableDeclaration v1, AbstractCodeMapping mapping) {
		return mapping.getFragment1().getVariables().contains(v1.getVariableName()) &&
				mapping.getFragment2().getVariables().contains(v1.getVariableName());
	}

	private static boolean containsVariableDeclarationWithName(List<VariableDeclaration> variableDeclarations, String variableName) {
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(variableName)) {
				return true;
			}
		}
		return false;
	}

	private VariableDeclaration getVariableDeclaration1(Replacement replacement) {
		VariableDeclaration v1 = null;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				v1 = mapping.getFragment1().searchVariableDeclaration(replacement.getBefore());
				break;
			}
		}
		if(v1 == null) {
			for(UMLParameter parameter : operation1.getParameters()) {
				VariableDeclaration vd = parameter.getVariableDeclaration();
				if(vd != null && vd.getVariableName().equals(replacement.getBefore())) {
					v1 = vd;
					break;
				}
			}
		}
		return v1;
	}

	private VariableDeclaration getVariableDeclaration2(Replacement replacement) {
		VariableDeclaration v2 = null;
		for(AbstractCodeMapping mapping : mappings) {
			if(mapping.getReplacements().contains(replacement)) {
				v2 = mapping.getFragment2().searchVariableDeclaration(replacement.getAfter());
				break;
			}
		}
		if(v2 == null) {
			for(UMLParameter parameter : operation2.getParameters()) {
				VariableDeclaration vd = parameter.getVariableDeclaration();
				if(vd != null && vd.getVariableName().equals(replacement.getAfter())) {
					v2 = vd;
					break;
				}
			}
		}
		return v2;
	}

	private boolean variableAppearsInExtractedMethod(VariableDeclaration v1, VariableDeclaration v2) {
		if(v1 != null) {
			for(UMLOperationBodyMapper mapper : additionalMappers) {
				for(AbstractCodeMapping mapping : mapper.getMappings()) {
					if(mapping.getFragment1().getVariableDeclarations().contains(v1)) {
						if(v2 != null && v2.getInitializer() != null) {
							UMLOperation extractedMethod = mapper.getOperation2();
							Map<String, OperationInvocation> methodInvocationMap = v2.getInitializer().getMethodInvocationMap();
							for(OperationInvocation invocation : methodInvocationMap.values()) {
								if(invocation.matchesOperation(extractedMethod, operation2.variableTypeMap(), null)) {
									return false;
								}
								else {
									//check if the extracted method is called in the initializer of a variable used in the initializer of v2
									List<String> initializerVariables = v2.getInitializer().getVariables();
									for(String variable : initializerVariables) {
										for(VariableDeclaration declaration : operation2.getAllVariableDeclarations()) {
											if(declaration.getVariableName().equals(variable) && declaration.getInitializer() != null) {
												Map<String, OperationInvocation> methodInvocationMap2 = declaration.getInitializer().getMethodInvocationMap();
												for(OperationInvocation invocation2 : methodInvocationMap2.values()) {
													if(invocation2.matchesOperation(extractedMethod, operation2.variableTypeMap(), null)) {
														return false;
													}
												}
											}
										}
									}
								}
							}
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean existsConflictingExtractVariableRefactoring(RenameVariableRefactoring ref) {
		for(Refactoring refactoring : refactorings) {
			if(refactoring instanceof ExtractVariableRefactoring) {
				ExtractVariableRefactoring extractVariableRef = (ExtractVariableRefactoring)refactoring;
				if(extractVariableRef.getVariableDeclaration().equals(ref.getRenamedVariable()) &&
						extractVariableRef.getOperation().equals(ref.getOperationAfter())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean potentialParameterRename(Replacement replacement) {
		int index1 = operation1.getParameterNameList().indexOf(replacement.getBefore());
		int index2 = operation2.getParameterNameList().indexOf(replacement.getAfter());
		return index1 >= 0 && index1 == index2;
	}
}
