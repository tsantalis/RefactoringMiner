/*
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 * Copyright (C) 2012-2013, Robin Rosenberg
 * Copyright (C) 2018-2022, Andre Bossert <andre.bossert@siemens.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.lib;

/**
 * Constants for use with the Configuration classes: section names,
 * configuration keys
 */
@SuppressWarnings("nls")
public final class ConfigConstants {
	/** The "core" section */
	public static final String CONFIG_CORE_SECTION = "core";

	/** The "branch" section */
	public static final String CONFIG_BRANCH_SECTION = "branch";

	/** The "remote" section */
	public static final String CONFIG_REMOTE_SECTION = "remote";

	/** The "diff" section */
	public static final String CONFIG_DIFF_SECTION = "diff";

	/**
	 * The "tool" key within "diff" or "merge" section
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_KEY_TOOL = "tool";

	/**
	 * The "guitool" key within "diff" or "merge" section
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_KEY_GUITOOL = "guitool";

	/**
	 * The "difftool" section
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_DIFFTOOL_SECTION = "difftool";

	/**
	 * The "prompt" key within "difftool" or "mergetool" section
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_KEY_PROMPT = "prompt";

	/**
	 * The "trustExitCode" key within "difftool" or "mergetool.&lt;name&gt;."
	 * section
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_KEY_TRUST_EXIT_CODE = "trustExitCode";

	/**
	 * The "cmd" key within "difftool.*." or "mergetool.*." section
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_KEY_CMD = "cmd";

	/** The "dfs" section */
	public static final String CONFIG_DFS_SECTION = "dfs";

	/**
	 * The "receive" section
	 * @since 4.6
	 */
	public static final String CONFIG_RECEIVE_SECTION = "receive";

	/** The "user" section */
	public static final String CONFIG_USER_SECTION = "user";

	/** The "gerrit" section */
	public static final String CONFIG_GERRIT_SECTION = "gerrit";

	/** The "workflow" section */
	public static final String CONFIG_WORKFLOW_SECTION = "workflow";

	/** The "submodule" section */
	public static final String CONFIG_SUBMODULE_SECTION = "submodule";

	/**
	 * The "rebase" section
	 * @since 3.2
	 */
	public static final String CONFIG_REBASE_SECTION = "rebase";

	/** The "gc" section */
	public static final String CONFIG_GC_SECTION = "gc";

	/** The "pack" section */
	public static final String CONFIG_PACK_SECTION = "pack";

	/**
	 * The "fetch" section
	 *
	 * @since 3.3
	 */
	public static final String CONFIG_FETCH_SECTION = "fetch";

	/**
	 * The "pull" section
	 * @since 3.5
	 */
	public static final String CONFIG_PULL_SECTION = "pull";

	/**
	 * The "merge" section
	 * @since 4.9
	 */
	public static final String CONFIG_MERGE_SECTION = "merge";

	/**
	 * The "mergetool" section
	 *
	 * @since 6.2
	 */
	public static final String CONFIG_MERGETOOL_SECTION = "mergetool";

	/**
	 * The "keepBackup" key within "mergetool" section
	 *
	 * @since 6.2
	 */
	public static final String CONFIG_KEY_KEEP_BACKUP = "keepBackup";

	/**
	 * The "keepTemporaries" key within "mergetool" section
	 *
	 * @since 6.2
	 */
	public static final String CONFIG_KEY_KEEP_TEMPORARIES = "keepTemporaries";

	/**
	 * The "writeToTemp" key within "mergetool" section
	 *
	 * @since 6.2
	 */
	public static final String CONFIG_KEY_WRITE_TO_TEMP = "writeToTemp";

	/**
	 * The "filter" section
	 * @since 4.6
	 */
	public static final String CONFIG_FILTER_SECTION = "filter";

	/**
	 * The "gpg" section
	 * @since 5.2
	 */
	public static final String CONFIG_GPG_SECTION = "gpg";

	/**
	 * The "protocol" section
	 * @since 5.9
	 */
	public static final String CONFIG_PROTOCOL_SECTION = "protocol";

	/**
	 * The "format" key
	 * @since 5.2
	 */
	public static final String CONFIG_KEY_FORMAT = "format";

	/**
	 * The "program" key
	 *
	 * @since 5.11
	 */
	public static final String CONFIG_KEY_PROGRAM = "program";

