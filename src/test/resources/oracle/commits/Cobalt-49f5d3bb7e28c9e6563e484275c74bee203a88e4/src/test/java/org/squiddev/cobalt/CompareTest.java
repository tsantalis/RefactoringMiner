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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


public class CompareTest {
	/**
	 * Test argument type check errors
	 *
	 * Results are compared for exact match with
	 * the installed C-based lua environment.
	 */
	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		"baselibargs", "coroutinelibargs", "iolibargs", "mathlibargs", "modulelibargs", "operators", "stringlibargs", "tablelibargs",
	})
	public void errors(String name) throws Exception {
		ScriptHelper helpers = new ScriptHelper("/compare/errors/");
		helpers.setup();
		helpers.runComparisonTest(name);
	}

	/**
	 * Compatibility tests for the LuaJ VM
	 *
	 * Results are compared for exact match with
	 * the installed C-based lua environment.
	 */
	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)
	@ValueSource(strings = {
		"baselib", "coroutinelib", "debuglib", "errors", "iolib", "format", "functions", "manyupvals", "mathlib",
		"metatags", "oslib", "stringlib", "tablelib", "tailcalls", "traceback", "upvalues", "vm",
	})
	public void libs(String name) throws Exception {
		ScriptHelper helpers = new ScriptHelper("/compare/");
		helpers.setup();
		helpers.runComparisonTest(name);
	}
}
