package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.data.DoubleData;
import net.sf.latexdraw.model.api.shape.ArrowStyle;
import net.sf.latexdraw.model.api.shape.Arc;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(Theories.class)
public class TestParsingPsarcn extends TestPSTParser {
	@Theory
	public void testAngle1Angle2(@DoubleData(vals = {100d, 200d, 20d, -200.15d}) final double angle1,
								@DoubleData(vals = {100d, 200d, 20d, -200.15d}) final double angle2) {
		parser("\\psarcn{10}{" + angle1 + "}{" + angle2 + "}");
		final Arc arc = getShapeAt(0);
		assertEquals(Math.toRadians(angle2), arc.getAngleStart(), 0.0000001);
		assertEquals(Math.toRadians(angle1), arc.getAngleEnd(), 0.0000001);
	}

	@Test
	public void testParamArrowsArrows() {
		parser("\\psarcn[arrows=<->]{1}{30}{40}");
		final Arc arc = getShapeAt(0);
		assertEquals(ArrowStyle.RIGHT_ARROW, arc.getArrowStyle(0));
		assertEquals(ArrowStyle.LEFT_ARROW, arc.getArrowStyle(1));
	}
}
