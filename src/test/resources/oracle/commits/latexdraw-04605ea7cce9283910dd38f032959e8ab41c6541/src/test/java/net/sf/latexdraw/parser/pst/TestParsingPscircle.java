package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.Circle;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.view.pst.PSTricksConstants;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestParsingPscircle extends TestPSTParser {
	@Test
	public void testUnit() {
		parser("\\psset{unit=4}\\pscircle(2,3cm){5}");
		final Circle cir = getShapeAt(0);
		assertEquals(4d * 2d * Shape.PPC - 5d * Shape.PPC, cir.getX(), 0.0001);
		assertEquals(-3d * Shape.PPC + 5d * Shape.PPC, cir.getY(), 0.0001);
		assertEquals(2d * 5d * Shape.PPC, cir.getWidth(), 0.0001);
	}

	@Test
	public void testCoordinatesPt() {
		parser("\\pscircle(35pt,20pt){10pt}");
		final Circle cir = getShapeAt(0);
		assertEquals(35d * Shape.PPC / PSTricksConstants.CM_VAL_PT - 10d * Shape.PPC / PSTricksConstants.CM_VAL_PT, cir.getPosition().getX(), 0.001);
		assertEquals((20d * Shape.PPC / PSTricksConstants.CM_VAL_PT - 10d * Shape.PPC / PSTricksConstants.CM_VAL_PT) * -1d, cir.getPosition().getY(), 0.001);
		assertEquals(10d * Shape.PPC / PSTricksConstants.CM_VAL_PT * 2d, cir.getWidth(), 0.0000001);
		assertEquals(10d * Shape.PPC / PSTricksConstants.CM_VAL_PT * 2d, cir.getHeight(), 0.0000001);
	}

	@Test
	public void testCoordinatesMm() {
		parser("\\pscircle(350mm,200mm){10mm}");
		final Circle cir = getShapeAt(0);
		assertEquals(35d * Shape.PPC - 1d * Shape.PPC, cir.getPosition().getX(), 0.001);
		assertEquals((20d * Shape.PPC - 1d * Shape.PPC) * -1d, cir.getPosition().getY(), 0.001);
		assertEquals(1d * Shape.PPC * 2d, cir.getWidth(), 0.0000001);
		assertEquals(1d * Shape.PPC * 2d, cir.getHeight(), 0.0000001);
	}

	@Test
	public void testCoordinatesInch() {
		parser("\\pscircle(35in,20in){1.2in}");
		final Circle cir = getShapeAt(0);
		assertEquals(35d * Shape.PPC / 2.54 - 1.2 * Shape.PPC / 2.54, cir.getPosition().getX(), 0.001);
		assertEquals((20d * Shape.PPC / 2.54 - 1.2 * Shape.PPC / 2.54) * -1d, cir.getPosition().getY(), 0.001);
		assertEquals(1.2 * Shape.PPC / 2.54 * 2d, cir.getWidth(), 0.0000001);
		assertEquals(1.2 * Shape.PPC / 2.54 * 2d, cir.getHeight(), 0.0000001);
	}

	@Test
	public void testCoordinatesCm() {
		parser("\\pscircle(35cm,20cm){.5cm}");
		final Circle cir = getShapeAt(0);
		assertEquals(35d * Shape.PPC - .5 * Shape.PPC, cir.getPosition().getX(), 0.001);
		assertEquals((20d * Shape.PPC - .5 * Shape.PPC) * -1d, cir.getPosition().getY(), 0.001);
		assertEquals(.5 * Shape.PPC * 2d, cir.getWidth(), 0.0000001);
		assertEquals(.5 * Shape.PPC * 2d, cir.getHeight(), 0.0000001);
	}

	@Test
	public void testCoordinatesRadius() {
		parser("\\pscircle(35,20){10}");
		final Circle cir = getShapeAt(0);
		assertEquals(35d * Shape.PPC - 10d * Shape.PPC, cir.getPosition().getX(), 0.001);
		assertEquals((20d * Shape.PPC - 10d * Shape.PPC) * -1d, cir.getPosition().getY(), 0.001);
		assertEquals(10d * Shape.PPC * 2d, cir.getWidth(), 0.0000001);
		assertEquals(10d * Shape.PPC * 2d, cir.getHeight(), 0.0000001);
	}

	@Test
	public void testFloatSigns() {
		parser("\\pscircle(+++35.5,--50.5){--+12}");
		final Circle cir = getShapeAt(0);
		assertEquals(35.5 * Shape.PPC - 12d * Shape.PPC, cir.getPosition().getX(), 0.001);
		assertEquals((50.5 * Shape.PPC - 12d * Shape.PPC) * -1d, cir.getPosition().getY(), 0.001);
		assertEquals(12d * Shape.PPC * 2d, cir.getWidth(), 0.0000001);
		assertEquals(12d * Shape.PPC * 2d, cir.getHeight(), 0.0000001);
	}

	@Test
	public void testNegativeRadius() {
		parser("\\pscircle(0,0){-1}");
		final Circle cir = getShapeAt(0);
		assertThat(cir.getWidth(), greaterThan(0d));
		assertThat(cir.getHeight(), greaterThan(0d));
	}

	@Test
	public void testParse2CoordinatesFloat2() {
		parser("\\pscircle(35.5,50.5){1.25}");
		final Circle cir = getShapeAt(0);
		assertEquals(35.5 * Shape.PPC - 1.25 * Shape.PPC, cir.getPosition().getX(), 0.001);
		assertEquals((50.5 * Shape.PPC - 1.25 * Shape.PPC) * -1d, cir.getPosition().getY(), 0.001);
		assertEquals(1.25 * Shape.PPC * 2d, cir.getWidth(), 0.0000001);
		assertEquals(1.25 * Shape.PPC * 2d, cir.getHeight(), 0.0000001);
	}

	@Test
	public void testCoordinatesMissing() {
		parser("\\pscircle(,){1}");
		final Circle cir = getShapeAt(0);
		assertEquals(Shape.PPC - 1d * Shape.PPC, cir.getPosition().getX(), 0.0000001);
		assertEquals((Shape.PPC - 1d * Shape.PPC) * -1d, cir.getPosition().getY(), 0.0000001);
		assertEquals(1d * Shape.PPC * 2d, cir.getWidth(), 0.0000001);
		assertEquals(1d * Shape.PPC * 2d, cir.getHeight(), 0.0000001);
	}
}
