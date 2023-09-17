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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.addthis.basis.util.LessStrings;
import com.addthis.basis.util.Varint;

import com.addthis.bundle.util.AutoField;
import com.addthis.bundle.value.ValueFactory;
import com.addthis.bundle.value.ValueObject;
import com.addthis.codec.annotations.FieldConfig;
import com.addthis.codec.codables.Codable;
import com.addthis.hydra.data.filter.value.ValueFilter;
import com.addthis.hydra.data.tree.DataTreeNode;
import com.addthis.hydra.data.tree.DataTreeNodeUpdater;
import com.addthis.hydra.data.tree.ReadTreeNode;
import com.addthis.hydra.data.tree.TreeDataParameters;
import com.addthis.hydra.data.tree.TreeNodeData;
import com.addthis.hydra.data.util.KeyTopper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

public class DataKeyTop extends TreeNodeData<DataKeyTop.Config> implements Codable {

    /**
     * This data attachment <span class="hydra-summary">keeps a record of the top N values
     * and the number of times they're encountered</span>.
     * <p/>
     * <p>Job Configuration Example:</p>
     * <pre>
     * {const:"shard-counter"}
     * {field:"DATE_YMD", data.top_ips.key-top {size:500, key:"IP"}}
     * </pre>
     *
     * <p><b>Query Path Directives</b>
     *
     * <p>"$" operations support the following commands in the format
     * $+{attachment}={command} :
     * <table>
     * <tr>
     * <td width="20%">size</td>
     * <td>number of entries in the data attachment</td></tr>
     * <tr>
     * <td>g[name]</td>
     * <td>value associated with the key "name"</td>
     * </tr>
     * <tr>
     * <td>k[number]</td>
     * <td>key associated with the i^th element. Counting is assumed to begin at 1</td>
     * </tr>
     * <tr>
     * <td>v[number]</td>
     * <td>value associated with the i^th element. Counting is assumed to begin at 1</td>
     * </tr>
     * <tr>
     * <td>e[number]</td>
     * <td>error associated with the i^th element. Counting is assumed to begin at 1</td>
     * </tr>
     * </table>
     *
     * <p>"%" operations support the following commands in the format /+%{attachment}={command}.
     * Two consecutive equals characters ("==") are necessary to use the "hit", "node",
     * "phit", or "vhit" operations.
     *
     * <table>
     * <tr>
     * <td width="25%">"=hit" or "=node"</td>
     * <td>retrieve the child nodes that are identified by the keys stored in the data attachment</td></tr>
     * <tr>
     * <td>"=vhit"</td>
     * <td>create virtual nodes using the keys stored in the data attachment</td>
     * </tr>
     * <tr>
     * <td>"=phit"</td>
     * <td>retrieve a cloned copy of the child nodes that are identified by the keys stored in the data attachment</td>
     * </tr>
     * <tr>
     * <td>"=guaranteed"</td>
     * <td>if error tracking has been enabled then only return the guaranteed top-k elements</td>
     * </tr>
     * <tr>
     * <td>"name1:name2:name3"</td>
     * <td>create virtual nodes using the keys specified in the command</td>
     * </tr>
     * </table>
     *
     * <p>Using "%" without any arguments creates virtual nodes using the keys
     * stored in the data attachment. If error estimation is enabled then the
     * errors appear in the 'stats' data attachment that is a {@link DataMap}
     * retrieved using the key 'error'.
     *
     * <p>Query Path Examples:</p>
     * <pre>
     *     /shard-counter/+130101$+top_ips=k1,k2,k3,k4,k5
     *     /shard-counter/+130101/+%top_ips==hit
     * </pre>
     *
     * @user-reference
     */
    public static final class Config extends TreeDataParameters<DataKeyTop> {

        /**
         * Bundle field name from which to draw values.
         * This field is required.
         */
        @FieldConfig(codable = true, required = true)
        private AutoField key;

