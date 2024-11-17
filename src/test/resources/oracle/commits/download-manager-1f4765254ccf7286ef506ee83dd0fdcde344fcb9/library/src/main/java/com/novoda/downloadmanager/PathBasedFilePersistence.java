package com.novoda.downloadmanager;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

class PathBasedFilePersistence implements FilePersistence {

    private static final String DOWNLOADS_DIR = "/downloads/";
    private static final String UNDEFINED_DIRECTORY_TYPE = null;
    private static final boolean APPEND = true;

    private Context context;
    private StorageRequirementRule storageRequirementRule;

    @Nullable
    private FileOutputStream fileOutputStream;

    @Override
    public void initialiseWith(Context context, StorageRequirementRule storageRequirementRule) {
        this.context = context.getApplicationContext();
        this.storageRequirementRule = storageRequirementRule;
    }

    @Override
    public FilePath basePath() {
        return FilePathCreator.create(getExternalFileDirWithBiggerAvailableSpace().getAbsolutePath(), DOWNLOADS_DIR);
    }

    @Override
    public FilePersistenceResult create(FilePath absoluteFilePath, FileSize fileSize) {
        if (fileSize.isTotalSizeUnknown()) {
            return FilePersistenceResult.ERROR_UNKNOWN_TOTAL_FILE_SIZE;
        }

        if (!isExternalStorageWritable()) {
            return FilePersistenceResult.ERROR_EXTERNAL_STORAGE_NON_WRITABLE;
        }

        File externalFileDir = getExternalFileDirWithBiggerAvailableSpace();

        if (storageRequirementRule.hasViolatedRule(externalFileDir, fileSize)) {
            return FilePersistenceResult.ERROR_INSUFFICIENT_SPACE;
        }

        return create(absoluteFilePath);
    }

    private FilePersistenceResult create(FilePath absoluteFilePath) {
        if (absoluteFilePath.isUnknown()) {
            return FilePersistenceResult.ERROR_OPENING_FILE;
        }

        try {
            File file = new File(absoluteFilePath.path());
            boolean parentDirectoriesExist = ensureParentDirectoriesExistFor(file);

            if (!parentDirectoriesExist) {
                return FilePersistenceResult.ERROR_OPENING_FILE;
            }

            fileOutputStream = new FileOutputStream(file, APPEND);
        } catch (FileNotFoundException e) {
            Logger.e(e, "File could not be opened");
            return FilePersistenceResult.ERROR_OPENING_FILE;
        }

        return FilePersistenceResult.SUCCESS;
    }

    private boolean ensureParentDirectoriesExistFor(File outputFile) {
        boolean parentExists = outputFile.getParentFile().exists();
        if (parentExists) {
            return true;
        }

        Logger.w(String.format("path: %s doesn't exist, creating parent directories...", outputFile.getAbsolutePath()));
        return outputFile.getParentFile().mkdirs();
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private File getExternalFileDirWithBiggerAvailableSpace() {
        File externalFileDir = null;
        long usableSpace = 0;

        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(context, UNDEFINED_DIRECTORY_TYPE);
        for (File dir : externalFilesDirs) {
            if (dir == null) {
                continue;
            }

            long localUsableSpace = dir.getUsableSpace();
            if (usableSpace < localUsableSpace) {
                externalFileDir = dir;
                usableSpace = localUsableSpace;
            }
        }

        return externalFileDir;
    }

    @Override
    public boolean write(byte[] buffer, int offset, int numberOfBytesToWrite) {
        if (fileOutputStream == null) {
            Logger.e("Cannot write, you must create the file first");
            return false;
        }

        try {
            fileOutputStream.write(buffer, offset, numberOfBytesToWrite);
            return true;
        } catch (IOException e) {
            Logger.e(e, "Exception while writing to internal physical storage");
            return false;
        }
    }

    @Override
    public void delete(FilePath absoluteFilePath) {
        if (absoluteFilePath == null || absoluteFilePath.isUnknown()) {
            Logger.w("Cannot delete, you must create the file first.");
            return;
        }

        File fileToDelete = new File(absoluteFilePath.path());
        if (!fileToDelete.exists()) {
            return;
        }

        boolean deleted = fileToDelete.delete();

        String message = String.format("File or Directory: %s deleted: %s", absoluteFilePath.path(), deleted);
        Logger.d(getClass().getSimpleName(), message);
    }

    @Override
    public long getCurrentSize(FilePath filePath) {
        File file = new File(filePath.path());
        return file.length();
    }

    @Override
    public void close() {
        if (fileOutputStream == null) {
            return;
        }

        try {
            fileOutputStream.close();
        } catch (IOException e) {
            Logger.e(e, "Failed to close fileOutputStream.");
        }
    }

    @Override
    public FilePersistenceType getType() {
        return FilePersistenceType.EXTERNAL;
    }
}
