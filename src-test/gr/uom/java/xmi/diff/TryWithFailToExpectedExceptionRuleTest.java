package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.Visibility;
import gr.uom.java.xmi.decomposition.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(Enclosed.class)
public class TryWithFailToExpectedExceptionRuleTest {
    abstract public static class ModelDiffFieldSetUp {
        UMLModelDiff modelDiff;

        @Before
        public void setUp() throws RefactoringMinerTimedOutException {
            var tryCatchVersionTestMethod = TestOperationDiffMother.createExampleTestMethod_TryCatchVersion();
            var tryCatchVersionTestClass = TestOperationDiffMother.createExampleClassTestCode(tryCatchVersionTestMethod);
            var before = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", tryCatchVersionTestClass), Set.of(), false).getUmlModel();
            var ruleVersionTestMethod = TestOperationDiffMother.createExampleTestMethod_RuleVersion();
            var ruleVersionTestClass = TestOperationDiffMother.createExampleClassTestCode(ruleVersionTestMethod);
            var after = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", ruleVersionTestClass), Set.of(), false).getUmlModel();
            modelDiff = before.diff(after);
        }
    }
    public static class RegressionTest  {
        UMLModelDiff modelDiff;
        UMLModel after;

        @Before
        public void setUp() {
            var ruleVersionTestClass = "package android.support.test.espresso.intent.matcher;\n" +
                    "\n" +
                    "public class IntentMatchersTest {\n" +
                    "    @Rule\n" +
                    "    public ExpectedException expectedException = none();\n" +
                    "    @Test\n" +
                    "    public void toPackageTesting() {\n" +
                    "        final String pkg = \"pkg1\";\n" +
                    "        ResolvedIntent intent = new FakeResolvedIntent(pkg);\n" +
                    "        assertTrue(toPackage(pkg).matches(intent));\n" +
                    "        assertFalse(toPackage(\"notpkg1\").matches(intent));\n" +
                    "        expectedException.expect(RuntimeException.class);\n" +
                    "        toPackage(\"whatever\").matches(new Intent(Intent.ACTION_VIEW));\n" +
                    "    }\n" +
                    "}";
            after = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", ruleVersionTestClass), Set.of(), false).getUmlModel();
        }

        @Test
        public void testFromTryFailToRule_LastTryChildStatementContainingZeroMethodInvocation_ShouldNotThrowIndexOutOfBoundsException() throws RefactoringMinerTimedOutException {
            var tryCatchVersionTestClass = "package android.support.test.espresso.intent.matcher;\n" +
                    "\n" +
                    "public class IntentMatchersTest {\n" +
                    "    public void testToPackage() {\n" +
                    "        final String pkg = \"pkg1\";\n" +
                    "        ResolvedIntent intent = new FakeResolvedIntent(pkg);\n" +
                    "        assertTrue(toPackage(pkg).matches(intent));\n" +
                    "        assertFalse(toPackage(\"notpkg1\").matches(intent));\n" +
                    "        try {\n" +
                    "            toPackage(\"whatever\").matches(new Intent(Intent.ACTION_VIEW));\n" +
                    "            pkg = \"pkg2\";\n" +
                    "        } catch (RuntimeException expected) {\n" +
                    "\n" +
                    "        }\n" +
                    "        assertEquals(pkg, \"pkg1\");\n" +
                    "    }\n" +
                    "}";
            var before = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", tryCatchVersionTestClass), Set.of(), false).getUmlModel();
            modelDiff = before.diff(after);
            modelDiff.getRefactorings();
        }

