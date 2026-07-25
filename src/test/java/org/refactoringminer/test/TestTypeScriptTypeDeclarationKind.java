package org.refactoringminer.test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.ChangeTypeDeclarationKindRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;

/**
 * Regression tests for issue #1124.
 * A TypeScript type alias has its own declaration kind and must not be
 * reported as a class.
 */
public class TestTypeScriptTypeDeclarationKind {

	private static ChangeTypeDeclarationKindRefactoring detect(String before, String after) throws Exception {
		String path = "src/app/account/account.types.ts";
		Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();
		fileContentsBefore.put(path, before);
		fileContentsCurrent.put(path, after);
		UMLModel parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, new LinkedHashSet<String>());
		UMLModel currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, new LinkedHashSet<String>());
		UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
		ChangeTypeDeclarationKindRefactoring found = null;
		for(Refactoring ref : modelDiff.getRefactorings()) {
			if(ref.getRefactoringType().equals(RefactoringType.CHANGE_TYPE_DECLARATION_KIND)) {
				Assertions.assertNull(found, "more than one Change Type Declaration Kind reported");
				found = (ChangeTypeDeclarationKindRefactoring) ref;
			}
		}
		return found;
	}

	@Test
	public void testInterfaceToTypeAliasIsNotReportedAsClass() throws Exception {
		String before =
				"import { User } from '@/app/admin/users/users.types';\n" +
				"\n" +
				"export interface Account extends User {}\n";
		String after =
				"import { User } from '@/app/admin/users/users.types';\n" +
				"\n" +
				"export type Account = User;\n";
		ChangeTypeDeclarationKindRefactoring ref = detect(before, after);
		Assertions.assertNotNull(ref);
		Assertions.assertEquals("interface", ref.getOriginalTypeDeclarationKind());
		Assertions.assertEquals("type alias", ref.getChangedTypeDeclarationKind());
		// toString() separates the refactoring name from its arguments with a tab
		Assertions.assertEquals(
				"Change Type Declaration Kind\tinterface to type alias in type account.types.Account",
				ref.toString());
	}

	@Test
	public void testTypeAliasToInterface() throws Exception {
		String before =
				"export type Account = { id: string; name: string };\n";
		String after =
				"export interface Account { id: string; name: string }\n";
		ChangeTypeDeclarationKindRefactoring ref = detect(before, after);
		Assertions.assertNotNull(ref);
		Assertions.assertEquals("type alias", ref.getOriginalTypeDeclarationKind());
		Assertions.assertEquals("interface", ref.getChangedTypeDeclarationKind());
	}

	@Test
	public void testTypeAliasToTypeAliasIsNotReported() throws Exception {
		String before =
				"export type Account = { id: string };\n";
		String after =
				"export type Account = { id: string; name: string };\n";
		Assertions.assertNull(detect(before, after));
	}
}
