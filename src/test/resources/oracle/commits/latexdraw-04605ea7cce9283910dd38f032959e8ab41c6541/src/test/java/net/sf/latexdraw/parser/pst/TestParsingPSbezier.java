package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.ArrowStyle;
import net.sf.latexdraw.model.api.shape.BezierCurve;
import net.sf.latexdraw.model.api.shape.Shape;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestParsingPSbezier extends TestPSTParser {
	@Test
	public void testSimpleBezierCurveOpen() {
		parser("\\psbezier[linecolor=black, linewidth=0.04](0.014142151,-1.4747834)(0.7212489,-2.1818902)(4.9070354,-1.3818903)(5.614142," +
			"-0.6747834777832031)(6.321249,0.032323305)(4.2040915,1.8666378)(3.214142,1.7252165)");
		final BezierCurve bc = getShapeAt(0);
		assertTrue(bc.isOpened());
	}

	@Test
	public void testSimpleBezierCurveClose() {
		parser("\\psbezier[linecolor=black, linewidth=0.04](0.10361466,-0.36860093)(0.76211923,-1.1211777)(7.561873,-1.9049373)(8.503614," +
			"-1.5686009216308594)(9.445356,-1.2322645)(6.693564,1.4899777)(5.7036147,1.631399)(4.713665,1.7728205)(-0.55489,0.38397577)(0.10361466," +
			"-0.36860093)");
		final BezierCurve bc = getShapeAt(0);
		assertFalse(bc.isOpened());
	}

	@Test
	public void testSimpleBezierCurve() {
		parser("\\psbezier[linewidth=0.02](1.3918242,0.7584497)(2.0668242,0.95844966)(4.3168244,0.95844966)(4.991824,0.7584497)");
		final BezierCurve bc = getShapeAt(0);
		assertEquals(bc.getPtAt(0).getY(), bc.getPtAt(1).getY(), 0.001);
		assertEquals(bc.getFirstCtrlPtAt(0).getY(), bc.getFirstCtrlPtAt(1).getY(), 0.001);
		assertFalse(bc.isFilled());
	}

	@Test
	public void testParamArrowsArrowsNoneNone() {
		parser("\\psbezier[arrows=<->]{-}(1,2)(3,4)(5,6)(7,8)");
		final BezierCurve bc = getShapeAt(0);
		assertEquals(ArrowStyle.NONE, bc.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, bc.getArrowStyle(1));
	}

	@Test
	public void testParamBarInSqureBracket() {
		parser("\\psbezier{|-]}(1,2)(3,4)(5,6)(7,8)");
		final BezierCurve bc = getShapeAt(0);
		assertEquals(ArrowStyle.BAR_IN, bc.getArrowStyle(0));
		assertEquals(ArrowStyle.RIGHT_SQUARE_BRACKET, bc.getArrowStyle(1));
	}

	@Test
	public void testParamArrowsArrows() {
		parser("\\psbezier[arrows=<->](1,2)(3,4)(5,6)(7,8)");
		final BezierCurve bc = getShapeAt(0);
		assertEquals(ArrowStyle.LEFT_ARROW, bc.getArrowStyle(0));
		assertEquals(ArrowStyle.RIGHT_ARROW, bc.getArrowStyle(1));
	}

	@Test
	public void testParse9Coordinates() {
		parser("\\psbezier(1,2)(3,4)(5,6)(7,8)(9,10)(11,12)(13,14)(15,16)(17,18)");
		final BezierCurve bc = getShapeAt(0);
		assertEquals(4, bc.getNbPoints());
		assertEquals(0d, bc.getPtAt(0).getX(), 0.0001);
		assertEquals(0d, bc.getPtAt(0).getY(), 0.0001);
		assertEquals(5d * Shape.PPC, bc.getPtAt(1).getX(), 0.0001);
		assertEquals(-6d * Shape.PPC, bc.getPtAt(1).getY(), 0.0001);
		assertEquals(11d * Shape.PPC, bc.getPtAt(2).getX(), 0.0001);
		assertEquals(-12d * Shape.PPC, bc.getPtAt(2).getY(), 0.0001);
		assertEquals(17d * Shape.PPC, bc.getPtAt(3).getX(), 0.0001);
		assertEquals(-18d * Shape.PPC, bc.getPtAt(3).getY(), 0.0001);
		assertEquals(-1d * Shape.PPC, bc.getSecondCtrlPtAt(0).getX(), 0.0001);
		assertEquals(2d * Shape.PPC, bc.getSecondCtrlPtAt(0).getY(), 0.0001);
		assertEquals(7d * Shape.PPC, bc.getSecondCtrlPtAt(1).getX(), 0.0001);
		assertEquals(-8d * Shape.PPC, bc.getSecondCtrlPtAt(1).getY(), 0.0001);
		assertEquals(13d * Shape.PPC, bc.getSecondCtrlPtAt(2).getX(), 0.0001);
		assertEquals(-14d * Shape.PPC, bc.getSecondCtrlPtAt(2).getY(), 0.0001);
		assertEquals(3d * Shape.PPC, bc.getFirstCtrlPtAt(1).getX(), 0.0001);
		assertEquals(-4d * Shape.PPC, bc.getFirstCtrlPtAt(1).getY(), 0.0001);
		assertEquals(9d * Shape.PPC, bc.getFirstCtrlPtAt(2).getX(), 0.0001);
		assertEquals(-10d * Shape.PPC, bc.getFirstCtrlPtAt(2).getY(), 0.0001);
		assertEquals(15d * Shape.PPC, bc.getFirstCtrlPtAt(3).getX(), 0.0001);
		assertEquals(-16d * Shape.PPC, bc.getFirstCtrlPtAt(3).getY(), 0.0001);
	}

	@Test
	public void testParse10Coordinates() {
		parser("\\psbezier(5,10)(1,2)(3,4)(5,6)(7,8)(9,10)(11,12)(13,14)(15,16)(17,18)");
		final BezierCurve bc = getShapeAt(0);
		assertEquals(4, bc.getNbPoints());
		assertEquals(5d * Shape.PPC, bc.getPtAt(0).getX(), 0.0001);
		assertEquals(-10d * Shape.PPC, bc.getPtAt(0).getY(), 0.0001);
		assertEquals(5d * Shape.PPC, bc.getPtAt(1).getX(), 0.0001);
		assertEquals(-6d * Shape.PPC, bc.getPtAt(1).getY(), 0.0001);
		assertEquals(11d * Shape.PPC, bc.getPtAt(2).getX(), 0.0001);
		assertEquals(-12d * Shape.PPC, bc.getPtAt(2).getY(), 0.0001);
		assertEquals(17d * Shape.PPC, bc.getPtAt(3).getX(), 0.0001);
		assertEquals(-18d * Shape.PPC, bc.getPtAt(3).getY(), 0.0001);
		assertEquals(9d * Shape.PPC, bc.getSecondCtrlPtAt(0).getX(), 0.0001);
		assertEquals(-18d * Shape.PPC, bc.getSecondCtrlPtAt(0).getY(), 0.0001);
		assertEquals(7d * Shape.PPC, bc.getSecondCtrlPtAt(1).getX(), 0.0001);
		assertEquals(-8d * Shape.PPC, bc.getSecondCtrlPtAt(1).getY(), 0.0001);
		assertEquals(13d * Shape.PPC, bc.getSecondCtrlPtAt(2).getX(), 0.0001);
		assertEquals(-14d * Shape.PPC, bc.getSecondCtrlPtAt(2).getY(), 0.0001);
		assertEquals(3d * Shape.PPC, bc.getFirstCtrlPtAt(1).getX(), 0.0001);
		assertEquals(-4d * Shape.PPC, bc.getFirstCtrlPtAt(1).getY(), 0.0001);
		assertEquals(9d * Shape.PPC, bc.getFirstCtrlPtAt(2).getX(), 0.0001);
		assertEquals(-10d * Shape.PPC, bc.getFirstCtrlPtAt(2).getY(), 0.0001);
		assertEquals(15d * Shape.PPC, bc.getFirstCtrlPtAt(3).getX(), 0.0001);
		assertEquals(-16d * Shape.PPC, bc.getFirstCtrlPtAt(3).getY(), 0.0001);
	}

	@Test
	public void testParse6Coordinates() {
		parser("\\psbezier(1,2)(3,4)(5,6)(7,8)(9,10)(11,12)");
		final BezierCurve bc = getShapeAt(0);
		assertEquals(3, bc.getNbPoints());
		assertEquals(0d, bc.getPtAt(0).getX(), 0.0001);
		assertEquals(0d, bc.getPtAt(0).getY(), 0.0001);
		assertEquals(5d * Shape.PPC, bc.getPtAt(1).getX(), 0.0001);
		assertEquals(-6d * Shape.PPC, bc.getPtAt(1).getY(), 0.0001);
		assertEquals(11d * Shape.PPC, bc.getPtAt(2).getX(), 0.0001);
		assertEquals(-12d * Shape.PPC, bc.getPtAt(2).getY(), 0.0001);
		assertEquals(-1d * Shape.PPC, bc.getSecondCtrlPtAt(0).getX(), 0.0001);
		assertEquals(2d * Shape.PPC, bc.getSecondCtrlPtAt(0).getY(), 0.0001);
		assertEquals(7d * Shape.PPC, bc.getSecondCtrlPtAt(1).getX(), 0.0001);
		assertEquals(-8d * Shape.PPC, bc.getSecondCtrlPtAt(1).getY(), 0.0001);
		assertEquals(3d * Shape.PPC, bc.getFirstCtrlPtAt(1).getX(), 0.0001);
		assertEquals(-4d * Shape.PPC, bc.getFirstCtrlPtAt(1).getY(), 0.0001);
		assertEquals(9d * Shape.PPC, bc.getFirstCtrlPtAt(2).getX(), 0.0001);
		assertEquals(-10d * Shape.PPC, bc.getFirstCtrlPtAt(2).getY(), 0.0001);
	}

	@Test
	public void testParse7Coordinates() {
		parser("\\psbezier(5,10)(1,2)(3,4)(5,6)(7,8)(9,10)(11,12)");
		final BezierCurve bc = getShapeAt(0);
		assertEquals(3, bc.getNbPoints());
		assertEquals(5d * Shape.PPC, bc.getPtAt(0).getX(), 0.0001);
		assertEquals(-10d * Shape.PPC, bc.getPtAt(0).getY(), 0.0001);
		assertEquals(5d * Shape.PPC, bc.getPtAt(1).getX(), 0.0001);
		assertEquals(-6d * Shape.PPC, bc.getPtAt(1).getY(), 0.0001);
		assertEquals(11d * Shape.PPC, bc.getPtAt(2).getX(), 0.0001);
		assertEquals(-12d * Shape.PPC, bc.getPtAt(2).getY(), 0.0001);
		assertEquals(9d * Shape.PPC, bc.getSecondCtrlPtAt(0).getX(), 0.0001);
		assertEquals(-18d * Shape.PPC, bc.getSecondCtrlPtAt(0).getY(), 0.0001);
		assertEquals(7d * Shape.PPC, bc.getSecondCtrlPtAt(1).getX(), 0.0001);
		assertEquals(-8d * Shape.PPC, bc.getSecondCtrlPtAt(1).getY(), 0.0001);
		assertEquals(3d * Shape.PPC, bc.getFirstCtrlPtAt(1).getX(), 0.0001);
		assertEquals(-4d * Shape.PPC, bc.getFirstCtrlPtAt(1).getY(), 0.0001);
		assertEquals(9d * Shape.PPC, bc.getFirstCtrlPtAt(2).getX(), 0.0001);
		assertEquals(-10d * Shape.PPC, bc.getFirstCtrlPtAt(2).getY(), 0.0001);
	}

	@Test
	public void testParse3Coordinates() {
		parser("\\psbezier(1,2)(3,4)(5,6)");
		final BezierCurve bc = getShapeAt(0);
		assertEquals(2, bc.getNbPoints());
		assertEquals(0d, bc.getPtAt(0).getX(), 0.0001);
		assertEquals(0d, bc.getPtAt(0).getY(), 0.0001);
		assertEquals(5d * Shape.PPC, bc.getPtAt(1).getX(), 0.0001);
		assertEquals(-6d * Shape.PPC, bc.getPtAt(1).getY(), 0.0001);
		assertEquals(-1d * Shape.PPC, bc.getSecondCtrlPtAt(0).getX(), 0.0001);
		assertEquals(2d * Shape.PPC, bc.getSecondCtrlPtAt(0).getY(), 0.0001);
		assertEquals(3d * Shape.PPC, bc.getFirstCtrlPtAt(1).getX(), 0.0001);
		assertEquals(-4d * Shape.PPC, bc.getFirstCtrlPtAt(1).getY(), 0.0001);
	}

	@Test
	public void testParse4Coordinates() {
		parser("\\psbezier(5,10)(1,2)(3,4)(5,6)");
		final BezierCurve bc = getShapeAt(0);
		assertEquals(2, bc.getNbPoints());
		assertEquals(5d * Shape.PPC, bc.getPtAt(0).getX(), 0.0001);
		assertEquals(-10d * Shape.PPC, bc.getPtAt(0).getY(), 0.0001);
		assertEquals(5d * Shape.PPC, bc.getPtAt(1).getX(), 0.0001);
		assertEquals(-6d * Shape.PPC, bc.getPtAt(1).getY(), 0.0001);
		assertEquals(9d * Shape.PPC, bc.getSecondCtrlPtAt(0).getX(), 0.0001);
		assertEquals(-18d * Shape.PPC, bc.getSecondCtrlPtAt(0).getY(), 0.0001);
		assertEquals(3d * Shape.PPC, bc.getFirstCtrlPtAt(1).getX(), 0.0001);
		assertEquals(-4d * Shape.PPC, bc.getFirstCtrlPtAt(1).getY(), 0.0001);
	}

	@Test
	public void testShowPoints() {
		parser("\\psbezier[showpoints=true](5,10)(1,2)(3,4)(5,6)");
		final BezierCurve bc = getShapeAt(0);
		assertTrue(bc.isShowPts());
	}
}
