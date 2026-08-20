package sortpom.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import sortpom.SortPomImpl;
import sortpom.logger.SortPomLogger;
import sortpom.parameter.PluginParameters;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * @author bjorn
 * @since 2012-08-23
 */
class TestHandler {

    private final SortPomImpl sortPomImpl = new SortPomImpl();

    private final List<String> infoLogger = new ArrayList<>();
    private final String inputResourceFileName;
    private final String expectedResourceFileName;
    private final File testpom;
    private final PluginParameters pluginParameters;

    private FileInputStream backupFileInputStream = null;
    private FileInputStream originalPomInputStream = null;
    private FileInputStream actualSortedPomInputStream = null;
    private FileInputStream expectedSortedPomInputStream = null;
    private final File backupFile;

    TestHandler(String inputResourceFileName, PluginParameters pluginParameters) {
        this(inputResourceFileName, null, pluginParameters);
    }

    TestHandler(String inputResourceFileName, String expectedResourceFileName, PluginParameters pluginParameters) {
        this.inputResourceFileName = inputResourceFileName;
        this.expectedResourceFileName = expectedResourceFileName;
        this.pluginParameters = pluginParameters;
        this.testpom = pluginParameters.pomFile;
        backupFile = new File(testpom.getAbsolutePath() + pluginParameters.backupFileExtension);
    }

    List<String> getInfoLogger() {
        return infoLogger;
    }

    void performSortThatSorted() throws Exception {
        try {
            removeOldTemporaryFiles();

            FileUtils.copyFile(new File("src/test/resources/" + inputResourceFileName), testpom);
            performSorting();

            assertTrue(testpom.exists());
                assertTrue(backupFile.exists());

            backupFileInputStream = new FileInputStream(backupFile);
            String actualBackup = IOUtils.toString(backupFileInputStream, pluginParameters.encoding);

            originalPomInputStream = new FileInputStream("src/test/resources/" + inputResourceFileName);
            String expectedBackup = IOUtils.toString(originalPomInputStream, pluginParameters.encoding);
            assertEquals(expectedBackup, actualBackup);

            actualSortedPomInputStream = new FileInputStream(testpom);
            String actualSorted = IOUtils.toString(actualSortedPomInputStream, pluginParameters.encoding);

            expectedSortedPomInputStream = new FileInputStream("src/test/resources/" + expectedResourceFileName);
            String expectedSorted = IOUtils.toString(expectedSortedPomInputStream, pluginParameters.encoding);
            assertThat(actualSorted, is(expectedSorted));
            assertEquals(expectedSorted, actualSorted);
        } finally {
            cleanupAfterTest();
        }
    }

    void performVerifyThatSorted() throws Exception {
        try {
            removeOldTemporaryFiles();

            FileUtils.copyFile(new File("src/test/resources/" + inputResourceFileName), testpom);
            performVerifyWithSort();

            assertTrue(testpom.exists());
            assertTrue(backupFile.exists());

            backupFileInputStream = new FileInputStream(backupFile);
            String actualBackup = IOUtils.toString(backupFileInputStream, pluginParameters.encoding);

            originalPomInputStream = new FileInputStream("src/test/resources/" + inputResourceFileName);
            String expectedBackup = IOUtils.toString(originalPomInputStream, pluginParameters.encoding);
            assertEquals(expectedBackup, actualBackup);

            actualSortedPomInputStream = new FileInputStream(testpom);
            String actualSorted = IOUtils.toString(actualSortedPomInputStream, pluginParameters.encoding);

            expectedSortedPomInputStream = new FileInputStream("src/test/resources/" + expectedResourceFileName);
            String expectedSorted = IOUtils.toString(expectedSortedPomInputStream, pluginParameters.encoding);
            assertEquals(expectedSorted, actualSorted);
        } finally {
            cleanupAfterTest();
        }
    }

