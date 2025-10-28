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
package org.evosuite.symbolic.solver.z3;

import org.evosuite.symbolic.solver.SolverTimeoutException;
import org.evosuite.symbolic.solver.TestSolverBitwise;
import org.junit.Test;

public class TestZ3Bitwise extends TestZ3 {

	@Test
	public void testBitAnd() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		Z3Solver solver = new Z3Solver();
		TestSolverBitwise.testBitAnd(solver);
	}

	@Test
	public void testBitNot() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		Z3Solver solver = new Z3Solver();
		TestSolverBitwise.testBitNot(solver);
	}

	@Test
	public void testBitOr() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		Z3Solver solver = new Z3Solver();
		TestSolverBitwise.testBitOr(solver);
	}

	@Test
	public void testBitXor() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		Z3Solver solver = new Z3Solver();
		TestSolverBitwise.testBitXor(solver);
	}

	@Test
	public void testShiftLeft() throws SecurityException,
			NoSuchMethodException, SolverTimeoutException {
		Z3Solver solver = new Z3Solver();
		TestSolverBitwise.testShiftLeft(solver);
	}

	@Test
	public void testShiftRight() throws SecurityException,
			NoSuchMethodException, SolverTimeoutException {
		Z3Solver solver = new Z3Solver();
		TestSolverBitwise.testShiftRight(solver);
	}

	@Test
	public void testShiftRightUnsigned() throws SecurityException,
			NoSuchMethodException, SolverTimeoutException {
		Z3Solver solver = new Z3Solver();
		TestSolverBitwise.testShiftRightUnsigned(solver);
	}
}