        /**
         * Optionally specify a bundle field name for
         * which to weight the insertions into the
         * data attachment. If weight is specified
         * then the weight field must have a valid
         * integer whenever the key field is non-null
         * or the job will error. Default is null.
         */
        @FieldConfig(codable = true)
        private AutoField weight;

        /**
         * Maximum capacity of the key topper.
         * This field is required.
         */
        @FieldConfig(codable = true, required = true)
        private int size;

        /**
         * Optionally track an error estimate associated with
         * each key/value pair. Default is false.
         */
        @FieldConfig(codable = true)
        private boolean errors;

        /**
         * Optionally split the input with this regular expression
         * before recording the data. Default is null.
         */
        @FieldConfig(codable = true)
        private String splitRegex;

        /**
         * Optionally apply a filter before recording the data.
         * Default is null.
         */
        @FieldConfig(codable = true)
        private ValueFilter filter;

        @Override
        public DataKeyTop newInstance() {
            DataKeyTop dataKeyTop = new DataKeyTop();
            dataKeyTop.size = size;
            dataKeyTop.top = new KeyTopper().init().setLossy(true).enableErrors(errors);
            dataKeyTop.filter = filter;
            return dataKeyTop;
        }
    }

    @FieldConfig(codable = true, required = true)
    private KeyTopper top;
    @FieldConfig(codable = true, required = true)
    private int size;

    private ValueFilter filter;
    private AutoField keyAccess;
    private AutoField weightAccess;

