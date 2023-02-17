package org.refactoringminer.astDiff.utils;

import org.refactoringminer.astDiff.actions.ASTDiff;

public class UtilMethods {
    private static final String COMMITS_MAPPINGS_PATH = "src-test/org/refactoringminer/astDiff/data/commits/";
    private static final String TREES_PATH = "src-test/org/refactoringminer/astDiff/data/trees";
    private static final String infoFile = "cases.json";

    private static final String JSON_SUFFIX = ".json";
    private static final String JAVA_SUFFIX = ".java";

    public static String getFinalFilePath(ASTDiff astDiff, String dir, String repo, String commit) {
        String exportName = getFileNameFromSrcDiff(astDiff.getSrcPath());
        return getFinalFolderPath(dir,repo,commit)+ exportName;
    }
    public static String getFileNameFromSrcDiff(String astSrcName)
    {
        String exportName1 = astSrcName.replace("/",".").replace(".java","");
        return exportName1 + JSON_SUFFIX;
    }
    public static String getSrcASTDiffFromFile(String astSrcName)
    {
        String exportName1 = astSrcName.replace(".","/").replace("/json","");
        return exportName1 + JAVA_SUFFIX;
    }

    public static String getFinalFolderPath(String dir, String repo, String commit) {
        String repoFolder = repoToFolder(repo);
        return dir + repoFolder + commit + "/";
    }

    public static String repoToFolder(String repo) {
        String folderName = repo.replace("https://github.com/", "").replace(".git","");
        return folderName.replace("/","_") + "/";
    }

    public static String getCommitsMappingsPath(){
        return COMMITS_MAPPINGS_PATH;
    }
    public static String getTreesPath() { return TREES_PATH; }

    public static String getTestInfoFile(){
        return infoFile;
    }
}
