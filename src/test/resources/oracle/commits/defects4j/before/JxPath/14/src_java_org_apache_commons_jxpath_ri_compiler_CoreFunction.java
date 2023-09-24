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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

import org.apache.commons.jxpath.BasicNodeSet;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;
import org.apache.commons.jxpath.JXPathInvalidSyntaxException;
import org.apache.commons.jxpath.NodeSet;
import org.apache.commons.jxpath.ri.Compiler;
import org.apache.commons.jxpath.ri.EvalContext;
import org.apache.commons.jxpath.ri.InfoSetUtil;
import org.apache.commons.jxpath.ri.axes.NodeSetContext;
import org.apache.commons.jxpath.ri.model.NodePointer;

/**
 * An element of the compile tree representing one of built-in functions
 * like "position()" or "number()".
 *
 * @author Dmitri Plotnikov
 * @version $Revision$ $Date$
 */
public class CoreFunction extends Operation {

    private static final Double ZERO = new Double(0);
    private int functionCode;

    public CoreFunction(int functionCode, Expression args[]) {
        super(args);
        this.functionCode = functionCode;
    }

    public int getFunctionCode() {
        return functionCode;
    }
    
    protected String getFunctionName() {
        switch (functionCode) {
            case Compiler.FUNCTION_LAST :
                return "last";
            case Compiler.FUNCTION_POSITION :
                return "position";
            case Compiler.FUNCTION_COUNT :
                return "count";
            case Compiler.FUNCTION_ID :
                return "id";
            case Compiler.FUNCTION_LOCAL_NAME :
                return "local-name";
            case Compiler.FUNCTION_NAMESPACE_URI :
                return "namespace-uri";
            case Compiler.FUNCTION_NAME :
                return "name";
            case Compiler.FUNCTION_STRING :
                return "string";
            case Compiler.FUNCTION_CONCAT :
                return "concat";
            case Compiler.FUNCTION_STARTS_WITH :
                return "starts-with";
            case Compiler.FUNCTION_CONTAINS :
                return "contains";
            case Compiler.FUNCTION_SUBSTRING_BEFORE :
                return "substring-before";
            case Compiler.FUNCTION_SUBSTRING_AFTER :
                return "substring-after";
            case Compiler.FUNCTION_SUBSTRING :
                return "substring";
            case Compiler.FUNCTION_STRING_LENGTH :
                return "string-length";
            case Compiler.FUNCTION_NORMALIZE_SPACE :
                return "normalize-space";
            case Compiler.FUNCTION_TRANSLATE :
                return "translate";
            case Compiler.FUNCTION_BOOLEAN :
                return "boolean";
            case Compiler.FUNCTION_NOT :
                return "not";
            case Compiler.FUNCTION_TRUE :
                return "true";
            case Compiler.FUNCTION_FALSE :
                return "false";
            case Compiler.FUNCTION_LANG :
                return "lang";
            case Compiler.FUNCTION_NUMBER :
                return "number";
            case Compiler.FUNCTION_SUM :
                return "sum";
            case Compiler.FUNCTION_FLOOR :
                return "floor";
            case Compiler.FUNCTION_CEILING :
                return "ceiling";
            case Compiler.FUNCTION_ROUND :
                return "round";
            case Compiler.FUNCTION_KEY :
                return "key";
            case Compiler.FUNCTION_FORMAT_NUMBER:
                return "format-number";
        }
        return "unknownFunction" + functionCode + "()";
    }

    public Expression getArg1() {
        return args[0];
    }

    public Expression getArg2() {
        return args[1];
    }

    public Expression getArg3() {
        return args[2];
    }

    public int getArgumentCount() {
        if (args == null) {
            return 0;
        }
        return args.length;
    }

