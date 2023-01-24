package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Detects expecting exception test encoding migration (from JUnit 3 to Rule-based JUnit 4)
 * JUnit3 relies on try-catch containing an Assert.fail call to expect thrown exceptions
 * JUnit4 usually relies on @Rule ExpectedException, a Single Member annotated field, which provides an expect method
 */
public class TryWithFailToExpectedExceptionRuleDetection {
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final List<UMLAttribute> addedAttributes;
    private final List<CompositeStatementObject> removedCompositeStmts;
    private final List<AbstractCodeFragment> addedStmts;
    private List<TryStatementObject> tryStatements;
    private List<String> capturedExceptions;
    private List<AbstractCall> assertFailInvocationsFound;
    private UMLAttribute expectedExceptionFieldDeclaration;

    List<TryStatementObject> getTryStatements() {
        return tryStatements;
    }

    List<String> getCapturedExceptions() {
        return capturedExceptions;
    }

    List<AbstractCall> getAssertFailInvocationsFound() {
        return assertFailInvocationsFound;
    }

    UMLAttribute getExpectedExceptionFieldDeclaration() {
        return expectedExceptionFieldDeclaration;
    }

    List<AbstractCall> getExpectInvocations() {
        return expectInvocations;
    }

    private List<AbstractCall> expectInvocations;

    public TryWithFailToExpectedExceptionRuleDetection(UMLOperationBodyMapper mapper, UMLClassBaseDiff classDiff) {
        this(mapper,classDiff.addedAttributes);
    }

    public TryWithFailToExpectedExceptionRuleDetection(UMLOperationBodyMapper mapper, List<UMLAttribute> addedAttributes) {
        this(mapper.getOperation1(), mapper.getOperation2(), mapper.getNonMappedInnerNodesT1(), mapper.getNonMappedLeavesT2(), addedAttributes);
    }

    public TryWithFailToExpectedExceptionRuleDetection(UMLOperation operationBefore, UMLOperation operationAfter, List<CompositeStatementObject> removedCompositeStmts, List<AbstractCodeFragment> addedStmts, List<UMLAttribute> addedAttributes) {
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.removedCompositeStmts = removedCompositeStmts;
        this.addedStmts = addedStmts;
        this.addedAttributes = addedAttributes;
    }

    public TryWithFailToExpectedExceptionRuleRefactoring check() {
        try {
            if (checkFromTryWithFail() && checkToExpectedException()) {
                return createRefactoring();
            }
            return null;
        }
        catch (NoSuchElementException exception) {
            return null;
        }
    }

    private TryWithFailToExpectedExceptionRuleRefactoring createRefactoring() {
        var tryStmt = tryStatements.get(0);
        var assertFailInvocation = assertFailInvocationsFound.get(0);
        var expectInvocation = expectInvocations.stream().filter(op -> capturedExceptions.contains(op.arguments().get(0))).findAny().orElseThrow();
        var capturedException = expectInvocation.arguments().get(0);
        return new TryWithFailToExpectedExceptionRuleRefactoring(operationBefore, operationAfter, tryStmt, assertFailInvocation, capturedException, expectInvocation, expectedExceptionFieldDeclaration);
    }

    private boolean checkFromTryWithFail() {
        tryStatements = filterTryStatement(removedCompositeStmts).collect(Collectors.toList());
        capturedExceptions = tryStatements.stream()
                .filter(stmt -> detectAssertFailInvocationAtTheEndOf(stmt).findAny().isPresent())
                .flatMap(TryWithFailToExpectedExceptionRuleDetection::detectCatchExceptions)
                .map(exception -> exception.concat(".class"))
                .collect(Collectors.toList());
        assertFailInvocationsFound = tryStatements.stream()
                .flatMap(TryWithFailToExpectedExceptionRuleDetection::detectAssertFailInvocationAtTheEndOf)
                .collect(Collectors.toList());
        return assertFailInvocationsFound.size() > 0;
    }

    private boolean checkToExpectedException() {
        expectedExceptionFieldDeclaration = addedAttributes.stream()
                .filter(field -> field.getType().getClassType().equals("ExpectedException"))
                .findAny()
                .orElseThrow();
        expectInvocations = detectAddedExpectInvocations(addedStmts,capturedExceptions, expectedExceptionFieldDeclaration)
                .collect(Collectors.toList());
        return expectInvocations.size() > 0;
    }

    private static Stream<AbstractCall> detectAddedExpectInvocations(List<AbstractCodeFragment> addedStmts, List<String> capturedExceptions, UMLAttribute expectedExceptionRuleFieldDeclaration) {
        return extractMethodInvocationsStream(addedStmts)
                .filter(invocation -> isExpectedExceptionExpectInvocation(capturedExceptions, invocation))
                .filter(expectInvocation -> expectedExceptionRuleFieldDeclaration.getName().equals(expectInvocation.getExpression()))
                .filter(expectInvocation -> expectInvocation.arguments().size() == 1)
                .filter(expectInvocation -> capturedExceptions.contains(expectInvocation.arguments().get(0)));
    }

    private static boolean isExpectedExceptionExpectInvocation(List<String> candidateExceptions, AbstractCall invocation) {
        return invocation.getName().equals("expect") && isAnyArgumentPassedTo(candidateExceptions, invocation);
    }

    private static boolean isAnyArgumentPassedTo(List<String> arguments, AbstractCall invocation) {
        return arguments.contains(invocation.arguments().get(0));
    }

    private static Stream<AbstractCall> extractMethodInvocationsStream(List<AbstractCodeFragment> addedStmts) {
        return addedStmts.stream().flatMap(st -> st.getMethodInvocations().stream());
    }

    private static Stream<AbstractCall> detectAssertFailInvocationAtTheEndOf(TryStatementObject tryStatement) {
        List<AbstractCall> operationInvocationsInLastStatement;
        try {
            var lastStatement = tryStatement.getStatements().get(tryStatement.getStatements().size() - 1);
            operationInvocationsInLastStatement = new ArrayList<>(lastStatement.getMethodInvocations());
        } catch (IndexOutOfBoundsException e) {
            return Stream.empty();
        }
        var nonNullInvocations = operationInvocationsInLastStatement.stream().filter(Objects::nonNull);
        var nonNullFailInvocations = nonNullInvocations.filter(invocation -> Objects.nonNull(invocation.getName()) && invocation.getName().equals("fail"));
        return nonNullFailInvocations.filter(invocation -> Objects.isNull(invocation.getExpression()) || invocation.getExpression().equals("Assert"));
    }

    private static Stream<String> detectCatchExceptions(TryStatementObject tryStatement) {
        return tryStatement.getCatchClauses().stream()
                .flatMap(clause -> clause.getVariableDeclarations().stream()
                        .map(variable -> variable.getType().getClassType()))
                .filter(classType -> classType.endsWith("Exception"));
    }

    private static Stream<TryStatementObject> filterTryStatement(List<CompositeStatementObject> stmts) {
        return stmts.stream()
                .filter(st->st instanceof TryStatementObject)
                .map(st -> (TryStatementObject)st);
    }
}
