package org.refactoringminer.astDiff.tests;

import com.github.gumtreediff.matchers.Mapping;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.astDiff.utils.MappingExportModel;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.astDiff.utils.UtilMethods;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.refactoringminer.astDiff.utils.ExportUtils.*;
import static org.refactoringminer.astDiff.utils.UtilMethods.*;

/* Created by pourya on 2023-02-28 4:48 p.m. */
public class SpecificCasesTest {
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
    private static void singleFileChecker(String url, String filePath) throws Exception {
        Set<ASTDiff> astDiffs = UtilMethods.getProjectDiffLocally(url);
        ASTDiff astDiff = astDiffs.iterator().next();
        String calculated = MappingExportModel.exportString(astDiff.getAllMappings()).replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
        String expected = FileUtils.readFileToString(new File(filePath), "utf-8");
        JSONAssert.assertEquals(expected, calculated, false);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "MethodInvocation/" + "," + "https://github.com/pouryafard75/TestCases/commit/f5a6302a6c16b5f52c219ba47566cb424170aec2" + "," + "src/test/resources/astDiff/commits/pouryafard75_TestCases/f5a6302a6c16b5f52c219ba47566cb424170aec2/Builder.MI.MethodInvocation.json",
            "Builder_KUBERNETES" + "," + "https://github.com/pouryafard75/TestCases/commit/f73e580d89fa63627601699a683d08f87d074c6a"  + "," + "src/test/resources/astDiff/commits/pouryafard75_TestCases/f73e580d89fa63627601699a683d08f87d074c6a/Builder.builderPattern.json",
            "Builder_ABCD" + "," + "https://github.com/pouryafard75/TestCases/commit/6fbf6c71e18addbca9d2a6fb56c45bfde4934525"  + "," + "src/test/resources/astDiff/commits/pouryafard75_TestCases/6fbf6c71e18addbca9d2a6fb56c45bfde4934525/Builder.kubernetes.chain.json",
            "MethodReference" + "," + "https://github.com/pouryafard75/TestCases/commit/562c4447a566170ac28872a88b323669a82db5c9"  + "," + "src/test/resources/astDiff/commits/pouryafard75_TestCases/562c4447a566170ac28872a88b323669a82db5c9/Builder.MethodRef.ByteBufferLogInputStreamTest.json",
            "InfixExpression" + "," + "https://github.com/pouryafard75/TestCases/commit/76ab18eeb36f3bc0a8e6a5655d970657187df276" + "," + "src/test/resources/astDiff/commits/pouryafard75_TestCases/76ab18eeb36f3bc0a8e6a5655d970657187df276/Builder.IfCondition.json",
            "AmbiguousWithinComposite" + "," + "https://github.com/pouryafard75/TestCases/commit/083ec23bc39ad46ebec7c68cb3931ee41891522e" + "," + "src/test/resources/astDiff/commits/pouryafard75_TestCases/083ec23bc39ad46ebec7c68cb3931ee41891522e/Builder.v1.json",
            "SamePositionInParentAmbiguous" + "," + "https://github.com/pouryafard75/TestCases/commit/ac1ec4f7c775423d3170887d7eefcccd013f2ae0" + "," + "src/test/resources/astDiff/commits/pouryafard75_TestCases/ac1ec4f7c775423d3170887d7eefcccd013f2ae0/Builder.sus.sus.json"
    })
    public void testRecreatedCases(String name, String url, String filePath) throws Exception {
        singleFileChecker(url,filePath);
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
    @ParameterizedTest(name= "{index}: File: {2}, Repo: {0}, Commit: {1}")
    @MethodSource("initData")
    public void toyExampleTest(String repo, String commit, String srcFileName, String expected, String actual) {
        String msg = String.format("Failed for %s/commit/%s , srcFileName: %s", repo.replace(".git", ""), commit, srcFileName);
        assertEquals(expected.length(), actual.length(), msg);
        assertEquals(expected, actual, msg);
    }
    private static void makeAllCases(List<Arguments> allCases, CaseInfo info, List<String> expectedFilesList, Set<ASTDiff> astDiffs, String mappingsPath) throws IOException {
        for (ASTDiff astDiff : astDiffs) {
            String finalFilePath = getFinalFilePath(astDiff, mappingsPath, info.getRepo(), info.getCommit());
            String calculated = MappingExportModel.exportString(astDiff.getAllMappings()).replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
            String expected = FileUtils.readFileToString(new File(finalFilePath), "utf-8");
            allCases.add(Arguments.of(info.getRepo(),info.getCommit(),astDiff.getSrcPath(),expected,calculated));
            expectedFilesList.remove(getFileNameFromSrcDiff(astDiff.getSrcPath()));
        }
        for (String expectedButNotGeneratedFile : expectedFilesList) {
            String expectedDiffName = getSrcASTDiffFromFile(expectedButNotGeneratedFile);
            allCases.add(Arguments.of
                    (
                            info.getRepo(),info.getCommit(),expectedDiffName,"{JSON}","NOT GENERATED"
                    )
            );
        }
    }
}