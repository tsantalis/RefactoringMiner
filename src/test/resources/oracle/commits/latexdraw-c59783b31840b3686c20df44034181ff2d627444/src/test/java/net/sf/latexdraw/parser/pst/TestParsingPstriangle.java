package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.model.api.shape.Triangle;
import net.sf.latexdraw.view.pst.PSTricksConstants;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestParsingPstriangle extends TestPSTParser {
	@Test
	public void testGangle() {
		parser("\\pstriangle[gangle=180](2,2)(4,1)");
		final Triangle tri = getShapeAt(0);
		assertEquals(0d, tri.getPosition().getX(), 0.001);
		assertEquals(-1d * Shape.PPC, tri.getPosition().getY(), 0.001);
		assertEquals(4d * Shape.PPC, tri.getWidth(), 0.001);
		assertEquals(1d * Shape.PPC, tri.getHeight(), 0.001);
		assertEquals(Math.toRadians(180d), tri.getRotationAngle(), 0.001);
	}

	@Test
	public void testHeightNegative() {
		parser("\\pstriangle(35,-10)");
		final Triangle tri = getShapeAt(0);
		assertEquals(10d * Shape.PPC, tri.getHeight(), 0.001);
		assertEquals(Math.PI, tri.getRotationAngle(), 0.001);
	}

	@Test
	public void testCoordinatesPt() {
		parser("\\pstriangle(0,0)(35pt,20pt)");
		final Triangle tri = getShapeAt(0);
		assertEquals(-35d / 2d * Shape.PPC / PSTricksConstants.CM_VAL_PT, tri.getPosition().getX(), 0.001);
		assertEquals(0d, tri.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC / PSTricksConstants.CM_VAL_PT, tri.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC / PSTricksConstants.CM_VAL_PT, tri.getHeight(), 0.001);
	}

	@Test
	public void testCoordinatesMm() {
		parser("\\pstriangle(0,0)(350mm,200mm)");
		final Triangle tri = getShapeAt(0);
		assertEquals(-35d / 2d * Shape.PPC, tri.getPosition().getX(), 0.001);
		assertEquals(0d, tri.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC, tri.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC, tri.getHeight(), 0.001);
	}

	@Test
	public void testCoordinatesInch() {
		parser("\\pstriangle(0,0)(35in,20in)");
		final Triangle tri = getShapeAt(0);
		assertEquals(-35d / 2d * Shape.PPC / 2.54, tri.getPosition().getX(), 0.001);
		assertEquals(0d, tri.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC / 2.54, tri.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC / 2.54, tri.getHeight(), 0.001);
	}

	@Test
	public void testCoordinatesCm() {
		parser("\\pstriangle(0,0)(35cm,20cm)");
		final Triangle tri = getShapeAt(0);
		assertEquals(-35d / 2d * Shape.PPC, tri.getPosition().getX(), 0.001);
		assertEquals(0d, tri.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC, tri.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC, tri.getHeight(), 0.001);
	}

	@Test
	public void testParse1Coordinates() {
		parser("\\pstriangle(35,20)");
		final Triangle tri = getShapeAt(0);
		assertEquals(-35d / 2d * Shape.PPC, tri.getPosition().getX(), 0.001);
		assertEquals(0d, tri.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC, tri.getWidth(), 0.001);
		assertEquals(20d * Shape.PPC, tri.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesInt() {
		parser("\\pstriangle(10,20)(35,50)");
		final Triangle tri = getShapeAt(0);
		assertEquals(10d * Shape.PPC - 35d / 2d * Shape.PPC, tri.getPosition().getX(), 0.001);
		assertEquals(20d * -Shape.PPC, tri.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC, tri.getWidth(), 0.001);
		assertEquals(50d * Shape.PPC, tri.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloatSigns2() {
		parser("\\pstriangle(-+.5,+-5)(+++35.5,--50.5)");
		final Triangle tri = getShapeAt(0);
		assertEquals(-.5 * Shape.PPC - 35.5 / 2d * Shape.PPC, tri.getPosition().getX(), 0.001);
		assertEquals(-5d * -Shape.PPC, tri.getPosition().getY(), 0.001);
		assertEquals(35.5 * Shape.PPC, tri.getWidth(), 0.001);
		assertEquals(50.5 * Shape.PPC, tri.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloatSigns() {
		parser("\\pstriangle(-+-.5,+--.5)(+++35.5,--50.5)");
		final Triangle tri = getShapeAt(0);
		assertEquals(0.5 * Shape.PPC - 35.5 / 2d * Shape.PPC, tri.getPosition().getX(), 0.001);
		assertEquals(0.5 * -Shape.PPC, tri.getPosition().getY(), 0.001);
		assertEquals(35.5 * Shape.PPC, tri.getWidth(), 0.001);
		assertEquals(50.5 * Shape.PPC, tri.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloat2() {
		parser("\\pstriangle(0.5,.5)(35.5,50.5)");
		final Triangle tri = getShapeAt(0);
		assertEquals(0.5 * Shape.PPC - 35.5 / 2d * Shape.PPC, tri.getPosition().getX(), 0.001);
		assertEquals(0.5 * -Shape.PPC, tri.getPosition().getY(), 0.001);
		assertEquals(35.5 * Shape.PPC, tri.getWidth(), 0.001);
		assertEquals(50.5 * Shape.PPC, tri.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesFloat() {
		parser("\\pstriangle(10.5,20.5)(35.5,50.5)");
		final Triangle tri = getShapeAt(0);
		assertEquals(10.5 * Shape.PPC - 35.5 / 2d * Shape.PPC, tri.getPosition().getX(), 0.001);
		assertEquals(20.5 * -Shape.PPC, tri.getPosition().getY(), 0.001);
		assertEquals(35.5 * Shape.PPC, tri.getWidth(), 0.001);
		assertEquals(50.5 * Shape.PPC, tri.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesTwoFirstMissing() {
		parser("\\pstriangle(,)(35,50)");
		final Triangle tri = getShapeAt(0);
		assertEquals(Shape.PPC - 35d / 2d * Shape.PPC, tri.getPosition().getX(), 0.001);
		assertEquals(Shape.PPC * -1d, tri.getPosition().getY(), 0.001);
		assertEquals(35d * Shape.PPC, tri.getWidth(), 0.001);
		assertEquals(50d * Shape.PPC, tri.getHeight(), 0.001);
	}

	@Test
	public void testParse2CoordinatesTwoLastMissing() {
		parser("\\pstriangle(0,0)(,)");
		final Triangle tri = getShapeAt(0);
		assertEquals(-Shape.PPC / 2d, tri.getPosition().getX(), 0.001);
		assertEquals(0d, tri.getPosition().getY(), 0.001);
		assertEquals(Shape.PPC, tri.getWidth(), 0.001);
		assertEquals(Shape.PPC, tri.getHeight(), 0.001);
	}

	@Test
	public void testParse2WidthHeight0() {
		parser("\\pstriangle(0,0)(0,0)");
		final Triangle tri = getShapeAt(0);
		assertThat(tri.getWidth(), greaterThan(0d));
		assertThat(tri.getHeight(), greaterThan(0d));
	}
}
