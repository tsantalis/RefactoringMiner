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
package com.addthis.hydra.data.tree.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.addthis.basis.util.ClosableIterator;
import com.addthis.basis.util.MemoryCounter.Mem;

import com.addthis.hydra.data.tree.AbstractTreeNode;
import com.addthis.hydra.data.tree.DataTreeNode;
import com.addthis.hydra.data.tree.DataTreeNodeActor;
import com.addthis.hydra.data.tree.DataTreeNodeInitializer;
import com.addthis.hydra.data.tree.DataTreeNodeUpdater;
import com.addthis.hydra.data.tree.TreeDataParameters;
import com.addthis.hydra.data.tree.TreeDataParent;
import com.addthis.hydra.data.tree.TreeNodeData;
import com.addthis.hydra.data.tree.TreeNodeDataDeferredOperation;
import com.addthis.hydra.store.db.DBKey;
import com.addthis.hydra.store.db.IPageDB.Range;


/**
 * Each instance has an AtomicInteger 'lease' that records the current
 * activity of a node. Values of lease signify the following behavior:
 *
 * N (for N > 0) : There are N threads that may be modifying the node. The node is active.
 * 0             : The node is idle. It may be evicted.
 * -1            : The node is currently being evicted. This is a transient state.
 * -2            : The node has been deleted.
 * -3            : The node has been evicted.
 *
 * Only an idle node may be evicted. Any node that has 0 or more leases
 * can be deleted (yes it is counterintuitive but for legacy purposes
 * deleting nodes is a higher priority operation that modifying nodes).
 *
 */
public class ConcurrentTreeNode extends AbstractTreeNode {

    public static final int ALIAS = 1 << 1;

    public static ConcurrentTreeNode getTreeRoot(ConcurrentTree tree) {
        ConcurrentTreeNode node = new ConcurrentTreeNode() {
            @Override
            void requireEditable() {

            }
        };
        node.tree = tree;
        node.leases.incrementAndGet();
        node.nodedb = 1L;
        return node;
    }

    /**
     * required for Codable. must be followed by an init() call.
     */
    public ConcurrentTreeNode() {
    }

    protected void initIfDecoded(ConcurrentTree tree, DBKey key, String name) {
        if (decoded.get()) {
            synchronized (initLock) {
                if (initOnce.compareAndSet(false, true)) {
                    this.tree = tree;
                    this.dbkey = key;
                    this.name = name;
                    decoded.set(false);
                }
            }
        }
    }

    protected void init(ConcurrentTree tree, DBKey key, String name, TreeDataParent path) {
        this.tree = tree;
        this.dbkey = key;
        this.name = name;
        // only create data attachment map
        // if there is data to put into it
        if (path != null && path.dataConfig() != null && !path.dataConfig().isEmpty()) {
            createMap(path.dataConfig().size());
        }
    }

    @Mem(estimate = false, size = 64)
    private ConcurrentTree tree;
    @Mem(estimate = false, size = 64)
    private AtomicInteger leases = new AtomicInteger(0);
    @Mem(estimate = false, size = 64)
    private AtomicBoolean changed = new AtomicBoolean(false);
    @Mem(estimate = false, size = 64)
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    @Mem(estimate = false, size = 64)
    private AtomicLong atomicHits = new AtomicLong();

    private AtomicBoolean decoded = new AtomicBoolean(false);
    private AtomicBoolean initOnce = new AtomicBoolean(false);
    private final Object initLock = new Object();

    protected String name;
    protected DBKey dbkey;

    public String toString() {
        return "TN[k=" + dbkey + ",db=" + nodedb + ",n#=" + nodes + ",h#=" + hits +
               ",nm=" + name + ",le=" + leases + ",ch=" + changed + ",bi=" + bits + "]";
    }

    @Override
    public byte[] bytesEncode(long version) {
        this.hits = atomicHits.get();
        return super.bytesEncode(version);
    }

    @Override public String getName() {
        return name;
    }

    @Override @SuppressWarnings("unchecked")
    public Map<String, TreeNodeData> getDataMap() {
        return data;
    }

    public int getLeaseCount() {
        return leases.get();
    }

