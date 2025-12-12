package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.FreeHandStyle;
import net.sf.latexdraw.model.api.shape.Freehand;
import net.sf.latexdraw.model.api.shape.Shape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestParsingPscustom extends TestPSTParser {
	@Test
	public void testPsCustomFreeHandClose() {
		parser("\\pscustom{\\moveto(0.0,2.64)\\lineto(0.19,2.82)\\curveto(0.285,2.91)(1.49,3.16)(2.6,3.32)" +
				"\\curveto(3.71,3.48)(5.625,3.205)(6.43,2.77)\\curveto(7.235,2.335)(8.07,1.135)(8.1,0.37)" +
				"\\curveto(8.13,-0.395)(7.64,-1.63)(7.12,-2.1)\\curveto(6.6,-2.57)(5.45,-3.18)(4.82,-3.32)\\closepath}");
		assertEquals(1, parsedShapes.size());
		final Freehand fh = getShapeAt(0);
		assertEquals(7, fh.getNbPoints());
		assertEquals(0d, fh.getPtAt(0).getX(), 0.001);
		assertEquals(-2.64 * Shape.PPC, fh.getPtAt(0).getY(), 0.001);
		assertEquals(0.19 * Shape.PPC, fh.getPtAt(1).getX(), 0.001);
		assertEquals(-2.82 * Shape.PPC, fh.getPtAt(1).getY(), 0.001);
		assertEquals(2.6 * Shape.PPC, fh.getPtAt(2).getX(), 0.001);
		assertEquals(-3.32 * Shape.PPC, fh.getPtAt(2).getY(), 0.001);
		assertEquals(6.43 * Shape.PPC, fh.getPtAt(3).getX(), 0.001);
		assertEquals(-2.77 * Shape.PPC, fh.getPtAt(3).getY(), 0.001);
		assertEquals(8.1 * Shape.PPC, fh.getPtAt(4).getX(), 0.001);
		assertEquals(-0.37 * Shape.PPC, fh.getPtAt(4).getY(), 0.001);
		assertEquals(FreeHandStyle.CURVES, fh.getType());
	}

	@Test
	public void testPsCustomFreeHandOpen() {
		parser("\\pscustom{\\moveto(0.0,2.64)\\lineto(0.19,2.82)\\curveto(0.285,2.91)(1.49,3.16)(2.6,3.32)" +
				"\\curveto(3.71,3.48)(5.625,3.205)(6.43,2.77)\\curveto(7.235,2.335)(8.07,1.135)(8.1,0.37)" +
				"\\curveto(8.13,-0.395)(7.64,-1.63)(7.12,-2.1)\\curveto(6.6,-2.57)(5.45,-3.18)(4.82,-3.32)}");
		assertEquals(1, parsedShapes.size());
		final Freehand fh = getShapeAt(0);
		assertEquals(7, fh.getNbPoints());
		assertEquals(0d, fh.getPtAt(0).getX(), 0.001);
		assertEquals(-2.64 * Shape.PPC, fh.getPtAt(0).getY(), 0.001);
		assertEquals(0.19 * Shape.PPC, fh.getPtAt(1).getX(), 0.001);
		assertEquals(-2.82 * Shape.PPC, fh.getPtAt(1).getY(), 0.001);
		assertEquals(2.6 * Shape.PPC, fh.getPtAt(2).getX(), 0.001);
		assertEquals(-3.32 * Shape.PPC, fh.getPtAt(2).getY(), 0.001);
		assertEquals(6.43 * Shape.PPC, fh.getPtAt(3).getX(), 0.001);
		assertEquals(-2.77 * Shape.PPC, fh.getPtAt(3).getY(), 0.001);
		assertEquals(8.1 * Shape.PPC, fh.getPtAt(4).getX(), 0.001);
		assertEquals(-0.37 * Shape.PPC, fh.getPtAt(4).getY(), 0.001);
		assertTrue(fh.isOpened());
		assertEquals(FreeHandStyle.CURVES, fh.getType());
	}

	@Test
	public void testPsCustomMovetoCurvetoLineTo() {
		parser("\\pscustom{\\moveto(1,2)\\curveto(3.1,4.1)(3.2,4.3)(3,4)\\lineto(5,6)}");
		assertEquals(1, parsedShapes.size());
		final Freehand fh = getShapeAt(0);
		assertEquals(3, fh.getNbPoints());
		assertEquals(1d * Shape.PPC, fh.getPtAt(0).getX(), 0.001);
		assertEquals(-2d * Shape.PPC, fh.getPtAt(0).getY(), 0.001);
		assertEquals(3d * Shape.PPC, fh.getPtAt(1).getX(), 0.001);
		assertEquals(-4d * Shape.PPC, fh.getPtAt(1).getY(), 0.001);
		assertEquals(5d * Shape.PPC, fh.getPtAt(2).getX(), 0.001);
		assertEquals(-6d * Shape.PPC, fh.getPtAt(2).getY(), 0.001);
		assertEquals(FreeHandStyle.LINES, fh.getType());
	}

	@Test
	public void testPsCustomMovetoCurveto() {
		parser("\\pscustom[linewidth=10cm]{\\moveto(1,2)\\curveto(3.1,4.1)(3.2,4.3)(3,4)}");
		final Freehand fh = getShapeAt(0);
		assertEquals(2, fh.getNbPoints());
		assertEquals(1d * Shape.PPC, fh.getPtAt(0).getX(), 0.001);
		assertEquals(-2d * Shape.PPC, fh.getPtAt(0).getY(), 0.001);
		assertEquals(3d * Shape.PPC, fh.getPtAt(1).getX(), 0.001);
		assertEquals(-4d * Shape.PPC, fh.getPtAt(1).getY(), 0.001);
		assertEquals(FreeHandStyle.CURVES, fh.getType());
	}

	@Test
	public void testPsCustomMovetoLineto() {
		parser("\\pscustom[linewidth=10cm]{\\moveto(1,2)\\lineto(3,4)}");
		final Freehand fh = getShapeAt(0);
		assertEquals(2, fh.getNbPoints());
		assertEquals(1d * Shape.PPC, fh.getPtAt(0).getX(), 0.001);
		assertEquals(-2d * Shape.PPC, fh.getPtAt(0).getY(), 0.001);
		assertEquals(3d * Shape.PPC, fh.getPtAt(1).getX(), 0.001);
		assertEquals(-4d * Shape.PPC, fh.getPtAt(1).getY(), 0.001);
		assertEquals(FreeHandStyle.LINES, fh.getType());
	}

	@Test
	public void testPsCustomNothingWithNewpathCommand() {
		parser("\\pscustom{\\newpath}");
		assertTrue(parsedShapes.isEmpty());
	}

	@Test
	public void testPsCustomMustConsiderParametersCommand() {
		parser("\\pscustom[linewidth=10cm]{\\moveto(1,2)\\lineto(3,4)\\lineto(4,4)}");
		final Shape sh = getShapeAt(0);
		assertEquals(10d * Shape.PPC, sh.getThickness(), 0.001);
	}

	@Test
	public void testPsCustomStarCommand() {
		parser("\\pscustom*{\\moveto(1,2)\\lineto(3,4)\\lineto(4,4)}");
		final Shape sh = getShapeAt(0);
		assertTrue(sh.isFilled());
	}

	@Test
	public void testPsCustomEmptyCommand() {
		parser("\\pscustom{}");
		assertTrue(parsedShapes.isEmpty());
	}
}
