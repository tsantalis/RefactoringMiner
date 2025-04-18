/* This file is part of VoltDB.
 * Copyright (C) 2008-2015 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voltdb;

import java.util.Map;
import java.util.Set;

import org.voltdb.iv2.TransactionTask;
import org.voltdb.messaging.Iv2InitiateTaskMessage;

import com.google_voltpatches.common.util.concurrent.ListenableFuture;

public interface CommandLog {
    /**
     *
     * @param context
     * @param txnId
     *            The txnId of the truncation snapshot at the end of restore, or
     * @param partitionCount
     */
    public abstract void init(
                                 CatalogContext context,
                                 long txnId,
                                 int partitionCount, String coreBinding,
                                 Map<Integer, Long> perPartitionTxnId);

    /**
    *
     * @param txnId
     *            The txnId of the truncation snapshot at the end of restore, or
     *            Long.MIN if there was none.
     * @param partitionCount
     */
    public abstract void initForRejoin(
                                          CatalogContext context,
                                          long txnId,
                                          int partitionCount, boolean isRejoin,
                                          String coreBinding, Map<Integer, Long> perPartitionTxnId);

    public abstract boolean needsInitialization();

    /*
     *
     * The listener is will be provided with the handle once the message is durable.
     *
     * Returns a listenable future. If the returned future is null, then synchronous command logging
     * is in use and durability will be indicated via the durability listener. If the returned future
     * is not null then async command logging is in use. If the command log isn't falling behind the future
     * will already be completed, but if the command log is falling behind the future will be completed
     * when the log successfully writes out enough data to the file (although it won't call fsync since async)
     */
    public abstract ListenableFuture<Object> log(
            Iv2InitiateTaskMessage message,
            long spHandle,
            int[] involvedPartitions,
            DurabilityListener listener,
            TransactionTask durabilityHandle);

    public abstract void shutdown() throws InterruptedException;

    /**
     * IV2-only method.  Write this Iv2FaultLogEntry to the fault log portion of the command log
     */
    public abstract void logIv2Fault(long writerHSId, Set<Long> survivorHSId,
            int partitionId, long spHandle);

    public interface DurabilityListener {
        /**
         * Called from Scheduler to set up how all future completion checks will be handled
         */
        public void createFirstCompletionCheck(boolean isSyncLogging, boolean haveMpGateway);
        /**
         * Called from CommandLog to notify a Scheduler of the tasks/uniqueIds that have been made durable
         */
        public void onDurability();
        /**
         * Called from CommandLog to assign a new task to be tracked by the DurabilityListener
         */
        public void addTransaction(TransactionTask pendingTask);
        /**
         * Used by CommandLog to calculate the next task list size
         */
        public int getNumberOfTasks();
        /**
         * Used by CommandLog to crate a new CompletionCheck so the last CompletionCheck can be
         * triggered when the sync completes
         */
        public void startNewTaskList(int nextMaxRowCnt);
    }

    /**
     * Is Command logging enabled?
     */
    public abstract boolean isEnabled();

    /**
     * Attempt to start a truncation snapshot
     * If a truncation snapshot is pending, passing false means don't start another one
     */
    public void requestTruncationSnapshot(final boolean queueIfPending);

    /**
     * Statistics-related interface
     * Implementation should populate the stats based on column name to index mapping
     */
    public void populateCommandLogStats(Map<String, Integer> columnNameToIndex, Object[] rowValues);

    /**
     * Does this logger do synchronous logging
     */
    public abstract boolean isSynchronous();

    /**
     * Assign DurabilityListener from each SpScheduler to commmand log
     */
    public abstract void registerDurabilityListener(DurabilityListener durabilityListener);
}
