/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.novoda.downloadmanager.lib;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;

import com.novoda.notils.logger.simple.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

/**
 * Performs background downloads as requested by applications that use
 * {DownloadManager}. Multiple start commands can be issued at this
 * service, and it will continue running until no downloads are being actively
 * processed. It may schedule alarms to resume downloads in future.
 * <p/>
 * Any database updates important enough to initiate tasks should always be
 * delivered through {Context#startService(Intent)}.
 */
public class DownloadService extends Service {
    // TODO: migrate WakeLock from individual DownloadThreads out into
    // DownloadReceiver to protect our entire workflow.

    private static final boolean DEBUG_LIFECYCLE = false;
    private final ContentLengthFetcher contentLengthFetcher = new ContentLengthFetcher();

    //    @VisibleForTesting
    SystemFacade mSystemFacade;

    private AlarmManager mAlarmManager;
    private StorageManager mStorageManager;

    /**
     * Observer to get notified when the content observer's data changes
     */
    private DownloadManagerContentObserver mObserver;

    /**
     * Class to handle Notification Manager updates
     */
    private DownloadNotifier mNotifier;

    /**
     * The Service's view of the list of downloads, mapping download IDs to the corresponding info
     * object. This is kept independently from the content provider, and the Service only initiates
     * downloads based on this data, so that it can deal with situation where the data in the
     * content provider changes or disappears.
     */
//    @GuardedBy("mDownloads")
    private final Map<Long, DownloadInfo> mDownloads = new HashMap<>();

    private final Map<Long, BatchInfo> batches = new HashMap<>();

    private ExecutorService mExecutor;

    private DownloadScanner mScanner;

    private HandlerThread mUpdateThread;
    private Handler mUpdateHandler;

    private volatile int mLastStartId;
    private DownloadClientReadyChecker downloadClientReadyChecker;
    private ContentResolver resolver;

    /**
     * Receives notifications when the data in the content provider changes
     */
    private class DownloadManagerContentObserver extends ContentObserver {
        public DownloadManagerContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(final boolean selfChange) {
            enqueueUpdate();
        }
    }

    /**
     * Returns an IBinder instance when someone wants to connect to this
     * service. Binding to this service is not allowed.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public IBinder onBind(Intent i) {
        throw new UnsupportedOperationException("Cannot bind to Download Manager Service");
    }

    /**
     * Initializes the service when it is first created
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.v("Service onCreate");

        if (mSystemFacade == null) {
            mSystemFacade = new RealSystemFacade(this);
        }

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mStorageManager = new StorageManager(this);

        mUpdateThread = new HandlerThread("DownloadManager-UpdateThread");
        mUpdateThread.start();
        mUpdateHandler = new Handler(mUpdateThread.getLooper(), mUpdateCallback);

        mScanner = new DownloadScanner(this);

        downloadClientReadyChecker = getDownloadClientReadyChecker();

        mNotifier = new DownloadNotifier(this, getNotificationImageRetriever(), getResources());
        mNotifier.cancelAll();

        mObserver = new DownloadManagerContentObserver();
        getContentResolver().registerContentObserver(
                Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                true, mObserver);

        ConcurrentDownloadsLimitProvider concurrentDownloadsLimitProvider = ConcurrentDownloadsLimitProvider.newInstance(this);
        DownloadExecutorFactory factory = new DownloadExecutorFactory(concurrentDownloadsLimitProvider);
        mExecutor = factory.createExecutor();
        resolver = getContentResolver();
    }

    private DownloadClientReadyChecker getDownloadClientReadyChecker() {
        if (!(getApplication() instanceof DownloadClientReadyChecker)) {
            return DownloadClientReadyChecker.READY;
        }
        return (DownloadClientReadyChecker) getApplication();
    }

    private NotificationImageRetriever getNotificationImageRetriever() {
        if (!(getApplication() instanceof NotificationImageRetrieverFactory)) {
            return new OkHttpNotificationImageRetriever();
        }
        return ((NotificationImageRetrieverFactory) getApplication()).createNotificationImageRetriever();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int returnValue = super.onStartCommand(intent, flags, startId);
        Log.v("Service onStart");
        mLastStartId = startId;
        enqueueUpdate();
        return returnValue;
    }

    @Override
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mObserver);
        mScanner.shutdown();
        mUpdateThread.quit();
        Log.v("Service onDestroy");
        super.onDestroy();
    }

    /**
     * Enqueue an {#updateLocked()} pass to occur in future.
     */
    private void enqueueUpdate() {
        mUpdateHandler.removeMessages(MSG_UPDATE);
        mUpdateHandler.obtainMessage(MSG_UPDATE, mLastStartId, -1).sendToTarget();
    }

