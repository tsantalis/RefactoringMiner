/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.novoda.downloadmanager.R;
import com.novoda.notils.logger.simple.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.novoda.downloadmanager.lib.Request.*;

/**
 * Update {@link NotificationManager} to reflect current {@link DownloadInfo}
 * states. Collapses similar downloads into a single notification, and builds
 * {@link PendingIntent} that launch towards {DownloadReceiver}.
 */
class DownloadNotifier {

    private static final int TYPE_ACTIVE = 1;
    private static final int TYPE_WAITING = 2;
    private static final int TYPE_SUCCESS = 3;
    private static final int TYPE_FAILED = 4;
    private static final int TYPE_CANCELLED = 5;

    private final Context mContext;
    private final NotificationImageRetriever imageRetriever;
    private final NotificationManager mNotifManager;

    /**
     * Currently active notifications, mapped from clustering tag to timestamp
     * when first shown.
     *
     * @see #buildNotificationTag(List, long)
     */
//    @GuardedBy("mActiveNotifs")
    private final HashMap<String, Long> mActiveNotifs = new HashMap<String, Long>();

    /**
     * Current speed of active downloads, mapped from {@link DownloadInfo#batchId}
     * to speed in bytes per second.
     */
//    @GuardedBy("mDownloadSpeed")
    // LongSparseLongArray
    private final LongSparseArray<Long> mDownloadSpeed = new LongSparseArray<Long>();

    /**
     * Last time speed was reproted, mapped from {@link DownloadInfo#batchId} to
     * {@link SystemClock#elapsedRealtime()}.
     */
//    @GuardedBy("mDownloadSpeed")
    // LongSparseLongArray
    private final LongSparseArray<Long> mDownloadTouch = new LongSparseArray<Long>();

    private final Resources resources;

