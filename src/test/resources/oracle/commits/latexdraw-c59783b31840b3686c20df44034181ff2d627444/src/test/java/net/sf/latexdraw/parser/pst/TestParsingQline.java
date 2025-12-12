package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.FillingStyle;
import net.sf.latexdraw.model.api.shape.Polyline;
import net.sf.latexdraw.model.api.shape.Shape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestParsingQline extends TestPSTParser {
	@Test
	public void testCoordinatesCm() {
		parser("\\qline(35cm,20cm)(11.12cm,-2cm)");
		final Polyline line = getShapeAt(0);
		assertEquals(2, line.getNbPoints());
		assertEquals(35d * Shape.PPC, line.getPtAt(0).getX(), 0.001);
		assertEquals(20d * Shape.PPC * -1d, line.getPtAt(0).getY(), 0.001);
		assertEquals(11.12 * Shape.PPC, line.getPtAt(1).getX(), 0.001);
		assertEquals(-2d * Shape.PPC * -1d, line.getPtAt(1).getY(), 0.001);
	}

	@Test
	public void testNoDbleBord() {
		parser("\\psset{doubleline=true}\\qline(35cm,20cm)(11.12cm,-2cm)");
		final Polyline line = getShapeAt(0);
		assertFalse(line.hasDbleBord());
	}

	@Test
	public void testNoShadow() {
		parser("\\psset{shadow=true}\\qline(35cm,20cm)(11.12cm,-2cm)");
		final Polyline line = getShapeAt(0);
		assertFalse(line.hasShadow());
	}

	@Test
	public void testMustNotBeFilled() {
		parser("\\qline(35cm,20cm)(11.12cm,-2cm)");
		final Polyline line = getShapeAt(0);
		assertEquals(FillingStyle.NONE, line.getFillingStyle());
	}
}
