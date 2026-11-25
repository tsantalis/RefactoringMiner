package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

public class TypeScriptEmptyDestructuringTest {
	@Test
	public void testEmptyDestructuringObjectLiteral() throws Exception {
		String code = "const [] = { a: 1 };\nconst {} = { b: 2 };\n";
		UMLModel model = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/test.ts", code), Set.of("src"));
		assertEquals(3, model.getClassList().size());
		assertEquals("a", model.getClassList().get(0).getAttributes().get(0).getName());
		assertEquals("b", model.getClassList().get(1).getAttributes().get(0).getName());
	}
}
