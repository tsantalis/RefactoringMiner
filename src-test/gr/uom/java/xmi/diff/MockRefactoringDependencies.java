package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class MockRefactoringDependencies {
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
