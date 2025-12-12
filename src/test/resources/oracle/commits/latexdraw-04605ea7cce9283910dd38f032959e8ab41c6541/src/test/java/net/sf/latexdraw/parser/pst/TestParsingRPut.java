package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.Rectangle;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.model.api.shape.Text;
import net.sf.latexdraw.model.api.shape.TextPosition;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestParsingRPut extends TestPSTParser {
	@Test
	public void testRefPointCombotbr() {
		parser("\\rput[t](0,0){\\rput[br](2,2){coucou}}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.BOT_RIGHT, txt.getTextPosition());
	}

	@Test
	public void testRputPosition() {
		parser("\\rput(1,2){coucou}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("coucou", text.getText());
		assertEquals(1d * Shape.PPC, text.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, text.getPosition().getY(), 0.001);
	}

	@Test
	public void testRefPointNone() {
		parser("\\rput(10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.CENTER, txt.getTextPosition());
	}

	@Test
	@Ignore("BASE not supported yet")
	public void testRefPointB() {
		parser("\\rput[B](10,20){coucou}");
	}

	@Test
	@Ignore("Br not supported yet")
	public void testRefPointBr() {
		parser("\\rput[Br](10,20){coucou}");
	}

	@Test
	@Ignore("Bl not supported yet")
	public void testRefPointBl() {
		parser("\\rput[Bl](10,20){coucou}");
	}

	@Test
	public void testRefPointr() {
		parser("\\rput[r](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.RIGHT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	public void testRefPointl() {
		parser("\\rput[l](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.LEFT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	public void testRefPointtr() {
		parser("\\rput[tr](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.TOP_RIGHT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	public void testRefPointb() {
		parser("\\rput[b](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.BOT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	public void testRefPointt() {
		parser("\\rput[t](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.TOP, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	public void testRefPointtl() {
		parser("\\rput[tl](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.TOP_LEFT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	public void testRefPointbl() {
		parser("\\rput[bl](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.BOT_LEFT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	public void testRefPointbr() {
		parser("\\rput[br](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.BOT_RIGHT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	public void testDoubleRputRotationMustNotRotateOtherShapes() {
		parser("\\rput{10}(0,0){\\rput{80}(0,0){coucou}}\\psframe(10,10)");
		final Text txt = getShapeAt(0);
		final Rectangle rec = getShapeAt(1);
		assertEquals(Math.toRadians(-90), txt.getRotationAngle(), 0.001);
		assertEquals(0d, rec.getRotationAngle(), 0.001);
	}

	@Test
	public void testTripleRputRotationWithStar() {
		parser("\\rput{10}(0,0){\\rput{*30}(0,0){\\rput{50}(0,0){coucou}}}");
		final Text txt = getShapeAt(0);
		assertEquals(-80d, Math.toDegrees(txt.getRotationAngle()), 0.001);
	}

	@Test
	public void testDoubleRputRotation() {
		parser("\\rput{10}(0,0){\\rput{80}(0,0){coucou}}");
		final Text txt = getShapeAt(0);
		assertEquals(Math.toRadians(-90), txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testDoubleRputPosition() {
		parser("\\rput(1,2){\\rput(2,3){coucou}}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(3d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-5d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	public void testRPutCoordStarFloatText() {
		parser("\\rput{*-90.8929}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(90.8929), txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordStarSignedIntText() {
		parser("\\rput{*-++-90}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-90d), txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordStarIntText() {
		parser("\\rput{*90}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-90d), txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordRotationRText() {
		parser("\\rput{R}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-270d), txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordRotationDText() {
		parser("\\rput{D}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-180d), txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordRotationLText() {
		parser("\\rput{L}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-90d), txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordRotationUText() {
		parser("\\rput{U}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(0d, txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordRotationEText() {
		parser("\\rput{E}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-270d), txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordRotationSText() {
		parser("\\rput{S}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-180d), txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordRotationWText() {
		parser("\\rput{W}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-90d), txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordRotationNText() {
		parser("\\rput{N}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(0d, txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordRotationDoubleText() {
		parser("\\rput{-10.0}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(10d), txt.getRotationAngle(), 0.001);
	}

	@Test
	public void testRPutCoordText() {
		parser("\\rput(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}
}
