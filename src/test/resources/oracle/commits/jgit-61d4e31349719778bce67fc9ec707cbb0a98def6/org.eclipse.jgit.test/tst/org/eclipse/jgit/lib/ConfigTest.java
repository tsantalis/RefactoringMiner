/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2009-2010, Google Inc.
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.lib;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.jgit.util.FileUtils.pathToString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.merge.MergeConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test reading of git config
 */
@SuppressWarnings("boxing")
public class ConfigTest {
	// A non-ASCII whitespace character: U+2002 EN QUAD.
	private static final char WS = '\u2002';

	private static final String REFS_ORIGIN = "+refs/heads/*:refs/remotes/origin/*";

	private static final String REFS_UPSTREAM = "+refs/heads/*:refs/remotes/upstream/*";

	private static final String REFS_BACKUP = "+refs/heads/*:refs/remotes/backup/*";

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@After
	public void tearDown() {
		SystemReader.setInstance(null);
	}

	@Test
	public void test001_ReadBareKey() throws ConfigInvalidException {
		final Config c = parse("[foo]\nbar\n");
		assertTrue(c.getBoolean("foo", null, "bar", false));
		assertEquals("", c.getString("foo", null, "bar"));
	}

	@Test
	public void test002_ReadWithSubsection() throws ConfigInvalidException {
		final Config c = parse("[foo \"zip\"]\nbar\n[foo \"zap\"]\nbar=false\nn=3\n");
		assertTrue(c.getBoolean("foo", "zip", "bar", false));
		assertEquals("", c.getString("foo","zip", "bar"));
		assertFalse(c.getBoolean("foo", "zap", "bar", true));
		assertEquals("false", c.getString("foo", "zap", "bar"));
		assertEquals(3, c.getInt("foo", "zap", "n", 4));
		assertEquals(4, c.getInt("foo", "zap","m", 4));
	}

	@Test
	public void test003_PutRemote() {
		final Config c = new Config();
		c.setString("sec", "ext", "name", "value");
		c.setString("sec", "ext", "name2", "value2");
		final String expText = "[sec \"ext\"]\n\tname = value\n\tname2 = value2\n";
		assertEquals(expText, c.toText());
	}

	@Test
	public void test004_PutGetSimple() {
		Config c = new Config();
		c.setString("my", null, "somename", "false");
		assertEquals("false", c.getString("my", null, "somename"));
		assertEquals("[my]\n\tsomename = false\n", c.toText());
	}

	@Test
	public void test005_PutGetStringList() {
		Config c = new Config();
		final LinkedList<String> values = new LinkedList<>();
		values.add("value1");
		values.add("value2");
		c.setStringList("my", null, "somename", values);

		final Object[] expArr = values.toArray();
		final String[] actArr = c.getStringList("my", null, "somename");
		assertArrayEquals(expArr, actArr);

		final String expText = "[my]\n\tsomename = value1\n\tsomename = value2\n";
		assertEquals(expText, c.toText());
	}

	@Test
	public void test006_readCaseInsensitive() throws ConfigInvalidException {
		final Config c = parse("[Foo]\nBar\n");
		assertTrue(c.getBoolean("foo", null, "bar", false));
		assertEquals("", c.getString("foo", null, "bar"));
	}

	@Test
	public void test007_readUserConfig() {
		final MockSystemReader mockSystemReader = new MockSystemReader();
		SystemReader.setInstance(mockSystemReader);
		final String hostname = mockSystemReader.getHostname();
		final Config userGitConfig = mockSystemReader.openUserConfig(null,
				FS.DETECTED);
		final Config localConfig = new Config(userGitConfig);
		mockSystemReader.clearProperties();

		String authorName;
		String authorEmail;

		// no values defined nowhere
		authorName = localConfig.get(UserConfig.KEY).getAuthorName();
		authorEmail = localConfig.get(UserConfig.KEY).getAuthorEmail();
		assertEquals(Constants.UNKNOWN_USER_DEFAULT, authorName);
		assertEquals(Constants.UNKNOWN_USER_DEFAULT + "@" + hostname, authorEmail);
		assertTrue(localConfig.get(UserConfig.KEY).isAuthorNameImplicit());
		assertTrue(localConfig.get(UserConfig.KEY).isAuthorEmailImplicit());

		// the system user name is defined
		mockSystemReader.setProperty(Constants.OS_USER_NAME_KEY, "os user name");
		localConfig.uncache(UserConfig.KEY);
		authorName = localConfig.get(UserConfig.KEY).getAuthorName();
		assertEquals("os user name", authorName);
		assertTrue(localConfig.get(UserConfig.KEY).isAuthorNameImplicit());

		if (hostname != null && hostname.length() != 0) {
			authorEmail = localConfig.get(UserConfig.KEY).getAuthorEmail();
			assertEquals("os user name@" + hostname, authorEmail);
		}
		assertTrue(localConfig.get(UserConfig.KEY).isAuthorEmailImplicit());

		// the git environment variables are defined
		mockSystemReader.setProperty(Constants.GIT_AUTHOR_NAME_KEY, "git author name");
		mockSystemReader.setProperty(Constants.GIT_AUTHOR_EMAIL_KEY, "author@email");
		localConfig.uncache(UserConfig.KEY);
		authorName = localConfig.get(UserConfig.KEY).getAuthorName();
		authorEmail = localConfig.get(UserConfig.KEY).getAuthorEmail();
		assertEquals("git author name", authorName);
		assertEquals("author@email", authorEmail);
		assertFalse(localConfig.get(UserConfig.KEY).isAuthorNameImplicit());
		assertFalse(localConfig.get(UserConfig.KEY).isAuthorEmailImplicit());

		// the values are defined in the global configuration
		// first clear environment variables since they would override
		// configuration files
		mockSystemReader.clearProperties();
		userGitConfig.setString("user", null, "name", "global username");
		userGitConfig.setString("user", null, "email", "author@globalemail");
		authorName = localConfig.get(UserConfig.KEY).getAuthorName();
		authorEmail = localConfig.get(UserConfig.KEY).getAuthorEmail();
		assertEquals("global username", authorName);
		assertEquals("author@globalemail", authorEmail);
		assertFalse(localConfig.get(UserConfig.KEY).isAuthorNameImplicit());
		assertFalse(localConfig.get(UserConfig.KEY).isAuthorEmailImplicit());

		// the values are defined in the local configuration
		localConfig.setString("user", null, "name", "local username");
		localConfig.setString("user", null, "email", "author@localemail");
		authorName = localConfig.get(UserConfig.KEY).getAuthorName();
		authorEmail = localConfig.get(UserConfig.KEY).getAuthorEmail();
		assertEquals("local username", authorName);
		assertEquals("author@localemail", authorEmail);
		assertFalse(localConfig.get(UserConfig.KEY).isAuthorNameImplicit());
		assertFalse(localConfig.get(UserConfig.KEY).isAuthorEmailImplicit());

		authorName = localConfig.get(UserConfig.KEY).getCommitterName();
		authorEmail = localConfig.get(UserConfig.KEY).getCommitterEmail();
		assertEquals("local username", authorName);
		assertEquals("author@localemail", authorEmail);
		assertFalse(localConfig.get(UserConfig.KEY).isCommitterNameImplicit());
		assertFalse(localConfig.get(UserConfig.KEY).isCommitterEmailImplicit());

		// also git environment variables are defined
		mockSystemReader.setProperty(Constants.GIT_AUTHOR_NAME_KEY,
				"git author name");
		mockSystemReader.setProperty(Constants.GIT_AUTHOR_EMAIL_KEY,
				"author@email");
		localConfig.setString("user", null, "name", "local username");
		localConfig.setString("user", null, "email", "author@localemail");
		authorName = localConfig.get(UserConfig.KEY).getAuthorName();
		authorEmail = localConfig.get(UserConfig.KEY).getAuthorEmail();
		assertEquals("git author name", authorName);
		assertEquals("author@email", authorEmail);
		assertFalse(localConfig.get(UserConfig.KEY).isAuthorNameImplicit());
		assertFalse(localConfig.get(UserConfig.KEY).isAuthorEmailImplicit());
	}

