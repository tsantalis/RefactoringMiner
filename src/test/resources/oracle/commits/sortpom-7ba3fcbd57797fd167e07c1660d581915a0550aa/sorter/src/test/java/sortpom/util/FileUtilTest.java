package sortpom.util;

import org.junit.Assert;
import org.junit.Test;
import sortpom.parameter.PluginParameters;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author bjorn
 * @since 2013-08-16
 */
public class FileUtilTest {
    @Test
    public void defaultSortOrderFromFileShouldWork() throws Exception {
        FileUtil fileUtil = createFileUtil("Attribute_expected.xml");

        String defaultSortOrderXml = fileUtil.getDefaultSortOrderXml();
        Assert.assertThat(defaultSortOrderXml, startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
                "<projec"));
    }


    @Test
    public void defaultSortOrderFromNonExistingShouldThrowException() {
        FileUtil fileUtil = createFileUtil("zzz_Attribute_expected.xml");

        try {
            fileUtil.getDefaultSortOrderXml();
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage(), startsWith("Could not find"));
            assertThat(e.getMessage(), endsWith("or zzz_Attribute_expected.xml in classpath"));
        }
    }

    @Test
    public void defaultSortOrderFromUrlShouldWork() throws IOException {
        FileUtil fileUtil = createFileUtil("https://en.wikipedia.org/wiki/Sweden");

        try {
        String defaultSortOrderXml = fileUtil.getDefaultSortOrderXml();
            Assert.assertThat(defaultSortOrderXml, containsString("Sverige"));
        } catch (UnknownHostException e) {
            // This is ok, we were not online when the test was perfomed
            // Which actually makes this test a bit pointless :-(
        }
    }

    @Test
    public void defaultSortOrderFromNonExistingHostShouldThrowException() throws IOException {
        FileUtil fileUtil = createFileUtil("http://jgerwzuujy.fjrmzaxklj.zfgmqavbhp/licenses/BSD-3-Clause");

        try {
            fileUtil.getDefaultSortOrderXml();
            fail();
        } catch (UnknownHostException e) {
            assertThat(e.getMessage(), is("jgerwzuujy.fjrmzaxklj.zfgmqavbhp"));
        }
    }

    @Test
    public void defaultSortOrderFromNonExistingPageShouldThrowException() throws IOException {
        FileUtil fileUtil = createFileUtil("https://github.com/Ekryd/sortpom/where_are_the_donations");

        try {
            fileUtil.getDefaultSortOrderXml();
            fail();
        } catch (UnknownHostException e) {
            // This is ok, we were not online when the test was performed
        } catch (FileNotFoundException e) {
            assertThat(e.getMessage(), is("https://github.com/Ekryd/sortpom/where_are_the_donations"));
        }
    }

    private FileUtil createFileUtil(String customSortOrderFile) {
        FileUtil fileUtil = new FileUtil();
        PluginParameters pluginParameters = PluginParameters.builder()
                .setSortOrder(customSortOrderFile, null)
                .setEncoding("UTF-8")
                .build();
        fileUtil.setup(pluginParameters);
        return fileUtil;
    }
}
