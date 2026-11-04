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
    private final List<UMLAbstractClass> declaringClasses;
    private UMLOperation resolvedProviderMethod;

    public MethodSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, List<UMLAbstractClass> declaringClasses) {
        super(annotation, ANNOTATION_TYPENAME);
        this.annotatedOperation = operation;
        this.declaringClasses = declaringClasses;
        //every provider of the annotation, i.e., @MethodSource({"a", "b"})
        for(String methodSourceName : getValue()) {
            processMethodSourceName(operation, methodSourceName);
        }
    }

    //the classes of the providers referenced by their fully qualified method names, i.e., @MethodSource("net.aholbrook.paseto.Sources#encodingProviders")
    public static List<String> providerClassNames(UMLAnnotation annotation) {
        List<AbstractExpression> values = new ArrayList<>();
        if(annotation.getValue() != null) {
            values.add(annotation.getValue());
        }
        values.addAll(annotation.getMemberValuePairs().values());
        List<String> classNames = new ArrayList<>();
        for(AbstractExpression value : values) {
            for(LeafExpression literal : value.getStringLiterals()) {
                String className = providerClassName(literal.getString().replace("\"", ""));
                if(className != null && !classNames.contains(className)) {
                    classNames.add(className);
                }
            }
        }
        return classNames;
    }

    //the class part of Class#method, with the binary name of a nested class, i.e., Outer$Inner, as its source name, or null for a local method
    private static String providerClassName(String methodSourceName) {
        int index = methodSourceName.indexOf('#');
        return index > 0 ? methodSourceName.substring(0, index).replace('$', '.') : null;
    }

    //the method part of Class#method or #method, without the parameter types, i.e., factory(int)
    private static String providerMethodName(String methodSourceName) {
        int index = methodSourceName.indexOf('#');
        String methodName = index != -1 ? methodSourceName.substring(index + 1) : methodSourceName;
        int parenthesis = methodName.indexOf('(');
        return parenthesis != -1 ? methodName.substring(0, parenthesis) : methodName;
    }

    private static boolean declares(UMLAbstractClass declaringClass, String className) {
        return declaringClass.getName().equals(className) || declaringClass.getName().endsWith("." + className);
    }

    public static boolean isMethodSourceAnnotation(UMLAnnotation annotation) {
        return annotation.getTypeName().equals("MethodSource");
    }

    public UMLOperation getResolvedProviderMethod() {
        return resolvedProviderMethod;
    }

    private void processMethodSourceName(UMLOperation operation, String methodSourceName) {
        String providerClassName = providerClassName(methodSourceName);
        String providerMethodName = providerMethodName(methodSourceName);
        List<UMLOperation> sameNameMethods = new ArrayList<UMLOperation>();
        for(UMLAbstractClass declaringClass : this.declaringClasses) {
            if(providerClassName != null && !declares(declaringClass, providerClassName)) {
                continue;
            }
            sameNameMethods.addAll(declaringClass.getOperations().stream().filter(op -> op.getName().equals(providerMethodName)).collect(Collectors.toList()));
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
        }
        if(sameNameMethods.size() > 0) {
            UMLOperation sourceMethod = sameNameMethods.get(0);
            if(this.resolvedProviderMethod == null) {
                this.resolvedProviderMethod = sourceMethod;
            }
            Optional<VariableDeclaration> returnedVarCandidates = sourceMethod.getBody().getAllVariableDeclarations().stream().filter(v -> sourceMethod.getReturnParameter().getType().equals(v.getType())).findAny();
            VariableDeclaration returnedVariable = returnedVariable(sourceMethod);
            if(returnedVariable != null) {
                addRowsOfFilledVariable(sourceMethod, returnedVariable);
            }
            else if (returnedVarCandidates.isEmpty()) {
                Optional<StatementObject> stmtCandidate = sourceMethod.getBody().getCompositeStatement().getStatements().stream()
                        .filter(s -> s instanceof StatementObject)
                        .map(s -> (StatementObject) s)
                        .filter(AbstractCodeFragment::isLastStatement)
                        .findAny();
                if (stmtCandidate.isPresent()) {
                    AbstractCall call = stmtCandidate.get().invocationCoveringEntireFragment();
                    if(call != null && call.getName().equals("of")) {
                        StatementObject statement = stmtCandidate.get();
                        List<AbstractCall> rowCallCandidates = rowCallCandidates(statement);
                        for(AbstractCall nestedCall : rowCallCandidates) {
                            boolean nestedInAnotherRowCall = rowCallCandidates.stream()
                                    .anyMatch(other -> other != nestedCall && other.getLocationInfo().subsumes(nestedCall.getLocationInfo()));
                            if(!nestedInAnotherRowCall) {
                                addRow(nestedCall.arguments(), nestedCall.getLocationInfo(), statement, new HashSet<>());
                            }
                        }
                        //a row of one value is not created by a row call, i.e., Stream.of(new JacksonJsonProvider(), new GsonJsonProvider()) or Stream.of(Locale.forLanguageTag("ar-EG"), ...)
                        if(rowCallCandidates.isEmpty()) {
                            Set<LocationInfo> claimedLocations = new HashSet<>();
                            for(String arg : call.arguments()) {
                                addRow(List.of(arg), call.getLocationInfo(), statement, claimedLocations);
                            }
                        }
                    }
                }
            }
        }
    }

    //the local variable returned by the last statement of the provider, i.e., return argsList;
    private static VariableDeclaration returnedVariable(UMLOperation provider) {
        for(AbstractCodeFragment statement : provider.getBody().getCompositeStatement().getStatements()) {
            if(statement instanceof StatementObject && statement.isLastStatement() && statement.getVariables().size() == 1 &&
                    statement.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                String name = statement.getVariables().get(0).getString();
                if(statement.getString().trim().equals("return " + name + ";")) {
                    return provider.getVariableDeclaration(name);
                }
            }
        }
        return null;
    }

    //a row for each value added to the returned variable, i.e., argsList.add(Linq.singleton(0), isEvenFunc, true) or list.add(Arguments.of("a", 1))
    private void addRowsOfFilledVariable(UMLOperation provider, VariableDeclaration returnedVariable) {
        for(AbstractCodeFragment leaf : provider.getBody().getCompositeStatement().getLeaves()) {
            if(!(leaf instanceof StatementObject statement)) {
                continue;
            }
            AbstractCall call = statement.invocationCoveringEntireFragment();
            if(call != null && call.getName().equals("add") && call.getExpression() != null && call.getExpression().equals(returnedVariable.getVariableName())) {
                //only the rows of JUnit are unwrapped, not a value created by a call named of, i.e., argsList.add(Linq.of(new int[]{1})) is a row of one value
                List<AbstractCall> rowCalls = rowCallCandidates(statement).stream().filter(MethodSourceAnnotation::isArgumentsCall).toList();
                if(call.arguments().size() == 1 && rowCalls.size() == 1 && rowCalls.get(0).actualString().equals(call.arguments().get(0))) {
                    addRow(rowCalls.get(0).arguments(), rowCalls.get(0).getLocationInfo(), statement, new HashSet<>());
                }
                else {
                    addRow(call.arguments(), call.getLocationInfo(), statement, new HashSet<>());
                }
            }
        }
    }

    //Arguments.of(...), Arguments.arguments(...) or arguments(...) creating a row of JUnit
    private static boolean isArgumentsCall(AbstractCall call) {
        return (call.getName().equals("of") || call.getName().equals("arguments")) && (call.getExpression() == null || call.getExpression().equals("Arguments") || call.getExpression().endsWith(".Arguments"));
    }

    //the values of a row created within rowLocation, with the leaf expressions not claimed by the previous values
    private void addRow(List<String> arguments, LocationInfo rowLocation, StatementObject statement, Set<LocationInfo> claimedLocations) {
        List<String> resolvedArguments = new ArrayList<>();
        List<LeafExpression> leafExpressions = new ArrayList<>();
        List<LocationInfo> resolvedArgumentSpans = new ArrayList<>();
        for(String arg : arguments) {
            LeafExpression constantLiteral = resolveConstantLiteral(arg);
            if(constantLiteral != null) {
                resolvedArguments.add(constantLiteral.getString());
                leafExpressions.add(constantLiteral);
                resolvedArgumentSpans.add(constantLiteral.getLocationInfo());
            }
            else {
                resolvedArguments.add(arg);
                List<LeafExpression> matches = statement.findExpression(arg);
                for(LeafExpression match : matches) {
                    boolean nestedInEarlierArgument = resolvedArgumentSpans.stream().anyMatch(span -> span.subsumes(match.getLocationInfo()));
                    if(rowLocation.subsumes(match.getLocationInfo()) && !claimedLocations.contains(match.getLocationInfo()) && !nestedInEarlierArgument) {
                        leafExpressions.add(match);
                        claimedLocations.add(match.getLocationInfo());
                        resolvedArgumentSpans.add(match.getLocationInfo());
                        break;
                    }
                }
            }
        }
        testParameters.add(resolvedArguments);
        testParameterLeafExpressions.add(leafExpressions);
    }

    private LeafExpression resolveConstantLiteral(String name) {
        for(UMLAbstractClass declaringClass : declaringClasses) {
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
                for(UMLAbstractClass declaringClass : declaringClasses) {
                    for(UMLAttribute attribute : declaringClass.getAttributes()) {
                        if(value.getString().equals(attribute.getName()) && attribute.getVariableDeclaration().getInitializer() !=  null) {
                            values.addAll(extractLiteralFromValue(attribute.getVariableDeclaration().getInitializer()));
                        }
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
            List<String> values = new ArrayList<>();
            for(LeafExpression literal : expr.getStringLiterals()) {
                values.add(literal.getString().replace("\"", ""));
            }
            return values;
        }
        return Collections.emptyList();
    }
}
