package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.util.CsvUtils;
import gr.uom.java.xmi.annotation.NormalAnnotation;
import gr.uom.java.xmi.annotation.SingleMemberAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvSourceAnnotation extends SourceAnnotation implements NormalAnnotation, SingleMemberAnnotation {
    public static final String ANNOTATION_TYPENAME = "CsvSource";

    public CsvSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass declaringClass) {
        super(annotation, ANNOTATION_TYPENAME);
        for (String csvParams : getValue()) {
            List<String> parameters = CsvUtils.extractParametersFromCsv(csvParams);
            testParameters.add(parameters);
        }
    }

    @Override
    public List<String> getValue() {
        List<String> result = new ArrayList<>();
        if (annotation.isSingleMemberAnnotation()) {
            for (LeafExpression literal : annotation.getValue().getStringLiterals()) {
                result.add(literal.getString());
                testParameterLeafExpressions.add(List.of(literal));
            }
            return result;
        } else if (annotation.isNormalAnnotation()) {
            Map<String, AbstractExpression> parameters = annotation.getMemberValuePairs();
            if (parameters.containsKey("value")) {
                // Value is a list of string literals as expected
                for (LeafExpression literal : parameters.get("value").getStringLiterals()) {
                    result.add(literal.getString());
                    testParameterLeafExpressions.add(List.of(literal));
                }
            } else if (parameters.containsKey("textBlock")) {
                List<LeafExpression> textBlock = parameters.get("textBlock").getStringLiterals();
                if (textBlock.size() == 1) {
                    // Text block is a single multi-line string literal as expected
                    for (String line : textBlock.get(0).getString().split("[\\r\\n]+")) {
                        result.add(line);
                    }
                    testParameterLeafExpressions.add(List.of(textBlock.get(0)));
                } else if (textBlock.size() > 1) {
                    // Text block contains multiple string literals concatenated
                    for (LeafExpression literal : textBlock) {
                        result.add(literal.getString());
                        testParameterLeafExpressions.add(List.of(literal));
                    }
                } else {
                    throw new IllegalArgumentException("@CsvSource text block should not be empty");
                }
            } else {
                throw new IllegalArgumentException("@CsvSource normal annotation should have a value or textBlock parameter");
            }
        }
        return result;
    }

    public List<List<LeafExpression>> getTestParameterLeafExpressions() {
        return testParameterLeafExpressions;
    }
}
