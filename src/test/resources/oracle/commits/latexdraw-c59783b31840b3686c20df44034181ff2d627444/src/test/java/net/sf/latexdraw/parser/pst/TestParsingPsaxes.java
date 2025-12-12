package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.Axes;
import net.sf.latexdraw.model.api.shape.AxesStyle;
import net.sf.latexdraw.model.api.shape.PlottingStyle;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.model.api.shape.TicksStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestParsingPsaxes extends TestPSTParser {
	@Test
	public void testParse3Coord() {
		parser("\\psaxes(0,0)(0,0)(3,4)");
		final Axes axes = getShapeAt(0);
		assertEquals(0d, axes.getOriginX(), 0.0001);
		assertEquals(0d, axes.getOriginY(), 0.0001);
		assertEquals(0d, axes.getGridStartX(), 0.0001);
		assertEquals(0d, axes.getGridStartY(), 0.0001);
		assertEquals(3d, axes.getGridEndX(), 0.0001);
		assertEquals(4d, axes.getGridEndY(), 0.0001);
	}

	@Test
	public void testParse3Coord2() {
		parser("\\psaxes(0,0)(1,2)(3,4)");
		final Axes axes = getShapeAt(0);
		assertEquals(0d, axes.getOriginX(), 0.0001);
		assertEquals(0d, axes.getOriginY(), 0.0001);
		assertEquals(1d, axes.getGridStartX(), 0.0001);
		assertEquals(2d, axes.getGridStartY(), 0.0001);
		assertEquals(3d, axes.getGridEndX(), 0.0001);
		assertEquals(4d, axes.getGridEndY(), 0.0001);
	}

	@Test
	public void testParse1Coord() {
		parser("\\psaxes(1,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(0d, axes.getOriginX(), 0.0001);
		assertEquals(0d, axes.getOriginY(), 0.0001);
		assertEquals(0d, axes.getGridMinX(), 0.0001);
		assertEquals(0d, axes.getGridMinY(), 0.0001);
		assertEquals(1d, axes.getGridMaxX(), 0.0001);
		assertEquals(2d, axes.getGridMaxY(), 0.0001);
	}

	@Test
	public void testParse2Coord() {
		parser("\\psaxes(1,2)(3,4)");
		final Axes axes = getShapeAt(0);
		assertEquals(0d, axes.getOriginX(), 0.0001);
		assertEquals(0d, axes.getOriginY(), 0.0001);
		assertEquals(0d, axes.getGridMinX(), 0.0001);
		assertEquals(0d, axes.getGridMinY(), 0.0001);
		assertEquals(2d, axes.getGridMaxX(), 0.0001);
		assertEquals(2d, axes.getGridMaxY(), 0.0001);
		assertEquals(Shape.PPC, axes.getPosition().getX(), 0.0001);
		assertEquals(2d * Shape.PPC, axes.getPosition().getY(), 0.0001);
	}

	@ParameterizedTest
	@EnumSource(value = AxesStyle.class)
	public void testAxesstyle(final AxesStyle style) {
		parser("\\psaxes[axesstyle=" + style.getPSTToken() + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(style, axes.getAxesStyle());
	}

	@Test
	public void testShowOrigin() {
		parser("\\psaxes[showorigin=true](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertTrue(axes.isShowOrigin());
	}

	@Test
	public void testShowOriginNot() {
		parser("\\psaxes[showorigin=false](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertFalse(axes.isShowOrigin());
	}

	@ParameterizedTest
	@EnumSource(value = TicksStyle.class)
	public void testTickStyle(final TicksStyle style) {
		parser("\\psaxes[tickstyle=" + style.getPSTToken() + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(style, axes.getTicksStyle());
	}

	@ParameterizedTest
	@EnumSource(value = PlottingStyle.class)
	public void testTickStyle(final PlottingStyle style) {
		parser("\\psaxes[ticks=" + style.getPSTToken() + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(style, axes.getTicksDisplayed());
	}

	@ParameterizedTest
	@EnumSource(value = PlottingStyle.class)
	public void testLabelsStyle(final PlottingStyle style) {
		parser("\\psaxes[labels=" + style.getPSTToken() + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(style, axes.getLabelsDisplayed());
	}

	@ParameterizedTest
	@ValueSource(doubles = {2d, 3.5})
	public void testParamDx(final double dx) {
		parser("\\psaxes[Dx=" + dx + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(dx, axes.getIncrementX(), 0.00001);
	}

	@ParameterizedTest
	@ValueSource(doubles = {2d, 3.5})
	public void testParamDY(final double dy) {
		parser("\\psaxes[Dy=" + dy + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(dy, axes.getIncrementY(), 0.00001);
	}

	@ParameterizedTest
	@ValueSource(doubles = {2d, 3.5})
	public void testParamdx(final double dx) {
		parser("\\psaxes[dx=" + dx + " cm](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(dx, axes.getDistLabelsX(), 0.00001);
	}

	@ParameterizedTest
	@ValueSource(doubles = {2d, 3.5})
	public void testParamdy(final double dy) {
		parser("\\psaxes[dy=" + dy + " cm](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(dy, axes.getDistLabelsY(), 0.00001);
	}

	@ParameterizedTest
	@ValueSource(doubles = {2d, 3.5})
	public void testOx(final double ox) {
		parser("\\psaxes[Ox=" + ox + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(ox, axes.getOriginX(), 0.00001);
	}

	@ParameterizedTest
	@ValueSource(doubles = {2d, 3.5})
	public void testOy(final double oy) {
		parser("\\psaxes[Oy=" + oy + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(oy, axes.getOriginY(), 0.00001);
	}
}
