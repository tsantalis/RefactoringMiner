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
		this(WebDiffBrowserLauncher::defaultView, port -> requireAvailablePortStatic(bindHost, port, false), bindHost, publicHost);
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
			requireAvailablePortStatic(bindHost, port, true);
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

	private static void requireAvailablePortStatic(String bindHost, int port, boolean reuseAddress) throws IOException {
		try (ServerSocket socket = new ServerSocket()) {
			socket.setReuseAddress(reuseAddress);
			socket.bind(new InetSocketAddress(bindHost, port));
		} catch (BindException e) {
			throw new IllegalArgumentException("port is already in use: " + port, e);
		}
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
