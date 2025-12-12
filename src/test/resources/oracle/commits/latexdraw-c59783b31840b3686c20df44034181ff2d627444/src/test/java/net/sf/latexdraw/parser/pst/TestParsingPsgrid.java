package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.Grid;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.view.latex.DviPsColors;
import net.sf.latexdraw.view.pst.PSTricksConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestParsingPsgrid extends TestPSTParser {
	@Test
	public void testGridXUnitCM() {
		parser("\\psgrid[xunit=20in](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(1d, grid.getUnit(), 0.00001);
	}

	@Test
	public void testGridXUnit() {
		parser("\\psgrid[xunit=0.3](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(1d, grid.getUnit(), 0.00001);
	}

	@Test
	public void testGridYUnitin() {
		parser("\\psgrid[yunit=20in](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(1d, grid.getUnit(), 0.00001);
	}

	@Test
	public void testGridYUnit() {
		parser("\\psgrid[yunit=0.3](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(1d, grid.getUnit(), 0.00001);
	}

	@Test
	public void testGridUnitin() {
		parser("\\psgrid[unit=20in](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(20d / PSTricksConstants.INCH_VAL_CM, grid.getUnit(), 0.00001);
	}

	@Test
	public void testGridUnit() {
		parser("\\psgrid[unit=0.3](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(0.3, grid.getUnit(), 0.00001);
	}

	@Test
	public void testGridSubGridWidthin() {
		parser("\\psgrid[subgridwidth=20in](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(20d * Shape.PPC / PSTricksConstants.INCH_VAL_CM, grid.getSubGridWidth(), 0.00001);
	}

	@Test
	public void testGridSubGridWidth() {
		parser("\\psgrid[subgridwidth=0.3](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(0.3 * Shape.PPC, grid.getSubGridWidth(), 0.00001);
	}

	@Test
	public void testGridLabelsin() {
		parser("\\psgrid[gridlabels=20in](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals((int) (20d * Shape.PPC / PSTricksConstants.INCH_VAL_CM), grid.getLabelsSize());

	}

	@Test
	public void testGridLabels() {
		parser("\\psgrid[gridlabels=0.3](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals((int) (0.3 * Shape.PPC), grid.getLabelsSize());
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 3})
	public void testGridSubGridDiv(final int div) {
		parser("\\psgrid[subgriddiv=" + div + "](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(div, grid.getSubGridDiv());
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 3})
	public void testSubGridDots(final int div) {
		parser("\\psgrid[subgriddots=" + div + "](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(div, grid.getSubGridDots());
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 3})
	public void testGridDots(final int div) {
		parser("\\psgrid[griddots=" + div + "](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(div, grid.getGridDots());
	}

	@Test
	public void testSubGridLabelColor() {
		parser("\\psgrid[subgridcolor=green](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(DviPsColors.GREEN, grid.getSubGridColour());
	}

	@Test
	public void testGridLabelColor() {
		parser("\\psgrid[gridlabelcolor=green](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(DviPsColors.GREEN, grid.getGridLabelsColour());
	}

	@Test
	public void testGridColor() {
		parser("\\psgrid[gridcolor=green](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(DviPsColors.GREEN, grid.getLineColour());
	}

	@Test
	public void testGridWidthcm() {
		parser("\\psgrid[gridwidth=1.3cm](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(1.3 * Shape.PPC, grid.getGridWidth(), 0.001);

	}

	@Test
	public void testGridWidthin() {
		parser("\\psgrid[gridwidth=.3in](0,0)(0,0)(1,1)");
		final Grid grid = getShapeAt(0);
		assertEquals(.3 * Shape.PPC / PSTricksConstants.INCH_VAL_CM, grid.getGridWidth(), 0.001);
	}

	@Test
	public void testParse0CoordDoubleValue() {
		parser("\\begin{pspicture}(2.1,2.6)(5.6,5.5)\\psgrid\\end{pspicture}");
		final Grid grid = getShapeAt(0);
		assertEquals(0d, grid.getOriginX(), 0.0001);
		assertEquals(0d, grid.getOriginY(), 0.0001);
		assertEquals(2d, grid.getGridMinX(), 0.0001);
		assertEquals(3d, grid.getGridMinY(), 0.0001);
		assertEquals(6d, grid.getGridMaxX(), 0.0001);
		assertEquals(6d, grid.getGridMaxY(), 0.0001);
		assertTrue(grid.isXLabelSouth());
		assertTrue(grid.isYLabelWest());
	}

	@Test
	public void testParse0Coord() {
		parser("\\begin{pspicture}(2,2)(5,5)\\psgrid\\end{pspicture}");
		final Grid grid = getShapeAt(0);
		assertEquals(0d, grid.getOriginX(), 0.0001);
		assertEquals(0d, grid.getOriginY(), 0.0001);
		assertEquals(2d, grid.getGridMinX(), 0.0001);
		assertEquals(2d, grid.getGridMinY(), 0.0001);
		assertEquals(5d, grid.getGridMaxX(), 0.0001);
		assertEquals(5d, grid.getGridMaxY(), 0.0001);
		assertTrue(grid.isXLabelSouth());
		assertTrue(grid.isYLabelWest());
	}

	@Test
	public void testParse1Coord() {
		parser("\\psgrid(1,2)");
		final Grid grid = getShapeAt(0);
		assertEquals(0d, grid.getOriginX(), 0.0001);
		assertEquals(0d, grid.getOriginY(), 0.0001);
		assertEquals(0d, grid.getGridMinX(), 0.0001);
		assertEquals(0d, grid.getGridMinY(), 0.0001);
		assertEquals(1d, grid.getGridMaxX(), 0.0001);
		assertEquals(2d, grid.getGridMaxY(), 0.0001);
		assertTrue(grid.isXLabelSouth());
		assertTrue(grid.isYLabelWest());
	}

	@Test
	public void testParse2CoordInverted() {
		parser("\\psgrid(3,4)(1,2)");
		final Grid grid = getShapeAt(0);
		assertEquals(3d, grid.getOriginX(), 0.0001);
		assertEquals(4d, grid.getOriginY(), 0.0001);
		assertEquals(1d, grid.getGridMinX(), 0.0001);
		assertEquals(2d, grid.getGridMinY(), 0.0001);
		assertEquals(3d, grid.getGridMaxX(), 0.0001);
		assertEquals(4d, grid.getGridMaxY(), 0.0001);
		assertFalse(grid.isXLabelSouth());
		assertFalse(grid.isYLabelWest());
	}

	@Test
	public void testParse2Coord() {
		parser("\\psgrid(1,2)(3,4)");
		final Grid grid = getShapeAt(0);
		assertEquals(1d, grid.getOriginX(), 0.0001);
		assertEquals(2d, grid.getOriginY(), 0.0001);
		assertEquals(1d, grid.getGridMinX(), 0.0001);
		assertEquals(2d, grid.getGridMinY(), 0.0001);
		assertEquals(3d, grid.getGridMaxX(), 0.0001);
		assertEquals(4d, grid.getGridMaxY(), 0.0001);
		assertTrue(grid.isXLabelSouth());
		assertTrue(grid.isYLabelWest());
	}

	@Test
	public void testParse3CoordInverted() {
		parser("\\psgrid(0,-1)(3,4)(1,2)");
		final Grid grid = getShapeAt(0);
		assertEquals(0d, grid.getOriginX(), 0.0001);
		assertEquals(-1d, grid.getOriginY(), 0.0001);
		assertEquals(1d, grid.getGridStartX(), 0.0001);
		assertEquals(2d, grid.getGridStartY(), 0.0001);
		assertEquals(3d, grid.getGridEndX(), 0.0001);
		assertEquals(4d, grid.getGridEndY(), 0.0001);
		assertFalse(grid.isXLabelSouth());
		assertFalse(grid.isYLabelWest());
	}

	@Test
	public void testParse3Coord() {
		parser("\\psgrid(0,-1)(1,2)(3,4)");
		final Grid grid = getShapeAt(0);
		assertEquals(0d, grid.getOriginX(), 0.0001);
		assertEquals(-1d, grid.getOriginY(), 0.0001);
		assertEquals(1d, grid.getGridStartX(), 0.0001);
		assertEquals(2d, grid.getGridStartY(), 0.0001);
		assertEquals(3d, grid.getGridEndX(), 0.0001);
		assertEquals(4d, grid.getGridEndY(), 0.0001);
		assertTrue(grid.isXLabelSouth());
		assertTrue(grid.isYLabelWest());
	}
}
