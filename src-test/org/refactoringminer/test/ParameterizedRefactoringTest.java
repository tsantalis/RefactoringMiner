package org.refactoringminer.test;

import net.joshka.junit.json.params.JsonFileSource;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.refactoringminer.utils.RefactoringJsonConverter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Iterator;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author  Victor Guerra Veloso victorgvbh@gmail.com
 */
public class ParameterizedRefactoringTest {

    @Disabled("This test is disabled because it is redundant with the testAllRefactorings")
    @ParameterizedTest
    @JsonFileSource(resources = "/oracle/data.json")
    public void testAllRefactoringsParameterized(@ConvertWith(RefactoringJsonConverter.class) RefactoringPopulator.Root testCase) throws Exception {
        GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
        GitService gitService = new GitServiceImpl();
        String folder = "tmp1/" + testCase.repository.substring(testCase.repository.lastIndexOf('/') + 1, testCase.repository.lastIndexOf('.'));
        try (Repository rep = gitService.cloneIfNotExists(folder, testCase.repository)) {
            detector.detectAtCommit(rep, testCase.sha1, new RefactoringHandler() {
                Set<String> foundRefactorings = null;

                @Override
                public boolean skipCommit(String commitId) {
                    return commitId != testCase.sha1;
                }

                @Override
                public void handle(String commitId, List<Refactoring> refactorings) {
                    foundRefactorings = new HashSet<>(refactorings.size());
                    for (Refactoring found : refactorings) {
                        foundRefactorings.add(found.getRefactoringType().getDisplayName());
                    }
                    for (RefactoringPopulator.Refactoring expectedRefactoring : testCase.refactorings) {
                        Assertions.assertTrue(foundRefactorings.remove(expectedRefactoring.type), String.format("Should find expected %s refactoring %s, but it is not found at commit %s%n", expectedRefactoring.validation, expectedRefactoring.type, testCase.sha1));
                    }
                }
            });
        }
        catch (Exception e) {
            Assertions.fail(e);
        }
    }
}
