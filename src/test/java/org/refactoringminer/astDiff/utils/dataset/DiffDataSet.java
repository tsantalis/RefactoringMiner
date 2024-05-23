package org.refactoringminer.astDiff.utils.dataset;

import com.beust.jcommander.IStringConverter;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.nio.file.Path;

import static org.refactoringminer.astDiff.utils.UtilMethods.*;

public enum DiffDataSet {
    RefOracle() {
        @Override
        public String getDir() {
            return getCommitsMappingsPath();
        }

        @Override
        public ProjectASTDiff getProjectASTDiff(String url, String repo, String commit) {
            if (url != null){
                repo = URLHelper.getRepo(url);
                commit = URLHelper.getCommit(url);
                return new GitHistoryRefactoringMinerImpl().diffAtCommitWithGitHubAPI
                        (repo, commit, new File(REPOS));
            }
            else if (repo != null && commit != null) {
                return new GitHistoryRefactoringMinerImpl().diffAtCommitWithGitHubAPI
                        (repo, commit, new File(REPOS));
            }
            else
                throw new IllegalArgumentException("Invalid URL or repository/commit pair");
        }

    },
    Defects4j {
        @Override
        public String getDir() {
            return getDefects4jMappingPath();
        }

        @Override
        public ProjectASTDiff getProjectASTDiff(String url, String repo, String commit) {
            if (repo != null && commit != null)
                return new GitHistoryRefactoringMinerImpl().diffAtDirectories(
                        Path.of(getDefect4jBeforeDir(repo, commit)),
                        Path.of(getDefect4jAfterDir(repo, commit)));
            else
                throw new IllegalArgumentException("Invalid URL or repository/commit pair");
        }
    },
    Misc {
        @Override
        public String getDir() {
            return getCommitsMappingsPath();
        }

        @Override
        protected String getPerfect() {
            return "cases-miscellaneous.json";
        }

        @Override
        protected String getProblematic() {
            throw new RuntimeException("Misc cannot have problematic cases at the moment");
        }

        @Override
        public ProjectASTDiff getProjectASTDiff(String url, String repo, String commit) {
            return RefOracle.getProjectASTDiff(url, repo, commit);
        }
    } // Miscellaneous
    ;
    public abstract String getDir();
    String getPerfect() {
        return getPerfectInfoFile();
    }
    String getProblematic(){
        return getProblematicInfoFile();
    }

    public String resolve(boolean problematic) {
        return (problematic) ? getProblematic() : getPerfect();
    }

    public abstract ProjectASTDiff getProjectASTDiff(String url, String repo, String commit);

    public static class Converter implements IStringConverter<DiffDataSet> {
        @Override
        public DiffDataSet convert(String value) {
            if (value.length() < 3) {
                throw new IllegalArgumentException("Invalid dataset name");
            }
            for (DiffDataSet dataset : DiffDataSet.values()) {
                if (dataset.name().toLowerCase().contains(value.toLowerCase())) {
                    return dataset;
                }
            }
            throw new IllegalArgumentException("Invalid dataset name");
        }
    }
}
