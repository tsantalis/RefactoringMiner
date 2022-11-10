package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall.StatementCoverageType;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper.ReplacementInfo;
import gr.uom.java.xmi.decomposition.replacement.AddVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.IntersectionReplacement;
import gr.uom.java.xmi.decomposition.replacement.MergeVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.SplitVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.SwapArgumentReplacement;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation.Direction;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLOperationDiff;
import gr.uom.java.xmi.diff.UMLParameterDiff;

public class StringBasedHeuristics {
	protected static final Pattern SPLIT_CONDITIONAL_PATTERN = Pattern.compile("(\\|\\|)|(&&)|(\\?)|(:)");
	protected static final Pattern SPLIT_CONCAT_STRING_PATTERN = Pattern.compile("(\\s)*(\\+)(\\s)*");

	protected static boolean containsMethodSignatureOfAnonymousClass(String s) {
		String[] lines = s.split("\\n");
		if(s.contains(" -> ") && lines.length > 1) {
			return true;
		}
		for(String line : lines) {
			line = VariableReplacementAnalysis.prepareLine(line);
			if(Visitor.METHOD_SIGNATURE_PATTERN.matcher(line).matches()) {
				return true;
			}
		}
		return false;
	}

	protected static boolean argumentsWithIdenticalMethodCalls(Set<String> arguments1, Set<String> arguments2,
			Set<String> variables1, Set<String> variables2) {
		int identicalMethodCalls = 0;
		if(arguments1.size() == arguments2.size()) {
			Iterator<String> it1 = arguments1.iterator();
			Iterator<String> it2 = arguments2.iterator();
			while(it1.hasNext() && it2.hasNext()) {
				String arg1 = it1.next();
				String arg2 = it2.next();
				if(arg1.contains("(") && arg2.contains("(") && arg1.contains(")") && arg2.contains(")")) {
					int indexOfOpeningParenthesis1 = arg1.indexOf("(");
					int indexOfClosingParenthesis1 = arg1.lastIndexOf(")");
					boolean openingParenthesisInsideSingleQuotes1 = ReplacementUtil.isInsideSingleQuotes(arg1, indexOfOpeningParenthesis1);
					boolean openingParenthesisInsideDoubleQuotes1 = ReplacementUtil.isInsideDoubleQuotes(arg1, indexOfOpeningParenthesis1);
					boolean closingParenthesisInsideSingleQuotes1 = ReplacementUtil.isInsideSingleQuotes(arg1, indexOfClosingParenthesis1);
					boolean closingParenthesisInsideDoubleQuotes1 = ReplacementUtil.isInsideDoubleQuotes(arg1, indexOfClosingParenthesis1);
					int indexOfOpeningParenthesis2 = arg2.indexOf("(");
					int indexOfClosingParenthesis2 = arg2.lastIndexOf(")");
					boolean openingParenthesisInsideSingleQuotes2 = ReplacementUtil.isInsideSingleQuotes(arg2, indexOfOpeningParenthesis2);
					boolean openingParenthesisInsideDoubleQuotes2 = ReplacementUtil.isInsideDoubleQuotes(arg2, indexOfOpeningParenthesis2);
					boolean closingParenthesisInsideSingleQuotes2 = ReplacementUtil.isInsideSingleQuotes(arg2, indexOfClosingParenthesis2);
					boolean closingParenthesisInsideDoubleQuotes2 = ReplacementUtil.isInsideDoubleQuotes(arg2, indexOfClosingParenthesis2);
					if(!openingParenthesisInsideSingleQuotes1 && !closingParenthesisInsideSingleQuotes1 &&
							!openingParenthesisInsideDoubleQuotes1 && !closingParenthesisInsideDoubleQuotes1 &&
							!openingParenthesisInsideSingleQuotes2 && !closingParenthesisInsideSingleQuotes2 &&
							!openingParenthesisInsideDoubleQuotes2 && !closingParenthesisInsideDoubleQuotes2) {
						String s1 = arg1.substring(0, indexOfOpeningParenthesis1);
						String s2 = arg2.substring(0, indexOfOpeningParenthesis2);
						if(s1.equals(s2) && s1.length() > 0) {
							String args1 = arg1.substring(indexOfOpeningParenthesis1+1, indexOfClosingParenthesis1);
							String args2 = arg2.substring(indexOfOpeningParenthesis2+1, indexOfClosingParenthesis2);
							if(variables1.contains(args1) && variables2.contains(args2)) {
								identicalMethodCalls++;
							}
						}
					}
				}
			}
		}
		return identicalMethodCalls == arguments1.size() && arguments1.size() > 0;
	}

	protected static boolean equalAfterParenthesisElimination(String s1, String s2) {
		String updatedS1 = s1.replace("(", "");
		updatedS1 = updatedS1.replace(")", "");
		String updatedS2 = s2.replace("(", "");
		updatedS2 = updatedS2.replace(")", "");
		return updatedS1.equals(updatedS2);
	}

