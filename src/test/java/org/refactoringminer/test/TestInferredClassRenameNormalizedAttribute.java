package org.refactoringminer.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLModel;

public class TestInferredClassRenameNormalizedAttribute {
	private static final String BEFORE = """
			export class OldWidget {
			  oldTitle: string = 'a';
			  oldCount: number = 1;
			}

			export function renderOld(w: OldWidget): string {
			  return w.oldTitle + String(w.oldCount);
			}
			""";

	private static final String AFTER = """
			export class NewWidget {
			  newTitle: string = 'a';
			  newCount: number = 1;
			}

			export function renderNew(w: NewWidget): string {
			  return w.newTitle + String(w.newCount);
			}
			""";

	@Test
	public void testInferredClassRenameWithQualifiedCandidateAttributes() throws Exception {
		UMLModel parent = GitHistoryRefactoringMinerImpl.createModel(
				Map.of("src/old.ts", BEFORE), Set.of("src"));
		UMLModel child = GitHistoryRefactoringMinerImpl.createModel(
				Map.of("src/new.ts", AFTER), Set.of("src"));

		List<Refactoring> refactorings = parent.diff(child).getRefactorings();

		assertTrue(refactorings.stream().anyMatch(r -> r.getRefactoringType() == RefactoringType.RENAME_CLASS
				|| r.getRefactoringType() == RefactoringType.MOVE_RENAME_CLASS),
				"Expected class rename refactoring, got: " + refactorings);
		assertTrue(refactorings.stream().anyMatch(r -> r.getRefactoringType() == RefactoringType.RENAME_ATTRIBUTE
				&& r.toString().contains("oldTitle") && r.toString().contains("newTitle")),
				"Expected Rename Attribute oldTitle -> newTitle, got: " + refactorings);
	}
}
