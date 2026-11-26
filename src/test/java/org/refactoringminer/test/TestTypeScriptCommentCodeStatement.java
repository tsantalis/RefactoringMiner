package org.refactoringminer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class TestTypeScriptCommentCodeStatement {
	private static final String JSDOC_BEFORE = """
			/**
			 * @class BadRequest
			 * @description A custom error to wrap the 400 HTTP status code.
			 * @example
			 * // import the error into a controller
			 * const BadRequest = require('lib/errors/BadRequest');
			 * throw new BadRequest('An authentication error occurred!');
			 */
			function BadRequest(description, key) {
			  this.status = 400;
			  this.code = key || 'ERRORS.BAD_REQUEST';
			  this.description = description || '';
			}
			""";

	private static final String JSDOC_AFTER = """
			/**
			 * @class BadRequest
			 * @description A custom error to wrap the 400 HTTP status code.
			 */
			class BadRequest extends Error {
			  constructor(description = '', code = 'ERRORS.BAD_REQUEST') {
			    super(description);
			    this.status = 400;
			    this.code = code;
			    this.description = description;
			  }
			}
			""";

	@Test
	public void testJsDocCommentDoesNotCrashJavaBlockParser() throws Exception {
		UMLModel parent = GitHistoryRefactoringMinerImpl.createModel(Map.of("server/lib/errors/BadRequest.js", JSDOC_BEFORE), Set.of("server"));
		UMLModel child = GitHistoryRefactoringMinerImpl.createModel(Map.of("server/lib/errors/BadRequest.js", JSDOC_AFTER), Set.of("server"));
		List<Refactoring> refactorings = parent.diff(child).getRefactorings();
		assertFalse(refactorings.isEmpty(), "Expected refactorings to be detected without parser failure");
	}

	private static final String COMMENTED_BEFORE = """
			function configure(opts) {
			  const mode = opts.mode;
			  this.status = 400;
			  return mode;
			}
			""";

	private static final String COMMENTED_AFTER = """
			function configure(opts) {
			  const mode = opts.mode;
			  // this.status = 400;
			  return mode;
			}
			""";

	@Test
	public void testCommentedJavaScriptStatementMatched() throws Exception {
		UMLModel parent = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/app.js", COMMENTED_BEFORE), Set.of("src"));
		UMLModel child = GitHistoryRefactoringMinerImpl.createModel(Map.of("src/app.js", COMMENTED_AFTER), Set.of("src"));
		UMLModelDiff modelDiff = parent.diff(child);
		List<UMLOperationBodyMapper> mappers = modelDiff.getCommonClassDiffList().stream()
				.map(UMLClassBaseDiff::getOperationBodyMapperList)
				.flatMap(List::stream)
				.toList();
		assertTrue(mappers.stream().anyMatch(m -> !m.getCommentedCode().isEmpty()),
				"Expected commented-out JS statement to be matched in commentedCode");
		assertEquals(Optional.empty(), UMLComment.typeScriptCodeStatement("src/app.js", "@class BadRequest\n@example\nthrow new BadRequest('x');"));
	}
}