    public DownloadNotifier(Context context, NotificationImageRetriever imageRetriever, Resources resources) {
        this.mContext = context;
        this.resources = resources;
        this.imageRetriever = imageRetriever;
        this.mNotifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void cancelAll() {
        mNotifManager.cancelAll();
    }

    /**
     * Notify the current speed of an active download, used for calculating
     * estimated remaining time.
     */
    public void notifyDownloadSpeed(long id, long bytesPerSecond) {
        synchronized (mDownloadSpeed) {
            if (bytesPerSecond != 0) {
                mDownloadSpeed.put(id, bytesPerSecond);
                mDownloadTouch.put(id, SystemClock.elapsedRealtime());
            } else {
                mDownloadSpeed.remove(id);
                mDownloadTouch.remove(id);
            }
        }
    }

    /**
     * Update {@link NotificationManager} to reflect the given set of
     * {@link DownloadInfo}, adding, collapsing, and removing as needed.
     */
    public void updateWith(Map<Long, BatchInfo> batches, Collection<DownloadInfo> downloads) {
        synchronized (mActiveNotifs) {
            updateWithLocked(batches, downloads);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void updateWithLocked(Map<Long, BatchInfo> batches, Collection<DownloadInfo> downloads) {
        Map<Long, List<DownloadInfo>> batchDownloads = getDownloadsPerBatch(downloads);
        Map<String, List<DownloadInfo>> clustered = getClustersByNotificationTag(batchDownloads);

        showNotificationPerCluster(batches, clustered);

        removeStaleTagsThatWereNotRenewed(clustered);
    }

    private void showNotificationPerCluster(Map<Long, BatchInfo> batches, Map<String, List<DownloadInfo>> clustered) {
        for (String tag : clustered.keySet()) {
            int type = getNotificationTagType(tag);
            List<DownloadInfo> cluster = clustered.get(tag);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
            useTimeWhenClusterFirstShownToAvoidShuffling(tag, builder);
            buildIcon(type, builder);
            buildActionIntents(tag, type, cluster, builder);

            Notification notification = buildTitlesAndDescription(type, cluster, builder, batches);
            mNotifManager.notify(tag.hashCode(), notification);
        }
    }

    @NonNull
    private Map<String, List<DownloadInfo>> getClustersByNotificationTag(Map<Long, List<DownloadInfo>> batchDownloads) {
        Map<String, List<DownloadInfo>> clustered = new HashMap<>();

        for (Map.Entry<Long, List<DownloadInfo>> batch : batchDownloads.entrySet()) {
            List<DownloadInfo> downloadsInBatch = batch.getValue();
            final String tag = buildNotificationTag(downloadsInBatch, batch.getKey());

            for (DownloadInfo download : downloadsInBatch) {
                addDownloadToCluster(tag, clustered, download);
            }
        }
        return clustered;
    }

    @NonNull
    private Map<Long, List<DownloadInfo>> getDownloadsPerBatch(Collection<DownloadInfo> downloads) {
        Map<Long, List<DownloadInfo>> batchDownloads = new HashMap<>();

        for (DownloadInfo download : downloads) {
            long batchId = download.batchId;
            if (batchDownloads.containsKey(batchId)) {
                batchDownloads.get(batchId).add(download);
            } else {
                List<DownloadInfo> downloadsInBatch = new ArrayList<>();
                downloadsInBatch.add(download);
                batchDownloads.put(batchId, downloadsInBatch);
            }
        }
        return batchDownloads;
    }

    private void addDownloadToCluster(String tag, Map<String, List<DownloadInfo>> cluster, DownloadInfo info) {
        if (tag == null) {
            return;
        }

        List<DownloadInfo> downloadInfos;

        if (cluster.containsKey(tag)) {
            downloadInfos = cluster.get(tag);
        } else {
            downloadInfos = new ArrayList<DownloadInfo>();
            cluster.put(tag, downloadInfos); // TODO not sure if this is right compared to ArrayListMultiMap
        }

        downloadInfos.add(info);
    }

    private void useTimeWhenClusterFirstShownToAvoidShuffling(String tag, NotificationCompat.Builder builder) {
        final long firstShown;
        if (mActiveNotifs.containsKey(tag)) {
            firstShown = mActiveNotifs.get(tag);
        } else {
            firstShown = System.currentTimeMillis();
            mActiveNotifs.put(tag, firstShown);
        }
        builder.setWhen(firstShown);
    }

    private void buildIcon(int type, NotificationCompat.Builder builder) {
        switch (type) {
            case TYPE_ACTIVE:
                builder.setSmallIcon(android.R.drawable.stat_sys_download);
                break;
            case TYPE_WAITING:
            case TYPE_FAILED:
                builder.setSmallIcon(android.R.drawable.stat_sys_warning);
                break;
            case TYPE_SUCCESS:
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
                break;
            default:
                //don't set an icon if none matches
                break;
        }
    }

    private void buildActionIntents(String tag, int type, Collection<DownloadInfo> cluster, NotificationCompat.Builder builder) {
        if (type == TYPE_ACTIVE || type == TYPE_WAITING) {
            // build a synthetic uri for intent identification purposes
            Uri uri = new Uri.Builder().scheme("active-dl").appendPath(tag).build();
            Intent clickIntent = new Intent(Constants.ACTION_LIST, uri, mContext, DownloadReceiver.class);
            clickIntent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS, getDownloadIds(cluster));
            builder.setContentIntent(PendingIntent.getBroadcast(mContext, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT));
            builder.setOngoing(true);

            DownloadInfo info = cluster.iterator().next();
            uri = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, info.mId);
            Intent cancelIntent = new Intent(Constants.ACTION_CANCEL, uri, mContext, DownloadReceiver.class);
            PendingIntent pendingCancelIntent = PendingIntent.getBroadcast(mContext, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.dl__ic_action_cancel, "Cancel", pendingCancelIntent);

        } else if (type == TYPE_SUCCESS) {
            final DownloadInfo info = cluster.iterator().next();
            final Uri uri = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, info.mId);
            builder.setAutoCancel(true);

            final String action;
            if (Downloads.Impl.isStatusError(info.mStatus)) {
                action = Constants.ACTION_LIST;
            } else if (info.mDestination != Downloads.Impl.DESTINATION_SYSTEMCACHE_PARTITION) {
                action = Constants.ACTION_OPEN;
            } else {
                action = Constants.ACTION_LIST;
            }

            final Intent intent = new Intent(action, uri, mContext, DownloadReceiver.class);
            intent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS, getDownloadIds(cluster));
            builder.setContentIntent(PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

            final Intent hideIntent = new Intent(Constants.ACTION_HIDE, uri, mContext, DownloadReceiver.class);
            builder.setDeleteIntent(PendingIntent.getBroadcast(mContext, 0, hideIntent, 0));
        }
    }

