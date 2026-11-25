package org.refactoringminer.test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class TestTypeScriptNestedObjectAttributeExtraction {
	private static final String BEFORE = """
			function buildRunnerSetters(block, hoistDeclarators) {
			  return this.ids.map(function (uid, source) {
			    var nodes = [];
			    traverse(block, {
			      enter: function (node) {
			        hoistDeclarators.push(node);
			        nodes.push(node);
			      }
			    });
			    return nodes;
			  });
			}
			""";

	private static final String AFTER = """
			function buildRunnerSetters(block, hoistDeclarators) {
			  return this.ids.map(function (uid, source) {
			    var state = {
			      nodes: [],
			      hoistDeclarators: hoistDeclarators
			    };
			    traverse(block, {
			      enter: function (node, state) {
			        state.hoistDeclarators.push(node);
			        state.nodes.push(node);
			      }
			    }, state);
			    return state.nodes;
			  });
			}
			""";

	@Test
	public void testExtractAttributeInNestedObjectLiteral() throws Exception {
		UMLModel parent = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/system.js", BEFORE), Set.of("src"));
		UMLModel child = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/system.js", AFTER), Set.of("src"));
		List<String> refactorings = parent.diff(child).getRefactorings().stream().map(Refactoring::toString).toList();
		assertTrue(refactorings.stream().anyMatch(r -> r.startsWith("Extract Attribute") && r.contains("hoistDeclarators")),
				"Expected Extract Attribute refactoring for hoistDeclarators, got: " + refactorings);
	}

	private static final String KOTLIN_BEFORE = """
			package demo

			class Watcher {
			  fun install(prefix: String): Runnable {
			    return object : Runnable {
			      override fun run() {
			        println(prefix + ".run")
			      }
			    }
			  }
			}
			""";

	private static final String KOTLIN_AFTER = """
			package demo

			class Watcher {
			  fun install(prefix: String): Runnable {
			    return object : Runnable {
			      private val label = prefix + ".run"
			      override fun run() {
			        println(label)
			      }
			    }
			  }
			}
			""";

	//the object expression is in the anonymous classes of the method, but not in the anonymous classes of the class
	@Test
	public void testFindAnonymousClassOfKotlinMethod() throws Exception {
		UMLModel parent = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/demo/Watcher.kt", KOTLIN_BEFORE), Set.of("src"));
		UMLModel child = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/demo/Watcher.kt", KOTLIN_AFTER), Set.of("src"));
		UMLAnonymousClass anonymous = child.getClassList().get(0).getOperations().get(0).getAnonymousClassList().get(0);
		UMLModelDiff modelDiff = parent.diff(child);
		assertSame(anonymous, modelDiff.findClassInChildModel(anonymous.getName()));
		assertSame(parent.getClassList().get(0).getOperations().get(0).getAnonymousClassList().get(0), modelDiff.findClassInParentModel(anonymous.getName()));
	}
}
