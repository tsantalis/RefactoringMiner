package org.refactoringminer.astDiff.utils.dataset.runners;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.refactoringminer.astDiff.utils.CaseInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import static org.refactoringminer.astDiff.utils.ExportUtils.getFinalFolderPath;


// Command class for the "remove" command
@Parameters(commandDescription = "Remove files from the index")
public class RemoveCommand extends BaseCommand {
    @Parameter(names = {"-t", "--test"}, description = "Files to remove", required = true)
    private String test;

    public void execute() {
        System.out.println("Remove command");
        System.out.println("Files to remove: " + test);
    }

    @Override
    void postValidationExecution() {
        String infoFile = diffDataSet.resolve(problematic);
        String dir = diffDataSet.getDir();
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = dir + infoFile;
        List<CaseInfo> infos = null;
        try {
            infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                String finalFolderPath = getFinalFolderPath(dir, repo, commit);
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