        @Test
        public void testFromTryFailToRule_StaticallyImportedAssertFail_ShouldThrowNullPointerException() throws RefactoringMinerTimedOutException {
            var tryCatchVersionTestClass = "package android.support.test.espresso.intent.matcher;\n" +
                    "\n" +
                    "public class IntentMatchersTest {\n" +
                    "    public void testToPackage() {\n" +
                    "        final String pkg = \"pkg1\";\n" +
                    "        ResolvedIntent intent = new FakeResolvedIntent(pkg);\n" +
                    "        assertTrue(toPackage(pkg).matches(intent));\n" +
                    "        assertFalse(toPackage(\"notpkg1\").matches(intent));\n" +
                    "        try {\n" +
                    "            toPackage(\"whatever\").matches(new Intent(Intent.ACTION_VIEW));\n" +
                    "            fail(\"Expected previous call to fail.\");\n" +
                    "        } catch (RuntimeException expected) {\n" +
                    "\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            var before = new UMLModelASTReader(Map.of("productionClass", TestOperationDiffMother.createExampleClassCode(), "testClass", tryCatchVersionTestClass), Set.of(), false).getUmlModel();
            modelDiff = before.diff(after);
            modelDiff.getRefactorings();
        }

    }
    public static class ImplementationTest extends ModelDiffFieldSetUp {
        @Test
        public void testFromTryFailToRule() {
            var classDiff = modelDiff.getUMLClassDiff("ca.concordia.victor.exception.ExampleClassTest");
            Assert.assertNotNull(classDiff);
            var operationMappers = classDiff.getOperationBodyMapperList();
            Assert.assertEquals(2, operationMappers.size());
            var possibleMapper = operationMappers.stream().filter(op-> !(op.operationNameEditDistance() == 0 && op.getOperation1().getName().equals("setUp"))).findAny();
            Assert.assertTrue(possibleMapper.isPresent());
            var mapper = possibleMapper.get();
            var detector = new TryWithFailToExpectedExceptionRuleDetection(mapper, classDiff.addedAttributes);
            var refactoring = detector.check();
            Assert.assertNotNull(refactoring);
            Assert.assertEquals(mapper.getOperation2(), refactoring.getOperationAfter());
            Assert.assertEquals(mapper.getOperation1(), refactoring.getOperationBefore());
            Assert.assertNotNull(detector.getExpectedExceptionFieldDeclaration());
            Assert.assertEquals(1, detector.getTryStatements().size());
            Assert.assertEquals(1, detector.getAssertFailInvocationsFound().size());
            Assert.assertEquals(1, detector.getCapturedExceptions().size());
            Assert.assertEquals(1, detector.getExpectInvocations().size());
            Assert.assertEquals("ExpectedException", detector.getExpectedExceptionFieldDeclaration().getType().getClassType());
            Assert.assertEquals(1, detector.getExpectInvocations().size());
            Assert.assertEquals(1, detector.getExpectInvocations().get(0).arguments().size());
            Assert.assertEquals("IllegalArgumentException.class", detector.getExpectInvocations().get(0).arguments().get(0));
            Assert.assertEquals(1, detector.getCapturedExceptions().size());
            Assert.assertEquals("IllegalArgumentException.class", detector.getCapturedExceptions().get(0));
        }
        @Test
        public void testFromTryFailToRule_ClassBaseDiff_getRefactorings() throws RefactoringMinerTimedOutException {
            var classDiff = modelDiff.getUMLClassDiff("ca.concordia.victor.exception.ExampleClassTest");
            Assert.assertNotNull(classDiff);
            var refactorings = classDiff.getRefactorings();
            Assert.assertEquals(1, refactorings.size());
            Assert.assertTrue(refactorings.stream().anyMatch(r->r instanceof TryWithFailToExpectedExceptionRuleRefactoring));
        }
        @Test
        public void testFromTryFailToRule_ModelDiff_getRefactorings() throws RefactoringMinerTimedOutException {
            var refactorings = modelDiff.getRefactorings();
            Assert.assertEquals(1, refactorings.size());
            Assert.assertTrue(refactorings.stream().anyMatch(r->r instanceof TryWithFailToExpectedExceptionRuleRefactoring));
            TryWithFailToExpectedExceptionRuleRefactoring r = (TryWithFailToExpectedExceptionRuleRefactoring) refactorings.get(0);
            Assert.assertEquals("IllegalArgumentException.class",r.getException());
            Assert.assertEquals("Replace Try And Fail With Rule",r.getName());
            Assert.assertEquals(RefactoringType.REPLACE_TRY_FAIL_WITH_RULE,r.getRefactoringType());
            Assert.assertEquals(1,r.getInvolvedClassesAfterRefactoring().size());
            Assert.assertEquals("testClass",new ArrayList<>(r.getInvolvedClassesAfterRefactoring()).get(0).left);
            Assert.assertEquals("ca.concordia.victor.exception.ExampleClassTest",new ArrayList<>(r.getInvolvedClassesAfterRefactoring()).get(0).right);
            Assert.assertEquals(1,r.getInvolvedClassesBeforeRefactoring().size());
            Assert.assertEquals("testClass",new ArrayList<>(r.getInvolvedClassesBeforeRefactoring()).get(0).left);
            Assert.assertEquals("ca.concordia.victor.exception.ExampleClassTest",new ArrayList<>(r.getInvolvedClassesBeforeRefactoring()).get(0).right);
            Assert.assertEquals("Replace Try And Fail With Rule\tIllegalArgumentException.class from method public testExampleMethod_WrongGuess() : void in class ca.concordia.victor.exception.ExampleClassTest", r.toString());
            var leftSideDescriptions = new String[]{"source method declaration before migration", "source method's try-statement", "source method's catch clause capturing the expected exception", "source method's assertFail invocation from the try-statement before migration"};
            Assert.assertArrayEquals(leftSideDescriptions, r.leftSide().stream().map(CodeRange::getDescription).toArray());
            var leftSideCodeElementTypes = new LocationInfo.CodeElementType[]{LocationInfo.CodeElementType.METHOD_DECLARATION,LocationInfo.CodeElementType.FIELD_DECLARATION,LocationInfo.CodeElementType.METHOD_INVOCATION};
            Assert.assertArrayEquals(leftSideCodeElementTypes, r.rightSide().stream().map(CodeRange::getCodeElementType).toArray());
            var rightSideDescriptions = new String[]{"method declaration after migration", "ExpectedException field annotated with @Rule", "method's statement invoking ExpectedException's expect method"};
            Assert.assertArrayEquals(rightSideDescriptions, r.rightSide().stream().map(CodeRange::getDescription).toArray());
            var rightSideCodeElementTypes = new LocationInfo.CodeElementType[]{LocationInfo.CodeElementType.METHOD_DECLARATION,LocationInfo.CodeElementType.FIELD_DECLARATION,LocationInfo.CodeElementType.METHOD_INVOCATION};
            Assert.assertArrayEquals(rightSideCodeElementTypes, r.rightSide().stream().map(CodeRange::getCodeElementType).toArray());
            Assert.assertEquals("fail", r.getAssertFailInvocation().getName());
            Assert.assertEquals("thrown", r.getRuleFieldDeclaration().getName());
            Assert.assertEquals(Visibility.PUBLIC, r.getRuleFieldDeclaration().getVisibility());
            Assert.assertEquals("ExpectedException", r.getRuleFieldDeclaration().getType().getClassType());
            Assert.assertEquals(2, r.getTryStatement().getStatements().size());
            Assert.assertEquals("IllegalArgumentException.class", r.getThrownExpectInvocations().arguments().get(0));
        }

    }
    public static class ExploringTest extends ModelDiffFieldSetUp {

