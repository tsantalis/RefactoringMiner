package com.novoda.downloadmanager.lib;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NotificationVisibility {
    /**
     * This download is visible but only shows in the notifications
     * while it's in progress.
     */
    public static final int ONLY_WHEN_ACTIVE = 0;
    /**
     * This download is visible and shows in the notifications while
     * in progress and after completion.
     */
    public static final int ACTIVE_OR_COMPLETE = 1;
    /**
     * This download doesn't show in the UI or in the notifications.
     */
    public static final int HIDDEN = 2;
    /**
     * This download shows in the notifications after completion ONLY.
     * It is usuable only with
     * {@link DownloadManager#addCompletedDownload(String, String,
     * boolean, String, String, long, boolean)}.
     */
    public static final int ONLY_WHEN_COMPLETE = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ONLY_WHEN_ACTIVE, ACTIVE_OR_COMPLETE, HIDDEN, ONLY_WHEN_COMPLETE})
    public @interface Value {
    }
}
