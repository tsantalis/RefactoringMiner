package ninja.javafx.smartcsv.files;

import ninja.javafx.smartcsv.FileReader;
import ninja.javafx.smartcsv.FileWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class FileStorageTest {

    private FileReader<String> reader;
    private FileWriter<String> writer;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // subject under test
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private FileStorage<String> sut;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // init
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @BeforeEach
    public void initialize() {
        reader = mock(FileReader.class);
        writer = mock(FileWriter.class);
        sut = new FileStorage<>(reader, writer);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // tests
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Test
    public void setFile_ShouldStoreFile() {

        // pre execution assertion
        assertThat("get file returns null",  sut.getFile(), nullValue());
        assertThat("property is null", sut.fileProperty().isNull().get(), equalTo(true));

        // execution
        final File file = new File("file");
        sut.setFile(file);

        // assertion
        assertThat("get file returns the file", sut.getFile(), sameInstance(file));
        assertThat("property is not null", sut.fileProperty().isNull().get(), equalTo(false));
    }

    @Test
    public void load_ShouldReadContentFromReader() throws Exception {
        // setup
        final File file = new File("file");
        sut.setFile(file);
        when(reader.getContent()).thenReturn("CONTENT");

        // execution
        sut.load();

        // assertion
        verify(reader).read(file);
        assertThat("content is set", sut.getContent(), equalTo("CONTENT"));
    }

    @Test
    public void load_ShouldResetFileChangedProperty() throws Exception {
        // setup
        sut.setFileChanged(true);
        when(reader.getContent()).thenReturn("CONTENT");

        // execution
        sut.load();

        // assertion
        assertThat("file changed is reset", sut.isFileChanged(), equalTo(false));
    }

    @Test
    public void save_ShouldWriteContentToWriter() throws Exception {
        // setup
        final String content = "CONTENT";
        sut.setContent(content);
        final File file = new File("file");
        sut.setFile(file);

        // execution
        sut.save();

        // assertion
        verify(writer).setContent(content);
        verify(writer).write(file);
    }

    @Test
    public void save_ShouldResetFileChangedProperty() throws Exception {
        // setup
        sut.setFileChanged(true);

        // execution
        sut.save();

        // assertion
        assertThat("file changed is reset", sut.isFileChanged(), equalTo(false));
    }

}
