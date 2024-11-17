package com.novoda.downloadmanager.lib;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains all the information necessary to request a new download. The URI is the
 * only required parameter.
 * <p/>
 * Note that the default download destination is a shared volume where the system might delete
 * your file if it needs to reclaim space for system use. If this is a problem, use a location
 * on external storage (see {@link #setDestinationUri(android.net.Uri)}.
 */
public class Request {
    /**
     * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
     * {@link android.net.ConnectivityManager#TYPE_MOBILE}.
     */
    public static final int NETWORK_MOBILE = 1 << 0;

    /**
     * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
     * {@link android.net.ConnectivityManager#TYPE_WIFI}.
     */
    public static final int NETWORK_WIFI = 1 << 1;

    /**
     * Bit flag for {@link #setAllowedNetworkTypes} corresponding to
     * {@link android.net.ConnectivityManager#TYPE_BLUETOOTH}.
     */
    public static final int NETWORK_BLUETOOTH = 1 << 2;

    private Uri mUri;
    private Uri mDestinationUri;
    private List<Pair<String, String>> mRequestHeaders = new ArrayList<Pair<String, String>>();
    private CharSequence mTitle;
    private CharSequence mDescription;
    private String mMimeType;
    private int mAllowedNetworkTypes = ~0; // default to all network types allowed
    private boolean mRoamingAllowed = true;
    private boolean mMeteredAllowed = true;
    private boolean mIsVisibleInDownloadsUi = true;
    private boolean mScannable = false;
    private String extraField;
    private String bigPictureUrl;
    private long batchId = -1L;
    /**
     * if a file is designated as a MediaScanner scannable file, the following value is
     * stored in the database column {@link com.novoda.downloadmanager.lib.Downloads.Impl#COLUMN_MEDIA_SCANNED}.
     */
    static final int SCANNABLE_VALUE_YES = 0;
    // value of 1 is stored in the above column by DownloadProvider after it is scanned by
    // MediaScanner
    /**
     * if a file is designated as a file that should not be scanned by MediaScanner,
     * the following value is stored in the database column
     * {@link com.novoda.downloadmanager.lib.Downloads.Impl#COLUMN_MEDIA_SCANNED}.
     */
    static final int SCANNABLE_VALUE_NO = 2;

    /**
     * can take any of the following values: {@link NotificationVisibility#HIDDEN}
     * {@link NotificationVisibility#ACTIVE_OR_COMPLETE}, {@link NotificationVisibility#ONLY_WHEN_ACTIVE},
     * {@link NotificationVisibility#ONLY_WHEN_COMPLETE}
     */
    private int mNotificationVisibility = NotificationVisibility.ONLY_WHEN_ACTIVE;

    /**
     * @param uri the HTTP URI to download.
     */
    public Request(Uri uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalArgumentException("Can only download HTTP/HTTPS URIs: " + uri);
        }
        mUri = uri;
    }

    Request(String uriString) {
        mUri = Uri.parse(uriString);
    }

    /**
     * Set the local destination for the downloaded file. Must be a file URI to a path on
     * external storage, and the calling application must have the WRITE_EXTERNAL_STORAGE
     * permission.
     * <p/>
     * The downloaded file is not scanned by MediaScanner.
     * But it can be made scannable by calling {@link #allowScanningByMediaScanner()}.
     * <p/>
     * By default, downloads are saved to a generated filename in the shared download cache and
     * may be deleted by the system at any time to reclaim space.
     *
     * @return this object
     */
    public Request setDestinationUri(Uri uri) {
        mDestinationUri = uri;
        return this;
    }

    /**
     * Set the local destination for the downloaded file to a path within
     * the application's external files directory (as returned by
     * {@link android.content.Context#getExternalFilesDir(String)}.
     * <p/>
     * The downloaded file is not scanned by MediaScanner. But it can be
     * made scannable by calling {@link #allowScanningByMediaScanner()}.
     *
     * @param dirType the directory type to pass to
     *                {@link Context#getExternalFilesDir(String)}
     * @param subPath the path within the external directory, including the
     *                destination filename
     * @return this object
     * @throws IllegalStateException If the external storage directory
     *                               cannot be found or created.
     */
    public Request setDestinationInExternalFilesDir(String dirType, String subPath) {
        final File file = GlobalState.getContext().getExternalFilesDir(dirType);
        if (file == null) {
            throw new IllegalStateException("Failed to get external storage files directory");
        } else if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalStateException(file.getAbsolutePath() + " already exists and is not a directory");
            }
        } else {
            if (!file.mkdirs()) {
                throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
            }
        }
        setDestinationFromBase(file, subPath);
        return this;
    }

    /**
     * Set the local destination for the downloaded file to a path within
     * the application's internal files directory (as returned by
     * {@link new File(context.getFilesDir(), dirType)}.
     * <p/>
     * The downloaded file is not scanned by MediaScanner. But it can be
     * made scannable by calling {@link #allowScanningByMediaScanner()}.
     *
     * @param dirType the directory type to pass to
     *                {@link Context#getExternalFilesDir(String)}
     * @param subPath the path within the external directory, including the
     *                destination filename
     * @return this object
     * @throws IllegalStateException If the external storage directory
     *                               cannot be found or created.
     */
    public Request setDestinationInInternalFilesDir(String dirType, String subPath) {
        final File file = new File(GlobalState.getContext().getFilesDir(), dirType);
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalStateException(file.getAbsolutePath() + " already exists and is not a directory");
            }
        } else {
            if (!file.mkdirs()) {
                throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
            }
        }
        setDestinationFromBase(file, subPath);
        return this;
    }

    /**
     * Set the local destination for the downloaded file to a path within
     * the public external storage directory (as returned by
     * {@link android.os.Environment#getExternalStoragePublicDirectory(String)}).
     * <p/>
     * The downloaded file is not scanned by MediaScanner. But it can be
     * made scannable by calling {@link #allowScanningByMediaScanner()}.
     *
     * @param dirType the directory type to pass to {@link android.os.Environment#getExternalStoragePublicDirectory(String)}
     * @param subPath the path within the external directory, including the
     *                destination filename
     * @return this object
     * @throws IllegalStateException If the external storage directory
     *                               cannot be found or created.
     */
    public Request setDestinationInExternalPublicDir(String dirType, String subPath) {
        File file = Environment.getExternalStoragePublicDirectory(dirType);
        if (file == null) {
            throw new IllegalStateException("Failed to get external storage public directory");
        } else if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalStateException(file.getAbsolutePath() + " already exists and is not a directory");
            }
        } else {
            if (!file.mkdirs()) {
                throw new IllegalStateException("Unable to create directory: " + file.getAbsolutePath());
            }
        }
        setDestinationFromBase(file, subPath);
        return this;
    }

    private void setDestinationFromBase(File base, String subPath) {
        if (subPath == null) {
            throw new NullPointerException("subPath cannot be null");
        }
        mDestinationUri = Uri.withAppendedPath(Uri.fromFile(base), subPath);
    }

    /**
     * If the file to be downloaded is to be scanned by MediaScanner, this method
     * should be called before {@link DownloadManager#enqueue(Request)} is called.
     */
    public void allowScanningByMediaScanner() {
        mScannable = true;
    }

    /**
     * Add an HTTP header to be included with the download request.  The header will be added to
     * the end of the list.
     *
     * @param header HTTP header name
     * @param value  header value
     * @return this object
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">HTTP/1.1
     * Message Headers</a>
     */
    public Request addRequestHeader(String header, String value) {
        if (header == null) {
            throw new NullPointerException("header cannot be null");
        }
        if (header.contains(":")) {
            throw new IllegalArgumentException("header may not contain ':'");
        }
        if (value == null) {
            value = "";
        }
        mRequestHeaders.add(Pair.create(header, value));
        return this;
    }

    /**
     * Set the title of this download, to be displayed in notifications (if enabled).  If no
     * title is given, a default one will be assigned based on the download filename, once the
     * download starts.
     *
     * @return this object
     */
    public Request setTitle(CharSequence title) {
        mTitle = title;
        return this;
    }

    /**
     * Set a description of this download, to be displayed in notifications (if enabled)
     *
     * @return this object
     */
    public Request setDescription(CharSequence description) {
        mDescription = description;
        return this;
    }

    /**
     * Set a drawable url that will be used for the Big Picture Style
     *
     * @param bigPictureUrl the drawable resource id
     * @return this object
     */
    public Request setBigPictureUrl(String bigPictureUrl) {
        this.bigPictureUrl = bigPictureUrl;
        return this;
    }

    /**
     * Set the ID of the batch that this request belongs to
     *
     * @param batchId the batch id
     * @return this object
     */
    Request setBatchId(long batchId) {
        this.batchId = batchId;
        return this;
    }

    /**
     * Set the MIME content type of this download.  This will override the content type declared
     * in the server's response.
     *
     * @return this object
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7">HTTP/1.1
     * Media Types</a>
     */
    public Request setMimeType(String mimeType) {
        mMimeType = mimeType;
        return this;
    }

    /**
     * Control whether a system notification is posted by the download manager while this
     * download is running. If enabled, the download manager posts notifications about downloads
     * through the system {@link android.app.NotificationManager}. By default, a notification is
     * shown.
     * <p/>
     * If set to false, this requires the permission
     * android.permission.DOWNLOAD_WITHOUT_NOTIFICATION.
     *
     * @param show whether the download manager should show a notification for this download.
     * @return this object
     * @deprecated use {@link #setNotificationVisibility(int)}
     */
    @Deprecated
    public Request setShowRunningNotification(boolean show) {
        return (show) ? setNotificationVisibility(NotificationVisibility.ONLY_WHEN_ACTIVE) : setNotificationVisibility(NotificationVisibility.HIDDEN);
    }

    /**
     * Control whether a system notification is posted by the download manager while this
     * download is running or when it is completed.
     * If enabled, the download manager posts notifications about downloads
     * through the system {@link android.app.NotificationManager}.
     * By default, a notification is shown only when the download is in progress.
     * <p/>
     * It can take the following values: {@link NotificationVisibility#HIDDEN},
     * {@link NotificationVisibility#ONLY_WHEN_ACTIVE},
     * {@link NotificationVisibility#ONLY_WHEN_COMPLETE}.
     * <p/>
     * If set to {@link NotificationVisibility#HIDDEN}, this requires the permission
     * android.permission.DOWNLOAD_WITHOUT_NOTIFICATION.
     *
     * @param visibility the visibility setting value
     * @return this object
     */
    public Request setNotificationVisibility(int visibility) {
        mNotificationVisibility = visibility;
        return this;
    }

    /**
     * Restrict the types of networks over which this download may proceed.
     * By default, all network types are allowed. Consider using
     * {@link #setAllowedOverMetered(boolean)} instead, since it's more
     * flexible.
     *
     * @param flags any combination of the NETWORK_* bit flags.
     * @return this object
     */
    public Request setAllowedNetworkTypes(int flags) {
        mAllowedNetworkTypes = flags;
        return this;
    }

    /**
     * Set whether this download may proceed over a roaming connection.  By default, roaming is
     * allowed.
     *
     * @param allowed whether to allow a roaming connection to be used
     * @return this object
     */
    public Request setAllowedOverRoaming(boolean allowed) {
        mRoamingAllowed = allowed;
        return this;
    }

    /**
     * Set whether this download may proceed over a metered network
     * connection. By default, metered networks are allowed.
     *
     * @see android.net.ConnectivityManager#isActiveNetworkMetered()
     */
    public Request setAllowedOverMetered(boolean allow) {
        mMeteredAllowed = allow;
        return this;
    }

    /**
     * Set whether this download should be displayed in the system's Downloads UI. True by
     * default.
     *
     * @param isVisible whether to display this download in the Downloads UI
     * @return this object
     */
    public Request setVisibleInDownloadsUi(boolean isVisible) {
        mIsVisibleInDownloadsUi = isVisible;
        return this;
    }

    public void setExtra(String extra) {
        extraField = extra;
    }

    long getBatchId() {
        return batchId;
    }

    /**
     * @return ContentValues to be passed to DownloadProvider.insert()
     */
    ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        assert mUri != null;
        values.put(Downloads.Impl.COLUMN_URI, mUri.toString());

        if (mDestinationUri != null) {
            values.put(Downloads.Impl.COLUMN_DESTINATION, Downloads.Impl.DESTINATION_FILE_URI);
            values.put(Downloads.Impl.COLUMN_FILE_NAME_HINT, mDestinationUri.toString());
        } else {
            values.put(Downloads.Impl.COLUMN_DESTINATION,
                    Downloads.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE);
        }
        // is the file supposed to be media-scannable?
        values.put(Downloads.Impl.COLUMN_MEDIA_SCANNED, (mScannable) ? SCANNABLE_VALUE_YES : SCANNABLE_VALUE_NO);

        if (!mRequestHeaders.isEmpty()) {
            encodeHttpHeaders(values);
        }

        putIfNonNull(values, Downloads.Impl.COLUMN_TITLE, mTitle);
        putIfNonNull(values, Downloads.Impl.COLUMN_DESCRIPTION, mDescription);
        putIfNonNull(values, Downloads.Impl.COLUMN_MIME_TYPE, mMimeType);

        values.put(Downloads.Impl.COLUMN_VISIBILITY, mNotificationVisibility);
        values.put(Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES, mAllowedNetworkTypes);
        values.put(Downloads.Impl.COLUMN_ALLOW_ROAMING, mRoamingAllowed);
        values.put(Downloads.Impl.COLUMN_ALLOW_METERED, mMeteredAllowed);
        values.put(Downloads.Impl.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI, mIsVisibleInDownloadsUi);
        values.put(Downloads.Impl.COLUMN_NOTIFICATION_EXTRAS, extraField);
        values.put(Downloads.Impl.COLUMN_BIG_PICTURE, bigPictureUrl);
        values.put(Downloads.Impl.COLUMN_BATCH_ID, batchId);

        return values;
    }

    private void encodeHttpHeaders(ContentValues values) {
        int index = 0;
        for (Pair<String, String> header : mRequestHeaders) {
            String headerString = header.first + ": " + header.second;
            values.put(Downloads.Impl.RequestHeaders.INSERT_KEY_PREFIX + index, headerString);
            index++;
        }
    }

    private void putIfNonNull(ContentValues contentValues, String key, Object value) {
        if (value != null) {
            contentValues.put(key, value.toString());
        }
    }

    RequestBatch asBatch() {
        RequestBatch requestBatch = new RequestBatch.Builder()
                .withTitle(mTitle.toString())
                .withDescription(mDescription.toString())
                .withBigPictureUrl(bigPictureUrl)
                .withVisibility(mNotificationVisibility)
                .build();
        requestBatch.addRequest(this);
        return requestBatch;
    }
}
