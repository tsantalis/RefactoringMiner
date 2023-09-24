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
package org.apache.commons.jxpath.ri.model.dom;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.jxpath.JXPathAbstractFactoryException;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;
import org.apache.commons.jxpath.Pointer;
import org.apache.commons.jxpath.ri.Compiler;
import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.compiler.NodeNameTest;
import org.apache.commons.jxpath.ri.compiler.NodeTest;
import org.apache.commons.jxpath.ri.compiler.NodeTypeTest;
import org.apache.commons.jxpath.ri.compiler.ProcessingInstructionTest;
import org.apache.commons.jxpath.ri.model.NodeIterator;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.apache.commons.jxpath.ri.model.beans.NullPointer;
import org.apache.commons.jxpath.util.TypeUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/**
 * A Pointer that points to a DOM node.
 *
 * @author Dmitri Plotnikov
 * @version $Revision$ $Date$
 */
public class DOMNodePointer extends NodePointer {

    private static final long serialVersionUID = -8751046933894857319L;
    
    private Node node;
    private Map namespaces;
    private String defaultNamespace;
    private String id;

    public static final String XML_NAMESPACE_URI = 
            "http://www.w3.org/XML/1998/namespace";
    public static final String XMLNS_NAMESPACE_URI = 
            "http://www.w3.org/2000/xmlns/";

    public DOMNodePointer(Node node, Locale locale) {
        super(null, locale);
        this.node = node;
    }

    public DOMNodePointer(Node node, Locale locale, String id) {
        super(null, locale);
        this.node = node;
        this.id = id;
    }

    public DOMNodePointer(NodePointer parent, Node node) {
        super(parent);
        this.node = node;
    }
    
    public boolean testNode(NodeTest test) {
        return testNode(node, test);
    }

    public static boolean testNode(Node node, NodeTest test) {
        if (test == null) {
            return true;
        }
        if (test instanceof NodeNameTest) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                return false;
            }

