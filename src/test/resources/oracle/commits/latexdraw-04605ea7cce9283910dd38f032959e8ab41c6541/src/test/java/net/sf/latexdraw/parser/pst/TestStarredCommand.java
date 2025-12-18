package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.BorderPos;
import net.sf.latexdraw.model.api.shape.FillingStyle;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.model.api.shape.LineStyle;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(Theories.class)
public class TestStarredCommand extends TestPSTParser {
	@DataPoints
	public static String[] cmds() {
		return new String[] {"\\psframe*(10, 11)", "\\psellipse*(10, 11)(1, 2)", "\\psdot*(1,1)", "\\psdots*(1,1)", "\\psline*(1,1)(2,2)(3,3)",
		"\\pscircle*(1, 2){2}", "\\pstriangle*(1,2)(3,4)", "\\pspolygon*(5,10)(15,20)", "\\psdiamond*(1,2)(3,4)", "\\pswedge*(35,50){1.25}{10}{20}",
		"\\psarc*(5,10){1}{30}{40}", "\\psbezier*(1,2)(3,4)(5,6)(7,8)"};
	}

	@Theory
	public void testFilled(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isFillable());
		assertTrue(sh.isFilled());
	}

	@Theory
	public void testFillStyle(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(FillingStyle.PLAIN, sh.getFillingStyle());
	}

	@Theory
	public void testBorderPos(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isBordersMovable());
		assertEquals(BorderPos.INTO, sh.getBordersPosition());
	}

	@Theory
	public void testLineStyle(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isLineStylable());
		assertEquals(LineStyle.SOLID, sh.getLineStyle());
	}

	@Theory
	public void testShadow(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isShadowable());
		assertFalse(sh.hasShadow());
	}

	@Theory
	public void testDbleBord(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isDbleBorderable());
		assertFalse(sh.hasDbleBord());
	}
}