    @Override
    public boolean updateChildData(DataTreeNodeUpdater state, DataTreeNode childNode, DataKeyTop.Config conf) {
        if (keyAccess == null) {
            keyAccess = conf.key;
            weightAccess = conf.weight;
            filter = conf.filter;
        }
        ValueObject val = keyAccess.getValue(state.getBundle());
        if (val != null) {
            if (filter != null) {
                val = filter.filter(val, state.getBundle());
                if (val == null) {
                    return false;
                }
            }
            int weight = 1;
            if (weightAccess != null) {
                weight = weightAccess.getValue(state.getBundle()).asLong().asNative().intValue();
            }
            if (conf.splitRegex != null) {
                String[] split = val.toString().split(conf.splitRegex);
                for (int i = 0; i < split.length; i++) {
                    top.increment(split[i], weight, size);
                }
            } else {
                if (val.getObjectType() == ValueObject.TYPE.ARRAY) {
                    for (ValueObject obj : val.asArray()) {
                        top.increment(obj.toString(), weight, size);
                    }
                } else {
                    top.increment(val.toString(), weight, size);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ValueObject getValue(String key) {
        if (key != null && key.length() > 0) {
            if (key.equals("size")) {
                return ValueFactory.create(top.size());
            }
            try {
                if (key.charAt(0) == 'g') {
                    String topKey = key.substring(1);
                    Long val = top.get(topKey);
                    return ValueFactory.create(val != null ? val : 0);
                }
                if (key.charAt(0) == 'v') {
                    int pos = Integer.parseInt(key.substring(1));
                    return pos <= top.size() ? ValueFactory.create(top.getSortedEntries()[pos - 1].getValue()) : null;
                }
                if (key.charAt(0) == 'e') {
                    if (top.hasErrors()) {
                        int pos = Integer.parseInt(key.substring(1));
                        if (pos <= top.size()) {
                            String element = top.getSortedEntries()[pos - 1].getKey();
                            Long error = top.getError(element);
                            return ValueFactory.create(error);
                        }
                    }
                    return null;
                }
                if (key.charAt(0) == 'k') {
                    key = key.substring(1);
                }
                int pos = Integer.parseInt(key);
                return pos <= top.size() ? ValueFactory.create(top.getSortedEntries()[pos - 1].getKey()) : null;
            } catch (Exception e) {
                return ValueFactory.create(e.toString());
            }
        }
        return ValueFactory.create(top.toString());
    }

    /**
     * return types of synthetic nodes returned
     */
    @Override public List<String> getNodeTypes() {
        return Arrays.asList(new String[]{"#"});
    }

    @Override
    public List<DataTreeNode> getNodes(DataTreeNode parent, String key) {
        if (key != null && key.startsWith("=")) {
            key = key.substring(1);
            if (key.equals("hit") || key.equals("node")) {
                Entry<String, Long>[] list = top.getSortedEntries();
                ArrayList<DataTreeNode> ret = new ArrayList<>(list.length);
                for (Entry<String, Long> e : list) {
                    DataTreeNode node = parent.getNode(e.getKey());
                    if (node != null) {
                        ret.add(node);
                    }
                }
                return ret;
            } else if (key.equals("vhit")) {
                Entry<String, Long>[] list = top.getSortedEntries();
                ArrayList<DataTreeNode> ret = new ArrayList<>(list.length);
                for (Entry<String, Long> e : list) {
                    ret.add(new VirtualTreeNode(e.getKey(), e.getValue()));
                }
                return ret;
            } else if (key.equals("phit")) {
                Entry<String, Long>[] list = top.getSortedEntries();
                ArrayList<DataTreeNode> ret = new ArrayList<>(list.length);
                for (Entry<String, Long> e : list) {
                    DataTreeNode node = parent.getNode(e.getKey());
                    if (node != null) {
                        node = ((ReadTreeNode) node).getCloneWithCount(e.getValue());
                        ret.add(node);
                    }
                }
                return ret;
            } else if (key.equals("guaranteed")) {
                return generateVirtualNodes(true);
            }
        } else if (key != null) {
            String[] keys = LessStrings.splitArray(key, ":");
            ArrayList<DataTreeNode> list = new ArrayList<>(keys.length);
            for (String k : keys) {
                Long v = top.get(k);
                if (v != null) {
                    list.add(new VirtualTreeNode(k, v));
                }
            }
            return list.size() > 0 ? list : null;
        }
        return generateVirtualNodes(false);
    }

    /**
     * Generate a list of virtual nodes and include error estimates
     * if they are available.
     *
     * @param onlyGuaranteed if true then only return guaranteed top-K
     *                       (requires error estimates)
     * @return list of virtual nodes
     */
    private List<DataTreeNode> generateVirtualNodes(boolean onlyGuaranteed) {
        ArrayList<DataTreeNode> list = new ArrayList<>(top.size());
        Entry<String, Long>[] entries = top.getSortedEntries();
        for (int i = 0; i < entries.length; i++) {
            Entry<String,Long> entry = entries[i];
            String name = entry.getKey();
            Long value = entry.getValue();
            VirtualTreeNode node = new VirtualTreeNode(name, value);
            if (top.hasErrors()) {
                Long error = top.getError(name);
                if (onlyGuaranteed && ((i + 1) < entries.length) &&
                    ((value - error) < entries[i + 1].getValue())) {
                    break;
                }
                Map<String, TreeNodeData> data = node.createMap();
                DataMap dataMap = new DataMap(1);
                dataMap.put("error", ValueFactory.create(error));
                data.put("stats", dataMap);
            }
            list.add(node);
        }
        return list;
    }

    @Override
    public byte[] bytesEncode(long version) {
        byte[] bytes = null;
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            byte[] topBytes = top.bytesEncode(version);
            Varint.writeUnsignedVarInt(topBytes.length, buf);
            buf.writeBytes(topBytes);
            Varint.writeUnsignedVarInt(size, buf);
            bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
        } finally {
            buf.release();
        }
        return bytes;
    }

    @Override
    public void bytesDecode(byte[] b, long version) {
        top = new KeyTopper();
        ByteBuf buf = Unpooled.wrappedBuffer(b);
        try {
            int topBytesLength = Varint.readUnsignedVarInt(buf);
            if (topBytesLength > 0) {
                byte[] topBytes = new byte[topBytesLength];
                buf.readBytes(topBytes);
                top.bytesDecode(topBytes, version);
            } else {
                top.init().setLossy(true);
            }
            size = Varint.readUnsignedVarInt(buf);
        } finally {
            buf.release();
        }
    }
}
