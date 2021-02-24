package com.github.singlethreaddownload.listener;

import java.io.File;

public interface FileDownloadListener {
    boolean onRepeatDownload();
    void onConnect(long totalSizeByte);
    void onSpeed(float bytePerSecond);
    void onProgress(long progressByte, long totalSizeByte);
    void onSuccess(File file);
    void onPause();
    void onDelete();
    void onError();
}
