package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.annotation.source.EmptySourceAnnotation;

import java.util.Collections;
import java.util.List;

public class NullAndEmptySourceAnnotation extends EmptySourceAnnotation {
    public static final String ANNOTATION_TYPENAME = "NullAndEmptySource";

    public NullAndEmptySourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        super(annotation, operation, model, ANNOTATION_TYPENAME);
        testParameters.add(Collections.singletonList("null"));
    }

    @Override
    public List<List<String>> getTestParameters() {
        return testParameters;
    }
}
