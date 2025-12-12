package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.data.DoubleData;
import net.sf.latexdraw.model.api.shape.AxesStyle;
import net.sf.latexdraw.model.api.shape.Axes;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.model.api.shape.PlottingStyle;
import net.sf.latexdraw.model.api.shape.TicksStyle;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(Theories.class)
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

	@Theory
	public void testAxesstyle(final AxesStyle style) {
		parser("\\psaxes[axesstyle=" + style.getPSTToken() + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(style, axes.getAxesStyle());
	}

	@Theory
	public void testShowOrigin(final boolean origin) {
		parser("\\psaxes[showorigin=" + origin + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(origin, axes.isShowOrigin());
	}

	@Theory
	public void testTickStyle(final TicksStyle style) {
		parser("\\psaxes[tickstyle=" + style.getPSTToken() + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(style, axes.getTicksStyle());
	}

	@Theory
	public void testTickStyle(final PlottingStyle style) {
		parser("\\psaxes[ticks=" + style.getPSTToken() + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(style, axes.getTicksDisplayed());
	}

	@Theory
	public void testLabelsStyle(final PlottingStyle style) {
		parser("\\psaxes[labels=" + style.getPSTToken() + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(style, axes.getLabelsDisplayed());
	}

	@Theory
	public void testParamDx(@DoubleData(vals = {2d, 3.5}) final double dx) {
		parser("\\psaxes[Dx=" + dx + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(dx, axes.getIncrementX(), 0.00001);
	}

	@Theory
	public void testParamDY(@DoubleData(vals = {2d, 3.5}) final double dy) {
		parser("\\psaxes[Dy=" + dy + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(dy, axes.getIncrementY(), 0.00001);
	}

	@Theory
	public void testParamdx(@DoubleData(vals = {2d, 3.5}) final double dx) {
		parser("\\psaxes[dx=" + dx + " cm](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(dx, axes.getDistLabelsX(), 0.00001);
	}

	@Theory
	public void testParamdy(@DoubleData(vals = {2d, 3.5}) final double dy) {
		parser("\\psaxes[dy=" + dy + " cm](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(dy, axes.getDistLabelsY(), 0.00001);
	}

	@Theory
	public void testOx(@DoubleData(vals = {2d, 3.5}) final double ox) {
		parser("\\psaxes[Ox=" + ox + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(ox, axes.getOriginX(), 0.00001);
	}

	@Theory
	public void testOy(@DoubleData(vals = {2d, 3.5}) final double oy) {
		parser("\\psaxes[Oy=" + oy + "](0,0)(0,0)(2,2)");
		final Axes axes = getShapeAt(0);
		assertEquals(oy, axes.getOriginY(), 0.00001);
	}
}
