/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.shuffle;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.runtime.io.network.buffer.NetworkBufferPool;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.runtime.io.network.partition.consumer.GateBuffersSpec;
import org.apache.flink.runtime.jobgraph.IntermediateDataSetID;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Optional;

import static org.apache.flink.runtime.io.network.partition.consumer.InputGateSpecUtils.createGateBuffersSpec;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * Utils to calculate network memory requirement of a vertex from network configuration and details
 * of input and output. The methods help to decide the volume of buffer pools when initializing
 * shuffle environment and also guide network memory announcing in fine-grained resource management.
 */
public class NettyShuffleUtils {

    // Temporarily declare the default value here, it would be moved to the configuration class
    // later.
    private static final int DEFAULT_HYBRID_SHUFFLE_MIN_BUFFERS = 8;

    /**
     * Calculates and returns the number of required exclusive network buffers per input channel.
     */
    public static int getNetworkBuffersPerInputChannel(
            final int configuredNetworkBuffersPerChannel) {
        return configuredNetworkBuffersPerChannel;
    }

    /**
     * Calculates and returns the floating network buffer pool size used by the input gate. The
     * left/right value of the returned pair represent the min/max buffers require by the pool.
     */
    public static Pair<Integer, Integer> getMinMaxFloatingBuffersPerInputGate(
            final int numFloatingBuffersPerGate) {
        // We should guarantee at-least one floating buffer for local channel state recovery.
        return Pair.of(1, numFloatingBuffersPerGate);
    }

    /**
     * Calculates and returns local network buffer pool size used by the result partition. The value
     * in the returned tuple respectively represent the expected, min and max buffers of the pool.
     */
    public static Tuple3<Integer, Integer, Integer> getMinMaxNetworkBuffersPerResultPartition(
            final int configuredNetworkBuffersPerChannel,
            final int numFloatingBuffersPerGate,
            final int sortShuffleMinParallelism,
            final int sortShuffleMinBuffers,
            final int numSubpartitions,
            final boolean enableTieredStorage,
            final int tieredStoreExclusiveBuffers,
            final ResultPartitionType type) {
        boolean isSortShuffle =
                type.isBlockingOrBlockingPersistentResultPartition()
                        && numSubpartitions >= sortShuffleMinParallelism;
        int expected;
        if (isSortShuffle) {
            expected = sortShuffleMinBuffers;
        } else {
            expected =
                    enableTieredStorage
                            ? Math.min(tieredStoreExclusiveBuffers, numSubpartitions + 1)
                            : (numSubpartitions + 1);
        }

        int min = expected;
        if (type.isHybridResultPartition()) {
            min = Math.min(DEFAULT_HYBRID_SHUFFLE_MIN_BUFFERS, expected);
        }

        int max =
                type.isBounded()
                        ? numSubpartitions * configuredNetworkBuffersPerChannel
                                + numFloatingBuffersPerGate
                        : (isSortShuffle
                                ? 4 * numSubpartitions
                                : NetworkBufferPool.UNBOUNDED_POOL_SIZE);
        max = Math.max(max, expected);
        // for each upstream hash-based blocking/pipelined subpartition, at least one buffer is
        // needed even the configured network buffers per channel is 0 and this behavior is for
        // performance. If it's not guaranteed that each subpartition can get at least one buffer,
        // more partial buffers with little data will be outputted to network/disk and recycled to
        // be used by other subpartitions which can not get a buffer for data caching.
        return Tuple3.of(expected, min, max);
    }

