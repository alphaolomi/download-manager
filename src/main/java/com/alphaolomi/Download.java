package com.alphaolomi;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;

class Download extends Observable implements Runnable {
    private static final int MAX_BUFFER_SIZE = 1024;

    static final String[] STATUSES = {"Downloading", "Paused", "Complete", "Cancelled", "Error"};

    static final int DOWNLOADING = 0;

    static final int PAUSED = 1;

    private static final int COMPLETE = 2;

    private static final int CANCELLED = 3;

    static final int ERROR = 4;

    private final URL url; // download URL

    private int size; // size of download in bytes

    private int downloaded; // number of bytes downloaded

    private int status; // current status of download

    // Constructor for Download.
    Download(URL url) {
        this.url = url;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;

// Begin the download.
        download();
    }

    // Get this download's URL.
    String getUrl() {
        return url.toString();
    }

    // Get this download's size.
    int getSize() {
        return size;
    }

    // Get this download's progress.
    float getProgress() {
        return ((float) downloaded / size) * 100;
    }

    int getStatus() {
        return status;
    }

    void pause() {
        status = PAUSED;
        stateChanged();
    }

    void resume() {
        status = DOWNLOADING;
        stateChanged();
        download();
    }

    void cancel() {
        status = CANCELLED;
        stateChanged();
    }

    private void error() {
        status = ERROR;
        stateChanged();
    }

    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }

    // Get file name portion of URL.
    private String getFileName(URL url) {
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    // Download file.
    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;

        try {
            // Open connection to URL.
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Specify what portion of file to download.
            connection.setRequestProperty("Range", "bytes=" + downloaded + "-");

            // Connect to server.
            connection.connect();

            // Make sure response code is in the 200 range.
            if (connection.getResponseCode() / 100 != 2) {
                error();
            }

            // Check for valid content length.
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error();
            }

            /*        * Set the size for this download if it hasn't been already set.        */

            if (size == -1) {
                size = contentLength;
                stateChanged();
            }

            // Open file and seek to the end of it.
            file = new RandomAccessFile(getFileName(url), "rw");
            file.seek(downloaded);

            stream = connection.getInputStream();
            while (status == DOWNLOADING) {
                /* Size buffer according to how much of the file is left to download.          */
                byte[] buffer;
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[size - downloaded];
                }

                // Read from server into buffer.
                int read = stream.read(buffer);
                if (read == -1) break;

                // Write buffer to file.
                file.write(buffer, 0, read);
                downloaded += read;
                stateChanged();
            }

            /* Change status to complete if this point was reached because downloading  has finished.  */
            if (status == DOWNLOADING) {
                status = COMPLETE;
                stateChanged();
            }
        } catch (Exception e) {
            error();
        } finally {
            // Close file.
            if (file != null) {
                try {
                    file.close();
                } catch (Exception ignored) {
                }
            }

// Close connection to server.
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void stateChanged() {
        setChanged();
        notifyObservers();
    }
}