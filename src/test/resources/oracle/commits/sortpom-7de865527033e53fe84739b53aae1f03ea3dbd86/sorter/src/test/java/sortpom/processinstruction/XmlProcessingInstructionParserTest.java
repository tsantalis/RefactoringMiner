package sortpom.processinstruction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import sortpom.exception.FailureException;
import sortpom.logger.SortPomLogger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author bjorn
 * @since 2013-12-28
 */
class XmlProcessingInstructionParserTest {

    private XmlProcessingInstructionParser parser;
    private final SortPomLogger logger = mock(SortPomLogger.class);

    @BeforeEach
    void setUp() {
        parser = new XmlProcessingInstructionParser();
        parser.setup(logger);
    }

    @Test
    void multipleErrorsShouldBeReportedInLogger() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                "  <artifactId>sortpom</artifactId>\n" +
                "  <description name=\"pelle\" id=\"id\" other=\"övrigt\">Här använder vi åäö</description>\n" +
                "  <groupId>sortpom</groupId>\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <name>SortPom</name>\n" +
                "  <!-- Egenskaper för projektet -->\n" +
                "  <properties>\n" +
                "    <?sortpom resume?>" +
                "<compileSource>1.6</compileSource>\n" +
                "    <?sortpom ignore?>" +
                "    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
                "    <?sortpom resume?>" +
                "  </properties>\n" +
                "    <?sortpom token='0'?>" +
                "    <?sortpom gurka?>" +
                "  <reporting />\n" +
                "    <?sortpom ignore?>" +
                "  <version>1.0.0-SNAPSHOT</version>\n" +
                "</project>";

        final Executable testMethod = () -> parser.scanForIgnoredSections(xml);

        final FailureException thrown = assertThrows(FailureException.class, testMethod);

        assertThat(thrown.getMessage(), is("Xml contained unexpected sortpom instruction 'resume'. Please use expected instruction <?sortpom IGNORE?>"));

        verify(logger).error("Xml contained unexpected sortpom instruction 'resume'. Please use expected instruction <?sortpom IGNORE?>");
        verify(logger).error("Xml contained unknown sortpom instruction 'token='0''. Please use <?sortpom IGNORE?> or <?sortpom RESUME?>");
        verify(logger).error("Xml contained unknown sortpom instruction 'gurka'. Please use <?sortpom IGNORE?> or <?sortpom RESUME?>");
        verify(logger).error("Xml processing instructions for sortpom was not properly terminated. Every <?sortpom IGNORE?> must be followed with <?sortpom RESUME?>");
        verifyNoMoreInteractions(logger);
    }

    @Test
    void replaceMultipleSectionShouldCreateManyTokens() {
        String xml = "abc<?sortpom ignore?>def0<?sortpom resume?>cbaabc<?SORTPOM Ignore?>def1<?sortPom reSUME?>cba";
        parser.scanForIgnoredSections(xml);
        String replaced = parser.replaceIgnoredSections();

        assertThat(replaced, is("abc<?sortpom token='0'?>cbaabc<?sortpom token='1'?>cba"));
        assertThat(parser.existsIgnoredSections(), is(true));
        verifyNoMoreInteractions(logger);
    }

    @Test
    void revertSectionsInRearrangedOrderShouldPlaceTextInRightOrder() {
        String xml = "abc<?sortpom ignore?>def0<?sortpom resume?>cbaabc<?SORTPOM Ignore?>def1<?sortPom reSUME?>cba";
        parser.scanForIgnoredSections(xml);
        String replaced = parser.replaceIgnoredSections();
        assertThat(replaced, is("abc<?sortpom token='0'?>cbaabc<?sortpom token='1'?>cba"));

        String sortedXml = "abc<?sortpom token='1'?>cbaabc<?sortpom token='0'?>cba";
        String outputXml = parser.revertIgnoredSections(sortedXml);

        assertThat(outputXml, is("abc<?SORTPOM Ignore?>def1<?sortPom reSUME?>cbaabc<?sortpom ignore?>def0<?sortpom resume?>cba"));
        verifyNoMoreInteractions(logger);
    }

    @Test
    void noInstructionsShouldWork() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                "  <artifactId>sortpom</artifactId>\n" +
                "  <description name=\"pelle\" id=\"id\" other=\"övrigt\">Här använder vi åäö</description>\n" +
                "  <groupId>sortpom</groupId>\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <name>SortPom</name>\n" +
                "  <!-- Egenskaper för projektet -->\n" +
                "  <properties>\n" +
                "    <compileSource>1.6</compileSource>\n" +
                "    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
                "  </properties>\n" +
                "  <reporting />\n" +
                "  <version>1.0.0-SNAPSHOT</version>\n" +
                "</project>";
        parser.scanForIgnoredSections(xml);

        assertThat(parser.existsIgnoredSections(), is(false));

        String replaced = parser.replaceIgnoredSections();
        String outputXml = parser.revertIgnoredSections(xml);

        assertThat(replaced, is(xml));
        assertThat(outputXml, is(xml));
        verifyNoMoreInteractions(logger);
    }
}
