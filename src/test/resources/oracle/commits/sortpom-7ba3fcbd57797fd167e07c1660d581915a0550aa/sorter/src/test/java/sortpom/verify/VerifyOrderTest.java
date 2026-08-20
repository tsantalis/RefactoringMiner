package sortpom.verify;

import org.junit.Test;
import sortpom.exception.FailureException;
import sortpom.util.SortPomImplUtil;

/**
 * @author bjorn
 * @since 2012-07-01
 */
public class VerifyOrderTest {

    @Test
    public final void sortedButUnformattedSortOrderShouldPass() throws Exception {
        SortPomImplUtil.create()
                .predefinedSortOrder("recommended_2008_06")
                .testVerifyXmlIsOrdered("/Real2_input.xml");
    }

    @Test
    public final void wrongSortedShouldNotPass() throws Exception {
        SortPomImplUtil.create()
                .predefinedSortOrder("custom_1")
                .testVerifyXmlIsNotOrdered("/Real2_input.xml",
                        "The xml element <properties> should be placed before <inceptionYear>");
    }

    @Test
    public void unsortedDefaultVerifyShouldPerformSort() throws Exception {
        SortPomImplUtil.create()
                .testVerifySort("/Real1_input.xml", "/Real1_expected.xml", "[INFO] The xml element <version> should be placed before <name>", false);
    }

    @Test
    public void unsortedSortVerifyShouldPerformSort() throws Exception {
        SortPomImplUtil.create()
                .verifyFail("SORT")
                .testVerifySort("/Real1_input.xml", "/Real1_expected.xml", "[INFO] The xml element <version> should be placed before <name>", false);
    }

    @Test
    public void unsortedStopVerifyShouldPerformSort() {
        SortPomImplUtil.create()
                .verifyFail("STOP")
                .testVerifyFail("/Real1_input.xml", FailureException.class, "[ERROR] The xml element <version> should be placed before <name>", false);
    }

    @Test
    public void unsortedWarnVerifyShouldPerformSort() throws Exception {
        SortPomImplUtil.create()
                .verifyFail("WARN")
                .testVerifyWarn("/Real1_input.xml",
                        "[WARNING] The xml element <version> should be placed before <name>", false);
    }

}
