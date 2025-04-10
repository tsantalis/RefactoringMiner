/* This file is part of VoltDB.
 * Copyright (C) 2008-2015 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb.importer;

import static com.google_voltpatches.common.base.Preconditions.checkNotNull;
import static com.google_voltpatches.common.base.Predicates.equalTo;
import static com.google_voltpatches.common.base.Predicates.isNull;
import static com.google_voltpatches.common.base.Predicates.not;
import static org.voltcore.zk.ZKUtil.joinZKPath;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

import org.apache.zookeeper_voltpatches.AsyncCallback;
import org.apache.zookeeper_voltpatches.AsyncCallback.Children2Callback;
import org.apache.zookeeper_voltpatches.AsyncCallback.DataCallback;
import org.apache.zookeeper_voltpatches.AsyncCallback.StatCallback;
import org.apache.zookeeper_voltpatches.AsyncCallback.StringCallback;
import org.apache.zookeeper_voltpatches.AsyncCallback.VoidCallback;
import org.apache.zookeeper_voltpatches.CreateMode;
import org.apache.zookeeper_voltpatches.KeeperException;
import org.apache.zookeeper_voltpatches.KeeperException.Code;
import org.apache.zookeeper_voltpatches.KeeperException.NodeExistsException;
import org.apache.zookeeper_voltpatches.WatchedEvent;
import org.apache.zookeeper_voltpatches.Watcher;
import org.apache.zookeeper_voltpatches.Watcher.Event.EventType;
import org.apache.zookeeper_voltpatches.Watcher.Event.KeeperState;
import org.apache.zookeeper_voltpatches.ZooDefs;
import org.apache.zookeeper_voltpatches.ZooKeeper;
import org.apache.zookeeper_voltpatches.data.Stat;
import org.json_voltpatches.JSONArray;
import org.json_voltpatches.JSONException;
import org.json_voltpatches.JSONStringer;
import org.voltcore.logging.VoltLogger;
import org.voltcore.utils.CoreUtils;
import org.voltcore.zk.ZKUtil;
import org.voltdb.OperationMode;
import org.voltdb.VoltZK;

import com.google_voltpatches.common.base.Function;
import com.google_voltpatches.common.base.Optional;
import com.google_voltpatches.common.base.Preconditions;
import com.google_voltpatches.common.base.Predicate;
import com.google_voltpatches.common.collect.FluentIterable;
import com.google_voltpatches.common.collect.ImmutableSortedMap;
import com.google_voltpatches.common.collect.ImmutableSortedSet;
import com.google_voltpatches.common.collect.Maps;
import com.google_voltpatches.common.collect.Sets;
import com.google_voltpatches.common.collect.TreeMultimap;
import com.google_voltpatches.common.eventbus.AsyncEventBus;
import com.google_voltpatches.common.eventbus.DeadEvent;
import com.google_voltpatches.common.eventbus.EventBus;
import com.google_voltpatches.common.eventbus.Subscribe;
import com.google_voltpatches.common.eventbus.SubscriberExceptionContext;
import com.google_voltpatches.common.eventbus.SubscriberExceptionHandler;

/**
 *  An importer channel distributer that uses zookeeper to coordinate how importer channels are
 *  distributed among VoltDB cluster nodes. A proposal is merged against a master channel lists,
 *  stored in the /import/master zookeeper node. The elected distributer leader distributes the
 *  merge differences among all available nodes, by writing each node's assigned list of
 *  channels in their respective /import/host/[host-name] nodes. When a node leaves the mesh
 *  its assigned channels are redistributed among the surviving nodes
 */
public class ChannelDistributer implements ChannelChangeCallback {

    private final static VoltLogger LOG = new VoltLogger("IMPORT");

    /** root for all importer nodes */
    static final String IMPORT_DN = "/import";
    /** parent node for all {@link CreateMode#EPHEMERAL ephemeral} host nodes */
    static final String HOST_DN = joinZKPath(IMPORT_DN, "host");
    /** parent directory for leader candidates and holder of the channels master list */
    static final String MASTER_DN = joinZKPath(IMPORT_DN, "master");
    /** leader candidate node prefix for {@link CreateMode#EPHEMERAL_SEQUENTIAL} nodes */
    static final String CANDIDATE_PN = joinZKPath(MASTER_DN, "candidate_");

    static final byte[] EMPTY_ARRAY = "[]".getBytes(StandardCharsets.UTF_8);

    static void mkdirs(ZooKeeper zk, String zkNode, byte[] content) {
        try {
            ZKUtil.asyncMkdirs(zk, zkNode, content).get();
        } catch (NodeExistsException itIsOk) {
        } catch (InterruptedException | KeeperException e) {
            String msg = "Unable to create zk directory: " + zkNode;
            LOG.error(msg, e);
            throw new DistributerException(msg, e);
        }
    }

    /**
     * Boiler plate method to log an error message and wrap, and return a {@link DistributerException}
     * around the message and cause
     *
     * @param cause fault origin {@link Throwable}
     * @param format a {@link String#format(String, Object...) compliant format string
     * @param args formatter arguments
     * @return a {@link DistributerException}
     */
    static DistributerException loggedDistributerException(Throwable cause, String format, Object...args) {
        Optional<DistributerException> causeFor = DistributerException.isCauseFor(cause);
        if (causeFor.isPresent()) {
            return causeFor.get();
        }
        String msg = String.format(format, args);
        if (cause != null) {
            LOG.error(msg, cause);
            return new DistributerException(msg, cause);
        } else {
            LOG.error(msg);
            return new DistributerException(msg);
        }
    }