            NodeNameTest nodeNameTest = (NodeNameTest) test;
            QName testName = nodeNameTest.getNodeName();
            String namespaceURI = nodeNameTest.getNamespaceURI();
            boolean wildcard = nodeNameTest.isWildcard();
            String testPrefix = testName.getPrefix();
            if (wildcard && testPrefix == null) {
                return true;
            }
            if (wildcard
                || testName.getName()
                        .equals(DOMNodePointer.getLocalName(node))) {
                String nodeNS = DOMNodePointer.getNamespaceURI(node);
                return equalStrings(namespaceURI, nodeNS) || nodeNS == null
                        && equalStrings(testPrefix, getPrefix(node));
            }
            return false;
        }
        if (test instanceof NodeTypeTest) {
            int nodeType = node.getNodeType();
            switch (((NodeTypeTest) test).getNodeType()) {
                case Compiler.NODE_TYPE_NODE :
                    return nodeType == Node.ELEMENT_NODE
                            || nodeType == Node.DOCUMENT_NODE;
                case Compiler.NODE_TYPE_TEXT :
                    return nodeType == Node.CDATA_SECTION_NODE
                        || nodeType == Node.TEXT_NODE;
                case Compiler.NODE_TYPE_COMMENT :
                    return nodeType == Node.COMMENT_NODE;
                case Compiler.NODE_TYPE_PI :
                    return nodeType == Node.PROCESSING_INSTRUCTION_NODE;
            }
            return false;
        }
        if (test instanceof ProcessingInstructionTest) {
            if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                String testPI = ((ProcessingInstructionTest) test).getTarget();
                String nodePI = ((ProcessingInstruction) node).getTarget();
                return testPI.equals(nodePI);
            }
        }
        return false;
    }

    private static boolean equalStrings(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        s1 = s1 == null ? "" : s1.trim();
        s2 = s2 == null ? "" : s2.trim();
        return s1.equals(s2);
    }

    public QName getName() {
        String ln = null;
        String ns = null;
        int type = node.getNodeType();
        if (type == Node.ELEMENT_NODE) {
            ns = DOMNodePointer.getPrefix(node);
            ln = DOMNodePointer.getLocalName(node);
        }
        else if (type == Node.PROCESSING_INSTRUCTION_NODE) {
            ln = ((ProcessingInstruction) node).getTarget();
        }
        return new QName(ns, ln);
    }

    public String getNamespaceURI() {
        return getNamespaceURI(node);
    }

    public NodeIterator childIterator(
        NodeTest test,
        boolean reverse,
        NodePointer startWith) 
    {
        return new DOMNodeIterator(this, test, reverse, startWith);
    }

    public NodeIterator attributeIterator(QName name) {
        return new DOMAttributeIterator(this, name);
    }

    public NodePointer namespacePointer(String prefix) {
        return new NamespacePointer(this, prefix);
    }

    public NodeIterator namespaceIterator() {
        return new DOMNamespaceIterator(this);
    }

    /* (non-Javadoc)
     * @see org.apache.commons.jxpath.ri.model.NodePointer#getNamespaceResolver()
     */

    public String getNamespaceURI(String prefix) {
        if (prefix == null || prefix.equals("")) {
            return getDefaultNamespaceURI();
        }

        if (prefix.equals("xml")) {
            return XML_NAMESPACE_URI;
        }

        if (prefix.equals("xmlns")) {
            return XMLNS_NAMESPACE_URI;
        }

        String namespace = null;
        if (namespaces == null) {
            namespaces = new HashMap();
        }
        else {
            namespace = (String) namespaces.get(prefix);
        }

        if (namespace == null) {
            String qname = "xmlns:" + prefix;
            Node aNode = node;
            if (aNode instanceof Document) {
                aNode = ((Document)aNode).getDocumentElement();
            }
            while (aNode != null) {
                if (aNode.getNodeType() == Node.ELEMENT_NODE) {
                    Attr attr = ((Element) aNode).getAttributeNode(qname);
                    if (attr != null) {
                        namespace = attr.getValue();
                        break;
                    }
                }
                aNode = aNode.getParentNode();
            }
            if (namespace == null || namespace.equals("")) {
                namespace = NodePointer.UNKNOWN_NAMESPACE;
            }
        }

        namespaces.put(prefix, namespace);
        if (namespace == UNKNOWN_NAMESPACE) {
            return null;
        }
        
        // TBD: We are supposed to resolve relative URIs to absolute ones.
        return namespace;
    }

    public String getDefaultNamespaceURI() {
        if (defaultNamespace == null) {
            Node aNode = node;
            if (aNode instanceof Document) {
                aNode = ((Document) aNode).getDocumentElement();
            }
            while (aNode != null) {
                if (aNode.getNodeType() == Node.ELEMENT_NODE) {
                    Attr attr = ((Element) aNode).getAttributeNode("xmlns");
                    if (attr != null) {
                        defaultNamespace = attr.getValue();
                        break;
                    }
                }
                aNode = aNode.getParentNode();
            }
        }
        if (defaultNamespace == null) {
            defaultNamespace = "";
        }
        // TBD: We are supposed to resolve relative URIs to absolute ones.
        return defaultNamespace.equals("") ? null : defaultNamespace;
    }

    public Object getBaseValue() {
        return node;
    }

    public Object getImmediateNode() {
        return node;
    }

    public boolean isActual() {
        return true;
    }

    public boolean isCollection() {
        return false;
    }

    public int getLength() {
        return 1;
    }

    public boolean isLeaf() {
        return !node.hasChildNodes();
    }

    /**
     * Returns true if the xml:lang attribute for the current node
     * or its parent has the specified prefix <i>lang</i>.
     * If no node has this prefix, calls <code>super.isLanguage(lang)</code>.
     */
    public boolean isLanguage(String lang) {
        String current = getLanguage();
        return current == null ? super.isLanguage(lang)
                : current.toUpperCase().startsWith(lang.toUpperCase());
    }

    protected static String findEnclosingAttribute(Node n, String attrName) {
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                String attr = e.getAttribute(attrName);
                if (attr != null && !attr.equals("")) {
                    return attr;
                }
            }
            n = n.getParentNode();
        }
        return null;
    }

    protected String getLanguage() {
        return findEnclosingAttribute(node, "xml:lang");
    }

    /**
     * Sets contents of the node to the specified value. If the value is
     * a String, the contents of the node are replaced with this text.
     * If the value is an Element or Document, the children of the
     * node are replaced with the children of the passed node.
     */
    public void setValue(Object value) {
        if (node.getNodeType() == Node.TEXT_NODE
            || node.getNodeType() == Node.CDATA_SECTION_NODE) {
            String string = (String) TypeUtils.convert(value, String.class);
            if (string != null && !string.equals("")) {
                node.setNodeValue(string);
            }
            else {
                node.getParentNode().removeChild(node);
            }
        }
        else {
            NodeList children = node.getChildNodes();
            int count = children.getLength();
            for (int i = count; --i >= 0;) {
                Node child = children.item(i);
                node.removeChild(child);
            }

            if (value instanceof Node) {
                Node valueNode = (Node) value;
                if (valueNode instanceof Element
                    || valueNode instanceof Document) {
                    children = valueNode.getChildNodes();
                    for (int i = 0; i < children.getLength(); i++) {
                        Node child = children.item(i);
                        node.appendChild(child.cloneNode(true));
                    }
                }
                else {
                    node.appendChild(valueNode.cloneNode(true));
                }
            }
            else {
                String string = (String) TypeUtils.convert(value, String.class);
                if (string != null && !string.equals("")) {
                    Node textNode =
                        node.getOwnerDocument().createTextNode(string);
                    node.appendChild(textNode);
                }
            }
        }
    }
    
    public NodePointer createChild(
        JXPathContext context,
        QName name,
        int index) 
    {
        if (index == WHOLE_COLLECTION) {
            index = 0;
        }
        boolean success =
            getAbstractFactory(context).createObject(
                context,
                this,
                node,
                name.toString(),
                index);
        if (success) {
            NodeTest nodeTest;
            String prefix = name.getPrefix();
            String namespaceURI = prefix == null ? null : 
                context.getNamespaceURI(prefix);
            nodeTest = new NodeNameTest(name, namespaceURI);

            NodeIterator it = childIterator(nodeTest, false, null);
            if (it != null && it.setPosition(index + 1)) {
                return it.getNodePointer();
            }
        }
        throw new JXPathAbstractFactoryException(
                "Factory could not create a child node for path: " + asPath()
                        + "/" + name + "[" + (index + 1) + "]");
    }

    public NodePointer createChild(JXPathContext context, 
                QName name, int index, Object value)
    {
        NodePointer ptr = createChild(context, name, index);
        ptr.setValue(value);
        return ptr;
    }

    public NodePointer createAttribute(JXPathContext context, QName name) {
        if (!(node instanceof Element)) {
            return super.createAttribute(context, name);
        }
        Element element = (Element) node;
        String prefix = name.getPrefix();
        if (prefix != null) {
            String ns = getNamespaceURI(prefix);
            if (ns == null) {
                throw new JXPathException(
                    "Unknown namespace prefix: " + prefix);
            }
            element.setAttributeNS(ns, name.toString(), "");
        }
        else {
            if (!element.hasAttribute(name.getName())) {
                element.setAttribute(name.getName(), "");
            }
        }
        NodeIterator it = attributeIterator(name);
        it.setPosition(1);
        return it.getNodePointer();
    }

    public void remove() {
        Node parent = node.getParentNode();
        if (parent == null) {
            throw new JXPathException("Cannot remove root DOM node");
        }
        parent.removeChild(node);
    }

    public String asPath() {
        if (id != null) {
            return "id('" + escape(id) + "')";
        }

        StringBuffer buffer = new StringBuffer();
        if (parent != null) {
            buffer.append(parent.asPath());
        }
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE :
                // If the parent pointer is not a DOMNodePointer, it is
                // the parent's responsibility to produce the node test part
                // of the path
                if (parent instanceof DOMNodePointer) {
                    if (buffer.length() == 0
                            || buffer.charAt(buffer.length() - 1) != '/') {
                        buffer.append('/');
                    }
                    String ln = DOMNodePointer.getLocalName(node);
                    String nsURI = getNamespaceURI();
                    if (nsURI == null) {
                        buffer.append(ln);
                        buffer.append('[');
                        buffer.append(getRelativePositionByName()).append(']');
                    }
                    else {
                        String prefix = getNamespaceResolver().getPrefix(nsURI);
                        if (prefix != null) {
                            buffer.append(prefix);
                            buffer.append(':');
                            buffer.append(ln);
                            buffer.append('[');
                            buffer.append(getRelativePositionByName());
                            buffer.append(']');
                        }
                        else {
                            buffer.append("node()");
                            buffer.append('[');
                            buffer.append(getRelativePositionOfElement());
                            buffer.append(']');
                        }
                    }
                }
            break;
            case Node.TEXT_NODE :
            case Node.CDATA_SECTION_NODE :
                buffer.append("/text()");
                buffer.append('[');
                buffer.append(getRelativePositionOfTextNode()).append(']');
                break;
            case Node.PROCESSING_INSTRUCTION_NODE :
                String target = ((ProcessingInstruction) node).getTarget();
                buffer.append("/processing-instruction(\'");
                buffer.append(target).append("')");
                buffer.append('[');
                buffer.append(getRelativePositionOfPI(target)).append(']');
                break;
            case Node.DOCUMENT_NODE :
                // That'll be empty
        }
        return buffer.toString();
    }

    private String escape(String string) {
        int index = string.indexOf('\'');
        while (index != -1) {
            string =
                string.substring(0, index)
                    + "&apos;"
                    + string.substring(index + 1);
            index = string.indexOf('\'');
        }
        index = string.indexOf('\"');
        while (index != -1) {
            string =
                string.substring(0, index)
                    + "&quot;"
                    + string.substring(index + 1);
            index = string.indexOf('\"');
        }
        return string;
    }

    private int getRelativePositionByName() {
        int count = 1;
        Node n = node.getPreviousSibling();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                String nm = n.getNodeName();
                if (nm.equals(node.getNodeName())) {
                    count++;
                }
            }
            n = n.getPreviousSibling();
        }
        return count;
    }
    
    private int getRelativePositionOfElement() {
        int count = 1;
        Node n = node.getPreviousSibling();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                count++;
            }
            n = n.getPreviousSibling();
        }
        return count;
    }

    private int getRelativePositionOfTextNode() {
        int count = 1;
        Node n = node.getPreviousSibling();
        while (n != null) {
            if (n.getNodeType() == Node.TEXT_NODE
                || n.getNodeType() == Node.CDATA_SECTION_NODE) {
                count++;
            }
            n = n.getPreviousSibling();
        }
        return count;
    }

    private int getRelativePositionOfPI(String target) {
        int count = 1;
        Node n = node.getPreviousSibling();
        while (n != null) {
            if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE
                && ((ProcessingInstruction) n).getTarget().equals(target)) {
                count++;
            }
            n = n.getPreviousSibling();
        }
        return count;
    }

    public int hashCode() {
        return System.identityHashCode(node);
    }

    public boolean equals(Object object) {
        return object == this || object instanceof DOMNodePointer && node == ((DOMNodePointer) object).node;
    }

    public static String getPrefix(Node node) {
        String prefix = node.getPrefix();
        if (prefix != null) {
            return prefix;
        }

        String name = node.getNodeName();
        int index = name.lastIndexOf(':');
        return index < 0 ? null : name.substring(0, index);
    }

    public static String getLocalName(Node node) {
        String localName = node.getLocalName();
        if (localName != null) {
            return localName;
        }

        String name = node.getNodeName();
        int index = name.lastIndexOf(':');
        return index < 0 ? name : name.substring(index + 1);
    }
    
    public static String getNamespaceURI(Node node) {
        if (node instanceof Document) {
            node = ((Document) node).getDocumentElement();
        }

        Element element = (Element) node;

        String uri = element.getNamespaceURI();
        if (uri != null) {
            return uri;
        }

        String prefix = getPrefix(node);
        String qname = prefix == null ? "xmlns" : "xmlns:" + prefix;

        Node aNode = node;
        while (aNode != null) {
            if (aNode.getNodeType() == Node.ELEMENT_NODE) {
                Attr attr = ((Element) aNode).getAttributeNode(qname);
                if (attr != null) {
                    return attr.getValue();
                }
            }
            aNode = aNode.getParentNode();
        }
        return null;
    }

    public Object getValue() {
        if (node.getNodeType() == Node.COMMENT_NODE) {
            String text = ((Comment) node).getData();
            return text == null ? "" : text.trim();
        }
        return stringValue(node);
    }

    private String stringValue(Node node) {
        int nodeType = node.getNodeType();
        if (nodeType == Node.COMMENT_NODE) {
            return "";
        }
        boolean trim = !"preserve".equals(findEnclosingAttribute(node, "xml:space"));
        if (nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE) {
            String text = node.getNodeValue();
            return text == null ? "" : trim ? text.trim() : text;
        }
        if (nodeType == Node.PROCESSING_INSTRUCTION_NODE) {
            String text = ((ProcessingInstruction) node).getData();
            return text == null ? "" : trim ? text.trim() : text;
        }
        NodeList list = node.getChildNodes();
        StringBuffer buf = new StringBuffer(16);
        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            buf.append(stringValue(child));
        }
        return buf.toString();
    }

    /**
     * Locates a node by ID.
     */
    public Pointer getPointerByID(JXPathContext context, String id) {
        Document document = node.getNodeType() == Node.DOCUMENT_NODE ? (Document) node
                : node.getOwnerDocument();
        Element element = document.getElementById(id);
        return element == null ? (Pointer) new NullPointer(getLocale(), id)
                : new DOMNodePointer(element, getLocale(), id);
    }

    private AbstractFactory getAbstractFactory(JXPathContext context) {
        AbstractFactory factory = context.getFactory();
        if (factory == null) {
            throw new JXPathException(
                "Factory is not set on the JXPathContext - "
                    + "cannot create path: "
                    + asPath());
        }
        return factory;
    }

    public int compareChildNodePointers(
            NodePointer pointer1, NodePointer pointer2)
    {
        Node node1 = (Node) pointer1.getBaseValue();
        Node node2 = (Node) pointer2.getBaseValue();
        if (node1 == node2) {
            return 0;
        }

        int t1 = node1.getNodeType();
        int t2 = node2.getNodeType();
        if (t1 == Node.ATTRIBUTE_NODE && t2 != Node.ATTRIBUTE_NODE) {
            return -1;
        }
        if (t1 != Node.ATTRIBUTE_NODE && t2 == Node.ATTRIBUTE_NODE) {
            return 1;
        }
        if (t1 == Node.ATTRIBUTE_NODE && t2 == Node.ATTRIBUTE_NODE) {
            NamedNodeMap map = ((Node) getNode()).getAttributes();
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node n = map.item(i);
                if (n == node1) {
                    return -1;
                }
                if (n == node2) {
                    return 1;
                }
            }
            return 0; // Should not happen
        }

        Node current = node.getFirstChild();
        while (current != null) {
            if (current == node1) {
                return -1;
            }
            if (current == node2) {
                return 1;
            }
            current = current.getNextSibling();
        }
        return 0;
    }
}
