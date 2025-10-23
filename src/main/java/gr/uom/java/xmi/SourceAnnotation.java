package gr.uom.java.xmi;

import gr.uom.java.xmi.annotation.source.*;
import gr.uom.java.xmi.decomposition.LeafExpression;

import org.apache.commons.lang3.function.TriFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class SourceAnnotation {
    protected static final Map<String, TriFunction<UMLAnnotation, UMLOperation, UMLAbstractClass, SourceAnnotation>> implementations = Map.of(
            CsvSourceAnnotation.ANNOTATION_TYPENAME, CsvSourceAnnotation::new,
            CsvFileSourceAnnotation.ANNOTATION_TYPENAME, CsvFileSourceAnnotation::new,
            ValueSourceAnnotation.ANNOTATION_TYPENAME, ValueSourceAnnotation::new,
            EnumSourceAnnotation.ANNOTATION_TYPENAME, EnumSourceAnnotation::new,
            NullAndEmptySourceAnnotation.ANNOTATION_TYPENAME, NullAndEmptySourceAnnotation::new,
            EmptySourceAnnotation.ANNOTATION_TYPENAME, EmptySourceAnnotation::new,
            NullSourceAnnotation.ANNOTATION_TYPENAME, NullSourceAnnotation::new,
            MethodSourceAnnotation.ANNOTATION_TYPENAME, MethodSourceAnnotation::new
    );
    protected List<List<String>> testParameters;
    protected List<List<LeafExpression>> testParameterLeafExpressions;
    protected UMLAnnotation annotation;

    protected SourceAnnotation(UMLAnnotation annotation, String typeName) {
        assert annotation.getTypeName().equals(typeName) : "Annotation is not a " + typeName + " annotation";
        this.annotation = annotation;
        this.testParameters = new ArrayList<>();
        this.testParameterLeafExpressions = new ArrayList<>();
    }

    public static SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLAbstractClass declaringClass) {
        if (implementations.containsKey(annotation.getTypeName())) {
            return implementations.get(annotation.getTypeName()).apply(annotation, operation, declaringClass);
        }
        throw new IllegalArgumentException("Annotation type " + annotation.getTypeName() + " is not supported");
    }

    public static String sanitizeLiteral(String literal) {
        literal = literal.startsWith("\"") ? literal.substring(1) : literal;
        literal = literal.endsWith("\"") ? literal.substring(0, literal.length() - 1) : literal;
        literal = literal.endsWith(".class") ? literal.substring(0, literal.length() - 6) : literal;
        return literal;
    }

    public List<List<String>> getTestParameters() {
        return testParameters;
    }

    public List<List<LeafExpression>> getTestParameterLeafExpressions() {
        return testParameterLeafExpressions;
    }
}
