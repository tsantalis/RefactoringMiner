package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.annotation.MarkerAnnotation;
import gr.uom.java.xmi.annotation.SingleMemberAnnotation;

import java.util.*;
import java.util.stream.Collectors;

public class MethodSourceAnnotation extends SourceAnnotation implements SingleMemberAnnotation, MarkerAnnotation {
    public static final String ANNOTATION_TYPENAME = "MethodSource";
    private final UMLOperation annotatedOperation;
    private final UMLAbstractClass declaringClass;

    public MethodSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass declaringClass) {
        super(annotation, ANNOTATION_TYPENAME);
        this.annotatedOperation = operation;
        this.declaringClass = declaringClass;
        List<String> values = getValue();
        if(values.size() > 0) {
            String methodSourceName = values.get(0);
            processMethodSourceName(operation, declaringClass, methodSourceName);
        }
    }

    private void processMethodSourceName(UMLOperation operation, UMLAbstractClass declaringClass, String methodSourceName) {
        List<UMLOperation> sameNameMethods = declaringClass.getOperations().stream().filter(op -> op.getName().equals(methodSourceName)).collect(Collectors.toList());
        for (int maxIterations = sameNameMethods.size(); sameNameMethods.size() > 1 && maxIterations-- > 0; ) {
            for (Iterator<UMLOperation> iterator = sameNameMethods.iterator(); iterator.hasNext(); ) {
                UMLOperation method = iterator.next();
                if (method.getAnnotations().containsAll(operation.getAnnotations())) {
                    iterator.remove();
                    break;
                }
                if (method.equalSignature(operation)) {
                    iterator.remove();
                    break;
                }
            }
        }
        if(sameNameMethods.size() > 0) {
            UMLOperation sourceMethod = sameNameMethods.get(0);
            Optional<VariableDeclaration> returnedVarCandidates = sourceMethod.getBody().getAllVariableDeclarations().stream().filter(v -> sourceMethod.getReturnParameter().getType().equals(v.getType())).findAny();
            if (returnedVarCandidates.isEmpty()) {
                Optional<StatementObject> stmtCandidate = sourceMethod.getBody().getCompositeStatement().getStatements().stream()
                        .filter(s -> s instanceof StatementObject)
                        .map(s -> (StatementObject) s)
                        .filter(AbstractCodeFragment::isLastStatement)
                        .findAny();
                if (stmtCandidate.isPresent()) {
                    AbstractCall call = stmtCandidate.get().invocationCoveringEntireFragment();
                    if(call != null && call.getName().equals("of")) {
                        for(AbstractCall nestedCall : stmtCandidate.get().getMethodInvocations()) {
                            if(nestedCall.getExpression() != null && !nestedCall.getExpression().equals("Stream") && nestedCall.getName().equals("of")) {
                                List<String> resolvedArguments = new ArrayList<>();
                                List<LeafExpression> leafExpressions = new ArrayList<>();
                                for(String arg : nestedCall.arguments()) {
                                    LeafExpression constantLiteral = resolveConstantLiteral(arg);
                                    if(constantLiteral != null) {
                                        resolvedArguments.add(constantLiteral.getString());
                                        leafExpressions.add(constantLiteral);
                                    }
                                    else {
                                        resolvedArguments.add(arg);
                                        List<LeafExpression> matches = stmtCandidate.get().findExpression(arg);
                                        for(LeafExpression match : matches) {
                                            if(nestedCall.getLocationInfo().subsumes(match.getLocationInfo())) {
                                                leafExpressions.add(match);
                                            }
                                        }
                                    }
                                }
                                testParameters.add(resolvedArguments);
                                testParameterLeafExpressions.add(leafExpressions);
                            }
                        }
                    }
                }
            }
        }
    }

    private LeafExpression resolveConstantLiteral(String name) {
        for(UMLAttribute attribute : declaringClass.getAttributes()) {
            if(attribute.getName().equals(name) && attribute.isFinal() && attribute.isStatic()) {
                AbstractExpression initializer = attribute.getVariableDeclaration().getInitializer();
                if(initializer != null) {
                    List<LeafExpression> literals = new ArrayList<>();
                    literals.addAll(initializer.getStringLiterals());
                    literals.addAll(initializer.getNumberLiterals());
                    literals.addAll(initializer.getBooleanLiterals());
                    literals.addAll(initializer.getNullLiterals());
                    if(!literals.isEmpty()) {
                        return literals.get(0);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<String> getValue() {
        if(annotation.isMarkerAnnotation()) {
            return Collections.singletonList(annotatedOperation.getName());
        }
        else if (annotation.isNormalAnnotation()) {
            ArrayList<String> values = new ArrayList<>();
            for (AbstractExpression value : annotation.getMemberValuePairs().values()) {
                values.addAll(extractLiteralFromValue(value));
            }
            return values;
        }
        else if(annotation.isSingleMemberAnnotation()) {
            ArrayList<String> values = new ArrayList<>();
            AbstractExpression value = annotation.getValue();
            values.addAll(extractLiteralFromValue(value));
            if(values.isEmpty()) {
                for(UMLAttribute attribute : declaringClass.getAttributes()) {
                    if(value.getString().equals(attribute.getName()) && attribute.getVariableDeclaration().getInitializer() !=  null) {
                        values.addAll(extractLiteralFromValue(attribute.getVariableDeclaration().getInitializer()));
                    }
                }
            }
            return values;
        }
        else {
            return extractLiteralFromValue(annotation.getValue());
        }
    }

    private static List<String> extractLiteralFromValue(AbstractExpression expr) {
        if(expr.getTypeLiterals().size() > 0) {
            return Collections.singletonList(expr.getTypeLiterals().get(0).getString());
        }
        else if (expr.getStringLiterals().size() > 0) {
            return Collections.singletonList(expr.getStringLiterals().get(0).getString().replace("\"", ""));
        }
        return Collections.emptyList();
    }
}