    public static int computeNetworkBuffersForAnnouncing(
            final int numBuffersPerChannel,
            final int numFloatingBuffersPerGate,
            final Optional<Integer> maxRequiredBuffersPerGate,
            final int sortShuffleMinParallelism,
            final int sortShuffleMinBuffers,
            final Map<IntermediateDataSetID, Integer> inputChannelNums,
            final Map<IntermediateDataSetID, Integer> partitionReuseCount,
            final Map<IntermediateDataSetID, Integer> subpartitionNums,
            final Map<IntermediateDataSetID, ResultPartitionType> inputPartitionTypes,
            final Map<IntermediateDataSetID, ResultPartitionType> partitionTypes) {

        int requirementForInputs = 0;
        for (IntermediateDataSetID dataSetId : inputChannelNums.keySet()) {
            int numChannels = inputChannelNums.get(dataSetId);
            ResultPartitionType inputPartitionType = inputPartitionTypes.get(dataSetId);
            checkNotNull(inputPartitionType);

            int numSingleGateBuffers =
                    getNumBuffersToAnnounceForInputGate(
                            inputPartitionType,
                            numBuffersPerChannel,
                            numFloatingBuffersPerGate,
                            maxRequiredBuffersPerGate,
                            numChannels);
            checkState(partitionReuseCount.containsKey(dataSetId));
            requirementForInputs += numSingleGateBuffers * partitionReuseCount.get(dataSetId);
        }

        int requirementForOutputs = 0;
        for (IntermediateDataSetID dataSetId : subpartitionNums.keySet()) {
            int numSubs = subpartitionNums.get(dataSetId);
            ResultPartitionType partitionType = partitionTypes.get(dataSetId);
            checkNotNull(partitionType);

            requirementForOutputs +=
                    getNumBuffersToAnnounceForResultPartition(
                            partitionType,
                            numBuffersPerChannel,
                            numFloatingBuffersPerGate,
                            sortShuffleMinParallelism,
                            sortShuffleMinBuffers,
                            numSubs);
        }

        return requirementForInputs + requirementForOutputs;
    }

    private static int getNumBuffersToAnnounceForInputGate(
            ResultPartitionType type,
            int configuredNetworkBuffersPerChannel,
            int floatingNetworkBuffersPerGate,
            Optional<Integer> maxRequiredBuffersPerGate,
            int numInputChannels) {
        GateBuffersSpec gateBuffersSpec =
                createGateBuffersSpec(
                        maxRequiredBuffersPerGate,
                        configuredNetworkBuffersPerChannel,
                        floatingNetworkBuffersPerGate,
                        type,
                        numInputChannels,
                        null);
        return gateBuffersSpec.getMaxBuffersPerGate();
    }

    private static int getNumBuffersToAnnounceForResultPartition(
            ResultPartitionType type,
            int configuredNetworkBuffersPerChannel,
            int floatingBuffersPerGate,
            int sortShuffleMinParallelism,
            int sortShuffleMinBuffers,
            int numSubpartitions) {

        Tuple3<Integer, Integer, Integer> tuple =
                getMinMaxNetworkBuffersPerResultPartition(
                        configuredNetworkBuffersPerChannel,
                        floatingBuffersPerGate,
                        sortShuffleMinParallelism,
                        sortShuffleMinBuffers,
                        numSubpartitions,
                        false,
                        0,
                        type);

        // In order to avoid network buffer request timeout (see FLINK-12852), we announce
        // network buffer requirement by below:
        // 1. For canBePipelined shuffle, the floating buffers may not be returned in time due to
        // back pressure so we need to include all the floating buffers in the announcement, i.e. we
        // should take the max value;
        // 2. For blocking shuffle, it is back pressure free and floating buffers can be recycled
        // in time, so that the minimum required buffers would be enough.
        int ret = type.canBePipelinedConsumed() ? tuple.f2 : tuple.f0;

        if (ret == Integer.MAX_VALUE) {
            // Should never reach this branch. Result partition will allocate an unbounded
            // buffer pool only when type is ResultPartitionType.PIPELINED. But fine-grained
            // resource management is disabled in such case.
            throw new IllegalArgumentException(
                    "Illegal to announce network memory requirement as Integer.MAX_VALUE, partition type: "
                            + type);
        }
        return ret;
    }

    /** Private default constructor to avoid being instantiated. */
    private NettyShuffleUtils() {}
}
