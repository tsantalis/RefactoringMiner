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
package jetbrains.mps.ide.projectView;

import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import jetbrains.mps.icons.MPSIcons.ProjectPane;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@State(
    name="ProjectView",
    storages= {
        @Storage(
            file = StoragePathMacros.WORKSPACE_FILE
        )}
)
public class ProjectViewImpl extends com.intellij.ide.projectView.impl.ProjectViewImpl {

  public ProjectViewImpl(@NotNull Project project,
      FileEditorManager fileEditorManager, ToolWindowManagerEx toolWindowManager) {
    super(project, fileEditorManager, toolWindowManager);
  }

  @Override
  public synchronized void setupImpl(@NotNull ToolWindow toolWindow, boolean loadPaneExtensions) {
    super.setupImpl(toolWindow, loadPaneExtensions);
    // override the superclass's logic for loading expanded tree paths

    toolWindow.getContentManager().addContentManagerListener(new ContentManagerAdapter() {
      @Override
      public void selectionChanged(ContentManagerEvent event) {
        if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
          viewSelectionChangedOverride();
        }
      }
    });
    viewSelectionChangedOverride();
  }

  @Override
  public synchronized void removeProjectPane(@NotNull AbstractProjectViewPane pane) {
    super.removeProjectPane(pane);
    // override the superclass's logic for loading expanded tree paths
    viewSelectionChangedOverride();
  }

  @Override
  public Element getState() {
    // simply forward to the superclass's implementation
    // we mimic the IDEA's mechanism to store/load tree state
    return super.getState();
  }

  @Override
  public void loadState(Element parentNode) {
    // simply forward to the superclass's implementation
    // we mimic the IDEA's mechanism to store/load tree state
    super.loadState(parentNode);
  }

  private boolean viewSelectionChangedOverride() {
    // the current view ID is set in the super.showPane(), which must be called before
    final AbstractProjectViewPane newPane = getProjectViewPaneById(getCurrentViewId());
    if (newPane == null) return false;
    if (!(newPane instanceof ProjectViewPaneOverride)) return false;

    ((ProjectViewPaneOverride) newPane).restoreExpandedPathsOverride();
    return true;
  }

  @Override
  protected boolean isShowMembersOptionSupported() {
    return false;
  }
}
