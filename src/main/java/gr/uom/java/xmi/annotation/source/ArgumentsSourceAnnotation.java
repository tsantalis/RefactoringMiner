package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.annotation.SingleMemberAnnotation;

import java.util.List;

class ArgumentsSourceAnnotation extends SourceAnnotation implements SingleMemberAnnotation {
    public ArgumentsSourceAnnotation(UMLAnnotation annotation) {
        super(annotation, "ArgumentsSource");
    }

    @Override
    public List<String> getValue() {
        return null;
    }

    @Override
    public List<List<String>> getTestParameters() {
        return null;
    }
}
