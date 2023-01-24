package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents expecting exception test encoding migration (from JUnit 4 to JUnit 5)
 * JUnit4 usually relies on @Test(expected), a member value pair which receives an exception type literal
 * JUnit5 introduces the assertThrows method that expects both an exception type and a lambda function
 */
public class ExpectedAnnotationToAssertThrowsRefactoring implements Refactoring {
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final ModifyMethodAnnotationRefactoring expectedExceptionAnnotation;
    private final AbstractExpression exception;
    private final LambdaExpressionObject lambda;
    private final AbstractCall assertThrows;

    public UMLOperation getOperationBefore() {
        return operationBefore;
    }

    public UMLOperation getOperationAfter() {
        return operationAfter;
    }

    public ModifyMethodAnnotationRefactoring getExpectedExceptionAnnotation() {
        return expectedExceptionAnnotation;
    }

    public AbstractExpression getException() {
        return exception;
    }

    public LambdaExpressionObject getLambda() {
        return lambda;
    }

    public AbstractCall getAssertThrows() {
        return assertThrows;
    }

    public ExpectedAnnotationToAssertThrowsRefactoring(UMLOperation operationBefore,
                                                       UMLOperation operationAfter,
                                                       ModifyMethodAnnotationRefactoring expectedExceptionAnnotation,
                                                       AbstractExpression exception,
                                                       LambdaExpressionObject lambda,
                                                       AbstractCall assertThrows) {
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.expectedExceptionAnnotation = expectedExceptionAnnotation;
        this.exception = exception;
        this.lambda = lambda;
        this.assertThrows = assertThrows;
    }

    @Override
    public String toString() {
        return getName() + "\t" +
                exception +
                " from method " +
                operationBefore +
                " in class " +
                getClassName();
    }

    private String getClassName() {
        return operationAfter.getClassName();
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(operationBefore.codeRange()
                    .setDescription("source method declaration before migration")
                    .setCodeElement(operationBefore.toString()));
        ranges.add(expectedExceptionAnnotation.getAnnotationBefore().codeRange()
                    .setDescription("source method's annotations before migration")
                    .setCodeElement(expectedExceptionAnnotation.getAnnotationBefore().toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(operationAfter.codeRange()
                .setDescription("method declaration after migration")
                .setCodeElement(operationAfter.toString()));
        ranges.add(expectedExceptionAnnotation.getAnnotationAfter().codeRange()
                .setDescription("method's annotations after migration")
                .setCodeElement(expectedExceptionAnnotation.getAnnotationAfter().toString()));
        ranges.add(assertThrows.codeRange()
                .setDescription("added Assert.assertThrows call")
                .setCodeElement(assertThrows.toString()));
        ranges.add(lambda.codeRange()
                .setDescription("extracted lambda from method's body")
                .setCodeElement(lambda.toString()));
        return ranges;
    }

    @Override
    public RefactoringType getRefactoringType() {
        return RefactoringType.REPLACE_EXPECTED_WITH_ASSERT_THROWS;
    }

    @Override
    public String getName() {
        return getRefactoringType().getDisplayName();
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(operationBefore.getLocationInfo().getFilePath(), operationBefore.getClassName()));
        return pairs;
    }

    @Override
    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(operationAfter.getLocationInfo().getFilePath(), operationAfter.getClassName()));
        return pairs;
    }
}
