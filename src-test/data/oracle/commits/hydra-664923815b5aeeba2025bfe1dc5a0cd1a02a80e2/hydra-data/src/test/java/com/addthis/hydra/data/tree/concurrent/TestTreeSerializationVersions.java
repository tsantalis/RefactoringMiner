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

import java.util.Map;

import com.addthis.basis.util.LessFiles;

import com.addthis.hydra.data.tree.TreeNodeData;
import com.addthis.hydra.data.tree.prop.DataTime;
import com.addthis.hydra.store.db.CloseOperation;
import com.addthis.hydra.store.skiplist.LegacyPage;
import com.addthis.hydra.store.skiplist.Page;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestTreeSerializationVersions {

    private static final CloseOperation close = CloseOperation.TEST;

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

    @Test
    public void legacyToSparseUpgradePath() throws Exception {
        File dir = makeTemporaryDirectory();
        try {
            int count = 1000;
            ConcurrentTree tree = new Builder(dir).
                    pageFactory(LegacyPage.LegacyPageFactory.singleton).build();
            ConcurrentTreeNode root = tree.getRootNode();
            /**
             * write count nodes that have a time data attachment. Use the legacy page encoding.
             */
            for (int i = 0; i < count; i++) {
                ConcurrentTreeNode node = tree.getOrCreateNode(root, Integer.toString(i), null, null);
                assertNotNull(node);
                assertEquals(Integer.toString(i), node.getName());
                DataTime attachment = new DataTime();
                attachment.setFirst(i);
                attachment.setLast(i + count);
                node.createMap(1).put("time", attachment);
                node.markChanged();
                node.release();
            }
            tree.close(false, close);
            tree = new Builder(dir).
                    pageFactory(Page.DefaultPageFactory.singleton).build();
            /**
             * Sanity check. Read the count notes and look for the data attachment.
             */
            for (int i = 0; i < count; i++) {
                ConcurrentTreeNode node = tree.getNode(root, Integer.toString(i), true);
                assertNotNull(node);
                assertEquals(1, node.getLeaseCount());
                assertEquals(Integer.toString(i), node.getName());
                Map<String, TreeNodeData> attachments = node.getDataMap();
                assertNotNull(attachments);
                assertTrue(attachments.containsKey("time"));
                DataTime attachment = (DataTime) attachments.get("time");
                assertEquals(i, attachment.first());
                assertEquals(i + count, attachment.last());
                node.release();
            }
            tree.close(false, close);
            tree = new Builder(dir).
                    pageFactory(Page.DefaultPageFactory.singleton).build();
            /**
             * Only on even nodes update the data attachment.
             * Use the new page encoding.
             */
            for (int i = 0; i < count; i += 2) {
                ConcurrentTreeNode node = tree.getNode(root, Integer.toString(i), true);
                assertNotNull("i = " + i, node);
                assertEquals(Integer.toString(i), node.getName());
                node.setCounter(1);
                DataTime attachment = new DataTime();
                attachment.setFirst(2 * i);
                attachment.setLast(2 * i + count);
                node.createMap(1).put("time", attachment);
                node.markChanged();
                node.release();
            }
            tree.close(false, close);
            tree = new Builder(dir).
                    pageFactory(Page.DefaultPageFactory.singleton).build();
            /**
             * Read all the nodes and verify that the data attachments are correct.
             */
            for (int i = 0; i < count; i++) {
                ConcurrentTreeNode node = tree.getNode(root, Integer.toString(i), true);
                assertNotNull(node);
                assertEquals(1, node.getLeaseCount());
                assertEquals(Integer.toString(i), node.getName());
                Map<String, TreeNodeData> attachments = node.getDataMap();
                assertNotNull(attachments);
                assertTrue(attachments.containsKey("time"));
                DataTime attachment = (DataTime) attachments.get("time");
                if (i % 2 == 0) {
                    assertEquals(2 * i, attachment.first());
                    assertEquals(2 * i + count, attachment.last());
                } else {
                    assertEquals(i, attachment.first());
                    assertEquals(i + count, attachment.last());
                }
                node.release();
            }
        } finally {
            if (dir != null) {
                LessFiles.deleteDir(dir);
            }
        }
    }


}
