package sortpom.util;

import sortpom.parameter.LineSeparatorUtil;
import sortpom.parameter.PluginParameters;

import java.io.StringWriter;

/**
 * Creates a composed writer for XmlOutput
 */
public class WriterFactory {
    private LineSeparatorUtil lineSeparatorUtil;
    private boolean expandEmptyElements;
    private boolean spaceBeforeCloseEmptyElement;

    /**
     * Setup default configuration
     */
    public void setup(PluginParameters pluginParameters) {
        this.lineSeparatorUtil = pluginParameters.lineSeparatorUtil;
        this.expandEmptyElements = pluginParameters.expandEmptyElements;
        spaceBeforeCloseEmptyElement = pluginParameters.spaceBeforeCloseEmptyElement;
    }

    public XmlWriter getWriter() {
        StringWriter out = new StringWriter();
        StringLineSeparatorWriter stringLineSeparatorWriter = new StringLineSeparatorWriter(out, lineSeparatorUtil.toString());
        if (expandEmptyElements || spaceBeforeCloseEmptyElement) {
            return stringLineSeparatorWriter;
        }
        return new NoSpaceBeforeCloseWriter(stringLineSeparatorWriter);
    }
}
