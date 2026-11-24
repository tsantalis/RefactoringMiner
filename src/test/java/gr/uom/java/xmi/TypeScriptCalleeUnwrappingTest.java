package gr.uom.java.xmi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.decomposition.AbstractCall;

public class TypeScriptCalleeUnwrappingTest {
	@Test
	public void testCalleeUnwrapping() throws Exception {
		String code = "const res1 = ParseErrorEnum`placeholders`(templates);\n" +
				"const res2 = (plugins?.some)(p => p === name);\n" +
				"const res3 = (0, fn)(args);\n" +
				"const res4 = (fn as any)(args);\n" +
				"const res5 = fn!(args);\n" +
				"const res6 = (fn)(args);\n" +
				"const res7 = (fn1 || fn2)(args);\n" +
				"const res8 = (target = fnTarget)(args);\n" +
				"const res9 = genericFn<string>(args);\n" +
				"const res10 = (Foo == null ? void 0 : Foo.m)();\n" +
				"const res11 = new Function()();\n";
		UMLModel model = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/test.ts", code), Set.of("src"));
		UMLClass moduleClass = model.getClassList().get(0);
		List<AbstractCall> calls = moduleClass.getContainer().get().getAllOperationInvocations();
		assertEquals(11, calls.size());
		assertEquals("ParseErrorEnum", calls.get(0).getName());
		assertEquals("some", calls.get(1).getName());
		assertEquals("fn", calls.get(2).getName());
		assertEquals("fn", calls.get(3).getName());
		assertEquals("fn", calls.get(4).getName());
		assertEquals("fn", calls.get(5).getName());
		assertEquals("fn1", calls.get(6).getName());
		assertEquals("fnTarget", calls.get(7).getName());
		assertEquals("genericFn", calls.get(8).getName());
		assertEquals("m", calls.get(9).getName());
		assertEquals("", calls.get(10).getName());
	}
}
