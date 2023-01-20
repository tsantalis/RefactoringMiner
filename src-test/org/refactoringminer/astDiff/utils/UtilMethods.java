package org.refactoringminer.astDiff.utils;

import org.refactoringminer.astDiff.actions.ASTDiff;

public class UtilMethods {
    private static String dir = "src-test/org/refactoringminer/astDiff/data/";
    private static String infoFile = "cases.json";

    public static String getFinalFilePath(ASTDiff astDiff, String dir, String repo, String commit) {
        String filename1 = astDiff.getSrcPath();
        String filename2 = astDiff.getSrcPath();
        String exportName1 = filename1.replace("/",".").replace(".java","");
        String exportName2 = filename2.replace("/",".").replace(".java","");
//        return getFinalFolderPath(dir,repo,commit)+ exportName1 + "-" + exportName2 + ".json";
        return getFinalFolderPath(dir,repo,commit)+ exportName1 + ".json";
    }
    public static String getFinalFolderPath(String dir, String repo, String commit) {
        String repoFolder = repoToFolder(repo);
        return dir + repoFolder + commit + "/";
    }
    public static String repoToFolder(String repo) {
        String folderName = repo.replace("https://github.com/", "").replace(".git","");
        return folderName.replace("/","_") + "/";
    }
    public static String getTestDir(){
        return dir;
    }

    public static String getTestInfoFile(){
        return infoFile;
    }
}
