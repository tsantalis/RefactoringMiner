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

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;

import com.addthis.basis.test.SlowTest;
import com.addthis.basis.util.ClosableIterator;
import com.addthis.basis.util.LessFiles;

import com.addthis.hydra.data.tree.DataTreeNode;
import com.addthis.hydra.store.db.CloseOperation;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestConcurrentTree {

    private static final Logger log = LoggerFactory.getLogger(TestConcurrentTree.class);

    private static final CloseOperation close = CloseOperation.TEST;

    static final int fastNumElements = 10000;
    static final int fastNumThreads = 8;

    static final int slowNumElements = 100000;
    static final int slowNumThreads = 8;

    private File makeTemporaryDirectory() throws IOException {
        final File temp;

        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return temp;
    }

    static final class InsertionThread extends Thread {

        final CyclicBarrier barrier;
        final List<Integer> values;
        final List<Integer> threadId;
        final int myId;
        final ConcurrentTree cache;
        final ConcurrentTreeNode parent;
        int counter;

        public InsertionThread(CyclicBarrier barrier, List<Integer> values,
                List<Integer> threadId, int id, ConcurrentTree cache,
                ConcurrentTreeNode parent) {
            super("InsertionThread" + id);
            this.barrier = barrier;
            this.values = values;
            this.threadId = threadId;
            this.myId = id;
            this.cache = cache;
            this.counter = 0;
            this.parent = parent;
        }

        @Override
        public void run() {
            try {
                barrier.await();
                int len = threadId.size();
                for (int i = 0; i < len; i++) {
                    if (threadId.get(i) == myId) {
                        Integer val = values.get(i);
                        ConcurrentTreeNode node = cache.getOrCreateNode(parent,
                                Integer.toString(val), null);
                        node.release();
                        counter++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }

    }

    static final class DeletionThread extends Thread {

        final CyclicBarrier barrier;
        final List<Integer> values;
        final List<Integer> threadId;
        final int myId;
        final ConcurrentTree cache;
        final ConcurrentTreeNode parent;
        int counter;

        public DeletionThread(CyclicBarrier barrier, List<Integer> values,
                List<Integer> threadId, int id, ConcurrentTree cache,
                ConcurrentTreeNode parent) {
            super("DeletionThread" + id);
            this.barrier = barrier;
            this.values = values;
            this.threadId = threadId;
            this.myId = id;
            this.cache = cache;
            this.counter = 0;
            this.parent = parent;
        }

        @Override
        public void run() {
            try {
                barrier.await();
                int len = threadId.size();
                for (int i = 0; i < len; i++) {
                    if (threadId.get(i) == myId) {
                        Integer val = values.get(i);
                        cache.deleteNode(parent, Integer.toString(val));
                        counter++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
    }

    @Test
    @Category(SlowTest.class)
    public void getOrCreateOneThreadIterations() throws Exception {
        for(int i = 0; i < 100; i++) {
            getOrCreateOneThread();
        }
    }

    @Test
    public void getOrCreateOneThread() throws Exception {
        log.info("getOrCreateOneThread");
        File dir = makeTemporaryDirectory();
        try {
            ConcurrentTree tree = new Builder(dir).build();
            ConcurrentTreeNode root = tree.getRootNode();
            for (int i = 0; i < 1000; i++) {
                ConcurrentTreeNode node = tree.getOrCreateNode(root, Integer.toString(i), null);
                assertNotNull(node);
                assertEquals(Integer.toString(i), node.getName());
                node.release();
            }
            for (int i = 0; i < 1000; i++) {
                ConcurrentTreeNode node = tree.getNode(root, Integer.toString(i), true);
                assertNotNull(node);
                assertEquals(1, node.getLeaseCount());
                assertEquals(Integer.toString(i), node.getName());
                node.release();
            }
            tree.close(false, close);
        } finally {
            if (dir != null) {
                LessFiles.deleteDir(dir);
            }
        }
    }

    @Test
    public void recursiveDeleteOneThread() throws Exception {
        log.info("recursiveDeleteOneThread");
        File dir = makeTemporaryDirectory();
        try {
            ConcurrentTree tree = new Builder(dir).build();
            ConcurrentTreeNode root = tree.getRootNode();
            ConcurrentTreeNode parent = tree.getOrCreateNode(root, "hello", null);
            for (int i = 0; i < (TreeCommonParameters.cleanQMax << 1); i++) {
                ConcurrentTreeNode child = tree.getOrCreateNode(parent, Integer.toString(i), null);
                assertNotNull(child);
                assertEquals(Integer.toString(i), child.getName());
                parent.release();
                parent = child;
            }
            parent.release();

            assertEquals(1, root.getNodeCount());
            assertEquals(TreeCommonParameters.cleanQMax, tree.getCache().size());

            tree.deleteNode(root, "hello");
            tree = waitForDeletion(tree, dir);
            assertEquals(2, tree.getCache().size());
            assertEquals(0, root.getNodeCount());
            assertTrue(tree.getTreeTrashNode().getCounter() >= 1);
            assertEquals(tree.getTreeTrashNode().getCounter(), tree.getTreeTrashNode().getNodeCount());
            tree.close(false, close);
        } finally {
            if (dir != null) {
                LessFiles.deleteDir(dir);
            }
        }
    }

    @Test
    public void recursiveDeleteMultiThreads() throws Exception {
        log.info("recursiveDeleteMultiThreads");
        File dir = makeTemporaryDirectory();
        try {
            ConcurrentTree tree = new Builder(dir).
                    numDeletionThreads(8).build();
            ConcurrentTreeNode root = tree.getRootNode();
            for (int i = 0; i < fastNumThreads; i++) {
                ConcurrentTreeNode parent = tree.getOrCreateNode(root, Integer.toString(i), null);
                for (int j = 0; j < TreeCommonParameters.cleanQMax; j++) {
                    ConcurrentTreeNode child = tree.getOrCreateNode(parent, Integer.toString(j), null);
                    assertNotNull(child);
                    assertEquals(Integer.toString(j), child.getName());
                    parent.release();
                    parent = child;
                }
                parent.release();
            }

            assertEquals(TreeCommonParameters.cleanQMax, tree.getCache().size());

            assertEquals(fastNumThreads, root.getNodeCount());

            for (int i = 0; i < fastNumThreads; i++) {
                tree.deleteNode(root, Integer.toString(i));
            }

            tree = waitForDeletion(tree, dir);
            assertEquals(2, tree.getCache().size());
            assertEquals(0, root.getNodeCount());

            tree.close(false, close);
        } finally {
            if (dir != null) {
                LessFiles.deleteDir(dir);
            }
        }
    }

    @Test
    public void getOrCreateFast() throws Exception {
        getOrCreateMultiThread(fastNumElements, fastNumThreads);
    }

    @Test
    @Category(SlowTest.class)
    public void getOrCreateSlow() throws Exception {
        getOrCreateMultiThread(slowNumElements, slowNumThreads);
    }

    private void getOrCreateMultiThread(int numElements, int numThreads) throws Exception {
        log.info("getOrCreateMultiThread");
        File dir = makeTemporaryDirectory();
        try {
            ArrayList<Integer> values = new ArrayList<>(numElements);
            final CyclicBarrier barrier = new CyclicBarrier(numThreads);
            ArrayList<Integer> threadId = new ArrayList<>(numElements);
            InsertionThread[] threads = new InsertionThread[numThreads];
            ConcurrentTree tree = new Builder(dir).build();
            ConcurrentTreeNode root = tree.getRootNode();

            for (int i = 0; i < numElements; i++) {
                values.add(i);
                threadId.add(i % numThreads);
            }

            for (int i = 0; i < numThreads; i++) {
                threads[i] = new InsertionThread(barrier, values, threadId, i, tree, root);
            }

            Collections.shuffle(values);

            for (int i = 0; i < numThreads; i++) {
                threads[i].start();
            }

            for (int i = 0; i < numThreads; i++) {
                threads[i].join();
            }

            for (int i = 0; i < numThreads; i++) {
                assertEquals(numElements / numThreads, threads[i].counter);
            }

            for (int i = 0; i < numElements; i++) {
                ConcurrentTreeNode node = tree.getNode(root, Integer.toString(i), true);
                assertNotNull(node);
                assertEquals(1, node.getLeaseCount());
                assertEquals(Integer.toString(i), node.getName());
                node.release();
            }
            tree.close(false, close);
        } finally {
            if (dir != null) {
                LessFiles.deleteDir(dir);
            }
        }
    }

    @Test
    public void deleteOneThreadForeground() throws Exception {
        log.info("deleteOneThreadForeground");
        deleteOneThread(0);
    }

    @Test
    public void deleteOneThreadBackground() throws Exception {
        log.info("deleteOneThreadBackground");
        deleteOneThread(fastNumThreads);
    }

    private void deleteOneThread(int numDeletionThreads) throws Exception {
        File dir = makeTemporaryDirectory();
        try {
            ConcurrentTree tree = new Builder(dir)
                    .numDeletionThreads(numDeletionThreads).build();
            ConcurrentTreeNode root = tree.getRootNode();
            for (int i = 0; i < 1000; i++) {
                ConcurrentTreeNode node = tree.getOrCreateNode(root, Integer.toString(i), null);
                assertNotNull(node);
                assertEquals(1, node.getLeaseCount());
                assertEquals(Integer.toString(i), node.getName());
                ConcurrentTreeNode child = tree.getOrCreateNode(node, Integer.toString(i), null);
                child.release();
                node.release();
            }
            for (int i = 0; i < 1000; i++) {
                tree.deleteNode(root, Integer.toString(i));
            }
            for (int i = 0; i < 1000; i++) {
                ConcurrentTreeNode node = tree.getNode(root, Integer.toString(i), false);
                assertNull(node);
            }
            tree = waitForDeletion(tree, dir);
            assertTrue(tree.getTreeTrashNode().getCounter() >= 1000);
            assertEquals(tree.getTreeTrashNode().getCounter(), tree.getTreeTrashNode().getNodeCount());
            tree.close(false, close);
        } finally {
            if (dir != null) {
                LessFiles.deleteDir(dir);
            }
        }
    }

    /**
     * Test the foreground deletion by closing the existing tree, reopening
     * the tree with 0 deletion threads, and the performing deletion
     * in the foreground.
     *
     * @param tree input tree
     * @param dir  directory containing database of the input tree
     * @return reopened copy of the input tree
     * @throws Exception
     */
    private ConcurrentTree waitForDeletion(ConcurrentTree tree, File dir) throws Exception {
        tree.close();
        tree = new Builder(dir).numDeletionThreads(0).build();
        tree.foregroundNodeDeletion(() -> false);
        return tree;
    }

    @Test
    public void deleteFast() throws Exception {
        deleteMultiThread(fastNumElements, fastNumThreads, fastNumThreads);
        deleteMultiThread(fastNumElements, fastNumThreads, 0);
    }

    @Test
    @Category(SlowTest.class)
    public void deleteSlow1() throws Exception {
        deleteMultiThread(slowNumElements, slowNumThreads, slowNumThreads);
        deleteMultiThread(slowNumElements, slowNumThreads, 0);
    }

    @Test
    @Category(SlowTest.class)
    public void deleteSlow2() throws Exception {
        for(int i = 0 ; i < 100; i++) {
            deleteMultiThread(fastNumElements, fastNumThreads, fastNumThreads);
            deleteMultiThread(fastNumElements, fastNumThreads, 0);
        }
    }

    @Test
    @Category(SlowTest.class)
    public void iterateAndDeleteSlow() throws Exception {
        for(int i = 0; i < 100; i++) {
            iterateAndDelete(fastNumThreads, 1000);
        }
    }

    @Test
    public void iterateAndDeleteFast() throws Exception {
        iterateAndDelete(fastNumThreads, 1000);
    }

    private void iterateAndDelete(int numThreads, int numElements) throws Exception {
        log.info("iterateAndDelete {} {}", numThreads, numElements);
        File dir = makeTemporaryDirectory();
        try {
            ConcurrentTree tree = new Builder(dir).numDeletionThreads(numThreads).
                    maxPageSize(5).maxCacheSize(500).build();
            ConcurrentTreeNode root = tree.getRootNode();

            for (int i = 0; i < numElements; i++) {
                ConcurrentTreeNode node = tree.getOrCreateNode(root, Integer.toString(i), null);
                assertNotNull(node);
                assertEquals(Integer.toString(i), node.getName());
                ConcurrentTreeNode child = tree.getOrCreateNode(node, Integer.toString(i), null);
                child.release();
                node.release();
            }

            Random rng = new Random();

            ClosableIterator<DataTreeNode> iterator = tree.getIterator();
            try {
                int counter = 0;
                while(iterator.hasNext()) {
                    DataTreeNode node = iterator.next();
                    if (rng.nextFloat() < 0.8) {
                        String name = node.getName();
                        tree.deleteNode(root, name);
                        counter++;
                    }
                }
                log.info("Deleted " + (((float) counter) / numElements * 100.0) + " % of nodes");
            } finally {
                iterator.close();
            }

            tree.close(false, close);

        } finally {
            if (dir != null) {
                LessFiles.deleteDir(dir);
            }
        }

    }

    private void deleteMultiThread(int numElements, int numThreads, int numDeletionThreads) throws Exception {
        log.info("deleteMultiThread {} {} {}", numElements, numThreads, numDeletionThreads);
        File dir = makeTemporaryDirectory();
        try {
            ArrayList<Integer> values = new ArrayList<>(numElements);
            final CyclicBarrier barrier = new CyclicBarrier(numThreads);
            ArrayList<Integer> threadId = new ArrayList<>(numElements);
            DeletionThread[] threads = new DeletionThread[numThreads];
            ConcurrentTree tree = new Builder(dir)
                    .numDeletionThreads(numDeletionThreads).build();
            ConcurrentTreeNode root = tree.getRootNode();

            for (int i = 0; i < numElements; i++) {
                values.add(i);
                threadId.add(i % numThreads);
                ConcurrentTreeNode node = tree.getOrCreateNode(root, Integer.toString(i), null);
                assertNotNull(node);
                assertEquals(Integer.toString(i), node.getName());
                ConcurrentTreeNode child = tree.getOrCreateNode(node, Integer.toString(i), null);
                child.release();
                node.release();
            }

            for (int i = 0; i < numThreads; i++) {
                threads[i] = new DeletionThread(barrier, values, threadId, i, tree, root);
            }

            Collections.shuffle(values);

            for (int i = 0; i < numThreads; i++) {
                threads[i].start();
            }

            for (int i = 0; i < numThreads; i++) {
                threads[i].join();
            }

            for (int i = 0; i < numThreads; i++) {
                assertEquals(numElements / numThreads, threads[i].counter);
            }

            for (int i = 0; i < numElements; i++) {
                ConcurrentTreeNode node = tree.getNode(root, Integer.toString(i), true);
                assertNull(node);
            }
            tree = waitForDeletion(tree, dir);
            assertTrue(tree.getTreeTrashNode().getCounter() >= numElements);
            assertEquals(tree.getTreeTrashNode().getCounter(), tree.getTreeTrashNode().getNodeCount());
            tree.close(false, close);
        } finally {
            if (dir != null) {
                LessFiles.deleteDir(dir);
            }
        }
    }

    @Test
    public void maximumNodeIdentifier() throws Exception {
        File dir = makeTemporaryDirectory();
        try {
            ConcurrentTree tree = new Builder(dir).build();
            ConcurrentTreeNode root = tree.getRootNode();
            for (int i = 0; i < 1000; i++) {
                ConcurrentTreeNode node = tree.getOrCreateNode(root, Integer.toString(i), null);
                assertNotNull(node);
                assertEquals(1, node.getLeaseCount());
                assertEquals(Integer.toString(i), node.getName());
                ConcurrentTreeNode child = tree.getOrCreateNode(node, Integer.toString(i), null);
                child.release();
                node.release();
            }
            assertTrue(tree.setNextNodeDB(Integer.MAX_VALUE));
            for (int i = 1000; i < 2000; i++) {
                ConcurrentTreeNode node = tree.getOrCreateNode(root, Integer.toString(i), null);
                assertNotNull(node);
                assertEquals(1, node.getLeaseCount());
                assertEquals(Integer.toString(i), node.getName());
                ConcurrentTreeNode child = tree.getOrCreateNode(node, Integer.toString(i), null);
                child.release();
                node.release();
            }
            tree.close(false, close);
        } finally {
            if (dir != null) {
                LessFiles.deleteDir(dir);
            }
        }
    }

}
