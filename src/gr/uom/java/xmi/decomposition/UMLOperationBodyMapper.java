package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.StringDistance;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UMLOperationBodyMapper implements Comparable<UMLOperationBodyMapper> {
	private UMLOperation operation1;
	private UMLOperation operation2;
	private boolean isInitialized = true;
	private List<AbstractCodeMapping> mappings;
	private List<StatementObject> nonMappedLeavesT1;
	private List<StatementObject> nonMappedLeavesT2;
	private List<CompositeStatementObject> nonMappedInnerNodesT1;
	private List<CompositeStatementObject> nonMappedInnerNodesT2;
	
	public UMLOperationBodyMapper(UMLOperation operation1, UMLOperation operation2) {
		this.operation1 = operation1;
		this.operation2 = operation2;
		this.mappings = new ArrayList<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<StatementObject>();
		this.nonMappedLeavesT2 = new ArrayList<StatementObject>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		this.isInitialized = false;
	}

	private void initialize() {
		if (this.isInitialized) {
			return;
		}
		this.isInitialized = true;
		OperationBody body1 = operation1.getBody();
		OperationBody body2 = operation2.getBody();
		if(body1 != null && body2 != null) {
			CompositeStatementObject composite1 = body1.getCompositeStatement();
			CompositeStatementObject composite2 = body2.getCompositeStatement();
			List<StatementObject> leaves1 = composite1.getLeaves();
			List<StatementObject> leaves2 = composite2.getLeaves();
			
			processLeaves(leaves1, leaves2);
			
			List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
			innerNodes1.remove(composite1);
			List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
			innerNodes2.remove(composite2);
			
			processInnerNodes(innerNodes1, innerNodes2);
			
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
		}
	}
	
	public UMLOperationBodyMapper(UMLOperationBodyMapper operationBodyMapper, UMLOperation addedOperation) {
		this.operation1 = operationBodyMapper.operation1;
		this.operation2 = addedOperation;
		this.mappings = new ArrayList<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<StatementObject>();
		this.nonMappedLeavesT2 = new ArrayList<StatementObject>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		
		OperationBody addedOperationBody = addedOperation.getBody();
		if(addedOperationBody != null) {
			CompositeStatementObject composite2 = addedOperationBody.getCompositeStatement();
			List<StatementObject> leaves1 = operationBodyMapper.getNonMappedLeavesT1();
			//adding leaves that were mapped with replacements
			Set<StatementObject> addedLeaves1 = new LinkedHashSet<StatementObject>();
			for(AbstractCodeMapping mapping : operationBodyMapper.getMappings()) {
				if(!mapping.getReplacements().isEmpty()) {
					AbstractCodeFragment fragment = mapping.getFragment1();
					if(fragment instanceof StatementObject) {
						StatementObject statement = (StatementObject)fragment;
						if(!leaves1.contains(statement)) {
							leaves1.add(statement);
							addedLeaves1.add(statement);
						}
					}
				}
			}
			List<StatementObject> leaves2 = composite2.getLeaves();
			//compare leaves from T1 with leaves from T2
			processLeaves(leaves1, leaves2);

			List<CompositeStatementObject> innerNodes1 = operationBodyMapper.getNonMappedInnerNodesT1();
			List<CompositeStatementObject> innerNodes2 = composite2.getInnerNodes();
			innerNodes2.remove(composite2);
			//compare inner nodes from T1 with inner nodes from T2
			processInnerNodes(innerNodes1, innerNodes2);
			
			//match expressions in inner nodes from T1 with leaves from T2
			List<AbstractExpression> expressionsT1 = new ArrayList<AbstractExpression>();
			for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT1()) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expressionsT1.add(expression);
				}
			}
			processLeaves(expressionsT1, leaves2);
			// TODO remove non-mapped inner nodes from T1 corresponding to mapped expressions
			
			operationBodyMapper.mappings.addAll(this.mappings);
			//remove the leaves that were mapped with replacement, if they are not mapped again for a second time
			leaves1.removeAll(addedLeaves1);
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
		}
	}

	public UMLOperationBodyMapper(UMLOperation removedOperation, UMLOperationBodyMapper operationBodyMapper, Map<String, String> parameterToArgumentMap) {
		this.operation1 = removedOperation;
		this.operation2 = operationBodyMapper.operation2;
		this.mappings = new ArrayList<AbstractCodeMapping>();
		this.nonMappedLeavesT1 = new ArrayList<StatementObject>();
		this.nonMappedLeavesT2 = new ArrayList<StatementObject>();
		this.nonMappedInnerNodesT1 = new ArrayList<CompositeStatementObject>();
		this.nonMappedInnerNodesT2 = new ArrayList<CompositeStatementObject>();
		
		OperationBody removedOperationBody = removedOperation.getBody();
		if(removedOperationBody != null) {
			CompositeStatementObject composite1 = removedOperationBody.getCompositeStatement();
			List<StatementObject> leaves1 = composite1.getLeaves();
			List<StatementObject> leaves2 = operationBodyMapper.getNonMappedLeavesT2();
			if(!parameterToArgumentMap.isEmpty()) {
				for(StatementObject leave1 : leaves1) {
					leave1.replaceParametersWithArguments(parameterToArgumentMap);
				}
			}
			//compare leaves from T1 with leaves from T2
			processLeaves(leaves1, leaves2);
			
			List<CompositeStatementObject> innerNodes1 = composite1.getInnerNodes();
			innerNodes1.remove(composite1);
			List<CompositeStatementObject> innerNodes2 = operationBodyMapper.getNonMappedInnerNodesT2();
			
			//compare inner nodes from T1 with inner nodes from T2
			processInnerNodes(innerNodes1, innerNodes2);
			
			//match expressions in inner nodes from T2 with leaves from T1
			List<AbstractExpression> expressionsT2 = new ArrayList<AbstractExpression>();
			for(CompositeStatementObject composite : operationBodyMapper.getNonMappedInnerNodesT2()) {
				for(AbstractExpression expression : composite.getExpressions()) {
					expressionsT2.add(expression);
				}
			}
			processLeaves(leaves1, expressionsT2);
			
			operationBodyMapper.mappings.addAll(this.mappings);
			nonMappedLeavesT1.addAll(leaves1);
			nonMappedLeavesT2.addAll(leaves2);
			nonMappedInnerNodesT1.addAll(innerNodes1);
			nonMappedInnerNodesT2.addAll(innerNodes2);
		}
	}

	public UMLOperation getOperation1() {
		return operation1;
	}

	public UMLOperation getOperation2() {
		return operation2;
	}

	public List<AbstractCodeMapping> getMappings() {
		initialize();
		return mappings;
	}

	public List<StatementObject> getNonMappedLeavesT1() {
		initialize();
		return nonMappedLeavesT1;
	}

	public List<StatementObject> getNonMappedLeavesT2() {
		initialize();
		return nonMappedLeavesT2;
	}

	public List<CompositeStatementObject> getNonMappedInnerNodesT1() {
		initialize();
		return nonMappedInnerNodesT1;
	}

	public List<CompositeStatementObject> getNonMappedInnerNodesT2() {
		initialize();
		return nonMappedInnerNodesT2;
	}

	public int nonMappedElementsT1() {
		return getNonMappedLeavesT1().size() + getNonMappedInnerNodesT1().size();
	}

	public int nonMappedElementsT2() {
		return getNonMappedLeavesT2().size() + getNonMappedInnerNodesT2().size();
	}

	public int exactMatches() {
		int count = 0;
		for(AbstractCodeMapping mapping : getMappings()) {
			if(mapping.isExact())
				count++;
		}
		return count;
	}

	public Set<Replacement> getReplacements() {
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		for(AbstractCodeMapping mapping : getMappings()) {
			replacements.addAll(mapping.getReplacements());
		}
		return replacements;
	}

	public Set<VariableReplacementWithMethodInvocation> getVariableReplacementsWithMethodInvocation() {
		Set<VariableReplacementWithMethodInvocation> replacements = new LinkedHashSet<VariableReplacementWithMethodInvocation>();
		for(AbstractCodeMapping mapping : getMappings()) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement instanceof VariableReplacementWithMethodInvocation) {
					replacements.add((VariableReplacementWithMethodInvocation)replacement);
				}
			}
		}
		return replacements;
	}

	public Set<MethodInvocationReplacement> getMethodInvocationReplacements() {
		Set<MethodInvocationReplacement> replacements = new LinkedHashSet<MethodInvocationReplacement>();
		for(AbstractCodeMapping mapping : getMappings()) {
			for(Replacement replacement : mapping.getReplacements()) {
				if(replacement instanceof MethodInvocationReplacement) {
					replacements.add((MethodInvocationReplacement)replacement);
				}
			}
		}
		return replacements;
	}

	public void processInnerNodes(List<CompositeStatementObject> innerNodes1,
			List<CompositeStatementObject> innerNodes2) {
		//exact string+depth matching - inner nodes
		for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
			CompositeStatementObject statement1 = innerNodeIterator1.next();
			TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				double score = compositeChildMatchingScore(statement1, statement2);
				if(statement1.getString().equals(statement2.getString()) && statement1.getDepth() == statement2.getDepth() &&
						(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
					CompositeStatementObjectMapping mapping = new CompositeStatementObjectMapping(statement1, statement2, operation1, operation2, score);
					mappingSet.add(mapping);
				}
			}
			if(!mappingSet.isEmpty()) {
				CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				innerNodes2.remove(minStatementMapping.getFragment2());
				innerNodeIterator1.remove();
			}
		}
		
		//exact string matching - inner nodes - finds moves to another level
		for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
			CompositeStatementObject statement1 = innerNodeIterator1.next();
			TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				double score = compositeChildMatchingScore(statement1, statement2);
				if(statement1.getString().equals(statement2.getString()) &&
						(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
					CompositeStatementObjectMapping mapping = new CompositeStatementObjectMapping(statement1, statement2, operation1, operation2, score);
					mappingSet.add(mapping);
				}
			}
			if(!mappingSet.isEmpty()) {
				CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				innerNodes2.remove(minStatementMapping.getFragment2());
				innerNodeIterator1.remove();
			}
		}
		
		// exact matching - inner nodes - with variable renames
		for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
			CompositeStatementObject statement1 = innerNodeIterator1.next();
			TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				Set<Replacement> replacements = findReplacementsWithExactMatching(statement1, statement2);
				
				double score = compositeChildMatchingScore(statement1, statement2);
				if(replacements != null &&
						(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
					CompositeStatementObjectMapping mapping = new CompositeStatementObjectMapping(statement1, statement2, operation1, operation2, score);
					mapping.addReplacements(replacements);
					mappingSet.add(mapping);
				}
			}
			if(!mappingSet.isEmpty()) {
				CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				innerNodes2.remove(minStatementMapping.getFragment2());
				innerNodeIterator1.remove();
			}
		}
		
		//Lcs matching - inner nodes
		for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
			CompositeStatementObject statement1 = innerNodeIterator1.next();
			TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				List<String> lcs = StringDistance.commonSubstrings(statement1.getString(), statement2.getString());
				double score = compositeChildMatchingScore(statement1, statement2);
				if(lcs.size() == 1 &&
						(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
					double similarity = (double)lcs.get(0).length()/(double)Math.max(statement1.getString().length(), statement2.getString().length());
					if(similarity > 0.5) {
						CompositeStatementObjectMapping mapping = new CompositeStatementObjectMapping(statement1, statement2, operation1, operation2, score);
						mappingSet.add(mapping);
					}
				}
			}
			if(!mappingSet.isEmpty()) {
				CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				innerNodes2.remove(minStatementMapping.getFragment2());
				innerNodeIterator1.remove();
			}
		}
		
		//string distance matching - inner nodes
		for(ListIterator<CompositeStatementObject> innerNodeIterator1 = innerNodes1.listIterator(); innerNodeIterator1.hasNext();) {
			CompositeStatementObject statement1 = innerNodeIterator1.next();
			TreeSet<CompositeStatementObjectMapping> mappingSet = new TreeSet<CompositeStatementObjectMapping>();
			for(ListIterator<CompositeStatementObject> innerNodeIterator2 = innerNodes2.listIterator(); innerNodeIterator2.hasNext();) {
				CompositeStatementObject statement2 = innerNodeIterator2.next();
				double score = compositeChildMatchingScore(statement1, statement2);
				String s1 = statement1.getString().toLowerCase();
				String s2 = statement2.getString().toLowerCase();
				double distance = (double)StringDistance.editDistance(s1, s2)/(double)Math.max(s1.length(), s2.length());
				if(distance < 0.4 &&
						(score > 0 || Math.max(statement1.getStatements().size(), statement2.getStatements().size()) == 0)) {
					CompositeStatementObjectMapping mapping = new CompositeStatementObjectMapping(statement1, statement2, operation1, operation2, score);
					mappingSet.add(mapping);
				}
			}
			if(!mappingSet.isEmpty()) {
				CompositeStatementObjectMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				innerNodes2.remove(minStatementMapping.getFragment2());
				innerNodeIterator1.remove();
			}
		}
	}

	public void processLeaves(List<? extends AbstractCodeFragment> leaves1, List<? extends AbstractCodeFragment> leaves2) {
		//exact string+depth matching - leaf nodes
		for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
			AbstractCodeFragment leaf1 = leafIterator1.next();
			TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				if(leaf1.equalFragment(leaf2) && leaf1.getDepth() == leaf2.getDepth()) {
					LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
					mappingSet.add(mapping);
				}
			}
			if(!mappingSet.isEmpty()) {
				LeafMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				leaves2.remove(minStatementMapping.getFragment2());
				leafIterator1.remove();
			}
		}
		
		//exact string matching - leaf nodes - finds moves to another level
		for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
			AbstractCodeFragment leaf1 = leafIterator1.next();
			TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				if(leaf1.equalFragment(leaf2)) {
					LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
					mappingSet.add(mapping);
				}
			}
			if(!mappingSet.isEmpty()) {
				LeafMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				leaves2.remove(minStatementMapping.getFragment2());
				leafIterator1.remove();
			}
		}
		
		// exact matching with variable renames
		for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
			AbstractCodeFragment leaf1 = leafIterator1.next();
			TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				
				Set<Replacement> replacements = findReplacementsWithExactMatching(leaf1, leaf2);
				if (replacements != null) {
					LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
					mapping.addReplacements(replacements);
					mappingSet.add(mapping);
				}
			}
			if(!mappingSet.isEmpty()) {
				LeafMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				leaves2.remove(minStatementMapping.getFragment2());
				leafIterator1.remove();
			}
		}
		
		//Lcs matching - leaf nodes
		for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
			AbstractCodeFragment leaf1 = leafIterator1.next();
			TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				List<String> lcs = StringDistance.commonSubstrings(leaf1.getString(), leaf2.getString());
				if(lcs.size() == 1) {
					double similarity = (double)lcs.get(0).length()/(double)Math.max(leaf1.getString().length(), leaf2.getString().length());
					if(similarity > 0.5) {
						LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
						mappingSet.add(mapping);
					}
				}
			}
			if(!mappingSet.isEmpty()) {
				LeafMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				leaves2.remove(minStatementMapping.getFragment2());
				leafIterator1.remove();
			}
		}
		
		//string distance matching - leaf nodes
		for(ListIterator<? extends AbstractCodeFragment> leafIterator1 = leaves1.listIterator(); leafIterator1.hasNext();) {
			AbstractCodeFragment leaf1 = leafIterator1.next();
			TreeSet<LeafMapping> mappingSet = new TreeSet<LeafMapping>();
			for(ListIterator<? extends AbstractCodeFragment> leafIterator2 = leaves2.listIterator(); leafIterator2.hasNext();) {
				AbstractCodeFragment leaf2 = leafIterator2.next();
				String s1 = leaf1.getString().toLowerCase();
				String s2 = leaf2.getString().toLowerCase();
				double distance = (double)StringDistance.editDistance(s1, s2)/(double)Math.max(s1.length(), s2.length());
				if(distance < 0.4) {
					LeafMapping mapping = new LeafMapping(leaf1, leaf2, operation1, operation2);
					mappingSet.add(mapping);
				}
			}
			if(!mappingSet.isEmpty()) {
				LeafMapping minStatementMapping = mappingSet.first();
				mappings.add(minStatementMapping);
				leaves2.remove(minStatementMapping.getFragment2());
				leafIterator1.remove();
			}
		}
	}

	private Set<Replacement> findReplacementsWithExactMatching(AbstractCodeFragment statement1, AbstractCodeFragment statement2) {
		Set<String> variables1 = new LinkedHashSet<String>(statement1.getVariables());
		Set<String> variables2 = new LinkedHashSet<String>(statement2.getVariables());
		Set<String> variableIntersection = new LinkedHashSet<String>(variables1);
		variableIntersection.retainAll(variables2);
		// remove common variables from the two sets
		variables1.removeAll(variableIntersection);
		variables2.removeAll(variableIntersection);
		
		Set<String> methodInvocations1 = new LinkedHashSet<String>(statement1.getMethodInvocationMap().keySet());
		Set<String> methodInvocations2 = new LinkedHashSet<String>(statement2.getMethodInvocationMap().keySet());
		Set<String> methodInvocationIntersection = new LinkedHashSet<String>(methodInvocations1);
		methodInvocationIntersection.retainAll(methodInvocations2);
		// remove common methodInvocations from the two sets
		methodInvocations1.removeAll(methodInvocationIntersection);
		methodInvocations2.removeAll(methodInvocationIntersection);
		
		Set<String> variablesAndMethodInvocations1 = new LinkedHashSet<String>();
		variablesAndMethodInvocations1.addAll(methodInvocations1);
		variablesAndMethodInvocations1.addAll(variables1);
		
		Set<String> variablesAndMethodInvocations2 = new LinkedHashSet<String>();
		variablesAndMethodInvocations2.addAll(methodInvocations2);
		variablesAndMethodInvocations2.addAll(variables2);
		
		int initialDistanceRaw = StringDistance.editDistance(statement1.getString(), statement2.getString());
		//double initialDistance = (double)StringDistance.editDistance(statement1.getString(), statement2.getString())/(double)Math.max(statement1.getString().length(), statement2.getString().length());
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		if (initialDistanceRaw > 0) {
			for(String s11 : variablesAndMethodInvocations1) {
				TreeMap<Double, Replacement> replacementMap = new TreeMap<Double, Replacement>();
				String original = statement1.getString();
				int minDistance = initialDistanceRaw;
				for(String s21 : variablesAndMethodInvocations2) {
					String temp = original.replaceAll(Pattern.quote(s11), Matcher.quoteReplacement(s21));
					int distanceRaw = StringDistance.editDistance(temp, statement2.getString(), minDistance);
					//double distance = (double)StringDistance.editDistance(temp, statement2.getString())/(double)Math.max(temp.length(), statement2.getString().length());
					if(distanceRaw >= 0 && distanceRaw < initialDistanceRaw) {
						minDistance = distanceRaw;
						Replacement replacement1 = null;
						if(variables1.contains(s11) && variables2.contains(s21)) {
							replacement1 = new VariableRename(s11, s21);
						}
						else if(variables1.contains(s11) && methodInvocations2.contains(s21)) {
							replacement1 = new VariableReplacementWithMethodInvocation(s11, s21, statement2.getMethodInvocationMap().get(s21));
						}
						else if(methodInvocations1.contains(s11) && methodInvocations2.contains(s21)) {
							double threshold = (double)Math.max(s11.length(), s21.length()) * 0.4;
							int methodInvocationDistanceRaw = StringDistance.editDistance(s11, s21, (int)Math.ceil(threshold));
							//double methodInvocationDistance = (double)StringDistance.editDistance(s11, s21)/(double)Math.max(s11.length(), s21.length());
							if(methodInvocationDistanceRaw >= 0 && methodInvocationDistanceRaw < threshold)
								replacement1 = new MethodInvocationReplacement(s11, s21, statement1.getMethodInvocationMap().get(s11), statement2.getMethodInvocationMap().get(s21));
						}
						if(replacement1 != null) {
							double distancenormalized = (double)distanceRaw/(double)Math.max(temp.length(), statement2.getString().length());
							replacementMap.put(distancenormalized, replacement1);
						}
						if(distanceRaw == 0) {
							break;
						}
					}
				}
				if(!replacementMap.isEmpty()) {
					replacements.add(replacementMap.firstEntry().getValue());
				}
			}
		}
		
		String s1 = statement1.getString();
		String s2 = statement2.getString();
		for(Replacement replacement : replacements) {
			s1 = s1.replaceAll(Pattern.quote(replacement.getBefore()), Matcher.quoteReplacement(replacement.getAfter()));
		}
		boolean isEqualWithReplacement = s1.equals(s2);
		if(isEqualWithReplacement) {
			return replacements;
		}
		return null;
	}

	private double compositeChildMatchingScore(CompositeStatementObject composite1, CompositeStatementObject composite2) {
		int childrenSize1 = composite1.getStatements().size();
		int childrenSize2 = composite2.getStatements().size();
		
		int mappedChildrenSize = 0;
		for(AbstractCodeMapping mapping : mappings) {
			if(composite1.getStatements().contains(mapping.getFragment1()) && composite2.getStatements().contains(mapping.getFragment2())) {
				mappedChildrenSize++;
			}
		}
		if(mappedChildrenSize == 0) {
			List<StatementObject> leaves1 = composite1.getLeaves();
			List<StatementObject> leaves2 = composite2.getLeaves();
			int leaveSize1 = leaves1.size();
			int leaveSize2 = leaves2.size();
			int mappedLeavesSize = 0;
			for(AbstractCodeMapping mapping : mappings) {
				if(leaves1.contains(mapping.getFragment1()) && leaves2.contains(mapping.getFragment2())) {
					mappedLeavesSize++;
				}
			}
			int max = Math.max(leaveSize1, leaveSize2);
			if(max == 0)
				return 0;
			else
				return (double)mappedLeavesSize/(double)max;
		}
		
		int max = Math.max(childrenSize1, childrenSize2);
		if(max == 0)
			return 0;
		else
			return (double)mappedChildrenSize/(double)max;
	}
	
	public boolean isEmpty() {
		return getNonMappedLeavesT1().isEmpty() && getNonMappedInnerNodesT1().isEmpty() &&
				getNonMappedLeavesT2().isEmpty() && getNonMappedInnerNodesT2().isEmpty();
	}
	
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		if(!isEmpty()) {
//			sb.append("operation ").append(operation1.toString()).append("\n");
//		}
//		if(!nonMappedLeavesT1.isEmpty()) {
//			sb.append("non mapped leaves T1\n");
//			sb.append(nonMappedLeavesT1).append("\n");
//		}
//		if(!nonMappedInnerNodesT1.isEmpty()) {
//			sb.append("non mapped inner nodes T1\n");
//			sb.append(nonMappedInnerNodesT1).append("\n");
//		}
//		
//		if(!nonMappedLeavesT2.isEmpty()) {
//			sb.append("non mapped leaves T2\n");
//			sb.append(nonMappedLeavesT2).append("\n");
//		}
//		if(!nonMappedInnerNodesT2.isEmpty()) {
//			sb.append("non mapped inner nodes T2\n");
//			sb.append(nonMappedInnerNodesT2).append("\n");
//		}
//		return sb.toString();
//	}

	@Override
	public int compareTo(UMLOperationBodyMapper operationBodyMapper) {
		int thisExactMatches = this.exactMatches();
		int otherExactMateches = operationBodyMapper.exactMatches();
		if(thisExactMatches != otherExactMateches)
			return -Integer.compare(thisExactMatches, otherExactMateches);
		return this.operation1.toString().compareTo(operationBodyMapper.operation1.toString());
	}
}
