/*
 * Copyright (C) 2009, Google Inc.
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2009, Yann Simon <yann.simon.fr@gmail.com>
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.util;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectChecker;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.time.MonotonicClock;
import org.eclipse.jgit.util.time.MonotonicSystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface to read values from the system.
 * <p>
 * When writing unit tests, extending this interface with a custom class
 * permits to simulate an access to a system variable or property and
 * permits to control the user's global configuration.
 * </p>
 */
public abstract class SystemReader {

	private static final Logger LOG = LoggerFactory
			.getLogger(SystemReader.class);

	private static final SystemReader DEFAULT;

	private static volatile Boolean isMacOS;

	private static volatile Boolean isWindows;

	private static volatile Boolean isLinux;

	private static final String GIT_TRACE_PERFORMANCE = "GIT_TRACE_PERFORMANCE"; //$NON-NLS-1$

	private static final boolean performanceTrace = initPerformanceTrace();

	private static boolean initPerformanceTrace() {
		String val = System.getenv(GIT_TRACE_PERFORMANCE);
		if (val == null) {
			val = System.getenv(GIT_TRACE_PERFORMANCE);
		}
		if (val != null) {
			return Boolean.valueOf(val).booleanValue();
		}
		return false;
	}

	static {
		SystemReader r = new Default();
		r.init();
		DEFAULT = r;
	}

	private static class Default extends SystemReader {
		private volatile String hostname;

		@Override
		public String getenv(String variable) {
			return System.getenv(variable);
		}

		@Override
		public String getProperty(String key) {
			return System.getProperty(key);
		}

		@Override
		public FileBasedConfig openSystemConfig(Config parent, FS fs) {
			if (StringUtils
					.isEmptyOrNull(getenv(Constants.GIT_CONFIG_NOSYSTEM_KEY))) {
				File configFile = fs.getGitSystemConfig();
				if (configFile != null) {
					return new FileBasedConfig(parent, configFile, fs);
				}
			}
			return new FileBasedConfig(parent, null, fs) {
				@Override
				public void load() {
					// empty, do not load
				}

				@Override
				public boolean isOutdated() {
					// regular class would bomb here
					return false;
				}
			};
		}

		@Override
		public FileBasedConfig openUserConfig(Config parent, FS fs) {
			return new FileBasedConfig(parent, new File(fs.userHome(), ".gitconfig"), //$NON-NLS-1$
					fs);
		}

		@Override
		public FileBasedConfig openJGitConfig(Config parent, FS fs) {
			Path xdgPath = getXdgConfigDirectory(fs);
			if (xdgPath != null) {
				Path configPath = xdgPath.resolve("jgit") //$NON-NLS-1$
						.resolve(Constants.CONFIG);
				return new FileBasedConfig(parent, configPath.toFile(), fs);
			}
			return new FileBasedConfig(parent,
					new File(fs.userHome(), ".jgitconfig"), fs); //$NON-NLS-1$
		}

		@Override
		public String getHostname() {
			if (hostname == null) {
				try {
					InetAddress localMachine = InetAddress.getLocalHost();
					hostname = localMachine.getCanonicalHostName();
				} catch (UnknownHostException e) {
					// we do nothing
					hostname = "localhost"; //$NON-NLS-1$
				}
				assert hostname != null;
			}
			return hostname;
		}

		@Override
		public long getCurrentTime() {
			return System.currentTimeMillis();
		}

		@Override
		public int getTimezone(long when) {
			return getTimeZone().getOffset(when) / (60 * 1000);
		}
	}

	private static volatile SystemReader INSTANCE = DEFAULT;

	/**
	 * Get the current SystemReader instance
	 *
	 * @return the current SystemReader instance.
	 */
	public static SystemReader getInstance() {
		return INSTANCE;
	}

	/**
	 * Set a new SystemReader instance to use when accessing properties.
	 *
	 * @param newReader
	 *            the new instance to use when accessing properties, or null for
	 *            the default instance.
	 */
	public static void setInstance(SystemReader newReader) {
		isMacOS = null;
		isWindows = null;
		isLinux = null;
		if (newReader == null)
			INSTANCE = DEFAULT;
		else {
			newReader.init();
			INSTANCE = newReader;
		}
	}

	private ObjectChecker platformChecker;

	private AtomicReference<FileBasedConfig> systemConfig = new AtomicReference<>();

	private AtomicReference<FileBasedConfig> userConfig = new AtomicReference<>();

