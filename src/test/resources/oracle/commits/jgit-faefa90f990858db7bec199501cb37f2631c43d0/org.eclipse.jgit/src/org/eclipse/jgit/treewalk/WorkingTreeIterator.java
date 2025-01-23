/*
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2012, 2022, Robin Rosenberg and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.treewalk;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.api.errors.FilterFailedException;
import org.eclipse.jgit.attributes.AttributesNode;
import org.eclipse.jgit.attributes.AttributesRule;
import org.eclipse.jgit.attributes.FilterCommand;
import org.eclipse.jgit.attributes.FilterCommandRegistry;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.ignore.FastIgnoreRule;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.CheckStat;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.CoreConfig.SymLinks;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.treewalk.TreeWalk.OperationType;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS.ExecutionResult;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.Holder;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.Paths;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.jgit.util.TemporaryBuffer;
import org.eclipse.jgit.util.TemporaryBuffer.LocalFile;
import org.eclipse.jgit.util.io.EolStreamTypeUtil;
import org.eclipse.jgit.util.sha1.SHA1;

/**
 * Walks a working directory tree as part of a
 * {@link org.eclipse.jgit.treewalk.TreeWalk}.
 * <p>
 * Most applications will want to use the standard implementation of this
 * iterator, {@link org.eclipse.jgit.treewalk.FileTreeIterator}, as that does
 * all IO through the standard <code>java.io</code> package. Plugins for a Java
 * based IDE may however wish to create their own implementations of this class
 * to allow traversal of the IDE's project space, as well as benefit from any
 * caching the IDE may have.
 *
 * @see FileTreeIterator
 */
public abstract class WorkingTreeIterator extends AbstractTreeIterator {
	private static final int MAX_EXCEPTION_TEXT_SIZE = 10 * 1024;

	/** An empty entry array, suitable for {@link #init(Entry[])}. */
	protected static final Entry[] EOF = {};

	/** Size we perform file IO in if we have to read and hash a file. */
	static final int BUFFER_SIZE = 2048;

	/**
	 * Maximum size of files which may be read fully into memory for performance
	 * reasons.
	 */
	private static final long MAXIMUM_FILE_SIZE_TO_READ_FULLY = 65536;

	/** Inherited state of this iterator, describing working tree, etc. */
	private final IteratorState state;

	/** The {@link #idBuffer()} for the current entry. */
	private byte[] contentId;

	/** Index within {@link #entries} that {@link #contentId} came from. */
	private int contentIdFromPtr;

	/** List of entries obtained from the subclass. */
	private Entry[] entries;

	/** Total number of entries in {@link #entries} that are valid. */
	private int entryCnt;

	/** Current position within {@link #entries}. */
	private int ptr;

	/** If there is a .gitignore file present, the parsed rules from it. */
	private IgnoreNode ignoreNode;

	/**
	 * cached clean filter command. Use a Ref in order to distinguish between
	 * the ref not cached yet and the value null
	 */
	private Holder<String> cleanFilterCommandHolder;

	/**
	 * cached eol stream type. Use a Ref in order to distinguish between the ref
	 * not cached yet and the value null
	 */
	private Holder<EolStreamType> eolStreamTypeHolder;

	/** Repository that is the root level being iterated over */
	protected Repository repository;

	/** Cached canonical length, initialized from {@link #idBuffer()} */
	private long canonLen = -1;

	/** The offset of the content id in {@link #idBuffer()} */
	private int contentIdOffset;

	/** A comparator for {@link Instant}s. */
	private final InstantComparator timestampComparator = new InstantComparator();

	/**
	 * Create a new iterator with no parent.
	 *
	 * @param options
	 *            working tree options to be used
	 */
	protected WorkingTreeIterator(WorkingTreeOptions options) {
		super();
		state = new IteratorState(options);
	}

	/**
	 * Create a new iterator with no parent and a prefix.
	 * <p>
	 * The prefix path supplied is inserted in front of all paths generated by
	 * this iterator. It is intended to be used when an iterator is being
	 * created for a subsection of an overall repository and needs to be
	 * combined with other iterators that are created to run over the entire
	 * repository namespace.
	 *
	 * @param prefix
	 *            position of this iterator in the repository tree. The value
	 *            may be null or the empty string to indicate the prefix is the
	 *            root of the repository. A trailing slash ('/') is
	 *            automatically appended if the prefix does not end in '/'.
	 * @param options
	 *            working tree options to be used
	 */
	protected WorkingTreeIterator(final String prefix,
			WorkingTreeOptions options) {
		super(prefix);
		state = new IteratorState(options);
	}

	/**
	 * Create an iterator for a subtree of an existing iterator.
	 *
	 * @param p
	 *            parent tree iterator.
	 */
	protected WorkingTreeIterator(WorkingTreeIterator p) {
		super(p);
		state = p.state;
		repository = p.repository;
	}

	/**
	 * Initialize this iterator for the root level of a repository.
	 * <p>
	 * This method should only be invoked after calling {@link #init(Entry[])},
	 * and only for the root iterator.
	 *
	 * @param repo
	 *            the repository.
	 */
	protected void initRootIterator(Repository repo) {
		repository = repo;
		Entry entry;
		if (ignoreNode instanceof PerDirectoryIgnoreNode)
			entry = ((PerDirectoryIgnoreNode) ignoreNode).entry;
		else
			entry = null;
		ignoreNode = new RootIgnoreNode(entry, repo);
	}

	/**
	 * Define the matching {@link org.eclipse.jgit.dircache.DirCacheIterator},
	 * to optimize ObjectIds.
	 *
	 * Once the DirCacheIterator has been set this iterator must only be
	 * advanced by the TreeWalk that is supplied, as it assumes that itself and
	 * the corresponding DirCacheIterator are positioned on the same file path
	 * whenever {@link #idBuffer()} is invoked.
	 *
	 * @param walk
	 *            the walk that will be advancing this iterator.
	 * @param treeId
	 *            index of the matching
	 *            {@link org.eclipse.jgit.dircache.DirCacheIterator}.
	 */
	public void setDirCacheIterator(TreeWalk walk, int treeId) {
		state.walk = walk;
		state.dirCacheTree = treeId;
	}

	/**
	 * Retrieves the {@link DirCacheIterator} at the current entry if
	 * {@link #setDirCacheIterator(TreeWalk, int)} was called.
	 *
	 * @return the DirCacheIterator, or {@code null} if not set or not at the
	 *         current entry
	 * @since 5.0
	 */
	protected DirCacheIterator getDirCacheIterator() {
		if (state.dirCacheTree >= 0 && state.walk != null) {
			return state.walk.getTree(state.dirCacheTree,
					DirCacheIterator.class);
		}
		return null;
	}

