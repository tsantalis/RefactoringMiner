package sortpom;

import org.jdom.Document;
import org.jdom.JDOMException;
import sortpom.exception.FailureException;
import sortpom.logger.SortPomLogger;
import sortpom.parameter.PluginParameters;
import sortpom.processinstruction.XmlProcessingInstructionParser;
import sortpom.util.FileUtil;
import sortpom.util.XmlOrderedResult;
import sortpom.wrapper.WrapperFactoryImpl;

import java.io.*;

/**
 * The implementation of the Mojo (Maven plugin) that sorts the pom file for a
 * maven project.
 *
 * @author Bjorn Ekryd
 */
public class SortPomService {
    private final FileUtil fileUtil;
    private final XmlProcessor xmlProcessor;
    private final WrapperFactoryImpl wrapperFactory;
    private final XmlProcessingInstructionParser xmlProcessingInstructionParser;
    private final XmlOutputGenerator xmlOutputGenerator;

    private SortPomLogger log;
    private File pomFile;
    private String encoding;
    private String backupFileExtension;
    private boolean ignoreLineSeparators;
    private String violationFilename;
    private boolean createBackupFile;

    private String originalXml;
    private String sortedXml;

    /**
     * Instantiates a new sort pom mojo and initiates dependencies to other
     * classes.
     */
    public SortPomService() {
        this.fileUtil = new FileUtil();
        this.wrapperFactory = new WrapperFactoryImpl(fileUtil);
        this.xmlProcessor = new XmlProcessor(wrapperFactory);
        this.xmlProcessingInstructionParser = new XmlProcessingInstructionParser();
        this.xmlOutputGenerator = new XmlOutputGenerator();
    }

    public void setup(SortPomLogger log, PluginParameters pluginParameters) {
        this.log = log;
        fileUtil.setup(pluginParameters);
        wrapperFactory.setup(pluginParameters);
        xmlProcessingInstructionParser.setup(log);
        xmlOutputGenerator.setup(pluginParameters);

        this.pomFile = pluginParameters.pomFile;
        this.encoding = pluginParameters.encoding;
        this.backupFileExtension = pluginParameters.backupFileExtension;
        this.ignoreLineSeparators = pluginParameters.ignoreLineSeparators;
        this.violationFilename = pluginParameters.violationFilename;
        this.createBackupFile = pluginParameters.createBackupFile;
    }

    /** Fetches and sorts the original xml. */
    void sortOriginalXml() {
        originalXml = fileUtil.getPomFileContent();
        xmlProcessingInstructionParser.scanForIgnoredSections(originalXml);
        String xml = xmlProcessingInstructionParser.replaceIgnoredSections();

        try (ByteArrayInputStream originalXmlInputStream = new ByteArrayInputStream(xml.getBytes(encoding))) {
            xmlProcessor.setOriginalXml(originalXmlInputStream);
        } catch (JDOMException | IOException e) {
            throw new FailureException("Could not sort " + pomFile.getAbsolutePath() + " content: " + xml, e);
        }
        xmlProcessor.sortXml();
    }

    /** Generates the sorted XML */
    void generateSortedXml() {
        if (sortedXml != null) {
            return;
        }
        sortedXml = xmlOutputGenerator.getSortedXml(xmlProcessor.getNewDocument());
        if (xmlProcessingInstructionParser.existsIgnoredSections()) {
            sortedXml = xmlProcessingInstructionParser.revertIgnoredSections(sortedXml);
        }
    }

    /** Creates the backup file for pom. */
    void createBackupFile() {
        if (!createBackupFile) {
            return;
        }
        if (backupFileExtension.trim().length() == 0) {
            throw new FailureException("Could not create backup file, extension name was empty");
        }
        fileUtil.backupFile();
        log.info(String.format("Saved backup of %s to %s%s", pomFile.getAbsolutePath(),
                pomFile.getAbsolutePath(), backupFileExtension));
    }

    void saveGeneratedXml() {
        fileUtil.savePomFile(sortedXml);
    }


    XmlOrderedResult isOriginalXmlStringSorted() {
        try (BufferedReader originalXmlReader = new BufferedReader(new StringReader(originalXml));
             BufferedReader sortedXmlReader = new BufferedReader(new StringReader(sortedXml))) {
            String originalXmlLine = originalXmlReader.readLine();
            String sortedXmlLine = sortedXmlReader.readLine();
            int line = 1;

            while (originalXmlLine != null && sortedXmlLine != null) {
                if (!originalXmlLine.equals(sortedXmlLine)) {
                    return XmlOrderedResult.lineDiffers(line, "'" + sortedXmlLine + "'");
                }
                line++;
                originalXmlLine = originalXmlReader.readLine();
                sortedXmlLine = sortedXmlReader.readLine();
            }
            if (originalXmlLine != null || sortedXmlLine != null) {
                return XmlOrderedResult.lineDiffers(line, sortedXmlLine == null ? "empty" : "'" + sortedXmlLine + "'");
            }
        } catch (IOException ioex) {
            throw new FailureException(ioex.getMessage(), ioex);
        }
        if (ignoreLineSeparators || originalXml.equals(sortedXml)) {
            return XmlOrderedResult.ordered();
        }
        return XmlOrderedResult.lineSeparatorCharactersDiffer();
    }

    XmlOrderedResult isOriginalXmlElementsSorted() {
        return xmlProcessor.isXmlOrdered();
    }

    void saveViolationFile(XmlOrderedResult xmlOrderedResult) {
        if (violationFilename != null) {
            log.info("Saving violation report to " + new File(violationFilename).getAbsolutePath());
            ViolationXmlProcessor violationXmlProcessor = new ViolationXmlProcessor();
            Document document = violationXmlProcessor.createViolationXmlContent(pomFile, xmlOrderedResult.getErrorMessage());
            String violationXmlString = xmlOutputGenerator.getSortedXml(document);
            fileUtil.saveViolationFile(violationXmlString);
        }
    }

}
