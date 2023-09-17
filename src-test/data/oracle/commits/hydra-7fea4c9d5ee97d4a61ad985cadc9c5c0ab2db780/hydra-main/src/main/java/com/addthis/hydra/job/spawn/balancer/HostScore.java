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

final class HostScore {

    private final double meanActiveTasks;
    private final double usedDiskPercent;
    private final double overallScore;

    HostScore(double meanActiveTasks, double usedDiskPercent, double overallScore) {
        this.meanActiveTasks = meanActiveTasks;
        this.usedDiskPercent = usedDiskPercent;
        this.overallScore = overallScore;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public double getScoreValue(boolean diskSpace) {
        return diskSpace ? usedDiskPercent : meanActiveTasks;
    }
}
