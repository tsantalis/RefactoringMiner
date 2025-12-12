package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.Ellipse;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.view.pst.PSTricksConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestParsingPsellipse extends TestPSTParser {
	@Test
	public void testUnit() {
		parser("\\psset{unit=4}\\psellipse(2,3cm)(2cm,5cm)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(4d * 2d * Shape.PPC - 2d * Shape.PPC, ell.getX(), 0.0001);
		assertEquals(-3d * Shape.PPC + 5d * Shape.PPC, ell.getY(), 0.0001);
		assertEquals(2d * 2d * Shape.PPC, ell.getWidth(), 0.0001);
		assertEquals(2d * 5d * Shape.PPC, ell.getHeight(), 0.0001);
	}

	@Test
	public void testCoordinatesPt() {
		parser("\\psellipse(0,0)(35pt,20pt)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(-35d * Shape.PPC / PSTricksConstants.CM_VAL_PT, ell.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC / PSTricksConstants.CM_VAL_PT * -1d, ell.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC / PSTricksConstants.CM_VAL_PT * 2d, ell.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC / PSTricksConstants.CM_VAL_PT * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testCoordinatesMm() {
		parser("\\psellipse(0,0)(350mm,200mm)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(-35d * Shape.PPC, ell.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC * -1d, ell.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC * 2d, ell.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testCoordinatesInch() {
		parser("\\psellipse(0,0)(35in,20in)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(-35d * Shape.PPC / 2.54, ell.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC / 2.54 * -1d, ell.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC / 2.54 * 2d, ell.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC / 2.54 * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testCoordinatesCm() {
		parser("\\psellipse(0,0)(35cm,20cm)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(-35d * Shape.PPC, ell.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC * -1d, ell.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC * 2d, ell.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testParse1Coordinates() {
		parser("\\psellipse(35,20)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(-35d * Shape.PPC, ell.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC * -1d, ell.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC * 2d, ell.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesInt() {
		parser("\\psellipse(10,20)(35,50)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(10d * Shape.PPC - 35d * Shape.PPC, ell.getPosition().getX(), 0.001);
		assertEquals(20d * -Shape.PPC + 50d * Shape.PPC, ell.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC * 2d, ell.getWidth(), 0.001);
		assertEquals(50d * Shape.PPC * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloatSigns2() {
		parser("\\psellipse(-+.5,+-5)(+++35.5,--50.5)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(-.5 * Shape.PPC - 35.5 * Shape.PPC, ell.getPosition().getX(), 0.001);
		assertEquals(-5d * -Shape.PPC + 50.5 * Shape.PPC, ell.getPosition().getY(), 0.001);
		assertEquals(35.5 * Shape.PPC * 2d, ell.getWidth(), 0.001);
		assertEquals(50.5 * Shape.PPC * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloatSigns() {
		parser("\\psellipse(-+-.5,+--.5)(+++35.5,--50.5)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(.5 * Shape.PPC - 35.5 * Shape.PPC, ell.getPosition().getX(), 0.001);
		assertEquals(.5 * -Shape.PPC + 50.5 * Shape.PPC, ell.getPosition().getY(), 0.001);
		assertEquals(35.5 * Shape.PPC * 2d, ell.getWidth(), 0.001);
		assertEquals(50.5 * Shape.PPC * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloat2() {
		parser("\\psellipse(.5,.5)(35.5,50.5)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(.5 * Shape.PPC - 35.5 * Shape.PPC, ell.getPosition().getX(), 0.001);
		assertEquals(.5 * -Shape.PPC + 50.5 * Shape.PPC, ell.getPosition().getY(), 0.001);
		assertEquals(35.5 * Shape.PPC * 2d, ell.getWidth(), 0.001);
		assertEquals(50.5 * Shape.PPC * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloat() {
		parser("\\psellipse(10.5,20.5)(35.5,50.5)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(10.5 * Shape.PPC - 35.5 * Shape.PPC, ell.getPosition().getX(), 0.001);
		assertEquals(20.5 * -Shape.PPC + 50.5 * Shape.PPC, ell.getPosition().getY(), 0.001);
		assertEquals(35.5 * Shape.PPC * 2d, ell.getWidth(), 0.001);
		assertEquals(50.5 * Shape.PPC * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesTwoFirstMissing() {
		parser("\\psellipse(,)(35,50)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(Shape.PPC - 35 * Shape.PPC, ell.getPosition().getX(), 0.001);
		assertEquals(-Shape.PPC + 50d * Shape.PPC, ell.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC * 2d, ell.getWidth(), 0.001);
		assertEquals(50d * Shape.PPC * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesTwoLastMissing() {
		parser("\\psellipse(0,0)(,)");
		final Ellipse ell = getShapeAt(0);
		assertEquals(-Shape.PPC, ell.getPosition().getX(), 0.001);
		assertEquals(Shape.PPC, ell.getPosition().getY(), 0.001);
		assertEquals(Shape.PPC * 2d, ell.getWidth(), 0.001);
		assertEquals(Shape.PPC * 2d, ell.getHeight(), 0.001);
	}

	@Test
	public void testParse2WidthHeight0() {
		parser("\\psellipse(0,0)(0,0)");
		final Ellipse ell = getShapeAt(0);
		assertTrue(ell.getWidth() > 0);
		assertTrue(ell.getHeight() > 0);
	}
}
