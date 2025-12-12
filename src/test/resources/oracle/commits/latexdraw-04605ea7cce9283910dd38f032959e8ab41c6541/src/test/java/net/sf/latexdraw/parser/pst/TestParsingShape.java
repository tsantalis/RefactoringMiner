package net.sf.latexdraw.parser.pst;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.sf.latexdraw.data.DoubleData;
import net.sf.latexdraw.model.api.shape.BorderPos;
import net.sf.latexdraw.model.api.shape.FillingStyle;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.model.api.shape.LineStyle;
import net.sf.latexdraw.util.Tuple;
import net.sf.latexdraw.view.latex.DviPsColors;
import org.junit.Before;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

@RunWith(Theories.class)
public class TestParsingShape extends TestPSTParser {
	@SuppressWarnings({"unchecked", "rawtypes"})
	@DataPoints
	public static Tuple<String, String>[] cmds() {
		return new Tuple[] {new Tuple<>("\\psline", "(1,1)(2,2)(3,3)"), new Tuple<>("\\psbezier", "(1,2)(3,4)(5,6)(7,8)"),
			new Tuple<>("\\psaxes", "(0,0)(0,0)(3,4)"), new Tuple<>("\\psframe", "(1,2)(3,4)"), new Tuple<>("\\psarc", "(5,10){1}{30}{40}"),
			new Tuple<>("\\pscircle", "(2,3cm){5}"), new Tuple<>("\\psdot", "(1,2)"), new Tuple<>("\\psdiamond", "(35,20)"),
			new Tuple<>("\\psellipse", "(1,2)"), new Tuple<>("\\psgrid", "(0,0)(0,0)(1,1)"),
			new Tuple<>("\\pspolygon", "(1,2)(3,4)(5,6)"), new Tuple<>("\\pstriangle", "(2,3)(4,5)"), new Tuple<>("\\pswedge", "(1,2)")};
	}

	@DataPoints("unit")
	public static String[] units() {
		return new String[] {"cm", "", "pt", "mm", "in"};
	}

