package gui.webdiff.viewers;

import com.github.gumtreediff.actions.Diff;

import java.util.List;

import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.rendersnake.Renderable;

/* Created by pourya on 2024-06-03*/
public abstract class AbstractDiffView implements Renderable {
    protected final String toolName;
    protected final String srcFileName;
    protected final String dstFileName;
    protected final Diff diff;
    protected final int id;
    protected final int numOfDiffs;
    protected final String routePath;
    protected final DiffMetaInfo metaInfo;
    protected boolean isMovedDiff;
    protected List<String> deletedFilePaths;
    protected List<String> addedFilePaths;
    public AbstractDiffView(String toolName, DiffMetaInfo metaInfo, String srcFileName, String dstFileName, Diff diff, int id, int numOfDiffs, String routePath, boolean isMovedDiff,
    		List<String> deletedFilePaths, List<String> addedFilePaths) {
        this.toolName = toolName;
        this.srcFileName = srcFileName;
        this.dstFileName = dstFileName;
        this.numOfDiffs = numOfDiffs;
        this.diff = diff;
        this.id = id;
        this.routePath = routePath;
        this.isMovedDiff = isMovedDiff;
        this.metaInfo = metaInfo;
        this.deletedFilePaths = deletedFilePaths;
        this.addedFilePaths = addedFilePaths;
    }
}
