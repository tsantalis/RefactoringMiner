/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.novoda.downloadmanager.lib.logger.LLog;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Allows application to interact with the download manager.
 */
public final class DownloadProvider extends ContentProvider {

    /**
     * Added so we can use our own ContentProvider
     */
    public static final String AUTHORITY = Reflector.reflectAuthority();

    /**
     * Database filename
     */
    private static final String DB_NAME = "downloads.db";

    /**
     * MIME type for the entire download list
     */
    private static final String DOWNLOAD_LIST_TYPE = "vnd.android.cursor.dir/download";
    /**
     * MIME type for an individual download
     */
    private static final String DOWNLOAD_TYPE = "vnd.android.cursor.item/download";

    /**
     * MIME type for the entire batch list
     */
    private static final String BATCH_LIST_TYPE = "vnd.android.cursor.dir/batch";
    /**
     * MIME type for an individual batch
     */
    private static final String BATCH_TYPE = "vnd.android.cursor.item/batch";

    /**
     * MIME type for the list of download by batch
     */
    private static final String DOWNLOADS_BY_BATCH_TYPE = "vnd.android.cursor.dir/download_by_batch";

    /**
     * URI matcher used to recognize URIs sent by applications
     */
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    /**
     * URI matcher constant for the URI of all downloads belonging to the calling UID
     */
    private static final int MY_DOWNLOADS = 1;
    /**
     * URI matcher constant for the URI of an individual download belonging to the calling UID
     */
    private static final int MY_DOWNLOADS_ID = 2;
    /**
     * URI matcher constant for the URI of all downloads in the system
     */
    private static final int ALL_DOWNLOADS = 3;
    /**
     * URI matcher constant for the URI of an individual download
     */
    private static final int ALL_DOWNLOADS_ID = 4;
    /**
     * URI matcher constant for the URI of a download's request headers
     */
    private static final int REQUEST_HEADERS_URI = 5;
    /**
     * URI matcher constant for the public URI returned by
     * {@link DownloadManager#getUriForDownloadedFile(long)} if the given downloaded file
     * is publicly accessible.
     */
    private static final int PUBLIC_DOWNLOAD_ID = 6;
    /**
     * URI matcher constant for the URI of a download's request headers
     */
    private static final int BATCHES = 7;
    /**
     * URI matcher constant for the URI of a download's request headers
     */
    private static final int BATCHES_ID = 8;
    /**
     * URI matcher constant for the URI of downloads with their batch data
     */
    private static final int DOWNLOADS_BY_BATCH = 9;

    private static final String[] APP_READABLE_COLUMNS_ARRAY = new String[]{
            DownloadContract.Downloads._ID,
            DownloadContract.Downloads.COLUMN_APP_DATA,
            DownloadContract.Downloads.COLUMN_DATA,
            DownloadContract.Downloads.COLUMN_MIME_TYPE,
            DownloadContract.Downloads.COLUMN_DESTINATION,
            DownloadContract.Downloads.COLUMN_CONTROL,
            DownloadContract.Downloads.COLUMN_STATUS,
            DownloadContract.Downloads.COLUMN_LAST_MODIFICATION,
            DownloadContract.Downloads.COLUMN_NOTIFICATION_CLASS,
            DownloadContract.Downloads.COLUMN_TOTAL_BYTES,
            DownloadContract.Downloads.COLUMN_CURRENT_BYTES,
            DownloadContract.Downloads.COLUMN_URI,
            DownloadContract.Downloads.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI,
            DownloadContract.Downloads.COLUMN_FILE_NAME_HINT,
            DownloadContract.Downloads.COLUMN_MEDIAPROVIDER_URI,
            DownloadContract.Downloads.COLUMN_DELETED,
            DownloadContract.Downloads.COLUMN_NOTIFICATION_EXTRAS,
            DownloadContract.Downloads.COLUMN_BATCH_ID,
            DownloadContract.Downloads.COLUMN_ALWAYS_RESUME,
            DownloadContract.Downloads.COLUMN_ALLOW_TAR_UPDATES,
            DownloadContract.Batches._ID,
            DownloadContract.Batches.COLUMN_STATUS,
            DownloadContract.Batches.COLUMN_TITLE,
            DownloadContract.Batches.COLUMN_DESCRIPTION,
            DownloadContract.Batches.COLUMN_BIG_PICTURE,
            DownloadContract.Batches.COLUMN_VISIBILITY,
            DownloadContract.BatchesWithSizes.COLUMN_TOTAL_BYTES,
            DownloadContract.BatchesWithSizes.COLUMN_CURRENT_BYTES,
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE,
    };

    private static final Set<String> APP_READABLE_COLUMNS_SET;

    private static final Map<String, String> COLUMNS_MAP;

    private static final List<String> DOWNLOAD_MANAGER_COLUMNS_LIST = Arrays.asList(DownloadManager.UNDERLYING_COLUMNS);

    private final DownloadsUriProvider downloadsUriProvider;