	@Test
	public void testReadUserConfigWithInvalidCharactersStripped() {
		final MockSystemReader mockSystemReader = new MockSystemReader();
		final Config localConfig = new Config(mockSystemReader.openUserConfig(
				null, FS.DETECTED));

		localConfig.setString("user", null, "name", "foo<bar");
		localConfig.setString("user", null, "email", "baz>\nqux@example.com");

		UserConfig userConfig = localConfig.get(UserConfig.KEY);
		assertEquals("foobar", userConfig.getAuthorName());
		assertEquals("bazqux@example.com", userConfig.getAuthorEmail());
	}

	@Test
	public void testReadBoolean_TrueFalse1() throws ConfigInvalidException {
		final Config c = parse("[s]\na = true\nb = false\n");
		assertEquals("true", c.getString("s", null, "a"));
		assertEquals("false", c.getString("s", null, "b"));

		assertTrue(c.getBoolean("s", "a", false));
		assertFalse(c.getBoolean("s", "b", true));
	}

	@Test
	public void testReadBoolean_TrueFalse2() throws ConfigInvalidException {
		final Config c = parse("[s]\na = TrUe\nb = fAlSe\n");
		assertEquals("TrUe", c.getString("s", null, "a"));
		assertEquals("fAlSe", c.getString("s", null, "b"));

		assertTrue(c.getBoolean("s", "a", false));
		assertFalse(c.getBoolean("s", "b", true));
	}

	@Test
	public void testReadBoolean_YesNo1() throws ConfigInvalidException {
		final Config c = parse("[s]\na = yes\nb = no\n");
		assertEquals("yes", c.getString("s", null, "a"));
		assertEquals("no", c.getString("s", null, "b"));

		assertTrue(c.getBoolean("s", "a", false));
		assertFalse(c.getBoolean("s", "b", true));
	}

	@Test
	public void testReadBoolean_YesNo2() throws ConfigInvalidException {
		final Config c = parse("[s]\na = yEs\nb = NO\n");
		assertEquals("yEs", c.getString("s", null, "a"));
		assertEquals("NO", c.getString("s", null, "b"));

		assertTrue(c.getBoolean("s", "a", false));
		assertFalse(c.getBoolean("s", "b", true));
	}

	@Test
	public void testReadBoolean_OnOff1() throws ConfigInvalidException {
		final Config c = parse("[s]\na = on\nb = off\n");
		assertEquals("on", c.getString("s", null, "a"));
		assertEquals("off", c.getString("s", null, "b"));

		assertTrue(c.getBoolean("s", "a", false));
		assertFalse(c.getBoolean("s", "b", true));
	}

	@Test
	public void testReadBoolean_OnOff2() throws ConfigInvalidException {
		final Config c = parse("[s]\na = ON\nb = OFF\n");
		assertEquals("ON", c.getString("s", null, "a"));
		assertEquals("OFF", c.getString("s", null, "b"));

		assertTrue(c.getBoolean("s", "a", false));
		assertFalse(c.getBoolean("s", "b", true));
	}

	enum TestEnum {
		ONE_TWO;
	}

	@Test
	public void testGetEnum() throws ConfigInvalidException {
		Config c = parse("[s]\na = ON\nb = input\nc = true\nd = off\n");
		assertSame(CoreConfig.AutoCRLF.TRUE, c.getEnum("s", null, "a",
				CoreConfig.AutoCRLF.FALSE));

		assertSame(CoreConfig.AutoCRLF.INPUT, c.getEnum("s", null, "b",
				CoreConfig.AutoCRLF.FALSE));

		assertSame(CoreConfig.AutoCRLF.TRUE, c.getEnum("s", null, "c",
				CoreConfig.AutoCRLF.FALSE));

		assertSame(CoreConfig.AutoCRLF.FALSE, c.getEnum("s", null, "d",
				CoreConfig.AutoCRLF.TRUE));

		c = new Config();
		assertSame(CoreConfig.AutoCRLF.FALSE, c.getEnum("s", null, "d",
				CoreConfig.AutoCRLF.FALSE));

		c = parse("[s \"b\"]\n\tc = one two\n");
		assertSame(TestEnum.ONE_TWO, c.getEnum("s", "b", "c", TestEnum.ONE_TWO));

		c = parse("[s \"b\"]\n\tc = one-two\n");
		assertSame(TestEnum.ONE_TWO, c.getEnum("s", "b", "c", TestEnum.ONE_TWO));
	}

	@Test
	public void testGetInvalidEnum() throws ConfigInvalidException {
		Config c = parse("[a]\n\tb = invalid\n");
		try {
			c.getEnum("a", null, "b", TestEnum.ONE_TWO);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Invalid value: a.b=invalid", e.getMessage());
		}

		c = parse("[a \"b\"]\n\tc = invalid\n");
		try {
			c.getEnum("a", "b", "c", TestEnum.ONE_TWO);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Invalid value: a.b.c=invalid", e.getMessage());
		}
	}

	@Test
	public void testSetEnum() {
		final Config c = new Config();
		c.setEnum("s", "b", "c", TestEnum.ONE_TWO);
		assertEquals("[s \"b\"]\n\tc = one two\n", c.toText());
	}

