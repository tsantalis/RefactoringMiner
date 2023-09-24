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
package org.apache.commons.jxpath.ri.model.jdom;

import java.util.List;
import java.util.Locale;

import org.apache.commons.jxpath.JXPathAbstractFactoryException;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;
import org.apache.commons.jxpath.ri.Compiler;
import org.apache.commons.jxpath.ri.NamespaceResolver;
import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.compiler.NodeNameTest;
import org.apache.commons.jxpath.ri.compiler.NodeTest;
import org.apache.commons.jxpath.ri.compiler.NodeTypeTest;
import org.apache.commons.jxpath.ri.compiler.ProcessingInstructionTest;
import org.apache.commons.jxpath.ri.model.NodeIterator;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.apache.commons.jxpath.util.TypeUtils;
import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;

/**
 * A Pointer that points to a DOM node.
 *
 * @author Dmitri Plotnikov
 * @version $Revision$ $Date$
 */
public class JDOMNodePointer extends NodePointer {
    private static final long serialVersionUID = -6346532297491082651L;

    private Object node;
    private String id;
    private NamespaceResolver localNamespaceResolver;

    /** XML ns uri */
    public static final String XML_NAMESPACE_URI =
            "http://www.w3.org/XML/1998/namespace";

    /** XMLNS ns uri */
    public static final String XMLNS_NAMESPACE_URI =
            "http://www.w3.org/2000/xmlns/";

    /**
     * Create a new JDOMNodePointer.
     * @param node pointed
     * @param locale Locale
     */
    public JDOMNodePointer(Object node, Locale locale) {
        super(null, locale);
        this.node = node;
    }

    /**
     * Create a new JDOMNodePointer.
     * @param node pointed
     * @param locale Locale
     * @param id String id
     */
    public JDOMNodePointer(Object node, Locale locale, String id) {
        super(null, locale);
        this.node = node;
        this.id = id;
    }

    /**
     * Create a new JDOMNodePointer.
     * @param parent NodePointer
     * @param node pointed
     */
    public JDOMNodePointer(NodePointer parent, Object node) {
        super(parent);
        this.node = node;
    }

    public NodeIterator childIterator(
        NodeTest test,
        boolean reverse,
        NodePointer startWith) {
        return new JDOMNodeIterator(this, test, reverse, startWith);
    }

    public NodeIterator attributeIterator(QName name) {
        return new JDOMAttributeIterator(this, name);
    }

    public NodeIterator namespaceIterator() {
        return new JDOMNamespaceIterator(this);
    }

    public NodePointer namespacePointer(String prefix) {
        return new JDOMNamespacePointer(this, prefix);
    }

    public String getNamespaceURI() {
        return getNamespaceURI(node);
    }

    /**
     * Get the ns uri of the specified node.
     * @param node Node to check
     * @return String
     */
    private static String getNamespaceURI(Object node) {
        if (node instanceof Element) {
            Element element = (Element) node;
            String ns = element.getNamespaceURI();
            if (ns != null && ns.equals("")) {
                ns = null;
            }
            return ns;
        }
        return null;
    }

    public synchronized NamespaceResolver getNamespaceResolver() {
        if (localNamespaceResolver == null) {
            localNamespaceResolver = new NamespaceResolver(super.getNamespaceResolver());
            localNamespaceResolver.setNamespaceContextPointer(this);
        }
        return localNamespaceResolver;
    }

    public String getNamespaceURI(String prefix) {
        if (prefix.equals("xml")) {
            return Namespace.XML_NAMESPACE.getURI();
        }
        Element element = null;
        if (node instanceof Document) {
            element = ((Document) node).getRootElement();
        }
        if (node instanceof Element) {
            element = (Element) node;
        }
        if (element == null) {
            return null;
        }
        Namespace ns = element.getNamespace(prefix);
        return ns == null ? null : ns.getURI();
    }

