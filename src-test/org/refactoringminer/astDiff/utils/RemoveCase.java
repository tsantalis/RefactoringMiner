package org.refactoringminer.astDiff.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import static org.refactoringminer.astDiff.utils.UtilMethods.*;

public class RemoveCase {

    public static void main(String[] args) {
        if (args.length == 3) {
            String destin = "";
            if (args[0].equals("defects4j")) {
                destin = getDefects4jMappingPath();
            }
            else if (args[0].equals("defects4j-problems")) {
                destin = getDefects4jProblemsMappingPath();
            }
            else throw new RuntimeException("not valid");
            String projectDir = args[1];
            String bugID = args[2];
            try {

                removeTestCase(projectDir, bugID,
                        destin);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if (args.length == 2) {
            String repo = args[0];
            String commit = args[1];
            try {
                removeTestCase(repo, commit, getCommitsMappingsPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if (args.length == 1) {
            String url = args[0];
            try {
                removeTestCase(url);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if (args.length == 0)
            System.err.println("No input were given");
    }

    private static void removeTestCase(String url) throws IOException {
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);
        removeTestCase(repo,commit, getCommitsMappingsPath());
    }

    private static void removeTestCase(String repo, String commit, String mappingsPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = mappingsPath + getTestInfoFile();
        List<CaseInfo> infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        CaseInfo caseInfo = new CaseInfo(repo,commit);
        boolean confirm = false;
        if (infos.contains(caseInfo))
        {
            System.err.println("Enter yes to confirm the deletion");
            String input = new Scanner(System.in).next();
            if (input.equals("yes")) confirm = true;
        }
        else {
            System.err.println("Repo-Commit pair doesn't exists in json");
        }
        if (confirm) {
            infos.remove(caseInfo);
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(jsonFile), infos);
                System.out.println("Testcase removed successfully");
                System.out.println("Repo=" + repo);
                System.out.println("Commit=" + commit);
                String finalFolderPath = getFinalFolderPath(mappingsPath, repo, commit);
                FileUtils.deleteDirectory(new File(finalFolderPath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            System.err.println("Nothing removed");
        }
    }
}