	@Test
	public void testGetFastForwardMergeoptions() throws ConfigInvalidException {
		Config c = new Config(null); // not set
		assertSame(FastForwardMode.FF, c.getEnum(
				ConfigConstants.CONFIG_BRANCH_SECTION, "side",
				ConfigConstants.CONFIG_KEY_MERGEOPTIONS, FastForwardMode.FF));
		MergeConfig mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.FF, mergeConfig.getFastForwardMode());
		c = parse("[branch \"side\"]\n\tmergeoptions = --ff-only\n");
		assertSame(FastForwardMode.FF_ONLY, c.getEnum(
				ConfigConstants.CONFIG_BRANCH_SECTION, "side",
				ConfigConstants.CONFIG_KEY_MERGEOPTIONS,
				FastForwardMode.FF_ONLY));
		mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.FF_ONLY, mergeConfig.getFastForwardMode());
		c = parse("[branch \"side\"]\n\tmergeoptions = --ff\n");
		assertSame(FastForwardMode.FF, c.getEnum(
				ConfigConstants.CONFIG_BRANCH_SECTION, "side",
				ConfigConstants.CONFIG_KEY_MERGEOPTIONS, FastForwardMode.FF));
		mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.FF, mergeConfig.getFastForwardMode());
		c = parse("[branch \"side\"]\n\tmergeoptions = --no-ff\n");
		assertSame(FastForwardMode.NO_FF, c.getEnum(
				ConfigConstants.CONFIG_BRANCH_SECTION, "side",
				ConfigConstants.CONFIG_KEY_MERGEOPTIONS, FastForwardMode.NO_FF));
		mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.NO_FF, mergeConfig.getFastForwardMode());
	}

	@Test
	public void testSetFastForwardMergeoptions() {
		final Config c = new Config();
		c.setEnum("branch", "side", "mergeoptions", FastForwardMode.FF);
		assertEquals("[branch \"side\"]\n\tmergeoptions = --ff\n", c.toText());
		c.setEnum("branch", "side", "mergeoptions", FastForwardMode.FF_ONLY);
		assertEquals("[branch \"side\"]\n\tmergeoptions = --ff-only\n",
				c.toText());
		c.setEnum("branch", "side", "mergeoptions", FastForwardMode.NO_FF);
		assertEquals("[branch \"side\"]\n\tmergeoptions = --no-ff\n",
				c.toText());
	}

	@Test
	public void testGetFastForwardMerge() throws ConfigInvalidException {
		Config c = new Config(null); // not set
		assertSame(FastForwardMode.Merge.TRUE, c.getEnum(
				ConfigConstants.CONFIG_KEY_MERGE, null,
				ConfigConstants.CONFIG_KEY_FF, FastForwardMode.Merge.TRUE));
		MergeConfig mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.FF, mergeConfig.getFastForwardMode());
		c = parse("[merge]\n\tff = only\n");
		assertSame(FastForwardMode.Merge.ONLY, c.getEnum(
				ConfigConstants.CONFIG_KEY_MERGE, null,
				ConfigConstants.CONFIG_KEY_FF, FastForwardMode.Merge.ONLY));
		mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.FF_ONLY, mergeConfig.getFastForwardMode());
		c = parse("[merge]\n\tff = true\n");
		assertSame(FastForwardMode.Merge.TRUE, c.getEnum(
				ConfigConstants.CONFIG_KEY_MERGE, null,
				ConfigConstants.CONFIG_KEY_FF, FastForwardMode.Merge.TRUE));
		mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.FF, mergeConfig.getFastForwardMode());
		c = parse("[merge]\n\tff = false\n");
		assertSame(FastForwardMode.Merge.FALSE, c.getEnum(
				ConfigConstants.CONFIG_KEY_MERGE, null,
				ConfigConstants.CONFIG_KEY_FF, FastForwardMode.Merge.FALSE));
		mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.NO_FF, mergeConfig.getFastForwardMode());
	}

	@Test
	public void testCombinedMergeOptions() throws ConfigInvalidException {
		Config c = new Config(null); // not set
		MergeConfig mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.FF, mergeConfig.getFastForwardMode());
		assertTrue(mergeConfig.isCommit());
		assertFalse(mergeConfig.isSquash());
		// branch..mergeoptions should win over merge.ff
		c = parse("[merge]\n\tff = false\n"
				+ "[branch \"side\"]\n\tmergeoptions = --ff-only\n");
		mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.FF_ONLY, mergeConfig.getFastForwardMode());
		assertTrue(mergeConfig.isCommit());
		assertFalse(mergeConfig.isSquash());
		// merge.ff used for ff setting if not set via mergeoptions
		c = parse("[merge]\n\tff = only\n"
				+ "[branch \"side\"]\n\tmergeoptions = --squash\n");
		mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.FF_ONLY, mergeConfig.getFastForwardMode());
		assertTrue(mergeConfig.isCommit());
		assertTrue(mergeConfig.isSquash());
		// mergeoptions wins if it has ff options amongst other options
		c = parse("[merge]\n\tff = false\n"
				+ "[branch \"side\"]\n\tmergeoptions = --ff-only --no-commit\n");
		mergeConfig = c.get(MergeConfig.getParser("side"));
		assertSame(FastForwardMode.FF_ONLY, mergeConfig.getFastForwardMode());
		assertFalse(mergeConfig.isCommit());
		assertFalse(mergeConfig.isSquash());
	}

	@Test
	public void testSetFastForwardMerge() {
		final Config c = new Config();
		c.setEnum("merge", null, "ff",
				FastForwardMode.Merge.valueOf(FastForwardMode.FF));
		assertEquals("[merge]\n\tff = true\n", c.toText());
		c.setEnum("merge", null, "ff",
				FastForwardMode.Merge.valueOf(FastForwardMode.FF_ONLY));
		assertEquals("[merge]\n\tff = only\n", c.toText());
		c.setEnum("merge", null, "ff",
				FastForwardMode.Merge.valueOf(FastForwardMode.NO_FF));
		assertEquals("[merge]\n\tff = false\n", c.toText());
	}

	@Test
	public void testReadLong() throws ConfigInvalidException {
		assertReadLong(1L);
		assertReadLong(-1L);
		assertReadLong(Long.MIN_VALUE);
		assertReadLong(Long.MAX_VALUE);
		assertReadLong(4L * 1024 * 1024 * 1024, "4g");
		assertReadLong(3L * 1024 * 1024, "3 m");
		assertReadLong(8L * 1024, "8 k");

		try {
			assertReadLong(-1, "1.5g");
			fail("incorrectly accepted 1.5g");
		} catch (IllegalArgumentException e) {
			assertEquals("Invalid integer value: s.a=1.5g", e.getMessage());
		}
	}

	@Test
	public void testBooleanWithNoValue() throws ConfigInvalidException {
		Config c = parse("[my]\n\tempty\n");
		assertEquals("", c.getString("my", null, "empty"));
		assertEquals(1, c.getStringList("my", null, "empty").length);
		assertEquals("", c.getStringList("my", null, "empty")[0]);
		assertTrue(c.getBoolean("my", "empty", false));
		assertEquals("[my]\n\tempty\n", c.toText());
	}

	@Test
	public void testUnsetBranchSection() throws ConfigInvalidException {
		Config c = parse("" //
				+ "[branch \"keep\"]\n"
				+ "  merge = master.branch.to.keep.in.the.file\n"
				+ "\n"
				+ "[branch \"remove\"]\n"
				+ "  merge = this.will.get.deleted\n"
				+ "  remote = origin-for-some-long-gone-place\n"
				+ "\n"
				+ "[core-section-not-to-remove-in-test]\n"
				+ "  packedGitLimit = 14\n");
		c.unsetSection("branch", "does.not.exist");
		c.unsetSection("branch", "remove");
		assertEquals("" //
				+ "[branch \"keep\"]\n"
				+ "  merge = master.branch.to.keep.in.the.file\n"
				+ "\n"
				+ "[core-section-not-to-remove-in-test]\n"
				+ "  packedGitLimit = 14\n", c.toText());
	}

	@Test
	public void testUnsetSingleSection() throws ConfigInvalidException {
		Config c = parse("" //
				+ "[branch \"keep\"]\n"
				+ "  merge = master.branch.to.keep.in.the.file\n"
				+ "\n"
				+ "[single]\n"
				+ "  merge = this.will.get.deleted\n"
				+ "  remote = origin-for-some-long-gone-place\n"
				+ "\n"
				+ "[core-section-not-to-remove-in-test]\n"
				+ "  packedGitLimit = 14\n");
		c.unsetSection("single", null);
		assertEquals("" //
				+ "[branch \"keep\"]\n"
				+ "  merge = master.branch.to.keep.in.the.file\n"
				+ "\n"
				+ "[core-section-not-to-remove-in-test]\n"
				+ "  packedGitLimit = 14\n", c.toText());
	}

	@Test
	public void test008_readSectionNames() throws ConfigInvalidException {
		final Config c = parse("[a]\n [B]\n");
		Set<String> sections = c.getSections();
		assertTrue("Sections should contain \"a\"", sections.contains("a"));
		assertTrue("Sections should contain \"b\"", sections.contains("b"));
	}

	@Test
	public void test009_readNamesInSection() throws ConfigInvalidException {
		String configString = "[core]\n" + "repositoryFormatVersion = 0\n"
				+ "filemode = false\n" + "logAllRefUpdates = true\n";
		final Config c = parse(configString);
		Set<String> names = c.getNames("core");
		assertEquals("Core section size", 3, names.size());
		assertTrue("Core section should contain \"filemode\"", names
				.contains("filemode"));

		assertTrue("Core section should contain \"repositoryFormatVersion\"",
				names.contains("repositoryFormatVersion"));

		assertTrue("Core section should contain \"repositoryformatversion\"",
				names.contains("repositoryformatversion"));

		Iterator<String> itr = names.iterator();
		assertEquals("filemode", itr.next());
		assertEquals("logAllRefUpdates", itr.next());
		assertEquals("repositoryFormatVersion", itr.next());
		assertFalse(itr.hasNext());
	}

	@Test
	public void test_ReadNamesInSectionRecursive()
			throws ConfigInvalidException {
		String baseConfigString = "[core]\n" + "logAllRefUpdates = true\n";
		String configString = "[core]\n" + "repositoryFormatVersion = 0\n"
				+ "filemode = false\n";
		final Config c = parse(configString, parse(baseConfigString));
		Set<String> names = c.getNames("core", true);
		assertEquals("Core section size", 3, names.size());
		assertTrue("Core section should contain \"filemode\"",
				names.contains("filemode"));
		assertTrue("Core section should contain \"repositoryFormatVersion\"",
				names.contains("repositoryFormatVersion"));
		assertTrue("Core section should contain \"logAllRefUpdates\"",
				names.contains("logAllRefUpdates"));
		assertTrue("Core section should contain \"logallrefupdates\"",
				names.contains("logallrefupdates"));

		Iterator<String> itr = names.iterator();
		assertEquals("filemode", itr.next());
		assertEquals("repositoryFormatVersion", itr.next());
		assertEquals("logAllRefUpdates", itr.next());
		assertFalse(itr.hasNext());
	}

	@Test
	public void test010_readNamesInSubSection() throws ConfigInvalidException {
		String configString = "[a \"sub1\"]\n"//
				+ "x = 0\n" //
				+ "y = false\n"//
				+ "z = true\n"//
				+ "[a \"sub2\"]\n"//
				+ "a=0\n"//
				+ "b=1\n";
		final Config c = parse(configString);
		Set<String> names = c.getNames("a", "sub1");
		assertEquals("Subsection size", 3, names.size());
		assertTrue("Subsection should contain \"x\"", names.contains("x"));
		assertTrue("Subsection should contain \"y\"", names.contains("y"));
		assertTrue("Subsection should contain \"z\"", names.contains("z"));
		names = c.getNames("a", "sub2");
		assertEquals("Subsection size", 2, names.size());
		assertTrue("Subsection should contain \"a\"", names.contains("a"));
		assertTrue("Subsection should contain \"b\"", names.contains("b"));
	}

	@Test
	public void readNamesInSubSectionRecursive() throws ConfigInvalidException {
		String baseConfigString = "[a \"sub1\"]\n"//
				+ "x = 0\n" //
				+ "y = false\n"//
				+ "[a \"sub2\"]\n"//
				+ "A=0\n";//
		String configString = "[a \"sub1\"]\n"//
				+ "z = true\n"//
				+ "[a \"sub2\"]\n"//
				+ "B=1\n";
		final Config c = parse(configString, parse(baseConfigString));
		Set<String> names = c.getNames("a", "sub1", true);
		assertEquals("Subsection size", 3, names.size());
		assertTrue("Subsection should contain \"x\"", names.contains("x"));
		assertTrue("Subsection should contain \"y\"", names.contains("y"));
		assertTrue("Subsection should contain \"z\"", names.contains("z"));
		names = c.getNames("a", "sub2", true);
		assertEquals("Subsection size", 2, names.size());
		assertTrue("Subsection should contain \"A\"", names.contains("A"));
		assertTrue("Subsection should contain \"a\"", names.contains("a"));
		assertTrue("Subsection should contain \"B\"", names.contains("B"));
	}


	@Test
	public void testNoFinalNewline() throws ConfigInvalidException {
		Config c = parse("[a]\n"
				+ "x = 0\n"
				+ "y = 1");
		assertEquals("0", c.getString("a", null, "x"));
		assertEquals("1", c.getString("a", null, "y"));
	}

	@Test
	public void testExplicitlySetEmptyString() throws Exception {
		Config c = new Config();
		c.setString("a", null, "x", "0");
		c.setString("a", null, "y", "");

		assertEquals("0", c.getString("a", null, "x"));
		assertEquals(0, c.getInt("a", null, "x", 1));

		assertEquals("", c.getString("a", null, "y"));
		assertArrayEquals(new String[]{""}, c.getStringList("a", null, "y"));
		assertEquals(1, c.getInt("a", null, "y", 1));

		assertNull(c.getString("a", null, "z"));
		assertArrayEquals(new String[]{}, c.getStringList("a", null, "z"));
	}

	@Test
	public void testParsedEmptyString() throws Exception {
		Config c = parse("[a]\n"
				+ "x = 0\n"
				+ "y =\n");

		assertEquals("0", c.getString("a", null, "x"));
		assertEquals(0, c.getInt("a", null, "x", 1));

		assertNull(c.getString("a", null, "y"));
		assertArrayEquals(new String[]{null}, c.getStringList("a", null, "y"));
		assertEquals(1, c.getInt("a", null, "y", 1));

		assertNull(c.getString("a", null, "z"));
		assertArrayEquals(new String[]{}, c.getStringList("a", null, "z"));
	}

	@Test
	public void testSetStringListWithEmptyValue() throws Exception {
		Config c = new Config();
		c.setStringList("a", null, "x", Arrays.asList(""));
		assertArrayEquals(new String[]{""}, c.getStringList("a", null, "x"));
	}

	@Test
	public void testEmptyValueAtEof() throws Exception {
		String text = "[a]\nx =";
		Config c = parse(text);
		assertNull(c.getString("a", null, "x"));
		assertArrayEquals(new String[]{null},
				c.getStringList("a", null, "x"));
		c = parse(text + "\n");
		assertNull(c.getString("a", null, "x"));
		assertArrayEquals(new String[]{null},
				c.getStringList("a", null, "x"));
	}

	@Test
	public void testReadMultipleValuesForName() throws ConfigInvalidException {
		Config c = parse("[foo]\nbar=false\nbar=true\n");
		assertTrue(c.getBoolean("foo", "bar", false));
	}

	@Test
	public void testIncludeInvalidName() {
		assertThrows(JGitText.get().invalidLineInConfigFile,
				ConfigInvalidException.class, () -> parse("[include]\nbar\n"));
	}

	@Test
	public void testIncludeNoValue() {
		assertThrows(JGitText.get().invalidLineInConfigFile,
				ConfigInvalidException.class, () -> parse("[include]\npath\n"));
	}

	@Test
	public void testIncludeEmptyValue() {
		assertThrows(JGitText.get().invalidLineInConfigFile,
				ConfigInvalidException.class,
				() -> parse("[include]\npath=\n"));
	}

	@Test
	public void testIncludeValuePathNotFound() throws ConfigInvalidException {
		// we do not expect an exception, included path not found are ignored
		String notFound = "/not/found";
		Config parsed = parse("[include]\npath=" + notFound + "\n");
		assertEquals(1, parsed.getSections().size());
		assertEquals(notFound, parsed.getString("include", null, "path"));
	}

	@Test
	public void testIncludeValuePathWithTilde() throws ConfigInvalidException {
		// we do not expect an exception, included path not supported are
		// ignored
		String notSupported = "~/someFile";
		Config parsed = parse("[include]\npath=" + notSupported + "\n");
		assertEquals(1, parsed.getSections().size());
		assertEquals(notSupported, parsed.getString("include", null, "path"));
	}

	@Test
	public void testIncludeValuePathRelative() throws ConfigInvalidException {
		// we do not expect an exception, included path not supported are
		// ignored
		String notSupported = "someRelativeFile";
		Config parsed = parse("[include]\npath=" + notSupported + "\n");
		assertEquals(1, parsed.getSections().size());
		assertEquals(notSupported, parsed.getString("include", null, "path"));
	}

	@Test
	public void testIncludeTooManyRecursions() throws IOException {
		File config = tmp.newFile("config");
		String include = "[include]\npath=" + pathToString(config) + "\n";
		Files.write(config.toPath(), include.getBytes(UTF_8));
		try {
			loadConfig(config);
			fail();
		} catch (ConfigInvalidException cie) {
			for (Throwable t = cie; t != null; t = t.getCause()) {
				if (t.getMessage()
						.equals(JGitText.get().tooManyIncludeRecursions)) {
					return;
				}
			}
			fail("Expected to find expected exception message: "
					+ JGitText.get().tooManyIncludeRecursions);
		}
	}

	@Test
	public void testIncludeIsNoop() throws IOException, ConfigInvalidException {
		File config = tmp.newFile("config");

		String fooBar = "[foo]\nbar=true\n";
		Files.write(config.toPath(), fooBar.getBytes(UTF_8));

		Config parsed = parse("[include]\npath=" + pathToString(config) + "\n");
		assertFalse(parsed.getBoolean("foo", "bar", false));
	}

	@Test
	public void testIncludeCaseInsensitiveSection()
			throws IOException, ConfigInvalidException {
		File included = tmp.newFile("included");
		String content = "[foo]\nbar=true\n";
		Files.write(included.toPath(), content.getBytes(UTF_8));

		File config = tmp.newFile("config");
		content = "[Include]\npath=" + pathToString(included) + "\n";
		Files.write(config.toPath(), content.getBytes(UTF_8));

		FileBasedConfig fbConfig = loadConfig(config);
		assertTrue(fbConfig.getBoolean("foo", "bar", false));
	}

	@Test
	public void testIncludeCaseInsensitiveKey()
			throws IOException, ConfigInvalidException {
		File included = tmp.newFile("included");
		String content = "[foo]\nbar=true\n";
		Files.write(included.toPath(), content.getBytes(UTF_8));

		File config = tmp.newFile("config");
		content = "[include]\nPath=" + pathToString(included) + "\n";
		Files.write(config.toPath(), content.getBytes(UTF_8));

		FileBasedConfig fbConfig = loadConfig(config);
		assertTrue(fbConfig.getBoolean("foo", "bar", false));
	}

	@Test
	public void testIncludeExceptionContainsLine() {
		try {
			parse("[include]\npath=\n");
			fail("Expected ConfigInvalidException");
		} catch (ConfigInvalidException e) {
			assertTrue(
					"Expected to find the problem line in the exception message",
					e.getMessage().contains("include.path"));
		}
	}

	@Test
	public void testIncludeExceptionContainsFile() throws IOException {
		File included = tmp.newFile("included");
		String includedPath = pathToString(included);
		String content = "[include]\npath=\n";
		Files.write(included.toPath(), content.getBytes(UTF_8));

		File config = tmp.newFile("config");
		String include = "[include]\npath=" + includedPath + "\n";
		Files.write(config.toPath(), include.getBytes(UTF_8));
		try {
			loadConfig(config);
			fail("Expected ConfigInvalidException");
		} catch (ConfigInvalidException e) {
			// Check that there is some exception in the chain that contains
			// includedPath
			for (Throwable t = e; t != null; t = t.getCause()) {
				if (t.getMessage().contains(includedPath)) {
					return;
				}
			}
			fail("Expected to find the path in the exception message: "
					+ includedPath);
		}
	}

	@Test
	public void testIncludeSetValueMustNotTouchIncludedLines1()
			throws IOException, ConfigInvalidException {
		File includedFile = createAllTypesIncludedContent();

		File configFile = tmp.newFile("config");
		String content = createAllTypesSampleContent("Alice Parker", false, 11,
				21, 31, CoreConfig.AutoCRLF.FALSE,
				"+refs/heads/*:refs/remotes/origin/*") + "\n[include]\npath="
				+ pathToString(includedFile);
		Files.write(configFile.toPath(), content.getBytes(UTF_8));

		FileBasedConfig fbConfig = loadConfig(configFile);
		assertValuesAsIncluded(fbConfig, REFS_ORIGIN, REFS_UPSTREAM);
		assertSections(fbConfig, "user", "core", "remote", "include");

		setAllValuesNew(fbConfig);
		assertValuesAsIsSaveLoad(fbConfig, config -> {
			assertValuesAsIncluded(config, REFS_BACKUP, REFS_UPSTREAM);
			assertSections(fbConfig, "user", "core", "remote", "include");
		});
	}

	@Test
	public void testIncludeSetValueMustNotTouchIncludedLines2()
			throws IOException, ConfigInvalidException {
		File includedFile = createAllTypesIncludedContent();

		File configFile = tmp.newFile("config");
		String content = "[include]\npath=" + pathToString(includedFile) + "\n"
				+ createAllTypesSampleContent("Alice Parker", false, 11, 21, 31,
						CoreConfig.AutoCRLF.FALSE,
						"+refs/heads/*:refs/remotes/origin/*");
		Files.write(configFile.toPath(), content.getBytes(UTF_8));

		FileBasedConfig fbConfig = loadConfig(configFile);
		assertValuesAsConfig(fbConfig, REFS_UPSTREAM, REFS_ORIGIN);
		assertSections(fbConfig, "include", "user", "core", "remote");

		setAllValuesNew(fbConfig);
		assertValuesAsIsSaveLoad(fbConfig, config -> {
			assertValuesAsNew(config, REFS_UPSTREAM, REFS_BACKUP);
			assertSections(fbConfig, "include", "user", "core", "remote");
		});
	}

	@Test
	public void testIncludeSetValueOnFileWithJustContainsInclude()
			throws IOException, ConfigInvalidException {
		File includedFile = createAllTypesIncludedContent();

		File configFile = tmp.newFile("config");
		String content = "[include]\npath=" + pathToString(includedFile);
		Files.write(configFile.toPath(), content.getBytes(UTF_8));

		FileBasedConfig fbConfig = loadConfig(configFile);
		assertValuesAsIncluded(fbConfig, REFS_UPSTREAM);
		assertSections(fbConfig, "include", "user", "core", "remote");

		setAllValuesNew(fbConfig);
		assertValuesAsIsSaveLoad(fbConfig, config -> {
			assertValuesAsNew(config, REFS_UPSTREAM, REFS_BACKUP);
			assertSections(fbConfig, "include", "user", "core", "remote");
		});
	}

	@Test
	public void testIncludeSetValueOnFileWithJustEmptySection1()
			throws IOException, ConfigInvalidException {
		File includedFile = createAllTypesIncludedContent();

		File configFile = tmp.newFile("config");
		String content = "[user]\n[include]\npath="
				+ pathToString(includedFile);
		Files.write(configFile.toPath(), content.getBytes(UTF_8));

		FileBasedConfig fbConfig = loadConfig(configFile);
		assertValuesAsIncluded(fbConfig, REFS_UPSTREAM);
		assertSections(fbConfig, "user", "include", "core", "remote");

		setAllValuesNew(fbConfig);
		assertValuesAsIsSaveLoad(fbConfig, config -> {
			assertValuesAsNewWithName(config, "Alice Muller", REFS_UPSTREAM,
					REFS_BACKUP);
			assertSections(fbConfig, "user", "include", "core", "remote");
		});
	}

	@Test
	public void testIncludeSetValueOnFileWithJustEmptySection2()
			throws IOException, ConfigInvalidException {
		File includedFile = createAllTypesIncludedContent();

		File configFile = tmp.newFile("config");
		String content = "[include]\npath=" + pathToString(includedFile)
				+ "\n[user]";
		Files.write(configFile.toPath(), content.getBytes(UTF_8));

		FileBasedConfig fbConfig = loadConfig(configFile);
		assertValuesAsIncluded(fbConfig, REFS_UPSTREAM);
		assertSections(fbConfig, "include", "user", "core", "remote");

		setAllValuesNew(fbConfig);
		assertValuesAsIsSaveLoad(fbConfig, config -> {
			assertValuesAsNew(config, REFS_UPSTREAM, REFS_BACKUP);
			assertSections(fbConfig, "include", "user", "core", "remote");
		});
	}

	@Test
	public void testIncludeSetValueOnFileWithJustExistingSection1()
			throws IOException, ConfigInvalidException {
		File includedFile = createAllTypesIncludedContent();

		File configFile = tmp.newFile("config");
		String content = "[user]\nemail=alice@home\n[include]\npath="
				+ pathToString(includedFile);
		Files.write(configFile.toPath(), content.getBytes(UTF_8));

		FileBasedConfig fbConfig = loadConfig(configFile);
		assertValuesAsIncluded(fbConfig, REFS_UPSTREAM);
		assertSections(fbConfig, "user", "include", "core", "remote");

		setAllValuesNew(fbConfig);
		assertValuesAsIsSaveLoad(fbConfig, config -> {
			assertValuesAsNewWithName(config, "Alice Muller", REFS_UPSTREAM,
					REFS_BACKUP);
			assertSections(fbConfig, "user", "include", "core", "remote");
		});
	}

	@Test
	public void testIncludeSetValueOnFileWithJustExistingSection2()
			throws IOException, ConfigInvalidException {
		File includedFile = createAllTypesIncludedContent();

		File configFile = tmp.newFile("config");
		String content = "[include]\npath=" + pathToString(includedFile)
				+ "\n[user]\nemail=alice@home\n";
		Files.write(configFile.toPath(), content.getBytes(UTF_8));

		FileBasedConfig fbConfig = loadConfig(configFile);
		assertValuesAsIncluded(fbConfig, REFS_UPSTREAM);
		assertSections(fbConfig, "include", "user", "core", "remote");

		setAllValuesNew(fbConfig);
		assertValuesAsIsSaveLoad(fbConfig, config -> {
			assertValuesAsNew(config, REFS_UPSTREAM, REFS_BACKUP);
			assertSections(fbConfig, "include", "user", "core", "remote");
		});
	}

	@Test
	public void testIncludeUnsetSectionMustNotTouchIncludedLines()
			throws IOException, ConfigInvalidException {
		File includedFile = tmp.newFile("included");
		RefSpec includedRefSpec = new RefSpec(REFS_UPSTREAM);
		String includedContent = "[remote \"origin\"]\n" + "fetch="
				+ includedRefSpec;
		Files.write(includedFile.toPath(), includedContent.getBytes(UTF_8));

		File configFile = tmp.newFile("config");
		RefSpec refSpec = new RefSpec(REFS_ORIGIN);
		String content = "[include]\npath=" + pathToString(includedFile) + "\n"
				+ "[remote \"origin\"]\n" + "fetch=" + refSpec;
		Files.write(configFile.toPath(), content.getBytes(UTF_8));

		FileBasedConfig fbConfig = loadConfig(configFile);

		Consumer<FileBasedConfig> assertion = config -> {
			assertEquals(Arrays.asList(includedRefSpec, refSpec),
					config.getRefSpecs("remote", "origin", "fetch"));
		};
		assertion.accept(fbConfig);

		fbConfig.unsetSection("remote", "origin");
		assertValuesAsIsSaveLoad(fbConfig, config -> {
			assertEquals(Collections.singletonList(includedRefSpec),
					config.getRefSpecs("remote", "origin", "fetch"));
		});
	}

	private File createAllTypesIncludedContent() throws IOException {
		File includedFile = tmp.newFile("included");
		String includedContent = createAllTypesSampleContent("Alice Muller",
				true, 10, 20, 30, CoreConfig.AutoCRLF.TRUE,
				"+refs/heads/*:refs/remotes/upstream/*");
		Files.write(includedFile.toPath(), includedContent.getBytes(UTF_8));
		return includedFile;
	}

	private static void assertValuesAsIsSaveLoad(FileBasedConfig fbConfig,
			Consumer<FileBasedConfig> assertion)
			throws IOException, ConfigInvalidException {
		assertion.accept(fbConfig);

		fbConfig.save();
		assertion.accept(fbConfig);

		fbConfig = loadConfig(fbConfig.getFile());
		assertion.accept(fbConfig);
	}

	private static void setAllValuesNew(Config config) {
		config.setString("user", null, "name", "Alice Bauer");
		config.setBoolean("core", null, "fileMode", false);
		config.setInt("core", null, "deltaBaseCacheLimit", 12);
		config.setLong("core", null, "packedGitLimit", 22);
		config.setLong("core", null, "repositoryCacheExpireAfter", 32);
		config.setEnum("core", null, "autocrlf", CoreConfig.AutoCRLF.FALSE);
		config.setString("remote", "origin", "fetch",
				"+refs/heads/*:refs/remotes/backup/*");
	}

	private static void assertValuesAsIncluded(Config config, String... refs) {
		assertAllTypesSampleContent("Alice Muller", true, 10, 20, 30,
				CoreConfig.AutoCRLF.TRUE, config, refs);
	}

	private static void assertValuesAsConfig(Config config, String... refs) {
		assertAllTypesSampleContent("Alice Parker", false, 11, 21, 31,
				CoreConfig.AutoCRLF.FALSE, config, refs);
	}

	private static void assertValuesAsNew(Config config, String... refs) {
		assertValuesAsNewWithName(config, "Alice Bauer", refs);
	}

	private static void assertValuesAsNewWithName(Config config, String name,
			String... refs) {
		assertAllTypesSampleContent(name, false, 12, 22, 32,
				CoreConfig.AutoCRLF.FALSE, config, refs);
	}

	private static void assertSections(Config config, String... sections) {
		assertEquals(Arrays.asList(sections),
				new ArrayList<>(config.getSections()));
	}

	private static String createAllTypesSampleContent(String name,
			boolean fileMode, int deltaBaseCacheLimit, long packedGitLimit,
			long repositoryCacheExpireAfter, CoreConfig.AutoCRLF autoCRLF,
			String fetchRefSpec) {
		final StringBuilder builder = new StringBuilder();
		builder.append("[user]\n");
		builder.append("name=");
		builder.append(name);
		builder.append("\n");

		builder.append("[core]\n");
		builder.append("fileMode=");
		builder.append(fileMode);
		builder.append("\n");

		builder.append("deltaBaseCacheLimit=");
		builder.append(deltaBaseCacheLimit);
		builder.append("\n");

		builder.append("packedGitLimit=");
		builder.append(packedGitLimit);
		builder.append("\n");

		builder.append("repositoryCacheExpireAfter=");
		builder.append(repositoryCacheExpireAfter);
		builder.append("\n");

		builder.append("autocrlf=");
		builder.append(autoCRLF.name());
		builder.append("\n");

		builder.append("[remote \"origin\"]\n");
		builder.append("fetch=");
		builder.append(fetchRefSpec);
		builder.append("\n");
		return builder.toString();
	}

	private static void assertAllTypesSampleContent(String name,
			boolean fileMode, int deltaBaseCacheLimit, long packedGitLimit,
			long repositoryCacheExpireAfter, CoreConfig.AutoCRLF autoCRLF,
			Config config, String... fetchRefSpecs) {
		assertEquals(name, config.getString("user", null, "name"));
		assertEquals(fileMode,
				config.getBoolean("core", "fileMode", !fileMode));
		assertEquals(deltaBaseCacheLimit,
				config.getInt("core", "deltaBaseCacheLimit", -1));
		assertEquals(packedGitLimit,
				config.getLong("core", "packedGitLimit", -1));
		assertEquals(repositoryCacheExpireAfter, config.getTimeUnit("core",
				null, "repositoryCacheExpireAfter", -1, MILLISECONDS));
		assertEquals(autoCRLF, config.getEnum("core", null, "autocrlf",
				CoreConfig.AutoCRLF.INPUT));
		final List<RefSpec> refspecs = new ArrayList<>();
		for (String fetchRefSpec : fetchRefSpecs) {
			refspecs.add(new RefSpec(fetchRefSpec));
		}

		assertEquals(refspecs, config.getRefSpecs("remote", "origin", "fetch"));
	}

	private static void assertReadLong(long exp) throws ConfigInvalidException {
		assertReadLong(exp, String.valueOf(exp));
	}

	private static void assertReadLong(long exp, String act)
			throws ConfigInvalidException {
		final Config c = parse("[s]\na = " + act + "\n");
		assertEquals(exp, c.getLong("s", null, "a", 0L));
	}

	static Config parse(String content)
			throws ConfigInvalidException {
		return parse(content, null);
	}

	private static Config parse(String content, Config baseConfig)
			throws ConfigInvalidException {
		final Config c = new Config(baseConfig);
		c.fromText(content);
		return c;
	}

	@Test
	public void testTimeUnit() throws ConfigInvalidException {
		assertEquals(0, parseTime("0", NANOSECONDS));
		assertEquals(2, parseTime("2ns", NANOSECONDS));
		assertEquals(200, parseTime("200 nanoseconds", NANOSECONDS));

		assertEquals(0, parseTime("0", MICROSECONDS));
		assertEquals(2, parseTime("2us", MICROSECONDS));
		assertEquals(2, parseTime("2000 nanoseconds", MICROSECONDS));
		assertEquals(200, parseTime("200 microseconds", MICROSECONDS));

		assertEquals(0, parseTime("0", MILLISECONDS));
		assertEquals(2, parseTime("2ms", MILLISECONDS));
		assertEquals(2, parseTime("2000microseconds", MILLISECONDS));
		assertEquals(200, parseTime("200 milliseconds", MILLISECONDS));

		assertEquals(0, parseTime("0s", SECONDS));
		assertEquals(2, parseTime("2s", SECONDS));
		assertEquals(231, parseTime("231sec", SECONDS));
		assertEquals(1, parseTime("1second", SECONDS));
		assertEquals(300, parseTime("300 seconds", SECONDS));

		assertEquals(2, parseTime("2m", MINUTES));
		assertEquals(2, parseTime("2min", MINUTES));
		assertEquals(1, parseTime("1 minute", MINUTES));
		assertEquals(10, parseTime("10 minutes", MINUTES));

		assertEquals(5, parseTime("5h", HOURS));
		assertEquals(5, parseTime("5hr", HOURS));
		assertEquals(1, parseTime("1hour", HOURS));
		assertEquals(48, parseTime("48hours", HOURS));

		assertEquals(5, parseTime("5 h", HOURS));
		assertEquals(5, parseTime("5 hr", HOURS));
		assertEquals(1, parseTime("1 hour", HOURS));
		assertEquals(48, parseTime("48 hours", HOURS));
		assertEquals(48, parseTime("48 \t \r hours", HOURS));

		assertEquals(4, parseTime("4d", DAYS));
		assertEquals(1, parseTime("1day", DAYS));
		assertEquals(14, parseTime("14days", DAYS));

		assertEquals(7, parseTime("1w", DAYS));
		assertEquals(7, parseTime("1week", DAYS));
		assertEquals(14, parseTime("2w", DAYS));
		assertEquals(14, parseTime("2weeks", DAYS));

		assertEquals(30, parseTime("1mon", DAYS));
		assertEquals(30, parseTime("1month", DAYS));
		assertEquals(60, parseTime("2mon", DAYS));
		assertEquals(60, parseTime("2months", DAYS));

		assertEquals(365, parseTime("1y", DAYS));
		assertEquals(365, parseTime("1year", DAYS));
		assertEquals(365 * 2, parseTime("2years", DAYS));
	}

	private long parseTime(String value, TimeUnit unit)
			throws ConfigInvalidException {
		Config c = parse("[a]\na=" + value + "\n");
		return c.getTimeUnit("a", null, "a", 0, unit);
	}

	@Test
	public void testTimeUnitDefaultValue() throws ConfigInvalidException {
		// value not present
		assertEquals(20, parse("[a]\na=0\n").getTimeUnit("a", null, "b", 20,
				MILLISECONDS));
		// value is empty
		assertEquals(20, parse("[a]\na=\" \"\n").getTimeUnit("a", null, "a", 20,
				MILLISECONDS));

		// value is not numeric
		assertEquals(20, parse("[a]\na=test\n").getTimeUnit("a", null, "a", 20,
				MILLISECONDS));
	}

	@Test
	public void testTimeUnitInvalid() {
		assertThrows("Invalid time unit value: a.a=1 monttthhh",
				IllegalArgumentException.class,
				() -> parseTime("1 monttthhh", DAYS));
	}

	@Test
	public void testTimeUnitInvalidWithSection() throws ConfigInvalidException {
		Config c = parse("[a \"b\"]\na=1 monttthhh\n");
		assertThrows("Invalid time unit value: a.b.a=1 monttthhh",
				IllegalArgumentException.class,
				() -> c.getTimeUnit("a", "b", "a", 0, DAYS));
	}

	@Test
	public void testTimeUnitNegative() {
		assertThrows(IllegalArgumentException.class,
				() -> parseTime("-1", MILLISECONDS));
	}

	@Test
	public void testEscapeSpacesOnly() throws ConfigInvalidException {
		// Empty string is read back as null, so this doesn't round-trip.
		assertEquals("", Config.escapeValue(""));

		assertValueRoundTrip(" ", "\" \"");
		assertValueRoundTrip("  ", "\"  \"");
	}

	@Test
	public void testEscapeLeadingSpace() throws ConfigInvalidException {
		assertValueRoundTrip("x", "x");
		assertValueRoundTrip(" x", "\" x\"");
		assertValueRoundTrip("  x", "\"  x\"");
	}

	@Test
	public void testEscapeTrailingSpace() throws ConfigInvalidException {
		assertValueRoundTrip("x", "x");
		assertValueRoundTrip("x  ","\"x  \"");
		assertValueRoundTrip("x ","\"x \"");
	}

	@Test
	public void testEscapeLeadingAndTrailingSpace()
			throws ConfigInvalidException {
		assertValueRoundTrip(" x ", "\" x \"");
		assertValueRoundTrip("  x ", "\"  x \"");
		assertValueRoundTrip(" x  ", "\" x  \"");
		assertValueRoundTrip("  x  ", "\"  x  \"");
	}

	@Test
	public void testNoEscapeInternalSpaces() throws ConfigInvalidException {
		assertValueRoundTrip("x y");
		assertValueRoundTrip("x  y");
		assertValueRoundTrip("x  y");
		assertValueRoundTrip("x  y   z");
		assertValueRoundTrip("x " + WS + " y");
	}

	@Test
	public void testNoEscapeSpecialCharacters() throws ConfigInvalidException {
		assertValueRoundTrip("x\\y", "x\\\\y");
		assertValueRoundTrip("x\"y", "x\\\"y");
		assertValueRoundTrip("x\ny", "x\\ny");
		assertValueRoundTrip("x\ty", "x\\ty");
		assertValueRoundTrip("x\by", "x\\by");
	}

	@Test
	public void testParseLiteralBackspace() throws ConfigInvalidException {
		// This is round-tripped with an escape sequence by JGit, but C git writes
		// it out as a literal backslash.
		assertEquals("x\by", parseEscapedValue("x\by"));
	}

	@Test
	public void testEscapeCommentCharacters() throws ConfigInvalidException {
		assertValueRoundTrip("x#y", "\"x#y\"");
		assertValueRoundTrip("x;y", "\"x;y\"");
	}

	@Test
	public void testEscapeValueInvalidCharacters() {
		assertIllegalArgumentException(() -> Config.escapeSubsection("x\0y"));
	}

	@Test
	public void testEscapeSubsectionInvalidCharacters() {
		assertIllegalArgumentException(() -> Config.escapeSubsection("x\ny"));
		assertIllegalArgumentException(() -> Config.escapeSubsection("x\0y"));
	}

	@Test
	public void testParseMultipleQuotedRegions() throws ConfigInvalidException {
		assertEquals("b a z; \n", parseEscapedValue("b\" a\"\" z; \\n\""));
	}

	@Test
	public void testParseComments() throws ConfigInvalidException {
		assertEquals("baz", parseEscapedValue("baz; comment"));
		assertEquals("baz", parseEscapedValue("baz# comment"));
		assertEquals("baz", parseEscapedValue("baz ; comment"));
		assertEquals("baz", parseEscapedValue("baz # comment"));

		assertEquals("baz", parseEscapedValue("baz ; comment"));
		assertEquals("baz", parseEscapedValue("baz # comment"));
		assertEquals("baz", parseEscapedValue("baz " + WS + " ; comment"));
		assertEquals("baz", parseEscapedValue("baz " + WS + " # comment"));

		assertEquals("baz ", parseEscapedValue("\"baz \"; comment"));
		assertEquals("baz ", parseEscapedValue("\"baz \"# comment"));
		assertEquals("baz ", parseEscapedValue("\"baz \" ; comment"));
		assertEquals("baz ", parseEscapedValue("\"baz \" # comment"));
	}

	@Test
	public void testEscapeSubsection() throws ConfigInvalidException {
		assertSubsectionRoundTrip("", "\"\"");
		assertSubsectionRoundTrip("x", "\"x\"");
		assertSubsectionRoundTrip(" x", "\" x\"");
		assertSubsectionRoundTrip("x ", "\"x \"");
		assertSubsectionRoundTrip(" x ", "\" x \"");
		assertSubsectionRoundTrip("x y", "\"x y\"");
		assertSubsectionRoundTrip("x  y", "\"x  y\"");
		assertSubsectionRoundTrip("x\\y", "\"x\\\\y\"");
		assertSubsectionRoundTrip("x\"y", "\"x\\\"y\"");

		// Unlike for values, \b and \t are not escaped.
		assertSubsectionRoundTrip("x\by", "\"x\by\"");
		assertSubsectionRoundTrip("x\ty", "\"x\ty\"");
	}

	@Test
	public void testParseInvalidValues() {
		assertInvalidValue(JGitText.get().newlineInQuotesNotAllowed, "x\"\n\"y");
		assertInvalidValue(JGitText.get().endOfFileInEscape, "x\\");
		assertInvalidValue(
				MessageFormat.format(JGitText.get().badEscape, 'q'), "x\\q");
	}

	@Test
	public void testParseInvalidSubsections() {
		assertInvalidSubsection(
				JGitText.get().newlineInQuotesNotAllowed, "\"x\ny\"");
	}

	@Test
	public void testDropBackslashFromInvalidEscapeSequenceInSubsectionName()
			throws ConfigInvalidException {
		assertEquals("x0", parseEscapedSubsection("\"x\\0\""));
		assertEquals("xq", parseEscapedSubsection("\"x\\q\""));
		// Unlike for values, \b, \n, and \t are not valid escape sequences.
		assertEquals("xb", parseEscapedSubsection("\"x\\b\""));
		assertEquals("xn", parseEscapedSubsection("\"x\\n\""));
		assertEquals("xt", parseEscapedSubsection("\"x\\t\""));
	}

	@Test
	public void testInvalidGroupHeader() {
		assertThrows(JGitText.get().badGroupHeader,
				ConfigInvalidException.class,
				() -> parse("[foo \"bar\" ]\nfoo=bar\n"));
	}

	@Test
	public void testCrLf() throws ConfigInvalidException {
		assertEquals("true", parseEscapedValue("true\r\n"));
	}

	@Test
	public void testLfContinuation() throws ConfigInvalidException {
		assertEquals("true", parseEscapedValue("tr\\\nue"));
	}

	@Test
	public void testCrCharContinuation() {
		assertThrows("Bad escape: \\u000d", ConfigInvalidException.class,
				() -> parseEscapedValue("tr\\\rue"));
	}

	@Test
	public void testCrEOFContinuation() {
		assertThrows("Bad escape: \\u000d", ConfigInvalidException.class,
				() -> parseEscapedValue("tr\\\r"));
	}

	@Test
	public void testCrLfContinuation() throws ConfigInvalidException {
		assertEquals("true", parseEscapedValue("tr\\\r\nue"));
	}

	@Test
	public void testWhitespaceContinuation() throws ConfigInvalidException {
		assertEquals("tr   ue", parseEscapedValue("tr \\\n  ue"));
		assertEquals("tr   ue", parseEscapedValue("tr \\\r\n  ue"));
	}

	@Test
	public void testCommitTemplateEmptyConfig()
			throws ConfigInvalidException, IOException {
		// no values defined nowhere
		Config config = new Config(null);
		assertNull(config.get(CommitConfig.KEY).getCommitTemplatePath());
		assertNull(config.get(CommitConfig.KEY).getCommitTemplateContent());
	}

	@Test
	public void testCommitTemplateConfig()
			throws ConfigInvalidException, IOException {

		File tempFile = tmp.newFile("testCommitTemplate-");
		String templateContent = "content of the template";
		JGitTestUtil.write(tempFile, templateContent);
		String expectedTemplatePath = tempFile.getPath();

		Config config = parse(
				"[commit]\n\ttemplate = " + expectedTemplatePath + "\n");

		String templatePath = config.get(CommitConfig.KEY)
				.getCommitTemplatePath();
		String commitEncoding = config.get(CommitConfig.KEY)
				.getCommitEncoding();
		assertEquals(expectedTemplatePath, templatePath);
		assertEquals(templateContent,
				config.get(CommitConfig.KEY).getCommitTemplateContent());
		assertNull("no commitEncoding has been set so it must be null",
				commitEncoding);
	}

	@Test
	public void testCommitTemplateEncoding()
			throws ConfigInvalidException, IOException {
		Config config = new Config(null);
		File tempFile = tmp.newFile("testCommitTemplate-");
		String templateContent = "content of the template";
		JGitTestUtil.write(tempFile, templateContent);
		String expectedTemplatePath = tempFile.getPath();
		config = parse("[i18n]\n\tcommitEncoding = utf-8\n"
				+ "[commit]\n\ttemplate = " + expectedTemplatePath + "\n");
		assertEquals(templateContent,
				config.get(CommitConfig.KEY).getCommitTemplateContent());
		String commitEncoding = config.get(CommitConfig.KEY)
				.getCommitEncoding();
		assertEquals("commitEncoding has been set to utf-8 it must be utf-8",
				"utf-8", commitEncoding);
	}

	@Test(expected = ConfigInvalidException.class)
	public void testCommitTemplateWithInvalidEncoding()
			throws ConfigInvalidException, IOException {
		Config config = new Config(null);
		File tempFile = tmp.newFile("testCommitTemplate-");
		String templateContent = "content of the template";
		JGitTestUtil.write(tempFile, templateContent);
		config = parse("[i18n]\n\tcommitEncoding = invalidEcoding\n"
				+ "[commit]\n\ttemplate = " + tempFile.getPath() + "\n");
		config.get(CommitConfig.KEY).getCommitTemplateContent();
	}

	@Test(expected = FileNotFoundException.class)
	public void testCommitTemplateWithInvalidPath()
			throws ConfigInvalidException, IOException {
		Config config = new Config(null);
		File tempFile = tmp.newFile("testCommitTemplate-");
		String templateContent = "content of the template";
		JGitTestUtil.write(tempFile, templateContent);
		// commit message encoding
		String expectedTemplatePath = "nonExistingTemplate";
		config = parse("[commit]\n\ttemplate = " + expectedTemplatePath + "\n");
		String templatePath = config.get(CommitConfig.KEY)
				.getCommitTemplatePath();
		assertEquals(expectedTemplatePath, templatePath);
		config.get(CommitConfig.KEY).getCommitTemplateContent();
	}

	private static void assertValueRoundTrip(String value)
			throws ConfigInvalidException {
		assertValueRoundTrip(value, value);
	}

	private static void assertValueRoundTrip(String value, String expectedEscaped)
			throws ConfigInvalidException {
		String escaped = Config.escapeValue(value);
		assertEquals("escape failed;", expectedEscaped, escaped);
		assertEquals("parse failed;", value, parseEscapedValue(escaped));
	}

	private static String parseEscapedValue(String escapedValue)
			throws ConfigInvalidException {
		String text = "[foo]\nbar=" + escapedValue;
		Config c = parse(text);
		return c.getString("foo", null, "bar");
	}

	private static void assertInvalidValue(String expectedMessage,
			String escapedValue) {
		try {
			parseEscapedValue(escapedValue);
			fail("expected ConfigInvalidException");
		} catch (ConfigInvalidException e) {
			assertEquals(expectedMessage, e.getMessage());
		}
	}

	private static void assertSubsectionRoundTrip(String subsection,
			String expectedEscaped) throws ConfigInvalidException {
		String escaped = Config.escapeSubsection(subsection);
		assertEquals("escape failed;", expectedEscaped, escaped);
		assertEquals("parse failed;", subsection, parseEscapedSubsection(escaped));
	}

	private static String parseEscapedSubsection(String escapedSubsection)
			throws ConfigInvalidException {
		String text = "[foo " + escapedSubsection + "]\nbar = value";
		Config c = parse(text);
		Set<String> subsections = c.getSubsections("foo");
		assertEquals("only one section", 1, subsections.size());
		return subsections.iterator().next();
	}

	private static void assertIllegalArgumentException(Runnable r) {
		try {
			r.run();
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// Expected.
		}
	}

	private static void assertInvalidSubsection(String expectedMessage,
			String escapedSubsection) {
		try {
			parseEscapedSubsection(escapedSubsection);
			fail("expected ConfigInvalidException");
		} catch (ConfigInvalidException e) {
			assertEquals(expectedMessage, e.getMessage());
		}
	}

	private static FileBasedConfig loadConfig(File file)
			throws IOException, ConfigInvalidException {
		final FileBasedConfig config = new FileBasedConfig(null, file,
				FS.DETECTED);
		config.load();
		return config;
	}
}