    public int compareChildNodePointers(
        NodePointer pointer1,
        NodePointer pointer2) {
        Object node1 = pointer1.getBaseValue();
        Object node2 = pointer2.getBaseValue();
        if (node1 == node2) {
            return 0;
        }

        if ((node1 instanceof Attribute) && !(node2 instanceof Attribute)) {
            return -1;
        }
        if (
            !(node1 instanceof Attribute) && (node2 instanceof Attribute)) {
            return 1;
        }
        if (
            (node1 instanceof Attribute) && (node2 instanceof Attribute)) {
            List list = ((Element) getNode()).getAttributes();
            int length = list.size();
            for (int i = 0; i < length; i++) {
                Object n = list.get(i);
                if (n == node1) {
                    return -1;
                }
                else if (n == node2) {
                    return 1;
                }
            }
            return 0; // Should not happen
        }

        if (!(node instanceof Element)) {
            throw new RuntimeException(
                "JXPath internal error: "
                    + "compareChildNodes called for "
                    + node);
        }

        List children = ((Element) node).getContent();
        int length = children.size();
        for (int i = 0; i < length; i++) {
            Object n = children.get(i);
            if (n == node1) {
                return -1;
            }
            if (n == node2) {
                return 1;
            }
        }

        return 0;
    }

    public Object getBaseValue() {
        return node;
    }

    public boolean isCollection() {
        return false;
    }

    public int getLength() {
        return 1;
    }

    public boolean isLeaf() {
        if (node instanceof Element) {
            return ((Element) node).getContent().size() == 0;
        }
        if (node instanceof Document) {
            return ((Document) node).getContent().size() == 0;
        }
        return true;
    }

    public QName getName() {
        String ns = null;
        String ln = null;
        if (node instanceof Element) {
            ns = ((Element) node).getNamespacePrefix();
            if (ns != null && ns.equals("")) {
                ns = null;
            }
            ln = ((Element) node).getName();
        }
        else if (node instanceof ProcessingInstruction) {
            ln = ((ProcessingInstruction) node).getTarget();
        }
        return new QName(ns, ln);
    }

    public Object getImmediateNode() {
        return node;
    }

    public Object getValue() {
        if (node instanceof Element) {
            StringBuffer buf = new StringBuffer();
            for (NodeIterator children = childIterator(null, false, null); children.setPosition(children.getPosition() + 1);) {
                NodePointer ptr = children.getNodePointer();
                if (ptr.getImmediateNode() instanceof Element || ptr.getImmediateNode() instanceof Text) {
                    buf.append(ptr.getValue());
                }
            }
            return buf.toString();
        }
        if (node instanceof Comment) {
            String text = ((Comment) node).getText();
            if (text != null) {
                text = text.trim();
            }
            return text;
        }
        String result = null;
        if (node instanceof Text) {
            result = ((Text) node).getText();
        }
        if (node instanceof ProcessingInstruction) {
            result = ((ProcessingInstruction) node).getData();
        }
        boolean trim = !"preserve".equals(findEnclosingAttribute(node, "space", Namespace.XML_NAMESPACE));
        return result != null && trim ? result.trim() : result;
    }

    public void setValue(Object value) {
        if (node instanceof Text) {
            String string = (String) TypeUtils.convert(value, String.class);
            if (string != null && !string.equals("")) {
                ((Text) node).setText(string);
            }
            else {
                nodeParent(node).removeContent((Text) node);
            }
        }
        else {
            Element element = (Element) node;
            element.getContent().clear();

            if (value instanceof Element) {
                Element valueElement = (Element) value;
                addContent(valueElement.getContent());
            }
            else if (value instanceof Document) {
                Document valueDocument = (Document) value;
                addContent(valueDocument.getContent());
            }
            else if (value instanceof Text || value instanceof CDATA) {
                String string = ((Text) value).getText();
                element.addContent(new Text(string));
            }
            else if (value instanceof ProcessingInstruction) {
                ProcessingInstruction pi =
                    (ProcessingInstruction) ((ProcessingInstruction) value)
                        .clone();
                element.addContent(pi);
            }
            else if (value instanceof Comment) {
                Comment comment = (Comment) ((Comment) value).clone();
                element.addContent(comment);
            }
            else {
                String string = (String) TypeUtils.convert(value, String.class);
                if (string != null && !string.equals("")) {
                    element.addContent(new Text(string));
                }
            }
        }
    }

