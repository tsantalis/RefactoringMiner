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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.novoda.downloadmanager.R;
import com.novoda.notils.logger.simple.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
     * @see #buildNotificationTag(DownloadBatch)
     */
//    @GuardedBy("mActiveNotifs")
    private final HashMap<String, Long> mActiveNotifs = new HashMap<>();

    /**
     * Current speed of active downloads, mapped from {@link DownloadInfo#batchId}
     * to speed in bytes per second.
     */
//    @GuardedBy("mDownloadSpeed")
    // LongSparseLongArray
    private final LongSparseArray<Long> mDownloadSpeed = new LongSparseArray<>();

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
            } else {
                mDownloadSpeed.remove(id);
            }
        }
    }

    /**
     * Update {@link NotificationManager} to reflect the given set of
     * {@link DownloadInfo}, adding, collapsing, and removing as needed.
     */
    public void updateWith(List<DownloadBatch> batches) {
        synchronized (mActiveNotifs) {
            Map<String, List<DownloadBatch>> clusters = getClustersByNotificationTag(batches);

            showNotificationPerCluster(clusters);

            removeStaleTagsThatWereNotRenewed(clusters);
        }
    }

    @NonNull
    private Map<String, List<DownloadBatch>> getClustersByNotificationTag(List<DownloadBatch> batches) {
        Map<String, List<DownloadBatch>> clustered = new HashMap<>();

        for (DownloadBatch batch : batches) {
            String tag = buildNotificationTag(batch);

            addBatchToCluster(tag, clustered, batch);
        }

        return clustered;
    }

    /**
     * Build tag used for collapsing several {@link DownloadInfo} into a single
     * {@link Notification}.
     */
    private String buildNotificationTag(DownloadBatch batch) {
        int status = batch.getStatus();
        int visibility = batch.getInfo().getVisibility();
        if (status == Downloads.Impl.STATUS_QUEUED_FOR_WIFI) {
            return TYPE_WAITING + ":" + mContext.getPackageName();
        } else if (status == Downloads.Impl.STATUS_RUNNING && shouldShowActiveItem(visibility)) {
            return TYPE_ACTIVE + ":" + mContext.getPackageName();
        } else if (Downloads.Impl.isStatusError(status) && !Downloads.Impl.isStatusCancelled(status)
                && shouldShowCompletedItem(visibility)) {
            // Failed downloads always have unique notifs
            return TYPE_FAILED + ":" + batch.getBatchId();
        } else if (Downloads.Impl.isStatusCancelled(status) && shouldShowCompletedItem(visibility)) {
            // Cancelled downloads always have unique notifs
            return TYPE_CANCELLED + ":" + batch.getBatchId();
        } else if (Downloads.Impl.isStatusSuccess(status) && shouldShowCompletedItem(visibility)) {
            // Complete downloads always have unique notifs
            return TYPE_SUCCESS + ":" + batch.getBatchId();
        } else {
            return null;
        }
    }

    private boolean shouldShowActiveItem(int visibility) {
        return visibility == NotificationVisibility.ONLY_WHEN_ACTIVE
                || visibility == NotificationVisibility.ACTIVE_OR_COMPLETE;
    }

    private boolean shouldShowCompletedItem(int visibility) {
        return visibility == NotificationVisibility.ONLY_WHEN_COMPLETE
                || visibility == NotificationVisibility.ACTIVE_OR_COMPLETE;
    }

    private void addBatchToCluster(String tag, Map<String, List<DownloadBatch>> cluster, DownloadBatch batch) {
        if (tag == null) {
            return;
        }

        List<DownloadBatch> batches;

        if (cluster.containsKey(tag)) {
            batches = cluster.get(tag);
        } else {
            batches = new ArrayList<>();
            cluster.put(tag, batches); // TODO not sure if this is right compared to ArrayListMultiMap
        }

        batches.add(batch);
    }

    private void showNotificationPerCluster(Map<String, List<DownloadBatch>> clusters) {
        for (String notificationId : clusters.keySet()) {
            int type = getNotificationTagType(notificationId);
            List<DownloadBatch> cluster = clusters.get(notificationId);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
            useTimeWhenClusterFirstShownToAvoidShuffling(notificationId, builder);
            buildIcon(type, builder);
            buildActionIntents(notificationId, type, cluster, builder);

            Notification notification = buildTitlesAndDescription(type, cluster, builder);
            mNotifManager.notify(notificationId.hashCode(), notification);
        }
    }

    /**
     * Return the cluster type of the given as created by
     * {@link #buildNotificationTag(DownloadBatch)}.
     */
    private static int getNotificationTagType(String tag) {
        return Integer.parseInt(tag.substring(0, tag.indexOf(':')));
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

    private void buildActionIntents(String tag, int type, List<DownloadBatch> cluster, NotificationCompat.Builder builder) {
        if (type == TYPE_ACTIVE || type == TYPE_WAITING) {
            // build a synthetic uri for intent identification purposes
            Uri uri = new Uri.Builder().scheme("active-dl").appendPath(tag).build();
            Intent clickIntent = new Intent(Constants.ACTION_LIST, uri, mContext, DownloadReceiver.class);
            clickIntent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS, getDownloadIds(cluster));
            builder.setContentIntent(PendingIntent.getBroadcast(mContext, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT));
            builder.setOngoing(true);

            DownloadBatch batch = cluster.iterator().next();
            Intent cancelIntent = new Intent(Constants.ACTION_CANCEL, null, mContext, DownloadReceiver.class);
            cancelIntent.putExtra(DownloadReceiver.EXTRA_BATCH_ID, batch.getBatchId());
            PendingIntent pendingCancelIntent = PendingIntent.getBroadcast(mContext, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.dl__ic_action_cancel, "Cancel", pendingCancelIntent);

        } else if (type == TYPE_SUCCESS) {
            DownloadBatch batch = cluster.iterator().next();
            // TODO: Decide how we handle notification clicks
            DownloadInfo downloadInfo = batch.getDownloads().get(0);
            Uri uri = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, downloadInfo.mId);
            builder.setAutoCancel(true);

            final String action;
            if (Downloads.Impl.isStatusError(batch.getStatus())) {
                action = Constants.ACTION_LIST;
            } else {
                action = Constants.ACTION_OPEN;
            }

            final Intent intent = new Intent(action, uri, mContext, DownloadReceiver.class);
            intent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS, getDownloadIds(cluster));
            intent.putExtra(DownloadReceiver.EXTRA_BATCH_ID, batch.getBatchId());
            builder.setContentIntent(PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

            final Intent hideIntent = new Intent(Constants.ACTION_HIDE, uri, mContext, DownloadReceiver.class);
            hideIntent.putExtra(DownloadReceiver.EXTRA_BATCH_ID, batch.getBatchId());
            builder.setDeleteIntent(PendingIntent.getBroadcast(mContext, 0, hideIntent, 0));
        }
    }

    private Notification buildTitlesAndDescription(
            int type,
            List<DownloadBatch> cluster,
            NotificationCompat.Builder builder) {

        String remainingText = null;
        String percentText = null;
        if (type == TYPE_ACTIVE) {
            long currentBytes = 0;
            long totalBytes = 0;
            long totalBytesPerSecond = 0;
            synchronized (mDownloadSpeed) {
                for (DownloadBatch batch : cluster) {
                    for (DownloadInfo info : batch.getDownloads()) {
                        if (info.hasTotalBytes()) {
                            currentBytes += info.mCurrentBytes;
                            totalBytes += info.mTotalBytes;
                            Long bytesPerSecond = mDownloadSpeed.get(info.mId);
                            if (bytesPerSecond != null) {
                                totalBytesPerSecond += bytesPerSecond;
                            }
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

        Set<DownloadBatch> currentBatches = new HashSet<>();
        for (DownloadBatch batch : cluster) {
            currentBatches.add(batch);
        }

        if (currentBatches.size() == 1) {
            DownloadBatch batch = currentBatches.iterator().next();
            return buildSingleNotification(type, builder, batch, remainingText, percentText);

        } else {
            return buildStackedNotification(type, builder, currentBatches, remainingText, percentText);
        }

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

    private Notification buildSingleNotification(int type, NotificationCompat.Builder builder, DownloadBatch batch, String remainingText, String percentText) {

        NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
        String imageUrl = batch.getInfo().getBigPictureUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            Bitmap bitmap = imageRetriever.retrieveImage(imageUrl);
            style.bigPicture(bitmap);
        }
        CharSequence title = getDownloadTitle(batch.getInfo());
        builder.setContentTitle(title);
        style.setBigContentTitle(title);

        if (type == TYPE_ACTIVE) {
            String description = batch.getInfo().getDescription();
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

    private Notification buildStackedNotification(int type, NotificationCompat.Builder builder, Set<DownloadBatch> currentBatches, String remainingText, String percentText) {
        final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle(builder);

        for (DownloadBatch batch : currentBatches) {
            inboxStyle.addLine(getDownloadTitle(batch.getInfo()));
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

    private void removeStaleTagsThatWereNotRenewed(Map<String, List<DownloadBatch>> clustered) {
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

    private long[] getDownloadIds(List<DownloadBatch> batches) {
        List<Long> ids = new ArrayList<>();
        for (DownloadBatch batch : batches) {
            for (DownloadInfo downloadInfo : batch.getDownloads()) {
                ids.add(downloadInfo.mId);
            }
        }

        long[] idArray = new long[ids.size()];

        for (int i = 0, idsSize = ids.size(); i < idsSize; i++) {
            idArray[i] = ids.get(i);
        }
        return idArray;
    }

    public void dumpSpeeds() {
        Log.e("dump at speed");
    }

}
