package sortpom.util;

import org.apache.commons.io.IOUtils;
import org.jdom.Content;
import org.jdom.Element;
import refutils.ReflectionHelper;
import sortpom.XmlOutputGenerator;
import sortpom.XmlProcessor;
import sortpom.parameter.PluginParameters;
import sortpom.wrapper.WrapperFactoryImpl;
import sortpom.wrapper.content.AlphabeticalSortedWrapper;
import sortpom.wrapper.content.UnsortedWrapper;
import sortpom.wrapper.content.Wrapper;
import sortpom.wrapper.operation.HierarchyRootWrapper;
import sortpom.wrapper.operation.WrapperFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test utility
 */
public class XmlProcessorTestUtil {
    private boolean sortAlphabeticalOnly = false;
    private boolean keepBlankLines = false;
    private boolean indentBlankLines = false;
    private String predefinedSortOrder = "default_0_4_0";
    private boolean expandEmptyElements = true;
    private String lineSeparator = "\r\n";

    private XmlProcessor xmlProcessor;
    private XmlOutputGenerator xmlOutputGenerator;


    public static XmlProcessorTestUtil create() {
        return new XmlProcessorTestUtil();
    }

    private XmlProcessorTestUtil() {
    }

    public void testInputAndExpected(final String inputFileName, final String expectedFileName) throws Exception {
        String actual = sortXmlAndReturnResult(inputFileName);

        final String expected = IOUtils.toString(new FileInputStream(expectedFileName), StandardCharsets.UTF_8);

        assertEquals(expected, actual);
    }

    public String sortXmlAndReturnResult(String inputFileName) throws Exception {
        setup(inputFileName);
        xmlProcessor.sortXml();
        return xmlOutputGenerator.getSortedXml(xmlProcessor.getNewDocument());
    }

    public void testVerifyXmlIsOrdered(final String inputFileName) throws Exception {
        setup(inputFileName);
        xmlProcessor.sortXml();
        assertTrue(xmlProcessor.isXmlOrdered().isOrdered());
    }

    public void testVerifyXmlIsNotOrdered(final String inputFileName, String infoMessage) throws Exception {
        setup(inputFileName);
        xmlProcessor.sortXml();
        XmlOrderedResult xmlOrdered = xmlProcessor.isXmlOrdered();
        assertFalse(xmlOrdered.isOrdered());
        assertEquals(infoMessage, xmlOrdered.getErrorMessage());
    }

    private void setup(String inputFileName) throws Exception {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setPomFile(null)
                .setFileOutput(false, ".bak", null, false)
                .setEncoding("UTF-8")
                .setFormatting(lineSeparator, expandEmptyElements, true, keepBlankLines)
                .setIndent(2, indentBlankLines, false)
                .setSortOrder(predefinedSortOrder + ".xml", null)
                .setSortEntities("", "", "", false, false, false).build();
        final String xml = IOUtils.toString(new FileInputStream(inputFileName), StandardCharsets.UTF_8);

        final FileUtil fileUtil = new FileUtil();
        fileUtil.setup(pluginParameters);

        WrapperFactory wrapperFactory = new WrapperFactoryImpl(fileUtil);
        ((WrapperFactoryImpl) wrapperFactory).setup(pluginParameters);

        xmlProcessor = new XmlProcessor(wrapperFactory);

        xmlOutputGenerator = new XmlOutputGenerator();
        xmlOutputGenerator.setup(pluginParameters);

        if (sortAlphabeticalOnly) {
            wrapperFactory = new WrapperFactory() {

                @Override
                public HierarchyRootWrapper createFromRootElement(final Element rootElement) {
                    return new HierarchyRootWrapper(new AlphabeticalSortedWrapper(rootElement));
                }

                @SuppressWarnings("unchecked")
                @Override
                public <T extends Content> Wrapper<T> create(final T content) {
                    if (content instanceof Element) {
                        Element element = (Element) content;
                        return (Wrapper<T>) new AlphabeticalSortedWrapper(element);
                    }
                    return new UnsortedWrapper<>(content);
                }

            };
        } else {
            new ReflectionHelper(wrapperFactory).setField(fileUtil);
        }
        new ReflectionHelper(xmlProcessor).setField(wrapperFactory);
        xmlProcessor.setOriginalXml(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    public XmlProcessorTestUtil sortAlphabeticalOnly() {
        sortAlphabeticalOnly = true;
        return this;
    }

    public XmlProcessorTestUtil keepBlankLines() {
        keepBlankLines = true;
        return this;
    }

    public XmlProcessorTestUtil lineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
        return this;
    }

    public XmlProcessorTestUtil indentBlankLines() {
        indentBlankLines = true;
        return this;
    }

    public XmlProcessorTestUtil predefinedSortOrder(String predefinedSortOrder) {
        this.predefinedSortOrder = predefinedSortOrder;
        return this;
    }

    public XmlProcessorTestUtil expandEmptyElements(boolean expand) {
        this.expandEmptyElements = expand;
        return this;
    }
}
