package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.annotation.NormalAnnotation;

import java.util.*;

public class ValueSourceAnnotation extends SourceAnnotation implements NormalAnnotation {
    public static final String ANNOTATION_TYPENAME = "ValueSource";
    private final Map<String, AbstractExpression> memberValuePairs;
    private final List<List<String>> testParameters;
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
        if (numberKeys.contains(memberKey)) {
            return annotationParameterValue.getNumberLiterals();
        } else if (stringKeys.contains(memberKey)) {
            return annotationParameterValue.getStringLiterals();
        } else if (memberKey.equals("booleans")) {
            return annotationParameterValue.getBooleanLiterals();
        } else if (memberKey.equals("classes")) {
            return annotationParameterValue.getTypeLiterals();
        }
        return Collections.emptyList();
    }

    public ValueSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        super(annotation, ANNOTATION_TYPENAME);
        memberValuePairs = annotation.getMemberValuePairs();
        Set<String> providedKeys = memberValuePairs.keySet();
        testParameters = new ArrayList<>();
        for (String key : providedKeys) {
            AbstractExpression annotationParameterValue = memberValuePairs.get(key);
            for (LeafExpression literal : extractLiterals(annotationParameterValue, key)) {
                testParameters.add(Collections.singletonList(sanitizeLiteral(literal.getString())));
            }
        }

    }

    @Override
    public List<List<String>> getTestParameters() {
        return testParameters;
    }
}
