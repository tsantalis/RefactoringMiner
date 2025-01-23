/*
 * Copyright (C) 2008, 2020 Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.transport;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static org.eclipse.jgit.lib.Constants.R_TAGS;
import static org.eclipse.jgit.transport.GitProtocolConstants.CAPABILITY_REF_IN_WANT;
import static org.eclipse.jgit.transport.GitProtocolConstants.CAPABILITY_SERVER_OPTION;
import static org.eclipse.jgit.transport.GitProtocolConstants.COMMAND_FETCH;
import static org.eclipse.jgit.transport.GitProtocolConstants.COMMAND_LS_REFS;
import static org.eclipse.jgit.transport.GitProtocolConstants.COMMAND_OBJECT_INFO;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_AGENT;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_ALLOW_REACHABLE_SHA1_IN_WANT;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_ALLOW_TIP_SHA1_IN_WANT;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_DEEPEN_RELATIVE;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_FILTER;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_INCLUDE_TAG;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_MULTI_ACK;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_MULTI_ACK_DETAILED;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_NO_DONE;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_NO_PROGRESS;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_OFS_DELTA;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_SHALLOW;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_SIDEBAND_ALL;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_SIDE_BAND;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_SIDE_BAND_64K;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_THIN_PACK;
import static org.eclipse.jgit.transport.GitProtocolConstants.OPTION_WAIT_FOR_DONE;
import static org.eclipse.jgit.transport.GitProtocolConstants.VERSION_2_REQUEST;
import static org.eclipse.jgit.util.RefMap.toRefMap;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.PackProtocolException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.pack.CachedPackUriProvider;
import org.eclipse.jgit.internal.storage.pack.PackWriter;
import org.eclipse.jgit.internal.transport.parser.FirstWant;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.AsyncRevObjectQueue;
import org.eclipse.jgit.revwalk.DepthWalk;
import org.eclipse.jgit.revwalk.ObjectReachabilityChecker;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.ReachabilityChecker;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevFlagSet;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.storage.pack.PackStatistics;
import org.eclipse.jgit.transport.GitProtocolConstants.MultiAck;
import org.eclipse.jgit.transport.RefAdvertiser.PacketLineOutRefAdvertiser;
import org.eclipse.jgit.transport.TransferConfig.ProtocolVersion;
import org.eclipse.jgit.util.io.InterruptTimer;
import org.eclipse.jgit.util.io.NullOutputStream;
import org.eclipse.jgit.util.io.TimeoutInputStream;
import org.eclipse.jgit.util.io.TimeoutOutputStream;

/**
 * Implements the server side of a fetch connection, transmitting objects.
 */
public class UploadPack implements Closeable {
	/** Policy the server uses to validate client requests */
	public enum RequestPolicy {
		/** Client may only ask for objects the server advertised a reference for. */
		ADVERTISED,

		/**
		 * Client may ask for any commit reachable from a reference advertised by
		 * the server.
		 */
		REACHABLE_COMMIT,

		/**
		 * Client may ask for objects that are the tip of any reference, even if not
		 * advertised.
		 * <p>
		 * This may happen, for example, when a custom {@link RefFilter} is set.
		 *
		 * @since 3.1
		 */
		TIP,

		/**
		 * Client may ask for any commit reachable from any reference, even if that
		 * reference wasn't advertised.
		 *
		 * @since 3.1
		 */
		REACHABLE_COMMIT_TIP,

		/** Client may ask for any SHA-1 in the repository. */
		ANY;
	}

	/**
	 * Validator for client requests.
	 *
	 * @since 3.1
	 */
	public interface RequestValidator {
		/**
		 * Check a list of client wants against the request policy.
		 *
		 * @param up
		 *            {@link UploadPack} instance.
		 * @param wants
		 *            objects the client requested that were not advertised.
		 *
		 * @throws PackProtocolException
		 *            if one or more wants is not valid.
		 * @throws IOException
		 *            if a low-level exception occurred.
		 * @since 3.1
		 */
		void checkWants(UploadPack up, List<ObjectId> wants)
				throws PackProtocolException, IOException;
	}

	/**
	 * Data in the first line of a want-list, the line itself plus options.
	 *
	 * @deprecated Use {@link FirstWant} instead
	 */
	@Deprecated
	public static class FirstLine {

		private final FirstWant firstWant;

		/**
		 * @param line
		 *            line from the client.
		 */
		public FirstLine(String line) {
			try {
				firstWant = FirstWant.fromLine(line);
			} catch (PackProtocolException e) {
				throw new UncheckedIOException(e);
			}
		}

		/** @return non-capabilities part of the line. */
		public String getLine() {
			return firstWant.getLine();
		}

		/** @return capabilities parsed from the line. */
		public Set<String> getOptions() {
			if (firstWant.getAgent() != null) {
				Set<String> caps = new HashSet<>(firstWant.getCapabilities());
				caps.add(OPTION_AGENT + '=' + firstWant.getAgent());
				return caps;
			}
			return firstWant.getCapabilities();
		}
	}

	/*
	 * {@link java.util.function.Consumer} doesn't allow throwing checked
	 * exceptions. Define our own to propagate IOExceptions.
	 */
	@FunctionalInterface
	private static interface IOConsumer<R> {
		void accept(R t) throws IOException;
	}

	/** Database we read the objects from. */
	private final Repository db;

	/** Revision traversal support over {@link #db}. */
	private final RevWalk walk;

	/** Configuration to pass into the PackWriter. */
	private PackConfig packConfig;

	/** Configuration for various transfer options. */
	private TransferConfig transferConfig;

	/** Timeout in seconds to wait for client interaction. */
	private int timeout;

	/**
	 * Is the client connection a bi-directional socket or pipe?
	 * <p>
	 * If true, this class assumes it can perform multiple read and write cycles
	 * with the client over the input and output streams. This matches the
	 * functionality available with a standard TCP/IP connection, or a local
	 * operating system or in-memory pipe.
	 * <p>
	 * If false, this class runs in a read everything then output results mode,
	 * making it suitable for single round-trip systems RPCs such as HTTP.
	 */
	private boolean biDirectionalPipe = true;

	/** Timer to manage {@link #timeout}. */
	private InterruptTimer timer;

	/**
	 * Whether the client requested to use protocol V2 through a side
	 * channel (such as the Git-Protocol HTTP header).
	 */
	private boolean clientRequestedV2;

	private InputStream rawIn;

	private ResponseBufferedOutputStream rawOut;

	private PacketLineIn pckIn;

	private OutputStream msgOut = NullOutputStream.INSTANCE;

	private ErrorWriter errOut = new PackProtocolErrorWriter();

	/**
	 * Refs eligible for advertising to the client, set using
	 * {@link #setAdvertisedRefs}.
	 */
	private Map<String, Ref> refs;

	/** Hook used while processing Git protocol v2 requests. */
	private ProtocolV2Hook protocolV2Hook = ProtocolV2Hook.DEFAULT;

	/** Hook used while advertising the refs to the client. */
	private AdvertiseRefsHook advertiseRefsHook = AdvertiseRefsHook.DEFAULT;

	/** Whether the {@link #advertiseRefsHook} has been invoked. */
	private boolean advertiseRefsHookCalled;

	/** Filter used while advertising the refs to the client. */
	private RefFilter refFilter = RefFilter.DEFAULT;

	/** Hook handling the various upload phases. */
	private PreUploadHook preUploadHook = PreUploadHook.NULL;

	/** Hook for taking post upload actions. */
	private PostUploadHook postUploadHook = PostUploadHook.NULL;

	/** Caller user agent */
	String userAgent;

	/** Raw ObjectIds the client has asked for, before validating them. */
	private Set<ObjectId> wantIds = new HashSet<>();

	/** Objects the client wants to obtain. */
	private final Set<RevObject> wantAll = new HashSet<>();

	/** Objects on both sides, these don't have to be sent. */
	private final Set<RevObject> commonBase = new HashSet<>();

	/** Commit time of the oldest common commit, in seconds. */
	private int oldestTime;

	/** null if {@link #commonBase} should be examined again. */
	private Boolean okToGiveUp;

	private boolean sentReady;

	/** Objects we sent in our advertisement list. */
	private Set<ObjectId> advertised;

	/** Marked on objects the client has asked us to give them. */
	private final RevFlag WANT;

	/** Marked on objects both we and the client have. */
	private final RevFlag PEER_HAS;

	/** Marked on objects in {@link #commonBase}. */
	private final RevFlag COMMON;

	/** Objects where we found a path from the want list to a common base. */
	private final RevFlag SATISFIED;

	private final RevFlagSet SAVE;

	private RequestValidator requestValidator = new AdvertisedRequestValidator();

	private MultiAck multiAck = MultiAck.OFF;

	private boolean noDone;

	private PackStatistics statistics;

	/**
	 * Request this instance is handling.
	 *
	 * We need to keep a reference to it for {@link PreUploadHook pre upload
	 * hooks}. They receive a reference this instance and invoke methods like
	 * getDepth() to get information about the request.
	 */
	private FetchRequest currentRequest;

	private CachedPackUriProvider cachedPackUriProvider;

	/**
	 * Create a new pack upload for an open repository.
	 *
	 * @param copyFrom
	 *            the source repository.
	 */
	public UploadPack(Repository copyFrom) {
		db = copyFrom;
		walk = new RevWalk(db);
		walk.setRetainBody(false);

		WANT = walk.newFlag("WANT"); //$NON-NLS-1$
		PEER_HAS = walk.newFlag("PEER_HAS"); //$NON-NLS-1$
		COMMON = walk.newFlag("COMMON"); //$NON-NLS-1$
		SATISFIED = walk.newFlag("SATISFIED"); //$NON-NLS-1$
		walk.carry(PEER_HAS);

		SAVE = new RevFlagSet();
		SAVE.add(WANT);
		SAVE.add(PEER_HAS);
		SAVE.add(COMMON);
		SAVE.add(SATISFIED);

		setTransferConfig(null);
	}

	/**
	 * Get the repository this upload is reading from.
	 *
	 * @return the repository this upload is reading from.
	 */
	public final Repository getRepository() {
		return db;
	}

	/**
	 * Get the RevWalk instance used by this connection.
	 *
	 * @return the RevWalk instance used by this connection.
	 */
	public final RevWalk getRevWalk() {
		return walk;
	}

