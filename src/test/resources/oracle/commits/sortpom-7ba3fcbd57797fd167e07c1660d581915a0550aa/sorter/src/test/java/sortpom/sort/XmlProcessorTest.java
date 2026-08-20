package sortpom.sort;

import org.junit.Test;
import sortpom.util.XmlProcessorTestUtil;

public class XmlProcessorTest {

    @Test
    public final void testSortXmlAttributes() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/Attribute_input.xml", "src/test/resources/Attribute_expected.xml");
    }

    @Test
    public final void testSortXmlCharacter() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/Character_input.xml", "src/test/resources/Character_expected.xml");
    }

    @Test
    public final void testSortXmlComplex() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/Complex_input.xml", "src/test/resources/Complex_expected.xml");
    }

    @Test
    public final void testSortXmlFullFromAlphabeticalOrder() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/full_alfa_input.xml", "src/test/resources/full_expected.xml");
    }

    @Test
    public final void testSortXmlFullToAlfabetical() throws Exception {
        XmlProcessorTestUtil.create()
                .sortAlfabeticalOnly()
                .testInputAndExpected(
                        "src/test/resources/full_unsorted_input.xml", "src/test/resources/full_alfa_input.xml");
    }

    @Test
    public final void testSortXmlMultilineComment() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/MultilineComment_input.xml",
                "src/test/resources/MultilineComment_expected.xml");
    }

    @Test
    public final void testSortXmlReal1() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/Real1_input.xml", "src/test/resources/Real1_expected.xml");
    }

    @Test
    public final void testSortXmlSimple() throws Exception {
        XmlProcessorTestUtil.create().testInputAndExpected(
                "src/test/resources/Simple_input.xml", "src/test/resources/Simple_expected.xml");
    }

}
