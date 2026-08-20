package sortpom.sort;

import org.junit.Test;
import sortpom.XmlOutputGenerator;
import sortpom.parameter.PluginParameters;

import static org.junit.Assert.assertEquals;
import static sortpom.sort.ExpandEmptyElementTest.createXmlFragment;

public class LineSeparatorTest {
    @Test
    public void formattingXmlWithNewlineShouldResultInOneLineBreakAtEnd() {
        XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator();
        xmlOutputGenerator.setup(PluginParameters.builder()
                .setEncoding("UTF-8")
                .setFormatting("\n", false, false)
                .setIndent(2, false)
                .build());

        String actual = xmlOutputGenerator.getSortedXml(createXmlFragment());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Gurka />\n", actual);
    }

    @Test
    public void formattingXmlWithCRShouldResultInOneLineBreakAtEnd() {
        XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator();
        xmlOutputGenerator.setup(PluginParameters.builder()
                .setEncoding("UTF-8")
                .setFormatting("\r", false, false)
                .setIndent(2, false)
                .build());

        String actual = xmlOutputGenerator.getSortedXml(createXmlFragment());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r<Gurka />\r", actual);
    }

    @Test
    public void formattingXmlWithCRNLShouldResultInOneLineBreakAtEnd() {
        XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator();
        xmlOutputGenerator.setup(PluginParameters.builder()
                .setEncoding("UTF-8")
                .setFormatting("\r\n", false, false)
                .setIndent(2, false)
                .build());

        String actual = xmlOutputGenerator.getSortedXml(createXmlFragment());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<Gurka />\r\n", actual);
    }

}
