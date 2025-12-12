package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.Rectangle;
import net.sf.latexdraw.model.api.shape.Text;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestParsingPSRotate extends TestPSTParser {
	@Test
	public void testPSRotation0Angle() {
		parser("\\psrotate(1,2){0}{coucou}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("coucou", text.getText());
		assertEquals(0d, text.getRotationAngle(), 0.001);
		assertEquals(0d, text.getPosition().getX(), 0.001);
		assertEquals(0d, text.getPosition().getY(), 0.001);
	}

	@Test
	public void testPSRotation10Angle() {
		parser("\\psrotate(1,1){10}{\\psframe(2,2)}\\psframe(2,2)");
		assertEquals(2, parsedShapes.size());
		final Rectangle rec = getShapeAt(0);
		assertEquals(-10d, Math.toDegrees(rec.getRotationAngle()), 0.001);
		assertEquals(parsedShapes.get(1).getGravityCentre(), rec.getGravityCentre());
	}

	@Test
	public void testPSRotationNegAngle() {
		parser("\\psrotate(1,2){-110.2}{\\psframe(2,4)}\\psframe(2,4)");
		assertEquals(2, parsedShapes.size());
		final Rectangle rec = getShapeAt(0);
		assertEquals(110.2, Math.toDegrees(rec.getRotationAngle()), 0.001);
		assertEquals(parsedShapes.get(1).getGravityCentre(), rec.getGravityCentre());
	}

	@Test
	public void testPSRotationSeveralAngles() {
		parser("\\psrotate(1,1){10}{\\psrotate(1,1){30}{\\psframe(2,2)}}\\psframe(2,2)");
		assertEquals(2, parsedShapes.size());
		final Rectangle rec = getShapeAt(0);
		assertEquals(-40d, Math.toDegrees(rec.getRotationAngle()), 0.001);
		assertEquals(parsedShapes.get(1).getGravityCentre(), rec.getGravityCentre());
	}
}
