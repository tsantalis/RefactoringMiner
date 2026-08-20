package sortpom;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Common parent for both SortMojo and VerifyMojo
 */
abstract class AbstractParentMojo extends AbstractMojo {

    /**
     * This is the File instance that refers to the location of the pom that
     * should be sorted.
     */
    @Parameter(property = "sort.pomFile", defaultValue = "${project.file}")
    File pomFile;

    /**
     * Should a backup copy be created for the sorted pom.
     */
    @Parameter(property = "sort.createBackupFile", defaultValue = "true")
    boolean createBackupFile;

    /**
     * Name of the file extension for the backup file.
     */
    @Parameter(property = "sort.backupFileExtension", defaultValue = ".bak")
    String backupFileExtension;

    /**
     * Encoding for the files.
     */
    @Parameter(property = "sort.encoding", defaultValue = "UTF-8")
    String encoding;

    /**
     * Line separator for sorted pom. Can be either \n, \r or \r\n
     */
    @Parameter(property = "sort.lineSeparator", defaultValue = "${line.separator}")
    String lineSeparator;

    /**
     * Should empty xml elements be expanded or not. Example:
     * &lt;configuration&gt;&lt;/configuration&gt; or &lt;configuration/&gt;
     */
    @Parameter(property = "sort.expandEmptyElements", defaultValue = "true")
    boolean expandEmptyElements;

    /**
     * Should blank lines in the pom-file be preserved. A maximum of one line is preserved between each tag.
     */
    @Parameter(property = "sort.keepBlankLines", defaultValue = "false")
    boolean keepBlankLines;

    /**
     * Number of space characters to use as indentation. A value of -1 indicates
     * that tab character should be used instead.
     */
    @Parameter(property = "sort.nrOfIndentSpace", defaultValue = "2")
    int nrOfIndentSpace;

    /**
     * Should blank lines (if preserved) have indentation.
     */
    @Parameter(property = "sort.indentBlankLines", defaultValue = "false")
    boolean indentBlankLines;

    /**
     * Choose between a number of predefined sort order files.
     */
    @Parameter(property = "sort.predefinedSortOrder")
    String predefinedSortOrder;

    /**
     * Custom sort order file.
     */
    @Parameter(property = "sort.sortOrderFile")
    String sortOrderFile;

    /**
     * Comma-separated ordered list how dependencies should be sorted. Example: scope,groupId,artifactId.
     * If scope is specified in the list then the scope ranking is COMPILE, PROVIDED, SYSTEM, RUNTIME, IMPORT and TEST.
     * The list can be separated by ",;:"
     */
    @Parameter(property = "sort.sortDependencies")
    String sortDependencies;

    /**
     * Comma-separated ordered list how exclusions, for dependencies, should be sorted. Example: groupId,artifactId
     * The list can be separated by ",;:"
     */
    @Parameter(property = "sort.sortDependencyExclusions")
    String sortDependencyExclusions;

    /**
     * Comma-separated ordered list how plugins should be sorted. Example: groupId,artifactId
     * The list can be separated by ",;:"
     */
    @Parameter(property = "sort.sortPlugins")
    String sortPlugins;

    /**
     * Should the Maven pom properties be sorted alphabetically. Affects both
     * project/properties and project/profiles/profile/properties
     */
    @Parameter(property = "sort.sortProperties", defaultValue = "false")
    boolean sortProperties;

    /**
     * Should the Maven pom sub modules be sorted alphabetically.
     */
    @Parameter(property = "sort.sortModules", defaultValue = "false")
    boolean sortModules;

    /**
     * Set this to 'true' to bypass sortpom plugin
     */
    @Parameter(property = "sort.skip", defaultValue = "false")
    boolean skip;

    /**
     * Whether to keep the file timestamps of old POM file when creating new POM file.
     */
    @Parameter(property = "sort.keepTimestamp", defaultValue = "false")
    boolean keepTimestamp;

    final SortPomImpl sortPomImpl = new SortPomImpl();

    /**
     * Execute plugin.
     *
     * @throws org.apache.maven.plugin.MojoFailureException exception that will be handled by plugin framework
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoFailureException {
        if (skip) {
            getLog().info("Skipping Sortpom");
        } else {
            setup();
            sortPom();
        }

    }

    protected abstract void sortPom() throws MojoFailureException;

    protected abstract void setup() throws MojoFailureException;
}
