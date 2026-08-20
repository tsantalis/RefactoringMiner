package sortpom.parameter;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sortpom.exception.FailureException;
import sortpom.util.SortPomImplUtil;

import java.io.File;
import java.nio.charset.Charset;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ViolationFileParameterTest {

    private static final String FILENAME_WITH_DIRECTORIES = "target/sortpom_reports/1/violation.xml";
    private static final String FILENAME_WITHOUT_DIRECTORIES = "target/violation.xml";
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public final void violationFileCanBeOverwritten() throws Exception {
        File tempFile = File.createTempFile("violation", ".xml", new File("target"));
        SortPomImplUtil.create()
                .violationFile(tempFile.getAbsolutePath())
                .testVerifySort("/full_unsorted_input.xml",
                        "/full_expected.xml",
                        "[INFO] The xml element <modelVersion> should be placed before <parent>",
                        true);
    }

    @Test
    public final void readOnlyViolationFileShouldReportError() throws Exception {
        File tempFile = File.createTempFile("violation", ".xml", new File("target"));
        assertTrue(tempFile.setReadOnly());
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Could not save violation file: " + tempFile.getAbsolutePath());

        SortPomImplUtil.create()
                .violationFile(tempFile.getAbsolutePath())
                .testVerifySort("/full_unsorted_input.xml",
                        "/full_expected.xml",
                        "[INFO] The xml element <modelVersion> should be placed before <parent>",
                        true);
    }

    @Test
    public final void violationFileShouldBeCreatedOnVerificationStop() {
        new File(FILENAME_WITHOUT_DIRECTORIES).delete();

        SortPomImplUtil.create()
                .verifyFail("Stop")
                .violationFile(FILENAME_WITHOUT_DIRECTORIES)
                .testVerifyFail("/full_unsorted_input.xml",
                        FailureException.class,
                        "[ERROR] The xml element <modelVersion> should be placed before <parent>",
                        true);
        File file = new File(FILENAME_WITHOUT_DIRECTORIES);
        assertThat(file.exists(), is(true));
    }

    @Test
    public final void violationFileWithParentDirectoryShouldBeCreatedOnVerificationWarn() throws Exception {
        new File(FILENAME_WITH_DIRECTORIES).delete();

        SortPomImplUtil.create()
                .verifyFail("Warn")
                .violationFile(FILENAME_WITH_DIRECTORIES)
                .testVerifyWarn("/full_unsorted_input.xml",
                        "[WARNING] The xml element <modelVersion> should be placed before <parent>",
                        true);
        File file = new File(FILENAME_WITH_DIRECTORIES);
        assertThat(file.exists(), is(true));
    }

    @Test
    public final void violationFileContentShouldBeEncodedOnVerificationSort() throws Exception {
        new File(FILENAME_WITH_DIRECTORIES).delete();

        SortPomImplUtil.create()
                .verifyFail("Sort")
                .violationFile(FILENAME_WITH_DIRECTORIES)
                .testVerifySort("/full_unsorted_input.xml",
                        "/full_expected.xml",
                        "[INFO] The xml element <modelVersion> should be placed before <parent>",
                        true);
        String xml = FileUtils.readFileToString(new File(FILENAME_WITH_DIRECTORIES), Charset.defaultCharset());
        assertThat(xml, containsString("The xml element &lt;modelVersion&gt; should be placed before &lt;parent&gt;"));
    }

}
