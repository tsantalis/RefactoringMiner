package sortpom.parameter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sortpom.XmlOutputGenerator;
import sortpom.util.FileUtil;
import sortpom.util.SortPomImplUtil;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static sortpom.sort.ExpandEmptyElementTest.createXmlFragment;

public class EncodingParameterTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void illegalEncodingWhenGettingPomFileShouldNotWork() throws Exception {
        thrown.expectMessage("gurka-2000");

        SortPomImplUtil.create()
                .encoding("gurka-2000")
                .testFiles("/Xml_deviations_input.xml", "/Xml_deviations_output.xml");
    }

    @Test
    public void illegalEncodingWhenGeneratingPomFileShouldWork() {
        XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator();
        xmlOutputGenerator.setup(PluginParameters.builder()
                .setEncoding("gurka-2000")
                .setFormatting("\n", true, false)
                .setIndent(2, false)
                .build());

        String actual = xmlOutputGenerator.getSortedXml(createXmlFragment());
        assertThat(actual, is("<?xml version=\"1.0\" encoding=\"gurka-2000\"?>\n<Gurka></Gurka>\n"));
    }

    @Test
    public void illegalEncodingWhenSavingPomFileShouldNotWork() throws IOException {
        File pomFileTemp = File.createTempFile("pom", ".xml", new File("target"));
        pomFileTemp.deleteOnExit();

        FileUtil fileUtil = new FileUtil();
        PluginParameters.Builder builder = PluginParameters.builder();

        builder.setPomFile(pomFileTemp);
        builder.setEncoding("gurka-2000");
        fileUtil.setup(builder.build());

        try {
            fileUtil.savePomFile("<?xml version=\"1.0\" encoding=\"gurka-2000\"?>\n<Gurka></Gurka>\n");
        } catch (Exception ex) {
            assertThat(ex.getCause().getClass().getName(), is("java.io.UnsupportedEncodingException"));
            assertThat(ex.getCause().getMessage(), is("gurka-2000"));
        }
    }

    @Test
    public void differentEncodingShouldWork1() throws Exception {
        SortPomImplUtil.create()
                .encoding("UTF-32BE")
                .testFiles("/UTF32Encoding_input.xml", "/UTF32Encoding_expected.xml");
    }

    @Test
    public void differentEncodingShouldWork2() throws Exception {
        SortPomImplUtil.create()
                .encoding("UTF-16")
                .testFiles("/UTF16Encoding_input.xml", "/UTF16Encoding_expected.xml");
    }

    @Test
    public void differentEncodingShouldWork3() throws Exception {
        SortPomImplUtil.create()
                .encoding("ISO-8859-1")
                .testFiles("/ISO88591Encoding_input.xml", "/ISO88591Encoding_expected.xml");
    }
}