    /**
     * Enqueue an {#updateLocked()} pass to occur after delay, usually to
     * catch any finished operations that didn't trigger an update pass.
     */
    private void enqueueFinalUpdate() {
        mUpdateHandler.removeMessages(MSG_FINAL_UPDATE);
        mUpdateHandler.sendMessageDelayed(
                mUpdateHandler.obtainMessage(MSG_FINAL_UPDATE, mLastStartId, -1),
                5 * MINUTE_IN_MILLIS);
    }

    private static final int MSG_UPDATE = 1;
    private static final int MSG_FINAL_UPDATE = 2;

    private Handler.Callback mUpdateCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            final int startId = msg.arg1;
            if (DEBUG_LIFECYCLE) {
                Log.v("Updating for startId " + startId);
            }

            // Since database is current source of truth, our "active" status
            // depends on database state. We always get one final update pass
            // once the real actions have finished and persisted their state.

            // TODO: switch to asking real tasks to derive active state
            // TODO: handle media scanner timeouts

            final boolean isActive;
            synchronized (mDownloads) {
                isActive = updateLocked();
            }

            if (msg.what == MSG_FINAL_UPDATE) {
                // Dump thread stacks belonging to pool
                for (Map.Entry<Thread, StackTraceElement[]> entry :
                        Thread.getAllStackTraces().entrySet()) {
                    if (entry.getKey().getName().startsWith("pool")) {
                        Log.d(entry.getKey() + ": " + Arrays.toString(entry.getValue()));
                    }
                }

                // Dump speed and update details
                mNotifier.dumpSpeeds();

                Log.wtf("Final update pass triggered, isActive=" + isActive, new IllegalStateException("someone didn't update correctly"));
            }

            if (isActive) {
                // Still doing useful work, keep service alive. These active
                // tasks will trigger another update pass when they're finished.

                // Enqueue delayed update pass to catch finished operations that
                // didn't trigger an update pass; these are bugs.
                enqueueFinalUpdate();

            } else {
                // No active tasks, and any pending update messages can be
                // ignored, since any updates important enough to initiate tasks
                // will always be delivered with a new startId.

                if (stopSelfResult(startId)) {
                    if (DEBUG_LIFECYCLE) {
                        Log.v("Nothing left; stopped");
                    }
                    getContentResolver().unregisterContentObserver(mObserver);
                    mScanner.shutdown();
                    mUpdateThread.quit();
                }
            }

