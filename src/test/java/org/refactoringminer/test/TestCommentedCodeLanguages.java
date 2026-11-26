package org.refactoringminer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLComment;

//commented code is detected only for Java and Python (PR #1233)
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
	public void testCodeStatementOnlyForJavaAndPython() {
		assertEquals(Optional.of("x=y;\n"), comment("A.java", "// x = y;", CodeElementType.LINE_COMMENT).getCodeStatement());
		assertEquals(Optional.of("x=y\n"), comment("a.py", "# x = y", CodeElementType.LINE_COMMENT).getCodeStatement());
		for(String filePath : List.of("a.js", "a.jsx", "a.ts", "a.tsx", "a.kt", "a.c", "a.cpp", "a.h")) {
			assertEquals(Optional.empty(), comment(filePath, "// x = y;", CodeElementType.LINE_COMMENT).getCodeStatement(), filePath);
		}
	}

	@Test
	public void testJsDocDoesNotCrashJavaParser() {
		assertEquals(Optional.empty(), comment("server/lib/errors/NotFound.js", JSDOC, CodeElementType.BLOCK_COMMENT).getCodeStatement());
		//the same comment within a Java method body
		assertEquals(Optional.empty(), comment("NotFound.java", JSDOC, CodeElementType.BLOCK_COMMENT).getCodeStatement());
	}

	private static UMLComment comment(String filePath, String text, CodeElementType type) {
		return new UMLComment(text, new LocationInfo("", filePath, 0, text.length(), text.length(), type, text));
	}
}
