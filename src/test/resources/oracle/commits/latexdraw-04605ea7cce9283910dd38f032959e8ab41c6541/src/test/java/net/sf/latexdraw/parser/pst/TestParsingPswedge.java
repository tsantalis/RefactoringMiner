package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.data.DoubleData;
import net.sf.latexdraw.model.api.shape.Arc;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.view.pst.PSTricksConstants;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

@RunWith(Theories.class)
public class TestParsingPswedge extends TestPSTParser {
	@DataPoints
	public static String[] cmds() {
		return new String[] {"\\psarc", "\\pswedge", "\\psarcn"};
	}

	@Theory
	public void testAngle1Angle2(final String cmd, @DoubleData(vals = {100d, 200d, 20d, -200.15d}) final double angle1,
								@DoubleData(vals = {100d, 200d, 20d, -200.15d}) final double angle2) {
		assumeThat(cmd, not(endsWith("psarcn")));
		parser(cmd + "{10}{" + angle1 + "}{" + angle2 + "}");
		final Arc arc = getShapeAt(0);
		assertEquals(Math.toRadians(angle1), arc.getAngleStart(), 0.0000001);
		assertEquals(Math.toRadians(angle2), arc.getAngleEnd(), 0.0000001);
	}

	@Theory
	public void testMissingOrigin(final String cmd) {
		parser(cmd + "{10}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(-10d * Shape.PPC, arc.getPosition().getX(), 0.0000001);
		assertEquals(-10d * Shape.PPC * -1d, arc.getPosition().getY(), 0.0000001);
		assertEquals(10d * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(10d * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}

	@Theory
	public void testCoordinatesPt(final String cmd) {
		parser(cmd + "(35pt,20pt){10pt}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35d * Shape.PPC / PSTricksConstants.CM_VAL_PT - 10d * Shape.PPC / PSTricksConstants.CM_VAL_PT, arc.getPosition().getX(), 0.0000001);
		assertEquals((20d * Shape.PPC / PSTricksConstants.CM_VAL_PT - 10d * Shape.PPC / PSTricksConstants.CM_VAL_PT) * -1d, arc.getPosition().getY(), 0.0000001);
		assertEquals(10d * Shape.PPC / PSTricksConstants.CM_VAL_PT * 2d, arc.getWidth(), 0.0000001);
		assertEquals(10d * Shape.PPC / PSTricksConstants.CM_VAL_PT * 2d, arc.getHeight(), 0.0000001);
	}

	@Theory
	public void testCoordinatesMm(final String cmd) {
		parser(cmd + "(350mm,200mm){10mm}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35d * Shape.PPC - 1d * Shape.PPC, arc.getPosition().getX(), 0.0000001);
		assertEquals((20d * Shape.PPC - 1d * Shape.PPC) * -1d, arc.getPosition().getY(), 0.0000001);
		assertEquals(1d * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(1d * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}

	@Theory
	public void testCoordinatesInch(final String cmd) {
		parser(cmd + "(35in,20in){1.2in}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35d * Shape.PPC / 2.54 - 1.2 * Shape.PPC / 2.54, arc.getPosition().getX(), 0.0000001);
		assertEquals((20d * Shape.PPC / 2.54 - 1.2 * Shape.PPC / 2.54) * -1d, arc.getPosition().getY(), 0.0000001);
		assertEquals(1.2 * Shape.PPC / 2.54 * 2d, arc.getWidth(), 0.0000001);
		assertEquals(1.2 * Shape.PPC / 2.54 * 2d, arc.getHeight(), 0.0000001);
	}

	@Theory
	public void testCoordinatesCm(final String cmd) {
		parser(cmd + "(35cm,20cm){.5cm}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35d * Shape.PPC - .5 * Shape.PPC, arc.getPosition().getX(), 0.001);
		assertEquals((20d * Shape.PPC - .5 * Shape.PPC) * -1d, arc.getPosition().getY(), 0.001);
		assertEquals(.5 * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(.5 * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}

	@Theory
	public void testCoordinatesRadius(final String cmd) {
		parser(cmd + "(35,20){10}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35d * Shape.PPC - 10d * Shape.PPC, arc.getPosition().getX(), 0.0000001);
		assertEquals((20d * Shape.PPC - 10d * Shape.PPC) * -1d, arc.getPosition().getY(), 0.0000001);
		assertEquals(10d * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(10d * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}

	@Theory
	public void testFloatSigns(final String cmd) {
		parser(cmd + "(+++35.5,--50.5){--+12}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35.5 * Shape.PPC - 12d * Shape.PPC, arc.getPosition().getX(), 0.001);
		assertEquals((50.5 * Shape.PPC - 12d * Shape.PPC) * -1d, arc.getPosition().getY(), 0.001);
		assertEquals(12d * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(12d * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}

	@Theory
	public void testNegativeRadius(final String cmd) {
		parser(cmd + "(0,0){-1}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertThat(arc.getWidth(), greaterThan(0d));
		assertThat(arc.getHeight(), greaterThan(0d));
	}

	@Theory
	public void testParse2CoordinatesFloat2(final String cmd) {
		parser(cmd + "(35.5,50.5){1.25}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35.5 * Shape.PPC - 1.25 * Shape.PPC, arc.getPosition().getX(), 0.001);
		assertEquals((50.5 * Shape.PPC - 1.25 * Shape.PPC) * -1d, arc.getPosition().getY(), 0.001);
		assertEquals(1.25 * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(1.25 * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}
}
