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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugHelpers;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.ResumableVarArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.squiddev.cobalt.OperationHelper.noUnwind;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKED;
import static org.squiddev.cobalt.debug.DebugFrame.FLAG_HOOKYIELD;

/**
 * Tests yielding in a whole load of places
 */
@Timeout(1)
public class CoroutineTest {
	private ScriptHelper helpers;

	public static String[] getTests() {
		return new String[]{
			"basic", "debug", "gsub", "ops", "pcall", "resume-boundary", "table", "tail", "yield-boundary", "xpcall",
		};
	}

	@BeforeEach
	public void setup() {
		helpers = new ScriptHelper("/coroutine/");
		helpers.setup();
		helpers.globals.load(helpers.state, new Functions());
	}

	private void setBlockingYield() {
		((LuaTable) helpers.globals.rawget("coroutine")).rawset("yield", new VarArgFunction() {
			@Override
			public Varargs invoke(LuaState state, Varargs args) throws LuaError {
				try {
					return LuaThread.yieldBlocking(state, args);
				} catch (InterruptedException e) {
					throw new InterruptedError(e);
				}
			}
		});
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@MethodSource("getTests")
	public void run(String name) throws IOException, CompileException, LuaError, InterruptedException {
		helpers.setup();
		setup();
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@MethodSource("getTests")
	public void runSuspend(String name) throws IOException, CompileException, LuaError, InterruptedException {
		helpers.setup(x -> x.debug(new SuspendingDebug()));
		setup();

		LuaFunction function = helpers.loadScript(name);
		Varargs result = LuaThread.runMain(helpers.state, function);
		while (result == null && !helpers.state.getMainThread().getStatus().equals("dead")) {
			result = LuaThread.run(helpers.state.getCurrentThread(), Constants.NONE);
		}

		assertEquals("dead", helpers.state.getMainThread().getStatus());
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@MethodSource("getTests")
	public void runBlocking(String name) throws IOException, CompileException, LuaError, InterruptedException {
		helpers.setup();
		setup();
		setBlockingYield();
		LuaThread.runMain(helpers.state, helpers.loadScript(name));
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@MethodSource("getTests")
	public void runSuspendBlocking(String name) throws IOException, CompileException, LuaError, InterruptedException {
		helpers.setup(x -> x.debug(new SuspendingDebug()));
		setup();
		setBlockingYield();

		LuaFunction function = helpers.loadScript(name);
		Varargs result = LuaThread.runMain(helpers.state, function);
		while (result == null && !helpers.state.getMainThread().getStatus().equals("dead")) {
			result = LuaThread.run(helpers.state.getCurrentThread(), Constants.NONE);
		}

		assertEquals("dead", helpers.state.getMainThread().getStatus());
	}

	private static class Functions extends ResumableVarArgFunction<LuaThread> implements LuaLibrary {
		@Override
		public LuaValue add(LuaState state, LuaTable environment) {
			bind(environment, Functions::new, new String[]{"suspend", "run", "assertEquals", "fail", "id", "noUnwind"});
			return environment;
		}

		@Override
		public Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
			switch (opcode) {
				case 0: // suspend
					LuaThread.suspend(state);
					return Constants.NONE;
				case 1: { // run
					LuaThread thread = new LuaThread(state, args.first().checkFunction(), getfenv());
					di.state = thread;
					Varargs value = Constants.NONE;
					while (thread.isAlive()) value = LuaThread.resume(state, thread, value);
					return value;
				}
				case 2: { // assertEquals
					String traceback = DebugHelpers.traceback(state.getCurrentThread(), 0);
					assertEquals(args.arg(1), args.arg(2), traceback);
					return Constants.NONE;
				}
				case 3: { // fail
					String traceback = DebugHelpers.traceback(state.getCurrentThread(), 0);
					fail(args.first().toString() + ":\n" + traceback);
					return Constants.NONE;
				}
				case 4: // id
					return args;
				case 5: // noYield
					return noUnwind(state, () -> args.first().checkFunction().call(state));
				default:
					return Constants.NONE;
			}
		}

		@Override
		public Varargs resumeThis(LuaState state, LuaThread thread, Varargs value) throws LuaError, UnwindThrowable {
			switch (opcode) {
				case 0:
					return Constants.NONE;
				case 1: // run
					while (thread.isAlive()) value = LuaThread.resume(state, thread, value);
					return value;
				default:
					throw new NonResumableException("Cannot resume " + debugName());
			}
		}
	}

	private static class SuspendingDebug extends DebugHandler {
		private boolean suspend = true;

		private int flags;
		private boolean inHook;

		@Override
		public void onInstruction(DebugState ds, DebugFrame di, int pc) throws LuaError, UnwindThrowable {
			di.pc = pc;

			if (suspend) {
				// Save the current state
				flags = di.flags;
				inHook = ds.inhook;

				// Set HOOK_YIELD and HOOKED flags so we know its an instruction hook
				di.flags |= FLAG_HOOKYIELD | FLAG_HOOKED;

				// We don't want to suspend next tick.
				suspend = false;
				LuaThread.suspend(ds.getLuaState());
			}

			// Restore the old state
			ds.inhook = inHook;
			di.flags = flags;
			suspend = true;

			// And continue as normal
			super.onInstruction(ds, di, pc);
		}
	}
}