    private Notification buildTitlesAndDescription(
            int type,
            List<DownloadInfo> cluster,
            NotificationCompat.Builder builder,
            Map<Long, BatchInfo> batches) {

        String remainingText = null;
        String percentText = null;
        if (type == TYPE_ACTIVE) {
            long currentBytes = 0;
            long totalBytes = 0;
            long totalBytesPerSecond = 0;
            synchronized (mDownloadSpeed) {
                for (DownloadInfo info : cluster) {
                    if (info.mTotalBytes != -1) {
                        currentBytes += info.mCurrentBytes;
                        totalBytes += info.mTotalBytes;
                        Long bytesPerSecond = mDownloadSpeed.get(info.batchId);
                        if (bytesPerSecond != null) {
                            totalBytesPerSecond += bytesPerSecond;
                        }
                    }
                }
            }

            if (totalBytes > 0) {
                int percent = (int) ((currentBytes * 100) / totalBytes);
                percentText = percent + "%";//res.getString(R.string.download_percent, percent);

                if (totalBytesPerSecond > 0) {
                    long remainingMillis = ((totalBytes - currentBytes) * 1000) / totalBytesPerSecond;
                    remainingText = "Duration " + formatDuration(remainingMillis);
                }

                builder.setProgress(100, percent, false);
            } else {
                builder.setProgress(100, 0, true);
            }
        }

        Set<BatchInfo> currentBatches = new HashSet<>();
        for (DownloadInfo info : cluster) {
            BatchInfo batch = batches.get(info.batchId);
            currentBatches.add(batch);
        }

        if (currentBatches.size() == 1) {
            BatchInfo batch = currentBatches.iterator().next();
            return buildSingleNotification(type, builder, batch, remainingText, percentText);

        } else {
            return buildStackedNotification(type, builder, currentBatches, remainingText, percentText);
        }

    }

    private Notification buildSingleNotification(int type, NotificationCompat.Builder builder, BatchInfo batch, String remainingText, String percentText) {

        NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
        String imageUrl = batch.getBigPictureUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            Bitmap bitmap = imageRetriever.retrieveImage(imageUrl);
            style.bigPicture(bitmap);
        }
        CharSequence title = getDownloadTitle(batch);
        builder.setContentTitle(title);
        style.setBigContentTitle(title);

        if (type == TYPE_ACTIVE) {
            String description = batch.getDescription();
            if (TextUtils.isEmpty(description)) {
                setSecondaryNotificationText(builder, style, remainingText);
            } else {
                setSecondaryNotificationText(builder, style, description);
            }
            builder.setContentInfo(percentText);

        } else if (type == TYPE_WAITING) {
            setSecondaryNotificationText(builder, style, "Download size requires Wi-Fi.");

        } else if (type == TYPE_SUCCESS) {
            setSecondaryNotificationText(builder, style, "Download complete.");
        } else if (type == TYPE_FAILED) {
            setSecondaryNotificationText(builder, style, "Download unsuccessful.");
        } else if (type == TYPE_CANCELLED) {
            setSecondaryNotificationText(builder, style, "Download cancelled.");
        }