    /**
     * Add the specified content to this element.
     * @param content List
     */
    private void addContent(List content) {
        Element element = (Element) node;
        int count = content.size();

        for (int i = 0; i < count; i++) {
            Object child = content.get(i);
            if (child instanceof Element) {
                child = ((Element) child).clone();
                element.addContent((Element) child);
            }
            else if (child instanceof Text) {
                child = ((Text) child).clone();
                element.addContent((Text) child);
            }
            else if (node instanceof CDATA) {
                child = ((CDATA) child).clone();
                element.addContent((CDATA) child);
            }
            else if (node instanceof ProcessingInstruction) {
                child = ((ProcessingInstruction) child).clone();
                element.addContent((ProcessingInstruction) child);
            }
            else if (node instanceof Comment) {
                child = ((Comment) child).clone();
                element.addContent((Comment) child);
            }
        }
    }

    public boolean testNode(NodeTest test) {
        return testNode(this, node, test);
    }

    /**
     * Execute test against node on behalf of pointer.
     * @param pointer Pointer
     * @param node to test
     * @param test to execute
     * @return true if node passes test
     */
    public static boolean testNode(
        NodePointer pointer,
        Object node,
        NodeTest test) {
        if (test == null) {
            return true;
        }
        if (test instanceof NodeNameTest) {
            if (!(node instanceof Element)) {
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
                        .equals(JDOMNodePointer.getLocalName(node))) {
                String nodeNS = JDOMNodePointer.getNamespaceURI(node);
                return equalStrings(namespaceURI, nodeNS) || nodeNS == null
                        && equalStrings(testPrefix, getPrefix(node));
            }
            return false;
        }
        if (test instanceof NodeTypeTest) {
            switch (((NodeTypeTest) test).getNodeType()) {
                case Compiler.NODE_TYPE_NODE :
                    return true;
                case Compiler.NODE_TYPE_TEXT :
                    return (node instanceof Text) || (node instanceof CDATA);
                case Compiler.NODE_TYPE_COMMENT :
                    return node instanceof Comment;
                case Compiler.NODE_TYPE_PI :
                    return node instanceof ProcessingInstruction;
                default:
                    return false;
            }
        }
        if (test instanceof ProcessingInstructionTest && node instanceof ProcessingInstruction) {
            String testPI = ((ProcessingInstructionTest) test).getTarget();
            String nodePI = ((ProcessingInstruction) node).getTarget();
            return testPI.equals(nodePI);
        }
        return false;
    }

    /**
     * Learn whether two strings are == or .equals()
     * @param s1 string 1
     * @param s2 string 2
     * @return true if equal
     */
    private static boolean equalStrings(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        s1 = s1 == null ? "" : s1.trim();
        s2 = s2 == null ? "" : s2.trim();
        return s1.equals(s2);
    }

    /**
     * Get the prefix from a given node.
     * @param node to check
     * @return String
     */
    public static String getPrefix(Object node) {
        if (node instanceof Element) {
            String prefix = ((Element) node).getNamespacePrefix();
            return (prefix == null || prefix.equals("")) ? null : prefix;
        }
        if (node instanceof Attribute) {
            String prefix = ((Attribute) node).getNamespacePrefix();
            return (prefix == null || prefix.equals("")) ? null : prefix;
        }
        return null;
    }

    /**
     * Get the local name of the specified node.
     * @param node to check
     * @return String local name
     */
    public static String getLocalName(Object node) {
        if (node instanceof Element) {
            return ((Element) node).getName();
        }
        if (node instanceof Attribute) {
            return ((Attribute) node).getName();
        }
        return null;
    }