	/**
	 * The "signingKey" key
	 *
	 * @since 5.2
	 */
	public static final String CONFIG_KEY_SIGNINGKEY = "signingKey";

	/**
	 * The "commit" section
	 * @since 5.2
	 */
	public static final String CONFIG_COMMIT_SECTION = "commit";

	/**
	 * The "template" key
	 *
	 * @since 5.13
	 */
	public static final String CONFIG_KEY_COMMIT_TEMPLATE = "template";

	/**
	 * The "tag" section
	 *
	 * @since 5.11
	 */
	public static final String CONFIG_TAG_SECTION = "tag";

	/**
	 * The "cleanup" key
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_KEY_CLEANUP = "cleanup";

	/**
	 * The "gpgSign" key
	 *
	 * @since 5.2
	 */
	public static final String CONFIG_KEY_GPGSIGN = "gpgSign";

	/**
	 * The "forceSignAnnotated" key
	 *
	 * @since 5.11
	 */
	public static final String CONFIG_KEY_FORCE_SIGN_ANNOTATED = "forceSignAnnotated";

	/**
	 * The "commentChar" key.
	 *
	 * @since 6.2
	 */
	public static final String CONFIG_KEY_COMMENT_CHAR = "commentChar";

	/**
	 * The "hooksPath" key.
	 *
	 * @since 5.6
	 */
	public static final String CONFIG_KEY_HOOKS_PATH = "hooksPath";

	/**
	 * The "quotePath" key.
	 * @since 5.6
	 */
	public static final String CONFIG_KEY_QUOTE_PATH = "quotePath";

	/** The "algorithm" key */
	public static final String CONFIG_KEY_ALGORITHM = "algorithm";

	/** The "autocrlf" key */
	public static final String CONFIG_KEY_AUTOCRLF = "autocrlf";

	/**
	 * The "auto" key
	 * @since 4.6
	 */
	public static final String CONFIG_KEY_AUTO = "auto";

	/**
	 * The "autogc" key
	 * @since 4.6
	 */
	public static final String CONFIG_KEY_AUTOGC = "autogc";

	/**
	 * The "autopacklimit" key
	 * @since 4.6
	 */
	public static final String CONFIG_KEY_AUTOPACKLIMIT = "autopacklimit";

	/**
	 * The "eol" key
	 *
	 * @since 4.3
	 */
	public static final String CONFIG_KEY_EOL = "eol";

	/** The "bare" key */
	public static final String CONFIG_KEY_BARE = "bare";

	/** The "excludesfile" key */
	public static final String CONFIG_KEY_EXCLUDESFILE = "excludesfile";

	/**
	 * The "attributesfile" key
	 *
	 * @since 3.7
	 */
	public static final String CONFIG_KEY_ATTRIBUTESFILE = "attributesfile";

	/** The "filemode" key */
	public static final String CONFIG_KEY_FILEMODE = "filemode";

	/** The "logallrefupdates" key */
	public static final String CONFIG_KEY_LOGALLREFUPDATES = "logallrefupdates";

	/** The "repositoryformatversion" key */
	public static final String CONFIG_KEY_REPO_FORMAT_VERSION = "repositoryformatversion";

	/** The "worktree" key */
	public static final String CONFIG_KEY_WORKTREE = "worktree";

	/** The "blockLimit" key */
	public static final String CONFIG_KEY_BLOCK_LIMIT = "blockLimit";

	/** The "blockSize" key */
	public static final String CONFIG_KEY_BLOCK_SIZE = "blockSize";

	/**
	 * The "concurrencyLevel" key
	 *
	 * @since 4.6
	 */
	public static final String CONFIG_KEY_CONCURRENCY_LEVEL = "concurrencyLevel";

	/** The "deltaBaseCacheLimit" key */
	public static final String CONFIG_KEY_DELTA_BASE_CACHE_LIMIT = "deltaBaseCacheLimit";

	/**
	 * The "symlinks" key
	 * @since 3.3
	 */
	public static final String CONFIG_KEY_SYMLINKS = "symlinks";

	/** The "streamFileThreshold" key */
	public static final String CONFIG_KEY_STREAM_FILE_TRESHOLD = "streamFileThreshold";

	/**
	 * The "packedGitMmap" key
	 * @since 5.1.13
	 */
	public static final String CONFIG_KEY_PACKED_GIT_MMAP = "packedgitmmap";

