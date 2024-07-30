package gui.webdiff.tree;

import java.util.Optional;

public class TreeNodeInfo {
    private final String name;
    private final String fullPath;
    private Optional<String> srcFilePath = Optional.empty();
    private int id = -1; // NonLeafNode has id = -1;

    public String getName() {
        return name;
    }

    public String getFullPath() {
        return fullPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSrcFilePath(String srcFilePath) {
    	this.srcFilePath = Optional.of(srcFilePath);
    }

    public Optional<String> getSrcFilePath() {
		return srcFilePath;
	}

	public TreeNodeInfo(String name, String fullPath) {
        this.name = name;
        this.fullPath = fullPath;
    }

    public TreeNodeInfo(String name, String fullPath, int id) {
        this.name = name;
        this.fullPath = fullPath;
        this.id = id;
    }
}
