/**
 * Copyright (C) 2010-2015 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser Public License as published by the
 * Free Software Foundation, either version 3.0 of the License, or (at your
 * option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License along
 * with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.symbolic.solver.cvc4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.evosuite.symbolic.solver.SolverTimeoutException;
import org.evosuite.symbolic.solver.TestSolverSimpleMath;
import org.junit.Test;

public class TestCVC4SimpleMath  extends TestCVC4{

	@Test
	public void testAdd() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testAdd(solver);
	}

	@Test
	public void testSub() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testSub(solver);
	}

	@Test
	public void testMul() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testMul(solver);
	}

	@Test
	public void testDiv() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		Map<String, Object> solution = TestSolverSimpleMath.testDiv(solver);
		
		assertNotNull(solution);
		Long var0 = (Long) solution.get("var0");
		Long var1 = (Long) solution.get("var1");

		assertEquals(var0.intValue(), var1.intValue() / 5);

	}

	@Test
	public void testEq() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testEq(solver);
	}

	@Test
	public void testNeq() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testNeq(solver);
	}

	@Test
	public void testLt() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testLt(solver);
	}

	@Test
	public void testGt() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testGt(solver);
	}

	@Test
	public void testLte() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testLte(solver);
	}

	@Test
	public void testGte() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testGte(solver);
	}

	@Test
	public void testMod() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testMod(solver);
	}

	@Test
	public void testMul2() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testMul2(solver);
	}

	@Test
	public void testCastRealToInt() throws SecurityException,
			NoSuchMethodException, SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testCastRealToInt(solver);
	}

	@Test
	public void testCastIntToReal() throws SecurityException,
			NoSuchMethodException, SolverTimeoutException {
		CVC4Solver solver = new CVC4Solver();
		TestSolverSimpleMath.testCastIntToReal(solver);
	}
}
