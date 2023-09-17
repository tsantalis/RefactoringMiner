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

import java.util.ArrayList;
import java.util.List;

import com.addthis.bundle.core.BundleField;
import com.addthis.bundle.util.ValueUtil;
import com.addthis.bundle.value.ValueFactory;
import com.addthis.bundle.value.ValueMap;
import com.addthis.bundle.value.ValueMapEntry;
import com.addthis.bundle.value.ValueObject;
import com.addthis.bundle.value.ValueString;
import com.addthis.codec.annotations.FieldConfig;
import com.addthis.hydra.data.filter.value.ValueFilter;
import com.addthis.hydra.data.tree.DataTreeNode;

/**
 * This {@link PathElement PathElement} <span class="hydra-summary">creates a single node with a specified value</span>.
 * <p/>
 * <p>Compare this path element to the "{@link PathKeyValue value}" path element. "value" creates one
 * or more nodes with a specified key.</p>
 * <p/>
 * <p>Example:</p>
 * <pre>paths : {
 *   "ROOT" : [
 *     {type:"const", value:"date"},
 *     {type:"value", key:"DATE_YMD"},
 *     {type:"value", key:"DATE_HH"},
 *   ],
 * },</pre>
 *
 * @user-reference
 */
public class PathValue extends PathElement {

    public PathValue() {
    }

    public PathValue(String value) {
        this.value = value;
    }

    public PathValue(String value, boolean count) {
        this.value = value;
        this.count = count;
    }

    /**
     * Value to be stored in the constructed node.
     */
    @FieldConfig(codable = true)
    protected String value;

    @FieldConfig(codable = true)
    protected String set;

    @FieldConfig(codable = true)
    protected ValueFilter vfilter;

    @FieldConfig(codable = true)
    protected boolean sync;

    @FieldConfig(codable = true)
    protected boolean create = true;

    @FieldConfig(codable = true)
    protected boolean once;

    @FieldConfig(codable = true)
    protected String mapTo;

    /** Deletes a node before attempting to (re)create or update it. Pure deletion requires {@code create: false}. */
    @FieldConfig protected boolean delete;

    @FieldConfig(codable = true)
    protected boolean push;

    @FieldConfig(codable = true)
    protected PathElement each;

    /**
     * If positive then limit the number of nodes that can be created.
     * Multiple threads may be writing to the tree concurrently.
     * Therefore this limit is a best-effort limit it is not a strict
     * guarantee. Default is zero.
     **/
    @FieldConfig(codable = true)
    protected int maxNodes = 0;

    private ValueString valueString;
    private BundleField setField;
    private BundleField mapField;

    public final ValueObject value() {
        if (valueString == null) {
            valueString = ValueFactory.create(value);
        }
        return valueString;
    }

    /**
     * override in subclasses
     */
    public ValueObject getPathValue(final TreeMapState state) {
        return value();
    }

    public final ValueObject getFilteredValue(final TreeMapState state) {
        ValueObject value = getPathValue(state);
        if (vfilter != null) {
            value = vfilter.filter(value, state.getBundle());
        }
        return value;
    }

    @Override
    public void resolve(final TreeMapper mapper) {
        super.resolve(mapper);
        if (set != null) {
            setField = mapper.bindField(set);
        }
        if (mapTo != null) {
            mapField = mapper.bindField(mapTo);
        }
        if (each != null) {
            each.resolve(mapper);
        }
    }

    @Override
    public String toString() {
        return "PathValue[" + value + "]";
    }

    /**
     * prevent subclasses from overriding as this is not used from here on
     */
    @Override
    public final List<DataTreeNode> getNextNodeList(final TreeMapState state) {
        ValueObject value = getFilteredValue(state);
        if (setField != null) {
            state.getBundle().setValue(setField, value);
        }
        if (ValueUtil.isEmpty(value)) {
            return op ? TreeMapState.empty() : null;
        }
        if (op) {
            return TreeMapState.empty();
        }
        List<DataTreeNode> list;
        if (sync) {
            synchronized (this) {
                list = processNodeUpdates(state, value);
            }
        } else {
            list = processNodeUpdates(state, value);
        }
        if (term) {
            if (list != null) {
                list.forEach(DataTreeNode::release);
            }
            return null;
        } else {
            return list;
        }
    }

