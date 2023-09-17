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

import javax.annotation.concurrent.GuardedBy;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import com.addthis.basis.concurrentlinkedhashmap.MediatedEvictionConcurrentHashMap;
import com.addthis.basis.util.LessBytes;
import com.addthis.basis.util.ClosableIterator;
import com.addthis.basis.util.LessFiles;
import com.addthis.basis.util.Meter;
import com.addthis.basis.util.Parameter;

import com.addthis.hydra.common.Configuration;
import com.addthis.hydra.data.tree.DataTree;
import com.addthis.hydra.data.tree.DataTreeNode;
import com.addthis.hydra.data.tree.DataTreeNodeActor;
import com.addthis.hydra.data.tree.DataTreeNodeInitializer;
import com.addthis.hydra.data.tree.DataTreeNodeUpdater;
import com.addthis.hydra.data.tree.TreeDataParent;
import com.addthis.hydra.data.tree.TreeNodeData;
import com.addthis.hydra.store.db.CloseOperation;
import com.addthis.hydra.store.db.DBKey;
import com.addthis.hydra.store.db.IPageDB;
import com.addthis.hydra.store.db.PageDB;
import com.addthis.hydra.store.kv.PagedKeyValueStore;
import com.addthis.hydra.store.skiplist.Page;
import com.addthis.hydra.store.skiplist.PageFactory;
import com.addthis.hydra.store.skiplist.SkipListCache;
import com.addthis.hydra.store.util.MeterFileLogger;
import com.addthis.hydra.store.util.MeterFileLogger.MeterDataSource;
import com.addthis.hydra.store.util.NamedThreadFactory;
import com.addthis.hydra.store.util.Raw;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AtomicDouble;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Gauge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @user-reference
 */
public final class ConcurrentTree implements DataTree, MeterDataSource {
    static final Logger log = LoggerFactory.getLogger(ConcurrentTree.class);

    // number of background deletion threads
    @Configuration.Parameter
    static final int defaultNumDeletionThreads = Parameter.intValue("hydra.tree.clean.threads", 1);

    // sleep interval of deletion threads in between polls of deletion queue
    @Configuration.Parameter
    static final int deletionThreadSleepMillis = Parameter.intValue("hydra.tree.clean.interval", 10);

    // number of nodes in between trash removal logging messages
    @Configuration.Parameter
    static final int deletionLogInterval = Parameter.intValue("hydra.tree.clean.logging", 100000);

    private static final AtomicInteger scopeGenerator = new AtomicInteger();

    private final String scope = "ConcurrentTree" + Integer.toString(scopeGenerator.getAndIncrement());

    public static enum METERTREE {
        CACHE_HIT, CACHE_MISS, NODE_PUT, NODE_CREATE, NODE_DELETE, SOURCE_MISS
    }

    static final String keyCacheGet = METERTREE.CACHE_HIT.toString();
    static final String keyCacheMiss = METERTREE.CACHE_MISS.toString();

    private final File root;
    private final File idFile;
    final IPageDB<DBKey, ConcurrentTreeNode> source;
    private final ConcurrentTreeNode treeRootNode;
    final ConcurrentTreeNode treeTrashNode;
    private final AtomicLong nextDBID;
    final AtomicBoolean closed = new AtomicBoolean(false);
    private final Meter<METERTREE> meter;
    private final MeterFileLogger logger;
    private final AtomicDouble cacheHitRate = new AtomicDouble(0.0);
    private final MediatedEvictionConcurrentHashMap<CacheKey, ConcurrentTreeNode> cache;
    private final ScheduledExecutorService deletionThreadPool;

    @GuardedBy("treeTrashNode")
    private IPageDB.Range<DBKey, ConcurrentTreeNode> trashIterator;

    @SuppressWarnings("unused")
    final Gauge<Integer> treeTrashNodeCount = Metrics.newGauge(SkipListCache.class,
            "treeTrashNodeCount", scope,
            new Gauge<Integer>() {
                @Override
                public Integer value() {
                    return treeTrashNode == null ? -1 : treeTrashNode.getNodeCount();
                }
            });

