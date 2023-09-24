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
package org.apache.commons.jxpath.ri.compiler;

import org.apache.commons.jxpath.Pointer;
import org.apache.commons.jxpath.ri.EvalContext;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.util.ValueUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;

/**
 * Common superclass for several types of nodes in the parse tree. Provides
 * APIs for optimization of evaluation of expressions.  Specifically, an
 * expression only needs to executed once during the evaluation of an xpath
 * if that expression is context-independent.  Expression.isContextDependent()
 * provides that hint.
 *
 * @author Dmitri Plotnikov
 * @version $Revision$ $Date$
 */
public abstract class Expression {

    protected static final Double ZERO = new Double(0);
    protected static final Double ONE = new Double(1);
    protected static final Double NOT_A_NUMBER = new Double(Double.NaN);

    private boolean contextDependencyKnown = false;
    private boolean contextDependent;

    /**
     * Returns true if this expression should be re-evaluated
     * each time the current position in the context changes.
     */
    public boolean isContextDependent() {
        if (!contextDependencyKnown) {
            contextDependent = computeContextDependent();
            contextDependencyKnown = true;
        }
        return contextDependent;
    }

    /**
     * Implemented by subclasses and result is cached by isContextDependent()
     */
    public abstract boolean computeContextDependent();

    /**
     * Evaluates the expression. If the result is a node set, returns
     * the first element of the node set.
     */
    public abstract Object computeValue(EvalContext context);
    public abstract Object compute(EvalContext context);

    public Iterator iterate(EvalContext context) {
        Object result = compute(context);
        if (result instanceof EvalContext) {
            return new ValueIterator((EvalContext) result);
        }
        return ValueUtils.iterate(result);
    }

    public Iterator iteratePointers(EvalContext context) {
        Object result = compute(context);
        if (result == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        if (result instanceof EvalContext) {
            return (EvalContext) result;
        }
        return new PointerIterator(ValueUtils.iterate(result),
                new QName(null, "value"),
                context.getRootContext().getCurrentNodePointer().getLocale());
    }

    public static class PointerIterator implements Iterator {
        private Iterator iterator;
        private QName qname;
        private Locale locale;

        //to what method does the following comment refer?
        /**
         * @deprecated Use the method that takes a NamespaceManager
         */
        public PointerIterator(Iterator it, QName qname, Locale locale) {
            this.iterator = it;
            this.qname = qname;
            this.locale = locale;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Object next() {
            Object o = iterator.next();
            return o instanceof Pointer ? o : NodePointer.newNodePointer(qname, o, locale);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class ValueIterator implements Iterator {
        private Iterator iterator;

        public ValueIterator(Iterator it) {
            this.iterator = it;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Object next() {
            Object o = iterator.next();
            if (o instanceof Pointer) {
                return ((Pointer) o).getValue();
            }
            return o;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
