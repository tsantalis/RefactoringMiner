package sortpom.util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import refutils.ReflectionHelper;
import sortpom.exception.FailureException;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class FileUtilExceptionsTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private File backupFileTemp;
    private File pomFileTemp;

    @Before
    public void setup() throws IOException {
        pomFileTemp = File.createTempFile("pom", ".xml", new File("target"));
        pomFileTemp.deleteOnExit();
        backupFileTemp = File.createTempFile("backupFile", ".xml", new File("target"));
        backupFileTemp.deleteOnExit();
    }

    @Test
    public void whenOldBackupFileCannotBeDeletedAnExceptionShouldBeThrown() {
        FileUtil fileUtil = createFileUtil();
        doNotAccessRealBackupFile(fileUtil);

        //Set backup file to a directory (which raises DirectoryNotEmptyException)
        new ReflectionHelper(fileUtil).setField("backupFile", backupFileTemp.getParentFile());

        thrown.expect(FailureException.class);
        thrown.expectMessage("Could not remove old backup file, filename: backupFileName");

        fileUtil.backupFile();
    }

    private void doNotAccessRealBackupFile(FileUtil fileUtil) {
        doNothing().when(fileUtil).createFileHandle();
    }

    @Test
    public void whenSourceFileCannotBeCopiedAnExceptionShouldBeThrown() {
        assertTrue(pomFileTemp.delete());

        FileUtil fileUtil = createFileUtil();

        thrown.expect(FailureException.class);
        thrown.expectMessage("Could not create backup file to filename: " + pomFileTemp.getAbsolutePath() + ".bak");

        fileUtil.backupFile();
    }

    @Test
    public void whenPomFileCannotBeReadAnExceptionShouldBeThrown() {
        assertTrue(pomFileTemp.delete());

        FileUtil fileUtil = createFileUtil();
        new ReflectionHelper(fileUtil).setField("pomFile", pomFileTemp);

        thrown.expect(FailureException.class);
        thrown.expectMessage("Could not read pom file: " + pomFileTemp.getAbsolutePath());

        fileUtil.getPomFileContent();
    }

    @Test
    public void whenPomFileHasWrongEncodingAnExceptionShouldBeThrown() {
        FileUtil fileUtil = createFileUtil();

        new ReflectionHelper(fileUtil).setField("encoding", "gurka-2000");

        thrown.expect(FailureException.class);
        thrown.expectMessage("Could not handle encoding: gurka-2000");

        fileUtil.getPomFileContent();
    }

    @Test
    public void whenPomFileCannotBeSavedAnExceptionShouldBeThrown() {
        assertTrue(pomFileTemp.setReadOnly());

        FileUtil fileUtil = createFileUtil();

        thrown.expect(FailureException.class);
        thrown.expectMessage("Could not save sorted pom file: " + pomFileTemp.getAbsolutePath());

        fileUtil.savePomFile("Whatever");
        assertTrue(pomFileTemp.setReadable(true));
    }

    @Test
    public void whenPomFileTimestampCannotBeRetrievedAnExceptionShouldBeThrown() {
      FileUtil fileUtil = createFileUtil();
      new ReflectionHelper(fileUtil).setField("keepTimestamp", true);

      thrown.expect(FailureException.class);
      thrown.expectMessage("Cound not retrieve the timestamp of the pom file: " + pomFileTemp.getAbsolutePath());

      fileUtil.getPomFileContent();
    }

    @Test
    public void whenPomFileTimestampCannotBeSetAnExceptionShouldBeThrown() {
      FileUtil fileUtil = createFileUtil();
      new ReflectionHelper(fileUtil).setField("keepTimestamp", true);

      thrown.expect(FailureException.class);
      thrown.expectMessage("Could not change timestamp of new pom file: " + pomFileTemp.getAbsolutePath());

      fileUtil.savePomFile("Whatever");
    }

    private FileUtil createFileUtil() {
        FileUtil originalFileUtil = new FileUtil();

        ReflectionHelper helper = new ReflectionHelper(originalFileUtil);
        helper.setField("pomFile", pomFileTemp);
        helper.setField("newName", "backupFileName");
        helper.setField("backupFileExtension", ".bak");
        helper.setField("encoding", "UTF-8");
        helper.setField(new FileAttributeUtilStub());
        return spy(originalFileUtil);
    }

    private class FileAttributeUtilStub extends FileAttributeUtil {
        @Override
        public long getLastModifiedTimestamp(File file) {
          return 0;
        }

        @Override
        public void setTimestamps(File file, long millis) throws IOException {
          throw new IOException();
        }
      }
}
