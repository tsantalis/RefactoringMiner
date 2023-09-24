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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.jxpath.ri.EvalContext;
import org.apache.commons.jxpath.ri.InfoSetUtil;
import org.apache.commons.jxpath.ri.axes.InitialContext;
import org.apache.commons.jxpath.ri.axes.SelfContext;

/**
 * Base implementation of Expression for the operations "&gt;", "&gt;=", "&lt;", "&lt;=".
 *
 * @author Matt Benson
 * @version $Revision:$ $Date:$
 */
public abstract class CoreOperationRelationalExpression extends CoreOperation {

    protected CoreOperationRelationalExpression(Expression[] args) {
        super(args);
    }

    public final Object computeValue(EvalContext context) {
        return compute(args[0].computeValue(context), args[1]
                .computeValue(context)) ? Boolean.TRUE : Boolean.FALSE;
    }

    protected final int getPrecedence() {
        return 3;
    }

    protected final boolean isSymmetric() {
        return false;
    }

    protected abstract boolean evaluateCompare(int compare);

    private boolean compute(Object left, Object right) {
        left = reduce(left);
        right = reduce(right);

        if (left instanceof InitialContext) {
            ((InitialContext) left).reset();
        }
        if (right instanceof InitialContext) {
            ((InitialContext) right).reset();
        }
        if (left instanceof Iterator && right instanceof Iterator) {
            return findMatch((Iterator) left, (Iterator) right);
        }
        if (left instanceof Iterator) {
            return containsMatch((Iterator) left, right);
        }
        if (right instanceof Iterator) {
            return containsMatch((Iterator) right, left);
        }
        return evaluateCompare(compare(left, right));
    }

    private Object reduce(Object o) {
        if (o instanceof SelfContext) {
            o = ((EvalContext) o).getSingleNodePointer();
        }
        if (o instanceof Collection) {
            o = ((Collection) o).iterator();
        }
        return o;
    }

    private boolean containsMatch(Iterator it, Object value) {
        while (it.hasNext()) {
            Object element = it.next();
            if (evaluateCompare(compare(element, value))) {
                return true;
            }
        }
        return false;
    }

    private boolean findMatch(Iterator lit, Iterator rit) {
        HashSet left = new HashSet();
        while (lit.hasNext()) {
            left.add(lit.next());
        }
        while (rit.hasNext()) {
            if (containsMatch(left.iterator(), rit.next())) {
                return true;
            }
        }
        return false;
    }

    private int compare(Object l, Object r) {
        double ld = InfoSetUtil.doubleValue(l);
        double rd = InfoSetUtil.doubleValue(r);
        return ld == rd ? 0 : ld < rd ? -1 : 1;
    }

}