        if (!TextUtils.isEmpty(imageUrl)) {
            builder.setStyle(style);
        }
        return builder.build();
    }

    private void setSecondaryNotificationText(NotificationCompat.Builder builder, NotificationCompat.BigPictureStyle style, String description) {
        builder.setContentText(description);
        style.setSummaryText(description);
    }

    private Notification buildStackedNotification(int type, NotificationCompat.Builder builder, Set<BatchInfo> currentBatches, String remainingText, String percentText) {
        final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle(builder);

        for (BatchInfo batch : currentBatches) {
            inboxStyle.addLine(getDownloadTitle(batch));
        }

        if (type == TYPE_ACTIVE) {
            builder.setContentTitle(resources.getQuantityString(R.plurals.dl__notif_summary_active, currentBatches.size(), currentBatches.size()));
            builder.setContentInfo(percentText);
            setSecondaryNotificationText(builder, inboxStyle, remainingText);
        } else if (type == TYPE_WAITING) {
            builder.setContentTitle(resources.getQuantityString(R.plurals.dl__notif_summary_waiting, currentBatches.size(), currentBatches.size()));
            setSecondaryNotificationText(builder, inboxStyle, "Download size requires Wi-Fi.");
        } else if (type == TYPE_SUCCESS) {
            setSecondaryNotificationText(builder, inboxStyle, "Download complete.");
        } else if (type == TYPE_FAILED) {
            setSecondaryNotificationText(builder, inboxStyle, "Download unsuccessful.");
        } else if (type == TYPE_CANCELLED) {
            setSecondaryNotificationText(builder, inboxStyle, "Download cancelled.");
        }

        return inboxStyle.build();
    }

    private void setSecondaryNotificationText(NotificationCompat.Builder builder, NotificationCompat.InboxStyle style, String description) {
        builder.setContentText(description);
        style.setSummaryText(description);
    }

    private void removeStaleTagsThatWereNotRenewed(Map<String, List<DownloadInfo>> clustered) {
        final Iterator<String> tags = mActiveNotifs.keySet().iterator();
        while (tags.hasNext()) {
            final String tag = tags.next();
            if (!clustered.containsKey(tag)) {
                mNotifManager.cancel(tag.hashCode());
                tags.remove();
            }
        }
    }

    private static CharSequence getDownloadTitle(BatchInfo batch) {
        String title = batch.getTitle();
        if (TextUtils.isEmpty(title)) {
            return "unknown";
        } else {
            return title;
        }
    }

    private long[] getDownloadIds(Collection<DownloadInfo> infos) {
        final long[] ids = new long[infos.size()];
        int i = 0;
        for (DownloadInfo info : infos) {
            ids[i++] = info.mId;
        }
        return ids;
    }

    public void dumpSpeeds() {
        Log.e("dump at speed");
    }

    /**
     * Build tag used for collapsing several {@link DownloadInfo} into a single
     * {@link Notification}.
     *
     * @param downloads
     * @param batchId
     */
    private String buildNotificationTag(List<DownloadInfo> downloads, long batchId) {
        if (areAllDownloadsQueued(downloads)) {
            return TYPE_WAITING + ":" + getPackageName();
        } else if (areAnyActiveAndVisible(downloads)) {
            return TYPE_ACTIVE + ":" + getPackageName();
        } else if (areAnyFailedAndVisible(downloads)) {
            // Failed downloads always have unique notifs
            return TYPE_FAILED + ":" + batchId;
        } else if (areAnyCancelledAndVisible(downloads)) {
            // Cancelled downloads always have unique notifs
            return TYPE_CANCELLED + ":" + batchId;
        } else if (areAllSuccessfulAndVisible(downloads)) {
            // Complete downloads always have unique notifs
            return TYPE_SUCCESS + ":" + batchId;
        } else {
            return null;
        }
    }

    private static boolean areAllDownloadsQueued(List<DownloadInfo> downloads) {
        for (DownloadInfo download : downloads) {
            boolean isNotQueuedForWiFi = download.mStatus != Downloads.Impl.STATUS_QUEUED_FOR_WIFI;
            if (isNotQueuedForWiFi) {
                return false;
            }
        }
        return true;
    }

    private static boolean areAnyFailedAndVisible(List<DownloadInfo> downloads) {
        for (DownloadInfo download : downloads) {
            boolean isFailed = Downloads.Impl.isStatusError(download.mStatus) && !Downloads.Impl.isStatusCancelled(download.mStatus);
            boolean isVisible = download.mVisibility == VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION
                    || download.mVisibility == VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
            if (isFailed && isVisible) {
                return true;
            }
        }
        return false;
    }

    private static boolean areAnyCancelledAndVisible(List<DownloadInfo> downloads) {
        for (DownloadInfo download : downloads) {
            boolean isCancelled = Downloads.Impl.isStatusCancelled(download.mStatus);
            boolean isVisible = download.mVisibility == VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION
                    || download.mVisibility == VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
            if (isCancelled && isVisible) {
                return true;
            }
        }
        return false;
    }

    private String getPackageName() {
        return mContext.getPackageName();
    }

    /**
     * Return the cluster type of the given as created by
     * {@link #buildNotificationTag(List, long)}.
     */
    private static int getNotificationTagType(String tag) {
        return Integer.parseInt(tag.substring(0, tag.indexOf(':')));
    }

    private static boolean areAnyActiveAndVisible(List<DownloadInfo> downloads) {
        for (DownloadInfo download : downloads) {
            boolean isActive = download.mStatus == Downloads.Impl.STATUS_RUNNING;
            boolean isVisible = download.mVisibility == VISIBILITY_VISIBLE
                    || download.mVisibility == VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
            if (isActive && isVisible) {
                return true;
            }
        }

        return false;
    }

    private static boolean areAllSuccessfulAndVisible(List<DownloadInfo> downloads) {
        for (DownloadInfo download : downloads) {
            boolean isSuccessful = Downloads.Impl.isStatusSuccess(download.mStatus);
            boolean isVisible = download.mVisibility == VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION
                    || download.mVisibility == VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
            if (!(isSuccessful && isVisible)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return given duration in a human-friendly format. For example, "4
     * minutes" or "1 second". Returns only largest meaningful unit of time,
     * from seconds up to hours.
     */
    public CharSequence formatDuration(long millis) {
        if (millis >= DateUtils.HOUR_IN_MILLIS) {
            int hours = (int) TimeUnit.MILLISECONDS.toHours(millis + TimeUnit.MINUTES.toMillis(30));
            return resources.getQuantityString(R.plurals.dl__duration_hours, hours, hours);
        } else if (millis >= DateUtils.MINUTE_IN_MILLIS) {
            int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(millis + TimeUnit.SECONDS.toMillis(30));
            return resources.getQuantityString(R.plurals.dl__duration_minutes, minutes, minutes);
        } else {
            int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(millis + 500);
            return resources.getQuantityString(R.plurals.dl__duration_seconds, seconds, seconds);
        }
    }
}
