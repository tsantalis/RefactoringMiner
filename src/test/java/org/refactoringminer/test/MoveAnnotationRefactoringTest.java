package org.refactoringminer.test;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.diff.AddClassAnnotationRefactoring;
import gr.uom.java.xmi.diff.MoveAnnotationRefactoring;
import gr.uom.java.xmi.diff.RemoveMethodAnnotationRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class MoveAnnotationRefactoringTest {
    private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";

    @Test
    void movesMethodAnnotationToClassDeclaration() throws Exception {
        String before = "package org.springframework.boot.actuate.hazelcast;\n" +
                "\n" +
                "import static org.junit.jupiter.api.Assertions.assertTrue;\n" +
                "\n" +
                "import org.junit.jupiter.api.Test;\n" +
                "import org.springframework.boot.test.support.classpath.resources.WithResource;\n" +
                "\n" +
                "class HazelcastHealthIndicatorTests {\n" +
                "    @Test\n" +
                "    @WithResource(name = \"hazelcast.xml\", content = \"<hazelcast/>\")\n" +
                "    void hazelcastUp() {\n" +
                "        assertTrue(true);\n" +
                "    }\n" +
                "}\n";

        String after = "package org.springframework.boot.actuate.hazelcast;\n" +
                "\n" +
                "import static org.junit.jupiter.api.Assertions.assertFalse;\n" +
                "import static org.junit.jupiter.api.Assertions.assertTrue;\n" +
                "\n" +
                "import org.junit.jupiter.api.Test;\n" +
                "import org.springframework.boot.test.support.classpath.resources.WithResource;\n" +
                "\n" +
                "@WithResource(name = \"hazelcast.xml\", content = \"<hazelcast/>\")\n" +
                "class HazelcastHealthIndicatorTests {\n" +
                "    @Test\n" +
                "    void hazelcastUp() {\n" +
                "        assertTrue(true);\n" +
                "    }\n" +
                "\n" +
                "    @Test\n" +
                "    void hazelcastShutdown() {\n" +
                "        assertFalse(false);\n" +
                "    }\n" +
                "}\n";

        UMLModel beforeModel = new UMLModelASTReader(Map.of("HazelcastHealthIndicatorTests.java", before), Set.of("."), false).getUmlModel();
        UMLModel afterModel = new UMLModelASTReader(Map.of("HazelcastHealthIndicatorTests.java", after), Set.of("."), false).getUmlModel();
        UMLModelDiff modelDiff = beforeModel.diff(afterModel);
        List<Refactoring> refactorings = modelDiff.getRefactorings();

        Assertions.assertAll(
                () -> Assertions.assertTrue(hasMoveAnnotation(refactorings, "WithResource"),
                        () -> "Expected Move Annotation refactoring, found: " + refactoringNames(refactorings)),
                () -> Assertions.assertFalse(hasConsumedAnnotationPair(refactorings),
                        () -> "Expected the add/remove pair for @WithResource to be coalesced, found: " + refactoringNames(refactorings))
        );
    }

    @Test
    void detectsIssue1054MethodAnnotationHoist() throws Exception {
        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
        UMLModelDiff modelDiff = miner.detectAtCommitWithGitHubAPI("https://github.com/spring-projects/spring-boot.git",
                "249620246f07f691c0dc79f2e219246b5f3a3340", new File(REPOS));
        List<Refactoring> refactorings = modelDiff.getRefactorings();

        Assertions.assertAll(
                () -> Assertions.assertTrue(hasMoveAnnotation(refactorings, "WithResource"),
                        () -> "Expected Move Annotation refactoring for issue #1054, found: " + refactoringNames(refactorings)),
                () -> Assertions.assertFalse(hasConsumedAnnotationPair(refactorings),
                        () -> "Expected the @WithResource add/remove pair to be coalesced for issue #1054, found: " + refactoringNames(refactorings))
        );
    }

    private static boolean hasMoveAnnotation(List<Refactoring> refactorings, String annotationTypeName) {
        return refactorings.stream().anyMatch(refactoring ->
                refactoring instanceof MoveAnnotationRefactoring moveAnnotationRefactoring &&
                        annotationTypeName.equals(moveAnnotationRefactoring.getAnnotation().getTypeName()));
    }

    private static boolean hasConsumedAnnotationPair(List<Refactoring> refactorings) {
        boolean hasClassAdd = refactorings.stream().anyMatch(refactoring ->
                refactoring instanceof AddClassAnnotationRefactoring addClassAnnotationRefactoring
                        && "WithResource".equals(addClassAnnotationRefactoring.getAnnotation().getTypeName()));
        boolean hasMethodRemove = refactorings.stream().anyMatch(refactoring ->
                refactoring instanceof RemoveMethodAnnotationRefactoring removeMethodAnnotationRefactoring
                        && "WithResource".equals(removeMethodAnnotationRefactoring.getAnnotation().getTypeName()));
        return hasClassAdd && hasMethodRemove;
    }

    private static List<String> refactoringNames(List<Refactoring> refactorings) {
        return refactorings.stream().map(Refactoring::getName).collect(Collectors.toList());
    }
}
