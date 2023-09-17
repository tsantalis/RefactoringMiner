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
package com.addthis.hydra.task.output.tree;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.addthis.basis.util.LessStrings;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.core.BundleFactory;
import com.addthis.bundle.core.BundleFormat;
import com.addthis.bundle.core.BundleFormatted;
import com.addthis.hydra.data.tree.DataTreeNode;
import com.addthis.hydra.data.tree.DataTreeNodeInitializer;
import com.addthis.hydra.data.tree.DataTreeNodeUpdater;

import com.addthis.hydra.data.tree.TreeDataParent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@SuppressWarnings("serial")
public final class TreeMapState implements DataTreeNodeUpdater, DataTreeNodeInitializer, BundleFactory, BundleFormatted {

    private static final Logger log = LoggerFactory.getLogger(TreeMapState.class);
    private static final int debug = Integer.parseInt(System.getProperty("hydra.process.debug", "0"));
    private static final boolean debugthread = System.getProperty("hydra.process.debugthread", "0").equals("1");
    // TODO: remove any uses of reference equality to this value and redefine as a proper empty list
    private static final List<DataTreeNode> empty = Collections.unmodifiableList(new ArrayList<>());

    static {
        if (debugthread) {
            log.warn("ENABLED debugthread");
        }
    }

    /**
     * test harness constructor
     */
    public TreeMapState(Bundle p) {
        this.bundle = p;
        this.path = null;
        this.processor = null;
        this.stack = null;
        this.thread = null;
        this.profiling = false;
    }

    /** */
    public TreeMapState(TreeMapper processor, DataTreeNode rootNode, PathElement[] path, Bundle bundle) {
        this.path = path;
        this.bundle = bundle;
        this.processor = processor;
        this.countValue = 1;
        this.stack = new LinkedList<>();
        this.thread = Thread.currentThread();
        this.profiling = processor != null ? processor.isProfiling() : false;
        push(rootNode);
    }

    private final LinkedList<DataTreeNode> stack;
    private final TreeMapper processor;
    private final PathElement[] path;
    private final Bundle bundle;
    private final Thread thread;
    private final boolean profiling;

    private boolean lastWasNew;
    private int touched;
    private int countValue;
    private long assignmentValue;

    private void checkThread() {
        if (Thread.currentThread() != thread) {
            throw new RuntimeException("invalid accessing thread " + Thread.currentThread() + " != " + thread);
        }
    }

    public static List<DataTreeNode> empty() {
        return empty;
    }

    @Override
    public int getCountValue() {
        return countValue;
    }

    public void setCountValue(int v) {
        countValue = v;
    }

    @Override
    public long getAssignmentValue() {
        return assignmentValue;
    }

    public TreeMapState setAssignmentValue(long assignmentValue) {
        this.assignmentValue = assignmentValue;
        return this;
    }

    public boolean getAndClearLastWasNew() {
        boolean ret = lastWasNew;
        lastWasNew = false;
        return ret;
    }

    public DataTreeNode getLeasedNode(String key) {
        DataTreeNode tn = current().getLeasedNode(key);
        return tn;
    }

    public DataTreeNode getOrCreateNode(String key, DataTreeNodeInitializer init, TreeDataParent path) {
        DataTreeNode tn = current().getOrCreateNode(key, init, path);
        return tn;
    }

    public DataTreeNode pop() {
        if (debugthread) {
            checkThread();
        }
        return stack.pop();
    }

    public int getNodeCount() {
        return current().getNodeCount();
    }

    public void push(List<DataTreeNode> tnl) {
        if (tnl.size() == 1) {
            push(tnl.get(0));
        } else {
            throw new RuntimeException("unexpected response: " + tnl.size() + " -> " + tnl);
        }
    }

    public void push(DataTreeNode tn) {
        if (debugthread) {
            checkThread();
        }
        stack.push(tn);
    }

    /** */
    public long getBundleTime() {
        throw new RuntimeException("fix me");
    }

    public void setBundleTime(long time) {
        throw new RuntimeException("fix me");
    }

    /** */
    public DataTreeNode peek(int back) {
        return (stack.size() > back) ? stack.get(back) : null;
    }

    /** */
    public DataTreeNode current() {
        return stack.peek();
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    /** */
    public int touched() {
        return touched;
    }

    /**
     * used by path elements to schedule delivering the current bundle to a new
     * rule, usually as part of conditional processing. currently called from
     * PathCall and PathEachChild
     */
    public void dispatchRule(TreeMapperPathReference t) {
        if ((t != null) && (bundle != null) && (processor != null)) {
            processor.processBundle(bundle, t);
        } else if (debug > 0) {
            log.warn("Proc Rule Dispatch DROP {} b/c p={} rp={}", t, bundle, processor);
        }
    }

    public void process() {
        List<DataTreeNode> list = processPath(path, 0);
        if ((debug > 0) && ((list == null) || list.isEmpty())) {
            log.warn("proc FAIL {}", list);
            log.warn(".... PATH {}", LessStrings.join(path, " // "));
            log.warn(".... PACK {}", bundle);
        }
        if (list != null) {
            list.forEach(DataTreeNode::release);
        }
    }

    /**
     * called from PathCall.processNode(), PathCombo.processNode() and
     * PathEach.processNode()
     */
    public List<DataTreeNode> processPath(PathElement[] path) {
        return processPath(path, 0);
    }

    /** */
    @Nullable private List<DataTreeNode> processPath(PathElement[] path, int index) {
        if ((path == null) || (path.length <= index)) {
            return null;
        }
        List<DataTreeNode> nodes = processPathElement(path[index]);
        if ((nodes != null) && nodes.isEmpty()) {
            // we get here from op elements, each elements, etc
            if ((index + 1) < path.length) {
                return processPath(path, index + 1);
            }
            return null;
        } else if (nodes == null) {
            return null;
        } else if ((index + 1) < path.length) {
            List<DataTreeNode> childNodes = null;
            for (DataTreeNode tn : nodes) {
                push(tn);
                List<DataTreeNode> childNodesPartition = processPath(path, index + 1);
                if (childNodesPartition != null) {
                    if ((childNodes == null) || (childNodes == empty)) {
                        childNodes = childNodesPartition;
                    } else {
                        childNodes.addAll(childNodesPartition);
                    }
                }
                pop().release();
            }
            return childNodes;
        } else {
            return nodes;
        }
    }

    /**
     * called from this.processPath(), PathEach.processNode() and
     * PathSplit.processNode()
     */
    public List<DataTreeNode> processPathElement(PathElement pe) {
        if (profiling) {
            long mark = System.nanoTime();
            List<DataTreeNode> list = processPathElementProfiled(pe);
            processor.updateProfile(pe, System.nanoTime() - mark);
            return list;
        } else {
            return processPathElementProfiled(pe);
        }
    }

    private List<DataTreeNode> processPathElementProfiled(PathElement pe) {
        if (pe.disabled()) {
            return empty();
        }
        if (debugthread) {
            checkThread();
        }
        List<DataTreeNode> list = pe.processNode(this);
        if (list != null) {
            touched += list.size();
        }
        return list;
    }

    @Override
    public void onNewNode(DataTreeNode child) {
        lastWasNew = true;
    }

    @Override
    public Bundle createBundle() {
        return processor.createBundle();
    }

    @Override
    public BundleFormat getFormat() {
        return processor.getFormat();
    }

    public boolean processorClosing() { return processor.isClosing(); }
}
