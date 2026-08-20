package sortpom.wrapper;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;
import sortpom.parameter.PluginParameters;
import sortpom.util.FileUtil;
import sortpom.wrapper.operation.HierarchyRootWrapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomSortOrderFileTest {
    @Test
    void compareDefaultSortOrderFileToString() throws IOException, JDOMException {
        String expected = IOUtils.toString(new FileInputStream("src/test/resources/sortOrderFiles/with_newline_tagsToString.txt"), StandardCharsets.UTF_8);
        assertEquals(expected, getToStringOnCustomSortOrderFile());
    }

    private String getToStringOnCustomSortOrderFile() throws IOException, JDOMException {
        PluginParameters pluginParameters = PluginParameters.builder()
                .setPomFile(null).setFileOutput(false, ".bak", null, false)
                .setEncoding("UTF-8")
                .setFormatting("\r\n", true, true, true)
                .setIndent(2, false, false)
                .setSortOrder("src/test/resources/sortOrderFiles/with_newline_tags.xml", null)
                .setSortEntities("scope,groupId,artifactId", "groupId,artifactId", "groupId,artifactId", true, true, true).build();

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
