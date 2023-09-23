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

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.addthis.basis.test.SlowTest;
import com.addthis.basis.util.JitterClock;
import com.addthis.basis.util.LessFiles;

import com.addthis.codec.config.Configs;
import com.addthis.hydra.job.Job;
import com.addthis.hydra.job.JobParameter;
import com.addthis.hydra.job.JobTask;
import com.addthis.hydra.job.JobTaskMoveAssignment;
import com.addthis.hydra.job.JobTaskReplica;
import com.addthis.hydra.job.JobTaskState;
import com.addthis.hydra.job.entity.JobCommand;
import com.addthis.hydra.job.mq.HostCapacity;
import com.addthis.hydra.job.mq.HostState;
import com.addthis.hydra.job.mq.JobKey;
import com.addthis.hydra.job.spawn.HostManager;
import com.addthis.hydra.job.spawn.Spawn;
import com.addthis.hydra.job.spawn.SpawnMQ;
import com.addthis.hydra.minion.Minion;
import com.addthis.hydra.util.ZkCodecStartUtil;

import org.apache.zookeeper.CreateMode;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import static org.junit.Assert.*;

@Category(SlowTest.class)
public class SpawnBalancerTest extends ZkCodecStartUtil {

    private Spawn spawn;
    private HostManager hostManager;
    private SpawnBalancer bal;
    private long now = JitterClock.globalTime();
    private String tmpRoot;

    @Before
    public void setup() throws Exception {

        tmpRoot = LessFiles.createTempDir().toString();
        System.setProperty("SPAWN_DATA_DIR", tmpRoot + "/tmp/spawn/data");
        System.setProperty("SPAWN_LOG_DIR", tmpRoot + "/tmp/spawn/log/events");
        if (zkClient.checkExists().forPath("/minion/up") == null) {
            zkClient.create().creatingParentsIfNeeded().forPath("/minon/up");
        }
        if (zkClient.checkExists().forPath("/minion/dead") == null) {
            zkClient.create().creatingParentsIfNeeded().forPath("/minon/dead");
        }
        try {
            Thread.sleep(100);
            spawn = Configs.newDefault(Spawn.class);
            hostManager = spawn.hostManager;
            bal = spawn.getSpawnBalancer();
            spawn.setSpawnMQ(Mockito.mock(SpawnMQ.class));
        } catch (Exception ex) {
        }
    }

    @After
    public void clear() throws Exception {
        if (zkClient.checkExists().forPath("/minion/up") != null) {
            zkClient.delete().forPath("/minon/up");
        }
        if (zkClient.checkExists().forPath("/minion/dead") != null) {
            zkClient.delete().forPath("/minon/dead");
        }
        LessFiles.deleteDir(new File(tmpRoot));
        spawn.close();
    }
    
    @Test
    public void saveLoadConfig() {
        SpawnBalancerConfig config = new SpawnBalancerConfig();
        config.setBytesMovedFullRebalance(123456);
        bal.saveConfigToDataStore();
        SpawnBalancerConfig loadedConfig = bal.loadConfigFromDataStore(null);
        assertNotNull("not null", loadedConfig);
        assertEquals("correct value saved/loaded", 123456, loadedConfig.getBytesMovedFullRebalance());
    }

    @Test
    public void replicaSuitabilityTest() throws Exception {
        List<HostState> hosts = new ArrayList<>();
        int numHosts = 8;
        for (int i = 0; i < numHosts; i++) {
            HostState nextHost = new HostState("id" + i);
            nextHost.setUsed(new HostCapacity(0, 0, 0, i));
            nextHost.setMax(new HostCapacity(0, 0, 0, 2 * numHosts));
            nextHost.setUp(true);
            hosts.add(nextHost);
        }
        bal.markRecentlyReplicatedTo("id4");
        hosts = bal.sortHostsByDiskSpace(hosts);
        assertEquals("should get lightest host first", "id0", hosts.get(0).getHostUuid());
        assertEquals("should get heaviest host second to last", "id" + (numHosts - 1), hosts.get(numHosts - 2).getHostUuid());
        assertEquals("should get recently-replicated-to host last", "id4", hosts.get(numHosts - 1).getHostUuid());
    }