	/**
	 * Defines whether this {@link WorkingTreeIterator} walks ignored
	 * directories.
	 *
	 * @param includeIgnored
	 *            {@code false} to skip ignored directories, if possible;
	 *            {@code true} to always include them in the walk
	 * @since 5.0
	 */
	public void setWalkIgnoredDirectories(boolean includeIgnored) {
		state.walkIgnored = includeIgnored;
	}

	/**
	 * Tells whether this {@link WorkingTreeIterator} walks ignored directories.
	 *
	 * @return {@code true} if it does, {@code false} otherwise
	 * @since 5.0
	 */
	public boolean walksIgnoredDirectories() {
		return state.walkIgnored;
	}

	@Override
	public boolean hasId() {
		if (contentIdFromPtr == ptr)
			return true;
		return (mode & FileMode.TYPE_MASK) == FileMode.TYPE_FILE;
	}

	@Override
	public byte[] idBuffer() {
		if (contentIdFromPtr == ptr)
			return contentId;

		if (state.walk != null) {
			// If there is a matching DirCacheIterator, we can reuse
			// its idBuffer, but only if we appear to be clean against
			// the cached index information for the path.
			DirCacheIterator i = state.walk.getTree(state.dirCacheTree,
							DirCacheIterator.class);
			if (i != null) {
				DirCacheEntry ent = i.getDirCacheEntry();
				if (ent != null && compareMetadata(ent) == MetadataDiff.EQUAL
						&& ((ent.getFileMode().getBits()
								& FileMode.TYPE_MASK) != FileMode.TYPE_GITLINK)) {
					contentIdOffset = i.idOffset();
					contentIdFromPtr = ptr;
					return contentId = i.idBuffer();
				}
				contentIdOffset = 0;
			} else {
				contentIdOffset = 0;
			}
		}
		switch (mode & FileMode.TYPE_MASK) {
		case FileMode.TYPE_SYMLINK:
		case FileMode.TYPE_FILE:
			contentIdFromPtr = ptr;
			return contentId = idBufferBlob(entries[ptr]);
		case FileMode.TYPE_GITLINK:
			contentIdFromPtr = ptr;
			return contentId = idSubmodule(entries[ptr]);
		}
		return zeroid;
	}

	@Override
	public boolean isWorkTree() {
		return true;
	}

	/**
	 * Get submodule id for given entry.
	 *
	 * @param e
	 *            a {@link org.eclipse.jgit.treewalk.WorkingTreeIterator.Entry}
	 *            object.
	 * @return non-null submodule id
	 */
	protected byte[] idSubmodule(Entry e) {
		if (repository == null)
			return zeroid;
		File directory;
		try {
			directory = repository.getWorkTree();
		} catch (NoWorkTreeException nwte) {
			return zeroid;
		}
		return idSubmodule(directory, e);
	}

	/**
	 * Get submodule id using the repository at the location of the entry
	 * relative to the directory.
	 *
	 * @param directory
	 *            a {@link java.io.File} object.
	 * @param e
	 *            a {@link org.eclipse.jgit.treewalk.WorkingTreeIterator.Entry}
	 *            object.
	 * @return non-null submodule id
	 */
	protected byte[] idSubmodule(File directory, Entry e) {
		try (Repository submoduleRepo = SubmoduleWalk.getSubmoduleRepository(
				directory, e.getName(),
				repository != null ? repository.getFS() : FS.DETECTED)) {
			if (submoduleRepo == null) {
				return zeroid;
			}
			ObjectId head = submoduleRepo.resolve(Constants.HEAD);
			if (head == null) {
				return zeroid;
			}
			byte[] id = new byte[Constants.OBJECT_ID_LENGTH];
			head.copyRawTo(id, 0);
			return id;
		} catch (IOException exception) {
			return zeroid;
		}
	}

	private static final byte[] digits = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9' };

	private static final byte[] hblob = Constants
			.encodedTypeString(Constants.OBJ_BLOB);

	private byte[] idBufferBlob(Entry e) {
		try {
			final InputStream is = e.openInputStream();
			if (is == null)
				return zeroid;
			try {
				state.initializeReadBuffer();

				final long len = e.getLength();
				InputStream filteredIs = possiblyFilteredInputStream(e, is,
						len);
				return computeHash(filteredIs, canonLen);
			} finally {
				safeClose(is);
			}
		} catch (IOException err) {
			// Can't read the file? Don't report the failure either.
			return zeroid;
		}
	}

	private long possiblyFilteredLength(Entry e, long len) throws IOException {
		if (getCleanFilterCommand() == null && getEolStreamType(
				OperationType.CHECKIN_OP) == EolStreamType.DIRECT) {
			return len;
		}

		if (len <= MAXIMUM_FILE_SIZE_TO_READ_FULLY) {
			InputStream is = e.openInputStream();
			try {
				ByteBuffer rawbuf = IO.readWholeStream(is, (int) len);
				rawbuf = filterClean(rawbuf.array(), rawbuf.limit());
				return rawbuf.limit();
			} finally {
				safeClose(is);
			}
		}

		if (getCleanFilterCommand() == null && isBinary(e)) {
			return len;
		}

		InputStream is = filterClean(e.openInputStream());
		try {
			return computeLength(is);
		} finally {
			safeClose(is);
		}
	}

	private InputStream possiblyFilteredInputStream(final Entry e,
			final InputStream is, final long len)
			throws IOException {
		if (getCleanFilterCommand() == null
				&& getEolStreamType(
						OperationType.CHECKIN_OP) == EolStreamType.DIRECT) {
			canonLen = len;
			return is;
		}

		if (len <= MAXIMUM_FILE_SIZE_TO_READ_FULLY) {
			ByteBuffer rawbuf = IO.readWholeStream(is, (int) len);
			rawbuf = filterClean(rawbuf.array(), rawbuf.limit());
			canonLen = rawbuf.limit();
			return new ByteArrayInputStream(rawbuf.array(), 0, (int) canonLen);
		}

		if (getCleanFilterCommand() == null && isBinary(e)) {
			canonLen = len;
			return is;
		}

		final InputStream lenIs = filterClean(e.openInputStream());
		try {
			canonLen = computeLength(lenIs);
		} finally {
			safeClose(lenIs);
		}
		return filterClean(is);
	}

