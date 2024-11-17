package com.novoda.downloadmanager.lib;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import com.novoda.notils.logger.simple.Log;

/**
 * Creates and updated database on demand when opening it.
 * Helper class to create database the first time the provider is
 * initialized and upgrade it when a new version of the provider needs
 * an updated version of the database.
 */
final class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * Current database version
     */
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context, String dbName) {
        super(context, dbName, null, DB_VERSION);
    }

    /**
     * Creates database the first time we try to open it.
     */
    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        Log.v("populating new database");
        createDownloadsTable(db);
        createHeadersTable(db);
        createBatchesTable(db);
        makeCacheDownloadsInvisible(db);
    }

    /**
     * Updates the database format when a content provider is used
     * with a database that was created with a different format.
     * <p/>
     * Note: to support downgrades, creating a table should always drop it first if it already
     * exists.
     */
    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, final int newVersion) {
        // no upgrade path yet
    }

    /**
     * Creates the table that'll hold the download information.
     */
    private void createDownloadsTable(SQLiteDatabase db) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + Downloads.Impl.TABLE_NAME);
            db.execSQL("CREATE TABLE " + Downloads.Impl.TABLE_NAME + "(" +
                    Downloads.Impl._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    Downloads.Impl.COLUMN_URI + " TEXT, " +
                    Constants.RETRY_AFTER_X_REDIRECT_COUNT + " INTEGER, " +
                    Downloads.Impl.COLUMN_APP_DATA + " TEXT, " +
                    Downloads.Impl.COLUMN_NO_INTEGRITY + " BOOLEAN, " +
                    Downloads.Impl.COLUMN_FILE_NAME_HINT + " TEXT, " +
                    Constants.OTA_UPDATE + " BOOLEAN, " +
                    Downloads.Impl._DATA + " TEXT, " +
                    Downloads.Impl.COLUMN_MIME_TYPE + " TEXT, " +
                    Downloads.Impl.COLUMN_DESTINATION + " INTEGER, " +
                    Constants.NO_SYSTEM_FILES + " BOOLEAN, " +
                    Downloads.Impl.COLUMN_VISIBILITY + " INTEGER, " +
                    Downloads.Impl.COLUMN_CONTROL + " INTEGER, " +
                    Downloads.Impl.COLUMN_STATUS + " INTEGER, " +
                    Downloads.Impl.COLUMN_FAILED_CONNECTIONS + " INTEGER, " +
                    Downloads.Impl.COLUMN_LAST_MODIFICATION + " BIGINT, " +
                    Downloads.Impl.COLUMN_NOTIFICATION_CLASS + " TEXT, " +
                    Downloads.Impl.COLUMN_NOTIFICATION_EXTRAS + " TEXT, " +
                    Downloads.Impl.COLUMN_COOKIE_DATA + " TEXT, " +
                    Downloads.Impl.COLUMN_USER_AGENT + " TEXT, " +
                    Downloads.Impl.COLUMN_REFERER + " TEXT, " +
                    Downloads.Impl.COLUMN_TOTAL_BYTES + " INTEGER NOT NULL DEFAULT -1, " +
                    Downloads.Impl.COLUMN_CURRENT_BYTES + " INTEGER NOT NULL DEFAULT 0, " +
                    Constants.ETAG + " TEXT, " +
                    Constants.UID + " INTEGER, " +
                    Downloads.Impl.COLUMN_OTHER_UID + " INTEGER, " +

                    // these 3 columns should be on the batch table only
                    Downloads.Impl.COLUMN_TITLE + " TEXT NOT NULL DEFAULT '', " +
                    Downloads.Impl.COLUMN_DESCRIPTION + " TEXT NOT NULL DEFAULT '', " +
                    Downloads.Impl.COLUMN_BIG_PICTURE + " TEXT, " +

                    Downloads.Impl.COLUMN_ALLOW_ROAMING + " INTEGER NOT NULL DEFAULT 0, " +
                    Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES + " INTEGER NOT NULL DEFAULT 0, " +
                    Downloads.Impl.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI + " INTEGER NOT NULL DEFAULT 1, " +
                    Downloads.Impl.COLUMN_BYPASS_RECOMMENDED_SIZE_LIMIT + " INTEGER NOT NULL DEFAULT 0, " +
                    Downloads.Impl.COLUMN_MEDIAPROVIDER_URI + " TEXT, " +
                    Downloads.Impl.COLUMN_DELETED + " BOOLEAN NOT NULL DEFAULT 0, " +
                    Downloads.Impl.COLUMN_ERROR_MSG + " TEXT, " +
                    Downloads.Impl.COLUMN_ALLOW_METERED + " INTEGER NOT NULL DEFAULT 1, " +
                    Downloads.Impl.COLUMN_BATCH_ID + " INTEGER, " +
                    Constants.MEDIA_SCANNED + " BOOLEAN);");
        } catch (SQLException ex) {
            Log.e("couldn't create table in downloads database");
            throw ex;
        }
    }

    /**
     * Set all existing downloads to the cache partition to be invisible in the downloads UI.
     */
    private void makeCacheDownloadsInvisible(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_IS_VISIBLE_IN_DOWNLOADS_UI, false);
        String cacheSelection = Downloads.Impl.COLUMN_DESTINATION + " != " + Downloads.Impl.DESTINATION_EXTERNAL;
        db.update(Downloads.Impl.TABLE_NAME, values, cacheSelection, null);
    }

    private void createHeadersTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + Downloads.Impl.RequestHeaders.HEADERS_DB_TABLE);
        db.execSQL(
                "CREATE TABLE " + Downloads.Impl.RequestHeaders.HEADERS_DB_TABLE + "(" +
                        Downloads.Impl.RequestHeaders._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        Downloads.Impl.RequestHeaders.COLUMN_DOWNLOAD_ID + " INTEGER NOT NULL," +
                        Downloads.Impl.RequestHeaders.COLUMN_HEADER + " TEXT NOT NULL," +
                        Downloads.Impl.RequestHeaders.COLUMN_VALUE + " TEXT NOT NULL" +
                        ");");
    }

    private void createBatchesTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + Downloads.Impl.Batches.BATCHES_DB_TABLE);
        db.execSQL(
                "CREATE TABLE " + Downloads.Impl.Batches.BATCHES_DB_TABLE + "(" +
                        Downloads.Impl.Batches._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        Downloads.Impl.Batches.COLUMN_TITLE + " TEXT NOT NULL," +
                        Downloads.Impl.Batches.COLUMN_DESCRIPTION + " TEXT NOT NULL," +
                        Downloads.Impl.Batches.COLUMN_BIG_PICTURE + " TEXT NOT NULL," +
                        Downloads.Impl.Batches.COLUMN_STATUS + " INTEGER," +
                        Downloads.Impl.Batches.COLUMN_VISIBILITY + " INTEGER" +
                        ");");
    }
}
