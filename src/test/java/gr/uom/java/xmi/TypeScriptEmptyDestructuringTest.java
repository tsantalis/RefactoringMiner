package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

public class TypeScriptEmptyDestructuringTest {
	@Test
	public void testEmptyDestructuringObjectLiteral() throws Exception {
		String code = "const [] = { a: 1 };\nconst {} = { b: 2 };\n";
		UMLModel model = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/test.ts", code), Set.of("src"));
		//an object literal assigned to a pattern without names is not a named class, but an anonymous class of the module statement
		assertEquals(1, model.getClassList().size());
		List<UMLAnonymousClass> anonymousClasses = model.getClassList().get(0).getContainer().get().getAnonymousClassList();
		assertEquals(2, anonymousClasses.size());
		assertEquals("a", anonymousClasses.get(0).getAttributes().get(0).getName());
		assertEquals("b", anonymousClasses.get(1).getAttributes().get(0).getName());
	}
}