        @Test
        public void testFromTryFailToRule() {
            var classDiff = modelDiff.getUMLClassDiff("ca.concordia.victor.exception.ExampleClassTest");
            Assert.assertNotNull(classDiff);
            var operationMappers = classDiff.getOperationBodyMapperList();
            Assert.assertEquals(2, operationMappers.size());
            var possibleMapper = operationMappers.stream().filter(op-> !(op.operationNameEditDistance() == 0 && op.getOperation1().getName().equals("setUp"))).findAny();
            Assert.assertTrue(possibleMapper.isPresent());
            var mapper = possibleMapper.get();

            var removedCompositeStmts = mapper.getNonMappedInnerNodesT1();
            var addedStmts = mapper.getNonMappedLeavesT2();
            Assert.assertEquals(3, removedCompositeStmts.size() + addedStmts.size());

            TryStatementObject tryStatement = getTryStatement(removedCompositeStmts);
            List<String> capturedExceptions = getExceptions(tryStatement);
            Assert.assertEquals(1, capturedExceptions.size());
            Assert.assertEquals("IllegalArgumentException.class", capturedExceptions.get(0));
            Assert.assertEquals(2, tryStatement.getStatements().size());
            Assert.assertTrue("Assert fail should be invoked at the end of TryStatement", hasAssertFailInvocationAtTheEnd(tryStatement));

            var thrownExpectInvocations = extractMethodInvocations(addedStmts).filter(invocation -> hasExpectedException(capturedExceptions, invocation)).collect(Collectors.toList());
            Assert.assertEquals(1, thrownExpectInvocations.size());
            var thrownExpect = thrownExpectInvocations.get(0);
            var ruleAnnotatedVariableName = thrownExpect.getExpression();
            Assert.assertEquals(1, thrownExpect.arguments().size());

            var ruleFieldDeclaration = classDiff.getAddedAttributes().get(0);
            Assert.assertEquals("ExpectedException", ruleFieldDeclaration.getType().getClassType());
            Assert.assertEquals(ruleAnnotatedVariableName, ruleFieldDeclaration.getName());
            Assert.assertEquals(1, thrownExpect.arguments().size());
            Assert.assertEquals("IllegalArgumentException.class", thrownExpect.arguments().get(0));
        }