    @Test
    public void replicaAllocationTest() throws Exception {
        // Suppose we allocate a job with N tasks to a cluster with N hosts, with one of the hosts near full disk.
        // Then we should put replicas on all the light hosts, but not the heavy one
        String fullHostID = "full";
        HostState fullHost = installHostStateWithUUID(fullHostID, spawn, true);
        fullHost.setUsed(new HostCapacity(0, 0, 0, 9998));
        fullHost.setMax(new HostCapacity(0, 0, 0, 10_000));
        int numLightHosts = 9;
        ArrayList<String> hostIDs = new ArrayList<>(numLightHosts + 1);
        hostIDs.add(fullHostID);
        for (int i = 0; i < numLightHosts; i++) {
            String hostID = "light" + i;
            hostIDs.add(hostID);
            HostState lightHost = installHostStateWithUUID(hostID, spawn, true);
            lightHost.setUsed(new HostCapacity(0, 0, 0, 30));
            lightHost.setMax(new HostCapacity(0, 0, 0, 10_000));
        }
        Job job = createJobAndUpdateHosts(spawn, numLightHosts + 1, hostIDs, now, 1000, 0);
        job.setReplicas(1);
        Map<Integer, List<String>> assignments = bal.getAssignmentsForNewReplicas(job);
        HashSet<String> usedHosts = new HashSet<>(numLightHosts);
        for (List<String> targets : assignments.values()) {
            assertEquals("should make one replica per task", 1, targets.size());
            assertTrue("should not put replicas on full host", !targets.contains(fullHostID));
            usedHosts.addAll(targets);
        }
        assertTrue("should use many light hosts", numLightHosts > .75 * usedHosts.size());
    }

    @Test
    public void sortHostsTest() throws Exception {
        // Suppose we have a cluster with the following machines:
        //  - a host that is readOnly
        //  - a host that is down
        //  - a host that is dead
        //  - a live host that has no jobs
        //  - a live host that has one old task and one new
        //  - a live host that has two new tasks
        // sortHostsByLiveTasks should omit the first two and return the last three in the given order.

        String readOnlyHostID = "read_only_host";
        HostState readOnlyHost = installHostStateWithUUID(readOnlyHostID, spawn, true, false, 0, "default");

        String downHostID = "down_host";
        HostState downHost = installHostStateWithUUID(downHostID, spawn, false);

        String deadHostID = "dead_host";
        HostState deadHost = installHostStateWithUUID(deadHostID, spawn, false);
        deadHost.setDead(true);

        String emptyHostID = "empty_host";
        HostState emptyHost = installHostStateWithUUID(emptyHostID, spawn, true);

        String oneOldOneNewHostID = "1old1new";
        HostState oldNewHost = installHostStateWithUUID(oneOldOneNewHostID, spawn, true);

        Job oldJob = createSpawnJob(spawn, 1, Arrays.asList(oneOldOneNewHostID), 0l, 1, 0);
        Job newJob1 = createSpawnJob(spawn, 1, Arrays.asList(oneOldOneNewHostID), now, 1, 0);
        oldNewHost.setStopped(simulateJobKeys(oldJob, newJob1));

        String twoNewHostID = "2new";
        HostState twoNewHost = installHostStateWithUUID(twoNewHostID, spawn, true);
        Job newJob2 = createSpawnJob(spawn, 1, Arrays.asList(twoNewHostID), now, 1, 0);
        Job newJob3 = createSpawnJob(spawn, 1, Arrays.asList(twoNewHostID), now, 1, 0);
        twoNewHost.setStopped(simulateJobKeys(newJob2, newJob3));

        List<HostState> hosts = Arrays.asList(downHost, deadHost, oldNewHost, twoNewHost, emptyHost);
        HostState[] desiredOrder = new HostState[]{emptyHost, oldNewHost, twoNewHost};
        List<HostState> sortedHosts = bal.sortHostsByActiveTasks(hosts);
        assertEquals("shouldn't include read only", false, sortedHosts.contains(readOnlyHost));
        assertEquals("shouldn't include down host", false, sortedHosts.contains(downHost));
        assertEquals("shouldn't include dead host", false, sortedHosts.contains(deadHost));
        assertEquals("three hosts should make it through the sort", 3, sortedHosts.size());
        assertArrayEquals("hosts should be in order [empty, old, new]", desiredOrder, sortedHosts.toArray());
    }