    /**
     * Either get an existing node or optionally create a new node
     * if one does not exist. The {@link #create} field determines
     * whether or not to create a new node. If {@link #maxNodes}
     * is a positive integer then test the current node count
     * to determine whether to create a new node.
     *
     * @param state   current state
     * @param name    name of target node
     * @return existing node or newly created node
     */
    public DataTreeNode getOrCreateNode(TreeMapState state, String name) {
        if (create && ((maxNodes == 0) || (state.getNodeCount() < maxNodes))) {
            return state.getOrCreateNode(name, state);
        } else {
            return state.getLeasedNode(name);
        }
    }

    /**
     * override this in subclasses. the rules for this path element are to be
     * applied to the child (next node) of the parent (current node).
     */
    public List<DataTreeNode> processNodeUpdates(TreeMapState state, ValueObject name) {
        List<DataTreeNode> list = new ArrayList<>(1);
        int pushed = 0;
        if (name.getObjectType() == ValueObject.TYPE.ARRAY) {
            for (ValueObject o : name.asArray()) {
                if (o != null) {
                    pushed += processNodeByValue(list, state, o);
                }
            }
        } else if (name.getObjectType() == ValueObject.TYPE.MAP) {
            ValueMap nameAsMap = name.asMap();
            for (ValueMapEntry e : nameAsMap) {
                String key = e.getKey();
                if (mapTo != null) {
                    state.getBundle().setValue(mapField, e.getValue());
                    pushed += processNodeByValue(list, state, ValueFactory.create(key));
                } else {
                    PathValue mapValue = new PathValue(key, count);
                    List<DataTreeNode> tnl = mapValue.processNode(state);
                    if (tnl != null) {
                        state.push(tnl);
                        List<DataTreeNode> children = processNodeUpdates(state, e.getValue());
                        if (children != null) {
                            list.addAll(children);
                        }
                        state.pop().release();
                    }
                }
            }
        } else {
            pushed += processNodeByValue(list, state, name);
        }
        while (pushed-- > 0) {
            state.pop().release();
        }
        if (!list.isEmpty()) {
            return list;
        } else {
            return null;
        }
    }

    /**
     * can be called by subclasses to create/update nodes
     */
    public final int processNodeByValue(List<DataTreeNode> list, TreeMapState state, ValueObject name) {
        if (each != null) {
            List<DataTreeNode> next = state.processPathElement(each);
            if (push) {
                if (next.size() > 1) {
                    throw new RuntimeException("push and each are incompatible for > 1 return nodes");
                }
                if (next.size() == 1) {
                    state.push(next.get(0));
                    return 1;
                }
            } else if (next != null) {
                list.addAll(next);
            }
            return 0;
        }
        DataTreeNode parent = state.current();
        String sv = ValueUtil.asNativeString(name);
        if (delete) {
            parent.deleteNode(sv);
        }
        /** get db for parent node once we're past it (since it has children) */
        DataTreeNode child = getOrCreateNode(state, sv);
        boolean isnew = state.getAndClearLastWasNew();
        /** can be null if parent is deleted by another thread or if create == false */
        if (child == null) {
            return 0;
        }
        /* bail if only new nodes are required */
        if (once && !isnew) {
            child.release();
            return 0;
        }
        try {
            /** child node accounting and custom data updates */
            if (assignHits()) {
                state.setAssignmentValue(hitsField.getLong(state.getBundle()).orElse(0l));
            }
            child.updateChildData(state, this);
            /** update node data accounting */
            parent.updateParentData(state, child, isnew);
            if (push) {
                state.push(child);
                return 1;
            } else {
                list.add(child);
                return 0;
            }
        } catch (Throwable t) {
            child.release();
            throw t;
        }
    }
}
