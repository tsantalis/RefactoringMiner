package sortpom;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import sortpom.exception.ExceptionConverter;
import sortpom.logger.MavenLogger;
import sortpom.parameter.PluginParameters;

/**
 * Verifies that the pom.xml is sorted. If the verification fails then the pom.xml is sorted.
 *
 * @author Bjorn Ekryd
 */
@Mojo(name = "verify", threadSafe = true, defaultPhase = LifecyclePhase.VALIDATE)
@SuppressWarnings({"UnusedDeclaration"})
public class VerifyMojo extends AbstractParentMojo {

    /**
     * What should happen if verification fails. Can be either 'sort', 'warn' or 'stop'
     */
    @Parameter(property = "sort.verifyFail", defaultValue = "sort")
    private String verifyFail;

    /**
     * What kind of differences should trigger verify failure. Can be either 'xmlElements' or 'strict'. Can be combined
     * with ignoreLineSeparators
     */
    @Parameter(property = "sort.verifyFailOn", defaultValue = "xmlElements")
    private String verifyFailOn;

    /**
     * Saves the verification failure to an external xml file, recommended filename is 'target/sortpom_reports/violation.xml'.
     */
    @Parameter(property = "sort.violationFilename")
    private String violationFilename;

    public void setup() throws MojoFailureException {
        new ExceptionConverter(() -> {

            PluginParameters pluginParameters = PluginParameters.builder()
                    .setPomFile(pomFile)
                    .setFileOutput(createBackupFile, backupFileExtension, violationFilename, keepTimestamp)
                    .setEncoding(encoding)
                    .setFormatting(lineSeparator, expandEmptyElements, spaceBeforeCloseEmptyElement, keepBlankLines)
                    .setIndent(nrOfIndentSpace, indentBlankLines, indentSchemaLocation)
                    .setSortOrder(sortOrderFile, predefinedSortOrder)
                    .setSortEntities(sortDependencies, sortDependencyExclusions, sortPlugins, sortProperties, sortModules, sortExecutions)
                    .setVerifyFail(verifyFail, verifyFailOn)
                    .build();

            sortPomImpl.setup(new MavenLogger(getLog()), pluginParameters);

        }).executeAndConvertException();
    }

    protected void sortPom() throws MojoFailureException {
        new ExceptionConverter(sortPomImpl::verifyPom).executeAndConvertException();
    }

}
