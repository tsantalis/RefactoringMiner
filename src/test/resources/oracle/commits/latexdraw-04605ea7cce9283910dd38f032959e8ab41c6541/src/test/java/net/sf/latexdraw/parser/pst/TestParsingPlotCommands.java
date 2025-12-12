package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.FillingStyle;
import net.sf.latexdraw.model.api.shape.Plot;
import net.sf.latexdraw.model.api.shape.PlotStyle;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(Theories.class)
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

	@Theory
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

	@Theory
	public void testPsplotPolar(final boolean polar) {
		parser("\\psplot[polarplot=" + polar + "]{0}{720}{x sin}");
		final Plot plot = getShapeAt(0);
		assertEquals(polar, plot.isPolar());
	}
}