	protected static boolean differOnlyInCastExpressionOrPrefixOperatorOrInfixOperand(String s1, String s2, Map<String, List<? extends AbstractCall>> methodInvocationMap1, Map<String, List<? extends AbstractCall>> methodInvocationMap2,
			List<String> infixExpressions1, List<String> infixExpressions2, List<VariableDeclaration> variableDeclarations1, List<VariableDeclaration> variableDeclarations2, ReplacementInfo info) {
		String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(s1, s2);
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
		if(!commonPrefix.isEmpty() && !commonSuffix.isEmpty()) {
			int beginIndexS1 = s1.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS1 = s1.lastIndexOf(commonSuffix);
			String diff1 = beginIndexS1 > endIndexS1 ? "" :	s1.substring(beginIndexS1, endIndexS1);
			int beginIndexS2 = s2.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS2 = s2.lastIndexOf(commonSuffix);
			String diff2 = beginIndexS2 > endIndexS2 ? "" :	s2.substring(beginIndexS2, endIndexS2);
			if(diff1.isEmpty() && diff2.equals("this.")) {
				return true;
			}
			else if(diff2.isEmpty() && diff1.equals("this.")) {
				return true;
			}
			if(cast(diff1, diff2)) {
				for(Replacement r : info.getReplacements()) {
					if(r.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS) && s2.startsWith(r.getAfter() + "=")) {
						if(variableDeclarations1.size() == 0 && !r.getBefore().contains("[") && !r.getBefore().contains("]")) {
							if(r.getAfter().contains("[") && r.getAfter().contains("]") && variableDeclarations2.size() == 0) {
								String arrayName = r.getAfter().substring(0, r.getAfter().indexOf("["));
								for(AbstractCodeFragment statement2 : info.getStatements2()) {
									if(statement2.getVariableDeclarations().size() > 0 && statement2.getVariableDeclarations().get(0).getVariableName().equals(arrayName)) {
										return false;
									}
								}
							}
						}
						else if(variableDeclarations2.size() == 0 && !r.getAfter().contains("[") && !r.getAfter().contains("]")) {
							if(r.getBefore().contains("[") && r.getBefore().contains("]") && variableDeclarations1.size() == 0) {
								String arrayName = r.getBefore().substring(0, r.getBefore().indexOf("["));
								for(AbstractCodeFragment statement1 : info.getStatements1()) {
									if(statement1.getVariableDeclarations().size() > 0 && statement1.getVariableDeclarations().get(0).getVariableName().equals(arrayName)) {
										return false;
									}
								}
							}
						}
					}
				}
				return true;
			}
			if(cast(diff2, diff1)) {
				for(Replacement r : info.getReplacements()) {
					if(r.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS) && s2.startsWith(r.getAfter() + "=")) {
						if(variableDeclarations1.size() == 0 && !r.getBefore().contains("[") && !r.getBefore().contains("]")) {
							if(r.getAfter().contains("[") && r.getAfter().contains("]") && variableDeclarations2.size() == 0) {
								String arrayName = r.getAfter().substring(0, r.getAfter().indexOf("["));
								for(AbstractCodeFragment statement2 : info.getStatements2()) {
									if(statement2.getVariableDeclarations().size() > 0 && statement2.getVariableDeclarations().get(0).getVariableName().equals(arrayName)) {
										return false;
									}
								}
							}
						}
						else if(variableDeclarations2.size() == 0 && !r.getAfter().contains("[") && !r.getAfter().contains("]")) {
							if(r.getBefore().contains("[") && r.getBefore().contains("]") && variableDeclarations1.size() == 0) {
								String arrayName = r.getBefore().substring(0, r.getBefore().indexOf("["));
								for(AbstractCodeFragment statement1 : info.getStatements1()) {
									if(statement1.getVariableDeclarations().size() > 0 && statement1.getVariableDeclarations().get(0).getVariableName().equals(arrayName)) {
										return false;
									}
								}
							}
						}
					}
				}
				return true;
			}
			if(diff1.isEmpty()) {
				if(diff2.equals("!") || diff2.equals("~")) {
					Replacement r = new Replacement(s1, s2, ReplacementType.INVERT_CONDITIONAL);
					info.addReplacement(r);
					return true;
				}
				if(infixExpressions2.size() - infixExpressions1.size() == 1 && !diff2.isEmpty() && countOperators(diff2) == 1) {
					for(String infix : infixExpressions2) {
						if(!infix.equals(diff2) && (infix.startsWith(diff2) || infix.endsWith(diff2))) {
							if(!variableDeclarationNameReplaced(variableDeclarations1, variableDeclarations2, info.getReplacements()) && !returnExpressionReplaced(s1, s2, info.getReplacements())) {
								return true;
							}
						}
					}
				}
				if(variableDeclarations1.isEmpty() && variableDeclarations2.size() == 1 && s1.startsWith("for(") && s2.startsWith("for(")) {
					String updatedS1 = "for(" + variableDeclarations2.get(0).getType().toString() + " " + commonSuffix;
					if(updatedS1.equals(s2)) {
						return true;
					}
				}
				if(variableDeclarations1.size() == 1 && variableDeclarations2.isEmpty() && s1.startsWith("for(") && s2.startsWith("for(")) {
					String updatedS2 = "for(" + variableDeclarations1.get(0).getType().toString() + " " + commonSuffix;
					if(updatedS2.equals(s1)) {
						return true;
					}
				}
			}
			if(diff2.isEmpty()) {
				if(diff1.equals("!") || diff1.equals("~")) {
					Replacement r = new Replacement(s1, s2, ReplacementType.INVERT_CONDITIONAL);
					info.addReplacement(r);
					return true;
				}
				if(infixExpressions1.size() - infixExpressions2.size() == 1 && !diff1.isEmpty() && countOperators(diff1) == 1) {
					for(String infix : infixExpressions1) {
						if(!infix.equals(diff1) && (infix.startsWith(diff1) || infix.endsWith(diff1))) {
							if(!variableDeclarationNameReplaced(variableDeclarations1, variableDeclarations2, info.getReplacements()) && !returnExpressionReplaced(s1, s2, info.getReplacements())) {
								return true;
							}
						}
					}
				}
			}
			for(String key1 : methodInvocationMap1.keySet()) {
				for(AbstractCall invocation1 : methodInvocationMap1.get(key1)) {
					if(invocation1.actualString().equals(diff1) && invocation1.getArguments().contains(diff2) &&
							(invocation1.getArguments().size() == 1 || (diff2.contains("?") && diff2.contains(":")))) {
						Replacement r = new VariableReplacementWithMethodInvocation(diff1, diff2, invocation1, Direction.INVOCATION_TO_VARIABLE);
						info.addReplacement(r);
						return true;
					}
				}
			}
			for(String key2 : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
					if(invocation2.actualString().equals(diff2) && invocation2.getArguments().contains(diff1) &&
							(invocation2.getArguments().size() == 1 || (diff1.contains("?") && diff1.contains(":")))) {
						Replacement r = new VariableReplacementWithMethodInvocation(diff1, diff2, invocation2, Direction.VARIABLE_TO_INVOCATION);
						info.addReplacement(r);
						return true;
					}
				}
			}
		}
		return false;
	}

	private static int countOperators(String input) {
		int count = 0;
		for(int i=0; i<input.length(); i++) {
			char c = input.charAt(i);
			if(c == '+' || c == '-') {
				count++;
			}
		}
		return count;
	}

	private static boolean returnExpressionReplaced(String s1, String s2, Set<Replacement> replacements) {
		if(s1.startsWith("return ") && s2.startsWith("return ")) {
			for(Replacement r : replacements) {
				if(s1.equals("return " + r.getAfter() + ";\n") || s2.equals("return " + r.getAfter() + ";\n")) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean variableDeclarationNameReplaced(List<VariableDeclaration> variableDeclarations1, List<VariableDeclaration> variableDeclarations2, Set<Replacement> replacements) {
		if(variableDeclarations1.size() == variableDeclarations2.size() && variableDeclarations1.size() == 1) {
			VariableDeclaration declaration1 = variableDeclarations1.get(0);
			VariableDeclaration declaration2 = variableDeclarations2.get(0);
			for(Replacement r : replacements) {
				if(r.getBefore().equals(declaration1.getVariableName()) && r.getAfter().equals(declaration2.getVariableName())) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean cast(String diff1, String diff2) {
		return (diff1.isEmpty() && diff2.startsWith("(") && diff2.endsWith(")")) || diff2.equals("(" + diff1 + ")");
	}

	protected static boolean differOnlyInFinalModifier(String s1, String s2) {;
		return differOnlyInPrefix(s1, s2, "for(", "for(final ") ||
				differOnlyInPrefix(s1, s2, "catch(", "catch(final ");
	}

	protected static boolean differOnlyInThis(String s1, String s2) {;
		if(differOnlyInPrefix(s1, s2, "", "this.")) {
			return true;
		}
		String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(s1, s2);
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
		if(!commonPrefix.isEmpty() && !commonSuffix.isEmpty()) {
			int beginIndexS1 = s1.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS1 = s1.lastIndexOf(commonSuffix);
			String diff1 = beginIndexS1 > endIndexS1 ? "" :	s1.substring(beginIndexS1, endIndexS1);
			int beginIndexS2 = s2.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS2 = s2.lastIndexOf(commonSuffix);
			String diff2 = beginIndexS2 > endIndexS2 ? "" :	s2.substring(beginIndexS2, endIndexS2);
			if(diff1.isEmpty() && diff2.equals("this.")) {
				return true;
			}
			else if(diff2.isEmpty() && diff1.equals("this.")) {
				return true;
			}
		}
		return false;
	}

	private static boolean differOnlyInPrefix(String s1, String s2, String prefixWithout, String prefixWith) {
		if(s1.startsWith(prefixWithout) && s2.startsWith(prefixWith)) {
			String suffix1 = s1.substring(prefixWithout.length(), s1.length());
			String suffix2 = s2.substring(prefixWith.length(), s2.length());
			if(suffix1.equals(suffix2)) {
				return true;
			}
		}
		if(s1.startsWith(prefixWith) && s2.startsWith(prefixWithout)) {
			String suffix1 = s1.substring(prefixWith.length(), s1.length());
			String suffix2 = s2.substring(prefixWithout.length(), s2.length());
			if(suffix1.equals(suffix2)) {
				return true;
			}
		}
		return false;
	}

	protected static boolean identicalVariableDeclarationsWithDifferentNames(String s1, String s2, List<VariableDeclaration> variableDeclarations1, List<VariableDeclaration> variableDeclarations2, ReplacementInfo replacementInfo) {
		if(variableDeclarations1.size() == variableDeclarations2.size() && variableDeclarations1.size() == 1) {
			VariableDeclaration declaration1 = variableDeclarations1.get(0);
			VariableDeclaration declaration2 = variableDeclarations2.get(0);
			if(!declaration1.getVariableName().equals(declaration2.getVariableName())) {
				String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
				String composedString1 = null;
				String composedString2 = null;
				if(s1.startsWith("catch(final ") && s2.startsWith("catch(final ") && declaration1.equalType(declaration2)) {
					composedString1 = "catch(final " + declaration1.getVariableName() + ")";
					composedString2 = "catch(final " + declaration2.getVariableName() + ")";
				}
				else if(s1.startsWith("catch(") && s2.startsWith("catch(") && declaration1.equalType(declaration2)) {
					composedString1 = "catch(" + declaration1.getVariableName() + ")";
					composedString2 = "catch(" + declaration2.getVariableName() + ")";
				}
				else {
					composedString1 = declaration1.getType() + " " + declaration1.getVariableName() + commonSuffix;
					composedString2 = declaration2.getType() + " " + declaration2.getVariableName() + commonSuffix;
				}
				if(s1.equals(composedString1) && s2.equals(composedString2)) {
					Replacement replacement = new Replacement(declaration1.getVariableName(), declaration2.getVariableName(), ReplacementType.VARIABLE_NAME);
					replacementInfo.addReplacement(replacement);
					return true;
				}
				if(s1.startsWith("catch(") && s2.startsWith("catch(") && !declaration1.equalType(declaration2)) {
					boolean containsAnotherUnmatchedCatch1 = false;
					for(AbstractCodeFragment statement1 : replacementInfo.getStatements1()) {
						if(statement1.getString().startsWith("catch(")) {
							containsAnotherUnmatchedCatch1 = true;
							break;
						}
					}
					boolean containsAnotherUnmatchedCatch2 = false;
					for(AbstractCodeFragment statement2 : replacementInfo.getStatements2()) {
						if(statement2.getString().startsWith("catch(")) {
							containsAnotherUnmatchedCatch2 = true;
							break;
						}
					}
					if(!containsAnotherUnmatchedCatch1 && !containsAnotherUnmatchedCatch2) {
						if(!declaration1.getVariableName().equals(declaration2.getVariableName())) {
							Replacement replacement = new Replacement(declaration1.getVariableName(), declaration2.getVariableName(), ReplacementType.VARIABLE_NAME);
							replacementInfo.addReplacement(replacement);
						}
						Replacement replacement = new Replacement(declaration1.getType().toString(), declaration2.getType().toString(), ReplacementType.TYPE);
						replacementInfo.addReplacement(replacement);
						return true;
					}
				}
			}
		}
		return false;
	}

	protected static boolean oneIsVariableDeclarationTheOtherIsVariableAssignment(String s1, String s2, List<VariableDeclaration> variableDeclarations1, List<VariableDeclaration> variableDeclarations2, ReplacementInfo replacementInfo) {
		if(variableDeclarations1.size() > 0 && variableDeclarations2.size() > 0) {
			String name1 = variableDeclarations1.get(0).getVariableName();
			String name2 = variableDeclarations2.get(0).getVariableName();
			AbstractExpression initializer1 = variableDeclarations1.get(0).getInitializer();
			AbstractExpression initializer2 = variableDeclarations2.get(0).getInitializer();
			if(initializer1 != null && initializer2 != null && !name1.equals(name2) && !initializer1.getString().equals(initializer2.getString())) {
				return false;
			}
		}
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
		if(s1.contains("=") && s2.contains("=")) {
			if(s1.equals(commonSuffix) || s2.equals(commonSuffix)) {
				if(replacementInfo.getReplacements().size() == 2) {
					StringBuilder sb = new StringBuilder();
					int counter = 0;
					for(Replacement r : replacementInfo.getReplacements()) {
						sb.append(r.getAfter());
						if(counter == 0) {
							sb.append("=");
						}
						else if(counter == 1) {
							sb.append(";\n");
						}
						counter++;
					}
					if(commonSuffix.equals(sb.toString())) {
						return false;
					}
				}
				else if(replacementInfo.getReplacements().size() == 1 && commonSuffix.endsWith("=false;\n")) {
					StringBuilder sb = new StringBuilder();
					for(Replacement r : replacementInfo.getReplacements()) {
						sb.append(r.getAfter());
					}
					sb.append("=false;\n");
					if(commonSuffix.equals(sb.toString())) {
						return false;
					}
				}
				else if(replacementInfo.getReplacements().size() == 1 && commonSuffix.endsWith("=true;\n")) {
					StringBuilder sb = new StringBuilder();
					for(Replacement r : replacementInfo.getReplacements()) {
						sb.append(r.getAfter());
					}
					sb.append("=true;\n");
					if(commonSuffix.equals(sb.toString())) {
						return false;
					}
				}
				else {
					String prefix = null;
					if(s1.equals(commonSuffix)) {
						prefix = s2.substring(0, s2.indexOf(commonSuffix));
					}
					else {
						prefix = s1.substring(0, s1.indexOf(commonSuffix));
					}
					int numberOfSpaces = 0;
					for(int i=0; i<prefix.length(); i++) {
						if(prefix.charAt(i) == ' ') {
							numberOfSpaces++;
						}
					}
					//allow final modifier and type
					if(numberOfSpaces > 2) {
						return false;
					}
				}
				for(Replacement r : replacementInfo.getReplacements()) {
					if(variableDeclarations1.size() > 0 && r.getBefore().equals(variableDeclarations1.get(0).getVariableName())) {
						if(r.getAfter().contains("[") && r.getAfter().contains("]") && variableDeclarations2.size() == 0) {
							String arrayName = r.getAfter().substring(0, r.getAfter().indexOf("["));
							for(AbstractCodeFragment statement2 : replacementInfo.getStatements2()) {
								if(statement2.getVariableDeclarations().size() > 0 && statement2.getVariableDeclarations().get(0).getVariableName().equals(arrayName)) {
									return false;
								}
							}
						}
					}
					else if(variableDeclarations2.size() > 0 && r.getAfter().equals(variableDeclarations2.get(0).getVariableName())) {
						if(r.getBefore().contains("[") && r.getBefore().contains("]") && variableDeclarations1.size() == 0) {
							String arrayName = r.getBefore().substring(0, r.getBefore().indexOf("["));
							for(AbstractCodeFragment statement1 : replacementInfo.getStatements1()) {
								if(statement1.getVariableDeclarations().size() > 0 && statement1.getVariableDeclarations().get(0).getVariableName().equals(arrayName)) {
									return false;
								}
							}
						}
					}
				}
				return true;
			}
			if(commonSuffix.contains("=") && replacementInfo.getReplacements().size() == 0) {
				for(AbstractCodeFragment fragment1 : replacementInfo.getStatements1()) {
					for(VariableDeclaration variableDeclaration : fragment1.getVariableDeclarations()) {
						if(s1.equals(variableDeclaration.getVariableName() + "." + commonSuffix)) {
							String prefix = s2.substring(0, s2.indexOf(commonSuffix));
							int numberOfSpaces = 0;
							for(int i=0; i<prefix.length(); i++) {
								if(prefix.charAt(i) == ' ') {
									numberOfSpaces++;
								}
							}
							//allow final modifier and type
							if(numberOfSpaces <= 2) {
								return true;
							}
						}
					}
				}
				for(AbstractCodeFragment fragment2 : replacementInfo.getStatements2()) {
					for(VariableDeclaration variableDeclaration : fragment2.getVariableDeclarations()) {
						if(s2.equals(variableDeclaration.getVariableName() + "." + commonSuffix)) {
							String prefix = s1.substring(0, s1.indexOf(commonSuffix));
							int numberOfSpaces = 0;
							for(int i=0; i<prefix.length(); i++) {
								if(prefix.charAt(i) == ' ') {
									numberOfSpaces++;
								}
							}
							//allow final modifier and type
							if(numberOfSpaces <= 2) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	protected static boolean oneIsVariableDeclarationTheOtherIsReturnStatement(String s1, String s2) {
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
		if(!commonSuffix.equals("null;\n") && !commonSuffix.equals("true;\n") && !commonSuffix.equals("false;\n") && !commonSuffix.equals("0;\n")) {
			if(s1.startsWith("return ") && s1.substring(7, s1.length()).equals(commonSuffix) &&
					s2.contains("=") && s2.substring(s2.indexOf("=")+1, s2.length()).equals(commonSuffix)) {
				return true;
			}
			if(s2.startsWith("return ") && s2.substring(7, s2.length()).equals(commonSuffix) &&
					s1.contains("=") && s1.substring(s1.indexOf("=")+1, s1.length()).equals(commonSuffix)) {
				return true;
			}
		}
		return false;
	}

	protected static boolean equalAfterNewArgumentAdditions(String s1, String s2, ReplacementInfo replacementInfo,
			VariableDeclarationContainer container1, VariableDeclarationContainer container2, UMLOperationDiff operationSignatureDiff, UMLAbstractClassDiff classDiff) {
		String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(s1, s2);
		String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(s1, s2);
		if(!commonPrefix.isEmpty() && !commonSuffix.isEmpty() && !commonPrefix.equals("return ")) {
			int beginIndexS1 = s1.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS1 = s1.lastIndexOf(commonSuffix);
			String diff1 = beginIndexS1 > endIndexS1 ? "" :	s1.substring(beginIndexS1, endIndexS1);
			int beginIndexS2 = s2.indexOf(commonPrefix) + commonPrefix.length();
			int endIndexS2 = s2.lastIndexOf(commonSuffix);
			String diff2 = beginIndexS2 > endIndexS2 ? "" :	s2.substring(beginIndexS2, endIndexS2);
			if(beginIndexS1 > endIndexS1) {
				diff2 = diff2 + commonSuffix.substring(0, beginIndexS1 - endIndexS1);
				if(diff2.charAt(diff2.length()-1) == ',') {
					diff2 = diff2.substring(0, diff2.length()-1);
				}
			}
			String characterAfterCommonPrefix = s1.equals(commonPrefix) ? "" : Character.toString(s1.charAt(commonPrefix.length())); 
			if(commonPrefix.contains(",") && commonPrefix.lastIndexOf(",") < commonPrefix.length()-1 &&
					!characterAfterCommonPrefix.equals(",") && !characterAfterCommonPrefix.equals(")")) {
				String prepend = commonPrefix.substring(commonPrefix.lastIndexOf(",")+1, commonPrefix.length());
				diff1 = prepend + diff1;
				diff2 = prepend + diff2;
			}
			//check for argument swap
			if(diff1.contains(",") && diff2.contains(",")) {
				String beforeComma1 = diff1.substring(0, diff1.indexOf(","));
				String afterComma1 = diff1.substring(diff1.indexOf(",") + 1, diff1.length());
				String beforeComma2 = diff2.substring(0, diff2.indexOf(","));
				String afterComma2 = diff2.substring(diff2.indexOf(",") + 1, diff2.length());
				if(beforeComma1.equals(afterComma2) && beforeComma2.equals(afterComma1)) {
					boolean conflictReplacement = false;
					for(Replacement r : replacementInfo.getReplacements()) {
						if(r.getAfter().equals(beforeComma2)) {
							conflictReplacement = true;
							break;
						}
					}
					if(!conflictReplacement) {
						SwapArgumentReplacement r = new SwapArgumentReplacement(beforeComma1, beforeComma2);
						replacementInfo.getReplacements().add(r);
						return true;
					}
				}
			}
			//if there is a variable replacement diff1 should be empty, otherwise diff1 should include a single variable
			if(diff1.isEmpty() ||
					(container1.getParameterNameList().contains(diff1) && !container2.getParameterNameList().contains(diff1) && !containsMethodSignatureOfAnonymousClass(diff2)) ||
					(classDiff != null && classDiff.getOriginalClass().containsAttributeWithName(diff1) && !classDiff.getNextClass().containsAttributeWithName(diff1) && !containsMethodSignatureOfAnonymousClass(diff2))) {
				List<UMLParameter> matchingAddedParameters = new ArrayList<UMLParameter>();
				List<UMLParameter> addedParameters = operationSignatureDiff != null ? operationSignatureDiff.getAddedParameters() : Collections.emptyList();
				for(UMLParameter addedParameter : addedParameters) {
					if(diff2.contains(addedParameter.getName())) {
						matchingAddedParameters.add(addedParameter);
					}
				}
				if(matchingAddedParameters.size() > 0) {
					Replacement matchingReplacement = null;
					for(Replacement replacement : replacementInfo.getReplacements()) {
						if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
							List<UMLParameterDiff> parameterDiffList = operationSignatureDiff != null ? operationSignatureDiff.getParameterDiffList() : Collections.emptyList();
							for(UMLParameterDiff parameterDiff : parameterDiffList) {
								if(parameterDiff.isNameChanged() &&
										replacement.getBefore().equals(parameterDiff.getRemovedParameter().getName()) &&
										replacement.getAfter().equals(parameterDiff.getAddedParameter().getName())) {
									matchingReplacement = replacement;
									break;
								}
							}
						}
						if(matchingReplacement != null) {
							break;
						}
					}
					if(matchingReplacement != null) {
						Set<String> splitVariables = new LinkedHashSet<String>();
						splitVariables.add(matchingReplacement.getAfter());
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(UMLParameter addedParameter : matchingAddedParameters) {
							splitVariables.add(addedParameter.getName());
							concat.append(addedParameter.getName());
							if(counter < matchingAddedParameters.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						SplitVariableReplacement split = new SplitVariableReplacement(matchingReplacement.getBefore(), splitVariables);
						if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
							replacementInfo.getReplacements().remove(matchingReplacement);
							replacementInfo.getReplacements().add(split);
							return true;
						}
					}
					else if(diff1.isEmpty() && replacementInfo.getReplacements().isEmpty()) {
						Set<String> addedVariables = new LinkedHashSet<String>();
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(UMLParameter addedParameter : matchingAddedParameters) {
							addedVariables.add(addedParameter.getName());
							concat.append(addedParameter.getName());
							if(counter < matchingAddedParameters.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						if(concat.toString().equals(diff2)) {
							AddVariableReplacement r = new AddVariableReplacement(addedVariables);
							replacementInfo.getReplacements().add(r);
							return true;
						}
					}
					if(container1.getParameterNameList().contains(diff1)) {
						Set<String> splitVariables = new LinkedHashSet<String>();
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(UMLParameter addedParameter : matchingAddedParameters) {
							splitVariables.add(addedParameter.getName());
							concat.append(addedParameter.getName());
							if(counter < matchingAddedParameters.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						SplitVariableReplacement split = new SplitVariableReplacement(diff1, splitVariables);
						if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
							replacementInfo.getReplacements().add(split);
							return true;
						}
					}
				}
				if(classDiff != null) {
					List<UMLAttribute> matchingAttributes = new ArrayList<UMLAttribute>();
					for(UMLAttribute attribute : classDiff.getNextClass().getAttributes()) {
						if(diff2.contains(attribute.getName())) {
							matchingAttributes.add(attribute);
						}
					}
					if(matchingAttributes.size() > 0) {
						Replacement matchingReplacement = null;
						for(Replacement replacement : replacementInfo.getReplacements()) {
							if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
								if(classDiff.getOriginalClass().containsAttributeWithName(replacement.getBefore()) &&
										classDiff.getNextClass().containsAttributeWithName(replacement.getAfter())) {
									matchingReplacement = replacement;
									break;
								}
							}
						}
						if(matchingReplacement != null) {
							Set<String> splitVariables = new LinkedHashSet<String>();
							splitVariables.add(matchingReplacement.getAfter());
							StringBuilder concat = new StringBuilder();
							int counter = 0;
							for(UMLAttribute attribute : matchingAttributes) {
								splitVariables.add(attribute.getName());
								concat.append(attribute.getName());
								if(counter < matchingAttributes.size()-1) {
									concat.append(",");
								}
								counter++;
							}
							SplitVariableReplacement split = new SplitVariableReplacement(matchingReplacement.getBefore(), splitVariables);
							if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
								replacementInfo.getReplacements().remove(matchingReplacement);
								replacementInfo.getReplacements().add(split);
								return true;
							}
						}
						else if(diff1.isEmpty() && replacementInfo.getReplacements().isEmpty()) {
							Set<String> addedVariables = new LinkedHashSet<String>();
							StringBuilder concat = new StringBuilder();
							int counter = 0;
							for(UMLAttribute attribute : matchingAttributes) {
								addedVariables.add(attribute.getName());
								concat.append(attribute.getName());
								if(counter < matchingAttributes.size()-1) {
									concat.append(",");
								}
								counter++;
							}
							if(concat.toString().equals(diff2)) {
								AddVariableReplacement r = new AddVariableReplacement(addedVariables);
								replacementInfo.getReplacements().add(r);
								return true;
							}
						}
						if(classDiff.getOriginalClass().containsAttributeWithName(diff1)) {
							Set<String> splitVariables = new LinkedHashSet<String>();
							StringBuilder concat = new StringBuilder();
							int counter = 0;
							for(UMLAttribute attribute : matchingAttributes) {
								splitVariables.add(attribute.getName());
								concat.append(attribute.getName());
								if(counter < matchingAttributes.size()-1) {
									concat.append(",");
								}
								counter++;
							}
							SplitVariableReplacement split = new SplitVariableReplacement(diff1, splitVariables);
							if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
								replacementInfo.getReplacements().add(split);
								return true;
							}
						}
					}
				}
				List<VariableDeclaration> matchingVariableDeclarations = new ArrayList<VariableDeclaration>();
				for(VariableDeclaration declaration : container2.getAllVariableDeclarations()) {
					if(diff2.contains(declaration.getVariableName())) {
						matchingVariableDeclarations.add(declaration);
					}
				}
				if(matchingVariableDeclarations.size() > 0) {
					Replacement matchingReplacement = null;
					for(Replacement replacement : replacementInfo.getReplacements()) {
						if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
							int indexOf1 = s1.indexOf(replacement.getAfter());
							int indexOf2 = s2.indexOf(replacement.getAfter());
							int characterIndex1 = indexOf1 + replacement.getAfter().length();
							int characterIndex2 = indexOf2 + replacement.getAfter().length();
							boolean isVariableDeclarationReplacement =
									characterIndex1 < s1.length() && (s1.charAt(characterIndex1) == '=' || s1.charAt(characterIndex1) == '.') &&
									characterIndex2 < s2.length() && (s2.charAt(characterIndex2) == '=' || s2.charAt(characterIndex2) == '.');
							boolean isCastExpression =
									indexOf1 > 0 && s1.charAt(indexOf1-1) == ')' &&
									indexOf2 > 0 && s2.charAt(indexOf2-1) == ')';
							if(!isVariableDeclarationReplacement && !isCastExpression &&
									container1.getVariableDeclaration(replacement.getBefore()) != null &&
									container2.getVariableDeclaration(replacement.getAfter()) != null) {
								matchingReplacement = replacement;
								break;
							}
						}
					}
					if(matchingReplacement != null) {
						Set<String> splitVariables = new LinkedHashSet<String>();
						splitVariables.add(matchingReplacement.getAfter());
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(VariableDeclaration declaration : matchingVariableDeclarations) {
							splitVariables.add(declaration.getVariableName());
							concat.append(declaration.getVariableName());
							if(counter < matchingVariableDeclarations.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						SplitVariableReplacement split = new SplitVariableReplacement(matchingReplacement.getBefore(), splitVariables);
						if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
							replacementInfo.getReplacements().remove(matchingReplacement);
							replacementInfo.getReplacements().add(split);
							return true;
						}
					}
					else if(diff1.isEmpty() && replacementInfo.getReplacements().isEmpty()) {
						Set<String> addedVariables = new LinkedHashSet<String>();
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(VariableDeclaration declaration : matchingVariableDeclarations) {
							addedVariables.add(declaration.getVariableName());
							concat.append(declaration.getVariableName());
							if(counter < matchingVariableDeclarations.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						if(concat.toString().equals(diff2)) {
							AddVariableReplacement r = new AddVariableReplacement(addedVariables);
							replacementInfo.getReplacements().add(r);
							return true;
						}
					}
					if(container1.getVariableDeclaration(diff1) != null) {
						Set<String> splitVariables = new LinkedHashSet<String>();
						StringBuilder concat = new StringBuilder();
						int counter = 0;
						for(VariableDeclaration declaration : matchingVariableDeclarations) {
							splitVariables.add(declaration.getVariableName());
							concat.append(declaration.getVariableName());
							if(counter < matchingVariableDeclarations.size()-1) {
								concat.append(",");
							}
							counter++;
						}
						SplitVariableReplacement split = new SplitVariableReplacement(diff1, splitVariables);
						if(!split.getSplitVariables().contains(split.getBefore()) && concat.toString().equals(diff2)) {
							replacementInfo.getReplacements().add(split);
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	protected static boolean equalAfterArgumentMerge(String s1, String s2, ReplacementInfo replacementInfo) {
		Map<String, Set<Replacement>> commonVariableReplacementMap = new LinkedHashMap<String, Set<Replacement>>();
		for(Replacement replacement : replacementInfo.getReplacements()) {
			if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
				String key = replacement.getAfter();
				if(commonVariableReplacementMap.containsKey(key)) {
					commonVariableReplacementMap.get(key).add(replacement);
					int index = s1.indexOf(key);
					if(index != -1) {
						if(s1.charAt(index+key.length()) == ',') {
							s1 = s1.substring(0, index) + s1.substring(index+key.length()+1, s1.length());
						}
						else if(index > 0 && s1.charAt(index-1) == ',') {
							s1 = s1.substring(0, index-1) + s1.substring(index+key.length(), s1.length());
						}
					}
				}
				else {
					Set<Replacement> replacements = new LinkedHashSet<Replacement>();
					replacements.add(replacement);
					commonVariableReplacementMap.put(key, replacements);
				}
			}
		}
		if(s1.equals(s2)) {
			for(String key : commonVariableReplacementMap.keySet()) {
				Set<Replacement> replacements = commonVariableReplacementMap.get(key);
				if(replacements.size() > 1) {
					replacementInfo.getReplacements().removeAll(replacements);
					Set<String> mergedVariables = new LinkedHashSet<String>();
					for(Replacement replacement : replacements) {
						mergedVariables.add(replacement.getBefore());
					}
					MergeVariableReplacement merge = new MergeVariableReplacement(mergedVariables, key);
					replacementInfo.getReplacements().add(merge);
				}
			}
			return true;
		}
		return false;
	}

	protected static boolean validStatementForConcatComparison(AbstractCodeFragment statement1, AbstractCodeFragment statement2) {
		List<VariableDeclaration> variableDeclarations1 = statement1.getVariableDeclarations();
		List<VariableDeclaration> variableDeclarations2 = statement2.getVariableDeclarations();
		if(variableDeclarations1.size() == variableDeclarations2.size()) {
			return true;
		}
		else {
			if(variableDeclarations1.size() > 0 && variableDeclarations2.size() == 0 && statement2.getString().startsWith("return ")) {
				return true;
			}
			else if(variableDeclarations1.size() == 0 && variableDeclarations2.size() > 0 && statement1.getString().startsWith("return ")) {
				return true;
			}
		}
		return false;
	}

	protected static boolean containsValidOperatorReplacements(ReplacementInfo replacementInfo) {
		List<Replacement> operatorReplacements = replacementInfo.getReplacements(ReplacementType.INFIX_OPERATOR);
		for(Replacement replacement : operatorReplacements) {
			if(replacement.getBefore().equals("==") && !replacement.getAfter().equals("!="))
				return false;
			if(replacement.getBefore().equals("!=") && !replacement.getAfter().equals("=="))
				return false;
			if(replacement.getBefore().equals("&&") && !replacement.getAfter().equals("||"))
				return false;
			if(replacement.getBefore().equals("||") && !replacement.getAfter().equals("&&"))
				return false;
		}
		return true;
	}

	protected static boolean commonConcat(String s1, String s2, Map<String, String> parameterToArgumentMap, ReplacementInfo info, AbstractCodeFragment statement1, AbstractCodeFragment statement2) {
		ObjectCreation creationCoveringTheEntireStatement1 = statement1.creationCoveringEntireFragment();
		ObjectCreation creationCoveringTheEntireStatement2 = statement2.creationCoveringEntireFragment();
		boolean arrayCreation1 = creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement1.isArray();
		boolean arrayCreation2 = creationCoveringTheEntireStatement2 != null && creationCoveringTheEntireStatement2.isArray();
		if(!arrayCreation1 && !arrayCreation2 && !containsMethodSignatureOfAnonymousClass(s1) && !containsMethodSignatureOfAnonymousClass(s2)) {
			if(s1.contains("+") && s2.contains("+") && !s1.contains("++") && !s2.contains("++")) {
				Set<String> tokens1 = new LinkedHashSet<String>(Arrays.asList(SPLIT_CONCAT_STRING_PATTERN.split(s1)));
				Set<String> tokens2 = new LinkedHashSet<String>(Arrays.asList(SPLIT_CONCAT_STRING_PATTERN.split(s2)));
				Set<String> intersection = new LinkedHashSet<String>(tokens1);
				intersection.retainAll(tokens2);
				Set<String> filteredIntersection = new LinkedHashSet<String>();
				for(String common : intersection) {
					boolean foundInReplacements = false;
					for(Replacement r : info.getReplacements()) {
						if(r.getBefore().contains(common) || r.getAfter().contains(common)) {
							foundInReplacements = true;
							break;
						}
					}
					if(!foundInReplacements) {
						filteredIntersection.add(common);
					}
				}
				int size = filteredIntersection.size();
				int threshold = Math.max(tokens1.size(), tokens2.size()) - size;
				if((size > 0 && size > threshold) || (size > 1 && size >= threshold)) {
					List<String> tokens1AsList = new ArrayList<>(tokens1);
					List<String> tokens2AsList = new ArrayList<>(tokens2);
					int counter = 0;
					boolean allTokensMatchInTheSameOrder = true;
					for(String s : filteredIntersection) {
						if(!tokens1AsList.get(counter).equals(s)) {
							allTokensMatchInTheSameOrder = false;
							break;
						}
						if(!tokens2AsList.get(counter).equals(s)) {
							allTokensMatchInTheSameOrder = false;
							break;
						}
						counter++;
					}
					if(allTokensMatchInTheSameOrder && tokens1.size() == size+1 && tokens2.size() == size+1) {
						return false;
					}
					IntersectionReplacement r = new IntersectionReplacement(s1, s2, intersection, ReplacementType.CONCATENATION);
					info.getReplacements().add(r);
					return true;
				}
			}
			List<String> arguments1 = null;
			AbstractCall invocation1 = null;
			if(creationCoveringTheEntireStatement1 != null) {
				arguments1 = creationCoveringTheEntireStatement1.getArguments();
			}
			else if((invocation1 = statement1.invocationCoveringEntireFragment()) != null) {
				arguments1 = invocation1.getArguments();
			}
			else if((invocation1 = statement1.assignmentInvocationCoveringEntireStatement()) != null) {
				arguments1 = invocation1.getArguments();
			}
			List<String> arguments2 = null;
			AbstractCall invocation2 = null;
			if(creationCoveringTheEntireStatement2 != null) {
				arguments2 = creationCoveringTheEntireStatement2.getArguments();
			}
			else if((invocation2 = statement2.invocationCoveringEntireFragment()) != null) {
				arguments2 = invocation2.getArguments();
			}
			else if((invocation2 = statement2.assignmentInvocationCoveringEntireStatement()) != null) {
				arguments2 = invocation2.getArguments();
			}
			if(arguments1 != null && arguments2 != null) {
				Set<Replacement> concatReplacements = new LinkedHashSet<>();
				int equalArguments = 0;
				int concatenatedArguments = 0;
				int replacedArguments = 0;
				int minSize = Math.min(arguments1.size(), arguments2.size());
				for(int i=0; i<minSize; i++) {
					String arg1 = arguments1.get(i);
					String arg2 = arguments2.get(i);
					if(arg1.equals(arg2)) {
						equalArguments++;
					}
					else if(!arg1.contains("+") && arg2.contains("+") && !arg2.contains("++")) {
						boolean tokenMatchesArgument = false;
						Set<String> tokens2 = new LinkedHashSet<String>(Arrays.asList(SPLIT_CONCAT_STRING_PATTERN.split(arg2)));
						StringBuilder sb = new StringBuilder();
						sb.append("\"");
						for(String token : tokens2) {
							if(arguments1.contains(token) && arguments1.size() == arguments2.size() && tokens2.size() <= 2) {
								tokenMatchesArgument = true;
							}
							if(token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
								sb.append(token.substring(1, token.length()-1));
							}
							else if(parameterToArgumentMap.containsKey(token)) {
								sb.append(parameterToArgumentMap.get(token));
							}
							else {
								sb.append(token);
							}
						}
						sb.append("\"");
						String concatenatedString = sb.toString();
						if(concatenatedString.equals(arg1)) {
							concatReplacements.add(new Replacement(arg1, arg2, ReplacementType.CONCATENATION));
							concatenatedArguments++;
						}
						else if(StringDistance.editDistance(concatenatedString, arg1) < tokens2.size()) {
							concatReplacements.add(new Replacement(arg1, arg2, ReplacementType.CONCATENATION));
							concatenatedArguments++;
						}
						if(tokenMatchesArgument) {
							concatenatedArguments++;
						}
					}
					else if(!arg2.contains("+") && arg1.contains("+") && !arg1.contains("++")) {
						boolean tokenMatchesArgument = false;
						Set<String> tokens1 = new LinkedHashSet<String>(Arrays.asList(SPLIT_CONCAT_STRING_PATTERN.split(arg1)));
						StringBuilder sb = new StringBuilder();
						sb.append("\"");
						for(String token : tokens1) {
							if(arguments2.contains(token) && arguments1.size() == arguments2.size() && tokens1.size() <= 2) {
								tokenMatchesArgument = true;
							}
							if(token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
								sb.append(token.substring(1, token.length()-1));
							}
							else if(parameterToArgumentMap.containsKey(token)) {
								sb.append(parameterToArgumentMap.get(token));
							}
							else {
								sb.append(token);
							}
						}
						sb.append("\"");
						String concatenatedString = sb.toString();
						if(concatenatedString.equals(arg2)) {
							concatReplacements.add(new Replacement(arg1, arg2, ReplacementType.CONCATENATION));
							concatenatedArguments++;
						}
						else if(StringDistance.editDistance(concatenatedString, arg2) < tokens1.size()) {
							concatReplacements.add(new Replacement(arg1, arg2, ReplacementType.CONCATENATION));
							concatenatedArguments++;
						}
						if(tokenMatchesArgument) {
							concatenatedArguments++;
						}
					}
					else {
						for(Replacement replacement : info.getReplacements()) {
							if(replacement.getBefore().equals(arg1) &&	replacement.getAfter().equals(arg2)) {
								replacedArguments++;
								break;
							}
						}
					}
				}
				if(equalArguments + replacedArguments + concatenatedArguments == minSize && concatenatedArguments > 0) {
					info.getReplacements().addAll(concatReplacements);
					return true;
				}
			}
		}
		return false;
	}

	protected static boolean equalAfterInfixExpressionExpansion(String s1, String s2, ReplacementInfo replacementInfo, List<String> infixExpressions1) {
		Set<Replacement> replacementsToBeRemoved = new LinkedHashSet<Replacement>();
		Set<Replacement> replacementsToBeAdded = new LinkedHashSet<Replacement>();
		String originalArgumentizedString1 = replacementInfo.getArgumentizedString1();
		for(Replacement replacement : replacementInfo.getReplacements()) {
			String before = replacement.getBefore();
			for(String infixExpression1 : infixExpressions1) {
				if(infixExpression1.startsWith(before)) {
					String suffix = infixExpression1.substring(before.length(), infixExpression1.length());
					String after = replacement.getAfter();
					if(s1.contains(after + suffix)) {
						String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), after + suffix, after);
						int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
						if(distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
							replacementsToBeRemoved.add(replacement);
							Replacement newReplacement = new Replacement(infixExpression1, after, ReplacementType.INFIX_EXPRESSION);
							replacementsToBeAdded.add(newReplacement);
							replacementInfo.setArgumentizedString1(temp);
						}
					}
				}
			}
		}
		if(replacementInfo.getRawDistance() == 0) {
			replacementInfo.removeReplacements(replacementsToBeRemoved);
			replacementInfo.addReplacements(replacementsToBeAdded);
			return true;
		}
		else {
			replacementInfo.setArgumentizedString1(originalArgumentizedString1);
			return false;
		}
	}

	protected static boolean thisConstructorCallWithEverythingReplaced(AbstractCall invocationCoveringTheEntireStatement1, AbstractCall invocationCoveringTheEntireStatement2,
			ReplacementInfo replacementInfo) {
		if(invocationCoveringTheEntireStatement1 != null && invocationCoveringTheEntireStatement2 != null &&
				invocationCoveringTheEntireStatement1.getName().equals("this") && invocationCoveringTheEntireStatement2.getName().equals("this")) {
			List<String> arguments1 = invocationCoveringTheEntireStatement1.getArguments();
			List<String> arguments2 = invocationCoveringTheEntireStatement2.getArguments();
			Set<String> argumentIntersection = invocationCoveringTheEntireStatement1.argumentIntersection(invocationCoveringTheEntireStatement2);
			int minArguments = Math.min(arguments1.size(), arguments2.size());
			int replacedArguments = 0;
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(arguments1.contains(replacement.getBefore()) && arguments2.contains(replacement.getAfter())) {
					replacedArguments++;
				}
			}
			if(replacedArguments == minArguments || replacedArguments > argumentIntersection.size()) {
				return true;
			}
		}
		return false;
	}

	protected static boolean classInstanceCreationWithEverythingReplaced(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			ReplacementInfo replacementInfo, Map<String, String> parameterToArgumentMap) {
		String string1 = statement1.getString();
		String string2 = statement2.getString();
		if(containsMethodSignatureOfAnonymousClass(string1)) {
			string1 = string1.substring(0, string1.indexOf("\n"));
		}
		if(containsMethodSignatureOfAnonymousClass(string2)) {
			string2 = string2.substring(0, string2.indexOf("\n"));
		}
		if(string1.contains("=") && string1.endsWith(";\n") && string2.startsWith("return ") && string2.endsWith(";\n")) {
			boolean typeReplacement = false, compatibleTypes = false, classInstanceCreationReplacement = false;
			String assignment1 = string1.substring(string1.indexOf("=")+1, string1.lastIndexOf(";\n"));
			String assignment2 = string2.substring(7, string2.lastIndexOf(";\n"));
			UMLType type1 = null, type2 = null;
			ObjectCreation objectCreation1 = null, objectCreation2 = null;
			Map<String, String> argumentToParameterMap = new LinkedHashMap<String, String>();
			Map<String, List<ObjectCreation>> creationMap1 = statement1.getCreationMap();
			for(String creation1 : creationMap1.keySet()) {
				if(creation1.equals(assignment1)) {
					objectCreation1 = creationMap1.get(creation1).get(0);
					type1 = objectCreation1.getType();
				}
			}
			Map<String, List<ObjectCreation>> creationMap2 = statement2.getCreationMap();
			for(String creation2 : creationMap2.keySet()) {
				if(creation2.equals(assignment2)) {
					objectCreation2 = creationMap2.get(creation2).get(0);
					type2 = objectCreation2.getType();
					for(String argument : objectCreation2.getArguments()) {
						if(parameterToArgumentMap.containsKey(argument)) {
							argumentToParameterMap.put(parameterToArgumentMap.get(argument), argument);
						}
					}
				}
			}
			int minArguments = 0;
			if(type1 != null && type2 != null) {
				compatibleTypes = type1.compatibleTypes(type2);
				minArguments = Math.min(objectCreation1.getArguments().size(), objectCreation2.getArguments().size());
			}
			int replacedArguments = 0;
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE)) {
					typeReplacement = true;
					if(string1.contains("new " + replacement.getBefore() + "(") && string2.contains("new " + replacement.getAfter() + "("))
						classInstanceCreationReplacement = true;
				}
				else if(objectCreation1 != null && objectCreation2 != null &&
						objectCreation1.getArguments().contains(replacement.getBefore()) &&
						(objectCreation2.getArguments().contains(replacement.getAfter()) || objectCreation2.getArguments().contains(argumentToParameterMap.get(replacement.getAfter())))) {
					replacedArguments++;
				}
				else if(replacement.getType().equals(ReplacementType.CLASS_INSTANCE_CREATION) &&
						assignment1.equals(replacement.getBefore()) &&
						assignment2.equals(replacement.getAfter()))
					classInstanceCreationReplacement = true;
			}
			if(typeReplacement && !compatibleTypes && replacedArguments == minArguments && classInstanceCreationReplacement) {
				return true;
			}
		}
		else if(string1.startsWith("return ") && string1.endsWith(";\n") && string2.contains("=") && string2.endsWith(";\n")) {
			boolean typeReplacement = false, compatibleTypes = false, classInstanceCreationReplacement = false;
			String assignment1 = string1.substring(7, string1.lastIndexOf(";\n"));
			String assignment2 = string2.substring(string2.indexOf("=")+1, string2.lastIndexOf(";\n"));
			UMLType type1 = null, type2 = null;
			ObjectCreation objectCreation1 = null, objectCreation2 = null;
			Map<String, String> argumentToParameterMap = new LinkedHashMap<String, String>();
			Map<String, List<ObjectCreation>> creationMap1 = statement1.getCreationMap();
			for(String creation1 : creationMap1.keySet()) {
				if(creation1.equals(assignment1)) {
					objectCreation1 = creationMap1.get(creation1).get(0);
					type1 = objectCreation1.getType();
				}
			}
			Map<String, List<ObjectCreation>> creationMap2 = statement2.getCreationMap();
			for(String creation2 : creationMap2.keySet()) {
				if(creation2.equals(assignment2)) {
					objectCreation2 = creationMap2.get(creation2).get(0);
					type2 = objectCreation2.getType();
					for(String argument : objectCreation2.getArguments()) {
						if(parameterToArgumentMap.containsKey(argument)) {
							argumentToParameterMap.put(parameterToArgumentMap.get(argument), argument);
						}
					}
				}
			}
			int minArguments = 0;
			if(type1 != null && type2 != null) {
				compatibleTypes = type1.compatibleTypes(type2);
				minArguments = Math.min(objectCreation1.getArguments().size(), objectCreation2.getArguments().size());
			}
			int replacedArguments = 0;
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE)) {
					typeReplacement = true;
					if(string1.contains("new " + replacement.getBefore() + "(") && string2.contains("new " + replacement.getAfter() + "("))
						classInstanceCreationReplacement = true;
				}
				else if(objectCreation1 != null && objectCreation2 != null &&
						objectCreation1.getArguments().contains(replacement.getBefore()) &&
						(objectCreation2.getArguments().contains(replacement.getAfter()) || objectCreation2.getArguments().contains(argumentToParameterMap.get(replacement.getAfter())))) {
					replacedArguments++;
				}
				else if(replacement.getType().equals(ReplacementType.CLASS_INSTANCE_CREATION) &&
						assignment1.equals(replacement.getBefore()) &&
						assignment2.equals(replacement.getAfter()))
					classInstanceCreationReplacement = true;
			}
			if(typeReplacement && !compatibleTypes && replacedArguments == minArguments && classInstanceCreationReplacement) {
				return true;
			}
		}
		return false;
	}

	protected static boolean variableAssignmentWithEverythingReplaced(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			ReplacementInfo replacementInfo) {
		String string1 = statement1.getString();
		String string2 = statement2.getString();
		if(containsMethodSignatureOfAnonymousClass(string1)) {
			string1 = string1.substring(0, string1.indexOf("\n"));
		}
		if(containsMethodSignatureOfAnonymousClass(string2)) {
			string2 = string2.substring(0, string2.indexOf("\n"));
		}
		if(string1.contains("=") && string1.endsWith(";\n") && string2.contains("=") && string2.endsWith(";\n")) {
			boolean typeReplacement = false, compatibleTypes = false, variableRename = false, classInstanceCreationReplacement = false, equalArguments = false, rightHandSideReplacement = false;
			String variableName1 = string1.substring(0, string1.indexOf("="));
			String variableName2 = string2.substring(0, string2.indexOf("="));
			String assignment1 = string1.substring(string1.indexOf("=")+1, string1.lastIndexOf(";\n"));
			String assignment2 = string2.substring(string2.indexOf("=")+1, string2.lastIndexOf(";\n"));
			UMLType type1 = null, type2 = null;
			AbstractCall inv1 = null, inv2 = null;
			Map<String, List<ObjectCreation>> creationMap1 = statement1.getCreationMap();
			for(String creation1 : creationMap1.keySet()) {
				if(creation1.equals(assignment1)) {
					ObjectCreation objectCreation = creationMap1.get(creation1).get(0);
					type1 = objectCreation.getType();
					inv1 = objectCreation;
				}
			}
			Map<String, List<ObjectCreation>> creationMap2 = statement2.getCreationMap();
			for(String creation2 : creationMap2.keySet()) {
				if(creation2.equals(assignment2)) {
					ObjectCreation objectCreation = creationMap2.get(creation2).get(0);
					type2 = objectCreation.getType();
					inv2 = objectCreation;
				}
			}
			if(type1 != null && type2 != null) {
				compatibleTypes = type1.compatibleTypes(type2);
			}
			Map<String, List<AbstractCall>> methodInvocationMap1 = statement1.getMethodInvocationMap();
			for(String invocation1 : methodInvocationMap1.keySet()) {
				if(invocation1.equals(assignment1)) {
					inv1 = methodInvocationMap1.get(invocation1).get(0);
				}
			}
			Map<String, List<AbstractCall>> methodInvocationMap2 = statement2.getMethodInvocationMap();
			for(String invocation2 : methodInvocationMap2.keySet()) {
				if(invocation2.equals(assignment2)) {
					inv2 = methodInvocationMap2.get(invocation2).get(0);
				}
			}
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE)) {
					typeReplacement = true;
					if(string1.contains("new " + replacement.getBefore() + "(") && string2.contains("new " + replacement.getAfter() + "("))
						classInstanceCreationReplacement = true;
				}
				else if(replacement.getType().equals(ReplacementType.VARIABLE_NAME) &&
						(variableName1.equals(replacement.getBefore()) || variableName1.endsWith(" " + replacement.getBefore())) &&
						(variableName2.equals(replacement.getAfter()) || variableName2.endsWith(" " + replacement.getAfter())))
					variableRename = true;
				else if(replacement.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL) &&
						assignment1.equals(replacement.getBefore()) &&
						assignment2.equals(replacement.getAfter()))
					rightHandSideReplacement = true;
				else if(replacement.getType().equals(ReplacementType.CLASS_INSTANCE_CREATION) &&
						assignment1.equals(replacement.getBefore()) &&
						assignment2.equals(replacement.getAfter()))
					classInstanceCreationReplacement = true;
			}
			if(inv1 != null && inv2 != null) {
				equalArguments = inv1.equalArguments(inv2) && inv1.getArguments().size() > 0;
			}
			if(typeReplacement && !compatibleTypes && variableRename && classInstanceCreationReplacement && !equalArguments) {
				return true;
			}
			if(variableRename && rightHandSideReplacement) {
				return true;
			}
			if(variableRename && inv1 != null && inv2 != null && inv1.differentExpressionNameAndArguments(inv2)) {
				if(inv1.getArguments().size() > inv2.getArguments().size()) {
					for(String argument : inv1.getArguments()) {
						List<AbstractCall> argumentInvocations = methodInvocationMap1.get(argument);
						if(argumentInvocations != null) {
							for(AbstractCall argumentInvocation : argumentInvocations) {
								if(!argumentInvocation.differentExpressionNameAndArguments(inv2)) {
									return false;
								}
							}
						}
					}
				}
				else if(inv1.getArguments().size() < inv2.getArguments().size()) {
					for(String argument : inv2.getArguments()) {
						List<AbstractCall> argumentInvocations = methodInvocationMap2.get(argument);
						if(argumentInvocations != null) {
							for(AbstractCall argumentInvocation : argumentInvocations) {
								if(!inv1.differentExpressionNameAndArguments(argumentInvocation)) {
									return false;
								}
							}
						}
					}
				}
				return true;
			}
		}
		return false;
	}

	protected static boolean operatorExpressionWithEverythingReplaced(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			ReplacementInfo replacementInfo, Map<String, String> parameterToArgumentMap) {
		String string1 = statement1.getString();
		String string2 = statement2.getString();
		if(containsMethodSignatureOfAnonymousClass(string1)) {
			string1 = string1.substring(0, string1.indexOf("\n"));
		}
		if(containsMethodSignatureOfAnonymousClass(string2)) {
			string2 = string2.substring(0, string2.indexOf("\n"));
		}
		List<String> operators1 = statement1.getInfixOperators();
		List<String> operators2 = statement2.getInfixOperators();
		if(operators1.size() == 1 && operators2.size() == 1) {
			String operator1 = operators1.get(0);
			String operator2 = operators2.get(0);
			int indexOfOperator1 = string1.indexOf(operator1);
			int indexOfOperator2 = string2.indexOf(operator2);
			if(indexOfOperator1 != -1 && indexOfOperator2 != -1) {
				String leftOperand1 = string1.substring(0, indexOfOperator1);
				String leftOperand2 = string2.substring(0, indexOfOperator2);
				String rightOperand1 = string1.substring(indexOfOperator1 + operator1.length(), string1.length());
				String rightOperand2 = string2.substring(indexOfOperator2 + operator2.length(), string2.length());
				boolean operatorReplacement = false;
				boolean leftOperandReplacement = false;
				boolean rightOperandReplacement = false;
				for(Replacement replacement : replacementInfo.getReplacements()) {
					if(parameterToArgumentMap.containsValue(replacement.getAfter())) {
						for(String key : parameterToArgumentMap.keySet()) {
							if(parameterToArgumentMap.get(key).equals(replacement.getAfter())) {
								if(leftOperand1.contains(replacement.getBefore()) && leftOperand2.contains(key)) {
									leftOperandReplacement = true;
								}
								if(rightOperand1.contains(replacement.getBefore()) && rightOperand2.contains(key)) {
									rightOperandReplacement = true;
								}
								break;
							}
						}
					}
					if(replacement.getType().equals(ReplacementType.INFIX_OPERATOR)) {
						if(replacement.getBefore().equals(operator1) && replacement.getAfter().equals(operator2)) {
							operatorReplacement = true;
						}
					}
					else if(leftOperand1.contains(replacement.getBefore()) && leftOperand2.contains(replacement.getAfter())) {
						leftOperandReplacement = true;
					}
					else if(rightOperand1.contains(replacement.getBefore()) && rightOperand2.contains(replacement.getAfter())) {
						rightOperandReplacement = true;
					}
				}
				if(operatorReplacement && leftOperandReplacement && rightOperandReplacement) {
					return true;
				}
			}
		}
		else if(replacementInfo.getReplacements().size() > 1) {
			StringBuilder stringBefore = new StringBuilder();
			StringBuilder stringAfter = new StringBuilder();
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.INFIX_OPERATOR)) {
					stringBefore.append(" " + replacement.getBefore() + " ");
					stringAfter.append(" " + replacement.getAfter() + " ");
				}
				else {
					stringBefore.append(replacement.getBefore());
					stringAfter.append(replacement.getAfter());
				}
			}
			if(statement1.getString().startsWith("return ") && statement2.getString().startsWith("return ")) {
				return statement1.getString().equals("return " + stringBefore + ";\n") && statement2.getString().equals("return " + stringAfter + ";\n");
			}
			else if(statement1.getString().startsWith("if(") && statement2.getString().startsWith("if(")) {
				return statement1.getString().equals("if(" + stringBefore + ")") && statement2.getString().equals("if(" + stringAfter + ")");
			}
			else if(statement1.getString().startsWith("while(") && statement2.getString().startsWith("while(")) {
				return statement1.getString().equals("while(" + stringBefore + ")") && statement2.getString().equals("while(" + stringAfter + ")");
			}
		}
		return false;
	}

	protected static boolean variableDeclarationsWithEverythingReplaced(List<VariableDeclaration> variableDeclarations1,
			List<VariableDeclaration> variableDeclarations2, ReplacementInfo replacementInfo) {
		if(variableDeclarations1.size() == 1 && variableDeclarations2.size() == 1) {
			boolean typeReplacement = false, variableRename = false, initializerReplacement = false, nullInitializer = false, zeroArgumentClassInstantiation = false, classInstantiationArgumentReplacement = false;
			UMLType type1 = variableDeclarations1.get(0).getType();
			UMLType type2 = variableDeclarations2.get(0).getType();
			AbstractExpression initializer1 = variableDeclarations1.get(0).getInitializer();
			AbstractExpression initializer2 = variableDeclarations2.get(0).getInitializer();
			if(initializer1 == null && initializer2 == null) {
				nullInitializer = true;
			}
			else if(initializer1 != null && initializer2 != null) {
				nullInitializer = initializer1.getExpression().equals("null") || initializer2.getExpression().equals("null");
				if(initializer1.getCreationMap().size() == 1 && initializer2.getCreationMap().size() == 1) {
					ObjectCreation creation1 = initializer1.getCreationMap().values().iterator().next().get(0);
					ObjectCreation creation2 = initializer2.getCreationMap().values().iterator().next().get(0);
					if(creation1.getArguments().size() == 0 && creation2.getArguments().size() == 0) {
						zeroArgumentClassInstantiation = true;
					}
					else if(creation1.getArguments().size() == 1 && creation2.getArguments().size() == 1) {
						String argument1 = creation1.getArguments().get(0);
						String argument2 = creation2.getArguments().get(0);
						for(Replacement replacement : replacementInfo.getReplacements()) {
							if(replacement.getBefore().equals(argument1) && replacement.getAfter().equals(argument2)) {
								classInstantiationArgumentReplacement = true;
								break;
							}
						}
					}
				}
				AbstractCall invocation1 = initializer1.invocationCoveringEntireFragment();
				AbstractCall invocation2 = initializer2.invocationCoveringEntireFragment();
				if(invocation1 != null && invocation2 != null && invocation1.getCoverage().equals(StatementCoverageType.CAST_CALL) != invocation2.getCoverage().equals(StatementCoverageType.CAST_CALL)) {
					initializerReplacement = true;
				}
			}
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE) &&
						type1 != null && type1.toQualifiedString().equals(replacement.getBefore()) &&
						type2 != null && type2.toQualifiedString().equals(replacement.getAfter()))
					typeReplacement = true;
				else if(replacement.getType().equals(ReplacementType.VARIABLE_NAME) &&
						variableDeclarations1.get(0).getVariableName().equals(replacement.getBefore()) &&
						variableDeclarations2.get(0).getVariableName().equals(replacement.getAfter()))
					variableRename = true;
				else if(initializer1 != null && initializer1.getExpression().equals(replacement.getBefore()) &&
						initializer2 != null && initializer2.getExpression().equals(replacement.getAfter())) {
					initializerReplacement = true;
				}
			}
			if(typeReplacement && !type1.compatibleTypes(type2) && variableRename && (initializerReplacement || nullInitializer || zeroArgumentClassInstantiation || classInstantiationArgumentReplacement)) {
				return true;
			}
		}
		return false;
	}

	protected static String statementWithoutAnonymous(AbstractCodeFragment statement, AnonymousClassDeclarationObject anonymousClassDeclaration, VariableDeclarationContainer operation) {
		int index = statement.getString().indexOf(anonymousClassDeclaration.toString());
		if(index != -1) {
			return statement.getString().substring(0, index);
		}
		else {
			for(LambdaExpressionObject lambda : statement.getLambdas()) {
				OperationBody body = lambda.getBody();
				if(body != null) {
					List<AbstractCodeFragment> leaves = body.getCompositeStatement().getLeaves();
					for(AbstractCodeFragment leaf : leaves) {
						for(AnonymousClassDeclarationObject anonymousObject : leaf.getAnonymousClassDeclarations()) {
							if(anonymousObject.getLocationInfo().equals(anonymousClassDeclaration.getLocationInfo())) {
								String statementWithoutAnonymous = statementWithoutAnonymous(leaf, anonymousClassDeclaration, operation);
								if(statementWithoutAnonymous != null) {
									return statementWithoutAnonymous;
								}
							}
						}
					}
				}
			}
			List<UMLOperation> anonymousOperations = new ArrayList<UMLOperation>();
			for(AnonymousClassDeclarationObject anonymousObject : statement.getAnonymousClassDeclarations()) {
				for(UMLAnonymousClass anonymousClass : operation.getAnonymousClassList()) {
					if(anonymousClass.getLocationInfo().equals(anonymousObject.getLocationInfo())) {
						anonymousOperations.addAll(anonymousClass.getOperations());
					}
				}
			}
			for(UMLOperation anonymousOperation : anonymousOperations) {
				OperationBody body = anonymousOperation.getBody();
				if(body != null) {
					List<AbstractCodeFragment> leaves = body.getCompositeStatement().getLeaves();
					for(AbstractCodeFragment leaf : leaves) {
						for(AnonymousClassDeclarationObject anonymousObject : leaf.getAnonymousClassDeclarations()) {
							if(anonymousObject.getLocationInfo().equals(anonymousClassDeclaration.getLocationInfo()) ||
									anonymousObject.getLocationInfo().subsumes(anonymousClassDeclaration.getLocationInfo())) {
								return statementWithoutAnonymous(leaf, anonymousClassDeclaration, anonymousOperation);
							}
						}
					}
				}
			}
		}
		return null;
	}

	protected static boolean onlyDifferentInvoker(String s1, String s2,
			AbstractCall invocationCoveringTheEntireStatement1, AbstractCall invocationCoveringTheEntireStatement2) {
		if(invocationCoveringTheEntireStatement1.identicalName(invocationCoveringTheEntireStatement2)) {
			if(invocationCoveringTheEntireStatement1.getExpression() == null && invocationCoveringTheEntireStatement2.getExpression() != null) {
				int index = s1.indexOf(invocationCoveringTheEntireStatement1.getName());
				String s1AfterReplacement = s1.substring(0, index) + invocationCoveringTheEntireStatement2.getExpression() + "." + s1.substring(index);
				if(s1AfterReplacement.equals(s2)) {
					return true;
				}
			}
			else if(invocationCoveringTheEntireStatement1.getExpression() != null && invocationCoveringTheEntireStatement2.getExpression() == null) {
				int index = s2.indexOf(invocationCoveringTheEntireStatement2.getName());
				String s2AfterReplacement = s2.substring(0, index) + invocationCoveringTheEntireStatement1.getExpression() + "." + s2.substring(index);
				if(s2AfterReplacement.equals(s1)) {
					return true;
				}
			}
			else if(invocationCoveringTheEntireStatement1.getExpression() != null && invocationCoveringTheEntireStatement2.getExpression() != null) {
				String s1AfterReplacement = ReplacementUtil.performReplacement(s1, s2, invocationCoveringTheEntireStatement1.getExpression(), invocationCoveringTheEntireStatement2.getExpression());
				if(s1AfterReplacement.equals(s2)) {
					return true;
				}
			}
		}
		return false;
	}

	protected static boolean identicalAfterVariableAndTypeReplacements(String s1, String s2, Set<Replacement> replacements) {
		String s1AfterReplacements = new String(s1);
		for(Replacement replacement : replacements) {
			String before = replacement.getBefore();
			String after = replacement.getAfter();
			if(before.contains("\n") && after.contains("\n")) {
				before = before.substring(0, before.indexOf("\n"));
				after = after.substring(0, after.indexOf("\n"));
				if(before.endsWith("{") && after.endsWith("{")) {
					before = before.substring(0, before.length()-1);
					after = after.substring(0, after.length()-1);
				}
			}
			s1AfterReplacements = ReplacementUtil.performReplacement(s1AfterReplacements, s2, before, after);
		}
		if(s1AfterReplacements.equals(s2)) {
			return true;
		}
		return false;
	}

	protected static VariableDeclaration declarationWithArrayInitializer(List<VariableDeclaration> declarations) {
		for(VariableDeclaration declaration : declarations) {
			AbstractExpression initializer = declaration.getInitializer();
			if(initializer != null && initializer.getString().startsWith("{") && initializer.getString().endsWith("}")) {
				return declaration;
			}
		}
		return null;
	}
}
