package gr.uom.java.xmi.annotation.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import gr.uom.java.xmi.LocationInfo;
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
        if (returnedVarCandidates.isEmpty()) {
			Optional<StatementObject> stmtCandidate = operation.getBody().getCompositeStatement().getStatements().stream()
					.filter(s -> s instanceof StatementObject)
					.map(s -> (StatementObject) s)
					.filter(AbstractCodeFragment::isLastStatement)
					.findAny();
			if (stmtCandidate.isPresent()) {
                AbstractCall call = stmtCandidate.get().invocationCoveringEntireFragment();
				if(call != null) {
					if(call.getName().equals("of")) {
						for(AbstractCall nestedCall : stmtCandidate.get().getMethodInvocations()) {
							if(nestedCall.getExpression() != null && !nestedCall.getExpression().equals("Stream") && nestedCall.getName().equals("of")) {
								testParameters.add(nestedCall.arguments());
								List<LeafExpression> leafExpressions = new ArrayList<>();
								Set<LocationInfo> claimedLocations = new HashSet<>();
								for(String arg : nestedCall.arguments()) {
									List<LeafExpression> matches = stmtCandidate.get().findExpression(arg);
									for(LeafExpression match : matches) {
										if(nestedCall.getLocationInfo().subsumes(match.getLocationInfo()) && !claimedLocations.contains(match.getLocationInfo())) {
											leafExpressions.add(match);
											claimedLocations.add(match.getLocationInfo());
											break;
										}
									}
								}
								testParameterLeafExpressions.add(leafExpressions);
							}
						}
					}
					else if(call.getName().equals("asList")) {
						List<AbstractCall> nestedMethodInvocations = stmtCandidate.get().getMethodInvocations();
						if(nestedMethodInvocations.size() == 1 && nestedMethodInvocations.get(0).equals(call)) {
							Set<LocationInfo> claimedAsListLocations = new HashSet<>();
							for(String arg : call.arguments()) {
								testParameters.add(Collections.singletonList(sanitizeLiteral(arg)));
								List<LeafExpression> matches = stmtCandidate.get().findExpression(arg);
								List<LeafExpression> leafExpressions = new ArrayList<>();
								for(LeafExpression match : matches) {
									if(call.getLocationInfo().subsumes(match.getLocationInfo()) && !claimedAsListLocations.contains(match.getLocationInfo())) {
										leafExpressions.add(match);
										claimedAsListLocations.add(match.getLocationInfo());
										break;
									}
								}
								testParameterLeafExpressions.add(leafExpressions);
							}
						}
						else {
							for(AbstractCall nestedCall : stmtCandidate.get().getMethodInvocations()) {
								if(nestedCall.getExpression() != null && !nestedCall.getExpression().equals("Stream") && nestedCall.getName().equals("of")) {
									testParameters.add(nestedCall.arguments());
									List<LeafExpression> leafExpressions = new ArrayList<>();
									Set<LocationInfo> claimedLocations = new HashSet<>();
									for(String arg : nestedCall.arguments()) {
										List<LeafExpression> matches = stmtCandidate.get().findExpression(arg);
										for(LeafExpression match : matches) {
											if(nestedCall.getLocationInfo().subsumes(match.getLocationInfo()) && !claimedLocations.contains(match.getLocationInfo())) {
												leafExpressions.add(match);
												claimedLocations.add(match.getLocationInfo());
												break;
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
	}

}
