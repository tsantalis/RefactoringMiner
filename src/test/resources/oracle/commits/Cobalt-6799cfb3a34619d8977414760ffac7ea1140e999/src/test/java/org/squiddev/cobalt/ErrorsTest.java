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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;


/**
 * Test argument type check errors
 *
 * Results are compared for exact match with
 * the installed C-based lua environment.
 */
@RunWith(Parameterized.class)
public class ErrorsTest {
	private ScriptDrivenHelpers helpers = new ScriptDrivenHelpers("/errors/");
	private String name;

	public ErrorsTest(String name) {
		this.name = name;
	}

	@Before
	public void setup() throws Exception {
		helpers.setup();
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		return Arrays.asList(new Object[][]{
			{"baselibargs"},
			{"coroutinelibargs"},
			{"iolibargs"},
			{"mathlibargs"},
			{"modulelibargs"},
			{"operators"},
			{"stringlibargs"},
			{"tablelibargs"},
		});
	}

	@Test
	public void testBaseLibArgs() throws Exception {
		helpers.state.stdin = new InputStream() {
			@Override
			public int read() throws IOException {
				return -1;
			}
		};

		helpers.runTest(name);
	}

}
