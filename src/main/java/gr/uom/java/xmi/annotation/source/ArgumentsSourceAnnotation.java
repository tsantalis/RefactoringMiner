package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.annotation.SingleMemberAnnotation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.StatementObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ArgumentsSourceAnnotation extends SourceAnnotation implements SingleMemberAnnotation {
    public static final String ANNOTATION_TYPENAME = "ArgumentsSource";
    private UMLAbstractClass resolvedProviderClass;

    public ArgumentsSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, List<UMLAbstractClass> declaringClasses) {
        super(annotation, ANNOTATION_TYPENAME);
        List<String> values = getValue();
        if(values.size() > 0) {
            String providerClassName = sanitizeLiteral(values.get(0));
            processProviderClassName(declaringClasses, providerClassName);
        }
    }

    public UMLAbstractClass getResolvedProviderClass() {
        return resolvedProviderClass;
    }

    private void processProviderClassName(List<UMLAbstractClass> declaringClasses, String providerClassName) {
        for(UMLAbstractClass declaringClass : declaringClasses) {
            if(declaringClass.getName().equals(providerClassName) || declaringClass.getName().endsWith("." + providerClassName)) {
                resolvedProviderClass = declaringClass;
                break;
            }
        }
        if(resolvedProviderClass == null) {
            return;
        }
        Optional<UMLOperation> providerMethod = resolvedProviderClass.getOperations().stream()
                .filter(op -> op.getName().equals("provideArguments"))
                .findFirst();
        if(providerMethod.isEmpty() || providerMethod.get().getBody() == null) {
            return;
        }
        extractLiteralArgumentRows(providerMethod.get());
    }

    private void extractLiteralArgumentRows(UMLOperation providerMethod) {
        Optional<StatementObject> stmtCandidate = providerMethod.getBody().getCompositeStatement().getStatements().stream()
                .filter(s -> s instanceof StatementObject)
                .map(s -> (StatementObject) s)
                .filter(AbstractCodeFragment::isLastStatement)
                .findAny();
        if(stmtCandidate.isEmpty()) {
            return;
        }
        StatementObject statement = stmtCandidate.get();
        AbstractCall call = statement.invocationCoveringEntireFragment();
        if(call == null) {
            return;
        }
        if(call.getName().equals("of")) {
            //e.g. return Stream.of(Arguments.of(a, b), Arguments.of(c, d));
            extractArgumentsOfRows(statement);
        }
        else if(call.getName().equals("map") && isArgumentsOfMethodReference(call)) {
            //e.g. return Stream.of(new Fixture("a"), new Fixture("b")).map(Arguments::of);
            //each element of the Stream.of(...) this is chained onto becomes its own single-value row
            extractStreamOfMapRows(statement);
        }
    }

    private boolean isArgumentsOfMethodReference(AbstractCall call) {
        List<String> callArguments = call.arguments();
        return callArguments.size() == 1 && callArguments.get(0).equals("Arguments::of");
    }

    private void extractArgumentsOfRows(StatementObject statement) {
        List<AbstractCall> rowCallCandidates = new ArrayList<>();
        for(AbstractCall nestedCall : statement.getMethodInvocations()) {
            if(nestedCall.getExpression() != null && !nestedCall.getExpression().equals("Stream") && nestedCall.getName().equals("of")) {
                rowCallCandidates.add(nestedCall);
            }
            else if(nestedCall.getExpression() == null && nestedCall.getName().equals("arguments")) {
                //it is possible to use the arguments() function
                //import static org.junit.jupiter.params.provider.Arguments.arguments
                rowCallCandidates.add(nestedCall);
            }
        }
        for(AbstractCall nestedCall : rowCallCandidates) {
            boolean nestedInAnotherRowCall = rowCallCandidates.stream()
                    .anyMatch(other -> other != nestedCall && other.getLocationInfo().subsumes(nestedCall.getLocationInfo()));
            if(nestedInAnotherRowCall) {
                continue;
            }
            addRowIfMatched(statement, nestedCall, nestedCall.arguments());
        }
    }

    private void extractStreamOfMapRows(StatementObject statement) {
        AbstractCall streamOfCall = statement.getMethodInvocations().stream()
                .filter(nestedCall -> nestedCall.getName().equals("of") && "Stream".equals(nestedCall.getExpression()))
                .findFirst().orElse(null);
        if(streamOfCall == null) {
            return;
        }
        for(String arg : streamOfCall.arguments()) {
            addRowIfMatched(statement, streamOfCall, Collections.singletonList(arg));
        }
    }

    private void addRowIfMatched(StatementObject statement, AbstractCall scopeCall, List<String> rowArguments) {
        List<String> resolvedArguments = new ArrayList<>();
        List<LeafExpression> leafExpressions = new ArrayList<>();
        for(String arg : rowArguments) {
            List<LeafExpression> matches = statement.findExpression(arg);
            LeafExpression match = matches.stream()
                    .filter(m -> scopeCall.getLocationInfo().subsumes(m.getLocationInfo()))
                    .findFirst().orElse(null);
            resolvedArguments.add(arg);
            if(match != null) {
                leafExpressions.add(match);
            }
        }
        if(!leafExpressions.isEmpty()) {
            testParameters.add(resolvedArguments);
            testParameterLeafExpressions.add(leafExpressions);
        }
    }

    @Override
    public List<String> getValue() {
        assert !annotation.isMarkerAnnotation() : "getValue() is not supported by marker ArgumentsSource annotation";
        List<LeafExpression> typeLiterals = annotation.getValue().getTypeLiterals();
        if(typeLiterals.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(typeLiterals.get(0).getString());
    }
}
