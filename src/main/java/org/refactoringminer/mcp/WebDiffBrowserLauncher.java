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
	private final String bindHost;
	private final String publicHost;
	private WebDiffView activeView;
	private Integer activePort;

	WebDiffBrowserLauncher() {
		this(WebDiff.configuredBindHost(), WebDiff.configuredPublicHost());
	}

	private WebDiffBrowserLauncher(String bindHost, String publicHost) {
		this(WebDiffBrowserLauncher::defaultView, port -> requireAvailablePort(bindHost, port), bindHost, publicHost);
	}

	WebDiffBrowserLauncher(WebDiffViewFactory factory, PortProbe portProbe) {
		this(factory, portProbe, WebDiff.LOCAL_HOST, WebDiff.LOCAL_HOST);
	}

	WebDiffBrowserLauncher(WebDiffViewFactory factory, PortProbe portProbe, String bindHost, String publicHost) {
		this.factory = factory;
		this.portProbe = portProbe;
		this.bindHost = bindHost;
		this.publicHost = publicHost;
	}

	@Override
	public synchronized McpDiffBrowserResult launch(ProjectASTDiff diff, int port, String inputSummary,
			List<String> warnings) throws Exception {
		if (diff == null) {
			throw new IllegalArgumentException("ProjectASTDiff is required.");
		}
		validatePort(port);
		boolean replacingSamePort = activePort != null && activePort == port;
		if (!replacingSamePort) {
			portProbe.requireAvailable(port);
		}
		stopActiveView();
		if (replacingSamePort) {
			waitForPortAvailable(port);
		}

		WebDiffView view = factory.create(diff);
		view.setBindHost(bindHost);
		view.setPublicHost(publicHost);
		view.setPort(port);
		try {
			view.start();
		} catch (Exception e) {
			view.terminate();
			throw e;
		}
		activeView = view;
		activePort = port;
		return McpDiffBrowserResult.ok(diff, port, publicHost, inputSummary, warnings);
	}

	private void stopActiveView() {
		if (activeView != null) {
			activeView.terminate();
			activeView = null;
			activePort = null;
		}
	}

	private static void validatePort(int port) {
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("port must be between 1 and 65535.");
		}
	}

	private static void requireAvailablePort(String bindHost, int port) throws IOException {
		try (ServerSocket socket = new ServerSocket()) {
			socket.setReuseAddress(false);
			socket.bind(new InetSocketAddress(bindHost, port));
		} catch (BindException e) {
			throw new IllegalArgumentException("port is already in use: " + port, e);
		}
	}

	private void waitForPortAvailable(int port) throws IOException {
		long deadline = System.currentTimeMillis() + 5_000;
		while (System.currentTimeMillis() < deadline) {
			try (ServerSocket socket = new ServerSocket()) {
				socket.setReuseAddress(true);
				socket.bind(new InetSocketAddress(bindHost, port));
				return;
			} catch (BindException e) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					throw new IOException("interrupted while waiting for port: " + port, interrupted);
				}
			}
		}
		throw new IOException("port timed out after waiting: " + port);
	}

	private static WebDiffView defaultView(ProjectASTDiff diff) {
		WebDiff webDiff = new WebDiff(diff);
		webDiff.setExitJvmOnQuit(false);
		webDiff.setQuitEnabled(false);
		return new ManagedWebDiffView(webDiff);
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
		default void setBindHost(String bindHost) {
		}

		default void setPublicHost(String publicHost) {
		}

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
		public void setBindHost(String bindHost) {
			webDiff.setBindHost(bindHost);
		}

		@Override
		public void setPublicHost(String publicHost) {
			webDiff.setPublicHost(publicHost);
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
				// Spark never finished starting, so there is nothing to wait for.
			}
		}
	}
}
