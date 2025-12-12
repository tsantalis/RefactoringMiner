package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.Arc;
import net.sf.latexdraw.model.api.shape.ArrowStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestParsingPsarcn extends TestPSTParser {
	@Test
	public void testAngle1Angle2() {
		parser("\\psarcn{10}{-200.15}{12}");
		final Arc arc = getShapeAt(0);
		assertEquals(Math.toRadians(12), arc.getAngleStart(), 0.0000001);
		assertEquals(Math.toRadians(-200.15), arc.getAngleEnd(), 0.0000001);
	}

	@Test
	public void testParamArrowsArrows() {
		parser("\\psarcn[arrows=<->]{1}{30}{40}");
		final Arc arc = getShapeAt(0);
		assertEquals(ArrowStyle.RIGHT_ARROW, arc.getArrowStyle(0));
		assertEquals(ArrowStyle.LEFT_ARROW, arc.getArrowStyle(1));
	}
}
