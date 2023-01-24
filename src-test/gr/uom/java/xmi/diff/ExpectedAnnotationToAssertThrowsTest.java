package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(Enclosed.class)
public class ExpectedAnnotationToAssertThrowsTest {
    abstract public static class ModelDiffFieldSetUp {
        UMLModelDiff modelDiff;

        @Before
        public void setUp() throws RefactoringMinerTimedOutException {
            var inlineVersionTestMethod = TestOperationDiffMother.createExampleTestMethod_InlineVersion();
            var inlineVersionTestClass = TestOperationDiffMother.createExampleClassTestCode(inlineVersionTestMethod);
            var before = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", inlineVersionTestClass), Set.of(), false).getUmlModel();
            var assertVersionTestMethod = TestOperationDiffMother.createExampleTestMethod_AssertVersion();
            var assertVersionTestClass = TestOperationDiffMother.createExampleClassTestCode(assertVersionTestMethod);
            var after = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", assertVersionTestClass), Set.of(), false).getUmlModel();
            modelDiff = before.diff(after);
        }
    }
    public static class RegressionTest {
        UMLModelDiff modelDiff;

        @Before
        public void setUp() throws RefactoringMinerTimedOutException {
            var inlineVersionTestClass = "package uk.gov.hmcts.reform.cwrdapi.service.impl;\n" +
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
                    "                .build()\n" +
                    "                ;\n" +
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
            var before = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", inlineVersionTestClass), Set.of(), false).getUmlModel();
            var assertVersionTestClass = "package uk.gov.hmcts.reform.cwrdapi.service.impl;\n" +
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
            var after = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", assertVersionTestClass), Set.of(), false).getUmlModel();
            modelDiff = before.diff(after);
        }
        @Test
        public void testFromInlineToAssertThrows_firstModifiedMethodAnnotationBelongsToOtherMethod() throws RefactoringMinerTimedOutException {
            var refactorings = modelDiff.getRefactorings();
            Assert.assertEquals(3, refactorings.size());
            Assert.assertTrue(refactorings.stream().allMatch(r -> r instanceof ModifyMethodAnnotationRefactoring || r instanceof ExpectedAnnotationToAssertThrowsRefactoring));
        }
    }

