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

import java.util.concurrent.TimeUnit;

import com.addthis.basis.util.Parameter;

import com.addthis.codec.annotations.Bytes;
import com.addthis.codec.annotations.FieldConfig;
import com.addthis.codec.annotations.Time;
import com.addthis.codec.codables.Codable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * This class stores various configuration parameters for spawn balancer, such as how many tasks to move, how many bytes to move, etc.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                setterVisibility = JsonAutoDetect.Visibility.NONE)
public class SpawnBalancerConfig implements Codable {

    // How aggressively balancing should be done. For now, 0=no rebalancing, 1=rebalance jobs that are over/under-allocated, 2=rebalance all jobs
    @FieldConfig
    private int autoBalanceLevel = 0;

    // During reallocation, don't move more than this many tasks
    @FieldConfig
    private int tasksMovedFullRebalance = Parameter.intValue("spawnbalance.tasks.fullbalance", 10);
    // During reallocation, don't move more than this many bytes
    @Bytes
    @FieldConfig
    private long bytesMovedFullRebalance = Parameter.longValue("spawnbalance.bytes.fullbalance", 300L * 1000 * 1000 * 1000);
    // During host reallocation, a single moved task can only be this portion of the overall byte maximum
    private double singleTaskBytesFactor = Double.parseDouble(Parameter.value("spawnbalance.task.factor", ".8"));
    // Don't move more than this percentage of bytes off of a loaded disk, and don't fill up more than that much of an unloaded disk
    private double hostDiskFactor = Double.parseDouble(Parameter.value("spawnbalance.host.factor", ".75"));
    // During reallocation, don't move this many tasks to/from a host unless that host was specifically rebalanced
    private int tasksMovedPerUnspecifiedHost = Parameter.intValue("spawnbalance.host.unspecified", 4);
    // Don't bother moving very small tasks during disk space balancing
    private int minTaskSizeBytes = Parameter.intValue("spawnbalance.mintaskbytes", 50 * 1000 * 1000);

    // Consider load balance of two hosts to be significantly different only if the difference is at least 10% above/below
    private double extremeHostRatio = Double.parseDouble(Parameter.value("spawnbalance.extreme.ratio", "1.1"));
    // Consider a host's load to be heavy (/light) only if it is heavier (/lighter) than 50% of hosts
    private double extremeHostPercentile = Double.parseDouble(Parameter.value("spawnbalance.extreme.perc", ".5"));
    // When rebalancing a heavy host, push onto the lightest 20% of suitable hosts. When rebalancing a light host, pull from
    // the heaviest 20% of suitable hosts.
    private double alleviateHostPercentage = Double.parseDouble(Parameter.value("spawnbalance.alleviate.perc", ".2"));

    private int autobalanceCheckInterval = Parameter.intValue("spawnbalance.check.autobalance", 60 * 1000);
    // Only do job autobalancing once per time interval
    @Time(TimeUnit.MILLISECONDS)
    @FieldConfig
    private int jobAutobalanceIntervalMillis = Parameter.intValue("spawnbalance.interval.job.autobalance", 4 * 60 * 60 * 1000);
    // Only do host autobalancing once per time interval
    @Time(TimeUnit.MILLISECONDS)
    @FieldConfig
    private int hostAutobalanceIntervalMillis = Parameter.intValue("spawnbalance.interval.host.autobalance", 6 * 60 * 60 * 1000);
    // Track the last time a job autobalance was done
    private long lastJobAutobalanceTime = 0L;

    // If you're at more than 80% disk capacity, you don't get assigned any new tasks or replicas
    private double minDiskPercentAvailToReceiveNewTasks = Double.parseDouble(Parameter.value("spawnbalance.min.disk.percent.avail.newtasks", ".2"));
    // If you're at more than 90% disk capacity, push tasks off before running additional tasks
    private double minDiskPercentAvailToRunJobs = Double.parseDouble(Parameter.value("spawnbalance.min.disk.percent.avail.runtasks", ".1"));
    // Max number of read-only-replicas for a given host
    private int maxReadonlyReplicas = Parameter.intValue("spawnbalance.max.job.task.replicas.per.host", 5);

    // Tasks that have run within this many milliseconds are considered "active" and weighted more heavily. Default is one day.
    private int activeTaskMilliCutoff = Parameter.intValue("spawnbalance.active.task.cutoff", 24 * 60 * 60 * 1000);
    // control whether minions on the same host can replicate to each other
    private boolean allowSameHostReplica = Parameter.boolValue("spawnbalance.replica.same_host", true);
    // The score weight for task siblings
    private int siblingWeight = Parameter.intValue("spawnbalance.sib.wt", 40);
    // The score weight for recently-run tasks
    private int activeTaskWeight = Parameter.intValue("spawnbalance.active.task.wt", 30);
    // The score weight for disk usage
    private int diskUsedWeight = Parameter.intValue("spawnbalance.disk.used.wt", 70);
    // The default score to give to new hosts
    private double defaultHostScore = Parameter.intValue("spawnbalance.default.host.score", 100);

    public int getAutoBalanceLevel() {
        return autoBalanceLevel;
    }

    public void setAutoBalanceLevel(int autoBalanceLevel) {
        this.autoBalanceLevel = autoBalanceLevel;
    }

    public int getTasksMovedFullRebalance() {
        return tasksMovedFullRebalance;
    }

