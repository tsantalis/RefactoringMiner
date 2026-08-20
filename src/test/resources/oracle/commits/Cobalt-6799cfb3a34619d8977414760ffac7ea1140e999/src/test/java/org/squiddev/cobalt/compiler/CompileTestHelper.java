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

import org.squiddev.cobalt.Print;
import org.squiddev.cobalt.Prototype;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.squiddev.cobalt.ValueFactory.valueOf;

public class CompileTestHelper {
	/**
	 * Compiled and compares the bytecode
	 *
	 * @param file The path of the file to use
	 * @throws IOException
	 */
	public static void compareResults(String dir, String file) throws IOException, CompileException {
		// Compile in memory
		String sourceBytecode = protoToString(LuaC.compile(new ByteArrayInputStream(bytesFromJar(dir + file + ".lua")), "@" + file + ".lua"));

		// Load expected value from jar
		Prototype expectedPrototype = loadFromBytes(bytesFromJar(dir + file + ".lc"), file + ".lua");
		String expectedBytecode = protoToString(expectedPrototype);

		// compare results
		assertEquals(expectedBytecode, sourceBytecode);

		// Redo bytecode
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		DumpState.dump(expectedPrototype, outputStream, false);
		String redumpBytecode = protoToString(loadFromBytes(outputStream.toByteArray(), file + ".lua"));

		// compare again
		assertEquals(sourceBytecode, redumpBytecode);
	}

	/**
	 * Read bytes from a resource
	 *
	 * @param path The path to the resource
	 * @return A byte array containing the file
	 */
	private static byte[] bytesFromJar(String path) throws IOException {
		InputStream is = CompileTestHelper.class.getResourceAsStream(path);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[2048];

		int n;
		while ((n = is.read(buffer)) >= 0) {
			outputStream.write(buffer, 0, n);
		}
		is.close();

		return outputStream.toByteArray();
	}

	/**
	 * Load a file from the bytes
	 *
	 * @param bytes  The bytes to load from
	 * @param script The name of the file to use
	 * @return A Prototype from the compiled bytecode
	 */
	private static Prototype loadFromBytes(byte[] bytes, String script) throws IOException, CompileException {
		InputStream is = new ByteArrayInputStream(bytes);
		return LoadState.loadBinaryChunk(is.read(), is, valueOf(script));
	}

	/**
	 * Pretty print a prototype as a String
	 *
	 * @param p The prototype to use
	 * @return String containing a pretty version of the prototype
	 */
	private static String protoToString(Prototype p) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Print.ps = new PrintStream(outputStream);
		Print.printFunction(p, true);
		return outputStream.toString();
	}
}
