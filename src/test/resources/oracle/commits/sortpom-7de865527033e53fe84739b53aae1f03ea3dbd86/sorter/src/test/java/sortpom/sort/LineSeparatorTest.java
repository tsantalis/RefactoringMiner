package sortpom.sort;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sortpom.XmlOutputGenerator;
import sortpom.parameter.PluginParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sortpom.sort.XmlFragment.createXmlFragment;

class LineSeparatorTest {
    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r", "\r\n"})
    void formattingXmlWithLineEndingsShouldResultInOneLineBreakAtEnd(String lineSeparator) {
        XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator();
        xmlOutputGenerator.setup(PluginParameters.builder()
                .setEncoding("UTF-8")
                .setFormatting(lineSeparator, false, true, false)
                .setIndent(2, false, false)
                .build());

        String actual = xmlOutputGenerator.getSortedXml(createXmlFragment());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator +
                "<Gurka />" + lineSeparator, actual);
    }
}