    /**
     * Different base URIs that could be used to access an individual download
     */
    private final Uri[] baseUris;


    /**
     * The database that lies underneath this content provider
     */
    private SQLiteOpenHelper openHelper;

    /**
     * List of uids that can access the downloads
     */
    private int systemUid = -1;

    private int defcontaineruid = -1;
    private File downloadsDataDir;
    //    @VisibleForTesting
    SystemFacade systemFacade;

    static {
        URI_MATCHER.addURI(AUTHORITY, "my_downloads", MY_DOWNLOADS);
        URI_MATCHER.addURI(AUTHORITY, "my_downloads/#", MY_DOWNLOADS_ID);
        URI_MATCHER.addURI(AUTHORITY, "all_downloads", ALL_DOWNLOADS);
        URI_MATCHER.addURI(AUTHORITY, "all_downloads/#", ALL_DOWNLOADS_ID);
        URI_MATCHER.addURI(AUTHORITY, "batches", BATCHES);
        URI_MATCHER.addURI(AUTHORITY, "batches/#", BATCHES_ID);
        URI_MATCHER.addURI(AUTHORITY, "downloads_by_batch", DOWNLOADS_BY_BATCH);
        URI_MATCHER.addURI(AUTHORITY, "my_downloads/#/" + DownloadContract.RequestHeaders.URI_SEGMENT, REQUEST_HEADERS_URI);
        URI_MATCHER.addURI(AUTHORITY, "all_downloads/#/" + DownloadContract.RequestHeaders.URI_SEGMENT, REQUEST_HEADERS_URI);
        // temporary, for backwards compatibility
        URI_MATCHER.addURI(AUTHORITY, "download", MY_DOWNLOADS);
        URI_MATCHER.addURI(AUTHORITY, "download/#", MY_DOWNLOADS_ID);
        URI_MATCHER.addURI(AUTHORITY, "download/#/" + DownloadContract.RequestHeaders.URI_SEGMENT, REQUEST_HEADERS_URI);
        URI_MATCHER.addURI(AUTHORITY, DownloadsDestination.PUBLICLY_ACCESSIBLE_DOWNLOADS_URI_SEGMENT + "/#", PUBLIC_DOWNLOAD_ID);

        APP_READABLE_COLUMNS_SET = new HashSet<>();
        Collections.addAll(APP_READABLE_COLUMNS_SET, APP_READABLE_COLUMNS_ARRAY);

        COLUMNS_MAP = new HashMap<>();
        COLUMNS_MAP.put(OpenableColumns.DISPLAY_NAME, DownloadContract.Batches.COLUMN_TITLE + " AS " + OpenableColumns.DISPLAY_NAME);
        COLUMNS_MAP.put(OpenableColumns.SIZE, DownloadContract.Downloads.COLUMN_TOTAL_BYTES + " AS " + OpenableColumns.SIZE);
    }

    public DownloadProvider() {
        downloadsUriProvider = DownloadsUriProvider.getInstance();

        baseUris = new Uri[]{
                downloadsUriProvider.getContentUri(),
                downloadsUriProvider.getAllDownloadsUri(),
                downloadsUriProvider.getBatchesUri()
        };
    }

    /**
     * This class encapsulates a SQL where clause and its parameters.  It makes it possible for
     * shared methods (like {@link DownloadProvider#getWhereClause(Uri, String, String[], int)})
     * to return both pieces of information, and provides some utility logic to ease piece-by-piece
     * construction of selections.
     */
    private static class SqlSelection {
        public final StringBuilder whereClause = new StringBuilder();
        public final List<String> parameters = new ArrayList<>();

        public void appendClause(String newClause, final String... parameters) {
            if (newClause == null || newClause.isEmpty()) {
                return;
            }
            if (whereClause.length() != 0) {
                whereClause.append(" AND ");
            }
            whereClause.append("(");
            whereClause.append(newClause);
            whereClause.append(")");
            if (parameters != null) {
                for (String parameter : parameters) {
                    this.parameters.add(parameter);
                }
            }
        }

        public String getSelection() {
            return whereClause.toString();
        }

        public String[] getParameters() {
            String[] array = new String[parameters.size()];
            return parameters.toArray(array);
        }
    }

