package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.Tree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ContentsContexts {
  private final Map<String, String> srcContents;
  private final Map<String, String> dstContents;
  private final Map<String, TreeContext> srcContexts;
  private final Map<String, TreeContext> dstContexts;

  ContentsContexts(Map<String, String> srcContents, Map<String, String> dstContents,
                   Map<String, TreeContext> srcContexts, Map<String, TreeContext> dstContexts) {
    this.srcContents = srcContents;
    this.dstContents = dstContents;
    this.srcContexts = srcContexts;
    this.dstContexts = dstContexts;
  }

  public Map<String, TreeContext> getSrcContexts() {
    return srcContexts;
  }
  public Map<String, TreeContext> getDstContexts() {
    return dstContexts;
  }

  public String getContent(SrcDst srcDst, String path) {
    return srcDst.equals(SrcDst.SRC) ? srcContents.get(path) : dstContents.get(path);
  }

  public Tree getRoot(SrcDst srcDst, String path) {
    return srcDst.equals(SrcDst.SRC) ? srcContexts.get(path).getRoot() : dstContexts.get(path).getRoot();
  }

  @Nullable
  public String getPath(SrcDst srcDst, Tree tree) {
    List<Tree> parents = tree.getParents();
    Tree root = parents.isEmpty() ? tree : parents.get(parents.size() - 1);

    Optional<Map.Entry<String, TreeContext>> rootEntry = (srcDst.equals(SrcDst.SRC) ? srcContexts : dstContexts).entrySet().stream()
            .filter(e -> e.getValue().getRoot().equals(root)).findFirst();
    return rootEntry.map(Map.Entry::getKey).orElse(null);
  }
}
