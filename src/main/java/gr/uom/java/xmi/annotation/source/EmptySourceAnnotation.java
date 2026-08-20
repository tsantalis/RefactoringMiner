package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.annotation.MarkerAnnotation;
import gr.uom.java.xmi.decomposition.LeafExpression;

import java.util.Collections;

public class EmptySourceAnnotation extends SourceAnnotation implements MarkerAnnotation {
    public static final String ANNOTATION_TYPENAME = "EmptySource";

    public EmptySourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass declaringClass) {
        this(annotation, operation, declaringClass, ANNOTATION_TYPENAME);
    }

    public EmptySourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass declaringClass, String typeName) {
        super(annotation, typeName);
        String sentinel;
        switch (operation.getParametersWithoutReturnType().get(0).getType().toQualifiedString()) {
            case "List":
            case "Collection":
            case "Set":
            case "Map":
                sentinel = "{}";
                break;
            case "Boolean":
            case "boolean":
                sentinel = "false";
                break;
            case "String":
                sentinel = "";
                break;
            case "Long":
            case "long":
            case "Short":
            case "short":
            case "Integer":
            case "int":
            case "Byte":
            case "byte":
            case "Double":
            case "double":
            case "Float":
            case "float":
                sentinel = "0";
                break;
            default:
                sentinel = null;
        }
        if (sentinel != null) {
            testParameters.add(Collections.singletonList(sentinel));
            testParameterLeafExpressions.add(Collections.singletonList(new LeafExpression(sentinel, annotation.getLocationInfo())));
        }
    }
}