	/**
	 * Get refs which were advertised to the client.
	 *
	 * @return all refs which were advertised to the client. Only valid during
	 *         the negotiation phase. Will return {@code null} if
	 *         {@link #setAdvertisedRefs(Map)} has not been called yet or if
	 *         {@code #sendPack()} has been called.
	 */
	public final Map<String, Ref> getAdvertisedRefs() {
		return refs;
	}

	/**
	 * Set the refs advertised by this UploadPack.
	 * <p>
	 * Intended to be called from a
	 * {@link org.eclipse.jgit.transport.PreUploadHook}.
	 *
	 * @param allRefs
	 *            explicit set of references to claim as advertised by this
	 *            UploadPack instance. This overrides any references that may
	 *            exist in the source repository. The map is passed to the
	 *            configured {@link #getRefFilter()}. If null, assumes all refs
	 *            were advertised.
	 */
	public void setAdvertisedRefs(@Nullable Map<String, Ref> allRefs) {
		if (allRefs != null) {
			refs = allRefs;
		} else {
			refs = getAllRefs();
		}
		if (refFilter == RefFilter.DEFAULT) {
			refs = transferConfig.getRefFilter().filter(refs);
		} else {
			refs = refFilter.filter(refs);
		}
	}

	/**
	 * Get timeout (in seconds) before aborting an IO operation.
	 *
	 * @return timeout (in seconds) before aborting an IO operation.
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Set the timeout before willing to abort an IO call.
	 *
	 * @param seconds
	 *            number of seconds to wait (with no data transfer occurring)
	 *            before aborting an IO read or write operation with the
	 *            connected client.
	 */
	public void setTimeout(int seconds) {
		timeout = seconds;
	}

	/**
	 * Whether this class expects a bi-directional pipe opened between the
	 * client and itself.
	 *
	 * @return true if this class expects a bi-directional pipe opened between
	 *         the client and itself. The default is true.
	 */
	public boolean isBiDirectionalPipe() {
		return biDirectionalPipe;
	}

	/**
	 * Set whether this class will assume the socket is a fully bidirectional
	 * pipe between the two peers
	 *
	 * @param twoWay
	 *            if true, this class will assume the socket is a fully
	 *            bidirectional pipe between the two peers and takes advantage
	 *            of that by first transmitting the known refs, then waiting to
	 *            read commands. If false, this class assumes it must read the
	 *            commands before writing output and does not perform the
	 *            initial advertising.
	 */
	public void setBiDirectionalPipe(boolean twoWay) {
		biDirectionalPipe = twoWay;
	}

	/**
	 * Get policy used by the service to validate client requests
	 *
	 * @return policy used by the service to validate client requests, or null
	 *         for a custom request validator.
	 */
	public RequestPolicy getRequestPolicy() {
		if (requestValidator instanceof AdvertisedRequestValidator)
			return RequestPolicy.ADVERTISED;
		if (requestValidator instanceof ReachableCommitRequestValidator)
			return RequestPolicy.REACHABLE_COMMIT;
		if (requestValidator instanceof TipRequestValidator)
			return RequestPolicy.TIP;
		if (requestValidator instanceof ReachableCommitTipRequestValidator)
			return RequestPolicy.REACHABLE_COMMIT_TIP;
		if (requestValidator instanceof AnyRequestValidator)
			return RequestPolicy.ANY;
		return null;
	}

	/**
	 * Set the policy used to enforce validation of a client's want list.
	 *
	 * @param policy
	 *            the policy used to enforce validation of a client's want list.
	 *            By default the policy is
	 *            {@link org.eclipse.jgit.transport.UploadPack.RequestPolicy#ADVERTISED},
	 *            which is the Git default requiring clients to only ask for an
	 *            object that a reference directly points to. This may be
	 *            relaxed to
	 *            {@link org.eclipse.jgit.transport.UploadPack.RequestPolicy#REACHABLE_COMMIT}
	 *            or
	 *            {@link org.eclipse.jgit.transport.UploadPack.RequestPolicy#REACHABLE_COMMIT_TIP}
	 *            when callers have {@link #setBiDirectionalPipe(boolean)} set
	 *            to false. Overrides any policy specified in a
	 *            {@link org.eclipse.jgit.transport.TransferConfig}.
	 */
	public void setRequestPolicy(RequestPolicy policy) {
		switch (policy) {
			case ADVERTISED:
			default:
				requestValidator = new AdvertisedRequestValidator();
				break;
			case REACHABLE_COMMIT:
				requestValidator = new ReachableCommitRequestValidator();
				break;
			case TIP:
				requestValidator = new TipRequestValidator();
				break;
			case REACHABLE_COMMIT_TIP:
				requestValidator = new ReachableCommitTipRequestValidator();
				break;
			case ANY:
				requestValidator = new AnyRequestValidator();
				break;
		}
	}

	/**
	 * Set custom validator for client want list.
	 *
	 * @param validator
	 *            custom validator for client want list.
	 * @since 3.1
	 */
	public void setRequestValidator(@Nullable RequestValidator validator) {
		requestValidator = validator != null ? validator
				: new AdvertisedRequestValidator();
	}

	/**
	 * Get the hook used while advertising the refs to the client.
	 *
	 * @return the hook used while advertising the refs to the client.
	 */
	public AdvertiseRefsHook getAdvertiseRefsHook() {
		return advertiseRefsHook;
	}

	/**
	 * Get the filter used while advertising the refs to the client.
	 *
	 * @return the filter used while advertising the refs to the client.
	 */
	public RefFilter getRefFilter() {
		return refFilter;
	}

	/**
	 * Set the hook used while advertising the refs to the client.
	 * <p>
	 * If the {@link org.eclipse.jgit.transport.AdvertiseRefsHook} chooses to
	 * call {@link #setAdvertisedRefs(Map)}, only refs set by this hook
	 * <em>and</em> selected by the {@link org.eclipse.jgit.transport.RefFilter}
	 * will be shown to the client.
	 *
	 * @param advertiseRefsHook
	 *            the hook; may be null to show all refs.
	 */
	public void setAdvertiseRefsHook(
			@Nullable AdvertiseRefsHook advertiseRefsHook) {
		this.advertiseRefsHook = advertiseRefsHook != null ? advertiseRefsHook
				: AdvertiseRefsHook.DEFAULT;
	}

	/**
	 * Set the protocol V2 hook.
	 *
	 * @param hook
	 *            the hook; if null no special actions are taken.
	 * @since 5.1
	 */
	public void setProtocolV2Hook(@Nullable ProtocolV2Hook hook) {
		this.protocolV2Hook = hook != null ? hook : ProtocolV2Hook.DEFAULT;
	}

	/**
	 * Get the currently installed protocol v2 hook.
	 *
	 * @return the hook or a default implementation if none installed.
	 *
	 * @since 5.5
	 */
	public ProtocolV2Hook getProtocolV2Hook() {
		return this.protocolV2Hook != null ? this.protocolV2Hook
				: ProtocolV2Hook.DEFAULT;
	}

	/**
	 * Set the filter used while advertising the refs to the client.
	 * <p>
	 * Only refs allowed by this filter will be sent to the client. The filter
	 * is run against the refs specified by the
	 * {@link org.eclipse.jgit.transport.AdvertiseRefsHook} (if applicable). If
	 * null or not set, uses the filter implied by the
	 * {@link org.eclipse.jgit.transport.TransferConfig}.
	 *
	 * @param refFilter
	 *            the filter; may be null to show all refs.
	 */
	public void setRefFilter(@Nullable RefFilter refFilter) {
		this.refFilter = refFilter != null ? refFilter : RefFilter.DEFAULT;
	}

	/**
	 * Get the configured pre upload hook.
	 *
	 * @return the configured pre upload hook.
	 */
	public PreUploadHook getPreUploadHook() {
		return preUploadHook;
	}

	/**
	 * Set the hook that controls how this instance will behave.
	 *
	 * @param hook
	 *            the hook; if null no special actions are taken.
	 */
	public void setPreUploadHook(@Nullable PreUploadHook hook) {
		preUploadHook = hook != null ? hook : PreUploadHook.NULL;
	}

	/**
	 * Get the configured post upload hook.
	 *
	 * @return the configured post upload hook.
	 * @since 4.1
	 */
	public PostUploadHook getPostUploadHook() {
		return postUploadHook;
	}

	/**
	 * Set the hook for post upload actions (logging, repacking).
	 *
	 * @param hook
	 *            the hook; if null no special actions are taken.
	 * @since 4.1
	 */
	public void setPostUploadHook(@Nullable PostUploadHook hook) {
		postUploadHook = hook != null ? hook : PostUploadHook.NULL;
	}

	/**
	 * Set the configuration used by the pack generator.
	 *
	 * @param pc
	 *            configuration controlling packing parameters. If null the
	 *            source repository's settings will be used.
	 */
	public void setPackConfig(@Nullable PackConfig pc) {
		this.packConfig = pc;
	}

	/**
	 * Set configuration controlling transfer options.
	 *
	 * @param tc
	 *            configuration controlling transfer options. If null the source
	 *            repository's settings will be used.
	 * @since 3.1
	 */
	public void setTransferConfig(@Nullable TransferConfig tc) {
		this.transferConfig = tc != null ? tc : new TransferConfig(db);
		if (transferConfig.isAllowTipSha1InWant()) {
			setRequestPolicy(transferConfig.isAllowReachableSha1InWant()
				? RequestPolicy.REACHABLE_COMMIT_TIP : RequestPolicy.TIP);
		} else {
			setRequestPolicy(transferConfig.isAllowReachableSha1InWant()
				? RequestPolicy.REACHABLE_COMMIT : RequestPolicy.ADVERTISED);
		}
	}

	/**
	 * Check whether the client expects a side-band stream.
	 *
	 * @return true if the client has advertised a side-band capability, false
	 *     otherwise.
	 * @throws org.eclipse.jgit.transport.RequestNotYetReadException
	 *             if the client's request has not yet been read from the wire, so
	 *             we do not know if they expect side-band. Note that the client
	 *             may have already written the request, it just has not been
	 *             read.
	 */
	public boolean isSideBand() throws RequestNotYetReadException {
		if (currentRequest == null) {
			throw new RequestNotYetReadException();
		}
		Set<String> caps = currentRequest.getClientCapabilities();
		return caps.contains(OPTION_SIDE_BAND)
				|| caps.contains(OPTION_SIDE_BAND_64K);
	}