    /**
     * Boiler plate method that checks a zookeeper callback @{link {@link KeeperException.Code},
     * converts it to a {@link DistributerException} and if it does not indicate success,
     *
     * @param code a {@link KeeperException.Code callback code}
     * @param format {@link String#format(String, Object...) compliant format string
     * @param args formatter arguments
     * @return an {@link Optional} that may contain a {@link DistributerException}
     */
    static Optional<DistributerException> checkCode(Code code, String format, Object...args) {
        if (code != Code.OK) {
            KeeperException kex = KeeperException.create(code);
            return Optional.of(loggedDistributerException(kex, format, args));
        } else {
            return Optional.absent();
        }
    }

    /**
     * Boiler plate method that acquires, and releases a {@link Semaphore}
     * @param lock a {@link Semaphore}
     */
    static void acquireAndRelease(Semaphore lock) {
        try {
            lock.acquire();
            lock.release();
        } catch (InterruptedException ex) {
            throw loggedDistributerException(ex, "iterruped while waiting for a semaphare");
        }
    }

    /**
     * Reads the JSON document contained in the byte array data, and
     * converts it to a {@link NavigableSet<ChannelSpec> set of channel specs}
     *
     * @param data zookeeper node data content
     * @return a {@link NavigableSet<ChannelSpec> set of channel specs}
     * @throws JSONException on JSON parse failures
     * @throws IllegalArgumentException on encoded channel spec parse failures
     */
    static NavigableSet<ChannelSpec> asChannelSet(byte[] data)
            throws JSONException, IllegalArgumentException {
        ImmutableSortedSet.Builder<ChannelSpec> sbld = ImmutableSortedSet.naturalOrder();
        JSONArray ja = new JSONArray(new String(data, StandardCharsets.UTF_8));
        for (int i=0; i< ja.length(); ++i) {
            sbld.add(new ChannelSpec(ja.getString(i)));
        }
        return  sbld.build();
    }