    /**
     * The synchronized methods protecting the {@code nodes} field
     * is a code smell. This should probably be protected by the
     * encoding reader/writer {@code lock} field. There is an invariant
     * for the page storage system that the encoding (write) locks of two nodes
     * cannot be held simultaneously and switching to the encoding lock
     * for these methods may violate the invariant.
     */

    protected synchronized int incrementNodeCount() {
        return nodes++;
    }

    protected synchronized int updateNodeCount(int delta) {
        nodes += delta;
        changed.set(true);
        return nodes;
    }

    void requireEditable() {
        int count = leases.get();
        if (!(count == -2 || count > 0)) {
            throw new RuntimeException("fail editable requirement: lease state is " + count);
        }
    }

    public final boolean isBitSet(int bitcheck) {
        return (bits & bitcheck) == bitcheck;
    }

    public boolean isAlias() {
        return isBitSet(ALIAS);
    }

    public boolean isDeleted() {
        int count = leases.get();
        return count == -2;
    }

    public void markChanged() {
        requireEditable();
        changed.set(true);
    }

    protected boolean markDeleted() {
        return leases.getAndSet(-2) != -2;
    }

    protected void evictionComplete() {
        leases.compareAndSet(-1, -3);
    }

    protected synchronized void markAlias() {
        bitSet(ALIAS);
    }

    protected boolean isChanged() {
        return changed.get();
    }

    private final void bitSet(int set) {
        bits |= set;
    }

    private final void bitUnset(int set) {
        bits &= (~set);
    }

    /**
     * A node is reactivated when it is retrieved from the backing storage
     * and its state transitions from the inactive state to the active state
     * with 0 leases.
     *
     */
    void reactivate() {
        while(true) {
            int count = leases.get();
            if (count == -3 && leases.compareAndSet(-3, 0)) {
                return;
            } else if (count == -1 && leases.compareAndSet(-1, 0)) {
                return;
            } else if (count != -3 && count != -1) {
                return;
            }
        }
    }

    /**
     * Atomically try to acquire a lease. If the node is either
     * in the inactive state or the deleted state then a lease
     * cannot be acquired.
     *
     * @return {@code true} if a lease is acquired
     */
    boolean tryLease() {
        while (true) {
            int count = leases.get();
            if (count < 0) {
                return false;
            }
            if (leases.compareAndSet(count, count + 1)) {
                return true;
            }
        }
    }

    /**
     * The node can be evicted if it has been deleted
     * or it transitions from the active leases with 0 leases
     * to the inactive state. If the node is already in the
     * inactive state then it cannot be evicted.
     *
     * @return true if the node can be evicted
     */
    boolean trySetEviction() {
        while (true) {
            int count = leases.get();
            if (count == -2) {
                return true;
            } else if (count != 0) {
                return false;
            }
            if (leases.compareAndSet(0, -1)) {
                return true;
            }
        }
    }

    /**
     * Atomically decrement the number of active leases.
     */
    @Override
    public void release() {
        while (true) {
            int count = leases.get();
            if (count <= 0) {
                return;
            }
            if (leases.compareAndSet(count, count - 1)) {
                return;
            }
        }
    }

    /**
     * double-checked locking idiom to avoid unnecessary synchronization.
     */
    protected void requireNodeDB() {
        if (!hasNodes()) {
            synchronized (this) {
                if (!hasNodes()) {
                    nodedb = tree.getNextNodeDB();
                }
            }
        }
    }

    protected long nodeDB() {
        return nodedb;
    }

    /**
     * returns an iterator of read-only nodes
     */
    public ClosableIterator<DataTreeNode> getNodeIterator() {
        return !hasNodes() || isDeleted() ? new Iter(null, false) : new Iter(tree.fetchNodeRange(nodedb), true);
    }

    /**
     * returns an iterator of read-only nodes
     */
    public ClosableIterator<DataTreeNode> getNodeIterator(String prefix) {
        if (!isDeleted() && prefix != null && prefix.length() > 0) {
            /**
             * the reason this behaves as a prefix as opposed to a "from" is the way
             * the "to" endpoint is calculated.  the last byte of the prefix is incremented
             * by a value of one and used as "to".  this is the effect of excluding all
             * other potential matches with a lexicographic value greater than prefix.
             */
            StringBuilder sb = new StringBuilder(prefix.substring(0, prefix.length() - 1));
            sb.append((char) (prefix.charAt(prefix.length() - 1) + 1));
            return getNodeIterator(prefix, sb.toString());
        } else {
            return new Iter(null, false);
        }
    }