	/**
	 * The "packedGitWindowSize" key
	 * @since 5.1.13
	 */
	public static final String CONFIG_KEY_PACKED_GIT_WINDOWSIZE = "packedgitwindowsize";

	/**
	 * The "packedGitLimit" key
	 * @since 5.1.13
	 */
	public static final String CONFIG_KEY_PACKED_GIT_LIMIT = "packedgitlimit";

	/**
	 * The "packedGitOpenFiles" key
	 * @since 5.1.13
	 */
	public static final String CONFIG_KEY_PACKED_GIT_OPENFILES = "packedgitopenfiles";

	/**
	 * The "packedGitUseStrongRefs" key
	 * @since 5.1.13
	 */
	public static final String CONFIG_KEY_PACKED_GIT_USE_STRONGREFS = "packedgitusestrongrefs";

	/** The "remote" key */
	public static final String CONFIG_KEY_REMOTE = "remote";

	/**
	 * The "pushRemote" key.
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_KEY_PUSH_REMOTE = "pushRemote";

	/**
	 * The "pushDefault" key.
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_KEY_PUSH_DEFAULT = "pushDefault";

	/** The "merge" key */
	public static final String CONFIG_KEY_MERGE = "merge";

	/** The "rebase" key */
	public static final String CONFIG_KEY_REBASE = "rebase";

	/** The "url" key */
	public static final String CONFIG_KEY_URL = "url";

	/** The "autosetupmerge" key */
	public static final String CONFIG_KEY_AUTOSETUPMERGE = "autosetupmerge";

	/** The "autosetuprebase" key */
	public static final String CONFIG_KEY_AUTOSETUPREBASE = "autosetuprebase";

	/**
	 * The "autostash" key
	 * @since 3.2
	 */
	public static final String CONFIG_KEY_AUTOSTASH = "autostash";

	/** The "name" key */
	public static final String CONFIG_KEY_NAME = "name";

	/** The "email" key */
	public static final String CONFIG_KEY_EMAIL = "email";

	/** The "false" key (used to configure {@link #CONFIG_KEY_AUTOSETUPMERGE} */
	public static final String CONFIG_KEY_FALSE = "false";

	/** The "true" key (used to configure {@link #CONFIG_KEY_AUTOSETUPMERGE} */
	public static final String CONFIG_KEY_TRUE = "true";

	/**
	 * The "always" key (used to configure {@link #CONFIG_KEY_AUTOSETUPREBASE}
	 * and {@link #CONFIG_KEY_AUTOSETUPMERGE}
	 */
	public static final String CONFIG_KEY_ALWAYS = "always";

	/** The "never" key (used to configure {@link #CONFIG_KEY_AUTOSETUPREBASE} */
	public static final String CONFIG_KEY_NEVER = "never";

	/** The "local" key (used to configure {@link #CONFIG_KEY_AUTOSETUPREBASE} */
	public static final String CONFIG_KEY_LOCAL = "local";

	/** The "createchangeid" key */
	public static final String CONFIG_KEY_CREATECHANGEID = "createchangeid";

	/** The "defaultsourceref" key */
	public static final String CONFIG_KEY_DEFBRANCHSTARTPOINT = "defbranchstartpoint";

	/** The "path" key */
	public static final String CONFIG_KEY_PATH = "path";

	/** The "update" key */
	public static final String CONFIG_KEY_UPDATE = "update";

	/**
	 * The "ignore" key
	 * @since 3.6
	 */
	public static final String CONFIG_KEY_IGNORE = "ignore";

	/** The "compression" key */
	public static final String CONFIG_KEY_COMPRESSION = "compression";

	/** The "indexversion" key */
	public static final String CONFIG_KEY_INDEXVERSION = "indexversion";

	/**
	 * The "skiphash" key
	 * @since 5.13.2
	 */
	public static final String CONFIG_KEY_SKIPHASH = "skiphash";

	/**
	 * The "hidedotfiles" key
	 * @since 3.5
	 */
	public static final String CONFIG_KEY_HIDEDOTFILES = "hidedotfiles";

	/**
	 * The "dirnogitlinks" key
	 * @since 4.3
	 */
	public static final String CONFIG_KEY_DIRNOGITLINKS = "dirNoGitLinks";

	/** The "precomposeunicode" key */
	public static final String CONFIG_KEY_PRECOMPOSEUNICODE = "precomposeunicode";

