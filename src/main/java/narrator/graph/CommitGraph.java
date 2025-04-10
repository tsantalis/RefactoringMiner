package narrator.graph;

import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.Map;

public class CommitGraph {
    public static org.jgrapht.Graph<Node, Edge> get(String url) {
        String repo = URLHelper.getRepo(url);
        String commit = URLHelper.getCommit(url);

        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, commit, 1000);

        Map<String, String> dstContentsMap = projectASTDiff.getFileContentsAfter();
        Map<String, String> srcContentsMap = projectASTDiff.getFileContentsBefore();
        GraphProcessor processor = new GraphProcessor(projectASTDiff.getModelDiff(), srcContentsMap, dstContentsMap,
                projectASTDiff.getChildContextMap());

        // TODO: support added files (https://github.com/LMAX-Exchange/disruptor/commit/6a93ff9e94a878cd2d7672b2a07b94a2307d41fe)
        for (ASTDiff astDiff : projectASTDiff.getDiffSet()) {
            processor.importHunks(astDiff.getDstPath(), astDiff.getSrcPath(), astDiff.getAddedDstTrees(),
                    astDiff.createRootNodesClassifier().getMovedDsts(), astDiff.getAllMappings().getMonoMappingStore());
        }

        processor.process();

        return processor.getGraph();
    }
}
