/*
 * Copyright 2003-2011 JetBrains s.r.o.
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
package jetbrains.mps.nodeEditor.cellProviders;

import jetbrains.mps.nodeEditor.cellActions.CellAction_InsertIntoCollection;
import jetbrains.mps.nodeEditor.cellLayout.CellLayout;
import jetbrains.mps.nodeEditor.cellLayout.CellLayout_Horizontal;
import jetbrains.mps.nodeEditor.cellLayout.CellLayout_Vertical;
import jetbrains.mps.nodeEditor.cells.EditorCell_Collection;
import jetbrains.mps.openapi.editor.EditorContext;
import jetbrains.mps.openapi.editor.cells.CellActionType;
import jetbrains.mps.openapi.editor.cells.EditorCell;
import org.jetbrains.mps.openapi.model.SNode;

import java.util.Iterator;
import java.util.List;

public abstract class AbstractCellListHandler {
  public static final String ELEMENT_CELL_ACTIONS_SET = "element-cell-actions-set";

  protected SNode myOwnerNode;
  protected EditorContext myEditorContext;
  protected EditorCell_Collection myListEditorCell_Collection;
  protected String myElementRole;

  public AbstractCellListHandler(SNode ownerNode, String elementRole, EditorContext editorContext) {
    myOwnerNode = ownerNode;
    myElementRole = elementRole;
    myEditorContext = editorContext;
  }

  public EditorContext getEditorContext() {
    return myEditorContext;
  }

  public SNode getOwner() {
    return myOwnerNode;
  }

  public String getElementRole() {
    return myElementRole;
  }

  protected abstract SNode getAnchorNode(EditorCell anchorCell);

  /**
   * After MPS 3.2 remove this method.
   * Left here for compatibility reasons with the existing code.
   *
   * @deprecated since MPS 3.2 all subclasses should override doInsertNode(SNode nodeToInsert, SNode anchorNode, boolean insertBefore)
   */
  @Deprecated
  protected void doInsertNode(SNode anchorNode, boolean insertBefore) {
  }

  /**
   * TODO: make this method abstract after MPS 3.2
   * All sub-classes should override this method starting from MPS 3.2
   */
  protected void doInsertNode(SNode nodeToInsert, SNode anchorNode, boolean insertBefore) {
    doInsertNode(anchorNode, insertBefore);
  }

  /**
   * After MPS 3.2 move all code from the body of this method into insertNewChild() and remove this method.
   *
   * @deprecated since MPS 3.2 use insertNewChild() instead.
   */
  @Deprecated
  public void startInsertMode(EditorContext editorContext, EditorCell anchorCell, boolean insertBefore) {
    SNode anchorNode = getAnchorNode(anchorCell);
    SNode nodeToInsert = createNodeToInsert(editorContext);
    doInsertNode(nodeToInsert, anchorNode, insertBefore);
  }

  public void insertNewChild(EditorContext editorContext, EditorCell anchorCell, boolean insertBefore) {
    startInsertMode(editorContext, anchorCell, insertBefore);
  }

  public abstract EditorCell createNodeCell(EditorContext editorContext, SNode node);

  protected EditorCell createSeparatorCell(EditorContext editorContext, SNode prevNode, SNode nextNode) {
    return null;
  }

  protected abstract EditorCell createEmptyCell(EditorContext editorContext);

  public abstract SNode createNodeToInsert(EditorContext editorContext);

  public EditorCell_Collection createCells_Vertical(EditorContext editorContext) {
    return createCells(editorContext, new CellLayout_Vertical());
  }

  public EditorCell_Collection createCells_Horizontal(EditorContext editorContext) {
    return createCells(editorContext, new CellLayout_Horizontal());
  }

  public EditorCell_Collection createCells(EditorContext editorContext, CellLayout cellLayout, boolean selectable) {
    EditorCell_Collection cellsCollection = createCells(editorContext, cellLayout);
    if (!selectable) {
      return cellsCollection;
    }

    // if the list compartment is selectable - create wrapping cell collection around it so
    // that actions intended to work for the list element do not work for the list owner.
    EditorCell_Collection wrapperCell = EditorCell_Collection.create(editorContext, myOwnerNode, new CellLayout_Horizontal(), null);
    wrapperCell.setSelectable(true);
    wrapperCell.addEditorCell(cellsCollection);
    return wrapperCell;
  }

  public EditorCell_Collection createCells(EditorContext editorContext, CellLayout cellLayout) {
    myListEditorCell_Collection = EditorCell_Collection.create(editorContext, myOwnerNode, cellLayout, this);
    myListEditorCell_Collection.setSelectable(false);

    createInnerCells(myOwnerNode, editorContext);

    // add insert/insert-before actions
    myListEditorCell_Collection.setAction(CellActionType.INSERT, new CellAction_InsertIntoCollection(this, false));
    myListEditorCell_Collection.setAction(CellActionType.INSERT_BEFORE, new CellAction_InsertIntoCollection(this, true));

    return myListEditorCell_Collection;
  }

  protected void createInnerCells(SNode node, EditorContext editorContext) {
    Iterator<? extends SNode> listNodes = getNodesForList().iterator();
    if (!listNodes.hasNext()) {
      EditorCell emptyCell = createEmptyCell(editorContext);
      emptyCell.setRole(getElementRole());
      myListEditorCell_Collection.addEditorCell(emptyCell);
    } else {
      SNode prevNode = null;
      while (listNodes.hasNext()) {
        SNode nextNode = listNodes.next();
        addSeparatorCell(editorContext, prevNode, nextNode);
        myListEditorCell_Collection.addEditorCell(createNodeCell(editorContext, nextNode));
        prevNode = nextNode;
      }
    }
  }

  protected abstract List<? extends SNode> getNodesForList();

  private void addSeparatorCell(EditorContext editorContext, SNode prevNode, SNode nextNode) {
    if (prevNode == null) {
      return;
    }
    EditorCell separatorCell = createSeparatorCell(editorContext, prevNode, nextNode);
    if (separatorCell != null) {
      myListEditorCell_Collection.addEditorCell(separatorCell);
    }
  }
}
