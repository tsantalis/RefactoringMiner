/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.checkpoint.filemerging;

import org.apache.flink.api.common.TaskInfoImpl;
import org.apache.flink.core.fs.CloseableRegistry;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.core.fs.local.LocalFileSystem;
import org.apache.flink.runtime.checkpoint.filemerging.FileMergingSnapshotManager.SubtaskKey;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.state.CheckpointedStateScope;
import org.apache.flink.runtime.state.filesystem.AbstractFsCheckpointStorageAccess;
import org.apache.flink.runtime.state.filesystem.FileMergingCheckpointStateOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests for {@link FileMergingSnapshotManager}. */
public class FileMergingSnapshotManagerTest {

    private final String tmId = "Testing";

    private final OperatorID operatorID = new OperatorID(289347923L, 75893479L);

    private SubtaskKey subtaskKey1;
    private SubtaskKey subtaskKey2;

    private Path checkpointBaseDir;

    private int writeBufferSize;

    @BeforeEach
    public void setup(@TempDir java.nio.file.Path tempFolder) {
        // use simplified job ids for the tests
        long jobId = 1;
        subtaskKey1 = new SubtaskKey(operatorID, new TaskInfoImpl("TestingTask", 128, 0, 128, 3));
        subtaskKey2 = new SubtaskKey(operatorID, new TaskInfoImpl("TestingTask", 128, 1, 128, 3));
        checkpointBaseDir = new Path(tempFolder.toString(), String.valueOf(jobId));
        writeBufferSize = 4096;
    }