	private static void safeClose(InputStream in) {
		try {
			in.close();
		} catch (IOException err2) {
			// Suppress any error related to closing an input
			// stream. We don't care, we should not have any
			// outstanding data to flush or anything like that.
		}
	}

	private static boolean isBinary(Entry entry) throws IOException {
		InputStream in = entry.openInputStream();
		try {
			return RawText.isBinary(in);
		} finally {
			safeClose(in);
		}
	}

	private ByteBuffer filterClean(byte[] src, int n)
			throws IOException {
		InputStream in = new ByteArrayInputStream(src);
		try {
			return IO.readWholeStream(filterClean(in), n);
		} finally {
			safeClose(in);
		}
	}

	private InputStream filterClean(InputStream in)
			throws IOException {
		in = EolStreamTypeUtil.wrapInputStream(in,
				getEolStreamType(OperationType.CHECKIN_OP));
		String filterCommand = getCleanFilterCommand();
		if (filterCommand != null) {
			if (FilterCommandRegistry.isRegistered(filterCommand)) {
				LocalFile buffer = new TemporaryBuffer.LocalFile(null);
				FilterCommand command = FilterCommandRegistry
						.createFilterCommand(filterCommand, repository, in,
								buffer);
				while (command.run() != -1) {
					// loop as long as command.run() tells there is work to do
				}
				return buffer.openInputStreamWithAutoDestroy();
			}
			FS fs = repository.getFS();
			ProcessBuilder filterProcessBuilder = fs.runInShell(filterCommand,
					new String[0]);
			filterProcessBuilder.directory(repository.getWorkTree());
			filterProcessBuilder.environment().put(Constants.GIT_DIR_KEY,
					repository.getDirectory().getAbsolutePath());
			ExecutionResult result;
			try {
				result = fs.execute(filterProcessBuilder, in);
			} catch (IOException | InterruptedException e) {
				throw new IOException(new FilterFailedException(e,
						filterCommand, getEntryPathString()));
			}
			int rc = result.getRc();
			if (rc != 0) {
				throw new IOException(new FilterFailedException(rc,
						filterCommand, getEntryPathString(),
						result.getStdout().toByteArray(MAX_EXCEPTION_TEXT_SIZE),
						result.getStderr().toString(MAX_EXCEPTION_TEXT_SIZE)));
			}
			return result.getStdout().openInputStreamWithAutoDestroy();
		}
		return in;
	}

	/**
	 * Returns the working tree options used by this iterator.
	 *
	 * @return working tree options
	 */
	public WorkingTreeOptions getOptions() {
		return state.options;
	}

	/**
	 * Retrieves the {@link Repository} this {@link WorkingTreeIterator}
	 * operates on.
	 *
	 * @return the {@link Repository}
	 * @since 5.9
	 */
	public Repository getRepository() {
		return repository;
	}

	@Override
	public int idOffset() {
		return contentIdOffset;
	}

	@Override
	public void reset() {
		if (!first()) {
			ptr = 0;
			if (!eof())
				parseEntry();
		}
	}

	@Override
	public boolean first() {
		return ptr == 0;
	}

	@Override
	public boolean eof() {
		return ptr == entryCnt;
	}

	@Override
	public void next(int delta) throws CorruptObjectException {
		ptr += delta;
		if (!eof()) {
			parseEntry();
		}
	}

	@Override
	public void back(int delta) throws CorruptObjectException {
		ptr -= delta;
		parseEntry();
	}

	private void parseEntry() {
		final Entry e = entries[ptr];
		mode = e.getMode().getBits();

		final int nameLen = e.encodedNameLen;
		ensurePathCapacity(pathOffset + nameLen, pathOffset);
		System.arraycopy(e.encodedName, 0, path, pathOffset, nameLen);
		pathLen = pathOffset + nameLen;
		canonLen = -1;
		cleanFilterCommandHolder = null;
		eolStreamTypeHolder = null;
	}

	/**
	 * Get the raw byte length of this entry.
	 *
	 * @return size of this file, in bytes.
	 */
	public long getEntryLength() {
		return current().getLength();
	}

	/**
	 * Get the filtered input length of this entry
	 *
	 * @return size of the content, in bytes
	 * @throws java.io.IOException
	 *             if an IO error occurred
	 */
	public long getEntryContentLength() throws IOException {
		if (canonLen == -1) {
			long rawLen = getEntryLength();
			if (rawLen == 0) {
				canonLen = 0;
			} else {
				canonLen = possiblyFilteredLength(current(),
						current().getLength());
			}
		}
		return canonLen;
	}

	/**
	 * Get the last modified time of this entry.
	 *
	 * @return last modified time of this file, in milliseconds since the epoch
	 *         (Jan 1, 1970 UTC).
	 * @deprecated use {@link #getEntryLastModifiedInstant()} instead
	 */
	@Deprecated
	public long getEntryLastModified() {
		return current().getLastModified();
	}

	/**
	 * Get the last modified time of this entry.
	 *
	 * @return last modified time of this file
	 * @since 5.1.9
	 */
	public Instant getEntryLastModifiedInstant() {
		return current().getLastModifiedInstant();
	}

	/**
	 * Obtain an input stream to read the file content.
	 * <p>
	 * Efficient implementations are not required. The caller will usually
	 * obtain the stream only once per entry, if at all.
	 * <p>
	 * The input stream should not use buffering if the implementation can avoid
	 * it. The caller will buffer as necessary to perform efficient block IO
	 * operations.
	 * <p>
	 * The caller will close the stream once complete.
	 *
	 * @return a stream to read from the file.
	 * @throws java.io.IOException
	 *             the file could not be opened for reading.
	 */
	public InputStream openEntryStream() throws IOException {
		InputStream rawis = current().openInputStream();
		if (getCleanFilterCommand() == null
				&& getEolStreamType(
						OperationType.CHECKIN_OP) == EolStreamType.DIRECT) {
			return rawis;
		}
		return filterClean(rawis);
	}

	/**
	 * Determine if the current entry path is ignored by an ignore rule.
	 *
	 * @return true if the entry was ignored by an ignore rule file.
	 * @throws java.io.IOException
	 *             a relevant ignore rule file exists but cannot be read.
	 */
	public boolean isEntryIgnored() throws IOException {
		return isEntryIgnored(pathLen);
	}

	/**
	 * Determine if the entry path is ignored by an ignore rule.
	 *
	 * @param pLen
	 *            the length of the path in the path buffer.
	 * @return true if the entry is ignored by an ignore rule.
	 * @throws java.io.IOException
	 *             a relevant ignore rule file exists but cannot be read.
	 */
	protected boolean isEntryIgnored(int pLen) throws IOException {
		return isEntryIgnored(pLen, mode);
	}