            return true;
        }
    };

    /**
     * Update {#mDownloads} to match {DownloadProvider} state.
     * Depending on current download state it may enqueue {DownloadThread}
     * instances, request {DownloadScanner} scans, update user-visible
     * notifications, and/or schedule future actions with {AlarmManager}.
     * <p/>
     * Should only be called from {#mUpdateThread} as after being
     * requested through {#enqueueUpdate()}.
     *
     * @return If there are active tasks being processed, as of the database
     * snapshot taken in this update.
     */
    private boolean updateLocked() {
        final long now = mSystemFacade.currentTimeMillis();

        boolean isActive = false;
        long nextActionMillis = Long.MAX_VALUE;

        final Set<Long> staleDownloadIds = new HashSet<Long>(mDownloads.keySet());

        Cursor batchesCursor = resolver.query(Downloads.Impl.BATCH_CONTENT_URI, null, null, null, null);
        batches.clear();
        try {
            int idColumn = batchesCursor.getColumnIndexOrThrow(Downloads.Impl.Batches._ID);
            while (batchesCursor.moveToNext()) {
                long id = batchesCursor.getLong(idColumn);

                String title = batchesCursor.getString(batchesCursor.getColumnIndexOrThrow(Downloads.Impl.Batches.COLUMN_TITLE));
                String description = batchesCursor.getString(batchesCursor.getColumnIndexOrThrow(Downloads.Impl.Batches.COLUMN_DESCRIPTION));
                String bigPictureUrl = batchesCursor.getString(batchesCursor.getColumnIndexOrThrow(Downloads.Impl.Batches.COLUMN_BIG_PICTURE));

                batches.put(id, new BatchInfo(title, description, bigPictureUrl));
            }
        } finally {
            batchesCursor.close();
        }

        final Cursor downloadsCursor = resolver.query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, null, null, null, null);
        try {
            final DownloadInfo.Reader reader = new DownloadInfo.Reader(resolver, downloadsCursor);
            final int idColumn = downloadsCursor.getColumnIndexOrThrow(Downloads.Impl._ID);
            while (downloadsCursor.moveToNext()) {
                long id = downloadsCursor.getLong(idColumn);
                staleDownloadIds.remove(id);

                DownloadInfo info = mDownloads.get(id);
                if (info != null) {
                    updateDownload(reader, info, now);
                } else {
                    info = insertDownloadLocked(reader, now);
                }

                if (info.mDeleted) {
                    afterCleanUpDeleteDownload(info);
                } else {
                    updateTotalBytesFor(info);
                    isActive = kickOffDownloadTaskIfReady(isActive, info);
                    isActive = kickOffMediaScanIfCompleted(isActive, info);
                }

                // Keep track of nearest next action
                nextActionMillis = Math.min(info.nextActionMillis(now), nextActionMillis);
            }
        } finally {
            downloadsCursor.close();
        }

        cleanUpStaleDownloadsThatDisappeared(staleDownloadIds);

        updateUserVisibleNotification();

        // Set alarm when next action is in future. It's okay if the service
        // continues to run in meantime, since it will kick off an update pass.
        if (nextActionMillis > 0 && nextActionMillis < Long.MAX_VALUE) {
            Log.v("scheduling start in " + nextActionMillis + "ms");

            final Intent intent = new Intent(Constants.ACTION_RETRY);
            intent.setClass(this, DownloadReceiver.class);
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, now + nextActionMillis, PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT));
        }

        return isActive;
    }

    private void updateTotalBytesFor(DownloadInfo info) {
        if (info.mTotalBytes == -1) {
            ContentValues values = new ContentValues();
            info.mTotalBytes = contentLengthFetcher.fetchContentLengthFor(info);
            resolver.update(info.getAllDownloadsUri(), values, null, null);
        }
    }

    private void afterCleanUpDeleteDownload(DownloadInfo info) {
        if (!TextUtils.isEmpty(info.mMediaProviderUri)) {
            resolver.delete(Uri.parse(info.mMediaProviderUri), null, null);
        }

        deleteFileIfExists(info.mFileName);
        resolver.delete(info.getAllDownloadsUri(), null, null);
    }

    private boolean kickOffDownloadTaskIfReady(boolean isActive, DownloadInfo info) {
        boolean readyToDownload = info.isReadyToDownload();
        if (readyToDownload) {
            isActive |= info.startDownloadIfNotActive(mExecutor);
        }
        return isActive;
    }

    private boolean kickOffMediaScanIfCompleted(boolean isActive, DownloadInfo info) {
        final boolean activeScan = info.startScanIfReady(mScanner);

        isActive |= activeScan;
        return isActive;
    }

    private void cleanUpStaleDownloadsThatDisappeared(Set<Long> staleIds) {
        for (Long id : staleIds) {
            deleteDownloadLocked(id);
        }
    }

    private void updateUserVisibleNotification() {
        mNotifier.updateWith(batches, mDownloads.values());
    }

    /**
     * Keeps a local copy of the info about a download, and initiates the
     * download if appropriate.
     */
    private DownloadInfo insertDownloadLocked(DownloadInfo.Reader reader, long now) {
        DownloadInfo info = reader.newDownloadInfo(this, mSystemFacade, mStorageManager, mNotifier, downloadClientReadyChecker);
        mDownloads.put(info.mId, info);

        Log.v("processing inserted download " + info.mId);

        return info;
    }

    /**
     * Updates the local copy of the info about a download.
     */
    private void updateDownload(DownloadInfo.Reader reader, DownloadInfo info, long now) {
        reader.updateFromDatabase(info);
        Log.v("processing updated download " + info.mId + ", status: " + info.mStatus);
    }

    /**
     * Removes the local copy of the info about a download.
     */
    private void deleteDownloadLocked(long id) {
        DownloadInfo info = mDownloads.get(id);
        if (info.mStatus == Downloads.Impl.STATUS_RUNNING) {
            info.mStatus = Downloads.Impl.STATUS_CANCELED;
        }
        if (info.mDestination != Downloads.Impl.DESTINATION_EXTERNAL && info.mFileName != null) {
            Log.d("deleteDownloadLocked() deleting " + info.mFileName);
            deleteFileIfExists(info.mFileName);
        }
        mDownloads.remove(info.mId);
    }

    private void deleteFileIfExists(String path) {
        if (!TextUtils.isEmpty(path)) {
            Log.d("deleteFileIfExists() deleting " + path);
            final File file = new File(path);
            if (file.exists() && !file.delete()) {
                Log.w("file: '" + path + "' couldn't be deleted");
            }
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        Log.e("I want to dump but nothing to dump into");
    }
}