    @Test
    void testCreateFileMergingSnapshotManager() throws IOException {
        try (FileMergingSnapshotManagerBase fmsm =
                (FileMergingSnapshotManagerBase)
                        createFileMergingSnapshotManager(checkpointBaseDir)) {
            fmsm.registerSubtaskForSharedStates(subtaskKey1);
            assertThat(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.EXCLUSIVE))
                    .isEqualTo(
                            new Path(
                                    checkpointBaseDir,
                                    AbstractFsCheckpointStorageAccess
                                                    .CHECKPOINT_TASK_OWNED_STATE_DIR
                                            + "/"
                                            + tmId));
            assertThat(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.SHARED))
                    .isEqualTo(
                            new Path(
                                    checkpointBaseDir,
                                    AbstractFsCheckpointStorageAccess.CHECKPOINT_SHARED_STATE_DIR
                                            + "/"
                                            + subtaskKey1.getManagedDirName()));
        }
    }

    @Test
    void testCreateAndReuseFiles() throws IOException {
        try (FileMergingSnapshotManagerBase fmsm =
                (FileMergingSnapshotManagerBase)
                        createFileMergingSnapshotManager(checkpointBaseDir)) {
            fmsm.registerSubtaskForSharedStates(subtaskKey1);
            fmsm.registerSubtaskForSharedStates(subtaskKey2);
            // firstly, we try shared state.
            PhysicalFile file1 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 0, CheckpointedStateScope.SHARED);
            assertThat(file1.getFilePath().getParent())
                    .isEqualTo(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.SHARED));
            // allocate another
            PhysicalFile file2 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 0, CheckpointedStateScope.SHARED);
            assertThat(file2.getFilePath().getParent())
                    .isEqualTo(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.SHARED));
            assertThat(file2).isNotEqualTo(file1);

            // return for reuse
            fmsm.returnPhysicalFileForNextReuse(subtaskKey1, 0, file1);

            // allocate for another subtask
            PhysicalFile file3 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey2, 0, CheckpointedStateScope.SHARED);
            assertThat(file3.getFilePath().getParent())
                    .isEqualTo(fmsm.getManagedDir(subtaskKey2, CheckpointedStateScope.SHARED));
            assertThat(file3).isNotEqualTo(file1);

            // allocate for another checkpoint
            PhysicalFile file4 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 1, CheckpointedStateScope.SHARED);
            assertThat(file4.getFilePath().getParent())
                    .isEqualTo(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.SHARED));
            assertThat(file4).isNotEqualTo(file1);

            // allocate for this checkpoint
            PhysicalFile file5 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 0, CheckpointedStateScope.SHARED);
            assertThat(file5.getFilePath().getParent())
                    .isEqualTo(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.SHARED));
            assertThat(file5).isEqualTo(file1);

            // a physical file whose size is bigger than maxPhysicalFileSize cannot be reused
            file5.incSize(fmsm.maxPhysicalFileSize);
            fmsm.returnPhysicalFileForNextReuse(subtaskKey1, 0, file5);
            PhysicalFile file6 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 0, CheckpointedStateScope.SHARED);
            assertThat(file6.getFilePath().getParent())
                    .isEqualTo(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.SHARED));
            assertThat(file6).isNotEqualTo(file5);

            // Secondly, we try private state
            PhysicalFile file7 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 0, CheckpointedStateScope.EXCLUSIVE);
            assertThat(file7.getFilePath().getParent())
                    .isEqualTo(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.EXCLUSIVE));

            // allocate another
            PhysicalFile file8 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 0, CheckpointedStateScope.EXCLUSIVE);
            assertThat(file8.getFilePath().getParent())
                    .isEqualTo(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.EXCLUSIVE));
            assertThat(file8).isNotEqualTo(file6);

            // return for reuse
            fmsm.returnPhysicalFileForNextReuse(subtaskKey1, 0, file7);

            // allocate for another checkpoint
            PhysicalFile file9 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 1, CheckpointedStateScope.EXCLUSIVE);
            assertThat(file9.getFilePath().getParent())
                    .isEqualTo(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.EXCLUSIVE));
            assertThat(file9).isNotEqualTo(file7);

            // allocate for this checkpoint but another subtask
            PhysicalFile file10 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey2, 0, CheckpointedStateScope.EXCLUSIVE);
            assertThat(file10.getFilePath().getParent())
                    .isEqualTo(fmsm.getManagedDir(subtaskKey2, CheckpointedStateScope.EXCLUSIVE));
            assertThat(file10).isEqualTo(file7);

            // a physical file whose size is bigger than maxPhysicalFileSize cannot be reused
            file10.incSize(fmsm.maxPhysicalFileSize);
            fmsm.returnPhysicalFileForNextReuse(subtaskKey1, 0, file10);
            PhysicalFile file11 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 0, CheckpointedStateScope.SHARED);
            assertThat(file11.getFilePath().getParent())
                    .isEqualTo(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.SHARED));
            assertThat(file11).isNotEqualTo(file10);

            assertThat(fmsm.getManagedDir(subtaskKey2, CheckpointedStateScope.EXCLUSIVE))
                    .isEqualTo(fmsm.getManagedDir(subtaskKey1, CheckpointedStateScope.EXCLUSIVE));
        }
    }

    @Test
    void testRefCountBetweenLogicalAndPhysicalFiles() throws IOException {
        try (FileMergingSnapshotManagerBase fmsm =
                (FileMergingSnapshotManagerBase)
                        createFileMergingSnapshotManager(checkpointBaseDir)) {
            fmsm.registerSubtaskForSharedStates(subtaskKey1);
            fmsm.registerSubtaskForSharedStates(subtaskKey2);

            PhysicalFile physicalFile1 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 0, CheckpointedStateScope.SHARED);
            assertThat(physicalFile1.isOpen()).isTrue();

            LogicalFile logicalFile1 = fmsm.createLogicalFile(physicalFile1, 0, 10, subtaskKey1);
            assertThat(logicalFile1.getSubtaskKey()).isEqualTo(subtaskKey1);
            assertThat(logicalFile1.getPhysicalFile()).isEqualTo(physicalFile1);
            assertThat(logicalFile1.getStartOffset()).isZero();
            assertThat(logicalFile1.getLength()).isEqualTo(10);
            assertThat(physicalFile1.getRefCount()).isOne();

            assertThat(logicalFile1.isDiscarded()).isFalse();
            logicalFile1.advanceLastCheckpointId(2);
            assertThat(logicalFile1.getLastUsedCheckpointID()).isEqualTo(2);
            logicalFile1.advanceLastCheckpointId(1);
            assertThat(logicalFile1.getLastUsedCheckpointID()).isEqualTo(2);
            logicalFile1.discardWithCheckpointId(1);
            assertThat(logicalFile1.isDiscarded()).isFalse();
            logicalFile1.discardWithCheckpointId(2);
            assertThat(logicalFile1.isDiscarded()).isTrue();

            // the stream is still open for reuse
            assertThat(physicalFile1.isOpen()).isTrue();
            assertThat(physicalFile1.isDeleted()).isFalse();
            assertThat(physicalFile1.getRefCount()).isZero();

            physicalFile1.close();
            assertThat(physicalFile1.isOpen()).isFalse();
            assertThat(physicalFile1.isDeleted()).isTrue();

            // try close physical file but not deleted
            PhysicalFile physicalFile2 =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 0, CheckpointedStateScope.SHARED);
            LogicalFile logicalFile2 = fmsm.createLogicalFile(physicalFile2, 0, 10, subtaskKey1);
            assertThat(logicalFile2.getPhysicalFile()).isEqualTo(physicalFile2);
            assertThat(logicalFile2.getStartOffset()).isZero();
            assertThat(logicalFile2.getLength()).isEqualTo(10);
            assertThat(physicalFile2.getRefCount()).isOne();
            logicalFile2.advanceLastCheckpointId(2);

            assertThat(physicalFile2.isOpen()).isTrue();
            assertThat(physicalFile2.isDeleted()).isFalse();
            physicalFile2.close();
            assertThat(physicalFile2.isOpen()).isFalse();
            assertThat(physicalFile2.isDeleted()).isFalse();
            assertThat(physicalFile2.getRefCount()).isOne();

            logicalFile2.discardWithCheckpointId(2);
            assertThat(logicalFile2.isDiscarded()).isTrue();
            assertThat(physicalFile2.isDeleted()).isTrue();
            assertThat(physicalFile2.getRefCount()).isZero();
        }
    }

    @Test
    void testSizeStatsInPhysicalFile() throws IOException {
        try (FileMergingSnapshotManagerBase fmsm =
                (FileMergingSnapshotManagerBase)
                        createFileMergingSnapshotManager(checkpointBaseDir)) {
            fmsm.registerSubtaskForSharedStates(subtaskKey1);
            fmsm.registerSubtaskForSharedStates(subtaskKey2);
            PhysicalFile physicalFile =
                    fmsm.getOrCreatePhysicalFileForCheckpoint(
                            subtaskKey1, 0, CheckpointedStateScope.SHARED);

            assertThat(physicalFile.getSize()).isZero();
            physicalFile.incSize(123);
            assertThat(physicalFile.getSize()).isEqualTo(123);
            physicalFile.incSize(456);
            assertThat(physicalFile.getSize()).isEqualTo(123 + 456);
        }
    }

    @Test
    public void testReusedFileWriting() throws Exception {
        long checkpointId = 1;
        int streamNum = 10;
        int perStreamWriteNum = 128;

        // write random bytes and then read them from the file
        byte[] bytes = new byte[streamNum * perStreamWriteNum];
        Random rd = new Random();
        rd.nextBytes(bytes);
        int byteIndex = 0;

        SegmentFileStateHandle[] handles = new SegmentFileStateHandle[streamNum];
        try (FileMergingSnapshotManager fmsm = createFileMergingSnapshotManager(checkpointBaseDir);
                CloseableRegistry closeableRegistry = new CloseableRegistry()) {

            // repeatedly get-write-close streams
            for (int i = 0; i < streamNum; i++) {
                FileMergingCheckpointStateOutputStream stream =
                        fmsm.createCheckpointStateOutputStream(
                                subtaskKey1, checkpointId, CheckpointedStateScope.EXCLUSIVE);
                try {
                    closeableRegistry.registerCloseable(stream);
                    for (int j = 0; j < perStreamWriteNum; j++) {
                        stream.write(bytes[byteIndex++]);
                    }
                    handles[i] = stream.closeAndGetHandle();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // assert the streams writes to the same file correctly
            byteIndex = 0;
            Path filePath = null;
            for (SegmentFileStateHandle handle : handles) {
                // check file path
                Path thisFilePath = handle.getFilePath();
                assertThat(filePath == null || filePath.equals(thisFilePath)).isTrue();
                filePath = thisFilePath;
                // check file content
                FSDataInputStream is = handle.openInputStream();

                closeableRegistry.registerCloseable(is);
                int readValue;

                while ((readValue = is.read()) != -1) {
                    assertThat((byte) readValue).isEqualTo(bytes[byteIndex++]);
                }
            }
        }
    }

    @Test
    public void testConcurrentWriting() throws Exception {
        long checkpointId = 1;
        int numThreads = 12;
        int perStreamWriteNum = 128;
        Set<Future<SegmentFileStateHandle>> futures = new HashSet<>();

        try (FileMergingSnapshotManager fmsm = createFileMergingSnapshotManager(checkpointBaseDir);
                CloseableRegistry closeableRegistry = new CloseableRegistry()) {
            // write data concurrently
            for (int i = 0; i < numThreads; i++) {
                futures.add(
                        CompletableFuture.supplyAsync(
                                () -> {
                                    FileMergingCheckpointStateOutputStream stream =
                                            fmsm.createCheckpointStateOutputStream(
                                                    subtaskKey1,
                                                    checkpointId,
                                                    CheckpointedStateScope.EXCLUSIVE);
                                    try {
                                        closeableRegistry.registerCloseable(stream);
                                        for (int j = 0; j < perStreamWriteNum; j++) {
                                            stream.write(j);
                                        }
                                        return stream.closeAndGetHandle();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }));
            }

            // assert that multiple segments in the same file were not written concurrently
            for (Future<SegmentFileStateHandle> future : futures) {
                SegmentFileStateHandle segmentFileStateHandle = future.get();
                FSDataInputStream is = segmentFileStateHandle.openInputStream();
                closeableRegistry.registerCloseable(is);
                int readValue;
                int expected = 0;
                while ((readValue = is.read()) != -1) {
                    assertThat(readValue).isEqualTo(expected++);
                }
            }
        }
    }

    @Test
    public void testCheckpointNotification() throws Exception {
        try (FileMergingSnapshotManager fmsm = createFileMergingSnapshotManager(checkpointBaseDir);
                CloseableRegistry closeableRegistry = new CloseableRegistry()) {
            FileMergingCheckpointStateOutputStream cp1Stream =
                    writeCheckpointAndGetStream(1, fmsm, closeableRegistry);
            SegmentFileStateHandle cp1StateHandle = cp1Stream.closeAndGetHandle();
            fmsm.notifyCheckpointComplete(subtaskKey1, 1);
            assertFileInManagedDir(fmsm, cp1StateHandle);

            // complete checkpoint-2
            FileMergingCheckpointStateOutputStream cp2Stream =
                    writeCheckpointAndGetStream(2, fmsm, closeableRegistry);
            SegmentFileStateHandle cp2StateHandle = cp2Stream.closeAndGetHandle();
            fmsm.notifyCheckpointComplete(subtaskKey1, 2);
            assertFileInManagedDir(fmsm, cp2StateHandle);

            // subsume checkpoint-1
            assertThat(fileExists(cp1StateHandle)).isTrue();
            fmsm.notifyCheckpointSubsumed(subtaskKey1, 1);
            assertThat(fileExists(cp1StateHandle)).isFalse();

            // abort checkpoint-3
            FileMergingCheckpointStateOutputStream cp3Stream =
                    writeCheckpointAndGetStream(3, fmsm, closeableRegistry);
            SegmentFileStateHandle cp3StateHandle = cp3Stream.closeAndGetHandle();
            assertFileInManagedDir(fmsm, cp3StateHandle);
            fmsm.notifyCheckpointAborted(subtaskKey1, 3);
            assertThat(fileExists(cp3StateHandle)).isFalse();
        }
    }

    private FileMergingSnapshotManager createFileMergingSnapshotManager(Path checkpointBaseDir)
            throws IOException {
        FileSystem fs = LocalFileSystem.getSharedInstance();
        Path sharedStateDir =
                new Path(
                        checkpointBaseDir,
                        AbstractFsCheckpointStorageAccess.CHECKPOINT_SHARED_STATE_DIR);
        Path taskOwnedStateDir =
                new Path(
                        checkpointBaseDir,
                        AbstractFsCheckpointStorageAccess.CHECKPOINT_TASK_OWNED_STATE_DIR);
        if (!fs.exists(checkpointBaseDir)) {
            fs.mkdirs(checkpointBaseDir);
            fs.mkdirs(sharedStateDir);
            fs.mkdirs(taskOwnedStateDir);
        }
        FileMergingSnapshotManager fmsm = new FileMergingSnapshotManagerBuilder(tmId).build();
        fmsm.initFileSystem(
                LocalFileSystem.getSharedInstance(),
                checkpointBaseDir,
                sharedStateDir,
                taskOwnedStateDir,
                writeBufferSize);
        assertThat(fmsm).isNotNull();
        return fmsm;
    }

    private FileMergingCheckpointStateOutputStream writeCheckpointAndGetStream(
            long checkpointId, FileMergingSnapshotManager fmsm, CloseableRegistry closeableRegistry)
            throws IOException {
        return writeCheckpointAndGetStream(checkpointId, fmsm, closeableRegistry, 32);
    }

    private FileMergingCheckpointStateOutputStream writeCheckpointAndGetStream(
            long checkpointId,
            FileMergingSnapshotManager fmsm,
            CloseableRegistry closeableRegistry,
            int numBytes)
            throws IOException {
        FileMergingCheckpointStateOutputStream stream =
                fmsm.createCheckpointStateOutputStream(
                        subtaskKey1, checkpointId, CheckpointedStateScope.EXCLUSIVE);
        closeableRegistry.registerCloseable(stream);
        for (int i = 0; i < numBytes; i++) {
            stream.write(i);
        }
        return stream;
    }

    private void assertFileInManagedDir(
            FileMergingSnapshotManager fmsm, SegmentFileStateHandle stateHandle) {
        assertThat(fmsm instanceof FileMergingSnapshotManagerBase).isTrue();
        assertThat(stateHandle).isNotNull();
        Path filePath = stateHandle.getFilePath();
        assertThat(filePath).isNotNull();
        assertThat(((FileMergingSnapshotManagerBase) fmsm).isResponsibleForFile(filePath)).isTrue();
    }

    private boolean fileExists(SegmentFileStateHandle stateHandle) throws IOException {
        assertThat(stateHandle).isNotNull();
        Path filePath = stateHandle.getFilePath();
        assertThat(filePath).isNotNull();
        return filePath.getFileSystem().exists(filePath);
    }
}
