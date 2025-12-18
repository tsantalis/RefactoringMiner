package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.FillingStyle;
import net.sf.latexdraw.model.api.shape.Plot;
import net.sf.latexdraw.model.api.shape.PlotStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestParsingPlotCommands extends TestPSTParser {
	@Test
	public void testPsplot() {
		parser("\\psplot{0}{720}{x sin}");
		final Plot plot = getShapeAt(0);
		assertEquals(0d, plot.getPlotMinX(), 0.00001);
		assertEquals(720d, plot.getPlotMaxX(), 0.00001);
		assertEquals("x sin", plot.getPlotEquation());
	}

	@Test
	public void testPsplotNbPoints() {
		parser("\\psplot[plotpoints=213]{0}{720}{x sin}");
		final Plot plot = getShapeAt(0);
		assertEquals(213, plot.getNbPlottedPoints());
	}

	@ParameterizedTest
	@EnumSource(value = PlotStyle.class)
	public void testPsplotPlotStyle(final PlotStyle style) {
		parser("\\psplot[plotstyle=" + style.getPSTToken() + "]{0}{720}{x sin}");
		final Plot plot = getShapeAt(0);
		assertEquals(style, plot.getPlotStyle());
	}

	@Test
	public void testPsplotXUnit() {
		parser("\\psplot[xunit=0.1]{0}{720}{x sin}");
		final Plot plot = getShapeAt(0);
		assertEquals(0.1, plot.getXScale(), 0.00001);
	}

	@Test
	public void testPsplotYUnit() {
		parser("\\psplot[yunit=0.1]{0}{720}{x sin}");
		final Plot plot = getShapeAt(0);
		assertEquals(0.1, plot.getYScale(), 0.00001);
	}

	@Test
	public void testPsplotStar() {
		parser("\\psplot*[plotpoints=200]{0}{720}{x sin}");
		final Plot plot = getShapeAt(0);
		assertEquals(0d, plot.getPlotMinX(), 0.00001);
		assertEquals(720d, plot.getPlotMaxX(), 0.00001);
		assertEquals("x sin", plot.getPlotEquation());
		assertEquals(FillingStyle.PLAIN, plot.getFillingStyle());
	}

	@Test
	public void testPsplotPolar() {
		parser("\\psplot[polarplot=true]{0}{720}{x sin}");
		final Plot plot = getShapeAt(0);
		assertTrue(plot.isPolar());
	}

	@Test
	public void testPsplotNotPolar() {
		parser("\\psplot[polarplot=false]{0}{720}{x sin}");
		final Plot plot = getShapeAt(0);
		assertFalse(plot.isPolar());
	}
}
