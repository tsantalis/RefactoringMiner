package sortpom;

import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import sortpom.exception.FailureException;
import sortpom.jdomcontent.NewlineText;
import sortpom.parameter.LineSeparatorUtil;
import sortpom.parameter.PluginParameters;
import sortpom.util.StringLineSeparatorWriter;

import java.io.IOException;
import java.io.Writer;

/**
 * Handles all generation of xml.
 */
public class XmlOutputGenerator {
    private String encoding;
    private LineSeparatorUtil lineSeparatorUtil;
    private String indentCharacters;
    private boolean expandEmptyElements;
    private boolean indentBlankLines;

    /** Setup default configuration */
    public void setup(PluginParameters pluginParameters) {
        this.indentCharacters = pluginParameters.indentCharacters;
        this.lineSeparatorUtil = pluginParameters.lineSeparatorUtil;
        this.encoding = pluginParameters.encoding;
        this.expandEmptyElements = pluginParameters.expandEmptyElements;
        this.indentBlankLines = pluginParameters.indentBlankLines;
    }

    /**
     * Returns the sorted xml as an OutputStream.
     *
     * @return the sorted xml
     */
    public String getSortedXml(Document newDocument) {
        try (StringLineSeparatorWriter writer = new StringLineSeparatorWriter(lineSeparatorUtil.toString())) {

            XMLOutputter xmlOutputter = new PatchedXMLOutputter(writer, indentBlankLines);
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
        private final StringLineSeparatorWriter stringLineSeparatorWriter;
        private final boolean indentBlankLines;

        PatchedXMLOutputter(StringLineSeparatorWriter stringLineSeparatorWriter, boolean indentBlankLines) {
            this.stringLineSeparatorWriter = stringLineSeparatorWriter;
            this.indentBlankLines = indentBlankLines;
            XMLOutputter.preserveFormat.setLineSeparator("\n");
        }

        /** Stop XMLOutputter from printing comment <!-- --> chars if it is just a newline */
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
            stringLineSeparatorWriter.clearLineBuffer();
        }
    }
}