	private AtomicReference<FileBasedConfig> jgitConfig = new AtomicReference<>();

	private volatile Charset defaultCharset;

	private void init() {
		// Creating ObjectChecker must be deferred. Unit tests change
		// behavior of is{Windows,MacOS} in constructor of subclass.
		if (platformChecker == null)
			setPlatformChecker();
	}

	/**
	 * Should be used in tests when the platform is explicitly changed.
	 *
	 * @since 3.6
	 */
	protected final void setPlatformChecker() {
		platformChecker = new ObjectChecker()
			.setSafeForWindows(isWindows())
			.setSafeForMacOS(isMacOS());
	}

	/**
	 * Gets the hostname of the local host. If no hostname can be found, the
	 * hostname is set to the default value "localhost".
	 *
	 * @return the canonical hostname
	 */
	public abstract String getHostname();

	/**
	 * Get value of the system variable
	 *
	 * @param variable
	 *            system variable to read
	 * @return value of the system variable
	 */
	public abstract String getenv(String variable);

	/**
	 * Get value of the system property
	 *
	 * @param key
	 *            of the system property to read
	 * @return value of the system property
	 */
	public abstract String getProperty(String key);

	/**
	 * Open the git configuration found in the user home. Use
	 * {@link #getUserConfig()} to get the current git configuration in the user
	 * home since it manages automatic reloading when the gitconfig file was
	 * modified and avoids unnecessary reloads.
	 *
	 * @param parent
	 *            a config with values not found directly in the returned config
	 * @param fs
	 *            the file system abstraction which will be necessary to perform
	 *            certain file system operations.
	 * @return the git configuration found in the user home
	 */
	public abstract FileBasedConfig openUserConfig(Config parent, FS fs);

	/**
	 * Open the gitconfig configuration found in the system-wide "etc"
	 * directory. Use {@link #getSystemConfig()} to get the current system-wide
	 * git configuration since it manages automatic reloading when the gitconfig
	 * file was modified and avoids unnecessary reloads.
	 *
	 * @param parent
	 *            a config with values not found directly in the returned
	 *            config. Null is a reasonable value here.
	 * @param fs
	 *            the file system abstraction which will be necessary to perform
	 *            certain file system operations.
	 * @return the gitconfig configuration found in the system-wide "etc"
	 *         directory
	 */
	public abstract FileBasedConfig openSystemConfig(Config parent, FS fs);

	/**
	 * Open the jgit configuration located at $XDG_CONFIG_HOME/jgit/config. Use
	 * {@link #getJGitConfig()} to get the current jgit configuration in the
	 * user home since it manages automatic reloading when the jgit config file
	 * was modified and avoids unnecessary reloads.
	 *
	 * @param parent
	 *            a config with values not found directly in the returned config
	 * @param fs
	 *            the file system abstraction which will be necessary to perform
	 *            certain file system operations.
	 * @return the jgit configuration located at $XDG_CONFIG_HOME/jgit/config
	 * @since 5.5.2
	 */
	public abstract FileBasedConfig openJGitConfig(Config parent, FS fs);

	/**
	 * Get the git configuration found in the user home. The configuration will
	 * be reloaded automatically if the configuration file was modified. Also
	 * reloads the system config if the system config file was modified. If the
	 * configuration file wasn't modified returns the cached configuration.
	 *
	 * @return the git configuration found in the user home
	 * @throws ConfigInvalidException
	 *             if configuration is invalid
	 * @throws IOException
	 *             if something went wrong when reading files
	 * @since 5.1.9
	 */
	public StoredConfig getUserConfig()
			throws ConfigInvalidException, IOException {
		FileBasedConfig c = userConfig.get();
		if (c == null) {
			userConfig.compareAndSet(null,
					openUserConfig(getSystemConfig(), FS.DETECTED));
			c = userConfig.get();
		}
		// on the very first call this will check a second time if the system
		// config is outdated
		updateAll(c);
		return c;
	}

	/**
	 * Get the jgit configuration located at $XDG_CONFIG_HOME/jgit/config. The
	 * configuration will be reloaded automatically if the configuration file
	 * was modified. If the configuration file wasn't modified returns the
	 * cached configuration.
	 *
	 * @return the jgit configuration located at $XDG_CONFIG_HOME/jgit/config
	 * @throws ConfigInvalidException
	 *             if configuration is invalid
	 * @throws IOException
	 *             if something went wrong when reading files
	 * @since 5.5.2
	 */
	public StoredConfig getJGitConfig()
			throws ConfigInvalidException, IOException {
		FileBasedConfig c = jgitConfig.get();
		if (c == null) {
			jgitConfig.compareAndSet(null,
					openJGitConfig(null, FS.DETECTED));
			c = jgitConfig.get();
		}
		updateAll(c);
		return c;
	}

