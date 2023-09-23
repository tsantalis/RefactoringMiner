/*
 * Copyright 2003-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.mps.ide.ui.tree;

import com.intellij.ide.DataManager;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.impl.IdeFocusManagerHeadless;
import com.intellij.ui.TreeUIHelper;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import jetbrains.mps.RuntimeFlags;
import jetbrains.mps.ide.ThreadUtils;
import jetbrains.mps.smodel.ModelAccess;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class MPSTree extends DnDAwareTree implements Disposable {
  public static final String PATH = "path";

  protected static final Logger LOG = LogManager.getLogger(MPSTree.class);

  public static final String TREE_PATH_SEPARATOR = "/";

  private int myTooltipManagerRecentInitialDelay;
  private boolean myAutoExpandEnabled = true;
  private boolean myAutoOpen = false;
  private boolean myLoadingDisabled;

  private Set<MPSTreeNode> myExpandingNodes = new HashSet<MPSTreeNode>();

  private List<MPSTreeNodeListener> myTreeNodeListeners = new ArrayList<MPSTreeNodeListener>();

  // todo: make unique name
  private MergingUpdateQueue myQueue = new MergingUpdateQueue("MPS Tree Rebuild Later Watcher Queue", 500, true, null);
  private final Object myUpdateId = new Object();

  private boolean myDisposed = false;

  protected MPSTree() {
    setRootNode(new TextTreeNode("Empty"));

    new MPSTreeSpeedSearch(this);

    ToolTipManager.sharedInstance().registerComponent(this);

    largeModel = true;

    TreeUIHelper.getInstance().installToolTipHandler(this);

    setCellRenderer(new NewMPSTreeCellRenderer());

    addTreeWillExpandListener(new MyTreeWillExpandListener());
    addTreeExpansionListener(new MyTreeExpansionListener());
    addMouseListener(new MyMouseAdapter());

    registerKeyboardAction(new MyOpenNodeAction(), KeyStroke.getKeyStroke("F4"), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    registerKeyboardAction(new MyRefreshAction(), KeyStroke.getKeyStroke("F5"), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
  }

  /**
   * Initialization node common for each node initialized in the tree.
   * Shall invoke {@link MPSTreeNode#doInit()} to perform actual initialization.
   * May add extra utility stuff, as model read or progress indication (hence left protected for subclasses).
   * Despite being accessible to subclasses, not deemed to be invoked by anything but {@link MPSTreeNode#init()} method.
   */
  protected void doInit(final MPSTreeNode node) {
    assert ThreadUtils.isInEDT();
    if (myExpandingNodes.contains(node)) {
      return;
    }

    myExpandingNodes.add(node);
    try {
      TextTreeNode progressNode = null;
      if (!myLoadingDisabled && node.isLoadingEnabled()) {
        progressNode = new TextTreeNode("loading...");
        node.add(progressNode);
        ((DefaultTreeModel) getModel()).nodeStructureChanged(node);
        expandPath(new TreePath(progressNode.getPath()));

        Graphics g = getGraphics();
        if (g != null && g.getClipBounds() != null) paint(g);
      }

      ModelAccess.instance().runReadAction(new Runnable() {
        @Override
        public void run() {
          node.doInit();
        }
      });
      ((DefaultTreeModel) getModel()).nodeStructureChanged(node);

      if (!myLoadingDisabled && node.isLoadingEnabled() && node.hasChild(progressNode)) { //node.init() might remove all the children
        node.remove(progressNode);
        ((DefaultTreeModel) getModel()).nodeStructureChanged(node);
      }

    } finally {
      myExpandingNodes.remove(node);
    }
  }

  public void addTreeNodeListener(MPSTreeNodeListener listener) {
    myTreeNodeListeners.add(listener);
  }

  public void removeTreeNodeListener(MPSTreeNodeListener listener) {
    myTreeNodeListeners.remove(listener);
  }

  public void fireBeforeTreeDisposed() {
    for (MPSTreeNodeListener listener : new HashSet<MPSTreeNodeListener>(myTreeNodeListeners)) {
      listener.beforeTreeDisposed(this);
    }
  }

  void fireTreeNodeUpdated(MPSTreeNode node) {
    for (MPSTreeNodeListener listener : new HashSet<MPSTreeNodeListener>(myTreeNodeListeners)) {
      listener.treeNodeUpdated(node, this);
    }
  }

  void fireTreeNodeAdded(MPSTreeNode node) {
    for (MPSTreeNodeListener listener : new HashSet<MPSTreeNodeListener>(myTreeNodeListeners)) {
      listener.treeNodeAdded(node, this);
    }
  }

  void fireTreeNodeRemoved(MPSTreeNode node) {
    for (MPSTreeNodeListener listener : new HashSet<MPSTreeNodeListener>(myTreeNodeListeners)) {
      listener.treeNodeRemoved(node, this);
    }
  }

  void myMouseReleased(MouseEvent e) {
    if (e.isPopupTrigger()) showPopup(e.getX(), e.getY());
  }

  void myMousePressed(final MouseEvent e) {
    Project p = PlatformDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(this));
    IdeFocusManager focusManager;
    if (p != null) {
      focusManager = IdeFocusManager.getInstance(p);
    } else {
      focusManager = IdeFocusManagerHeadless.INSTANCE;
    }

    focusManager.requestFocus(this, true);

    TreePath path = getClosestPathForLocation(e.getX(), e.getY());
    if (path == null) return;

    Object lastPathComponent = path.getLastPathComponent();
    MPSTreeNode nodeToClick=null;
    if (lastPathComponent instanceof MPSTreeNode && ((MPSTreeNode) lastPathComponent).canBeOpened()) {
      nodeToClick = (MPSTreeNode) lastPathComponent;
      if ((e.getClickCount() == 1 && isAutoOpen())) {
        autoscroll(nodeToClick);
      } else if (e.getClickCount() == 2) {
        e.consume();
      }
    } else if (e.getButton() == MouseEvent.BUTTON3) {
      if (!isPathSelected(path)) {
        setSelectionPath(path);
      }
    }

    //workaround for context acquiers
    final MPSTreeNode node2dc = e.getClickCount()==2?nodeToClick:null;
    focusManager.doWhenFocusSettlesDown(new Runnable() {
      @Override
      public void run() {
        if (node2dc != null) {
          doubleClick(node2dc);
        }
        if (e.isPopupTrigger()) showPopup(e.getX(), e.getY());
      }
    });
  }

  /**
   * Gives owning tree a chance to process double-click event.
   * By default, delegates to {@link MPSTreeNode#doubleClick()}
   */
  protected void doubleClick(@NotNull MPSTreeNode nodeToClick) {
    nodeToClick.doubleClick();
  }

  /**
   * Single point to dispatch auto scrolling event.
   * By default, delegates to {@link MPSTreeNode#autoscroll()} ()}
   */
  protected void autoscroll(@NotNull MPSTreeNode nodeToClick) {
    nodeToClick.autoscroll();
  }

  public void runWithoutExpansion(Runnable r) {
    try {
      myAutoExpandEnabled = false;
      r.run();
    } finally {
      myAutoExpandEnabled = true;
    }
  }

  public boolean isAutoOpen() {
    return myAutoOpen;
  }

  public void setAutoOpen(boolean autoOpen) {
    myAutoOpen = autoOpen;
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    TreePath path = getPathForLocation(event.getX(), event.getY());
    if (path != null && path.getLastPathComponent() instanceof MPSTreeNode) {
      final MPSTreeNode node = (MPSTreeNode) path.getLastPathComponent();
      return node.getTooltipText();
    }
    return null;
  }

  protected JPopupMenu createDefaultPopupMenu() {
    return null;
  }

  protected final JPopupMenu createPopupMenu(final MPSTreeNode node) {
    ActionGroup actionGroup = createPopupActionGroup(node);
    if (actionGroup == null) return null;
    return ActionManager.getInstance().createActionPopupMenu(getPopupMenuPlace(), actionGroup).getComponent();
  }

  protected String getPopupMenuPlace() {
    return ActionPlaces.UNKNOWN;
  }

  protected ActionGroup createPopupActionGroup(final MPSTreeNode node) {
    return null;
  }

  private void showPopup(int x, int y) {
    TreePath path = getPathForLocation(x, y);
    if (path != null && path.getLastPathComponent() instanceof MPSTreeNode) {
      final MPSTreeNode node = (MPSTreeNode) path.getLastPathComponent();
      JPopupMenu menu = createPopupMenu(node);
      if (menu != null) {
        if (!getSelectedPaths().contains(pathToString(path))) {
          setSelectionPath(path);
        }
        menu.show(this, x, y);
        return;
      }
    }

    JPopupMenu defaultMenu = createDefaultPopupMenu();
    if (defaultMenu == null) return;
    defaultMenu.show(this, x, y);
  }

  @Nullable
  public Comparator<Object> getChildrenComparator() {
    return null;
  }

  protected abstract MPSTreeNode rebuild();

  public void expandAll() {
    MPSTreeNode node = getRootNode();
    expandAll(node);
  }

  public void collapseAll() {
    MPSTreeNode node = getRootNode();
    collapseAll(node);
  }

  public void selectFirstLeaf() {
    List<MPSTreeNode> path = new ArrayList<MPSTreeNode>();
    MPSTreeNode current = getRootNode();

    while (true) {
      path.add(current);
      if (current.getChildCount() == 0) break;
      current = (MPSTreeNode) current.getChildAt(0);
    }

    setSelectionPath(new TreePath(path.toArray()));
  }

  public void expandRoot() {
    expandPath(new TreePath(new Object[]{getRootNode()}));
    getRootNode().init();
  }

  public void expandAll(MPSTreeNode node) {
    boolean wasLoadingDisabled = myLoadingDisabled;
    myLoadingDisabled = true;
    try {
      expandAllImpl(node);
    } finally {
      myLoadingDisabled = wasLoadingDisabled;
    }
  }
  private void expandAllImpl(MPSTreeNode node) {
    expandPath(new TreePath(node.getPath()));
    for (MPSTreeNode c : node) {
      expandAllImpl(c);
    }
  }

  public void collapseAll(MPSTreeNode node) {
    boolean wasAutoExpandEnabled = myAutoExpandEnabled;
    try {
      myAutoExpandEnabled = false;
      collapseAllImpl(node);
    } finally {
      myAutoExpandEnabled = wasAutoExpandEnabled;
    }
  }

  private void collapseAllImpl(MPSTreeNode node) {
    for (MPSTreeNode c : node) {
      collapseAllImpl(c);
    }
    if (node.isInitialized()) {
      super.collapsePath(new TreePath(node.getPath()));
    }
  }

  public void selectNode(TreeNode node) {
    List<TreeNode> nodes = new ArrayList<TreeNode>();
    while (node != null) {
      nodes.add(0, node);
      node = node.getParent();
    }
    if (nodes.size() == 0) return;
    TreePath path = new TreePath(nodes.toArray());
    setSelectionPath(path);
    scrollRowToVisible(getRowForPath(path));
  }

  // FIXME perhaps, shall be protected, rebuildAction is sort of implementation detail when there are rebuildNow() and rebuildLater()
  public void runRebuildAction(final Runnable rebuildAction, final boolean saveExpansion) {
    if (RuntimeFlags.isTestMode()) {
      return;
    }
    if (!ThreadUtils.isInEDT()) {
      throw new RuntimeException("Rebuild now can be only called from UI thread");
    }


    myLoadingDisabled = true;
    try {
      Runnable restoreExpansion = null;
      if (saveExpansion) {
        final List<String> expansion = getExpandedPaths();
        final List<String> selection = getSelectedPaths();
        restoreExpansion = new Runnable() {
          @Override
          public void run() {
            expandPaths(expansion);
            selectPaths(selection);
          }
        };
      }
      ModelAccess.instance().runReadAction(rebuildAction);
      if (restoreExpansion != null) {
          runWithoutExpansion(restoreExpansion);
      }
    } finally {
      myLoadingDisabled = false;
    }
  }

  public void rebuildLater() {
    myQueue.queue(new Update(myUpdateId) {
      @Override
      public void run() {
        ThreadUtils.runInUIThreadNoWait(new Runnable() {
          @Override
          public void run() {
            if (MPSTree.this.isDisposed()) return;
            rebuildNow();
          }
        });
      }
    });
  }

  public void rebuildNow() {
    if (!ThreadUtils.isInEDT()) {
      throw new RuntimeException("Rebuild now can be only called from UI thread");
    }
    assert !isDisposed() : "Trying to reconstruct disposed tree. Try finding \"later\" in stacktrace";

    runRebuildAction(new Runnable() {
      @Override
      public void run() {
        setAnchorSelectionPath(null);
        setLeadSelectionPath(null);

        MPSTreeNode root = rebuild();
        setRootNode(root);
      }
    }, true);
  }

  public void clear() {
    setRootNode(new TextTreeNode("Empty"));
  }

  private void setRootNode(@Nullable MPSTreeNode root) {
    final Object oldRoot = getModel().getRoot();
    if (oldRoot instanceof MPSTreeNode) {
      ((MPSTreeNode) oldRoot).removeThisAndChildren();
      ((MPSTreeNode) oldRoot).setTree(null);
    }

    if (root != null) {
      root.setTree(this);
      root.addThisAndChildren();
    }

    DefaultTreeModel model = new DefaultTreeModel(root);
    setModel(model);
  }

  private String pathToString(TreePath path) {
    StringBuilder result = new StringBuilder();
    for (int i = 1; i < path.getPathCount(); i++) {
      MPSTreeNode node = (MPSTreeNode) path.getPathComponent(i);
      result.append(TREE_PATH_SEPARATOR);
      result.append(node.getNodeIdentifier().replaceAll(TREE_PATH_SEPARATOR, "-"));
    }
    if (result.length() == 0) return TREE_PATH_SEPARATOR;
    return result.toString();
  }

  public TreeNode findNodeWith(Object userObject) {
    MPSTreeNode root = getRootNode();
    return findNodeWith(root, userObject);
  }

  public MPSTreeNode getRootNode() {
    return (MPSTreeNode) getModel().getRoot();
  }

  public MPSTreeNode getCurrentNode() {
    javax.swing.tree.TreePath path = getLeadSelectionPath();
    if (path == null) {
      return null;
    }
    Object obj = path.getLastPathComponent();
    if (!(obj instanceof TreeNode)) {
      return null;
    }
    return (MPSTreeNode) obj;
  }

  public void setCurrentNode(MPSTreeNode node) {
    TreePath path = new TreePath(node.getPath());
    setSelectionPath(path);
    this.scrollPathToVisible(path);
  }

  private MPSTreeNode findNodeWith(MPSTreeNode root, Object userObject) {
    if (root.getUserObject() == userObject) return root;
    if (!(root.isInitialized() || root.hasInfiniteSubtree())) root.init();
    for (MPSTreeNode child : root) {
      MPSTreeNode result = findNodeWith(child, userObject);
      if (result != null) return result;
    }
    return null;
  }

  private TreePath listToPath(List<String> pathComponents) {
    return getTreePath(pathComponents, false);
  }

  private TreePath stringToPath(String pathString) {
    List<String> components = Arrays.asList(pathString.split(TREE_PATH_SEPARATOR));

    return getTreePath(components, true);
  }

  @Nullable
  private TreePath getTreePath(List<String> components, boolean escapePathSep) {
    List<Object> path = new ArrayList<Object>();
    MPSTreeNode current = getRootNode();

    current.init();

    path.add(current);

    for (Iterator<String> it = components.iterator(); it.hasNext(); ) {
      String component = it.next();
      assert current.isInitialized();
      if (component == null || component.length() == 0) continue;
      boolean found = false;
      for (int i = 0; i < current.getChildCount(); i++) {
        MPSTreeNode node = (MPSTreeNode) current.getChildAt(i);
        String treeNodeId =
            escapePathSep ? node.getNodeIdentifier().replaceAll(TREE_PATH_SEPARATOR, "-") :
                            node.getNodeIdentifier();
        if (treeNodeId.equals(component)) {
          current = node;
          path.add(current);
          if (!current.isInitialized() && it.hasNext()) {
            current.init();
          }
          found = true;
          break;
        }
      }
      if (!found) {
        return null;
      }
    }
    return new TreePath(path.toArray());
  }

  protected void expandPathsRaw(List<List<String>> paths) {
    for (List<String> path : paths) {
      TreePath treePath = listToPath(path);
      if (treePath != null) {
        ensurePathInitialized(treePath);
        expandPath(treePath);
      }
    }
  }

  protected void expandPaths(List<String> paths) {
    for (String path : paths) {
      TreePath treePath = stringToPath(path);
      if (treePath != null) {
        ensurePathInitialized(treePath);
        expandPath(treePath);
      }
    }
  }

  private void ensurePathInitialized(TreePath path) {
    for (Object item : path.getPath()) {
      MPSTreeNode node = (MPSTreeNode) item;
      node.init();
    }
  }

  protected void selectPaths(List<String> paths) {
    List<TreePath> treePaths = new ArrayList<TreePath>();
    for (String path : paths) {
      treePaths.add(stringToPath(path));
    }
    setSelectionPaths(treePaths.toArray(new TreePath[treePaths.size()]));
  }

  protected void selectPathsRaw(List<List<String>> paths) {
    List<TreePath> treePaths = new ArrayList<TreePath>();
    for (List<String> path : paths) {
      treePaths.add(listToPath(path));
    }
    setSelectionPaths(treePaths.toArray(new TreePath[treePaths.size()]));
  }

  // TODO: refactor TreeState to include these instead of the old format
  public List<List<String>> getExpandedPathsRaw() {
    List<List<String>> result = new ArrayList<List<String>>();
    Enumeration<TreePath> expanded = getExpandedDescendants(new TreePath(new Object[]{getModel().getRoot()}));
    if (expanded == null) return result;
    while (expanded.hasMoreElements()) {
      List<String> path = new ArrayList<String>();
      TreePath expandedPath = expanded.nextElement();
      if (expandedPath.getLastPathComponent() == getModel().getRoot()) {
        continue;
      }
      for (int i = 1; i < expandedPath.getPathCount(); i++) {
        MPSTreeNode node = (MPSTreeNode) expandedPath.getPathComponent(i);
        path.add(node.getNodeIdentifier());
      }
      result.add(path);
    }
    return result;
  }

  private List<String> getExpandedPaths() {
    List<String> result = new ArrayList<String>();
    Enumeration<TreePath> expanded = getExpandedDescendants(new TreePath(new Object[]{getModel().getRoot()}));
    if (expanded == null) return result;
    while (expanded.hasMoreElements()) {
      TreePath path = expanded.nextElement();
      String pathString = pathToString(path);
      if (result.contains(pathString))
        LOG.warn("two expanded paths have the same string representation");
      result.add(pathString);
    }
    return result;
  }

  // TODO: refactor TreeState to include these instead of the old format
  public List<List<String>> getSelectedPathsRaw() {
    List<List<String>> result = new ArrayList<List<String>>();
    if (getSelectionPaths() == null) return result;
    for (TreePath selectedPath: getSelectionPaths()) {
      List<String> path = new ArrayList<String>();
      for (int i = 1; i < selectedPath.getPathCount(); i++) {
        MPSTreeNode node = (MPSTreeNode) selectedPath.getPathComponent(i);
        path.add(node.getNodeIdentifier());
      }
      result.add(path);
    }
    return result;
  }

  private List<String> getSelectedPaths() {
    List<String> result = new ArrayList<String>();
    if (getSelectionPaths() == null) return result;
    for (TreePath selectionPart : getSelectionPaths()) {
      String pathString = pathToString(selectionPart);
      if (result.contains(pathString))
        LOG.warn("two selected paths have the same string representation");
      result.add(pathString);
    }
    return result;
  }

  public TreeState saveState() {
    TreeState result = new TreeState();
    result.myExpansion.addAll(getExpandedPaths());
    result.mySelection.addAll(getSelectedPaths());
    return result;
  }

  public void loadState(TreeState state) {
    selectPaths(state.mySelection);
    expandPaths(state.myExpansion);
  }

  // TODO: refactor TreeState to include these instead of the old format
  public void loadState(List<List<String>> expandedPaths, List<List<String>> selectedPaths) {
    expandPathsRaw(expandedPaths);
    selectPathsRaw(selectedPaths);
  }

  @Override
  public int getToggleClickCount() {
    TreePath selection = getSelectionPath();
    if (selection == null) return -1;
    if (selection.getLastPathComponent() instanceof MPSTreeNode) {
      MPSTreeNode node = (MPSTreeNode) selection.getLastPathComponent();
      return node.getToggleClickCount();
    }
    return -1;
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  @Override
  public void dispose() {
    assert !myDisposed;

    fireBeforeTreeDisposed();
    myDisposed = true;
    setRootNode(null);
    myTreeNodeListeners.clear();
  }

  public static class TreeState {
    private List<String> myExpansion = new ArrayList<String>();
    private List<String> mySelection = new ArrayList<String>();

    public List<String> getExpansion() {
      return myExpansion;
    }

    public void setExpansion(List<String> expansion) {
      myExpansion = expansion;
    }

    public List<String> getSelection() {
      return mySelection;
    }

    public void setSelection(List<String> selection) {
      mySelection = selection;
    }
  }

  private class MyTreeWillExpandListener implements TreeWillExpandListener {
    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
      TreePath path = event.getPath();
      Object node = path.getLastPathComponent();
      MPSTreeNode treeNode = (MPSTreeNode) node;
      treeNode.init();
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
    }
  }

  private class MyTreeExpansionListener implements TreeExpansionListener {
    @Override
    public void treeExpanded(TreeExpansionEvent event) {
      if (!myAutoExpandEnabled) return;

      TreePath eventPath = event.getPath();
      MPSTreeNode node = (MPSTreeNode) eventPath.getLastPathComponent();

      if (node.getChildCount() == 1) {
        List<MPSTreeNode> path = new ArrayList<MPSTreeNode>();
        for (Object item : eventPath.getPath()) {
          path.add((MPSTreeNode) item);
        }
        MPSTreeNode onlyChild = (MPSTreeNode) node.getChildAt(0);

        if (onlyChild.isAutoExpandable()) {
          path.add(onlyChild);
          expandPath(new TreePath(path.toArray()));
        }
      }
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
    }
  }

  private class MyMouseAdapter extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      //this is a workaround for handling context menu button
      if (e.getButton() == 0) {
        TreePath path = getSelectionPath();
        if (path == null) return;
        int rowNum = getRowForPath(path);
        Rectangle r = getRowBounds(rowNum);
        showPopup(r.x, r.y);
      } else {
        requestFocus();
        myMousePressed(e);
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      myMouseReleased(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      myTooltipManagerRecentInitialDelay = ToolTipManager.sharedInstance().getInitialDelay();
      ToolTipManager.sharedInstance().setInitialDelay(10);
    }

    @Override
    public void mouseExited(MouseEvent e) {
      ToolTipManager.sharedInstance().setInitialDelay(myTooltipManagerRecentInitialDelay);
    }
  }

  private class MyOpenNodeAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
      TreePath selPath = getSelectionPath();
      if (selPath == null) return;
      MPSTreeNode selNode = (MPSTreeNode) selPath.getLastPathComponent();
      doubleClick(selNode);
    }
  }

  private class MyRefreshAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
      rebuildNow();
    }
  }
}
