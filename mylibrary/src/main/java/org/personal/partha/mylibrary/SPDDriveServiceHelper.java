package org.personal.partha.mylibrary;

import android.util.Log;
import android.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SPDDriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public SPDDriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    public Task<String> createFile(String mimeType, String fileName, String folderId) {
        return Tasks.call(mExecutor, () -> {
            String finalFolderId = folderId;
            if (finalFolderId.equalsIgnoreCase("")) {
                File metaDataFolder = new File()
                        .setParents(Collections.singletonList("root"))
                        .setMimeType("application/vnd.google-apps.folder")
                        .setName("org.personal.partha.myaccountstracker");
                File googleFileFolder = mDriveService.files().create(metaDataFolder).setFields("id").execute();
                if (googleFileFolder == null) {
                    return null;
                }
                finalFolderId = googleFileFolder.getId();
            }
            File metaData = new File()
                    .setParents(Collections.singletonList(finalFolderId))
                    .setMimeType(mimeType)
                    .setName(fileName);

            File googleFile = mDriveService.files().create(metaData).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }
            return googleFile.getId();
        });
    }

    /**
     * Opens the file identified by {@code fileId} and returns a {@link Pair} of its name and
     * contents.
     */
    public Task<Void> readFile(String fileId, String filePath) {
        return Tasks.call(mExecutor, () -> {
            File metadata = mDriveService.files().get(fileId).execute();
            String fileName = metadata.getName();
            FileOutputStream fileStream = null;
            try {
                fileStream = new FileOutputStream(filePath + "/" + fileName);
                mDriveService.files().get(fileId).executeMediaAndDownloadTo(fileStream);
                return null;
            } catch (FileNotFoundException e) {
                Log.e(MyUtility.MONGO_DB_NAME, e.getMessage());
                return null;
            } finally {
                fileStream.flush();
                fileStream.close();
            }
        });
    }

    /**
     * Updates the file identified by {@code fileId} with the {@code uploadedFile} content.
     */
    public Task<Void> saveFile(String fileId, File uploadedFile, FileContent mediaContent) {
        return Tasks.call(mExecutor, () -> {
            // Update the file contents.
            mDriveService.files().update(fileId, uploadedFile, mediaContent).execute();
            return null;
        });
    }

    /**
     * Deletes the file identified by {@code fileId}.
     */
    public Task<Void> deleteFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            // Update the file contents.
            mDriveService.files().delete(fileId).execute();
            return null;
        });
    }

    /**
     * Returns a {@link FileList} containing all the visible files in the user's My Drive.
     *
     * <p>The returned list will only contain files visible to this app, i.e. those which were
     * created by this app. To perform operations on files not created by the app, the project must
     * request Drive Full Scope in the <a href="https://play.google.com/apps/publish">Google
     * Developer's Console</a> and be submitted to Google for verification.</p>
     */
    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                return mDriveService.files().list().setSpaces("drive").execute();
            }
        });
    }
}