    public void setTasksMovedFullRebalance(int tasksMovedFullRebalance) {
        this.tasksMovedFullRebalance = tasksMovedFullRebalance;
    }

    public long getBytesMovedFullRebalance() {
        return bytesMovedFullRebalance;
    }

    public void setBytesMovedFullRebalance(long bytesMovedFullRebalance) {
        this.bytesMovedFullRebalance = bytesMovedFullRebalance;
    }

    public double getSingleTaskBytesFactor() {
        return singleTaskBytesFactor;
    }

    public void setSingleTaskBytesFactor(double singleTaskBytesFactor) {
        this.singleTaskBytesFactor = singleTaskBytesFactor;
    }

    public int getTasksMovedPerUnspecifiedHost() {
        return tasksMovedPerUnspecifiedHost;
    }

    public void setTasksMovedPerUnspecifiedHost(int tasksMovedPerUnspecifiedHost) {
        this.tasksMovedPerUnspecifiedHost = tasksMovedPerUnspecifiedHost;
    }

    public int getMinTaskSizeBytes() {
        return minTaskSizeBytes;
    }

    public void setMinTaskSizeBytes(int minTaskSizeBytes) {
        this.minTaskSizeBytes = minTaskSizeBytes;
    }

    public double getExtremeHostRatio() {
        return extremeHostRatio;
    }

    public void setExtremeHostRatio(double extremeHostRatio) {
        this.extremeHostRatio = extremeHostRatio;
    }

    public double getExtremeHostPercentile() {
        return extremeHostPercentile;
    }

    public void setExtremeHostPercentile(double extremeHostPercentile) {
        this.extremeHostPercentile = extremeHostPercentile;
    }

    public double getAlleviateHostPercentage() {
        return alleviateHostPercentage;
    }

    public void setAlleviateHostPercentage(double alleviateHostPercentage) {
        this.alleviateHostPercentage = alleviateHostPercentage;
    }

    public int getAutobalanceCheckInterval() {
        return autobalanceCheckInterval;
    }

    public int getJobAutobalanceIntervalMillis() {
        return jobAutobalanceIntervalMillis;
    }

    public void setJobAutobalanceIntervalMillis(int jobAutobalanceIntervalMillis) {
        this.jobAutobalanceIntervalMillis = jobAutobalanceIntervalMillis;
    }

    public int getHostAutobalanceIntervalMillis() {
        return hostAutobalanceIntervalMillis;
    }

    public void setHostAutobalanceIntervalMillis(int hostAutobalanceIntervalMillis) {
        this.hostAutobalanceIntervalMillis = hostAutobalanceIntervalMillis;
    }

    public long getLastJobAutobalanceTime() {
        return lastJobAutobalanceTime;
    }

    public void setLastJobAutobalanceTime(long lastJobAutobalanceTime) {
        this.lastJobAutobalanceTime = lastJobAutobalanceTime;
    }

    public double getMinDiskPercentAvailToReceiveNewTasks() {
        return minDiskPercentAvailToReceiveNewTasks;
    }

    public void setMinDiskPercentAvailToReceiveNewTasks(double minDiskPercentAvailToReceiveNewTasks) {
        this.minDiskPercentAvailToReceiveNewTasks = minDiskPercentAvailToReceiveNewTasks;
    }

    public double getMinDiskPercentAvailToRunJobs() {
        return minDiskPercentAvailToRunJobs;
    }

    public void setMinDiskPercentAvailToRunJobs(double minDiskPercentAvailToRunJobs) {
        this.minDiskPercentAvailToRunJobs = minDiskPercentAvailToRunJobs;
    }

    public int getMaxReadonlyReplicas() {
        return maxReadonlyReplicas;
    }

    public void setMaxReadonlyReplicas(int maxReadonlyReplicas) {
        this.maxReadonlyReplicas = maxReadonlyReplicas;
    }

    public int getActiveTaskMilliCutoff() {
        return activeTaskMilliCutoff;
    }

    public void setActiveTaskMilliCutoff(int getActiveTaskMilliCutoff) {
        this.activeTaskMilliCutoff = getActiveTaskMilliCutoff;
    }

    public int getSiblingWeight() {
        return siblingWeight;
    }

    public void setSiblingWeight(int siblingWeight) {
        this.siblingWeight = siblingWeight;
    }

    public int getActiveTaskWeight() {
        return activeTaskWeight;
    }

    public void setActiveTaskWeight(int activeTaskWeight) {
        this.activeTaskWeight = activeTaskWeight;
    }

    public int getDiskUsedWeight() {
        return diskUsedWeight;
    }

    public void setDiskUsedWeight(int diskUsedWeight) {
        this.diskUsedWeight = diskUsedWeight;
    }

    public double getDefaultHostScore() {
        return defaultHostScore;
    }

    public void setDefaultHostScore(double defaultHostScore) {
        this.defaultHostScore = defaultHostScore;
    }

    public boolean allowSameHostReplica() {
        return allowSameHostReplica;
    }

    public void setAllowSameHostReplica(boolean allowSameHostReplica) {
        this.allowSameHostReplica = allowSameHostReplica;
    }

    public double getHostDiskFactor() {
        return hostDiskFactor;
    }

    public void setHostDiskFactor(double hostDiskFactor) {
        this.hostDiskFactor = hostDiskFactor;
    }
}
