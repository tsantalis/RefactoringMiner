package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.annotation.MarkerAnnotation;
import gr.uom.java.xmi.annotation.SingleMemberAnnotation;

import java.util.*;
import java.util.stream.Collectors;

public class MethodSourceAnnotation extends SourceAnnotation implements SingleMemberAnnotation, MarkerAnnotation {
    private static final String ANNOTATION_TYPENAME = "MethodSource";
    private final UMLOperation annotatedOperation;

    public MethodSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        super(annotation, ANNOTATION_TYPENAME);
        annotatedOperation = operation;
        List<String> values = getValue();
        assert values.size() == 1;
        String methodSourceName = values.get(0);
        String pkg = operation.getClassName().substring(0, operation.getClassName().lastIndexOf("."));
        String name = operation.getNonQualifiedClassName();
        LocationInfo locationInfo = operation.getLocationInfo();
        UMLClass declaringClass = model.getClass(new UMLClass(pkg, name, locationInfo, true, Collections.emptyList()));
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
            }
        }
    }

    @Override
    public List<String> getValue() {
        return Collections.singletonList(annotation.isMarkerAnnotation() ? annotatedOperation.getName() : annotation.getValue().getTypeLiterals().get(0).getString());
    }

    @Override
    public List<List<String>> getTestParameters() {
        return null;
    }
}