    public static class ImplementationTest extends ModelDiffFieldSetUp {
        @Test
        public void testFromInlineToAssertThrows_detector() {
            var classDiff = modelDiff.getUMLClassDiff("ca.concordia.victor.exception.ExampleClassTest");
            Assert.assertNotNull(classDiff);
            Assert.assertEquals(2, classDiff.operationBodyMapperList.size());
            var testMethodMapperOptional = classDiff.operationBodyMapperList.stream().filter(UMLOperationBodyMapper::involvesTestMethods).findAny();
            Assert.assertTrue(testMethodMapperOptional.isPresent());
            var mapper = testMethodMapperOptional.get();
            var annotationsBefore = mapper.getOperation1().getAnnotations();
            Assert.assertEquals(annotationsBefore.size(), 1);
            UMLAnnotation annotationBefore = annotationsBefore.get(0);
            var annotationsAfter = mapper.getOperation2().getAnnotations();
            Assert.assertEquals(annotationsAfter.size(), 1);
            UMLAnnotation annotationAfter = annotationsAfter.get(0);
            List<Refactoring> annotations = List.of(new ModifyMethodAnnotationRefactoring(annotationBefore, annotationAfter, mapper.getOperation1(), mapper.getOperation2()));
            var detector = new ExpectedAnnotationToAssertThrowsDetection(mapper.getOperation1(), mapper.getOperation2(), annotations);
            var refactoring = detector.check();
            Assert.assertNotNull(refactoring);
            Assert.assertEquals(mapper.getOperation2(), refactoring.getOperationAfter());
            Assert.assertEquals(mapper.getOperation1(), refactoring.getOperationBefore());
        }
        @Test
        public void testFromInlineToAssertThrows_classDiff() throws RefactoringMinerTimedOutException {
            var classDiff = modelDiff.getUMLClassDiff("ca.concordia.victor.exception.ExampleClassTest");
            Assert.assertNotNull(classDiff);
            var refactorings = classDiff.getRefactorings();
            Assert.assertEquals(2, refactorings.size());
            Assert.assertTrue(refactorings.stream().allMatch(r->r instanceof ModifyMethodAnnotationRefactoring || r instanceof ExpectedAnnotationToAssertThrowsRefactoring));
        }
        @Test
        public void testFromInlineToAssertThrows_modelDiff() throws RefactoringMinerTimedOutException {
            var refactorings = modelDiff.getRefactorings();
            Assert.assertEquals(2, refactorings.size());
            Assert.assertTrue(refactorings.stream().allMatch(r->r instanceof ModifyMethodAnnotationRefactoring || r instanceof ExpectedAnnotationToAssertThrowsRefactoring));
            var possibleRefactoring = refactorings.stream()
                    .filter(r -> r instanceof ExpectedAnnotationToAssertThrowsRefactoring)
                    .findAny();
            Assert.assertTrue(possibleRefactoring.isPresent());
            var refactoring = (ExpectedAnnotationToAssertThrowsRefactoring) possibleRefactoring.get();
            Assert.assertEquals("IllegalArgumentException.class",refactoring.getException().getExpression());
            Assert.assertEquals("Replace Expect Annotation With Assert Throws",refactoring.getName());
            Assert.assertEquals(RefactoringType.REPLACE_EXPECTED_WITH_ASSERT_THROWS,refactoring.getRefactoringType());
            Assert.assertEquals(1,refactoring.getInvolvedClassesAfterRefactoring().size());
            Assert.assertEquals("testClass",new ArrayList<>(refactoring.getInvolvedClassesAfterRefactoring()).get(0).left);
            Assert.assertEquals("ca.concordia.victor.exception.ExampleClassTest",new ArrayList<>(refactoring.getInvolvedClassesAfterRefactoring()).get(0).right);
            Assert.assertEquals(1,refactoring.getInvolvedClassesBeforeRefactoring().size());
            Assert.assertEquals("testClass",new ArrayList<>(refactoring.getInvolvedClassesBeforeRefactoring()).get(0).left);
            Assert.assertEquals("ca.concordia.victor.exception.ExampleClassTest",new ArrayList<>(refactoring.getInvolvedClassesBeforeRefactoring()).get(0).right);
            Assert.assertEquals("Replace Expect Annotation With Assert Throws\tIllegalArgumentException.class from method public testExampleMethod_WrongGuess() : void in class ca.concordia.victor.exception.ExampleClassTest", refactoring.toString());
            var leftSideDescriptions = new String[]{"source method declaration before migration", "source method's annotations before migration"};
            Assert.assertArrayEquals(leftSideDescriptions, refactoring.leftSide().stream().map(CodeRange::getDescription).toArray());
            var leftSideCodeElementTypes = new LocationInfo.CodeElementType[]{LocationInfo.CodeElementType.METHOD_DECLARATION,LocationInfo.CodeElementType.ANNOTATION,LocationInfo.CodeElementType.METHOD_INVOCATION,LocationInfo.CodeElementType.LAMBDA_EXPRESSION};
            Assert.assertArrayEquals(leftSideCodeElementTypes, refactoring.rightSide().stream().map(CodeRange::getCodeElementType).toArray());
            var rightSideDescriptions = new String[]{"method declaration after migration", "method's annotations after migration", "added Assert.assertThrows call","extracted lambda from method's body"};
            Assert.assertArrayEquals(rightSideDescriptions, refactoring.rightSide().stream().map(CodeRange::getDescription).toArray());
            var rightSideCodeElementTypes = new LocationInfo.CodeElementType[]{LocationInfo.CodeElementType.METHOD_DECLARATION,LocationInfo.CodeElementType.ANNOTATION,LocationInfo.CodeElementType.METHOD_INVOCATION,LocationInfo.CodeElementType.LAMBDA_EXPRESSION};
            Assert.assertArrayEquals(rightSideCodeElementTypes, refactoring.rightSide().stream().map(CodeRange::getCodeElementType).toArray());
            Assert.assertEquals("Modify Method Annotation\t@Test(expected = IllegalArgumentException.class) to @Test in method public testExampleMethod_WrongGuess() : void from class ca.concordia.victor.exception.ExampleClassTest",refactoring.getExpectedExceptionAnnotation().toString());
            Assert.assertEquals("IllegalArgumentException.class",refactoring.getException().getExpression());
            Assert.assertEquals(1,refactoring.getLambda().getBody().getCompositeStatement().getStatements().size());
            Assert.assertEquals(LocationInfo.CodeElementType.EXPRESSION_STATEMENT,refactoring.getLambda().getBody().getCompositeStatement().getStatements().get(0).codeRange().getCodeElementType());
            Assert.assertEquals(2,refactoring.getAssertThrows().arguments().size());
            Assert.assertEquals("IllegalArgumentException.class",refactoring.getAssertThrows().arguments().get(0));
            Assert.assertEquals("() -> {\n" +
                    "  exampleObj.exampleMethod(0);\n" +
                    "}\n",refactoring.getAssertThrows().arguments().get(1));
        }
    }