	/**
	 * Determine if the entry path is ignored by an ignore rule.
	 *
	 * @param pLen
	 *            the length of the path in the path buffer.
	 * @param fileMode
	 *            the original iterator file mode
	 * @return true if the entry is ignored by an ignore rule.
	 * @throws IOException
	 *             a relevant ignore rule file exists but cannot be read.
	 */
	private boolean isEntryIgnored(int pLen, int fileMode)
			throws IOException {
		// The ignore code wants path to start with a '/' if possible.
		// If we have the '/' in our path buffer because we are inside
		// a sub-directory include it in the range we convert to string.
		//
		final int pOff = 0 < pathOffset ? pathOffset - 1 : pathOffset;
		String pathRel = TreeWalk.pathOf(this.path, pOff, pLen);
		String parentRel = getParentPath(pathRel);

		// CGit is processing .gitignore files by starting at the root of the
		// repository and then recursing into subdirectories. With this
		// approach, top-level ignored directories will be processed first which
		// allows to skip entire subtrees and further .gitignore-file processing
		// within these subtrees.
		//
		// We will follow the same approach by marking directories as "ignored"
		// here. This allows to have a simplified FastIgnore.checkIgnore()
		// implementation (both in terms of code and computational complexity):
		//
		// Without the "ignored" flag, we would have to apply the ignore-check
		// to a path and all of its parents always(!), to determine whether a
		// path is ignored directly or by one of its parent directories; with
		// the "ignored" flag, we know at this point that the parent directory
		// is definitely not ignored, thus the path can only become ignored if
		// there is a rule matching the path itself.
		if (isDirectoryIgnored(parentRel)) {
			return true;
		}

		IgnoreNode rules = getIgnoreNode();
		final Boolean ignored = rules != null
				? rules.checkIgnored(pathRel, FileMode.TREE.equals(fileMode))
				: null;
		if (ignored != null) {
			return ignored.booleanValue();
		}
		return parent instanceof WorkingTreeIterator
				&& ((WorkingTreeIterator) parent).isEntryIgnored(pLen,
						fileMode);
	}

	private IgnoreNode getIgnoreNode() throws IOException {
		if (ignoreNode instanceof PerDirectoryIgnoreNode)
			ignoreNode = ((PerDirectoryIgnoreNode) ignoreNode).load();
		return ignoreNode;
	}

	/**
	 * Retrieves the {@link org.eclipse.jgit.attributes.AttributesNode} for the
	 * current entry.
	 *
	 * @return the {@link org.eclipse.jgit.attributes.AttributesNode} for the
	 *         current entry.
	 * @throws IOException
	 *             if an IO error occurred
	 */
	public AttributesNode getEntryAttributesNode() throws IOException {
		if (attributesNode instanceof PerDirectoryAttributesNode)
			attributesNode = ((PerDirectoryAttributesNode) attributesNode)
					.load();
		return attributesNode;
	}

	private static final Comparator<Entry> ENTRY_CMP = (Entry a,
			Entry b) -> Paths.compare(a.encodedName, 0, a.encodedNameLen,
					a.getMode().getBits(), b.encodedName, 0, b.encodedNameLen,
					b.getMode().getBits());

	/**
	 * Constructor helper.
	 *
	 * @param list
	 *            files in the subtree of the work tree this iterator operates
	 *            on
	 */
	protected void init(Entry[] list) {
		// Filter out nulls, . and .. as these are not valid tree entries,
		// also cache the encoded forms of the path names for efficient use
		// later on during sorting and iteration.
		//
		entries = list;
		int i, o;

		final CharsetEncoder nameEncoder = state.nameEncoder;
		for (i = 0, o = 0; i < entries.length; i++) {
			final Entry e = entries[i];
			if (e == null)
				continue;
			final String name = e.getName();
			if (".".equals(name) || "..".equals(name)) //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			if (Constants.DOT_GIT.equals(name))
				continue;
			if (Constants.DOT_GIT_IGNORE.equals(name))
				ignoreNode = new PerDirectoryIgnoreNode(
						TreeWalk.pathOf(path, 0, pathOffset)
								+ Constants.DOT_GIT_IGNORE,
						e);
			if (Constants.DOT_GIT_ATTRIBUTES.equals(name))
				attributesNode = new PerDirectoryAttributesNode(e);
			if (i != o)
				entries[o] = e;
			e.encodeName(nameEncoder);
			o++;
		}
		entryCnt = o;
		Arrays.sort(entries, 0, entryCnt, ENTRY_CMP);

		contentIdFromPtr = -1;
		ptr = 0;
		if (!eof())
			parseEntry();
		else if (pathLen == 0) // see bug 445363
			pathLen = pathOffset;
	}

	/**
	 * Obtain the current entry from this iterator.
	 *
	 * @return the currently selected entry.
	 */
	protected Entry current() {
		return entries[ptr];
	}

	/**
	 * The result of a metadata-comparison between the current entry and a
	 * {@link DirCacheEntry}
	 */
	public enum MetadataDiff {
		/**
		 * The entries are equal by metaData (mode, length,
		 * modification-timestamp) or the <code>assumeValid</code> attribute of
		 * the index entry is set
		 */
		EQUAL,

		/**
		 * The entries are not equal by metaData (mode, length) or the
		 * <code>isUpdateNeeded</code> attribute of the index entry is set
		 */
		DIFFER_BY_METADATA,

		/** index entry is smudged - can't use that entry for comparison */
		SMUDGED,

		/**
		 * The entries are equal by metaData (mode, length) but differ by
		 * modification-timestamp.
		 */
		DIFFER_BY_TIMESTAMP
	}

	/**
	 * Is the file mode of the current entry different than the given raw mode?
	 *
	 * @param rawMode
	 *            an int.
	 * @return true if different, false otherwise
	 */
	public boolean isModeDifferent(int rawMode) {
		// Determine difference in mode-bits of file and index-entry. In the
		// bitwise presentation of modeDiff we'll have a '1' when the two modes
		// differ at this position.
		int modeDiff = getEntryRawMode() ^ rawMode;

		if (modeDiff == 0)
			return false;

		// Do not rely on filemode differences in case of symbolic links
		if (getOptions().getSymLinks() == SymLinks.FALSE)
			if (FileMode.SYMLINK.equals(rawMode))
				return false;

		// Ignore the executable file bits if WorkingTreeOptions tell me to
		// do so. Ignoring is done by setting the bits representing a
		// EXECUTABLE_FILE to '0' in modeDiff
		if (!state.options.isFileMode())
			modeDiff &= ~FileMode.EXECUTABLE_FILE.getBits();
		return modeDiff != 0;
	}

