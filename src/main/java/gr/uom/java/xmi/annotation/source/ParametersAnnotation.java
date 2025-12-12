package gr.uom.java.xmi.annotation.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;

public class ParametersAnnotation extends SourceAnnotation {
	public static final String ANNOTATION_TYPENAME = "Parameters";
	public static final String QUALIFIED_ANNOTATION_TYPENAME = "Parameterized.Parameters";

	public ParametersAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass declaringClass) {
		super(annotation, annotation.getTypeName().equals(QUALIFIED_ANNOTATION_TYPENAME) ? QUALIFIED_ANNOTATION_TYPENAME : ANNOTATION_TYPENAME);
		Optional<VariableDeclaration> returnedVarCandidates = operation.getBody().getAllVariableDeclarations().stream().filter(v -> operation.getReturnParameter().getType().equals(v.getType())).findAny();
        String strLiterals;
        if (returnedVarCandidates.isPresent()) {
            Set<AbstractCodeFragment> stmtsUsingVar = returnedVarCandidates.get().getStatementsInScopeUsingVariable();
            strLiterals = stmtsUsingVar.stream()
                    .flatMap(stmt -> stmt.getStringLiterals().stream())
                    .map(str -> str.getString())
                    .collect(Collectors.joining(System.getProperty("line.separator")));
        } else {
            Optional<StatementObject> stmtCandidate = operation.getBody().getCompositeStatement().getStatements().stream()
                    .filter(s -> s instanceof StatementObject)
                    .map(s -> (StatementObject) s)
                    .filter(s -> s.isLastStatement())
                    .findAny();
            if (stmtCandidate.isPresent()) {
                strLiterals = stmtCandidate.get().getStringLiterals().stream()
                        .map(str -> str.getString())
                        .collect(Collectors.joining(System.getProperty("line.separator")));
                AbstractCall call = stmtCandidate.get().invocationCoveringEntireFragment();
                if(call != null) {
                	if(call.getName().equals("of")) {
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
                	else if(call.getName().equals("asList")) {
                		testParameters.add(call.arguments());
                        List<LeafExpression> leafExpressions = new ArrayList<>();
                        for(String arg : call.arguments()) {
                        	List<LeafExpression> matches = stmtCandidate.get().findExpression(arg);
                        	for(LeafExpression match : matches) {
                        		if(call.getLocationInfo().subsumes(match.getLocationInfo())) {
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
