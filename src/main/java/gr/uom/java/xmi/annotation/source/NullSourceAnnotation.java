package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.annotation.MarkerAnnotation;

import java.util.Collections;
import java.util.List;

public class NullSourceAnnotation extends SourceAnnotation implements MarkerAnnotation {
    public static final String ANNOTATION_TYPENAME = "NullSource";

    public NullSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass declaringClass) {
        super(annotation, ANNOTATION_TYPENAME);
    }

    @Override
    public List<List<String>> getTestParameters() {
        return Collections.singletonList(Collections.singletonList("null"));
    }
}
