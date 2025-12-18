package net.sf.latexdraw.parser.pst;

import java.util.List;
import net.sf.latexdraw.model.api.shape.DotStyle;
import net.sf.latexdraw.model.api.shape.Dot;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.view.latex.DviPsColors;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Theories.class)
public class TestParsingPsdots extends TestPSTParser {
	@Test
	public void testParse2Points() {
		parser("\\psdots(35.5,50.5)(2,2)");
		final List<Shape> group = parsedShapes;
		assertEquals(2, group.size());
		assertTrue(group.get(0) instanceof Dot);
		assertTrue(group.get(1) instanceof Dot);
		assertEquals(35.5 * Shape.PPC, group.get(0).getPtAt(0).getX(), 0.0001);
		assertEquals(-50.5 * Shape.PPC, group.get(0).getPtAt(0).getY(), 0.0001);
		assertEquals(2d * Shape.PPC, group.get(1).getPtAt(0).getX(), 0.0001);
		assertEquals(-2d * Shape.PPC, group.get(1).getPtAt(0).getY(), 0.0001);
	}

	@Theory
	public void testDotStyleo(final DotStyle style) {
		parser("\\psdots[dotstyle=" + style.getPSTToken() + "](1,1)(2,2)");
		final List<Shape> group = parsedShapes;
		assertEquals(2, group.size());
		assertEquals(style, ((Dot) group.get(0)).getDotStyle());
		assertEquals(style, ((Dot) group.get(1)).getDotStyle());
	}

	@Test
	public void testDotStyleDot() {
		parser("\\psdots" + "[dotstyle=*](1,1)(2,2)");
		final List<Shape> group = parsedShapes;
		assertEquals(2, group.size());
		assertEquals(DotStyle.DOT, ((Dot) group.get(0)).getDotStyle());
		assertEquals(DotStyle.DOT, ((Dot) group.get(1)).getDotStyle());
	}

	@Test
	public void testNoDotStyle() {
		parser("\\psdots(1,1)(2,2)");
		final List<Shape> group = parsedShapes;
		assertEquals(2, group.size());
		assertEquals(DotStyle.DOT, ((Dot) group.get(0)).getDotStyle());
		assertEquals(DotStyle.DOT, ((Dot) group.get(1)).getDotStyle());
	}

	@Test
	public void testParse1Coordinates() {
		parser("\\psdots" + "(5,10)");
		final List<Shape> group = parsedShapes;
		assertEquals(1, group.size());
		assertEquals(5d * Shape.PPC, group.get(0).getPtAt(0).getX(), 0.0001);
		assertEquals(-10d * Shape.PPC, group.get(0).getPtAt(0).getY(), 0.0001);
	}

	@Test
	public void testFloatSigns() {
		parser("\\psdots(+++35.5,--50.5)(+-++35.5,---50.5)");
		final List<Shape> group = parsedShapes;
		assertEquals(35.5 * Shape.PPC, group.get(0).getPtAt(0).getX(), 0.0001);
		assertEquals(-50.5 * Shape.PPC, group.get(0).getPtAt(0).getY(), 0.0001);
		assertEquals(-35.5 * Shape.PPC, group.get(1).getPtAt(0).getX(), 0.0001);
		assertEquals(50.5 * Shape.PPC, group.get(1).getPtAt(0).getY(), 0.0001);
	}

	@Test
	public void testStarLineColourIsFillingColour() {
		parser("\\psdots*[" + "linecolor=green, dotstyle=o](1,1)(2,2)");
		final List<Shape> group = parsedShapes;
		assertEquals(DviPsColors.GREEN, group.get(0).getFillingCol());
		assertEquals(DviPsColors.GREEN, group.get(0).getLineColour());
		assertEquals(DviPsColors.GREEN, group.get(1).getFillingCol());
		assertEquals(DviPsColors.GREEN, group.get(1).getLineColour());
	}

	@Test
	public void testCoordinatesFloat2() {
		parser("\\psdots(35.5,50.5)");
		final Dot dot = getShapeAt(0);
		assertEquals(35.5 * Shape.PPC, dot.getPtAt(0).getX(), 0.0001);
		assertEquals(-50.5 * Shape.PPC, dot.getPtAt(0).getY(), 0.0001);
	}
}
