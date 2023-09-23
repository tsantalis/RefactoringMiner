package org.refactoringminer.astDiff.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.params.provider.Arguments;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class UtilMethods {
    public static final String DIFF_DATA_PATH = "src-test/data/astDiff/";
    private static final String COMMITS_MAPPINGS_PATH = DIFF_DATA_PATH + "commits/";
    private static final String TREES_PATH = DIFF_DATA_PATH + "trees";
    private static final String perfectInfoFile = "cases.json";
    private static final String problematicInfoFile = "cases-problematic.json";
    private static final String JSON_SUFFIX = ".json";
    private static final String JAVA_SUFFIX = ".java";
    public static final String REPOS = "tmp1";
    private static final GitService gitService = new GitServiceImpl();
    private static final String DEFECTS4J_MAPPING_PATH = DIFF_DATA_PATH + "defects4j/";
    public static String getDefects4jMappingPath() { return DEFECTS4J_MAPPING_PATH; }

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

    public static String getPerfectInfoFile(){
        return perfectInfoFile;
    }
    public static String getProblematicInfoFile(){
        return problematicInfoFile;
    }
    public static Set<ASTDiff> getProjectDiffLocally(String url) throws Exception {
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);
        String repoFolder = repo.substring(repo.lastIndexOf("/"), repo.indexOf(".git"));
        Repository repository = gitService.cloneIfNotExists(REPOS + repoFolder, repo);
        return new GitHistoryRefactoringMinerImpl().diffAtCommit(repository, commit).getDiffSet();
    }

    public static Set<ASTDiff> getProjectDiffLocally(CaseInfo info) throws Exception {
        return getProjectDiffLocally(info.makeURL());
    }
    public static String getDefect4jAfterDir(String projectDir, String bugID) {
        return getProperDir("after",projectDir,bugID);
    }
    public static String getDefect4jBeforeDir(String projectDir, String bugID) {
        return getProperDir("before",projectDir,bugID);
    }

    private static String getProperDir(String prefix, String projectDir, String bugID) {
        String dir = getDefect4jDir();
        return dir + prefix + "/" + projectDir + "/" + bugID;
    }
    public static String getDefect4jDir() {
        return  System.getProperty("user.dir") + "/" + REPOS + "/defects4j/";
    }
}
