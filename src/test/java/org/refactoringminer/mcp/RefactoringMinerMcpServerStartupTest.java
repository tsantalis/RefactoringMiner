package org.refactoringminer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class RefactoringMinerMcpServerStartupTest {
	@Test
	void doesNotWriteToStdoutInSmokeMode() throws Exception {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
			RefactoringMinerMcpServer.main(new String[] { "--smoke" });
		} finally {
			System.setOut(originalOut);
		}
		assertEquals("", stdout.toString(StandardCharsets.UTF_8));
	}
}
