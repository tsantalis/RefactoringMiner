package sortpom.sort;

import org.junit.Before;
import org.junit.Test;
import refutils.ReflectionHelper;
import sortpom.SortMojo;
import sortpom.SortPomImpl;
import sortpom.XmlOutputGenerator;
import sortpom.util.FileUtil;
import sortpom.wrapper.ElementWrapperCreator;
import sortpom.wrapper.TextWrapperCreator;
import sortpom.wrapper.WrapperFactoryImpl;

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class SortMojoParametersTest {
    private final File pomFile = mock(File.class);

    private SortPomImpl sortPomImpl;
    private FileUtil fileUtil;
    private SortMojo sortMojo;
    private ElementWrapperCreator elementWrapperCreator;
    private TextWrapperCreator textWrapperCreator;
    private XmlOutputGenerator xmlOutputGenerator;

    @Before
    public void setup() throws SecurityException, IllegalArgumentException {
        sortMojo = new SortMojo();
        new ReflectionHelper(sortMojo).setField("lineSeparator", "\n");
        new ReflectionHelper(sortMojo).setField("encoding", "UTF-8");

        sortPomImpl = new ReflectionHelper(sortMojo).getField(SortPomImpl.class);
        ReflectionHelper sortPomImplHelper = new ReflectionHelper(sortPomImpl);
        fileUtil = sortPomImplHelper.getField(FileUtil.class);
        xmlOutputGenerator = sortPomImplHelper.getField(XmlOutputGenerator.class);
        WrapperFactoryImpl wrapperFactoryImpl = sortPomImplHelper.getField(WrapperFactoryImpl.class);
        elementWrapperCreator = new ReflectionHelper(wrapperFactoryImpl).getField(ElementWrapperCreator.class);
        textWrapperCreator = new ReflectionHelper(wrapperFactoryImpl).getField(TextWrapperCreator.class);
    }

    @Test
    public void pomFileParameter() throws Exception {
        testParameterMoveFromMojoToRestOfApplication("pomFile", pomFile, sortPomImpl, fileUtil);
    }

    @Test
    public void createBackupFileParameter() throws Exception {
        testParameterMoveFromMojoToRestOfApplicationForBoolean("createBackupFile", sortPomImpl);
    }

    @Test
    public void keepTimestampParameter() throws Exception {
    	testParameterMoveFromMojoToRestOfApplicationForBoolean("keepTimestamp", fileUtil);
    }

    @Test
    public void backupFileExtensionParameter() throws Exception {
        testParameterMoveFromMojoToRestOfApplication("backupFileExtension", ".gurka", sortPomImpl, fileUtil);
    }

    @Test
    public void encodingParameter() throws Exception {
        testParameterMoveFromMojoToRestOfApplication("encoding", "GURKA-2000", fileUtil, sortPomImpl, xmlOutputGenerator);
    }

    @Test
    public void lineSeparatorParameter() throws Exception {
        testParameterMoveFromMojoToRestOfApplication("lineSeparator", "\r");

        assertEquals("\r", new ReflectionHelper(xmlOutputGenerator).getField("lineSeparatorUtil").toString());
    }

    @Test
    public void parameterNrOfIndentSpaceShouldEndUpInXmlProcessor() throws Exception {
        testParameterMoveFromMojoToRestOfApplication("nrOfIndentSpace", 6);

        assertEquals("      ", new ReflectionHelper(xmlOutputGenerator).getField("indentCharacters"));
    }

    @Test
    public void expandEmptyElementsParameter() throws Exception {
        testParameterMoveFromMojoToRestOfApplicationForBoolean("expandEmptyElements", xmlOutputGenerator);
    }

    @Test
    public void predefinedSortOrderParameter() throws Exception {
        testParameterMoveFromMojoToRestOfApplication("predefinedSortOrder", "tomatoSort", fileUtil);
    }

    @Test
    public void parameterSortOrderFileShouldEndUpInFileUtil() throws Exception {
        testParameterMoveFromMojoToRestOfApplication("sortOrderFile", "sortOrderFile.gurka");

        Object actual = new ReflectionHelper(fileUtil).getField("customSortOrderFile");
        assertEquals("sortOrderFile.gurka", actual);
    }

    @Test
    public void parameterSortDependenciesShouldEndUpInElementWrapperCreator() throws Exception {
        testParameterMoveFromMojoToRestOfApplication("sortDependencies", "groupId,scope");

        Object sortDependencies = new ReflectionHelper(elementWrapperCreator).getField("sortDependencies");
        assertThat(sortDependencies.toString(), is("DependencySortOrder{childElementNames=[groupId, scope]}"));
    }

    @Test
    public void parameterSortDependencyExclusionsShouldEndUpInElementWrapperCreator() throws Exception {
        testParameterMoveFromMojoToRestOfApplication("sortDependencyExclusions", "groupId,scope");

        Object sortDependencyExclusions = new ReflectionHelper(elementWrapperCreator).getField("sortDependencyExclusions");
        assertThat(sortDependencyExclusions.toString(), is("DependencySortOrder{childElementNames=[groupId, scope]}"));
    }

    @Test
    public void parameterSortPluginsShouldEndUpInWrapperFactoryImpl() throws Exception {
        testParameterMoveFromMojoToRestOfApplication("sortPlugins", "alfa,beta");

        Object sortPlugins = new ReflectionHelper(elementWrapperCreator).getField("sortPlugins");
        assertThat(sortPlugins.toString(), is("DependencySortOrder{childElementNames=[alfa, beta]}"));
    }

    @Test
    public void parameterSortModulesShouldEndUpInWrapperFactoryImpl() throws Exception {
        testParameterMoveFromMojoToRestOfApplicationForBoolean("sortModules", elementWrapperCreator);
    }

    @Test
    public void parameterSortPropertiesShouldEndUpInWrapperFactoryImpl() throws Exception {
        testParameterMoveFromMojoToRestOfApplicationForBoolean("sortProperties", elementWrapperCreator);
    }

    @Test
    public void parameterKeepBlankLineShouldEndUpInXmlProcessor() throws Exception {
        testParameterMoveFromMojoToRestOfApplicationForBoolean("keepBlankLines", textWrapperCreator);
    }

    @Test
    public void parameterIndentBlankLineShouldEndUpInXmlProcessor() throws Exception {
        testParameterMoveFromMojoToRestOfApplicationForBoolean("indentBlankLines", xmlOutputGenerator);
    }

    private void testParameterMoveFromMojoToRestOfApplication(String parameterName, Object parameterValue,
                                                              Object... whereParameterCanBeFound) throws
            Exception {
        new ReflectionHelper(sortMojo).setField(parameterName, parameterValue);

        sortMojo.setup();

        for (Object someInstanceThatContainparameter : whereParameterCanBeFound) {
            Object actual = new ReflectionHelper(someInstanceThatContainparameter).getField(parameterName);
            assertSame(parameterValue, actual);
        }
    }

    private void testParameterMoveFromMojoToRestOfApplicationForBoolean(String parameterName,
                                                                        Object... whereParameterCanBeFound) throws
            Exception {
        new ReflectionHelper(sortMojo).setField(parameterName, true);

        sortMojo.setup();

        for (Object someInstanceThatContainparameter : whereParameterCanBeFound) {
            Object actual = new ReflectionHelper(someInstanceThatContainparameter).getField(parameterName);
            assertEquals(true, actual);
        }
    }

}