    @SuppressWarnings("unused")
    final Gauge<Long> treeTrashHitsCount = Metrics.newGauge(SkipListCache.class,
            "treeTrashHitsCount", scope,
            new Gauge<Long>() {
                @Override
                public Long value() {
                    return treeTrashNode == null ? -1 : treeTrashNode.getCounter();
                }
            });

    ConcurrentTree(File root, int numDeletionThreads, int cleanQSize, int maxCacheSize,
                   int maxPageSize, PageFactory factory) throws Exception {
        LessFiles.initDirectory(root);
        this.root = root;
        long start = System.currentTimeMillis();

        // setup metering
        meter = new Meter<>(METERTREE.values());
        for (METERTREE m : METERTREE.values()) {
            meter.addCountMetric(m, m.toString());
        }

        // create meter logging thread
        if (TreeCommonParameters.meterLogging > 0) {
            logger = new MeterFileLogger(this, root, "tree-metrics",
                                         TreeCommonParameters.meterLogging, TreeCommonParameters.meterLogLines);
        } else {
            logger = null;
        }
        source = new PageDB.Builder<>(root, ConcurrentTreeNode.class, maxPageSize, maxCacheSize)
                .pageFactory(factory).build();
        source.setCacheMem(TreeCommonParameters.maxCacheMem);
        source.setPageMem(TreeCommonParameters.maxPageMem);
        source.setMemSampleInterval(TreeCommonParameters.memSample);
        // create cache
        cache = new MediatedEvictionConcurrentHashMap.
                Builder<CacheKey, ConcurrentTreeNode>().
                mediator(new CacheMediator(source)).
                maximumWeightedCapacity(cleanQSize).build();

        // get stored next db id
        idFile = new File(root, "nextID");
        if (idFile.exists() && idFile.isFile() && idFile.length() > 0) {
            nextDBID = new AtomicLong(Long.parseLong(LessBytes.toString(LessFiles.read(idFile))));
        } else {
            nextDBID = new AtomicLong(1);
        }

        // get tree root
        ConcurrentTreeNode dummyRoot = ConcurrentTreeNode.getTreeRoot(this);
        treeRootNode = (ConcurrentTreeNode) dummyRoot.getOrCreateEditableNode("root");
        treeTrashNode = (ConcurrentTreeNode) dummyRoot.getOrCreateEditableNode("trash");
        treeTrashNode.requireNodeDB();
        deletionThreadPool = Executors.newScheduledThreadPool(numDeletionThreads,
                new NamedThreadFactory(scope + "-deletion-", true));

        for (int i = 0; i < numDeletionThreads; i++) {
            deletionThreadPool.scheduleAtFixedRate(
                    new ConcurrentTreeDeletionTask(this, closed::get, LoggerFactory.getLogger(
                            ConcurrentTreeDeletionTask.class.getName() + ".Background")),
                    i, deletionThreadSleepMillis, TimeUnit.MILLISECONDS);
        }

        long openTime = System.currentTimeMillis() - start;
        log.info("dir={} root={} trash={} cache={} nextdb={} openms={}",
                 root, treeRootNode, treeTrashNode, TreeCommonParameters.cleanQMax, nextDBID, openTime);
    }

    public ConcurrentTree(File root) throws Exception {
        this(root, defaultNumDeletionThreads, TreeCommonParameters.cleanQMax,
             TreeCommonParameters.maxCacheSize, TreeCommonParameters.maxPageSize,
             Page.DefaultPageFactory.singleton);
    }

    public void meter(METERTREE meterval) {
        meter.inc(meterval);
    }

    /**
     * This method is only for testing purposes.
     * It has a built in safeguard but nonetheless
     * it should not be invoked for other purposes.
     */
    @VisibleForTesting
    boolean setNextNodeDB(long id) {
        while (true) {
            long current = nextDBID.get();
            if (current > id) {
                return false;
            } else if (nextDBID.compareAndSet(current, id)) {
                return true;
            }
        }
    }

    long getNextNodeDB() {
        long nextValue = nextDBID.incrementAndGet();
        return nextValue;
    }