    @Test
    public void allocateTasksAcrossHostsTest() throws Exception {
        // If we assign a job with 3N tasks to 3 hosts, N tasks should go on each host.
        String firstHostUUID = "first";
        String secondHostUUID = "second";
        String thirdHostUUID = "third";
        installHostStateWithUUID(firstHostUUID, spawn, true);
        installHostStateWithUUID(secondHostUUID, spawn, true);
        installHostStateWithUUID(thirdHostUUID, spawn, true);
        spawn.getJobCommandManager().putEntity("foo", new JobCommand(), false);
        int numTasks = 15;
        Job job = createJobAndUpdateHosts(spawn, numTasks, Arrays.asList(firstHostUUID, secondHostUUID, thirdHostUUID), now, 1, 0);
        assertEquals("should divide tasks evenly", numTasks / 3, numTasksOnHost(job, firstHostUUID));
        assertEquals("should divide tasks evenly", numTasks / 3, numTasksOnHost(job, secondHostUUID));
        assertEquals("should divide tasks evenly", numTasks / 3, numTasksOnHost(job, thirdHostUUID));
    }

    @Test
    public void multiHostJobReallocationTaskSelectionTest() throws Exception {
        // Suppose we put a 4-node job on a single heavily loaded host, and there are two light hosts available.
        // After a job reallocation, we want to move one tasks to each light host.
        String heavyHostUUID = "heavy";
        String lightHost1UUID = "light1";
        String lightHost2UUID = "light2";
        HostState heavyHost = installHostStateWithUUID(heavyHostUUID, spawn, true);
        HostState lightHost1 = installHostStateWithUUID(lightHost1UUID, spawn, true);
        HostState lightHost2 = installHostStateWithUUID(lightHost2UUID, spawn, true);
        List<HostState> hosts = Arrays.asList(heavyHost, lightHost1, lightHost2);
        Job smallJob = createJobAndUpdateHosts(spawn, 6, Arrays.asList(heavyHostUUID), now, 1000, 0);
        List<JobTaskMoveAssignment> assignments = bal.getAssignmentsForJobReallocation(smallJob, -1, hosts);
        assertEquals("should move 4 tasks total for small job", 4, assignments.size());
        List<String> assignedHosts = new ArrayList<>();
        for (JobTaskMoveAssignment assignment : assignments) {
            assignedHosts.add(assignment.getTargetUUID());
        }
        Collections.sort(assignedHosts);
        assertArrayEquals("should move two tasks to each host",
                new String[]{lightHost1UUID, lightHost1UUID, lightHost2UUID, lightHost2UUID}, assignedHosts.toArray());
        Job job2 = createJobAndUpdateHosts(spawn, 6, Arrays.asList(heavyHostUUID, lightHost1UUID, lightHost2UUID), now, 1000, 0);
        String brandNewHostUUID = "brandnew";
        HostState brandNewHost = installHostStateWithUUID(brandNewHostUUID, spawn, true);
        List<HostState> newHosts = Arrays.asList(heavyHost, lightHost1, lightHost2, brandNewHost);
        bal.updateAggregateStatistics(newHosts);
        List<JobTaskMoveAssignment> assignments2 = bal.getAssignmentsForJobReallocation(job2, -1, newHosts);
        assertEquals("should move one task", 1, assignments2.size());
        assertEquals("should move a task to the new host", brandNewHostUUID, assignments2.get(0).getTargetUUID());
    }

    @Test
    public void assignMoreTasksToLighterHostsTest() throws Exception {
        // Suppose we have a cluster with three hosts, one with a mild load.
        // If we create a 5-node job, we want to assign two tasks to each light host and one to the heavier host
        String heavyHostUUID = "heavy";
        String lightHost1UUID = "light1";
        String lightHost2UUID = "light2";
        HostState heavyHost = installHostStateWithUUID(heavyHostUUID, spawn, true);
        HostState lightHost1 = installHostStateWithUUID(lightHost1UUID, spawn, true);
        HostState lightHost2 = installHostStateWithUUID(lightHost2UUID, spawn, true);
        Job weightJob = createJobAndUpdateHosts(spawn, 10, Arrays.asList(heavyHostUUID), now, 1000, 0);
        bal.updateAggregateStatistics(hostManager.listHostStatus(null));
        Job otherJob = createJobAndUpdateHosts(spawn, 5, Arrays.asList(heavyHostUUID, lightHost1UUID, lightHost2UUID), now, 500, 0);
        assertEquals("should put two tasks on lightHost1", 2, numTasksOnHost(otherJob, lightHost1UUID));
        assertEquals("should put two tasks on lightHost2", 2, numTasksOnHost(otherJob, lightHost2UUID));
        assertEquals("should put one task on heavyHost", 1, numTasksOnHost(otherJob, heavyHostUUID));
    }