    /**
     * Initializes the content provider when it is created.
     */
    @Override
    public boolean onCreate() {
        if (systemFacade == null) {
            systemFacade = new RealSystemFacade(getContext(), new Clock());
        }

        Context context = getContext();
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getApplicationContext().getPackageName();
        DatabaseFilenameProvider databaseFilenameProvider = new DatabaseFilenameProvider(packageManager, packageName, DB_NAME);
        String databaseFilename = databaseFilenameProvider.getDatabaseFilename();
        openHelper = new DatabaseHelper(context, databaseFilename);
        // Initialize the system uid
        systemUid = Process.SYSTEM_UID;
        // Initialize the default container uid. Package name hardcoded
        // for now.
        ApplicationInfo appInfo = null;
        try {
            appInfo = getContext().getPackageManager().
                    getApplicationInfo("com.android.defcontainer", 0);
        } catch (NameNotFoundException e) {
            LLog.wtf(e, "Could not get ApplicationInfo for com.android.defconatiner");
        }
        if (appInfo != null) {
            defcontaineruid = appInfo.uid;
        }
        // start the DownloadService class. don't wait for the 1st download to be issued.
        // saves us by getting some initialization code in DownloadService out of the way.
        context.startService(new Intent(context, DownloadService.class));
//        downloadsDataDir = StorageManager.getDownloadDataDirectory(getContext());
        downloadsDataDir = context.getCacheDir();
//        try {
//            android.os.SELinux.restorecon(downloadsDataDir.getCanonicalPath());
//        } catch (IOException e) {
//            LLog.wtf("Could not get canonical path for download directory", e);
//        }
        return true;
    }

    /**
     * Returns the content-provider-style MIME types of the various
     * types accessible through this content provider.
     */
    @NonNull
    @Override
    public String getType(@NonNull Uri uri) {
        int match = URI_MATCHER.match(uri);
        switch (match) {
            case MY_DOWNLOADS:
            case ALL_DOWNLOADS:
                return DOWNLOAD_LIST_TYPE;

            case MY_DOWNLOADS_ID:
            case ALL_DOWNLOADS_ID:
            case PUBLIC_DOWNLOAD_ID: {
                // return the mimetype of this id from the database
                final String id = getDownloadIdFromUri(uri);
                final SQLiteDatabase db = openHelper.getReadableDatabase();
                final String mimeType = DatabaseUtils.stringForQuery(
                        db,
                        "SELECT " + DownloadContract.Downloads.COLUMN_MIME_TYPE + " FROM " + DownloadContract.Downloads.DOWNLOADS_TABLE_NAME +
                                " WHERE " + DownloadContract.Downloads._ID + " = ?",
                        new String[]{id});
                if (TextUtils.isEmpty(mimeType)) {
                    return DOWNLOAD_TYPE;
                } else {
                    return mimeType;
                }
            }

            case BATCHES:
                return BATCH_LIST_TYPE;

            case BATCHES_ID:
                return BATCH_TYPE;

            case DOWNLOADS_BY_BATCH:
                return DOWNLOADS_BY_BATCH_TYPE;

            default:
                LLog.v("calling getType on an unknown URI: " + uri);
                throw new IllegalArgumentException("Unknown URI: " + uri);

        }
    }

