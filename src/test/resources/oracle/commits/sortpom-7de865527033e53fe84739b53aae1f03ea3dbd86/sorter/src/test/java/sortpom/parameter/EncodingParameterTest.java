package sortpom.parameter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import sortpom.XmlOutputGenerator;
import sortpom.exception.FailureException;
import sortpom.util.FileUtil;
import sortpom.util.SortPomImplUtil;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static sortpom.sort.XmlFragment.createXmlFragment;

class EncodingParameterTest {

    @Test
    void illegalEncodingWhenGettingPomFileShouldNotWork() {

        final Executable testMethod = () -> SortPomImplUtil.create()
                .encoding("gurka-2000")
                .testFiles("/Xml_deviations_input.xml", "/Xml_deviations_output.xml");

        final FailureException thrown = assertThrows(FailureException.class, testMethod);

        assertThat(thrown.getMessage(), is(equalTo("Could not handle encoding: gurka-2000")));
    }

    @Test
    void illegalEncodingWhenGeneratingPomFileShouldWork() {
        XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator();
        xmlOutputGenerator.setup(PluginParameters.builder()
                .setEncoding("gurka-2000")
                .setFormatting("\n", true, true, false)
                .setIndent(2, false, false)
                .build());

        String actual = xmlOutputGenerator.getSortedXml(createXmlFragment());
        assertThat(actual, is("<?xml version=\"1.0\" encoding=\"gurka-2000\"?>\n<Gurka></Gurka>\n"));
    }

    @Test
    void illegalEncodingWhenSavingPomFileShouldNotWork() throws IOException {
        File pomFileTemp = File.createTempFile("pom", ".xml", new File("target"));
        pomFileTemp.deleteOnExit();

        FileUtil fileUtil = new FileUtil();
        PluginParameters.Builder builder = PluginParameters.builder();

        builder.setPomFile(pomFileTemp);
        builder.setEncoding("gurka-2000");
        fileUtil.setup(builder.build());

        final Executable testMethod = () -> fileUtil.savePomFile("<?xml version=\"1.0\" encoding=\"gurka-2000\"?>\n<Gurka></Gurka>\n");

        final RuntimeException thrown = assertThrows(RuntimeException.class, testMethod);

        assertThat(thrown.getCause().getClass().getName(), is("java.io.UnsupportedEncodingException"));
        assertThat(thrown.getCause().getMessage(), is("gurka-2000"));
    }

    @Test
    void differentEncodingShouldWork1() throws Exception {
        SortPomImplUtil.create()
                .encoding("UTF-32BE")
                .testFiles("/UTF32Encoding_input.xml", "/UTF32Encoding_expected.xml");
    }

    @Test
    void differentEncodingShouldWork2() throws Exception {
        SortPomImplUtil.create()
                .encoding("UTF-16")
                .testFiles("/UTF16Encoding_input.xml", "/UTF16Encoding_expected.xml");
    }

    @Test
    void differentEncodingShouldWork3() throws Exception {
        SortPomImplUtil.create()
                .encoding("ISO-8859-1")
                .testFiles("/ISO88591Encoding_input.xml", "/ISO88591Encoding_expected.xml");
    }

}
