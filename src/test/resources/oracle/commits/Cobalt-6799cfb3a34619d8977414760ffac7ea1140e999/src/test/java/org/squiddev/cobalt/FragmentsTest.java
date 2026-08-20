/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Test compilation of various fragments that have
 * caused problems for LuaJC compiling during development.
 */
public class FragmentsTest {
	@Rule
	public TestName name = new TestName();


	public void runFragment(Varargs expected, String script) {
		LuaState state = new LuaState();
		try {
			String name = this.name.getMethodName();
			LuaTable _G = JsePlatform.standardGlobals(state);
			InputStream is = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
			LuaFunction chunk = LuaC.INSTANCE.load(is, valueOf(name), _G);
			Varargs actual = LuaThread.runMain(state, chunk);
			assertEquals(expected.count(), actual.count());
			for (int i = 1; i <= actual.count(); i++) {
				assertEquals(expected.arg(i), actual.arg(i));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void testForloopParamUpvalues() {
		runFragment(varargsOf(new LuaValue[]{
				valueOf(77),
				valueOf(1)}),
			"for n,p in ipairs({77}) do\n" +
				"	print('n,p',n,p)\n" +
				"	foo = function()\n" +
				"		return p,n\n" +
				"	end\n" +
				"	return foo()\n" +
				"end\n");

	}

	@Test
	public void testVarVarargsUseArg() {
		runFragment(varargsOf(valueOf("a"),
			valueOf(2),
			valueOf("b"),
			valueOf("c"),
			NIL),
			"function q(a,...)\n" +
				"	return a,arg.n,arg[1],arg[2],arg[3]\n" +
				"end\n" +
				"return q('a','b','c')\n");
	}

	@Test
	public void testVarVarargsUseBoth() {
		runFragment(varargsOf(valueOf("a"),
			valueOf("nil"),
			valueOf("b"),
			valueOf("c")),
			"function r(a,...)\n" +
				"	return a,type(arg),...\n" +
				"end\n" +
				"return r('a','b','c')\n");
	}

	@Test
	public void testArgVarargsUseBoth() {
		runFragment(varargsOf(new LuaValue[]{
				NIL,
				valueOf("b"),
				valueOf("c")}),
			"function v(arg,...)\n" +
				"	return arg,...\n" +
				"end\n" +
				"return v('a','b','c')\n");
	}

	@Test
	public void testArgParamUseNone() {
		// the name "arg" is treated specially, and ends up masking the argument value in 5.1
		runFragment(valueOf("table"),
			"function v(arg,...)\n" +
				"	return type(arg)\n" +
				"end\n" +
				"return v('abc')\n");
	}

	@Test
	public void testSetlistVarargs() {
		runFragment(valueOf("abc"),
			"local f = function() return 'abc' end\n" +
				"local g = { f() }\n" +
				"return g[1]\n");
	}

	@Test
	public void testSelfOp() {
		runFragment(valueOf("bcd"),
			"local s = 'abcde'\n" +
				"return s:sub(2,4)\n");
	}

	@Test
	public void testSetListWithOffsetAndVarargs() {
		runFragment(valueOf(1003),
			"local bar = {1000, math.sqrt(9)}\n" +
				"return bar[1]+bar[2]\n");
	}

	@Test
	public void testMultiAssign() {
		// arargs evaluations are all done before assignments
		runFragment(varargsOf(new LuaValue[]{
				valueOf(111),
				valueOf(111),
				valueOf(111)}),
			"a,b,c = 1,10,100\n" +
				"a,b,c = a+b+c, a+b+c, a+b+c\n" +
				"return a,b,c\n");
	}

	@Test
	public void testUpvalues() {
		runFragment(valueOf(999),
			"local a = function(x)\n" +
				"  return function(y)\n" +
				"    return x + y\n" +
				"  end\n" +
				"end\n" +
				"local b = a(222)\n" +
				"local c = b(777)\n" +
				"print( 'c=', c )\n" +
				"return c\n");
	}

	@Test
	public void testNeedsArgAndHasArg() {
		runFragment(varargsOf(valueOf(333), NIL, valueOf(222)),
			"function r(q,...)\n" +
				"	local a=arg\n" +
				"	return a and a[2]\n" +
				"end\n" +
				"function s(q,...)\n" +
				"	local a=arg\n" +
				"	local b=...\n" +
				"	return a and a[2],b\n" +
				"end\n" +
				"return r(111,222,333),s(111,222,333)");

	}

	@Test
	public void testNonAsciiStringLiterals() {
		runFragment(valueOf("7,8,12,10,9,11,133,222"),
			"local a='\\a\\b\\f\\n\\t\\v\\133\\222'\n" +
				"local t={string.byte(a,1,#a)}\n" +
				"return table.concat(t,',')\n");
	}

	@Test
	public void testControlCharStringLiterals() {
		runFragment(valueOf("97,0,98,18,99,18,100,18,48,101"),
			"local a='a\\0b\\18c\\018d\\0180e'\n" +
				"local t={string.byte(a,1,#a)}\n" +
				"return table.concat(t,',')\n");
	}

	@Test
	public void testLoopVarNames() {
		runFragment(valueOf(" 234,1,aa 234,2,bb"),
			"local w = ''\n" +
				"function t()\n" +
				"	for f,var in ipairs({'aa','bb'}) do\n" +
				"		local s = 234\n" +
				"		w = w..' '..s..','..f..','..var\n" +
				"	end\n" +
				"end\n" +
				"t()\n" +
				"return w\n");

	}

	@Test
	public void testForLoops() {
		runFragment(valueOf("12345 357 963"),
			"local s,t,u = '','',''\n" +
				"for m=1,5 do\n" +
				"	s = s..m\n" +
				"end\n" +
				"for m=3,7,2 do\n" +
				"	t = t..m\n" +
				"end\n" +
				"for m=9,3,-3 do\n" +
				"	u = u..m\n" +
				"end\n" +
				"return s..' '..t..' '..u\n");
	}

	@Test
	public void testLocalFunctionDeclarations() {
		runFragment(varargsOf(valueOf("function"), valueOf("nil")),
			"local function aaa()\n" +
				"	return type(aaa)\n" +
				"end\n" +
				"local bbb = function()\n" +
				"	return type(bbb)\n" +
				"end\n" +
				"return aaa(),bbb()\n");
	}

	@Test
	public void testNilsInTableConstructor() {
		runFragment(valueOf("1=111 2=222 3=333 "),
			"local t = { 111, 222, 333, nil, nil }\n" +
				"local s = ''\n" +
				"for i,v in ipairs(t) do \n" +
				"	s=s..tostring(i)..'='..tostring(v)..' '\n" +
				"end\n" +
				"return s\n");

	}

	@Test
	public void testUnreachableCode() {
		runFragment(valueOf(66),
			"local function foo(x) return x * 2 end\n" +
				"local function bar(x, y)\n" +
				"	if x==y then\n" +
				"		return y\n" +
				"	else\n" +
				"		return foo(x)\n" +
				"	end\n" +
				"end\n" +
				"return bar(33,44)\n");

	}

	@Test
	public void testVarargsWithParameters() {
		runFragment(valueOf(222),
			"local func = function(t,...)\n" +
				"	return (...)\n" +
				"end\n" +
				"return func(111,222,333)\n");
	}

	@Test
	public void testNoReturnValuesPlainCall() {
		runFragment(TRUE,
			"local testtable = {}\n" +
				"return pcall( function() testtable[1]=2 end )\n");
	}

	@Test
	public void testVarargsInTableConstructor() {
		runFragment(valueOf(222),
			"local function foo() return 111,222,333 end\n" +
				"local t = {'a','b',c='c',foo()}\n" +
				"return t[4]\n");
	}

	@Test
	public void testVarargsInFirstArg() {
		runFragment(valueOf(123),
			"function aaa(x) return x end\n" +
				"function bbb(y) return y end\n" +
				"function ccc(z) return z end\n" +
				"return ccc( aaa(bbb(123)), aaa(456) )\n");
	}

	@Test
	public void testSetUpvalueTableInitializer() {
		runFragment(valueOf("b"),
			"local aliases = {a='b'}\n" +
				"local foo = function()\n" +
				"	return aliases\n" +
				"end\n" +
				"return foo().a\n");
	}

	@Test
	public void testLoadNilUpvalue() {
		runFragment(NIL,
			"tostring = function() end\n" +
				"local pc \n" +
				"local pcall = function(...)\n" +
				"	pc(...)\n" +
				"end\n" +
				"return NIL\n");
	}

	@Test
	public void testUpvalueClosure() {
		runFragment(NIL,
			"print()\n" +
				"local function f2() end\n" +
				"local function f3()\n" +
				"	return f3\n" +
				"end\n" +
				"return NIL\n");
	}

	@Test
	public void testUninitializedUpvalue() {
		runFragment(NIL,
			"local f\n" +
				"do\n" +
				"	function g()\n" +
				"		print(f())\n" +
				"	end\n" +
				"end\n" +
				"return NIL\n");
	}

	@Test
	public void testTestOpUpvalues() {
		runFragment(varargsOf(valueOf(1), valueOf(2), valueOf(3)),
			"print( nil and 'T' or 'F' )\n" +
				"local a,b,c = 1,2,3\n" +
				"function foo()\n" +
				"	return a,b,c\n" +
				"end\n" +
				"return foo()\n");
	}

	@Test
	public void testTestSimpleBinops() {
		runFragment(varargsOf(FALSE, FALSE, TRUE, TRUE, FALSE),
			"local a,b,c = 2,-2.5,0\n" +
				"return (a==c), (b==c), (a==a), (a>c), (b>0)\n");
	}

	@Test
	public void testNumericForUpvalues() {
		runFragment(valueOf(8),
			"for i = 3,4 do\n" +
				"	i = i + 5\n" +
				"	local a = function()\n" +
				"		return i\n" +
				"	end\n" +
				"	return a()\n" +
				"end\n");
	}

	@Test
	public void testNumericForUpvalues2() {
		runFragment(valueOf("222 222"),
			"local t = {}\n" +
				"local template = [[123 456]]\n" +
				"for i = 1,2 do\n" +
				"	t[i] = template:gsub('%d', function(s)\n" +
				"		return i\n" +
				"	end)\n" +
				"end\n" +
				"return t[2]\n");
	}

	@Test
	public void testReturnUpvalue() {
		runFragment(varargsOf(new LuaValue[]{ONE, valueOf(5),}),
			"local a = 1\n" +
				"local b\n" +
				"function c()\n" +
				"	b=5\n" +
				"	return a\n" +
				"end\n" +
				"return c(),b\n");
	}

	@Test
	public void testUninitializedAroundBranch() {
		runFragment(valueOf(333),
			"local state\n" +
				"if _G then\n" +
				"    state = 333\n" +
				"end\n" +
				"return state\n");
	}

	@Test
	public void testLoadedNilUpvalue() {
		runFragment(NIL,
			"local a = print()\n" +
				"local b = c and { d = e }\n" +
				"local f\n" +
				"local function g()\n" +
				"	return f\n" +
				"end\n" +
				"return g()\n");
	}

	@Test
	public void testUpvalueInFirstSlot() {
		runFragment(valueOf("foo"),
			"local p = {'foo'}\n" +
				"bar = function()\n" +
				"	return p \n" +
				"end\n" +
				"for i,key in ipairs(p) do\n" +
				"	print()\n" +
				"end\n" +
				"return bar()[1]");
	}

	@Test
	public void testReadOnlyAndReadWriteUpvalues() {
		runFragment(varargsOf(new LuaValue[]{valueOf(333), valueOf(222)}),
			"local a = 111\n" +
				"local b = 222\n" +
				"local c = function()\n" +
				"	a = a + b\n" +
				"	return a,b\n" +
				"end\n" +
				"return c()\n");
	}

	@Test
	public void testNestedUpvalues() {
		runFragment(varargsOf(new LuaValue[]{valueOf(5), valueOf(8), valueOf(9)}),
			"local x = 3\n" +
				"local y = 5\n" +
				"local function f()\n" +
				"   return y\n" +
				"end\n" +
				"local function g(x1, y1)\n" +
				"   x = x1\n" +
				"   y = y1\n" +
				"	return x,y\n" +
				"end\n" +
				"return f(), g(8,9)\n" +
				"\n");
	}

	@Test
	public void testLoadBool() {
		runFragment(NONE,
			"print( type(foo)=='string' )\n" +
				"local a,b\n" +
				"if print() then\n" +
				"	b = function()\n" +
				"		return a\n" +
				"	end\n" +
				"end\n");
	}

	@Test
	public void testBasicForLoop() {
		runFragment(valueOf(2),
			"local data\n" +
				"for i = 1, 2 do\n" +
				"     data = i\n" +
				"end\n" +
				"local bar = function()\n" +
				"	return data\n" +
				"end\n" +
				"return bar()\n");
	}

	@Test
	public void testGenericForMultipleValues() {
		runFragment(varargsOf(valueOf(3), valueOf(2), valueOf(1)),
			"local iter = function() return 1,2,3,4 end\n" +
				"local foo  = function() return iter,5 end\n" +
				"for a,b,c in foo() do\n" +
				"    return c,b,a\n" +
				"end\n");
	}

	@Test
	public void testPhiUpvalue() {
		runFragment(valueOf(6),
			"local a = foo or 0\n" +
				"local function b(c)\n" +
				"	if c > a then a = c end\n" +
				"	return a\n" +
				"end\n" +
				"b(6)\n" +
				"return a\n");
	}

	@Test
	public void testAssignReferUpvalues() {
		runFragment(valueOf(123),
			"local entity = 234\n" +
				"local function c()\n" +
				"    return entity\n" +
				"end\n" +
				"entity = (a == b) and 123\n" +
				"if entity then\n" +
				"    return entity\n" +
				"end\n");
	}

	@Test
	public void testSimpleRepeatUntil() {
		runFragment(valueOf(5),
			"local a\n" +
				"local w\n" +
				"repeat\n" +
				"	a = w\n" +
				"until not a\n" +
				"return 5\n");
	}

	@Test
	public void testLoopVarUpvalues() {
		runFragment(valueOf("b"),
			"local env = {}\n" +
				"for a,b in pairs(_G) do\n" +
				"	c = function()\n" +
				"		return b\n" +
				"	end\n" +
				"end\n" +
				"local e = env\n" +
				"local f = {a='b'}\n" +
				"for k,v in pairs(f) do\n" +
				"	return env[k] or v\n" +
				"end\n");
	}

	@Test
	public void testPhiVarUpvalue() {
		runFragment(valueOf(2),
			"local a = 1\n" +
				"local function b()\n" +
				"    a = a + 1\n" +
				"    return function() end\n" +
				"end\n" +
				"for i in b() do\n" +
				"	a = 3\n" +
				"end\n" +
				"return a\n");
	}

	@Test
	public void testUpvaluesInElseClauses() {
		runFragment(valueOf(111),
			"if a then\n" +
				"   foo(bar)\n" +
				"elseif _G then\n" +
				"    local x = 111\n" +
				"    if d then\n" +
				"        foo(bar)\n" +
				"    else\n" +
				"    	local y = function()\n" +
				"    		return x\n" +
				"        end\n" +
				"    	return y()\n" +
				"    end\n" +
				"end\n");
	}
}