    /**
     * returns an iterator of read-only nodes
     */
    public ClosableIterator<DataTreeNode> getNodeIterator(String from, String to) {
        if (!hasNodes() || isDeleted()) {
            return new Iter(null, false);
        }
        return new Iter(tree.fetchNodeRange(nodedb, from, to), true);
    }

    @Override public ConcurrentTreeNode getNode(String name) {
        return tree.getNode(this, name, false);
    }

    @Override public ConcurrentTreeNode getLeasedNode(String name) {
        return tree.getNode(this, name, true);
    }

    public DataTreeNode getOrCreateEditableNode(String name) {
        return getOrCreateEditableNode(name, null, null);
    }

    public DataTreeNode getOrCreateEditableNode(String name, DataTreeNodeInitializer creator, TreeDataParent path) {
        return tree.getOrCreateNode(this, name, creator, path);
    }

    @Override public boolean deleteNode(String name) {
        return tree.deleteNode(this, name);
    }

    /**
     * link this node (aliasing) to another node in the tree. they will share
     * children, but not meta-data. should only be called from within a
     * TreeNodeInitializer passed to getOrCreateEditableNode.
     */
    @Override
    public boolean aliasTo(DataTreeNode node) {
        if (node.getClass() != ConcurrentTreeNode.class) {
            return false;
        }
        requireEditable();
        if (hasNodes()) {
            return false;
        }
        ((ConcurrentTreeNode) node).requireNodeDB();
        nodedb = ((ConcurrentTreeNode) node).nodedb;
        markAlias();
        return true;
    }

    protected HashMap<String, TreeNodeData> createMap(int size) {
        if (data == null) {
            data = new HashMap<>(size);
        }
        return data;
    }

