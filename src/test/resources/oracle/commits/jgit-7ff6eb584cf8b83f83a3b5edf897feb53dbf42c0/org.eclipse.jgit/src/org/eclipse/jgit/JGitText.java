/*
 * Copyright (C) 2010, Sasa Zivkov <sasa.zivkov@sap.com>
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

package org.eclipse.jgit;

import org.eclipse.jgit.nls.NLS;
import org.eclipse.jgit.nls.TranslationBundle;

/**
 * Translation bundle for JGit core
 */
public class JGitText extends TranslationBundle {

	/**
	 * @return an instance of this translation bundle
	 */
	public static JGitText get() {
		return NLS.getBundleFor(JGitText.class);
	}

	/***/ public String DIRCChecksumMismatch;
	/***/ public String DIRCExtensionIsTooLargeAt;
	/***/ public String DIRCExtensionNotSupportedByThisVersion;
	/***/ public String DIRCHasTooManyEntries;
	/***/ public String DIRCUnrecognizedExtendedFlags;
	/***/ public String JRELacksMD5Implementation;
	/***/ public String URINotSupported;
	/***/ public String URLNotFound;
	/***/ public String aNewObjectIdIsRequired;
	/***/ public String abbreviationLengthMustBeNonNegative;
	/***/ public String abortingRebase;
	/***/ public String abortingRebaseFailed;
	/***/ public String advertisementCameBefore;
	/***/ public String advertisementOfCameBefore;
	/***/ public String amazonS3ActionFailed;
	/***/ public String amazonS3ActionFailedGivingUp;
	/***/ public String ambiguousObjectAbbreviation;
	/***/ public String anExceptionOccurredWhileTryingToAddTheIdOfHEAD;
	/***/ public String applyingCommit;
	/***/ public String anSSHSessionHasBeenAlreadyCreated;
	/***/ public String atLeastOnePathIsRequired;
	/***/ public String atLeastOnePatternIsRequired;
	/***/ public String atLeastTwoFiltersNeeded;
	/***/ public String authenticationNotSupported;
	/***/ public String badBase64InputCharacterAt;
	/***/ public String badEntryDelimiter;
	/***/ public String badEntryName;
	/***/ public String badEscape;
	/***/ public String badGroupHeader;
	/***/ public String badObjectType;
	/***/ public String badSectionEntry;
	/***/ public String base64InputNotProperlyPadded;
	/***/ public String baseLengthIncorrect;
	/***/ public String bareRepositoryNoWorkdirAndIndex;
	/***/ public String blameNotCommittedYet;
	/***/ public String blobNotFound;
	/***/ public String blobNotFoundForPath;
	/***/ public String branchNameInvalid;
	/***/ public String cachedPacksPreventsIndexCreation;
	/***/ public String cannotBeCombined;
	/***/ public String cannotCombineTreeFilterWithRevFilter;
	/***/ public String cannotCommitOnARepoWithState;
	/***/ public String cannotCommitWriteTo;
	/***/ public String cannotConnectPipes;
	/***/ public String cannotConvertScriptToText;
	/***/ public String cannotCreateConfig;
	/***/ public String cannotCreateDirectory;
	/***/ public String cannotCreateHEAD;
	/***/ public String cannotDeleteCheckedOutBranch;
	/***/ public String cannotDeleteFile;
	/***/ public String cannotDeleteStaleTrackingRef2;
	/***/ public String cannotDeleteStaleTrackingRef;
	/***/ public String cannotDetermineProxyFor;
	/***/ public String cannotDownload;
	/***/ public String cannotExecute;
	/***/ public String cannotGet;
	/***/ public String cannotListRefs;
	/***/ public String cannotLock;
	/***/ public String cannotLockFile;
	/***/ public String cannotLockPackIn;
	/***/ public String cannotMatchOnEmptyString;
	/***/ public String cannotMoveIndexTo;
	/***/ public String cannotMovePackTo;
	/***/ public String cannotOpenService;
	/***/ public String cannotParseGitURIish;
	/***/ public String cannotPullOnARepoWithState;
	/***/ public String cannotRead;
	/***/ public String cannotReadBlob;
	/***/ public String cannotReadCommit;
	/***/ public String cannotReadFile;
	/***/ public String cannotReadHEAD;
	/***/ public String cannotReadObject;
	/***/ public String cannotReadTree;
	/***/ public String cannotRebaseWithoutCurrentHead;
	/***/ public String cannotResolveLocalTrackingRefForUpdating;
	/***/ public String cannotStoreObjects;
	/***/ public String cannotUnloadAModifiedTree;
	/***/ public String cannotWorkWithOtherStagesThanZeroRightNow;
	/***/ public String canOnlyCherryPickCommitsWithOneParent;
	/***/ public String canOnlyRevertCommitsWithOneParent;
	/***/ public String cantFindObjectInReversePackIndexForTheSpecifiedOffset;
	/***/ public String cantPassMeATree;
	/***/ public String channelMustBeInRange0_255;
	/***/ public String characterClassIsNotSupported;
	/***/ public String checkoutUnexpectedResult;
	/***/ public String checkoutConflictWithFile;
	/***/ public String checkoutConflictWithFiles;
	/***/ public String classCastNotA;
	/***/ public String collisionOn;
	/***/ public String commandWasCalledInTheWrongState;
	/***/ public String commitAlreadyExists;
	/***/ public String commitMessageNotSpecified;
	/***/ public String commitOnRepoWithoutHEADCurrentlyNotSupported;
	/***/ public String compressingObjects;
	/***/ public String connectionFailed;
	/***/ public String connectionTimeOut;
	/***/ public String contextMustBeNonNegative;
	/***/ public String corruptObjectBadStream;
	/***/ public String corruptObjectBadStreamCorruptHeader;
	/***/ public String corruptObjectGarbageAfterSize;
	/***/ public String corruptObjectIncorrectLength;
	/***/ public String corruptObjectInvalidEntryMode;
	/***/ public String corruptObjectInvalidMode2;
	/***/ public String corruptObjectInvalidMode3;
	/***/ public String corruptObjectInvalidMode;
	/***/ public String corruptObjectInvalidType2;
	/***/ public String corruptObjectInvalidType;
	/***/ public String corruptObjectMalformedHeader;
	/***/ public String corruptObjectNegativeSize;
	/***/ public String corruptObjectNoAuthor;
	/***/ public String corruptObjectNoCommitter;
	/***/ public String corruptObjectNoHeader;
	/***/ public String corruptObjectNoObject;
	/***/ public String corruptObjectNoTagName;
	/***/ public String corruptObjectNoTaggerBadHeader;
	/***/ public String corruptObjectNoTaggerHeader;
	/***/ public String corruptObjectNoType;
	/***/ public String corruptObjectNotree;
	/***/ public String corruptObjectPackfileChecksumIncorrect;
	/***/ public String corruptionDetectedReReadingAt;
	/***/ public String couldNotCheckOutBecauseOfConflicts;
	/***/ public String couldNotDeleteLockFileShouldNotHappen;
	/***/ public String couldNotDeleteTemporaryIndexFileShouldNotHappen;
	/***/ public String couldNotGetAdvertisedRef;
	/***/ public String couldNotLockHEAD;
	/***/ public String couldNotReadIndexInOneGo;
	/***/ public String couldNotReadObjectWhileParsingCommit;
	/***/ public String couldNotRenameDeleteOldIndex;
	/***/ public String couldNotRenameTemporaryFile;
	/***/ public String couldNotRenameTemporaryIndexFileToIndex;
	/***/ public String couldNotURLEncodeToUTF8;
	/***/ public String couldNotWriteFile;
	/***/ public String countingObjects;
	/***/ public String createBranchFailedUnknownReason;
	/***/ public String createBranchUnexpectedResult;
	/***/ public String createNewFileFailed;
	/***/ public String credentialPassword;
	/***/ public String credentialUsername;
	/***/ public String daemonAlreadyRunning;
	/***/ public String daysAgo;
	/***/ public String deleteBranchUnexpectedResult;
	/***/ public String deleteFileFailed;
	/***/ public String deletingNotSupported;
	/***/ public String destinationIsNotAWildcard;
	/***/ public String detachedHeadDetected;
	/***/ public String dirCacheDoesNotHaveABackingFile;
	/***/ public String dirCacheFileIsNotLocked;
	/***/ public String dirCacheIsNotLocked;
	/***/ public String dirtyFilesExist;
	/***/ public String doesNotHandleMode;
	/***/ public String downloadCancelled;
	/***/ public String downloadCancelledDuringIndexing;
	/***/ public String duplicateAdvertisementsOf;
	/***/ public String duplicateRef;
	/***/ public String duplicateRemoteRefUpdateIsIllegal;
	/***/ public String duplicateStagesNotAllowed;
	/***/ public String eitherGitDirOrWorkTreeRequired;
	/***/ public String emptyCommit;
	/***/ public String emptyPathNotPermitted;
	/***/ public String encryptionError;
	/***/ public String endOfFileInEscape;
	/***/ public String entryNotFoundByPath;
	/***/ public String enumValueNotSupported2;
	/***/ public String enumValueNotSupported3;
	/***/ public String enumValuesNotAvailable;
	/***/ public String errorDecodingFromFile;
	/***/ public String errorEncodingFromFile;
	/***/ public String errorInBase64CodeReadingStream;
	/***/ public String errorInPackedRefs;
	/***/ public String errorInvalidProtocolWantedOldNewRef;
	/***/ public String errorListing;
	/***/ public String errorOccurredDuringUnpackingOnTheRemoteEnd;
	/***/ public String errorReadingInfoRefs;
	/***/ public String exceptionCaughtDuringExecutionOfAddCommand;
	/***/ public String exceptionCaughtDuringExecutionOfCherryPickCommand;
	/***/ public String exceptionCaughtDuringExecutionOfCommitCommand;
	/***/ public String exceptionCaughtDuringExecutionOfFetchCommand;
	/***/ public String exceptionCaughtDuringExecutionOfLsRemoteCommand;
	/***/ public String exceptionCaughtDuringExecutionOfMergeCommand;
	/***/ public String exceptionCaughtDuringExecutionOfPushCommand;
	/***/ public String exceptionCaughtDuringExecutionOfPullCommand;
	/***/ public String exceptionCaughtDuringExecutionOfResetCommand;
	/***/ public String exceptionCaughtDuringExecutionOfRevertCommand;
	/***/ public String exceptionCaughtDuringExecutionOfRmCommand;
	/***/ public String exceptionCaughtDuringExecutionOfTagCommand;
	/***/ public String exceptionOccurredDuringAddingOfOptionToALogCommand;
	/***/ public String exceptionOccurredDuringReadingOfGIT_DIR;
	/***/ public String expectedACKNAKFoundEOF;
	/***/ public String expectedACKNAKGot;
	/***/ public String expectedBooleanStringValue;
	/***/ public String expectedCharacterEncodingGuesses;
	/***/ public String expectedEOFReceived;
	/***/ public String expectedGot;
	/***/ public String expectedPktLineWithService;
	/***/ public String expectedReceivedContentType;
	/***/ public String expectedReportForRefNotReceived;
	/***/ public String failedUpdatingRefs;
	/***/ public String failureDueToOneOfTheFollowing;
	/***/ public String failureUpdatingFETCH_HEAD;
	/***/ public String failureUpdatingTrackingRef;
	/***/ public String fileCannotBeDeleted;
	/***/ public String fileIsTooBigForThisConvenienceMethod;
	/***/ public String fileIsTooLarge;
	/***/ public String fileModeNotSetForPath;
	/***/ public String flagIsDisposed;
	/***/ public String flagNotFromThis;
	/***/ public String flagsAlreadyCreated;
	/***/ public String funnyRefname;
	/***/ public String hoursAgo;
	/***/ public String hugeIndexesAreNotSupportedByJgitYet;
	/***/ public String hunkBelongsToAnotherFile;
	/***/ public String hunkDisconnectedFromFile;
	/***/ public String hunkHeaderDoesNotMatchBodyLineCountOf;
	/***/ public String illegalArgumentNotA;
	/***/ public String illegalCombinationOfArguments;
	/***/ public String illegalStateExists;
	/***/ public String improperlyPaddedBase64Input;
	/***/ public String inMemoryBufferLimitExceeded;
	/***/ public String incorrectHashFor;
	/***/ public String incorrectOBJECT_ID_LENGTH;
	/***/ public String indexFileIsInUse;
	/***/ public String indexFileIsTooLargeForJgit;
	/***/ public String indexSignatureIsInvalid;
	/***/ public String indexWriteException;
	/***/ public String integerValueOutOfRange;
	/***/ public String internalRevisionError;
	/***/ public String internalServerError;
	/***/ public String interruptedWriting;
	/***/ public String inTheFuture;
	/***/ public String invalidAdvertisementOf;
	/***/ public String invalidAncestryLength;
	/***/ public String invalidBooleanValue;
	/***/ public String invalidChannel;
	/***/ public String invalidCharacterInBase64Data;
	/***/ public String invalidCommitParentNumber;
	/***/ public String invalidEncryption;
	/***/ public String invalidGitType;
	/***/ public String invalidId;
	/***/ public String invalidIdLength;
	/***/ public String invalidIntegerValue;
	/***/ public String invalidKey;
	/***/ public String invalidLineInConfigFile;
	/***/ public String invalidModeFor;
	/***/ public String invalidModeForPath;
	/***/ public String invalidObject;
	/***/ public String invalidOldIdSent;
	/***/ public String invalidPacketLineHeader;
	/***/ public String invalidPath;
	/***/ public String invalidRemote;
	/***/ public String invalidRefName;
	/***/ public String invalidStageForPath;
	/***/ public String invalidTagOption;
	/***/ public String invalidTimeout;
	/***/ public String invalidURL;
	/***/ public String invalidWildcards;
	/***/ public String invalidWindowSize;
	/***/ public String isAStaticFlagAndHasNorevWalkInstance;
	/***/ public String kNotInRange;
	/***/ public String largeObjectException;
	/***/ public String largeObjectOutOfMemory;
	/***/ public String largeObjectExceedsByteArray;
	/***/ public String largeObjectExceedsLimit;
	/***/ public String lengthExceedsMaximumArraySize;
	/***/ public String listingAlternates;
	/***/ public String localObjectsIncomplete;
	/***/ public String localRefIsMissingObjects;
	/***/ public String lockCountMustBeGreaterOrEqual1;
	/***/ public String lockError;
	/***/ public String lockOnNotClosed;
	/***/ public String lockOnNotHeld;
	/***/ public String malformedpersonIdentString;
	/***/ public String mergeConflictOnNotes;
	/***/ public String mergeConflictOnNonNoteEntries;
	/***/ public String mergeStrategyAlreadyExistsAsDefault;
	/***/ public String mergeStrategyDoesNotSupportHeads;
	/***/ public String mergeUsingStrategyResultedInDescription;
	/***/ public String minutesAgo;
	/***/ public String missingAccesskey;
	/***/ public String missingConfigurationForKey;
	/***/ public String missingDeltaBase;
	/***/ public String missingForwardImageInGITBinaryPatch;
	/***/ public String missingObject;
	/***/ public String missingPrerequisiteCommits;
	/***/ public String missingRequiredParameter;
	/***/ public String missingSecretkey;
	/***/ public String mixedStagesNotAllowed;
	/***/ public String mkDirFailed;
	/***/ public String mkDirsFailed;
	/***/ public String month;
	/***/ public String months;
	/***/ public String monthsAgo;
	/***/ public String multipleMergeBasesFor;
	/***/ public String need2Arguments;
	/***/ public String needPackOut;
	/***/ public String needsAtLeastOneEntry;
	/***/ public String needsWorkdir;
	/***/ public String newlineInQuotesNotAllowed;
	/***/ public String noApplyInDelete;
	/***/ public String noClosingBracket;
	/***/ public String noHEADExistsAndNoExplicitStartingRevisionWasSpecified;
	/***/ public String noHMACsupport;
	/***/ public String noMergeHeadSpecified;
	/***/ public String noSuchRef;
	/***/ public String noXMLParserAvailable;
	/***/ public String notABoolean;
	/***/ public String notABundle;
	/***/ public String notADIRCFile;
	/***/ public String notAGitDirectory;
	/***/ public String notAPACKFile;
	/***/ public String notARef;
	/***/ public String notASCIIString;
	/***/ public String notAuthorized;
	/***/ public String notAValidPack;
	/***/ public String notFound;
	/***/ public String nothingToFetch;
	/***/ public String nothingToPush;
	/***/ public String notMergedExceptionMessage;
	/***/ public String objectAtHasBadZlibStream;
	/***/ public String objectAtPathDoesNotHaveId;
	/***/ public String objectIsCorrupt;
	/***/ public String objectIsNotA;
	/***/ public String objectNotFoundIn;
	/***/ public String obtainingCommitsForCherryPick;
	/***/ public String offsetWrittenDeltaBaseForObjectNotFoundInAPack;
	/***/ public String onlyAlreadyUpToDateAndFastForwardMergesAreAvailable;
	/***/ public String onlyOneFetchSupported;
	/***/ public String onlyOneOperationCallPerConnectionIsSupported;
	/***/ public String openFilesMustBeAtLeast1;
	/***/ public String openingConnection;
	/***/ public String operationCanceled;
	/***/ public String outputHasAlreadyBeenStarted;
	/***/ public String packChecksumMismatch;
	/***/ public String packCorruptedWhileWritingToFilesystem;
	/***/ public String packDoesNotMatchIndex;
	/***/ public String packFileInvalid;
	/***/ public String packHasUnresolvedDeltas;
	/***/ public String packObjectCountMismatch;
	/***/ public String packTooLargeForIndexVersion1;
	/***/ public String packetSizeMustBeAtLeast;
	/***/ public String packetSizeMustBeAtMost;
	/***/ public String packfileCorruptionDetected;
	/***/ public String packfileIsTruncated;
	/***/ public String packingCancelledDuringObjectsWriting;
	/***/ public String packWriterStatistics;
	/***/ public String pathIsNotInWorkingDir;
	/***/ public String peeledLineBeforeRef;
	/***/ public String peerDidNotSupplyACompleteObjectGraph;
	/***/ public String prefixRemote;
	/***/ public String problemWithResolvingPushRefSpecsLocally;
	/***/ public String progressMonUploading;
	/***/ public String propertyIsAlreadyNonNull;
	/***/ public String pullTaskName;
	/***/ public String pushCancelled;
	/***/ public String pushIsNotSupportedForBundleTransport;
	/***/ public String pushNotPermitted;
	/***/ public String rawLogMessageDoesNotParseAsLogEntry;
	/***/ public String readTimedOut;
	/***/ public String readingObjectsFromLocalRepositoryFailed;
	/***/ public String receivingObjects;
	/***/ public String refAlreadExists;
	/***/ public String refNotResolved;
	/***/ public String refUpdateReturnCodeWas;
	/***/ public String reflogsNotYetSupportedByRevisionParser;
	/***/ public String remoteConfigHasNoURIAssociated;
	/***/ public String remoteDoesNotHaveSpec;
	/***/ public String remoteDoesNotSupportSmartHTTPPush;
	/***/ public String remoteHungUpUnexpectedly;
	/***/ public String remoteNameCantBeNull;
	/***/ public String renameBranchFailedBecauseTag;
	/***/ public String renameBranchFailedUnknownReason;
	/***/ public String renameBranchUnexpectedResult;
	/***/ public String renamesAlreadyFound;
	/***/ public String renamesBreakingModifies;
	/***/ public String renamesFindingByContent;
	/***/ public String renamesFindingExact;
	/***/ public String renamesRejoiningModifies;
	/***/ public String repositoryAlreadyExists;
	/***/ public String repositoryConfigFileInvalid;
	/***/ public String repositoryIsRequired;
	/***/ public String repositoryNotFound;
	/***/ public String repositoryState_applyMailbox;
	/***/ public String repositoryState_bisecting;
	/***/ public String repositoryState_conflicts;
	/***/ public String repositoryState_merged;
	/***/ public String repositoryState_normal;
	/***/ public String repositoryState_rebase;
	/***/ public String repositoryState_rebaseInteractive;
	/***/ public String repositoryState_rebaseOrApplyMailbox;
	/***/ public String repositoryState_rebaseWithMerge;
	/***/ public String requiredHashFunctionNotAvailable;
	/***/ public String resettingHead;
	/***/ public String resolvingDeltas;
	/***/ public String resultLengthIncorrect;
	/***/ public String rewinding;
	/***/ public String searchForReuse;
	/***/ public String searchForSizes;
	/***/ public String secondsAgo;
	/***/ public String sequenceTooLargeForDiffAlgorithm;
	/***/ public String serviceNotEnabledNoName;
	/***/ public String serviceNotPermitted;
	/***/ public String serviceNotPermittedNoName;
	/***/ public String shortCompressedStreamAt;
	/***/ public String shortReadOfBlock;
	/***/ public String shortReadOfOptionalDIRCExtensionExpectedAnotherBytes;
	/***/ public String shortSkipOfBlock;
	/***/ public String signingNotSupportedOnTag;
	/***/ public String similarityScoreMustBeWithinBounds;
	/***/ public String sizeExceeds2GB;
	/***/ public String smartHTTPPushDisabled;
	/***/ public String sourceDestinationMustMatch;
	/***/ public String sourceIsNotAWildcard;
	/***/ public String sourceRefDoesntResolveToAnyObject;
	/***/ public String sourceRefNotSpecifiedForRefspec;
	/***/ public String staleRevFlagsOn;
	/***/ public String startingReadStageWithoutWrittenRequestDataPendingIsNotSupported;
	/***/ public String statelessRPCRequiresOptionToBeEnabled;
	/***/ public String submodulesNotSupported;
	/***/ public String symlinkCannotBeWrittenAsTheLinkTarget;
	/***/ public String systemConfigFileInvalid;
	/***/ public String tagNameInvalid;
	/***/ public String tagOnRepoWithoutHEADCurrentlyNotSupported;
	/***/ public String tSizeMustBeGreaterOrEqual1;
	/***/ public String theFactoryMustNotBeNull;
	/***/ public String timerAlreadyTerminated;
	/***/ public String topologicalSortRequired;
	/***/ public String transportExceptionBadRef;
	/***/ public String transportExceptionEmptyRef;
	/***/ public String transportExceptionInvalid;
	/***/ public String transportExceptionMissingAssumed;
	/***/ public String transportExceptionReadRef;
	/***/ public String transportProtoAmazonS3;
	/***/ public String transportProtoBundleFile;
	/***/ public String transportProtoFTP;
	/***/ public String transportProtoGitAnon;
	/***/ public String transportProtoHTTP;
	/***/ public String transportProtoLocal;
	/***/ public String transportProtoSFTP;
	/***/ public String transportProtoSSH;
	/***/ public String treeEntryAlreadyExists;
	/***/ public String treeIteratorDoesNotSupportRemove;
	/***/ public String truncatedHunkLinesMissingForAncestor;
	/***/ public String truncatedHunkNewLinesMissing;
	/***/ public String truncatedHunkOldLinesMissing;
	/***/ public String unableToCheckConnectivity;
	/***/ public String unableToStore;
	/***/ public String unableToWrite;
	/***/ public String unencodeableFile;
	/***/ public String unexpectedCompareResult;
	/***/ public String unexpectedEndOfConfigFile;
	/***/ public String unexpectedHunkTrailer;
	/***/ public String unexpectedOddResult;
	/***/ public String unexpectedRefReport;
	/***/ public String unexpectedReportLine2;
	/***/ public String unexpectedReportLine;
	/***/ public String unknownDIRCVersion;
	/***/ public String unknownHost;
	/***/ public String unknownIndexVersionOrCorruptIndex;
	/***/ public String unknownObject;
	/***/ public String unknownObjectType;
	/***/ public String unknownRepositoryFormat2;
	/***/ public String unknownRepositoryFormat;
	/***/ public String unknownZlibError;
	/***/ public String unmergedPath;
	/***/ public String unmergedPaths;
	/***/ public String unpackException;
	/***/ public String unreadablePackIndex;
	/***/ public String unrecognizedRef;
	/***/ public String unsupportedCommand0;
	/***/ public String unsupportedEncryptionAlgorithm;
	/***/ public String unsupportedEncryptionVersion;
	/***/ public String unsupportedOperationNotAddAtEnd;
	/***/ public String unsupportedPackIndexVersion;
	/***/ public String unsupportedPackVersion;
	/***/ public String updatingReferences;
	/***/ public String updatingRefFailed;
	/***/ public String uriNotFound;
	/***/ public String userConfigFileInvalid;
	/***/ public String walkFailure;
	/***/ public String wantNotValid;
	/***/ public String weeksAgo;
	/***/ public String windowSizeMustBeLesserThanLimit;
	/***/ public String windowSizeMustBePowerOf2;
	/***/ public String writeTimedOut;
	/***/ public String writerAlreadyInitialized;
	/***/ public String writingNotPermitted;
	/***/ public String writingNotSupported;
	/***/ public String writingObjects;
	/***/ public String wrongDecompressedLength;
	/***/ public String wrongRepositoryState;
	/***/ public String year;
	/***/ public String years;
	/***/ public String yearsAgo;
	/***/ public String yearsMonthsAgo;
}
