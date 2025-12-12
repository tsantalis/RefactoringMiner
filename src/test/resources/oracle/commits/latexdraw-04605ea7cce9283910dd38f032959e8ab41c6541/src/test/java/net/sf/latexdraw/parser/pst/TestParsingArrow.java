package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.ArrowStyle;
import net.sf.latexdraw.model.api.shape.ArrowableSingleShape;
import net.sf.latexdraw.model.api.shape.Shape;
import net.sf.latexdraw.util.Tuple;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(Theories.class)
public class TestParsingArrow extends TestPSTParser {
	@DataPoints
	public static Tuple<String, String>[] cmds() {
		return new Tuple[] {new Tuple<>("\\psline", "(1,1)(2,2)"), new Tuple<>("\\psbezier", "(1,2)(3,4)(5,6)(7,8)"),
			new Tuple<>("\\psaxes", "(0,0)(0,0)(3,4)")};
	}

	@Theory
	public void testParamArrowScaleDimDim(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=<->, arrowscale=0.1 cm 3]" + cmd.b);
	}

	@Theory
	public void testParamArrowScaleDim(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=<->, arrowscale=2.55]" + cmd.b);
	}

	@Theory
	public void testParamArrowtRBracketLength(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=<->, rbracketlength=2.55]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(2.55, line.getRBracketNum(), 0.0001);
	}

	@Theory
	public void testParamArrowBracketLength(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=<->, bracketlength=2.55]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(2.55, line.getBracketNum(), 0.0001);
	}

	@Theory
	public void testParamArrowInset(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=<->, arrowinset=2.5]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(2.5, line.getArrowInset(), 0.0001);
	}

	@Theory
	public void testParamArrowlength(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=<->, arrowlength=1.5]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(1.5, line.getArrowLength(), 0.0001);
	}

	@Theory
	public void testParamArrowtbarDimNum(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=<->, tbarsize=1.5cm 3]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(1.5 * Shape.PPC, line.getTBarSizeDim(), 0.0001);
		assertEquals(3d, line.getTBarSizeNum(), 0.0001);
	}

	@Theory
	public void testParamArrowsizeDimNum(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=<->, arrowsize=1.5cm 3]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(1.5 * Shape.PPC, line.getArrowSizeDim(), 0.0001);
		assertEquals(3d, line.getArrowSizeNum(), 0.0001);
	}

	@Theory
	public void testParamArrowsizeDim(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=<->, arrowsize=2cm]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(2d * Shape.PPC, line.getArrowSizeDim(), 0.0001);
		assertEquals(0d, line.getArrowSizeNum(), 0.0001);
	}

	@Theory
	public void testParamArrowsArrows(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=<->]{-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testParamArrowsRRBracker(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=)-]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.RIGHT_ROUND_BRACKET, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testParamArrowsBarEnd(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=|*-]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.BAR_END, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testParamArrowsRLBracker(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=(-]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.LEFT_ROUND_BRACKET, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testParamArrowsSLBracket(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=[-]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.LEFT_SQUARE_BRACKET, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testParamArrows(final Tuple<String, String> cmd) {
		parser(cmd.a + "[arrows=<->]" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.LEFT_ARROW, line.getArrowStyle(0));
		assertEquals(ArrowStyle.RIGHT_ARROW, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneC(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-C}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.SQUARE_END, line.getArrowStyle(1));
	}

	@Theory
	public void testArrowCNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{C-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.SQUARE_END, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNonecc(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-cc}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.ROUND_IN, line.getArrowStyle(1));
	}

	@Theory
	public void testCcNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{cc-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.ROUND_IN, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNonec(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-c}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.ROUND_END, line.getArrowStyle(1));
	}

	@Theory
	public void testArrowcNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{c-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.ROUND_END, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneDiskIn(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-**}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.DISK_IN, line.getArrowStyle(1));
	}

	@Theory
	public void testDiskInNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{**-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.DISK_IN, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneCircleIn(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-oo}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.CIRCLE_IN, line.getArrowStyle(1));
	}

	@Theory
	public void testCircleInNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{oo-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.CIRCLE_IN, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneDiskEnd(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-*}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.DISK_END, line.getArrowStyle(1));
	}

	@Theory
	public void testDiskEndNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{*-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.DISK_END, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneCircleEnd(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-o}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.CIRCLE_END, line.getArrowStyle(1));
	}

	@Theory
	public void testCircleEndNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{o-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.CIRCLE_END, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneRightRoundBracket(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-(}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.LEFT_ROUND_BRACKET, line.getArrowStyle(1));
	}

	@Theory
	public void testLeftRoundBracketNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{)-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.RIGHT_ROUND_BRACKET, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneLeftRoundBracket(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-)}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.RIGHT_ROUND_BRACKET, line.getArrowStyle(1));
	}

	@Theory
	public void testRoundRightBracketNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{(-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.LEFT_ROUND_BRACKET, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneRightSquareBracket(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-[}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.LEFT_SQUARE_BRACKET, line.getArrowStyle(1));
	}

	@Theory
	public void testSquareLeftBracketNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{]-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.RIGHT_SQUARE_BRACKET, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneLeftSquareBracket(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-]}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.RIGHT_SQUARE_BRACKET, line.getArrowStyle(1));
	}

	@Theory
	public void testSquareRightBracketTbarEnd(final Tuple<String, String> cmd) {
		parser(cmd.a + "{[-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.LEFT_SQUARE_BRACKET, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneTbarEnd(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-|*}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.BAR_END, line.getArrowStyle(1));
	}

	@Theory
	public void testTbarEndNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{|*-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.BAR_END, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneTbar(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-|}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.BAR_IN, line.getArrowStyle(1));
	}

	@Theory
	public void testTbarNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{|-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.BAR_IN, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testDbleArrowLeftDbleArrowRight(final Tuple<String, String> cmd) {
		parser(cmd.a + "{<<->>}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.LEFT_DBLE_ARROW, line.getArrowStyle(0));
		assertEquals(ArrowStyle.RIGHT_DBLE_ARROW, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testArrowheadRightArrowheadLeft(final Tuple<String, String> cmd) {
		parser(cmd.a + "{>-<}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.RIGHT_ARROW, line.getArrowStyle(0));
		assertEquals(ArrowStyle.LEFT_ARROW, line.getArrowStyle(1));
	}

	@Theory
	public void testNoneArrowheadRight(final Tuple<String, String> cmd) {
		parser(cmd.a + "{->}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.RIGHT_ARROW, line.getArrowStyle(1));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
	}

	@Theory
	public void testNoneArrowheadLeft(final Tuple<String, String> cmd) {
		parser(cmd.a + "{-<}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.LEFT_ARROW, line.getArrowStyle(1));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(0));
	}

	@Theory
	public void testArrowheadRightNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{>-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.RIGHT_ARROW, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}

	@Theory
	public void testArrowheadLeftNone(final Tuple<String, String> cmd) {
		parser(cmd.a + "{<-}" + cmd.b);
		final ArrowableSingleShape line = (ArrowableSingleShape) parsedShapes.get(0);
		assertEquals(ArrowStyle.LEFT_ARROW, line.getArrowStyle(0));
		assertEquals(ArrowStyle.NONE, line.getArrowStyle(1));
	}
}
