package net.sf.latexdraw.parser.pst;

import com.codepoetics.protonpack.StreamUtils;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.stream.Stream;
import net.sf.latexdraw.model.api.shape.BorderPos;
import net.sf.latexdraw.model.api.shape.FillingStyle;
import net.sf.latexdraw.model.api.shape.LineStyle;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.util.Tuple;
import net.sf.latexdraw.view.latex.DviPsColors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TestParsingShape extends TestPSTParser {
	public static Stream<Tuple<String, String>> cmds() {
		return Stream.of(new Tuple<>("\\psline", "(1,1)(2,2)(3,3)"), new Tuple<>("\\psbezier", "(1,2)(3,4)(5,6)(7,8)"),
			new Tuple<>("\\psaxes", "(0,0)(0,0)(3,4)"), new Tuple<>("\\psframe", "(1,2)(3,4)"), new Tuple<>("\\psarc", "(5,10){1}{30}{40}"),
			new Tuple<>("\\pscircle", "(2,3cm){5}"), new Tuple<>("\\psdot", "(1,2)"), new Tuple<>("\\psdiamond", "(35,20)"),
			new Tuple<>("\\psellipse", "(1,2)"), new Tuple<>("\\psgrid", "(0,0)(0,0)(1,1)"),
			new Tuple<>("\\pspolygon", "(1,2)(3,4)(5,6)"), new Tuple<>("\\pstriangle", "(2,3)(4,5)"), new Tuple<>("\\pswedge", "(1,2)"));
	}

	public static Stream<String> units() {
		return Stream.of("cm", "", "pt", "mm", "in");
	}

	public static Stream<Arguments> cmdUnitProvider() {
		return StreamUtils.zip(cmds(), units(), (a, b) -> Arguments.of(a, b));
	}

	public static Stream<Arguments> cmdBorderPosProvider() {
		return StreamUtils.zip(cmds(), Stream.of(BorderPos.values()), (a, b) -> Arguments.of(a, b));
	}

	public static Stream<Arguments> cmdLineStyleProvider() {
		return StreamUtils.zip(cmds(), Stream.of(LineStyle.values()), (a, b) -> Arguments.of(a, b));
	}

	public static Stream<Arguments> cmdFillStyleProvider() {
		return StreamUtils.zip(cmds(), Stream.of(FillingStyle.values()), (a, b) -> Arguments.of(a, b));
	}

	DecimalFormat df;

	@BeforeEach
	void setUpShape() {
		df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		df.setMaximumFractionDigits(10);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamGradlines(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=gradient, gradlines=200]" + cmd.b);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamShadowangle(final Tuple<String, String> cmd) {
		parser(cmd.a + "[shadow=true, shadowangle=-23.1]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isShadowable());
		assertEquals(Math.toRadians(-23.1), sh.getShadowAngle(), 0.00001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamGradangle(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=gradient, gradangle=2.34]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(Math.toRadians(2.34), sh.getGradAngle(), 0.00001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamGradmidpoint(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=gradient, gradmidpoint=0.2]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(0.2, sh.getGradMidPt(), 0.00001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamHatchangle(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=clines, hatchangle=-23]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertThat(sh.getHatchingsAngle(), closeTo(Math.toRadians(-23), 0.00001));
	}

	@ParameterizedTest
	@MethodSource(value = "cmdUnitProvider")
	void testParamHatchsep(final Tuple<String, String> cmd, final String unit) {
		parser(cmd.a + "[fillstyle=clines, hatchsep=2.1" + unit + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(PSTContext.doubleUnitToUnit(2.1, unit) * Shape.PPC, sh.getHatchingsSep(), 0.00001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmdUnitProvider")
	void testParamHatchwidth(final Tuple<String, String> cmd, final String unit) {
		parser(cmd.a + "[fillstyle=clines, hatchwidth=23" + unit + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(PSTContext.doubleUnitToUnit(23, unit) * Shape.PPC, sh.getHatchingsWidth(), 0.00001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmdUnitProvider")
	void testParamDoublesep(final Tuple<String, String> cmd, final String unit) {
		parser(cmd.a + "[doubleline=true, doublesep=3.1" + unit + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isDbleBorderable());
		assertEquals(PSTContext.doubleUnitToUnit(3.1, unit) * Shape.PPC, sh.getDbleBordSep(), 0.00001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmdUnitProvider")
	void testParamShadowsize(final Tuple<String, String> cmd, final String unit) {
		parser(cmd.a + "[shadow=true, shadowsize=5.4" + unit + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isShadowable());
		assertEquals(PSTContext.doubleUnitToUnit(5.4, unit) * Shape.PPC, sh.getShadowSize(), 0.00001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmdUnitProvider")
	void testParamLinewidth(final Tuple<String, String> cmd, final String unit) {
		parser(cmd.a + "[linewidth=2" + unit + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isThicknessable());
		assertEquals(PSTContext.doubleUnitToUnit(2, unit) * Shape.PPC, sh.getThickness(), 0.00001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmdBorderPosProvider")
	void testParamDimen(final Tuple<String, String> cmd, final BorderPos pos) {
		parser(cmd.a + "[dimen=" + pos.getLatexToken() + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isBordersMovable());
		assertEquals(pos, sh.getBordersPosition());
	}

	@ParameterizedTest
	@MethodSource(value = "cmdLineStyleProvider")
	void testParamLineStyle(final Tuple<String, String> cmd, final LineStyle style) {
		parser(cmd.a + "[linestyle=" + style.getLatexToken() + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isLineStylable());
		assertEquals(style, sh.getLineStyle());
	}

	@ParameterizedTest
	@MethodSource(value = "cmdFillStyleProvider")
	void testParamFillingStyle(final Tuple<String, String> cmd, final FillingStyle style) {
		parser(cmd.a + "[fillstyle=" + style.getLatexToken() + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isFillable());
		assertEquals(style, sh.getFillingStyle());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamLinecolorPredefinedColor(final Tuple<String, String> cmd) {
		parser(cmd.a + "[linecolor=blue]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isLineStylable());
		assertEquals(DviPsColors.BLUE, sh.getLineColour());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamShadow(final Tuple<String, String> cmd) {
		parser(cmd.a + "[shadow =true]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isShadowable());
		assertTrue(sh.hasShadow());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamNoShadow(final Tuple<String, String> cmd) {
		parser(cmd.a + "[shadow = false]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isShadowable());
		assertFalse(sh.hasShadow());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamshadowcolorPredefinedColor(final Tuple<String, String> cmd) {
		parser(cmd.a + "[shadow=true, shadowcolor=blue]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isShadowable());
		assertEquals(DviPsColors.BLUE, sh.getShadowCol());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamDoublecolorPredefined(final Tuple<String, String> cmd) {
		parser(cmd.a + "[doubleline=true, doublecolor=red]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isDbleBorderable());
		assertEquals(DviPsColors.RED, sh.getDbleBordCol());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamFillingcolorPredefined(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=solid, fillcolor=green]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isFillable());
		assertEquals(DviPsColors.GREEN, sh.getFillingCol());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamGradbegin(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=gradient, gradbegin=yellow]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(DviPsColors.YELLOW, sh.getGradColStart());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamGradend(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=gradient, gradend=blue]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(DviPsColors.BLUE, sh.getGradColEnd());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamHatchcolor(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=clines, hatchcolor=blue]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(DviPsColors.BLUE, sh.getHatchingsCol());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamDoubleLine(final Tuple<String, String> cmd) {
		parser(cmd.a + "[doubleline = true]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isDbleBorderable());
		assertTrue(sh.hasDbleBord());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testParamNoDoubleLine(final Tuple<String, String> cmd) {
		parser(cmd.a + "[doubleline = false]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isDbleBorderable());
		assertFalse(sh.hasDbleBord());
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testStrokeOpacity(final Tuple<String, String> cmd) {
		parser(cmd.a + "[strokeopacity = 0.2]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isLineStylable());
		assertEquals(0.2, sh.getLineColour().getO(), 0.00001);
	}

	@ParameterizedTest
	@MethodSource(value = "cmds")
	void testOpacity(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle = solid, opacity =0.5]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isFillable());
		assertEquals(0.5, sh.getFillingCol().getO(), 0.00001);
	}
}
