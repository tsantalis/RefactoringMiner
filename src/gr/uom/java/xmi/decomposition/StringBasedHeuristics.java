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

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.LeafType;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCall.StatementCoverageType;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper.ReplacementInfo;
import gr.uom.java.xmi.decomposition.replacement.AddVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.CompositeReplacement;
import gr.uom.java.xmi.decomposition.replacement.IntersectionReplacement;
import gr.uom.java.xmi.decomposition.replacement.MergeVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.SplitVariableReplacement;
import gr.uom.java.xmi.decomposition.replacement.SwapArgumentReplacement;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation.Direction;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InvertConditionRefactoring;
import gr.uom.java.xmi.diff.MergeConditionalRefactoring;
import gr.uom.java.xmi.diff.SplitConditionalRefactoring;
import gr.uom.java.xmi.diff.StringDistance;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLOperationDiff;
import gr.uom.java.xmi.diff.UMLParameterDiff;

public class StringBasedHeuristics {
	protected static final Pattern SPLIT_CONDITIONAL_PATTERN = Pattern.compile("(\\|\\|)|(&&)|(\\?)|(:)");
	protected static final Pattern SPLIT_CONCAT_STRING_PATTERN = Pattern.compile("(\\s)*( \\+ )(\\s)*");
	protected static final Pattern SPLIT_COMMA_PATTERN = Pattern.compile("(\\s)*(\\,)(\\s)*");

