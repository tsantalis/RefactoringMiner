package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.decomposition.AbstractCall;

public class TypeScriptObjectCreationTest {
	@Test
	public void testObjectCreation() throws Exception {
		String code = "const c1 = new (Foo)();\n" +
				"const c2 = new (Foo as any)();\n" +
				"const c3 = new (<any>Foo)();\n" +
				"const c4 = new Foo!();\n" +
				"const c5 = new Foo<string>();\n" +
				"const c6 = new (_temp = Bar)();\n" +
				"const c7 = new (0, Baz)();\n" +
				"const c8 = new (canUseWeakSet ? WeakSet : Set)();\n" +
				"const c9 = new (Foo == null ? void 0 : Foo.Bar)();\n" +
				"const c10 = new this();\n" +
				"const c11 = new class extends Base {}();\n" +
				"const c12 = new class {}();\n" +
				"const c13 = new function() {}();\n" +
				"const c14 = new function named() {}();\n" +
				"function f() { return new new.target(); }\n";
		UMLModel model = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/test.ts", code), Set.of("src"));
		UMLClass moduleClass = model.getClassList().get(0);
		List<AbstractCall> creations = moduleClass.getContainer().get().getAllCreations();
		assertEquals(14, creations.size());
		assertEquals("Foo", creations.get(0).getName());
		assertEquals("Foo", creations.get(1).getName());
		assertEquals("Foo", creations.get(2).getName());
		assertEquals("Foo", creations.get(3).getName());
		assertEquals("Foo", creations.get(4).getName());
		assertEquals("Bar", creations.get(5).getName());
		assertEquals("Baz", creations.get(6).getName());
		assertEquals("canUseWeakSet ? WeakSet : Set", creations.get(7).getName());
		assertEquals("Bar", creations.get(8).getName());
		assertEquals("this", creations.get(9).getName());
		assertEquals("Base", creations.get(10).getName());
		assertEquals("AnonymousClass", creations.get(11).getName());
		assertEquals("AnonymousFunction", creations.get(12).getName());
		assertEquals("named", creations.get(13).getName());
		UMLOperation f = moduleClass.getOperations().get(0);
		assertEquals("new.target", f.getAllCreations().get(0).getName());
	}
}
