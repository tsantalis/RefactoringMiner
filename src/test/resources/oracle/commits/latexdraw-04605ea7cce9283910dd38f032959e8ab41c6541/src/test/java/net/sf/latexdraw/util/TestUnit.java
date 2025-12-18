package net.sf.latexdraw.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestUnit {
	@ParameterizedTest
	@EnumSource(Unit.class)
	public void testGetUnit(final Unit unit) {
		assertEquals(unit, Unit.getUnit(unit.name()));
	}

	@ParameterizedTest
	@EnumSource(Unit.class)
	public void testgetLabel(final Unit unit) {
		assertThat(unit.getLabel(new LangService(new SystemService()).getBundle()), not(is(emptyOrNullString())));
	}

	@ParameterizedTest
	@EnumSource(Unit.class)
	public void testGetUnitNULL() {
		assertEquals(Unit.CM, Unit.getUnit(null));
	}

	@ParameterizedTest
	@EnumSource(Unit.class)
	public void testGetUnitKO() {
		assertEquals(Unit.CM, Unit.getUnit("bad"));
	}
}