	/**
	 * Compare the metadata (mode, length, modification-timestamp) of the
	 * current entry and a {@link org.eclipse.jgit.dircache.DirCacheEntry}
	 *
	 * @param entry
	 *            the {@link org.eclipse.jgit.dircache.DirCacheEntry} to compare
	 *            with
	 * @return a
	 *         {@link org.eclipse.jgit.treewalk.WorkingTreeIterator.MetadataDiff}
	 *         which tells whether and how the entries metadata differ
	 */
	public MetadataDiff compareMetadata(DirCacheEntry entry) {
		if (entry.isAssumeValid())
			return MetadataDiff.EQUAL;

		if (entry.isUpdateNeeded())
			return MetadataDiff.DIFFER_BY_METADATA;

		if (isModeDifferent(entry.getRawMode()))
			return MetadataDiff.DIFFER_BY_METADATA;

		// Don't check for length or lastmodified on folders
		int type = mode & FileMode.TYPE_MASK;
		if (type == FileMode.TYPE_TREE || type == FileMode.TYPE_GITLINK)
			return MetadataDiff.EQUAL;

		if (!entry.isSmudged() && entry.getLength() != (int) getEntryLength())
			return MetadataDiff.DIFFER_BY_METADATA;

		// Cache and file timestamps may differ in resolution. Therefore don't
		// compare instants directly but use a comparator that compares only
		// up to the lower apparent resolution of either timestamp.
		//
		// If core.checkstat is set to "minimal", compare only the seconds part.
		Instant cacheLastModified = entry.getLastModifiedInstant();
		Instant fileLastModified = getEntryLastModifiedInstant();
		if (timestampComparator.compare(cacheLastModified, fileLastModified,
				getOptions().getCheckStat() == CheckStat.MINIMAL) != 0) {
			return MetadataDiff.DIFFER_BY_TIMESTAMP;
		}

		if (entry.isSmudged()) {
			return MetadataDiff.SMUDGED;
		}
		// The file is clean when when comparing timestamps
		return MetadataDiff.EQUAL;
	}

	/**
	 * Checks whether this entry differs from a given entry from the
	 * {@link org.eclipse.jgit.dircache.DirCache}.
	 *
	 * File status information is used and if status is same we consider the
	 * file identical to the state in the working directory. Native git uses
	 * more stat fields than we have accessible in Java.
	 *
	 * @param entry
	 *            the entry from the dircache we want to compare against
	 * @param forceContentCheck
	 *            True if the actual file content should be checked if
	 *            modification time differs.
	 * @param reader
	 *            access to repository objects if necessary. Should not be null.
	 * @return true if content is most likely different.
	 * @throws java.io.IOException
	 *             if an IO error occurred
	 * @since 3.3
	 */
	public boolean isModified(DirCacheEntry entry, boolean forceContentCheck,
			ObjectReader reader) throws IOException {
		if (entry == null)
			return !FileMode.MISSING.equals(getEntryFileMode());
		MetadataDiff diff = compareMetadata(entry);
		switch (diff) {
		case DIFFER_BY_TIMESTAMP:
			if (forceContentCheck) {
				// But we are told to look at content even though timestamps
				// tell us about modification
				return contentCheck(entry, reader);
			}
			// We are told to assume a modification if timestamps differs
			return true;
		case SMUDGED:
			// The file is clean by timestamps but the entry was smudged.
			// Lets do a content check
			return contentCheck(entry, reader);
		case EQUAL:
			if (mode == FileMode.SYMLINK.getBits()) {
				return contentCheck(entry, reader);
			}
			return false;
		case DIFFER_BY_METADATA:
			if (mode == FileMode.TREE.getBits()
					&& entry.getFileMode().equals(FileMode.GITLINK)) {
				byte[] idBuffer = idBuffer();
				int idOffset = idOffset();
				if (entry.getObjectId().compareTo(idBuffer, idOffset) == 0) {
					return true;
				} else if (ObjectId.zeroId().compareTo(idBuffer,
						idOffset) == 0) {
					Path p = repository.getWorkTree().toPath()
							.resolve(entry.getPathString());
					return FileUtils.hasFiles(p);
				}
				return false;
			} else if (mode == FileMode.SYMLINK.getBits())
				return contentCheck(entry, reader);
			return true;
		default:
			throw new IllegalStateException(MessageFormat.format(
					JGitText.get().unexpectedCompareResult, diff.name()));
		}
	}

	/**
	 * Get the file mode to use for the current entry when it is to be updated
	 * in the index.
	 *
	 * @param indexIter
	 *            {@link org.eclipse.jgit.dircache.DirCacheIterator} positioned
	 *            at the same entry as this iterator or null if no
	 *            {@link org.eclipse.jgit.dircache.DirCacheIterator} is
	 *            available at this iterator's current entry
	 * @return index file mode
	 */
	public FileMode getIndexFileMode(DirCacheIterator indexIter) {
		final FileMode wtMode = getEntryFileMode();
		if (indexIter == null) {
			return wtMode;
		}
		final FileMode iMode = indexIter.getEntryFileMode();
		if (iMode == FileMode.SYMLINK
				&& getOptions().getSymLinks() == SymLinks.FALSE
				&& (wtMode == FileMode.REGULAR_FILE
						|| wtMode == FileMode.EXECUTABLE_FILE)) {
			return iMode;
		}
		if (getOptions().isFileMode() && iMode != FileMode.GITLINK && iMode != FileMode.TREE) {
			return wtMode;
		}
		if (!getOptions().isFileMode()) {
			if (FileMode.REGULAR_FILE == wtMode
					&& FileMode.EXECUTABLE_FILE == iMode) {
				return iMode;
			}
			if (FileMode.EXECUTABLE_FILE == wtMode
					&& FileMode.REGULAR_FILE == iMode) {
				return iMode;
			}
		}
		if (FileMode.GITLINK == iMode
				&& FileMode.TREE == wtMode && !getOptions().isDirNoGitLinks()) {
			return iMode;
		}
		if (FileMode.TREE == iMode
				&& FileMode.GITLINK == wtMode) {
			return iMode;
		}
		return wtMode;
	}

