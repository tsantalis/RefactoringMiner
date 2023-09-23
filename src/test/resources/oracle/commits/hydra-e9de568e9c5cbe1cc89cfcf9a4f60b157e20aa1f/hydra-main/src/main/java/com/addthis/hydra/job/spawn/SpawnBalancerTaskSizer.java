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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.addthis.basis.net.HttpUtil;
import com.addthis.basis.util.Parameter;

import com.addthis.hydra.job.Job;
import com.addthis.hydra.job.JobTask;
import com.addthis.hydra.job.mq.HostState;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class in charge of estimating the full size of job tasks, since JobTask.getByteCount does not include backups.
 * <p/>
 * This class uses a simplifying assumption that all tasks within a single job have the same ratio between the actual
 * byte count and the expected the byte count, which is empirically true in nearly all cases. It is also assumed that
 * this ratio will remain roughly constant on the time scale of hours.
 */
public class SpawnBalancerTaskSizer {
    private static final Logger log = LoggerFactory.getLogger(SpawnBalancerTaskSizer.class);

    private static final double defaultRatio = Double.parseDouble(Parameter.value("spawn.balancer.task.sizer.defaultratio", "1.5"));
    private static final double maxRatio = Double.parseDouble(Parameter.value("spawn.balancer.task.sizer.maxratio", "10.0"));
    private static final double minRatio = Double.parseDouble(
            Parameter.value("spawn.balancer.task.sizer.minratio", "1.0"));
    private static final long queueConsumptionInterval = Parameter.longValue("spawn.balancer.task.sizer.interval",
                                                                             60 * 1000);
    private static final int ratioExpirationHours = Parameter.intValue("spawn.balancer.task.sizer.ratio.expire", 12);

    private static final AtomicBoolean pollingStarted = new AtomicBoolean(false);

    private final Spawn spawn;
    private final HostManager hostManager;
    private final LinkedHashMap<String, Integer> queuedJobIds;
    private final Cache<String, Double> cachedJobRatios;

    public SpawnBalancerTaskSizer(Spawn spawn, HostManager hostManager) {
        queuedJobIds = new LinkedHashMap<>();
        cachedJobRatios = CacheBuilder.newBuilder().expireAfterWrite(ratioExpirationHours, TimeUnit.HOURS).build();
        this.spawn = spawn;
        this.hostManager = hostManager;
    }

    public void startPolling(ScheduledExecutorService executor) {
        if (pollingStarted.compareAndSet(false, true)) {
            executor.scheduleWithFixedDelay(new QueueConsumer(), queueConsumptionInterval, queueConsumptionInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Estimate the true size of a task, using the cached ratio if it is available
     * This method is designed to always return immediately so SpawnBalancer can continue working immediately.
     * If necessary, it will queue a more accurate value to be updated at some point in the future.
     *
     * @param task The task to estimate the size for
     * @return An estimated number of bytes for the true size of the task
     */
    public long estimateTrueSize(JobTask task) {
        if (task == null) {
            return 0L;
        }
        long taskReportedSize = getReportedSize(task);
        Double cachedRatio = cachedJobRatios.getIfPresent(task.getJobUUID());
        if (cachedRatio == null) {
            requestJobSizeFetch(task.getJobUUID(), task.getTaskID());
            return (long) (defaultRatio * taskReportedSize);
        }
        return (long) (cachedRatio * taskReportedSize);
    }

    private long getReportedSize(JobTask task) {
        /* Return task.getByteCount if it is sensible.
        Sometimes tasks erroneously report 0 bytes used. Use the job average in this case. */
        long byteCount = task != null ? task.getByteCount() : 0;
        if (task == null || byteCount > 0) {
            return byteCount;
        }
        Job job = spawn.getJob(task.getJobUUID());
        if (job != null) {
            return job.calcAverageTaskSizeBytes();
        }
        return byteCount;
    }

    public void requestJobSizeFetch(String jobId, int taskId) {
        synchronized (queuedJobIds) {
            if (cachedJobRatios.getIfPresent(jobId) != null || queuedJobIds.containsKey(jobId)) {
                return;
            }
            queuedJobIds.put(jobId, taskId);
        }
    }

    /**
     * Fetch the true size of a task from the minion that currently hosts it
     *
     * @param jobId  The job id of the task
     * @param taskId The task # within the job
     * @return A non-negative number of bytes on success; -1 otherwise
     */
    private long fetchTaskTrueSize(String jobId, int taskId) {
        JobTask task = spawn.getTask(jobId, taskId);
        if (task == null) {
            return -1;
        }
        HostState liveHost = hostManager.getHostState(task.getHostUUID());
        if (liveHost == null) {
            return -1;
        }
        String url = "http://" + liveHost.getHost() + ":" + liveHost.getPort() + "/task.size?id=" + jobId + "&node=" + taskId;
        try {
            byte[] result = HttpUtil.httpGet(url, 0).getBody();
            return Long.parseLong(new String(result));
        } catch (Exception e) {
            log.warn("Failed to fetch task size for " + task.getJobKey());
        }
        return -1;
    }

    /**
     * Take the earliest job to be requested on the queue, fetch the ratio for that job, and update the cache
     */
    private void consumeFromQueue() {
        String jobId;
        int taskId;
        synchronized (queuedJobIds) {
            if (queuedJobIds.isEmpty()) {
                return;
            }
            Map.Entry<String, Integer> entry = queuedJobIds.entrySet().iterator().next();
            queuedJobIds.remove(entry.getKey());
            jobId = entry.getKey();
            taskId = entry.getValue();
        }
        long trueSize = fetchTaskTrueSize(jobId, taskId);
        long reportedSize = getReportedSize(spawn.getTask(jobId, taskId));
        double ratio = getRatio(trueSize, reportedSize);
        log.info("[spawn.balancer.task.sizer] updated ratio for job " + jobId + " reported=" + reportedSize + " true=" + trueSize + " ratio=" + ratio);
        cachedJobRatios.put(jobId, ratio);
    }

    /**
     * Get the ratio for the specified true/reported sizes, subject to some sanity restraints
     *
     * @param trueSize     The true size as reported by minion running du
     * @param reportedSize The result of JobTask.getByteCount
     * @return The "sanity-ized" ratio
     */
    private static double getRatio(long trueSize, long reportedSize) {
        if (trueSize <= 0 || reportedSize <= 0) {
            return defaultRatio;
        }
        double rawRatio = (double) trueSize / reportedSize;
        return Math.max(minRatio, Math.min(rawRatio, maxRatio));
    }

    /**
     * A runnable that will consume from the queue of jobs that SpawnBalancer wants to size up
     */
    private class QueueConsumer implements Runnable {

        @Override
        public void run() {
            try {
                consumeFromQueue();
            } catch (Exception e) {
                log.warn("Failed to consume from TaskSizer queue: " + e, e);
                }
        }
    }
}
