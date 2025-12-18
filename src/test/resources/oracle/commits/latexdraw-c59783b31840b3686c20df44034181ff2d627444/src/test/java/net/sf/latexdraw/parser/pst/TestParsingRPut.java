package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.Rectangle;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.model.api.shape.Text;
import net.sf.latexdraw.model.api.shape.TextPosition;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestParsingRPut extends TestPSTParser {
	@Test
	void testRefPointCombotbr() {
		parser("\\rput[t](0,0){\\rput[br](2,2){coucou}}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.BOT_RIGHT, txt.getTextPosition());
	}

	@Test
	void testRputPosition() {
		parser("\\rput(1,2){coucou}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("coucou", text.getText());
		assertEquals(1d * Shape.PPC, text.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, text.getPosition().getY(), 0.001);
	}

	@Test
	void testRefPointNone() {
		parser("\\rput(10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.CENTER, txt.getTextPosition());
	}

	@Test
	@Disabled("BASE not supported yet")
	void testRefPointB() {
		parser("\\rput[B](10,20){coucou}");
	}

	@Test
	@Disabled("Br not supported yet")
	void testRefPointBr() {
		parser("\\rput[Br](10,20){coucou}");
	}

	@Test
	@Disabled("Bl not supported yet")
	void testRefPointBl() {
		parser("\\rput[Bl](10,20){coucou}");
	}

	@Test
	void testRefPointr() {
		parser("\\rput[r](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.RIGHT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	void testRefPointl() {
		parser("\\rput[l](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.LEFT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	void testRefPointtr() {
		parser("\\rput[tr](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.TOP_RIGHT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	void testRefPointb() {
		parser("\\rput[b](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.BOT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	void testRefPointt() {
		parser("\\rput[t](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.TOP, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	void testRefPointtl() {
		parser("\\rput[tl](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.TOP_LEFT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	void testRefPointbl() {
		parser("\\rput[bl](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.BOT_LEFT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	void testRefPointbr() {
		parser("\\rput[br](10,20){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals(TextPosition.BOT_RIGHT, txt.getTextPosition());
		assertEquals(10d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-20d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	void testDoubleRputRotationMustNotRotateOtherShapes() {
		parser("\\rput{10}(0,0){\\rput{80}(0,0){coucou}}\\psframe(10,10)");
		final Text txt = getShapeAt(0);
		final Rectangle rec = getShapeAt(1);
		assertEquals(Math.toRadians(-90), txt.getRotationAngle(), 0.001);
		assertEquals(0d, rec.getRotationAngle(), 0.001);
	}

	@Test
	void testTripleRputRotationWithStar() {
		parser("\\rput{10}(0,0){\\rput{*30}(0,0){\\rput{50}(0,0){coucou}}}");
		final Text txt = getShapeAt(0);
		assertEquals(-80d, Math.toDegrees(txt.getRotationAngle()), 0.001);
	}

	@Test
	void testDoubleRputRotation() {
		parser("\\rput{10}(0,0){\\rput{80}(0,0){coucou}}");
		final Text txt = getShapeAt(0);
		assertEquals(Math.toRadians(-90), txt.getRotationAngle(), 0.001);
	}

	@Test
	void testDoubleRputPosition() {
		parser("\\rput(1,2){\\rput(2,3){coucou}}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(3d * Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-5d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}

	@Test
	void testRPutCoordStarFloatText() {
		parser("\\rput{*-90.8929}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(90.8929), txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordStarSignedIntText() {
		parser("\\rput{*-++-90}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-90d), txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordStarIntText() {
		parser("\\rput{*90}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-90d), txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordRotationRText() {
		parser("\\rput{R}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-270d), txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordRotationDText() {
		parser("\\rput{D}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-180d), txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordRotationLText() {
		parser("\\rput{L}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-90d), txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordRotationUText() {
		parser("\\rput{U}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(0d, txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordRotationEText() {
		parser("\\rput{E}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-270d), txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordRotationSText() {
		parser("\\rput{S}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-180d), txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordRotationWText() {
		parser("\\rput{W}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(-90d), txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordRotationNText() {
		parser("\\rput{N}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(0d, txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordRotationDoubleText() {
		parser("\\rput{-10.0}(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
		assertEquals(Math.toRadians(10d), txt.getRotationAngle(), 0.001);
	}

	@Test
	void testRPutCoordText() {
		parser("\\rput(1,2){coucou}");
		final Text txt = getShapeAt(0);
		assertEquals("coucou", txt.getText());
		assertEquals(Shape.PPC, txt.getPosition().getX(), 0.001);
		assertEquals(-2d * Shape.PPC, txt.getPosition().getY(), 0.001);
	}
}
