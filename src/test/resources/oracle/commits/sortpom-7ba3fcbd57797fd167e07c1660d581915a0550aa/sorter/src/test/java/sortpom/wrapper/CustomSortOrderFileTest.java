package sortpom.wrapper;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.junit.Test;
import sortpom.parameter.PluginParameters;
import sortpom.util.FileUtil;
import sortpom.wrapper.operation.HierarchyRootWrapper;

import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author bjorn
 * @since 2018-04-12
 */
public class CustomSortOrderFileTest {
    private static final String UTF_8 = "UTF-8";

    @Test
    public void compareDefaultSortOrderFileToString() throws IOException, JDOMException {
        String expected = IOUtils.toString(new FileInputStream("src/test/resources/sortOrderFiles/with_newline_tagsToString.txt"), UTF_8);
        assertEquals(expected, getToStringOnCustomSortOrderFile());
    }

    private String getToStringOnCustomSortOrderFile() throws IOException, JDOMException {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setPomFile(null).setFileOutput(false, ".bak", null, false)
                .setEncoding("UTF-8")
                .setFormatting("\r\n", true, true)
                .setIndent(2, false)
                .setSortOrder("src/test/resources/sortOrderFiles/with_newline_tags.xml", null)
                .setSortEntities("scope,groupId,artifactId", "groupId,artifactId", "groupId,artifactId", true, true).build();

        FileUtil fileUtil = new FileUtil();
        fileUtil.setup(pluginParameters);

        WrapperFactoryImpl wrapperFactory = new WrapperFactoryImpl(fileUtil);

        Document documentFromDefaultSortOrderFile = wrapperFactory.createDocumentFromDefaultSortOrderFile();
        new HierarchyRootWrapper(wrapperFactory.create(documentFromDefaultSortOrderFile.getRootElement()));

        HierarchyRootWrapper rootWrapper = new HierarchyRootWrapper(wrapperFactory.create(documentFromDefaultSortOrderFile.getRootElement()));
        rootWrapper.createWrappedStructure(wrapperFactory);

        return rootWrapper.toString();
    }


}
