package org.refactoringminer.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLModel;

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
}
