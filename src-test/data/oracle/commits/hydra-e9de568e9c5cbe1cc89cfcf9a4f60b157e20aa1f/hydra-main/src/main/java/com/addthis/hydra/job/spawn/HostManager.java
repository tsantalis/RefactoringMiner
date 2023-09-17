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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.addthis.hydra.job.mq.HostState;

import org.apache.curator.framework.CuratorFramework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.addthis.hydra.job.store.SpawnDataStoreKeys.MINION_DEAD_PATH;
import static com.addthis.hydra.job.store.SpawnDataStoreKeys.MINION_UP_PATH;

public class HostManager {
    private static final Logger log = LoggerFactory.getLogger(HostManager.class);

    @Nonnull final ConcurrentMap<String, HostState> monitored;
    @Nonnull final SetMembershipListener minionMembers;
    @Nonnull final SetMembershipListener deadMinionMembers;

    public HostManager(CuratorFramework zkClient) {
        this.monitored = new ConcurrentHashMap<>();
        this.minionMembers = new SetMembershipListener(zkClient, MINION_UP_PATH);
        this.deadMinionMembers = new SetMembershipListener(zkClient, MINION_DEAD_PATH);
    }

    public HostState getHostState(String hostUuid) {
        if (hostUuid == null) {
            return null;
        }
        synchronized (monitored) {
            return monitored.get(hostUuid);
        }
    }

    public void updateHostState(HostState state) {
        synchronized (monitored) {
            if (!deadMinionMembers.getMemberSet().contains(state.getHostUuid())) {
                log.debug("Updating host state for : {}", state.getHost());
                monitored.put(state.getHostUuid(), state);
            }
        }
    }

    /**
     * List all hosts belonging to a particular minion type.
     *
     * @param minionType The minion type to find. If null, return all hosts.
     * @return A list of hoststates
     */
    public List<HostState> listHostStatus(@Nullable String minionType) {
        synchronized (monitored) {
            Set<String> availableMinions = minionMembers.getMemberSet();
            Set<String> deadMinions = deadMinionMembers.getMemberSet();
            List<HostState> allMinions = new ArrayList<>();
            for (HostState minion : monitored.values()) {
                if (availableMinions.contains(minion.getHostUuid()) && !deadMinions.contains(minion.getHostUuid())) {
                    minion.setUp(true);
                } else {
                    minion.setUp(false);
                }
                if ((minionType == null) || minion.hasType(minionType)) {
                    allMinions.add(minion);
                }
            }
            return allMinions;
        }
    }

    public List<HostState> getLiveHosts(String minionType) {
        List<HostState> allHosts = listHostStatus(minionType);
        List<HostState> rv = new ArrayList<>(allHosts.size());
        for (HostState host : allHosts) {
            if (host.isUp() && !host.isDead()) {
                rv.add(host);
            }
        }
        return rv;
    }
}