package net.sf.latexdraw.parser.pst;

import net.sf.latexdraw.model.api.shape.Text;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestParsingPsframebox extends TestPSTParser {
	@Test
	public void testParsepstriboxstar() {
		parser("\\pstribox*[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\pstribox*[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepstribox() {
		parser("\\pstribox[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\pstribox[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepsdiaboxstar() {
		parser("\\psdiabox*[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\psdiabox*[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepsdiabox() {
		parser("\\psdiabox[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\psdiabox[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepsovalboxstar() {
		parser("\\psovalbox*[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\psovalbox*[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepsovalbox() {
		parser("\\psovalbox[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\psovalbox[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepscircleboxstar() {
		parser("\\pscirclebox*[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\pscirclebox*[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepscirclebox() {
		parser("\\pscirclebox[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\pscirclebox[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepsshadowboxstar() {
		parser("\\psshadowbox*[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\psshadowbox*[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepsshadowbox() {
		parser("\\psshadowbox[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\psshadowbox[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testBug911816() {
		// https://bugs.launchpad.net/latexdraw/+bug/911816
		parser("\\psframebox{$E=mc^2$}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\psframebox{$E=mc^2$}", text.getText());
	}

	@Test
	public void testParsepsframebox() {
		parser("\\psframebox[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\psframebox[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepsdblframebox() {
		parser("\\psdblframebox{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\psdblframebox{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepsframeboxstar() {
		parser("\\psframebox*[doubleline=true]{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\psframebox*[doubleline=true]{\\psframe(0,1)}", text.getText());
	}

	@Test
	public void testParsepsdblframeboxstar() {
		parser("\\psdblframebox*{\\psframe(0,1)}");
		assertEquals(1, parsedShapes.size());
		final Text text = getShapeAt(0);
		assertEquals("\\psdblframebox*{\\psframe(0,1)}", text.getText());
	}
}