    @Test
    public void multiJobHostReallocationTaskSelectionTest() throws Exception {
        // Suppose we have two 3-node jobs all running on a single heavy host.
        // After a host reallocation, we want to move one task from each job to a light host.
        String heavyHostUUID = "heavy";
        String lightHostUUID = "light";
        HostState heavyHost = installHostStateWithUUID(heavyHostUUID, spawn, true);
        HostState lightHost = installHostStateWithUUID(lightHostUUID, spawn, true);
        spawn.getJobCommandManager().putEntity("foo", new JobCommand(), false);
        int numTasks = 3;
        Job job1 = createJobAndUpdateHosts(spawn, numTasks, Arrays.asList(heavyHostUUID), now, 1, 0);
        Job job2 = createJobAndUpdateHosts(spawn, numTasks, Arrays.asList(heavyHostUUID), now, 1, 0);
        bal.updateAggregateStatistics(hostManager.listHostStatus(null));
        List<HostState> hosts = Arrays.asList(heavyHost, lightHost);
        List<JobTaskMoveAssignment> heavyHostTaskAssignments = bal.getAssignmentsToBalanceHost(heavyHost, hosts);
        assertEquals("should move 2 tasks", 2, heavyHostTaskAssignments.size());
        for (JobTaskMoveAssignment assignment : heavyHostTaskAssignments) {
            assertEquals("should move task from heavy host", heavyHostUUID, assignment.getSourceUUID());
            assertEquals("should move task to light host", lightHostUUID, assignment.getTargetUUID());
        }
        bal.clearRecentlyRebalancedHosts();
        List<JobTaskMoveAssignment> lightHostTaskAssignments = bal.getAssignmentsToBalanceHost(lightHost, hosts);
        assertEquals("should move 2 tasks", 2, lightHostTaskAssignments.size());
        for (JobTaskMoveAssignment assignment : lightHostTaskAssignments) {
            assertEquals("should move task from heavy host", heavyHostUUID, assignment.getSourceUUID());
            assertEquals("should move task to light host", lightHostUUID, assignment.getTargetUUID());
        }
    }

    @Test
    public void diskSpaceBalancingTest() throws Exception {
        // Suppose we have one host with full disk and another with mostly empty disk.
        // Should move some tasks from heavy to light, but not too much.
        String heavyHostUUID = "heavy";
        String lightHostUUID = "light";
        String lightHost2UUID = "light2";
        String readOnlyHostUUID = "readOnlyHost";
        HostState heavyHost = installHostStateWithUUID(heavyHostUUID, spawn, true);
        HostState lightHost = installHostStateWithUUID(lightHostUUID, spawn, true);
        HostState lightHost2 = installHostStateWithUUID(lightHost2UUID, spawn, true);
        HostState readOnlyHost = installHostStateWithUUID(readOnlyHostUUID, spawn, true, true, 0, "default");
        List<HostState> hosts = Arrays.asList(heavyHost, lightHost, lightHost2, readOnlyHost);
        spawn.getJobCommandManager().putEntity("foo", new JobCommand(), false);
        Job gargantuanJob = createSpawnJob(spawn, 1, Arrays.asList(heavyHostUUID), now, 8_000_000_000l, 0);
        Job movableJob1 = createSpawnJob(spawn, 1, Arrays.asList(heavyHostUUID), now, 850_000_000l, 0);
        Job movableJob2 = createSpawnJob(spawn, 1, Arrays.asList(heavyHostUUID), now, 820_000_000l, 0);
        heavyHost.setStopped(simulateJobKeys(gargantuanJob, movableJob1, movableJob2));
        heavyHost.setMax(new HostCapacity(10, 10, 10, 10_000_000_000l));
        heavyHost.setUsed(new HostCapacity(10, 10, 10, 9_900_000_000l));
        lightHost.setMax(new HostCapacity(10, 10, 10, 10_000_000_000l));
        lightHost.setUsed(new HostCapacity(10, 10, 10, 20_000_0000l));
        lightHost2.setMax(new HostCapacity(10, 10, 10, 10_000_000_000l));
        lightHost2.setUsed(new HostCapacity(10, 10, 10, 20_000_000l));
        readOnlyHost.setMax(new HostCapacity(10, 10, 10, 10_000_000_000l));
        readOnlyHost.setUsed(new HostCapacity(10, 10, 10, 20_000_000l));
        hostManager.updateHostState(heavyHost);
        hostManager.updateHostState(lightHost);
        hostManager.updateHostState(lightHost2);
        hostManager.updateHostState(readOnlyHost);
        bal.updateAggregateStatistics(hostManager.listHostStatus(null));
        List<JobTaskMoveAssignment> assignments = bal.getAssignmentsToBalanceHost(heavyHost, hosts);
        long bytesMoved = 0;
        for (JobTaskMoveAssignment assignment : assignments) {
            assertTrue("shouldn't move gargantuan task", !assignment.getJobKey().getJobUuid().equals(gargantuanJob.getId()));
            assertTrue("shouldn't move to read-only host", !(assignment.getTargetUUID().equals(readOnlyHostUUID)));
        }
        assertTrue("should move something", !assignments.isEmpty());
    }

