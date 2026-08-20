package sortpom.verify;

import org.junit.jupiter.api.Test;
import sortpom.exception.FailureException;
import sortpom.util.SortPomImplUtil;

/**
 * @author bjorn
 * @since 2012-07-01
 */
class VerifyOrderTest {

    @Test
    final void sortedButUnformattedSortOrderShouldPass() throws Exception {
        SortPomImplUtil.create()
                .predefinedSortOrder("recommended_2008_06")
                .testVerifyXmlIsOrdered("/Real2_input.xml");
    }

    @Test
    final void wrongSortedShouldNotPass() throws Exception {
        SortPomImplUtil.create()
                .predefinedSortOrder("custom_1")
                .testVerifyXmlIsNotOrdered("/Real2_input.xml",
                        "The xml element <properties> should be placed before <inceptionYear>");
    }

    @Test
    final void wrongSortedWithStrictVerifyShouldNotPass() throws Exception {
        SortPomImplUtil.create()
                .verifyFailOn("strict")
                .predefinedSortOrder("custom_1")
                .testVerifyXmlIsNotOrdered("/Real2_input.xml",
                        "The line 2 is not considered sorted, should be '<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">'");
    }

    @Test
    void unsortedDefaultVerifyShouldPerformSort() throws Exception {
        SortPomImplUtil.create()
                .testVerifySort("/Real1_input.xml", "/Real1_expected.xml", 
                        "[INFO] The xml element <version> should be placed before <name>", false);
    }

    @Test
    void unsortedDefaultStrictVerifyShouldPerformSort() throws Exception {
        SortPomImplUtil.create()
                .verifyFailOn("STRICT")
                .createBackupFile(false)
                .testVerifySort("/Real1_input.xml", "/Real1_expected.xml", 
                        "[INFO] The line 1 is not considered sorted, should be '<?xml version=\"1.0\" encoding=\"UTF-8\"?>'", false);
    }

    @Test
    void unsortedSortVerifyShouldPerformSort() throws Exception {
        SortPomImplUtil.create()
                .verifyFail("SORT")
                .testVerifySort("/Real1_input.xml", "/Real1_expected.xml", 
                        "[INFO] The xml element <version> should be placed before <name>", false);
    }

    @Test
    void unsortedStopVerifyShouldPerformSort() {
        SortPomImplUtil.create()
                .verifyFail("STOP")
                .testVerifyFail("/Real1_input.xml", FailureException.class, 
                        "[ERROR] The xml element <version> should be placed before <name>", false);
    }

    @Test
    void unsortedWarnVerifyShouldPerformSort() throws Exception {
        SortPomImplUtil.create()
                .verifyFail("WARN")
                .testVerifyWarn("/Real1_input.xml",
                        "[WARNING] The xml element <version> should be placed before <name>", false);
    }

}
