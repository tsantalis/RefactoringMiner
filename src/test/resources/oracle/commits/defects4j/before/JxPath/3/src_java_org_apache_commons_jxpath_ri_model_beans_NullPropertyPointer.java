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
package org.apache.commons.jxpath.ri.model.beans;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathInvalidAccessException;
import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.model.NodePointer;

/**
 * @author Dmitri Plotnikov
 * @version $Revision$ $Date$
 */
public class NullPropertyPointer extends PropertyPointer {

    private String propertyName = "*";
    private boolean byNameAttribute = false;

    /**
     */
    public NullPropertyPointer(NodePointer parent) {
        super(parent);
    }

    public QName getName() {
        return new QName(propertyName);
    }

    public void setPropertyIndex(int index) {
    }

    public int getLength() {
        return 0;
    }

    public Object getBaseValue() {
        return null;
    }

    public Object getImmediateNode() {
        return null;
    }

    public boolean isLeaf() {
        return true;
    }    

    public NodePointer getValuePointer() {
        return new NullPointer(this,  new QName(getPropertyName()));
    }

    protected boolean isActualProperty() {
        return false;
    }

    public boolean isActual() {
        return false;
    }

    public boolean isContainer() {
        return true;
    }

    public void setValue(Object value) {
        if (parent == null || parent.isContainer()) {
            throw new JXPathInvalidAccessException(
                "Cannot set property "
                    + asPath()
                    + ", the target object is null");
        }
        else if (parent instanceof PropertyOwnerPointer &&
                ((PropertyOwnerPointer) parent).
                    isDynamicPropertyDeclarationSupported()){
            // If the parent property owner can create
            // a property automatically - let it do so
            PropertyPointer propertyPointer =
                ((PropertyOwnerPointer) parent).getPropertyPointer();
            propertyPointer.setPropertyName(propertyName);
            propertyPointer.setValue(value);
        }
        else {
            throw new JXPathInvalidAccessException(
                "Cannot set property "
                    + asPath()
                    + ", path does not match a changeable location");
        }
    }

    public NodePointer createPath(JXPathContext context) {
        NodePointer newParent = parent.createPath(context);
        if (isAttribute()) {
            return newParent.createAttribute(context, getName());
        }
        else {
            // Consider these two use cases:
            // 1. The parent pointer of NullPropertyPointer is 
            //    a PropertyOwnerPointer other than NullPointer. When we call 
            //    createPath on it, it most likely returns itself. We then
            //    take a PropertyPointer from it and get the PropertyPointer
            //    to expand the collection for the corresponding property.
            //
            // 2. The parent pointer of NullPropertyPointer is a NullPointer.
            //    When we call createPath, it may return a PropertyOwnerPointer
            //    or it may return anything else, like a DOMNodePointer.
            //    In the former case we need to do exactly what we did in use 
            //    case 1.  In the latter case, we simply request that the 
            //    non-property pointer expand the collection by itself.
            if (newParent instanceof PropertyOwnerPointer) {
                PropertyOwnerPointer pop = (PropertyOwnerPointer) newParent;
                newParent = pop.getPropertyPointer();
            }
            return newParent.createChild(context, getName(), getIndex());
        }
    }

    public NodePointer createPath(JXPathContext context, Object value) {
        NodePointer newParent = parent.createPath(context);
        if (isAttribute()) {
            NodePointer pointer = newParent.createAttribute(context, getName());
            pointer.setValue(value);
            return pointer;
        }
        else {
            if (newParent instanceof PropertyOwnerPointer) {
                PropertyOwnerPointer pop = (PropertyOwnerPointer) newParent;
                newParent = pop.getPropertyPointer();
            }
            return newParent.createChild(context, getName(), index, value);
        }
    }
    
    public NodePointer createChild(
            JXPathContext context,
            QName name, 
            int index)
    {
        return createPath(context).createChild(context, name, index);
    }
        
    public NodePointer createChild(
            JXPathContext context,
            QName name, 
            int index,
            Object value) 
    {
        return createPath(context).createChild(context, name, index, value);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setNameAttributeValue(String attributeValue) {
        this.propertyName = attributeValue;
        byNameAttribute = true;
    }

    public boolean isCollection() {
        return getIndex() != WHOLE_COLLECTION;
    }

    public int getPropertyCount() {
        return 0;
    }

    public String[] getPropertyNames() {
        return new String[0];
    }

    public String asPath() {
        if (!byNameAttribute) {
            return super.asPath();
        }
        else {
            StringBuffer buffer = new StringBuffer();
            buffer.append(getImmediateParentPointer().asPath());
            buffer.append("[@name='");
            buffer.append(escape(getPropertyName()));
            buffer.append("']");
            if (index != WHOLE_COLLECTION) {
                buffer.append('[').append(index + 1).append(']');
            }
            return buffer.toString();
        }
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
}
