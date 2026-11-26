package org.refactoringminer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;

//commented code is detected only for Java, Python, TypeScript and JavaScript (PR #1233)
public class TestCommentedCodeLanguages {
	//JSDoc from third-culture-software/bhima server/lib/errors/NotFound.js, which made the statement recovery of JDT throw IllegalArgumentException (negative length)
	private static final String JSDOC = """
			/**
			 * @class NotFound
			 *
			 * @description
			 * A custom error to wrap the 404 HTTP status code within the server.  This
			 * should only be thrown in a context where it can be caught by ExpressJS's
			 * {@link http://expressjs.com/en/guide/routing.html|next()} function and
			 * returned to the client.
			 *
			 * @param {String} description - a custom description to be sent to the client
			 *
			 * @example
			 * // import the error into a controller
			 * const NotFound = require('lib/errors/NotFound');
			 *
			 * // use by directly throwing ...
			 * throw new NotFound('An authentication error occurred!');
			 *
			 * // or by calling next in the server context
			 * next(new NotFound('Some description...'));
			 *
			 * // or by combining both in a promise chain!
			 * Promise.then(() => {
			 *   throw new NotFound('This will be caught in Promise.catch()');
			 * })
			 * .catch(next);
			 *
			 * @requires util
			 */""";

	@Test
	public void testCodeStatementOnlySupportedLanguages() {
		assertEquals(Optional.of("x=y;\n"), comment("A.java", "// x = y;", CodeElementType.LINE_COMMENT).getCodeStatement());
		assertEquals(Optional.of("x=y\n"), comment("a.py", "# x = y", CodeElementType.LINE_COMMENT).getCodeStatement());
		for(String filePath : List.of("a.kt", "a.c", "a.cpp", "a.h")) {
			assertEquals(Optional.empty(), comment(filePath, "// x = y;", CodeElementType.LINE_COMMENT).getCodeStatement(), filePath);
		}
	}

	@Test
	public void testJsDocDoesNotCrashJavaParser() {
		assertEquals(Optional.empty(), comment("server/lib/errors/NotFound.js", JSDOC, CodeElementType.BLOCK_COMMENT).getCodeStatement());
		//the same comment within a Java method body
		assertEquals(Optional.empty(), comment("NotFound.java", JSDOC, CodeElementType.BLOCK_COMMENT).getCodeStatement());
	}

	@Test
	public void testTypeScriptCodeStatement() {
		assertEquals(Optional.of("this.status = 400;\n"), UMLComment.typeScriptCodeStatement("a.js", "this.status = 400;"));
		assertEquals(Optional.of("this.status = 400;\n"), comment("a.js", "// this.status = 400;", CodeElementType.LINE_COMMENT).getCodeStatement());
		assertEquals(Optional.of("const x: number = await f(a, b);\n"), UMLComment.typeScriptCodeStatement("a.ts", "const x: number = await f(a, b);"));
		assertEquals(Optional.of("return <div>{x}</div>;\n"), UMLComment.typeScriptCodeStatement("a.tsx", "return <div>{x}</div>;"));
		//not code, several statements, unbalanced braces
		assertEquals(Optional.empty(), UMLComment.typeScriptCodeStatement("a.js", "TODO remove this when the server is fixed"));
		assertEquals(Optional.empty(), UMLComment.typeScriptCodeStatement("a.js", "a = 1; b = 2;"));
		assertEquals(Optional.empty(), UMLComment.typeScriptCodeStatement("a.js", "} function g() {"));
		assertEquals(Optional.empty(), UMLComment.typeScriptCodeStatement("a.js", "if (x) {"));
	}

	private static final String JS_STATEMENT = """
			function configure(opts) {
			  const mode = opts.mode;
			  this.status = 400;
			  return mode;
			}
			""";

	private static final String JS_COMMENTED = """
			function configure(opts) {
			  const mode = opts.mode;
			  // this.status = 400;
			  return mode;
			}
			""";

	@Test
	public void testJavaScriptStatementCommentedAndUncommented() throws Exception {
		UMLModel withStatement = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/app.js", JS_STATEMENT), Set.of("src"));
		UMLModel withComment = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/app.js", JS_COMMENTED), Set.of("src"));
		UMLOperationBodyMapper commented = mapper(withStatement, withComment);
		assertEquals(1, commented.getCommentedCode().size());
		assertEquals("this.status = 400;\n", commented.getCommentedCode().iterator().next().getLeft().getString());
		UMLOperationBodyMapper uncommented = mapper(withComment, withStatement);
		assertEquals(1, uncommented.getUnCommentedCode().size());
		assertEquals("this.status = 400;\n", uncommented.getUnCommentedCode().iterator().next().getRight().getString());
	}

	private static UMLOperationBodyMapper mapper(UMLModel parent, UMLModel child) throws Exception {
		List<UMLOperationBodyMapper> mappers = parent.diff(child).getCommonClassDiffList().stream()
				.map(UMLClassBaseDiff::getOperationBodyMapperList)
				.flatMap(List::stream)
				.filter(m -> m.getContainer1().getName().equals("configure"))
				.toList();
		assertEquals(1, mappers.size());
		return mappers.get(0);
	}

	private static UMLComment comment(String filePath, String text, CodeElementType type) {
		return new UMLComment(text, new LocationInfo("", filePath, 0, text.length(), text.length(), type, text));
	}
}
