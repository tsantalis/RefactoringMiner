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
package org.squiddev.cobalt.compiler;

import org.junit.Before;
import org.junit.Test;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;

public class SimpleTests {
	private LuaState state;
	private LuaTable _G;

	@Before
	public void setup() {
		state = new LuaState();
		_G = JsePlatform.standardGlobals(state);
	}

	private void doTest(String script) throws UnwindThrowable {
		try {
			InputStream is = new ByteArrayInputStream(script.getBytes("UTF8"));
			LuaFunction c = LuaC.INSTANCE.load(is, valueOf("script"), _G);
			LuaThread.runMain(state, c);
		} catch (Exception e) {
			fail("i/o exception: " + e);
		}
	}

	@Test
	public void testTrivial() throws UnwindThrowable {
		String s = "print( 2 )\n";
		doTest(s);
	}

	@Test
	public void testAlmostTrivial() throws UnwindThrowable {
		String s = "print( 2 )\n" +
			"print( 3 )\n";
		doTest(s);
	}

	@Test
	public void testSimple() throws UnwindThrowable {
		String s = "print( 'hello, world' )\n" +
			"for i = 2,4 do\n" +
			"	print( 'i', i )\n" +
			"end\n";
		doTest(s);
	}

	@Test
	public void testBreak() throws UnwindThrowable {
		String s = "a=1\n" +
			"while true do\n" +
			"  if a>10 then\n" +
			"     break\n" +
			"  end\n" +
			"  a=a+1\n" +
			"  print( a )\n" +
			"end\n";
		doTest(s);
	}

	@Test
	public void testShebang() throws UnwindThrowable {
		String s = "#!../lua\n" +
			"print( 2 )\n";
		doTest(s);
	}

	@Test
	public void testInlineTable() throws UnwindThrowable {
		String s = "A = {g=10}\n" +
			"print( A )\n";
		doTest(s);
	}

	@Test
	public void testEqualsAnd() throws UnwindThrowable {
		String s = "print( 1 == b and b )\n";
		doTest(s);
	}

	private static final int[] samehash = {0, 1, -1, 2, -2, 4, 8, 16, 32, Integer.MAX_VALUE, Integer.MIN_VALUE};
	private static final double[] diffhash = {.5, 1, 1.5, 1, .5, 1.5, 1.25, 2.5};

	@Test
	public void testDoubleHashCode() {
		for (int aSamehash : samehash) {
			LuaValue j = LuaInteger.valueOf(aSamehash);
			LuaValue d = valueOf(aSamehash);
			int hj = j.hashCode();
			int hd = d.hashCode();
			assertEquals(hj, hd);
		}
		for (int i = 0; i < diffhash.length; i += 2) {
			LuaValue c = valueOf(diffhash[i]);
			LuaValue d = valueOf(diffhash[i + 1]);
			int hc = c.hashCode();
			int hd = d.hashCode();
			assertTrue("hash codes are same: " + hc, hc != hd);
		}
	}
}
