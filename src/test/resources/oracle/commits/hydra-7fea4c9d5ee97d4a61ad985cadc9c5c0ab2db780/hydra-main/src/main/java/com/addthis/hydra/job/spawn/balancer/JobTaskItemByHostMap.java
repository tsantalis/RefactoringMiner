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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.addthis.hydra.job.JobTask;
import com.addthis.hydra.job.JobTaskReplica;
import com.addthis.hydra.job.mq.HostState;

/**
 * A class for maintaining a count of live/replica tasks by host
 */
class JobTaskItemByHostMap extends HashMap<String, Set<JobTaskItem>> {

    private final SpawnBalancer spawnBalancer;
    private final HashMap<String, Integer> pushedTaskCounts;
    private final HashMap<String, Integer> pulledTaskCounts;
    private final int maxPulledFromHost;
    private final int maxPushedToHost;

    private List<String> hostsSorted = null;

    public JobTaskItemByHostMap(SpawnBalancer spawnBalancer,
                                Iterable<HostState> hosts,
                                int maxPulledFromHost,
                                int maxPushedToHost) {
        this.spawnBalancer = spawnBalancer;
        this.pulledTaskCounts = new HashMap<>();
        this.pushedTaskCounts = new HashMap<>();
        this.maxPulledFromHost = maxPulledFromHost;
        this.maxPushedToHost = maxPushedToHost;
        for (HostState host : hosts) {
            if (host.isUp() && !host.isDead()) {
                put(host.getHostUuid(), new HashSet<>());
            }
        }
    }

    public void add(String hostID, JobTask task) {
        if (containsKey(hostID)) {
            Set<JobTaskItem> current = get(hostID);
            current.add(new JobTaskItem(task));
        }
    }

    public void addLiveAndReplicasForTask(JobTask task) {
        add(task.getHostUUID(), task);
        List<JobTaskReplica> replicas = task.getReplicas();
        if (replicas != null) {
            for (JobTaskReplica replica : replicas) {
                add(replica.getHostUUID(), task);
            }
        }
    }

    public boolean moveTask(JobTaskItem item, String fromHost, String toHost) {
        if (!containsKey(fromHost)
            || !containsKey(toHost)
            || !get(fromHost).contains(item)
            || get(toHost).contains(item)
            || !hasCapacity(pulledTaskCounts, fromHost, maxPulledFromHost)
            || !hasCapacity(pushedTaskCounts, toHost, maxPushedToHost)) {
            return false;
        } else {
            boolean success = get(fromHost).remove(item) && get(toHost).add(item);
            if (success) {
                claimCapacity(pulledTaskCounts, fromHost);
                claimCapacity(pushedTaskCounts, toHost);
            }
            return success;
        }
    }

    public List<String> generateHostsSorted() {
        List<String> rv = new ArrayList<>(this.keySet());
        Collections.sort(rv, (s, s1) -> {
            int count = get(s).size();
            int count1 = get(s1).size();
            if (count == count1) {
                return Double.compare(spawnBalancer.getHostScoreCached(s), spawnBalancer.getHostScoreCached(s1));
            } else {
                return Double.compare(count, count1);
            }
        });
        hostsSorted = rv;
        return new ArrayList<>(rv);
    }


    public Iterator<String> getHostIterator(boolean smallFirst) {
        if (hostsSorted == null) {
            generateHostsSorted();
        }
        List<String> copy = new ArrayList<>(hostsSorted);
        if (!smallFirst) {
            Collections.reverse(copy);
        }
        return copy.iterator();
    }

    private boolean hasCapacity(HashMap<String, Integer> map, String key, int max) {
        return !map.containsKey(key) || (map.get(key) < max);
    }

    private void claimCapacity(HashMap<String, Integer> map, String key) {
        if (map.containsKey(key)) {
            map.put(key, map.get(key) + 1);
        } else {
            map.put(key, 1);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        for (Entry<String, Set<JobTaskItem>> entry : this.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue().size()).append("; ");
        }
        return sb.append("}").toString();
    }

    public int findLeastTasksOnHost() {
        if (hostsSorted == null) {
            generateHostsSorted();
        }
        return get(hostsSorted.get(0)).size();
    }

    public int findMostTasksOnHost() {
        if (hostsSorted == null) {
            generateHostsSorted();
        }
        return get(hostsSorted.get(hostsSorted.size() - 1)).size();
    }
}