	/** The "pruneexpire" key */
	public static final String CONFIG_KEY_PRUNEEXPIRE = "pruneexpire";

	/**
	 * The "prunepackexpire" key
	 * @since 4.3
	 */
	public static final String CONFIG_KEY_PRUNEPACKEXPIRE = "prunepackexpire";

	/**
	 * The "logexpiry" key
	 *
	 * @since 4.7
	 */
	public static final String CONFIG_KEY_LOGEXPIRY = "logExpiry";

	/**
	 * The "autodetach" key
	 *
	 * @since 4.7
	 */
	public static final String CONFIG_KEY_AUTODETACH = "autoDetach";

	/**
	 * The "aggressiveDepth" key
	 * @since 3.6
	 */
	public static final String CONFIG_KEY_AGGRESSIVE_DEPTH = "aggressiveDepth";

	/**
	 * The "aggressiveWindow" key
	 * @since 3.6
	 */
	public static final String CONFIG_KEY_AGGRESSIVE_WINDOW = "aggressiveWindow";

	/** The "mergeoptions" key */
	public static final String CONFIG_KEY_MERGEOPTIONS = "mergeoptions";

	/** The "ff" key */
	public static final String CONFIG_KEY_FF = "ff";

	/**
	 * The "conflictStyle" key.
	 *
	 * @since 5.12
	 */
	public static final String CONFIG_KEY_CONFLICTSTYLE = "conflictStyle";

	/**
	 * The "checkstat" key
	 *
	 * @since 3.0
	 */
	public static final String CONFIG_KEY_CHECKSTAT = "checkstat";

	/**
	 * The "renamelimit" key in the "diff" section
	 * @since 3.0
	 */
	public static final String CONFIG_KEY_RENAMELIMIT = "renamelimit";

	/**
	 * The "trustfolderstat" key in the "core" section
	 * @since 3.6
	 */
	public static final String CONFIG_KEY_TRUSTFOLDERSTAT = "trustfolderstat";

	/**
	 * The "supportsAtomicFileCreation" key in the "core" section
	 *
	 * @since 4.5
	 */
	public static final String CONFIG_KEY_SUPPORTSATOMICFILECREATION = "supportsatomicfilecreation";

	/**
	 * The "sha1Implementation" key in the "core" section
	 *
	 * @since 5.13.2
	 */
	public static final String SHA1_IMPLEMENTATION = "sha1implementation";

	/**
	 * The "noprefix" key in the "diff" section
	 * @since 3.0
	 */
	public static final String CONFIG_KEY_NOPREFIX = "noprefix";

	/**
	 * A "renamelimit" value in the "diff" section
	 * @since 3.0
	 */
	public static final String CONFIG_RENAMELIMIT_COPY = "copy";

	/**
	 * A "renamelimit" value in the "diff" section
	 * @since 3.0
	 */
	public static final String CONFIG_RENAMELIMIT_COPIES = "copies";

	/**
	 * The "renames" key in the "diff" section
	 * @since 3.0
	 */
	public static final String CONFIG_KEY_RENAMES = "renames";

	/**
	 * The "inCoreLimit" key in the "merge" section. It's a size limit (bytes) used to
	 * control a file to be stored in {@code Heap} or {@code LocalFile} during the merge.
	 * @since 4.9
	 */
	public static final String CONFIG_KEY_IN_CORE_LIMIT = "inCoreLimit";

	/**
	 * The "prune" key
	 * @since 3.3
	 */
	public static final String CONFIG_KEY_PRUNE = "prune";

	/**
	 * The "streamBuffer" key
	 * @since 4.0
	 */
	public static final String CONFIG_KEY_STREAM_BUFFER = "streamBuffer";

	/**
	 * The "streamRatio" key
	 * @since 4.0
	 */
	public static final String CONFIG_KEY_STREAM_RATIO = "streamRatio";

	/**
	 * Flag in the filter section whether to use JGit's implementations of
	 * filters and hooks
	 * @since 4.6
	 */
	public static final String CONFIG_KEY_USEJGITBUILTIN = "useJGitBuiltin";

	/**
	 * The "fetchRecurseSubmodules" key
	 * @since 4.7
	 */
	public static final String CONFIG_KEY_FETCH_RECURSE_SUBMODULES = "fetchRecurseSubmodules";

