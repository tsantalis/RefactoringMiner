package org.refactoringminer.astDiff.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static org.refactoringminer.astDiff.utils.UtilMethods.*;
import static org.refactoringminer.astDiff.utils.UtilMethods.getTestDir;

public class AddCase {
    public static void main(String[] args) {
        if (args.length == 2) {
            String repo = args[0];
            String commit = args[1];
            try {
                addTestCase(repo, commit);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (args.length == 1) {
            String url = args[0];
            try {
                addTestCase(url);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (args.length == 0)
            System.err.println("No input were given");
    }
    private static void addTestCase(String url) throws IOException {
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);
        addTestCase(repo,commit);
    }
    private static void addTestCase(String repo, String commit) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = getTestDir() + getTestInfoFile();

        Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);
        for (ASTDiff astDiff : astDiffs) {
            String finalPath = getFinalFilePath(astDiff, getTestDir(),  repo, commit);
            Files.createDirectories(Paths.get(getFinalFolderPath(getTestDir(),repo,commit)));
            astDiff.getMultiMappings().exportToFile(new File(finalPath));
        }
        List<CaseInfo> infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        CaseInfo caseInfo = new CaseInfo(repo,commit);
        boolean goingToAdd = true;
        if (infos.contains(caseInfo))
        {
            System.err.println("Repo-Commit pair already exists in the data folder");
            System.err.println("Enter yes to confirm the overwrite");
            String input = new Scanner(System.in).next();
            if (!input.equals("yes")) goingToAdd = false;
        }
        else {
            infos.add(caseInfo);
        }
        if (goingToAdd) {
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(jsonFile), infos);
                System.err.println("New case added/updated successfully");
                System.err.println("Repo=" + repo);
                System.err.println("Commit=" + commit);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            System.err.println("Nothing updated");
        }
    }
}

