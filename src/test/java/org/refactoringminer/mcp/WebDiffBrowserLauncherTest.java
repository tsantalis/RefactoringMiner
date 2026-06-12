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

import gui.webdiff.WebDiff;

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
			McpDiffBrowserResult result = launcher.launch(projectDiff(), 6790, "test input", List.of(), 20);

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

		launcher.launch(projectDiff(), 6790, "first", List.of(), 20);
		launcher.launch(projectDiff(), 6791, "second", List.of(), 20);

		assertEquals(2, views.size());
		assertTrue(views.get(0).terminated);
		assertTrue(views.get(1).started);
		assertFalse(views.get(1).terminated);
	}

	@Test
	void launchKeepsPreviousViewWhenDifferentNewPortIsUnavailable() throws Exception {
		List<FakeWebDiffView> views = new ArrayList<>();
		WebDiffBrowserLauncher launcher = new WebDiffBrowserLauncher(diff -> {
			FakeWebDiffView view = new FakeWebDiffView();
			views.add(view);
			return view;
		}, port -> {
			if (port == 6791) {
				throw new IllegalArgumentException("port is already in use: " + port);
			}
		});

		launcher.launch(projectDiff(), 6790, "first", List.of(), 20);
		IllegalArgumentException occupied = assertThrows(IllegalArgumentException.class,
				() -> launcher.launch(projectDiff(), 6791, "second", List.of(), 20));

		assertTrue(occupied.getMessage().contains("port is already in use"));
		assertEquals(1, views.size());
		assertTrue(views.get(0).started);
		assertFalse(views.get(0).terminated);
	}

	@Test
	void launchTerminatesNewViewWhenStartFails() throws Exception {
		FakeWebDiffView failingView = new FakeWebDiffView();
		failingView.failOnStart = true;
		WebDiffBrowserLauncher launcher = new WebDiffBrowserLauncher(diff -> failingView, port -> {
		});

		assertThrows(IllegalStateException.class, () -> launcher.launch(projectDiff(), 6790, "input", List.of(), 20));

		assertTrue(failingView.started);
		assertTrue(failingView.terminated);
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
				() -> invalidPortLauncher.launch(projectDiff(), 0, "input", List.of(), 20));
		IllegalArgumentException occupied = assertThrows(IllegalArgumentException.class,
				() -> occupiedPortLauncher.launch(projectDiff(), 6790, "input", List.of(), 20));

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

		launcher.launch(diff, 6790, "input", List.of(), 20);

		assertSame(diff, received.get(0));
	}

	@Test
	void launchUsesConfiguredBindAndPublicHosts() throws Exception {
		List<FakeWebDiffView> views = new ArrayList<>();
		WebDiffBrowserLauncher launcher = new WebDiffBrowserLauncher(diff -> {
			FakeWebDiffView view = new FakeWebDiffView();
			views.add(view);
			return view;
		}, port -> {
		}, "0.0.0.0", "localhost");

		McpDiffBrowserResult result = launcher.launch(projectDiff(), 6790, "input", List.of(), 20);

		assertEquals("http://localhost:6790", result.url());
		assertEquals("Starting server: http://localhost:6790", result.message());
		assertEquals("0.0.0.0", views.get(0).bindHost);
		assertEquals("localhost", views.get(0).publicHost);
	}

	@Test
	void webDiffReadsSystemPropertyHostDefaults() {
		String originalBindHost = System.getProperty(WebDiff.BIND_HOST_PROPERTY);
		String originalPublicHost = System.getProperty(WebDiff.PUBLIC_HOST_PROPERTY);
		try {
			System.setProperty(WebDiff.BIND_HOST_PROPERTY, "0.0.0.0");
			System.setProperty(WebDiff.PUBLIC_HOST_PROPERTY, "localhost");

			assertEquals("0.0.0.0", WebDiff.configuredBindHost());
			assertEquals("localhost", WebDiff.configuredPublicHost());
			assertEquals("http://localhost:6790", WebDiff.localUrl(WebDiff.configuredPublicHost(), 6790));
			assertEquals("Starting server: http://localhost:6790", WebDiff.startupMessage("localhost", 6790));
		} finally {
			restoreProperty(WebDiff.BIND_HOST_PROPERTY, originalBindHost);
			restoreProperty(WebDiff.PUBLIC_HOST_PROPERTY, originalPublicHost);
		}
	}

	private static ProjectASTDiff projectDiff() {
		return new ProjectASTDiff(Map.of("src/main/java/A.java", "class A {}"),
				Map.of("src/main/java/A.java", "class A { int x; }"));
	}

	private static void restoreProperty(String name, String value) {
		if (value == null) {
			System.clearProperty(name);
		} else {
			System.setProperty(name, value);
		}
	}

	private static final class FakeWebDiffView implements WebDiffBrowserLauncher.WebDiffView {
		private boolean started;
		private boolean terminated;
		private boolean failOnStart;
		private String bindHost;
		private String publicHost;

		@Override
		public void setBindHost(String bindHost) {
			this.bindHost = bindHost;
		}

		@Override
		public void setPublicHost(String publicHost) {
			this.publicHost = publicHost;
		}

		@Override
		public void setPort(int port) {
		}

		@Override
		public String start() {
			started = true;
			if (failOnStart) {
				throw new IllegalStateException("failed to start");
			}
			return "unused";
		}

		@Override
		public void terminate() {
			terminated = true;
		}
	}
}
