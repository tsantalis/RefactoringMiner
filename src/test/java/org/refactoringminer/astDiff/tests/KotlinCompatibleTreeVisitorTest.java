package org.refactoringminer.astDiff.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.refactoringminer.astDiff.matchers.ProjectASTDiffer;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import com.github.gumtreediff.tree.Tree;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class KotlinCompatibleTreeVisitorTest {
	@Test
	public void kotlinAstDiffTreeUsesJavaCompatibleNodeNames() throws Exception {
		String filePath = "src/main/kotlin/com/example/Foo.kt";
		String source = """
				package com.example

				import kotlin.math.*

				class Foo {
				    val count: Int = 1

				    fun inc(value: Int): Int {
				        return max(count, value + 1)
				    }
				}
				""";
		Map<String, String> fileContents = new LinkedHashMap<>();
		fileContents.put(filePath, source);
		UMLModel model = GitHistoryRefactoringMinerImpl.createModelForASTDiff(fileContents, new LinkedHashSet<>());

		Tree root = model.getTreeContextMap().get(filePath).getRoot();

		assertEquals("CompilationUnit", root.getType().name);
		assertHasType(root, "PackageDeclaration");
		assertHasType(root, "ImportDeclaration");
		assertHasType(root, "TypeDeclaration");
		assertHasType(root, "TYPE_DECLARATION_KIND");
		assertHasType(root, "FieldDeclaration");
		assertHasType(root, "MethodDeclaration");
		assertHasType(root, "SingleVariableDeclaration");
		assertHasType(root, "ReturnStatement");
		assertHasType(root, "MethodInvocation");
		assertHasType(root, "METHOD_INVOCATION_ARGUMENTS");
		assertHasType(root, "InfixExpression");
		assertHasTypeAndLabel(root, "ASSIGNMENT_OPERATOR", "=");
		assertCovers(source, find(root, "ASSIGNMENT_OPERATOR", "="), "=");
		assertHasTypeAndLabel(root, "QualifiedName", "kotlin.math.*");
		assertCovers(source, find(root, "QualifiedName", "kotlin.math.*"), "kotlin.math.*");
		assertHasTypeAndLabel(root, "SimpleName", "Foo");
		assertHasTypeAndLabel(root, "SimpleName", "inc");
		assertHasTypeAndLabel(root, "SimpleName", "value");
		assertFalse(containsType(root, "source_file"));
		assertFalse(containsType(root, "class_declaration"));
		assertFalse(containsType(root, "function_declaration"));
		assertFalse(containsType(root, "property_declaration"));
	}

	@Test
	public void javaToKotlinAstDiffUsesCompatibleRoots() throws Exception {
		String javaPath = "src/main/java/com/example/Foo.java";
		String kotlinPath = "src/main/kotlin/com/example/Foo.kt";
		Map<String, String> before = new LinkedHashMap<>();
		before.put(javaPath, """
				package com.example;

				class Foo {
				    int inc(int value) {
				        return value + 1;
				    }
				}
				""");
		Map<String, String> after = new LinkedHashMap<>();
		after.put(kotlinPath, """
				package com.example

				class Foo {
				    fun inc(value: Int): Int {
				        return value + 1
				    }
				}
				""");
		UMLModel parent = GitHistoryRefactoringMinerImpl.createModelForASTDiff(before, new LinkedHashSet<>());
		UMLModel child = GitHistoryRefactoringMinerImpl.createModelForASTDiff(after, new LinkedHashSet<>());
		UMLModelDiff modelDiff = parent.diff(child);

		ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, before, after);

		assertFalse(differ.getProjectASTDiff().getDiffSet().isEmpty());
		ASTDiff astDiff = differ.getProjectASTDiff().getDiffSet().iterator().next();
		assertEquals("CompilationUnit", astDiff.src.getRoot().getType().name);
		assertEquals("CompilationUnit", astDiff.dst.getRoot().getType().name);
		assertHasTypeAndLabel(astDiff.src.getRoot(), "SimpleName", "inc");
		assertHasTypeAndLabel(astDiff.dst.getRoot(), "SimpleName", "inc");
	}

	@Test
	public void kotlinAstDiffTreeKeepsNestedControlFlowAddressable() throws Exception {
		String filePath = "src/main/kotlin/com/example/Flow.kt";
		String source = """
				package com.example

				class Flow {
				    fun run(items: List<String>): Int {
				        var total = 0
				        items.forEach { item ->
				            try {
				                while (total < 10) {
				                    // before when
				                    when (item.length) {
				                        0 -> break
				                        1 -> continue
				                        else -> total += item.length
				                    }
				                }
				            } catch (e: RuntimeException) {
				                /* before throw */
				                throw IllegalStateException(e)
				            }
				        }
				        return total
				    }
				}
				""";
		Map<String, String> fileContents = new LinkedHashMap<>();
		fileContents.put(filePath, source);
		UMLModel model = GitHistoryRefactoringMinerImpl.createModelForASTDiff(fileContents, new LinkedHashSet<>());

		Tree root = model.getTreeContextMap().get(filePath).getRoot();

		assertHasType(root, "LambdaExpression");
		assertHasType(root, "TryStatement");
		assertHasType(root, "CatchClause");
		assertHasType(root, "WhileStatement");
		assertHasType(root, "SwitchStatement");
		assertHasType(root, "BreakStatement");
		assertHasType(root, "ContinueStatement");
		assertHasType(root, "ThrowStatement");
		assertHasType(root, "Assignment");
		assertHasTypeAndLabel(root, "NumberLiteral", "0");
		assertHasTypeAndLabel(root, "NumberLiteral", "1");
		assertHasTypeAndLabel(root, "SimpleName", "else");
		assertHasTypeAndLabel(root, "LineComment", "// before when");
		assertHasTypeAndLabel(root, "BlockComment", "/* before throw */");
	}

	@Test
	public void kotlinAstDiffTreeUsesCharacterOffsetsForNonAsciiSource() throws Exception {
		String filePath = "src/main/kotlin/com/example/Unicode.kt";
		String source = """
				package com.example

				class Unicode {
				    fun café(): String {
				        return "π"
				    }
				}
				""";
		Map<String, String> fileContents = new LinkedHashMap<>();
		fileContents.put(filePath, source);
		UMLModel model = GitHistoryRefactoringMinerImpl.createModelForASTDiff(fileContents, new LinkedHashSet<>());

		Tree root = model.getTreeContextMap().get(filePath).getRoot();
		Tree methodName = find(root, "SimpleName", "café");
		Tree stringLiteral = find(root, "StringLiteral", "\"π\"");

		assertNotNull(methodName);
		assertNotNull(stringLiteral);
		assertCovers(source, methodName, "café");
		assertCovers(source, stringLiteral, "\"π\"");
		for(Tree tree : root.preOrder()) {
			assertTrue(tree.getPos() >= 0, () -> "Negative start offset for " + tree);
			assertTrue(tree.getEndPos() <= source.length(), () -> "Out-of-bounds end offset for " + tree);
		}
	}

	private static void assertHasType(Tree root, String type) {
		assertTrue(containsType(root, type), () -> "Missing tree node type " + type + " in:\n" + root.toTreeString());
	}

	private static void assertHasTypeAndLabel(Tree root, String type, String label) {
		assertNotNull(find(root, type, label), () -> "Missing " + type + ": " + label + " in:\n" + root.toTreeString());
	}

	private static boolean containsType(Tree root, String type) {
		return find(root, type, null) != null;
	}

	private static Tree find(Tree root, String type, String label) {
		for(Tree tree : root.preOrder()) {
			if(tree.getType().name.equals(type) && (label == null || tree.getLabel().equals(label))) {
				return tree;
			}
		}
		return null;
	}

	private static void assertCovers(String source, Tree tree, String expected) {
		assertEquals(expected, source.substring(tree.getPos(), tree.getEndPos()));
	}
}
