package org.refactoringminer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLModel;

//a Python dict literal or a TypeScript object literal extracted to or inlined from a variable, i.e., NikolAI PR 127 pull_request_payload
public class TestObjectLiteralVariable {
	private static final String PYTHON_RETURN = """
			def payload(action):
			    return {"action": action, "id": 100, "repo": {"id": 200, "private": True}}
			""";
	private static final String PYTHON_VARIABLE = """
			def payload(action, head_sha=None):
			    payload = {"action": action, "id": 100, "repo": {"id": 200, "private": True}}
			    if head_sha is not None:
			        payload["repo"]["head"] = head_sha
			    return payload
			""";
	private static final String TYPESCRIPT_RETURN = """
			export function payload(action: string) {
			  return {action: action, id: 100, repo: {id: 200, private: true}};
			}
			""";
	private static final String TYPESCRIPT_VARIABLE = """
			export function payload(action: string, headSha?: string) {
			  const payload = {action: action, id: 100, repo: {id: 200, private: true}};
			  if (headSha !== undefined) {
			    payload.repo.head = headSha;
			  }
			  return payload;
			}
			""";

	@Test
	public void testExtractDictLiteral() throws Exception {
		assertEquals(List.of("Extract Variable\tpayload in method public payload(action, head_sha) from class a.__module__",
				"Add Parameter\thead_sha in method public payload(action, head_sha) from class a.__module__"),
				refactorings("src/a.py", PYTHON_RETURN, PYTHON_VARIABLE));
	}

	@Test
	public void testInlineDictLiteral() throws Exception {
		assertEquals(List.of("Inline Variable\tpayload in method public payload(action, head_sha) from class a.__module__",
				"Remove Parameter\thead_sha in method public payload(action, head_sha) from class a.__module__"),
				refactorings("src/a.py", PYTHON_VARIABLE, PYTHON_RETURN));
	}

	@Test
	public void testExtractObjectLiteral() throws Exception {
		assertEquals(List.of("Extract Variable\tpayload : Inferred in method public payload(action string, headSha string) from class a",
				"Add Parameter\theadSha : string in method public payload(action string, headSha string) from class a"),
				refactorings("src/a.ts", TYPESCRIPT_RETURN, TYPESCRIPT_VARIABLE));
	}

	private static List<String> refactorings(String filePath, String before, String after) throws Exception {
		UMLModel parent = GitHistoryRefactoringMinerImpl.createModel(Map.of(filePath, before), Set.of("src"));
		UMLModel child = GitHistoryRefactoringMinerImpl.createModel(Map.of(filePath, after), Set.of("src"));
		return parent.diff(child).getRefactorings().stream().map(Refactoring::toString).toList();
	}
}
