package org.refactoringminer.perforce;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to avoid passing around the Maps and Sets required by ASTDiff methods
 * @author Davood Mazinanian
 */
public class ASTDiffInput {

    private final Map<String, String> fileContentsBefore;
    private final Map<String, String> fileContentsAfter;
    private final Set<String> directoriesBefore;
    private final Set<String> directoriesAfter;

    public ASTDiffInput() {
        fileContentsBefore = new LinkedHashMap<>();
        fileContentsAfter = new LinkedHashMap<>();
        directoriesBefore = new LinkedHashSet<>();
        directoriesAfter = new LinkedHashSet<>();
    }

    public Set<String> getDirectoriesAfter() {
        return directoriesAfter;
    }

    public Set<String> getDirectoriesBefore() {
        return directoriesBefore;
    }

    public Map<String, String> getFileContentsAfter() {
        return fileContentsAfter;
    }

    public Map<String, String> getFileContentsBefore() {
        return fileContentsBefore;
    }

    public void addFileContentsBefore(String key, String fileContents) {
        fileContentsBefore.put(key, fileContents);
    }

    public void addFileContentsAfter(String key, String fileContents) {
        fileContentsAfter.put(key, fileContents);
    }
}
