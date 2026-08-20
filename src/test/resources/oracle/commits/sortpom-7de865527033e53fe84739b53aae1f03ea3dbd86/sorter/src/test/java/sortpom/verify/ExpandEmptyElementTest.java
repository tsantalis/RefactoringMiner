package sortpom.verify;

import org.junit.jupiter.api.Test;
import sortpom.util.XmlProcessorTestUtil;

class ExpandEmptyElementTest {
    @Test
    void trueExpandedParameterAndTrueExpandedElementShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .expandEmptyElements(true)
                .testVerifyXmlIsOrdered("src/test/resources/ExpandedElement_input.xml");
    }

    @Test
    void falseExpandedParameterAndTrueExpandedElementShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .expandEmptyElements(false)
                .testVerifyXmlIsOrdered("src/test/resources/ExpandedElement_input.xml");
    }

    @Test
    void trueExpandedParameterAndFalseExpandedElementShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .expandEmptyElements(true)
                .testVerifyXmlIsOrdered("src/test/resources/ExpandedElementNot_input.xml");
    }

    @Test
    void falseExpandedParameterAndFalseExpandedElementShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .expandEmptyElements(true)
                .testVerifyXmlIsOrdered("src/test/resources/ExpandedElementNot_input.xml");
    }

}