    @Test
    public void dontDoPointlessMovesTest() throws Exception {
        // Suppose we have a cluster that is essentially balanced. Rebalancing it shouldn't do anything.
        int numHosts = 3;
        List<HostState> hosts = new ArrayList<>(numHosts);
        List<String> hostNames = new ArrayList<>(numHosts);
        for (int i = 0; i < numHosts; i++) {
            String hostName = "host" + i;
            hostNames.add(hostName);
            hosts.add(installHostStateWithUUID(hostName, spawn, true));
        }
        Job job1 = createJobAndUpdateHosts(spawn, numHosts, hostNames, now, 1000, 0);
        Job job2 = createJobAndUpdateHosts(spawn, numHosts - 1, hostNames, now, 1000, 0);
        for (HostState host : hosts) {
            assertEquals("shouldn't move anything for " + host.getHostUuid(), 0,
                    bal.getAssignmentsToBalanceHost(host, hosts).size());
        }
        assertEquals("shouldn't move anything for " + job1.getId(), 0,
                bal.getAssignmentsForJobReallocation(job1, -1, hosts).size());
        assertEquals("shouldn't move anything for " + job2.getId(), 0,
                bal.getAssignmentsForJobReallocation(job2, -1, hosts).size());

    }

    @Test
    public void kickOnSuitableHosts() throws Exception {
        String availHostID = "host2";
        HostState avail = installHostStateWithUUID(availHostID, spawn, true, false, 1, "default");
        assertTrue("available host should be able to run task", avail.canMirrorTasks());
    }

    @Ignore("Alwawys fail")
    @Test
    public void queuePersist() throws Exception {
        spawn.getJobCommandManager().putEntity("foo", new JobCommand(), false);
        spawn.getSystemManager().quiesceCluster(true, "unknown");
        installHostStateWithUUID("host", spawn, true);
        Job job = createJobAndUpdateHosts(spawn, 4, Arrays.asList("host"), now, 1000, 0);
        JobKey myKey = new JobKey(job.getId(), 0);
        spawn.addToTaskQueue(myKey, 0, false);
        spawn.writeSpawnQueue();
        // FIXME spawn2 can't be instantiated due to 5050 already being used by spawn
        try (Spawn spawn2 = Configs.newDefault(Spawn.class)) {
            spawn2.getSystemManager().quiesceCluster(true, "unknown");
            spawn2.loadSpawnQueue();
            assertEquals("should have one queued task", 1, spawn.getTaskQueuedCount());
        }
    }

    @Test
    public void multipleMinionsPerHostReplicaTest() throws Exception {
        bal.getConfig().setAllowSameHostReplica(true);
        HostState host1m1 = installHostStateWithUUID("m1", spawn, true);
        host1m1.setHost("h1");
        hostManager.updateHostState(host1m1);
        HostState host1m2 = installHostStateWithUUID("m2", spawn, true);
        host1m2.setHost("h1");
        hostManager.updateHostState(host1m2);
        Job job = createJobAndUpdateHosts(spawn, 1, Arrays.asList("m1", "m2"), now, 1000, 0);
        job.setReplicas(1);
        spawn.updateJob(job);
        assertEquals("should get one replica when we allow same host replicas", 1, spawn.getTask(job.getId(), 0).getAllReplicas().size(), 1);
        bal.getConfig().setAllowSameHostReplica(false);
        spawn.updateJob(job);
        assertEquals("should get no replicas when we disallow same host replicas", 0, spawn.getTask(job.getId(), 0).getAllReplicas().size());
    }

