/**
    Copyright (C) 2014 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.forrestguice.suntimeswidget.getfix;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;

public class ExportPlacesTask extends AsyncTask<Object, Object, ExportPlacesTask.ExportResult>
{
    public static final long MIN_WAIT_TIME = 2000;
    public static final long CACHE_MAX = 256000;

    private WeakReference<Context> contextRef;

    private String exportTarget;
    private File exportFile;
    private int numEntries;
    protected boolean usedExternalStorage = false;
    private String newLine = System.getProperty("line.separator");

    private boolean isPaused = false;
    public void pauseTask()
    {
        isPaused = true;
    }
    public void resumeTask()
    {
        isPaused = false;
    }
    public boolean isPaused()
    {
        return isPaused;
    }

    public ExportPlacesTask(Context context, String exportTarget)
    {
        this(context, exportTarget, false);
    }
    public ExportPlacesTask(Context context, String exportTarget, boolean saveToCache)
    {
        this.contextRef = new WeakReference<Context>(context);
        this.exportTarget = exportTarget;
        this.saveToCache = saveToCache;
    }

    /**
     * Property: save/export to the cache
     */
    private boolean saveToCache = false;
    public boolean willSaveToCache() { return saveToCache; }

    /**
     * Property: overwrite existing file
     */
    private boolean overwriteTarget = false;
    public boolean getOverwriteFlag() { return overwriteTarget; }
    public void setOverwriteFlag(boolean value) { this.overwriteTarget = value; }

    /**
     * onPreExecute
     * Runs before task begins.
     */
    @Override
    protected void onPreExecute()
    {
        numEntries = 0;
        signalStarted();
    }

    /**
     * doInBackground
     */
    @Override
    protected ExportResult doInBackground(Object... params)
    {
        final Context context = contextRef.get();
        if (context == null)
        {
            Log.w("ExportPlacesTask", "Reference (weak) to context is null at start of doInBackground; cancelling...");
            return new ExportResult(false, null);
        }

        long startTime = System.currentTimeMillis();

        //
        // Step 1: get a handle to the exportFile
        // (from the external cache, external dl dir, or internal cache)
        //
        String storageState = Environment.getExternalStorageState();
        usedExternalStorage = storageState.equals(Environment.MEDIA_MOUNTED);
        if (usedExternalStorage)
        {
            if (saveToCache)         // save to: external cache
            {
                try {
                    cleanupExternalCache(context);
                    exportFile = File.createTempFile(exportTarget, ".csv", context.getExternalCacheDir());
                } catch (IOException e) {
                    Log.w("ExportPlaces", "Canceling export; failed to create external temp file.");
                    return new ExportResult(false, null);
                }

            } else {                 // save to: external download dir

                File exportPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                exportFile = new File(exportPath, exportTarget);

                boolean targetExists = exportFile.exists();
                if (targetExists && !overwriteTarget)
                {
                    Log.w("ExportPlaces", "Canceling export; the target already exists (and overwrite flag is false). " + exportFile.getAbsolutePath());
                    return new ExportResult(false, exportFile);

                } else if (targetExists) {
                    int c = 0;
                    String outFile;
                    do {
                        outFile = exportTarget + "-" + c + ".csv";
                        c++;
                    } while ((exportFile = new File(exportPath, outFile)).exists());
                }
            }

        } else if (saveToCache) {    // save to: internal cache

            try {
                cleanupInternalCache(context);
                exportFile = File.createTempFile(exportTarget, ".csv", context.getCacheDir());

            } catch (IOException e) {
                Log.w("ExportPlaces", "Canceling export; failed to create internal temp file.");
                return new ExportResult(false, null);
            }

        } else {
            Log.w("ExportPlaces", "Canceling export; external storage is unavailable.");
            return new ExportResult(false, null);
        }

        //
        // Step 2: open database, save to file
        //
        boolean exported = false;
        BufferedOutputStream out = null;
        Cursor cursor = null;
        GetFixDatabaseAdapter db = new GetFixDatabaseAdapter(context.getApplicationContext());

        try {
            db.open();
            numEntries = db.getPlaceCount();
            out = new BufferedOutputStream(new FileOutputStream(exportFile));
            cursor = db.getAllPlaces(-1, true);
            exported = exportDatabase(db, cursor, out);

        } catch (IOException e) {
            Log.w("ExportPlaces", "FAILED to write to the export target! " + exportFile.getAbsolutePath() + " :: " + e);

        } finally {
            //
            // Step 3: cleanup
            //
            if (out != null)
            {
                try {
                    out.close();
                } catch (IOException e2) {
                    Log.w("ExportPlaces", "FAILED to close the export target! " + exportFile.getAbsolutePath() + " :: " + e2);
                }
            }
            if (cursor != null)
            {
                cursor.close();
            }
            db.close();
        }

        //
        // Step 4: wait for UI to spin a second, then return
        //
        long endTime = System.currentTimeMillis();
        while ((endTime - startTime) < MIN_WAIT_TIME || isPaused)
        {
            endTime = System.currentTimeMillis();
        }
        return new ExportResult(exported, exportFile);
    }

    /**
     * Runs after the task completes.
     * @param results an ExportResult object wrapping the result
     */
    @Override
    protected void onPostExecute(ExportResult results)
    {
        signalFinished(results);
    }

    /**
     * @param db a GetFixDatabaseAdapter helper
     * @param cursor a database Cursor pointing to records to export
     * @param out a BufferedOutputStream (open and ready) to export to
     * @return true export was successful, false otherwise
     * @throws IOException
     */
    private boolean exportDatabase( GetFixDatabaseAdapter db, Cursor cursor, BufferedOutputStream out ) throws IOException
    {
        if (cursor == null)
        {
            Log.w("ExportPlaces", "Canceling export; the database returned a null cursor.");
            return false;
        }

        String csvHeader = db.addPlaceCSV_header() + newLine;
        out.write(csvHeader.getBytes());

        int i = 0;
        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            ContentValues entryValues = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(cursor, entryValues);

            String csvRow = db.addPlaceCSV_row(entryValues) + newLine;
            out.write(csvRow.getBytes());

            cursor.moveToNext();
            i++;

            String msg = entryValues.getAsString(GetFixDatabaseAdapter.KEY_PLACE_NAME);
            ExportProgress progressObj = new ExportProgress(i, numEntries, msg);
            publishProgress(progressObj);
        }
        return true;
    }

    /**
     * Export Result
     */
    public static class ExportResult
    {
        public ExportResult( boolean result, File exportFile )
        {
            this.result = result;
            this.exportFile = exportFile;
        }

        private boolean result;
        public boolean getResult() { return result; }

        private File exportFile;
        public File getExportFile() { return exportFile; }
    }

    /**
     * Export Progress
     */
    public static class ExportProgress
    {
        public ExportProgress(int current, int max, String msg)
        {
            progressNow = current;
            progressMax = max;
            progressMsg = msg;
        }

        private int progressNow;
        public int getProgress() { return progressNow; }

        private int progressMax;
        public int getProgressMax() { return progressMax; }

        private String progressMsg;
        public String getProgressMsg() { return progressMsg; }
    }

    /**
     * Cleanup the internal cache.
     */
    private void cleanupInternalCache(Context context)
    {
        File cacheDir = context.getCacheDir();
        cleanupCache(cacheDir, CACHE_MAX);
    }

    /**
     * Cleanup the external cache.
     */
    private void cleanupExternalCache(Context context)
    {
        File cacheDir = context.getExternalCacheDir();
        cleanupCache(cacheDir, CACHE_MAX);
    }

    /**
     */
    private void cleanupCache(File cacheDir, long cacheLimit)
    {
        long cacheSize = cacheSize(cacheDir);
        if (cacheSize > cacheLimit)
        {
            File[] cacheFiles = cacheDir.listFiles();
            Arrays.sort(cacheFiles, new Comparator<File>()
            {
                public int compare(File file1, File file2)
                {
                    return Long.valueOf(file1.lastModified()).compareTo(file2.lastModified());
                }
            });

            int i = 0, deletedSize = 0;
            do {
                deletedSize += cacheFiles[i].length();
                if (!cacheFiles[i].delete())
                {
                    Log.w("ExportPlaces", "Failed to cleanup cache; unable to delete " + cacheFiles[i].getPath());
                }
                i++;
            } while (cacheSize - deletedSize > CACHE_MAX);
        }
    }

    /**
     */
    private static long cacheSize(File dir)
    {
        long result = 0;
        if (dir.exists())
        {
            for (File file : dir.listFiles())
            {
                result += (file.isDirectory()) ? cacheSize(file)
                        : file.length();
            }
        }
        return result;
    }

    /**
     * Task Listener
     */
    public static abstract class TaskListener
    {
        public void onStarted() {}
        public void onFinished( ExportResult result ) {}
    }
    private TaskListener taskListener = null;
    public void setTaskListener( TaskListener listener )
    {
        taskListener = listener;
    }
    public void clearTaskListener()
    {
        taskListener = null;
    }
    private void signalStarted()
    {
        if (taskListener != null)
        {
            taskListener.onStarted();
        }
    }
    private void signalFinished( ExportResult result )
    {
        if (taskListener != null)
        {
            taskListener.onFinished(result);
        }
    }
}
