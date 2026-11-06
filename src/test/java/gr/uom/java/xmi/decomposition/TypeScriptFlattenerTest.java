package gr.uom.java.xmi.decomposition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.caoccao.javet.swc4j.Swc4j;
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstModuleItem;
import com.caoccao.javet.swc4j.ast.program.Swc4jAstModule;
import com.caoccao.javet.swc4j.enums.Swc4jParseMode;
import com.caoccao.javet.swc4j.options.Swc4jParseOptions;

import gr.uom.java.xmi.TypeScriptFileProcessor;

public class TypeScriptFlattenerTest {
	private static final Swc4j SWC4J = new Swc4j();

	private static String print(String fileName, String code) throws Exception {
		Swc4jParseOptions options = new Swc4jParseOptions()
				.setSpecifier(Path.of(fileName).toUri().toURL())
				.setMediaType(TypeScriptFileProcessor.mediaType(fileName))
				.setCaptureAst(true)
				.setParseMode(Swc4jParseMode.Module);
		Swc4jAstModule module = (Swc4jAstModule) SWC4J.parse(code, options).getProgram();
		StringBuilder sb = new StringBuilder();
		for(ISwc4jAstModuleItem item : module.getBody()) {
			sb.append(TypeScriptFlattener.flatten(item, code));
		}
		return sb.toString();
	}

	private static void assertSamePrint(String fileName, String code1, String code2) throws Exception {
		assertEquals(print(fileName, code1), print(fileName, code2));
	}

	//printing the printed code gives the same code
	private static void assertIdempotent(String fileName, String code) throws Exception {
		String printed = print(fileName, code);
		assertEquals(printed, print(fileName, printed));
	}

	@Test
	public void testLineBreaksAndTrailingCommas() throws Exception {
		assertSamePrint("a.ts",
				"const total = parseDurationSeconds(step.staticDuration ?? step.duration);",
				"const total = parseDurationSeconds(\n  step.staticDuration ?? step.duration,\n);");
		assertSamePrint("a.ts",
				"const edges = all.filter((e) => ids.has(e.source) && ids.has(e.target));",
				"const edges = all.filter(\n  e =>\n    ids.has(e.source) &&\n    ids.has(e.target),\n)");
		assertSamePrint("a.ts",
				"const s = value.trim().replace(/\\s+/g, ' ').trim();",
				"const s = value\n    .trim()\n    .replace(/\\s+/g, ' ')\n    .trim();");
		assertSamePrint("a.ts",
				"const label = item?.name?.trim();",
				"const label = item\n  ?.name\n  ?.trim()");
	}

	@Test
	public void testSpacingAndCommentsAreNormalized() throws Exception {
		assertSamePrint("a.ts",
				"const o = { a: 1, b: [2, 3] };\nreturn ok ? value : null;",
				"const o={a:1,b:[2,3],}\nreturn ok?value:null");
		assertSamePrint("a.ts",
				"foo(a, b);",
				"foo(a, /* first */ b) // trailing");
	}

	@Test
	public void testCanonicalLayout() throws Exception {
		assertEquals("const o = { a: 1, b: [2, 3] };\n", print("a.ts", "const o = {a: 1, b: [2, 3]}"));
		assertEquals("items.map((item) => item.id);\n", print("a.ts", "items.map(item => item.id)"));
		//a block body prints one statement per line, without indentation
		assertEquals("function f(x: number): number {\nif (x > 0) {\nreturn x;\n} else {\nreturn -x;\n}\n}\n",
				print("a.ts", "function f(x: number): number {\n  if (x > 0) { return x } else { return -x }\n}"));
		//an object literal holding a function body prints one property per line
		assertEquals("const tool = define({\nname: 'click',\nhandler: async (request) => {\nawait request.run();\n}\n});\n",
				print("a.ts", "const tool = define({ name: 'click', handler: async (request) => { await request.run(); } });"));
	}

	@Test
	public void testJsx() throws Exception {
		assertSamePrint("a.tsx",
				"const v = <View style={cardStyle}><Text>Hello world</Text></View>;",
				"const v = (\n  <View style={cardStyle}>\n    {/* the greeting */}\n    <Text>\n      Hello\n      world\n    </Text>\n  </View>\n);".replace("(\n", "").replace("\n);", ";"));
		assertEquals("const v = <A b=\"c\" {...rest} />;\n", print("a.tsx", "const v = <A b=\"c\" {...rest}/>"));
	}

	@Test
	public void testTypes() throws Exception {
		assertEquals("let x: Map<string, Array<number | null>> = new Map();\n", print("a.ts", "let x : Map<string,Array<number|null>> = new Map()"));
		assertEquals("type F = (a: string, ...rest: number[]) => void;\n", print("a.ts", "type F = (a:string, ...rest:number[])=>void"));
		assertEquals("interface I {\nreadonly a?: string;\nm<T>(x: T): T;\n[key: string]: unknown;\n}\n",
				print("a.ts", "interface I { readonly a?: string, m<T>(x: T): T, [key: string]: unknown }"));
	}

	@Test
	public void testIdempotent() throws Exception {
		assertIdempotent("a.ts", "class A<T> extends B implements C {\n  private readonly x = 1;\n  static #y?: number;\n  constructor(public a: string) { super(); }\n  get v() { return this.x; }\n  async *gen() { yield* other(); }\n}");
		assertIdempotent("a.ts", "for await (const x of xs) { label: for (let i = 0, j = 1; i < j; i++) { continue label; } }");
		assertIdempotent("a.ts", "switch (k) { case 1: case 2: f(); break; default: g(); }\ntry { a() } catch { b() } finally { c() }");
		assertIdempotent("a.ts", "const { a, b: [c, , d = 1], ...rest } = obj;\nconst t = tag`x${y}z`;\nconst n = new Foo<string>;");
		assertIdempotent("a.ts", "export default class extends Base {}\nexport { a as b, type C } from './m';\nimport x, * as ns from 'y';");
		assertIdempotent("a.tsx", "export const App = () => <><A x={() => { return 1; }} />text {value}</>;");
	}
}