	/**
	 * Compares the entries content with the content in the filesystem.
	 * Unsmudges the entry when it is detected that it is clean.
	 *
	 * @param entry
	 *            the entry to be checked
	 * @param reader
	 *            acccess to repository data if necessary
	 * @return <code>true</code> if the content doesn't match,
	 *         <code>false</code> if it matches
	 * @throws IOException
	 *             if an IO error occurred
	 */
	private boolean contentCheck(DirCacheEntry entry, ObjectReader reader)
			throws IOException {
		if (getEntryObjectId().equals(entry.getObjectId())) {
			// Content has not changed

			// We know the entry can't be racily clean because it's still clean.
			// Therefore we unsmudge the entry!
			// If by any chance we now unsmudge although we are still in the
			// same time-slot as the last modification to the index file the
			// next index write operation will smudge again.
			// Caution: we are unsmudging just by setting the length of the
			// in-memory entry object. It's the callers task to detect that we
			// have modified the entry and to persist the modified index.
			entry.setLength((int) getEntryLength());

			return false;
		}
		if (mode == FileMode.SYMLINK.getBits()) {
			return !new File(readSymlinkTarget(current())).equals(
					new File(readContentAsNormalizedString(entry, reader)));
		}
		// Content differs: that's a real change
		return true;
	}

	private static String readContentAsNormalizedString(DirCacheEntry entry,
			ObjectReader reader) throws MissingObjectException, IOException {
		ObjectLoader open = reader.open(entry.getObjectId());
		byte[] cachedBytes = open.getCachedBytes();
		return FS.detect().normalize(RawParseUtils.decode(cachedBytes));
	}

	/**
	 * Reads the target of a symlink as a string. This default implementation
	 * fully reads the entry's input stream and converts it to a normalized
	 * string. Subclasses may override to provide more specialized
	 * implementations.
	 *
	 * @param entry
	 *            to read
	 * @return the entry's content as a normalized string
	 * @throws java.io.IOException
	 *             if the entry cannot be read or does not denote a symlink
	 * @since 4.6
	 */
	protected String readSymlinkTarget(Entry entry) throws IOException {
		if (!entry.getMode().equals(FileMode.SYMLINK)) {
			throw new java.nio.file.NotLinkException(entry.getName());
		}
		long length = entry.getLength();
		byte[] content = new byte[(int) length];
		try (InputStream is = entry.openInputStream()) {
			int bytesRead = IO.readFully(is, content, 0);
			return FS.detect()
					.normalize(RawParseUtils.decode(content, 0, bytesRead));
		}
	}

	private static long computeLength(InputStream in) throws IOException {
		// Since we only care about the length, use skip. The stream
		// may be able to more efficiently wade through its data.
		//
		long length = 0;
		for (;;) {
			long n = in.skip(1 << 20);
			if (n <= 0)
				break;
			length += n;
		}
		return length;
	}

	private byte[] computeHash(InputStream in, long length) throws IOException {
		SHA1 contentDigest = SHA1.newInstance();
		final byte[] contentReadBuffer = state.contentReadBuffer;

		contentDigest.update(hblob);
		contentDigest.update((byte) ' ');

		long sz = length;
		if (sz == 0) {
			contentDigest.update((byte) '0');
		} else {
			final int bufn = contentReadBuffer.length;
			int p = bufn;
			do {
				contentReadBuffer[--p] = digits[(int) (sz % 10)];
				sz /= 10;
			} while (sz > 0);
			contentDigest.update(contentReadBuffer, p, bufn - p);
		}
		contentDigest.update((byte) 0);

		for (;;) {
			final int r = in.read(contentReadBuffer);
			if (r <= 0)
				break;
			contentDigest.update(contentReadBuffer, 0, r);
			sz += r;
		}
		if (sz != length)
			return zeroid;
		return contentDigest.digest();
	}

	/**
	 * A single entry within a working directory tree.
	 *
	 * @since 5.0
	 */
	public abstract static class Entry {
		byte[] encodedName;

		int encodedNameLen;

		void encodeName(CharsetEncoder enc) {
			final ByteBuffer b;
			try {
				b = enc.encode(CharBuffer.wrap(getName()));
			} catch (CharacterCodingException e) {
				// This should so never happen.
				throw new RuntimeException(MessageFormat.format(
						JGitText.get().unencodeableFile, getName()), e);
			}

			encodedNameLen = b.limit();
			if (b.hasArray() && b.arrayOffset() == 0)
				encodedName = b.array();
			else
				b.get(encodedName = new byte[encodedNameLen]);
		}

		@Override
		public String toString() {
			return getMode().toString() + " " + getName(); //$NON-NLS-1$
		}

		/**
		 * Get the type of this entry.
		 * <p>
		 * <b>Note: Efficient implementation required.</b>
		 * <p>
		 * The implementation of this method must be efficient. If a subclass
		 * needs to compute the value they should cache the reference within an
		 * instance member instead.
		 *
		 * @return a file mode constant from {@link FileMode}.
		 */
		public abstract FileMode getMode();

		/**
		 * Get the byte length of this entry.
		 * <p>
		 * <b>Note: Efficient implementation required.</b>
		 * <p>
		 * The implementation of this method must be efficient. If a subclass
		 * needs to compute the value they should cache the reference within an
		 * instance member instead.
		 *
		 * @return size of this file, in bytes.
		 */
		public abstract long getLength();

		/**
		 * Get the last modified time of this entry.
		 * <p>
		 * <b>Note: Efficient implementation required.</b>
		 * <p>
		 * The implementation of this method must be efficient. If a subclass
		 * needs to compute the value they should cache the reference within an
		 * instance member instead.
		 *
		 * @return time since the epoch (in ms) of the last change.
		 * @deprecated use {@link #getLastModifiedInstant()} instead
		 */
		@Deprecated
		public abstract long getLastModified();

		/**
		 * Get the last modified time of this entry.
		 * <p>
		 * <b>Note: Efficient implementation required.</b>
		 * <p>
		 * The implementation of this method must be efficient. If a subclass
		 * needs to compute the value they should cache the reference within an
		 * instance member instead.
		 *
		 * @return time of the last change.
		 * @since 5.1.9
		 */
		public abstract Instant getLastModifiedInstant();

		/**
		 * Get the name of this entry within its directory.
		 * <p>
		 * Efficient implementations are not required. The caller will obtain
		 * the name only once and cache it once obtained.
		 *
		 * @return name of the entry.
		 */
		public abstract String getName();

