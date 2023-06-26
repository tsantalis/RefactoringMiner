package org.refactoringminer.astDiff.tests;

import com.github.gumtreediff.matchers.Mapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.astDiff.utils.MappingExportModel;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.astDiff.utils.UtilMethods;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.refactoringminer.astDiff.utils.UtilMethods.*;

/* Created by pourya on 2023-02-28 4:48 p.m. */
public class SpecificCasesTest {

    @Test
    public void testAmbiguousWithinComposite() throws Exception {
        String url = "https://github.com/pouryafard75/TestCases/commit/083ec23bc39ad46ebec7c68cb3931ee41891522e";
        Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(URLHelper.getRepo(url), URLHelper.getCommit(url), 1000);

        Set<Mapping> mappings = null;
        for (ASTDiff astDiff : astDiffs) {
            mappings = astDiff.getAllMappings().getMappings();
        }
        String filePath = "src-test/data/astDiff/commits/pouryafard75_TestCases/083ec23bc39ad46ebec7c68cb3931ee41891522e/Builder.v1.json";
        String expected = new String(Files.readAllBytes(Path.of(filePath)));
        String exportedMappings = MappingExportModel.exportString(mappings);
        assertEquals(expected.length(), exportedMappings.length(), "Different mappings for ambiguous composite");
        assertEquals(
                expected,
                exportedMappings,"Different mappings for ambiguous composite");
    }
    @Test
    public void testRenameParameter() throws Exception {

        String m1 = "SingleVariableDeclaration [3886,3897] -> SingleVariableDeclaration [2778,2797]";
        String m2 = "PrimitiveType: long [3886,3890] -> PrimitiveType: long [2778,2782]";
        String m3 = "SimpleName: millis [3891,3897] -> SimpleName: durationMillis [2783,2797]";
        String url = "https://github.com/apache/commons-lang/commit/5111ae7db08a70323a51a21df0bbaf46f21e072e";
        Set<ASTDiff> astDiffs = UtilMethods.getProjectDiffLocally(url);
        boolean executed = false;
        for (ASTDiff astDiff : astDiffs) {
            System.out.println();
            if (!astDiff.getSrcPath().equals("src/java/org/apache/commons/lang/time/DurationFormatUtils.java"))
                continue;
            Set<Mapping> mappings = astDiff.getAllMappings().getMappings();
            boolean m1Check = false, m2Check = false, m3Check = false;
            for (Mapping mapping : mappings) {
                if (mapping.toString().equals(m1)) m1Check = true;
                if (mapping.toString().equals(m2)) m2Check = true;
                if (mapping.toString().equals(m3)) m3Check = true;
            }
            assertTrue(m1Check, "SingleVariableDeclaration For RenameParameter Refactoring ");
            assertTrue(m2Check, "PrimitiveType Long For RenameParameter Refactoring ");
            assertTrue(m3Check, "SimpleName For RenameParameter Refactoring ");
            executed = true;
        }
        assertTrue(executed, "RenameParameter test case not executed properly");
    }
    @Test
    public void testExtractMethodReturnStatement() throws Exception {
        String returnTreeSrc = "ReturnStatement [17511,17714]";
        String url = "https://github.com/ReactiveX/RxJava/commit/8ad226067434cd39ce493b336bd0659778625959";
        Set<ASTDiff> astDiffs = UtilMethods.getProjectDiffLocally(url);
        boolean executed = false;
        for (ASTDiff astDiff : astDiffs) {
            if (!astDiff.getSrcPath().equals("src/test/java/rx/observables/BlockingObservableTest.java"))
                continue;
            Set<Mapping> mappings = astDiff.getAllMappings().getMappings();
            int numOfMappingsForReturnSubTree = 0;
            for (Mapping mapping : mappings) {
                if (mapping.first.toString().equals(returnTreeSrc))
                    numOfMappingsForReturnSubTree += 1;
            }
            executed = true;
            assertEquals(1, numOfMappingsForReturnSubTree, String.format("Number of mappings for %s not equal to 1", returnTreeSrc));
        }
        assertTrue(executed, "ExtractMethodReturnStatement not executed properly");
    }

    public static Stream<Arguments> initData() throws Exception {
        String url = "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";
        List<Arguments> allCases = new ArrayList<>();
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);
        List<CaseInfo> infos = new ArrayList<>();
        infos.add(new CaseInfo(repo,commit));
        for (CaseInfo info : infos) {
            List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(new File(getFinalFolderPath(getCommitsMappingsPath(), info.getRepo(), info.getCommit())).list())));
            Set<ASTDiff> astDiffs = UtilMethods.getProjectDiffLocally(url);
            makeAllCases(allCases, info, expectedFilesList, astDiffs, getCommitsMappingsPath());
        }
        return allCases.stream();
    }
    @Test
    public void testMethodReference() throws Exception {
        String url = "https://github.com/pouryafard75/TestCases/commit/562c4447a566170ac28872a88b323669a82db5c9";
        Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(URLHelper.getRepo(url), URLHelper.getCommit(url), 1000);
        Set<Mapping> mappings = null;
        for (ASTDiff astDiff : astDiffs) {
            mappings = astDiff.getAllMappings().getMappings();
        }
        String filePath = "src-test/data/astDiff/commits/pouryafard75_TestCases/562c4447a566170ac28872a88b323669a82db5c9/Builder.MethodRef.ByteBufferLogInputStreamTest.json";
        String expected = new String(Files.readAllBytes(Path.of(filePath)));
        String exportedMappings = MappingExportModel.exportString(mappings);
        assertEquals(expected.length(), exportedMappings.length(), "Different mappings for mock migration commit");
        assertEquals(
                expected,
                exportedMappings,"Different mappings for mock migration commit");
    }
    @ParameterizedTest(name= "{index}: File: {2}, Repo: {0}, Commit: {1}")
    @MethodSource("initData")
    public void toyExampleTest(String repo, String commit, String srcFileName, String expected, String actual) {
        String msg = String.format("Failed for %s/commit/%s , srcFileName: %s", repo.replace(".git", ""), commit, srcFileName);
        assertEquals(expected.length(), actual.length(), msg);
        assertEquals(expected, actual, msg);
    }
}