	/**
	 * The "recurseSubmodules" key
	 * @since 4.7
	 */
	public static final String CONFIG_KEY_RECURSE_SUBMODULES = "recurseSubmodules";

	/**
	 * The "required" key
	 * @since 4.11
	 */
	public static final String CONFIG_KEY_REQUIRED = "required";

	/**
	 * The "lfs" section
	 * @since 4.11
	 */
	public static final String CONFIG_SECTION_LFS = "lfs";

	/**
	 * The "i18n" section
	 *
	 * @since 5.2
	 */
	public static final String CONFIG_SECTION_I18N = "i18n";

	/**
	 * The "commitEncoding" key
	 *
	 * @since 5.13
	 */
	public static final String CONFIG_KEY_COMMIT_ENCODING = "commitEncoding";

	/**
	 * The "logOutputEncoding" key
	 *
	 * @since 5.2
	 */
	public static final String CONFIG_KEY_LOG_OUTPUT_ENCODING = "logOutputEncoding";

	/**
	 * The "filesystem" section
	 * @since 5.1.9
	 */
	public static final String CONFIG_FILESYSTEM_SECTION = "filesystem";

	/**
	 * The "timestampResolution" key
	 * @since 5.1.9
	 */
	public static final String CONFIG_KEY_TIMESTAMP_RESOLUTION = "timestampResolution";

	/**
	 * The "minRacyThreshold" key
	 * @since 5.1.9
	 */
	public static final String CONFIG_KEY_MIN_RACY_THRESHOLD = "minRacyThreshold";


	/**
	 * The "refStorage" key
	 *
	 * @since 5.6.2
	 */
	public static final String CONFIG_KEY_REF_STORAGE = "refStorage";

	/**
	 * The "extensions" section
	 *
	 * @since 5.6.2
	 */
	public static final String CONFIG_EXTENSIONS_SECTION = "extensions";

	/**
	 * The extensions.refStorage key
	 * @since 5.7
	 */
	public static final String CONFIG_KEY_REFSTORAGE = "refStorage";

	/**
	 * The "reftable" refStorage format
	 * @since 5.7
	 */
	public static final String CONFIG_REF_STORAGE_REFTABLE = "reftable";

	/**
	 * The "jmx" section
	 * @since 5.1.13
	 */
	public static final String CONFIG_JMX_SECTION = "jmx";

	/**
	 * The "pack.bigfilethreshold" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_BIGFILE_THRESHOLD = "bigfilethreshold";

	/**
	 * The "pack.bitmapContiguousCommitCount" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_BITMAP_CONTIGUOUS_COMMIT_COUNT = "bitmapcontiguouscommitcount";

	/**
	 * The "pack.bitmapDistantCommitSpan" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_BITMAP_DISTANT_COMMIT_SPAN = "bitmapdistantcommitspan";

	/**
	 * The "pack.bitmapExcessiveBranchCount" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_BITMAP_EXCESSIVE_BRANCH_COUNT = "bitmapexcessivebranchcount";

	/**
	 * The "pack.bitmapExcludedRefsPrefixes" key
	 * @since 5.13.2
	 */
	public static final String CONFIG_KEY_BITMAP_EXCLUDED_REFS_PREFIXES = "bitmapexcludedrefsprefixes";

	/**
	 * The "pack.bitmapInactiveBranchAgeInDays" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_BITMAP_INACTIVE_BRANCH_AGE_INDAYS = "bitmapinactivebranchageindays";

	/**
	 * The "pack.bitmapRecentCommitSpan" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_BITMAP_RECENT_COMMIT_COUNT = "bitmaprecentcommitspan";

	/**
	 * The "pack.writeReverseIndex" key
	 *
	 * @since 6.6
	 */
	public static final String CONFIG_KEY_WRITE_REVERSE_INDEX = "writeReverseIndex";

