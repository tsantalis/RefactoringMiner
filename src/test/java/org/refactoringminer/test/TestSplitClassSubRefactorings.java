package org.refactoringminer.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLModel;

public class TestSplitClassSubRefactorings {
	private static final String BEFORE = """
			export class OriginalService {
			  private count: number = 0;

			  computeAlpha(x: number): number {
			    const base = x + 1;
			    return base * 2;
			  }

			  helperAlpha(value: number): number {
			    return value + 10;
			  }

			  computeBeta(y: number): number {
			    const factor = y + 3;
			    return factor * 4;
			  }
			}
			""";

	private static final String AFTER_A = """
			export class AlphaService {
			  private count: bigint = 0n;

			  computeAlpha(x: number): number {
			    const base = x + 1;
			    return base * 2;
			  }

			  helperAlpha(value: string): number {
			    return Number(value) + 10;
			  }
			}
			""";

	private static final String AFTER_B = """
			export class BetaService {
			  computeBeta(y: number): number {
			    const factor = y + 3;
			    return factor * 4;
			  }
			}
			""";

	@Test
	public void testSplitClassWithSubRefactoringsDoesNotInsertNull() throws Exception {
		UMLModel parent = GitHistoryRefactoringMinerImpl.createModel(
				Map.of("src/original.ts", BEFORE), Set.of("src"));
		UMLModel child = GitHistoryRefactoringMinerImpl.createModel(
				Map.of("src/alpha.ts", AFTER_A, "src/beta.ts", AFTER_B), Set.of("src"));

		List<Refactoring> refactorings = parent.diff(child).getRefactorings();

		assertFalse(refactorings.contains(null), "Detected refactorings must not contain null");
		assertTrue(refactorings.stream().anyMatch(r -> r.getRefactoringType() == RefactoringType.SPLIT_CLASS),
				"Expected SPLIT_CLASS refactoring, got: " + refactorings);
		assertTrue(refactorings.stream().anyMatch(r -> r.getRefactoringType() == RefactoringType.MOVE_OPERATION),
				"Expected MOVE_OPERATION refactoring, got: " + refactorings);
		assertTrue(refactorings.stream().anyMatch(r -> r.getRefactoringType() == RefactoringType.MOVE_ATTRIBUTE),
				"Expected MOVE_ATTRIBUTE refactoring, got: " + refactorings);
	}
}
