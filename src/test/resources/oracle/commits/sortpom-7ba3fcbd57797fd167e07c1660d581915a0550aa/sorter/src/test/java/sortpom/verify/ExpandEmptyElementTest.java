package sortpom.verify;

import org.junit.Test;
import sortpom.util.XmlProcessorTestUtil;

public class ExpandEmptyElementTest {
    @Test
    public void trueExpandedParameterAndTrueExpandedElementShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .expandEmptyElements(true)
                .testVerifyXmlIsOrdered("src/test/resources/ExpandedElement_input.xml");
    }

    @Test
    public void falseExpandedParameterAndTrueExpandedElementShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .expandEmptyElements(false)
                .testVerifyXmlIsOrdered("src/test/resources/ExpandedElement_input.xml");
    }

    @Test
    public void trueExpandedParameterAndFalseExpandedElementShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .expandEmptyElements(true)
                .testVerifyXmlIsOrdered("src/test/resources/ExpandedElementNot_input.xml");
    }

    @Test
    public void falseExpandedParameterAndFalseExpandedElementShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .expandEmptyElements(true)
                .testVerifyXmlIsOrdered("src/test/resources/ExpandedElementNot_input.xml");
    }

}
