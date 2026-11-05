package org.refactoringminer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLModel;

//Issue #1124: an interface converted to a type alias with the same properties
public class TestTypeScriptInterfaceToTypeAlias {
	private static final String INTERFACE = """
			export interface Item {
			  id: string;
			  name: string;
			}
			""";
	private static final String TYPE_ALIAS = """
			export type Item = {
			  id: string;
			  name: string;
			};
			""";

	@Test
	public void testInterfaceConvertedInPlace() throws Exception {
		assertEquals(List.of("Change Type Declaration Kind\tinterface to type alias in type a.Item"),
				refactorings(Map.of("src/a.ts", INTERFACE), Map.of("src/a.ts", TYPE_ALIAS)));
	}

	@Test
	public void testInterfaceConvertedAndMoved() throws Exception {
		assertEquals(List.of("Move Class\ta.__module__ moved to b.__module__",
				"Move Class\ta.Item moved to b.Item",
				"Change Type Declaration Kind\tinterface to type alias in type b.Item"),
				refactorings(Map.of("src/a.ts", INTERFACE), Map.of("src/b.ts", TYPE_ALIAS)));
	}

	private static List<String> refactorings(Map<String, String> before, Map<String, String> after) throws Exception {
		UMLModel parent = GitHistoryRefactoringMinerImpl.createModel(before, Set.of("src"));
		UMLModel child = GitHistoryRefactoringMinerImpl.createModel(after, Set.of("src"));
		return parent.diff(child).getRefactorings().stream().map(Refactoring::toString).toList();
	}
}