    /**
     * Converts the given a {@link NavigableSet<ChannelSpec> set of channel specs}
     * into a byte array with the content of a JSON document
     *
     * @param specs a a {@link NavigableSet<ChannelSpec> set of channel specs}
     * @return a byte array
     * @throws JSONException on JSON building failures
     * @throws IllegalArgumentException on channel spec encoding failures
     */
    static byte [] asHostData(NavigableSet<ChannelSpec> specs)
            throws JSONException, IllegalArgumentException {
        JSONStringer js = new JSONStringer();
        js.array();
        for (ChannelSpec spec: specs) {
            js.value(spec.asJSONValue());
        }
        js.endArray();
        return js.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Tracing utility method useful for debugging
     *
     * @param o and object
     * @return string with information on the given object
     */
    static String id(Object o) {
        if (o == null) return "(null)";
        Thread t = Thread.currentThread();
        StringBuilder sb = new StringBuilder(128);
        sb.append("(T[").append(t.getName()).append("]@");
        sb.append(Long.toString(t.getId(), Character.MAX_RADIX));
        sb.append(":O[").append(o.getClass().getSimpleName());
        sb.append("]@");
        sb.append(Long.toString(System.identityHashCode(o),Character.MAX_RADIX));
        sb.append(")");
        return sb.toString();
    }

    final static SubscriberExceptionHandler eventBusFaultHandler = new SubscriberExceptionHandler() {
        @Override
        public void handleException(Throwable exception, SubscriberExceptionContext context) {
            loggedDistributerException(
                    exception,
                    "fault during callback dispatch for event %s",
                    context.getEvent()
                    );
        }
    };

    private final ExecutorService m_es;
    private final AtomicBoolean m_done = new AtomicBoolean(false);
    private final ZooKeeper m_zk;
    private final String m_hostId;
    private final String m_candidate;
    private final Deque<ImporterChannelAssignment> m_undispatched;
    private final EventBus m_eb;
    private final ExecutorService m_buses;

    volatile boolean m_isLeader = false;
    final SpecsRef m_specs = new SpecsRef();
    final HostsRef m_hosts = new HostsRef();
    final ChannelsRef m_channels = new ChannelsRef();
    final CallbacksRef m_callbacks = new CallbacksRef();
    final AtomicStampedReference<OperationMode> m_mode;

    /**
     * Initialize a distributer within importer channel distribution mesh by performing the
     * following actions:
     * <ul>
     * <li>registers a leader candidate</li>
     * <li>starts watchers on the channel master list, on the directory holding host nodes,
     * and the directory used for leader elections</li>
     * <li>create election candidate node</li>
     * <li>create its own host node</li>
     * </ul>
     * @param zk
     * @param hostId
     * @param queue
     */
    public ChannelDistributer(ZooKeeper zk, String hostId, BlockingDeque<ChannelAssignment> queue) {
        Preconditions.checkArgument(
                hostId != null && !hostId.trim().isEmpty(),
                "hostId is null or empty"
                );
        m_hostId = hostId;
        m_zk = Preconditions.checkNotNull(zk, "zookeeper is null");
        m_es = CoreUtils.getCachedSingleThreadExecutor("Import Channel Distributer for Host " + hostId, 15000);
        m_buses = CoreUtils.getCachedSingleThreadExecutor(
                "Import Channel Distributer Event Bus Dispatcher for Host " + hostId, 15000
                );
        m_eb = new AsyncEventBus(m_buses, eventBusFaultHandler);
        m_eb.register(this);
        m_mode = new AtomicStampedReference<>(OperationMode.INITIALIZING, 0);
        m_undispatched = new LinkedList<>();

        // Prime directory structure if needed
        mkdirs(zk, VoltZK.operationMode, OperationMode.INITIALIZING.getBytes());
        mkdirs(zk, HOST_DN, EMPTY_ARRAY);
        mkdirs(zk, MASTER_DN, EMPTY_ARRAY);

        GetOperationalMode opMode = new GetOperationalMode(VoltZK.operationMode);
        MonitorHostNodes monitor = new MonitorHostNodes(HOST_DN);
        CreateNode createHostNode = new CreateNode(
                joinZKPath(HOST_DN, hostId),
                EMPTY_ARRAY, CreateMode.EPHEMERAL
                );
        CreateNode electionCandidate = new CreateNode(
                CANDIDATE_PN,
                EMPTY_ARRAY, CreateMode.EPHEMERAL_SEQUENTIAL
                );
        ElectLeader elector = new ElectLeader(MASTER_DN, electionCandidate);

        opMode.getMode();
        monitor.getChildren();
        createHostNode.getNode();
        elector.elect();

        m_candidate = electionCandidate.getNode();
        // monitor the master list
        new GetChannels(MASTER_DN).getChannels();
    }

    public String getHostId() {
        return m_hostId;
    }

    public VersionedOperationMode getOperationMode() {
        return new VersionedOperationMode(m_mode);
    }

    /**
     * Register channels for the given importer. If they match to what is already registered
     * then nothing is done. Before registering channels, you need to register a callback
     * handler for channel assignments {@link #registerCallback(String, Object)}
     *
     * @param importer importer designation
     * @param uris list of channel URIs
     */
    public void registerChannels(String importer, Set<URI> uris) {
        NavigableSet<String> registered = m_callbacks.getReference().navigableKeySet();
        Preconditions.checkArgument(
                importer != null && !importer.trim().isEmpty(),
                "importer is null or empty"
                );
        Preconditions.checkArgument(uris != null, "uris set is null");
        Preconditions.checkArgument(
                !FluentIterable.from(uris).anyMatch(isNull()),
                "uris set %s contains null elements", uris
                );
        Preconditions.checkState(
                registered.contains(importer),
                "no callbacks registered for %s", importer
                );

        Predicate<ChannelSpec> forImporter = ChannelSpec.importerIs(importer);
        Function<URI,ChannelSpec> asSpec = ChannelSpec.fromUri(importer);

        // convert method parameters to a set of ChannelSpecs
        NavigableSet<ChannelSpec> proposed = ImmutableSortedSet.copyOf(
                FluentIterable.from(uris).transform(asSpec)
                );

        LOG.info("(" + m_hostId + ") proposing channels " + proposed);

        int [] stamp = new int[]{0};

        ImmutableSortedSet.Builder<ChannelSpec> sbldr = null;
        NavigableSet<ChannelSpec> prev = null;
        SetData setter = null;

        // retry writes when merging with stale data
        do {
            prev = m_channels.get(stamp);

            NavigableSet<ChannelSpec> current  = Sets.filter(prev, forImporter);
            if (current.equals(proposed)) {
                return;
            }
            sbldr = ImmutableSortedSet.naturalOrder();
            sbldr.addAll(Sets.filter(prev, not(forImporter)));
            sbldr.addAll(proposed);

            byte [] data = null;
            try {
                data = asHostData(sbldr.build());
            } catch (JSONException|IllegalArgumentException e) {
                throw loggedDistributerException(e, "failed to serialize the registration as json");
            }

            setter = new SetData(MASTER_DN, stamp[0], data);
        } while (setter.getCallbackCode() == Code.BADVERSION);

        setter.getStat();
    }

    /**
     * Registers a (@link ChannelChangeCallback} for the given importer.
     * @param importer
     * @param callback a (@link ChannelChangeCallback}
     */
    public void registerCallback(String importer, ChannelChangeCallback callback) {
        Preconditions.checkArgument(
                importer != null && !importer.trim().isEmpty(),
                "importer is null or empty"
                );
        callback = checkNotNull(callback, "callback is null");

        if (m_done.get()) return;

        int [] stamp = new int[]{0};
        NavigableMap<String,ChannelChangeCallback> prev = null;
        NavigableMap<String,ChannelChangeCallback> next = null;
        ImmutableSortedMap.Builder<String,ChannelChangeCallback> mbldr = null;

        synchronized (m_undispatched) {
            do {
                prev = m_callbacks.get(stamp);
                mbldr = ImmutableSortedMap.naturalOrder();
                mbldr.putAll(Maps.filterKeys(prev, not(equalTo(importer))));
                mbldr.put(importer, callback);
                next = mbldr.build();
            } while (!m_callbacks.compareAndSet(prev, next, stamp[0], stamp[0]+1));

            NavigableSet<String> registered = next.navigableKeySet();

            Iterator<ImporterChannelAssignment> itr = m_undispatched.iterator();
            while (itr.hasNext()) {
                final ImporterChannelAssignment assignment = itr.next();
                if (registered.contains(assignment.getImporter())) {
                    final ChannelChangeCallback dispatch = next.get(assignment.getImporter());
                    m_buses.submit(new DistributerRunnable() {
                        @Override
                        public void susceptibleRun() throws Exception {
                            dispatch.onChange(assignment);
                        }
                    });
                    itr.remove();
                }
            }
        }
    }

    /**
     * Sets the done flag, shuts down its executor thread, and deletes its own host
     * and candidate nodes
     */
    public void shutdown() {
        if (m_done.compareAndSet(false, true)) {
            m_es.shutdown();
            m_buses.shutdown();
            DeleteNode deleteHost = new DeleteNode(joinZKPath(HOST_DN, m_hostId));
            DeleteNode deleteCandidate = new DeleteNode(m_candidate);
            try {
                m_es.awaitTermination(365, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                throw loggedDistributerException(e, "interrupted while waiting for executor termination");
            }
            try {
                m_buses.awaitTermination(365, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                throw loggedDistributerException(e, "interrupted while waiting for executor termination");
            }
            deleteHost.onComplete();
            deleteCandidate.onComplete();
        }
    }

    /**
     * Keeps assignments for unregistered importers
     * @param e
     */
    @Subscribe
    public void undispatched(DeadEvent e) {
        if (!m_done.get() && e.getEvent() instanceof ImporterChannelAssignment) {
            ImporterChannelAssignment assignment = (ImporterChannelAssignment)e.getEvent();

            synchronized (m_undispatched) {
                NavigableSet<String> registered = m_callbacks.getReference().navigableKeySet();
                if (registered.contains(assignment.getImporter())) {
                    m_eb.post(assignment);
                } else {
                    m_undispatched.add(assignment);
                }
            }
        }
    }

    @Override
    @Subscribe
    public void onChange(ImporterChannelAssignment assignment) {
        ChannelChangeCallback cb = m_callbacks.getReference().get(assignment.getImporter());
        if (cb != null && !m_done.get()) try {
            cb.onChange(assignment);
        } catch (Exception callbackException) {
            throw loggedDistributerException(
                    callbackException,
                    "failed to invoke the onChange() calback for importer %s",
                    assignment.getImporter()
                    );
        } else synchronized(m_undispatched) {
            m_undispatched.add(assignment);
        }
    }

    @Override
    @Subscribe
    public void onClusterStateChange(VersionedOperationMode mode) {
        Optional<DistributerException> fault = Optional.absent();
        for (Map.Entry<String, ChannelChangeCallback> e: m_callbacks.getReference().entrySet()) try {
            if (!m_done.get()) e.getValue().onClusterStateChange(mode);
        } catch (Exception callbackException) {
            fault = Optional.of(loggedDistributerException(
                    callbackException,
                    "failed to invoke the onClusterStateChange() calback for importer %s",
                    e.getKey()
                    ));
        }
        if (fault.isPresent()) {
            throw fault.get();
        }
    }

    /**
     * Base class for all runnables submitted to the executor service
     */
    abstract class DistributerRunnable implements Runnable {
        @Override
        public void run() {
            try {
                if (!m_done.get()) {
                    susceptibleRun();
                }
            } catch (Exception ex) {
                throw loggedDistributerException(ex, "Fault occured while executing runnable");
            }
        }

        public abstract void susceptibleRun() throws Exception;
    }

    /**
     * A {@link DistributerRunnable} that compares the registered {@link ChannelSpec} list
     * against the already assigned list of {@link ChannelSpec}. Any additions are distributed
     * as evenly as possible to all the nodes participating in the distributer mesh, taking into
     * account the removals. Then it writes to each node their assigned list of importer
     * channels. This is run exclusively by the mesh leader.
     *
     */
    class AssignChannels extends DistributerRunnable {

        /** registered channels */
        final NavigableSet<ChannelSpec> channels = m_channels.getReference();
        /** assigned channels */
        final NavigableMap<ChannelSpec,String> specs = m_specs.getReference();
        /** mesh nodes */
        final NavigableMap<String,AtomicInteger> hosts = m_hosts.getReference();

        @Override
        public void susceptibleRun() throws Exception {
            if (m_mode.getReference() == OperationMode.INITIALIZING) {
                return;
            }

            NavigableSet<ChannelSpec> assigned = specs.navigableKeySet();
            Set<ChannelSpec> added   = Sets.difference(channels, assigned);
            Set<ChannelSpec> removed = Sets.difference(assigned, channels);

            if (added.isEmpty() && removed.isEmpty()) {
                return;
            }

            Predicate<Map.Entry<ChannelSpec,String>> withoutRemoved =
                    not(ChannelSpec.specKeyIn(removed, String.class));
            NavigableMap<ChannelSpec,String> pruned =
                    Maps.filterEntries(specs, withoutRemoved);

            if (!removed.isEmpty()) {
                LOG.info("LEADER (" + m_hostId + ") removing channels " + removed);
            }
            // makes it easy to group channels by host
            TreeMultimap<String, ChannelSpec> byhost = TreeMultimap.create();

            for (Map.Entry<ChannelSpec,String> e: pruned.entrySet()) {
                byhost.put(e.getValue(), e.getKey());
            }
            // approximation of how many channels should be assigned to each node
            int fair = new Double(Math.ceil(channels.size()/(double)hosts.size())).intValue();
            List<String> hostassoc = new ArrayList<>(added.size());
            for (String host: hosts.navigableKeySet()) {
                // negative means it is over allocated
                int room = fair - byhost.get(host).size();
                for (int i = 0; i < room; ++i) {
                    hostassoc.add(host);
                }
            }
            Collections.shuffle(hostassoc, new Random(System.identityHashCode(this)));

            Iterator<String> hitr = hostassoc.iterator();
            Iterator<ChannelSpec> citr = added.iterator();
            while (citr.hasNext()) {
                String host = hitr.next();
                ChannelSpec spec = citr.next();
                byhost.put(host, spec);
                LOG.info("LEADER (" + m_hostId + ") assingning " + spec + " to host " + host);
            }

            try {
                // write to each node their assigned channel list
                NavigableSet<ChannelSpec> previous = null;
                NavigableSet<ChannelSpec> needed = null;
                SetNodeChannels setter = null;

                for (String host: hosts.navigableKeySet()) {
                    previous = Maps.filterValues(specs,equalTo(host)).navigableKeySet();
                    needed = byhost.get(host);
                    if (!needed.equals(previous)) {
                        int version = hosts.get(host).get();
                        byte [] nodedata = asHostData(needed);
                        setter = new SetNodeChannels(joinZKPath(HOST_DN, host), version, nodedata);
                    }
                }
                // wait for the last write to complete
                if (setter != null) {
                    setter.getCallbackCode();
                }
            } catch (JSONException|IllegalArgumentException e) {
                LOG.fatal("unable to create json document to assign imported channels to nodes", e);
            }
        }
    }

    /**
     * A wrapper around {@link ZooKeeper#setData(String, byte[], int, StatCallback, Object)} that
     * acts as its own invocation {@link AsyncCallback.StatCallback}
     */
    class SetData implements StatCallback {

        final String path;
        final int version;

        final Semaphore lock = new Semaphore(0);
        volatile Optional<Stat> stat = Optional.absent();
        volatile Optional<DistributerException> fault = Optional.absent();
        volatile Optional<Code> callbackCode = Optional.absent();

        SetData(String path, int version, byte [] data ) {
            this.path = path;
            this.version = version;
            m_zk.setData(path, data, version, this, null);
        }

        void internalProcessResult(int rc, String path, Object ctx, Stat stat) {
            callbackCode = Optional.of(Code.get(rc));
            Code code = callbackCode.get();
            if (code == Code.OK) {
                this.stat = Optional.of(stat);
            } else if (code == Code.NONODE || code == Code.BADVERSION) {
                // keep the fault but don't log it
                KeeperException e = KeeperException.create(code);
                fault = Optional.of(new DistributerException("failed to write to " + path, e));
            } else if (!m_done.get()) {
                fault = checkCode(code, "failed to write to %s", path);
            }
        }

        @Override
        public void processResult(int rc, String path, Object ctx, Stat stat) {
            try {
                internalProcessResult(rc, path, ctx, stat);
            } finally {
                lock.release();
            }
        }

        public Stat getStat() {
            acquireAndRelease(lock);
            if (fault.isPresent()) throw fault.get();
            return stat.get();
        }

        public Code getCallbackCode() {
            acquireAndRelease(lock);
            return callbackCode.get();
        }
    }

    /**
     * An extension of {@link SetData} that is used to write to nodes their
     * assigned list of import channels. NB the mesh leader is the only one
     * that instantiates and uses this class
     */
    class SetNodeChannels extends SetData {

        SetNodeChannels(String path, int version, byte[] data) {
            super(path, version, data);
        }

        @Override
        public void processResult(int rc, String path, Object ctx, Stat stat) {
            try {
                internalProcessResult(rc, path, ctx, stat);
                Code code = Code.get(rc);
                // no node, or bad version means that we need to work on the assignments
                // again.
                if ((code == Code.NONODE || code == Code.BADVERSION) && !m_done.get()) {
                    m_es.submit(new AssignChannels());
                }
            } finally {
                lock.release();
            }
        }
    }

    /**
     * A wrapper around {@link ZooKeeper#create(String, byte[], List, CreateMode, StringCallback, Object)}
     * that acts as its own invocation {@link AsyncCallback.StringCallback}
     */
    class CreateNode implements StringCallback {

        final Semaphore lock = new Semaphore(0);
        volatile Optional<String> node = Optional.absent();
        volatile Optional<DistributerException> fault = Optional.absent();

        CreateNode(String path, byte [] data, CreateMode cmode) {
            m_zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, cmode, this, null);
        }

        @Override
        public void processResult(int rc, String path, Object ctx, String name) {
            try {
                Code code = Code.get(rc);
                switch(code) {
                case NODEEXISTS:
                    code = Code.OK;
                    break;
                case OK:
                    node = Optional.of(name);
                    break;
                default:
                    node = Optional.of(path);
                }
                fault = checkCode(code, "cannot create node %s", node.get());
            } finally {
                lock.release();
            }
        }

        String getNode() {
            acquireAndRelease(lock);
            if (fault.isPresent()) throw fault.get();
            return node.get();
        }
    }

    /**
     * A wrapper around a more tolerant {@link ZooKeeper#delete(String, int, VoidCallback, Object)} that
     * acts as its own invocation {@link AsyncCallback.VoidCallback}.
     */
    class DeleteNode implements VoidCallback {
        final String path;

        final Semaphore lock = new Semaphore(0);
        volatile Optional<DistributerException> fault = Optional.absent();
        volatile Optional<Code> callbackCode = Optional.absent();

        DeleteNode(String path) {
            this.path = path;
            m_zk.delete(path, -1, this, null);
        }

        void internalProcessResult(int rc, String path, Object ctx) {
            callbackCode = Optional.of(Code.get(rc));
            switch (callbackCode.get()) {
            case OK:
            case NONODE:
            case SESSIONEXPIRED:
            case SESSIONMOVED:
            case CONNECTIONLOSS:
                break;
            default:
                fault = checkCode(callbackCode.get(), "failed to delete %s", path);
                break;
            }
        }

        @Override
        public void processResult(int rc, String path, Object ctx) {
            try {
                internalProcessResult(rc, path, ctx);
            } finally {
                lock.release();
            }
        }

        public Code getCallbackCode() {
            acquireAndRelease(lock);
            return callbackCode.get();
        }

        public void onComplete() {
            acquireAndRelease(lock);
            if (fault.isPresent()) throw fault.get();
        }
    }

    /**
     * A wrapper around {@link ZooKeeper#getChildren(String, Watcher, Children2Callback, Object)} that
     * acts as its own one time {@link Watcher} and its own {@link AsyncCallback.Children2Callback}
     */
    class GetChildren extends DistributerRunnable implements Children2Callback, Watcher {

        final String path;
        final Semaphore lock = new Semaphore(0);

        volatile Optional<Stat> stat = Optional.absent();
        volatile Optional<NavigableSet<String>> children = Optional.absent();
        volatile Optional<DistributerException> fault = Optional.absent();

        GetChildren(String path) {
            this.path = path;
            m_zk.getChildren(path, this, this, null);
        }

        void internalProcessResults(int rc, String path, Object ctx,
                List<String> children, Stat stat) {
            Code code = Code.get(rc);
            if (code == Code.OK) {
                NavigableSet<String> childset = ImmutableSortedSet.copyOf(children);
                this.stat = Optional.of(stat);
                this.children = Optional.of(childset);
            } else if (code == Code.SESSIONEXPIRED) {
                // keep the fault but don't log it
                KeeperException e = KeeperException.create(code);
                fault = Optional.of(new DistributerException("unable to get children for " + path, e));
            } else if (!m_done.get()) {
                fault = checkCode(code, "unable to get children for %s", path);
            }
        }

        @Override
        public void processResult(int rc, String path, Object ctx,
                List<String> children, Stat stat) {
            try {
                internalProcessResults(rc, path, ctx, children, stat);
            } finally {
                lock.release();
            }
        }

        public NavigableSet<String> getChildren() {
            acquireAndRelease(lock);
            if (fault.isPresent()) throw fault.get();
            return children.get();
        }

        public Optional<Stat> getStat() {
            return stat;
        }

        @Override
        public void process(WatchedEvent e) {
            if (   e.getState() == KeeperState.SyncConnected
                && e.getType() == EventType.NodeChildrenChanged
                && !m_done.get())
            {
                m_es.submit(this);
            }
        }

        @Override
        public void susceptibleRun() throws Exception {
            new GetChildren(path);
        }
    }

    /**
     * An extension of {@link GetChildren} that is used to determine the distributer mesh leader
     */
    class ElectLeader extends GetChildren {
        final CreateNode leaderCandidate;

        ElectLeader(String path, CreateNode leaderCandidate) {
            super(path);
            this.leaderCandidate = Preconditions.checkNotNull(leaderCandidate,"candidate is null");
        }

        @Override
        public void processResult(int rc, String path, Object ctx,
                List<String> children, Stat stat) {
            try {
                internalProcessResults(rc, path, ctx, children, stat);
                if (Code.get(rc) != Code.OK || m_done.get()) {
                    return;
                }
                m_es.submit(new DistributerRunnable() {
                    @Override
                    public void susceptibleRun() throws Exception {
                        String candidate = basename.apply(leaderCandidate.getNode());
                        if (!m_isLeader && candidate.equals(ElectLeader.this.children.get().first())) {
                            m_isLeader = true;
                            LOG.info("LEADER (" + m_hostId + ") is now the importer channel leader");
                            // determine node importer channel assignments
                            new AssignChannels().run();
                        }
                    }
                });
            } finally {
                lock.release();
            }
        }

        boolean elect() {
            return getChildren().first().equals(leaderCandidate.getNode());
        }

        @Override
        public void susceptibleRun() throws Exception {
            new ElectLeader(path, leaderCandidate);
        }
    }

    /**
     * A wrapper around {@link ZooKeeper#getData(String, Watcher, DataCallback, Object)} that acts
     * as its own {@link Watcher}, and {@link AsyncCallback.DataCallback}
     */
    class GetData extends DistributerRunnable implements DataCallback, Watcher {

        final String path;
        final Semaphore lock = new Semaphore(0);

        volatile Optional<Stat> stat = Optional.absent();
        volatile Optional<byte[]> data = Optional.absent();
        volatile Optional<DistributerException> fault = Optional.absent();

        GetData(String path) {
            this.path = path;
            m_zk.getData(path, this, this, null);
        }

        @Override
        public void process(WatchedEvent e) {
            if (   e.getState() == KeeperState.SyncConnected
                && e.getType() == EventType.NodeDataChanged
                && !m_done.get())
            {
                m_es.submit(this);
            }
        }

        void internalProcessResults(int rc, String path, Object ctx, byte[] data, Stat stat) {
            Code code = Code.get(rc);
            if (code == Code.OK) {
                this.stat = Optional.of(stat);
                this.data = Optional.of(data != null ? data : EMPTY_ARRAY);
            } else if (code == Code.NONODE || code == Code.SESSIONEXPIRED) {
                // keep the fault but don't log it
                KeeperException e = KeeperException.create(code);
                fault = Optional.of(new DistributerException(path + " went away", e));
            } else if (!m_done.get()) {
                fault = checkCode(code, "unable to read data in %s", path);
            }
        }

        @Override
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            try {
                internalProcessResults(rc, path, ctx, data, stat);
            } finally {
                lock.release();
            }
        }

        @Override
        public void susceptibleRun() throws Exception {
            new GetData(path);
        }

        public byte [] getData() {
            acquireAndRelease(lock);
            if (fault.isPresent()) throw fault.get();
            return data.get();
        }
    }

    /**
     * An extension of {@link GetData} that reads the date contents of a host
     * node as set of assigned importer channels. It merges the set against the
     * the import channels in {@link ChannelDistributer#m_specs}, and if the host
     * node is its own, it sends to the notification queue instances of
     * {@link ChannelAssignment} that describe assignment changes.
     *
     * It is instantiated mainly in {@link MonitorHostNodes}
     *
     */
    class GetHostChannels extends GetData {
        Optional<DistributerException> fault = Optional.absent();
        Optional<NavigableSet<ChannelSpec>> nodespecs = Optional.absent();

        final String host;
        final Predicate<Map.Entry<ChannelSpec,String>> thisHost;

        public GetHostChannels(String path) {
            super(path);
            this.host = basename.apply(path);
            this.thisHost = hostValueIs(this.host, ChannelSpec.class);
        }

        @Override
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            try {
                internalProcessResults(rc, path, ctx, data, stat);
                if (Code.get(rc) != Code.OK) {
                    return;
                }
                try {
                    nodespecs = Optional.of(asChannelSet(data));
                } catch (IllegalArgumentException|JSONException e) {
                    fault = Optional.of(
                            loggedDistributerException(e, "failed to parse json in %s", path)
                            );
                    return;
                }

                int [] sstamp = new int[]{0};
                AtomicInteger dstamp = m_hosts.getReference().get(host);
                if (dstamp == null) {
                    LOG.warn("(" + m_hostId + ") has no data stamp for "
                            + host + ", host registry contains: " + m_hosts.getReference()
                            );
                    dstamp = new AtomicInteger(0);
                }
                NavigableMap<ChannelSpec,String> prev = null;
                NavigableSet<ChannelSpec> oldspecs = null;
                ImmutableSortedMap.Builder<ChannelSpec,String> mbldr = null;

                do {
                    final int specversion = dstamp.get();
                    // callback has a stale version
                    if (specversion >= stat.getVersion()) {
                        return;
                    }
                    // register the data node version
                    if (!dstamp.compareAndSet(specversion, stat.getVersion())) {
                        return;
                    }

                    prev = m_specs.get(sstamp);
                    oldspecs = Maps.filterEntries(prev, thisHost).navigableKeySet();
                    // rebuild the assigned channel spec list
                    mbldr = ImmutableSortedMap.naturalOrder();
                    mbldr.putAll(Maps.filterEntries(prev, not(thisHost)));
                    for (ChannelSpec spec: nodespecs.get()) {
                        mbldr.put(spec, host);
                    }
                } while (!m_specs.compareAndSet(prev, mbldr.build(), sstamp[0], sstamp[0]+1));

                if (host.equals(m_hostId) && !m_done.get()) {
                    ChannelAssignment assignment = new ChannelAssignment(
                            oldspecs, nodespecs.get(), stat.getVersion()
                            );
                    for (ImporterChannelAssignment cassigns: assignment.getImporterChannelAssignments()) {
                        m_eb.post(cassigns);
                    }
                    if (!assignment.getRemoved().isEmpty()) {
                        LOG.info("(" + m_hostId + ") removing the following channel assingments: " + assignment.getRemoved());
                    }
                    if (!assignment.getAdded().isEmpty()) {
                        LOG.info("(" + m_hostId + ") adding the following channel assingments: " + assignment.getAdded());
                    }
                }
            } finally {
                lock.release();
            }
        }

        @Override
        public void susceptibleRun() throws Exception {
            new GetHostChannels(path);
        }

        NavigableSet<ChannelSpec> getSpecs() {
            acquireAndRelease(lock);
            if (fault.isPresent()) throw fault.get();
            return nodespecs.get();
        }
    }

