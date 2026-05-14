package org.refactoringminer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

class WebDiffBrowserLauncherTest {
	@Test
	void launchStartsViewWithoutWritingStartupMessageToStdout() throws Exception {
		List<FakeWebDiffView> views = new ArrayList<>();
		WebDiffBrowserLauncher launcher = new WebDiffBrowserLauncher(diff -> {
			FakeWebDiffView view = new FakeWebDiffView();
			views.add(view);
			return view;
		}, port -> {
		});
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		PrintStream originalOut = System.out;

		try {
			System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
			McpDiffBrowserResult result = launcher.launch(projectDiff(), 6790, "test input", List.of());

			assertEquals("ok", result.status());
			assertEquals("Starting server: http://127.0.0.1:6790", result.message());
			assertEquals("", stdout.toString(StandardCharsets.UTF_8));
		} finally {
			System.setOut(originalOut);
		}
		assertEquals(1, views.size());
		assertTrue(views.get(0).started);
		assertFalse(views.get(0).terminated);
	}

	@Test
	void launchStopsPreviousViewBeforeStartingNextOne() throws Exception {
		List<FakeWebDiffView> views = new ArrayList<>();
		WebDiffBrowserLauncher launcher = new WebDiffBrowserLauncher(diff -> {
			FakeWebDiffView view = new FakeWebDiffView();
			views.add(view);
			return view;
		}, port -> {
		});

		launcher.launch(projectDiff(), 6790, "first", List.of());
		launcher.launch(projectDiff(), 6791, "second", List.of());

		assertEquals(2, views.size());
		assertTrue(views.get(0).terminated);
		assertTrue(views.get(1).started);
		assertFalse(views.get(1).terminated);
	}

	@Test
	void launchRejectsInvalidOrOccupiedPortsBeforeStartingView() {
		WebDiffBrowserLauncher invalidPortLauncher = new WebDiffBrowserLauncher(diff -> new FakeWebDiffView(),
				port -> {
				});
		WebDiffBrowserLauncher occupiedPortLauncher = new WebDiffBrowserLauncher(diff -> new FakeWebDiffView(),
				port -> {
					throw new IllegalArgumentException("port is already in use: " + port);
				});

		assertThrows(IllegalArgumentException.class,
				() -> invalidPortLauncher.launch(projectDiff(), 0, "input", List.of()));
		IllegalArgumentException occupied = assertThrows(IllegalArgumentException.class,
				() -> occupiedPortLauncher.launch(projectDiff(), 6790, "input", List.of()));

		assertTrue(occupied.getMessage().contains("port is already in use"));
	}

	@Test
	void launchPassesProjectDiffToFactory() throws Exception {
		ProjectASTDiff diff = projectDiff();
		List<ProjectASTDiff> received = new ArrayList<>();
		WebDiffBrowserLauncher launcher = new WebDiffBrowserLauncher(projectDiff -> {
			received.add(projectDiff);
			return new FakeWebDiffView();
		}, port -> {
		});

		launcher.launch(diff, 6790, "input", List.of());

		assertSame(diff, received.get(0));
	}

	private static ProjectASTDiff projectDiff() {
		return new ProjectASTDiff(Map.of("src/main/java/A.java", "class A {}"),
				Map.of("src/main/java/A.java", "class A { int x; }"));
	}

	private static final class FakeWebDiffView implements WebDiffBrowserLauncher.WebDiffView {
		private boolean started;
		private boolean terminated;

		@Override
		public void setPort(int port) {
		}

		@Override
		public String start() {
			started = true;
			return "unused";
		}

		@Override
		public void terminate() {
			terminated = true;
		}
	}
}
