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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.IOException;

/**
 * Compiles Lua's test files to bytecode and asserts that it is equal to a golden file produced by luac.
 */
public class CompilerUnitTests {
	@BeforeEach
	public void setup() {
		LuaState state = new LuaState();
		JsePlatform.standardGlobals(state);
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		"all", "api", "attrib", "big", "calls", "checktable", "closure", "code", "constructs", "db", "errors",
		"events", "files", "gc", "literals", "locals", "main", "math", "nextvar", "pm", "sort", "strings", "vararg",
	})
	public void lua51(String filename) throws IOException, CompileException {
		CompileTestHelper.compareResults("/bytecode-compiler/lua5.1/", filename);
	}

	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		"modulo", "construct", "bigattr", "controlchars", "comparators", "mathrandomseed", "varargs",
	})
	public void regression(String filename) throws IOException, CompileException {
		CompileTestHelper.compareResults("/bytecode-compiler/regressions/", filename);
	}
}
