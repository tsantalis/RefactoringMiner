package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.util.CsvUtils;
import gr.uom.java.xmi.annotation.NormalAnnotation;
import gr.uom.java.xmi.annotation.SingleMemberAnnotation;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvFileSourceAnnotation extends SourceAnnotation implements NormalAnnotation, SingleMemberAnnotation {
    public static final String ANNOTATION_TYPENAME = "CsvFileSource";

    public CsvFileSourceAnnotation(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        // TODO: Add non-java files to UMLModel + Use model to get CSV based on relative path
        super(annotation, ANNOTATION_TYPENAME);
        testParameters = CsvUtils.extractParametersFromCsvFile(getValue());
    }

    @Override
    public List<List<String>> getTestParameters() {
        return testParameters;
    }

    @Override
    public List<String> getValue() {
        Stream st = Stream.empty();
        Stream nextFileContent;
        if (annotation.isSingleMemberAnnotation()) {
            try {
                for (LeafExpression expression : annotation.getValue().getStringLiterals()) {
                    nextFileContent = CsvUtils.readLinesOfCsvFile(expression.getString()).stream();
                    st = Stream.concat(st, nextFileContent);
                }
                return (List<String>) st.collect(Collectors.toList());
            } catch (IOException e) {
                return Collections.emptyList();
            }
        } else if (annotation.isNormalAnnotation()) {
            Map<String, AbstractExpression> parameters = annotation.getMemberValuePairs();
            if (parameters.containsKey("files")) {
                try {
                    for (LeafExpression expression : parameters.get("files").getStringLiterals()) {
                        String literal = expression.getString();
                        literal = literal.startsWith("\"") ? literal.substring(1) : literal;
                        literal = literal.endsWith("\"") ? literal.substring(0, literal.length() - 1) : literal;
                        nextFileContent = CsvUtils.readLinesOfCsvFile(literal.strip()).stream();
                        st = Stream.concat(st, nextFileContent);
                    }
                    return (List<String>) st.collect(Collectors.toList());
                } catch (IOException e) {
                    e.printStackTrace();
                    return Collections.emptyList();
                }
            } else if (parameters.containsKey("resources")) {
                try {
                    for (LeafExpression expression : parameters.get("resources").getStringLiterals()) {
                        String filePath = expression.getString();
                        final String resPath = "/src/test/resources";
                        filePath = filePath.startsWith("/") ? resPath + filePath : resPath + "/" + filePath;
                        nextFileContent = CsvUtils.readLinesOfCsvFile(filePath).stream();
                        st = Stream.concat(st, nextFileContent);
                    }
                    return (List<String>) st.collect(Collectors.toList());
                } catch (IOException e) {
                    return Collections.emptyList();
                }
            } else {
                throw new IllegalArgumentException("@CsvFileSource normal annotation should have a value or resources parameter");
            }
        }
        throw new IllegalArgumentException("@CsvFileSource should be a normal or single member annotation");
    }
}
