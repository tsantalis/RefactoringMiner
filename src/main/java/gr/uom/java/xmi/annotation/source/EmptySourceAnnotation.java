package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.annotation.MarkerAnnotation;

import java.util.Collections;
import java.util.List;

public class EmptySourceAnnotation extends SourceAnnotation implements MarkerAnnotation {
    public static final String ANNOTATION_TYPENAME = "EmptySource";

    public EmptySourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        this(annotation, operation, model, ANNOTATION_TYPENAME);
    }

    public EmptySourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLModel model, String typeName) {
        super(annotation, typeName);
        switch (operation.getParametersWithoutReturnType().get(0).getType().toQualifiedString()) {
            case "List":
            case "Collection":
            case "Set":
            case "Map":
                testParameters.add(Collections.singletonList("{}"));
                break;
            case "Boolean":
            case "boolean":
                testParameters.add(Collections.singletonList("false"));
                break;
            case "String":
                testParameters.add(Collections.singletonList(""));
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
                testParameters.add(Collections.singletonList("0"));
                break;
        }
    }

    @Override
    public List<List<String>> getTestParameters() {
        return testParameters;
    }
}
