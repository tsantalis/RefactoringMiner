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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

import static org.squiddev.cobalt.ValueFactory.valueOf;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(jvmArgsAppend = {"-server", "-disablesystemassertions"})
public class PerformanceBenchmark {
	@State(Scope.Thread)
	public static class ScriptScope {
		final ScriptHelper helpers = new ScriptHelper("/perf/");

		@Setup(Level.Iteration)
		public void setup() {
			helpers.setupQuiet();
		}
	}

	@Benchmark
	public void binarytrees(ScriptScope scope) throws Exception {
		LuaThread.runMain(scope.helpers.state, scope.helpers.loadScript("binarytrees"), valueOf(10));
	}

	@Benchmark
	public void fannkuch(ScriptScope scope) throws Exception {
		LuaThread.runMain(scope.helpers.state, scope.helpers.loadScript("fannkuch"), valueOf(8));
	}

	@Benchmark
	public void nbody(ScriptScope scope) throws Exception {
		LuaThread.runMain(scope.helpers.state, scope.helpers.loadScript("nbody"), valueOf(50000));
	}

	@Benchmark
	public void nsieve(ScriptScope scope) throws Exception {
		LuaThread.runMain(scope.helpers.state, scope.helpers.loadScript("nsieve"), valueOf(8));
	}

	public static void main(String... args) throws RunnerException {
		Options opts = new OptionsBuilder()
			.include("org.squiddev.cobalt.PerformanceBenchmark.*")
			.warmupIterations(5)
			.measurementIterations(5)
			.measurementTime(TimeValue.milliseconds(12000))
			.jvmArgsPrepend("-server")
			.forks(3)
			.build();
		new Runner(opts).run();
	}
}
