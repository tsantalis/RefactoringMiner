package net.sf.latexdraw.parser.pst;

import java.util.stream.Stream;
import net.sf.latexdraw.model.api.shape.Arc;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.view.pst.PSTricksConstants;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestParsingPswedge extends TestPSTParser {
	public static Stream<String> cmds() {
		return Stream.of("\\psarc", "\\pswedge", "\\psarcn");
	}

	@ParameterizedTest
	@ValueSource(strings = {"\\psarc", "\\pswedge"})
	public void testAngle1Angle2(final String cmd) {
		parser(cmd + "{10}{100.3}{-3.12}");
		final Arc arc = getShapeAt(0);
		assertEquals(Math.toRadians(100.3), arc.getAngleStart(), 0.0000001);
		assertEquals(Math.toRadians(-3.12), arc.getAngleEnd(), 0.0000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testMissingOrigin(final String cmd) {
		parser(cmd + "{10}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(-10d * Shape.PPC, arc.getPosition().getX(), 0.0000001);
		assertEquals(-10d * Shape.PPC * -1d, arc.getPosition().getY(), 0.0000001);
		assertEquals(10d * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(10d * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testCoordinatesPt(final String cmd) {
		parser(cmd + "(35pt,20pt){10pt}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35d * Shape.PPC / PSTricksConstants.CM_VAL_PT - 10d * Shape.PPC / PSTricksConstants.CM_VAL_PT, arc.getPosition().getX(), 0.0000001);
		assertEquals((20d * Shape.PPC / PSTricksConstants.CM_VAL_PT - 10d * Shape.PPC / PSTricksConstants.CM_VAL_PT) * -1d, arc.getPosition().getY(), 0.0000001);
		assertEquals(10d * Shape.PPC / PSTricksConstants.CM_VAL_PT * 2d, arc.getWidth(), 0.0000001);
		assertEquals(10d * Shape.PPC / PSTricksConstants.CM_VAL_PT * 2d, arc.getHeight(), 0.0000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testCoordinatesMm(final String cmd) {
		parser(cmd + "(350mm,200mm){10mm}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35d * Shape.PPC - 1d * Shape.PPC, arc.getPosition().getX(), 0.0000001);
		assertEquals((20d * Shape.PPC - 1d * Shape.PPC) * -1d, arc.getPosition().getY(), 0.0000001);
		assertEquals(1d * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(1d * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testCoordinatesInch(final String cmd) {
		parser(cmd + "(35in,20in){1.2in}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35d * Shape.PPC / 2.54 - 1.2 * Shape.PPC / 2.54, arc.getPosition().getX(), 0.0000001);
		assertEquals((20d * Shape.PPC / 2.54 - 1.2 * Shape.PPC / 2.54) * -1d, arc.getPosition().getY(), 0.0000001);
		assertEquals(1.2 * Shape.PPC / 2.54 * 2d, arc.getWidth(), 0.0000001);
		assertEquals(1.2 * Shape.PPC / 2.54 * 2d, arc.getHeight(), 0.0000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testCoordinatesCm(final String cmd) {
		parser(cmd + "(35cm,20cm){.5cm}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35d * Shape.PPC - .5 * Shape.PPC, arc.getPosition().getX(), 0.001);
		assertEquals((20d * Shape.PPC - .5 * Shape.PPC) * -1d, arc.getPosition().getY(), 0.001);
		assertEquals(.5 * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(.5 * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testCoordinatesRadius(final String cmd) {
		parser(cmd + "(35,20){10}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35d * Shape.PPC - 10d * Shape.PPC, arc.getPosition().getX(), 0.0000001);
		assertEquals((20d * Shape.PPC - 10d * Shape.PPC) * -1d, arc.getPosition().getY(), 0.0000001);
		assertEquals(10d * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(10d * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testFloatSigns(final String cmd) {
		parser(cmd + "(+++35.5,--50.5){--+12}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35.5 * Shape.PPC - 12d * Shape.PPC, arc.getPosition().getX(), 0.001);
		assertEquals((50.5 * Shape.PPC - 12d * Shape.PPC) * -1d, arc.getPosition().getY(), 0.001);
		assertEquals(12d * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(12d * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testNegativeRadius(final String cmd) {
		parser(cmd + "(0,0){-1}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertThat(arc.getWidth(), greaterThan(0d));
		assertThat(arc.getHeight(), greaterThan(0d));
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testParse2CoordinatesFloat2(final String cmd) {
		parser(cmd + "(35.5,50.5){1.25}{10}{20}");
		final Arc arc = getShapeAt(0);
		assertEquals(35.5 * Shape.PPC - 1.25 * Shape.PPC, arc.getPosition().getX(), 0.001);
		assertEquals((50.5 * Shape.PPC - 1.25 * Shape.PPC) * -1d, arc.getPosition().getY(), 0.001);
		assertEquals(1.25 * Shape.PPC * 2d, arc.getWidth(), 0.0000001);
		assertEquals(1.25 * Shape.PPC * 2d, arc.getHeight(), 0.0000001);
	}
}