    @Test
    public void rebalanceOntoNewHostsTest() throws Exception {
        // Suppose we start out with eight hosts, and have a job with 10 live tasks.
        // Then if we add two hosts and rebalance the job, we should move tasks onto each.
        spawn.setSpawnMQ(Mockito.mock(SpawnMQ.class));
        bal.getConfig().setAllowSameHostReplica(true);
        ArrayList<String> hosts = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            installHostStateWithUUID("h" + i, spawn, true);
            hosts.add("h" + i);
        }
        Job myJob = createJobAndUpdateHosts(spawn, 20, hosts, JitterClock.globalTime(), 2000l, 0);
        installHostStateWithUUID("hNEW1", spawn, true);
        installHostStateWithUUID("hNEW2", spawn, true);
        int tries = 50;
        while (spawn.listAvailableHostIds().size() < 10 && tries-- > 0) {
            // Takes a little while for the new hosts to show up as available
            Thread.sleep(100);
        }
        List<HostState> hostStates = hostManager.listHostStatus(null);
        bal.updateAggregateStatistics(hostStates);
        List<JobTaskMoveAssignment> assignments = bal.getAssignmentsForJobReallocation(myJob, -1, hostStates);
        int h1count = 0;
        int h2count = 0;
        for (JobTaskMoveAssignment assignment : assignments) {
            if (assignment.getTargetUUID().equals("hNEW1")) {
                h1count++;
            } else if (assignment.getTargetUUID().equals("hNEW2")) {
                h2count++;
            } else {
                throw new RuntimeException("should not push anything onto host " + assignment.getTargetUUID());
            }
        }
        assertTrue("should move tasks onto first new host", h1count > 1);
        assertTrue("should move tasks onto second new host", h2count > 1);

    }

    @Test
    public void hostScoreTest() throws Exception {
        // Test that heavily-loaded and lightly-loaded hosts are identified as such, and that medium-loaded hosts are
        // not identified as heavy or light
        long[] used = new long[]{1000, 9900, 10000, 10500, 20000};
        int i = 0;
        for (long usedVal : used) {
            HostState hostState = installHostStateWithUUID("host" + (i++), spawn, true);
            hostState.setUsed(new HostCapacity(0, 0, 0, usedVal));
            hostState.setMax(new HostCapacity(0, 0, 0, 25000));
        }
        bal.updateAggregateStatistics(hostManager.listHostStatus(null));
        assertTrue("should correctly identify light host", bal.isExtremeHost("host0", true, false));
        assertTrue("should not identify light host as heavy", !bal.isExtremeHost("host0", true, true));
        assertTrue("should not identify medium host as light", !bal.isExtremeHost("host1", true, false));
        assertTrue("should not identify medium host as heavy", !bal.isExtremeHost("host1", true, true));
        assertTrue("should correctly identify heavy host", bal.isExtremeHost("host4", true, true));
        assertTrue("should not identify heavy host as light", !bal.isExtremeHost("host4", true, false));
    }

    @Test
    public void jobStateChangeTest() throws Exception
    {
        // Simulate some task state changes, and make sure job.isFinished() behaves as expected.
        List<String> hosts = Arrays.asList("h1", "h2", "h3");
        for (String host : hosts)
        {
            installHostStateWithUUID(host, spawn, true);
        }
        spawn.getJobCommandManager().putEntity("a", new JobCommand(), true);
        Job job = spawn.createJob("fsm", 3, hosts, "default", "a", false);
        JobTask task0 = job.getTask(0);
        JobTask task1 = job.getTask(1);
        job.setTaskState(task0, JobTaskState.BUSY);
        // If a task is busy, the job should not be finished
        assertTrue("job should not be finished", !job.isFinished());
        job.setTaskState(task0, JobTaskState.IDLE);
        assertTrue("job should be finished", job.isFinished());
        job.setTaskState(task1, JobTaskState.MIGRATING, true);
        // If a task is migrating, the job should not be finished.
        assertTrue("job should not be finished", !job.isFinished());
        job.setTaskState(task1, JobTaskState.IDLE, true);
        job.setTaskState(task0, JobTaskState.REBALANCE);
        // If a task is rebalancing, the job _should_ be finished.
        // The idea is that the task successfully ran, got into idle state, then a rebalance action was called afterwards.
        assertTrue("job should be finished", job.isFinished());
    }

    @Test
    public void jobDependencyTest() throws Exception {
        installHostStateWithUUID("a", spawn, true);
        installHostStateWithUUID("b", spawn, true);
        Job sourceJob = createSpawnJob(spawn, 1, Arrays.asList("a", "b"), 1l, 1l, 0);
        Job downstreamJob = createSpawnJob(spawn, 1, Arrays.asList("a", "b"), 1l, 1l, 0);
        downstreamJob.setParameters(Arrays.asList(new JobParameter("param", sourceJob.getId(), "DEFAULT")));
        spawn.updateJob(downstreamJob);
        assertEquals("dependency graph should have two nodes", 2, spawn.getJobDependencies().getNodes().size());
    }

    private Job createSpawnJob(Spawn spawn, int numTasks, List<String> hosts, long startTime, long taskSizeBytes, int numReplicas) throws Exception {

        Job job = spawn.createJob("fsm", numTasks, hosts, Minion.defaultMinionType, "foo", false);
        job.setReplicas(numReplicas);
        for (JobTask task : job.getCopyOfTasks()) {
            task.setByteCount(taskSizeBytes);
        }
        job.setStartTime(startTime);
        return job;
    }

    private Job createJobAndUpdateHosts(Spawn spawn, int numTasks, List<String> hosts, long startTime, long taskSizeBytes, int numReplicas) throws Exception {
        Job job = createSpawnJob(spawn, numTasks, hosts, startTime, taskSizeBytes, numReplicas);
        spawn.updateJob(job);
        for (JobTask task : job.getCopyOfTasks()) {
            task.setFileCount(1L);
            HostState host = hostManager.getHostState(task.getHostUUID());
            host.setStopped(updateJobKeyArray(host.getStopped(), task.getJobKey()));
            host.setMeanActiveTasks(1);
            hostManager.updateHostState(host);
            if (task.getReplicas() != null) {
                for (JobTaskReplica replica : task.getReplicas()) {
                    HostState rHost = hostManager.getHostState(replica.getHostUUID());
                    rHost.setStopped(updateJobKeyArray(rHost.getStopped(), task.getJobKey()));
                    hostManager.updateHostState(host);
                }
            }
        }
        return job;
    }

    private JobKey[] updateJobKeyArray(JobKey[] keys, JobKey newKey) {
        List<JobKey> keyList = keys == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(keys));
        keyList.add(newKey);
        return keyList.toArray(new JobKey[]{});
    }

    private int numTasksOnHost(Job job, String hostID) {
        int count = 0;
        for (JobTask task : job.getCopyOfTasks()) {
            if (task.getHostUUID().equals(hostID)) count++;
        }
        return count;
    }

    private JobKey[] simulateJobKeys(Job... jobs) {
        ArrayList<JobKey> keyList = new ArrayList<>();
        JobKey[] sampleArray = {new JobKey("", 0)};
        for (Job job : jobs) {
            for (int i = 0; i < job.getCopyOfTasks().size(); i++) {
                keyList.add(new JobKey(job.getId(), i));
            }
        }
        return keyList.toArray(sampleArray);
    }

    private HostState installHostStateWithUUID(String hostUUID, Spawn spawn, boolean isUp) throws Exception {
        return installHostStateWithUUID(hostUUID, spawn, isUp, false, 1, "default");
    }

    private HostState installHostStateWithUUID(String hostUUID, Spawn spawn, boolean isUp, boolean readOnly, int availableSlots, String minionType) throws Exception {
        String zkPath = isUp ? "/minion/up/" + hostUUID : "/minion/dead/" + hostUUID;
        zkClient.create().withMode(CreateMode.EPHEMERAL).forPath(zkPath);
        HostState newHostState = new HostState(hostUUID);
        newHostState.setMax(new HostCapacity(10, 10, 10, 1000));
        newHostState.setUsed(new HostCapacity(0, 0, 0, 100));
        newHostState.setHost("hostname-for:" + hostUUID);
        newHostState.setUp(isUp);
        newHostState.setAvailableTaskSlots(availableSlots);
        newHostState.setMinionTypes(minionType);
        hostManager.updateHostState(newHostState);
        return newHostState;
    }
}