    private static boolean setLease(final ConcurrentTreeNode node, final boolean lease) {
        return (!lease || node.tryLease());
    }

    public ConcurrentTreeNode getNode(final ConcurrentTreeNode parent, final String child, final boolean lease) {
        long nodedb = parent.nodeDB();
        if (nodedb <= 0) {
            log.trace("[node.get] {} --> {} NOMAP --> null", parent, child);
            return null;
        }
        CacheKey key = new CacheKey(nodedb, child);

        /**
         * (1) First check the cache for the (key, value) pair. If the value
         * is found and the value is successfully leased then return it.
         * (2) Otherwise the value is not found in the cache. Check the backing store
         * for the value. If the value is not found in the backing store then (3) return
         * null. Otherwise (4) if the value is found in the backing store and
         * successfully inserted into the cache and the value is leased then
         * return the value. If all of these steps are unsuccessful then repeat.
         */

        while (true) {
            ConcurrentTreeNode node = cache.get(key);
            if (node != null) {
                if (node.isDeleted()) {
                    cache.remove(key, node);
                } else if (setLease(node, lease)) {
                    reportCacheHit();
                    return node; // (1)
                }
            } else {// (2)
                DBKey dbkey = key.dbkey();
                reportCacheMiss();
                node = source.get(dbkey);

                if (node == null) {
                    meter.inc(METERTREE.SOURCE_MISS);
                    return null; // (3)
                }

                if (node.isDeleted()) {
                    source.remove(dbkey);
                } else {
                    node.initIfDecoded(this, dbkey, key.name);

                    ConcurrentTreeNode prev = cache.putIfAbsent(key, node);
                    if (prev == null) {
                        node.reactivate();
                        if (setLease(node, lease)) {
                            return node; // (4)
                        }
                    }
                }
            }
        }
    }

    public ConcurrentTreeNode getOrCreateNode(final ConcurrentTreeNode parent, final String child,
                                              final DataTreeNodeInitializer creator) {
        parent.requireNodeDB();
        CacheKey key = new CacheKey(parent.nodeDB(), child);
        ConcurrentTreeNode newNode = null;

        while (true) {
            ConcurrentTreeNode node = cache.get(key);
            if (node != null) {
                if (node.isDeleted()) {
                    cache.remove(key, node);
                } else if (setLease(node, true)) {
                    reportCacheHit();
                    return node;
                }
            } else {
                DBKey dbkey = key.dbkey();
                reportCacheMiss();
                node = source.get(dbkey);

                if (node != null) {
                    if (node.isDeleted()) {
                        source.remove(dbkey);
                    } else {
                        node.initIfDecoded(this, dbkey, key.name);
                        ConcurrentTreeNode prev = cache.putIfAbsent(key, node);
                        if (prev == null) {
                            node.reactivate();
                            if (setLease(node, true)) {
                                return node;
                            }
                        }
                    }
                } else { // create a new node
                    if (newNode == null) {
                        newNode = new ConcurrentTreeNode();
                        newNode.init(this, dbkey, key.name);
                        newNode.tryLease();
                        newNode.markChanged();
                        if (creator != null) {
                            creator.onNewNode(newNode);
                        }
                    }
                    node = newNode;
                    if (cache.putIfAbsent(key, node) == null) {
                        /**
                         * We must insert the new node into the external storage
                         * because our iterators traverse this data
                         * structure to search for nodes.
                         */
                        source.put(dbkey, node);
                        parent.updateNodeCount(1);
                        return node;
                    }
                }
            }
        }
    }

