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
package org.evosuite.symbolic.solver.z3str2;

import org.evosuite.symbolic.solver.SolverTimeoutException;
import org.evosuite.symbolic.solver.TestSolverMathFloat;
import org.junit.Test;

public class TestZ3Str2MathFloat extends TestZ3Str2{

	@Test
	public void testFloatAbs() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		Z3Str2Solver solver = new Z3Str2Solver();
		TestSolverMathFloat.testAbs(solver);
	}

	@Test
	public void testFloatTrigonometry() throws SecurityException,
			NoSuchMethodException, SolverTimeoutException {
		Z3Str2Solver solver = new Z3Str2Solver();
		TestSolverMathFloat.testTrigonometry(solver);
	}

	@Test
	public void testFloatMax() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		Z3Str2Solver solver = new Z3Str2Solver();
		TestSolverMathFloat.testMax(solver);
	}

	@Test
	public void testFloatMin() throws SecurityException, NoSuchMethodException,
			SolverTimeoutException {
		Z3Str2Solver solver = new Z3Str2Solver();
		TestSolverMathFloat.testMin(solver);
	}

	@Test
	public void testFloatRound() throws SecurityException,
			NoSuchMethodException, SolverTimeoutException {
		Z3Str2Solver solver = new Z3Str2Solver();
		TestSolverMathFloat.testRound(solver);
	}

}
