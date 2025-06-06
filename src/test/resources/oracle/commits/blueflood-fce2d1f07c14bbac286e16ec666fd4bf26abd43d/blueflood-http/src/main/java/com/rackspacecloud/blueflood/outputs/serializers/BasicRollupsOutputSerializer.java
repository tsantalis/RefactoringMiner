/*
 * Copyright 2015 Rackspace
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.rackspacecloud.blueflood.outputs.serializers;

import com.rackspacecloud.blueflood.exceptions.SerializationException;
import com.rackspacecloud.blueflood.outputs.formats.MetricData;
import com.rackspacecloud.blueflood.types.BasicRollup;
import com.rackspacecloud.blueflood.types.CounterRollup;
import com.rackspacecloud.blueflood.types.GaugeRollup;
import com.rackspacecloud.blueflood.types.Rollup;
import com.rackspacecloud.blueflood.types.SetRollup;
import com.rackspacecloud.blueflood.types.TimerRollup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BasicRollupsOutputSerializer<T> {
    public T transformRollupData(MetricData metricData, Set<MetricStat> filterStats) throws SerializationException;

    public static enum MetricStat {
        AVERAGE("average") {
            @Override
            Object convertRollupToObject(Rollup rollup) throws Exception {
                if (rollup instanceof BasicRollup)
                    return ((BasicRollup) rollup).getAverage();
                else if (rollup instanceof TimerRollup)
                    return ((TimerRollup) rollup).getAverage();
                else
                    // counters, sets
                    throw new Exception(String.format("average not supported for this type: %s", rollup.getClass().getSimpleName()));
            }

            @Override
            Object convertRawSampleToObject(Object rawSample) {
                return rawSample;
            }
        },
        VARIANCE("variance") {
            @Override
            Object convertRollupToObject(Rollup rollup) throws Exception {
                if (rollup instanceof BasicRollup)
                    return ((BasicRollup) rollup).getVariance();
                else if (rollup instanceof TimerRollup)
                    return ((TimerRollup) rollup).getVariance();
                else
                    // counters, sets.
                    throw new Exception(String.format("variance not supported for this type: %s", rollup.getClass().getSimpleName()));
            }

            @Override
            Object convertRawSampleToObject(Object rawSample) {
                return 0;
            }
        },
        MIN("min") {
            @Override
            Object convertRollupToObject(Rollup rollup) throws Exception {
                if (rollup instanceof BasicRollup)
                    return ((BasicRollup) rollup).getMinValue();
                else if (rollup instanceof TimerRollup)
                    return ((TimerRollup) rollup).getMinValue();
                else
                    // counters, sets.
                    throw new Exception(String.format("min not supported for this type: %s", rollup.getClass().getSimpleName()));
            }

            @Override
            Object convertRawSampleToObject(Object rawSample) {
                return rawSample;
            }
        },
        MAX("max") {
            @Override
            Object convertRollupToObject(Rollup rollup) throws Exception {
                if (rollup instanceof BasicRollup)
                    return ((BasicRollup) rollup).getMaxValue();
                else if (rollup instanceof TimerRollup)
                    return ((TimerRollup) rollup).getMaxValue();
                else
                    // counters, sets.
                    throw new Exception(String.format("min not supported for this type: %s", rollup.getClass().getSimpleName()));
            }

            @Override
            Object convertRawSampleToObject(Object rawSample) {
                return rawSample;
            }
        },
        NUM_POINTS("numPoints") {
            @Override
            Object convertRollupToObject(Rollup rollup) throws Exception {
                if (rollup instanceof BasicRollup)
                    return ((BasicRollup) rollup).getCount();
                else if (rollup instanceof TimerRollup)
                    return ((TimerRollup) rollup).getCount();
                else if (rollup instanceof CounterRollup)
                    return ((CounterRollup) rollup).getCount();
                else if (rollup instanceof SetRollup)
                    return ((SetRollup) rollup).getCount();
                else
                    // gauge.
                    throw new Exception(String.format("numPoints not supported for this type: %s", rollup.getClass().getSimpleName()));
            }

            @Override
            Object convertRawSampleToObject(Object rawSample) {
                return 1;
            }
        },
        LATEST("latest") {
            @Override
            Object convertRollupToObject(Rollup rollup) throws Exception {
                if (rollup instanceof GaugeRollup)
                    return ((GaugeRollup) rollup).getLatestValue().getValue();
                else
                    // every other type.
                    throw new Exception(String.format("latest value not supported for this type: %s", rollup.getClass().getSimpleName()));
            }

            @Override
            Object convertRawSampleToObject(Object rawSample) {
                return rawSample;
            }
        },
        RATE("rate") {
            @Override
            Object convertRollupToObject(Rollup rollup) throws Exception {
                if (rollup instanceof TimerRollup)
                    return ((TimerRollup) rollup).getRate();
                else if (rollup instanceof CounterRollup)
                    return ((CounterRollup) rollup).getRate();
                else
                    // gauge, set, basic
                    throw new Exception(String.format("rate not supported for this type: %s", rollup.getClass().getSimpleName()));
            }

            @Override
            Object convertRawSampleToObject(Object rawSample) {
                return rawSample;
            }
        },
        SUM("sum") {
            @Override
            Object convertRollupToObject(Rollup rollup) throws Exception {
                if (rollup instanceof TimerRollup)
                    return ((TimerRollup) rollup).getSum();
                else if (rollup instanceof CounterRollup)
                    return ((CounterRollup) rollup).getCount();
                else
                    // every other type.
                    throw new Exception(String.format("sum not supported for this type: %s", rollup.getClass().getSimpleName()));
            }

            @Override
            Object convertRawSampleToObject(Object rawSample) {
                return rawSample;
            }
        },
        PERCENTILE("percentiles") {
            @Override
            Object convertRollupToObject(Rollup rollup) throws Exception {
                if (rollup instanceof TimerRollup)
                    return ((TimerRollup) rollup).getPercentiles();
                else
                    // every other type.
                    throw new Exception(String.format("percentiles supported for this type: %s", rollup.getClass().getSimpleName()));
            }

            @Override
            Object convertRawSampleToObject(Object rawSample) {
                return rawSample;
            }
        }
        ;
        
        private MetricStat(String s) {
            this.stringRep = s;
        }
        private String stringRep;
        private static final Map<String, MetricStat> stringToEnum = new HashMap<String, MetricStat>();
        static {
            for (MetricStat ms : values()) {
                stringToEnum.put(ms.toString().toLowerCase(), ms);
            }
        }
        public static MetricStat fromString(String s) {
            return stringToEnum.get(s.toLowerCase());
        }
        public static Set<MetricStat> fromStringList(List<String> statList) {
            Set<MetricStat> set = new HashSet<MetricStat>();
            for (String stat : statList ) {
                MetricStat metricStat = fromString(stat);
                if (metricStat != null) {
                    set.add(fromString(stat));
                }
            }
            return set;
        }
        @Override
        public String toString() {
            return this.stringRep;
        }
        abstract Object convertRollupToObject(Rollup rollup) throws Exception;
        abstract Object convertRawSampleToObject(Object rawSample);
    }
}

