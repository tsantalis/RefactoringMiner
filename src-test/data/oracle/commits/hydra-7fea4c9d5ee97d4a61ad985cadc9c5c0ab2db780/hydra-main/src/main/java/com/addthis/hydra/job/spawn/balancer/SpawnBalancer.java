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
package com.addthis.hydra.job.spawn.balancer;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.addthis.basis.util.JitterClock;
import com.addthis.basis.util.Parameter;

import com.addthis.codec.annotations.FieldConfig;
import com.addthis.codec.codables.Codable;
import com.addthis.codec.config.Configs;
import com.addthis.codec.json.CodecJSON;
import com.addthis.hydra.job.HostFailWorker;
import com.addthis.hydra.job.IJob;
import com.addthis.hydra.job.Job;
import com.addthis.hydra.job.JobState;
import com.addthis.hydra.job.JobTask;
import com.addthis.hydra.job.JobTaskMoveAssignment;
import com.addthis.hydra.job.JobTaskReplica;
import com.addthis.hydra.job.JobTaskState;
import com.addthis.hydra.job.mq.CommandTaskStop;
import com.addthis.hydra.job.mq.CoreMessage;
import com.addthis.hydra.job.mq.HostState;
import com.addthis.hydra.job.mq.JobKey;
import com.addthis.hydra.job.spawn.HostManager;
import com.addthis.hydra.job.spawn.Spawn;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.addthis.hydra.job.JobTaskState.IDLE;
import static com.addthis.hydra.job.JobTaskState.QUEUED;
import static com.addthis.hydra.job.JobTaskState.QUEUED_HOST_UNAVAIL;
import static com.addthis.hydra.job.JobTaskState.QUEUED_NO_SLOT;
import static com.addthis.hydra.job.store.SpawnDataStoreKeys.SPAWN_BALANCE_PARAM_PATH;

