package org.refactoringminer.astDiff;

import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.matchers.ProjectASTDiffer;
import org.refactoringminer.astDiff.webdiff.WebDiff;

/* Created by pourya on 2022-12-26 9:30 p.m. */
public class Run {
    public static void main(String[] args) throws RefactoringMinerTimedOutException {
        ProjectASTDiff projectASTDiff;
        String url;
        url =  "https://github.com/pouryafard75/TestCases/commit/0ae8f723a59722694e394300656128f9136ef466";
//        url =  "https://github.com/pouryafard75/TestCases/commit/dbafc9a519af30a58de22eeb96783f36030558c1";
//        url = "https://github.com/pouryafard75/TestCases/commit/ad1fec1e1a193125a72cc6efba30a454749656ec";
        projectASTDiff = ProjectASTDiffer.fromURL(url,false).diff();
        new WebDiff(projectASTDiff).run();


    }
}