    void performSortThatDidNotSort() throws Exception {
        try {
            removeOldTemporaryFiles();

            FileUtils.copyFile(new File("src/test/resources/" + inputResourceFileName), testpom);
            performSorting();

            assertTrue(testpom.exists());
            assertFalse(backupFile.exists());

            actualSortedPomInputStream = new FileInputStream(testpom);
            String actualSorted = IOUtils.toString(actualSortedPomInputStream, pluginParameters.encoding);

            expectedSortedPomInputStream = new FileInputStream("src/test/resources/" + expectedResourceFileName);
            String expectedSorted = IOUtils.toString(expectedSortedPomInputStream, pluginParameters.encoding);
            assertEquals(expectedSorted, actualSorted);
        } finally {
            cleanupAfterTest();
        }
    }

    private void performSorting() {
        sortPomImpl.setup(
                createDummyLog(),
                pluginParameters);
        sortPomImpl.sortPom();
    }

    XmlOrderedResult performVerify() throws Exception {
        try {
            removeOldTemporaryFiles();
            FileUtils.copyFile(new File("src/test/resources/" + inputResourceFileName), testpom);
            XmlOrderedResult verifyOk = isVerifyOk();

            assertTrue(testpom.exists());
            return verifyOk;
        } finally {
            cleanupAfterTest();
        }
    }

    void performVerifyThatDidNotSort() throws Exception {
        try {
            removeOldTemporaryFiles();

            FileUtils.copyFile(new File("src/test/resources/" + inputResourceFileName), testpom);
            performVerifyWithSort();

            assertTrue(testpom.exists());
            assertFalse(backupFile.exists());

            actualSortedPomInputStream = new FileInputStream(testpom);
            String actualSorted = IOUtils.toString(actualSortedPomInputStream, pluginParameters.encoding);

            expectedSortedPomInputStream = new FileInputStream("src/test/resources/" + expectedResourceFileName);
            String expectedSorted = IOUtils.toString(expectedSortedPomInputStream, pluginParameters.encoding);
            assertEquals(expectedSorted, actualSorted);
        } finally {
            cleanupAfterTest();
        }
    }

    void performSortThatTestsTimestamps() throws Exception {
        try {
            removeOldTemporaryFiles();

            FileUtils.copyFile(new File("src/test/resources/" + inputResourceFileName), testpom);
            long pomTimestamp = testpom.lastModified();
            performSorting();

            if (pluginParameters.keepTimestamp) {
                assertThat(testpom.lastModified(), is(pomTimestamp));
                // Do not assert anything about the backup file, since that timestamp is OS dependent
            } else {
                assertThat(testpom.lastModified(), greaterThan(pomTimestamp));
                // Do not assert anything about the backup file, since that timestamp is OS dependent
            }
        } finally {
            cleanupAfterTest();
        }
    }

    private void performVerifyWithSort() {
        SortPomImpl sortPomImpl = new SortPomImpl();
        sortPomImpl.setup(
                createDummyLog(),
                pluginParameters);

        sortPomImpl.verifyPom();
    }

    private XmlOrderedResult isVerifyOk() {
        sortPomImpl.setup(
                createDummyLog(),
                pluginParameters);
        return sortPomImpl.isPomElementsSorted();
    }

    private void removeOldTemporaryFiles() {
        if (testpom.exists()) {
            assertTrue(testpom.delete());
        }
    }

    private void cleanupAfterTest() {
        closeQuietly(backupFileInputStream);
        closeQuietly(originalPomInputStream);
        closeQuietly(actualSortedPomInputStream);
        closeQuietly(expectedSortedPomInputStream);
        if (testpom.exists()) {
            assertTrue(testpom.delete());
        }
        if (backupFile.exists()) {
            assertTrue(backupFile.delete());
        }
    }

    private void closeQuietly(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private SortPomLogger createDummyLog() {
        return new SortPomLogger() {

            @Override
            public void info(String content) {
                infoLogger.add("[INFO] " + content);
            }

            @Override
            public void warn(String content) {
                infoLogger.add("[WARNING] " + content);
            }

            @Override
            public void error(String content) {
                infoLogger.add("[ERROR] " + content);
            }
        };
    }

}
