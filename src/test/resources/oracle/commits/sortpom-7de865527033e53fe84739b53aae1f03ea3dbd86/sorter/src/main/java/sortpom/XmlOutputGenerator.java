package sortpom;

import org.jdom.Attribute;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import sortpom.exception.FailureException;
import sortpom.jdomcontent.NewlineText;
import sortpom.parameter.PluginParameters;
import sortpom.util.WriterFactory;
import sortpom.util.XmlWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Handles all generation of xml.
 */
public class XmlOutputGenerator {
    private String encoding;
    private String indentCharacters;
    private boolean expandEmptyElements;
    private boolean indentBlankLines;
    private boolean indentSchemaLocation;
    private final WriterFactory writerFactory = new WriterFactory();

    /**
     * Setup default configuration
     */
    public void setup(PluginParameters pluginParameters) {
        writerFactory.setup(pluginParameters);
        this.indentCharacters = pluginParameters.indentCharacters;
        this.encoding = pluginParameters.encoding;
        this.expandEmptyElements = pluginParameters.expandEmptyElements;
        this.indentBlankLines = pluginParameters.indentBlankLines;
        this.indentSchemaLocation = pluginParameters.indentSchemaLocation;
    }

    /**
     * Returns the sorted xml as an OutputStream.
     *
     * @return the sorted xml
     */
    public String getSortedXml(Document newDocument) {
        try (XmlWriter writer = writerFactory.getWriter()) {

            XMLOutputter xmlOutputter = new PatchedXMLOutputter(writer, indentBlankLines, indentSchemaLocation);
            xmlOutputter.setFormat(createPrettyFormat());
            xmlOutputter.output(newDocument, writer);

            return writer.toString();
        } catch (IOException ioex) {
            throw new FailureException("Could not format pom files content", ioex);
        }
    }

    private Format createPrettyFormat() {
        final Format prettyFormat = Format.getPrettyFormat();
        prettyFormat.setExpandEmptyElements(expandEmptyElements);
        prettyFormat.setEncoding(encoding);
        prettyFormat.setLineSeparator("\n");
        prettyFormat.setIndent(indentCharacters);
        return prettyFormat;
    }

    private static class PatchedXMLOutputter extends XMLOutputter {
        private final XmlWriter writer;
        private final boolean indentBlankLines;
        private final boolean indentSchemaLocation;

        PatchedXMLOutputter(XmlWriter writer, boolean indentBlankLines, boolean indentSchemaLocation) {
            this.writer = writer;
            this.indentBlankLines = indentBlankLines;
            this.indentSchemaLocation = indentSchemaLocation;
            XMLOutputter.preserveFormat.setLineSeparator("\n");
        }

        /**
         * Stop XMLOutputter from printing comment <!-- --> chars if it is just a newline
         */
        @Override
        protected void printComment(Writer stringWriter, Comment comment) throws IOException {
            if (comment instanceof NewlineText) {
                if (!indentBlankLines) {
                    clearIndentationForCurrentLine(stringWriter);
                }
            } else {
                super.printComment(stringWriter, comment);
            }
        }

        private void clearIndentationForCurrentLine(Writer stringWriter) throws IOException {
            // Force all xml lines to be written to stream (via the writer)
            stringWriter.flush();

            // Remove all inset that has just been written since last newline
            writer.clearLineBuffer();
        }

        @Override
        protected void printAttributes(Writer out, List attributes, Element parent, NamespaceStack namespaces) throws IOException {
            if (indentSchemaLocation && attributes.size() == 1) {
                Object attributeObject = attributes.get(0);
                if (attributeObject instanceof Attribute && "schemaLocation".equals(((Attribute) attributeObject).getName())) {
                    out.write(currentFormat.getLineSeparator());
                    out.write(currentFormat.getIndent());
                    out.write(currentFormat.getIndent());
                }
            }
            super.printAttributes(out, attributes, parent, namespaces);
        }
    }
}