    /**
     * TODO: warning. if you annotate a path with data then have another path
     * that intersects that node in the tree with some other data, the first one
     * wins and the new data will not be added. further, every time the
     * annotated node is crossed, the attached data will be updated if that path
     * declares annotated data.
     */
    @Override @SuppressWarnings("unchecked")
    public void updateChildData(DataTreeNodeUpdater state, TreeDataParent path) {
        requireEditable();
        boolean updated = false;
        HashMap<String, TreeDataParameters> dataconf = path.dataConfig();
        if (data == null || data.isEmpty()) {
            // no lock required to update atomic values
            updateHits(state, path);
        } else {
            lock.writeLock().lock();
            try {
                updated = updateHits(state, path);
                if (dataconf != null) {
                    if (data == null) {
                        data = new HashMap<>(dataconf.size());
                    }
                    for (Entry<String, TreeDataParameters> el : dataconf.entrySet()) {
                        TreeNodeData tnd = data.get(el.getKey());
                        if (tnd == null) {
                            tnd = el.getValue().newInstance(this);
                            data.put(el.getKey(), tnd);
                            updated = true;
                        }
                        if (tnd.updateChildData(state, this, el.getValue())) {
                            updated = true;
                        }
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        if (updated) {
            changed.set(true);
        }
    }

    public boolean updateHits(DataTreeNodeUpdater state, TreeDataParent path) {
        boolean updated = false;
        if (path.assignHits()) {
            atomicHits.set(state.getAssignmentValue());
            updated = true;
        } else if (path.countHits()) {
            atomicHits.addAndGet(state.getCountValue());
            updated = true;
        }
        return updated;
    }

    @Override public void updateParentData(DataTreeNodeUpdater state, DataTreeNode child, boolean isnew) {
        List<TreeNodeDataDeferredOperation> deferredOps = null;
        // since data is initialized at start time we don't need the write lock
        // unless data is non null and non empty because if it is empty
        // there is nothing to update.
        if (child != null && data != null) {
            requireEditable();
            if (!data.isEmpty()) {
                lock.writeLock().lock();
                try {
                    deferredOps = new ArrayList<>(1);
                    for (TreeNodeData<?> tnd : data.values()) {
                        if (isnew && tnd.updateParentNewChild(state, this, child, deferredOps)) {
                            changed.set(true);
                        }
                        if (tnd.updateParentData(state, this, child, deferredOps)) {
                            changed.set(true);
                        }
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }
        if (deferredOps != null) {
            for (TreeNodeDataDeferredOperation currentOp : deferredOps) {
                currentOp.run();
            }
        }
    }

    // TODO concurrent broken -- data classes should be responsible for their
    // own get/update sync
    @Override public DataTreeNodeActor getData(String key) {
        lock.readLock().lock();
        try {
            return data != null ? data.get(key) : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    // TODO concurrent broken -- data classes should be responsible for their
    // own get/update sync
    public Collection<String> getDataFields() {
        lock.readLock().lock();
        try {
            if (data == null || data.size() == 0) {
                return null;
            }
            return data.keySet();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getNodeCount() {
        return nodes;
    }

    @Override
    public void postDecode() {
        super.postDecode();
        atomicHits.set(hits);
        decoded.set(true);
    }

    /**
     * TODO warning: not thread safe. sync around next(), hasNext() when
     * concurrency is required.
     */
    private final class Iter implements ClosableIterator<DataTreeNode> {

        private Range<DBKey, ConcurrentTreeNode> range;
        private ConcurrentTreeNode next;
        private boolean filterDeleted;

        private Iter(Range<DBKey, ConcurrentTreeNode> range, boolean filterDeleted) {
            this.range = range;
            this.filterDeleted = filterDeleted;
            fetchNext();
        }

        public String toString() {
            return "Iter(" + range + "," + next + ")";
        }

        void fetchNext() {
            if (range != null) {
                next = null;
                while (range.hasNext()) {
                    Entry<DBKey, ConcurrentTreeNode> tne = range.next();
                    next = tree.getNode(ConcurrentTreeNode.this, tne.getKey().rawKey().toString(), false);
                    if (next != null) {
                        if (filterDeleted && next.isDeleted()) {
                            next = null;
                            continue;
                        }
                        break;
                    }
                }
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public ConcurrentTreeNode next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            ConcurrentTreeNode ret = next;
            fetchNext();
            return ret;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            if (range != null) {
                range.close();
                range = null;
            }
        }
    }

    @Override
    public void encodeLock() {
        lock.readLock().lock();
    }

    @Override
    public void encodeUnlock() {
        lock.readLock().unlock();
    }


    @Override
    public void writeLock() {
        lock.writeLock().lock();
    }

    @Override
    public void writeUnlock() {
        lock.writeLock().unlock();
    }

    @Override
    public Iterator<DataTreeNode> iterator() {
        return getNodeIterator();
    }

    @Override
    public ClosableIterator<DataTreeNode> getIterator() {
        return getNodeIterator();
    }

    @Override
    public ConcurrentTree getTreeRoot() {
        return tree;
    }

    @Override
    public ClosableIterator<DataTreeNode> getIterator(String begin) {
        return getNodeIterator(begin);
    }

    @Override
    public ClosableIterator<DataTreeNode> getIterator(String from, String to) {
        return getNodeIterator(from, to);
    }

    @Override
    public DataTreeNode getOrCreateNode(String name, DataTreeNodeInitializer init, TreeDataParent path) {
        return getOrCreateEditableNode(name, init, path);
    }

    /**
     * The synchronized methods protecting the {@code counter} field
     * is a code smell. This should probably be protected by the
     * encoding reader/writer {@code lock} field. There is an invariant
     * for the page storage system that the encoding (write) locks of two nodes
     * cannot be held simultaneously and switching to the encoding lock
     * for these methods may violate the invariant.
     */

    @Override
    public long getCounter() {
        return atomicHits.get();
    }

    @Override
    public void incrementCounter() {
        atomicHits.incrementAndGet();
    }

    @Override
    public long incrementCounter(long val) {
        return atomicHits.addAndGet(val);
    }

    @Override
    public void setCounter(long val) {
        atomicHits.set(val);
    }
}
