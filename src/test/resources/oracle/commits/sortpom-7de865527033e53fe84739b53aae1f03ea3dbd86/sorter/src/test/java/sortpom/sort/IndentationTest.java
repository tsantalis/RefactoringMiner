package sortpom.sort;

import org.jdom.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sortpom.XmlOutputGenerator;
import sortpom.parameter.PluginParameters;
import sortpom.util.SortPomImplUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sortpom.sort.XmlFragment.createXmlProjectFragment;

class IndentationTest {

    @ParameterizedTest()
    @ValueSource(ints = {0, 1, 4, -1})
    void differentIndentsShouldWork(int indent) throws Exception {
        String expectedFile = "/SortModules_expectedIndent" + indent + ".xml";
        SortPomImplUtil.create()
                .sortProperties()
                .sortPlugins("true")
                .sortModules()
                .sortDependencies("true")
                .lineSeparator("\n")
                .nrOfIndentSpace(indent)
                .testFiles("/SortModules_input.xml", expectedFile);
    }

    @ParameterizedTest()
    @ValueSource(ints = {0, 1, 4, -1})
    final void indentSchemaLocationShouldBeIndentTimesTwoPlusOne(int indent) throws Exception {
        String expectedFile = "/SortModules_expectedSchemaIndent" + indent + ".xml";
        SortPomImplUtil.create()
                .sortProperties()
                .sortPlugins("true")
                .sortModules()
                .sortDependencies("true")
                .lineSeparator("\n")
                .nrOfIndentSpace(indent)
                .indentSchemaLocation()
                .testFiles("/SortModules_input.xml", expectedFile);
    }


    @ParameterizedTest
    @ValueSource(ints = {0, 1, 4, -1})
    void indentSchemaLocationShouldAddNewlineAndIndentation(int indent) {
        XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator();
        String lineSeparator = "\n";
        xmlOutputGenerator.setup(PluginParameters.builder()
                .setEncoding("UTF-8")
                .setFormatting(lineSeparator, true, true, false)
                .setIndent(indent, false, true)
                .build());

        String actual = xmlOutputGenerator.getSortedXml(createXmlProjectFragment());
        String indentChars = "";
        switch (indent) {
            case 1:
                indentChars = " ";
                break;
            case 4:
                indentChars = "    ";
                break;
            case -1:
                indentChars = "\t";
                break;
        }
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + lineSeparator
                + indentChars + indentChars + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">" + lineSeparator
                + indentChars + "<Gurka xmlns=\"\"></Gurka>" + lineSeparator
                + "</project>" + lineSeparator, actual);
    }

    @Test
    void otherAttributeShouldNotBeIndented() {
        XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator();
        String lineSeparator = "\n";
        xmlOutputGenerator.setup(PluginParameters.builder()
                .setEncoding("UTF-8")
                .setFormatting(lineSeparator, true, true, false)
                .setIndent(2, false, true)
                .build());

        Document xmlProjectFragment = createXmlProjectFragment();
        xmlProjectFragment.getRootElement().getChild("Gurka").setAttribute("key", "value");
        String actual = xmlOutputGenerator.getSortedXml(xmlProjectFragment);
        String indentChars = "  ";

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + lineSeparator
                + indentChars + indentChars + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">" + lineSeparator
                + indentChars + "<Gurka xmlns=\"\" key=\"value\"></Gurka>" + lineSeparator
                + "</project>" + lineSeparator, actual);
    }
}
