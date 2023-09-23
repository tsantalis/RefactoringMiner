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

import com.addthis.hydra.job.JobTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* A class for storing a task and its live/replica status */
class JobTaskItem {

    private final JobTask task;

    public JobTaskItem(JobTask task) {
        this.task = task;
    }

    public JobTask getTask() {
        return task;
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass() != getClass()) {
            return false;
        }
        JobTaskItem item2 = (JobTaskItem) o;
        return task.getJobKey().matches(item2.getTask().getJobKey());
    }

    @Override
    public String toString() {
        return "JobTaskItem{task=" + task.getJobKey() + "'";
    }
}
