package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.TryStatementObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents expecting exception test encoding migration (from JUnit 3 to Rule-based JUnit 4)
 * JUnit3 relies on try-catch containing an Assert.fail call to expect thrown exceptions
 * JUnit4 usually relies on @Rule ExpectedException, a Single Member annotated field, which provides an expect method
 */
public class TryWithFailToExpectedExceptionRuleRefactoring implements Refactoring {
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;
    private final TryStatementObject tryStatement;
    private final AbstractCall assertFailInvocation;
    private final String exception;
    private final AbstractCall thrownExpectInvocations;
    private final UMLAttribute ruleFieldDeclaration;

    public UMLOperation getOperationBefore() {
        return operationBefore;
    }

    public UMLOperation getOperationAfter() {
        return operationAfter;
    }

    public TryStatementObject getTryStatement() {
        return tryStatement;
    }

    public AbstractCall getAssertFailInvocation() {
        return assertFailInvocation;
    }

    public String getException() {
        return exception;
    }

    public AbstractCall getThrownExpectInvocations() {
        return thrownExpectInvocations;
    }

    public UMLAttribute getRuleFieldDeclaration() {
        return ruleFieldDeclaration;
    }

    public TryWithFailToExpectedExceptionRuleRefactoring(UMLOperation operationBefore, UMLOperation operationAfter, TryStatementObject tryStatement, AbstractCall assertFailInvocation, String capturedException, AbstractCall thrownExpectInvocation, UMLAttribute ruleFieldDeclaration) {
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.tryStatement = tryStatement;
        this.assertFailInvocation = assertFailInvocation;
        this.exception = capturedException;
        this.thrownExpectInvocations = thrownExpectInvocation;
        this.ruleFieldDeclaration = ruleFieldDeclaration;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(operationBefore.codeRange()
                .setDescription("source method declaration before migration")
                .setCodeElement(operationBefore.toString()));
        ranges.add(tryStatement.codeRange()
                .setDescription("source method's try-statement")
                .setCodeElement(tryStatement.toString()));
        var catchClause = tryStatement.getCatchClauses().stream().filter(clause -> clause.getVariableDeclarations().stream().anyMatch(v->v.getType().getClassType().equals("IllegalArgumentException"))).findAny();
        if (catchClause.isPresent()) {
            var clause = catchClause.get();
            ranges.add(clause.codeRange()
                        .setDescription("source method's catch clause capturing the expected exception")
                        .setCodeElement(clause.toString()));
        }
        ranges.add(assertFailInvocation.codeRange()
                .setDescription("source method's assertFail invocation from the try-statement before migration")
                .setCodeElement(assertFailInvocation.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(operationAfter.codeRange()
                .setDescription("method declaration after migration")
                .setCodeElement(operationAfter.toString()));
        ranges.add(ruleFieldDeclaration.codeRange()
                .setDescription("ExpectedException field annotated with @Rule")
                .setCodeElement(ruleFieldDeclaration.toString()));
        ranges.add(thrownExpectInvocations.codeRange()
                .setDescription("method's statement invoking ExpectedException's expect method")
                .setCodeElement(thrownExpectInvocations.toString()));
        return ranges;
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
    public RefactoringType getRefactoringType() {
        return RefactoringType.REPLACE_TRY_FAIL_WITH_RULE;
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