	/**
	 * Get the gitconfig configuration found in the system-wide "etc" directory.
	 * The configuration will be reloaded automatically if the configuration
	 * file was modified otherwise returns the cached system level config.
	 *
	 * @return the gitconfig configuration found in the system-wide "etc"
	 *         directory
	 * @throws ConfigInvalidException
	 *             if configuration is invalid
	 * @throws IOException
	 *             if something went wrong when reading files
	 * @since 5.1.9
	 */
	public StoredConfig getSystemConfig()
			throws ConfigInvalidException, IOException {
		FileBasedConfig c = systemConfig.get();
		if (c == null) {
			systemConfig.compareAndSet(null,
					openSystemConfig(getJGitConfig(), FS.DETECTED));
			c = systemConfig.get();
		}
		updateAll(c);
		return c;
	}

	/**
	 * Gets the directory denoted by environment variable XDG_CONFIG_HOME. If
	 * the variable is not set or empty, return a path for
	 * {@code $HOME/.config}.
	 *
	 * @param fileSystem
	 *            {@link FS} to get the user's home directory
	 * @return a {@link Path} denoting the directory, which may exist or not, or
	 *         {@code null} if the environment variable is not set and there is
	 *         no home directory, or the path is invalid.
	 * @since 6.7
	 */
	public Path getXdgConfigDirectory(FS fileSystem) {
		String configHomePath = getenv(Constants.XDG_CONFIG_HOME);
		if (StringUtils.isEmptyOrNull(configHomePath)) {
			File home = fileSystem.userHome();
			if (home == null) {
				return null;
			}
			configHomePath = new File(home, ".config").getAbsolutePath(); //$NON-NLS-1$
		}
		try {
			return Paths.get(configHomePath);
		} catch (InvalidPathException e) {
			LOG.error(JGitText.get().logXDGConfigHomeInvalid, configHomePath,
					e);
		}
		return null;
	}

	/**
	 * Update config and its parents if they seem modified
	 *
	 * @param config
	 *            configuration to reload if outdated
	 * @throws ConfigInvalidException
	 *             if configuration is invalid
	 * @throws IOException
	 *             if something went wrong when reading files
	 */
	private void updateAll(Config config)
			throws ConfigInvalidException, IOException {
		if (config == null) {
			return;
		}
		updateAll(config.getBaseConfig());
		if (config instanceof FileBasedConfig) {
			FileBasedConfig cfg = (FileBasedConfig) config;
			if (cfg.isOutdated()) {
				LOG.debug("loading config {}", cfg); //$NON-NLS-1$
				cfg.load();
			}
		}
	}

	/**
	 * Get the current system time
	 *
	 * @return the current system time
	 */
	public abstract long getCurrentTime();

	/**
	 * Get clock instance preferred by this system.
	 *
	 * @return clock instance preferred by this system.
	 * @since 4.6
	 */
	public MonotonicClock getClock() {
		return new MonotonicSystemClock();
	}

	/**
	 * Get the local time zone
	 *
	 * @param when
	 *            a system timestamp
	 * @return the local time zone
	 */
	public abstract int getTimezone(long when);

	/**
	 * Get system time zone, possibly mocked for testing
	 *
	 * @return system time zone, possibly mocked for testing
	 * @since 1.2
	 */
	public TimeZone getTimeZone() {
		return TimeZone.getDefault();
	}

	/**
	 * Get the locale to use
	 *
	 * @return the locale to use
	 * @since 1.2
	 */
	public Locale getLocale() {
		return Locale.getDefault();
	}

