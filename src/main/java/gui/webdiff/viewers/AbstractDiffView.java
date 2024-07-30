package gui.webdiff.viewers;

import com.github.gumtreediff.actions.Diff;
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
    protected boolean isMovedDiff;
    public AbstractDiffView(String toolName, String srcFileName, String dstFileName, Diff diff, int id, int numOfDiffs, String routePath, boolean isMovedDiff) {
        this.toolName = toolName;
        this.srcFileName = srcFileName;
        this.dstFileName = dstFileName;
        this.numOfDiffs = numOfDiffs;
        this.diff = diff;
        this.id = id;
        this.routePath = routePath;
        this.isMovedDiff = isMovedDiff;
    }
}