    boolean deleteNode(final ConcurrentTreeNode parent, final String child) {
        log.trace("[node.delete] {} --> {}", parent, child);
        long nodedb = parent.nodeDB();
        if (nodedb <= 0) {
            log.debug("parent has no children on delete : {} --> {}", parent, child);
            return false;
        }
        CacheKey key = new CacheKey(nodedb, child);
        // lease node to prevent eviction from cache and thereby disrupting our {@code source.remove()}
        ConcurrentTreeNode node = getNode(parent, child, true);
        if (node != null) {
            // first ensure no one can rehydrate into a different instance
            source.remove(key.dbkey());
            // "markDeleted" causes other threads to remove the node at will, so it is semantically the same
            // as removing it from the cache ourselves. Since this is the last and only instance, we can safely
            // coordinate concurrent deletion attempts with the lease count (-2 is used as a special flag) even
            // though most other code stops bothering with things like "thread safety" around this stage.
            if (node.markDeleted()) {
                // node could have already been dropped from the cache, and then re-created (sharing the same cache
                // key equality). That is a fresh node that needs its own deletion, so only try to remove our instance.
                cache.remove(key, node);
                parent.updateNodeCount(-1);
                if (node.hasNodes() && !node.isAlias()) {
                    markForChildDeletion(node);
                }
                return true;
            }
        }
        return false;
    }

    private void markForChildDeletion(final ConcurrentTreeNode node) {
        /*
         * only put nodes in the trash if they have children because they've
         * otherwise already been purged from backing store by release() in the
         * TreeCache.
         */
        assert node.hasNodes();
        assert !node.isAlias();
        long nodeDB = treeTrashNode.nodeDB();
        int next = treeTrashNode.incrementNodeCount();
        DBKey key = new DBKey(nodeDB, Raw.get(LessBytes.toBytes(next)));
        source.put(key, node);
        log.trace("[trash.mark] {} --> {}", next, treeTrashNode);
    }

    @SuppressWarnings("unchecked") IPageDB.Range<DBKey, ConcurrentTreeNode> fetchNodeRange(long db) {
        return source.range(new DBKey(db), new DBKey(db + 1));
    }

    @SuppressWarnings({"unchecked", "unused"}) private IPageDB.Range<DBKey, ConcurrentTreeNode> fetchNodeRange(long db, String from) {
        return source.range(new DBKey(db, Raw.get(from)), new DBKey(db + 1));
    }

    @SuppressWarnings("unchecked") IPageDB.Range<DBKey, ConcurrentTreeNode> fetchNodeRange(long db, String from, String to) {
        return source.range(new DBKey(db, Raw.get(from)),
                to == null ? new DBKey(db+1, (Raw)null) : new DBKey(db, Raw.get(to)));
    }

    @Override public ConcurrentTreeNode getRootNode() {
        return treeRootNode;
    }

    /**
     * Package-level visibility is for testing purposes only.
     */
    @VisibleForTesting
    void waitOnDeletions() {
        shutdownDeletionThreadPool();
        synchronized (treeTrashNode) {
            if (trashIterator != null) {
                trashIterator.close();
                trashIterator = null;
            }
        }
    }

    /**
     * Package-level visibility is for testing purposes only.
     */
    @VisibleForTesting
    ConcurrentMap<CacheKey, ConcurrentTreeNode> getCache() {
        return cache;
    }

