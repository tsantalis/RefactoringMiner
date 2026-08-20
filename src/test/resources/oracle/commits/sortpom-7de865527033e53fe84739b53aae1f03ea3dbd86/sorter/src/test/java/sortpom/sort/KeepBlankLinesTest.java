package sortpom.sort;

import org.junit.jupiter.api.Test;
import sortpom.util.SortPomImplUtil;
import sortpom.util.XmlProcessorTestUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

class KeepBlankLinesTest {
    @Test
    final void emptyRowsInSimplePomShouldBePreserved() throws Exception {
        XmlProcessorTestUtil.create()
                .keepBlankLines()
                .testInputAndExpected("src/test/resources/EmptyRow_input.xml", "src/test/resources/EmptyRow_expected.xml");
    }

    @Test
    final void emptyRowsInLargePomShouldBePreserved1() throws Exception {
        XmlProcessorTestUtil.create()
                .keepBlankLines()
                .testInputAndExpected("src/test/resources/Real1_input.xml", "src/test/resources/Real1_expected_keepBlankLines.xml");
    }

    @Test
    final void emptyRowsInLargePomShouldBePreservedAndIndented1() throws Exception {
        XmlProcessorTestUtil.create()
                .keepBlankLines()
                .indentBlankLines()
                .testInputAndExpected("src/test/resources/Real1_input.xml", "src/test/resources/Real1_expected_keepBlankLines_indented.xml");
    }

    @Test
    final void emptyRowsInLargePomShouldBePreserved2() throws Exception {
        SortPomImplUtil.create()
                .keepBlankLines()
                .testFiles("/Real1_input.xml", "/Real1_expected_keepBlankLines.xml");
    }

    @Test
    final void emptyRowsInLargePomShouldBePreservedAndIndented2() throws Exception {
        SortPomImplUtil.create()
                .keepBlankLines()
                .indentBLankLines()
                .testFiles("/Real1_input.xml", "/Real1_expected_keepBlankLines_indented.xml");
    }

    @Test
    final void simpleLineBreaksShouldNotBePreserved() throws Exception {
        XmlProcessorTestUtil.create()
                .keepBlankLines()
                .testInputAndExpected("src/test/resources/LineBreak_input.xml", "src/test/resources/Character_expected.xml");
    }

    @Test
    final void allLineBreaksInXmlShouldBeNewlines() throws Exception {
        String actual = XmlProcessorTestUtil.create()
                .keepBlankLines()
                .lineSeparator("\n")
                .sortXmlAndReturnResult("src/test/resources/LineBreak_N_input.xml");

        assertThat(actual, containsString("</groupId>\n  <artifactId>whitespace-test</artifactId>\n  <version>"));
        assertThat(actual, containsString(",\nembedded,\nand"));
    }

    @Test
    final void allLineBreaksInXmlShouldBeCarriageReturnNewlines() throws Exception {
        String actual = XmlProcessorTestUtil.create()
                .keepBlankLines()
                .lineSeparator("\r\n")
                .sortXmlAndReturnResult("src/test/resources/LineBreak_N_input.xml");

        assertThat(actual, containsString("</groupId>\r\n  <artifactId>whitespace-test</artifactId>\r\n  <version>"));
        assertThat(actual, containsString(",\r\nembedded,\r\nand"));
    }

    @Test
    final void allLineBreaksInXmlShouldBeCarriageReturn() throws Exception {
        String actual = XmlProcessorTestUtil.create()
                .keepBlankLines()
                .lineSeparator("\r")
                .sortXmlAndReturnResult("src/test/resources/LineBreak_N_input.xml");

        assertThat(actual, containsString("</groupId>\r  <artifactId>whitespace-test</artifactId>\r  <version>"));
        assertThat(actual, containsString(",\rembedded,\rand"));
    }

    @Test
    final void allLineBreaksInXmlShouldBeNewlines2() throws Exception {
        String actual = XmlProcessorTestUtil.create()
                .keepBlankLines()
                .lineSeparator("\n")
                .sortXmlAndReturnResult("src/test/resources/LineBreak_RN_input.xml");

        assertThat(actual, containsString("</groupId>\n  <artifactId>whitespace-test</artifactId>\n  <version>"));
        assertThat(actual, containsString(",\nembedded,\nand"));
    }

    @Test
    final void allLineBreaksInXmlShouldBeCarriageReturnNewlines2() throws Exception {
        String actual = XmlProcessorTestUtil.create()
                .keepBlankLines()
                .lineSeparator("\r\n")
                .sortXmlAndReturnResult("src/test/resources/LineBreak_RN_input.xml");

        assertThat(actual, containsString("</groupId>\r\n  <artifactId>whitespace-test</artifactId>\r\n  <version>"));
        assertThat(actual, containsString(",\r\nembedded,\r\nand"));
    }

    @Test
    final void allLineBreaksInXmlShouldBeCarriageReturn2() throws Exception {
        String actual = XmlProcessorTestUtil.create()
                .keepBlankLines()
                .lineSeparator("\r")
                .sortXmlAndReturnResult("src/test/resources/LineBreak_RN_input.xml");

        assertThat(actual, containsString("</groupId>\r  <artifactId>whitespace-test</artifactId>\r  <version>"));
        assertThat(actual, containsString(",\rembedded,\rand"));
    }

}
