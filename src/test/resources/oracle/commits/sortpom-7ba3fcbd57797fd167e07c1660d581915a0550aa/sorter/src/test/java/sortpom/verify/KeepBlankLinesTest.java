package sortpom.verify;

import org.junit.Test;
import sortpom.util.SortPomImplUtil;
import sortpom.util.XmlProcessorTestUtil;

public class KeepBlankLinesTest {
    @Test
    public final void emptyLinesInXmlShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .testVerifyXmlIsOrdered("src/test/resources/EmptyRow_input2.xml");
    }

    @Test
    public final void emptyLinesInXmlShouldNotAffectVerify2() throws Exception {
        XmlProcessorTestUtil.create()
                .keepBlankLines()
                .testVerifyXmlIsOrdered("src/test/resources/EmptyRow_input2.xml");
    }

    @Test
    public final void emptyLinesInXmlAndIndentParameterShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .keepBlankLines()
                .indentBlankLines()
                .testVerifyXmlIsOrdered("src/test/resources/EmptyRow_input2.xml");
    }

    @Test
    public final void emptyLinesInXmlShouldNotAffectVerify3() throws Exception {
        SortPomImplUtil.create()
                .keepBlankLines()
                .testVerifyXmlIsOrdered("/EmptyRow_input2.xml");
    }

    @Test
    public final void emptyLinesInXmlAndIndentParameterShouldNotAffectVerify2() throws Exception {
        SortPomImplUtil.create()
                .keepBlankLines()
                .indentBLankLines()
                .testVerifyXmlIsOrdered("/EmptyRow_input2.xml");
    }

    @Test
    public final void unsortedXmlAndIndentParameterShouldAffectVerify() throws Exception {
        SortPomImplUtil.create()
                .keepBlankLines()
                .indentBLankLines()
                .testVerifyXmlIsNotOrdered("/EmptyRow_input.xml",
                        "The xml element <modelVersion> should be placed before <artifactId>");
    }

    @Test
    public final void simpleLineBreaksShouldNotAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .keepBlankLines()
                .testVerifyXmlIsOrdered("src/test/resources/LineBreak_input2.xml");
    }

    @Test
    public final void unsortedXmlShouldAffectVerify() throws Exception {
        XmlProcessorTestUtil.create()
                .keepBlankLines()
                .testVerifyXmlIsNotOrdered("src/test/resources/LineBreak_input.xml",
                        "The xml element <modelVersion> should be placed before <artifactId>");
    }
}