    /**
     * Inserts a row in the database
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabase db = openHelper.getWritableDatabase();

        // note we disallow inserting into ALL_DOWNLOADS
        int match = URI_MATCHER.match(uri);
        if (match == MY_DOWNLOADS) {
            return insertDownload(uri, values, db, match);
        }
        if (match == BATCHES) {
            long rowId = db.insert(DownloadContract.Batches.BATCHES_TABLE_NAME, null, values);
            notifyBatchesStatusChanged();
            return ContentUris.withAppendedId(downloadsUriProvider.getBatchesUri(), rowId);
        }
        LLog.d("calling insert on an unknown/invalid URI: " + uri);
        throw new IllegalArgumentException("Unknown/Invalid URI " + uri);

    }

    @Nullable
    private Uri insertDownload(Uri uri, ContentValues values, SQLiteDatabase db, int match) {
        // copy some of the input values as it
        ContentValues filteredValues = new ContentValues();
        copyString(DownloadContract.Downloads.COLUMN_URI, values, filteredValues);
        copyString(DownloadContract.Downloads.COLUMN_APP_DATA, values, filteredValues);
        copyBoolean(DownloadContract.Downloads.COLUMN_NO_INTEGRITY, values, filteredValues);
        copyString(DownloadContract.Downloads.COLUMN_FILE_NAME_HINT, values, filteredValues);
        copyString(DownloadContract.Downloads.COLUMN_MIME_TYPE, values, filteredValues);

        // validate the destination column
        Integer dest = values.getAsInteger(DownloadContract.Downloads.COLUMN_DESTINATION);
        if (dest != null) {
            if (getContext().checkCallingPermission(DownloadsPermission.PERMISSION_ACCESS_ADVANCED)
                    != PackageManager.PERMISSION_GRANTED
                    && (dest == DownloadsDestination.DESTINATION_CACHE_PARTITION
                    || dest == DownloadsDestination.DESTINATION_CACHE_PARTITION_NOROAMING
                    || dest == DownloadsDestination.DESTINATION_SYSTEMCACHE_PARTITION)) {
                throw new SecurityException(
                        "setting destination to : " + dest +
                                " not allowed, unless PERMISSION_ACCESS_ADVANCED is granted");
            }
            // for public API behavior, if an app has CACHE_NON_PURGEABLE permission, automatically
            // switch to non-purgeable download
            boolean hasNonPurgeablePermission =
                    getContext().checkCallingPermission(DownloadsPermission.PERMISSION_CACHE_NON_PURGEABLE) == PackageManager.PERMISSION_GRANTED;
            if (dest == DownloadsDestination.DESTINATION_CACHE_PARTITION_PURGEABLE && hasNonPurgeablePermission) {
                dest = DownloadsDestination.DESTINATION_CACHE_PARTITION;
            }
            if (dest == DownloadsDestination.DESTINATION_FILE_URI) {
                getContext().enforcePermission(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Binder.getCallingPid(), Binder.getCallingUid(),
                        "need WRITE_EXTERNAL_STORAGE permission to use DESTINATION_FILE_URI");
                checkFileUriDestination(values);
            } else if (dest == DownloadsDestination.DESTINATION_SYSTEMCACHE_PARTITION) {
                getContext().enforcePermission(
                        "android.permission.ACCESS_CACHE_FILESYSTEM", Binder.getCallingPid(), Binder.getCallingUid(),
                        "need ACCESS_CACHE_FILESYSTEM permission to use system cache");
            }
            filteredValues.put(DownloadContract.Downloads.COLUMN_DESTINATION, dest);
        }

        // copy the control column as is
        copyInteger(DownloadContract.Downloads.COLUMN_CONTROL, values, filteredValues);

        /*
         * requests coming from
         * DownloadManager.addCompletedDownload(String, String, String,
         * boolean, String, String, long) need special treatment
         */
        if (values.getAsInteger(DownloadContract.Downloads.COLUMN_DESTINATION) == DownloadsDestination.DESTINATION_NON_DOWNLOADMANAGER_DOWNLOAD) {
            // these requests always are marked as 'completed'
            filteredValues.put(DownloadContract.Downloads.COLUMN_STATUS, DownloadStatus.SUCCESS);
            filteredValues.put(DownloadContract.Downloads.COLUMN_TOTAL_BYTES, values.getAsLong(DownloadContract.Downloads.COLUMN_TOTAL_BYTES));
            filteredValues.put(DownloadContract.Downloads.COLUMN_CURRENT_BYTES, 0);
            copyInteger(DownloadContract.Downloads.COLUMN_MEDIA_SCANNED, values, filteredValues);
            copyString(DownloadContract.Downloads.COLUMN_DATA, values, filteredValues);
        } else {
            filteredValues.put(DownloadContract.Downloads.COLUMN_STATUS, DownloadStatus.PENDING);
            filteredValues.put(DownloadContract.Downloads.COLUMN_TOTAL_BYTES, -1);
            filteredValues.put(DownloadContract.Downloads.COLUMN_CURRENT_BYTES, 0);
        }

        // set lastupdate to current time
        long lastMod = systemFacade.currentTimeMillis();
        filteredValues.put(DownloadContract.Downloads.COLUMN_LAST_MODIFICATION, lastMod);

        // use packagename of the caller to set the notification columns
        String clazz = values.getAsString(DownloadContract.Downloads.COLUMN_NOTIFICATION_CLASS);
        if (clazz != null) {
            int uid = Binder.getCallingUid();
            try {
                if ((uid == 0) || systemFacade.userOwnsPackage(uid, getContext().getPackageName())) {
                    filteredValues.put(DownloadContract.Downloads.COLUMN_NOTIFICATION_CLASS, clazz);
                }
            } catch (NameNotFoundException ex) {
                /* ignored for now */
            }
        }

        // copy some more columns as is
        copyString(DownloadContract.Downloads.COLUMN_NOTIFICATION_EXTRAS, values, filteredValues);
        copyString(DownloadContract.Downloads.COLUMN_EXTRA_DATA, values, filteredValues);
        copyString(DownloadContract.Downloads.COLUMN_COOKIE_DATA, values, filteredValues);
        copyString(DownloadContract.Downloads.COLUMN_USER_AGENT, values, filteredValues);
        copyString(DownloadContract.Downloads.COLUMN_REFERER, values, filteredValues);

        // UID, PID columns
        if (getContext().checkCallingPermission(DownloadsPermission.PERMISSION_ACCESS_ADVANCED) == PackageManager.PERMISSION_GRANTED) {
            copyInteger(DownloadContract.Downloads.COLUMN_OTHER_UID, values, filteredValues);
        }
        filteredValues.put(Constants.UID, Binder.getCallingUid());
        if (Binder.getCallingUid() == 0) {
            copyInteger(Constants.UID, values, filteredValues);
        }

        // is_visible_in_downloads_ui column
        if (values.containsKey(DownloadContract.Downloads.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI)) {
            copyBoolean(DownloadContract.Downloads.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI, values, filteredValues);
        } else {
            // by default, make external downloads visible in the UI
            boolean isExternal = (dest == null || dest == DownloadsDestination.DESTINATION_EXTERNAL);
            filteredValues.put(DownloadContract.Downloads.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI, isExternal);
        }

