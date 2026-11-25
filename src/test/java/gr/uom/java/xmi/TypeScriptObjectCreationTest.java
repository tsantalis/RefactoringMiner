package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.ObjectCreation;

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
				"const c15 = new (a?.B)();\n" +
				"const c16 = new (Foo || Bar)();\n" +
				"const c17 = new (getCtor())();\n" +
				"const c18 = new class Named {}();\n" +
				"function f() { return new new.target(); }\n" +
				"async function g() { return new (await getCtor())(); }\n";
		UMLModel model = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/test.ts", code), Set.of("src"));
		UMLClass moduleClass = model.getClassList().get(0);
		List<AbstractCall> creations = moduleClass.getContainer().get().getAllCreations();
		assertEquals(18, creations.size());
		assertEquals("Foo", creations.get(0).getName());
		assertEquals("Foo", creations.get(1).getName());
		assertEquals("Foo", creations.get(2).getName());
		assertEquals("Foo", creations.get(3).getName());
		assertEquals("Foo", creations.get(4).getName());
		assertEquals(List.of("string"), creations.get(4).getTypeArguments().stream().map(UMLType::toString).toList());
		assertEquals("Bar", creations.get(5).getName());
		assertEquals("Baz", creations.get(6).getName());
		assertEquals("canUseWeakSet ? WeakSet : Set", creations.get(7).getName());
		assertEquals("Bar", creations.get(8).getName());
		assertEquals("this", creations.get(9).getName());
		assertEquals("Base", creations.get(10).getName());
		assertNotNull(((ObjectCreation) creations.get(10)).getAnonymousClassDeclaration());
		assertEquals("AnonymousClass", creations.get(11).getName());
		assertEquals("AnonymousFunction", creations.get(12).getName());
		assertEquals("named", creations.get(13).getName());
		assertEquals("B", creations.get(14).getName());
		assertEquals("Foo", creations.get(15).getName());
		assertEquals("getCtor()", creations.get(16).getName());
		assertEquals("Named", creations.get(17).getName());
		UMLOperation f = moduleClass.getOperations().get(0);
		assertEquals("new.target", f.getAllCreations().get(0).getName());
		UMLOperation g = moduleClass.getOperations().get(1);
		assertEquals("getCtor()", g.getAllCreations().get(0).getName());
	}
}