    /**
     * Returns true if the xml:lang attribute for the current node
     * or its parent has the specified prefix <i>lang</i>.
     * If no node has this prefix, calls <code>super.isLanguage(lang)</code>.
     * @param lang to compare
     * @return true if this element uses the specified language.
     */
    public boolean isLanguage(String lang) {
        String current = getLanguage();
        return current == null ? super.isLanguage(lang) : current.toUpperCase(
                Locale.ENGLISH).startsWith(lang.toUpperCase(Locale.ENGLISH));
    }

    /**
     * Get the language of this element.
     * @return String language
     */
    protected String getLanguage() {
        return findEnclosingAttribute(node, "lang", Namespace.XML_NAMESPACE);
    }

    /**
     * Find the nearest occurrence of the specified attribute
     * on the specified and enclosing elements.
     * @param n current node
     * @param attrName attribute name
     * @param ns Namespace
     * @return attribute value
     */
    protected static String findEnclosingAttribute(Object n, String attrName, Namespace ns) {
        while (n != null) {
            if (n instanceof Element) {
                Element e = (Element) n;
                String attr = e.getAttributeValue(attrName, ns);
                if (attr != null && !attr.equals("")) {
                    return attr;
                }
            }
            n = nodeParent(n);
        }
        return null;
    }

    /**
     * Get the parent of the specified node.
     * @param node to check
     * @return parent Element
     */
    private static Element nodeParent(Object node) {
        if (node instanceof Element) {
            Object parent = ((Element) node).getParent();
            return parent instanceof Element ? (Element) parent : null;
        }
        if (node instanceof Text) {
            return (Element) ((Text) node).getParent();
        }
        if (node instanceof CDATA) {
            return (Element) ((CDATA) node).getParent();
        }
        if (node instanceof ProcessingInstruction) {
            return (Element) ((ProcessingInstruction) node).getParent();
        }
        if (node instanceof Comment) {
            return (Element) ((Comment) node).getParent();
        }
        return null;
    }

