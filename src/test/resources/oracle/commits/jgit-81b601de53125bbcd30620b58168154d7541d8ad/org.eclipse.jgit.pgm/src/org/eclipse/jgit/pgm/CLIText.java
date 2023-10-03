/*
 * Copyright (C) 2010, 2012 Sasa Zivkov <sasa.zivkov@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.pgm;

import java.text.MessageFormat;

import org.eclipse.jgit.nls.NLS;
import org.eclipse.jgit.nls.TranslationBundle;

/**
 * Translation bundle for JGit command line interface
 */
public class CLIText extends TranslationBundle {

	/**
	 * @return an instance of this translation bundle
	 */
	public static CLIText get() {
		return NLS.getBundleFor(CLIText.class);
	}

	/**
	 * Format the given line for using the format defined by {@link #lineFormat}
	 * ("# " by default).
	 *
	 * @param line
	 *            the line to format
	 * @return the formatted line
	 * @since 2.2
	 */
	public static String formatLine(String line) {
		return MessageFormat.format(get().lineFormat, line);
	}

	// @formatter:off
	/***/ public String alreadyOnBranch;
	/***/ public String alreadyUpToDate;
	/***/ public String archiveEntryModeIgnored;
	/***/ public String authorInfo;
	/***/ public String averageMSPerRead;
	/***/ public String branchAlreadyExists;
	/***/ public String branchCreatedFrom;
	/***/ public String branchDetachedHEAD;
	/***/ public String branchIsNotAnAncestorOfYourCurrentHEAD;
	/***/ public String branchNotFound;
	/***/ public String cacheTreePathInfo;
	/***/ public String configFileNotFound;
	/***/ public String cannotBeRenamed;
	/***/ public String cannotChekoutNoHeadsAdvertisedByRemote;
	/***/ public String cannotCombineSquashWithNoff;
	/***/ public String cannotCreateCommand;
	/***/ public String cannotCreateOutputStream;
	/***/ public String cannotDeatchHEAD;
	/***/ public String cannotDeleteFile;
	/***/ public String cannotDeleteTheBranchWhichYouAreCurrentlyOn;
	/***/ public String cannotGuessLocalNameFrom;
	/***/ public String cannotLock;
	/***/ public String cannotMergeDetachedHead;
	/***/ public String cannotReadBecause;
	/***/ public String cannotReadPackageInformation;
	/***/ public String cannotRenameDetachedHEAD;
	/***/ public String cannotResolve;
	/***/ public String cannotSetupConsole;
	/***/ public String cannotUseObjectsWithGlog;
	/***/ public String cannotWrite;
	/***/ public String cantFindGitDirectory;
	/***/ public String cantWrite;
	/***/ public String changesNotStagedForCommit;
	/***/ public String changesToBeCommitted;
	/***/ public String commitLabel;
	/***/ public String conflictingUsageOf_git_dir_andArguments;
	/***/ public String couldNotCreateBranch;
	/***/ public String dateInfo;
	/***/ public String deletedBranch;
	/***/ public String deletedRemoteBranch;
	/***/ public String doesNotExist;
	/***/ public String dontOverwriteLocalChanges;
	/***/ public String everythingUpToDate;
	/***/ public String expectedNumberOfbytes;
	/***/ public String exporting;
	/***/ public String failedToCommitIndex;
	/***/ public String failedToLockIndex;
	/***/ public String failedToLockTag;
	/***/ public String fatalError;
	/***/ public String fatalThisProgramWillDestroyTheRepository;
	/***/ public String fileIsRequired;
	/***/ public String ffNotPossibleAborting;
	/***/ public String forcedUpdate;
	/***/ public String fromURI;
	/***/ public String initializedEmptyGitRepositoryIn;
	/***/ public String invalidHttpProxyOnlyHttpSupported;
	/***/ public String jgitVersion;
	/***/ public String lineFormat;
	/***/ public String listeningOn;
	/***/ public String mergeConflict;
	/***/ public String mergeFailed;
	/***/ public String mergeMadeBy;
	/***/ public String mergedSquashed;
	/***/ public String metaVar_KEY;
	/***/ public String metaVar_archiveFormat;
	/***/ public String metaVar_arg;
	/***/ public String metaVar_author;
	/***/ public String metaVar_bucket;
	/***/ public String metaVar_command;
	/***/ public String metaVar_commandDetail;
	/***/ public String metaVar_commitOrTag;
	/***/ public String metaVar_commitPaths;
	/***/ public String metaVar_commitish;
	/***/ public String metaVar_configFile;
	/***/ public String metaVar_connProp;
	/***/ public String metaVar_diffAlg;
	/***/ public String metaVar_directory;
	/***/ public String metaVar_file;
	/***/ public String metaVar_filepattern;
	/***/ public String metaVar_gitDir;
	/***/ public String metaVar_hostName;
	/***/ public String metaVar_linesOfContext;
	/***/ public String metaVar_message;
	/***/ public String metaVar_n;
	/***/ public String metaVar_name;
	/***/ public String metaVar_object;
	/***/ public String metaVar_op;
	/***/ public String metaVar_pass;
	/***/ public String metaVar_path;
	/***/ public String metaVar_paths;
	/***/ public String metaVar_port;
	/***/ public String metaVar_ref;
	/***/ public String metaVar_refs;
	/***/ public String metaVar_refspec;
	/***/ public String metaVar_remoteName;
	/***/ public String metaVar_seconds;
	/***/ public String metaVar_service;
	/***/ public String metaVar_treeish;
	/***/ public String metaVar_uriish;
	/***/ public String metaVar_url;
	/***/ public String metaVar_user;
	/***/ public String metaVar_version;
	/***/ public String mostCommonlyUsedCommandsAre;
	/***/ public String needApprovalToDestroyCurrentRepository;
	/***/ public String noGitRepositoryConfigured;
	/***/ public String noSuchFile;
	/***/ public String noSuchRemoteRef;
	/***/ public String noTREESectionInIndex;
	/***/ public String nonFastForward;
	/***/ public String notABranch;
	/***/ public String notACommit;
	/***/ public String notAGitRepository;
	/***/ public String notAJgitCommand;
	/***/ public String notARevision;
	/***/ public String notATagVersionIsRequired;
	/***/ public String notATree;
	/***/ public String notAValidRefName;
	/***/ public String notAnIndexFile;
	/***/ public String notAnObject;
	/***/ public String notFound;
	/***/ public String notOnAnyBranch;
	/***/ public String noteObjectTooLargeToPrint;
	/***/ public String nothingToSquash;
	/***/ public String onBranchToBeBorn;
	/***/ public String onBranch;
	/***/ public String onlyOneMetaVarExpectedIn;
	/***/ public String onlyOneOfIncludeOnlyAllInteractiveCanBeUsed;
	/***/ public String pathspecDidNotMatch;
	/***/ public String pushTo;
	/***/ public String pathsRequired;
	/***/ public String refDoesNotExistOrNoCommit;
	/***/ public String remoteMessage;
	/***/ public String remoteRefObjectChangedIsNotExpectedOne;
	/***/ public String remoteSideDoesNotSupportDeletingRefs;
	/***/ public String repaint;
	/***/ public String serviceNotSupported;
	/***/ public String skippingObject;
	/***/ public String statusFileListFormat;
	/***/ public String statusFileListFormatWithPrefix;
	/***/ public String statusModified;
	/***/ public String statusNewFile;
	/***/ public String statusRemoved;
	/***/ public String switchedToNewBranch;
	/***/ public String switchedToBranch;
	/***/ public String tagAlreadyExists;
	/***/ public String tagLabel;
	/***/ public String taggerInfo;
	/***/ public String timeInMilliSeconds;
	/***/ public String tooManyRefsGiven;
	/***/ public String treeIsRequired;
	/***/ public char[] unknownIoErrorStdout;
	/***/ public String unknownMergeStrategy;
	/***/ public String unmergedPaths;
	/***/ public String unsupportedOperation;
	/***/ public String untrackedFiles;
	/***/ public String updating;
	/***/ public String warningNoCommitGivenOnCommandLine;
}