	DecimalFormat df;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		df.setMaximumFractionDigits(10);
	}

	@Theory
	public void testParamGradlines(final Tuple<String, String> cmd, @TestedOn(ints = {200, 2}) final int gradlines) {
		parser(cmd.a + "[fillstyle=gradient, gradlines=" + gradlines + "]" + cmd.b);
	}

	@Theory
	public void testParamShadowangle(final Tuple<String, String> cmd, @DoubleData final double angle) {
		parser(cmd.a + "[shadow=true, shadowangle=" + df.format(angle) + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isShadowable());
		assertEquals(Math.toRadians(angle), sh.getShadowAngle(), 0.00001);
	}

	@Theory
	public void testParamGradangle(final Tuple<String, String> cmd, @DoubleData final double angle) {
		parser(cmd.a + "[fillstyle=gradient, gradangle=" + df.format(angle) + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(Math.toRadians(angle), sh.getGradAngle(), 0.00001);
	}

	@Theory
	public void testParamGradmidpoint(final Tuple<String, String> cmd, @DoubleData(vals = {1d, 0.5, 0.22, 0d}) final double pt) {
		parser(cmd.a + "[fillstyle=gradient, gradmidpoint=" + df.format(pt) + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(pt, sh.getGradMidPt(), 0.00001);
	}

	@Theory
	public void testParamHatchangle(final Tuple<String, String> cmd, @DoubleData final double angle) {
		parser(cmd.a + "[fillstyle=clines, hatchangle=" + df.format(angle) + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertThat(sh.getHatchingsAngle(), closeTo(Math.toRadians(angle), 0.00001));
	}

	@Theory
	public void testParamHatchsep(final Tuple<String, String> cmd, @DoubleData(vals = {0.2, 2}) final double val, @FromDataPoints("unit") final String unit) {
		parser(cmd.a + "[fillstyle=clines, hatchsep=" + df.format(val) + unit + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(PSTContext.doubleUnitToUnit(val, unit) * Shape.PPC, sh.getHatchingsSep(), 0.00001);
	}

	@Theory
	public void testParamHatchwidth(final Tuple<String, String> cmd, @DoubleData(vals = {0.2, 2}) final double val, @FromDataPoints("unit") final String
		unit) {
		parser(cmd.a + "[fillstyle=clines, hatchwidth=" + df.format(val) + unit + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(PSTContext.doubleUnitToUnit(val, unit) * Shape.PPC, sh.getHatchingsWidth(), 0.00001);
	}

	@Theory
	public void testParamDoublesep(final Tuple<String, String> cmd, @DoubleData(vals = {0.2, 2}) final double val, @FromDataPoints("unit") final String unit) {
		parser(cmd.a + "[doubleline=true, doublesep=" + df.format(val) + unit + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isDbleBorderable());
		assertEquals(PSTContext.doubleUnitToUnit(val, unit) * Shape.PPC, sh.getDbleBordSep(), 0.00001);
	}

	@Theory
	public void testParamShadowsize(final Tuple<String, String> cmd, @DoubleData(vals = {0.2, 2}) final double val, @FromDataPoints("unit") final String
		unit) {
		parser(cmd.a + "[shadow=true, shadowsize=" + df.format(val) + unit + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isShadowable());
		assertEquals(PSTContext.doubleUnitToUnit(val, unit) * Shape.PPC, sh.getShadowSize(), 0.00001);
	}

	@Theory
	public void testParamLinewidth(final Tuple<String, String> cmd, @DoubleData(vals = {0.2, 2}) final double val, @FromDataPoints("unit") final String unit) {
		parser(cmd.a + "[linewidth=" + df.format(val) + unit + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isThicknessable());
		assertEquals(PSTContext.doubleUnitToUnit(val, unit) * Shape.PPC, sh.getThickness(), 0.00001);
	}

	@Theory
	public void testParamDimen(final Tuple<String, String> cmd, final BorderPos pos) {
		parser(cmd.a + "[dimen=" + pos.getLatexToken() + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isBordersMovable());
		assertEquals(pos, sh.getBordersPosition());
	}

	@Theory
	public void testParamLineStyle(final Tuple<String, String> cmd, final LineStyle style) {
		parser(cmd.a + "[linestyle=" + style.getLatexToken() + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isLineStylable());
		assertEquals(style, sh.getLineStyle());
	}

	@Theory
	public void testParamFillingStyle(final Tuple<String, String> cmd, final FillingStyle style) {
		parser(cmd.a + "[fillstyle=" + style.getLatexToken() + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isFillable());
		assertEquals(style, sh.getFillingStyle());
	}

	@Theory
	public void testParamLinecolorPredefinedColor(final Tuple<String, String> cmd) {
		parser(cmd.a + "[linecolor=blue]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isLineStylable());
		assertEquals(DviPsColors.BLUE, sh.getLineColour());
	}

	@Theory
	public void testParamShadow(final Tuple<String, String> cmd, final boolean shadow) {
		parser(cmd.a + "[shadow =" + shadow + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isShadowable());
		assertEquals(shadow, sh.hasShadow());
	}

	@Theory
	public void testParamshadowcolorPredefinedColor(final Tuple<String, String> cmd) {
		parser(cmd.a + "[shadow=true, shadowcolor=blue]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isShadowable());
		assertEquals(DviPsColors.BLUE, sh.getShadowCol());
	}

	@Theory
	public void testParamDoublecolorPredefined(final Tuple<String, String> cmd) {
		parser(cmd.a + "[doubleline=true, doublecolor=red]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isDbleBorderable());
		assertEquals(DviPsColors.RED, sh.getDbleBordCol());
	}

	@Theory
	public void testParamFillingcolorPredefined(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=solid, fillcolor=green]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isFillable());
		assertEquals(DviPsColors.GREEN, sh.getFillingCol());
	}

	@Theory
	public void testParamGradbegin(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=gradient, gradbegin=yellow]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(DviPsColors.YELLOW, sh.getGradColStart());
	}

	@Theory
	public void testParamGradend(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=gradient, gradend=blue]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(DviPsColors.BLUE, sh.getGradColEnd());
	}

	@Theory
	public void testParamHatchcolor(final Tuple<String, String> cmd) {
		parser(cmd.a + "[fillstyle=clines, hatchcolor=blue]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(DviPsColors.BLUE, sh.getHatchingsCol());
	}

	@Theory
	public void testParamDoubleLine(final Tuple<String, String> cmd, final boolean dble) {
		parser(cmd.a + "[doubleline = " + dble + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isDbleBorderable());
		assertEquals(dble, sh.hasDbleBord());
	}

	@Theory
	public void testStrokeOpacity(final Tuple<String, String> cmd, @DoubleData(vals = {0d, 0.2, 0.5, 1d}) final double opacity) {
		parser(cmd.a + "[strokeopacity = " + df.format(opacity) + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isLineStylable());
		assertEquals(opacity, sh.getLineColour().getO(), 0.00001);
	}

	@Theory
	public void testOpacity(final Tuple<String, String> cmd, @DoubleData(vals = {0d, 0.2, 0.5, 1d}) final double opacity) {
		parser(cmd.a + "[fillstyle = solid, opacity = " + df.format(opacity) + "]" + cmd.b);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isFillable());
		assertEquals(opacity, sh.getFillingCol().getO(), 0.00001);
	}
}