        // public api requests and networktypes/roaming columns
        copyInteger(DownloadContract.Downloads.COLUMN_ALLOWED_NETWORK_TYPES, values, filteredValues);
        copyBoolean(DownloadContract.Downloads.COLUMN_ALLOW_ROAMING, values, filteredValues);
        copyBoolean(DownloadContract.Downloads.COLUMN_ALLOW_METERED, values, filteredValues);
        copyBoolean(DownloadContract.Downloads.COLUMN_ALWAYS_RESUME, values, filteredValues);
        copyBoolean(DownloadContract.Downloads.COLUMN_ALLOW_TAR_UPDATES, values, filteredValues);

        copyInteger(DownloadContract.Downloads.COLUMN_BATCH_ID, values, filteredValues);

        LLog.v("initiating download with UID " + filteredValues.getAsInteger(Constants.UID));
        if (filteredValues.containsKey(DownloadContract.Downloads.COLUMN_OTHER_UID)) {
            LLog.v("other UID " + filteredValues.getAsInteger(DownloadContract.Downloads.COLUMN_OTHER_UID));
        }

        long rowID = db.insert(DownloadContract.Downloads.DOWNLOADS_TABLE_NAME, null, filteredValues);
        if (rowID == -1) {
            LLog.d("couldn't insert into downloads database");
            return null;
        }

