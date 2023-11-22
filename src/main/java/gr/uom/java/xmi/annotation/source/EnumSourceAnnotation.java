package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.annotation.MarkerAnnotation;
import gr.uom.java.xmi.annotation.NormalAnnotation;
import gr.uom.java.xmi.annotation.SingleMemberAnnotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnumSourceAnnotation extends SourceAnnotation implements SingleMemberAnnotation, MarkerAnnotation, NormalAnnotation {
    public static final String ANNOTATION_TYPENAME = "EnumSource";
    private List<List<String>> testParameters = new ArrayList<>();

    public EnumSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        super(annotation, ANNOTATION_TYPENAME);
        String enumClassLiteral;
        if (annotation.isMarkerAnnotation()) {
            enumClassLiteral = sanitizeLiteral(getFirstParameterType(operation));
        } else {
            enumClassLiteral = sanitizeLiteral(getValue().get(0));
        }
        UMLClass enumClassDeclaration = findEnumDeclaration(model, enumClassLiteral);
        if (annotation.isNormalAnnotation() && annotation.getMemberValuePairs().containsKey("names")) {
            if (annotation.getMemberValuePairs().containsKey("mode")) {
                String mode = annotation.getMemberValuePairs().get("mode").getString();
                if ((mode.equals("EXCLUDE") || mode.equals("Mode.EXCLUDE") || mode.equals("EnumSource.Mode.EXCLUDE")) && enumClassDeclaration != null) {
                    for (UMLEnumConstant constant : enumClassDeclaration.getEnumConstants()) {
                        if (!isExcluded(annotation, constant)) {
                            testParameters.add(Collections.singletonList(sanitizeLiteral(constant.getName())));
                        }
                    }
                } else {
                    testParameters.addAll(extractIncludedNames(annotation));
                }
            } else {
                testParameters.addAll(extractIncludedNames(annotation));
            }
        } else if (enumClassDeclaration != null) {
            for (UMLEnumConstant constant : enumClassDeclaration.getEnumConstants()) {
                testParameters.add(Collections.singletonList(sanitizeLiteral(constant.getName())));
            }
        }
    }

    private static boolean isExcluded(UMLAnnotation annotation, UMLEnumConstant constant) {
        return annotation.getMemberValuePairs().get("names").getStringLiterals().contains(constant.getName());
    }

    private static List<List<String>> extractIncludedNames(UMLAnnotation annotation) {
        List<List<String>> includedValues = new ArrayList<>();
        for (LeafExpression expression : annotation.getMemberValuePairs().get("names").getStringLiterals()) {
            includedValues.add(Collections.singletonList(sanitizeLiteral(expression.getString())));
        }
        return includedValues;
    }

    private static UMLClass findEnumDeclaration(UMLModel model, String enumClassLiteral) {
        UMLClass enumClassDeclaration = null;
        for (UMLClass aClass : model.getClassList()) {
            if (aClass.getName().contains(enumClassLiteral)) {
                enumClassDeclaration = aClass;
            }
        }
        return enumClassDeclaration;
    }

    private static String getFirstParameterType(UMLOperation operation) {
        return operation.getParametersWithoutReturnType().get(0).getType().getClassType();
    }

    @Override
    public List<String> getValue() {
        assert !annotation.isMarkerAnnotation() : "getValue() is not supported by marker EnumSource annotation";
        AbstractExpression value = annotation.isSingleMemberAnnotation() ? annotation.getValue() : annotation.getMemberValuePairs().get("value");
        List<LeafExpression> typeLiterals = value.getTypeLiterals();
        assert typeLiterals.size() == 1;
        return Collections.singletonList(typeLiterals.get(0).getString());
    }

    @Override
    public List<List<String>> getTestParameters() {
        return testParameters;
    }
}
