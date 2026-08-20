package sortpom.verify;

import org.junit.jupiter.api.Test;
import sortpom.util.SortPomImplUtil;

class SortOrderFilesTest {
    @Test
    final void sortedCustomSortOrderShouldNotTriggerVerify() throws Exception {
        SortPomImplUtil.create()
                .defaultOrderFileName("difforder/differentOrder.xml")
                .lineSeparator("\n")
                .testVerifyXmlIsOrdered("/sortOrderFiles/sorted_differentOrder.xml");
    }

    @Test
    final void unsortedCustomSortOrderShouldTriggerVerify() throws Exception {
        SortPomImplUtil.create()
                .defaultOrderFileName("difforder/differentOrder.xml")
                .lineSeparator("\n")
                .testVerifyXmlIsNotOrdered("/full_unsorted_input.xml",
                        "The xml element <modelVersion> should be placed before <parent>");
    }

    @Test
    final void default040ShouldWorkAsPredefinedSortOrder() throws Exception {
        SortPomImplUtil.create()
                .predefinedSortOrder("default_0_4_0")
                .lineSeparator("\n")
                .testVerifyXmlIsOrdered("/sortOrderFiles/sorted_default_0_4_0.xml");
    }

    @Test
    final void custom1ShouldWorkAsPredefinedSortOrder() throws Exception {
        SortPomImplUtil.create()
                .predefinedSortOrder("custom_1")
                .lineSeparator("\n")
                .testVerifyXmlIsOrdered("/sortOrderFiles/sorted_custom_1.xml");
    }

    @Test
    final void recommended2008_06ShouldWorkAsPredefinedSortOrder() throws Exception {
        SortPomImplUtil.create()
                .predefinedSortOrder("recommended_2008_06")
                .lineSeparator("\n")
                .testVerifyXmlIsOrdered("/sortOrderFiles/sorted_recommended_2008_06.xml");
    }

    @Test
    final void default100ShouldWorkAsPredefinedSortOrder() throws Exception {
        SortPomImplUtil.create()
                .predefinedSortOrder("default_1_0_0")
                .lineSeparator("\n")
                .testVerifyXmlIsOrdered("/sortOrderFiles/sorted_default_1_0_0.xml");
    }

    @Test
    final void defaultPredefinedSortOrderShouldWork() throws Exception {
        SortPomImplUtil.create()
                .predefinedSortOrder(null)
                .lineSeparator("\n")
                .testVerifyXmlIsOrdered("/sortOrderFiles/sorted_default_1_0_0.xml");
    }

    @Test
    void xmlDeviationsShouldNotHarmPlugin() throws Exception {
        SortPomImplUtil.create()
                .lineSeparator("\n")
                .testVerifyXmlIsOrdered("/Xml_deviations_output.xml");
    }

}
