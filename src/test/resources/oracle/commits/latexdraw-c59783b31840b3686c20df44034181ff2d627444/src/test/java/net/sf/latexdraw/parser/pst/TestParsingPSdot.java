package net.sf.latexdraw.parser.pst;

import com.codepoetics.protonpack.StreamUtils;
import java.util.stream.Stream;
import net.sf.latexdraw.model.api.shape.Dot;
import net.sf.latexdraw.model.api.shape.DotStyle;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.view.latex.DviPsColors;
import net.sf.latexdraw.view.pst.PSTricksConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestParsingPSdot extends TestPSTParser {
	public static Stream<String> cmds() {
		return Stream.of("\\psdot", "\\psdots");
	}

	public static Stream<Arguments> cmdDotStyleProvider() {
		return StreamUtils.zip(cmds(), Stream.of(DotStyle.values()), (a, b) -> Arguments.of(a, b));
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testPssetunityunit(final String cmd) {
		parser("\\psset{unit=2,yunit=3}" + cmd + "(1,1)");
		final Dot dot = getShapeAt(0);
		assertEquals(2d * Shape.PPC, dot.getX(), 0.000001);
		assertEquals(-2d * 3d * Shape.PPC, dot.getY(), 0.000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testPssetunitxunit(final String cmd) {
		parser("\\psset{unit=2,xunit=3}" + cmd + "(1,1)");
		final Dot dot = getShapeAt(0);
		assertEquals(2d * 3d * Shape.PPC, dot.getX(), 0.000001);
		assertEquals(-2d * Shape.PPC, dot.getY(), 0.000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testPssetdotunitdot(final String cmd) {
		parser("\\psdot(1,1)\\psset{unit=2}" + cmd + "(1,1)");
		Dot dot = getShapeAt(1);
		assertEquals(2d * Shape.PPC, dot.getX(), 0.000001);
		assertEquals(-2d * Shape.PPC, dot.getY(), 0.000001);
		dot = getShapeAt(0);
		assertEquals(Shape.PPC, dot.getX(), 0.000001);
		assertEquals(-Shape.PPC, dot.getY(), 0.000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testPssetunit(final String cmd) {
		parser("\\psset{unit=2}" + cmd + "(1,1)");
		final Dot dot = getShapeAt(0);
		assertEquals(2d * Shape.PPC, dot.getX(), 0.000001);
		assertEquals(-2d * Shape.PPC, dot.getY(), 0.000001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testDotAngle(final String cmd) {
		parser(cmd + "[dotangle=90](1,1)");
		final Dot dot = getShapeAt(0);
		assertEquals(Math.PI / 2d, dot.getRotationAngle(), 0.001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testDotScale2num(final String cmd) {
		parser(cmd + "(1,1)" + cmd + "[dotscale=2 3](1,1)");
		final Dot dot1 = getShapeAt(0);
		final Dot dot2 = getShapeAt(1);
		assertEquals(dot1.getDiametre() * 2d, dot2.getDiametre(), 0.001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testDotScale1num(final String cmd) {
		parser(cmd + "(1,1)" + cmd + "[dotscale=2](1,1)");
		final Dot dot1 = getShapeAt(0);
		final Dot dot2 = getShapeAt(1);
		assertEquals(dot1.getDiametre() * 2d, dot2.getDiametre(), 0.001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testDotsizeNoUnit(final String cmd) {
		parser(cmd + "[dotsize=1.5 2](1,1)");
		final Dot dot = getShapeAt(0);
		assertEquals(1.5 * Shape.PPC + 2d * PSTricksConstants.DEFAULT_LINE_WIDTH * Shape.PPC, dot.getDiametre(), 0.001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testDotsizeNoNum(final String cmd) {
		parser(cmd + "[dotsize=1.5](1,1)");
		final Dot dot = getShapeAt(0);
		assertEquals(1.5 * Shape.PPC, dot.getDiametre(), 0.001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testDotsizeNoNumWithUnit(final String cmd) {
		parser(cmd + "[dotsize=15 mm](1,1)");
		final Dot dot = getShapeAt(0);
		assertEquals(1.5 * Shape.PPC, dot.getDiametre(), 0.001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testDotsizeNoNumWithWhitespace(final String cmd) {
		parser(cmd + "[dotsize=15 mm](1,1)");
		final Dot dot = getShapeAt(0);
		assertEquals(1.5 * Shape.PPC, dot.getDiametre(), 0.001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testDotsize(final String cmd) {
		parser(cmd + "[dotsize=1.5 cm 4](1,1)");
		final Dot dot = getShapeAt(0);
		assertEquals(1.5 * Shape.PPC + 4d * PSTricksConstants.DEFAULT_LINE_WIDTH * Shape.PPC, dot.getDiametre(), 0.001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmdDotStyleProvider")
	public void testDotStyleOK(final String cmd, final DotStyle style) {
		parser(cmd + "[dotstyle=" + style.getPSTToken() + "](1,1)");
		final Dot dot = getShapeAt(0);
		assertEquals(style, dot.getDotStyle());
	}

	@Test
	public void testNoDotStyle() {
		parser("\\psdot");
		final Dot dot = getShapeAt(0);
		assertEquals(DotStyle.DOT, dot.getDotStyle());
	}

	@Test
	public void testNoCoordinate() {
		parser("\\psdot");
		final Dot dot = getShapeAt(0);
		assertEquals(0d, dot.getPtAt(0).getX(), 0.0001);
		assertEquals(0d, dot.getPtAt(0).getY(), 0.0001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testParse1Coordinates(final String cmd) {
		parser(cmd + "(5,10)");
		final Dot dot = getShapeAt(0);
		assertEquals(5d * Shape.PPC, dot.getPtAt(0).getX(), 0.0001);
		assertEquals(-10d * Shape.PPC, dot.getPtAt(0).getY(), 0.0001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testCoordinatesPt(final String cmd) {
		parser(cmd + "(35pt,20pt)");
		final Dot dot = getShapeAt(0);
		assertEquals(35d * Shape.PPC / PSTricksConstants.CM_VAL_PT, dot.getPtAt(0).getX(), 0.0001);
		assertEquals(-20d * Shape.PPC / PSTricksConstants.CM_VAL_PT, dot.getPtAt(0).getY(), 0.0001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testCoordinatesMm(final String cmd) {
		parser(cmd + "(350mm,200mm)");
		final Dot dot = getShapeAt(0);
		assertEquals(35d * Shape.PPC, dot.getPtAt(0).getX(), 0.0001);
		assertEquals(-20d * Shape.PPC, dot.getPtAt(0).getY(), 0.0001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testCoordinatesInch(final String cmd) {
		parser(cmd + "(35in,20in)");
		final Dot dot = getShapeAt(0);
		assertEquals(35d * Shape.PPC / 2.54, dot.getPtAt(0).getX(), 0.0001);
		assertEquals(-20d * Shape.PPC / 2.54, dot.getPtAt(0).getY(), 0.0001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testCoordinatesCm(final String cmd) {
		parser(cmd + "(35cm,20cm)");
		final Dot dot = getShapeAt(0);
		assertEquals(35d * Shape.PPC, dot.getPtAt(0).getX(), 0.0001);
		assertEquals(-20d * Shape.PPC, dot.getPtAt(0).getY(), 0.0001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testFloatSigns(final String cmd) {
		parser(cmd + "(+++35.5,--50.5)");
		final Dot dot = getShapeAt(0);
		assertEquals(35.5 * Shape.PPC, dot.getPtAt(0).getX(), 0.0001);
		assertEquals(-50.5 * Shape.PPC, dot.getPtAt(0).getY(), 0.0001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testStarLineColourIsFillingColour(final String cmd) {
		parser(cmd + "*[" + "linecolor=green, dotstyle=o](1,1)");
		final Dot dot = getShapeAt(0);
		assertEquals(DviPsColors.GREEN, dot.getFillingCol());
		assertEquals(DviPsColors.GREEN, dot.getLineColour());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	public void testCoordinatesFloat2(final String cmd) {
		parser(cmd + "(35.5,50.5)");
		final Dot dot = getShapeAt(0);
		assertEquals(35.5 * Shape.PPC, dot.getPtAt(0).getX(), 0.0001);
		assertEquals(-50.5 * Shape.PPC, dot.getPtAt(0).getY(), 0.0001);
	}
}
