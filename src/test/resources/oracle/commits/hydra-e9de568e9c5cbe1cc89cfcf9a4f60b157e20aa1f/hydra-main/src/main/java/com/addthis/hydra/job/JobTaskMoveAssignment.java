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
package com.addthis.hydra.job;

import com.addthis.hydra.job.mq.JobKey;

/**
 * A simple class for communicating where and how a JobTask should be moved.
 */
public class JobTaskMoveAssignment {

    private final JobKey jobKey;
    private final String sourceUUID;
    private final String targetUUID;
    private final boolean delete;
    private final boolean fromReplica;

    public JobTaskMoveAssignment(JobKey jobKey, String sourceUUID, String targetUUID, boolean fromReplica, boolean delete) {
        this.jobKey = jobKey;
        this.sourceUUID = sourceUUID;
        this.targetUUID = targetUUID;
        this.fromReplica = fromReplica;
        this.delete = delete;
    }

    public JobKey getJobKey() {
        return jobKey;
    }

    public String getSourceUUID() {
        return sourceUUID;
    }

    public String getTargetUUID() {
        return targetUUID;
    }

    public boolean isFromReplica() {
        return fromReplica;
    }

    public boolean delete() {
        return delete;
    }

    @Override
    public String toString() {
        return "JobTaskMoveAssignment{" +
               "jobKey=" + jobKey +
               ", sourceUUID='" + sourceUUID + '\'' +
               ", targetUUID='" + targetUUID + '\'' +
               ", fromReplica=" + fromReplica +
               '}';
    }
}
