package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
class MockMultiMemberRefactoringDependencies extends MockRefactoringDependencies<MultiMemberRefactoring, VariableDeclarationContainer> {
    @Override
    protected Map<? super VariableDeclarationContainer, Set<? super MultiMemberRefactoring>> mapMemberToRefactoring(List<? extends MultiMemberRefactoring> refactorings) {
        IdentityHashMap<? super VariableDeclarationContainer, Set<? super MultiMemberRefactoring>> result = new IdentityHashMap();
        for (MultiMemberRefactoring refactoring : refactorings) {
            this.mapMemberToRefactoring(result, refactoring);
        }
        return result;
    }

    @Override
    protected void mapMemberToRefactoring(IdentityHashMap<? super VariableDeclarationContainer, Set<? super MultiMemberRefactoring>> result, MultiMemberRefactoring refactoring) {
        mapMemberToRefactoring(result, refactoring, refactoring::getMembersBefore);
        mapMemberToRefactoring(result, refactoring, refactoring::getMembersAfter);
    }

}
class MockMultiClassRefactoringDependencies extends MockRefactoringDependencies<MultiClassRefactoring, UMLAbstractClass> {
    @Override
    protected Map<? super UMLAbstractClass, Set<? super MultiClassRefactoring>> mapMemberToRefactoring(List<? extends MultiClassRefactoring> refactorings) {
        IdentityHashMap<? super UMLAbstractClass, Set<? super MultiClassRefactoring>> result = new IdentityHashMap();
        for (MultiClassRefactoring refactoring : refactorings) {
            this.mapMemberToRefactoring(result, refactoring);
        }
        return result;
    }
    @Override
    protected void mapMemberToRefactoring(IdentityHashMap<? super UMLAbstractClass, Set<? super MultiClassRefactoring>> result, MultiClassRefactoring refactoring) {
        mapMemberToRefactoring(result, refactoring, refactoring::getClassesBefore);
        mapMemberToRefactoring(result, refactoring, refactoring::getClassesAfter);
    }
}
class MockMultiStatementRefactoringDependencies extends MockRefactoringDependencies<MultiStatementRefactoring, AbstractCodeFragment> {
    @Override
    protected Map<? super AbstractCodeFragment, Set<? super MultiStatementRefactoring>> mapMemberToRefactoring(List<? extends MultiStatementRefactoring> refactorings) {
        IdentityHashMap<? super AbstractCodeFragment, Set<? super MultiStatementRefactoring>> result = new IdentityHashMap();
        for (MultiStatementRefactoring refactoring : refactorings) {
            this.mapMemberToRefactoring(result, refactoring);
        }
        return result;
    }
    @Override
    protected void mapMemberToRefactoring(IdentityHashMap<? super AbstractCodeFragment, Set<? super MultiStatementRefactoring>> result, MultiStatementRefactoring refactoring) {
        mapMemberToRefactoring(result, refactoring, refactoring::getStatementsBefore);
        mapMemberToRefactoring(result, refactoring, refactoring::getStatementsAfter);
    }

}
@ExtendWith(MockitoExtension.class)
public abstract class MockRefactoringDependencies<T, R> {
    protected Map<? super R, Set<? super T>> mapMemberToRefactoring(List<? extends T> refactorings) {
        IdentityHashMap<? super R, Set<? super T>> result = new IdentityHashMap();
        for (T refactoring : refactorings) {
            mapMemberToRefactoring(result, refactoring);
        }
        return result;
    }
    protected void mapMemberToRefactoring(IdentityHashMap<? super R, Set<? super T>> result, T refactoring) {
        throw new UnsupportedOperationException("Not implemented");
    }
    protected  <T,R> void mapMemberToRefactoring(IdentityHashMap<? super R, Set<? super T>> result, T refactoring, Supplier<Collection<? extends R>> supplier) {
        for (R member1 : supplier.get()) {
            if (result.containsKey(member1)) {
                result.get(member1).add(refactoring);
            }
            else {
                result.put(member1, new ArraySet<>(Collections.singleton(refactoring)));
            }
        }
    }

    protected class ArraySet<E> extends AbstractSet<E> {
        private final ArrayList<E> list = new ArrayList<>();

        public ArraySet() {
        }

        public ArraySet(Collection<E> c) {
            addAll(c);
        }

        @Override
        public boolean add(E e) {
            if (!list.contains(e)) {
                list.add(e);
                return true;
            }
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            return list.iterator();
        }

        @Override
        public int size() {
            return list.size();
        }
    }
    /* Mock dependencies */
    @Mock
    AbstractCodeFragment statementBefore;
    @Mock
    AbstractCodeFragment statementAfter;
    @Mock
    UMLAnnotation annotation;
    @Mock
    UMLOperation operationBefore;
    @Mock
    UMLOperationBodyMapper mapper;
    @Mock
    UMLOperation operationAfter;
    @Mock
    VariableDeclaration variableBefore;
    @Mock
    VariableDeclaration variableAfter;
    @Mock
    AbstractCodeMapping variableReference;
    @Mock
    VariableDeclaration newVariable;
    Set<UMLOperationBodyMapper> mappers = Collections.singleton(mapper);
    Set<AbstractCodeFragment> statementsBefore = Collections.singleton(statementBefore);
    Set<AbstractCodeFragment> statementsAfter = Collections.singleton(statementAfter);
    Set<AbstractCodeMapping> variableReferences = Collections.singleton(variableReference);
    Set<VariableDeclaration> mergedVariables = Collections.singleton(newVariable);
    boolean insideExtractedOrInlinedMethod = false;
    @Mock
    AbstractCall methodCall;
    List<AbstractCall> operationInvocations = Collections.singletonList(methodCall);
    @Mock
    UMLOperation extractedMethod;
    @Mock
    UMLAttribute attributeBefore;
    @Mock
    UMLAttribute attributeAfter;
    @Mock
    UMLAbstractClass classBefore;
    @Mock
    UMLAbstractClass classAfter;
    @Mock CandidateMergeVariableRefactoring candidate;
    Set<CandidateMergeVariableRefactoring> candidatesMerge = Collections.singleton(candidate);
    @Mock CandidateSplitVariableRefactoring candidateSplit;

    Set<CandidateSplitVariableRefactoring> candidatesSplit = Collections.singleton(candidateSplit);
}
