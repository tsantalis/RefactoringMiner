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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.OneArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.ByteArrayInputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.ValueFactory.valueOf;

public class OrphanedThreadTest {
	private static final Executor executor = Executors.newCachedThreadPool(command ->
		new Thread(command, "Coroutine"));

	private LuaState state;

	private LuaFunction function;
	private LuaTable env;

	private final AtomicInteger active = new AtomicInteger();

	@Before
	public void setup() {
		LuaThread.orphanCheckInterval = TimeUnit.MILLISECONDS.toNanos(5);
		state = new LuaState.Builder()
			// Our executor keeps track of how many threads we're using
			.coroutineExecutor(f -> executor.execute(() -> {
				active.incrementAndGet();
				try {
					f.run();
				} finally {
					active.decrementAndGet();
				}
			}))
			.build();

		// And force coroutine.yield to actually be a blocking one.
		env = JsePlatform.standardGlobals(state);
		((LuaTable) env.rawget("coroutine")).rawset("yield", new VarArgFunction() {
			@Override
			public Varargs invoke(LuaState state, Varargs args) throws LuaError {
				return OperationHelper.noUnwind(state, () -> LuaThread.yield(state, args));
			}
		});
	}

	@After
	public void tearDown() {
		LuaThread.orphanCheckInterval = TimeUnit.SECONDS.toNanos(30);
	}

	@Test
	public void testCollectOrphanedNormalThread() throws Exception {
		function = new NormalFunction();
		doTest(true, ZERO);
	}

	@Test
	public void testCollectOrphanedEarlyCompletionThread() throws Exception {
		function = new EarlyCompletionFunction();
		doTest(true, ZERO);
	}

	@Test
	public void testCollectOrphanedAbnormalThread() throws Exception {
		function = new AbnormalFunction();
		doTest(false, valueOf("abnormal condition"));
	}

	@Test
	public void testCollectOrphanedClosureThread() throws Exception {
		String script =
			"print('in closure, arg is '..(...))\n" +
				"arg = coroutine.yield(1)\n" +
				"print('in closure.2, arg is '..arg)\n" +
				"arg = coroutine.yield(0)\n" +
				"print('leakage in closure.3, arg is '..arg)\n" +
				"return 'done'\n";

		function = LoadState.load(state, new ByteArrayInputStream(script.getBytes()), "script", env);
		doTest(true, ZERO);
	}

	@Test
	public void testCollectOrphanedPcallClosureThread() throws Exception {
		String script =
			"f = function(x)\n" +
				"  print('in pcall-closure, arg is '..(x))\n" +
				"  arg = coroutine.yield(1)\n" +
				"  print('in pcall-closure.2, arg is '..arg)\n" +
				"  arg = coroutine.yield(0)\n" +
				"  print('leakage in pcall-closure.3, arg is '..arg)\n" +
				"  return 'done'\n" +
				"end\n" +
				"print( 'pcall-closre.result:', pcall( f, ... ) )\n";

		function = LoadState.load(state, new ByteArrayInputStream(script.getBytes()), "script", env);
		doTest(true, ZERO);
	}

	@Test
	public void testCollectOrphanedLoadClosureThread() throws Exception {
		String script =
			"t = { \"print \", \"'hello, \", \"world'\", }\n" +
				"i = 0\n" +
				"arg = ...\n" +
				"f = function()\n" +
				"	i = i + 1\n" +
				"   print('in load-closure, arg is', arg, 'next is', t[i])\n" +
				"   arg = coroutine.yield(1)\n" +
				"	return t[i]\n" +
				"end\n" +
				"load(f)()\n";

		function = LoadState.load(state, new ByteArrayInputStream(script.getBytes()), "script", env);
		doTest(true, ONE);
	}

	@Test
	public void testAbandon() throws Exception {
		String script =
			"local function foo(n)\n" +
				"\tif n == 0 then\n" +
				"\t\tcoroutine.yield()\n" +
				"\t\terror(\"Should never reach this point\")\n" +
				"\telse\n" +
				"\t\tlocal f = coroutine.create(function() foo(n - 1) end)\n" +
				"\t\twhile true do\n" +
				"\t\t\tcoroutine.resume(f)\n" +
				"\t\t\tcoroutine.yield()\n" +
				"\t\t\t\n" +
				"\t\t\tif coroutine.status(f) == \"dead\" then\n" +
				"\t\t\t\tbreak\n" +
				"\t\t\tend\n" +
				"\t\tend\n" +
				"\tend\n" +
				"end\n" +
				"\n" +
				"foo(5)";

		function = LoadState.load(state, new ByteArrayInputStream(script.getBytes()), "script", env);

		LuaThread thread = new LuaThread(state, function, env);

		LuaThread.run(thread, NONE);

		assertEquals("suspended", thread.getStatus());
		assertEquals("Should have some active threads", 6, active.get());

		state.abandon();

		for (int i = 0; i < 100 && active.get() > 0; i++) Thread.sleep(5);
		assertEquals("Should have no active threads", 0, active.get());
	}

	private void doTest(boolean secondOk, LuaValue secondValue) throws Exception {
		LuaThread thread = new LuaThread(state, function, env);
		WeakReference<LuaThread> luaThreadRef = new WeakReference<>(thread);
		WeakReference<LuaValue> luaFuncRef = new WeakReference<>(function);
		assertNotNull(luaThreadRef.get());

		// resume two times
		{
			Varargs result = LuaThread.run(thread, valueOf("foo"));
			assertEquals(ONE, result.first());
		}

		try {
			Varargs result = LuaThread.run(thread, valueOf("bar"));
			assertTrue("Expected to error, but succeeded", secondOk);
			assertEquals(secondValue, result);
		} catch (LuaError e) {
			assertFalse("Expected to succeed, but errored (" + e.getMessage() + ")", secondOk);
			assertEquals(secondValue, e.value);
		}

		// Drop strong references
		state.currentThread = thread = null;
		function = null;

		// gc
		final int count = 10000;
		for (int i = 0; i < count && (luaThreadRef.get() != null || luaFuncRef.get() != null); i++) {
			System.out.printf("Preparing to spin: %d/%d (%f%%)\n", i + 1, count, (1.0 + i) / count * 100);
			Runtime.getRuntime().gc();
			Thread.sleep(5);
		}

		// check reference
		assertNull("Thread should have been GCed:", luaThreadRef.get());
		assertNull("Function should have been GCed:", luaFuncRef.get());
	}


	private static class NormalFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
			try {
				System.out.println("in normal.1, arg is " + arg);
				arg = LuaThread.yieldBlocking(state, ONE).first();
				System.out.println("in normal.2, arg is " + arg);
				arg = LuaThread.yieldBlocking(state, ZERO).first();
				System.out.println("in normal.3, arg is " + arg);
				return NONE;
			} catch (InterruptedException e) {
				throw new InterruptedError(e);
			}
		}
	}

	private static class EarlyCompletionFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
			try {
				System.out.println("in early.1, arg is " + arg);
				arg = LuaThread.yieldBlocking(state, ONE).first();
				System.out.println("in early.2, arg is " + arg);
				return ZERO;
			} catch (InterruptedException e) {
				throw new InterruptedError(e);
			}
		}
	}

	private static class AbnormalFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaState state, LuaValue arg) throws LuaError {
			try {
				System.out.println("in abnormal.1, arg is " + arg);
				arg = LuaThread.yieldBlocking(state, ONE).first();
				System.out.println("in abnormal.2, arg is " + arg);
				throw new LuaError("abnormal condition");
			} catch (InterruptedException e) {
				throw new InterruptedError(e);
			}
		}
	}
}