	/**
	 * Retrieves the default {@link Charset} depending on the system locale.
	 *
	 * @return the {@link Charset}
	 * @since 6.0
	 * @see <a href="https://openjdk.java.net/jeps/400">JEP 400</a>
	 */
	public Charset getDefaultCharset() {
		Charset result = defaultCharset;
		if (result == null) {
			// JEP 400: Java 18 populates this system property.
			String encoding = getProperty("native.encoding"); //$NON-NLS-1$
			try {
				if (!StringUtils.isEmptyOrNull(encoding)) {
					result = Charset.forName(encoding);
				}
			} catch (IllegalCharsetNameException
					| UnsupportedCharsetException e) {
				LOG.error(JGitText.get().logInvalidDefaultCharset, encoding);
			}
			if (result == null) {
				// This is always UTF-8 on Java >= 18.
				result = Charset.defaultCharset();
			}
			defaultCharset = result;
		}
		return result;
	}

	/**
	 * Returns a simple date format instance as specified by the given pattern.
	 *
	 * @param pattern
	 *            the pattern as defined in
	 *            {@link java.text.SimpleDateFormat#SimpleDateFormat(String)}
	 * @return the simple date format
	 * @since 2.0
	 */
	public SimpleDateFormat getSimpleDateFormat(String pattern) {
		return new SimpleDateFormat(pattern);
	}

	/**
	 * Returns a simple date format instance as specified by the given pattern.
	 *
	 * @param pattern
	 *            the pattern as defined in
	 *            {@link java.text.SimpleDateFormat#SimpleDateFormat(String)}
	 * @param locale
	 *            locale to be used for the {@code SimpleDateFormat}
	 * @return the simple date format
	 * @since 3.2
	 */
	public SimpleDateFormat getSimpleDateFormat(String pattern, Locale locale) {
		return new SimpleDateFormat(pattern, locale);
	}

	/**
	 * Returns a date/time format instance for the given styles.
	 *
	 * @param dateStyle
	 *            the date style as specified in
	 *            {@link java.text.DateFormat#getDateTimeInstance(int, int)}
	 * @param timeStyle
	 *            the time style as specified in
	 *            {@link java.text.DateFormat#getDateTimeInstance(int, int)}
	 * @return the date format
	 * @since 2.0
	 */
	public DateFormat getDateTimeInstance(int dateStyle, int timeStyle) {
		return DateFormat.getDateTimeInstance(dateStyle, timeStyle);
	}

	/**
	 * Whether we are running on Windows.
	 *
	 * @return true if we are running on Windows.
	 */
	public boolean isWindows() {
		if (isWindows == null) {
			String osDotName = getOsName();
			isWindows = Boolean.valueOf(osDotName.startsWith("Windows")); //$NON-NLS-1$
		}
		return isWindows.booleanValue();
	}

	/**
	 * Whether we are running on Mac OS X
	 *
	 * @return true if we are running on Mac OS X
	 */
	public boolean isMacOS() {
		if (isMacOS == null) {
			String osDotName = getOsName();
			isMacOS = Boolean.valueOf(
					"Mac OS X".equals(osDotName) || "Darwin".equals(osDotName)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return isMacOS.booleanValue();
	}

	/**
	 * Whether we are running on Linux.
	 *
	 * @return true if we are running on Linux.
	 * @since 6.3
	 */
	public boolean isLinux() {
		if (isLinux == null) {
			String osname = getOsName();
			isLinux = Boolean.valueOf(osname.toLowerCase().startsWith("linux")); //$NON-NLS-1$
		}
		return isLinux.booleanValue();
	}

	/**
	 * Whether performance trace is enabled
	 *
	 * @return whether performance trace is enabled
	 * @since 6.5
	 */
	public boolean isPerformanceTraceEnabled() {
		return performanceTrace;
	}

	private String getOsName() {
		return AccessController.doPrivileged(
				(PrivilegedAction<String>) () -> getProperty("os.name") //$NON-NLS-1$
		);
	}

	/**
	 * Check tree path entry for validity.
	 * <p>
	 * Scans a multi-directory path string such as {@code "src/main.c"}.
	 *
	 * @param path path string to scan.
	 * @throws org.eclipse.jgit.errors.CorruptObjectException path is invalid.
	 * @since 3.6
	 */
	public void checkPath(String path) throws CorruptObjectException {
		platformChecker.checkPath(path);
	}

	/**
	 * Check tree path entry for validity.
	 * <p>
	 * Scans a multi-directory path string such as {@code "src/main.c"}.
	 *
	 * @param path
	 *            path string to scan.
	 * @throws org.eclipse.jgit.errors.CorruptObjectException
	 *             path is invalid.
	 * @since 4.2
	 */
	public void checkPath(byte[] path) throws CorruptObjectException {
		platformChecker.checkPath(path, 0, path.length);
	}
}