	protected static boolean containsMethodSignatureOfAnonymousClass(String s) {
		String[] lines = s.split("\\n");
		if(s.contains(" -> ")) {
			if(lines.length > 1)
				return true;
			else if(lines.length == 1 && s.endsWith(";\n"))
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

	protected static boolean differOnlyInCastExpressionOrPrefixOperatorOrInfixOperand(String s1, String s2, Map<String, List<AbstractCall>> methodInvocationMap1, Map<String, List<AbstractCall>> methodInvocationMap2,
			List<LeafExpression> infixExpressions1, List<LeafExpression> infixExpressions2, List<VariableDeclaration> variableDeclarations1, List<VariableDeclaration> variableDeclarations2, ReplacementInfo info) {
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
			if(diff1.isEmpty() && (diff2.equals("+") || diff2.equals("-")) && commonSuffix.startsWith("=")) {
				return true;
			}
			else if(diff2.isEmpty() && (diff1.equals("+") || diff1.equals("-")) && commonSuffix.startsWith("=")) {
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
					for(LeafExpression infixExpression : infixExpressions2) {
						String infix = infixExpression.getString();
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
					for(LeafExpression infixExpression : infixExpressions1) {
						String infix = infixExpression.getString();
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
					if(invocation1.actualString().equals(diff1) && invocation1.arguments().contains(diff2) &&
							(invocation1.arguments().size() == 1 || (diff2.contains("?") && diff2.contains(":")))) {
						Replacement r = new VariableReplacementWithMethodInvocation(diff1, diff2, invocation1, Direction.INVOCATION_TO_VARIABLE);
						info.addReplacement(r);
						return true;
					}
				}
			}
			for(String key2 : methodInvocationMap2.keySet()) {
				for(AbstractCall invocation2 : methodInvocationMap2.get(key2)) {
					if(invocation2.actualString().equals(diff2) && invocation2.arguments().contains(diff1) &&
							(invocation2.arguments().size() == 1 || (diff1.contains("?") && diff1.contains(":")))) {
						Replacement r = new VariableReplacementWithMethodInvocation(diff1, diff2, invocation2, Direction.VARIABLE_TO_INVOCATION);
						info.addReplacement(r);
						return true;
					}
				}
			}
			for(LeafExpression infixExpression2 : infixExpressions2) {
				String infix = infixExpression2.getString();
				if(infix.equals(diff1) || infix.equals("(" + diff1) || infix.equals(diff1 + ")")) {
					for(Replacement r : info.getReplacements()) {
						if(diff1.contains(r.getAfter())) {
							return false;
						}
					}
					IntersectionReplacement r = new IntersectionReplacement(s1, s2, Set.of(infix), ReplacementType.CONCATENATION);
					info.getReplacements().add(r);
					return true;
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

	protected static boolean differOnlyInThrow(String s1, String s2) {
		return differOnlyInPrefix(s1, s2, "", "throw ");
	}

	protected static boolean differOnlyInFinalModifier(String s1, String s2, List<VariableDeclaration> variableDeclarations1, List<VariableDeclaration> variableDeclarations2, ReplacementInfo replacementInfo) {
		return differOnlyInPrefix(s1, s2, "for(", "for(final ") ||
				differOnlyInPrefix(s1, s2, "catch(", "catch(final ") ||
				catchDifferInFinalModifierAndExceptionName(s1, s2, variableDeclarations1, variableDeclarations2, replacementInfo);
	}

	private static boolean catchDifferInFinalModifierAndExceptionName(String s1, String s2, List<VariableDeclaration> variableDeclarations1, List<VariableDeclaration> variableDeclarations2, ReplacementInfo replacementInfo) {
		if(s1.startsWith("catch(") && s2.startsWith("catch(")) {
			if(variableDeclarations1.size() > 0 && variableDeclarations1.size() == variableDeclarations2.size()) {
				VariableDeclaration v1 = variableDeclarations1.get(0);
				VariableDeclaration v2 = variableDeclarations2.get(0);
				if(v1.getType().equals(v2.getType())) {
					if((s1.startsWith("catch(final ") && s2.startsWith("catch(")) || (s1.startsWith("catch(") && s2.startsWith("catch(final "))) {
						if(!v1.getVariableName().equals(v2.getVariableName())) {
							Replacement r = new Replacement(v1.getVariableName(), v2.getVariableName(), ReplacementType.VARIABLE_NAME);
							replacementInfo.addReplacement(r);
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	protected static boolean differOnlyInThis(String s1, String s2) {
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

	protected static boolean differOnlyInDefaultInitializer(String s1, String s2, List<VariableDeclaration> variableDeclarations1, List<VariableDeclaration> variableDeclarations2) {
		if(variableDeclarations1.size() > 0 && variableDeclarations1.toString().equals(variableDeclarations2.toString())) {
			StringBuilder tmpS1 = new StringBuilder();
			StringBuilder tmpS2 = new StringBuilder();
			int defaultInitializers = 0;
			for(int i=0; i<variableDeclarations1.size(); i++) {
				VariableDeclaration variableDeclaration1 = variableDeclarations1.get(i);
				tmpS1.append(variableDeclarationAsString(variableDeclaration1));
				VariableDeclaration variableDeclaration2 = variableDeclarations2.get(i);
				tmpS2.append(variableDeclarationAsString(variableDeclaration2));
				if(i < variableDeclarations1.size()-1) {
					tmpS1.append(", ");
					tmpS2.append(", ");
				}
				if(variableDeclaration1.getInitializer() == null && variableDeclaration2.getInitializer() != null &&
						(variableDeclaration2.getInitializer().getExpression().equals("null") ||
						variableDeclaration2.getInitializer().getExpression().equals("0") ||
						variableDeclaration2.getInitializer().getExpression().equals("false"))) {
					defaultInitializers++;
				}
				else if(variableDeclaration2.getInitializer() == null && variableDeclaration1.getInitializer() != null &&
						(variableDeclaration1.getInitializer().getExpression().equals("null") ||
						variableDeclaration1.getInitializer().getExpression().equals("0") ||
						variableDeclaration1.getInitializer().getExpression().equals("false"))) {
					defaultInitializers++;
				}
			}
			tmpS1.append(";\n");
			tmpS2.append(";\n");
			if(s1.equals(tmpS1.toString()) && s2.equals(tmpS2.toString()) && defaultInitializers == variableDeclarations1.size()) {
				return true;
			}
		}
		return false;
	}

	private static String variableDeclarationAsString(VariableDeclaration variableDeclaration) {
		StringBuilder sb = new StringBuilder();
		sb.append(variableDeclaration.getType());
		sb.append(" ");
		sb.append(variableDeclaration.getVariableName());
		if(variableDeclaration.getInitializer() != null) {
			sb.append("=").append(variableDeclaration.getInitializer());
		}
		return sb.toString();
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

	protected static boolean oneIsVariableDeclarationTheOtherIsReturnStatement(String s1, String s2, List<VariableDeclaration> variableDeclarations1, List<VariableDeclaration> variableDeclarations2) {
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
		if(variableDeclarations1.size() == 0 && variableDeclarations2.size() == 0 && commonSuffix.equals("0;\n")) {
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
		boolean mergeFound = false;
		for(Replacement replacement : replacementInfo.getReplacements()) {
			if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
				String key = replacement.getAfter();
				if(commonVariableReplacementMap.containsKey(key)) {
					commonVariableReplacementMap.get(key).add(replacement);
					int index = s1.indexOf(key);
					if(index != -1) {
						if(!s1.endsWith(key) && s1.charAt(index+key.length()) == ',') {
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
				if(s1.equals(s2)) {
					mergeFound = true;
				}
			}
			else if(replacement.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_THIS_EXPRESSION)) {
				String key = replacement.getAfter();
				int index = s1.indexOf(key);
				if(index != -1) {
					if(!s1.endsWith(key) && s1.charAt(index+key.length()) == ',') {
						s1 = s1.substring(0, index) + s1.substring(index+key.length()+1, s1.length());
						String reservedTokens1 = ReplacementUtil.keepReservedTokens(s1);
						String reservedTokens2 = ReplacementUtil.keepReservedTokens(s2);
						if(compatibleReservedTokens(reservedTokens1, reservedTokens2)) {
							Set<Replacement> replacements = new LinkedHashSet<Replacement>();
							replacements.add(replacement);
							commonVariableReplacementMap.put(key, replacements);
							if(s1.contains("=") && !s2.contains("=") && !s1.startsWith("return ") && s2.startsWith("return ")) {
								s1 = s1.substring(s1.indexOf("=") + 1);
								s2 = s2.substring(7);
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
								if(!diff1.isEmpty() && !diff2.isEmpty() && diff2.equals(key)) {
									Replacement r = new Replacement(diff1, diff2, replacement.getType());
									commonVariableReplacementMap.get(key).add(r);
									mergeFound = true;
								}
							}
						}
					}
					else if(index > 0 && s1.charAt(index-1) == ',') {
						s1 = s1.substring(0, index-1) + s1.substring(index+key.length(), s1.length());
						String reservedTokens1 = ReplacementUtil.keepReservedTokens(s1);
						String reservedTokens2 = ReplacementUtil.keepReservedTokens(s2);
						if(compatibleReservedTokens(reservedTokens1, reservedTokens2)) {
							Set<Replacement> replacements = new LinkedHashSet<Replacement>();
							replacements.add(replacement);
							commonVariableReplacementMap.put(key, replacements);
							if(s1.contains("=") && !s2.contains("=") && !s1.startsWith("return ") && s2.startsWith("return ")) {
								s1 = s1.substring(s1.indexOf("=") + 1);
								s2 = s2.substring(7);
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
								if(!diff1.isEmpty() && !diff2.isEmpty() && diff2.equals(key)) {
									Replacement r = new Replacement(diff1, diff2, replacement.getType());
									commonVariableReplacementMap.get(key).add(r);
									mergeFound = true;
								}
							}
						}
					}
				}
			}
		}
		if(mergeFound) {
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

	private static boolean compatibleReservedTokens(String reservedTokens1, String reservedTokens2) {
		if(reservedTokens1.equals(reservedTokens2)) {
			return true;
		}
		else if(reservedTokens1.contains("(") && reservedTokens1.contains("(")) {
			String s1 = reservedTokens1.substring(reservedTokens1.indexOf("("));
			String s2 = reservedTokens2.substring(reservedTokens2.indexOf("("));
			return s1.equals(s2);
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
			if(s1.contains(" + ") && s2.contains(" + ")) {
				Set<String> tokens1 = new LinkedHashSet<String>(Arrays.asList(SPLIT_CONCAT_STRING_PATTERN.split(s1)));
				Set<String> tokens2 = new LinkedHashSet<String>(Arrays.asList(SPLIT_CONCAT_STRING_PATTERN.split(s2)));
				Set<String> intersection = new LinkedHashSet<String>();
				for(String token1 : tokens1) {
					for(String token2 : tokens2) {
						if(token1.equals(token2)) {
							intersection.add(token1);
						}
						else if(token1.equals(token2 + ";\n")) {
							intersection.add(token2);
						}
						else if(token2.equals(token1 + ";\n")) {
							intersection.add(token1);
						}
					}
				}
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
			else if(s1.contains(" + ") && !s2.contains(" + ") && !s1.contains(",") && s2.contains(",")) {
				List<String> tokens1 = Arrays.asList(SPLIT_CONCAT_STRING_PATTERN.split(s1));
				List<String> tokens2 = Arrays.asList(SPLIT_COMMA_PATTERN.split(s2));
				List<String> commonTokens = new ArrayList<>();
				for(String token1 : tokens1) {
					if(tokens2.contains(token1)) {
						commonTokens.add(token1);
					}
				}
				Set<String> filteredIntersection = new LinkedHashSet<>();
				for(String common : commonTokens) {
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
				if(filteredIntersection.size() > 0) {
					IntersectionReplacement r = new IntersectionReplacement(s1, s2, filteredIntersection, ReplacementType.CONCATENATION);
					info.getReplacements().add(r);
					return true;
				}
			}
			else if(s1.contains(",") && s1.contains("(") && s1.contains(")") && !s2.contains(",") && !s2.contains("(") && !s2.contains(")")) {
				List<LeafExpression> variables1 = statement1.getVariables();
				List<LeafExpression> variables2 = statement2.getVariables();
				List<VariableDeclaration> variableDeclarations1 = statement1.getVariableDeclarations();
				List<VariableDeclaration> variableDeclarations2 = statement2.getVariableDeclarations();
				if((variableDeclarations1.size() > 0 && variableDeclarations2.size() > 0 &&
						variableDeclarations1.toString().equals(variableDeclarations2.toString())) ||
						(variables1.size() > 0 && variables2.size() > 0 &&
						statement1.getString().startsWith(variables1.get(0).getString() + "=") &&
						statement2.getString().startsWith(variables2.get(0).getString() + "=") &&
						variables1.get(0).getString().equals(variables2.get(0).getString()))) {
					List<String> tokens1 = Arrays.asList(SPLIT_COMMA_PATTERN.split(s1.substring(s1.indexOf("(") + 1, s1.lastIndexOf(")"))));
					int count = 0;
					for(String token1 : tokens1) {
						if(token1.startsWith("-")) {
							//add space between - and the string that follows
							token1 = "- " + token1.substring(1);
						}
						if(ReplacementUtil.contains(s2, token1)) {
							count++;
						}
					}
					if(count > 1 && count == tokens1.size()) {
						IntersectionReplacement r = new IntersectionReplacement(s1, s2, new LinkedHashSet<>(tokens1), ReplacementType.CONCATENATION);
						info.getReplacements().add(r);
						return true;
					}
				}
			}
			List<String> arguments1 = null;
			AbstractCall invocation1 = null;
			if(creationCoveringTheEntireStatement1 != null) {
				arguments1 = creationCoveringTheEntireStatement1.arguments();
			}
			else if((invocation1 = statement1.invocationCoveringEntireFragment()) != null) {
				arguments1 = invocation1.arguments();
			}
			else if((invocation1 = statement1.assignmentInvocationCoveringEntireStatement()) != null) {
				arguments1 = invocation1.arguments();
			}
			List<String> arguments2 = null;
			AbstractCall invocation2 = null;
			if(creationCoveringTheEntireStatement2 != null) {
				arguments2 = creationCoveringTheEntireStatement2.arguments();
			}
			else if((invocation2 = statement2.invocationCoveringEntireFragment()) != null) {
				arguments2 = invocation2.arguments();
			}
			else if((invocation2 = statement2.assignmentInvocationCoveringEntireStatement()) != null) {
				arguments2 = invocation2.arguments();
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
					else if(!arg1.contains(" + ") && arg2.contains(" + ")) {
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
							concatReplacements.add(new Replacement(arg1, arg2, ReplacementType.CONCATENATION));
							concatenatedArguments++;
						}
					}
					else if(!arg2.contains(" + ") && arg1.contains(" + ")) {
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
							concatReplacements.add(new Replacement(arg1, arg2, ReplacementType.CONCATENATION));
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

	protected static boolean equalAfterInfixExpressionExpansion(String s1, String s2, ReplacementInfo replacementInfo, List<LeafExpression> infixExpressions1) {
		Set<Replacement> replacementsToBeRemoved = new LinkedHashSet<Replacement>();
		Set<Replacement> replacementsToBeAdded = new LinkedHashSet<Replacement>();
		String originalArgumentizedString1 = replacementInfo.getArgumentizedString1();
		for(Replacement replacement : replacementInfo.getReplacements()) {
			String before = replacement.getBefore();
			for(LeafExpression infixExpression1 : infixExpressions1) {
				String infix = infixExpression1.getString();
				if(infix.startsWith(before)) {
					String suffix = infix.substring(before.length(), infix.length());
					String after = replacement.getAfter();
					if(s1.contains(after + suffix)) {
						String temp = ReplacementUtil.performReplacement(replacementInfo.getArgumentizedString1(), after + suffix, after);
						int distanceRaw = StringDistance.editDistance(temp, replacementInfo.getArgumentizedString2());
						if(distanceRaw >= 0 && distanceRaw < replacementInfo.getRawDistance()) {
							replacementsToBeRemoved.add(replacement);
							Replacement newReplacement = new Replacement(infix, after, ReplacementType.INFIX_EXPRESSION);
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
			List<String> arguments1 = invocationCoveringTheEntireStatement1.arguments();
			List<String> arguments2 = invocationCoveringTheEntireStatement2.arguments();
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
		if(containsMethodSignatureOfAnonymousClass(string1) && string1.contains("\n")) {
			string1 = string1.substring(0, string1.indexOf("\n"));
		}
		if(containsMethodSignatureOfAnonymousClass(string2) && string2.contains("\n")) {
			string2 = string2.substring(0, string2.indexOf("\n"));
		}
		if(string1.contains("=") && string1.endsWith(";\n") && string2.startsWith("return ") && string2.endsWith(";\n")) {
			boolean typeReplacement = false, compatibleTypes = false, classInstanceCreationReplacement = false;
			String assignment1 = string1.substring(string1.indexOf("=")+1, string1.lastIndexOf(";\n"));
			String assignment2 = string2.substring(7, string2.lastIndexOf(";\n"));
			UMLType type1 = null, type2 = null;
			ObjectCreation objectCreation1 = null, objectCreation2 = null;
			Map<String, String> argumentToParameterMap = new LinkedHashMap<String, String>();
			List<AbstractCall> creations1 = statement1.getCreations();
			for(AbstractCall creation1 : creations1) {
				if(creation1.getString().equals(assignment1)) {
					objectCreation1 = (ObjectCreation)creation1;
					type1 = objectCreation1.getType();
				}
			}
			List<AbstractCall> creations2 = statement2.getCreations();
			for(AbstractCall creation2 : creations2) {
				if(creation2.getString().equals(assignment2)) {
					objectCreation2 = (ObjectCreation)creation2;
					type2 = objectCreation2.getType();
					for(String argument : objectCreation2.arguments()) {
						if(parameterToArgumentMap.containsKey(argument)) {
							argumentToParameterMap.put(parameterToArgumentMap.get(argument), argument);
						}
					}
				}
			}
			int minArguments = 0;
			if(type1 != null && type2 != null) {
				compatibleTypes = type1.compatibleTypes(type2);
				minArguments = Math.min(objectCreation1.arguments().size(), objectCreation2.arguments().size());
			}
			int replacedArguments = 0;
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE)) {
					typeReplacement = true;
					if(string1.contains("new " + replacement.getBefore() + "(") && string2.contains("new " + replacement.getAfter() + "("))
						classInstanceCreationReplacement = true;
				}
				else if(objectCreation1 != null && objectCreation2 != null &&
						objectCreation1.arguments().contains(replacement.getBefore()) &&
						(objectCreation2.arguments().contains(replacement.getAfter()) || objectCreation2.arguments().contains(argumentToParameterMap.get(replacement.getAfter())))) {
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
			List<AbstractCall> creations1 = statement1.getCreations();
			for(AbstractCall creation1 : creations1) {
				if(creation1.getString().equals(assignment1)) {
					objectCreation1 = (ObjectCreation)creation1;
					type1 = objectCreation1.getType();
				}
			}
			List<AbstractCall> creations2 = statement2.getCreations();
			for(AbstractCall creation2 : creations2) {
				if(creation2.getString().equals(assignment2)) {
					objectCreation2 = (ObjectCreation)creation2;
					type2 = objectCreation2.getType();
					for(String argument : objectCreation2.arguments()) {
						if(parameterToArgumentMap.containsKey(argument)) {
							argumentToParameterMap.put(parameterToArgumentMap.get(argument), argument);
						}
					}
				}
			}
			int minArguments = 0;
			if(type1 != null && type2 != null) {
				compatibleTypes = type1.compatibleTypes(type2);
				minArguments = Math.min(objectCreation1.arguments().size(), objectCreation2.arguments().size());
			}
			int replacedArguments = 0;
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE)) {
					typeReplacement = true;
					if(string1.contains("new " + replacement.getBefore() + "(") && string2.contains("new " + replacement.getAfter() + "("))
						classInstanceCreationReplacement = true;
				}
				else if(objectCreation1 != null && objectCreation2 != null &&
						objectCreation1.arguments().contains(replacement.getBefore()) &&
						(objectCreation2.arguments().contains(replacement.getAfter()) || objectCreation2.arguments().contains(argumentToParameterMap.get(replacement.getAfter())))) {
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
			ReplacementInfo replacementInfo, UMLOperationBodyMapper mapper) {
		String string1 = statement1.getString();
		String string2 = statement2.getString();
		if(containsMethodSignatureOfAnonymousClass(string1) && string1.contains("\n")) {
			string1 = string1.substring(0, string1.indexOf("\n"));
		}
		if(containsMethodSignatureOfAnonymousClass(string2) && string2.contains("\n")) {
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
			List<AbstractCall> creations1 = statement1.getCreations();
			for(AbstractCall creation1 : creations1) {
				if(creation1.getString().equals(assignment1)) {
					ObjectCreation objectCreation = (ObjectCreation)creation1;
					type1 = objectCreation.getType();
					inv1 = objectCreation;
				}
			}
			List<AbstractCall> creations2 = statement2.getCreations();
			for(AbstractCall creation2 : creations2) {
				if(creation2.getString().equals(assignment2)) {
					ObjectCreation objectCreation = (ObjectCreation)creation2;
					type2 = objectCreation.getType();
					inv2 = objectCreation;
				}
			}
			if(type1 != null && type2 != null) {
				compatibleTypes = type1.compatibleTypes(type2);
			}
			for(AbstractCall invocation1 : statement1.getMethodInvocations()) {
				if(invocation1.getString().equals(assignment1)) {
					inv1 = invocation1;
				}
			}
			for(AbstractCall invocation2 : statement2.getMethodInvocations()) {
				if(invocation2.getString().equals(assignment2)) {
					inv2 = invocation2;
				}
			}
			for(Replacement replacement : replacementInfo.getReplacements()) {
				if(replacement.getType().equals(ReplacementType.TYPE)) {
					typeReplacement = true;
					if(string1.contains("new " + replacement.getBefore() + "(") && string2.contains("new " + replacement.getAfter() + "("))
						classInstanceCreationReplacement = true;
				}
				else if((replacement.getType().equals(ReplacementType.VARIABLE_NAME) || replacement.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_ARRAY_ACCESS)) &&
						(variableName1.equals(replacement.getBefore()) || variableName1.endsWith(" " + replacement.getBefore()) || variableName1.equals("this." + replacement.getBefore())) &&
						(variableName2.equals(replacement.getAfter()) || variableName2.endsWith(" " + replacement.getAfter()) || variableName2.equals("this." + replacement.getAfter())) &&
						!variableName1.equals("this." + variableName2) && !variableName2.equals("this." + variableName1))
					variableRename = true;
				else if(replacement.getType().equals(ReplacementType.VARIABLE_NAME) &&
						assignment1.equals(replacement.getBefore()) && assignment2.equals(replacement.getAfter()) &&
						!mapper.getContainer1().isConstructor() && !mapper.getContainer2().isConstructor() && !mapper.getContainer1().getName().equals(mapper.getContainer2().getName()) &&
						zeroCallsToExtractedMethodsOrParentMapperWithNonIdenticalSignature(mapper))
					rightHandSideReplacement = true;
				else if(replacement.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_NULL_LITERAL) &&
						assignment1.equals(replacement.getBefore()) &&
						assignment2.equals(replacement.getAfter()))
					rightHandSideReplacement = true;
				else if(replacement.getType().equals(ReplacementType.VARIABLE_REPLACED_WITH_CLASS_INSTANCE_CREATION) &&
						assignment1.equals(replacement.getBefore()) &&
						assignment2.equals(replacement.getAfter()) &&
						!referencedParameterForClassInstanceCreationReplacement(replacement, mapper.getContainer1(), mapper.getContainer2()))
					rightHandSideReplacement = true;
				else if(replacement instanceof VariableReplacementWithMethodInvocation &&
						assignment1.equals(replacement.getBefore()) &&
						assignment2.equals(replacement.getAfter()) &&
						!assignment1.startsWith(assignment2) && !assignment2.startsWith(assignment1) &&
						!referencedParameter((VariableReplacementWithMethodInvocation)replacement, mapper.getContainer1(), mapper.getContainer2()))
					rightHandSideReplacement = true;
				else if(replacement.getType().equals(ReplacementType.CLASS_INSTANCE_CREATION) &&
						assignment1.equals(replacement.getBefore()) &&
						assignment2.equals(replacement.getAfter()))
					classInstanceCreationReplacement = true;
			}
			if(inv1 != null && inv2 != null) {
				equalArguments = inv1.equalArguments(inv2) && inv1.arguments().size() > 0;
			}
			if(typeReplacement && !compatibleTypes && variableRename && classInstanceCreationReplacement && !equalArguments) {
				return true;
			}
			if(variableRename && rightHandSideReplacement) {
				String[] tokens1 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(variableName1);
				String[] tokens2 = LeafType.CAMEL_CASE_SPLIT_PATTERN.split(variableName2);
				int commonTokens = 0;
				for(String token1 : tokens1) {
					for(String token2 : tokens2) {
						if(token1.equals(token2)) {
							commonTokens++;
						}
					}
				}
				if(commonTokens < Math.max(tokens1.length, tokens2.length)/2.0)
					return true;
			}
			if(variableRename && inv1 != null && inv2 != null && inv1.differentExpressionNameAndArguments(inv2)) {
				if(inv1.arguments().size() > inv2.arguments().size()) {
					for(String argument : inv1.arguments()) {
						for(AbstractCall invocation1 : statement1.getMethodInvocations()) {
							if(invocation1.getString().equals(argument) && !invocation1.differentExpressionNameAndArguments(inv2)) {
								return false;
							}
						}
					}
				}
				else if(inv1.arguments().size() < inv2.arguments().size()) {
					for(String argument : inv2.arguments()) {
						for(AbstractCall invocation2 : statement2.getMethodInvocations()) {
							if(invocation2.getString().equals(argument) && !inv1.differentExpressionNameAndArguments(invocation2)) {
								return false;
							}
						}
					}
				}
				return true;
			}
		}
		return false;
	}

	private static boolean zeroCallsToExtractedMethodsOrParentMapperWithNonIdenticalSignature(UMLOperationBodyMapper mapper) {
		if(mapper.getCallsToExtractedMethod() == 0) {
			return true;
		}
		else if(mapper.getParentMapper() != null) {
			return !mapper.getParentMapper().getContainer1().equals(mapper.getParentMapper().getContainer2());
		}
		return false;
	}

	private static boolean referencedParameterForClassInstanceCreationReplacement(Replacement replacement, VariableDeclarationContainer container1, VariableDeclarationContainer container2) {
		int index1 = -1;
		UMLType type1 = null;
		int index2 = -1;
		UMLType type2 = null;
		if(replacement.getAfter().contains("new ")) {
			int counter = 0;
			for(VariableDeclaration parameter : container1.getParameterDeclarationList()) {
				if(parameter.getVariableName().equals(replacement.getBefore())) {
					index1 = counter;
					type1 = parameter.getType();
				}
				counter++;
			}
			counter = 0;
			for(VariableDeclaration parameter : container2.getParameterDeclarationList()) {
				if(replacement.getAfter().contains(parameter.getVariableName())) {
					index2 = counter;
					type2 = parameter.getType();
				}
				counter++;
			}
		}
		else if(replacement.getBefore().contains("new ")) {
			int counter = 0;
			for(VariableDeclaration parameter : container1.getParameterDeclarationList()) {
				if(replacement.getBefore().contains(parameter.getVariableName())) {
					index1 = counter;
					type1 = parameter.getType();
				}
				counter++;
			}
			counter = 0;
			for(VariableDeclaration parameter : container2.getParameterDeclarationList()) {
				if(parameter.getVariableName().equals(replacement.getAfter())) {
					index2 = counter;
					type2 = parameter.getType();
				}
				counter++;
			}
		}
		if(type1 != null && type2 != null && type1.equals(type2) && index1 == index2) {
			return true;
		}
		return false;
	}

	private static boolean referencedParameter(VariableReplacementWithMethodInvocation replacement, VariableDeclarationContainer container1, VariableDeclarationContainer container2) {
		int index1 = -1;
		UMLType type1 = null;
		int index2 = -1;
		UMLType type2 = null;
		if(replacement.getDirection().equals(Direction.VARIABLE_TO_INVOCATION)) {
			int counter = 0;
			for(VariableDeclaration parameter : container1.getParameterDeclarationList()) {
				if(parameter.getVariableName().equals(replacement.getBefore())) {
					index1 = counter;
					type1 = parameter.getType();
				}
				counter++;
			}
			counter = 0;
			for(VariableDeclaration parameter : container2.getParameterDeclarationList()) {
				if(replacement.getAfter().contains(parameter.getVariableName())) {
					index2 = counter;
					type2 = parameter.getType();
				}
				counter++;
			}
		}
		else if(replacement.getDirection().equals(Direction.INVOCATION_TO_VARIABLE)) {
			int counter = 0;
			for(VariableDeclaration parameter : container1.getParameterDeclarationList()) {
				if(replacement.getBefore().contains(parameter.getVariableName())) {
					index1 = counter;
					type1 = parameter.getType();
				}
				counter++;
			}
			counter = 0;
			for(VariableDeclaration parameter : container2.getParameterDeclarationList()) {
				if(parameter.getVariableName().equals(replacement.getAfter())) {
					index2 = counter;
					type2 = parameter.getType();
				}
				counter++;
			}
		}
		if(type1 != null && type2 != null && type1.equals(type2) && index1 == index2) {
			return true;
		}
		return false;
	}

	protected static boolean operatorExpressionWithEverythingReplaced(AbstractCodeFragment statement1, AbstractCodeFragment statement2,
			ReplacementInfo replacementInfo, Map<String, String> parameterToArgumentMap) {
		String string1 = statement1.getString();
		String string2 = statement2.getString();
		if(containsMethodSignatureOfAnonymousClass(string1) && string1.contains("\n")) {
			string1 = string1.substring(0, string1.indexOf("\n"));
		}
		if(containsMethodSignatureOfAnonymousClass(string2) && string2.contains("\n")) {
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
				if(initializer1.getCreations().size() == 1 && initializer2.getCreations().size() == 1) {
					ObjectCreation creation1 = (ObjectCreation) initializer1.getCreations().get(0);
					ObjectCreation creation2 = (ObjectCreation) initializer2.getCreations().get(0);
					if(creation1.arguments().size() == 0 && creation2.arguments().size() == 0) {
						zeroArgumentClassInstantiation = true;
					}
					else if(creation1.arguments().size() == 1 && creation2.arguments().size() == 1) {
						String argument1 = creation1.arguments().get(0);
						String argument2 = creation2.arguments().get(0);
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

	protected static Set<String> subConditionIntersection(List<String> subConditionsAsList1, List<String> subConditionsAsList2) {
		Set<String> intersection = new LinkedHashSet<String>();
		for(String c1 : subConditionsAsList1) {
			for(String c2 : subConditionsAsList2) {
				if(c1.equals(c2)) {
					intersection.add(c1);
					break;
				}
				else if(c1.equals("(" + c2)) {
					intersection.add(c2);
					break;
				}
				else if(c1.equals(c2 + ")")) {
					intersection.add(c2);
					break;
				}
				else if(c1.equals("!" + c2) || c1.equals("!(" + c2 + ")")) {
					intersection.add(c2);
					break;
				}
				else if(c2.equals("(" + c1)) {
					intersection.add(c1);
					break;
				}
				else if(c2.equals(c1 + ")")) {
					intersection.add(c1);
					break;
				}
				else if(c2.equals("!" + c1) || c2.equals("!(" + c1 + ")")) {
					intersection.add(c1);
					break;
				}
				else if(c1.contains("!=") && c2.contains("==")) {
					String prefix1 = c1.substring(0, c1.indexOf("!="));
					String prefix2 = c2.substring(0, c2.indexOf("=="));
					String suffix1 = c1.substring(c1.indexOf("!=")+2);
					String suffix2 = c2.substring(c2.indexOf("==")+2);
					if(prefix1.equals(prefix2) && suffix1.equals(suffix2)) {
						intersection.add(c1);
						break;
					}
				}
				else if(c1.contains("==") && c2.contains("!=")) {
					String prefix1 = c1.substring(0, c1.indexOf("=="));
					String prefix2 = c2.substring(0, c2.indexOf("!="));
					String suffix1 = c1.substring(c1.indexOf("==")+2);
					String suffix2 = c2.substring(c2.indexOf("!=")+2);
					if(prefix1.equals(prefix2) && suffix1.equals(suffix2)) {
						intersection.add(c1);
						break;
					}
				}
			}
		}
		return intersection;
	}

	private static int checkForInvertedConditionals(List<String> subConditionsAsList1, List<String> subConditionsAsList2, ReplacementInfo info) {
		int invertedConditionals = 0;
		for(String subCondition1 : subConditionsAsList1) {
			for(String subCondition2 : subConditionsAsList2) {
				if(subCondition1.equals("!" + subCondition2) || subCondition1.equals("!(" + subCondition2 + ")")) {
					Replacement r2 = new Replacement(subCondition1, subCondition2, ReplacementType.INVERT_CONDITIONAL);
					info.addReplacement(r2);
					invertedConditionals++;
					break;
				}
				if(subCondition2.equals("!" + subCondition1) || subCondition2.equals("!(" + subCondition1 + ")")) {
					Replacement r2 = new Replacement(subCondition1, subCondition2, ReplacementType.INVERT_CONDITIONAL);
					info.addReplacement(r2);
					invertedConditionals++;
					break;
				}
				if(subCondition1.contains("==") && subCondition2.contains("!=")) {
					String prefix1 = subCondition1.substring(0, subCondition1.indexOf("==")).trim();
					String prefix2 = subCondition2.substring(0, subCondition2.indexOf("!=")).trim();
					String suffix1 = subCondition1.substring(subCondition1.indexOf("==")+2).trim();
					String suffix2 = subCondition2.substring(subCondition2.indexOf("!=")+2).trim();
					boolean prefixReplaced = false;
					boolean suffixReplaced = false;
					for(Replacement r : info.getReplacements()) {
						if(!r.getBefore().equals(r.getAfter())) {
							if(r.getAfter().equals(prefix1) && r.getAfter().equals(prefix2)) {
								prefixReplaced = true;
							}
							if(r.getAfter().equals(suffix1) && r.getAfter().equals(suffix2)) {
								suffixReplaced = true;
							}
						}
					}
					boolean bothPrefixAndSuffixReplaced = prefixReplaced && suffixReplaced;
					if(prefix1.equals(prefix2) && suffix1.equals(suffix2) && !bothPrefixAndSuffixReplaced) {
						Replacement r2 = new Replacement(subCondition1, subCondition2, ReplacementType.INVERT_CONDITIONAL);
						info.addReplacement(r2);
						invertedConditionals++;
						break;
					}
				}
				else if(subCondition1.contains("!=") && subCondition2.contains("==")) {
					String prefix1 = subCondition1.substring(0, subCondition1.indexOf("!=")).trim();
					String prefix2 = subCondition2.substring(0, subCondition2.indexOf("==")).trim();
					String suffix1 = subCondition1.substring(subCondition1.indexOf("!=")+2).trim();
					String suffix2 = subCondition2.substring(subCondition2.indexOf("==")+2).trim();
					boolean prefixReplaced = false;
					boolean suffixReplaced = false;
					for(Replacement r : info.getReplacements()) {
						if(!r.getBefore().equals(r.getAfter())) {
							if(r.getAfter().equals(prefix1) && r.getAfter().equals(prefix2)) {
								prefixReplaced = true;
							}
							if(r.getAfter().equals(suffix1) && r.getAfter().equals(suffix2)) {
								suffixReplaced = true;
							}
						}
					}
					boolean bothPrefixAndSuffixReplaced = prefixReplaced && suffixReplaced;
					if(prefix1.equals(prefix2) && suffix1.equals(suffix2) && !bothPrefixAndSuffixReplaced) {
						Replacement r2 = new Replacement(subCondition1, subCondition2, ReplacementType.INVERT_CONDITIONAL);
						info.addReplacement(r2);
						invertedConditionals++;
						break;
					}
				}
			}
		}
		return invertedConditionals;
	}

	private static String prepareConditional(String s) {
		String conditional = s;
		if(s.startsWith("if(") && s.endsWith(")")) {
			conditional = s.substring(3, s.length()-1);
		}
		if(s.startsWith("do(") && s.endsWith(")")) {
			conditional = s.substring(3, s.length()-1);
		}
		if(s.startsWith("while(") && s.endsWith(")")) {
			conditional = s.substring(6, s.length()-1);
		}
		if(s.startsWith("return ") && s.endsWith(";\n")) {
			conditional = s.substring(7, s.length()-2);
		}
		int indexOfEquals = s.indexOf("=");
		if(indexOfEquals > -1 && s.charAt(indexOfEquals+1) != '=' && s.charAt(indexOfEquals-1) != '!' && s.endsWith(";\n")) {
			conditional = s.substring(indexOfEquals+1, s.length()-2);
		}
		return conditional;
	}

	protected static boolean commonConditional(String s1, String s2, Map<String, String> parameterToArgumentMap, ReplacementInfo info, AbstractCodeFragment statement1, AbstractCodeFragment statement2, UMLOperationBodyMapper mapper) {
		Set<Refactoring> refactorings = mapper.getRefactoringsAfterPostProcessing();
		VariableDeclarationContainer container1 = mapper.getContainer1();
		VariableDeclarationContainer container2 = mapper.getContainer2();
		Set<AbstractCodeMapping> mappings = mapper.getMappings();
		UMLOperationBodyMapper parentMapper = mapper.getParentMapper();
		ObjectCreation creationCoveringTheEntireStatement1 = statement1.creationCoveringEntireFragment();
		ObjectCreation creationCoveringTheEntireStatement2 = statement2.creationCoveringEntireFragment();
		boolean arrayCreation1 = creationCoveringTheEntireStatement1 != null && creationCoveringTheEntireStatement1.isArray();
		boolean arrayCreation2 = creationCoveringTheEntireStatement2 != null && creationCoveringTheEntireStatement2.isArray();
		if(!arrayCreation1 && !arrayCreation2 && !containsMethodSignatureOfAnonymousClass(s1) && !containsMethodSignatureOfAnonymousClass(s2)) {
			List<String> ternaryConditionals1 = new ArrayList<>();
			for(TernaryOperatorExpression ternary : statement1.getTernaryOperatorExpressions()) {
				String condition = ternary.getCondition().getString();
				String temp0 = new String(condition);
				for(Replacement replacement : info.getReplacements()) {
					temp0 = ReplacementUtil.performReplacement(temp0, replacement.getBefore(), replacement.getAfter());
				}
				ternaryConditionals1.add(temp0);
				String thenExpression = ternary.getThenExpression().getString();
				String temp1 = new String(thenExpression);
				for(Replacement replacement : info.getReplacements()) {
					temp1 = ReplacementUtil.performReplacement(temp1, replacement.getBefore(), replacement.getAfter());
				}
				ternaryConditionals1.add(temp1);
				String elseExpression = ternary.getElseExpression().getString();
				String temp2 = new String(elseExpression);
				for(Replacement replacement : info.getReplacements()) {
					temp2 = ReplacementUtil.performReplacement(temp2, replacement.getBefore(), replacement.getAfter());
				}
				ternaryConditionals1.add(temp2);
			}
			List<String> ternaryConditionals2 = new ArrayList<>();
			for(TernaryOperatorExpression ternary : statement2.getTernaryOperatorExpressions()) {
				ternaryConditionals2.add(ternary.getCondition().getString());
				ternaryConditionals2.add(ternary.getThenExpression().getString());
				ternaryConditionals2.add(ternary.getElseExpression().getString());
			}
			boolean containsTernaryOperatorReplacement = false;
			for(Replacement replacement : info.getReplacements()) {
				if(replacement.getAfter().contains("?") && replacement.getAfter().contains(":")) {
					containsTernaryOperatorReplacement = true;
				}
			}
			boolean ternaryConditions = !containsTernaryOperatorReplacement && ternaryConditionals1.isEmpty() != ternaryConditionals2.isEmpty() &&
					(statement1.getLocationInfo().getCodeElementType().equals(statement2.getLocationInfo().getCodeElementType()) || statement1 instanceof AbstractExpression);
			boolean containLogicalOperator = s1.contains("||") || s1.contains("&&") || s2.contains("||") || s2.contains("&&");
			boolean containsNotOperator = s1.contains("!") != s2.contains("!");
			if(containLogicalOperator || ternaryConditions || containsNotOperator) {
				List<String> subConditionsAsList1 = new ArrayList<String>();
				List<String> subConditionsAsList2 = new ArrayList<String>();
				Map<String, LeafExpression> subConditionMap1 = new LinkedHashMap<>();
				Map<String, LeafExpression> subConditionMap2 = new LinkedHashMap<>();
				Map<String, LeafExpression> subConditionMap = null;
				if(ternaryConditions && (!containLogicalOperator || statement1 instanceof AbstractExpression)) {
					if(ternaryConditionals1.isEmpty() && ternaryConditionals2.size() > 0) {
						String conditional1 = prepareConditional(s1);
						String[] subConditions1 = SPLIT_CONDITIONAL_PATTERN.split(conditional1);
						for(String s : subConditions1) {
							String trimmed = s.trim();
							subConditionsAsList1.add(trimmed);
							List<LeafExpression> leafExpressions = statement1.findExpression(trimmed);
							if(leafExpressions.size() > 0) {
								subConditionMap1.put(trimmed, leafExpressions.get(0));
							}
						}
						for(String ternaryConditional : ternaryConditionals2) {
							String conditional2 = prepareConditional(ternaryConditional);
							String[] subConditions2 = SPLIT_CONDITIONAL_PATTERN.split(conditional2);
							for(String s : subConditions2) {
								String trimmed = s.trim();
								subConditionsAsList2.add(trimmed);
								List<LeafExpression> leafExpressions = statement2.findExpression(trimmed);
								if(leafExpressions.size() > 0) {
									subConditionMap2.put(trimmed, leafExpressions.get(0));
								}
							}
						}
					}
					else if(ternaryConditionals2.isEmpty() && ternaryConditionals1.size() > 0) {
						for(String ternaryConditional : ternaryConditionals1) {
							String conditional1 = prepareConditional(ternaryConditional);
							String[] subConditions1 = SPLIT_CONDITIONAL_PATTERN.split(conditional1);
							for(String s : subConditions1) {
								String trimmed = s.trim();
								subConditionsAsList1.add(trimmed);
								List<LeafExpression> leafExpressions = statement1.findExpression(trimmed);
								if(leafExpressions.size() > 0) {
									subConditionMap1.put(trimmed, leafExpressions.get(0));
								}
							}
						}
						String conditional2 = prepareConditional(s2);
						String[] subConditions2 = SPLIT_CONDITIONAL_PATTERN.split(conditional2);
						for(String s : subConditions2) {
							String trimmed = s.trim();
							subConditionsAsList2.add(trimmed);
							List<LeafExpression> leafExpressions = statement2.findExpression(trimmed);
							if(leafExpressions.size() > 0) {
								subConditionMap2.put(trimmed, leafExpressions.get(0));
							}
						}
					}
				}
				else {
					String conditional1 = prepareConditional(s1);
					String conditional2 = prepareConditional(s2);
					String[] subConditions1 = SPLIT_CONDITIONAL_PATTERN.split(conditional1);
					String[] subConditions2 = SPLIT_CONDITIONAL_PATTERN.split(conditional2);
					for(String s : subConditions1) {
						String trimmed = s.trim();
						subConditionsAsList1.add(trimmed);
						List<LeafExpression> leafExpressions = statement1.findExpression(trimmed);
						if(leafExpressions.size() > 0) {
							subConditionMap1.put(trimmed, leafExpressions.get(0));
						}
					}
					for(String s : subConditions2) {
						String trimmed = s.trim();
						subConditionsAsList2.add(trimmed);
						List<LeafExpression> leafExpressions = statement2.findExpression(trimmed);
						if(leafExpressions.size() > 0) {
							subConditionMap2.put(trimmed, leafExpressions.get(0));
						}
					}
				}
				Set<String> intersection = subConditionIntersection(subConditionsAsList1, subConditionsAsList2);
				Set<String> intersection2 = null;
				int matches = matchCount(intersection, info);
				boolean pass = pass(subConditionsAsList1, subConditionsAsList2, intersection, matches);
				int invertedConditionals = 0;
				if(pass && info.getReplacements(ReplacementType.TYPE).isEmpty() && info.getReplacements(ReplacementType.METHOD_INVOCATION).isEmpty()) {
					IntersectionReplacement r = new IntersectionReplacement(s1, s2, intersection, ReplacementType.CONDITIONAL);
					info.addReplacement(r);
					CompositeStatementObject root1 = statement1.getParent();
					CompositeStatementObject root2 = statement2.getParent();
					Set<CompositeStatementObject> ifNodes1 = new LinkedHashSet<>(), ifNodes2 = new LinkedHashSet<>();
					if(root1 != null && root1.getParent() != null && root1.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && !mapper.alreadyMatched1(root1.getParent())) {
						ifNodes1.add(root1.getParent());
						if(hasElseIfBranch(root1.getParent())) {
							ifNodes1.add((CompositeStatementObject)root1.getParent().getStatements().get(1));
						}
					}
					if(root2 != null && root2.getParent() != null && root2.getParent().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && !mapper.alreadyMatched2(root2.getParent())) {
						ifNodes2.add(root2.getParent());
						if(hasElseIfBranch(root2.getParent())) {
							ifNodes2.add((CompositeStatementObject)root2.getParent().getStatements().get(1));
						}
					}
					if(root1 != null && root1.getParent() == null && statement1 instanceof CompositeStatementObject && root2 != null && root2.getParent() == null && statement2 instanceof CompositeStatementObject) {
						root1 = (CompositeStatementObject)statement1;
						root2 = (CompositeStatementObject)statement2;
					}
					if(root1 != null && root2 != null) {
						for(CompositeStatementObject innerNode : root1.getInnerNodes()) {
							if(innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && !mapper.alreadyMatched1(innerNode)) {
								ifNodes1.add(innerNode);
							}
						}
						if(root1.getParent() != null && parentMapper == null) {
							for(CompositeStatementObject innerNode : root1.getParent().getInnerNodes()) {
								if(innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && !mapper.alreadyMatched1(innerNode)) {
									ifNodes1.add(innerNode);
								}
							}
						}
						for(CompositeStatementObject innerNode : root2.getInnerNodes()) {
							if(innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && !mapper.alreadyMatched2(innerNode)) {
								ifNodes2.add(innerNode);
							}
						}
						if(root2.getParent() != null && parentMapper == null) {
							for(CompositeStatementObject innerNode : root2.getParent().getInnerNodes()) {
								if(innerNode.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && !mapper.alreadyMatched2(innerNode)) {
									ifNodes2.add(innerNode);
								}
							}
						}
					}
					if(ifNodes1.size() < ifNodes2.size()) {
						boolean splitConditional = false;
						for(CompositeStatementObject ifNode2 : ifNodes2) {
							List<AbstractExpression> expressions2 = ifNode2.getExpressions();
							if(expressions2.size() > 0 && !statement2.equals(ifNode2) && !ifNode2.getExpressions().contains(statement2) && !containsIdenticalIfNode(ifNodes1, ifNode2) && sequentiallySplitConditional(statement1, ifNode2, statement2, mappings)) {
								AbstractExpression ifExpression2 = expressions2.get(0);
								String conditional = ifExpression2.getString();
								String[] subConditions = SPLIT_CONDITIONAL_PATTERN.split(conditional);
								List<String> subConditionsAsList = new ArrayList<String>();
								subConditionMap = new LinkedHashMap<>();
								for(String s : subConditions) {
									String trimmed = s.trim();
									subConditionsAsList.add(trimmed);
									List<LeafExpression> leafExpressions = ifNode2.findExpression(trimmed);
									if(leafExpressions.size() > 0) {
										subConditionMap.put(trimmed, leafExpressions.get(0));
									}
								}
								intersection2 = subConditionIntersection(subConditionsAsList1, subConditionsAsList);
								int matches2 = matchCount(intersection2, info);
								boolean pass2 = pass(subConditionsAsList1, subConditionsAsList, intersection2, matches2);
								if(pass2 && !intersection.containsAll(intersection2)) {
									Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
									additionallyMatchedStatements2.add(ifNode2);
									CompositeReplacement composite = new CompositeReplacement(statement1.getString(), ifNode2.getString(), new LinkedHashSet<>(), additionallyMatchedStatements2);
									info.addReplacement(composite);
									splitConditional = true;
									invertedConditionals = checkForInvertedConditionals(subConditionsAsList1, subConditionsAsList, info);
								}
								else if(statement1 instanceof CompositeStatementObject) {
									CompositeStatementObject composite1 = (CompositeStatementObject)statement1;
									for(AbstractExpression expression : composite1.getExpressions()) {
										String originalConditional1 = prepareConditional(expression.getString());
										String[] originalSubConditions1 = SPLIT_CONDITIONAL_PATTERN.split(originalConditional1);
										List<String> originalSubConditionsAsList1 = new ArrayList<String>();
										for(String s : originalSubConditions1) {
											originalSubConditionsAsList1.add(s.trim());
										}
										for(String commonElement : r.getCommonElements()) {
											originalSubConditionsAsList1.remove(commonElement);
										}
										for(String subCondition1 : originalSubConditionsAsList1) {
											for(String subCondition2 : subConditionsAsList) {
												if(subCondition1.equals(subCondition2)) {
													Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
													additionallyMatchedStatements2.add(ifNode2);
													CompositeReplacement composite = new CompositeReplacement(statement1.getString(), ifNode2.getString(), new LinkedHashSet<>(), additionallyMatchedStatements2);
													info.addReplacement(composite);
													splitConditional = true;
												}
												if((subCondition1.endsWith(" != null") && subCondition2.endsWith(" != null")) ||
														(subCondition1.endsWith(" == null") && subCondition2.endsWith(" == null"))) {
													String prefix1 = subCondition1.substring(0, subCondition1.length() - 8);
													String prefix2 = subCondition2.substring(0, subCondition2.length() - 8);
													for(AbstractCodeMapping mapping : mappings) {
														VariableDeclaration variableDeclaration1 = mapping.getFragment1().getVariableDeclaration(prefix1);
														VariableDeclaration variableDeclaration2 = mapping.getFragment2().getVariableDeclaration(prefix1);
														if(variableDeclaration1 != null && variableDeclaration2 != null) {
															boolean relatedVariables = false;
															if(variableDeclaration1.getInitializer() != null && variableDeclaration1.getInitializer().getExpression().startsWith(prefix2 + ".")) {
																relatedVariables = true;
															}
															if(variableDeclaration2.getInitializer() != null && variableDeclaration2.getInitializer().getExpression().startsWith(prefix2 + ".")) {
																relatedVariables = true;
															}
															if(relatedVariables) {
																Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
																additionallyMatchedStatements2.add(ifNode2);
																CompositeReplacement composite = new CompositeReplacement(statement1.getString(), ifNode2.getString(), new LinkedHashSet<>(), additionallyMatchedStatements2);
																info.addReplacement(composite);
																splitConditional = true;
																break;
															}
														}
													}
												}
												String commonPrefix = PrefixSuffixUtils.longestCommonPrefix(subCondition1, subCondition2);
												String commonSuffix = PrefixSuffixUtils.longestCommonSuffix(subCondition1, subCondition2);
												if(commonPrefix.isEmpty() && !commonSuffix.isEmpty()) {
													int endIndexS1 = subCondition1.lastIndexOf(commonSuffix);
													String diff1 = subCondition1.substring(0, endIndexS1);
													int endIndexS2 = subCondition2.lastIndexOf(commonSuffix);
													String diff2 = subCondition2.substring(0, endIndexS2);
													for(AbstractCodeFragment fragment2 : info.getStatements2()) {
														if(fragment2.getVariableDeclarations().size() > 0) {
															VariableDeclaration variableDeclaration = fragment2.getVariableDeclarations().get(0);
															if(variableDeclaration.getVariableName().equals(diff2) && variableDeclaration.getInitializer() != null &&
																	variableDeclaration.getInitializer().getString().equals(diff1)) {
																Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
																additionallyMatchedStatements2.add(ifNode2);
																CompositeReplacement composite = new CompositeReplacement(statement1.getString(), ifNode2.getString(), new LinkedHashSet<>(), additionallyMatchedStatements2);
																info.addReplacement(composite);
																splitConditional = true;
																ExtractVariableRefactoring extract = new ExtractVariableRefactoring(variableDeclaration, container1, container2, parentMapper != null);
																List<LeafExpression> subExpressions = expression.findExpression(variableDeclaration.getInitializer().getString());
																for(LeafExpression subExpression : subExpressions) {
																	LeafMapping leafMapping = new LeafMapping(subExpression, variableDeclaration.getInitializer(), container1, container2);
																	extract.addSubExpressionMapping(leafMapping);
																}
																refactorings.add(extract);
																break;
															}
														}
													}
												}
												else if(!commonPrefix.isEmpty() && !commonSuffix.isEmpty()) {
													int beginIndexS1 = subCondition1.indexOf(commonPrefix) + commonPrefix.length();
													int endIndexS1 = subCondition1.lastIndexOf(commonSuffix);
													String diff1 = beginIndexS1 > endIndexS1 ? "" :	subCondition1.substring(beginIndexS1, endIndexS1);
													int beginIndexS2 = subCondition2.indexOf(commonPrefix) + commonPrefix.length();
													int endIndexS2 = subCondition2.lastIndexOf(commonSuffix);
													String diff2 = beginIndexS2 > endIndexS2 ? "" :	subCondition2.substring(beginIndexS2, endIndexS2);
													for(Replacement replacement : info.getReplacements()) {
														if(diff1.contains(replacement.getBefore()) && diff2.contains(replacement.getAfter())) {
															String tmp = diff2.replace(replacement.getAfter(), replacement.getBefore());
															if(tmp.equals(diff1) || (tmp.length() == diff1.length() && StringDistance.editDistance(diff1, tmp) <= 1)) {
																Set<AbstractCodeFragment> additionallyMatchedStatements2 = new LinkedHashSet<>();
																additionallyMatchedStatements2.add(ifNode2);
																CompositeReplacement composite = new CompositeReplacement(statement1.getString(), ifNode2.getString(), new LinkedHashSet<>(), additionallyMatchedStatements2);
																info.addReplacement(composite);
																splitConditional = true;
																break;
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
						if(splitConditional) {
							List<Replacement> compositeReplacements = info.getReplacements(ReplacementType.COMPOSITE);
							Set<AbstractCodeFragment> splitConditionals = new LinkedHashSet<>();
							if(statement2 instanceof AbstractExpression) {
								CompositeStatementObject owner = ((AbstractExpression)statement2).getOwner();
								if(owner != null)
									splitConditionals.add(owner);
							}
							else {
								splitConditionals.add(statement2);
							}
							for(Replacement compositeReplacement : compositeReplacements) {
								splitConditionals.addAll(((CompositeReplacement)compositeReplacement).getAdditionallyMatchedStatements2());
							}
							if(sequentiallySplitConditionals(statement1, splitConditionals, mappings)) {
								SplitConditionalRefactoring split = new SplitConditionalRefactoring(statement1, splitConditionals, container1, container2);
								refactorings.add(split);
								for(String key : intersection) {
									LeafExpression leaf1 = subConditionMap1.get(key);
									if(leaf1 == null)
										leaf1 = subConditionMap1.get("!" + key);
									LeafExpression leaf2 = subConditionMap2.get(key);
									if(leaf2 == null)
										leaf2 = subConditionMap2.get("!" + key);
									if(leaf1 != null && leaf2 != null) {
										LeafMapping leafMapping = new LeafMapping(leaf1, leaf2, container1, container2);
										split.addSubExpressionMapping(leafMapping);
									}
								}
								for(String key : intersection2) {
									LeafExpression leaf1 = subConditionMap1.get(key);
									if(leaf1 == null)
										leaf1 = subConditionMap1.get("!" + key);
									LeafExpression leaf2 = subConditionMap.get(key);
									if(leaf2 == null)
										leaf2 = subConditionMap.get("!" + key);
									if(leaf1 != null && leaf2 != null) {
										LeafMapping leafMapping = new LeafMapping(leaf1, leaf2, container1, container2);
										split.addSubExpressionMapping(leafMapping);
									}
								}
							}
						}
					}
					else if(ifNodes1.size() > ifNodes2.size()) {
						boolean mergeConditional = false;
						for(CompositeStatementObject ifNode1 : ifNodes1) {
							List<AbstractExpression> expressions1 = ifNode1.getExpressions();
							if(expressions1.size() > 0 && !statement1.equals(ifNode1) && !ifNode1.getExpressions().contains(statement1) && !containsIdenticalIfNode(ifNodes2, ifNode1) && sequentiallyMergedConditional(ifNode1, statement1, statement2, mappings)) {
								AbstractExpression ifExpression1 = expressions1.get(0);
								String conditional = ifExpression1.getString();
								String[] subConditions = SPLIT_CONDITIONAL_PATTERN.split(conditional);
								List<String> subConditionsAsList = new ArrayList<String>();
								subConditionMap = new LinkedHashMap<>();
								for(String s : subConditions) {
									String trimmed = s.trim();
									String temp1 = new String(trimmed);
									List<LeafExpression> leafExpressions = ifNode1.findExpression(trimmed);
									for(Replacement replacement : info.getReplacements()) {
										if(!(replacement instanceof IntersectionReplacement)) {
											temp1 = ReplacementUtil.performReplacement(temp1, replacement.getBefore(), replacement.getAfter());
										}
									}
									subConditionsAsList.add(temp1);
									if(leafExpressions.size() > 0) {
										subConditionMap.put(temp1, leafExpressions.get(0));
									}
								}
								intersection2 = subConditionIntersection(subConditionsAsList, subConditionsAsList2);
								int matches2 = matchCount(intersection2, info);
								boolean pass2 = pass(subConditionsAsList, subConditionsAsList2, intersection2, matches2);
								if(pass2 && !intersection.containsAll(intersection2)) {
									Set<AbstractCodeFragment> additionallyMatchedStatements1 = new LinkedHashSet<>();
									additionallyMatchedStatements1.add(ifNode1);
									CompositeReplacement composite = new CompositeReplacement(ifNode1.getString(), statement2.getString(), additionallyMatchedStatements1, new LinkedHashSet<>());
									info.addReplacement(composite);
									mergeConditional = true;
								}
							}
						}
						if(mergeConditional) {
							List<Replacement> compositeReplacements = info.getReplacements(ReplacementType.COMPOSITE);
							Set<AbstractCodeFragment> mergedConditionals = new LinkedHashSet<>();
							if(statement1 instanceof AbstractExpression) {
								CompositeStatementObject owner = ((AbstractExpression)statement1).getOwner();
								if(owner != null)
									mergedConditionals.add(owner);
							}
							else {
								mergedConditionals.add(statement1);
							}
							for(Replacement compositeReplacement : compositeReplacements) {
								mergedConditionals.addAll(((CompositeReplacement)compositeReplacement).getAdditionallyMatchedStatements1());
							}
							if(sequentiallyMergedConditionals(mergedConditionals, statement2, mappings)) {
								MergeConditionalRefactoring merge = new MergeConditionalRefactoring(mergedConditionals, statement2, container1, container2);
								refactorings.add(merge);
								for(String key : intersection) {
									LeafExpression leaf1 = subConditionMap1.get(key);
									if(leaf1 == null)
										leaf1 = subConditionMap1.get("!" + key);
									LeafExpression leaf2 = subConditionMap2.get(key);
									if(leaf2 == null)
										leaf2 = subConditionMap2.get("!" + key);
									if(leaf1 != null && leaf2 != null) {
										LeafMapping leafMapping = new LeafMapping(leaf1, leaf2, container1, container2);
										merge.addSubExpressionMapping(leafMapping);
									}
								}
								for(String key : intersection2) {
									LeafExpression leaf1 = subConditionMap.get(key);
									if(leaf1 == null)
										leaf1 = subConditionMap.get("!" + key);
									LeafExpression leaf2 = subConditionMap2.get(key);
									if(leaf2 == null)
										leaf2 = subConditionMap2.get("!" + key);
									if(leaf1 != null && leaf2 != null) {
										LeafMapping leafMapping = new LeafMapping(leaf1, leaf2, container1, container2);
										merge.addSubExpressionMapping(leafMapping);
									}
								}
								mapper.createMultiMappingsForDuplicatedStatements(mergedConditionals, statement2, parameterToArgumentMap);
							}
						}
					}
				}
				invertedConditionals = checkForInvertedConditionals(subConditionsAsList1, subConditionsAsList2, info);
				if((invertedConditionals > 0 || matches > 0) && info.getReplacements(ReplacementType.TYPE).isEmpty() && info.getReplacements(ReplacementType.METHOD_INVOCATION).isEmpty() && !includesLocalVariable(statement1, statement2, intersection, container1, container2)) {
					List<Replacement> operatorReplacements = info.getReplacements(ReplacementType.INFIX_OPERATOR);
					boolean booleanOperatorReversed = false;
					for(Replacement r : operatorReplacements) {
						if(r.getBefore().equals("&&") && r.getAfter().equals("||")) {
							booleanOperatorReversed = true;
						}
						else if(r.getBefore().equals("||") && r.getAfter().equals("&&")) {
							booleanOperatorReversed = true;
						}
					}
					if(matches == invertedConditionals && (booleanOperatorReversed || !containLogicalOperator)) {
						if(statement1 instanceof AbstractExpression) {
							CompositeStatementObject owner = ((AbstractExpression)statement1).getOwner();
							if(owner != null) {
								InvertConditionRefactoring invert = new InvertConditionRefactoring(owner, statement2, container1, container2);
								refactorings.add(invert);
							}
						}
						else {
							InvertConditionRefactoring invert = new InvertConditionRefactoring(statement1, statement2, container1, container2);
							refactorings.add(invert);
						}
					}
					return true;
				}
				if(!pass && statement1 instanceof AbstractExpression && !(statement2 instanceof AbstractExpression)) {
					AbstractCall invocation1 = statement1.invocationCoveringEntireFragment();
					if(invocation1 != null) {
						for(AbstractCall invocation2 : statement2.getMethodInvocations()) {
							if(invocation1.compatibleName(invocation2) && invocation1.arguments().size() == invocation2.arguments().size()) {
								if(subConditionsAsList2.size() > 1) {
									IntersectionReplacement r = new IntersectionReplacement(s1, s2, intersection, ReplacementType.CONDITIONAL);
									info.addReplacement(r);
								}
								if(statement1.getString().contains("!" + invocation1.actualString()) && !statement2.getString().contains("!" + invocation2.actualString())) {
									Replacement r2 = new Replacement("!" + invocation1.actualString(), invocation2.actualString(), ReplacementType.INVERT_CONDITIONAL);
									info.addReplacement(r2);
								}
								else if(!statement1.getString().contains("!" + invocation1.actualString()) && statement2.getString().contains("!" + invocation2.actualString())) {
									Replacement r2 = new Replacement(invocation1.actualString(), "!" + invocation2.actualString(), ReplacementType.INVERT_CONDITIONAL);
									info.addReplacement(r2);
								}
								return true;
							}
						}
					}
				}
			}
			if(s1.contains(" >= ") && s2.contains(" <= ")) {
				Replacement r = invertConditionalDirection(s1, s2, " >= ", " <= ");
				if(r != null) {
					info.addReplacement(r);
					return true;
				}
			}
			if(s1.contains(" <= ") && s2.contains(" >= ")) {
				Replacement r = invertConditionalDirection(s1, s2, " <= ", " >= ");
				if(r != null) {
					info.addReplacement(r);
					return true;
				}
			}
			if(s1.contains(" > ") && s2.contains(" < ")) {
				Replacement r = invertConditionalDirection(s1, s2, " > ", " < ");
				if(r != null) {
					info.addReplacement(r);
					return true;
				}
			}
			if(s1.contains(" < ") && s2.contains(" > ")) {
				Replacement r = invertConditionalDirection(s1, s2, " < ", " > ");
				if(r != null) {
					info.addReplacement(r);
					return true;
				}
			}
		}
		return false;
	}

	private static int matchCount(Set<String> intersection, ReplacementInfo info) {
		int matches = 0;
		if(!intersection.isEmpty()) {
			for(String element : intersection) {
				boolean replacementFound = false;
				for(Replacement r : info.getReplacements()) {
					boolean getterReplacement = false;
					if(r instanceof VariableReplacementWithMethodInvocation) {
						getterReplacement = ((VariableReplacementWithMethodInvocation)r).getterReplacement();
					}
					if(element.equals(r.getAfter()) || element.equals("(" + r.getAfter()) || element.equals(r.getAfter() + ")")) {
						replacementFound = true;
						break;
					}
					if(element.equals("!" + r.getAfter())) {
						replacementFound = true;
						break;
					}
					if(r.getType().equals(ReplacementType.INFIX_OPERATOR) && element.contains(r.getAfter())) {
						replacementFound = true;
						break;
					}
					if(r.getType().equals(ReplacementType.INFIX_EXPRESSION) && element.contains(r.getAfter())) {
						replacementFound = true;
						break;
					}
					if(!getterReplacement && ReplacementUtil.contains(element, r.getAfter()) && element.startsWith(r.getAfter()) &&
							(element.endsWith(" != null") || element.endsWith(" == null") || element.endsWith(" != 0") || element.endsWith(" == 0"))) {
						replacementFound = true;
						break;
					}
				}
				if(!replacementFound) {
					matches++;
				}
			}
		}
		return matches;
	}

	private static boolean pass(List<String> subConditionsAsList1, List<String> subConditionsAsList2, Set<String> intersection,
			int matches) {
		boolean pass = false;
		if(matches == 1 && intersection.size() == 1 && intersection.iterator().next().endsWith("null")) {
			pass = matches == Math.min(subConditionsAsList1.size(), subConditionsAsList2.size());
		}
		else {
			pass = matches > 0;
		}
		return pass;
	}

	private static boolean includesLocalVariable(AbstractCodeFragment statement1, AbstractCodeFragment statement2, Set<String> intersection, VariableDeclarationContainer container1, VariableDeclarationContainer container2) {
		if(statement1 instanceof AbstractExpression) {
			for(String commonString : intersection) {
				if((statement1.getString().equals(commonString) || statement1.getString().equals("!" + commonString)) &&
						container1.getVariableDeclaration(commonString) != null) {
					return true;
				}
			}
		}
		if(statement2 instanceof AbstractExpression) {
			for(String commonString : intersection) {
				if((statement2.getString().equals(commonString) || statement2.getString().equals("!" + commonString)) &&
						container2.getVariableDeclaration(commonString) != null) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean containsIdenticalIfNode(Set<CompositeStatementObject> ifNodes1, CompositeStatementObject ifNode2) {
		for(CompositeStatementObject ifNode1 : ifNodes1) {
			if(ifNode1.getString().equals(ifNode2.getString())) {
				return true;
			}
		}
		return false;
	}

	private static Replacement invertConditionalDirection(String s1, String s2, String operator1, String operator2) {
		int indexS1 = s1.indexOf(operator1);
		int indexS2 = s2.indexOf(operator2);
		//s1 goes right, s2 goes left
		int i = indexS1 + operator1.length();
		int j = indexS2 - 1;
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		while(i < s1.length() && j >= 0) {
			sb1.append(s1.charAt(i));
			sb2.insert(0, s2.charAt(j));
			if(sb1.toString().equals(sb2.toString())) {
				String subCondition1 = operator1 + sb1.toString();
				String subCondition2 = sb2.toString() + operator2;
				Replacement r = new Replacement(subCondition1, subCondition2, ReplacementType.INVERT_CONDITIONAL);
				return r;
			}
			i++;
			j--;
		}
		//s1 goes left, s2 goes right
		i = indexS1 - 1;
		j = indexS2 + operator2.length();
		sb1 = new StringBuilder();
		sb2 = new StringBuilder();
		while(i >= 0 && j < s2.length()) {
			sb1.insert(0, s1.charAt(i));
			sb2.append(s2.charAt(j));
			if(sb1.toString().equals(sb2.toString())) {
				String subCondition1 = sb1.toString() + operator1;
				String subCondition2 = operator2 + sb2.toString();
				Replacement r = new Replacement(subCondition1, subCondition2, ReplacementType.INVERT_CONDITIONAL);
				return r;
			}
			i--;
			j++;
		}
		return null;
	}

	private static boolean sequentiallyMergedConditionals(Set<AbstractCodeFragment> mergedConditionals, AbstractCodeFragment statement2, Set<AbstractCodeMapping> mappings) {
		for(AbstractCodeMapping mapping : mappings) {
			int nestedFragment1 = 0;
			for(AbstractCodeFragment mergedConditional : mergedConditionals) {
				if(mergedConditional.getLocationInfo().subsumes(mapping.getFragment1().getLocationInfo()) || subsumedByOther(mergedConditionals, mergedConditional)) {
					nestedFragment1++;
				}
			}
			boolean nestedFragment2 = statement2.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo()) || statement2.getTernaryOperatorExpressions().size() > 0;
			if(nestedFragment1 == mergedConditionals.size() && nestedFragment2) {
				return true;
			}
		}
		boolean leafConditional1 = false;
		for(AbstractCodeFragment mergedConditional : mergedConditionals) {
			if(mergedConditional instanceof StatementObject) {
				leafConditional1 = true;
				break;
			}
		}
		boolean leafConditional2 = statement2 instanceof StatementObject;
		if(leafConditional1 && leafConditional2) {
			return true;
		}
		//check if all mergedConditionals have inverted conditions
		if(statement2.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			List<String> subConditionsAsList2 = new ArrayList<String>();
			CompositeStatementObject comp2 = (CompositeStatementObject)statement2;
			String conditional2 = prepareConditional(comp2.getExpressions().get(0).getString());
			String[] subConditions2 = SPLIT_CONDITIONAL_PATTERN.split(conditional2);
			for(String s : subConditions2) {
				subConditionsAsList2.add(s.trim());
			}
			int invertedMergedConditionals = 0;
			for(AbstractCodeFragment mergedConditional : mergedConditionals) {
				List<String> subConditionsAsList1 = new ArrayList<String>();
				if(mergedConditional.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
					CompositeStatementObject comp1 = (CompositeStatementObject)mergedConditional;
					String conditional1 = prepareConditional(comp1.getExpressions().get(0).getString());
					String[] subConditions1 = SPLIT_CONDITIONAL_PATTERN.split(conditional1);
					for(String s : subConditions1) {
						subConditionsAsList1.add(s.trim());
					}
				}
				Set<String> intersection = subConditionIntersection(subConditionsAsList1, subConditionsAsList2);
				int invertedConditions = 0;
				for(String intersectionElement : intersection) {
					if(!subConditionsAsList2.contains(intersectionElement)) {
						invertedConditions++;
					}
				}
				if(invertedConditions == intersection.size()) {
					invertedMergedConditionals++;
				}
			}
			if(invertedMergedConditionals == mergedConditionals.size()) {
				for(AbstractCodeFragment leaf : comp2.getLeaves()) {
					if((leaf.isKeyword() || leaf.getString().equals("return false;\n") || leaf.getString().equals("return true;\n")) &&
							(statement2.getLocationInfo().subsumes(leaf.getLocationInfo()) || statement2.getLocationInfo().before(leaf.getLocationInfo()))) {
						return true;
					}
				}
			}
		}
		if(conditionalsUnderTheSameParent(mergedConditionals)) {
			return true;
		}
		return false;
	}

	private static boolean sequentiallyMergedConditional(AbstractCodeFragment mergedConditional, AbstractCodeFragment statement1, AbstractCodeFragment statement2, Set<AbstractCodeMapping> mappings) {
		for(AbstractCodeMapping mapping : mappings) {
			boolean nestedFragment1 = false;
			if(mergedConditional.getLocationInfo().subsumes(mapping.getFragment1().getLocationInfo())) {
				nestedFragment1 = true;
			}
			else if(mergedConditional instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)mergedConditional;
				for(AbstractCodeFragment leaf : composite.getLeaves()) {
					if((leaf.isKeyword() || leaf.getString().equals("return false;\n") || leaf.getString().equals("return true;\n")) &&
							(statement1.getLocationInfo().subsumes(leaf.getLocationInfo()) || statement1.getLocationInfo().before(leaf.getLocationInfo()))) {
						nestedFragment1 = true;
						break;
					}
				}
			}
			boolean nestedFragment2 = false;
			if(statement2.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo())) {
				nestedFragment2 = true;
			}
			else if(statement2 instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement2;
				for(AbstractCodeFragment leaf : composite.getLeaves()) {
					if((leaf.isKeyword() || leaf.getString().equals("return false;\n") || leaf.getString().equals("return true;\n")) &&
							(statement2.getLocationInfo().subsumes(leaf.getLocationInfo()) || statement2.getLocationInfo().before(leaf.getLocationInfo()))) {
						nestedFragment2 = true;
						break;
					}
				}
			}
			if(nestedFragment1 && nestedFragment2) {
				return true;
			}
		}
		if(mergedConditional instanceof StatementObject || statement2 instanceof StatementObject) {
			return true;
		}
		return false;
	}

	private static boolean sequentiallySplitConditionals(AbstractCodeFragment statement1, Set<AbstractCodeFragment> splitConditionals, Set<AbstractCodeMapping> mappings) {
		for(AbstractCodeMapping mapping : mappings) {
			int nestedFragment2 = 0;
			for(AbstractCodeFragment splitConditional : splitConditionals) {
				if(splitConditional.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo()) || subsumedByOther(splitConditionals, splitConditional)) {
					nestedFragment2++;
				}
			}
			boolean nestedFragment1 = statement1.getLocationInfo().subsumes(mapping.getFragment1().getLocationInfo()) || statement1.getTernaryOperatorExpressions().size() > 0;
			if(nestedFragment2 == splitConditionals.size() && nestedFragment1) {
				return true;
			}
		}
		boolean leafConditional2 = false;
		for(AbstractCodeFragment splitConditional : splitConditionals) {
			if(splitConditional instanceof StatementObject) {
				leafConditional2 = true;
				break;
			}
		}
		boolean leafConditional1 = statement1 instanceof StatementObject;
		if(leafConditional1 && leafConditional2) {
			return true;
		}
		//check if all splitConditionals have inverted conditions
		if(statement1.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			List<String> subConditionsAsList1 = new ArrayList<String>();
			CompositeStatementObject comp1 = (CompositeStatementObject)statement1;
			String conditional1 = prepareConditional(comp1.getExpressions().get(0).getString());
			String[] subConditions1 = SPLIT_CONDITIONAL_PATTERN.split(conditional1);
			for(String s : subConditions1) {
				subConditionsAsList1.add(s.trim());
			}
			int invertedSplitConditionals = 0;
			for(AbstractCodeFragment splitConditional : splitConditionals) {
				List<String> subConditionsAsList2 = new ArrayList<String>();
				if(splitConditional.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
					CompositeStatementObject comp2 = (CompositeStatementObject)splitConditional;
					String conditional2 = prepareConditional(comp2.getExpressions().get(0).getString());
					String[] subConditions2 = SPLIT_CONDITIONAL_PATTERN.split(conditional2);
					for(String s : subConditions2) {
						subConditionsAsList2.add(s.trim());
					}
				}
				Set<String> intersection = subConditionIntersection(subConditionsAsList2, subConditionsAsList1);
				int invertedConditions = 0;
				for(String intersectionElement : intersection) {
					if(!subConditionsAsList1.contains(intersectionElement)) {
						invertedConditions++;
					}
				}
				if(invertedConditions == intersection.size()) {
					invertedSplitConditionals++;
				}
			}
			if(invertedSplitConditionals == splitConditionals.size()) {
				for(AbstractCodeFragment leaf : comp1.getLeaves()) {
					if((leaf.isKeyword() || leaf.getString().equals("return false;\n") || leaf.getString().equals("return true;\n")) &&
							(statement1.getLocationInfo().subsumes(leaf.getLocationInfo()) || statement1.getLocationInfo().before(leaf.getLocationInfo()))) {
						return true;
					}
				}
			}
		}
		if(conditionalsUnderTheSameParent(splitConditionals)) {
			return true;
		}
		return false;
	}

	private static boolean sequentiallySplitConditional(AbstractCodeFragment statement1, AbstractCodeFragment splitConditional, AbstractCodeFragment statement2, Set<AbstractCodeMapping> mappings) {
		for(AbstractCodeMapping mapping : mappings) {
			boolean nestedFragment2 = false;
			if(splitConditional.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo())) {
				nestedFragment2 = true;
			}
			else if(splitConditional instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)splitConditional;
				for(AbstractCodeFragment leaf : composite.getLeaves()) {
					if((leaf.isKeyword() || leaf.getString().equals("return false;\n") || leaf.getString().equals("return true;\n")) &&
							(statement2.getLocationInfo().subsumes(leaf.getLocationInfo()) || statement2.getLocationInfo().before(leaf.getLocationInfo()))) {
						nestedFragment2 = true;
						break;
					}
				}
			}
			boolean nestedFragment1 = false;
			if(statement1.getLocationInfo().subsumes(mapping.getFragment1().getLocationInfo())) {
				nestedFragment1 = true;
			}
			else if(statement1 instanceof CompositeStatementObject) {
				CompositeStatementObject composite = (CompositeStatementObject)statement1;
				for(AbstractCodeFragment leaf : composite.getLeaves()) {
					if((leaf.isKeyword() || leaf.getString().equals("return false;\n") || leaf.getString().equals("return true;\n")) &&
							(statement1.getLocationInfo().subsumes(leaf.getLocationInfo()) || statement1.getLocationInfo().before(leaf.getLocationInfo()))) {
						nestedFragment1 = true;
						break;
					}
				}
			}
			if(nestedFragment2 && nestedFragment1) {
				return true;
			}
		}
		if(statement1 instanceof StatementObject || splitConditional instanceof StatementObject) {
			return true;
		}
		return false;
	}

	private static boolean conditionalsUnderTheSameParent(Set<AbstractCodeFragment> conditionals) {
		CompositeStatementObject parent = null;
		int commonParentCount = 0;
		for(AbstractCodeFragment fragment : conditionals) {
			if(parent == null) {
				parent = fragment.getParent();
				commonParentCount++;
			}
			else if(parent != null && parent.equals(fragment.getParent())) {
				commonParentCount++;
			}
		}
		return conditionals.size() > 1 && commonParentCount == conditionals.size();
	}

	private static boolean subsumedByOther(Set<AbstractCodeFragment> conditionals, AbstractCodeFragment conditional) {
		for(AbstractCodeFragment otherConditional : conditionals) {
			if(!otherConditional.equals(conditional)) {
				if(otherConditional.getLocationInfo().subsumes(conditional.getLocationInfo()) ||
						otherConditional.getLocationInfo().startsAtTheEndLineOf(conditional.getLocationInfo()) ||
						conditional.getLocationInfo().startsAtTheEndLineOf(otherConditional.getLocationInfo()) ||
						otherConditional.getLocationInfo().nextLine(conditional.getLocationInfo()) ||
						conditional.getLocationInfo().nextLine(otherConditional.getLocationInfo())) {
					return true;
				}
				if(conditional.getParent() != null && isElseIfBranch(conditional, conditional.getParent()) && conditional.getParent().getLocationInfo().subsumes(otherConditional.getLocationInfo())) {
					return true;
				}
			}
		}
		return false;
	}

	protected static boolean hasElseIfBranch(CompositeStatementObject parent) {
		if(parent != null && parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
			return parent.getStatements().size() == 2 && parent.getStatements().get(1).getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT);
		}
		return false;
	}

	protected static boolean isElseIfBranch(AbstractCodeFragment child, CompositeStatementObject parent) {
		return parent.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
				child.getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) &&
				parent.getStatements().size() == 2 && parent.getStatements().indexOf(child) == 1;
	}
}
