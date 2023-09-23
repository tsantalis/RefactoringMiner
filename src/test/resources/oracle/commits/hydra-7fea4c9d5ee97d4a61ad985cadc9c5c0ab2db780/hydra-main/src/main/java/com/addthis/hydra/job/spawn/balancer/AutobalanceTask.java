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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class performs job/host rebalancing at specified intervals
 */
class AutobalanceTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(AutobalanceTask.class);

    private final SpawnBalancer spawnBalancer;

    private long lastJobAutobalanceTime = 0L;
    private long lastHostAutobalanceTime = 0L;
    private int numHostRebalances = 0;

    public AutobalanceTask(SpawnBalancer spawnBalancer) {
        this.spawnBalancer = spawnBalancer;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        // Don't autobalance if spawn is quiesced, busy, etc.
        if (spawnBalancer.okayToAutobalance()) {
            if ((now - lastHostAutobalanceTime) > spawnBalancer.config.getHostAutobalanceIntervalMillis()) {
                numHostRebalances++;
                log.warn("Performing host autobalance.");
                RebalanceWeight weight =
                        ((numHostRebalances % 2) == 0) ? RebalanceWeight.HEAVY : RebalanceWeight.LIGHT;
                spawnBalancer.spawn.executeReallocationAssignments(
                        spawnBalancer.getAssignmentsForAutoBalance(RebalanceType.HOST, weight),
                        false);
                lastHostAutobalanceTime = now;
            } else if ((now - lastJobAutobalanceTime) > spawnBalancer.config.getJobAutobalanceIntervalMillis()) {
                log.warn("Performing job autobalance.");
                spawnBalancer.spawn.executeReallocationAssignments(
                        spawnBalancer.getAssignmentsForAutoBalance(RebalanceType.JOB, RebalanceWeight.HEAVY), false);
                lastJobAutobalanceTime = now;
            }
        }
    }
}