    /**
     * An extension of {@link GetData} that monitors the content of the registered
     * importer channel list.
     */
    class GetChannels extends GetData {

        Optional<DistributerException> fault = Optional.absent();
        Optional<NavigableSet<ChannelSpec>> channels = Optional.absent();

        public GetChannels(String path) {
            super(path);
        }

        @Override
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            try {
                internalProcessResults(rc, path, ctx, data, stat);
                if (Code.get(rc) != Code.OK) {
                    return;
                }
                try {
                    channels = Optional.of(asChannelSet(data));
                } catch (IllegalArgumentException|JSONException e) {
                    fault = Optional.of(
                            loggedDistributerException(e, "failed to parse json in %s", path)
                            );
                    return;
                }
                int [] stamp = new int[]{0};
                NavigableSet<ChannelSpec> oldspecs = m_channels.get(stamp);
                if (stamp[0] >= stat.getVersion()) {
                    return;
                }
                if (!m_channels.compareAndSet(oldspecs, channels.get(), stamp[0], stat.getVersion())) {
                    return;
                }
                LOG.info("(" + m_hostId + ") succesfully received channel assignment master copy");
                if (m_isLeader && !m_done.get()) {
                    m_es.submit(new AssignChannels());
                }
            } finally {
                lock.release();
            }
        }

