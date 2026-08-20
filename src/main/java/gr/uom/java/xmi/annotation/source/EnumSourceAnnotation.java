package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.annotation.MarkerAnnotation;
import gr.uom.java.xmi.annotation.NormalAnnotation;
import gr.uom.java.xmi.annotation.SingleMemberAnnotation;

import java.util.Collections;
import java.util.List;

public class EnumSourceAnnotation extends SourceAnnotation implements SingleMemberAnnotation, MarkerAnnotation, NormalAnnotation {
    public static final String ANNOTATION_TYPENAME = "EnumSource";

    public EnumSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass enumClassDeclaration) {
        super(annotation, ANNOTATION_TYPENAME);
        if (annotation.isNormalAnnotation() && annotation.getMemberValuePairs().containsKey("names")) {
            if (annotation.getMemberValuePairs().containsKey("mode")) {
                String mode = annotation.getMemberValuePairs().get("mode").getString();
                if ((mode.equals("EXCLUDE") || mode.equals("Mode.EXCLUDE") || mode.equals("EnumSource.Mode.EXCLUDE")) && enumClassDeclaration != null) {
                    for (UMLEnumConstant constant : enumClassDeclaration.getEnumConstants()) {
                        if (!isExcluded(annotation, constant)) {
                            addConstantRow(constant);
                        }
                    }
                } else {
                    addIncludedNameRows(annotation);
                }
            } else {
                addIncludedNameRows(annotation);
            }
        } else if (enumClassDeclaration != null) {
            for (UMLEnumConstant constant : enumClassDeclaration.getEnumConstants()) {
                addConstantRow(constant);
            }
        }
    }

    private static boolean isExcluded(UMLAnnotation annotation, UMLEnumConstant constant) {
        return annotation.getMemberValuePairs().get("names").getStringLiterals().contains(constant.getName());
    }

    private void addConstantRow(UMLEnumConstant constant) {
        testParameters.add(Collections.singletonList(sanitizeLiteral(constant.getName())));
        testParameterLeafExpressions.add(Collections.singletonList(new LeafExpression(sanitizeLiteral(constant.getName()), constant.getLocationInfo())));
    }

    private void addIncludedNameRows(UMLAnnotation annotation) {
        for (LeafExpression expression : annotation.getMemberValuePairs().get("names").getStringLiterals()) {
            testParameters.add(Collections.singletonList(sanitizeLiteral(expression.getString())));
            testParameterLeafExpressions.add(Collections.singletonList(expression));
        }
    }

    @Override
    public List<String> getValue() {
        assert !annotation.isMarkerAnnotation() : "getValue() is not supported by marker EnumSource annotation";
        AbstractExpression value = annotation.isSingleMemberAnnotation() ? annotation.getValue() : annotation.getMemberValuePairs().get("value");
        List<LeafExpression> typeLiterals = value.getTypeLiterals();
        assert typeLiterals.size() == 1;
        return Collections.singletonList(typeLiterals.get(0).getString());
    }
}