        private boolean hasExpectedException(List<String> capturedExceptions, AbstractCall invocation) {
            return invocation.getName().equals("expect") && isAnyArgumentPassedTo(capturedExceptions, invocation);
        }

        private boolean isAnyArgumentPassedTo(List<String> arguments, AbstractCall invocation) {
            return arguments.contains(invocation.arguments().get(0));
        }

        private Stream<AbstractCall> extractMethodInvocations(List<AbstractCodeFragment> addedStmts) {
            return addedStmts.stream().flatMap(st -> st.getMethodInvocations().stream());
        }

        private boolean hasAssertFailInvocationAtTheEnd(TryStatementObject tryStatement) {
            var lastStatement = tryStatement.getStatements().get(tryStatement.getStatements().size() - 1);
            var operationInvocationsInLastStatement = new ArrayList<>(lastStatement.getMethodInvocations());
            var assertFailInvocations = operationInvocationsInLastStatement.stream()
                    .filter(invocation -> invocation.getExpression().equals("Assert") && invocation.getName().equals("fail"))
                    .collect(Collectors.toList());
            return assertFailInvocations.size() > 0;
        }

        private List<String> getExceptions(TryStatementObject tryStatement) {
            return tryStatement.getCatchClauses().stream()
                    .flatMap(clause -> clause.getVariableDeclarations().stream()
                            .map(variable -> variable.getType().getClassType()))
                    .filter(classType -> classType.endsWith("Exception"))
                    .map(classType -> classType.concat(".class"))
                    .collect(Collectors.toList());
        }

        private TryStatementObject getTryStatement(List<CompositeStatementObject> stmt) {
            Optional<TryStatementObject> possibleTry = stmt.stream()
                    .filter(st->st instanceof TryStatementObject)
                    .map(st -> (TryStatementObject)st)
                    .findAny();
            Assert.assertTrue(possibleTry.isPresent());
            return possibleTry.get();
        }

    }
}
