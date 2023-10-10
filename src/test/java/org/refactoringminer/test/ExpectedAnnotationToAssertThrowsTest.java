package org.refactoringminer.test;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.AssertThrowsRefactoring;
import gr.uom.java.xmi.diff.ModifyMethodAnnotationRefactoring;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(Enclosed.class)
public class ExpectedAnnotationToAssertThrowsTest {
    abstract public static class ModelDiffFieldSetUp {
        UMLModelDiff modelDiff;

        @Before
        public void setUp() throws RefactoringMinerTimedOutException {
        	String inlineVersionTestMethod = TestOperationDiffMother.createExampleTestMethod_InlineVersion();
        	String inlineVersionTestClass = TestOperationDiffMother.createExampleClassTestCode(inlineVersionTestMethod);
            UMLModel before = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", inlineVersionTestClass), Set.of(), false).getUmlModel();
            String assertVersionTestMethod = TestOperationDiffMother.createExampleTestMethod_AssertVersion();
            String assertVersionTestClass = TestOperationDiffMother.createExampleClassTestCode(assertVersionTestMethod);
            UMLModel after = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", assertVersionTestClass), Set.of(), false).getUmlModel();
            modelDiff = before.diff(after);
        }
    }
    public static class RegressionTest {
        UMLModelDiff modelDiff;

        @Before
        public void setUp() throws RefactoringMinerTimedOutException {
            String inlineVersionTestClass = "package uk.gov.hmcts.reform.cwrdapi.service.impl;\n" +
                    "\n" +
                    "public class CaseWorkerServiceImplTest {\n" +
                    "    @Test(expected = InvalidRequestException.class)\n" +
                    "    public void testInvalidRequestExceptionForInvalidSortColumn() {\n" +
                    "        validateAndBuildPaginationObject(0, 1,\n" +
                    "                \"invalid\", \"ASC\",\n" +
                    "                20, \"invalid\", CaseWorkerProfile.class);\n" +
                    "    }\n" +
                    "    @Test(expected = StaffReferenceException.class)\n" +
                    "    public void testRefreshRoleAllocationWhenLrdResponseReturns400() throws JsonProcessingException {\n" +
                    "        ErrorResponse errorResponse = ErrorResponse\n" +
                    "                .builder()\n" +
                    "                .errorCode(400)\n" +
                    "                .errorDescription(\"testErrorDesc\")\n" +
                    "                .errorMessage(\"testErrorMsg\")\n" +
                    "                .build();\n" +
                    "        String body = mapper.writeValueAsString(errorResponse);\n" +
                    "\n" +
                    "        when(locationReferenceDataFeignClient.getLocationRefServiceMapping(\"cmc\"))\n" +
                    "                .thenReturn(Response.builder()\n" +
                    "                        .request(mock(Request.class)).body(body, defaultCharset()).status(400).build());\n" +
                    "        PageRequest pageRequest = RequestUtils.validateAndBuildPaginationObject(0, 1,\n" +
                    "                \"caseWorkerId\", \"ASC\",\n" +
                    "                20, \"id\", CaseWorkerProfile.class);\n" +
                    "\n" +
                    "        caseWorkerServiceImpl\n" +
                    "                .fetchStaffProfilesForRoleRefresh(\"cmc\", pageRequest);\n" +
                    "    }\n" +
                    "}";
            UMLModel before = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", inlineVersionTestClass), Set.of(), false).getUmlModel();
            String assertVersionTestClass = "package uk.gov.hmcts.reform.cwrdapi.service.impl;\n" +
                    "\n" +
                    "public class CaseWorkerServiceImplTest {\n" +
                    "    @Test\n" +
                    "    public void testInvalidRequestExceptionForInvalidSortColumn() {\n" +
                    "        Assertions.assertThrows(Exception.class, () -> {\n" +
                    "            validateAndBuildPaginationObject(0, 1,\n" +
                    "                    \"invalid\", \"ASC\",\n" +
                    "                    20, \"invalid\", CaseWorkerProfile.class);\n" +
                    "        });\n" +
                    "    }\n" +
                    "    @Test\n" +
                    "    public void testRefreshRoleAllocationWhenLrdResponseReturns400() throws JsonProcessingException {\n" +
                    "        ErrorResponse errorResponse = ErrorResponse\n" +
                    "                .builder()\n" +
                    "                .errorCode(400)\n" +
                    "                .errorDescription(\"testErrorDesc\")\n" +
                    "                .errorMessage(\"testErrorMsg\")\n" +
                    "                .build();\n" +
                    "        String body = mapper.writeValueAsString(errorResponse);\n" +
                    "\n" +
                    "        when(locationReferenceDataFeignClient.getLocationRefServiceMapping(\"cmc\"))\n" +
                    "                .thenReturn(Response.builder()\n" +
                    "                        .request(mock(Request.class)).body(body, defaultCharset()).status(400).build());\n" +
                    "        PageRequest pageRequest = RequestUtils.validateAndBuildPaginationObject(0, 1,\n" +
                    "                \"caseWorkerId\", \"ASC\",\n" +
                    "                20, \"id\", CaseWorkerProfile.class);\n" +
                    "\n" +
                    "        Assertions.assertThrows(StaffReferenceException.class, () -> {\n" +
                    "            caseWorkerServiceImpl\n" +
                    "                    .fetchStaffProfilesForRoleRefresh(\"cmc\", pageRequest);\n" +
                    "        });\n" +
                    "    }\n" +
                    "}";
            UMLModel after = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", assertVersionTestClass), Set.of(), false).getUmlModel();
            modelDiff = before.diff(after);
        }
        @Test
        public void testFromInlineToAssertThrows_firstModifiedMethodAnnotationBelongsToOtherMethod() throws RefactoringMinerTimedOutException {
            List<Refactoring> refactorings = modelDiff.getRefactorings();
            Assert.assertEquals(4, refactorings.size());
            Assert.assertTrue(refactorings.stream().allMatch(r -> r instanceof ModifyMethodAnnotationRefactoring || r instanceof AssertThrowsRefactoring));
        }
    }

