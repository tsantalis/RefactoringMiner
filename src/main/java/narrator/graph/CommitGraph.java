package narrator.graph;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.Map;
import java.util.Set;

public class CommitGraph {
    public static org.jgrapht.Graph<Node, Edge> get(String url) {
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);

        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);

        Map<String, String> dstContentsMap = projectASTDiff.getFileContentsAfter();
        GraphProcessor processor = new GraphProcessor(projectASTDiff.getModelDiff());

        // TODO: support added files (https://github.com/LMAX-Exchange/disruptor/commit/6a93ff9e94a878cd2d7672b2a07b94a2307d41fe)
        // TODO: add context to use it for same parent edge

        //        get unique addition hunks
        for (ASTDiff astDiff : projectASTDiff.getDiffSet()) {
            Set<Tree> additions = astDiff.getAddedDstTrees();
            for (Tree subject : additions) {
                boolean isUnique = true;
                for (Tree object : additions) {
                    if (object.getPos() <= subject.getPos() && subject.getEndPos() <= object.getEndPos() && !subject.equals(object)) {
                        isUnique = false;
                        break;
                    }
                }

                if (isUnique) {
                    String dstContent = dstContentsMap.get(astDiff.getDstPath());
                    processor.addHunkNode(dstContent, astDiff.getDstPath(), subject);
                }
            }
        }

        processor.process();

        return processor.getGraph();
    }
}