    private void shutdownDeletionThreadPool() {
        if (deletionThreadPool == null)
            return;

        deletionThreadPool.shutdown();

        try {
            if (!deletionThreadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Waiting on outstanding node deletions to complete.");
                deletionThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ignored) {
        }
    }


    /**
     * Delete from the backing storage all nodes that have been moved to be
     * children of the trash node where they are waiting deletion. Also delete
     * all subtrees of these nodes. After deleting each subtree then test
     * the provided {@param terminationCondition}. If it returns true then
     * stop deletion.
     *
     * @param terminationCondition invoked between subtree deletions to
     *                             determine whether to return from method.
     */
    @Override
    public void foregroundNodeDeletion(BooleanSupplier terminationCondition) {
        ConcurrentTreeDeletionTask deletionTask = new ConcurrentTreeDeletionTask(this, terminationCondition, log);
        deletionTask.run();
    }

    @Override
    public void sync() throws IOException {
        log.debug("[sync] start");
        for (ConcurrentTreeNode node : cache.values()) {
            if (!node.isDeleted() && node.isChanged()) {
                source.put(node.dbkey, node);
            }
        }
        log.debug("[sync] end nextdb={}", nextDBID);
        LessFiles.write(idFile, LessBytes.toBytes(nextDBID.toString()), false);
    }

    @Override
    public long getDBCount() {
        return nextDBID.get();
    }

    @Override
    public int getCacheSize() {
        return cache.size();
    }

    @Override
    public double getCacheHitRate() {
        if (logger == null) {
            getIntervalData();
        }
        return cacheHitRate.get();
    }

    /**
     * Close the tree.
     *
     * @param cleanLog if true then wait for the BerkeleyDB clean thread to finish.
     * @param operation optionally test or repair the berkeleyDB.
     */
    @Override
    public void close(boolean cleanLog, CloseOperation operation) throws IOException {
        if (!closed.compareAndSet(false, true)) {
            log.trace("already closed");
            return;
        }
        log.debug("closing {}", this);
        waitOnDeletions();
        if (treeRootNode != null) {
            treeRootNode.markChanged();
            treeRootNode.release();
            if (treeRootNode.getLeaseCount() != 0) {
                throw new IllegalStateException("invalid root state on shutdown : " + treeRootNode);
            }
        }
        if (treeTrashNode != null) {
            treeTrashNode.markChanged();
            treeTrashNode.release();
        }
        sync();
        if (source != null) {
            int status = source.close(cleanLog, operation);
            if (status != 0) {
                throw new RuntimeException("page db close returned a non-zero exit code : " + status);
            }
        }
        if (logger != null) {
            logger.terminate();
        }
    }


    @Override
    public void close() throws IOException {
        close(false, CloseOperation.NONE);
    }

    @Override
    public Map<String, Long> getIntervalData() {
        Map<String, Long> mark = meter.mark();
        Long gets = mark.get(keyCacheGet);
        Long miss = mark.get(keyCacheMiss);
        if (gets == null || miss == null || miss == 0) {
            cacheHitRate.set(0);
        } else {
            cacheHitRate.set(1.0d - ((miss * 1.0d) / ((gets + miss) * 1.0d)));
        }
        return mark;
    }

    private void reportCacheHit() {
        meter.inc(METERTREE.CACHE_HIT);
    }

    private void reportCacheMiss() {
        meter.inc(METERTREE.CACHE_MISS);
    }

    @Override
    public String toString() {
        return "Tree@" + root;
    }

    @Override
    public DataTreeNode getLeasedNode(String name) {
        return getRootNode().getLeasedNode(name);
    }

    @Override
    public DataTreeNode getOrCreateNode(String name, DataTreeNodeInitializer init) {
        return getRootNode().getOrCreateNode(name, init);
    }

    @Override
    public boolean deleteNode(String node) {
        return getRootNode().deleteNode(node);
    }

    @Override
    public ClosableIterator<DataTreeNode> getIterator() {
        ConcurrentTreeNode rootNode = getRootNode();
        if (rootNode != null) {
            return getRootNode().getIterator();
        }
        return null;
    }

    @Override
    public ClosableIterator<DataTreeNode> getIterator(String begin) {
        return getRootNode().getIterator(begin);
    }

    @Override
    public ClosableIterator<DataTreeNode> getIterator(String from, String to) {
        return getRootNode().getIterator(from, to);
    }

    @Override
    public Iterator<DataTreeNode> iterator() {
        return getRootNode().iterator();
    }

    @Override
    public String getName() {
        return getRootNode().getName();
    }

    @Override
    public int getNodeCount() {
        return getRootNode().getNodeCount();
    }

    @Override
    public long getCounter() {
        return getRootNode().getCounter();
    }

    @Override
    public void incrementCounter() {
        getRootNode().incrementCounter();
    }

    @Override
    public long incrementCounter(long val) {
        return getRootNode().incrementCounter(val);
    }

    @Override
    public void writeLock() {
        getRootNode().writeLock();
    }

    @Override
    public void writeUnlock() {
        getRootNode().writeUnlock();
    }

    @Override
    public void setCounter(long val) {
        getRootNode().setCounter(val);
    }

    @Override
    public void updateChildData(DataTreeNodeUpdater state, TreeDataParent path) {
        getRootNode().updateChildData(state, path);
    }

    @Override
    public void updateParentData(DataTreeNodeUpdater state, DataTreeNode child, boolean isnew) {
        getRootNode().updateParentData(state, child, isnew);
    }

    @Override
    public boolean aliasTo(DataTreeNode target) {
        throw new RuntimeException("root node cannot be an alias");
    }

    @Override
    public void release() {
        getRootNode().release();
    }

    @Override
    public DataTreeNodeActor getData(String key) {
        return getRootNode().getData(key);
    }

    @Override
    public Map<String, TreeNodeData> getDataMap() {
        return getRootNode().getDataMap();
    }

    private static String keyName(DBKey dbkey) {
        try {
            return new String(dbkey.key(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            log.warn("Could not decode the following dbkey bytes into a string: " +
                     Arrays.toString(dbkey.key()));
            return null;
        }
    }

    /**
     * Recursively delete all the children of the input node.
     * Use a non-negative value for the counter parameter to
     * tally the nodes that have been deleted. Use a negative
     * value to disable logging of the number of deleted nodes.
     *
     * @param rootNode root of the subtree to delete
     * @param counter if non-negative then tally the nodes that have been deleted
     */
    long deleteSubTree(ConcurrentTreeNode rootNode,
                       long counter,
                       BooleanSupplier terminationCondition,
                       Logger deletionLogger) {
        long nodeDB = rootNode.nodeDB();
        IPageDB.Range<DBKey, ConcurrentTreeNode> range = fetchNodeRange(nodeDB);
        DBKey endRange;
        boolean reschedule;
        try {
            while (range.hasNext() && !terminationCondition.getAsBoolean()) {
                if ((++counter % deletionLogInterval) == 0) {
                    deletionLogger.info("Deleted {} nodes from the tree.", counter);
                }
                Map.Entry<DBKey, ConcurrentTreeNode> entry = range.next();
                ConcurrentTreeNode next = entry.getValue();

                if (next.hasNodes() && !next.isAlias()) {
                    counter = deleteSubTree(next, counter, terminationCondition, deletionLogger);
                }
                String name = entry.getKey().rawKey().toString();
                CacheKey key = new CacheKey(nodeDB, name);
                ConcurrentTreeNode cacheNode = cache.remove(key);
                /* Mark the node as deleted so that it will not be
                 * pushed to disk when removed from the eviction queue.
                 */
                if (cacheNode != null) {
                    cacheNode.markDeleted();
                }
            }
            if (range.hasNext()) {
                endRange = range.next().getKey();
                reschedule = true;
            } else {
                endRange = new DBKey(nodeDB + 1);
                reschedule = false;
            }
        } finally {
            range.close();
        }
        source.remove(new DBKey(nodeDB), endRange);
        if (reschedule) {
            markForChildDeletion(rootNode);
        }
        return counter;
    }

    Map.Entry<DBKey, ConcurrentTreeNode> nextTrashNode() {
        synchronized (treeTrashNode) {
            if (trashIterator == null) {
                return recreateTrashIterator();
            } else if (trashIterator.hasNext()) {
                return trashIterator.next();
            } else {
                return recreateTrashIterator();
            }
        }
    }

    @GuardedBy("treeTrashNode")
    private Map.Entry<DBKey, ConcurrentTreeNode> recreateTrashIterator() {
        if (trashIterator != null) {
            trashIterator.close();
        }
        trashIterator = fetchNodeRange(treeTrashNode.nodeDB());
        if (trashIterator.hasNext()) {
            return trashIterator.next();
        } else {
            trashIterator.close();
            trashIterator = null;
            return null;
        }
    }

    /**
     * For testing purposes only.
     */
    @VisibleForTesting
    ConcurrentTreeNode getTreeTrashNode() {
        return treeTrashNode;
    }

    public void repairIntegrity() {
        PagedKeyValueStore store = source.getEps();
        if (store instanceof SkipListCache) {
            ((SkipListCache) store).testIntegrity(true);
        }
    }

}
