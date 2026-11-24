package de.uni_mannheim.informatik.dws.melt.matching_eval_client;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {


    private static final Logger LOGGER = LoggerFactory.getLogger(MainTest.class);

    /**
     * Quickly show the help / make sure no exceptions are thrown.
     */
    @Test
    void showHelp() {
        Main.main(new String[]{"-h"});
    }

    /**
     * Make sure not exceptions are thrown.
     */
    @Test
    void noParameters() {
        Main.main(null);
    }

    /**
     * Simple SEALS evaluation.
     * This requires that the JRE is Java 8.
     */
    @Test
    @Tag("Java8")
    void evaluateSingleSealsFileOnJava8() {
        String resultsDirectoryPath = "./cli_results";
        File resultsDirectory = new File(resultsDirectoryPath);
        Main.main(new String[]{"-s",
                loadFile("simpleSealsMatcher-1.0-seals_external.zip").getAbsolutePath(),
                "-t", "http://oaei.webdatacommons.org/tdrs/", "conference", "conference-v1",
                "-r", resultsDirectoryPath
        });

        // check directory
        assertTrue(resultsDirectory.exists());

        // check single file in directory
        File trackPerformanceCubeFile = new File(resultsDirectory, "trackPerformanceCube.csv");
        assertTrue(trackPerformanceCubeFile.exists());
    }

    @AfterAll
    static void cleanUp(){
        deleteDirectory("./cli_results");
    }

    static void deleteDirectory(String dirPath){
        try {
            FileUtils.deleteDirectory(new File(dirPath));
        } catch (IOException ioe){
            LOGGER.error("Could not delete directory " + dirPath);
        }
    }

    /**
     * Simple SEALS evaluation.
     */
    @Test
    @Disabled
    void evaluateSingleSealsFile() {
        Main.main(new String[]{"-s",
                loadFile("simpleSealsMatcher-1.0-seals_external.zip").getAbsolutePath(),
                "-t", "http://oaei.webdatacommons.org/tdrs/", "conference", "conference-v1",
                "-j", "/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/bin/java"});
    }

    /**
     * Simple docker evaluation.
     */
    @Test
    @Disabled
    void evaluateSingleDockerFile() {
        Main.main(new String[]{"-s",
                loadFile("simplewebmatcher-1.0-web-latest.tar.gz").getAbsolutePath(),
                "-t", "http://oaei.webdatacommons.org/tdrs/", "conference", "conference-v1",
                "-j", "/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/bin/java"});
    }

    /**
     * Simple combined evaluation.
     */
    @Test
    @Disabled
    void evaluateMultiplePackages() {
        Main.main(new String[]{"-s",
                loadFile("simplewebmatcher-1.0-web-latest.tar.gz").getAbsolutePath(),
                loadFile("simpleSealsMatcher-1.0-seals_external.zip").getAbsolutePath(),
                "-t", "http://oaei.webdatacommons.org/tdrs/", "conference", "conference-v1",
                "-j", "/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/bin/java"});
    }

    /**
     * Helper function to load files in class path that contain spaces.
     *
     * @param fileName Name of the file.
     * @return File in case of success, else null.
     */
    private File loadFile(String fileName) {
        URL fileURL = this.getClass().getClassLoader().getResource(fileName);
        assertNotNull(fileURL, "Could not load file.");
        File result = FileUtils.toFile(fileURL);
        assertTrue(result.exists(), "Required resource not available.");
        return result;
    }

}