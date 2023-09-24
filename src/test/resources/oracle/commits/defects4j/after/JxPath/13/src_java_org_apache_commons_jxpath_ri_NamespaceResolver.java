/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jxpath.ri;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.jxpath.Pointer;
import org.apache.commons.jxpath.ri.model.NodeIterator;
import org.apache.commons.jxpath.ri.model.NodePointer;

/**
 * Namespace resolver for JXPathContextReferenceImpl.
 *
 * @author Dmitri Plotnikov
 * @version $Revision$ $Date$
 */
public class NamespaceResolver implements Cloneable {
    final protected NamespaceResolver parent;
    protected HashMap namespaceMap = new HashMap();
    protected HashMap reverseMap;
    protected NodePointer pointer;
    private boolean sealed;

    /**
     * Find the namespace prefix for the specified namespace URI and NodePointer.
     * @param pointer
     * @param namespaceURI
     * @return prefix if found
     * @since JXPath 1.3
     */
    protected static String getPrefix(NodePointer pointer, String namespaceURI) {
        NodePointer currentPointer = pointer;
        while (currentPointer != null) {
            NodeIterator ni = currentPointer.namespaceIterator();
            for (int position = 1; ni != null && ni.setPosition(position); position++) {
                NodePointer nsPointer = ni.getNodePointer();
                String uri = nsPointer.getNamespaceURI();
                if (uri.equals(namespaceURI)) {
                    String prefix = nsPointer.getName().getName();
                    if (!"".equals(prefix)) {
                        return prefix;
                    }
                }
            }
            currentPointer = pointer.getParent();
        }
        return null;
    }

    /**
     * Create a new NamespaceResolver.
     */
    public NamespaceResolver() {
        this(null);
    }

    /**
     * Create a new NamespaceResolver.
     * @param parent
     */
    public NamespaceResolver(NamespaceResolver parent) {
        this.parent = parent;
    }

    /**
     * Registers a namespace prefix.
     * 
     * @param prefix A namespace prefix
     * @param namespaceURI A URI for that prefix
     */
    public synchronized void registerNamespace(String prefix, String namespaceURI) {
        if (isSealed()) {
            throw new IllegalStateException(
                    "Cannot register namespaces on a sealed NamespaceResolver");
        }
        namespaceMap.put(prefix, namespaceURI);
        reverseMap = null;
    }
    
    /**
     * Register a namespace for the expression context.
     * @param pointer the Pointer to set.
     */
    public void setNamespaceContextPointer(NodePointer pointer) {
        this.pointer = pointer;
    }

    /**
     * Get the namespace context pointer.
     * @return Pointer
     */
    public Pointer getNamespaceContextPointer() {
        if (pointer == null && parent != null) {
            return parent.getNamespaceContextPointer();
        }
        return pointer;
    }
    
    /**
     * Given a prefix, returns a registered namespace URI. If the requested
     * prefix was not defined explicitly using the registerNamespace method,
     * JXPathContext will then check the context node to see if the prefix is
     * defined there. See
     * {@link #setNamespaceContextPointer(NodePointer) setNamespaceContextPointer}.
     * 
     * @param prefix The namespace prefix to look up
     * @return namespace URI or null if the prefix is undefined.
     */
    public synchronized String getNamespaceURI(String prefix) {
        String uri = getExternallyRegisteredNamespaceURI(prefix);
        return uri == null && pointer != null ? pointer.getNamespaceURI(prefix)
                : uri;
    }

    /**
     * Given a prefix, returns an externally registered namespace URI.
     * 
     * @param prefix The namespace prefix to look up
     * @return namespace URI or null if the prefix is undefined.
     * @since JXPath 1.3
     */
     protected synchronized String getExternallyRegisteredNamespaceURI(
            String prefix) {
        String uri = (String) namespaceMap.get(prefix);
        return uri == null && parent != null ? parent
                .getExternallyRegisteredNamespaceURI(prefix) : uri;
    }

    /**
     * Get the prefix associated with the specifed namespace URI.
     * @param namespaceURI the ns URI to check.
     * @return String prefix
     */
    public synchronized String getPrefix(String namespaceURI) {
        String prefix = getExternallyRegisteredPrefix(namespaceURI);
        return prefix == null && pointer != null ? getPrefix(pointer,
                namespaceURI) : prefix;
    }

    /**
     * Get the nearest prefix found that matches an externally-registered namespace. 
     * @param namespaceURI
     * @return String prefix if found.
     * @since JXPath 1.3
     */
    protected synchronized String getExternallyRegisteredPrefix(String namespaceURI) {
        if (reverseMap == null) {
            reverseMap = new HashMap();
            Iterator it = namespaceMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                reverseMap.put(entry.getValue(), entry.getKey());
            }
        }
        String prefix = (String) reverseMap.get(namespaceURI);
        return prefix == null && parent != null ? parent
                .getExternallyRegisteredPrefix(namespaceURI) : prefix;
    }

    /**
     * Learn whether this NamespaceResolver has been sealed.
     * @return
     */
    public boolean isSealed() {
        return sealed;
    }

    /**
     * Seal this {@link NamespaceResolver}.
     */
    public void seal() {
        sealed = true;
        if (parent != null) {
            parent.seal();
        }
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        try {
            NamespaceResolver result = (NamespaceResolver) super.clone();
            result.sealed = false;
            return result;
        }
        catch (CloneNotSupportedException e) {
            // Of course, it's supported.
            e.printStackTrace();
            return null;
        }
    }

}