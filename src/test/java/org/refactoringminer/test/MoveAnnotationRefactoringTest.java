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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class MoveAnnotationRefactoringTest {
    @Test
    void movesMethodAnnotationToClassDeclaration() throws Exception {
        String before = """
                package org.springframework.boot.actuate.hazelcast;

                import static org.junit.jupiter.api.Assertions.assertTrue;

                import org.junit.jupiter.api.Test;
                import org.springframework.boot.test.support.classpath.resources.WithResource;

                class HazelcastHealthIndicatorTests {
                    @Test
                    @WithResource(name = "hazelcast.xml", content = "<hazelcast/>")
                    void hazelcastUp() {
                        assertTrue(true);
                    }
                }
                """;

        String after = """
                package org.springframework.boot.actuate.hazelcast;

                import static org.junit.jupiter.api.Assertions.assertFalse;
                import static org.junit.jupiter.api.Assertions.assertTrue;

                import org.junit.jupiter.api.Test;
                import org.springframework.boot.test.support.classpath.resources.WithResource;

                @WithResource(name = "hazelcast.xml", content = "<hazelcast/>")
                class HazelcastHealthIndicatorTests {
                    @Test
                    void hazelcastUp() {
                        assertTrue(true);
                    }

                    @Test
                    void hazelcastShutdown() {
                        assertFalse(false);
                    }
                }
                """;

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
        String before = """
                package org.springframework.boot.actuate.hazelcast;

                import org.junit.jupiter.api.Test;
                import org.springframework.boot.test.support.classpath.resources.WithResource;

                class HazelcastHealthIndicatorTests {
                    @Test
                    @WithResource(name = "hazelcast.xml", content = \"""
                            <hazelcast xmlns="http://www.hazelcast.com/schema/config">
                              <network>
                                <join>
                                  <auto-detection enabled="false"/>
                                  <multicast enabled="false"/>
                                </join>
                              </network>
                            </hazelcast>
                            \""")
                    void hazelcastUp() {
                    }
                }
                """;

        String after = """
                package org.springframework.boot.actuate.hazelcast;

                import org.junit.jupiter.api.Test;
                import org.springframework.boot.test.support.classpath.resources.WithResource;

                @WithResource(name = "hazelcast.xml", content = \"""
                        <hazelcast xmlns="http://www.hazelcast.com/schema/config">
                          <network>
                            <join>
                              <auto-detection enabled="false"/>
                              <multicast enabled="false"/>
                            </join>
                          </network>
                        </hazelcast>
                        \""")
                class HazelcastHealthIndicatorTests {
                    @Test
                    void hazelcastUp() {
                    }

                    @Test
                    void hazelcastShutdown() {
                    }
                }
                """;

        UMLModel beforeModel = new UMLModelASTReader(Map.of("HazelcastHealthIndicatorTests.java", before), Set.of("."), false).getUmlModel();
        UMLModel afterModel = new UMLModelASTReader(Map.of("HazelcastHealthIndicatorTests.java", after), Set.of("."), false).getUmlModel();
        UMLModelDiff modelDiff = beforeModel.diff(afterModel);
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
