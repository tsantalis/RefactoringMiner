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

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class MethodSourceAnnotation extends SourceAnnotation implements SingleMemberAnnotation, MarkerAnnotation {
    public static final String ANNOTATION_TYPENAME = "MethodSource";
    private final UMLOperation annotatedOperation;

    public MethodSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass declaringClass) {
        super(annotation, ANNOTATION_TYPENAME);
        this.annotatedOperation = operation;
        List<String> values = getValue();
        assert values.size() == 1;
        String methodSourceName = values.get(0);
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
	        String strLiterals;
	        if (returnedVarCandidates.isPresent()) {
	            Set<AbstractCodeFragment> stmtsUsingVar = returnedVarCandidates.get().getStatementsInScopeUsingVariable();
	            strLiterals = stmtsUsingVar.stream()
	                    .flatMap(stmt -> stmt.getStringLiterals().stream())
	                    .map(str -> str.getString())
	                    .collect(Collectors.joining(System.getProperty("line.separator")));
	        } else {
	            Optional<StatementObject> stmtCandidate = sourceMethod.getBody().getCompositeStatement().getStatements().stream()
	                    .filter(s -> s instanceof StatementObject)
	                    .map(s -> (StatementObject) s)
	                    .filter(s -> s.isLastStatement())
	                    .findAny();
	            if (stmtCandidate.isPresent()) {
	                strLiterals = stmtCandidate.get().getStringLiterals().stream()
	                        .map(str -> str.getString())
	                        .collect(Collectors.joining(System.getProperty("line.separator")));
	                AbstractCall call = stmtCandidate.get().invocationCoveringEntireFragment();
	                if(call != null && call.getName().equals("of")) {
	                    for(AbstractCall nestedCall : stmtCandidate.get().getMethodInvocations()) {
	                        if(nestedCall.getExpression() != null && !nestedCall.getExpression().equals("Stream") && nestedCall.getName().equals("of")) {
	                            testParameters.add(nestedCall.arguments());
	                            List<LeafExpression> leafExpressions = new ArrayList<>();
	                            for(String arg : nestedCall.arguments()) {
	                            	List<LeafExpression> matches = stmtCandidate.get().findExpression(arg);
	                            	for(LeafExpression match : matches) {
	                            		if(nestedCall.getLocationInfo().subsumes(match.getLocationInfo())) {
	                            			leafExpressions.add(match);
	                            		}
	                            	}
	                            }
	                            testParameterLeafExpressions.add(leafExpressions);
	                        }
	                    }
	                }
	            }
	        }
        }
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
