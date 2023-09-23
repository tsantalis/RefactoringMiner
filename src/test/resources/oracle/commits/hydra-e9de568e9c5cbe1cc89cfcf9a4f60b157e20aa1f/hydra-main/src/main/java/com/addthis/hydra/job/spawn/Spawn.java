/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.hydra.job.spawn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.text.ParseException;

import com.addthis.basis.util.JitterClock;
import com.addthis.basis.util.LessFiles;
import com.addthis.basis.util.LessStrings;
import com.addthis.basis.util.Parameter;
import com.addthis.basis.util.RollingLog;
import com.addthis.basis.util.TokenReplacerOverflowException;

import com.addthis.bark.StringSerializer;
import com.addthis.bark.ZkUtil;
import com.addthis.codec.annotations.Time;
import com.addthis.codec.codables.Codable;
import com.addthis.codec.config.Configs;
import com.addthis.codec.jackson.Jackson;
import com.addthis.codec.json.CodecJSON;
import com.addthis.hydra.common.util.CloseTask;
import com.addthis.hydra.job.HostFailWorker;
import com.addthis.hydra.job.IJob;
import com.addthis.hydra.job.Job;
import com.addthis.hydra.job.JobConfigManager;
import com.addthis.hydra.job.JobDefaults;
import com.addthis.hydra.job.JobEvent;
import com.addthis.hydra.job.JobExpand;
import com.addthis.hydra.job.JobParameter;
import com.addthis.hydra.job.JobState;
import com.addthis.hydra.job.JobTask;
import com.addthis.hydra.job.JobTaskDirectoryMatch;
import com.addthis.hydra.job.JobTaskErrorCode;
import com.addthis.hydra.job.JobTaskMoveAssignment;
import com.addthis.hydra.job.JobTaskReplica;
import com.addthis.hydra.job.JobTaskState;
import com.addthis.hydra.job.RebalanceOutcome;
import com.addthis.hydra.job.alert.JobAlertManager;
import com.addthis.hydra.job.alert.JobAlertManagerImpl;
import com.addthis.hydra.job.alias.AliasManager;
import com.addthis.hydra.job.alias.AliasManagerImpl;
import com.addthis.hydra.job.auth.PermissionsManager;
import com.addthis.hydra.job.backup.ScheduledBackupType;
import com.addthis.hydra.job.entity.JobCommand;
import com.addthis.hydra.job.entity.JobCommandManager;
import com.addthis.hydra.job.entity.JobEntityManager;
import com.addthis.hydra.job.entity.JobMacro;
import com.addthis.hydra.job.entity.JobMacroManager;
import com.addthis.hydra.job.mq.CommandTaskDelete;
import com.addthis.hydra.job.mq.CommandTaskKick;
import com.addthis.hydra.job.mq.CommandTaskReplicate;
import com.addthis.hydra.job.mq.CommandTaskRevert;
import com.addthis.hydra.job.mq.CommandTaskStop;
import com.addthis.hydra.job.mq.CoreMessage;
import com.addthis.hydra.job.mq.HostMessage;
import com.addthis.hydra.job.mq.HostState;
import com.addthis.hydra.job.mq.JobKey;
import com.addthis.hydra.job.mq.ReplicaTarget;
import com.addthis.hydra.job.mq.StatusTaskBackup;
import com.addthis.hydra.job.mq.StatusTaskBegin;
import com.addthis.hydra.job.mq.StatusTaskCantBegin;
import com.addthis.hydra.job.mq.StatusTaskEnd;
import com.addthis.hydra.job.mq.StatusTaskPort;
import com.addthis.hydra.job.mq.StatusTaskReplica;
import com.addthis.hydra.job.mq.StatusTaskReplicate;
import com.addthis.hydra.job.mq.StatusTaskRevert;
import com.addthis.hydra.job.spawn.JobOnFinishStateHandler.JobOnFinishState;
import com.addthis.hydra.job.store.DataStoreUtil;
import com.addthis.hydra.job.store.JobStore;
import com.addthis.hydra.job.store.SpawnDataStore;
import com.addthis.hydra.job.store.SpawnDataStoreKeys;
import com.addthis.hydra.job.web.SpawnService;
import com.addthis.hydra.job.web.SpawnServiceConfiguration;
import com.addthis.hydra.minion.Minion;
import com.addthis.hydra.task.run.TaskExitState;
import com.addthis.hydra.util.DirectedGraph;
import com.addthis.hydra.util.WebSocketManager;
import com.addthis.maljson.JSONArray;
import com.addthis.maljson.JSONObject;
import com.addthis.meshy.service.file.FileReference;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yammer.metrics.Metrics;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.addthis.hydra.job.store.SpawnDataStoreKeys.MINION_DEAD_PATH;
import static com.addthis.hydra.job.store.SpawnDataStoreKeys.SPAWN_QUEUE_PATH;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * manages minions running on remote notes. runs master http server to
 * communicate with and control those instances.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Spawn implements Codable, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(Spawn.class);

    // misc spawn configs

    static final int DEFAULT_REPLICA_COUNT = Parameter.intValue("spawn.defaultReplicaCount", 1);
    static final boolean ENABLE_JOB_FIXDIRS_ONCOMPLETE = Parameter.boolValue("job.fixdirs.oncomplete", true);

    public static final long inputMaxNumberOfCharacters = Parameter.longValue("spawn.input.max.length", 1_000_000);

    private static final int clientDropTimeMillis = Parameter.intValue("spawn.client.drop.time", 60_000);
    private static final int clientDropQueueSize  = Parameter.intValue("spawn.client.drop.queue", 2000);

    // log configs

    private static final boolean eventLogCompress = Parameter.boolValue("spawn.eventlog.compress", true);
    private static final int logMaxAge  = Parameter.intValue("spawn.event.log.maxAge", 60 * 60 * 1000);
    private static final int logMaxSize = Parameter.intValue("spawn.event.log.maxSize", 100 * 1024 * 1024);
    private static final String logDir = Parameter.value("spawn.event.log.dir", "log");

    // metrics

    public static void main(String[] args) throws Exception {
        Spawn spawn = Configs.newDefault(Spawn.class);
        spawn.startWebInterface();
        // register jvm shutdown hook to clean up resources
        Runtime.getRuntime().addShutdownHook(new Thread(new CloseTask(spawn), "Spawn Shutdown Hook"));
    }

    SpawnQueuesByPriority taskQueuesByPriority = new SpawnQueuesByPriority();

    private SpawnMQ spawnMQ;
    private SpawnService spawnService;

    private volatile int lastQueueSize = 0;

    final Lock jobLock = new ReentrantLock();
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final BlockingQueue<String> jobUpdateQueue = new LinkedBlockingQueue<>();
    private final SpawnJobFixer spawnJobFixer = new SpawnJobFixer(this);
    //To track web socket connections
    private final WebSocketManager webSocketManager = new WebSocketManager();

    @Nonnull public final HostManager hostManager;

    @Nonnull final SpawnState spawnState;
    @Nonnull final ConcurrentMap<String, ClientEventListener> listeners;
    @Nonnull final SpawnFormattedLogger spawnFormattedLogger;

    @Nonnull final PermissionsManager permissionsManager;
    @Nonnull final JobDefaults jobDefaults;

    @Nonnull private final File stateFile;
    @Nonnull private final ExecutorService expandKickExecutor;
    @Nonnull private final ScheduledExecutorService scheduledExecutor;
    @Nonnull private final CuratorFramework zkClient;
    @Nonnull private final SpawnDataStore spawnDataStore;
    @Nonnull private final JobConfigManager jobConfigManager;
    @Nonnull private final AliasManager aliasManager;
    @Nonnull private final JobAlertManager jobAlertManager;
    @Nonnull private final SpawnMesh spawnMesh;
    @Nonnull private final JobEntityManager<JobMacro> jobMacroManager;
    @Nonnull private final JobEntityManager<JobCommand> jobCommandManager;
    @Nonnull private final JobOnFinishStateHandler jobOnFinishStateHandler;
    @Nonnull private final SpawnBalancer balancer;
    @Nonnull private final HostFailWorker hostFailWorker;
    @Nonnull private final SystemManager systemManager;
    @Nonnull private final RollingLog eventLog;

    @Nullable private final JobStore jobStore;

    @JsonCreator
    private Spawn(@JsonProperty("debug") String debug,
                  @JsonProperty(value = "queryPort", required = true) int queryPort,
                  @JsonProperty("queryHttpHost") String queryHttpHost,
                  @JsonProperty("httpHost") String httpHost,
                  @JsonProperty("dataDir") File dataDir,
                  @JsonProperty("stateFile") File stateFile,
                  @JsonProperty("expandKickExecutor") ExecutorService expandKickExecutor,
                  @JsonProperty("scheduledExecutor") ScheduledExecutorService scheduledExecutor,
                  @Time(MILLISECONDS) @JsonProperty(value = "taskQueueDrainInterval", required = true)
                  int taskQueueDrainInterval,
                  @Time(MILLISECONDS) @JsonProperty(value = "hostStatusRequestInterval", required = true)
                  int hostStatusRequestInterval,
                  @Time(MILLISECONDS) @JsonProperty(value = "queueKickInterval", required = true)
                  int queueKickInterval,
                  @Time(MILLISECONDS) @JsonProperty("jobTaskUpdateHeartbeatInterval")
                  int jobTaskUpdateHeartbeatInterval,
                  @Nullable @JsonProperty("structuredLogDir") File structuredLogDir,
                  @Nullable @JsonProperty("jobStore") JobStore jobStore,
                  @Nullable @JsonProperty("queueType") String queueType,
                  @Nullable @JacksonInject CuratorFramework providedZkClient,
                  @JsonProperty(value = "permissionsManager", required = true) PermissionsManager permissionsManager,
                  @JsonProperty(value = "jobDefaults", required = true) JobDefaults jobDefaults) throws Exception {
        LessFiles.initDirectory(dataDir);
        this.stateFile = stateFile;
        this.permissionsManager = permissionsManager;
        this.jobDefaults = jobDefaults;
        if (stateFile.exists() && stateFile.isFile()) {
            spawnState = Jackson.defaultMapper().readValue(stateFile, SpawnState.class);
        } else {
            spawnState = Jackson.defaultCodec().newDefault(SpawnState.class);
        }
        File webDir = new File("web");
        this.listeners = new ConcurrentHashMap<>();
        this.expandKickExecutor = expandKickExecutor;
        this.scheduledExecutor = scheduledExecutor;
        if (structuredLogDir == null) {
            this.spawnFormattedLogger = SpawnFormattedLogger.createNullLogger();
        } else {
            this.spawnFormattedLogger =  SpawnFormattedLogger.createFileBasedLogger(structuredLogDir);
        }
        if (providedZkClient == null) {
            this.zkClient = ZkUtil.makeStandardClient();
        } else {
            this.zkClient = providedZkClient;
        }
        this.hostManager = new HostManager(zkClient);
        this.spawnDataStore = DataStoreUtil.makeCanonicalSpawnDataStore(true);
        this.systemManager = new SystemManagerImpl(
                this, debug, queryHttpHost + ":" + queryPort,
                httpHost + ":" + SpawnServiceConfiguration.SINGLETON.webPort,
                SpawnServiceConfiguration.SINGLETON.authenticationTimeout,
                SpawnServiceConfiguration.SINGLETON.sudoTimeout);
        this.jobConfigManager = new JobConfigManager(spawnDataStore);
        // look for local object to import
        log.info("[init] beginning to load stats from data store");
        aliasManager = new AliasManagerImpl(spawnDataStore);
        jobMacroManager = new JobMacroManager(this);
        jobCommandManager = new JobCommandManager(this);
        jobOnFinishStateHandler = new JobOnFinishStateHandlerImpl(this);
        loadSpawnQueue();
        // fix up null pointers
        for (Job job : spawnState.jobs.values()) {
            if (job.getSubmitTime() == null) {
                job.setSubmitTime(System.currentTimeMillis());
            }
        }
        loadJobs();
        // XXX Instantiate HostFailWorker/SpawnBalancer before SpawnMQ to avoid NPE during startup
        // Once connected, SpawnMQ will call HostFailWorker/SpawnBalancer to get host information, 
        // so the latter components must be created first.
        hostFailWorker = new HostFailWorker(this, hostManager, scheduledExecutor);
        balancer = new SpawnBalancer(this, hostManager);

        // connect to mesh
        this.spawnMesh = new SpawnMesh(this);
        // connect to message broker or fail
        if ("rabbit".equals(queueType)) {
            log.info("[init] connecting to rabbit message queue");
            this.spawnMQ = new SpawnMQImpl(zkClient, this);
            this.spawnMQ.connectToMQ(getUuid());
        } else if (queueType == null) {
            log.info("[init] skipping message queue");
        } else {
            throw new IllegalArgumentException("queueType (" + queueType +
                                               ") must be either a valid message queue type or null");
        }

        // XXX start FailHostTask schedule separately from HostFailWorker instantiation.
        // Since FailHostTask has a lot of runtime dependencies on other spawn components such as 
        // SpawnBalancer, it's safer to start as late in the spawn init cycle as possible.
        hostFailWorker.initFailHostTaskSchedule();
        // start JobAlertManager
        jobAlertManager = new JobAlertManagerImpl(this, scheduledExecutor);
        // start job scheduler
        scheduledExecutor.scheduleWithFixedDelay(new UpdateEventRunnable(this), 0, 1, TimeUnit.MINUTES);
        scheduledExecutor.scheduleWithFixedDelay(new JobRekickTask(this), 0, 500, MILLISECONDS);
        scheduledExecutor.scheduleWithFixedDelay(this::drainJobTaskUpdateQueue,
                                                 taskQueueDrainInterval, taskQueueDrainInterval, MILLISECONDS);
        scheduledExecutor.scheduleWithFixedDelay(this::jobTaskUpdateHeartbeatCheck,
                                                 jobTaskUpdateHeartbeatInterval, jobTaskUpdateHeartbeatInterval,
                                                 MILLISECONDS);
        // request hosts to send their status
        scheduledExecutor.scheduleWithFixedDelay(this::requestHostsUpdate,
                                                 hostStatusRequestInterval, hostStatusRequestInterval, MILLISECONDS);
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            kickJobsOnQueue();
            writeSpawnQueue();
        }, queueKickInterval, queueKickInterval, MILLISECONDS);
        balancer.startAutobalanceTask();
        balancer.startTaskSizePolling();
        this.jobStore = jobStore;
        this.eventLog = new RollingLog(new File(logDir, "events-jobs"), "job",
                                       eventLogCompress, logMaxSize, logMaxAge);
        Metrics.newGauge(Spawn.class, "minionsDown", new DownMinionGauge(hostManager));
        writeState();
    }

    public void startWebInterface() throws Exception {
        spawnService = new SpawnService(this, SpawnServiceConfiguration.SINGLETON);
        spawnService.start();
    }

    void writeState() {
        try {
            LessFiles.write(stateFile, CodecJSON.INSTANCE.encode(spawnState), false);
        } catch (Exception e) {
            log.warn("Failed to write spawn state to log file at {}", stateFile, e);
        }
    }

    @Nonnull
    public HostFailWorker getHostFailWorker() {
        return hostFailWorker;
    }

    public SpawnBalancer getSpawnBalancer() {
        return balancer;
    }

    @Nonnull
    public PermissionsManager getPermissionsManager() { return permissionsManager; }

    @Nonnull
    public AliasManager getAliasManager() {
        return aliasManager;
    }
    
    @Nonnull
    public JobAlertManager getJobAlertManager() {
        return jobAlertManager;
    }
    
    @Nonnull
    public JobEntityManager<JobMacro> getJobMacroManager() {
        return jobMacroManager;
    }
    
    @Nonnull
    public JobEntityManager<JobCommand> getJobCommandManager() {
        return jobCommandManager;
    }
    
    @Nonnull
    public SystemManager getSystemManager() {
        return systemManager;
    }

    public void acquireJobLock() {
        jobLock.lock();
    }

    public void releaseJobLock() {
        jobLock.unlock();
    }

    public String getUuid() {
        return spawnState.uuid;
    }

    private void closeZkClients() {
        spawnDataStore.close();
        zkClient.close();
    }

    public void setSpawnMQ(SpawnMQ spawnMQ) {
        this.spawnMQ = spawnMQ;
    }

    @VisibleForTesting
    protected void loadSpawnQueue() throws Exception {
        String queueFromZk = spawnDataStore.get(SPAWN_QUEUE_PATH);
        if (queueFromZk == null) {
            return;
        }
        try {
            taskQueuesByPriority = new ObjectMapper().readValue(queueFromZk, SpawnQueuesByPriority.class);
        } catch (Exception ex) {
            log.warn("[task.queue] exception during spawn queue deserialization: ", ex);
        }
    }

    protected void writeSpawnQueue() {
        ObjectMapper om = new ObjectMapper();
        try {
            taskQueuesByPriority.lock();
            try {
                spawnDataStore.put(SPAWN_QUEUE_PATH, new String(om.writeValueAsBytes(taskQueuesByPriority)));
            } finally {
                taskQueuesByPriority.unlock();
            }
        } catch (Exception ex) {
            log.warn("[task.queue] exception during spawn queue serialization", ex);
        }
    }

    @VisibleForTesting
    protected void loadJobs() {
        jobLock.lock();
        try {
            for (IJob iJob : jobConfigManager.getJobs().values()) {
                if (iJob != null) {
                    putJobInSpawnState(new Job(iJob));
                }
            }
        } finally {
            jobLock.unlock();
        }
        Thread loadDependencies = new Thread(() -> {
            Set<String> jobIds = spawnState.jobs.keySet();
            for (String jobId : jobIds) {
                IJob job = getJob(jobId);
                if (job != null) {
                    updateJobDependencies(jobId);
                }
            }
        }, "spawn job dependency calculator");
        loadDependencies.setDaemon(true);
        loadDependencies.start();
    }

    // -------------------- BEGIN API ---------------------

    public ClientEventListener getClientEventListener(String id) {
        ClientEventListener listener = listeners.get(id);
        if (listener == null) {
            listener = new ClientEventListener();
            listeners.put(id, listener);
        }
        listener.lastSeen = System.currentTimeMillis();
        return listener;
    }

    public HostState markHostStateDead(String hostUUID) {
        HostState state = hostManager.getHostState(hostUUID);
        if (state != null) {
            state.setDead(true);
            state.setUpdated();
            // delete minion state
            spawnDataStore.delete(Minion.MINION_ZK_PATH + hostUUID);
            try {
                zkClient.create().creatingParentsIfNeeded().forPath(MINION_DEAD_PATH + "/" + hostUUID, null);
            } catch (KeeperException.NodeExistsException ne) {
                // host already marked as dead
            } catch (Exception e) {
                log.error("Unable to add host: {} to " + MINION_DEAD_PATH, hostUUID, e);
            }
            sendHostUpdateEvent(state);
            hostManager.updateHostState(state);
        }
        return state;
    }

    public Collection<String> listAvailableHostIds() {
        return hostManager.minionMembers.getMemberSet();
    }

    public void requestHostsUpdate() {
        try {
            spawnMQ.sendControlMessage(new HostState(HostMessage.ALL_HOSTS));
        } catch (Exception e) {
            log.warn("unable to request host state update: ", e);
        }
    }

    public Set<String> getDataSources(String jobId) {
        HashSet<String> dataSources = new HashSet<>();
        Job job = this.getJob(jobId);
        if (job == null || job.getParameters() == null) {
            return dataSources;
        }
        jobLock.lock();
        try {
            for (JobParameter param : job.getParameters()) {
                String value = param.getValue();
                if (LessStrings.isEmpty(value)) {
                    value = param.getDefaultValue();
                }
                if (value != null) {
                    try {
                        value = JobExpand.macroExpand(this, value);
                    } catch (TokenReplacerOverflowException ex) {
                        log.error("Token replacement overflow for input '{}'", value);
                    }
                }
                if (value != null && spawnState.jobs.containsKey(value)) {
                    dataSources.add(value);
                }
            }
        } finally {
            jobLock.unlock();
        }
        return dataSources;
    }

    public DirectedGraph<String> getJobDependencies() {
        return spawnState.jobDependencies;
    }

    /**
     * Gets the backup times for a given job and node of all backup types by using MeshyClient. If the nodeId is -1 it will
     * get the backup times for all nodes.
     *
     * @return Set of date time mapped by backup type in reverse chronological order
     * @throws IOException thrown if mesh client times out, ParseException thrown if filename does not meet valid format
     */
    public Map<ScheduledBackupType, SortedSet<Long>> getJobBackups(String jobUUID, int nodeId) throws IOException, ParseException {
        Map<ScheduledBackupType, SortedSet<Long>> fileDates = new HashMap<>();
        for (ScheduledBackupType backupType : ScheduledBackupType.getBackupTypes().values()) {
            final String typePrefix = "*/" + jobUUID + "/" + ((nodeId < 0) ? "*" : Integer.toString(nodeId)) + "/" + backupType.getPrefix() + "*";
            List<FileReference> files = new ArrayList<>(spawnMesh.getClient().listFiles(new String[]{typePrefix}));
            fileDates.put(backupType, new TreeSet<>(Collections.reverseOrder()));
            for (FileReference file : files) {
                String filename = file.name.split("/")[4];
                fileDates.get(backupType).add(backupType.parseDateFromName(filename).getTime());
            }
        }
        return fileDates;
    }

    public boolean isSpawnMeshAvailable() {
        return spawnMesh.getClient() != null;
    }

    public void deleteHost(String hostuuid) {
        HostFailWorker.FailState failState = hostFailWorker.getFailureState(hostuuid);
        if (failState == HostFailWorker.FailState.FAILING_FS_DEAD || failState == HostFailWorker.FailState.FAILING_FS_OKAY) {
            log.warn("Refused to drop host because it was in the process of being failed {}", hostuuid);
            throw new RuntimeException("Cannot drop a host that is in the process of being failed");
        }
        synchronized (hostManager.monitored) {
            HostState state = hostManager.monitored.remove(hostuuid);
            if (state != null) {
                log.info("Deleted host {}", hostuuid);
                sendHostUpdateEvent("host.delete", state);
            } else {
                log.warn("Attempted to delete host {} But it was not found", hostuuid);
            }
        }
    }

    public Collection<Job> listJobs() {
        ArrayList<Job> clones = new ArrayList<>(spawnState.jobs.size());
        jobLock.lock();
        try {
            for (Job job : spawnState.jobs.values()) {
                clones.add(job);
            }
            return clones;
        } finally {
            jobLock.unlock();
        }
    }

    public Collection<Job> listJobsConcurrentImmutable() {
        return Collections.unmodifiableCollection(spawnState.jobs.values());
    }

    public int getTaskQueuedCount() {
        return lastQueueSize;
    }

    public Job getJob(String jobUUID) {
        if (jobUUID == null) {
            return null;
        }
        jobLock.lock();
        try {
            return spawnState.jobs.get(jobUUID);
        } finally {
            jobLock.unlock();
        }
    }

    public void setJobConfig(String jobUUID, String config) throws Exception {
        jobConfigManager.setConfig(jobUUID, config);
    }

    public String getJobConfig(String jobUUID) {
        if (jobUUID == null) {
            return null;
        }
        jobLock.lock();
        try {
            return jobConfigManager.getConfig(jobUUID);
        } finally {
            jobLock.unlock();
        }
    }

    public Job putJobInSpawnState(Job job) {
        if (job == null) {
            return null;
        }
        // Null out the job config before inserting to reduce the amount stored in memory.
        // Calling getJob will fill it back in -- or call jobConfigManager.getConfig(id)
        job.setConfig(null);
        return spawnState.jobs.put(job.getId(), job);
    }

    public Job getJob(JobKey jobKey) {
        String jobUUID = jobKey.getJobUuid();
        return getJob(jobUUID);
    }

    public JSONArray getJobHistory(String jobId) {
        return jobStore != null ? jobStore.getHistory(jobId) : new JSONArray();
    }

    public String getJobHistoricalConfig(String jobId, String commitId) {
        return jobStore != null ? jobStore.fetchHistoricalConfig(jobId, commitId) : null;
    }

    public String diff(String jobId, String commitId) {
        return jobStore != null ? jobStore.getDiff(jobId, commitId) : null;
    }
    
    public String getDeletedJobConfig(String jobId) throws Exception {
        requireJobStore();
        return jobStore.getDeletedJobConfig(jobId);
    }

    private void requireJobStore() throws Exception {
        if (jobStore == null) {
            throw new Exception("Job history is disabled.");
        }
    }

    public Job createJob(String creator, int taskCount, Collection<String> taskHosts,
                         String minionType, String command, boolean defaults) throws Exception {
        jobLock.lock();
        try {
            Job job = new Job(UUID.randomUUID().toString(), creator);
            job.setMinionType(minionType);
            job.setCommand(command);
            job.setState(JobState.IDLE);
            if (defaults) {
                job.setOwnerWritable(jobDefaults.ownerWritable);
                job.setGroupWritable(jobDefaults.groupWritable);
                job.setWorldWritable(jobDefaults.worldWritable);
                job.setOwnerExecutable(jobDefaults.ownerExecutable);
                job.setGroupExecutable(jobDefaults.groupExecutable);
                job.setWorldExecutable(jobDefaults.worldExecutable);
                job.setDailyBackups(jobDefaults.dailyBackups);
                job.setWeeklyBackups(jobDefaults.weeklyBackups);
                job.setMonthlyBackups(jobDefaults.monthlyBackups);
                job.setHourlyBackups(jobDefaults.hourlyBackups);
                job.setReplicas(jobDefaults.replicas);
                job.setAutoRetry(jobDefaults.autoRetry);
            }
            List<HostState> hostStates = getOrCreateHostStateList(minionType, taskHosts);
            List<JobTask> tasksAssignedToHosts = balancer.generateAssignedTasksForNewJob(job.getId(), taskCount, hostStates);
            job.setTasks(tasksAssignedToHosts);
            for (JobTask task : tasksAssignedToHosts) {
                HostState host = hostManager.getHostState(task.getHostUUID());
                if (host == null) {
                    throw new Exception("Unable to allocate job tasks because no suitable host was found");
                }
                host.addJob(job.getId());
            }
            putJobInSpawnState(job);
            jobConfigManager.addJob(job);
            submitConfigUpdate(job.getId(), creator, null);
            return job;
        } finally {
            jobLock.unlock();
        }
    }

    public Response synchronizeJobState(String jobUUID, String user,
                                       String token, String sudo) {
        if (jobUUID == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{error:\"missing id parameter\"}").build();
        }
        if (jobUUID.equals("ALL")) {
            if (!getPermissionsManager().adminAction(user, token, sudo)) {
                return Response.status(Response.Status.UNAUTHORIZED)
                               .entity("{error:\"insufficient priviledges\"}").build();
            }
            Collection<Job> jobList = listJobs();
            for (Job job : jobList) {
                Response status = synchronizeSingleJob(job.getId(), user, token, sudo);
                if (status.getStatus() != 200) {
                    log.warn("Stopping synchronize all jobs to to failure synchronizing job: " + job.getId());
                    return status;
                }
            }
            return Response.ok("{id:'" + jobUUID + "',action:'synchronzied'}").build();
        } else {
            return synchronizeSingleJob(jobUUID, user, token, sudo);
        }
    }

    private Response synchronizeSingleJob(String jobUUID, String user, String token, String sudo) {
        Job job = getJob(jobUUID);
        if (job == null) {
            log.warn("[job.synchronize] job uuid {} not found", jobUUID);
            return Response.status(Response.Status.NOT_FOUND).entity("job " + jobUUID + " not found").build();
        } else if (!permissionsManager.isExecutable(user, token, sudo, job)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{error:\"insufficient priviledges\"}").build();
        }
        ObjectMapper mapper = new ObjectMapper();
        for (JobTask task : job.getCopyOfTasks()) {
            String taskHost = task.getHostUUID();
            if (hostManager.deadMinionMembers.getMemberSet().contains(taskHost)) {
                log.warn("task is currently assigned to a dead minion, need to check job: {} host/node:{}/{}",
                         job.getId(), task.getHostUUID(), task.getTaskID());
                continue;
            }
            String hostStateString;
            try {
                hostStateString = StringSerializer.deserialize(zkClient.getData().forPath(Minion.MINION_ZK_PATH + taskHost));
            } catch (Exception e) {
                log.error("Unable to get hostStateString from zookeeper for " + Minion.MINION_ZK_PATH + taskHost, e);
                continue;
            }
            HostState hostState;
            try {
                hostState = mapper.readValue(hostStateString, HostState.class);
            } catch (IOException e) {
                log.warn("Unable to deserialize host state for host: " + hostStateString + " serialized string was\n" + hostStateString);
                return Response.serverError().entity("Serialization error").build();
            }
            boolean matched = matchJobNodeAndId(jobUUID, task, hostState.getRunning(), hostState.getStopped(), hostState.getQueued());
            if (!matched) {
                log.warn("Spawn thinks job: " + jobUUID + " node:" + task.getTaskID() + " is running on host: " + hostState.getHost() + " but that host disagrees.");
                if (matchJobNodeAndId(jobUUID, task, hostState.getReplicas())) {
                    log.warn("Host: " + hostState.getHost() + " has a replica for the task/node: " + jobUUID + "/" + task.getTaskID() + " promoting replica");
                    try {
                        rebalanceReplicas(job);
                    } catch (Exception e) {
                        log.warn("Exception promoting replica during job synchronization on host: " + taskHost + " job/node" + job.getId() + "/" + job.getId());
                    }
                } else {
                    log.warn("Host: " + hostState.getHost() + " does NOT have a replica for the task/node: " + jobUUID + "/" + task.getTaskID());
                }
            } else {
                log.warn("Spawn and minion agree, job/node: " + jobUUID + "/" + task.getTaskID() + " is on host: " + hostState.getHost());
            }
        }
        return Response.ok().entity("success").build();
    }

    private static boolean matchJobNodeAndId(String jobUUID, JobTask task, JobKey[]... jobKeys) {
        for (JobKey[] jobKeyArray : jobKeys) {
            for (JobKey jobKey : jobKeyArray) {
                if (jobKey == null) {
                    log.warn("runningJob was null, this shouldn't happen");
                    continue;
                } else if (jobKey.getJobUuid() == null) {
                    log.warn("JobUUID for jobKey: " + jobKey + " was null");
                    continue;
                } else if (jobKey.getNodeNumber() == null) {
                    log.warn("NodeNumber for jobKey: " + jobKey + " was null");
                    continue;
                }
                if (jobKey.getJobUuid().equals(jobUUID) && jobKey.getNodeNumber().equals(task.getTaskID())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Reallocate some of a job's tasks to different hosts, hopefully improving its performance.
     *
     * @param jobUUID     The ID of the job
     * @param tasksToMove The number of tasks to move. If <= 0, use the default.
     * @return a list of move assignments that were attempted
     */
    public List<JobTaskMoveAssignment> reallocateJob(String jobUUID, int tasksToMove) {
        Job job;
        if (jobUUID == null || (job = getJob(jobUUID)) == null) {
            throw new NullPointerException("invalid job uuid");
        }
        if (job.getState() != JobState.IDLE) {
            log.warn("[job.reallocate] can't reallocate non-idle job");
            return Collections.emptyList();
        }
        List<JobTaskMoveAssignment> assignments = balancer.getAssignmentsForJobReallocation(job, tasksToMove,
                                                                                            hostManager.getLiveHosts(
                                                                                                    job.getMinionType()));
        return executeReallocationAssignments(assignments, false);
    }

    /**
     * Promote a task to live on one of its replica hosts, demoting the existing live to a replica.
     *
     * @param task           The task to modify
     * @param replicaHostID  The host holding the replica that should be promoted
     * @param kickOnComplete Whether to kick the task after the move is complete
     * @return true on success
     */
    public boolean swapTask(JobTask task, String replicaHostID, boolean kickOnComplete) {
        if (task == null) {
            log.warn("[task.swap] received null task");
            return false;
        }
        if (!checkHostStatesForSwap(task.getJobKey(), task.getHostUUID(), replicaHostID, true)) {
            log.warn("[swap.task.stopped] failed for {}; exiting", task.getJobKey());
            return false;
        }
        Job job;
        jobLock.lock();
        try {
            job = getJob(task.getJobUUID());
            task.replaceReplica(replicaHostID, task.getHostUUID());
            task.setHostUUID(replicaHostID);
            queueJobTaskUpdateEvent(job);
        } finally {
            jobLock.unlock();
        }
        if (kickOnComplete) {
            try {
                scheduleTask(job, task, expandJob(job));
            } catch (Exception e) {
                log.warn("Warning: failed to kick task {} with: {}", task.getJobKey(), e, e);
                job.errorTask(task, JobTaskErrorCode.KICK_ERROR);
            }
        }
        return true;
    }

    /**
     * Get a replacement host for a new task
     *
     * @param job The job for the task to be reassigned
     * @return A replacement host ID, if one can be found; null otherwise
     */
    private String getReplacementHost(Job job) {
        List<HostState> hosts = hostManager.getLiveHosts(job.getMinionType());
        for (HostState host : hosts) {
            if (host.canMirrorTasks()) {
                return host.getHostUuid();
            }
        }
        return null;
    }

    /**
     * Given a new task, replace any hosts that are down/disabled to ensure that it can kick
     *
     * @param task The task to modify
     * @return True if at least one host was removed
     */
    private boolean replaceDownHosts(JobTask task) {
        checkArgument(isNewTask(task), "%s is not a new task, and so this method is not safe to call", task);
        Job job = getJob(task.getJobKey());
        if (job == null) {
            return false;
        }
        HostState host = hostManager.getHostState(task.getHostUUID());
        boolean changed = false;
        if (host == null || !host.canMirrorTasks()) {
            String replacementHost = getReplacementHost(job);
            if (replacementHost != null) {
                task.setHostUUID(replacementHost);
                changed = true;
            }
        }
        if (task.getReplicas() != null) {
            List<JobTaskReplica> tempReplicas = new ArrayList<>(task.getReplicas());
            for (JobTaskReplica replica : tempReplicas) {
                HostState replicaHost = hostManager.getHostState(replica.getHostUUID());
                if (replicaHost == null || !replicaHost.canMirrorTasks()) {
                    changed = true;
                    task.setReplicas(removeReplicasForHost(replica.getHostUUID(), task.getReplicas()));
                }
            }
        }
        if (changed) {
            try {
                updateJob(job);
            } catch (Exception ex) {
                log.warn("Failed to sent replication message for new task " + task.getJobKey() + ": " + ex, ex);
                return false;
            }
        }
        return changed;

    }

    /**
     * Check whether it is acceptable to swap a task between two hosts
     *
     * @param key           The task to consider swapping
     * @param liveHostID    The current host for the task
     * @param replicaHostID The potential target host to check
     * @return True if both hosts are up and have the appropriate task directory
     */
    private boolean checkHostStatesForSwap(JobKey key, String liveHostID, String replicaHostID, boolean checkTargetReplica) {
        if (key == null || liveHostID == null || replicaHostID == null) {
            log.warn("[task.swap] failed due to null input");
            return false;
        }
        JobTask task = getTask(key.getJobUuid(), key.getNodeNumber());
        if (task == null) {
            log.warn("[task.swap] failed: nonexistent task/replicas");
            return false;
        }
        HostState liveHost = hostManager.getHostState(liveHostID);
        HostState replicaHost = hostManager.getHostState(replicaHostID);
        if (liveHost == null || replicaHost == null || liveHost.isDead() || !liveHost.isUp() || replicaHost.isDead() || !replicaHost.isUp()) {
            log.warn("[task.swap] failed due to invalid host states for " + liveHostID + "," + replicaHostID);
            return false;
        }
        if (checkTargetReplica && !isNewTask(task)) {
            if (!replicaHost.hasLive(key)) {
                log.warn("[task.swap] failed because the replica host " + replicaHostID + " does not have a complete replica of task " + key);
                return false;
            }
        }
        return true;
    }

    /**
     * Push or pull tasks off of a host to balance its load with the rest of the cluster.
     *
     * @param hostUUID The ID of the host
     * @return a boolean describing if at least one task was scheduled to be moved
     */
    public RebalanceOutcome rebalanceHost(String hostUUID) {
        HostState host = hostManager.getHostState(hostUUID);
        if (host == null) {
            return new RebalanceOutcome(hostUUID, "missing host", null, null);
        }
        log.info("[job.reallocate] starting reallocation for host: {} host is not a read only host", hostUUID);
        List<JobTaskMoveAssignment> assignments = balancer.getAssignmentsToBalanceHost(host,
                                                                                       hostManager.getLiveHosts(null));
        return new RebalanceOutcome(hostUUID, null, null,
                                    LessStrings.join(executeReallocationAssignments(assignments, false).toArray(), "\n"));
    }

    /**
     * Sanity-check a series of task move assignments coming from SpawnBalancer, then execute the sensible ones.
     *
     * @param assignments The assignments to execute
     * @param limitToAvailableSlots Whether movements should honor their host's availableTaskSlots count
     * @return The number of tasks that were actually moved
     */
    public List<JobTaskMoveAssignment> executeReallocationAssignments(List<JobTaskMoveAssignment> assignments, boolean limitToAvailableSlots) {
        List<JobTaskMoveAssignment> executedAssignments = new ArrayList<>();
        if (assignments == null) {
            return executedAssignments;
        }
        HashSet<String> jobsNeedingUpdate = new HashSet<>();
        HashSet<String> hostsAlreadyMovingTasks = new HashSet<>();
        for (JobTaskMoveAssignment assignment : assignments) {
            if (assignment.delete()) {
                log.warn("[job.reallocate] deleting " + assignment.getJobKey() + " off " + assignment.getSourceUUID());
                deleteTask(assignment.getJobKey().getJobUuid(), assignment.getSourceUUID(), assignment.getJobKey().getNodeNumber(), false);
                deleteTask(assignment.getJobKey().getJobUuid(), assignment.getSourceUUID(), assignment.getJobKey().getNodeNumber(), true);
                executedAssignments.add(assignment);
            } else {
                String sourceHostID = assignment.getSourceUUID();
                String targetHostID = assignment.getTargetUUID();
                HostState targetHost = hostManager.getHostState(targetHostID);
                if (sourceHostID == null || targetHostID == null || sourceHostID.equals(targetHostID) || targetHost == null) {
                    log.warn("[job.reallocate] received invalid host assignment: from " + sourceHostID + " to " + targetHostID);
                    continue;
                }
                JobKey key = assignment.getJobKey();
                JobTask task = getTask(key);
                Job job = getJob(key);
                if (job == null || task == null) {
                    log.warn("[job.reallocate] invalid job or task");
                    // Continue with the next assignment
                }
                else {
                    HostState liveHost = hostManager.getHostState(task.getHostUUID());
                    if (limitToAvailableSlots && liveHost != null && (liveHost.getAvailableTaskSlots() == 0 || hostsAlreadyMovingTasks.contains(task.getHostUUID()))) {
                        continue;
                    }
                    log.warn("[job.reallocate] replicating task " + key + " onto " + targetHostID + " as " + (assignment.isFromReplica() ? "replica" : "live"));
                    TaskMover tm = new TaskMover(this, hostManager, key, targetHostID, sourceHostID);
                    if (tm.execute()) {
                        hostsAlreadyMovingTasks.add(task.getHostUUID());
                        executedAssignments.add(assignment);
                    }
                }
            }
        }
        for (String jobUUID : jobsNeedingUpdate) {
            try {
                updateJob(getJob(jobUUID));
            } catch (Exception ex) {
                log.warn("WARNING: failed to update job " + jobUUID + ": " + ex, ex);
            }
        }
        return executedAssignments;
    }

    /**
     * A method to ensure all live/replicas exist where they should, and optimize their locations if all directories are correct
     *
     * @param jobUUID     The job id to rebalance
     * @param tasksToMove The number of tasks to move. If < 0, use the default.
     * @return a RebalanceOutcome describing which steps were performed
     * @throws Exception If there is a failure when rebalancing replicas
     */
    public RebalanceOutcome rebalanceJob(String jobUUID, int tasksToMove, String user,
                                         String token, String sudo) throws Exception {
        Job job = getJob(jobUUID);
        if (jobUUID == null || job == null) {
            log.warn("[job.rebalance] job uuid " + jobUUID + " not found");
            return new RebalanceOutcome(jobUUID, "job not found", null, null);
        }
        if (permissionsManager.isExecutable(user, token, sudo, job)) {
            log.warn("[job.rebalance] insufficient priviledges to rebalance " + jobUUID);
            return new RebalanceOutcome(jobUUID, "insufficient priviledges", null, null);
        }
        if (job.getState() != JobState.IDLE && job.getState() != JobState.DEGRADED) {
            log.warn("[job.rebalance] job must be IDLE or DEGRADED to rebalance " + jobUUID);
            return new RebalanceOutcome(jobUUID, "job not idle/degraded", null, null);
        }
        // First, make sure each task has claimed all the replicas it should have
        if (!rebalanceReplicas(job)) {
            log.warn("[job.rebalance] failed to fill out replica assignments for " + jobUUID);
            return new RebalanceOutcome(jobUUID, "couldn't fill out replicas", null, null);
        }
        try {
            List<JobTaskDirectoryMatch> allMismatches = new ArrayList<>();
            // Check each task to see if any live/replica directories are missing or incorrectly placed
            for (JobTask task : job.getCopyOfTasks()) {
                List<JobTaskDirectoryMatch> directoryMismatches = matchTaskToDirectories(task, false);
                if (!directoryMismatches.isEmpty()) {
                    // If there are issues with a task's directories, resolve them.
                    resolveJobTaskDirectoryMatches(task, false);
                    allMismatches.addAll(directoryMismatches);
                }
            }
            updateJob(job);
            // If any mismatches were found, skip the optimization step
            if (!allMismatches.isEmpty()) {
                return new RebalanceOutcome(jobUUID, null, LessStrings.join(allMismatches.toArray(), "\n"), null);
            } else {
                // If all tasks had all expected directories, consider moving some tasks to better hosts
                return new RebalanceOutcome(jobUUID, null, null, LessStrings.join(
                        reallocateJob(jobUUID, tasksToMove).toArray(), "\n"));
            }
        } catch (Exception ex) {
            log.warn("[job.rebalance] exception during rebalance for " + jobUUID, ex);
            return new RebalanceOutcome(jobUUID, "exception during rebalancing: " + ex, null, null);
        }

    }

    /**
     * For a particular task, ensure all live/replica copies exist where they should
     *
     * @param jobId           The job id to fix
     * @param node            The task id to fix, or -1 to fix all
     * @param ignoreTaskState Whether to ignore the task's state (mostly when recovering from a host failure)
     * @param orphansOnly     Whether to only delete orphans for idle tasks
     * @return A string description
     */
    public JSONObject fixTaskDir(String jobId, int node, boolean ignoreTaskState, boolean orphansOnly) {
        jobLock.lock();
        try {
            Job job = getJob(jobId);
            int numChanged = 0;
            if (job != null) {
                List<JobTask> tasks = node < 0 ? job.getCopyOfTasks() : Arrays.asList(job.getTask(node));
                for (JobTask task : tasks) {
                    boolean shouldModifyTask = !spawnJobFixer.haveRecentlyFixedTask(task.getJobKey()) &&
                                               (ignoreTaskState
                                                || (task.getState() == JobTaskState.IDLE)
                                                || (!orphansOnly && (task.getState() == JobTaskState.ERROR)));
                    if (log.isDebugEnabled()) {
                        log.debug("[fixTaskDir] considering modifying task " + task.getJobKey() + " shouldModifyTask=" + shouldModifyTask);
                    }
                    if (shouldModifyTask) {
                        try {
                            numChanged += resolveJobTaskDirectoryMatches(task, orphansOnly) ? 1 : 0;
                            spawnJobFixer.markTaskRecentlyFixed(task.getJobKey());
                        } catch (Exception ex) {
                            log.warn("fixTaskDir exception " + ex, ex);
                        }
                    }
                }
            }
            return new JSONObject(ImmutableMap.of("tasksChanged", numChanged));
        } finally {
            jobLock.unlock();
        }

    }

    /**
     * Go through the hosts in the cluster, making sure that a task has copies everywhere it should and doesn't have orphans living elsewhere
     * @param task The task to examine
     * @param deleteOrphansOnly Whether to ignore missing copies and only delete orphans
     * @return True if the task was changed
     */
    public boolean resolveJobTaskDirectoryMatches(JobTask task, boolean deleteOrphansOnly) {
        Set<String> expectedHostsWithTask = new HashSet<>();
        Set<String> expectedHostsMissingTask = new HashSet<>();
        Set<String> unexpectedHostsWithTask = new HashSet<>();
        for (HostState host : hostManager.listHostStatus(null)) {
            if (hostSuitableForReplica(host)) {
                String hostId = host.getHostUuid();
                if (hostId.equals(task.getHostUUID()) || task.hasReplicaOnHost(hostId)) {
                    if (host.hasLive(task.getJobKey())) {
                        expectedHostsWithTask.add(hostId);
                    } else {
                        expectedHostsMissingTask.add(hostId);
                    }
                } else if (host.hasLive(task.getJobKey()) || host.hasIncompleteReplica(task.getJobKey())) {
                    unexpectedHostsWithTask.add(hostId);
                }
            }
        }
        log.trace("fixTaskDirs found expectedWithTask {} expectedMissingTask {} unexpectedWithTask {} ",
                  expectedHostsWithTask, expectedHostsMissingTask, unexpectedHostsWithTask);
        if (deleteOrphansOnly) {
            // If we're only deleting orphans, ignore any expected hosts missing the task
            expectedHostsMissingTask = new HashSet<>();
        }
        return performTaskFixes(task, expectedHostsWithTask, expectedHostsMissingTask, unexpectedHostsWithTask);
    }

    private boolean performTaskFixes(JobTask task, Set<String> expectedHostsWithTask, Set<String> expectedHostsMissingTask, Set<String> unexpectedHostsWithTask) {
        if (expectedHostsWithTask.isEmpty()) {
            // No copies of the task were found on the expected live/replica hosts. Attempt to recover other copies from the cluster.
            if (unexpectedHostsWithTask.isEmpty()) {
                // No copies of the task were found anywhere in the cluster. Have to recreate it.
                log.warn("No copies of {} were found. Recreating it on new hosts. ", task.getJobKey());
                recreateTask(task);
                return true;
            }
            // Found at least one host with data. Iterate through the hosts with data; first host becomes live, any others become replicas
            Iterator<String> unexpectedHostsIter = unexpectedHostsWithTask.iterator();
            List<JobTaskReplica> newReplicas = new ArrayList<>();
            task.setHostUUID(unexpectedHostsIter.next());
            while (unexpectedHostsIter.hasNext()) {
                newReplicas.add(new JobTaskReplica(unexpectedHostsIter.next(), task.getJobUUID(), 0, 0));
            }
            task.setReplicas(newReplicas);
            return true;
        } else {
            // Found copies of task on expected hosts. Copy to any hosts missing the data, and delete from any unexpected hosts
            boolean changed = false;
            if (!expectedHostsMissingTask.isEmpty()) {
                swapTask(task, expectedHostsWithTask.iterator().next(), false);
                copyTaskToReplicas(task);
                changed = true;
            }
            for (String unexpectedHost : unexpectedHostsWithTask) {
                deleteTask(task.getJobUUID(), unexpectedHost, task.getTaskID(), false);
            }
            return changed;
        }
    }

    private static boolean hostSuitableForReplica(HostState host) {
        return host != null && host.isUp() && !host.isDead();
    }

    private void copyTaskToReplicas(JobTask task) {
        sendControlMessage(new CommandTaskReplicate(task.getHostUUID(), task.getJobUUID(), task.getTaskID(), getTaskReplicaTargets(task, task.getReplicas()), null, null, false, false));
    }

    private void recreateTask(JobTask task) {
        Job job = getJob(task.getJobUUID());
        Map<JobTask, String> assignmentMap = balancer.assignTasksFromMultipleJobsToHosts(Arrays.asList(task), getOrCreateHostStateList(job.getMinionType(), null));
        if (assignmentMap != null && assignmentMap.containsKey(task)) {
            String newHostUUID = assignmentMap.get(task);
            log.warn("[job.rebalance] assigning new host for " + task.getJobUUID() + ":" + task.getTaskID() + " all data on previous host will be lost");
            task.setHostUUID(newHostUUID);
            task.resetTaskMetrics();
        } else {
            log.warn("[job.rebalance] unable to assign new host for " + task.getJobUUID() + ":" + task.getTaskID() + " could not find suitable host");
        }
    }

    public JSONArray checkTaskDirJSON(String jobId, int node) {
        JSONArray resultList = new JSONArray();
        jobLock.lock();
        try {
            Job job = getJob(jobId);
            if (job == null) {
                return resultList;
            }
            List<JobTask> tasks = node < 0 ? new ArrayList<>(job.getCopyOfTasksSorted()) : Arrays.asList(job.getTask(node));
            for (JobTask task : tasks) {
                List<JobTaskDirectoryMatch> taskMatches = matchTaskToDirectories(task, true);
                for (JobTaskDirectoryMatch taskMatch : taskMatches) {
                    JSONObject jsonObject = CodecJSON.encodeJSON(taskMatch);
                    resultList.put(jsonObject);
                }
            }
        } catch (Exception ex) {
            log.warn("Error: checking dirs for job: " + jobId + ", node: " + node);
        } finally {
            jobLock.unlock();
        }
        return resultList;
    }

    public List<JobTaskDirectoryMatch> matchTaskToDirectories(JobTask task, boolean includeCorrect) {
        List<JobTaskDirectoryMatch> rv = new ArrayList<>();
        JobTaskDirectoryMatch match = checkHostForTask(task, task.getHostUUID());
        if (includeCorrect || match.getType() != JobTaskDirectoryMatch.MatchType.MATCH) {
            rv.add(match);
        }
        if (task.getAllReplicas() != null) {
            for (JobTaskReplica replica : task.getAllReplicas()) {
                match = checkHostForTask(task, replica.getHostUUID());
                if (match.getType() != JobTaskDirectoryMatch.MatchType.MATCH) {
                    if (task.getState() == JobTaskState.REPLICATE || task.getState() == JobTaskState.FULL_REPLICATE) {
                        // If task is replicating, it will temporarily look like it's missing on the target host. Make this visible to the UI.
                        rv.add(new JobTaskDirectoryMatch(JobTaskDirectoryMatch.MatchType.REPLICATE_IN_PROGRESS, match.getJobKey(), match.getHostId()));
                    } else {
                        rv.add(match);
                    }
                }
                else if (includeCorrect) {
                    rv.add(match);
                }
            }
        }
        rv.addAll(findOrphansForTask(task));
        return rv;
    }

    private JobTaskDirectoryMatch checkHostForTask(JobTask task, String hostID) {
        JobTaskDirectoryMatch.MatchType type;
        HostState host = hostManager.getHostState(hostID);
        if (host == null || !host.hasLive(task.getJobKey())) {
            type = JobTaskDirectoryMatch.MatchType.MISMATCH_MISSING_LIVE;
        } else {
            type = JobTaskDirectoryMatch.MatchType.MATCH;
        }
        return new JobTaskDirectoryMatch(type, task.getJobKey(), hostID);
    }

    private List<JobTaskDirectoryMatch> findOrphansForTask(JobTask task) {
        List<JobTaskDirectoryMatch> rv = new ArrayList<>();
        Job job = getJob(task.getJobUUID());
        if (job == null) {
            log.warn("got find orphans request for missing job " + task.getJobUUID());
            return rv;
        }
        Set<String> expectedTaskHosts = task.getAllTaskHosts();
        for (HostState host : hostManager.listHostStatus(job.getMinionType())) {
            if (host == null || !host.isUp() || host.isDead() || host.getHostUuid().equals(task.getRebalanceTarget())) {
                continue;
            }
            if (!expectedTaskHosts.contains(host.getHostUuid())) {
                JobTaskDirectoryMatch.MatchType type = null;
                if (host.hasLive(task.getJobKey()) || host.hasIncompleteReplica(task.getJobKey())) {
                    type = JobTaskDirectoryMatch.MatchType.ORPHAN_LIVE;
                }
                if (type != null) {
                    rv.add(new JobTaskDirectoryMatch(type, task.getJobKey(), host.getHostUuid()));
                }
            }
        }
        return rv;
    }

    public boolean checkStatusForMove(String hostID) {
        HostState host = hostManager.getHostState(hostID);
        if (host == null) {
            log.warn("[host.status] received null host for id " + hostID);
            return false;
        }
        if (host.isDead() || !host.isUp()) {
            log.warn("[host.status] host is down: " + hostID);
            return false;
        }
        return true;
    }

    public boolean prepareTaskStatesForRebalance(Job job, JobTask task, boolean isMigration) {
        jobLock.lock();
        try {
            if (!balancer.isInMovableState(task)) {
                log.warn("[task.mover] decided not to move non-idle task " + task);
                return false;
            }
            JobTaskState newState = isMigration ? JobTaskState.MIGRATING : JobTaskState.REBALANCE;
            job.setTaskState(task, newState, true);
            queueJobTaskUpdateEvent(job);
            return true;
        } finally {
            jobLock.unlock();
        }
    }

    /**
     * exclude failed hosts from eligible pool
     * iterate over tasks
     * assemble hosts job spread across
     * count replicas per host
     * iterate over tasks and make reductions
     * iterate over tasks and make additions
     * exclude task host from replica
     * assign in order of least replicas per host
     * <p/>
     * TODO synchronize on job
     * TODO allow all cluster hosts to be considered for replicas
     * TODO consider host group "rack aware" keep 1/first replica in same group
     *
     * @return true if rebalance was successful
     */
    public boolean rebalanceReplicas(Job job) throws Exception {
        return rebalanceReplicas(job, -1);
    }

    /**
     * exclude failed hosts from eligible pool
     * iterate over tasks
     * assemble hosts job spread across
     * count replicas per host
     * iterate over tasks and make reductions
     * iterate over tasks and make additions
     * exclude task host from replica
     * assign in order of least replicas per host
     * <p/>
     * TODO synchronize on job
     * TODO allow all cluster hosts to be considered for replicas
     * TODO consider host group "rack aware" keep 1/first replica in same group
     *
     * @param job      the job to rebalance replicas
     * @param taskID   The task # to fill out replicas, or -1 for all tasks
     * @return true if rebalance was successful
     */
    public boolean rebalanceReplicas(Job job, int taskID) throws Exception {
        if (job == null) {
            return false;
        }
        // Ensure that there aren't any replicas pointing towards the live host or duplicate replicas
        balancer.removeInvalidReplicas(job);
        // Ask SpawnBalancer where new replicas should be sent
        Map<Integer, List<String>> replicaAssignments = balancer.getAssignmentsForNewReplicas(job, taskID);
        List<JobTask> tasks = taskID > 0 ? Arrays.asList(job.getTask(taskID)) : job.getCopyOfTasks();
        for (JobTask task : tasks) {
            List<String> replicasToAdd = replicaAssignments.get(task.getTaskID());
            // Make the new replicas as dictated by SpawnBalancer
            task.setReplicas(addReplicasAndRemoveExcess(task, replicasToAdd, job.getReplicas(), task.getReplicas()));
        }
        return validateReplicas(job);
    }

    /**
     * check all tasks. If there are still not enough replicas, record failure.
     *
     * @param job - the job to validate
     * @return true if the job has met its replica requirements
     */
    private boolean validateReplicas(Job job) {
        for (JobTask task : job.getCopyOfTasks()) {
            List<JobTaskReplica> replicas = task.getReplicas();
            if (job.getReplicas() > 0) {
                if ((replicas == null) || (replicas.size() < job.getReplicas())) {
                    HostState currHost = hostManager.getHostState(task.getHostUUID());
                    // If current host is dead and there are no replicas, mark degraded
                    if (((currHost == null) || currHost.isDead())
                        && ((replicas == null) || replicas.isEmpty())) {
                        job.setState(JobState.DEGRADED);
                    } else {
                        job.setState(JobState.ERROR); // Otherwise, just mark errored so we will know that at least on replica failed
                        job.setEnabled(false);
                    }
                    log.warn("[replica.add] ERROR - unable to replicate task because there are not enough suitable hosts, job: " + job.getId());
                    return false;
                }
            }
        }
        return true;
    }

    private List<JobTaskReplica> addReplicasAndRemoveExcess(JobTask task,
                                                            List<String> replicaHostsToAdd,
                                                            int desiredNumberOfReplicas,
                                                            List<JobTaskReplica> currentReplicas) throws Exception {
        List<JobTaskReplica> newReplicas;
        if (currentReplicas == null) {
            newReplicas = new ArrayList<>();
        } else {
            newReplicas = new ArrayList<>(currentReplicas);
        }
        if (replicaHostsToAdd != null) {
            newReplicas.addAll(replicateTask(task, replicaHostsToAdd));
        }
        if (!isNewTask(task)) {
            while (newReplicas.size() > desiredNumberOfReplicas) {
                JobTaskReplica replica = newReplicas.remove(newReplicas.size() - 1);
                spawnMQ.sendControlMessage(new CommandTaskDelete(replica.getHostUUID(), task.getJobUUID(), task.getTaskID(), task.getRunCount()));
                log.info("[replica.delete] " + task.getJobUUID() + "/" + task.getTaskID() + " from " + replica
                        .getHostUUID() + " @ " +
                         hostManager.getHostState(replica.getHostUUID()).getHost());
            }
        }
        return newReplicas;
    }

    protected List<JobTaskReplica> replicateTask(JobTask task, List<String> targetHosts) {
        List<JobTaskReplica> newReplicas = new ArrayList<>();
        for (String targetHostUUID : targetHosts) {
            JobTaskReplica replica = new JobTaskReplica();
            replica.setHostUUID(targetHostUUID);
            replica.setJobUUID(task.getJobUUID());
            newReplicas.add(replica);
        }
        Job job = getJob(task.getJobUUID());
        JobCommand jobcmd = getJobCommandManager().getEntity(job.getCommand());
        String command = (jobcmd != null && jobcmd.getCommand() != null) ? LessStrings.join(jobcmd.getCommand(), " ") : null;
        spawnMQ.sendControlMessage(new CommandTaskReplicate(task.getHostUUID(), task.getJobUUID(), task.getTaskID(), getTaskReplicaTargets(task, newReplicas), command, null, false, false));
        log.info("[replica.add] " + task.getJobUUID() + "/" + task.getTaskID() + " to " + targetHosts);
        taskQueuesByPriority.markHostTaskActive(task.getHostUUID());
        return newReplicas;
    }


    private void updateJobDependencies(String jobId) {
        DirectedGraph<String> dependencies = spawnState.jobDependencies;
        Set<String> sources = dependencies.getSourceEdges(jobId);
        if (sources != null) {
            for (String source : sources) {
                dependencies.removeEdge(source, jobId);
            }
        } else {
            dependencies.addNode(jobId);
        }
        Set<String> newSources = this.getDataSources(jobId);
        if (newSources != null) {
            for (String source : newSources) {
                dependencies.addEdge(source, jobId);
            }
        }
    }

    /**
     * Submit a config update to the job store
     *
     * @param jobId         The job to submit
     * @param commitMessage If specified, the commit message to use
     */
    public void submitConfigUpdate(String jobId, String user, String commitMessage) {
        Job job;
        if (jobId == null || jobId.isEmpty() || (job = getJob(jobId)) == null) {
            return;
        }
        if (jobStore != null) {
            jobStore.submitConfigUpdate(job.getId(), user, getJobConfig(jobId), commitMessage);
        }
    }

    public void updateJob(IJob ijob) throws Exception {
        updateJob(ijob, true);
    }

    /**
     * requires 'job' to be a different object from the one in cache.  make sure
     * to clone() any job fetched from cache before submitting to updateJob().
     */
    public void updateJob(IJob ijob, boolean reviseReplicas) throws Exception {
        Job job = new Job(ijob);
        jobLock.lock();
        try {
            checkArgument(getJob(job.getId()) != null, "job " + job.getId() + " does not exist");
            updateJobDependencies(job.getId());
            Job oldjob = putJobInSpawnState(job);
            // take action on trigger changes (like # replicas)
            if (oldjob != job && reviseReplicas) {
                int oldReplicaCount = oldjob.getReplicas();
                int newReplicaCount = job.getReplicas();
                checkArgument(oldReplicaCount == newReplicaCount || job.getState() == JobState.IDLE ||
                              job.getState() == JobState.DEGRADED, "job must be IDLE or DEGRADED to change replicas");
                checkArgument(newReplicaCount < hostManager.monitored.size(), "replication factor must be < # live hosts");
                rebalanceReplicas(job);
            }
            queueJobTaskUpdateEvent(job);
        } finally {
            jobLock.unlock();
        }
    }

    public DeleteStatus deleteJob(String jobUUID) throws Exception {
        jobLock.lock();
        try {
            Job job = getJob(jobUUID);
            if (job == null) {
                return DeleteStatus.JOB_MISSING;
            }
            if (job.getDontDeleteMe()) {
                return DeleteStatus.JOB_DO_NOT_DELETE;
            }
            spawnState.jobs.remove(jobUUID);
            spawnState.jobDependencies.removeNode(jobUUID);
            log.warn("[job.delete] {}", job.getId());
            if (spawnMQ != null) {
                spawnMQ.sendControlMessage(
                        new CommandTaskDelete(HostMessage.ALL_HOSTS, job.getId(), null, job.getRunCount()));
            }
            sendJobUpdateEvent("job.delete", job);
            jobConfigManager.deleteJob(job.getId());
            if (jobStore != null) {
                jobStore.delete(jobUUID);
            }
            Job.logJobEvent(job, JobEvent.DELETE, eventLog);
        } finally {
            jobLock.unlock();
        }
        jobAlertManager.removeAlertsForJob(jobUUID);
        return DeleteStatus.SUCCESS;
    }

    public void sendControlMessage(HostMessage hostMessage) {
        spawnMQ.sendControlMessage(hostMessage);
    }

    /**
     * Deletes a job only a specific host, useful when there are replicas and
     * a job has been migrated to another host
     *
     * @param jobUUID   The job to delete
     * @param hostUuid  The host where the delete message should be sent
     * @param node      The specific task to be deleted
     * @param isReplica Whether the task to be deleted is a replica or a live
     * @return True if the task is successfully removed
     */
    public boolean deleteTask(String jobUUID, String hostUuid, Integer node, boolean isReplica) {
        jobLock.lock();
        try {
            if (jobUUID == null || node == null) {
                return false;
            }
            log.warn("[job.delete.host] " + hostUuid + "/" + jobUUID + " >> " + node);
            spawnMQ.sendControlMessage(new CommandTaskDelete(hostUuid, jobUUID, node, 0));
            Job job = getJob(jobUUID);
            if (isReplica && job != null) {
                JobTask task = job.getTask(node);
                task.setReplicas(removeReplicasForHost(hostUuid, task.getReplicas()));
                queueJobTaskUpdateEvent(job);
            }
            return true;
        } finally {
            jobLock.unlock();
        }
    }

    private static List<JobTaskReplica> removeReplicasForHost(String hostUuid, List<JobTaskReplica> currentReplicas) {
        if (currentReplicas == null || currentReplicas.size() == 0) {
            return new ArrayList<>();
        }
        List<JobTaskReplica> replicasCopy = new ArrayList<>(currentReplicas);
        Iterator<JobTaskReplica> iterator = replicasCopy.iterator();
        while (iterator.hasNext()) {
            JobTaskReplica replica = iterator.next();
            if (replica.getHostUUID().equals(hostUuid)) {
                iterator.remove();
            }
        }
        return replicasCopy;
    }

    /**
     * The entry point for requests to start every task from a job (for example, from the UI.)
     *
     * @param jobUUID      Job ID
     * @param priority     immediacy of start request
     * @throws Exception
     */
    public void startJob(String jobUUID, int priority) throws Exception {
        Job job = getJob(jobUUID);
        checkArgument(job != null, "job not found");
        checkArgument(job.isEnabled(), "job disabled");
        checkArgument(scheduleJob(job, priority), "unable to schedule job");
        queueJobTaskUpdateEvent(job);
        Job.logJobEvent(job, JobEvent.START, eventLog);
    }

    public String expandJob(String jobUUID) throws Exception {
        Job job = getJob(jobUUID);
        checkArgument(job != null, "job not found");
        return expandJob(job);
    }

    public String expandJob(Job job) throws TokenReplacerOverflowException {
        return expandJob(job.getId(), job.getParameters(), getJobConfig(job.getId()));
    }

    public String expandJob(String id, Collection<JobParameter> parameters, String rawConfig)
            throws TokenReplacerOverflowException {
        // macro recursive expansion
        String pass0 = JobExpand.macroExpand(this, rawConfig);
        // template in params that "may" contain other macros
        String pass1 = JobExpand.macroTemplateParams(pass0, parameters);
        // macro recursive expansion again
        String pass2 = JobExpand.macroExpand(this, pass1);
        // replace remaining params not caught in pass 1
        String pass3 = JobExpand.macroTemplateParams(pass2, parameters);
        // inject job metadata from spawn
        return JobExpand.magicMacroExpand(this, pass3, id);
    }

    public void stopJob(String jobUUID) throws Exception {
        Job job = getJob(jobUUID);
        checkArgument(job != null, "job not found");
        for (JobTask task : job.getCopyOfTasks()) {
            if (task.getState() == JobTaskState.QUEUED) {
                removeFromQueue(task);
            }
            stopTask(jobUUID, task.getTaskID());
        }
        Job.logJobEvent(job, JobEvent.STOP, eventLog);
    }

    public void killJob(String jobUUID) throws Exception {
        boolean success = false;
        while (!success & !shuttingDown.get()) {
            try {
                jobLock.lock();
                if (taskQueuesByPriority.tryLock()) {
                    success = true;
                    Job job = getJob(jobUUID);
                    Job.logJobEvent(job, JobEvent.KILL, eventLog);
                    checkArgument(job != null, "job not found");
                    for (JobTask task : job.getCopyOfTasks()) {
                        if (task.getState() == JobTaskState.QUEUED) {
                            removeFromQueue(task);
                        }
                        killTask(jobUUID, task.getTaskID());
                    }
                }
            } finally {
                jobLock.unlock();
                if (success) {
                    taskQueuesByPriority.unlock();
                }
            }
        }
    }

    /**
     * not a clone like jobs, because there is no updater.
     * yes, there is no clean symmetry here.  it could use cleanup.
     */
    public JobTask getTask(String jobUUID, int taskID) {
        Job job = getJob(jobUUID);
        if (job != null) {
            return job.getTask(taskID);
        }
        return null;
    }

    public JobTask getTask(JobKey jobKey) {
        if (jobKey == null || jobKey.getJobUuid() == null || jobKey.getNodeNumber() == null) {
            return null;
        }
        return getTask(jobKey.getJobUuid(), jobKey.getNodeNumber());
    }

    /**
     * The entry point for requests to start tasks (for example, from the UI.) Does some checking, and ultimately
     * kicks the task or adds it to the task queue as appropriate
     *
     * @param jobUUID      Job ID
     * @param taskID       Node #
     * @param addToQueue   Whether the task should be added to the queue (false if the task is already on the queue)
     * @param priority     Immediacy of the start request
     * @param toQueueHead  Whether to add the task to the head of the queue rather than the end
     * @throws Exception When the task is invalid or already active
     */
    public void startTask(String jobUUID, int taskID, boolean addToQueue, int priority, boolean toQueueHead) throws Exception {
        Job job = getJob(jobUUID);
        checkArgument(job != null, "job not found");
        checkArgument(job.isEnabled(), "job is disabled");
        checkArgument(job.getState() != JobState.DEGRADED, "job in degraded state");
        checkArgument(taskID >= 0, "invalid task id");
        JobTask task = getTask(jobUUID, taskID);
        checkArgument(task != null, "no such task");
        checkArgument(task.getState() != JobTaskState.BUSY && task.getState() != JobTaskState.ALLOCATED &&
                      task.getState() != JobTaskState.QUEUED, "invalid task state");
        if (addToQueue) {
            addToTaskQueue(task.getJobKey(), priority, toQueueHead);
        } else {
            kickIncludingQueue(job, task, expandJob(job), false, priority);
        }
        log.warn("[task.kick] started " + job.getId() + " / " + task.getTaskID() + " = " + job.getDescription());
        queueJobTaskUpdateEvent(job);
    }

    public void stopTask(String jobUUID, int taskID) throws Exception {
        stopTask(jobUUID, taskID, false, false);
    }

    private void stopTask(String jobUUID, int taskID, boolean force, boolean onlyIfQueued) throws Exception {
        Job job = getJob(jobUUID);
        JobTask task = getTask(jobUUID, taskID);
        if (job != null && task != null) {
            taskQueuesByPriority.setStoppedJob(true); // Terminate the current queue iteration cleanly
            HostState host = hostManager.getHostState(task.getHostUUID());
            if (force) {
                task.setRebalanceSource(null);
                task.setRebalanceTarget(null);
            }
            if (task.getState().isQueuedState()) {
                removeFromQueue(task);
                log.warn("[task.stop] stopping queued " + task.getJobKey());
            } else if (task.getState() == JobTaskState.REBALANCE) {
                log.warn("[task.stop] stopping rebalancing " + task.getJobKey() + " with force=" + force);
            } else if (task.getState() == JobTaskState.MIGRATING ) {
                log.warn("[task.stop] stopping migrating " + task.getJobKey());
                task.setRebalanceSource(null);
                task.setRebalanceTarget(null);
            }
            else if (force && (task.getState() == JobTaskState.REVERT)) {
                log.warn("[task.stop] " + task.getJobKey() + " killed in revert state");
                int code  = JobTaskErrorCode.EXIT_REVERT_FAILURE;
                job.errorTask(task, code);
                queueJobTaskUpdateEvent(job);
            } else if (force && (host == null || host.isDead() || !host.isUp())) {
                log.warn("[task.stop] " + task.getJobKey() + " killed on down host");
                job.setTaskState(task, JobTaskState.IDLE);
                queueJobTaskUpdateEvent(job);
                return;
            } else if (host != null && !host.hasLive(task.getJobKey())) {
                log.warn("[task.stop] node that minion doesn't think is running: " + task.getJobKey());
                job.setTaskState(task, JobTaskState.IDLE);
                queueJobTaskUpdateEvent(job);
            }
            else if (task.getState() == JobTaskState.ALLOCATED) {
                log.warn("[task.stop] node in allocated state " + jobUUID + "/" + taskID + " host = " + (host != null ? host.getHost() : "unknown"));
            }

            // The following is called regardless of task state, unless the host is nonexistent/failed
            if (host != null) {
                spawnMQ.sendControlMessage(new CommandTaskStop(host.getHostUuid(), jobUUID, taskID, job.getRunCount(), force, onlyIfQueued));
            } else {
                log.warn("[task.stop]" + jobUUID + "/" + taskID + "]: no host monitored for uuid " + task.getHostUUID());
            }
        } else {
            log.warn("[task.stop] job/task {}/{} not found", jobUUID, taskID);
        }
    }

    protected boolean removeFromQueue(JobTask task) {
        boolean removed = false;
        Job job = getJob(task.getJobUUID());
        if (job != null) {
            log.warn("[taskQueuesByPriority] setting " + task.getJobKey() + " as idle and removing from queue");
            job.setTaskState(task, JobTaskState.IDLE, true);
            removed = taskQueuesByPriority.remove(job.getPriority(), task.getJobKey());
            queueJobTaskUpdateEvent(job);
            sendTaskQueueUpdateEvent();
        }
        writeSpawnQueue();
        return removed;

    }

    public void killTask(String jobUUID, int taskID) throws Exception {
        stopTask(jobUUID, taskID, true, false);
    }

    public boolean moveTask(JobKey jobKey, String sourceUUID, String targetUUID) {
        TaskMover tm = new TaskMover(this, hostManager, jobKey, targetUUID, sourceUUID);
        log.info("[task.move] attempting move for " + jobKey);
        return tm.execute();
    }

    public boolean revertJobOrTask(String jobUUID,
                                String user,
                                String token,
                                String sudo,
                                int taskID,
                                String backupType,
                                int rev,
                                long time) throws Exception {
        Job job = getJob(jobUUID);
        if (job == null) {
            return true;
        }
        if (!permissionsManager.isExecutable(user, token, sudo, job)) {
            return false;
        }
        if (taskID == -1) {
            // Revert entire job
            Job.logJobEvent(job, JobEvent.REVERT, eventLog);
            int numTasks = job.getTaskCount();
            for (int i = 0; i < numTasks; i++) {
                log.warn("[task.revert] " + jobUUID + "/" + i);
                revert(jobUUID, backupType, rev, time, i);
            }
        } else {
            // Revert single task
            log.warn("[task.revert] " + jobUUID + "/" + taskID);
            revert(jobUUID, backupType, rev, time, taskID);
        }
        return true;
    }

    private void revert(String jobUUID, String backupType, int rev, long time, int taskID) throws Exception {
        JobTask task = getTask(jobUUID, taskID);
        if (task != null) {
            task.setPreFailErrorCode(0);
            HostState host = hostManager.getHostState(task.getHostUUID());
            if (task.getState() == JobTaskState.ALLOCATED || task.getState().isQueuedState()) {
                log.warn("[task.revert] node in allocated state " + jobUUID + "/" + task.getTaskID() + " host = " + host.getHost());
            }
            log.warn("[task.revert] sending revert message to host: " + host.getHost() + "/" + host.getHostUuid());
            spawnMQ.sendControlMessage(new CommandTaskRevert(host.getHostUuid(), jobUUID, task.getTaskID(), backupType, rev, time, getTaskReplicaTargets(task, task.getAllReplicas()), false));
        } else {
            log.warn("[task.revert] task " + jobUUID + "/" + taskID + "] not found");
        }

    }


    // --------------------- END API ----------------------

    private List<HostState> getOrCreateHostStateList(String minionType, Collection<String> hostList) {
        List<HostState> hostStateList;
        if (hostList == null || hostList.size() == 0) {
            hostStateList = balancer.sortHostsByActiveTasks(hostManager.listHostStatus(minionType));
        } else {
            hostStateList = new ArrayList<>();
            for (String hostId : hostList) {
                hostStateList.add(hostManager.getHostState(hostId));
            }
        }
        return hostStateList;
    }


    /**
     * mq message dispatch
     */
    protected void handleMessage(CoreMessage core) {
        Job job;
        JobTask task;
        if (hostManager.deadMinionMembers.getMemberSet().contains(core.getHostUuid())) {
            log.warn("[mq.core] ignoring message from host: " + core.getHostUuid() + " because it is dead");
            return;
        }
        if (core instanceof HostState) {
            Set<String> upMinions = hostManager.minionMembers.getMemberSet();
            HostState state = (HostState) core;
            HostState oldState = hostManager.getHostState(state.getHostUuid());
            if (oldState == null) {
                log.warn("[host.status] from unmonitored " + state.getHostUuid() + " = " + state.getHost() + ":" + state.getPort());
                taskQueuesByPriority.updateHostAvailSlots(state);
            }
            boolean hostEnabled = true;
            if (spawnState.disabledHosts.contains(state.getHost()) ||
                spawnState.disabledHosts.contains(state.getHostUuid())) {
                hostEnabled = false;
                state.setDisabled(true);
            } else {
                state.setDisabled(false);
            }
            // Propagate minion state for ui
            if (upMinions.contains(state.getHostUuid()) && hostEnabled) {
                state.setUp(true);
            }
            state.setUpdated();
            sendHostUpdateEvent(state);
            hostManager.updateHostState(state);
        } else if (core instanceof StatusTaskBegin) {
            StatusTaskBegin begin = (StatusTaskBegin) core;
            SpawnMetrics.tasksStartedPerHour.mark();
            if (systemManager.debug("-begin-")) {
                log.info("[task.begin] :: " + begin.getJobKey());
            }
            try {
                job = getJob(begin.getJobUuid());
                if (job == null) {
                    log.warn("[task.begin] on dead job " + begin.getJobKey() + " from " + begin.getHostUuid());
                } else {
                    if (job.getStartTime() == null) {
                        job.setStartTime(System.currentTimeMillis());
                    }
                    task = job.getTask(begin.getNodeID());
                    if (checkTaskMessage(task, begin.getHostUuid())) {
                        if (task != null) {
                            job.setTaskState(task, JobTaskState.BUSY);
                            task.incrementStarts();
                            queueJobTaskUpdateEvent(job);
                        } else {
                            log.warn("[task.begin] done report for missing node " + begin.getJobKey());
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("", ex);
            }
        } else if (core instanceof StatusTaskCantBegin) {
            StatusTaskCantBegin cantBegin = (StatusTaskCantBegin) core;
            log.info("[task.cantbegin] received cantbegin from " + cantBegin.getHostUuid() + " for task " + cantBegin.getJobUuid() + "," + cantBegin.getNodeID());
            job = getJob(cantBegin.getJobUuid());
            task = getTask(cantBegin.getJobUuid(), cantBegin.getNodeID());
            if (job != null && task != null) {
                if (checkTaskMessage(task, cantBegin.getHostUuid())) {
                    try {
                        job.setTaskState(task, JobTaskState.IDLE);
                        log.info("[task.cantbegin] kicking " + task.getJobKey());
                        startTask(cantBegin.getJobUuid(), cantBegin.getNodeID(), true, 1, true);
                    } catch (Exception ex) {
                        log.warn("[task.schedule] failed to reschedule task for " + task.getJobKey(), ex);
                    }
                }
            } else {
                log.warn("[task.cantbegin] received cantbegin from " + cantBegin.getHostUuid() + " for nonexistent job " + cantBegin.getJobUuid());
            }
        } else if (core instanceof StatusTaskPort) {
            StatusTaskPort port = (StatusTaskPort) core;
            job = getJob(port.getJobUuid());
            task = getTask(port.getJobUuid(), port.getNodeID());
            if (task != null) {
                log.info("[task.port] " + job.getId() + "/" + task.getTaskID() + " @ " + port.getPort());
                task.setPort(port.getPort());
                queueJobTaskUpdateEvent(job);
            }
        } else if (core instanceof StatusTaskBackup) {
            StatusTaskBackup backup = (StatusTaskBackup) core;
            job = getJob(backup.getJobUuid());
            task = getTask(backup.getJobUuid(), backup.getNodeID());
            if (task != null && task.getState() != JobTaskState.REBALANCE && task.getState() != JobTaskState.MIGRATING) {
                log.info("[task.backup] " + job.getId() + "/" + task.getTaskID());
                job.setTaskState(task, JobTaskState.BACKUP);
                queueJobTaskUpdateEvent(job);
            }
        } else if (core instanceof StatusTaskReplicate) {
            StatusTaskReplicate replicate = (StatusTaskReplicate) core;
            job = getJob(replicate.getJobUuid());
            task = getTask(replicate.getJobUuid(), replicate.getNodeID());
            if (task != null) {
                if (checkTaskMessage(task, replicate.getHostUuid())) {
                    log.info("[task.replicate] " + job.getId() + "/" + task.getTaskID());
                    JobTaskState taskState = task.getState();
                    if (taskState != JobTaskState.REBALANCE && taskState != JobTaskState.MIGRATING) {
                        job.setTaskState(task, replicate.isFullReplication() ? JobTaskState.FULL_REPLICATE : JobTaskState.REPLICATE, true);
                    }
                    queueJobTaskUpdateEvent(job);
                }
            }
        } else if (core instanceof StatusTaskRevert) {
            StatusTaskRevert revert = (StatusTaskRevert) core;
            job = getJob(revert.getJobUuid());
            task = getTask(revert.getJobUuid(), revert.getNodeID());
            if (task != null) {
                if (checkTaskMessage(task, revert.getHostUuid())) {
                    log.info("[task.revert] " + job.getId() + "/" + task.getTaskID());
                    job.setTaskState(task, JobTaskState.REVERT, true);
                    queueJobTaskUpdateEvent(job);
                }
            }
        } else if (core instanceof StatusTaskReplica) {
            StatusTaskReplica replica = (StatusTaskReplica) core;
            job = getJob(replica.getJobUuid());
            task = getTask(replica.getJobUuid(), replica.getNodeID());
            if (task != null) {
                if (task.getReplicas() != null) {
                    for (JobTaskReplica taskReplica : task.getReplicas()) {
                        if (taskReplica.getHostUUID().equals(replica.getHostUuid())) {
                            taskReplica.setVersion(replica.getVersion());
                            taskReplica.setLastUpdate(replica.getUpdateTime());
                        }
                    }
                    log.info("[task.replica] version updated for " + job.getId() + "/" + task.getTaskID() + " ver " + task.getRunCount() + "/" + replica.getVersion());
                    queueJobTaskUpdateEvent(job);
                }
            }
        } else if (core instanceof StatusTaskEnd) {
            StatusTaskEnd end = (StatusTaskEnd) core;
            log.info("[task.end] :: " + end.getJobUuid() + "/" + end.getNodeID() + " exit=" + end.getExitCode());
            SpawnMetrics.tasksCompletedPerHour.mark();
            try {
                job = getJob(end.getJobUuid());
                if (job == null) {
                    log.warn("[task.end] on dead job " + end.getJobKey() + " from " + end.getHostUuid());
                } else {
                    task = job.getTask(end.getNodeID());
                    if (checkTaskMessage(task, end.getHostUuid())) {
                        if (task.isRunning()) {
                            taskQueuesByPriority.incrementHostAvailableSlots(end.getHostUuid());
                        }
                        handleStatusTaskEnd(job, task, end);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to handle end message: " + ex, ex);
            }
        } else {
            log.warn("[mq.core] unhandled type = " + core.getClass().toString());
        }
    }

    /**
     * Before updating task state, make sure the message source matches the host of the task
     * @param task The task to consider
     * @param messageSourceUuid The source of the message regarding that task
     * @return True if the message source matches the task's expected host
     */
    private static boolean checkTaskMessage(JobTask task, String messageSourceUuid) {
        if (task == null || messageSourceUuid == null || !messageSourceUuid.equals(task.getHostUUID())) {
            log.warn("Ignoring task state message from non-live host {}", messageSourceUuid);
            SpawnMetrics.nonHostTaskMessageCounter.inc();
            return false;
        }
        return true;
    }

    /**
     * Handle the various actions in response to a StatusTaskEnd sent by a minion
     *
     * @param job    The job to modify
     * @param task   The task to modify
     * @param update The message
     */
    private void handleStatusTaskEnd(Job job, JobTask task, StatusTaskEnd update) {
        TaskExitState exitState = update.getExitState();
        boolean wasStopped = exitState != null && exitState.getWasStopped();
        task.setFileCount(update.getFileCount());
        task.setByteCount(update.getByteCount());
        boolean errored = update.getExitCode() != 0 && update.getExitCode() != JobTaskErrorCode.REBALANCE_PAUSE;
        if (update.getRebalanceSource() != null) {
            handleRebalanceFinish(job, task, update);
        } else {
            if (exitState != null) {
                task.setInput(exitState.getInput());
                task.setMeanRate(exitState.getMeanRate());
                task.setTotalEmitted(exitState.getTotalEmitted());
            }
            task.setWasStopped(wasStopped);
        }
        if (errored) {
            handleTaskError(job, task, update.getExitCode());
        } else if (!update.wasQueued()) {
            job.setTaskFinished(task);
        }
        if (job.isFinished() && update.getRebalanceSource() == null) {
            finishJob(job, errored);
        }
        queueJobTaskUpdateEvent(job);
    }

    public void handleTaskError(Job job, JobTask task, int exitCode) {
        log.warn("[task.end] " + task.getJobKey() + " exited abnormally with " + exitCode);
        task.incrementErrors();
        try {
            spawnJobFixer.fixTask(job, task, exitCode);
        } catch (Exception ex) {
            job.errorTask(task, exitCode);
        }
    }

    public void handleRebalanceFinish(Job job, JobTask task, StatusTaskEnd update) {
        String rebalanceSource = update.getRebalanceSource();
        String rebalanceTarget = update.getRebalanceTarget();
        if (update.getExitCode() == 0) {
            // Rsync succeeded. Swap to the new host, assuming it is still healthy.
            task.setRebalanceSource(null);
            task.setRebalanceTarget(null);
            if (checkHostStatesForSwap(task.getJobKey(), rebalanceSource, rebalanceTarget, false)) {
                if (task.getHostUUID().equals(rebalanceSource)) {
                    task.setHostUUID(rebalanceTarget);
                } else {
                    task.replaceReplica(rebalanceSource, update.getRebalanceTarget());
                }

                deleteTask(job.getId(), rebalanceSource, task.getTaskID(), false);
                if (update.wasQueued()) {
                    addToTaskQueue(task.getJobKey(), 0, false);
                }
            } else {
                // The hosts returned by end message were not found, or weren't in a usable state.
                fixTaskDir(job.getId(), task.getTaskID(), true, true);
            }
        } else if (update.getExitCode() == JobTaskErrorCode.REBALANCE_PAUSE) {
            // Rebalancing was paused. No special action necessary.
            log.warn("[task.move] task rebalance for " + task.getJobKey() + " paused until next run");
        } else {
            // The rsync failed. Clean up the extra task directory.
            fixTaskDir(job.getId(), task.getTaskID(), true, true);
        }
    }

    /**
     * Perform cleanup tasks once per job completion. Triggered when the last running task transitions to an idle state.
     * In particular: perform any onComplete/onError triggers, set the end time, and possibly do a fixdirs.
     * @param job     The job that just finished
     * @param errored Whether the job ended up in error state
     */
    private void finishJob(Job job, boolean errored) {
        String callback = errored ? job.getOnErrorURL() : job.getOnCompleteURL();
        log.info("[job.done] {} :: errored={}. callback={}", job.getId(), errored, callback);
        SpawnMetrics.jobsCompletedPerHour.mark();
        job.setFinishTime(System.currentTimeMillis());
        spawnFormattedLogger.finishJob(job);
        if (!systemManager.isQuiesced()) {
            if (job.isEnabled() && !errored) {
                jobOnFinishStateHandler.handle(job, JobOnFinishState.OnComplete);
                if (ENABLE_JOB_FIXDIRS_ONCOMPLETE && job.getRunCount() > 1) {
                    // Perform a fixDirs on completion, cleaning up missing replicas/orphans.
                    fixTaskDir(job.getId(), -1, false, true);
                }
            } else {
                jobOnFinishStateHandler.handle(job, JobOnFinishState.OnError);
            }
        }
        Job.logJobEvent(job, JobEvent.FINISH, eventLog);
        balancer.requestJobSizeUpdate(job.getId(), 0);
    }

    public JobMacro createJobHostMacro(String job, int port) {
        String sPort = Integer.valueOf(port).toString();
        Set<String> jobHosts = new TreeSet<>();// best set?
        jobLock.lock();
        try {
            Collection<HostState> hosts = hostManager.listHostStatus(null);
            Map<String, String> uuid2Host = new HashMap<>();
            for (HostState host : hosts) {
                if (host.isUp()) {
                    uuid2Host.put(host.getHostUuid(), host.getHost());
                }
            }
            if (uuid2Host.size() == 0) {
                log.warn("[createJobHostMacro] warning job was found on no available hosts: " + job);
            }
            IJob ijob = getJob(job);
            if (ijob == null) {
                log.warn("[createJobHostMacro] Unable to get job config for job: " + job);
                throw new RuntimeException("[createJobHostMacro] Unable to get job config for job: " + job);
            }
            for (JobTask task : ijob.getCopyOfTasks()) {
                String host = uuid2Host.get(task.getHostUUID());
                if (host != null) {
                    jobHosts.add(host);
                }
            }
        } finally {
            jobLock.unlock();
        }

        List<String> hostStrings = new ArrayList<>();
        for (String host : jobHosts) {
            hostStrings.add("{host:\"" + host + "\", port:" + sPort + "}");
        }
        return new JobMacro("spawn", "", "createJobHostMacro-" + job, Joiner.on(',').join(hostStrings));
    }

    // TODO: 1. Why is this not in SpawnMQ?  2.  Who actually listens to job config changes
    // TODO: answer: this is for the web ui and live updating via SpawnManager /listen.batch

    /**
     * send job update event to registered listeners (usually http clients)
     */
    private void sendJobUpdateEvent(Job job) {
        jobLock.lock();
        try {
            jobConfigManager.updateJob(job);
        } finally {
            jobLock.unlock();
        }
        sendJobUpdateEvent("job.update", job);
    }

    public void queueJobTaskUpdateEvent(Job job) {
        jobLock.lock();
        try {
            jobUpdateQueue.add(job.getId());
        } finally {
            jobLock.unlock();
        }
    }

    private void drainJobTaskUpdateQueue() {
        try {
            long start = System.currentTimeMillis();
            Set<String> jobIds = new HashSet<>();
            jobUpdateQueue.drainTo(jobIds);
            log.trace("[drain] Draining {} jobs from the update queue", jobIds.size());
            for (String jobId : jobIds) {
                try {
                    Job job = getJob(jobId);
                    if (job == null) {
                        log.warn("[drain] Job {} does not exist - it may have been deleted", jobId);
                    } else {
                        sendJobUpdateEvent(job);
                    }
                } catch (Throwable e) {
                    log.error("[drain] Unexpected error when saving job update for {}", jobId, e);
                }
            }
            log.trace("[drain] Finished Draining {} jobs from the update queue in {}ms",
                      jobIds.size(), System.currentTimeMillis() - start);
        } catch (Throwable e) {
            log.error("[drain] Unexpected error when draining job task update queue", e);
        }
    }

    /**
     * Push all jobs to JobConfigManager. Primarily for use in extraordinary circumstances where job updates were not sent for a while.
     */
    public void saveAllJobs() {
        jobLock.lock();
        try {
            for (Job job : listJobs()) {
                if (job != null) {
                    sendJobUpdateEvent(job);
                }
            }
        } finally {
            jobLock.unlock();
        }
    }

    private synchronized void jobTaskUpdateHeartbeatCheck() {
        try {
            String now = Long.toString(System.currentTimeMillis());
            spawnDataStore.put(SpawnDataStoreKeys.SPAWN_JOB_CONFIG_HEARTBEAT_PATH, now);
            String received = spawnDataStore.get(SpawnDataStoreKeys.SPAWN_JOB_CONFIG_HEARTBEAT_PATH);
            if (received != null && received.equals(now)) {
                SpawnMetrics.jobTaskUpdateHeartbeatSuccessMeter.mark();
            } else {
                SpawnMetrics.jobTaskUpdateHeartbeatFailureCounter.inc();
            }
        } catch (Exception e) {
            SpawnMetrics.jobTaskUpdateHeartbeatFailureCounter.inc();
            log.warn("Failed to perform jobtaskupdate heartbeat check", e);
        }

    }

    public void sendJobUpdateEvent(String label, Job job) {
        try {
            sendEventToClientListeners(label, getJobUpdateEvent(job));
        } catch (Exception e) {
            log.warn("", e);
        }
    }

    /**
     * This method adds a cluster.quiesce event to  be sent to clientListeners to notify those using the UI that the cluster
     * has been quiesced.
     *
     * @param username
     */
    public void sendClusterQuiesceEvent(String username) {
        try {
            JSONObject info = new JSONObject();
            info.put("username", username);
            info.put("date", JitterClock.globalTime());
            info.put("quiesced", systemManager.isQuiesced());
            sendEventToClientListeners("cluster.quiesce", info);
        } catch (Exception e) {
            log.warn("", e);
        }
    }

    /**
     * Adds the task.queue.size event to be sent to clientListeners on next batch.listen update
     */
    public void sendTaskQueueUpdateEvent() {
        try {
            int numQueued = 0;
            int numQueuedWaitingOnSlot = 0;
            int numQueuedWaitingOnError = 0;
            taskQueuesByPriority.lock();
            try {
                LinkedList<JobKey>[] queues =
                        taskQueuesByPriority.values().toArray(new LinkedList[taskQueuesByPriority.size()]);

                for (LinkedList<JobKey> queue : queues) {
                    numQueued += queue.size();
                    for (JobKey key : queue) {
                        Job job = getJob(key);
                        if (job != null && !job.isEnabled()) {
                            numQueuedWaitingOnError += 1;
                        } else {
                            JobTask task = job.getTask(key.getNodeNumber());
                            if (task.getState() == JobTaskState.QUEUED_NO_SLOT) {
                                numQueuedWaitingOnSlot += 1;
                            }
                        }
                    }
                }
                lastQueueSize = numQueued;
            } finally {
                taskQueuesByPriority.unlock();
            }
            JSONObject json = new JSONObject("{'size':" + Integer.toString(numQueued) +
                                             ",'sizeErr':" + Integer.toString(numQueuedWaitingOnError) +
                                             ",'sizeSlot':" + Integer.toString(numQueuedWaitingOnSlot) + "}");
            sendEventToClientListeners("task.queue.size", json);
        } catch (Exception e) {
            log.warn("[task.queue.update] received exception while sending task queue update event (this is ok unless" +
                     " it happens repeatedly)", e);
        }
    }

    public int getLastQueueSize() {
        return lastQueueSize;
    }

    public static JSONObject getJobUpdateEvent(IJob job) throws Exception {
        long files = 0;
        long bytes = 0;
        int running = 0;
        int errored = 0;
        int done = 0;
        if (job == null) {
            String errMessage = "getJobUpdateEvent called with null job";
            log.warn(errMessage);
            throw new Exception(errMessage);
        }
        List<JobTask> jobNodes = job.getCopyOfTasks();
        int numNodes = 0;
        if (jobNodes != null) {
            numNodes = jobNodes.size();
            for (JobTask task : jobNodes) {
                files += task.getFileCount();
                bytes += task.getByteCount();
                if (task.getState() != JobTaskState.ALLOCATED && !task.getState().isQueuedState()) {
                    running++;
                }
                switch (task.getState()) {
                    case IDLE:
                        done++;
                        break;
                    case ERROR:
                        done++;
                        errored++;
                        break;
                    default:
                        break;
                }
            }
        }
        JSONObject ojob = job.toJSON().put("config", "").put("parameters", "");
        ojob.put("nodes", numNodes);
        ojob.put("running", running);
        ojob.put("errored", errored);
        ojob.put("done", done);
        ojob.put("files", files);
        ojob.put("bytes", bytes);
        return ojob;
    }

    public void sendHostUpdateEvent(HostState state) {
        sendHostUpdateEvent("host.update", state);
    }

    private void sendHostUpdateEvent(String label, HostState state) {
        try {
            sendEventToClientListeners(label, getHostStateUpdateEvent(state));
        } catch (Exception e) {
            log.warn("", e);
        }
    }

    public JSONObject getHostStateUpdateEvent(HostState state) throws Exception {
        if (state == null) {
            return null;
        }
        JSONObject ohost = CodecJSON.encodeJSON(state);
        ohost.put("spawnState", getSpawnStateString(state));
        ohost.put("stopped", ohost.getJSONArray("stopped").length());
        ohost.put("total", state.countTotalLive());
        double score = 0;
        try {
            score = balancer.getHostScoreCached(state.getHostUuid());
        } catch (NullPointerException npe) {
            log.warn("[host.status] exception in getHostStateUpdateEvent", npe);
        }
        ohost.put("score", score);
        return ohost;
    }

    private String getSpawnStateString(HostState state) {
        if (state.isDead()) {
            return "failed";
        } else if (state.isDisabled()) {
            return "disabled";
        }
        return hostFailWorker.getFailureStateString(state.getHostUuid(), state.isUp());
    }

    /**
     * send codable message to registered listeners as json
     */
    private void sendEventToClientListeners(final String topic, final JSONObject message) {
        long time = System.currentTimeMillis();
        for (Entry<String, ClientEventListener> ev : listeners.entrySet()) {
            ClientEventListener client = ev.getValue();
            boolean queueTooLarge = clientDropQueueSize > 0 && client.events.size() > clientDropQueueSize;
            // Drop listeners we haven't heard from in a while, or if they don't seem to be consuming from their queue
            if (time - client.lastSeen > clientDropTimeMillis || queueTooLarge) {
                ClientEventListener listener = listeners.remove(ev.getKey());
                if (systemManager.debug("-listen-")) {
                    log.warn("[listen] dropping listener queue for " + ev.getKey() + " = " + listener);
                }
                if (queueTooLarge) {
                    SpawnMetrics.nonConsumingClientDropCounter.inc();
                }
                continue;
            }
            try {
                client.events.put(new ClientEvent(topic, message));
            } catch (Exception ex) {
                log.warn("", ex);
            }
        }
        webSocketManager.addEvent(new ClientEvent(topic, message));
    }

    /** Called by Thread registered to Runtime triggered by sig-term. */
    @Override public void close() {
        shuttingDown.set(true);
        try {
            expandKickExecutor.shutdown();
            scheduledExecutor.shutdown();
            expandKickExecutor.awaitTermination(120, TimeUnit.SECONDS);
            scheduledExecutor.awaitTermination(120, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("Exception shutting down background processes", ex);
        }

        try {
            permissionsManager.close();
        } catch (Exception ex) {
            log.warn("Exception closing permissions manager", ex);
        }

        try {
            drainJobTaskUpdateQueue();
        } catch (Exception ex) {
            log.warn("Exception draining job task update queue", ex);
        }

        try {
            spawnFormattedLogger.close();
        } catch (Exception ex) {
            log.warn("", ex);
        }

        try {
            hostManager.minionMembers.shutdown();
            hostManager.deadMinionMembers.shutdown();
        } catch (IOException ex) {
            log.warn("Unable to cleanly shutdown membership listeners", ex);
        }

        jobOnFinishStateHandler.close();

        try {
            closeZkClients();
        } catch (Exception ex) {
            log.warn("Exception closing zk clients", ex);
        }
    }

    protected void autobalance(SpawnBalancer.RebalanceType type, SpawnBalancer.RebalanceWeight weight) {
        executeReallocationAssignments(balancer.getAssignmentsForAutoBalance(type, weight), false);
    }

    private boolean schedulePrep(Job job) {
        JobCommand jobCommand = getJobCommandManager().getEntity(job.getCommand());
        if (jobCommand == null) {
            log.warn("[schedule] failed submit : invalid command {}", job.getCommand());
            return false;
        }
        return job.isEnabled();
    }

    private ReplicaTarget[] getTaskReplicaTargets(JobTask task, List<JobTaskReplica> replicaList) {
        ReplicaTarget[] replicas = null;
        if (replicaList != null) {
            int next = 0;
            replicas = new ReplicaTarget[replicaList.size()];
            for (JobTaskReplica replica : replicaList) {
                HostState host = hostManager.getHostState(replica.getHostUUID());
                if (host == null) {
                    log.warn("[getTaskReplicaTargets] error - replica host: " + replica.getHostUUID() + " does not exist!");
                    throw new RuntimeException("[getTaskReplicaTargets] error - replica host: " + replica.getHostUUID() + " does not exist.  Rebalance the job to correct issue");
                }
                replicas[next++] = new ReplicaTarget(host.getHostUuid(), host.getHost(), host.getUser(), host.getPath());
            }
        }
        return replicas;
    }

    /**
     * Attempt to kick a task. Add it to the queue instead if appropriate.
     *
     * @param job           Job to kick
     * @param task          Task to kick
     * @param config        Config for the job
     * @param inQueue       Whether the task is already in the queue (in which case we shouldn't add it again)
     * @param priority      Priority of the kick request. Not the job priority.
     * @throws Exception If there is a problem scheduling the task
     */
    private void kickIncludingQueue(Job job, JobTask task, String config, boolean inQueue, int priority) throws Exception {
        boolean success = false;
        while (!success && !shuttingDown.get()) {
            jobLock.lock();
            try {
                if (taskQueuesByPriority.tryLock()) {
                    success = true;
                    boolean kicked = kickOnExistingHosts(job, task, config, 0L, true);
                    if (!kicked && !inQueue) {
                        addToTaskQueue(task.getJobKey(), priority, false);
                    }
                }
            } finally {
                jobLock.unlock();
                if (success) {
                    taskQueuesByPriority.unlock();
                }
            }
        }
    }

    /**
     * Schedule every task from a job.
     *
     * @param job          Job to kick
     * @param priority     immediacy of kick request
     * @return True if the job is scheduled successfully
     * @throws Exception If there is a problem scheduling a task
     */
    boolean scheduleJob(Job job, int priority) throws Exception {
        if (!schedulePrep(job)) {
            return false;
        }
        if (job.getCountActiveTasks() == job.getTaskCount()) {
            return false;
        }
        job.setSubmitTime(JitterClock.globalTime());
        job.setStartTime(null);
        job.setEndTime(null);
        job.incrementRunCount();
        Job.logJobEvent(job, JobEvent.SCHEDULED, eventLog);
        log.info("[job.schedule] assigning " + job.getId() + " with " + job.getCopyOfTasks().size() + " tasks");
        SpawnMetrics.jobsStartedPerHour.mark();
        for (JobTask task : job.getCopyOfTasks()) {
            if (task == null || task.getState() != JobTaskState.IDLE) {
                continue;
            }
            addToTaskQueue(task.getJobKey(), priority, false);
        }
        updateJob(job);
        return true;
    }

    /* helper for SpawnMesh */
    CommandTaskKick getCommandTaskKick(Job job, JobTask task) {
        JobCommand jobCmd = getJobCommandManager().getEntity(job.getCommand());
        final String expandedJob;
        try {
            expandedJob = expandJob(job);
        } catch (TokenReplacerOverflowException e) {
            return null;
        }
        CommandTaskKick kick = new CommandTaskKick(
                task.getHostUUID(),
                task.getJobKey(),
                job.getOwner(),
                job.getGroup(),
                job.getPriority(),
                job.getCopyOfTasks().size(),
                job.getMaxRunTime() != null ? job.getMaxRunTime() * 60000 : 0,
                job.getRunCount(),
                expandedJob,
                LessStrings.join(jobCmd.getCommand(), " "),
                job.getHourlyBackups(),
                job.getDailyBackups(),
                job.getWeeklyBackups(),
                job.getMonthlyBackups(),
                getTaskReplicaTargets(task, task.getAllReplicas()),
                job.getAutoRetry(),
                task.getStarts()
                );
        return kick;
    }

    /**
     * Send a start message to a minion.
     *
     * @param job    Job to kick
     * @param task   Task to kick
     * @param config Config for job
     * @return True if the start message is sent successfully
     */
    public boolean scheduleTask(Job job, JobTask task, String config) {
        if (!schedulePrep(job)) {
            return false;
        }
        if (task.getState() != JobTaskState.IDLE && task.getState() != JobTaskState.ERROR && task.getState() != JobTaskState.QUEUED) {
            return false;
        }
        JobState oldState = job.getState();
        if (!job.setTaskState(task, JobTaskState.ALLOCATED)) {
            return false;
        }
        if (oldState == JobState.IDLE && job.getRunCount() <= task.getRunCount()) {
            log.warn("Somehow a task ({}) was ALLOCATED from an IDLE, not queued or running, job ({})", task, job);
            job.incrementRunCount();
            job.setEndTime(null);
        }
        task.setRunCount(job.getRunCount());
        task.setErrorCode(0);
        task.setPreFailErrorCode(0);
        JobCommand jobcmd = getJobCommandManager().getEntity(job.getCommand());

        if (task.getRebalanceSource() != null && task.getRebalanceTarget() != null) {
            // If a rebalance was stopped cleanly, resume it.
            if (new TaskMover(this, hostManager, task.getJobKey(), task.getRebalanceTarget(), task.getRebalanceSource()).execute()) {
                return true;
            } else {
                // Unable to complete the stopped rebalance. Clear out source/target and kick as normal.
                task.setRebalanceSource(null);
                task.setRebalanceTarget(null);
            }
        }
        final CommandTaskKick kick = new CommandTaskKick(
                task.getHostUUID(),
                task.getJobKey(),
                job.getOwner(),
                job.getGroup(),
                job.getPriority(),
                job.getCopyOfTasks().size(),
                job.getMaxRunTime() != null ? job.getMaxRunTime() * 60000 : 0,
                job.getRunCount(),
                null,
                LessStrings.join(jobcmd.getCommand(), " "),
                job.getHourlyBackups(),
                job.getDailyBackups(),
                job.getWeeklyBackups(),
                job.getMonthlyBackups(),
                getTaskReplicaTargets(task, task.getAllReplicas()),
                job.getAutoRetry(),
                task.getStarts()
        );

        // Creating a runnable to expand the job and send kick message outside of the main queue-iteration thread.
        // Reason: the jobLock is held for duration of the queue-iteration and expanding some (kafka) jobs can be very
        // slow.  By making job expansion non-blocking we prevent other (UI) threads from waiting on zookeeper.
        // Note: we make a copy of job id, parameters to ignore modifications from outside the queue-iteration thread
        ArrayList<JobParameter> jobParameters = new ArrayList<>();          // deep clone of JobParameter list
        for (JobParameter parameter : job.getParameters()) {
            jobParameters.add(new JobParameter(parameter.getName(), parameter.getValue(), parameter.getDefaultValue()));
        }
        ScheduledTaskKick scheduledKick = new ScheduledTaskKick(this, job.getId(), jobParameters, config, getJobConfig(job.getId()), spawnMQ, kick, job, task);
        expandKickExecutor.submit(scheduledKick);
        return true;
    }

    /**
     * Helper function for kickOnExistingHosts.
     *
     * @param task A task, typically one that is about to be kicked
     * @return a List of HostStates from the task, either live or replica,
     * that are unable to support a task kick (down, read-only, or scheduled to be failed)
     */
    private List<HostState> hostsBlockingTaskKick(JobTask task) {
        List<HostState> unavailable = new ArrayList<>();
        HostState liveHost = hostManager.getHostState(task.getHostUUID());
        if (shouldBlockTaskKick(liveHost)) {
            unavailable.add(liveHost);
        }
        List<JobTaskReplica> replicas = (task.getReplicas() != null ? task.getReplicas() : new ArrayList<>());
        for (JobTaskReplica replica : replicas) {
            HostState replicaHost = hostManager.getHostState(replica.getHostUUID());
            if (shouldBlockTaskKick(replicaHost)) {
                unavailable.add(replicaHost);
            }
        }
        return unavailable;
    }

    private boolean shouldBlockTaskKick(HostState host) {
        if (host == null || !host.canMirrorTasks()) {
            return true;
        }
        HostFailWorker.FailState failState = hostFailWorker.getFailureState(host.getHostUuid());
        return failState == HostFailWorker.FailState.DISK_FULL || failState == HostFailWorker.FailState.FAILING_FS_DEAD;
    }

    /**
     * Attempt to find a host that has the capacity to run a task. Try the live host first, then any replica hosts,
     * swapping onto them only if one is available and if allowed to do so.
     *
     * @param job           Job to kick
     * @param task          Task to kick
     * @param config        Config for job
     * @param timeOnQueue   Time that the task has been on the queue
     * @param allowSwap     Whether to allow swapping to replica hosts
     * @return True if some host had the capacity to run the task and the task was sent there; false otherwise
     */
    public boolean kickOnExistingHosts(Job job, JobTask task, String config, long timeOnQueue, boolean allowSwap) {
        if (!jobTaskCanKick(job, task)) {
            if (task.getState() == JobTaskState.QUEUED_NO_SLOT) {
                job.setTaskState(task, JobTaskState.QUEUED);
                queueJobTaskUpdateEvent(job);
            }
            return false;
        }
        List<HostState> possibleHosts = new ArrayList<>();
        if (allowSwap && isNewTask(task)) {
            for (HostState state : hostManager.listHostStatus(job.getMinionType())) {
                // Don't swap new tasks onto hosts in the fs-okay queue.
                if (hostFailWorker.getFailureState(state.getHostUuid()) == HostFailWorker.FailState.ALIVE) {
                    possibleHosts.add(state);
                }
            }
        }
        else {
            possibleHosts.addAll(getHealthyHostStatesHousingTask(task, allowSwap));
        }
        HostState bestHost = findHostWithAvailableSlot(task, timeOnQueue, possibleHosts, false);
        if (bestHost != null) {
            if (task.getState() == JobTaskState.QUEUED_NO_SLOT) {
                job.setTaskState(task, JobTaskState.QUEUED);
                queueJobTaskUpdateEvent(job);
            }
            String bestHostUuid = bestHost.getHostUuid();
            if (task.getHostUUID().equals(bestHostUuid)) {
                taskQueuesByPriority.markHostTaskActive(bestHostUuid);
                scheduleTask(job, task, config);
                log.info("[taskQueuesByPriority] sending {} to {}", task.getJobKey(), bestHostUuid);
                return true;
            } else if (swapTask(task, bestHostUuid, true)) {
                taskQueuesByPriority.markHostTaskActive(bestHostUuid);
                log.info("[taskQueuesByPriority] swapping {} onto {}", task.getJobKey(), bestHostUuid);
                return true;
            }
        }
        if (taskQueuesByPriority.isMigrationEnabled()
            && !job.getQueryConfig().getCanQuery()
            && !job.getDontAutoBalanceMe()
            && attemptMigrateTask(job, task, timeOnQueue)) {
            return true;
        }
        if (task.getState() != JobTaskState.QUEUED_NO_SLOT) {
            job.setTaskState(task, JobTaskState.QUEUED_NO_SLOT);
            queueJobTaskUpdateEvent(job);
        }
        return false;
    }

    private boolean jobTaskCanKick(Job job, JobTask task) {
        if ((job == null) || !job.isEnabled()) {
            return false;
        }
        boolean isNewTask = isNewTask(task);
        List<HostState> unavailableHosts = hostsBlockingTaskKick(task);
        if (isNewTask && !unavailableHosts.isEmpty()) {
            // If a task is new, just replace any down hosts since there is no existing data
            boolean changed = replaceDownHosts(task);
            if (changed) {
                return false; // Reconsider the task the next time through the queue
            }
        }
        if (!unavailableHosts.isEmpty()) {
            log.warn("[taskQueuesByPriority] cannot kick {} because one or more of its hosts is down or scheduled to " +
                     "be failed: {}", task.getJobKey(), unavailableHosts);
            if (task.getState() != JobTaskState.QUEUED_HOST_UNAVAIL) {
                job.setTaskState(task, JobTaskState.QUEUED_HOST_UNAVAIL);
                queueJobTaskUpdateEvent(job);
            }
            return false;
        } else if (task.getState() == JobTaskState.QUEUED_HOST_UNAVAIL) {
            // Task was previously waiting on an unavailable host, but that host is back. Update state accordingly.
            job.setTaskState(task, JobTaskState.QUEUED);
            queueJobTaskUpdateEvent(job);
        }
        // Obey the maximum simultaneous task running limit for this job, if it is set.
        return !((job.getMaxSimulRunning() > 0) && (job.getCountActiveTasks() >= job.getMaxSimulRunning()));
    }

    List<HostState> getHealthyHostStatesHousingTask(JobTask task, boolean allowReplicas) {
        List<HostState> rv = new ArrayList<>();
        HostState liveHost = hostManager.getHostState(task.getHostUUID());
        if (liveHost != null && hostFailWorker.shouldKickTasks(task.getHostUUID())) {
            rv.add(liveHost);
        }
        if (allowReplicas && task.getReplicas() != null) {
            for (JobTaskReplica replica : task.getReplicas()) {
                HostState replicaHost = replica.getHostUUID() != null ?
                                        hostManager.getHostState(replica.getHostUUID()) : null;
                if (replicaHost != null && replicaHost.hasLive(task.getJobKey()) && hostFailWorker.shouldKickTasks(task.getHostUUID())) {
                    rv.add(replicaHost);
                }
            }
        }
        return rv;
    }

    /**
     * Select a host that can run a task
     *
     * @param task         The task being moved
     * @param timeOnQueue  How long the task has been on the queue
     * @param hosts        A collection of hosts
     * @param forMigration Whether the host in question is being used for migration
     * @return A suitable host that has an available task slot, if one exists; otherwise, null
     */
    private HostState findHostWithAvailableSlot(JobTask task, long timeOnQueue, List<HostState> hosts, boolean forMigration) {
        if (hosts == null) {
            return null;
        }
        List<HostState> filteredHosts = new ArrayList<>();
        for (HostState host : hosts) {
            if (host == null || (forMigration && hostFailWorker.getFailureState(host.getHostUuid()) != HostFailWorker.FailState.ALIVE)) {
                // Don't migrate onto hosts that are being failed in any capacity
                continue;
            }
            if (forMigration && !taskQueuesByPriority.shouldMigrateTaskToHost(task, host.getHostUuid())) {
                // Not a valid migration target
                continue;
            }
            if (isNewTask(task) && !taskQueuesByPriority.shouldKickNewTaskOnHost(timeOnQueue, host)) {
                // Not a valid target for new tasks
                continue;
            }
            if (host.canMirrorTasks()  && taskQueuesByPriority.shouldKickTaskOnHost(host.getHostUuid())) {
                filteredHosts.add(host);
            }
        }
        return taskQueuesByPriority.findBestHostToRunTask(filteredHosts, true);
    }

    /**
     * Consider migrating a task to a new host and run it there, subject to a limit on the overall 
     * number of such migrations to do per time interval and how many bytes are allowed to be 
     * migrated.
     *
     * @param job           The job for the task to kick
     * @param task          The task to kick
     * @param timeOnQueue   How long the task has been on the queue
     * @return True if the task was migrated
     */
    private boolean attemptMigrateTask(Job job, JobTask task, long timeOnQueue) {
        // If spawn is not quiesced, and the task is small enough that migration is sensible, and 
        // there is a host with available capacity that can run the job, Migrate the task to the 
        // target host and kick it on completion
        if (!systemManager.isQuiesced() && 
                taskQueuesByPriority.checkSizeAgeForMigration(task.getByteCount(), timeOnQueue)) {
            HostState target = findHostWithAvailableSlot(task, timeOnQueue,
                                                         hostManager.listHostStatus(job.getMinionType()), true);
            if (target != null) {
                log.warn("Migrating {} to {}", task.getJobKey(), target.getHostUuid());
                taskQueuesByPriority.markMigrationBetweenHosts(task.getHostUUID(), target.getHostUuid());
                taskQueuesByPriority.markHostTaskActive(target.getHostUuid());
                TaskMover tm = new TaskMover(this, hostManager, task.getJobKey(), target.getHostUuid(), task.getHostUUID());
                tm.setMigration(true);
                tm.execute();
                return true;
            }
        }
        return false;
    }

    protected boolean isNewTask(JobTask task) {
        HostState liveHost = hostManager.getHostState(task.getHostUUID());
        return (liveHost != null)
               && !liveHost.hasLive(task.getJobKey())
               && (task.getFileCount() == 0)
               && (task.getByteCount() == 0);
    }

    /**
     * Add a jobkey to the appropriate task queue, given its priority
     *
     * @param jobKey        The jobkey to add
     * @param priority      immediacy of the addition operation
     */
    public void addToTaskQueue(JobKey jobKey, int priority, boolean toHead) {
        Job job = getJob(jobKey.getJobUuid());
        JobTask task = getTask(jobKey.getJobUuid(), jobKey.getNodeNumber());
        if (job != null && task != null) {
            if (task.getState() == JobTaskState.QUEUED || job.setTaskState(task, JobTaskState.QUEUED)) {
                log.info("[taskQueuesByPriority] adding " + jobKey + " to queue with priority=" + priority);
                taskQueuesByPriority.addTaskToQueue(job.getPriority(), jobKey, priority, toHead);
                queueJobTaskUpdateEvent(job);
                sendTaskQueueUpdateEvent();
            } else {
                log.warn("[task.queue] failed to add task " + jobKey + " with state " + task.getState());
            }
        }
    }

    /**
     * Iterate over each queue looking for jobs that can run. By design, the queues are processed in descending order
     * of priority, so we try priority 2 tasks before priority 1, etc.
     */
    public void kickJobsOnQueue() {
        LinkedList[] queues = null;
        boolean success = false;
        while (!success && !shuttingDown.get()) {
            // need the job lock first
            jobLock.lock();
            try {
                if (taskQueuesByPriority.tryLock()) {
                    success = true;
                    taskQueuesByPriority.setStoppedJob(false);
                    taskQueuesByPriority.updateAllHostAvailSlots(hostManager.listHostStatus(null));
                    queues = taskQueuesByPriority.values().toArray(new LinkedList[taskQueuesByPriority.size()]);
                    for (LinkedList<SpawnQueueItem> queue : queues) {
                        iterateThroughTaskQueue(queue);
                    }
                    sendTaskQueueUpdateEvent();
                }
            } finally {
                jobLock.unlock();
                if (success) {
                    taskQueuesByPriority.unlock();
                }
            }
            if (!success) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * Iterate over a particular queue of same-priority tasks, kicking any that can run.
     * Must be inside of a block synchronized on the queue.
     *
     * @param queue The queue to look over
     */
    private void iterateThroughTaskQueue(LinkedList<SpawnQueueItem> queue) {
        ListIterator<SpawnQueueItem> iter = queue.listIterator(0);
        int skippedQuiesceCount = 0;
        long now = System.currentTimeMillis();
        // Terminate if out of tasks or we stopped a job, requiring a queue modification
        while (iter.hasNext() && !taskQueuesByPriority.getStoppedJob()) {
            SpawnQueueItem key = iter.next();
            Job job = getJob(key.getJobUuid());
            JobTask task = getTask(key.getJobUuid(), key.getNodeNumber());
            try {
                if ((job == null) || (task == null) || !task.getState().isQueuedState()) {
                    log.warn("[task.queue] removing invalid task {}", key);
                    iter.remove();
                    continue;
                }
                if (systemManager.isQuiesced() && (key.getPriority() < 1)) {
                    skippedQuiesceCount++;
                    log.debug("[task.queue] skipping {} because spawn is quiesced and the kick wasn't manual", key);
                    if (task.getState() == JobTaskState.QUEUED_NO_SLOT) {
                        job.setTaskState(task, JobTaskState.QUEUED);
                        queueJobTaskUpdateEvent(job);
                    }
                } else {
                    boolean kicked = kickOnExistingHosts(job, task, null, now - key.getCreationTime(),
                                                         !job.getDontAutoBalanceMe());
                    if (kicked) {
                        log.info("[task.queue] removing kicked task {}", task.getJobKey());
                        iter.remove();
                    }
                }
            } catch (Exception ex) {
                log.warn("[task.queue] received exception during task kick: ", ex);
                if ((task != null) && (job != null)) {
                    job.errorTask(task, JobTaskErrorCode.KICK_ERROR);
                    iter.remove();
                    queueJobTaskUpdateEvent(job);
                }
            }
        }
        if (skippedQuiesceCount > 0) {
            log.warn("[task.queue] skipped {} queued tasks because spawn is quiesced and the kick wasn't manual",
                     skippedQuiesceCount);
        }
    }

    public WebSocketManager getWebSocketManager() {
        return this.webSocketManager;
    }

    public void toggleHosts(String hosts, boolean disable) {
        if (hosts != null) {
            String[] hostsArray = hosts.split(",");
            for (String host : hostsArray) {
                if (host.isEmpty()) {
                    continue;
                }
                boolean changed;
                changed = disable ? spawnState.disabledHosts.add(host) : spawnState.disabledHosts.remove(host);
                if (changed) {
                    updateToggledHosts(host, disable);
                }
            }
            writeState();
        }
    }

    public void updateToggledHosts(String id, boolean disable) {
        for (HostState host : hostManager.listHostStatus(null)) {
            if (id.equals(host.getHost()) || id.equals(host.getHostUuid())) {
                host.setDisabled(disable);
                sendHostUpdateEvent(host);
                hostManager.updateHostState(host);
            }
        }
    }

    @Nonnull
    public SpawnState getSpawnState() {
        return spawnState;
    }

    @Nonnull
    public JobDefaults getJobDefaults() { return jobDefaults; }

    @Nonnull
    public SpawnDataStore getSpawnDataStore() {
        return spawnDataStore;
    }

}