/**
 * A class in charge of balancing load among spawn's hosts.
 * General assumptions:
 * The boxes are not so asymmetrical that running a job on three boxes is slower than running it on a single box.
 * Jobs run faster when they have as few tasks grouped together on individual boxes as possible.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class SpawnBalancer implements Codable, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(SpawnBalancer.class);

    private static final Set<JobTaskState> movableTaskStates = ImmutableSet.of(
            IDLE, QUEUED, QUEUED_HOST_UNAVAIL, QUEUED_NO_SLOT);

    // How often to update aggregate host statistics
    static final long AGGREGATE_STAT_UPDATE_INTERVAL = Parameter.intValue("spawnbalance.stat.update", 15 * 1000);

    private final ConcurrentHashMap<String, HostScore> cachedHostScores;
    private final ReentrantLock aggregateStatisticsLock;
    private final AtomicBoolean autobalanceStarted;
    private final Cache<String, Boolean> recentlyAutobalancedJobs;
    private final Cache<String, Boolean> recentlyBalancedHosts;
    private final Cache<String, Boolean> recentlyReplicatedToHosts;
    private final SpawnBalancerTaskSizer taskSizer;
    private final Comparator<HostAndScore> hostAndScoreComparator;
    private final Comparator<HostState> hostStateScoreComparator;
    private final Comparator<Job> jobAverageTaskSizeComparator;
    private final Comparator<HostState> hostStateReplicationSuitabilityComparator;
    private final ScheduledExecutorService taskExecutor;

    final Spawn spawn;
    final HostManager hostManager;

    private Set<String> activeJobIDs;
    long lastAggregateStatUpdateTime;

    @FieldConfig SpawnBalancerConfig config;

    public SpawnBalancer(Spawn spawn, HostManager hostManager) {
        this.spawn = spawn;
        this.hostManager = hostManager;
        config = loadConfigFromDataStore(new SpawnBalancerConfig());
        taskExecutor = new ScheduledThreadPoolExecutor(
                2, new ThreadFactoryBuilder().setNameFormat("spawnBalancer-%d").build());
        taskExecutor.scheduleAtFixedRate(new AggregateStatUpdaterTask(this), AGGREGATE_STAT_UPDATE_INTERVAL,
                                         AGGREGATE_STAT_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
        taskSizer = new SpawnBalancerTaskSizer(spawn, hostManager);
        cachedHostScores = new ConcurrentHashMap<>();
        aggregateStatisticsLock = new ReentrantLock();
        lastAggregateStatUpdateTime = 0;
        autobalanceStarted = new AtomicBoolean(false);
        recentlyAutobalancedJobs = CacheBuilder.newBuilder().expireAfterWrite(
                Parameter.intValue("spawnbalance.job.autobalance.interval.mins", 60 * 12), TimeUnit.MINUTES
        ).build();
        recentlyBalancedHosts = CacheBuilder.newBuilder().expireAfterWrite(
                Parameter.intValue("spawnbalance.host.balance.interval.mins", 3), TimeUnit.MINUTES
        ).build();
        recentlyReplicatedToHosts = CacheBuilder.newBuilder().expireAfterWrite(
                Parameter.intValue("spawnbalance.host.replicate.interval.mins", 15), TimeUnit.MINUTES
        ).build();
        hostAndScoreComparator = (firstHAS, secondHAS) -> Double.compare(firstHAS.score, secondHAS.score);
        hostStateScoreComparator = (hostState, hostState1) -> Double.compare(
                getHostScoreCached(hostState.getHostUuid()),
                getHostScoreCached(hostState1.getHostUuid()));
        jobAverageTaskSizeComparator = (job, job1) -> {
            if ((job == null) || (job1 == null)) {
                return 0;
            } else {
                return Double.compare(job.calcAverageTaskSizeBytes(), job1.calcAverageTaskSizeBytes());
            }
        };
        hostStateReplicationSuitabilityComparator = (hostState, hostState1) -> {
            // Treat recently-replicated-to hosts as having fewer than their reported available bytes
            long availBytes = getAvailDiskBytes(hostState);
            long availBytes1 = getAvailDiskBytes(hostState1);
            if (recentlyReplicatedToHosts.getIfPresent(hostState.getHostUuid()) != null) {
                availBytes /= 2;
            }
            if (recentlyReplicatedToHosts.getIfPresent(hostState1.getHostUuid()) != null) {
                availBytes1 /= 2;
            }
            return -Double.compare(availBytes, availBytes1);
        };
    }

    /** Loads SpawnBalancerConfig from data store; if no data or failed, returns the default. */
    @VisibleForTesting
    protected SpawnBalancerConfig loadConfigFromDataStore(SpawnBalancerConfig defaultValue) {
        String configString = spawn.getSpawnDataStore().get(SPAWN_BALANCE_PARAM_PATH);
        if (!Strings.isNullOrEmpty(configString)) {
            try {
                return Configs.decodeObject(SpawnBalancerConfig.class, configString);
            } catch (Exception e) {
                log.warn("Failed to decode SpawnBalancerConfig", e);
            }
        }
        return defaultValue;
    }

    /**
     * A cached version of getHostScore that will keep host score calculation outside the main spawn thread
     *
     * @param hostId The host to measure
     * @return A non-negative number representing load.
     */
    public double getHostScoreCached(String hostId) {
        double defaultScore = config.getDefaultHostScore();
        if (hostId == null) {
            return defaultScore;
        }
        aggregateStatisticsLock.lock();
        try {
            if (cachedHostScores == null) {
                return defaultScore;
            }
            HostScore score = cachedHostScores.get(hostId);
            if (score != null) {
                return score.getOverallScore();
            } else {
                return defaultScore;
            }
        } finally {
            aggregateStatisticsLock.unlock();
        }
    }

    private static long getAvailDiskBytes(HostState host) {
        if ((host.getMax() == null) || (host.getUsed() == null)) {
            return 1; // Fix some tests
        }
        return host.getMax().getDisk() - host.getUsed().getDisk();
    }

    /** Start a thread that will perform autobalancing in the background if appropriate to do so */
    public synchronized void startAutobalanceTask() {
        if (autobalanceStarted.compareAndSet(false, true)) {
            taskExecutor.scheduleWithFixedDelay(new AutobalanceTask(this), config.getAutobalanceCheckInterval(),
                                                config.getAutobalanceCheckInterval(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Is this job unbalanced enough to warrant a rebalance?
     *
     * @param job   The job to check
     * @param hosts The hosts in the cluster
     * @return True if it is appropriate to rebalance the job
     */
    private synchronized boolean shouldAutobalanceJob(Job job, List<HostState> hosts) {
        if ((job == null)
            || (recentlyAutobalancedJobs.getIfPresent(job.getId()) != null)
            || (JobState.IDLE != job.getState())
            || job.getDontAutoBalanceMe()
            || (job.getRunCount() < 1)) {
            return false;
        }
        if (config.getAutoBalanceLevel() >= 2) {
            // At level 2, rebalance every job that hasn't already been rebalanced recently
            return true;
        }
        JobTaskItemByHostMap tasksByHost = generateTaskCountByHost(hosts, job.getCopyOfTasks());
        int maxPerHost = maxTasksPerHost(job, hosts.size());
        // If any host has sufficiently more or less than the expected fair share, this job is unbalanced.
        return (tasksByHost.findLeastTasksOnHost() <= (maxPerHost - 2)) ||
               (tasksByHost.findMostTasksOnHost() >= (maxPerHost + 1));
    }

    @Override public void close() {
        MoreExecutors.shutdownAndAwaitTermination(taskExecutor, 120, TimeUnit.SECONDS);
    }

    public void saveConfigToDataStore() {
        try {
            spawn.getSpawnDataStore().put(SPAWN_BALANCE_PARAM_PATH, CodecJSON.encodeString(config));
        } catch (Exception e) {
            log.warn("Failed to save SpawnBalancerConfig to data store", e);
        }
    }

    public void startTaskSizePolling() {
        taskSizer.startPolling(taskExecutor);
    }

    /**
     * Takes any arbitrary list of tasks and finds the best hosts to house all of them.
     *
     * @param tasks The tasks to assign to hosts
     * @param hosts All available hosts in the cluster
     * @return A map describing where to send each task
     */
    public Map<JobTask, String> assignTasksFromMultipleJobsToHosts(Collection<JobTask> tasks,
                                                                   Collection<HostState> hosts) {
        // Populate a map grouping the tasks by job ID.
        Map<String, List<JobTask>> tasksByJobID = new HashMap<>();
        for (JobTask task : tasks) {
            if (task.getJobUUID() != null) {
                Job job = spawn.getJob(task.getJobUUID());
                if (job != null) {
                    List<JobTask> taskList = tasksByJobID.computeIfAbsent(job.getId(), key -> new ArrayList<>());
                    taskList.add(task);
                }
            }
        }
        // This map of hosts to scores will be updated by every call to assignTasksFromSingleJobToHosts.
        Map<HostState, Double> hostScoreMap = generateHostStateScoreMap(hosts, null);
        // This map stores where to send each task.
        Map<JobTask, String> hostAssignments = new HashMap<>(tasks.size());
        for (Map.Entry<String, List<JobTask>> entry : tasksByJobID.entrySet()) {
            Map<JobTask, String> singleHostAssignments =
                    assignTasksFromSingleJobToHosts(entry.getValue(), hostScoreMap);
            hostAssignments.putAll(singleHostAssignments);
        }
        return hostAssignments;
    }

    /**
     * Internal function that creates a map of HostStates to their hostScores.
     *
     * @param hosts The hosts from spawn
     * @param jobID is specified, adds a factor that scales with the number of siblings on each host from that job.
     * @return A map taking each HostState to its score
     */
    private Map<HostState, Double> generateHostStateScoreMap(Collection<HostState> hosts, @Nullable String jobID) {
        final Map<HostState, Double> hostScores = new HashMap<>(hosts.size());
        for (HostState host : hosts) {
            if ((host != null) && host.isUp() && !host.isDead()) {
                int siblingScore = (jobID != null) ? (host.getTaskCount(jobID) * config.getSiblingWeight()) : 0;
                double score = getHostScoreCached(host.getHostUuid()) + siblingScore;
                hostScores.put(host, score);
            }
        }
        return hostScores;
    }

    /**
     * Take a list of tasks belonging to a single job and find the best hosts to house these tasks.
     * A map of host scores is passed as a parameter so that it can be persisted across multiple calls to this function
     * by, for example, assignTasksFromMultipleJobsToHosts.
     */
    private Map<JobTask, String> assignTasksFromSingleJobToHosts(List<JobTask> tasks,
                                                                 Map<HostState, Double> storedHostScores) {
        if ((tasks == null) || tasks.isEmpty()) {
            return new HashMap<>();
        }
        // Make a heap of hosts based on the storedHostScores, from lightest to heaviest.
        PriorityQueue<HostAndScore> hostScores =
                new PriorityQueue<>(1 + storedHostScores.size(), hostAndScoreComparator);
        for (Map.Entry<HostState, Double> hostStateDoubleEntry : storedHostScores.entrySet()) {
            if (canReceiveNewTasks(hostStateDoubleEntry.getKey())) {
                hostScores.add(new HostAndScore(hostStateDoubleEntry.getKey(), hostStateDoubleEntry.getValue()));
            }
        }
        if (hostScores.isEmpty()) {
            log.warn("[spawn.balancer] found no hosts eligible to receive new tasks");
            throw new RuntimeException("no eligible hosts for new task");
        }
        // Make a list of hosts as big as the list of tasks. This list may repeat hosts if it is necessary to do so.
        Collection<String> hostsToAssign = new ArrayList<>(tasks.size());
        for (JobTask task : tasks) {
            if (task == null) {
                continue;
            }
            // Pick the lightest host.
            HostAndScore h = hostScores.poll();
            HostState host = h.host;
            hostsToAssign.add(host.getHostUuid());
            // Then moderately weight that host in the heap so we won't pick it again immediately.
            hostScores.add(new HostAndScore(host, h.score + config.getSiblingWeight()));
            // Lightly weight that host in storedHostScores so we won't pick the same hosts over and over if we call
            // this method repeatedly
            storedHostScores.put(host, h.score + config.getActiveTaskWeight());
        }
        return pairTasksAndHosts(tasks, hostsToAssign);
    }

    public boolean canReceiveNewTasks(HostState host) {
        if (host == null) {
            return false;
        }
        if (spawn.getHostFailWorker().getFailureState(host.getHostUuid()) != HostFailWorker.FailState.ALIVE) {
            return false;
        }
        return host.canMirrorTasks() &&
               (getUsedDiskPercent(host) < (1 - config.getMinDiskPercentAvailToReceiveNewTasks()));
    }

    /**
     * Given a list of tasks and a list of potential hosts of the same size, pair them up.
     * Keep tasks on their existing hosts as much as possible.
     */
    private static Map<JobTask, String> pairTasksAndHosts(List<JobTask> tasks, Collection<String> hosts) {
        if ((tasks == null) || (hosts == null) || (tasks.size() != hosts.size())) {
            log.warn("[spawn.balancer] invalid call to pairTasksAndHosts: tasks={} hosts={}", tasks, hosts);
            return new HashMap<>(0);
        }
        Map<JobTask, String> rv = new HashMap<>(tasks.size());
        // For any task already living on a host in our hosts list, keep that task where it is.
        Collection<JobTask> unassignedTasks = new ArrayList<>();
        String jobID = tasks.get(0).getJobUUID();
        for (JobTask task : tasks) {
            if (!task.getJobUUID().equals(jobID)) {
                throw new RuntimeException(
                        "Illegal call to assignTasksFromSingleJobToHosts: not all tasks came from the same job");
            }
            String hostID = task.getHostUUID();
            if ((hostID != null) && hosts.contains(hostID)) {
                hosts.remove(hostID);
            } else {
                unassignedTasks.add(task);
            }
        }
        // Assign the remaining tasks by iterating down the remaining hosts.
        Iterator<String> hostIterator = hosts.iterator();
        for (JobTask task : unassignedTasks) {
            rv.put(task, hostIterator.next());
        }
        return rv;
    }

    private static double getUsedDiskPercent(HostState host) {
        if ((host.getMax() == null) || (host.getUsed() == null) || (host.getMax().getDisk() <= 0)) {
            return 0;
        }
        return (double) host.getUsed().getDisk() / host.getMax().getDisk();
    }

    public List<JobTaskMoveAssignment> pushTasksOffDiskForFilesystemOkayFailure(HostState host, int moveLimit) {
        List<HostState> hosts = hostManager.listHostStatus(null);
        return pushTasksOffHost(host, hosts, false, 1, moveLimit, false);
    }

    /* Push/pull the tasks on a host to balance its disk, obeying an overall limit on the number of tasks/bytes to
    move */
    private List<JobTaskMoveAssignment> pushTasksOffHost(HostState host,
                                                         Collection<HostState> otherHosts,
                                                         boolean limitBytes,
                                                         double byteLimitFactor,
                                                         int moveLimit,
                                                         boolean obeyDontAutobalanceMe) {
        List<JobTaskMoveAssignment> rv = purgeMisplacedTasks(host, moveLimit);
        if (rv.size() <= moveLimit) {
            long byteLimit = (long) (byteLimitFactor * config.getBytesMovedFullRebalance());
            List<HostState> hostsSorted = sortHostsByDiskSpace(otherHosts);
            for (JobTask task : findTasksToMove(host, obeyDontAutobalanceMe)) {
                long taskTrueSize = getTaskTrueSize(task);
                if (limitBytes && (taskTrueSize > byteLimit)) {
                    continue;
                }
                JobTaskMoveAssignment assignment = moveTask(task, host.getHostUuid(), hostsSorted);
                if (assignment != null) {
                    markRecentlyReplicatedTo(assignment.getTargetUUID());
                    rv.add(assignment);
                    byteLimit -= taskTrueSize;
                }
                if (rv.size() >= moveLimit) {
                    break;
                }
            }
        }
        return rv;
    }

    /* Look through a hoststate to find tasks that don't correspond to an actual job or are on the wrong host */
    private List<JobTaskMoveAssignment> purgeMisplacedTasks(HostState host, int deleteLimit) {
        List<JobTaskMoveAssignment> rv = new ArrayList<>();
        for (JobKey key : host.allJobKeys()) {
            if (spawn.getJob(key) == null) {
                // Nonexistent job
                rv.add(new JobTaskMoveAssignment(key, host.getHostUuid(), null, false, true));
            } else {
                // Task has a copy on the wrong host. Do a fixTaskDir to ensure we aren't deleting the only remaining
                // copy
                JobTask task = spawn.getTask(key);
                if (!host.getHostUuid().equals(task.getHostUUID()) && !task.hasReplicaOnHost(host.getHostUuid())) {
                    spawn.fixTaskDir(key.getJobUuid(), key.getNodeNumber(), false, false);
                    deleteLimit -= 1;
                }
            }
            if (rv.size() >= deleteLimit) {
                break;
            }
        }
        return rv;
    }

    /**
     * Sorts the host based on their available disk space, from most space available to least
     *
     * @param hosts - the hosts to sort
     * @return the sorted list of hosts
     */
    @VisibleForTesting
    protected List<HostState> sortHostsByDiskSpace(Collection<HostState> hosts) {
        List<HostState> hostList = new ArrayList<>(hosts);
        removeDownHosts(hostList);
        Collections.sort(hostList, hostStateReplicationSuitabilityComparator);
        return hostList;
    }

    /**
     * Find suitable tasks to move off a host.
     *
     * @param host                  The hoststate to move tasks off of
     * @param obeyDontAutobalanceMe If true, obey the job parameter dontAutobalanceMe. See notes below.
     * @return A list of tasks that are suitable to be moved
     */
    private Iterable<JobTask> findTasksToMove(HostState host, boolean obeyDontAutobalanceMe) {
        Collection<JobTask> rv = new ArrayList<>();
        if (host != null) {
            String hostId = host.getHostUuid();
            for (JobKey jobKey : host.allJobKeys()) {
                Job job = spawn.getJob(jobKey);
                JobTask task = spawn.getTask(jobKey);
                if ((job != null) && (task != null) && isInMovableState(task)
                    // Only add non-null tasks that are either idle or queued
                    && (hostId.equals(task.getHostUUID()) || task.hasReplicaOnHost(
                        host.getHostUuid()))) // Only add tasks that are supposed to live on the specified host.
                {
                    if (obeyDontAutobalanceMe && job.getDontAutoBalanceMe()) {
                        // obeyDontAutobalanceMe is set to false when spawn is doing a filesystem-okay host failure.
                        // In this case, spawn needs to move the task even if the job owner specified no swapping,
                        // because the box is likely to be ailing/scheduled for decommission.
                        // All rebalancing actions use obeyDontAutobalanceMe=true and will conform to the job owner's
                        // wishes.
                        continue;
                    }
                    rv.add(task);
                }
            }
        }
        return rv;
    }

    public long getTaskTrueSize(JobTask task) {
        return taskSizer.estimateTrueSize(task);
    }

    /**
     * Given an ordered list of hosts, move the task to a suitable host, then move that host to the end of the list
     *
     * @param task       The task to move
     * @param fromHostId The task to move the host from
     * @param otherHosts The potential target hosts
     * @return An assignment describing how to move the task
     */
    @Nullable private JobTaskMoveAssignment moveTask(JobTask task,
                                                     String fromHostId,
                                                     Collection<HostState> otherHosts) {
        Iterator<HostState> hostStateIterator = otherHosts.iterator();
        String taskHost = task.getHostUUID();
        boolean live = task.getHostUUID().equals(fromHostId);
        if (!live && !task.hasReplicaOnHost(fromHostId)) {
            return null;
        }
        while (hostStateIterator.hasNext()) {
            HostState next = hostStateIterator.next();
            String nextId = next.getHostUuid();
            if (!taskHost.equals(nextId) && !task.hasReplicaOnHost(nextId) && next.canMirrorTasks() &&
                okToPutReplicaOnHost(next, task)) {
                hostStateIterator.remove();
                otherHosts.add(next);
                return new JobTaskMoveAssignment(task.getJobKey(), fromHostId, nextId, !live, false);
            }
        }
        return null;
    }

    @VisibleForTesting
    protected void markRecentlyReplicatedTo(String hostId) {
        if (hostId != null) {
            recentlyReplicatedToHosts.put(hostId, true);
        }
    }

    private static void removeDownHosts(Iterable<HostState> hosts) {
        Iterator<HostState> hostIter = hosts.iterator();
        while (hostIter.hasNext()) {
            HostState host = hostIter.next();
            if (host.isDead() || !host.isUp()) {
                hostIter.remove();
            }
        }
    }

    public static boolean isInMovableState(JobTask task) {
        return (task != null) && movableTaskStates.contains(task.getState());
    }

    /**
     * Is it acceptable to put a replica of this task on this host? (task can't have a live and replica version on
     * host)
     *
     * @param hostCandidate The host that is being considered to house the new replica
     * @param task          The task that might be replicated to that host
     * @return True if it is okay to put a replica on the host
     */
    private boolean okToPutReplicaOnHost(HostState hostCandidate, JobTask task) {
        Job job;
        String hostId;
        if ((hostCandidate == null) || ((hostId = hostCandidate.getHostUuid()) == null) ||
            !canReceiveNewTasks(hostCandidate) || ((job = spawn.getJob(task.getJobKey())) == null) ||
            !hostCandidate.getMinionTypes().contains(job.getMinionType())) {
            return false;
        }
        if (spawn.getHostFailWorker().getFailureState(hostId) != HostFailWorker.FailState.ALIVE) {
            return false;
        }
        HostState taskHost = hostManager.getHostState(task.getHostUUID());
        /* Protect against npe in case the existing host has disappeared somehow */
        String existingHost = (taskHost != null) ? taskHost.getHost() : null;
        /* in non-local-stack, prevent replicates to same host (multi-minion-per-host-setup) */
        if (!config.allowSameHostReplica() && hostCandidate.getHost().equals(existingHost)) {
            return false;
        }
        /* don't let the same minion have duplicate tasks */
        if (task.getHostUUID().equals(hostCandidate.getHostUuid()) ||
            task.hasReplicaOnHost(hostCandidate.getHostUuid())) {
            return false;
        }
        /* try not to put a task on a host if it would almost fill the host */
        if (taskSizer.estimateTrueSize(task) > (config.getHostDiskFactor() * getAvailDiskBytes(hostCandidate))) {
            return false;
        }
        return true;
    }

    /**
     * Decide if spawn is in a good state to perform an autobalance.
     *
     * @return True if it is okay to autobalance
     */
    public boolean okayToAutobalance() {
        // Don't autobalance if it is disabled, spawn is quiesced, or the number of queued tasks is high
        if ((config.getAutoBalanceLevel() == 0) ||
            spawn.getSystemManager().isQuiesced() ||
            (spawn.getLastQueueSize() > hostManager.listHostStatus(null).size())) {
            return false;
        }
        // Don't autobalance if there are still jobs in rebalance state
        for (Job job : spawn.listJobs()) {
            if (JobState.REBALANCE.equals(job.getState())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Find some task move assignments to autobalance the cluster
     *
     * @param type   Whether to balance hosts or jobs
     * @param weight Whether to balance light, medium, or heavy items of the chosen type
     * @return A list of assignments to perform the specified balancing operation
     */
    @Nullable public List<JobTaskMoveAssignment> getAssignmentsForAutoBalance(RebalanceType type,
                                                                              RebalanceWeight weight) {
        List<HostState> hosts = hostManager.getLiveHosts(null);
        switch (type) {
            case HOST:
                if (hosts.isEmpty()) {
                    return null;
                }
                List<HostState> hostsSorted = new ArrayList<>(hosts);
                Collections.sort(hostsSorted, hostStateScoreComparator);
                HostState hostToBalance = hostsSorted.get(getWeightedElementIndex(hostsSorted.size(), weight));
                return getAssignmentsToBalanceHost(hostToBalance,
                                                   hostManager.listHostStatus(hostToBalance.getMinionTypes()));
            case JOB:
                List<Job> autobalanceJobs = getJobsToAutobalance(hosts);
                if ((autobalanceJobs == null) || autobalanceJobs.isEmpty()) {
                    return null;
                }
                Job jobToBalance = autobalanceJobs.get(getWeightedElementIndex(autobalanceJobs.size(), weight));
                recentlyAutobalancedJobs.put(jobToBalance.getId(), true);
                return getAssignmentsForJobReallocation(jobToBalance, -1,
                                                        hostManager.listHostStatus(jobToBalance.getMinionType()));
            default:
                throw new IllegalArgumentException("unknown rebalance type " + type);
        }
    }

    public List<Job> getJobsToAutobalance(List<HostState> hosts) {
        List<Job> autobalanceJobs = new ArrayList<>();
        for (Job job : spawn.listJobs()) {
            if (shouldAutobalanceJob(job, hosts)) {
                autobalanceJobs.add(job);
            }
        }
        Collections.sort(autobalanceJobs, jobAverageTaskSizeComparator);
        return autobalanceJobs;
    }

    public Map<Integer, List<String>> getAssignmentsForNewReplicas(IJob job) {
        return getAssignmentsForNewReplicas(job, -1);
    }

    /**
     * Given a job, decide where to put additional replicas so that every task will have its full quantity of replicas.
     *
     * @param job    The job in question (not altered)
     * @param taskID The task that needs replicas, or -1 for all tasks
     * @return Map sending nodeid => list of host IDs for which to make new replicas
     */
    public Map<Integer, List<String>> getAssignmentsForNewReplicas(IJob job, int taskID) {
        Map<Integer, List<String>> rv = new HashMap<>();
        if (job == null) {
            return rv;
        }
        int replicaCount = job.getReplicas();
        Map<String, Double> scoreMap = generateTaskCountHostScoreMap(job);
        PriorityQueue<HostAndScore> scoreHeap = new PriorityQueue<>(1, hostAndScoreComparator);
        for (Map.Entry<String, Double> entry : scoreMap.entrySet()) {
            scoreHeap.add(new HostAndScore(hostManager.getHostState(entry.getKey()), entry.getValue()));
        }
        Map<String, Integer> allocationMap = new HashMap<>();
        List<JobTask> tasks = (taskID > 0) ? Collections.singletonList(job.getTask(taskID)) : job.getCopyOfTasks();
        for (JobTask task : tasks) {
            int numExistingReplicas = task.getReplicas() != null ? task.getReplicas().size() : 0;
            List<String> hostIDsToAdd = new ArrayList<>(replicaCount);
            // Add new replicas as long as the task needs them & there are remaining hosts
            for (int i = 0; i < (replicaCount - numExistingReplicas); i++) {
                for (HostAndScore hostAndScore : scoreHeap) {
                    HostState candidateHost = hostAndScore.host;
                    if ((candidateHost == null) || !candidateHost.canMirrorTasks()) {
                        continue;
                    }
                    int currentCount;
                    if (allocationMap.containsKey(candidateHost.getHostUuid())) {
                        currentCount = allocationMap.get(candidateHost.getHostUuid());
                    } else {
                        currentCount = 0;
                    }

                    if (okToPutReplicaOnHost(candidateHost, task)) {
                        hostIDsToAdd.add(candidateHost.getHostUuid());
                        scoreHeap.remove(hostAndScore);
                        scoreHeap.add(new HostAndScore(candidateHost, hostAndScore.score + 1));
                        allocationMap.put(candidateHost.getHostUuid(), currentCount + 1);
                        break;
                    }
                }
            }
            if (!hostIDsToAdd.isEmpty()) {
                rv.put(task.getTaskID(), hostIDsToAdd);
            }
        }
        return rv;
    }

    /**
     * Count the number of tasks per host for a single job, then add in a small factor for how heavily weighted each
     * host's disk is
     *
     * @param job The job to count
     * @return A map describing how heavily a job is assigned to each of its hosts
     */
    private Map<String, Double> generateTaskCountHostScoreMap(IJob job) {
        Map<String, Double> rv = new HashMap<>();
        if (job != null) {
            List<JobTask> tasks = job.getCopyOfTasks();
            for (JobTask task : tasks) {
                rv.put(task.getHostUUID(), addOrIncrement(rv.get(task.getHostUUID()), 1d));
                if (task.getReplicas() == null) {
                    continue;
                }
                for (JobTaskReplica replica : task.getReplicas()) {
                    rv.put(replica.getHostUUID(), addOrIncrement(rv.get(replica.getHostUUID()), 1d));
                }
            }
            for (HostState host : hostManager.listHostStatus(job.getMinionType())) {
                if (host.isUp() && !host.isDead()) {
                    double availDisk = 1 - getUsedDiskPercent(host);
                    rv.put(host.getHostUuid(), addOrIncrement(rv.get(host.getHostUuid()), availDisk));
                }
            }
        }
        return rv;
    }

    private static double addOrIncrement(Double currentValue, Double value) {
        if (currentValue != null) {
            return currentValue + value;
        } else {
            return value;
        }
    }

    public void requestJobSizeUpdate(String jobId, int taskId) {
        taskSizer.requestJobSizeFetch(jobId, taskId);
    }

    public SpawnBalancerConfig getConfig() {
        return config;
    }

    public void setConfig(SpawnBalancerConfig config) {
        this.config = config;
    }

    public void clearRecentlyRebalancedHosts() {
        recentlyBalancedHosts.invalidateAll();
    }

    /**
     * Given a list of tasks with lives/replicas on a failed host, fix all of the tasks
     *
     * @param hosts      All available hosts in the cluster of the appropriate type
     * @param failedHost The id of the host being failed
     */
    public void fixTasksForFailedHost(List<HostState> hosts, String failedHost) {
        List<JobTask> tasks = findAllTasksAssignedToHost(failedHost);
        List<JobTask> sortedTasks = new ArrayList<>(tasks);
        Collections.sort(sortedTasks,
                         (o1, o2) -> Long.compare(taskSizer.estimateTrueSize(o1), taskSizer.estimateTrueSize(o2)));
        hosts = sortHostsByDiskSpace(hosts);
        Collection<String> modifiedJobIds = new HashSet<>();
        for (JobTask task : sortedTasks) {
            modifiedJobIds.add(task.getJobUUID());
            try {
                attemptFixTaskForFailedHost(task, hosts, failedHost);
            } catch (Exception ex) {
                log.warn("Warning: failed to recover task {}", task.getJobKey(), ex);
            }
        }
        for (String jobId : modifiedJobIds) {
            try {
                spawn.updateJob(spawn.getJob(jobId));
            } catch (Exception e) {
                log.warn("Warning: failed to update job: {}", jobId, e);
            }
        }
    }

    private List<JobTask> findAllTasksAssignedToHost(String failedHostUUID) {
        List<JobTask> rv = new ArrayList<>();
        spawn.acquireJobLock();
        try {
            for (Job job : spawn.listJobs()) {
                if (job != null) {
                    for (JobTask task : job.getCopyOfTasks()) {
                        if ((task != null) &&
                            (task.getHostUUID().equals(failedHostUUID) || task.hasReplicaOnHost(failedHostUUID))) {
                            rv.add(task);
                        }
                    }
                }
            }
            return rv;
        } finally {
            spawn.releaseJobLock();
        }
    }

    /**
     * For a particular task with a copy on a failed host, attempt to find a suitable replica; mark it degraded
     * otherwise
     *
     * @param task           The task to modify
     * @param hosts          A list of available hosts
     * @param failedHostUuid The host being failed
     */
    private void attemptFixTaskForFailedHost(JobTask task, Collection<HostState> hosts, String failedHostUuid) {
        Iterator<HostState> hostIterator = hosts.iterator();
        Job job;
        if ((task == null) || (task.getJobUUID() == null) || ((job = spawn.getJob(task.getJobUUID())) == null)) {
            log.warn("Skipping nonexistent job for task {} during host fail.", task);
            return;
        }
        if (!task.getHostUUID().equals(failedHostUuid) && !task.hasReplicaOnHost(failedHostUuid)) {
            // This task was not actually assigned to the failed host. Nothing to do.
            return;
        }
        if (!spawn.isNewTask(task) && ((task.getReplicas() == null) || task.getReplicas().isEmpty())) {
            log.warn("Found no replica for task {}", task.getJobKey());
            job.setState(JobState.DEGRADED, true);
            return;
        }
        while (hostIterator.hasNext()) {
            HostState host = hostIterator.next();
            if (host.getHostUuid().equals(failedHostUuid)) {
                continue;
            }
            if (host.canMirrorTasks() && okToPutReplicaOnHost(host, task)) {
                // Host found! Move this host to the end of the host list so we don't immediately pick it again
                hostIterator.remove();
                hosts.add(host);
                executeHostFailureRecovery(task, failedHostUuid, host);
                return;
            }
        }
        log.warn("Failed to find a host that could hold {} after host failure", task.getJobKey());
        job.setState(JobState.DEGRADED, true);
    }

    /**
     * Modify the live/replica copies of a task to handle a failed host
     *
     * @param task           The task to be modified
     * @param failedHostUuid The host being failed
     * @param newReplicaHost A host that will receive a new copy of the data
     */
    private void executeHostFailureRecovery(JobTask task, String failedHostUuid, CoreMessage newReplicaHost) {
        boolean liveOnFailedHost = task.getHostUUID().equals(failedHostUuid);
        String newReplicaUuid = newReplicaHost.getHostUuid();
        if (liveOnFailedHost) {
            if (spawn.isNewTask(task)) {
                // Task has never run before. Just switch to the new host.
                task.setHostUUID(newReplicaHost.getHostUuid());
            } else {
                // Send a kill message if the task is running on the failed host
                spawn.sendControlMessage(
                        new CommandTaskStop(failedHostUuid, task.getJobUUID(), task.getTaskID(), 0, true, false));
                // Find a replica, promote it, and tell it to replicate to the new replica on completion
                String chosenReplica = task.getReplicas().get(0).getHostUUID();
                task.replaceReplica(chosenReplica, newReplicaUuid);
                task.setHostUUID(chosenReplica);
                spawn.replicateTask(task, Collections.singletonList(newReplicaUuid));
            }
        } else {
            // Replace the replica on the failed host with one on a new host
            task.replaceReplica(failedHostUuid, newReplicaUuid);
            if (!spawn.isNewTask(task)) {
                spawn.replicateTask(task, Collections.singletonList(newReplicaUuid));
            }
        }
    }

    /**
     * Makes the specified number of tasks for the given job ID, and assigns each task to a suitable host.
     *
     * @param jobID     the job in question
     * @param taskCount how many tasks should be created
     * @param hosts     a current set of hosts
     * @return a list of tasks with HostID set.
     */
    public List<JobTask> generateAssignedTasksForNewJob(String jobID, int taskCount, Collection<HostState> hosts)
            throws Exception {
        List<JobTask> tasks = generateUnassignedTaskList(jobID, taskCount);
        Map<JobTask, String> hostAssignments =
                assignTasksFromSingleJobToHosts(tasks, generateHostStateScoreMap(hosts, null));
        List<JobTask> rv = new ArrayList<>(tasks.size());
        for (Map.Entry<JobTask, String> entry : hostAssignments.entrySet()) {
            JobTask task = entry.getKey();
            String hostID = entry.getValue();
            if (hostID == null) {
                throw new RuntimeException("Unable to allocate job tasks because no suitable host was found");
            }
            task.setHostUUID(hostID);
            rv.add(task);
        }
        return rv;
    }

    /**
     * Makes a list of empty tasks which will be assigned to hosts later.
     */
    private static List<JobTask> generateUnassignedTaskList(String jobID, int taskCount) {
        List<JobTask> rv = new ArrayList<>(Math.max(0, taskCount));
        for (int i = 0; i < taskCount; i++) {
            JobTask task = new JobTask();
            task.setJobUUID(jobID);
            task.setTaskID(i);
            rv.add(task);
        }
        return rv;
    }

    /**
     * Advises Spawn how to reallocate a job, sending some tasks to different hosts
     *
     * @param job         The job being reallocated
     * @param tasksToMove The number of tasks to move. If <= 0, use the default.
     * @param hosts       The available target hosts
     * @return a map assigning a good host for each jobtask
     */
    public List<JobTaskMoveAssignment> getAssignmentsForJobReallocation(Job job,
                                                                        int tasksToMove,
                                                                        List<HostState> hosts) {
        int maxTasksToMove = (tasksToMove > 0) ? tasksToMove : config.getTasksMovedFullRebalance();
        List<JobTaskMoveAssignment> candidateAssignments = new ArrayList<>();
        // Count the number of tasks per host
        JobTaskItemByHostMap tasksByHost = generateTaskCountByHost(hosts, job.getCopyOfTasks());
        // Find the max number of tasks each host should have
        int maxPerHost = maxTasksPerHost(job, tasksByHost.size());
        if (log.isDebugEnabled()) {
            log.debug("Rebalancing job: {} maxTasksToMove={} maxPerHost={}", job.getId(), maxTasksToMove, maxPerHost);
        }
        while (candidateAssignments.size() < maxTasksToMove) {
            MoveAssignmentList moves = null;
            List<String> hostsSorted = tasksByHost.generateHostsSorted();
            String hostWithMost = hostsSorted.get(hostsSorted.size() - 1);
            String hostWithLeast = hostsSorted.get(0);
            int mostTasksOnHost = tasksByHost.findMostTasksOnHost();
            int leastTasksOnHost = tasksByHost.findLeastTasksOnHost();
            boolean isExtremeHost = isExtremeHost(hostWithMost, true, true);
            if (log.isDebugEnabled()) {
                log.debug(
                        "hostsSorted.size={} hostWithMost:{} hostWithLeast:{} mostTasksOnHost: {} leastTasksOnHost: {}",
                        hostsSorted.size(), hostWithMost, hostWithLeast, mostTasksOnHost, leastTasksOnHost);
            }

            // If any host has more than the max number, move some tasks off that host
            if (mostTasksOnHost > maxPerHost) {
                moves = moveTasksOffHost(tasksByHost, maxPerHost, 1, -1, hostWithMost);
            } else if (leastTasksOnHost < (maxPerHost -
                                           1)) { // If a host has significantly fewer than the max number, move some
                // tasks onto that host
                moves = moveTasksOntoHost(tasksByHost, maxPerHost, 1, -1, hostWithLeast);
            } else if (isExtremeHost) { // If a host with many tasks is heavily weighted, move a task off that host
                moves = moveTasksOffHost(tasksByHost, maxPerHost, 1, -1, hostWithMost);
            }
            if ((moves == null) || moves.isEmpty()) {
                break;
            } else {
                candidateAssignments.addAll(moves);
            }
        }
        if (candidateAssignments.size() > maxTasksToMove) {
            candidateAssignments = candidateAssignments.subList(0, maxTasksToMove);
        }
        candidateAssignments = removeDuplicateAssignments(candidateAssignments);
        return pruneTaskReassignments(candidateAssignments);
    }

    /**
     * Advises Spawn how to reallocate a host, pushing or pulling jobs to balance the number of tasks run by each
     * machine.
     *
     * @param host  The particular host to consider
     * @param hosts All available hosts; should include host
     * @return a (possibly empty) map specifying some tasks and advised destinations for those tasks
     */
    public List<JobTaskMoveAssignment> getAssignmentsToBalanceHost(HostState host, List<HostState> hosts) {
        String hostID = host.getHostUuid();
        List<JobTaskMoveAssignment> rv = new ArrayList<>();
        if ((hosts == null) || hosts.isEmpty()) {
            log.warn("[spawn.balancer] {} reallocation failed: host list empty", hostID);
            return rv;
        }
        List<HostState> sortedHosts = sortHostsByDiskSpace(hosts);
        HostFailWorker.FailState failState = spawn.getHostFailWorker().getFailureState(hostID);
        int numAlleviateHosts = (int) Math.ceil(sortedHosts.size() * config.getAlleviateHostPercentage());
        if ((failState == HostFailWorker.FailState.FAILING_FS_OKAY) || isExtremeHost(hostID, true, true) ||
            (getUsedDiskPercent(host) > (1 - config.getMinDiskPercentAvailToReceiveNewTasks()))) {
            // Host disk is overloaded
            log.info("[spawn.balancer] {} categorized as overloaded host; looking for tasks to push off of it", hostID);
            List<HostState> lightHosts = sortedHosts.subList(0, numAlleviateHosts);
            rv.addAll(pushTasksOffHost(host, lightHosts, true, 1, config.getTasksMovedFullRebalance(), true));
        } else if (isExtremeHost(hostID, true, false)) {
            // Host disk is underloaded
            log.info("[spawn.balancer] {} categorized as underloaded host; looking for tasks to pull onto it", hostID);
            List<HostState> heavyHosts =
                    Lists.reverse(sortedHosts.subList(sortedHosts.size() - numAlleviateHosts, sortedHosts.size()));
            pushTasksOntoDisk(host, heavyHosts);
        } else if (isExtremeHost(hostID, false, true)) {
            // Host is overworked
            log.info("[spawn.balance] {} categorized as overworked host; looking for tasks to push off it", hostID);
            rv.addAll(balanceActiveJobsOnHost(host, hosts));
        }
        if (rv.isEmpty()) {
            rv.addAll(balanceActiveJobsOnHost(host, hosts));
        }
        return pruneTaskReassignments(rv);
    }

    /**
     * Sorts the hosts based on their load as measured by hostScores, lightest to heaviest.
     *
     * @param hosts - the hosts to sort
     * @return the sorted list of hosts, light to heavy.
     */
    public List<HostState> sortHostsByActiveTasks(Collection<HostState> hosts) {
        List<HostState> hostList = new ArrayList<>(hosts);
        removeDownHosts(hostList);
        Collections.sort(hostList, (hostState, hostState1) ->
                Double.compare(countTotalActiveTasksOnHost(hostState), countTotalActiveTasksOnHost(hostState1)));
        return hostList;
    }

    private int countTotalActiveTasksOnHost(HostState host) {
        int count = 0;
        if (host != null) {
            host.generateJobTaskCountMap();
            aggregateStatisticsLock.lock();
            try {
                if (activeJobIDs == null) {
                    activeJobIDs = findActiveJobIDs();
                }
                for (String jobID : activeJobIDs) {
                    count += host.getTaskCount(jobID);
                }
            } finally {
                aggregateStatisticsLock.unlock();
            }
        }

        return count;
    }

    private Set<String> findActiveJobIDs() {
        aggregateStatisticsLock.lock();
        try {
            activeJobIDs = new HashSet<>();
            Collection<Job> jobs = spawn.listJobs();
            if ((jobs != null) && !jobs.isEmpty()) {
                for (Job job : jobs) {
                    if (isWellFormedAndActiveJob(job)) {
                        activeJobIDs.add(job.getId());
                    }
                }
            }
            return new HashSet<>(activeJobIDs);
        } finally {
            aggregateStatisticsLock.unlock();
        }

    }

    /**
     * Is this job non-null and has it run recently?
     */
    private boolean isWellFormedAndActiveJob(IJob job) {
        long earliestActiveTime = JitterClock.globalTime() - config.getActiveTaskMilliCutoff();
        return (job != null) && (job.getStartTime() != null) && (job.getStartTime() > earliestActiveTime);
    }

    /**
     * Given a job, for each task, remove duplicate replicas, replicas pointing at the live host
     *
     * @param job The job to modify
     */
    public void removeInvalidReplicas(IJob job) {
        if ((job != null) && (job.getCopyOfTasks() != null)) {
            List<JobTask> tasks = job.getCopyOfTasks();
            for (JobTask task : tasks) {
                List<JobTaskReplica> newReplicas = new ArrayList<>(job.getReplicas());
                if (task.getReplicas() != null) {
                    Iterable<JobTaskReplica> oldReplicas = new ArrayList<>(task.getReplicas());
                    for (JobTaskReplica replica : oldReplicas) {
                        Collection<String> replicasSeen = new ArrayList<>();
                        String replicaHostID = replica.getHostUUID();
                        if (hostManager.getHostState(replicaHostID) == null) {
                            log.warn("[spawn.balancer] removing replica for missing host {}", replicaHostID);
                        } else if (replicaHostID.equals(task.getHostUUID()) || replicasSeen.contains(replicaHostID)) {
                            log.warn("[spawn.balancer] removing erroneous replica for {} on {}",
                                     task.getJobKey(), replicaHostID);
                        } else if (!config.allowSameHostReplica() && onSameHost(replicaHostID, task.getHostUUID())) {
                            log.warn("[spawn.balancer] removing replica on same host for {}live={} replica={}",
                                     task.getJobKey(), task.getHostUUID(), replicaHostID);
                        } else {
                            replicasSeen.add(replicaHostID);
                            newReplicas.add(replica);
                        }
                    }
                }
                task.setReplicas(newReplicas);
            }
        }
    }

    private boolean onSameHost(String hostID1, String hostID2) {
        HostState host1 = hostManager.getHostState(hostID1);
        HostState host2 = hostManager.getHostState(hostID2);
        if ((host1 == null) || (host2 == null)) {
            return false;
        } else {
            return host1.getHost().equals(host2.getHost());
        }
    }

    /**
     * Check the live and replica hosts for a given task to see whether any of these hosts has a nearly-full disk
     *
     * @param task The task to check
     * @return True if at least one host is near full
     */
    protected boolean hasFullDiskHost(JobTask task) {
        Collection<HostState> hostsToCheck = new ArrayList<>();
        hostsToCheck.add(hostManager.getHostState(task.getHostUUID()));
        if (task.getReplicas() != null) {
            for (JobTaskReplica replica : task.getReplicas()) {
                if (replica != null) {
                    hostsToCheck.add(hostManager.getHostState(replica.getHostUUID()));
                }
            }
            for (HostState host : hostsToCheck) {
                if ((host != null) && isDiskFull(host)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does this host have a nearly full disk?
     *
     * @param host The host to check
     * @return True if the disk is nearly full
     */
    public boolean isDiskFull(HostState host) {
        boolean full = (host != null) && (getUsedDiskPercent(host) > (1 - config.getMinDiskPercentAvailToRunJobs()));
        if (full) {
            log.warn("[spawn.balancer] Host {} with uuid {} is nearly full, with used disk {}", host.getHost(),
                     host.getHostUuid(), getUsedDiskPercent(host));
        }
        return full;
    }

    /**
     * Update SpawnBalancer's cluster-wide metrics, including host scores and active jobs.
     *
     * @param hosts A list of HostStates
     */
    protected void updateAggregateStatistics(Iterable<HostState> hosts) {
        spawn.acquireJobLock();
        try {
            aggregateStatisticsLock.lock();
            try {
                lastAggregateStatUpdateTime = JitterClock.globalTime();
                findActiveJobIDs();
                double maxMeanActive = -1;
                double maxDiskPercentUsed = -1;
                for (HostState host : hosts) {
                    maxMeanActive = Math.max(maxMeanActive, host.getMeanActiveTasks());
                    maxDiskPercentUsed = Math.max(maxDiskPercentUsed, getUsedDiskPercent(host));
                }
                for (HostState host : hosts) {
                    cachedHostScores.put(host.getHostUuid(),
                                         calculateHostScore(host, maxMeanActive, maxDiskPercentUsed));
                }
            } finally {
                aggregateStatisticsLock.unlock();
            }
        } finally {
            spawn.releaseJobLock();
        }
    }

    private HostScore calculateHostScore(HostState host, double clusterMaxMeanActive, double clusterMaxDiskUsed) {
        double score = 0;
        double meanActive = host.getMeanActiveTasks();
        double usedDiskPercent = getUsedDiskPercent(host);
        // If either metric is zero across the whole cluster, treat every host as having full load in that aspect
        if (clusterMaxMeanActive <= 0) {
            meanActive = 1;
            clusterMaxMeanActive = 1;
        }
        if (clusterMaxDiskUsed <= 0) {
            usedDiskPercent = 1;
            clusterMaxDiskUsed = 1;
        }
        int activeTaskWeight = config.getActiveTaskWeight();
        int diskUsedWeight = config.getDiskUsedWeight();
        // Assemble the score as a combination of the mean active tasks and the disk used
        score += activeTaskWeight * Math.pow(meanActive / clusterMaxMeanActive, 2.5);
        score += diskUsedWeight * Math.pow(usedDiskPercent / clusterMaxDiskUsed, 2.5);
        // If host is very full, make sure to give the host a big score
        score = Math.max(score, (activeTaskWeight + diskUsedWeight) * usedDiskPercent);
        return new HostScore(meanActive, usedDiskPercent, score);
    }

    /**
     * Is this host's load significantly different from the rest of the cluster?
     *
     * @param hostID    The host to check
     * @param diskSpace Whether to consider load based on disk space (as opposed to number of tasks)
     * @param high      Whether to look for heavy load as opposed to light load
     * @return True if the host has the specified level of load
     */
    protected boolean isExtremeHost(@Nullable String hostID, boolean diskSpace, boolean high) {
        aggregateStatisticsLock.lock();
        try {
            if ((hostID == null) || (cachedHostScores == null) || !cachedHostScores.containsKey(hostID) ||
                cachedHostScores.isEmpty()) {
                return false;
            }
            double clusterAverage = 0;
            for (HostScore score : cachedHostScores.values()) {
                clusterAverage += score.getScoreValue(diskSpace);
            }
            clusterAverage /= cachedHostScores.size(); // Nonzero as we check if cachedHostScores.isEmpty first
            double hostValue = cachedHostScores.get(hostID).getScoreValue(diskSpace);
            return (high && (hostValue > (clusterAverage * config.getExtremeHostRatio()))) ||
                   (!high && (hostValue < (clusterAverage / config.getExtremeHostRatio())));
        } finally {
            aggregateStatisticsLock.unlock();
        }

    }

    /* Pull tasks off the given host and move them elsewhere, obeying the maxPerHost and maxBytesToMove limits */
    private MoveAssignmentList moveTasksOffHost(JobTaskItemByHostMap tasksByHost,
                                                int maxPerHost,
                                                int numToMove,
                                                long maxBytesToMove,
                                                String pushHost) {
        if (log.isDebugEnabled()) {
            log.debug("received move assignment maxPerHost:{} numToMove:{} pushHost:{}", maxPerHost, numToMove,
                      pushHost);
        }
        MoveAssignmentList rv = new MoveAssignmentList(spawn, taskSizer);
        Collection<JobKey> alreadyMoved = new HashSet<>();
        Iterator<String> otherHosts = tasksByHost.getHostIterator(true);
        while (otherHosts.hasNext() && (rv.size() < numToMove)) {
            String pullHost = otherHosts.next();
            if (pushHost.equals(pullHost)) {
                continue;
            }
            HostState pullHostState = hostManager.getHostState(pullHost);
            Iterator<JobTaskItem> itemIterator = new ArrayList<>(tasksByHost.get(pushHost)).iterator();
            while (itemIterator.hasNext() && (rv.size() < numToMove) &&
                   (tasksByHost.get(pullHost).size() < maxPerHost)) {
                JobTaskItem nextTaskItem = itemIterator.next();
                long trueSizeBytes = taskSizer.estimateTrueSize(nextTaskItem.getTask());
                JobKey jobKey = nextTaskItem.getTask().getJobKey();
                Job job = spawn.getJob(jobKey);
                if ((job == null) || !pullHostState.getMinionTypes().contains(job.getMinionType())) {
                    continue;
                }
                // Reject the move if the target host is heavily loaded, already has a copy of the task, or the task
                // is too large
                if (isExtremeHost(pullHost, true, true) || pullHostState.hasLive(jobKey) ||
                    ((maxBytesToMove > 0) && (trueSizeBytes > maxBytesToMove))) {
                    if (log.isDebugEnabled()) {
                        log.debug("Unable to move task to host {} fullDisk={} alreadyLive={} byteCount={}>{} {}",
                                  pullHost, isExtremeHost(pullHost, true, true), pullHostState.hasLive(jobKey),
                                  trueSizeBytes, maxBytesToMove, trueSizeBytes > maxBytesToMove);
                    }
                    continue;
                }
                if (!alreadyMoved.contains(jobKey) && (pullHost != null) &&
                    tasksByHost.moveTask(nextTaskItem, pushHost, pullHost)) {
                    rv.add(new JobTaskMoveAssignment(nextTaskItem.getTask().getJobKey(), pushHost, pullHost, false,
                                                     false));
                    alreadyMoved.add(nextTaskItem.getTask().getJobKey());
                    maxBytesToMove -= trueSizeBytes;
                }
            }
        }
        return rv;
    }

    /* Push tasks onto the given host, obeying the maxPerHost and maxBytesToMove limits */
    private MoveAssignmentList moveTasksOntoHost(JobTaskItemByHostMap tasksByHost,
                                                 int maxPerHost,
                                                 int numToMove,
                                                 long maxBytesToMove,
                                                 String pullHost) {
        MoveAssignmentList rv = new MoveAssignmentList(spawn, taskSizer);
        Collection<JobKey> alreadyMoved = new HashSet<>();
        Iterator<String> otherHosts = tasksByHost.getHostIterator(false);
        if (isExtremeHost(pullHost, true, true)) {
            return rv;
        }
        if (isExtremeHost(pullHost, false, true)) {
            numToMove = Math.max(1, numToMove / 2); // Move fewer tasks onto a host if it's already doing a lot of work
        }
        HostState pullHostState = hostManager.getHostState(pullHost);
        if (pullHostState == null) {
            return rv;
        }
        while (otherHosts.hasNext() && (rv.size() < numToMove) && (tasksByHost.get(pullHost).size() < maxPerHost)) {
            String pushHost = otherHosts.next();
            if ((pushHost == null) || pushHost.equals(pullHost)) {
                continue;
            }
            Collection<JobTaskItem> pushHostItems = new ArrayList<>(tasksByHost.get(pushHost));
            if (pushHostItems.size() < maxPerHost) {
                break;
            }
            for (JobTaskItem item : pushHostItems) {
                JobKey jobKey = item.getTask().getJobKey();
                Job job = spawn.getJob(jobKey);
                if ((job == null) || !pullHostState.getMinionTypes().contains(job.getMinionType())) {
                    continue;
                }
                long trueSizeBytes = taskSizer.estimateTrueSize(item.getTask());
                if (pullHostState.hasLive(item.getTask().getJobKey()) ||
                    ((maxBytesToMove > 0) && (trueSizeBytes > maxBytesToMove))) {
                    continue;
                }
                if (!alreadyMoved.contains(jobKey) && tasksByHost.moveTask(item, pushHost, pullHost)) {
                    rv.add(new JobTaskMoveAssignment(item.getTask().getJobKey(), pushHost, pullHost, false, false));
                    alreadyMoved.add(item.getTask().getJobKey());
                    maxBytesToMove -= trueSizeBytes;
                }
                if (rv.size() >= numToMove) {
                    break;
                }
            }
        }
        return rv;
    }

    /* Count the number of tasks that live on each host */
    private JobTaskItemByHostMap generateTaskCountByHost(List<HostState> hosts, Iterable<JobTask> tasks) {
        JobTaskItemByHostMap rv = new JobTaskItemByHostMap(this, hosts, config.getTasksMovedPerUnspecifiedHost(),
                                                           config.getTasksMovedPerUnspecifiedHost());
        for (JobTask task : tasks) {
            rv.addLiveAndReplicasForTask(task);
        }
        return rv;
    }

    private List<JobTaskMoveAssignment> pushTasksOntoDisk(HostState host, Iterable<HostState> heavyHosts) {
        MoveAssignmentList moveAssignments = new MoveAssignmentList(spawn, taskSizer);
        for (HostState heavyHost : heavyHosts) {
            double byteLimitFactor =
                    1 - ((double) moveAssignments.getBytesUsed() / config.getBytesMovedFullRebalance());
            moveAssignments.addAll(pushTasksOffHost(heavyHost, Collections.singletonList(host), true, byteLimitFactor,
                                                    config.getTasksMovedFullRebalance(), true));
        }
        moveAssignments.addAll(purgeMisplacedTasks(host, 1));
        return moveAssignments;
    }

    /* For each active job, ensure that the given host has a fair share of tasks from that job */
    private Collection<JobTaskMoveAssignment> balanceActiveJobsOnHost(HostState host, List<HostState> hosts) {
        int totalTasksToMove = config.getTasksMovedFullRebalance();
        long totalBytesToMove = config.getBytesMovedFullRebalance();
        Set<String> activeJobs = findActiveJobIDs();
        List<JobTaskMoveAssignment> rv = purgeMisplacedTasks(host, 1);
        String hostID = host.getHostUuid();
        for (String jobID : activeJobs) {
            spawn.acquireJobLock();
            try {
                Job job = spawn.getJob(jobID);
                if (job != null) {
                    JobTaskItemByHostMap tasksByHost =
                            new JobTaskItemByHostMap(this, hosts, config.getTasksMovedPerUnspecifiedHost(),
                                                     config.getTasksMovedPerUnspecifiedHost());
                    for (JobTask task : job.getCopyOfTasks()) {
                        tasksByHost.addLiveAndReplicasForTask(task);
                    }
                    int maxPerHost = maxTasksPerHost(job, hosts.size());
                    int numExistingTasks = tasksByHost.get(hostID).size();
                    if ((tasksByHost.findLeastTasksOnHost() >= (maxPerHost - 1)) ||
                        (tasksByHost.findMostTasksOnHost() <= maxPerHost)) {
                        continue;
                    }
                    boolean pushFrom = (numExistingTasks > maxPerHost) || ((numExistingTasks == maxPerHost) &&
                                                                           (tasksByHost.findLeastTasksOnHost() <
                                                                            (maxPerHost - 1)));
                    if (totalTasksToMove > 0) {
                        MoveAssignmentList assignments = pushFrom ?
                                                         moveTasksOffHost(tasksByHost, maxPerHost, 1, totalBytesToMove,
                                                                          host.getHostUuid())
                                                                  :
                                                         moveTasksOntoHost(tasksByHost, maxPerHost, 1, totalBytesToMove,
                                                                           host.getHostUuid());
                        rv.addAll(assignments);
                        totalTasksToMove -= assignments.size();
                        totalBytesToMove -= assignments.getBytesUsed();
                    } else {
                        break;
                    }
                }
            } finally {
                spawn.releaseJobLock();
            }
        }
        return rv;
    }

    /**
     * Prune a tentative list of task reassignments, removing illegal moves or moves to overburdened hosts
     *
     * @param candidateAssignments The initial list of assignments
     * @return A list of assignments with illogical moves removed
     */
    private List<JobTaskMoveAssignment> pruneTaskReassignments(Iterable<JobTaskMoveAssignment> candidateAssignments) {
        List<JobTaskMoveAssignment> rv = new ArrayList<>();
        Map<String, Boolean> snapshot = new HashMap<>(recentlyBalancedHosts.asMap());
        for (JobTaskMoveAssignment assignment : candidateAssignments) {
            String newHostID = assignment.getTargetUUID();
            JobKey jobKey = assignment.getJobKey();
            String jobID = (jobKey == null) ? null : jobKey.getJobUuid();
            if (isExtremeHost(newHostID, true, true)) {
                log.warn("[spawn.balancer] decided not to move task from job {} to host {} " +
                         "because it is already heavily loaded", jobID, newHostID);
                continue;
            }
            HostState newHost = hostManager.getHostState(newHostID);
            if ((newHost == null) || newHost.hasLive(jobKey) ||
                !canReceiveNewTasks(newHost)) {
                log.warn("[spawn.balancer] decided not to move task from job {} to host {} " +
                         "because it cannot receive the new task", jobID, newHostID);
                continue;
            }
            if (snapshot.containsKey(newHostID)) {
                log.warn("[spawn.balancer] decided not to move task from job {} to host {} " +
                         "because it already received a different task recently", jobID, newHostID);
                continue;
            }
            rv.add(assignment);
            recentlyBalancedHosts.put(newHostID, true);
        }
        return rv;
    }

    private static List<JobTaskMoveAssignment> removeDuplicateAssignments(Iterable<JobTaskMoveAssignment>
                                                                                  candidateAssignments) {
        Collection<JobKey> movedTasks = new HashSet<>();
        List<JobTaskMoveAssignment> rv = new ArrayList<>();
        for (JobTaskMoveAssignment assignment : candidateAssignments) {
            JobKey jobKey = assignment.getJobKey();
            if (!movedTasks.contains(jobKey)) {
                rv.add(assignment);
                movedTasks.add(jobKey);
            }
        }
        return rv;
    }

    /**
     * Assuming a job has n tasks, each with R replicas, and there are m hosts available, each host should have no more
     * than n*(1+R)/m, rounded up.
     */
    private static int maxTasksPerHost(Job job, int numHosts) {
        if (job == null) {
            return 0;
        }
        numHosts = Math.max(1, numHosts);
        return (int) Math.ceil((double) (job.getTaskCount() * (1 + job.getReplicas())) / numHosts);
    }

    private static int getWeightedElementIndex(int numItems, RebalanceWeight weight) {
        switch (weight) {
            case LIGHT:
                return 0;
            case MEDIUM:
                return numItems / 2;
            case HEAVY:
                return numItems - 1;
            default:
                throw new IllegalArgumentException("unknown weight type " + weight);
        }
    }

}