    public static class ImplementationTest extends ModelDiffFieldSetUp {
        @Test
        public void testFromInlineToAssertThrows_detector() throws RefactoringMinerTimedOutException {
            UMLClassBaseDiff classDiff = modelDiff.getUMLClassDiff("ca.concordia.victor.exception.ExampleClassTest");
            Assert.assertNotNull(classDiff);
            Assert.assertEquals(2, classDiff.getOperationBodyMapperList().size());
            Optional<UMLOperationBodyMapper> testMethodMapperOptional = classDiff.getOperationBodyMapperList().stream().filter(UMLOperationBodyMapper::involvesTestMethods).findAny();
            Assert.assertTrue(testMethodMapperOptional.isPresent());
            UMLOperationBodyMapper mapper = testMethodMapperOptional.get();
            List<UMLAnnotation> annotationsBefore = mapper.getOperation1().getAnnotations();
            Assert.assertEquals(annotationsBefore.size(), 1);
            List<UMLAnnotation> annotationsAfter = mapper.getOperation2().getAnnotations();
            Assert.assertEquals(annotationsAfter.size(), 1);
            List<Refactoring> refactorings = classDiff.getRefactorings();
            Optional<Refactoring> possibleRefactoring = refactorings.stream()
                    .filter(r -> r instanceof AssertThrowsRefactoring)
                    .findAny();
            Assert.assertTrue(possibleRefactoring.isPresent());
            AssertThrowsRefactoring refactoring = (AssertThrowsRefactoring) possibleRefactoring.get();
            Assert.assertEquals(mapper.getOperation2(), refactoring.getOperationAfter());
            Assert.assertEquals(mapper.getOperation1(), refactoring.getOperationBefore());
        }
        @Test
        public void testFromInlineToAssertThrows_classDiff() throws RefactoringMinerTimedOutException {
        	UMLClassBaseDiff classDiff = modelDiff.getUMLClassDiff("ca.concordia.victor.exception.ExampleClassTest");
            Assert.assertNotNull(classDiff);
            List<Refactoring> refactorings = classDiff.getRefactorings();
            Assert.assertEquals(2, refactorings.size());
            Assert.assertTrue(refactorings.stream().allMatch(r->r instanceof ModifyMethodAnnotationRefactoring || r instanceof AssertThrowsRefactoring));
        }
        @Test
        public void testFromInlineToAssertThrows_modelDiff() throws RefactoringMinerTimedOutException {
            List<Refactoring> refactorings = modelDiff.getRefactorings();
            Assert.assertEquals(2, refactorings.size());
            Assert.assertTrue(refactorings.stream().allMatch(r->r instanceof ModifyMethodAnnotationRefactoring || r instanceof AssertThrowsRefactoring));
            Optional<Refactoring> possibleRefactoring = refactorings.stream()
                    .filter(r -> r instanceof AssertThrowsRefactoring)
                    .findAny();
            Assert.assertTrue(possibleRefactoring.isPresent());
            AssertThrowsRefactoring refactoring = (AssertThrowsRefactoring) possibleRefactoring.get();
            Assert.assertEquals("Assert Throws",refactoring.getName());
            Assert.assertEquals(RefactoringType.ASSERT_THROWS,refactoring.getRefactoringType());
            Assert.assertEquals(1,refactoring.getInvolvedClassesAfterRefactoring().size());
            Assert.assertEquals("testClass",new ArrayList<>(refactoring.getInvolvedClassesAfterRefactoring()).get(0).left);
            Assert.assertEquals("ca.concordia.victor.exception.ExampleClassTest",new ArrayList<>(refactoring.getInvolvedClassesAfterRefactoring()).get(0).right);
            Assert.assertEquals(1,refactoring.getInvolvedClassesBeforeRefactoring().size());
            Assert.assertEquals("testClass",new ArrayList<>(refactoring.getInvolvedClassesBeforeRefactoring()).get(0).left);
            Assert.assertEquals("ca.concordia.victor.exception.ExampleClassTest",new ArrayList<>(refactoring.getInvolvedClassesBeforeRefactoring()).get(0).right);
            Assert.assertEquals("Assert Throws\tAssert.assertThrows(IllegalArgumentException.class,() -> { in method public testExampleMethod_WrongGuess() : void from class ca.concordia.victor.exception.ExampleClassTest", refactoring.toString());
            Assert.assertEquals(2,refactoring.getAssertThrowsCall().arguments().size());
            Assert.assertEquals("IllegalArgumentException.class",refactoring.getAssertThrowsCall().arguments().get(0));
            Assert.assertEquals("() -> {\n" +
                    "  exampleObj.exampleMethod(0);\n" +
                    "}\n",refactoring.getAssertThrowsCall().arguments().get(1));
            possibleRefactoring = refactorings.stream()
                    .filter(r -> r instanceof ModifyMethodAnnotationRefactoring)
                    .findAny();
            Assert.assertTrue(possibleRefactoring.isPresent());
            Assert.assertEquals("Modify Method Annotation\t@Test(expected = IllegalArgumentException.class) to @Test in method public testExampleMethod_WrongGuess() : void from class ca.concordia.victor.exception.ExampleClassTest",possibleRefactoring.get().toString());
            
        }
    }

