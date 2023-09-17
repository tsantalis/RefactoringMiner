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

import com.addthis.hydra.job.JobTaskMoveAssignment;
import com.addthis.hydra.job.spawn.Spawn;

class MoveAssignmentList extends ArrayList<JobTaskMoveAssignment> {
    private static final long serialVersionUID = -563719566151798849L;

    private final Spawn spawn;
    private final SpawnBalancerTaskSizer taskSizer;

    private long bytesUsed = 0;

    public MoveAssignmentList(Spawn spawn, SpawnBalancerTaskSizer taskSizer) {
        this.spawn = spawn;
        this.taskSizer = taskSizer;
    }

    @Override
    public boolean add(JobTaskMoveAssignment assignment) {
        bytesUsed += taskSizer.estimateTrueSize(spawn.getTask(assignment.getJobKey()));
        return super.add(assignment);
    }

    public long getBytesUsed() {
        return bytesUsed;
    }
}