    public NodePointer createChild(
        JXPathContext context,
        QName name,
        int index) {
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
            String namespaceURI = prefix == null ? null : context
                    .getNamespaceURI(prefix);
            nodeTest = new NodeNameTest(name, namespaceURI);

            NodeIterator it =
                childIterator(nodeTest, false, null);
            if (it != null && it.setPosition(index + 1)) {
                return it.getNodePointer();
            }
        }
        throw new JXPathAbstractFactoryException("Factory could not create "
                + "a child node for path: " + asPath() + "/" + name + "["
                + (index + 1) + "]");
    }

    public NodePointer createChild(
            JXPathContext context, QName name, int index, Object value) {
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
            String namespaceUri = getNamespaceResolver().getNamespaceURI(prefix);
            if (namespaceUri == null) {
                throw new JXPathException(
                    "Unknown namespace prefix: " + prefix);
            }
            Namespace ns = Namespace.getNamespace(prefix, namespaceUri);
            Attribute attr = element.getAttribute(name.getName(), ns);
            if (attr == null) {
                element.setAttribute(name.getName(), "", ns);
            }
        }
        else {
            Attribute attr = element.getAttribute(name.getName());
            if (attr == null) {
                element.setAttribute(name.getName(), "");
            }
        }
        NodeIterator it = attributeIterator(name);
        it.setPosition(1);
        return it.getNodePointer();
    }

    public void remove() {
        Element parent = nodeParent(node);
        if (parent == null) {
            throw new JXPathException("Cannot remove root JDOM node");
        }
        parent.getContent().remove(node);
    }

    public String asPath() {
        if (id != null) {
            return "id('" + escape(id) + "')";
        }

        StringBuffer buffer = new StringBuffer();
        if (parent != null) {
            buffer.append(parent.asPath());
        }
        if (node instanceof Element) {
            // If the parent pointer is not a JDOMNodePointer, it is
            // the parent's responsibility to produce the node test part
            // of the path
            if (parent instanceof JDOMNodePointer) {
                if (buffer.length() == 0
                    || buffer.charAt(buffer.length() - 1) != '/') {
                    buffer.append('/');
                }
                String nsURI = getNamespaceURI();
                String ln = JDOMNodePointer.getLocalName(node);

                if (nsURI == null) {
                    buffer.append(ln);
                    buffer.append('[');
                    buffer.append(getRelativePositionByQName()).append(']');
                }
                else {
                    String prefix = getNamespaceResolver().getPrefix(nsURI);
                    if (prefix != null) {
                        buffer.append(prefix);
                        buffer.append(':');
                        buffer.append(ln);
                        buffer.append('[');
                        buffer.append(getRelativePositionByQName());
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
        }
        else if (node instanceof Text || node instanceof CDATA) {
            buffer.append("/text()");
            buffer.append('[').append(getRelativePositionOfTextNode()).append(
                ']');
        }
        else if (node instanceof ProcessingInstruction) {
            buffer.append("/processing-instruction(\'").append(((ProcessingInstruction) node).getTarget()).append(
                "')");
            buffer.append('[').append(getRelativePositionOfPI()).append(
                ']');
        }
        return buffer.toString();
    }

    /**
     * Get relative position of this among like-named siblings.
     * @return 1..n
     */
    private int getRelativePositionByQName() {
        if (node instanceof Element) {
            Object parent = ((Element) node).getParent();
            if (!(parent instanceof Element)) {
                return 1;
            }

            List children = ((Element) parent).getContent();
            int count = 0;
            String name = ((Element) node).getQualifiedName();
            for (int i = 0; i < children.size(); i++) {
                Object child = children.get(i);
                if ((child instanceof Element)
                    && ((Element) child).getQualifiedName().equals(name)) {
                    count++;
                }
                if (child == node) {
                    break;
                }
            }
            return count;
        }
        return 1;
    }


    /**
     * Get relative position of this among all siblings.
     * @return 1..n
     */
    private int getRelativePositionOfElement() {
        Object parent = ((Element) node).getParent();
        if (parent == null) {
            return 1;
        }
        List children;
        if (parent instanceof Element) {
            children = ((Element) parent).getContent();
        }
        else {
            children = ((Document) parent).getContent();
        }
        int count = 0;
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            if (child instanceof Element) {
                count++;
            }
            if (child == node) {
                break;
            }
        }
        return count;
    }

    /**
     * Get the relative position of this among sibling text nodes.
     * @return 1..n
     */
    private int getRelativePositionOfTextNode() {
        Element parent;
        if (node instanceof Text) {
            parent = (Element) ((Text) node).getParent();
        }
        else {
            parent = (Element) ((CDATA) node).getParent();
        }
        if (parent == null) {
            return 1;
        }
        List children = parent.getContent();
        int count = 0;
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            if (child instanceof Text || child instanceof CDATA) {
                count++;
            }
            if (child == node) {
                break;
            }
        }
        return count;
    }

    /**
     * Get the relative position of this among same-target processing instruction siblings.
     * @return 1..n
     */
    private int getRelativePositionOfPI() {
        String target = ((ProcessingInstruction) node).getTarget();
        Element parent = (Element) ((ProcessingInstruction) node).getParent();
        if (parent == null) {
            return 1;
        }
        List children = parent.getContent();
        int count = 0;
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            if (child instanceof ProcessingInstruction
                && (target == null
                    || target.equals(
                        ((ProcessingInstruction) child).getTarget()))) {
                count++;
            }
            if (child == node) {
                break;
            }
        }
        return count;
    }

    public int hashCode() {
        return node.hashCode();
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof JDOMNodePointer)) {
            return false;
        }

        JDOMNodePointer other = (JDOMNodePointer) object;
        return node == other.node;
    }

}