		/**
		 * Obtain an input stream to read the file content.
		 * <p>
		 * Efficient implementations are not required. The caller will usually
		 * obtain the stream only once per entry, if at all.
		 * <p>
		 * The input stream should not use buffering if the implementation can
		 * avoid it. The caller will buffer as necessary to perform efficient
		 * block IO operations.
		 * <p>
		 * The caller will close the stream once complete.
		 *
		 * @return a stream to read from the file.
		 * @throws IOException
		 *             the file could not be opened for reading.
		 */
		public abstract InputStream openInputStream() throws IOException;
	}

	/** Magic type indicating we know rules exist, but they aren't loaded. */
	private static class PerDirectoryIgnoreNode extends IgnoreNode {
		protected final Entry entry;

		private final String name;

		PerDirectoryIgnoreNode(String name, Entry entry) {
			super(Collections.<FastIgnoreRule> emptyList());
			this.name = name;
			this.entry = entry;
		}

		IgnoreNode load() throws IOException {
			return load(null);
		}

		IgnoreNode load(IgnoreNode parent) throws IOException {
			IgnoreNodeWithParent r = new IgnoreNodeWithParent(parent);
			try (InputStream in = entry.openInputStream()) {
				r.parse(name, in);
			}
			return r.getRules().isEmpty() && parent == null ? null : r;
		}
	}

	/** Magic type indicating there may be rules for the top level. */
	private static class RootIgnoreNode extends PerDirectoryIgnoreNode {
		final Repository repository;

		RootIgnoreNode(Entry entry, Repository repository) {
			super(entry != null ? entry.getName() : null, entry);
			this.repository = repository;
		}

		@Override
		IgnoreNode load(IgnoreNode parent) throws IOException {
			IgnoreNode coreExclude = new IgnoreNodeWithParent(parent);
			FS fs = repository.getFS();
			Path path = repository.getConfig().getPath(
					ConfigConstants.CONFIG_CORE_SECTION, null,
					ConfigConstants.CONFIG_KEY_EXCLUDESFILE, fs, null, null);
			if (path != null) {
				if (Files.exists(path)) {
					loadRulesFromFile(coreExclude, path.toFile());
				}
			} else {
				loadRulesFromDefaultFile(coreExclude, fs);
			}
			if (coreExclude.getRules().isEmpty()) {
				coreExclude = parent;
			}

			IgnoreNode infoExclude = new IgnoreNodeWithParent(
					coreExclude);
			File exclude = fs.resolve(repository.getDirectory(),
					Constants.INFO_EXCLUDE);
			if (fs.exists(exclude)) {
				loadRulesFromFile(infoExclude, exclude);
			}
			if (infoExclude.getRules().isEmpty()) {
				infoExclude = null;
			}

			IgnoreNode parentNode = infoExclude != null ? infoExclude
					: coreExclude;

			IgnoreNode r;
			if (entry != null) {
				r = super.load(parentNode);
				if (r == null) {
					return null;
				}
			} else {
				return parentNode;
			}
			return r.getRules().isEmpty() ? parentNode : r;
		}

		private static void loadRulesFromFile(IgnoreNode r, File exclude)
				throws FileNotFoundException, IOException {
			try (FileInputStream in = new FileInputStream(exclude)) {
				r.parse(exclude.getAbsolutePath(), in);
			}
		}

		private static void loadRulesFromDefaultFile(IgnoreNode r,
				FS fileSystem) throws FileNotFoundException, IOException {
			Path cfg = SystemReader.getInstance()
					.getXdgConfigDirectory(fileSystem);
			if (cfg != null) {
				Path cfgPath = cfg.resolve("git").resolve("ignore"); //$NON-NLS-1$ //$NON-NLS-2$
				if (Files.exists(cfgPath)) {
					loadRulesFromFile(r, cfgPath.toFile());
				}
			}
		}
	}

	private static class IgnoreNodeWithParent extends IgnoreNode {

		private final IgnoreNode parent;

		IgnoreNodeWithParent(IgnoreNode parent) {
			this.parent = parent;
		}

		@Override
		public Boolean checkIgnored(String path, boolean isDirectory) {
			Boolean result = super.checkIgnored(path, isDirectory);
			if (result == null && parent != null) {
				return parent.checkIgnored(path, isDirectory);
			}
			return result;
		}
	}

	/** Magic type indicating we know rules exist, but they aren't loaded. */
	private static class PerDirectoryAttributesNode extends AttributesNode {
		final Entry entry;

		PerDirectoryAttributesNode(Entry entry) {
			super(Collections.<AttributesRule> emptyList());
			this.entry = entry;
		}

		AttributesNode load() throws IOException {
			AttributesNode r = new AttributesNode();
			try (InputStream in = entry.openInputStream()) {
				r.parse(in);
			}
			return r.getRules().isEmpty() ? null : r;
		}
	}


	private static final class IteratorState {
		/** Options used to process the working tree. */
		final WorkingTreeOptions options;

		/** File name character encoder. */
		final CharsetEncoder nameEncoder;

		/** Buffer used to perform {@link #contentId} computations. */
		byte[] contentReadBuffer;

		/** TreeWalk with a (supposedly) matching DirCacheIterator. */
		TreeWalk walk;

		/** Position of the matching {@link DirCacheIterator}. */
		int dirCacheTree = -1;

		/** Whether the iterator shall walk ignored directories. */
		boolean walkIgnored = false;

		final Map<String, Boolean> directoryToIgnored = new HashMap<>();

		IteratorState(WorkingTreeOptions options) {
			this.options = options;
			this.nameEncoder = UTF_8.newEncoder();
		}

		void initializeReadBuffer() {
			if (contentReadBuffer == null) {
				contentReadBuffer = new byte[BUFFER_SIZE];
			}
		}
	}

	/**
	 * Get the clean filter command for the current entry.
	 *
	 * @return the clean filter command for the current entry or
	 *         <code>null</code> if no such command is defined
	 * @throws java.io.IOException
	 *             if an IO error occurred
	 * @since 4.2
	 */
	public String getCleanFilterCommand() throws IOException {
		if (cleanFilterCommandHolder == null) {
			String cmd = null;
			if (state.walk != null) {
				cmd = state.walk
						.getFilterCommand(Constants.ATTR_FILTER_TYPE_CLEAN);
			}
			cleanFilterCommandHolder = new Holder<>(cmd);
		}
		return cleanFilterCommandHolder.get();
	}