	/**
	 * The "pack.buildBitmaps" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_BUILD_BITMAPS = "buildbitmaps";

	/**
	 * The "pack.cutDeltaChains" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_CUT_DELTACHAINS = "cutdeltachains";

	/**
	 * The "pack.deltaCacheLimit" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_DELTA_CACHE_LIMIT = "deltacachelimit";

	/**
	 * The "pack.deltaCacheSize" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_DELTA_CACHE_SIZE = "deltacachesize";

	/**
	 * The "pack.deltaCompression" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_DELTA_COMPRESSION = "deltacompression";

	/**
	 * The "pack.depth" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_DEPTH = "depth";

	/**
	 * The "pack.minSizePreventRacyPack" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_MIN_SIZE_PREVENT_RACYPACK = "minsizepreventracypack";

	/**
	 * The "pack.reuseDeltas" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_REUSE_DELTAS = "reusedeltas";

	/**
	 * The "pack.reuseObjects" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_REUSE_OBJECTS = "reuseobjects";

	/**
	 * The "pack.singlePack" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_SINGLE_PACK = "singlepack";

	/**
	 * The "pack.threads" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_THREADS = "threads";

	/**
	 * The "pack.waitPreventRacyPack" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_WAIT_PREVENT_RACYPACK = "waitpreventracypack";

	/**
	 * The "pack.window" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_WINDOW = "window";

	/**
	 * The "pack.windowMemory" key
	 * @since 5.8
	 */
	public static final String CONFIG_KEY_WINDOW_MEMORY = "windowmemory";

	/**
	 * the "pack.minBytesForObjSizeIndex" key
	 *
	 * @since 6.5
	 */
	public static final String CONFIG_KEY_MIN_BYTES_OBJ_SIZE_INDEX = "minBytesForObjSizeIndex";

	/**
	 * The "feature" section
	 *
	 * @since 5.9
	 */
	public static final String CONFIG_FEATURE_SECTION = "feature";

	/**
	 * The "feature.manyFiles" key
	 *
	 * @since 5.9
	 */
	public static final String CONFIG_KEY_MANYFILES = "manyFiles";

	/**
	 * The "index" section
	 *
	 * @since 5.9
	 */
	public static final String CONFIG_INDEX_SECTION = "index";

	/**
	 * The "version" key
	 *
	 * @since 5.9
	 */
	public static final String CONFIG_KEY_VERSION = "version";

	/**
	 * The "init" section
	 *
	 * @since 5.11
	 */
	public static final String CONFIG_INIT_SECTION = "init";

	/**
	 * The "defaultBranch" key
	 *
	 * @since 5.11
	 */
	public static final String CONFIG_KEY_DEFAULT_BRANCH = "defaultbranch";

	/**
	 * The "pack.searchForReuseTimeout" key
	 *
	 * @since 5.13
	 */
	public static final String CONFIG_KEY_SEARCH_FOR_REUSE_TIMEOUT = "searchforreusetimeout";

	/**
	 * The "push" section.
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_PUSH_SECTION = "push";

	/**
	 * The "default" key.
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_KEY_DEFAULT = "default";

	/**
	 * The "abbrev" key
	 *
	 * @since 6.1
	 */
	public static final String CONFIG_KEY_ABBREV = "abbrev";

	/**
	 * The "writeCommitGraph" key
	 *
	 * @since 6.5
	 */
	public static final String CONFIG_KEY_WRITE_COMMIT_GRAPH = "writeCommitGraph";

	/**
	 * The "commitGraph" used by commit-graph feature
	 *
	 * @since 6.5
	 */
	public static final String CONFIG_COMMIT_GRAPH = "commitGraph";

	/**
	 * The "trustPackedRefsStat" key
	 *
	 * @since 6.1.1
	 */
	public static final String CONFIG_KEY_TRUST_PACKED_REFS_STAT = "trustPackedRefsStat";

	/**
	 * The "pack.preserveOldPacks" key
	 *
	 * @since 5.13.2
	 */
	public static final String CONFIG_KEY_PRESERVE_OLD_PACKS = "preserveoldpacks";

	/**
	 * The "pack.prunePreserved" key
	 *
	 * @since 5.13.2
	 */
	public static final String CONFIG_KEY_PRUNE_PRESERVED = "prunepreserved";

	/**
	 * The "commitGraph" section
	 *
	 * @since 6.7
	 */
	public static final String CONFIG_COMMIT_GRAPH_SECTION = "commitGraph";

	/**
	 * The "writeChangedPaths" key
	 *
	 * @since 6.7
	 */
	public static final String CONFIG_KEY_WRITE_CHANGED_PATHS = "writeChangedPaths";

	/**
	 * The "readChangedPaths" key
	 *
	 * @since 6.7
	 */
	public static final String CONFIG_KEY_READ_CHANGED_PATHS = "readChangedPaths";
}