    /**
     * Returns true if any argument is context dependent or if
     * the function is last(), position(), boolean(), local-name(),
     * name(), string(), lang(), number().
     */
    public boolean computeContextDependent() {
        if (super.computeContextDependent()) {
            return true;
        }

        switch(functionCode) {
            case Compiler.FUNCTION_LAST:
            case Compiler.FUNCTION_POSITION:
                return true;

            case Compiler.FUNCTION_BOOLEAN:
            case Compiler.FUNCTION_LOCAL_NAME:
            case Compiler.FUNCTION_NAME:
            case Compiler.FUNCTION_NAMESPACE_URI:
            case Compiler.FUNCTION_STRING:
            case Compiler.FUNCTION_LANG:
            case Compiler.FUNCTION_NUMBER:
                return args == null || args.length == 0;

            case Compiler.FUNCTION_COUNT:
            case Compiler.FUNCTION_ID:
            case Compiler.FUNCTION_CONCAT:
            case Compiler.FUNCTION_STARTS_WITH:
            case Compiler.FUNCTION_CONTAINS:
            case Compiler.FUNCTION_SUBSTRING_BEFORE:
            case Compiler.FUNCTION_SUBSTRING_AFTER:
            case Compiler.FUNCTION_SUBSTRING:
            case Compiler.FUNCTION_STRING_LENGTH:
            case Compiler.FUNCTION_NORMALIZE_SPACE:
            case Compiler.FUNCTION_TRANSLATE:
            case Compiler.FUNCTION_NOT:
            case Compiler.FUNCTION_TRUE:
            case Compiler.FUNCTION_FALSE:
            case Compiler.FUNCTION_SUM:
            case Compiler.FUNCTION_FLOOR:
            case Compiler.FUNCTION_CEILING:
            case Compiler.FUNCTION_ROUND:
                return false;
                
            case Compiler.FUNCTION_FORMAT_NUMBER:
                return args != null && args.length == 2;                             
        }

        return false;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(getFunctionName());
        buffer.append('(');
        Expression args[] = getArguments();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    buffer.append(", ");
                }
                buffer.append(args[i]);
            }
        }
        buffer.append(')');
        return buffer.toString();
    }

    public Object compute(EvalContext context) {
        return computeValue(context);
    }

    /**
     * Computes a built-in function
     */
    public Object computeValue(EvalContext context) {
        switch (functionCode) {
            case Compiler.FUNCTION_LAST :
                return functionLast(context);
            case Compiler.FUNCTION_POSITION :
                return functionPosition(context);
            case Compiler.FUNCTION_COUNT :
                return functionCount(context);
            case Compiler.FUNCTION_LANG :
                return functionLang(context);
            case Compiler.FUNCTION_ID :
                return functionID(context);
            case Compiler.FUNCTION_LOCAL_NAME :
                return functionLocalName(context);
            case Compiler.FUNCTION_NAMESPACE_URI :
                return functionNamespaceURI(context);
            case Compiler.FUNCTION_NAME :
                return functionName(context);
            case Compiler.FUNCTION_STRING :
                return functionString(context);
            case Compiler.FUNCTION_CONCAT :
                return functionConcat(context);
            case Compiler.FUNCTION_STARTS_WITH :
                return functionStartsWith(context);
            case Compiler.FUNCTION_CONTAINS :
                return functionContains(context);
            case Compiler.FUNCTION_SUBSTRING_BEFORE :
                return functionSubstringBefore(context);
            case Compiler.FUNCTION_SUBSTRING_AFTER :
                return functionSubstringAfter(context);
            case Compiler.FUNCTION_SUBSTRING :
                return functionSubstring(context);
            case Compiler.FUNCTION_STRING_LENGTH :
                return functionStringLength(context);
            case Compiler.FUNCTION_NORMALIZE_SPACE :
                return functionNormalizeSpace(context);
            case Compiler.FUNCTION_TRANSLATE :
                return functionTranslate(context);
            case Compiler.FUNCTION_BOOLEAN :
                return functionBoolean(context);
            case Compiler.FUNCTION_NOT :
                return functionNot(context);
            case Compiler.FUNCTION_TRUE :
                return functionTrue(context);
            case Compiler.FUNCTION_FALSE :
                return functionFalse(context);
            case Compiler.FUNCTION_NULL :
                return functionNull(context);
            case Compiler.FUNCTION_NUMBER :
                return functionNumber(context);
            case Compiler.FUNCTION_SUM :
                return functionSum(context);
            case Compiler.FUNCTION_FLOOR :
                return functionFloor(context);
            case Compiler.FUNCTION_CEILING :
                return functionCeiling(context);
            case Compiler.FUNCTION_ROUND :
                return functionRound(context);
            case Compiler.FUNCTION_KEY :
                return functionKey(context);
            case Compiler.FUNCTION_FORMAT_NUMBER :
                return functionFormatNumber(context);
        }
        return null;
    }

    protected Object functionLast(EvalContext context) {
        assertArgCount(0);
        // Move the position to the beginning and iterate through
        // the context to count nodes.
        int old = context.getCurrentPosition();
        context.reset();
        int count = 0;
        while (context.nextNode()) {
            count++;
        }

        // Restore the current position.
        if (old != 0) {
            context.setPosition(old);
        }
        return new Double(count);
    }

    protected Object functionPosition(EvalContext context) {
        assertArgCount(0);
        return new Integer(context.getCurrentPosition());
    }

    protected Object functionCount(EvalContext context) {
        assertArgCount(1);
        Expression arg1 = getArg1();
        int count = 0;
        Object value = arg1.compute(context);
        if (value instanceof NodePointer) {
            value = ((NodePointer) value).getValue();
        }
        if (value instanceof EvalContext) {
            EvalContext ctx = (EvalContext) value;
            while (ctx.hasNext()) {
                ctx.next();
                count++;
            }
        }
        else if (value instanceof Collection) {
            count = ((Collection) value).size();
        }
        else if (value == null) {
            count = 0;
        }
        else {
            count = 1;
        }
        return new Double(count);
    }

    protected Object functionLang(EvalContext context) {
        assertArgCount(1);
        String lang = InfoSetUtil.stringValue(getArg1().computeValue(context));
        NodePointer pointer = (NodePointer) context.getSingleNodePointer();
        if (pointer == null) {
            return Boolean.FALSE;
        }
        return pointer.isLanguage(lang) ? Boolean.TRUE : Boolean.FALSE;
    }

    protected Object functionID(EvalContext context) {
        assertArgCount(1);
        String id = InfoSetUtil.stringValue(getArg1().computeValue(context));
        JXPathContext jxpathContext = context.getJXPathContext();
        NodePointer pointer = (NodePointer) jxpathContext.getContextPointer();
        return pointer.getPointerByID(jxpathContext, id);
    }

    protected Object functionKey(EvalContext context) {
        assertArgCount(2);
        String key = InfoSetUtil.stringValue(getArg1().computeValue(context));
        Object value = getArg2().compute(context);
        EvalContext ec = null;
        if (value instanceof EvalContext) {
            ec = (EvalContext) value;
            if (ec.hasNext()) {
                value = ((NodePointer) ec.next()).getValue();
            } else { // empty context -> empty results
                return new BasicNodeSet();
            }
        }
        JXPathContext jxpathContext = context.getJXPathContext();
        NodeSet nodeSet = jxpathContext.getNodeSetByKey(key, value);
        if (ec != null && ec.hasNext()) {
            BasicNodeSet accum = new BasicNodeSet();
            accum.add(nodeSet);
            while (ec.hasNext()) {
                value = ((NodePointer) ec.next()).getValue();
                accum.add(jxpathContext.getNodeSetByKey(key, value));
            }
            nodeSet = accum;
        }
        return new NodeSetContext(context, nodeSet);
    }

    protected Object functionNamespaceURI(EvalContext context) {
        if (getArgumentCount() == 0) {
            NodePointer ptr = context.getCurrentNodePointer();
            String str = ptr.getNamespaceURI();
            return str == null ? "" : str;
        }
        assertArgCount(1);
        Object set = getArg1().compute(context);
        if (set instanceof EvalContext) {
            EvalContext ctx = (EvalContext) set;
            if (ctx.hasNext()) {
                NodePointer ptr = (NodePointer) ctx.next();
                String str = ptr.getNamespaceURI();
                return str == null ? "" : str;
            }
        }
        return "";
    }

    protected Object functionLocalName(EvalContext context) {
        if (getArgumentCount() == 0) {
            NodePointer ptr = context.getCurrentNodePointer();
            return ptr.getName().getName();
        }
        assertArgCount(1);
        Object set = getArg1().compute(context);
        if (set instanceof EvalContext) {
            EvalContext ctx = (EvalContext) set;
            if (ctx.hasNext()) {
                NodePointer ptr = (NodePointer) ctx.next();
                return ptr.getName().getName();
            }
        }
        return "";
    }

    protected Object functionName(EvalContext context) {
        if (getArgumentCount() == 0) {
            NodePointer ptr = context.getCurrentNodePointer();
            return ptr.getName().toString();
        }
        assertArgCount(1);
        Object set = getArg1().compute(context);
        if (set instanceof EvalContext) {
            EvalContext ctx = (EvalContext) set;
            if (ctx.hasNext()) {
                NodePointer ptr = (NodePointer) ctx.next();
                return ptr.getName().toString();
            }
        }
        return "";
    }

    protected Object functionString(EvalContext context) {
        if (getArgumentCount() == 0) {
            return InfoSetUtil.stringValue(context.getCurrentNodePointer());
        }
        assertArgCount(1);
        return InfoSetUtil.stringValue(getArg1().computeValue(context));
    }

    protected Object functionConcat(EvalContext context) {
        if (getArgumentCount() < 2) {
            assertArgCount(2);
        }
        StringBuffer buffer = new StringBuffer();
        Expression args[] = getArguments();
        for (int i = 0; i < args.length; i++) {
            buffer.append(InfoSetUtil.stringValue(args[i].compute(context)));
        }
        return buffer.toString();
    }

    protected Object functionStartsWith(EvalContext context) {
        assertArgCount(2);
        String s1 = InfoSetUtil.stringValue(getArg1().computeValue(context));
        String s2 = InfoSetUtil.stringValue(getArg2().computeValue(context));
        return s1.startsWith(s2) ? Boolean.TRUE : Boolean.FALSE;
    }

    protected Object functionContains(EvalContext context) {
        assertArgCount(2);
        String s1 = InfoSetUtil.stringValue(getArg1().computeValue(context));
        String s2 = InfoSetUtil.stringValue(getArg2().computeValue(context));
        return s1.indexOf(s2) != -1 ? Boolean.TRUE : Boolean.FALSE;
    }

    protected Object functionSubstringBefore(EvalContext context) {
        assertArgCount(2);
        String s1 = InfoSetUtil.stringValue(getArg1().computeValue(context));
        String s2 = InfoSetUtil.stringValue(getArg2().computeValue(context));
        int index = s1.indexOf(s2);
        if (index == -1) {
            return "";
        }
        return s1.substring(0, index);
    }

    protected Object functionSubstringAfter(EvalContext context) {
        assertArgCount(2);
        String s1 = InfoSetUtil.stringValue(getArg1().computeValue(context));
        String s2 = InfoSetUtil.stringValue(getArg2().computeValue(context));
        int index = s1.indexOf(s2);
        if (index == -1) {
            return "";
        }
        return s1.substring(index + s2.length());
    }

    protected Object functionSubstring(EvalContext context) {
        int ac = getArgumentCount();
        if (ac != 2 && ac != 3) {
            assertArgCount(2);
        }

        String s1 = InfoSetUtil.stringValue(getArg1().computeValue(context));
        double from = InfoSetUtil.doubleValue(getArg2().computeValue(context));
        if (Double.isNaN(from)) {
            return "";
        }

        from = Math.round(from);
        if (from > s1.length() + 1) {
            return "";
        }
        if (ac == 2) {
            if (from < 1) {
                from = 1;
            }
            return s1.substring((int) from - 1);
        }
        double length =
            InfoSetUtil.doubleValue(getArg3().computeValue(context));
        length = Math.round(length);
        if (length < 0) {
            return "";
        }

        double to = from + length;
        if (to < 1) {
            return "";
        }

        if (to > s1.length() + 1) {
            if (from < 1) {
                from = 1;
            }
            return s1.substring((int) from - 1);
        }

        if (from < 1) {
            from = 1;
        }
        return s1.substring((int) from - 1, (int) (to - 1));
    }

    protected Object functionStringLength(EvalContext context) {
        String s;
        if (getArgumentCount() == 0) {
            s = InfoSetUtil.stringValue(context.getCurrentNodePointer());
        }
        else {
            assertArgCount(1);
            s = InfoSetUtil.stringValue(getArg1().computeValue(context));
        }
        return new Double(s.length());
    }

    protected Object functionNormalizeSpace(EvalContext context) {
        assertArgCount(1);
        String s = InfoSetUtil.stringValue(getArg1().computeValue(context));
        char chars[] = s.toCharArray();
        int out = 0;
        int phase = 0;
        for (int in = 0; in < chars.length; in++) {
            switch(chars[in]) {
                case 0x20:
                case 0x9:
                case 0xD:
                case 0xA:
                    if (phase == 0) {      // beginning
                        ;
                    }
                    else if (phase == 1) { // non-space
                        phase = 2;
                        chars[out++] = ' ';
                    }
                    break;
                default:
                    chars[out++] = chars[in];
                    phase = 1;
            }
        }
        if (phase == 2) { // trailing-space
            out--;
        }
        return new String(chars, 0, out);
    }

    protected Object functionTranslate(EvalContext context) {
        assertArgCount(3);
        String s1 = InfoSetUtil.stringValue(getArg1().computeValue(context));
        String s2 = InfoSetUtil.stringValue(getArg2().computeValue(context));
        String s3 = InfoSetUtil.stringValue(getArg3().computeValue(context));
        char chars[] = s1.toCharArray();
        int out = 0;
        for (int in = 0; in < chars.length; in++) {
            char c = chars[in];
            int inx = s2.indexOf(c);
            if (inx != -1) {
                if (inx < s3.length()) {
                    chars[out++] = s3.charAt(inx);
                }
            }
            else {
                chars[out++] = c;
            }
        }
        return new String(chars, 0, out);
    }

    protected Object functionBoolean(EvalContext context) {
        assertArgCount(1);
        return InfoSetUtil.booleanValue(getArg1().computeValue(context))
            ? Boolean.TRUE
            : Boolean.FALSE;
    }

    protected Object functionNot(EvalContext context) {
        assertArgCount(1);
        return InfoSetUtil.booleanValue(getArg1().computeValue(context))
            ? Boolean.FALSE
            : Boolean.TRUE;
    }

    protected Object functionTrue(EvalContext context) {
        assertArgCount(0);
        return Boolean.TRUE;
    }

    protected Object functionFalse(EvalContext context) {
        assertArgCount(0);
        return Boolean.FALSE;
    }

    protected Object functionNull(EvalContext context) {
        assertArgCount(0);
        return null;
    }

    protected Object functionNumber(EvalContext context) {
        if (getArgumentCount() == 0) {
            return InfoSetUtil.number(context.getCurrentNodePointer());
        }
        assertArgCount(1);
        return InfoSetUtil.number(getArg1().computeValue(context));
    }

    protected Object functionSum(EvalContext context) {
        assertArgCount(1);
        Object v = getArg1().compute(context);
        if (v == null) {
            return ZERO;
        }
        if (v instanceof EvalContext) {
            double sum = 0.0;
            EvalContext ctx = (EvalContext) v;
            while (ctx.hasNext()) {
                NodePointer ptr = (NodePointer) ctx.next();
                sum += InfoSetUtil.doubleValue(ptr);
            }
            return new Double(sum);
        }
        throw new JXPathException(
            "Invalid argument type for 'sum': " + v.getClass().getName());
    }

    protected Object functionFloor(EvalContext context) {
        assertArgCount(1);
        double v = InfoSetUtil.doubleValue(getArg1().computeValue(context));
        return new Double(Math.floor(v));
    }

    protected Object functionCeiling(EvalContext context) {
        assertArgCount(1);
        double v = InfoSetUtil.doubleValue(getArg1().computeValue(context));
        return new Double(Math.ceil(v));
    }

    protected Object functionRound(EvalContext context) {
        assertArgCount(1);
        double v = InfoSetUtil.doubleValue(getArg1().computeValue(context));
        return new Double(Math.round(v));
    }

    private Object functionFormatNumber(EvalContext context) {
        int ac = getArgumentCount();
        if (ac != 2 && ac != 3) {
            assertArgCount(2);
        }

        double number =
            InfoSetUtil.doubleValue(getArg1().computeValue(context));
        String pattern =
            InfoSetUtil.stringValue(getArg2().computeValue(context));

        DecimalFormatSymbols symbols = null;
        if (ac == 3) {
            String symbolsName =
                InfoSetUtil.stringValue(getArg3().computeValue(context));
            symbols =
                context.getJXPathContext().getDecimalFormatSymbols(symbolsName);
        }
        else {
            NodePointer pointer = context.getCurrentNodePointer();
            Locale locale;
            if (pointer != null) {
                locale = pointer.getLocale();
            }
            else {
                locale = context.getJXPathContext().getLocale();
            }
            symbols = new DecimalFormatSymbols(locale);
        }
        
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance();
        format.setDecimalFormatSymbols(symbols);
        format.applyLocalizedPattern(pattern);
        return format.format(number);
    }

    private void assertArgCount(int count) {
        assertArgRange(count, count);
    }

    private void assertArgRange(int min, int max) {
        int ct = getArgumentCount();
        if (ct < min || ct > max) {
            throw new JXPathInvalidSyntaxException(
                    "Incorrect number of arguments: " + this);
        }
    }
}