    @Ignore("The base from what the implementation emerged (Do not test implementation but the implementation's dependencies)")
    public static class ExploringTest extends ModelDiffFieldSetUp {

        @Test
        public void testFromInlineToAssertThrows() throws RefactoringMinerTimedOutException {
            ModifyMethodAnnotationRefactoring modifyAnnotationRefactoring = detectModifyMethodAnnotationRefactoring();
            String expectedException = detectExpectedExceptionTypeLiteral(modifyAnnotationRefactoring.getAnnotationBefore());

            var after = modifyAnnotationRefactoring.getOperationAfter();
            var assertThrows = getAssertThrows(after);
            Assert.assertEquals("Number of assertThrows call is not 1", 1, assertThrows.size());
            var args = assertThrows.get(0).getArguments();
            var exceptionClassLiteral = args.get(0);
            Assert.assertEquals(expectedException, exceptionClassLiteral);

            verifyAssertThrowsLambdaHasPreviousTestBodyStatements(after, args.get(1));
        }

        private ModifyMethodAnnotationRefactoring detectModifyMethodAnnotationRefactoring() throws RefactoringMinerTimedOutException {
            var refactorings = modelDiff.getRefactorings();
            Assert.assertEquals("There should be two refactorings in the example test method", 2, refactorings.size());
            var refactoring = refactorings.stream().filter(r -> r.getRefactoringType().equals(RefactoringType.REPLACE_EXPECTED_WITH_ASSERT_THROWS)).findAny();
            Assert.assertTrue("Migration from @Test(expected) to assertThrows not detected", refactoring.isPresent());
            refactoring = refactorings.stream().filter(r -> r.getRefactoringType().equals(RefactoringType.MODIFY_METHOD_ANNOTATION)).findAny();
            Assert.assertTrue("@Test memberValuePair change not detected", refactoring.isPresent());
            return (ModifyMethodAnnotationRefactoring) refactoring.get();
        }

        private String detectExpectedExceptionTypeLiteral(UMLAnnotation before) {
            Assert.assertTrue("@Test(expected) normalAnnotation not found", hasExpectedException(before));
            var expectedException = before.getMemberValuePairs().get("expected");
            Assert.assertEquals("@Test(expected) should contain a single type literal", 1, expectedException.getTypeLiterals().size());
            return expectedException.getExpression();
        }

        private void verifyAssertThrowsLambdaHasPreviousTestBodyStatements(UMLOperation after, LeafExpression lambdaExpression) {
            var allLambdas = after.getBody().getAllLambdas();
            Assert.assertEquals(1, allLambdas.size());
            var lambda = allLambdas.get(0);
            var expectedLines = lambdaExpression.getString().lines().collect(Collectors.toList());
            expectedLines.remove(0);
            expectedLines.remove(expectedLines.size() - 1);
            var lines = lambda.getBody().stringRepresentation();
            lines.remove(0);
            lines.remove(lines.size() - 1);
            var expectedIter = expectedLines.iterator();
            var linesIter = lines.iterator();
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