	/**
	 * Get the eol stream type for the current entry.
	 *
	 * @return the eol stream type for the current entry or <code>null</code> if
	 *         it cannot be determined. When state or state.walk is null or the
	 *         {@link org.eclipse.jgit.treewalk.TreeWalk} is not based on a
	 *         {@link org.eclipse.jgit.lib.Repository} then null is returned.
	 * @throws java.io.IOException
	 *             if an IO error occurred
	 * @since 4.3
	 */
	public EolStreamType getEolStreamType() throws IOException {
		return getEolStreamType(null);
	}

	/**
	 * @param opType
	 *            The operationtype (checkin/checkout) which should be used
	 * @return the eol stream type for the current entry or <code>null</code> if
	 *         it cannot be determined. When state or state.walk is null or the
	 *         {@link TreeWalk} is not based on a {@link Repository} then null
	 *         is returned.
	 * @throws IOException
	 *             if an IO error occurred
	 */
	private EolStreamType getEolStreamType(OperationType opType)
			throws IOException {
		if (eolStreamTypeHolder == null) {
			EolStreamType type = null;
			if (state.walk != null) {
				type = state.walk.getEolStreamType(opType);
				OperationType operationType = opType != null ? opType
						: state.walk.getOperationType();
				if (OperationType.CHECKIN_OP.equals(operationType)
						&& EolStreamType.AUTO_LF.equals(type)
						&& hasCrLfInIndex(getDirCacheIterator())) {
					// If text=auto (or core.autocrlf=true) and the file has
					// already been committed with CR/LF, then don't convert.
					type = EolStreamType.DIRECT;
				}
			} else {
				switch (getOptions().getAutoCRLF()) {
				case FALSE:
					type = EolStreamType.DIRECT;
					break;
				case TRUE:
				case INPUT:
					type = EolStreamType.AUTO_LF;
					break;
				}
			}
			eolStreamTypeHolder = new Holder<>(type);
		}
		return eolStreamTypeHolder.get();
	}

	/**
	 * Determines whether the file was committed un-normalized. If the iterator
	 * points to a conflict entry, checks the "ours" version.
	 *
	 * @param dirCache
	 *            iterator pointing to the current entry for the file in the
	 *            index
	 * @return {@code true} if the file in the index is not binary and has CR/LF
	 *         line endings, {@code false} otherwise
	 */
	private boolean hasCrLfInIndex(DirCacheIterator dirCache) {
		if (dirCache == null) {
			return false;
		}
		// Read blob from index and check for CR/LF-delimited text.
		DirCacheEntry entry = dirCache.getDirCacheEntry();
		if ((entry.getRawMode() & FileMode.TYPE_MASK) == FileMode.TYPE_FILE) {
			ObjectId blobId = entry.getObjectId();
			if (entry.getStage() > 0
					&& entry.getStage() != DirCacheEntry.STAGE_2) {
				blobId = null;
				// Merge conflict: check ours (stage 2)
				byte[] name = entry.getRawPath();
				int i = 0;
				while (!dirCache.eof()) {
					dirCache.next(1);
					i++;
					entry = dirCache.getDirCacheEntry();
					if (entry == null
							|| !Arrays.equals(name, entry.getRawPath())) {
						break;
					}
					if (entry.getStage() == DirCacheEntry.STAGE_2) {
						if ((entry.getRawMode()
								& FileMode.TYPE_MASK) == FileMode.TYPE_FILE) {
							blobId = entry.getObjectId();
						}
						break;
					}
				}
				dirCache.back(i);
			}
			if (blobId != null) {
				try (ObjectReader reader = repository.newObjectReader()) {
					ObjectLoader loader = reader.open(blobId,
							Constants.OBJ_BLOB);
					try {
						byte[] raw = loader.getCachedBytes();
						return RawText.isCrLfText(raw, raw.length, true);
					} catch (LargeObjectException e) {
						try (InputStream in = loader.openStream()) {
							return RawText.isCrLfText(in);
						}
					}
				} catch (IOException e) {
					// Ignore and return false below
				}
			}
		}
		return false;
	}

	private boolean isDirectoryIgnored(String pathRel) throws IOException {
		final int pOff = 0 < pathOffset ? pathOffset - 1 : pathOffset;
		final String base = TreeWalk.pathOf(this.path, 0, pOff);
		final String pathAbs = concatPath(base, pathRel);
		return isDirectoryIgnored(pathRel, pathAbs);
	}

	private boolean isDirectoryIgnored(String pathRel, String pathAbs)
			throws IOException {
		assert pathRel.length() == 0 || (pathRel.charAt(0) != '/'
				&& pathRel.charAt(pathRel.length() - 1) != '/');
		assert pathAbs.length() == 0 || (pathAbs.charAt(0) != '/'
				&& pathAbs.charAt(pathAbs.length() - 1) != '/');
		assert pathAbs.endsWith(pathRel);

		Boolean ignored = state.directoryToIgnored.get(pathAbs);
		if (ignored != null) {
			return ignored.booleanValue();
		}

		final String parentRel = getParentPath(pathRel);
		if (parentRel != null && isDirectoryIgnored(parentRel)) {
			state.directoryToIgnored.put(pathAbs, Boolean.TRUE);
			return true;
		}

		final IgnoreNode node = getIgnoreNode();
		for (String p = pathRel; node != null
				&& !"".equals(p); p = getParentPath(p)) { //$NON-NLS-1$
			ignored = node.checkIgnored(p, true);
			if (ignored != null) {
				state.directoryToIgnored.put(pathAbs, ignored);
				return ignored.booleanValue();
			}
		}

		if (!(this.parent instanceof WorkingTreeIterator)) {
			state.directoryToIgnored.put(pathAbs, Boolean.FALSE);
			return false;
		}

		final WorkingTreeIterator wtParent = (WorkingTreeIterator) this.parent;
		final String parentRelPath = concatPath(
				TreeWalk.pathOf(this.path, wtParent.pathOffset, pathOffset - 1),
				pathRel);
		assert concatPath(TreeWalk.pathOf(wtParent.path, 0,
				Math.max(0, wtParent.pathOffset - 1)), parentRelPath)
						.equals(pathAbs);
		return wtParent.isDirectoryIgnored(parentRelPath, pathAbs);
	}

	private static String getParentPath(String path) {
		final int slashIndex = path.lastIndexOf('/', path.length() - 2);
		if (slashIndex > 0) {
			return path.substring(path.charAt(0) == '/' ? 1 : 0, slashIndex);
		}
		return path.length() > 0 ? "" : null; //$NON-NLS-1$
	}

	private static String concatPath(String p1, String p2) {
		return p1 + (p1.length() > 0 && p2.length() > 0 ? "/" : "") + p2; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
