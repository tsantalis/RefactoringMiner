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
				"const res11 = new Function()();\n" +
				"const res12 = (<any>fn)(args);\n" +
				"const res13 = (cond ? fnA : fnB)(args);\n" +
				"const res14 = (this)(args);\n" +
				"const res15 = (getFn?.())(args);\n" +
				"const res16 = getFn()(args);\n" +
				"async function a() { return (await fetchFn)(args); }\n" +
				"function* g() { return (yield genFn)(args); }\n";
		UMLModel model = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/test.ts", code), Set.of("src"));
		UMLClass moduleClass = model.getClassList().get(0);
		List<AbstractCall> calls = moduleClass.getContainer().get().getAllOperationInvocations();
		assertEquals(17, calls.size());
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
		assertEquals("fn", calls.get(11).getName());
		assertEquals("fnA", calls.get(12).getName());
		assertEquals("this", calls.get(13).getName());
		assertEquals("getFn", calls.get(14).getName());
		assertEquals("getFn", calls.get(15).getName());
		assertEquals("getFn", calls.get(16).getName());
		UMLOperation a = moduleClass.getOperations().get(0);
		assertEquals("fetchFn", a.getAllOperationInvocations().get(0).getName());
		UMLOperation g = moduleClass.getOperations().get(1);
		assertEquals("", g.getAllOperationInvocations().get(0).getName());
	}
}
