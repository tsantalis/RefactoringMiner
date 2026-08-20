package sortpom.verify;

import org.junit.Test;
import sortpom.util.XmlProcessorTestUtil;

public class XmlProcessorTest {

    @Test
    public final void testSortXmlAttributesShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create().testVerifyXmlIsOrdered(
                "src/test/resources/Attribute_expected.xml");
    }

    @Test
    public final void testSortXmlMultilineCommentShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create().testVerifyXmlIsOrdered(
                "src/test/resources/MultilineComment_expected.xml");
    }


    @Test
    public final void testSortXmlAttributesShouldAffectVerify() throws Exception {
        XmlProcessorTestUtil.create().testVerifyXmlIsNotOrdered(
                "src/test/resources/Attribute_input.xml",
                "The xml element <modelVersion> should be placed before <artifactId>");
    }

    @Test
    public final void testSortXmlMultilineCommentShouldAffectVerify() throws Exception {
        XmlProcessorTestUtil.create().testVerifyXmlIsNotOrdered(
                "src/test/resources/MultilineComment_input.xml",
                "The xml element <groupId> should be placed before <artifactId>");
    }
}
