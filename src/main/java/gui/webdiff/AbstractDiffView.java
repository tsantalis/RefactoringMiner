package gui.webdiff;

import com.github.gumtreediff.actions.Diff;
import org.rendersnake.Renderable;

/* Created by pourya on 2024-06-03*/
public abstract class AbstractDiffView implements Renderable {
    protected String toolName;
    protected String srcFileName;
    protected String dstFileName;
    protected Diff diff;
    protected int id;
    protected int numOfDiffs;
    public AbstractDiffView(String toolName, String srcFileName, String dstFileName, Diff diff, int id, int numOfDiffs) {
        this.toolName = toolName;
        this.srcFileName = srcFileName;
        this.dstFileName = dstFileName;
        this.numOfDiffs = numOfDiffs;
        this.diff = diff;
        this.id = id;
    }
}
