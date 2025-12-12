package net.sf.latexdraw.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestTriple {
	Triple<Double, String, Boolean> triple;

	@BeforeEach
	void setUp() {
		triple = new Triple<>(1.2, "bar", true);
	}

	@Test
	void testA() {
		assertEquals(1.2, triple.a, 0.00001);
	}

	@Test
	void testB() {
		assertEquals("bar", triple.b);
	}

	@Test
	void testC() {
		assertTrue(triple.c);
	}

	@Test
	void testToString() {
		assertThat(triple.toString(), not(is(emptyOrNullString())));
	}
}
