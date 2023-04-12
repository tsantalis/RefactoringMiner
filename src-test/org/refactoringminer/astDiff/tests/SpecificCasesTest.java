package org.refactoringminer.astDiff.tests;

import com.github.gumtreediff.matchers.Mapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.refactoringminer.astDiff.utils.UtilMethods.*;

/* Created by pourya on 2023-02-28 4:48 p.m. */
public class SpecificCasesTest {
    @Test
    public void testRenameParameter() {
        String m1 = "SingleVariableDeclaration [3886,3897] -> SingleVariableDeclaration [2778,2797]";
        String m2 = "PrimitiveType: long [3886,3890] -> PrimitiveType: long [2778,2782]";
        String m3 = "SimpleName: millis [3891,3897] -> SimpleName: durationMillis [2783,2797]";
        String url = "https://github.com/apache/commons-lang/commit/5111ae7db08a70323a51a21df0bbaf46f21e072e";
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);
        Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);
        boolean executed = false;
        for (ASTDiff astDiff : astDiffs) {
            System.out.println();
            if (!astDiff.getSrcPath().equals("src/java/org/apache/commons/lang/time/DurationFormatUtils.java"))
                continue;
            Set<Mapping> mappings = astDiff.getMultiMappings().getMappings();
            boolean m1Check = false, m2Check = false, m3Check = false;
            for (Mapping mapping : mappings) {
                if (mapping.toString().equals(m1)) m1Check = true;
                if (mapping.toString().equals(m2)) m2Check = true;
                if (mapping.toString().equals(m3)) m3Check = true;
            }
            assertTrue("SingleVariableDeclaration For RenameParameter Refactoring ", m1Check);
            assertTrue("PrimitiveType Long For RenameParameter Refactoring ", m2Check);
            assertTrue("SimpleName For RenameParameter Refactoring ", m3Check);
            executed = true;
        }
        assertTrue("RenameParameter test case not executed properly", executed);

    }

    @Test
    public void testExtractMethodReturnStatement() {
        String returnTreeSrc = "ReturnStatement [17511,17714]";
        String url = "https://github.com/ReactiveX/RxJava/commit/8ad226067434cd39ce493b336bd0659778625959";
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);
        Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);
        boolean executed = false;
        for (ASTDiff astDiff : astDiffs) {
            if (!astDiff.getSrcPath().equals("src/test/java/rx/observables/BlockingObservableTest.java"))
                continue;
            Set<Mapping> mappings = astDiff.getMultiMappings().getMappings();
            int numOfMappingsForReturnSubTree = 0;
            for (Mapping mapping : mappings) {
                if (mapping.first.toString().equals(returnTreeSrc))
                    numOfMappingsForReturnSubTree += 1;
            }
            executed = true;
            assertEquals(String.format("Number of mappings for %s not equal to 1", returnTreeSrc), 1, numOfMappingsForReturnSubTree);
        }
        assertTrue("ExtractMethodReturnStatement not executed properly", executed);
    }

    public static Stream<Arguments> initData() throws IOException {
        String url = "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";
        List<Arguments> allCases = new ArrayList<>();
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);
        boolean executed = false;
        List<CaseInfo> infos = new ArrayList<>();
        infos.add(new CaseInfo(repo,commit));
        for (CaseInfo info : infos) {
            List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(new File(getFinalFolderPath(getCommitsMappingsPath(), info.getRepo(), info.getCommit())).list())));
            Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);
            makeAllCases(allCases, info, expectedFilesList, astDiffs);
        }
        return allCases.stream();
    }
    @ParameterizedTest(name= "{index}: File: {2}, Repo: {0}, Commit: {1}")
    @MethodSource("initData")
    public void testChecker(String repo, String commit, String srcFileName, String expected, String actual) {
        String msg = String.format("Failed for %s/commit/%s , srcFileName: %s",repo.replace(".git",""),commit,srcFileName);
        assertEquals(msg, expected.length(),actual.length());
        assertEquals(msg, expected,actual);
    }

}