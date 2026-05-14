package org.refactoringminer.mcp;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;

import gui.webdiff.WebDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

final class WebDiffBrowserLauncher implements DiffBrowserLauncher {
	private final WebDiffViewFactory factory;
	private final PortProbe portProbe;
	private WebDiffView activeView;

	WebDiffBrowserLauncher() {
		this(WebDiffBrowserLauncher::defaultView, WebDiffBrowserLauncher::requireAvailablePort);
	}

	WebDiffBrowserLauncher(WebDiffViewFactory factory, PortProbe portProbe) {
		this.factory = factory;
		this.portProbe = portProbe;
	}

	@Override
	public synchronized McpDiffBrowserResult launch(ProjectASTDiff diff, int port, String inputSummary,
			List<String> warnings) throws Exception {
		if (diff == null) {
			throw new IllegalArgumentException("ProjectASTDiff is required.");
		}
		validatePort(port);
		stopActiveView();
		portProbe.requireAvailable(port);

		WebDiffView view = factory.create(diff);
		view.setPort(port);
		view.start();
		activeView = view;
		return McpDiffBrowserResult.ok(diff, port, inputSummary, warnings);
	}

	private void stopActiveView() {
		if (activeView != null) {
			activeView.terminate();
			activeView = null;
		}
	}

	private static void validatePort(int port) {
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("port must be between 1 and 65535.");
		}
	}

	private static void requireAvailablePort(int port) throws IOException {
		try (ServerSocket socket = new ServerSocket()) {
			socket.setReuseAddress(false);
			socket.bind(new InetSocketAddress("127.0.0.1", port));
		} catch (BindException e) {
			throw new IllegalArgumentException("port is already in use: " + port, e);
		}
	}

	private static WebDiffView defaultView(ProjectASTDiff diff) {
		return new ManagedWebDiffView(new WebDiff(diff));
	}

	@FunctionalInterface
	interface WebDiffViewFactory {
		WebDiffView create(ProjectASTDiff diff);
	}

	@FunctionalInterface
	interface PortProbe {
		void requireAvailable(int port) throws Exception;
	}

	interface WebDiffView {
		void setPort(int port);
		String start();
		void terminate();
	}

	private static final class ManagedWebDiffView implements WebDiffView {
		private final WebDiff webDiff;

		private ManagedWebDiffView(WebDiff webDiff) {
			this.webDiff = webDiff;
		}

		@Override
		public void setPort(int port) {
			webDiff.setPort(port);
		}

		@Override
		public String start() {
			return webDiff.start();
		}

		@Override
		public void terminate() {
			webDiff.terminate();
			try {
				spark.Spark.awaitStop();
			} catch (IllegalStateException ignored) {
				// Spark was not fully initialized; there is nothing to wait for.
			}
		}
	}
}
