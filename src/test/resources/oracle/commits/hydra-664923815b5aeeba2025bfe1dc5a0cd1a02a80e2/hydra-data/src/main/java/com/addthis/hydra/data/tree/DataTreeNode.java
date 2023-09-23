/*
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
package com.addthis.hydra.data.tree;

import java.util.Map;

import com.addthis.basis.util.ClosableIterator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;


@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE)
public interface DataTreeNode extends Iterable<DataTreeNode> {

    /** Returns the name of this node. This node can be found by querying for this against its parent node. */
    public String getName();

    /** Returns the tree that this node belongs to. */
    public DataTree getTreeRoot();

    /** Returns the number of child nodes. */
    public int getNodeCount();

    /** Returns the number of "hits". "Hits" are increments to the node's intrinsic counter. */
    public long getCounter();

    /** Returns data attachment (if any) with the given name. */
    public DataTreeNodeActor getData(String key);

    /** Return node if it exists. Does not create otherwise. returned node is read only -- do not call release(). */
    public DataTreeNode getNode(String name);

    /** Returns the map of data attachments. */
    public Map<String, TreeNodeData> getDataMap();

    /** Returns an iterator of all child nodes. */
    public ClosableIterator<DataTreeNode> getIterator();

    /** Returns an iterator over the set of child nodes with the matching prefix. */
    public ClosableIterator<DataTreeNode> getIterator(String prefix);

    /** Returns an iterator over child nodes whose names are in the range [from, to).
     * The 'from' is inclusive and the 'to' is exclusive. */
    public ClosableIterator<DataTreeNode> getIterator(String from, String to);

    // Mutation Methods

    /** atomically increment intrinsic counter */
    public default void incrementCounter() {
        throw new UnsupportedOperationException("incrementCounter");
    }

    /** atomically increment intrinsic counter and return new value */
    public default long incrementCounter(long val) {
        throw new UnsupportedOperationException("incrementCounter");
    }

    /** set value of intrinsic counter */
    public default void setCounter(long val) {
        throw new UnsupportedOperationException("setCounter");
    }

    /**
     * TODO for immediate compatibility -- rethink this
     */
    public default void updateChildData(DataTreeNodeUpdater state, TreeDataParent path) {
        throw new UnsupportedOperationException("updateChildData");
    }

    /**
     * TODO for immediate compatibility -- rethink this
     */
    public default void updateParentData(DataTreeNodeUpdater state, DataTreeNode child, boolean isnew) {
        throw new UnsupportedOperationException("updateParentData");
    }

    /** Make this node an alias (link) to another node. This can succeed only if this node currently has no children. */
    public default boolean aliasTo(DataTreeNode target) {
        throw new UnsupportedOperationException("aliasTo");
    }

    /** Attempts to delete named child node. Returns true if node existed and was successfully deleted. */
    public default boolean deleteNode(String node) {
        throw new UnsupportedOperationException("deleteNode");
    }

    // Leasing / Locking methods

    /**
     * return node if it exists, create and return new otherwise.
     * returned node is read/write.  MUST call release() when complete to commit changes OR discard.
     */
    public default DataTreeNode getOrCreateNode(String name, DataTreeNodeInitializer init, TreeDataParent path) {
        throw new UnsupportedOperationException("getOrCreateNode");
    }

    /**
     * return node if it exists, do not create otherwise.
     * returned node is mutable.  MUST call release().
     */
    public default DataTreeNode getLeasedNode(String name) {
        throw new UnsupportedOperationException("getLeasedNode");
    }

    /** TODO temporary workaround.  MUST call ONLY for nodes retrieved via getOrCreateNode(). */
    public default void release() {
        throw new UnsupportedOperationException("release");
    }

    public default void writeLock() {
        throw new UnsupportedOperationException("writeLock");
    }

    public default void writeUnlock() {
        throw new UnsupportedOperationException("writeUnlock");
    }
}
