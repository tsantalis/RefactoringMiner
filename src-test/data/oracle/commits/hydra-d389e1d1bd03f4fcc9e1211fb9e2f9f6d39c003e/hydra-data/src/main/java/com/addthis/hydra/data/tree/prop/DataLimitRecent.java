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
package com.addthis.hydra.data.tree.prop;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.core.BundleField;
import com.addthis.bundle.util.ValueUtil;
import com.addthis.bundle.value.ValueFactory;
import com.addthis.bundle.value.ValueObject;
import com.addthis.codec.annotations.FieldConfig;
import com.addthis.codec.codables.Codable;
import com.addthis.hydra.data.tree.DataTreeNode;
import com.addthis.hydra.data.tree.DataTreeNodeUpdater;
import com.addthis.hydra.data.tree.TreeDataParameters;
import com.addthis.hydra.data.tree.TreeNodeData;
import com.addthis.hydra.data.tree.TreeNodeDataDeferredOperation;


public class DataLimitRecent extends TreeNodeData<DataLimitRecent.Config> {

    /**
     * This data attachment <span class="hydra-summary">limits child nodes to recent values</span>.
     *
     * @user-reference
     */
    public static final class Config extends TreeDataParameters<DataLimitRecent> {

        /**
         * If non-zero then remove values that are older than the oldest bundle by this amount.
         * Either this field or {@link #size} must be nonzero.
         */
        @FieldConfig(codable = true)
        private long age;

        /**
         * If non-zero then accept at most N bundles.
         * Either this field or {@link #age} must be nonzero.
         */
        @FieldConfig(codable = true)
        private int size;

        /**
         * Bundle field name from which to draw time values.
         * If null then the system time is used while processing each bundle.
         */
        @FieldConfig(codable = true)
        private String timeKey;

        /**
         * If true then use the time value to sort the observed values.
         * The sorting is inefficient and should be
         * reimplemented. Default is false.
         */
        @FieldConfig(codable = true)
        private boolean sortQueue;

        @Override
        public DataLimitRecent newInstance() {
            DataLimitRecent dc = new DataLimitRecent();
            dc.size = size;
            dc.age = age;
            dc.timeKey = timeKey;
            dc.sortQueue = sortQueue;
            dc.queue = new LinkedList<>();
            return dc;
        }
    }

    /** */
    public static final class KeyTime implements Codable {

        @FieldConfig(codable = true)
        private String key;
        @FieldConfig(codable = true)
        private long time;
    }

    @FieldConfig(codable = true)
    private int size;
    @FieldConfig(codable = true)
    private long age;
    @FieldConfig(codable = true)
    private long deleted;
    @FieldConfig(codable = true)
    private LinkedList<KeyTime> queue;
    @FieldConfig(codable = true)
    private String timeKey;
    @FieldConfig(codable = true)
    private boolean sortQueue;

    private BundleField keyAccess;

    @Override
    public boolean updateChildData(DataTreeNodeUpdater state, DataTreeNode tn, Config conf) {
        return false;
    }

    public static class DataLimitRecentDeferredOperation extends TreeNodeDataDeferredOperation {

        final DataTreeNode parentNode;
        final KeyTime e;

        DataLimitRecentDeferredOperation(DataTreeNode parentNode, KeyTime e) {
            this.parentNode = parentNode;
            this.e = e;
        }

        @Override
        public void run() {
            DataTreeNode check = parentNode.getOrCreateNode(e.key, null);
            if (check != null) {
                // TODO need 'count' field from current PathElement to be
                // strict instead of using 1
                // TODO requires update to TreeNodeUpdater to keep ptr to
                // PathElement being processed

                /**
                 * The following try/finally block locks 'parentNode'
                 * when it should be locking 'check' instead. This is
                 * to maintain the original behavior of the updateParentData()
                 * method before the introduction of DataLimitRecentDeferredOperation.
                 *
                 * Prior to the introduction of deferred operations this
                 * code was executed holding the parentNode lock.
                 * Therefore we will continue to mimic this behavior
                 * until the TreeNode class is properly refactored with
                 * a policy for maintaining consistent state during
                 * concurrent operations.
                 */
                parentNode.writeLock();
                long counterVal;
                try {
                    counterVal = check.incrementCounter(-1);
                } finally {
                    parentNode.writeUnlock();
                }
                if (counterVal <= 0) {
                    if (!parentNode.deleteNode(e.key)) {
                        // TODO booo hiss
                    }
                }
//                  check.release();
            }
        }
    }


    @Override
    public boolean updateParentData(DataTreeNodeUpdater state, DataTreeNode parentNode,
            DataTreeNode childNode,
            List<TreeNodeDataDeferredOperation> deferredOps) {
        try {
            KeyTime e = new KeyTime();
            e.key = childNode.getName();
            if (timeKey != null) {
                Bundle p = state.getBundle();
                if (keyAccess == null) {
                    keyAccess = p.getFormat().getField(timeKey);
                }
                e.time = ValueUtil.asNumber(p.getValue(keyAccess)).asLong().getLong();
            } else {
                e.time = System.currentTimeMillis();
            }
            queue.addFirst(e);
            if ((size > 0 && queue.size() > size) || (age > 0 && e.time - queue.peekLast().time > age)) {
                if (sortQueue) {
                    Collections.sort(queue, new Comparator<KeyTime>() {
                        @Override
                        public int compare(KeyTime o, KeyTime o1) {
                            if (o.time > o1.time) {
                                return -1;
                            } else if (o.time < o1.time) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    });
                }
                e = queue.removeLast();
                deferredOps.add(new DataLimitRecentDeferredOperation(parentNode, e));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public ValueObject getValue(String key) {
        return ValueFactory.create(deleted);
    }
}
