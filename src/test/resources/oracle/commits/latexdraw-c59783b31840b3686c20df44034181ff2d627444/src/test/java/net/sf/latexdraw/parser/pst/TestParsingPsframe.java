package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.Rectangle;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.view.pst.PSTricksConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestParsingPsframe extends TestPSTParser {
	@Test
	public void testPssetunityunit() {
		parser("\\psset{unit=2,yunit=3}\\psframe(1,1)(5,5)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(2d * Shape.PPC, rec.getX(), 0.000001);
		assertEquals(-2d * 3d * Shape.PPC, rec.getY(), 0.000001);
		assertEquals(2d * 4d * Shape.PPC, rec.getWidth(), 0.000001);
		assertEquals(2d * 4d * 3d * Shape.PPC, rec.getHeight(), 0.000001);
	}

	@Test
	public void testPssetunitxunit() {
		parser("\\psset{unit=2,xunit=3}\\psframe(1,1)(5,5)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(2d * 3d * Shape.PPC, rec.getX(), 0.000001);
		assertEquals(-2d * Shape.PPC, rec.getY(), 0.000001);
		assertEquals(2d * 4d * 3d * Shape.PPC, rec.getWidth(), 0.000001);
		assertEquals(2d * 4d * Shape.PPC, rec.getHeight(), 0.000001);
	}

	@Test
	public void testPssetunit() {
		parser("\\psset{unit=2}\\psframe(1,1)(5,5)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(2d * Shape.PPC, rec.getX(), 0.000001);
		assertEquals(-2d * Shape.PPC, rec.getY(), 0.000001);
		assertEquals(2d * 4d * Shape.PPC, rec.getWidth(), 0.000001);
		assertEquals(2d * 4d * Shape.PPC, rec.getHeight(), 0.000001);
	}

	@Test
	public void testParamAddfillstyle() {
		parser("\\psframe[addfillstyle=hlines](35,20)");
	}

	@Test
	public void testParamBorder() {
		parser("\\psframe[border=2.1in](35,20)");
	}

	@Test
	public void testParamDotsep() {
		parser("\\psframe[dotsep=2.1in](35,20)");
	}

	@Test
	public void testParamDashNumNum() {
		parser("\\psframe[dash=2.1 +0.3](35,20)");
	}

	@Test
	public void testParamDashDimDim() {
		parser("\\psframe[dash=2cm 0.3 pt](35,20)");
	}

	@Test
	public void testParamDashDimNum() {
		parser("\\psframe[dash=2cm 0.3](35,20)");
	}

	@ParameterizedTest
	@ValueSource(doubles = {0d, 1d, 0.5})
	public void testParamFramearcOK(final double arc) {
		parser("\\psframe[framearc=" + arc + "](35,20)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(arc, rec.getLineArc(), 0.00001);
	}

	@Test
	public void testParamFramearcReset() {
		parser("\\psframe[framearc=0.2, framearc=0.3](35,20)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(0.3, rec.getLineArc(), 0.00001);
	}

	@ParameterizedTest
	@ValueSource(doubles = {-1d, 2})
	public void testParamFramearcKO(final double arc) {
		parser("\\psframe[framearc=" + arc + "](35,20)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(0d, rec.getLineArc(), 0.00001);
	}

	@Test
	public void testCoordinatesPt() {
		parser("\\psframe(0,0)(35pt,20pt)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(0d, rec.getPosition().getX(), 0.001);
		assertEquals(0d, rec.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC / PSTricksConstants.CM_VAL_PT, rec.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC / PSTricksConstants.CM_VAL_PT, rec.getHeight(), 0.001);
	}

	@Test
	public void testCoordinatesMm() {
		parser("\\psframe(0,0)(350mm,200mm)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(0d, rec.getPosition().getX(), 0.001);
		assertEquals(0d, rec.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testCoordinatesInch() {
		parser("\\psframe(0,0)(35in,20in)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(0d, rec.getPosition().getX(), 0.001);
		assertEquals(0d, rec.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC / 2.54, rec.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC / 2.54, rec.getHeight(), 0.001);
	}

	@Test
	public void testCoordinatesCm() {
		parser("\\psframe(0,0)(35cm,20cm)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(0d, rec.getPosition().getX(), 0.001);
		assertEquals(0d, rec.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testParse1Coordinates() {
		parser("\\psframe(35,20)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(0d, rec.getPosition().getX(), 0.001);
		assertEquals(0d, rec.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesIntOppositeAll() {
		parser("\\psframe(35,50)(10,20)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(10d * Shape.PPC, rec.getPosition().getX(), 0.001);
		assertEquals(20d * -Shape.PPC, rec.getPosition().getY(), 0.001);
		assertEquals(25d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(30d * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesIntOppositeX() {
		parser("\\psframe(35,20)(10,50)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(10d * Shape.PPC, rec.getPosition().getX(), 0.001);
		assertEquals(20d * -Shape.PPC, rec.getPosition().getY(), 0.001);
		assertEquals(25d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(30d * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesIntOppositeY() {
		parser("\\psframe(10,50)(35,20)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(10d * Shape.PPC, rec.getPosition().getX(), 0.001);
		assertEquals(20d * -Shape.PPC, rec.getPosition().getY(), 0.001);
		assertEquals(25d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(30d * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesInt() {
		parser("\\psframe(10,20)(35,50)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(10d * Shape.PPC, rec.getPosition().getX(), 0.001);
		assertEquals(20d * -Shape.PPC, rec.getPosition().getY(), 0.001);
		assertEquals(25d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(30d * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloatSigns2() {
		parser("\\psframe(-+.5,+-5)(+++35.5,--50.5)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(-0.5 * Shape.PPC, rec.getPosition().getX(), 0.001);
		assertEquals(-5d * -Shape.PPC, rec.getPosition().getY(), 0.001);
		assertEquals(36d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(55.5 * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloatSigns() {
		parser("\\psframe(-+-.5,+--.5)(+++35.5,--50.5)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(0.5 * Shape.PPC, rec.getPosition().getX(), 0.001);
		assertEquals(0.5 * -Shape.PPC, rec.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(50d * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloat2() {
		parser("\\psframe(.5,.5)(35.5,50.5)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(0.5 * Shape.PPC, rec.getPosition().getX(), 0.001);
		assertEquals(0.5 * -Shape.PPC, rec.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(50d * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloat() {
		parser("\\psframe(10.5,20.5)(35.5,50.5)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(10.5 * Shape.PPC, rec.getPosition().getX(), 0.001);
		assertEquals(20.5 * -Shape.PPC, rec.getPosition().getY(), 0.001);
		assertEquals(25d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(30d * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesTwoFirstMissing() {
		parser("\\psframe(,)(35,50)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(Shape.PPC, rec.getPosition().getX(), 0.001);
		assertEquals(-Shape.PPC, rec.getPosition().getY(), 0.001);
		assertEquals(34d * Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(49d * Shape.PPC, rec.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesTwoLastMissing() {
		parser("\\psframe(0,0)(,)");
		final Rectangle rec = getShapeAt(0);
		assertEquals(0d, rec.getPosition().getX(), 0.001);
		assertEquals(0d, rec.getPosition().getY(), 0.001);
		assertEquals(Shape.PPC, rec.getWidth(), 0.001);
		assertEquals(Shape.PPC, rec.getHeight(), 0.001);
	}
}