        @Override
        public void susceptibleRun() throws Exception {
            new GetChannels(path);
        }

        NavigableSet<ChannelSpec> getChannels() {
            acquireAndRelease(lock);
            if (fault.isPresent()) throw fault.get();
            return channels.get();
        }
    }

    /**
     * An extension of {@link GetData} that monitors the content of the cluster
     * operational mode
     */
    class GetOperationalMode extends GetData {
        Optional<VersionedOperationMode> mode = Optional.absent();

        GetOperationalMode(String path) {
            super(path);
        }

        @Override
        public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
            try {
                internalProcessResults(rc, path, ctx, data, stat);
                if (Code.get(rc) != Code.OK) {
                    return;
                }

                int [] stamp = new int[]{0};
                OperationMode prev = m_mode.get(stamp);
                if ( stamp[0] > stat.getVersion()) {
                    return;
                }
                OperationMode next = data != null ?  OperationMode.valueOf(data) : OperationMode.INITIALIZING;
                if (!m_mode.compareAndSet(prev, next, stamp[0], stat.getVersion())) {
                    return;
                }
                mode = Optional.of(new VersionedOperationMode(next, stat.getVersion()));
                if (m_isLeader && !m_done.get() && next == OperationMode.RUNNING) {
                    m_es.submit(new AssignChannels());
                }
                m_eb.post(mode.get());

            } finally {
                lock.release();
            }
        }

        @Override
        public void susceptibleRun() throws Exception {
            new GetOperationalMode(path);
        }

        VersionedOperationMode getMode() {
            acquireAndRelease(lock);
            if (fault.isPresent()) throw fault.get();
            if (!mode.isPresent()) {
                throw new DistributerException("failed to mirror cluster operation mode");
            }
            return mode.get();
        }
    }

    /**
     * An extension of {@link GetChildren} that monitor hosts that participate in the distributer mesh.
     * if any nodes leave the mesh, then they are removed from the assigned list, if any are added
     * then their channel assignments will start to get monitored.
     */
    class MonitorHostNodes extends GetChildren {

        MonitorHostNodes(String path) {
            super(path);
        }

        @Override
        public void processResult(int rc, String path, Object ctx,
                List<String> children, Stat stat) {
            try {
                internalProcessResults(rc, path, ctx, children, stat);
                if (Code.get(rc) != Code.OK) {
                    return;
                }

                int [] hstamp = new int[]{0};
                NavigableMap<String,AtomicInteger> oldgen = m_hosts.get(hstamp);
                if (hstamp[0] >= stat.getCversion()) {
                    return;
                }

                final Set<String> added   = Sets.difference(this.children.get(), oldgen.navigableKeySet());
                final Set<String> removed = Sets.difference(oldgen.navigableKeySet(), this.children.get());

                ImmutableSortedMap.Builder<String,AtomicInteger> hbldr = ImmutableSortedMap.naturalOrder();
                hbldr.putAll(Maps.filterEntries(oldgen, not(hostKeyIn(removed, AtomicInteger.class))));
                for (String add: added) {
                    hbldr.put(add, new AtomicInteger(0));
                }
                NavigableMap<String,AtomicInteger> newgen = hbldr.build();

                if (!m_hosts.compareAndSet(oldgen, newgen, hstamp[0], stat.getCversion())) {
                    return;
                }

                if (!removed.isEmpty()) {
                    final Predicate<Map.Entry<ChannelSpec,String>> inRemoved =
                            hostValueIn(removed, ChannelSpec.class);

                    int [] sstamp = new int[]{0};
                    NavigableMap<ChannelSpec,String> prev = null;
                    NavigableMap<ChannelSpec,String> next = null;

                    do {
                        prev = m_specs.get(sstamp);
                        next = Maps.filterEntries(prev, not(inRemoved));
                    } while (!m_specs.compareAndSet(prev, next, sstamp[0], sstamp[0]+1));

                    LOG.info("(" + m_hostId + ") hosts " + removed + " no longer servicing importer channels");

                    if (m_isLeader && !m_done.get()) {
                        m_es.submit(new AssignChannels());
                    }
                }

                if (!added.isEmpty() && !m_done.get()) {
                    m_es.submit(new DistributerRunnable() {
                        @Override
                        public void susceptibleRun() throws Exception {
                            for (String host: added) {
                                LOG.info("(" + m_hostId + ") starting to monitor host node " + host);
                                new GetHostChannels(joinZKPath(HOST_DN, host));
                            }
                        }
                    });
                }
            } finally {
                lock.release();
            }
        }

        @Override
        public void susceptibleRun() throws Exception {
            new MonitorHostNodes(path);
        }
    }

    // a form of type alias
    final static class HostsRef extends AtomicStampedReference<NavigableMap<String,AtomicInteger>> {
        static final NavigableMap<String,AtomicInteger> EMPTY_MAP = ImmutableSortedMap.of();

        public HostsRef(NavigableMap<String,AtomicInteger> initialRef, int initialStamp) {
            super(initialRef, initialStamp);
        }

        public HostsRef() {
            this(EMPTY_MAP, 0);
        }
    }

    // a form of type alias
    final static class ChannelsRef extends AtomicStampedReference<NavigableSet<ChannelSpec>> {
        static final NavigableSet<ChannelSpec> EMPTY_SET = ImmutableSortedSet.of();

        public ChannelsRef(NavigableSet<ChannelSpec> initialRef, int initialStamp) {
            super(initialRef, initialStamp);
        }

        public ChannelsRef() {
            this(EMPTY_SET, 0);
        }
    }

    // a form of type alias
    final static class SpecsRef extends AtomicStampedReference<NavigableMap<ChannelSpec,String>> {
        static final NavigableMap<ChannelSpec,String> EMPTY_MAP = ImmutableSortedMap.of();

        public SpecsRef(NavigableMap<ChannelSpec,String> initialRef, int initialStamp) {
            super(initialRef, initialStamp);
        }

        public SpecsRef() {
            this(EMPTY_MAP, 0);
        }
    }

    // a form of type alias
    final static class CallbacksRef
        extends AtomicStampedReference<NavigableMap<String,ChannelChangeCallback>> {

        static final NavigableMap<String,ChannelChangeCallback> EMTPY_MAP =
                ImmutableSortedMap.of();

        public CallbacksRef(
                NavigableMap<String,ChannelChangeCallback> initialRef,
                int initialStamp) {
            super(initialRef, initialStamp);
        }

        public CallbacksRef() {
            this(EMTPY_MAP,0);
        }
    }

    static <K> Predicate<Map.Entry<K, String>> hostValueIs(final String s, Class<K> clazz) {
        return new Predicate<Map.Entry<K,String>>() {
            @Override
            public boolean apply(Entry<K, String> e) {
                return s.equals(e.getValue());
            }
        };
    }

    static <K> Predicate<Map.Entry<K, String>> hostValueIn(final Set<String> s, Class<K> clazz) {
        return new Predicate<Map.Entry<K,String>>() {
            @Override
            public boolean apply(Entry<K, String> e) {
                return s.contains(e.getValue());
            }
        };
    }

    static <V> Predicate<Map.Entry<String,V>> hostKeyIn(final Set<String> s, Class<V> clazz) {
        return new Predicate<Map.Entry<String,V>>() {
            @Override
            public boolean apply(Entry<String,V> e) {
                return s.contains(e.getKey());
            }
        };
    }

    final static Function<String,String> basename = new Function<String, String>() {
        @Override
        public String apply(String path) {
            return new File(path).getName();
        }
    };

    public final static Predicate<ImporterChannelAssignment> importerIs(final String importer) {
        return new Predicate<ImporterChannelAssignment>() {
            @Override
            public boolean apply(ImporterChannelAssignment assignment) {
                return importer.equals(assignment.getImporter());
            }
        };
    }

    public final static Predicate<ImporterChannelAssignment> importerIn(final Set<String> importers) {
        return new Predicate<ImporterChannelAssignment>() {
            @Override
            public boolean apply(ImporterChannelAssignment assignment) {
                return importers.contains(assignment.getImporter());
            }
        };
    }
}