    public static class ExploringTest extends ModelDiffFieldSetUp {
        @Test
        public void testFromInlineToAssertThrows() throws RefactoringMinerTimedOutException {
            ModifyMethodAnnotationRefactoring modifyAnnotationRefactoring = detectModifyMethodAnnotationRefactoring();
            String expectedException = detectExpectedExceptionTypeLiteral(modifyAnnotationRefactoring.getAnnotationBefore());

            UMLOperation after = modifyAnnotationRefactoring.getOperationAfter();
            List<AbstractCall> assertThrows = getAssertThrows(after);
            Assert.assertEquals("Number of assertThrows call is not 1", 1, assertThrows.size());
            List<String> args = assertThrows.get(0).arguments();
            String exceptionClassLiteral = args.get(0);
            Assert.assertEquals(expectedException, exceptionClassLiteral);

            verifyAssertThrowsLambdaHasPreviousTestBodyStatements(after, args.get(1));
        }

        private ModifyMethodAnnotationRefactoring detectModifyMethodAnnotationRefactoring() throws RefactoringMinerTimedOutException {
            List<Refactoring> refactorings = modelDiff.getRefactorings();
            Assert.assertEquals("There should be two refactorings in the example test method", 2, refactorings.size());
            Optional<Refactoring> refactoring = refactorings.stream().filter(r -> r.getRefactoringType().equals(RefactoringType.ASSERT_THROWS)).findAny();
            Assert.assertTrue("Migration from @Test(expected) to assertThrows not detected", refactoring.isPresent());
            refactoring = refactorings.stream().filter(r -> r.getRefactoringType().equals(RefactoringType.MODIFY_METHOD_ANNOTATION)).findAny();
            Assert.assertTrue("@Test memberValuePair change not detected", refactoring.isPresent());
            return (ModifyMethodAnnotationRefactoring) refactoring.get();
        }

        private String detectExpectedExceptionTypeLiteral(UMLAnnotation before) {
            Assert.assertTrue("@Test(expected) normalAnnotation not found", hasExpectedException(before));
            AbstractExpression expectedException = before.getMemberValuePairs().get("expected");
            Assert.assertEquals("@Test(expected) should contain a single type literal", 1, expectedException.getTypeLiterals().size());
            return expectedException.getExpression();
        }

        private void verifyAssertThrowsLambdaHasPreviousTestBodyStatements(UMLOperation after, String lambdaExpression) {
        	List<LambdaExpressionObject> allLambdas = after.getBody().getAllLambdas();
            Assert.assertEquals(1, allLambdas.size());
            LambdaExpressionObject lambda = allLambdas.get(0);
            List<String> expectedLines = lambdaExpression.lines().collect(Collectors.toList());
            expectedLines.remove(0);
            expectedLines.remove(expectedLines.size() - 1);
            List<String> lines = lambda.getBody().stringRepresentation();
            lines.remove(0);
            lines.remove(lines.size() - 1);
            Iterator<String> expectedIter = expectedLines.iterator();
            Iterator<String> linesIter = lines.iterator();
            for (String expectedLine = expectedIter.next(), line = linesIter.next();
                 expectedIter.hasNext() && linesIter.hasNext();
                 expectedLine = expectedIter.next(), line = linesIter.next()) {
                Assert.assertEquals(expectedLine.strip(), line.strip());
            }
        }

        private boolean hasExpectedException(gr.uom.java.xmi.UMLAnnotation before) {
            return before.isNormalAnnotation() && before.getTypeName().equals("Test") && before.getMemberValuePairs().containsKey("expected");
        }

        private List<AbstractCall> getAssertThrows(UMLOperation operation) {
            return operation.getAllOperationInvocations().stream()
                    .filter((op) -> op.getName().equals("assertThrows") &&
                            (op.getExpression().equals("Assert") || op.getExpression().equals("Assertions")))
                    .collect(Collectors.toList());
        }
    }
}
