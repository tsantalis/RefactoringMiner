package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.annotation.NormalAnnotation;

import java.util.*;

public class ValueSourceAnnotation extends SourceAnnotation implements NormalAnnotation {
    public static final String ANNOTATION_TYPENAME = "ValueSource";
    private final Map<String, AbstractExpression> memberValuePairs;

    private Set<String> numberKeys = Set.of(
            "bytes",
            "doubles",
            "floats",
            "ints",
            "longs",
            "shorts"
    );
    private Set<String> stringKeys = Set.of(
            "strings",
            "chars"
    );

    private List<LeafExpression> extractLiterals(AbstractExpression annotationParameterValue, String memberKey) {
        List<LeafExpression> baseLiterals;
        if (numberKeys.contains(memberKey)) {
            baseLiterals = annotationParameterValue.getNumberLiterals();
        } else if (stringKeys.contains(memberKey)) {
            baseLiterals = annotationParameterValue.getStringLiterals();
        } else if (memberKey.equals("booleans")) {
            baseLiterals = annotationParameterValue.getBooleanLiterals();
        } else if (memberKey.equals("classes")) {
            baseLiterals = annotationParameterValue.getTypeLiterals();
        } else {
            return Collections.emptyList();
        }
        List<LeafExpression> infixExpressions = annotationParameterValue.getInfixExpressions();
        if (infixExpressions.isEmpty()) {
            return baseLiterals;
        }
        List<LeafExpression> combined = new ArrayList<>();
        for (LeafExpression literal : baseLiterals) {
            boolean subsumedByInfix = infixExpressions.stream()
                    .anyMatch(infix -> infix.getLocationInfo().subsumes(literal.getLocationInfo())
                            && !infix.getLocationInfo().equals(literal.getLocationInfo()));
            if (!subsumedByInfix) {
                combined.add(literal);
            }
        }
        combined.addAll(infixExpressions);
        combined.sort(Comparator.comparingInt(le -> le.getLocationInfo().getStartOffset()));
        return combined;
    }

    public ValueSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass declaringClass) {
        super(annotation, ANNOTATION_TYPENAME);
        memberValuePairs = annotation.getMemberValuePairs();
        Set<String> providedKeys = memberValuePairs.keySet();
        for (String key : providedKeys) {
            AbstractExpression annotationParameterValue = memberValuePairs.get(key);
            List<LeafExpression> literals = extractLiterals(annotationParameterValue, key);
            boolean numeric = numberKeys.contains(key);
            for (LeafExpression literal : literals) {
                String value = sanitizeLiteral(literal.getString());
                if (numeric) {
                    value = stripNumericSuffix(value);
                }
                testParameters.add(Collections.singletonList(value));
                testParameterLeafExpressions.add(Collections.singletonList(literal));
            }
        }

    }
}