	/**
	 * Set the Extra Parameters provided by the client.
	 *
	 * <p>These are parameters passed by the client through a side channel
	 * such as the Git-Protocol HTTP header, to allow a client to request
	 * a newer response format while remaining compatible with older servers
	 * that do not understand different request formats.
	 *
	 * @param params
	 *            parameters supplied by the client, split at colons or NUL
	 *            bytes.
	 * @since 5.0
	 */
	public void setExtraParameters(Collection<String> params) {
		this.clientRequestedV2 = params.contains(VERSION_2_REQUEST);
	}

	/**
	 * @param p provider of URIs corresponding to cached packs (to support
	 *     the packfile URIs feature)
	 * @since 5.5
	 */
	public void setCachedPackUriProvider(@Nullable CachedPackUriProvider p) {
		cachedPackUriProvider = p;
	}

	private boolean useProtocolV2() {
		return (transferConfig.protocolVersion == null
			|| ProtocolVersion.V2.equals(transferConfig.protocolVersion))
				&& clientRequestedV2;
	}

	@Override
	public void close() {
		if (timer != null) {
			try {
				timer.terminate();
			} finally {
				timer = null;
			}
		}
	}

	/**
	 * Execute the upload task on the socket.
	 *
	 * <p>
	 * Same as {@link #uploadWithExceptionPropagation} except that the thrown
	 * exceptions are handled in the method, and the error messages are sent to
	 * the clients.
	 *
	 * <p>
	 * Call this method if the caller does not have an error handling mechanism.
	 * Call {@link #uploadWithExceptionPropagation} if the caller wants to have
	 * its own error handling mechanism.
	 *
	 * @param input
	 * @param output
	 * @param messages
	 * @throws java.io.IOException
	 */
	public void upload(InputStream input, OutputStream output,
			@Nullable OutputStream messages) throws IOException {
		try {
			uploadWithExceptionPropagation(input, output, messages);
		} catch (ServiceMayNotContinueException err) {
			if (!err.isOutput() && err.getMessage() != null) {
				try {
					errOut.writeError(err.getMessage());
				} catch (IOException e) {
					err.addSuppressed(e);
					throw err;
				}
				err.setOutput();
			}
			throw err;
		} catch (IOException | RuntimeException | Error err) {
			if (rawOut != null) {
				String msg = err instanceof PackProtocolException
						? err.getMessage()
						: JGitText.get().internalServerError;
				try {
					errOut.writeError(msg);
				} catch (IOException e) {
					err.addSuppressed(e);
					throw err;
				}
				throw new UploadPackInternalServerErrorException(err);
			}
			throw err;
		} finally {
			close();
		}
	}

	/**
	 * Execute the upload task on the socket.
	 *
	 * <p>
	 * If the client passed extra parameters (e.g., "version=2") through a side
	 * channel, the caller must call setExtraParameters first to supply them.
	 * Callers of this method should call {@link #close()} to terminate the
	 * internal interrupt timer thread. If the caller fails to terminate the
	 * thread, it will (eventually) terminate itself when the InterruptTimer
	 * instance is garbage collected.
	 *
	 * @param input
	 *            raw input to read client commands from. Caller must ensure the
	 *            input is buffered, otherwise read performance may suffer.
	 * @param output
	 *            response back to the Git network client, to write the pack
	 *            data onto. Caller must ensure the output is buffered,
	 *            otherwise write performance may suffer.
	 * @param messages
	 *            secondary "notice" channel to send additional messages out
	 *            through. When run over SSH this should be tied back to the
	 *            standard error channel of the command execution. For most
	 *            other network connections this should be null.
	 * @throws ServiceMayNotContinueException
	 *             thrown if one of the hooks throws this.
	 * @throws IOException
	 *             thrown if the server or the client I/O fails, or there's an
	 *             internal server error.
	 * @since 5.6
	 */
	public void uploadWithExceptionPropagation(InputStream input,
			OutputStream output, @Nullable OutputStream messages)
			throws ServiceMayNotContinueException, IOException {
		try {
			rawIn = input;
			if (messages != null) {
				msgOut = messages;
			}

			if (timeout > 0) {
				final Thread caller = Thread.currentThread();
				timer = new InterruptTimer(caller.getName() + "-Timer"); //$NON-NLS-1$
				TimeoutInputStream i = new TimeoutInputStream(rawIn, timer);
				@SuppressWarnings("resource")
				TimeoutOutputStream o = new TimeoutOutputStream(output, timer);
				i.setTimeout(timeout * 1000);
				o.setTimeout(timeout * 1000);
				rawIn = i;
				output = o;
			}

			rawOut = new ResponseBufferedOutputStream(output);
			if (biDirectionalPipe) {
				rawOut.stopBuffering();
			}

			pckIn = new PacketLineIn(rawIn);
			PacketLineOut pckOut = new PacketLineOut(rawOut);
			if (useProtocolV2()) {
				serviceV2(pckOut);
			} else {
				service(pckOut);
			}
		} finally {
			msgOut = NullOutputStream.INSTANCE;
			walk.close();
		}
	}

	/**
	 * Get the PackWriter's statistics if a pack was sent to the client.
	 *
	 * @return statistics about pack output, if a pack was sent. Null if no pack
	 *         was sent, such as during the negotiation phase of a smart HTTP
	 *         connection, or if the client was already up-to-date.
	 * @since 4.1
	 */
	public PackStatistics getStatistics() {
		return statistics;
	}

