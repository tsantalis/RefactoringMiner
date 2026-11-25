package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.decomposition.AbstractCall;

//the names of the constructors and functions of TypeScript calls whose callee is not a name, which were null or empty (PR #1228 and #1229)
public class TypeScriptCalleeNamesTest {
	@Test
	public void testObjectCreationNames() throws Exception {
		String code = "const c1 = new (Foo)();\n" +
				"const c2 = new (Foo as any)();\n" +
				"const c3 = new (<any>Foo)();\n" +
				"const c4 = new Foo!();\n" +
				"const c5 = new (Foo<string>)();\n" +
				"const c6 = new (canUseWeakSet ? WeakSet : Set)();\n" +
				"const c7 = new (0, Baz)();\n" +
				"const c8 = new this();\n" +
				"const c9 = new (getCtor())();\n" +
				"const c10 = new class {}();\n" +
				"const c11 = new class Named {}();\n" +
				"const c12 = new function() {}();\n";
		List<AbstractCall> creations = module(code).getContainer().get().getAllCreations();
		assertEquals(List.of("Foo", "Foo", "Foo", "Foo", "Foo", "canUseWeakSet ? WeakSet : Set", "0, Baz", "this", "getCtor()", "class", "Named", "function"),
				creations.stream().map(AbstractCall::getName).toList());
		assertEquals(List.of("string"), creations.get(4).getTypeArguments().stream().map(UMLType::toString).toList());
	}

	@Test
	public void testOperationInvocationNames() throws Exception {
		String code = "const t1 = template.expression`BASE(REF)`({ BASE: 1 });\n" +
				"const t2 = tag`x`();\n";
		List<AbstractCall> invocations = module(code).getContainer().get().getAllOperationInvocations();
		assertEquals(List.of("template.expression`BASE(REF)`", "tag`x`"), invocations.stream().map(AbstractCall::getName).toList());
	}

	@Test
	public void testWrappedOperationInvocationNames() throws Exception {
		String code = "const r1 = (0, fn)(args);\n" +
				"const r2 = (fn as any)(args);\n" +
				"const r3 = (<any>fn)(args);\n" +
				"const r4 = fn!(args);\n" +
				"const r5 = (Foo == null ? void 0 : Foo.m)();\n" +
				"const r6 = (plugins?.some)(args);\n" +
				"const r7 = (fn1 || fn2)(args);\n" +
				"const r8 = (() => 1)();\n";
		List<AbstractCall> invocations = module(code).getContainer().get().getAllOperationInvocations();
		assertEquals(List.of("fn", "fn", "fn", "fn", "m", "some", "", ""), invocations.stream().map(AbstractCall::getName).toList());
		assertEquals("Foo", invocations.get(4).getExpression());
		assertEquals("plugins", invocations.get(5).getExpression());
	}

	private static UMLClass module(String code) throws Exception {
		UMLModel model = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/test.ts", code), Set.of("src"));
		return model.getClassList().get(0);
	}
}
