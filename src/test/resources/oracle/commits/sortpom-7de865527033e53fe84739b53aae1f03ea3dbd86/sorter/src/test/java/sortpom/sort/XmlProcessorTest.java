package sortpom.sort;

import org.junit.jupiter.api.Test;
import sortpom.util.XmlProcessorTestUtil;

class XmlProcessorTest {

    @Test
    final void testSortXmlAttributes() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/Attribute_input.xml", "src/test/resources/Attribute_expected.xml");
    }

    @Test
    final void testSortXmlCharacter() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/Character_input.xml", "src/test/resources/Character_expected.xml");
    }

    @Test
    final void testSortXmlComplex() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/Complex_input.xml", "src/test/resources/Complex_expected.xml");
    }

    @Test
    final void testSortXmlFullFromAlphabeticalOrder() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/full_alfa_input.xml", "src/test/resources/full_expected.xml");
    }

    @Test
    final void testSortXmlFullToAlfabetical() throws Exception {
        XmlProcessorTestUtil.create()
                .sortAlphabeticalOnly()
                .testInputAndExpected(
                        "src/test/resources/full_unsorted_input.xml", "src/test/resources/full_alfa_input.xml");
    }

    @Test
    final void testSortXmlMultilineComment() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/MultilineComment_input.xml",
                "src/test/resources/MultilineComment_expected.xml");
    }

    @Test
    final void testSortXmlReal1() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/Real1_input.xml", "src/test/resources/Real1_expected.xml");
    }

    @Test
    final void testSortXmlSimple() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/Simple_input.xml", "src/test/resources/Simple_expected.xml");
    }

}