        insertRequestHeaders(db, rowID, values);
        /*
         * requests coming from
         * DownloadManager.addCompletedDownload(String, String, String,
         * boolean, String, String, long) need special treatment
         */
        Context context = getContext();
        context.startService(new Intent(context, DownloadService.class));
        notifyContentChanged(uri, match);
        notifyDownloadStatusChanged();
        return ContentUris.withAppendedId(downloadsUriProvider.getContentUri(), rowID);
    }

    private void notifyDownloadStatusChanged() {
        getContext().getContentResolver().notifyChange(downloadsUriProvider.getDownloadsWithoutProgressUri(), null);
    }

    private void notifyBatchesStatusChanged() {
        getContext().getContentResolver().notifyChange(downloadsUriProvider.getBatchesWithoutProgressUri(), null);
    }

    /**
     * Check that the file URI provided for DESTINATION_FILE_URI is valid.
     */
    private void checkFileUriDestination(ContentValues values) {
        String fileUri = values.getAsString(DownloadContract.Downloads.COLUMN_FILE_NAME_HINT);
        if (fileUri == null) {
            throw new IllegalArgumentException(
                    "DESTINATION_FILE_URI must include a file URI under COLUMN_FILE_NAME_HINT");
        }
        Uri uri = Uri.parse(fileUri);
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals("file")) {
            throw new IllegalArgumentException("Not a file URI: " + uri);
        }
        final String path = uri.getPath();
        if (path == null) {
            throw new IllegalArgumentException("Invalid file URI: " + uri);
        }
    }

    /**
     * Starts a database query
     */
    @NonNull
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {

        Helpers.validateSelection(selection, APP_READABLE_COLUMNS_SET);

        SQLiteDatabase db = openHelper.getReadableDatabase();

        int match = URI_MATCHER.match(uri);
        switch (match) {
            case ALL_DOWNLOADS:
            case ALL_DOWNLOADS_ID:
            case MY_DOWNLOADS:
            case MY_DOWNLOADS_ID:
                return queryDownloads(uri, projection, selection, selectionArgs, sort, db, match);
            case BATCHES:
            case BATCHES_ID:
                SqlSelection batchSelection = getWhereClause(uri, selection, selectionArgs, match);
                return db.query(
                        DownloadContract.BatchesWithSizes.VIEW_NAME_BATCHES_WITH_SIZES, projection, batchSelection.getSelection(),
                        batchSelection.getParameters(), null, null, sort);
            case DOWNLOADS_BY_BATCH:
                return db.query(DownloadContract.DownloadsByBatch.VIEW_NAME_DOWNLOADS_BY_BATCH, projection, selection, selectionArgs, null, null, sort);
            case REQUEST_HEADERS_URI:
                if (projection != null || selection != null || sort != null) {
                    throw new UnsupportedOperationException(
                            "Request header queries do not support "
                                    + "projections, selections or sorting");
                }
                return queryRequestHeaders(db, uri);
            default:
                LLog.v("querying unknown URI: " + uri);
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    private Cursor queryDownloads(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort, SQLiteDatabase db, int match) {
        SqlSelection fullSelection = getWhereClause(uri, selection, selectionArgs, match);

        if (shouldRestrictVisibility()) {
            if (projection == null) {
                projection = APP_READABLE_COLUMNS_ARRAY.clone();
            } else {
                // check the validity of the columns in projection
                for (int i = 0; i < projection.length; ++i) {
                    if (!APP_READABLE_COLUMNS_SET.contains(projection[i]) &&
                            !DOWNLOAD_MANAGER_COLUMNS_LIST.contains(projection[i])) {
                        throw new IllegalArgumentException(
                                "column " + projection[i] + " is not allowed in queries");
                    }
                }
            }

            for (int i = 0; i < projection.length; i++) {
                final String newColumn = COLUMNS_MAP.get(projection[i]);
                if (newColumn != null) {
                    projection[i] = newColumn;
                }
            }
        }

        if (GlobalState.hasVerboseLogging()) {
            logVerboseQueryInfo(projection, selection, selectionArgs, sort, db);
        }

        Cursor ret = db.query(
                DownloadContract.Downloads.DOWNLOADS_TABLE_NAME, projection, fullSelection.getSelection(),
                fullSelection.getParameters(), null, null, sort);

        if (ret == null) {
            LLog.v("query failed in downloads database");
        } else {
            ret.setNotificationUri(getContext().getContentResolver(), uri);
            LLog.v("created cursor " + ret + " on behalf of " + Binder.getCallingPid());
        }
        return ret;
    }

    private void logVerboseQueryInfo(String[] projection,
                                     final String selection,
                                     final String[] selectionArgs,
                                     final String sort,
                                     SQLiteDatabase db) {
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        sb.append("starting query, database is ");
        if (db != null) {
            sb.append("not ");
        }
        sb.append("null; ");
        if (projection == null) {
            sb.append("projection is null; ");
        } else if (projection.length == 0) {
            sb.append("projection is empty; ");
        } else {
            for (int i = 0; i < projection.length; ++i) {
                sb.append("projection[");
                sb.append(i);
                sb.append("] is ");
                sb.append(projection[i]);
                sb.append("; ");
            }
        }
        sb.append("selection is ");
        sb.append(selection);
        sb.append("; ");
        if (selectionArgs == null) {
            sb.append("selectionArgs is null; ");
        } else if (selectionArgs.length == 0) {
            sb.append("selectionArgs is empty; ");
        } else {
            for (int i = 0; i < selectionArgs.length; ++i) {
                sb.append("selectionArgs[");
                sb.append(i);
                sb.append("] is ");
                sb.append(selectionArgs[i]);
                sb.append("; ");
            }
        }
        sb.append("sort is ");
        sb.append(sort);
        sb.append(".");
        LLog.v(sb.toString());
    }

    private String getDownloadIdFromUri(final Uri uri) {
        return uri.getPathSegments().get(1);
    }

    /**
     * Insert request headers for a download into the DB.
     */
    private void insertRequestHeaders(SQLiteDatabase db, long downloadId, ContentValues values) {
        ContentValues rowValues = new ContentValues();
        rowValues.put(DownloadContract.RequestHeaders.COLUMN_DOWNLOAD_ID, downloadId);
        for (Map.Entry<String, Object> entry : values.valueSet()) {
            String key = entry.getKey();
            if (key.startsWith(DownloadContract.RequestHeaders.INSERT_KEY_PREFIX)) {
                String headerLine = entry.getValue().toString();
                if (!headerLine.contains(":")) {
                    throw new IllegalArgumentException("Invalid HTTP header line: " + headerLine);
                }
                String[] parts = headerLine.split(":", 2);
                rowValues.put(DownloadContract.RequestHeaders.COLUMN_HEADER, parts[0].trim());
                rowValues.put(DownloadContract.RequestHeaders.COLUMN_VALUE, parts[1].trim());
                db.insert(DownloadContract.RequestHeaders.HEADERS_DB_TABLE, null, rowValues);
            }
        }
    }

    /**
     * Handle a query for the custom request headers registered for a download.
     */
    private Cursor queryRequestHeaders(SQLiteDatabase db, Uri uri) {
        String where = DownloadContract.RequestHeaders.COLUMN_DOWNLOAD_ID + "="
                + getDownloadIdFromUri(uri);
        String[] projection = new String[]{DownloadContract.RequestHeaders.COLUMN_HEADER,
                DownloadContract.RequestHeaders.COLUMN_VALUE};
        return db.query(
                DownloadContract.RequestHeaders.HEADERS_DB_TABLE, projection, where,
                null, null, null, null);
    }

    /**
     * Delete request headers for downloads matching the given query.
     */
    private void deleteRequestHeaders(SQLiteDatabase db, String where, String[] whereArgs) {
        String[] projection = new String[]{DownloadContract.Downloads._ID};
        Cursor cursor = db.query(DownloadContract.Downloads.DOWNLOADS_TABLE_NAME, projection, where, whereArgs, null, null, null, null);
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String idWhere = DownloadContract.RequestHeaders.COLUMN_DOWNLOAD_ID + "=" + id;
                db.delete(DownloadContract.RequestHeaders.HEADERS_DB_TABLE, idWhere, null);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * @return true if we should restrict the columns readable by this caller
     */
    private boolean shouldRestrictVisibility() {
        int callingUid = Binder.getCallingUid();
        return Binder.getCallingPid() != Process.myPid() &&
                callingUid != systemUid &&
                callingUid != defcontaineruid;
    }

    /**
     * Updates a row in the database
     */
    @Override
    public int update(final Uri uri, final ContentValues values, final String where, final String[] whereArgs) {

        Helpers.validateSelection(where, APP_READABLE_COLUMNS_SET);

        SQLiteDatabase db = openHelper.getWritableDatabase();

        int count;
        boolean startService = false;

        if (values.containsKey(DownloadContract.Downloads.COLUMN_DELETED)) {
            if (values.getAsInteger(DownloadContract.Downloads.COLUMN_DELETED) == 1) {
                // some rows are to be 'deleted'. need to start DownloadService.
                startService = true;
            }
        }

        ContentValues filteredValues;
        if (Binder.getCallingPid() != Process.myPid()) {
            filteredValues = new ContentValues();
            copyString(DownloadContract.Downloads.COLUMN_APP_DATA, values, filteredValues);
            Integer i = values.getAsInteger(DownloadContract.Downloads.COLUMN_CONTROL);
            if (i != null) {
                filteredValues.put(DownloadContract.Downloads.COLUMN_CONTROL, i);
                startService = true;
            }

            copyInteger(DownloadContract.Downloads.COLUMN_CONTROL, values, filteredValues);
            copyString(DownloadContract.Downloads.COLUMN_MEDIAPROVIDER_URI, values, filteredValues);
            copyInteger(DownloadContract.Downloads.COLUMN_DELETED, values, filteredValues);
        } else {
            filteredValues = values;

            Integer status = values.getAsInteger(DownloadContract.Downloads.COLUMN_STATUS);
            boolean isRestart = status != null && status == DownloadStatus.PENDING;
            boolean isUserBypassingSizeLimit =
                    values.containsKey(DownloadContract.Downloads.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT);
            if (isRestart || isUserBypassingSizeLimit) {
                startService = true;
            }
        }

        int match = URI_MATCHER.match(uri);
        switch (match) {
            case MY_DOWNLOADS:
            case MY_DOWNLOADS_ID:
            case ALL_DOWNLOADS:
            case ALL_DOWNLOADS_ID:
                SqlSelection selection = getWhereClause(uri, where, whereArgs, match);
                if (filteredValues.size() > 0) {
                    count = db.update(DownloadContract.Downloads.DOWNLOADS_TABLE_NAME, filteredValues, selection.getSelection(), selection.getParameters());
                } else {
                    count = 0;
                }
                notifyStatusIfDownloadStatusChanged(values);
                break;
            case BATCHES:
            case BATCHES_ID:
                SqlSelection batchSelection = getWhereClause(uri, where, whereArgs, match);
                count = db.update(DownloadContract.Batches.BATCHES_TABLE_NAME, values, batchSelection.getSelection(), batchSelection.getParameters());
                notifyStatusIfBatchesStatusChanged(values);
                break;
            default:
                LLog.d("updating unknown/invalid URI: " + uri);
                throw new UnsupportedOperationException("Cannot update URI: " + uri);
        }

        notifyContentChanged(uri, match);

        if (startService) {
            Context context = getContext();
            context.startService(new Intent(context, DownloadService.class));
        }
        return count;
    }

    private void notifyStatusIfDownloadStatusChanged(ContentValues values) {
        if (values.containsKey(DownloadContract.Downloads.COLUMN_STATUS)) {
            notifyDownloadStatusChanged();
        }
    }

    private void notifyStatusIfBatchesStatusChanged(ContentValues values) {
        if (values.containsKey(DownloadContract.Batches.COLUMN_STATUS)) {
            notifyBatchesStatusChanged();
        }
    }


    /**
     * Notify of a change through both URIs (/my_downloads and /all_downloads)
     *
     * @param uri      either URI for the changed download(s)
     * @param uriMatch the match ID from {@link #URI_MATCHER}
     */
    private void notifyContentChanged(final Uri uri, int uriMatch) {
        Long downloadId = null;
        if (uriMatch == MY_DOWNLOADS_ID || uriMatch == ALL_DOWNLOADS_ID) {
            downloadId = Long.parseLong(getDownloadIdFromUri(uri));
        }
        for (Uri uriToNotify : baseUris) {
            if (downloadId != null) {
                uriToNotify = ContentUris.withAppendedId(uriToNotify, downloadId);
            }
            getContext().getContentResolver().notifyChange(uriToNotify, null);
        }
    }

    private SqlSelection getWhereClause(final Uri uri, final String where, final String[] whereArgs,
                                        int uriMatch) {
        SqlSelection selection = new SqlSelection();
        selection.appendClause(where, whereArgs);
        if (uriMatch == MY_DOWNLOADS_ID || uriMatch == ALL_DOWNLOADS_ID ||
                uriMatch == PUBLIC_DOWNLOAD_ID) {
            selection.appendClause(DownloadContract.Downloads._ID + " = ?", getDownloadIdFromUri(uri));
        }
        if (uriMatch == BATCHES_ID) {
            selection.appendClause(DownloadContract.Batches._ID + " = ?", uri.getLastPathSegment());
        }
        if ((uriMatch == MY_DOWNLOADS || uriMatch == MY_DOWNLOADS_ID)
                && getContext().checkCallingPermission(DownloadsPermission.PERMISSION_ACCESS_ALL)
                != PackageManager.PERMISSION_GRANTED) {
            String callingUid = String.valueOf(Binder.getCallingUid());
            selection.appendClause(
                    Constants.UID + "= ? OR " + DownloadContract.Downloads.COLUMN_OTHER_UID + "= ?",
                    callingUid, callingUid);
        }
        return selection;
    }

    /**
     * Deletes a row in the database
     */
    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {

        Helpers.validateSelection(where, APP_READABLE_COLUMNS_SET);

        SQLiteDatabase db = openHelper.getWritableDatabase();
        int count;
        int match = URI_MATCHER.match(uri);
        switch (match) {
            case MY_DOWNLOADS:
            case MY_DOWNLOADS_ID:
            case ALL_DOWNLOADS:
            case ALL_DOWNLOADS_ID:
                SqlSelection selection = getWhereClause(uri, where, whereArgs, match);
                deleteRequestHeaders(db, selection.getSelection(), selection.getParameters());
                count = db.delete(DownloadContract.Downloads.DOWNLOADS_TABLE_NAME, selection.getSelection(), selection.getParameters());
                notifyDownloadStatusChanged();
                break;
            case BATCHES:
            case BATCHES_ID:
                SqlSelection batchSelection = getWhereClause(uri, where, whereArgs, match);
                count = db.delete(DownloadContract.Batches.BATCHES_TABLE_NAME, batchSelection.getSelection(), batchSelection.getParameters());
                notifyBatchesStatusChanged();
                break;

            default:
                LLog.d("deleting unknown/invalid URI: " + uri);
                throw new UnsupportedOperationException("Cannot delete URI: " + uri);
        }
        notifyContentChanged(uri, match);
        return count;
    }

    /**
     * Remotely opens a file
     */
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, String mode) throws FileNotFoundException {
        if (GlobalState.hasVerboseLogging()) {
            logVerboseOpenFileInfo(uri, mode);
        }

        String path;
        Cursor cursor = query(uri, new String[]{"_data"}, null, null, null);
        try {
            int count = cursor.getCount();
            if (count != 1) {
                // If there is not exactly one result, throw an appropriate exception.
                if (count == 0) {
                    throw new FileNotFoundException("No entry for " + uri);
                }
                throw new FileNotFoundException("Multiple items at " + uri);
            }

            cursor.moveToFirst();
            path = cursor.getString(0);
        } finally {
            cursor.close();
        }

        if (path == null) {
            throw new FileNotFoundException("No filename found.");
        }
        if (!Helpers.isFilenameValid(path, downloadsDataDir)) {
            LLog.d("INTERNAL FILE DOWNLOAD LOL COMMENTED EXCEPTION");
//            throw new FileNotFoundException("Invalid filename: " + path);
        }
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Bad mode for " + uri + ": " + mode);
        }

        ParcelFileDescriptor ret = ParcelFileDescriptor.open(
                new File(path),
                ParcelFileDescriptor.MODE_READ_ONLY);

        if (ret == null) {
            LLog.v("couldn't open file");
            throw new FileNotFoundException("couldn't open file");
        }
        return ret;
    }

    @Override
    public void dump(FileDescriptor fd, @NonNull PrintWriter writer, String[] args) {
        LLog.e("I want dump, but nothing to dump into");
    }

    private void logVerboseOpenFileInfo(Uri uri, String mode) {
        LLog.v(
                "openFile uri: " + uri + ", mode: " + mode
                        + ", uid: " + Binder.getCallingUid()
        );
        Cursor cursor = query(downloadsUriProvider.getContentUri(), new String[]{"_id"}, null, null, "_id");
        if (cursor == null) {
            LLog.v("null cursor in openFile");
        } else {
            if (!cursor.moveToFirst()) {
                LLog.v("empty cursor in openFile");
            } else {
                do {
                    LLog.v("row " + cursor.getInt(0) + " available");
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        cursor = query(uri, new String[]{"_data"}, null, null, null);
        if (cursor == null) {
            LLog.v("null cursor in openFile");
        } else {
            if (!cursor.moveToFirst()) {
                LLog.v("empty cursor in openFile");
            } else {
                String filename = cursor.getString(0);
                LLog.v("filename in openFile: " + filename);
                if (new java.io.File(filename).isFile()) {
                    LLog.v("file exists in openFile");
                }
            }
            cursor.close();
        }
    }

    private static void copyInteger(String key, ContentValues from, ContentValues to) {
        Integer i = from.getAsInteger(key);
        if (i != null) {
            to.put(key, i);
        }
    }

    private static void copyBoolean(String key, ContentValues from, ContentValues to) {
        Boolean b = from.getAsBoolean(key);
        if (b != null) {
            to.put(key, b);
        }
    }

    private static void copyString(String key, ContentValues from, ContentValues to) {
        String s = from.getAsString(key);
        if (s != null) {
            to.put(key, s);
        }
    }

}
