package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.LeafExpression;

import java.util.Collections;
import java.util.List;

public class NullAndEmptySourceAnnotation extends EmptySourceAnnotation {
    public static final String ANNOTATION_TYPENAME = "NullAndEmptySource";

    public NullAndEmptySourceAnnotation(UMLAnnotation annotation, UMLOperation operation, List<UMLAbstractClass> declaringClasses) {
        super(annotation, operation, declaringClasses, ANNOTATION_TYPENAME);
        testParameters.add(Collections.singletonList("null"));
        testParameterLeafExpressions.add(Collections.singletonList(new LeafExpression("null", annotation.getLocationInfo())));
    }
}
