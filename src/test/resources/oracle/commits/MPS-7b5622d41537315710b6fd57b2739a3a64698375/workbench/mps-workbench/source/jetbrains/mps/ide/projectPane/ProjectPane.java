/*
 * Copyright 2003-2014 JetBrains s.r.o.
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
package jetbrains.mps.ide.projectPane;

import com.intellij.ide.SelectInTarget;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import jetbrains.mps.RuntimeFlags;
import jetbrains.mps.icons.MPSIcons;
import jetbrains.mps.ide.ThreadUtils;
import jetbrains.mps.ide.editor.MPSFileNodeEditor;
import jetbrains.mps.ide.platform.watching.ReloadListener;
import jetbrains.mps.ide.platform.watching.ReloadManager;
import jetbrains.mps.ide.projectPane.logicalview.ProjectPaneTree;
import jetbrains.mps.ide.projectPane.logicalview.ProjectTree;
import jetbrains.mps.ide.projectPane.logicalview.ProjectTreeFindHelper;
import jetbrains.mps.ide.projectView.ProjectViewPaneOverride;
import jetbrains.mps.ide.ui.tree.MPSTree;
import jetbrains.mps.ide.ui.tree.MPSTree.TreeState;
import jetbrains.mps.ide.ui.tree.MPSTreeNode;
import jetbrains.mps.ide.ui.tree.MPSTreeNodeEx;
import jetbrains.mps.ide.ui.tree.TreeHighlighterExtension;
import jetbrains.mps.ide.ui.tree.smodel.SModelTreeNode;
import jetbrains.mps.ide.ui.tree.smodel.SNodeTreeNode;
import jetbrains.mps.openapi.editor.EditorComponent;
import jetbrains.mps.project.MPSProject;
import jetbrains.mps.smodel.ModelAccess;
import jetbrains.mps.util.Computable;
import jetbrains.mps.util.SNodeOperations;
import jetbrains.mps.util.annotation.Hack;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.model.SModel;
import org.jetbrains.mps.openapi.model.SNode;
import org.jetbrains.mps.openapi.module.SModule;
import org.jetbrains.mps.util.Condition;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@State(
    name = "MPSProjectPane",
    storages = {
        @Storage(
            id = "other",
            file = "$WORKSPACE_FILE$"
        )
    }
)
public class ProjectPane extends BaseLogicalViewProjectPane implements ProjectViewPaneOverride {
  private static final Logger LOG = LogManager.getLogger(ProjectPane.class);
  private ProjectTreeFindHelper myFindHelper = new ProjectTreeFindHelper() {
    @Override
    protected ProjectTree getTree() {
      return ProjectPane.this.getTree();
    }
  };

  private MyScrollPane myScrollPane;
  private MergingUpdateQueue myUpdateQueue = new MergingUpdateQueue("Project Pane Updates Queue", 500, true, myScrollPane, null, null, true);

  public static final String ID = ProjectViewPane.ID;

  private FileEditorManagerAdapter myEditorListener = new FileEditorManagerAdapter() {
    @Override
    public void selectionChanged(FileEditorManagerEvent event) {
      FileEditor fileEditor = event.getNewEditor();
      if (fileEditor instanceof MPSFileNodeEditor) {
        final MPSFileNodeEditor editor = (MPSFileNodeEditor) fileEditor;
        if (getProjectView().isAutoscrollFromSource(ID)) {
          EditorComponent editorComponent = editor.getNodeEditor().getCurrentEditorComponent();
          if (editorComponent == null) return;
          final SNode sNode = editorComponent.getEditedNode();
          ModelAccess.instance().runReadInEDT(new Runnable() {
            @Override
            public void run() {
              selectNodeWithoutExpansion(sNode);
            }
          });
        }
      }
    }
  };
  private Set<ComponentCreationListener> myComponentCreationListeners;
  private static boolean ourShowGenStatus = true;
  private List<List<String>> myExpandedPathsRaw = Collections.emptyList();
  private List<List<String>> mySelectedPathsRaw = Collections.emptyList();

  public ProjectPane(final Project project, ProjectView projectView) {
    super(project, projectView);
    myUpdateQueue.setRestartTimerOnAdd(true);
    ReloadManager.getInstance().addReloadListener(new ReloadListener() {
      @Override
      public void reloadStarted() {

      }

      @Override
      public void reloadFinished() {
        rebuild();
      }
    });
  }

  @Override
  protected void removeListeners() {
    super.removeListeners();
    FileEditorManager fileEditorManager = getProject().getComponent(FileEditorManager.class);
    fileEditorManager.removeFileEditorManagerListener(myEditorListener);
  }

  @Override
  protected void addListeners() {
    super.addListeners();
    getProject().getComponent(FileEditorManager.class).addFileEditorManagerListener(myEditorListener);
  }

  @Hack
  public static ProjectPane getInstance(Project project) {
    final ProjectView projectView = ProjectView.getInstance(project);

    //to ensure panes are initialized
    //filed http://jetbrains.net/tracker/issue/IDEA-24732
    projectView.getSelectInTargets();

    return (ProjectPane) projectView.getProjectViewPaneById(ID);
  }

  public static ProjectPane getInstance(jetbrains.mps.project.Project mpsProject) {
    if (mpsProject instanceof MPSProject) {
      return getInstance(((MPSProject) mpsProject).getProject());
    }
    return null;
  }

  @Override
  public ProjectTree getTree() {
    return (jetbrains.mps.ide.projectPane.logicalview.ProjectTree) myTree;
  }

  @Override
  public String getTitle() {
    return "Logical View";
  }

  @Override
  @NotNull
  public String getId() {
    return ID;
  }

  @Override
  public int getWeight() {
    return 0;
  }

  @Override
  public SelectInTarget createSelectInTarget() {
    return new ProjectPaneSelectInTarget(this.myProject, true);
  }

  @Override
  public Icon getIcon() {
    return MPSIcons.ProjectPane.LogicalView;
  }

  @Override
  public ActionCallback updateFromRoot(boolean restoreExpandedPaths) {
    myUpdateQueue.queue(new AbstractUpdate(UpdateID.REBUILD) {
      @Override
      public void run() {
        if (getTree() == null) {
          return;
        }
        getTree().rebuildNow();
      }
    });
    return new ActionCallback(); // todo
  }

  @Override
  public void select(Object element, final VirtualFile file, final boolean requestFocus) {

  }

  @Override
  public JComponent createComponent() {
    if (isComponentCreated()) return myScrollPane;

    ProjectPaneTree tree = new ProjectPaneTree(this, myProject);
    Disposer.register(this, tree);
    tree.setShowStructureCondition(new Computable<Boolean>() {
      @Override
      public Boolean compute() {
        if (myProject.isDisposed()) return false;
        return ProjectPane.getInstance(myProject).showNodeStructure();
      }
    });
    myTree = tree;

    myScrollPane = new MyScrollPane(getTree());
    addListeners();
    if (!RuntimeFlags.isTestMode()) {
      // Looks like this method can be called from different threads
      ThreadUtils.runInUIThreadNoWait(new Runnable() {
        @Override
        public void run() {
          rebuildTree();
        }
      });
    }
    TreeHighlighterExtension.attachHighlighters(tree, myProject);
    fireComponentCreated();
    return myScrollPane;
  }

  @Override
  protected boolean isComponentCreated() {
    return myScrollPane != null;
  }

  public void rebuildTree() {
    myUpdateQueue.queue(new AbstractUpdate(UpdateID.REBUILD) {
      @Override
      public void run() {
        if (getTree() == null || getProject().isDisposed()) {
          return;
        }
        getTree().rebuildNow();
        getTree().expandProjectNode();
      }
    });
  }

  public void activate() {
    ThreadUtils.assertEDT();
    activatePane(new PaneActivator(false), true);
  }

  @Override
  public void rebuild() {
    ModelAccess.instance().runReadInEDT(new Runnable() {
      @Override
      public void run() {
        if (isDisposed() || getTree() == null) return;
        rebuildTree();
      }
    });
  }

  @Override
  protected void saveExpandedPaths() {
    // this gets called from the IDEA's implementation of ProjectViewImpl
    // thankfully, the method is declared protected
    if (myTree != null) {
      myExpandedPathsRaw = ((MPSTree) myTree).getExpandedPathsRaw();
      mySelectedPathsRaw = ((MPSTree) myTree).getSelectedPathsRaw();
    }
    else {
      myExpandedPathsRaw = Collections.emptyList();
      mySelectedPathsRaw = Collections.emptyList();
    }
  }

  @Override
  public void restoreExpandedPathsOverride () {
    // this gets called from the MPS's implementation of ProjectViewImpl
    // we must resort to this hack because the method in the superclass is declared private

    if (myTree != null) {
      myUpdateQueue.queue(new AbstractUpdate(UpdateID.RESTORE_EXPAND) {
        @Override
        public void run() {
          ((MPSTree) myTree).loadState(myExpandedPathsRaw, mySelectedPathsRaw);

        }
      });
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    saveExpandedPaths();

    // keep the binary format in sync with what IDEA writes
    Element subPane = new Element("subPane");
    // we probabbly don't need this...
    if (getSubId() != null) {
      subPane.setAttribute("subId", getSubId());
    }

    witePaths(subPane, myExpandedPathsRaw, "PATH");
    witePaths(subPane, mySelectedPathsRaw, "SELECTED");

    element.addContent(subPane);
  }

  private void witePaths(Element parentElement, List<List<String>> pathsRaw, String elementName) {
    for (List<String> path: pathsRaw) {
      Element pathElement = new Element(elementName);
      writePath(path, pathElement);
      parentElement.addContent(pathElement);
    }
  }

  private void writePath(List<String> path, Element pathElement) {
    for (String treeNodeId : path) {
      Element elm = new Element("PATH_ELEMENT");
      writeNodeId(treeNodeId, elm);
      pathElement.addContent(elm);
    }
  }

  private void writeNodeId(String treeNodeId, Element elm) {
    Element option1 = new Element("option");
    option1.setAttribute("name", "myItemId");
    option1.setAttribute("value", treeNodeId);
    elm.addContent(option1);
    Element option2 = new Element("option");
    option2.setAttribute("name", "myItemType");
    option2.setAttribute("value", "");
    elm.addContent(option2);
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    // emulate the superclass's readExternal using the same binary format
    List<Element> subPanes = element.getChildren("subPane");
    for (Element subPane : subPanes) {
      myExpandedPathsRaw = readPaths(subPane, "PATH");
      mySelectedPathsRaw = readPaths(subPane, "SELECTED");
    }
  }

  private List<List<String>> readPaths(Element parentElement, String name) {
    List<List<String>> result = new ArrayList<List<String>>();

    for (Element pathElement : parentElement.getChildren(name)) {
      List<String> path = readPath(pathElement);
      result.add(path);
    }
    return result;
  }

  @NotNull
  private List<String> readPath(Element pathElement) {
    List<String> path = new ArrayList<String>();
    for (Element elm : pathElement.getChildren("PATH_ELEMENT")) {
      String treeNodeId = readNodeId(elm);
      if (treeNodeId != null) {
        path.add(treeNodeId);
      }
    }
    return path;
  }

  @Nullable
  private String readNodeId(Element elm) {
    List<Element> options = elm.getChildren("option");
    String treeNodeId = null;
    for (Element option : options) {
      if ("myItemId".equals(option.getAttributeValue("name"))) {
        treeNodeId = option.getAttributeValue("value");
        break;
      }
    }
    return treeNodeId;
  }

  //----selection----

  public void selectModule(@NotNull final SModule module, final boolean autofocus) {
    ModelAccess.instance().runReadInEDT(new Runnable() {
      @Override
      public void run() {
        activatePane(new PaneActivator(true) {
          @Override
          public void doOnPaneActivation() {
            MPSTreeNode moduleTreeNode = myFindHelper.findMostSuitableModuleTreeNode(module);

            if (moduleTreeNode == null) {
              LOG.warn("Couldn't select module \"" + module.getModuleName() + "\" : tree node not found.");
              return;
            }

            getTree().selectNode(moduleTreeNode);
          }
        }, autofocus);
      }
    });
  }

  public void selectModel(@NotNull final SModel model, boolean autofocus) {
    if (!ThreadUtils.isEventDispatchThread()) {
      throw new IllegalStateException("Can't use this outside of EDT");
    }
    activatePane(new PaneActivator(true) {
      @Override
      public void doOnPaneActivation() {
        SModelTreeNode modelTreeNode = myFindHelper.findMostSuitableModelTreeNode(model);
        if (modelTreeNode == null) {
          LOG.warn("Couldn't select model \"" + SNodeOperations.getModelLongName(model) + "\" : tree node not found.");
          return;
        }
        getTree().selectNode(modelTreeNode);
      }
    }, autofocus);
  }

  private void activatePane(PaneActivator activator, boolean autoFocusContents) {
    ToolWindowManager windowManager = ToolWindowManager.getInstance(getProject());
    ToolWindow projectViewToolWindow = windowManager.getToolWindow(ToolWindowId.PROJECT_VIEW);
    //In unit test mode projectViewToolWindow == null
    if(!ApplicationManager.getApplication().isUnitTestMode()) {
      projectViewToolWindow.activate(activator, autoFocusContents);
    }
  }

  public void selectNode(@NotNull final SNode node, boolean autofocus) {
    if (!ThreadUtils.isEventDispatchThread()) {
      throw new IllegalStateException("Can't use this outside of EDT");
    }
    activatePane(new PaneActivator(true) {
      @Override
      public void doOnPaneActivation() {
        selectNodeWithoutExpansion(node);
      }
    }, autofocus);
  }

  private void selectNodeWithoutExpansion(final SNode node) {
    getTree().runWithoutExpansion(new Runnable() {
      @Override
      public void run() {
        MPSTreeNodeEx sNodeNode = myFindHelper.findMostSuitableSNodeTreeNode(node);
        if (sNodeNode == null) {
          LOG.warn("Couldn't select node \"" + node.getName() + "\" : tree node not found.");
          return;
        }
        getTree().selectNode(sNodeNode);
      }
    });
  }

  //----select next queries----

  @Override
  public void selectNextModel(SModel modelDescriptor) {
    final MPSTreeNode mpsTreeNode = myFindHelper.findNextTreeNode(modelDescriptor);
    ThreadUtils.runInUIThreadNoWait(new Runnable() {
      @Override
      public void run() {
        ProjectTree tree = getTree();
        if (tree != null) {
          tree.selectNode(mpsTreeNode);
        }
      }
    });
  }

  public void selectNextNode(SNode node) {
    final MPSTreeNode mpsTreeNode = myFindHelper.findNextTreeNode(node);
    ThreadUtils.runInUIThreadNoWait(new Runnable() {
      @Override
      public void run() {
        getTree().selectNode(mpsTreeNode);
      }
    });
  }

  //----tree node selection queries---

  public MPSTreeNode findNextTreeNode(SNode node) {
    return myFindHelper.findNextTreeNode(node);
  }

  private void fireComponentCreated() {
    if (myComponentCreationListeners == null) {
      return;
    }
    for (ComponentCreationListener l : myComponentCreationListeners.toArray(new ComponentCreationListener[myComponentCreationListeners.size()])) {
      l.componentCreated(this);
    }
  }

  public void addComponentCreationListener(@NotNull ComponentCreationListener l) {
    if (myComponentCreationListeners == null) {
      myComponentCreationListeners = new HashSet();
    }
    myComponentCreationListeners.add(l);
  }

  public void removeComponentCreationListener(@NotNull ComponentCreationListener l) {
    if (myComponentCreationListeners == null) {
      return;
    }
    myComponentCreationListeners.remove(l);
    if (myComponentCreationListeners.isEmpty()) {
      myComponentCreationListeners = null;
    }
  }

  //---gen status---

  public static void setShowGenStatus(boolean showGenStatusInTree) {
    ourShowGenStatus = showGenStatusInTree;
  }

  public static boolean isShowGenStatus() {
    return ourShowGenStatus;
  }

  //----UI----

  private class MyScrollPane extends JBScrollPane implements DataProvider {
    private MyScrollPane(Component view) {
      super(view);
    }

    @Override
    @Nullable
    public Object getData(@NonNls String dataId) {
      return ProjectPane.this.getData(dataId);
    }
  }

  private class PaneActivator implements Runnable {
    private boolean myRunReadAction;

    private PaneActivator(boolean runReadAction) {
      myRunReadAction = runReadAction;
    }

    @Override
    public final void run() {
      getProjectView().changeView(getId());
      myUpdateQueue.queue(new AbstractUpdate(UpdateID.SELECT) {
        @Override
        public void run() {
          // TODO: check if we need running read action here, or should we better do it inside myFindHelper methods.
          if (myRunReadAction) {
            ModelAccess.instance().runReadAction(new Runnable() {
              @Override
              public void run() {
                doOnPaneActivation();
              }
            });
          } else {
            doOnPaneActivation();
          }
        }
      });
    }

    protected void doOnPaneActivation() {
    }
  }

  public interface ComponentCreationListener {
    void componentCreated(ProjectPane projectPane);
  }

  private enum UpdateID {
    REBUILD(20),
    SELECT(30),
    RESTORE_EXPAND(40);

    private int myPriority;

    UpdateID(int priority) {
      myPriority = priority;
    }

    public int getPriority() {
      return myPriority;
    }
  }

  private abstract class AbstractUpdate extends Update {
    private AbstractUpdate(UpdateID id) {
      super(id, id.getPriority());
    }
  }
}
