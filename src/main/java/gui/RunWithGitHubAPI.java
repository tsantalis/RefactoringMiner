package gui;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.internal.core.JavadocContents.Range;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import com.github.gumtreediff.tree.Tree;

import gui.webdiff.WebDiff;


/* Created by pourya on 2022-12-26 9:30 p.m. */
public class RunWithGitHubAPI {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {

        // String url = "https://github.com/NationalSecurityAgency/ghidra/commit/cab8ed6068aaa8dcab4c72c82c6009c99b6dfc5f";
        // String url = "https://github.com/NationalSecurityAgency/ghidra/commit/bf795b73d760ca800f97b60d6d1d9f18a698c63e";
        String url = "https://github.com/facebook/buck/commit/a1525ac";

        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);

        String output = "...\n";

        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);

        Set<ASTDiff> diffs = projectASTDiff.getDiffSet();
        for (ASTDiff diff : diffs) {
            // String srcContent = projectASTDiff.getFileContentsBefore().get(diff.getSrcPath());
            String dstContent = projectASTDiff.getFileContentsAfter().get(diff.getDstPath());

            // ExtendedTreeClassifier classifier = (ExtendedTreeClassifier) diff.createRootNodesClassifier();
            // ExtendedMultiMappingStore mappings = diff.getAllMappings();
            // Tree[] srcs = classifier.getMovedSrcs().toArray(Tree[]::new);
            // for (Tree src : srcs) {
            //     System.out.println(srcContent.substring(src.getPos(), src.getEndPos()));
                
            //     System.out.println("=>");

            //     Tree[] dsts = mappings.getDsts(src).toArray(Tree[]::new);
            //     for (Tree dst : dsts) {
            //         System.out.println(dstContent.substring(dst.getPos(), dst.getEndPos()));
            //         System.out.println("||||||||||||");
            //     }
            // }

            // Mapping[] mappings = diff.getAllMappings().getMappings().toArray(Mapping[]::new);
            // for (Mapping mapping : mappings) {
            //     Tree src = mapping.first;
            //     Tree dst = mapping.second;

            //     System.out.println(srcContent.substring(src.getPos(), src.getEndPos()));
            //     System.out.println("=>");
            //     System.out.println(dstContent.substring(dst.getPos(), dst.getEndPos()));
            //     System.out.println("|||||||||");
            // }

            Tree[] additions = diff.getAddedDstTrees().toArray(Tree[]::new);

            Arrays.sort(additions, (Tree t1, Tree t2) -> t1.getPos() - t2.getPos());

            List<Range> mergedAdditions = mergeRanges(additions);
            for (Range range : mergedAdditions) {
                output += dstContent.substring(range.start(), range.end());
                output += "\n...\n";
            }
        }

        try (FileWriter writer = new FileWriter("./" + commit + ".txt")) {
            writer.write(output);
        }

        new WebDiff(projectASTDiff).run();
    }

    public static List<Range> mergeRanges(Tree[] additions) {
        List<Range> merged = new ArrayList<>();

        Range current = new Range(additions[0].getPos(), additions[0].getEndPos());
        for (int i = 1; i < additions.length; i++) {
            Range next = new Range(additions[i].getPos(), additions[i].getEndPos());

            if (current.end() >= next.start() - 1) {
                // If additions overlap or are contiguous, merge them
                current = new Range(current.start(), Math.max(current.end(), next.end()));
            } else {
                // No overlap, add the current range to the merged list
                merged.add(current);
                current = next;
            }
        }

        // Add the last range
        merged.add(current);

        return merged;
    }
}