	/**
	 * Extract the full list of refs from the ref-db.
	 *
	 * @return Map of all refname/ref
	 */
	private Map<String, Ref> getAllRefs() {
		try {
			return db.getRefDatabase().getRefs().stream().collect(
					Collectors.toMap(Ref::getName, Function.identity()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Map<String, Ref> getAdvertisedOrDefaultRefs() throws IOException {
		if (refs != null) {
			return refs;
		}

		if (!advertiseRefsHookCalled) {
			advertiseRefsHook.advertiseRefs(this);
			advertiseRefsHookCalled = true;
		}
		if (refs == null) {
			// Fall back to all refs.
			setAdvertisedRefs(
					db.getRefDatabase().getRefs().stream()
							.collect(toRefMap((a, b) -> b)));
		}
		return refs;
	}

	private Map<String, Ref> getFilteredRefs(Collection<String> refPrefixes)
					throws IOException {
		if (refPrefixes.isEmpty()) {
			return getAdvertisedOrDefaultRefs();
		}
		if (refs == null && !advertiseRefsHookCalled) {
			advertiseRefsHook.advertiseRefs(this);
			advertiseRefsHookCalled = true;
		}
		if (refs == null) {
			// Fast path: the advertised refs hook did not set advertised refs.
			String[] prefixes = refPrefixes.toArray(new String[0]);
			Map<String, Ref> rs =
					db.getRefDatabase().getRefsByPrefix(prefixes).stream()
							.collect(toRefMap((a, b) -> b));
			if (refFilter != RefFilter.DEFAULT) {
				return refFilter.filter(rs);
			}
			return transferConfig.getRefFilter().filter(rs);
		}

		// Slow path: filter the refs provided by the advertised refs hook.
		// refFilter has already been applied to refs.
		return refs.values().stream()
				.filter(ref -> refPrefixes.stream()
						.anyMatch(ref.getName()::startsWith))
				.collect(toRefMap((a, b) -> b));
	}

	/**
	 * Returns the specified references.
	 * <p>
	 * This produces an immutable map containing whatever subset of the
	 * refs named by the caller are present in the supplied {@code refs}
	 * map.
	 *
	 * @param refs
	 *            Map to search for refs to return.
	 * @param names
	 *            which refs to search for in {@code refs}.
	 * @return the requested Refs, omitting any that are null or missing.
	 */
	@NonNull
	private static Map<String, Ref> mapRefs(
			Map<String, Ref> refs, List<String> names) {
		return unmodifiableMap(
				names.stream()
					.map(refs::get)
					.filter(Objects::nonNull)
						.collect(toRefMap((a, b) -> b)));
	}

	/**
	 * Read refs on behalf of the client.
	 * <p>
	 * This checks whether the refs are present in the ref advertisement
	 * since otherwise the client might not be supposed to be able to
	 * read them.
	 *
	 * @param names
	 *            unabbreviated names of references.
	 * @return the requested Refs, omitting any that are not visible or
	 *         do not exist.
	 * @throws java.io.IOException
	 *            on failure to read a ref or check it for visibility.
	 */
	@NonNull
	private Map<String, Ref> exactRefs(List<String> names) throws IOException {
		if (refs != null) {
			return mapRefs(refs, names);
		}
		if (!advertiseRefsHookCalled) {
			advertiseRefsHook.advertiseRefs(this);
			advertiseRefsHookCalled = true;
		}
		if (refs == null &&
				refFilter == RefFilter.DEFAULT &&
				transferConfig.hasDefaultRefFilter()) {
			// Fast path: no ref filtering is needed.
			String[] ns = names.toArray(new String[0]);
			return unmodifiableMap(db.getRefDatabase().exactRef(ns));
		}
		return mapRefs(getAdvertisedOrDefaultRefs(), names);
	}

	/**
	 * Find a ref in the usual search path on behalf of the client.
	 * <p>
	 * This checks that the ref is present in the ref advertisement since
	 * otherwise the client might not be supposed to be able to read it.
	 *
	 * @param name
	 *            short name of the ref to find, e.g. "master" to find
	 *            "refs/heads/master".
	 * @return the requested Ref, or {@code null} if it is not visible or
	 *         does not exist.
	 * @throws java.io.IOException
	 *            on failure to read the ref or check it for visibility.
	 */
	@Nullable
	private Ref findRef(String name) throws IOException {
		if (refs != null) {
			return RefDatabase.findRef(refs, name);
		}
		if (!advertiseRefsHookCalled) {
			advertiseRefsHook.advertiseRefs(this);
			advertiseRefsHookCalled = true;
		}
		if (refs == null &&
				refFilter == RefFilter.DEFAULT &&
				transferConfig.hasDefaultRefFilter()) {
			// Fast path: no ref filtering is needed.
			return db.getRefDatabase().findRef(name);
		}
		return RefDatabase.findRef(getAdvertisedOrDefaultRefs(), name);
	}

	private void service(PacketLineOut pckOut) throws IOException {
		boolean sendPack = false;
		// If it's a non-bidi request, we need to read the entire request before
		// writing a response. Buffer the response until then.
		PackStatistics.Accumulator accumulator = new PackStatistics.Accumulator();
		List<ObjectId> unshallowCommits = new ArrayList<>();
		FetchRequest req;
		try {
			if (biDirectionalPipe)
				sendAdvertisedRefs(new PacketLineOutRefAdvertiser(pckOut));
			else if (requestValidator instanceof AnyRequestValidator)
				advertised = Collections.emptySet();
			else
				advertised = refIdSet(getAdvertisedOrDefaultRefs().values());

			Instant negotiateStart = Instant.now();
			accumulator.advertised = advertised.size();

			ProtocolV0Parser parser = new ProtocolV0Parser(transferConfig);
			req = parser.recvWants(pckIn);
			currentRequest = req;

			wantIds = req.getWantIds();

			if (req.getWantIds().isEmpty()) {
				preUploadHook.onBeginNegotiateRound(this, req.getWantIds(), 0);
				preUploadHook.onEndNegotiateRound(this, req.getWantIds(), 0, 0,
						false);
				return;
			}
			accumulator.wants = req.getWantIds().size();

			if (req.getClientCapabilities().contains(OPTION_MULTI_ACK_DETAILED)) {
				multiAck = MultiAck.DETAILED;
				noDone = req.getClientCapabilities().contains(OPTION_NO_DONE);
			} else if (req.getClientCapabilities().contains(OPTION_MULTI_ACK))
				multiAck = MultiAck.CONTINUE;
			else
				multiAck = MultiAck.OFF;

			if (!req.getClientShallowCommits().isEmpty()) {
				verifyClientShallow(req.getClientShallowCommits());
			}

			if (req.getDepth() != 0 || req.getDeepenSince() != 0) {
				computeShallowsAndUnshallows(req, shallow -> {
					pckOut.writeString("shallow " + shallow.name() + '\n'); //$NON-NLS-1$
				}, unshallow -> {
					pckOut.writeString("unshallow " + unshallow.name() + '\n'); //$NON-NLS-1$
					unshallowCommits.add(unshallow);
				}, Collections.emptyList());
				pckOut.end();
			}

			if (!req.getClientShallowCommits().isEmpty())
				walk.assumeShallow(req.getClientShallowCommits());
			sendPack = negotiate(req, accumulator, pckOut);
			accumulator.timeNegotiating = Duration
					.between(negotiateStart, Instant.now()).toMillis();

			if (sendPack && !biDirectionalPipe) {
				// Ensure the request was fully consumed. Any remaining input must
				// be a protocol error. If we aren't at EOF the implementation is broken.
				int eof = rawIn.read();
				if (0 <= eof) {
					sendPack = false;
					throw new CorruptObjectException(MessageFormat.format(
							JGitText.get().expectedEOFReceived,
							"\\x" + Integer.toHexString(eof))); //$NON-NLS-1$
				}
			}
		} finally {
			if (!sendPack && !biDirectionalPipe) {
				while (0 < rawIn.skip(2048) || 0 <= rawIn.read()) {
					// Discard until EOF.
				}
			}
			rawOut.stopBuffering();
		}

		if (sendPack) {
			sendPack(accumulator, req, refs == null ? null : refs.values(),
					unshallowCommits, Collections.emptyList(), pckOut);
		}
	}

	private void lsRefsV2(PacketLineOut pckOut) throws IOException {
		ProtocolV2Parser parser = new ProtocolV2Parser(transferConfig);
		LsRefsV2Request req = parser.parseLsRefsRequest(pckIn);
		protocolV2Hook.onLsRefs(req);

		rawOut.stopBuffering();
		PacketLineOutRefAdvertiser adv = new PacketLineOutRefAdvertiser(pckOut);
		adv.init(db);
		adv.setUseProtocolV2(true);
		if (req.getPeel()) {
			adv.setDerefTags(true);
		}
		Map<String, Ref> refsToSend = getFilteredRefs(req.getRefPrefixes());
		if (req.getSymrefs()) {
			findSymrefs(adv, refsToSend);
		}

		adv.send(refsToSend.values());
		adv.end();
	}

	// Resolves ref names from the request's want-ref lines to
	// object ids, throwing PackProtocolException if any are missing.
	private Map<String, ObjectId> wantedRefs(FetchV2Request req)
			throws IOException {
		Map<String, ObjectId> result = new TreeMap<>();

		List<String> wanted = req.getWantedRefs();
		Map<String, Ref> resolved = exactRefs(wanted);

		for (String refName : wanted) {
			Ref ref = resolved.get(refName);
			if (ref == null) {
				throw new PackProtocolException(MessageFormat
						.format(JGitText.get().invalidRefName, refName));
			}
			ObjectId oid = ref.getObjectId();
			if (oid == null) {
				throw new PackProtocolException(MessageFormat
						.format(JGitText.get().invalidRefName, refName));
			}
			result.put(refName, oid);
		}
		return result;
	}

	private void fetchV2(PacketLineOut pckOut) throws IOException {
		// Depending on the requestValidator, #processHaveLines may
		// require that advertised be set. Set it only in the required
		// circumstances (to avoid a full ref lookup in the case that
		// we don't need it).
		if (requestValidator instanceof TipRequestValidator ||
				requestValidator instanceof ReachableCommitTipRequestValidator ||
				requestValidator instanceof AnyRequestValidator) {
			advertised = Collections.emptySet();
		} else {
			advertised = refIdSet(getAdvertisedOrDefaultRefs().values());
		}

		PackStatistics.Accumulator accumulator = new PackStatistics.Accumulator();
		Instant negotiateStart = Instant.now();

		ProtocolV2Parser parser = new ProtocolV2Parser(transferConfig);
		FetchV2Request req = parser.parseFetchRequest(pckIn);
		currentRequest = req;
		rawOut.stopBuffering();

		protocolV2Hook.onFetch(req);

		if (req.getSidebandAll()) {
			pckOut.setUsingSideband(true);
		}

		// TODO(ifrade): Refactor to pass around the Request object, instead of
		// copying data back to class fields
		List<ObjectId> deepenNots = new ArrayList<>();
		for (String s : req.getDeepenNotRefs()) {
			Ref ref = findRef(s);
			if (ref == null) {
				throw new PackProtocolException(MessageFormat
						.format(JGitText.get().invalidRefName, s));
			}
			deepenNots.add(ref.getObjectId());
		}

		Map<String, ObjectId> wantedRefs = wantedRefs(req);
		// TODO(ifrade): Avoid mutating the parsed request.
		req.getWantIds().addAll(wantedRefs.values());
		wantIds = req.getWantIds();

		boolean sectionSent = false;
		boolean mayHaveShallow = req.getDepth() != 0
				|| req.getDeepenSince() != 0
				|| !req.getDeepenNotRefs().isEmpty();
		List<ObjectId> shallowCommits = new ArrayList<>();
		List<ObjectId> unshallowCommits = new ArrayList<>();

		if (!req.getClientShallowCommits().isEmpty()) {
			verifyClientShallow(req.getClientShallowCommits());
		}
		if (mayHaveShallow) {
			computeShallowsAndUnshallows(req,
					shallowCommit -> shallowCommits.add(shallowCommit),
					unshallowCommit -> unshallowCommits.add(unshallowCommit),
					deepenNots);
		}
		if (!req.getClientShallowCommits().isEmpty())
			walk.assumeShallow(req.getClientShallowCommits());

		if (req.wasDoneReceived()) {
			processHaveLines(
					req.getPeerHas(), ObjectId.zeroId(),
					new PacketLineOut(NullOutputStream.INSTANCE, false),
					accumulator, req.wasWaitForDoneReceived() ? Option.WAIT_FOR_DONE : Option.NONE);
		} else {
			pckOut.writeString(
					GitProtocolConstants.SECTION_ACKNOWLEDGMENTS + '\n');
			for (ObjectId id : req.getPeerHas()) {
				if (walk.getObjectReader().has(id)) {
					pckOut.writeString("ACK " + id.getName() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			processHaveLines(req.getPeerHas(), ObjectId.zeroId(),
					new PacketLineOut(NullOutputStream.INSTANCE, false),
					accumulator, Option.NONE);
			if (!req.wasWaitForDoneReceived() && okToGiveUp()) {
				pckOut.writeString("ready\n"); //$NON-NLS-1$
			} else if (commonBase.isEmpty()) {
				pckOut.writeString("NAK\n"); //$NON-NLS-1$
			}
			sectionSent = true;
		}

		if (req.wasDoneReceived() || (!req.wasWaitForDoneReceived() && okToGiveUp())) {
			if (mayHaveShallow) {
				if (sectionSent)
					pckOut.writeDelim();
				pckOut.writeString("shallow-info\n"); //$NON-NLS-1$
				for (ObjectId o : shallowCommits) {
					pckOut.writeString("shallow " + o.getName() + '\n'); //$NON-NLS-1$
				}
				for (ObjectId o : unshallowCommits) {
					pckOut.writeString("unshallow " + o.getName() + '\n'); //$NON-NLS-1$
				}
				sectionSent = true;
			}

			if (!wantedRefs.isEmpty()) {
				if (sectionSent) {
					pckOut.writeDelim();
				}
				pckOut.writeString("wanted-refs\n"); //$NON-NLS-1$
				for (Map.Entry<String, ObjectId> entry :
						wantedRefs.entrySet()) {
					pckOut.writeString(entry.getValue().getName() + ' ' +
							entry.getKey() + '\n');
				}
				sectionSent = true;
			}

			if (sectionSent)
				pckOut.writeDelim();
			if (!pckOut.isUsingSideband()) {
				// sendPack will write "packfile\n" for us if sideband-all is used.
				// But sideband-all is not used, so we have to write it ourselves.
				pckOut.writeString(
						GitProtocolConstants.SECTION_PACKFILE + '\n');
			}

			accumulator.timeNegotiating = Duration
					.between(negotiateStart, Instant.now()).toMillis();

			sendPack(accumulator,
					req,
					req.getClientCapabilities().contains(OPTION_INCLUDE_TAG)
						? db.getRefDatabase().getRefsByPrefix(R_TAGS)
						: null,
					unshallowCommits, deepenNots, pckOut);
			// sendPack invokes pckOut.end() for us, so we do not
			// need to invoke it here.
		} else {
			// Invoke pckOut.end() by ourselves.
			pckOut.end();
		}
	}

	private void objectInfo(PacketLineOut pckOut) throws IOException {
		ProtocolV2Parser parser = new ProtocolV2Parser(transferConfig);
		ObjectInfoRequest req = parser.parseObjectInfoRequest(pckIn);

		protocolV2Hook.onObjectInfo(req);

		ObjectReader or = getRepository().newObjectReader();

		// Size is the only attribute currently supported.
		pckOut.writeString("size"); //$NON-NLS-1$

		for (ObjectId oid : req.getObjectIDs()) {
			long size;
			try {
				size = or.getObjectSize(oid, ObjectReader.OBJ_ANY);
			} catch (MissingObjectException e) {
				throw new PackProtocolException(MessageFormat
						.format(JGitText.get().missingObject, oid.name()), e);
			}

			pckOut.writeString(oid.getName() + " " + size); //$NON-NLS-1$
		}

		pckOut.end();
	}

	/*
	 * Returns true if this is the last command and we should tear down the
	 * connection.
	 */
	private boolean serveOneCommandV2(PacketLineOut pckOut) throws IOException {
		String command;
		try {
			command = pckIn.readString();
		} catch (EOFException eof) {
			/* EOF when awaiting command is fine */
			return true;
		}
		if (PacketLineIn.isEnd(command)) {
			// A blank request is valid according
			// to the protocol; do nothing in this
			// case.
			return true;
		}
		if (command.equals("command=" + COMMAND_LS_REFS)) { //$NON-NLS-1$
			lsRefsV2(pckOut);
			return false;
		}
		if (command.equals("command=" + COMMAND_FETCH)) { //$NON-NLS-1$
			fetchV2(pckOut);
			return false;
		}
		if (command.equals("command=" + COMMAND_OBJECT_INFO)) { //$NON-NLS-1$
			objectInfo(pckOut);
			return false;
		}
		throw new PackProtocolException(MessageFormat
				.format(JGitText.get().unknownTransportCommand, command));
	}

	@SuppressWarnings("nls")
	private List<String> getV2CapabilityAdvertisement() {
		ArrayList<String> caps = new ArrayList<>();
		caps.add("version 2");
		caps.add(COMMAND_LS_REFS);
		boolean advertiseRefInWant = transferConfig.isAllowRefInWant()
				&& db.getConfig().getBoolean("uploadpack", null,
						"advertiserefinwant", true);
		caps.add(COMMAND_FETCH + '='
				+ (transferConfig.isAllowFilter() ? OPTION_FILTER + ' ' : "")
				+ (advertiseRefInWant ? CAPABILITY_REF_IN_WANT + ' ' : "")
				+ (transferConfig.isAdvertiseSidebandAll()
						? OPTION_SIDEBAND_ALL + ' '
						: "")
				+ (cachedPackUriProvider != null ? "packfile-uris " : "")
				+ (transferConfig.isAdvertiseWaitForDone()
						? OPTION_WAIT_FOR_DONE + ' '
						: "")
				+ OPTION_SHALLOW);
		caps.add(CAPABILITY_SERVER_OPTION);
		return caps;
	}

	private void serviceV2(PacketLineOut pckOut) throws IOException {
		if (biDirectionalPipe) {
			// Just like in service(), the capability advertisement
			// is sent only if this is a bidirectional pipe. (If
			// not, the client is expected to call
			// sendAdvertisedRefs() on its own.)
			protocolV2Hook
					.onCapabilities(CapabilitiesV2Request.builder().build());
			for (String s : getV2CapabilityAdvertisement()) {
				pckOut.writeString(s + "\n"); //$NON-NLS-1$
			}
			pckOut.end();

			while (!serveOneCommandV2(pckOut)) {
				// Repeat until an empty command or EOF.
			}
			return;
		}

		try {
			serveOneCommandV2(pckOut);
		} finally {
			while (0 < rawIn.skip(2048) || 0 <= rawIn.read()) {
				// Discard until EOF.
			}
			rawOut.stopBuffering();
		}
	}

	private static Set<ObjectId> refIdSet(Collection<Ref> refs) {
		Set<ObjectId> ids = new HashSet<>(refs.size());
		for (Ref ref : refs) {
			ObjectId id = ref.getObjectId();
			if (id != null) {
				ids.add(id);
			}
			id = ref.getPeeledObjectId();
			if (id != null) {
				ids.add(id);
			}
		}
		return ids;
	}

	/*
	 * Determines what object ids must be marked as shallow or unshallow for the
	 * client.
	 */
	private void computeShallowsAndUnshallows(FetchRequest req,
			IOConsumer<ObjectId> shallowFunc,
			IOConsumer<ObjectId> unshallowFunc,
			List<ObjectId> deepenNots)
			throws IOException {
		if (req.getClientCapabilities().contains(OPTION_DEEPEN_RELATIVE)) {
			// TODO(jonathantanmy): Implement deepen-relative
			throw new UnsupportedOperationException();
		}

		int walkDepth = req.getDepth() == 0 ? Integer.MAX_VALUE
				: req.getDepth() - 1;
		try (DepthWalk.RevWalk depthWalk = new DepthWalk.RevWalk(
				walk.getObjectReader(), walkDepth)) {

			depthWalk.setDeepenSince(req.getDeepenSince());

			// Find all the commits which will be shallow
			for (ObjectId o : req.getWantIds()) {
				try {
					depthWalk.markRoot(depthWalk.parseCommit(o));
				} catch (IncorrectObjectTypeException notCommit) {
					// Ignore non-commits in this loop.
				}
			}

			depthWalk.setDeepenNots(deepenNots);

			RevCommit o;
			boolean atLeastOne = false;
			while ((o = depthWalk.next()) != null) {
				DepthWalk.Commit c = (DepthWalk.Commit) o;
				atLeastOne = true;

				boolean isBoundary = (c.getDepth() == walkDepth) || c.isBoundary();

				// Commits at the boundary which aren't already shallow in
				// the client need to be marked as such
				if (isBoundary && !req.getClientShallowCommits().contains(c)) {
					shallowFunc.accept(c.copy());
				}

				// Commits not on the boundary which are shallow in the client
				// need to become unshallowed
				if (!isBoundary && req.getClientShallowCommits().remove(c)) {
					unshallowFunc.accept(c.copy());
				}
			}
			if (!atLeastOne) {
				throw new PackProtocolException(
					JGitText.get().noCommitsSelectedForShallow);
			}
		}
	}

	/*
	 * Verify all shallow lines refer to commits
	 *
	 * It can mutate the input set (removing missing object ids from it)
	 */
	private void verifyClientShallow(Set<ObjectId> shallowCommits)
			throws IOException, PackProtocolException {
		AsyncRevObjectQueue q = walk.parseAny(shallowCommits, true);
		try {
			for (;;) {
				try {
					// Shallow objects named by the client must be commits.
					RevObject o = q.next();
					if (o == null) {
						break;
					}
					if (!(o instanceof RevCommit)) {
						throw new PackProtocolException(
							MessageFormat.format(
								JGitText.get().invalidShallowObject,
								o.name()));
					}
				} catch (MissingObjectException notCommit) {
					// shallow objects not known at the server are ignored
					// by git-core upload-pack, match that behavior.
					shallowCommits.remove(notCommit.getObjectId());
					continue;
				}
			}
		} finally {
			q.release();
		}
	}

	/**
	 * Generate an advertisement of available refs and capabilities.
	 *
	 * @param adv
	 *            the advertisement formatter.
	 * @throws java.io.IOException
	 *            the formatter failed to write an advertisement.
	 * @throws org.eclipse.jgit.transport.ServiceMayNotContinueException
	 *            the hook denied advertisement.
	 */
	public void sendAdvertisedRefs(RefAdvertiser adv) throws IOException,
			ServiceMayNotContinueException {
		sendAdvertisedRefs(adv, null);
	}

	/**
	 * Generate an advertisement of available refs and capabilities.
	 *
	 * @param adv
	 *            the advertisement formatter.
	 * @param serviceName
	 *            if not null, also output "# service=serviceName" followed by a
	 *            flush packet before the advertisement. This is required
	 *            in v0 of the HTTP protocol, described in Git's
	 *            Documentation/technical/http-protocol.txt.
	 * @throws java.io.IOException
	 *            the formatter failed to write an advertisement.
	 * @throws org.eclipse.jgit.transport.ServiceMayNotContinueException
	 *            the hook denied advertisement.
	 * @since 5.0
	 */
	public void sendAdvertisedRefs(RefAdvertiser adv,
			@Nullable String serviceName) throws IOException,
			ServiceMayNotContinueException {
		if (useProtocolV2()) {
			// The equivalent in v2 is only the capabilities
			// advertisement.
			protocolV2Hook
					.onCapabilities(CapabilitiesV2Request.builder().build());
			for (String s : getV2CapabilityAdvertisement()) {
				adv.writeOne(s);
			}
			adv.end();
			return;
		}

		Map<String, Ref> advertisedOrDefaultRefs = getAdvertisedOrDefaultRefs();

		if (serviceName != null) {
			adv.writeOne("# service=" + serviceName + '\n'); //$NON-NLS-1$
			adv.end();
		}
		adv.init(db);
		adv.advertiseCapability(OPTION_INCLUDE_TAG);
		adv.advertiseCapability(OPTION_MULTI_ACK_DETAILED);
		adv.advertiseCapability(OPTION_MULTI_ACK);
		adv.advertiseCapability(OPTION_OFS_DELTA);
		adv.advertiseCapability(OPTION_SIDE_BAND);
		adv.advertiseCapability(OPTION_SIDE_BAND_64K);
		adv.advertiseCapability(OPTION_THIN_PACK);
		adv.advertiseCapability(OPTION_NO_PROGRESS);
		adv.advertiseCapability(OPTION_SHALLOW);
		if (!biDirectionalPipe)
			adv.advertiseCapability(OPTION_NO_DONE);
		RequestPolicy policy = getRequestPolicy();
		if (policy == RequestPolicy.TIP
				|| policy == RequestPolicy.REACHABLE_COMMIT_TIP
				|| policy == null)
			adv.advertiseCapability(OPTION_ALLOW_TIP_SHA1_IN_WANT);
		if (policy == RequestPolicy.REACHABLE_COMMIT
				|| policy == RequestPolicy.REACHABLE_COMMIT_TIP
				|| policy == null)
			adv.advertiseCapability(OPTION_ALLOW_REACHABLE_SHA1_IN_WANT);
		adv.advertiseCapability(OPTION_AGENT, UserAgent.get());
		if (transferConfig.isAllowFilter()) {
			adv.advertiseCapability(OPTION_FILTER);
		}
		adv.setDerefTags(true);
		findSymrefs(adv, advertisedOrDefaultRefs);
		advertised = adv.send(advertisedOrDefaultRefs.values());

		if (adv.isEmpty())
			adv.advertiseId(ObjectId.zeroId(), "capabilities^{}"); //$NON-NLS-1$
		adv.end();
	}

	/**
	 * Send a message to the client, if it supports receiving them.
	 * <p>
	 * If the client doesn't support receiving messages, the message will be
	 * discarded, with no other indication to the caller or to the client.
	 *
	 * @param what
	 *            string describing the problem identified by the hook. The
	 *            string must not end with an LF, and must not contain an LF.
	 * @since 3.1
	 */
	public void sendMessage(String what) {
		try {
			msgOut.write(Constants.encode(what + "\n")); //$NON-NLS-1$
		} catch (IOException e) {
			// Ignore write failures.
		}
	}

	/**
	 * Get an underlying stream for sending messages to the client
	 *
	 * @return an underlying stream for sending messages to the client, or null.
	 * @since 3.1
	 */
	public OutputStream getMessageOutputStream() {
		return msgOut;
	}

	/**
	 * Returns the clone/fetch depth. Valid only after calling recvWants(). A
	 * depth of 1 means return only the wants.
	 *
	 * @return the depth requested by the client, or 0 if unbounded.
	 * @since 4.0
	 */
	public int getDepth() {
		if (currentRequest == null)
			throw new RequestNotYetReadException();
		return currentRequest.getDepth();
	}

	/**
	 * Deprecated synonym for {@code getFilterSpec().getBlobLimit()}.
	 *
	 * @return filter blob limit requested by the client, or -1 if no limit
	 * @since 5.3
	 * @deprecated Use {@link #getFilterSpec()} instead
	 */
	@Deprecated
	public final long getFilterBlobLimit() {
		return getFilterSpec().getBlobLimit();
	}

	/**
	 * Returns the filter spec for the current request. Valid only after
	 * calling recvWants(). This may be a no-op filter spec, but it won't be
	 * null.
	 *
	 * @return filter requested by the client
	 * @since 5.4
	 */
	public final FilterSpec getFilterSpec() {
		if (currentRequest == null) {
			throw new RequestNotYetReadException();
		}
		return currentRequest.getFilterSpec();
	}

	/**
	 * Get the user agent of the client.
	 * <p>
	 * If the client is new enough to use {@code agent=} capability that value
	 * will be returned. Older HTTP clients may also supply their version using
	 * the HTTP {@code User-Agent} header. The capability overrides the HTTP
	 * header if both are available.
	 * <p>
	 * When an HTTP request has been received this method returns the HTTP
	 * {@code User-Agent} header value until capabilities have been parsed.
	 *
	 * @return user agent supplied by the client. Available only if the client
	 *         is new enough to advertise its user agent.
	 * @since 4.0
	 */
	public String getPeerUserAgent() {
		if (currentRequest != null && currentRequest.getAgent() != null) {
			return currentRequest.getAgent();
		}

		return userAgent;
	}

	private boolean negotiate(FetchRequest req,
			PackStatistics.Accumulator accumulator,
			PacketLineOut pckOut)
			throws IOException {
		okToGiveUp = Boolean.FALSE;

		ObjectId last = ObjectId.zeroId();
		List<ObjectId> peerHas = new ArrayList<>(64);
		for (;;) {
			String line;
			try {
				line = pckIn.readString();
			} catch (EOFException eof) {
				// EOF on stateless RPC (aka smart HTTP) and non-shallow request
				// means the client asked for the updated shallow/unshallow data,
				// disconnected, and will try another request with actual want/have.
				// Don't report the EOF here, its a bug in the protocol that the client
				// just disconnects without sending an END.
				if (!biDirectionalPipe && req.getDepth() > 0)
					return false;
				throw eof;
			}

			if (PacketLineIn.isEnd(line)) {
				last = processHaveLines(peerHas, last, pckOut, accumulator, Option.NONE);
				if (commonBase.isEmpty() || multiAck != MultiAck.OFF)
					pckOut.writeString("NAK\n"); //$NON-NLS-1$
				if (noDone && sentReady) {
					pckOut.writeString("ACK " + last.name() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
					return true;
				}
				if (!biDirectionalPipe)
					return false;
				pckOut.flush();

			} else if (line.startsWith("have ") && line.length() == 45) { //$NON-NLS-1$
				peerHas.add(ObjectId.fromString(line.substring(5)));
				accumulator.haves++;
			} else if (line.equals("done")) { //$NON-NLS-1$
				last = processHaveLines(peerHas, last, pckOut, accumulator, Option.NONE);

				if (commonBase.isEmpty())
					pckOut.writeString("NAK\n"); //$NON-NLS-1$

				else if (multiAck != MultiAck.OFF)
					pckOut.writeString("ACK " + last.name() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$

				return true;

			} else {
				throw new PackProtocolException(MessageFormat.format(JGitText.get().expectedGot, "have", line)); //$NON-NLS-1$
			}
		}
	}

	private enum Option {
		WAIT_FOR_DONE,
		NONE;
	}

	private ObjectId processHaveLines(List<ObjectId> peerHas, ObjectId last,
			PacketLineOut out, PackStatistics.Accumulator accumulator,
			Option option)
			throws IOException {
		preUploadHook.onBeginNegotiateRound(this, wantIds, peerHas.size());
		if (wantAll.isEmpty() && !wantIds.isEmpty())
			parseWants(accumulator);
		if (peerHas.isEmpty())
			return last;

		sentReady = false;
		int haveCnt = 0;
		walk.getObjectReader().setAvoidUnreachableObjects(true);
		AsyncRevObjectQueue q = walk.parseAny(peerHas, false);
		try {
			for (;;) {
				RevObject obj;
				try {
					obj = q.next();
				} catch (MissingObjectException notFound) {
					continue;
				}
				if (obj == null)
					break;

				last = obj;
				haveCnt++;

				if (obj instanceof RevCommit) {
					RevCommit c = (RevCommit) obj;
					if (oldestTime == 0 || c.getCommitTime() < oldestTime)
						oldestTime = c.getCommitTime();
				}

				if (obj.has(PEER_HAS))
					continue;

				obj.add(PEER_HAS);
				if (obj instanceof RevCommit)
					((RevCommit) obj).carry(PEER_HAS);
				addCommonBase(obj);

				// If both sides have the same object; let the client know.
				//
				switch (multiAck) {
				case OFF:
					if (commonBase.size() == 1)
						out.writeString("ACK " + obj.name() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case CONTINUE:
					out.writeString("ACK " + obj.name() + " continue\n"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case DETAILED:
					out.writeString("ACK " + obj.name() + " common\n"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				}
			}
		} finally {
			q.release();
			walk.getObjectReader().setAvoidUnreachableObjects(false);
		}

		int missCnt = peerHas.size() - haveCnt;

		// If we don't have one of the objects but we're also willing to
		// create a pack at this point, let the client know so it stops
		// telling us about its history.
		//
		if (option != Option.WAIT_FOR_DONE) {
			sentReady = shouldGiveUp(peerHas, out, missCnt);
		}

		preUploadHook.onEndNegotiateRound(this, wantAll, haveCnt, missCnt, sentReady);
		peerHas.clear();
		return last;
	}

	private boolean shouldGiveUp(List<ObjectId> peerHas, PacketLineOut out, int missCnt)
			throws IOException {
		boolean readySent = false;
		boolean didOkToGiveUp = false;
		if (0 < missCnt) {
			for (int i = peerHas.size() - 1; i >= 0; i--) {
				ObjectId id = peerHas.get(i);
				if (walk.lookupOrNull(id) == null) {
					didOkToGiveUp = true;
					if (okToGiveUp()) {
						switch (multiAck) {
						case OFF:
							break;
						case CONTINUE:
							out.writeString(
									"ACK " + id.name() + " continue\n"); //$NON-NLS-1$ //$NON-NLS-2$
							break;
						case DETAILED:
							out.writeString(
									"ACK " + id.name() + " ready\n"); //$NON-NLS-1$ //$NON-NLS-2$
							readySent = true;
							break;
						}
					}
					break;
				}
			}
		}

		if (multiAck == MultiAck.DETAILED && !didOkToGiveUp
				&& okToGiveUp()) {
			ObjectId id = peerHas.get(peerHas.size() - 1);
			out.writeString("ACK " + id.name() + " ready\n"); //$NON-NLS-1$ //$NON-NLS-2$
			readySent = true;
		}

		return readySent;
	}

	private void parseWants(PackStatistics.Accumulator accumulator) throws IOException {
		List<ObjectId> notAdvertisedWants = null;
		for (ObjectId obj : wantIds) {
			if (!advertised.contains(obj)) {
				if (notAdvertisedWants == null)
					notAdvertisedWants = new ArrayList<>();
				notAdvertisedWants.add(obj);
			}
		}
		if (notAdvertisedWants != null) {
			accumulator.notAdvertisedWants = notAdvertisedWants.size();

			Instant startReachabilityChecking = Instant.now();

			requestValidator.checkWants(this, notAdvertisedWants);

			accumulator.reachabilityCheckDuration = Duration
					.between(startReachabilityChecking, Instant.now())
					.toMillis();
		}

		AsyncRevObjectQueue q = walk.parseAny(wantIds, true);
		try {
			RevObject obj;
			while ((obj = q.next()) != null) {
				want(obj);

				if (!(obj instanceof RevCommit))
					obj.add(SATISFIED);
				if (obj instanceof RevTag) {
					obj = walk.peel(obj);
					if (obj instanceof RevCommit)
						want(obj);
				}
			}
			wantIds.clear();
		} catch (MissingObjectException notFound) {
			throw new WantNotValidException(notFound.getObjectId(), notFound);
		} finally {
			q.release();
		}
	}

	private void want(RevObject obj) {
		if (!obj.has(WANT)) {
			obj.add(WANT);
			wantAll.add(obj);
		}
	}

	/**
	 * Validator corresponding to {@link RequestPolicy#ADVERTISED}.
	 *
	 * @since 3.1
	 */
	public static final class AdvertisedRequestValidator
			implements RequestValidator {
		@Override
		public void checkWants(UploadPack up, List<ObjectId> wants)
				throws PackProtocolException, IOException {
			if (!up.isBiDirectionalPipe())
				new ReachableCommitRequestValidator().checkWants(up, wants);
			else if (!wants.isEmpty())
				throw new WantNotValidException(wants.iterator().next());
		}
	}

	/**
	 * Validator corresponding to {@link RequestPolicy#REACHABLE_COMMIT}.
	 *
	 * @since 3.1
	 */
	public static final class ReachableCommitRequestValidator
			implements RequestValidator {
		@Override
		public void checkWants(UploadPack up, List<ObjectId> wants)
				throws PackProtocolException, IOException {
			checkNotAdvertisedWants(up, wants, up.getAdvertisedRefs().values());
		}
	}

	/**
	 * Validator corresponding to {@link RequestPolicy#TIP}.
	 *
	 * @since 3.1
	 */
	public static final class TipRequestValidator implements RequestValidator {
		@Override
		public void checkWants(UploadPack up, List<ObjectId> wants)
				throws PackProtocolException, IOException {
			if (!up.isBiDirectionalPipe())
				new ReachableCommitTipRequestValidator().checkWants(up, wants);
			else if (!wants.isEmpty()) {
				Set<ObjectId> refIds =
						refIdSet(up.getRepository().getRefDatabase().getRefs());
				for (ObjectId obj : wants) {
					if (!refIds.contains(obj))
						throw new WantNotValidException(obj);
				}
			}
		}
	}

	/**
	 * Validator corresponding to {@link RequestPolicy#REACHABLE_COMMIT_TIP}.
	 *
	 * @since 3.1
	 */
	public static final class ReachableCommitTipRequestValidator
			implements RequestValidator {
		@Override
		public void checkWants(UploadPack up, List<ObjectId> wants)
				throws PackProtocolException, IOException {
			checkNotAdvertisedWants(up, wants,
					up.getRepository().getRefDatabase().getRefs());
		}
	}

	/**
	 * Validator corresponding to {@link RequestPolicy#ANY}.
	 *
	 * @since 3.1
	 */
	public static final class AnyRequestValidator implements RequestValidator {
		@Override
		public void checkWants(UploadPack up, List<ObjectId> wants)
				throws PackProtocolException, IOException {
			// All requests are valid.
		}
	}

	private static void checkNotAdvertisedWants(UploadPack up,
			List<ObjectId> notAdvertisedWants, Collection<Ref> visibleRefs)
			throws IOException {

		ObjectReader reader = up.getRevWalk().getObjectReader();
		Set<ObjectId> directlyVisibleObjects = refIdSet(visibleRefs);
		List<ObjectId> nonTipWants = notAdvertisedWants.stream()
				.filter(not(directlyVisibleObjects::contains))
				.collect(Collectors.toList());

		try (RevWalk walk = new RevWalk(reader)) {
			walk.setRetainBody(false);
			// Missing "wants" throw exception here
			List<RevObject> wantsAsObjs = objectIdsToRevObjects(walk,
					nonTipWants);
			List<RevCommit> wantsAsCommits = wantsAsObjs.stream()
					.filter(obj -> obj instanceof RevCommit)
					.map(obj -> (RevCommit) obj)
					.collect(Collectors.toList());
			boolean allWantsAreCommits = wantsAsObjs.size() == wantsAsCommits
					.size();
			boolean repoHasBitmaps = reader.getBitmapIndex() != null;

			if (!allWantsAreCommits) {
				if (!repoHasBitmaps && !up.transferConfig.isAllowFilter()) {
					// Checking unadvertised non-commits without bitmaps
					// requires an expensive manual walk. Use allowFilter as an
					// indication that the server operator is willing to pay
					// this cost. Reject the request otherwise.
					RevObject nonCommit = wantsAsObjs
							.stream()
							.filter(obj -> !(obj instanceof RevCommit))
							.limit(1)
							.collect(Collectors.toList()).get(0);
					throw new WantNotValidException(nonCommit);
				}

				try (ObjectWalk objWalk = walk.toObjectWalkWithSameObjects()) {
					Stream<RevObject> startersAsObjs = importantRefsFirst(visibleRefs)
							.map(UploadPack::refToObjectId)
							.map(objId -> objectIdToRevObject(objWalk, objId))
							.filter(Objects::nonNull); // Ignore missing tips

					ObjectReachabilityChecker reachabilityChecker = reader
							.createObjectReachabilityChecker(objWalk);
					Optional<RevObject> unreachable = reachabilityChecker
							.areAllReachable(wantsAsObjs, startersAsObjs);
					if (unreachable.isPresent()) {
						throw new WantNotValidException(unreachable.get());
					}
				}
				return;
			}

			// All wants are commits, we can use ReachabilityChecker
			ReachabilityChecker reachabilityChecker = reader
					.createReachabilityChecker(walk);

			Stream<RevCommit> reachableCommits = importantRefsFirst(visibleRefs)
					.map(UploadPack::refToObjectId)
					.map(objId -> objectIdToRevCommit(walk, objId))
					.filter(Objects::nonNull); // Ignore missing tips

			Optional<RevCommit> unreachable = reachabilityChecker
					.areAllReachable(wantsAsCommits, reachableCommits);
			if (unreachable.isPresent()) {
				throw new WantNotValidException(unreachable.get());
			}

		} catch (MissingObjectException notFound) {
			throw new WantNotValidException(notFound.getObjectId(), notFound);
		}
	}

	private static <T> Predicate<T> not(Predicate<T> t) {
	    return t.negate();
	}

	static Stream<Ref> importantRefsFirst(
			Collection<Ref> visibleRefs) {
		Predicate<Ref> startsWithRefsHeads = ref -> ref.getName()
				.startsWith(Constants.R_HEADS);
		Predicate<Ref> startsWithRefsTags = ref -> ref.getName()
				.startsWith(Constants.R_TAGS);
		Predicate<Ref> allOther = ref -> !startsWithRefsHeads.test(ref)
				&& !startsWithRefsTags.test(ref);

		return Stream.concat(
				visibleRefs.stream().filter(startsWithRefsHeads),
				Stream.concat(
						visibleRefs.stream().filter(startsWithRefsTags),
						visibleRefs.stream().filter(allOther)));
	}

	private static ObjectId refToObjectId(Ref ref) {
		return ref.getObjectId() != null ? ref.getObjectId()
				: ref.getPeeledObjectId();
	}

	/**
	 * Translate an object id to a RevCommit.
	 *
	 * @param walk
	 *            walk on the relevant object storae
	 * @param objectId
	 *            Object Id
	 * @return RevCommit instance or null if the object is missing
	 */
	@Nullable
	private static RevCommit objectIdToRevCommit(RevWalk walk,
			ObjectId objectId) {
		if (objectId == null) {
			return null;
		}

		try {
			return walk.parseCommit(objectId);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Translate an object id to a RevObject.
	 *
	 * @param walk
	 *            walk on the relevant object storage
	 * @param objectId
	 *            Object Id
	 * @return RevObject instance or null if the object is missing
	 */
	@Nullable
	private static RevObject objectIdToRevObject(RevWalk walk,
			ObjectId objectId) {
		if (objectId == null) {
			return null;
		}

		try {
			return walk.parseAny(objectId);
		} catch (IOException e) {
			return null;
		}
	}

	// Resolve the ObjectIds into RevObjects. Any missing object raises an
	// exception
	private static List<RevObject> objectIdsToRevObjects(RevWalk walk,
			Iterable<ObjectId> objectIds)
			throws MissingObjectException, IOException {
		List<RevObject> result = new ArrayList<>();
		for (ObjectId objectId : objectIds) {
			result.add(walk.parseAny(objectId));
		}
		return result;
	}

	private void addCommonBase(RevObject o) {
		if (!o.has(COMMON)) {
			o.add(COMMON);
			commonBase.add(o);
			okToGiveUp = null;
		}
	}

	private boolean okToGiveUp() throws PackProtocolException {
		if (okToGiveUp == null)
			okToGiveUp = Boolean.valueOf(okToGiveUpImp());
		return okToGiveUp.booleanValue();
	}

	private boolean okToGiveUpImp() throws PackProtocolException {
		if (commonBase.isEmpty())
			return false;

		try {
			for (RevObject obj : wantAll) {
				if (!wantSatisfied(obj))
					return false;
			}
			return true;
		} catch (IOException e) {
			throw new PackProtocolException(JGitText.get().internalRevisionError, e);
		}
	}

	private boolean wantSatisfied(RevObject want) throws IOException {
		if (want.has(SATISFIED))
			return true;

		if (((RevCommit) want).getParentCount() == 0) {
			want.add(SATISFIED);
			return true;
		}

		walk.resetRetain(SAVE);
		walk.markStart((RevCommit) want);
		if (oldestTime != 0)
			walk.setRevFilter(CommitTimeRevFilter.after(oldestTime * 1000L));
		for (;;) {
			final RevCommit c = walk.next();
			if (c == null)
				break;
			if (c.has(PEER_HAS)) {
				addCommonBase(c);
				want.add(SATISFIED);
				return true;
			}
		}
		return false;
	}

	/**
	 * Send the requested objects to the client.
	 *
	 * @param accumulator
	 *            where to write statistics about the content of the pack.
	 * @param req
	 *            request in process
	 * @param allTags
	 *            refs to search for annotated tags to include in the pack if
	 *            the {@link #OPTION_INCLUDE_TAG} capability was requested.
	 * @param unshallowCommits
	 *            shallow commits on the client that are now becoming unshallow
	 * @param deepenNots
	 *            objects that the client specified using --shallow-exclude
	 * @param pckOut
	 *            output writer
	 * @throws IOException
	 *             if an error occurred while generating or writing the pack.
	 */
	private void sendPack(PackStatistics.Accumulator accumulator,
			FetchRequest req,
			@Nullable Collection<Ref> allTags,
			List<ObjectId> unshallowCommits,
			List<ObjectId> deepenNots,
			PacketLineOut pckOut) throws IOException {
		Set<String> caps = req.getClientCapabilities();
		boolean sideband = caps.contains(OPTION_SIDE_BAND)
				|| caps.contains(OPTION_SIDE_BAND_64K);

		if (sideband) {
			errOut = new SideBandErrorWriter();

			int bufsz = SideBandOutputStream.SMALL_BUF;
			if (req.getClientCapabilities().contains(OPTION_SIDE_BAND_64K)) {
				bufsz = SideBandOutputStream.MAX_BUF;
			}
			OutputStream packOut = new SideBandOutputStream(
					SideBandOutputStream.CH_DATA, bufsz, rawOut);

			ProgressMonitor pm = NullProgressMonitor.INSTANCE;
			if (!req.getClientCapabilities().contains(OPTION_NO_PROGRESS)) {
				msgOut = new SideBandOutputStream(
						SideBandOutputStream.CH_PROGRESS, bufsz, rawOut);
				pm = new SideBandProgressMonitor(msgOut);
			}

			sendPack(pm, pckOut, packOut, req, accumulator, allTags,
					unshallowCommits, deepenNots);
			pckOut.end();
		} else {
			sendPack(NullProgressMonitor.INSTANCE, pckOut, rawOut, req,
					accumulator, allTags, unshallowCommits, deepenNots);
		}
	}

	/**
	 * Send the requested objects to the client.
	 *
	 * @param pm
	 *            progress monitor
	 * @param pckOut
	 *            PacketLineOut that shares the output with packOut
	 * @param packOut
	 *            packfile output
	 * @param req
	 *            request being processed
	 * @param accumulator
	 *            where to write statistics about the content of the pack.
	 * @param allTags
	 *            refs to search for annotated tags to include in the pack if
	 *            the {@link #OPTION_INCLUDE_TAG} capability was requested.
	 * @param unshallowCommits
	 *            shallow commits on the client that are now becoming unshallow
	 * @param deepenNots
	 *            objects that the client specified using --shallow-exclude
	 * @throws IOException
	 *             if an error occurred while generating or writing the pack.
	 */
	private void sendPack(ProgressMonitor pm, PacketLineOut pckOut,
			OutputStream packOut, FetchRequest req,
			PackStatistics.Accumulator accumulator,
			@Nullable Collection<Ref> allTags, List<ObjectId> unshallowCommits,
			List<ObjectId> deepenNots) throws IOException {
		if (wantAll.isEmpty()) {
			preUploadHook.onSendPack(this, wantIds, commonBase);
		} else {
			preUploadHook.onSendPack(this, wantAll, commonBase);
		}
		msgOut.flush();

		// Advertised objects and refs are not used from here on and can be
		// cleared.
		advertised = null;
		refs = null;

		PackConfig cfg = packConfig;
		if (cfg == null)
			cfg = new PackConfig(db);
		@SuppressWarnings("resource") // PackWriter is referenced in the finally
										// block, and is closed there
		final PackWriter pw = new PackWriter(cfg, walk.getObjectReader(),
				accumulator);
		try {
			pw.setIndexDisabled(true);
			if (req.getFilterSpec().isNoOp()) {
				pw.setUseCachedPacks(true);
			} else {
				pw.setFilterSpec(req.getFilterSpec());
				pw.setUseCachedPacks(false);
			}
			pw.setUseBitmaps(
					req.getDepth() == 0
							&& req.getClientShallowCommits().isEmpty()
							&& req.getFilterSpec().getTreeDepthLimit() == -1);
			pw.setClientShallowCommits(req.getClientShallowCommits());
			pw.setReuseDeltaCommits(true);
			pw.setDeltaBaseAsOffset(
					req.getClientCapabilities().contains(OPTION_OFS_DELTA));
			pw.setThin(req.getClientCapabilities().contains(OPTION_THIN_PACK));
			pw.setReuseValidatingObjects(false);

			// Objects named directly by references go at the beginning
			// of the pack.
			if (commonBase.isEmpty() && refs != null) {
				Set<ObjectId> tagTargets = new HashSet<>();
				for (Ref ref : refs.values()) {
					if (ref.getPeeledObjectId() != null)
						tagTargets.add(ref.getPeeledObjectId());
					else if (ref.getObjectId() == null)
						continue;
					else if (ref.getName().startsWith(Constants.R_HEADS))
						tagTargets.add(ref.getObjectId());
				}
				pw.setTagTargets(tagTargets);
			}

			RevWalk rw = walk;
			if (req.getDepth() > 0 || req.getDeepenSince() != 0 || !deepenNots.isEmpty()) {
				int walkDepth = req.getDepth() == 0 ? Integer.MAX_VALUE
						: req.getDepth() - 1;
				pw.setShallowPack(req.getDepth(), unshallowCommits);

				@SuppressWarnings("resource") // Ownership is transferred below
				DepthWalk.RevWalk dw = new DepthWalk.RevWalk(
						walk.getObjectReader(), walkDepth);
				dw.setDeepenSince(req.getDeepenSince());
				dw.setDeepenNots(deepenNots);
				dw.assumeShallow(req.getClientShallowCommits());
				rw = dw;
			}

			if (wantAll.isEmpty()) {
				pw.preparePack(pm, wantIds, commonBase,
						req.getClientShallowCommits());
			} else {
				walk.reset();

				ObjectWalk ow = rw.toObjectWalkWithSameObjects();
				pw.preparePack(pm, ow, wantAll, commonBase, PackWriter.NONE);
				rw = ow;
			}

			if (req.getClientCapabilities().contains(OPTION_INCLUDE_TAG)
					&& allTags != null) {
				for (Ref ref : allTags) {
					ObjectId objectId = ref.getObjectId();
					if (objectId == null) {
						// skip unborn branch
						continue;
					}

					// If the object was already requested, skip it.
					if (wantAll.isEmpty()) {
						if (wantIds.contains(objectId))
							continue;
					} else {
						RevObject obj = rw.lookupOrNull(objectId);
						if (obj != null && obj.has(WANT))
							continue;
					}

					if (!ref.isPeeled())
						ref = db.getRefDatabase().peel(ref);

					ObjectId peeledId = ref.getPeeledObjectId();
					objectId = ref.getObjectId();
					if (peeledId == null || objectId == null)
						continue;

					if (pw.willInclude(peeledId)) {
						// We don't need to handle parseTag throwing an
						// IncorrectObjectTypeException as we only reach
						// here when ref is an annotated tag
						addTagChain(rw.parseTag(objectId), pw);
					}
				}
			}

			if (pckOut.isUsingSideband()) {
				if (req instanceof FetchV2Request &&
						cachedPackUriProvider != null &&
						!((FetchV2Request) req).getPackfileUriProtocols().isEmpty()) {
					FetchV2Request reqV2 = (FetchV2Request) req;
					pw.setPackfileUriConfig(new PackWriter.PackfileUriConfig(
							pckOut,
							reqV2.getPackfileUriProtocols(),
							cachedPackUriProvider));
				} else {
					// PackWriter will write "packfile-uris\n" and "packfile\n"
					// for us if provided a PackfileUriConfig. In this case, we
					// are not providing a PackfileUriConfig, so we have to
					// write this line ourselves.
					pckOut.writeString(
							GitProtocolConstants.SECTION_PACKFILE + '\n');
				}
			}
			pw.enableSearchForReuseTimeout();
			pw.writePack(pm, NullProgressMonitor.INSTANCE, packOut);

			if (msgOut != NullOutputStream.INSTANCE) {
				String msg = pw.getStatistics().getMessage() + '\n';
				msgOut.write(Constants.encode(msg));
				msgOut.flush();
			}

		} finally {
			statistics = pw.getStatistics();
			if (statistics != null) {
				postUploadHook.onPostUpload(statistics);
			}
			pw.close();
		}
	}

	private static void findSymrefs(
			final RefAdvertiser adv, final Map<String, Ref> refs) {
		Ref head = refs.get(Constants.HEAD);
		if (head != null && head.isSymbolic()) {
			adv.addSymref(Constants.HEAD, head.getLeaf().getName());
		}
	}

	private void addTagChain(
			RevTag tag, PackWriter pw) throws IOException {
		RevObject o = tag;
		do {
			tag = (RevTag) o;
			walk.parseBody(tag);
			if (!pw.willInclude(tag.getId())) {
				pw.addObject(tag);
			}
			o = tag.getObject();
		} while (Constants.OBJ_TAG == o.getType());
	}

	private static class ResponseBufferedOutputStream extends OutputStream {
		private final OutputStream rawOut;

		private OutputStream out;

		ResponseBufferedOutputStream(OutputStream rawOut) {
			this.rawOut = rawOut;
			this.out = new ByteArrayOutputStream();
		}

		@Override
		public void write(int b) throws IOException {
			out.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			out.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			out.flush();
		}

		@Override
		public void close() throws IOException {
			out.close();
		}

		void stopBuffering() throws IOException {
			if (out != rawOut) {
				((ByteArrayOutputStream) out).writeTo(rawOut);
				out = rawOut;
			}
		}
	}

	private interface ErrorWriter {
		void writeError(String message) throws IOException;
	}

	private class SideBandErrorWriter implements ErrorWriter {
		@Override
		public void writeError(String message) throws IOException {
			@SuppressWarnings("resource" /* java 7 */)
			SideBandOutputStream err = new SideBandOutputStream(
					SideBandOutputStream.CH_ERROR,
					SideBandOutputStream.SMALL_BUF, requireNonNull(rawOut));
			err.write(Constants.encode(message));
			err.flush();
		}
	}

	private class PackProtocolErrorWriter implements ErrorWriter {
		@Override
		public void writeError(String message) throws IOException {
			new PacketLineOut(requireNonNull(rawOut))
					.writeString("ERR " + message + '\n'); //$NON-NLS-1$
		}
	}
}
