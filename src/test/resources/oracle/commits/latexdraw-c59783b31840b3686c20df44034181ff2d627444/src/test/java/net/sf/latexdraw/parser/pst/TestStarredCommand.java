package net.sf.latexdraw.parser.pst;

import java.util.stream.Stream;
import net.sf.latexdraw.model.api.shape.BorderPos;
import net.sf.latexdraw.model.api.shape.FillingStyle;
import net.sf.latexdraw.model.api.shape.LineStyle;
import net.sf.latexdraw.model.api.shape.Shape;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TestStarredCommand extends TestPSTParser {
	public static Stream<String> cmds() {
		return Stream.of("\\psframe*(10, 11)", "\\psellipse*(10, 11)(1, 2)", "\\psdot*(1,1)", "\\psdots*(1,1)", "\\psline*(1,1)(2,2)(3,3)",
		"\\pscircle*(1, 2){2}", "\\pstriangle*(1,2)(3,4)", "\\pspolygon*(5,10)(15,20)", "\\psdiamond*(1,2)(3,4)", "\\pswedge*(35,50){1.25}{10}{20}",
		"\\psarc*(5,10){1}{30}{40}", "\\psbezier*(1,2)(3,4)(5,6)(7,8)");
	}

	@ParameterizedTest
	@MethodSource("cmds")
	void testFilled(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isFillable());
		assertTrue(sh.isFilled());
	}

	@ParameterizedTest
	@MethodSource("cmds")
	void testFillStyle(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isInteriorStylable());
		assertEquals(FillingStyle.PLAIN, sh.getFillingStyle());
	}

	@ParameterizedTest
	@MethodSource("cmds")
	void testBorderPos(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isBordersMovable());
		assertEquals(BorderPos.INTO, sh.getBordersPosition());
	}

	@ParameterizedTest
	@MethodSource("cmds")
	void testLineStyle(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isLineStylable());
		assertEquals(LineStyle.SOLID, sh.getLineStyle());
	}

	@ParameterizedTest
	@MethodSource("cmds")
	void testShadow(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isShadowable());
		assertFalse(sh.hasShadow());
	}

	@ParameterizedTest
	@MethodSource("cmds")
	void testDbleBord(final String cmd) {
		parser(cmd);
		final Shape sh = getShapeAt(0);
		assumeTrue(sh.isDbleBorderable());
		assertFalse(sh.hasDbleBord());
	}
}
