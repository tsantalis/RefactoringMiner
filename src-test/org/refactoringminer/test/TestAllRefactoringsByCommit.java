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
public class TestAllRefactoringsByCommit {
    private static final String REPOS = "tmp1";

    @Disabled("This test is disabled because it is redundant with the testAllRefactorings")
    @ParameterizedTest
    @JsonFileSource(resources = "/oracle/data.json")
    public void testAllRefactoringsParameterized(@ConvertWith(RefactoringJsonConverter.class) RefactoringPopulator.Root testCase) throws Exception {
        GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
        GitService gitService = new GitServiceImpl();
        String folder = REPOS + "/" + testCase.repository.substring(testCase.repository.lastIndexOf('/') + 1, testCase.repository.lastIndexOf('.'));
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
                        foundRefactorings.add(found.toString().replace("\t"," "));
                    }
                    Iterator<RefactoringPopulator.Refactoring> iter = testCase.refactorings.iterator();
                    while(iter.hasNext()){
                        RefactoringPopulator.Refactoring expectedRefactoring = iter.next();
                        String description = expectedRefactoring.description;
                        iter.remove();
                        Assertions.assertTrue(foundRefactorings.remove(description), String.format("Should find expected %s refactoring %s, but it is not found at commit %s (%s)%n", expectedRefactoring.validation, description, testCase.sha1,foundRefactorings));
                    }
                    Assertions.assertEquals(Collections.emptySet(), foundRefactorings, String.format("Should have zero False Positives, but False Positives were found: %s", foundRefactorings.toString()));
                    Assertions.assertEquals(Collections.emptyList(), testCase.refactorings, String.format("Should have zero False Negatives, but False Negatives were found: %s", testCase.refactorings.toString()));
                }
            });
        }
        catch (Exception e) {
            Assertions.fail(e);
        }
    }
}
