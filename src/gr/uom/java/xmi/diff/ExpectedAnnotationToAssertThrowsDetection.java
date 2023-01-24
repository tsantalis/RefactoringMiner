package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LambdaExpressionObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects expecting exception test encoding migration (from JUnit 4 to JUnit 5)
 * JUnit4 usually relies on @Test(expected), a member value pair which receives an exception type literal
 * JUnit5 introduces the assertThrows method that expects both an exception type and a lambda function
 */
public class ExpectedAnnotationToAssertThrowsDetection {
    private final Collection<Refactoring> refactorings;
    private AbstractCall operationInvocation;
    private ModifyMethodAnnotationRefactoring annotationChange;
    private AbstractExpression exception;
    private LambdaExpressionObject lambda;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;

    public ExpectedAnnotationToAssertThrowsDetection(UMLOperation operationBefore, UMLOperation operationAfter, Collection<Refactoring> refactorings) {
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.refactorings = refactorings;
    }

    public ExpectedAnnotationToAssertThrowsRefactoring check() {
        var expectedRemovalFromTestAnnotation = getRemovedExpectedAttributeFromTestAnnotation();
        try {
            annotationChange = expectedRemovalFromTestAnnotation.orElseThrow();
            exception = annotationChange.getAnnotationBefore().getMemberValuePairs().get("expected");
            operationInvocation = getAssertThrows(operationAfter).stream()
                    .filter(i -> i.arguments().get(0).equals(exception.getExpression()))
                    .filter(i -> containsAtLeastOneLineInCommon(operationBefore, i.arguments().get(1)))
                    .findAny()
                    .orElseThrow();
            lambda = operationAfter.getAllLambdas().stream()
                    .filter(lambda -> isEnclosedBy(lambda, operationInvocation))
                    .findAny()
                    .orElseThrow();
            return new ExpectedAnnotationToAssertThrowsRefactoring(operationBefore, operationAfter, annotationChange, exception, lambda, operationInvocation);
        } catch (NoSuchElementException ex) {
            return null;
        }
    }

    private boolean isEnclosedBy(LambdaExpressionObject lambda, AbstractCall invocation) {
        var invocationRange = invocation.codeRange();
        var lambdaRange = lambda.codeRange();
        return invocationRange.getStartLine() <= lambdaRange.getStartLine() &&
                invocationRange.getEndLine() >= lambdaRange.getEndLine() &&
                invocationRange.getStartColumn() <= lambdaRange.getStartColumn() &&
                invocationRange.getEndColumn() >= lambdaRange.getEndColumn();
    }

    private boolean containsAtLeastOneLineInCommon(UMLOperation operation, String lambda) {
        return lambda
                .lines()
                .map(String::strip)
                .map(line -> lambdaBodyIsExpression(line) ? extractExpressionAndConvertToStatement(line) : line)
                .filter(line -> line.length() > 1) // Ignore "{" and "}" lines
                .anyMatch(lambdaLine -> operationContainsLine(operation, lambdaLine));
    }

    private String extractExpressionAndConvertToStatement(String line) {
        return line.replaceFirst("\\(\\) -> ", "") + ";";
    }

    private boolean lambdaBodyIsExpression(String line) {
        return line.endsWith(")");
    }

    private boolean operationContainsLine(UMLOperation operation, String line) {
        return operation.getBody().stringRepresentation().stream()
                .map(String::strip)
                .filter(s -> s.length() > 1) // Ignore "{" and "}" lines
                .anyMatch(operationBodyLine -> operationBodyLine.equals(line));
    }

    private Optional<ModifyMethodAnnotationRefactoring> getRemovedExpectedAttributeFromTestAnnotation() {
        return refactorings.stream()
                .filter(r -> r.getRefactoringType().equals(RefactoringType.MODIFY_METHOD_ANNOTATION))
                .map(r -> (ModifyMethodAnnotationRefactoring) r)
                .filter(r -> r.getOperationBefore().equals(operationBefore))
                .filter(r -> r.getOperationAfter().equals(operationAfter))
                .filter(r -> hasExpectedException(r.getAnnotationBefore()))
                .filter(r -> !hasExpectedException(r.getAnnotationAfter()))
                .findAny();
    }

    private boolean hasExpectedException(gr.uom.java.xmi.UMLAnnotation before) {
        return before.isNormalAnnotation() && before.getTypeName().equals("Test") && before.getMemberValuePairs().containsKey("expected");
    }

    private List<AbstractCall> getAssertThrows(UMLOperation operation) {
        return operation.getAllOperationInvocations().stream()
                .filter((op) -> op.getName().equals("assertThrows") &&
                        (Objects.isNull(op.getExpression()) || op.getExpression().equals("Assert") || op.getExpression().equals("Assertions")))
                .collect(Collectors.toList());
    }
}
