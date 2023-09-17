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

import java.util.Iterator;
import java.util.Map;

import com.addthis.basis.util.ClosableIterator;

import com.addthis.hydra.data.tree.DataTreeNode;
import com.addthis.hydra.data.tree.ReadTree;
import com.addthis.hydra.data.tree.ReadTreeNode;
import com.addthis.hydra.data.tree.TreeNodeData;
import com.addthis.hydra.store.db.CloseOperation;

public class ConcurrentTreeConverter {

    public static void main(String[] args) throws Exception {
        int maxNodes = (args.length >= 3) ? Integer.parseInt(args[2]) : -1;
        new ConcurrentTreeConverter(new File(args[0]), new File(args[1]), maxNodes);
    }

    public ConcurrentTreeConverter(File readRoot, File writeRoot, int maxNodes) throws Exception {
        long mark = System.currentTimeMillis();
        ReadTree readTree = new ReadTree(readRoot);
        ConcurrentTree writeTree = new ConcurrentTree(writeRoot);
        long openTime = System.currentTimeMillis() - mark;
        long testTime = 0;
        try {
            mark += openTime;
            ReadTreeNode readNode = readTree.getRootNode();
            ConcurrentTreeNode writeNode = writeTree.getRootNode();
            explore(writeTree, writeNode, readNode, 0, maxNodes);
            testTime = System.currentTimeMillis() - mark;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            readTree.close();
            writeTree.close(true, CloseOperation.NONE);
        }
        System.out.println("nodeCount=" + nodeCount + " hasNodes=" + hasNodes + " maxLeafs=" +
                           maxLeafs + " maxDepth=" + maxDepth + " openTime=" + openTime + " testTime=" + testTime);
    }

    private int nodeCount;
    private int maxLeafs;
    private int hasNodes;
    private int maxDepth;

    private void generateNode(ConcurrentTree writeTree,
            ConcurrentTreeNode writeParent,
            ReadTreeNode readChild) {
        ConcurrentTreeNode writeNode = writeTree.getOrCreateNode(writeParent, readChild.getName(), null, null);
        writeNode.writeLock();
        if (readChild.hasNodes()) {
            writeNode.requireNodeDB();
        }
        writeNode.setCounter(readChild.getCounter());
        Map<String, TreeNodeData> readMap = readChild.getDataMap();
        if (readMap != null) {
            Map<String, TreeNodeData> writeMap = writeNode.createMap(readMap.size());
            for (Map.Entry<String, TreeNodeData> entry : readMap.entrySet()) {
                writeMap.put(entry.getKey(), entry.getValue());
            }
        }
        writeNode.markChanged();
        writeNode.writeUnlock();
        writeNode.release();
    }

    private void explore(ConcurrentTree writeTree,
            ConcurrentTreeNode writeNode,
            ReadTreeNode readNode, int depth,
            int maxNodes) throws Exception {
        int count = 0;
        if (maxNodes > 0 && nodeCount >= maxNodes) {
            return;
        }
        Iterator<DataTreeNode> iter = readNode.iterator();
        if (iter != null) {
            if (iter.hasNext()) {
                hasNodes++;
            }
            try {
                while (iter.hasNext()) {
                    count++;
                    ReadTreeNode readChild = (ReadTreeNode) iter.next();
                    generateNode(writeTree, writeNode, readChild);
                }
            } finally {
                if (iter instanceof ClosableIterator) {
                    ((ClosableIterator<DataTreeNode>) iter).close();
                }
            }
            iter = readNode.iterator();
            try {
                while (iter.hasNext()) {
                    ReadTreeNode readChild = (ReadTreeNode) iter.next();
                    ConcurrentTreeNode writeChild = writeTree.getNode(writeNode, readChild.getName(), false);
                    explore(writeTree, writeChild, readChild, depth + 1, maxNodes);
                }
            } finally {
                if (iter instanceof ClosableIterator) {
                    ((ClosableIterator<DataTreeNode>) iter).close();
                }
            }
        }
        maxLeafs = Math.max(count, maxLeafs);
        maxDepth = Math.max(depth, maxDepth);
        nodeCount++;
        if (nodeCount % 10000000 == 0) {
            System.out.println(".. nodeCount=" + nodeCount + " hasNodes=" + hasNodes + " maxLeafs=" + maxLeafs + " maxDepth=" + maxDepth);
        }
    }
}
