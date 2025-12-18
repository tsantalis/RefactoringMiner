package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.ArrowStyle;
import net.sf.latexdraw.model.api.shape.Arc;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestParsingPsarc extends TestPSTParser {
	@Test
	public void testArcsepParsed() {
		parser("\\psarc[arcsep=0.3cm](5,10){1}{30}{40}");
	}

	@Test
	public void testArcsepBParsed() {
		parser("\\psarc[arcsepB=0.3cm](5,10){1}{30}{40}");
	}

	@Test
	public void testArcsepAParsed() {
		parser("\\psarc[arcsepA=0.3cm](5,10){1}{30}{40}");
	}

	@Test
	public void testParamArrowsArrowsNoneNone() {
		parser("\\psarc[arrows=<->]{-}{1}{30}{40}");
		final Arc arc = getShapeAt(0);
		assertEquals(ArrowStyle.NONE, arc.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, arc.getArrowStyle(1));
	}

	@Test
	public void testParamBarInSqureBracket() {
		parser("\\psarc{|-]}{1}{30}{40}");
		final Arc arc = getShapeAt(0);
		assertEquals(ArrowStyle.BAR_IN, arc.getArrowStyle(0));
		assertEquals(ArrowStyle.RIGHT_SQUARE_BRACKET, arc.getArrowStyle(1));
	}

	@Test
	public void testParamArrowsArrows() {
		parser("\\psarc[arrows=<->]{1}{30}{40}");
		final Arc arc = getShapeAt(0);
		assertEquals(ArrowStyle.LEFT_ARROW, arc.getArrowStyle(0));
		assertEquals(ArrowStyle.RIGHT_ARROW, arc.getArrowStyle(1));
	}
}
