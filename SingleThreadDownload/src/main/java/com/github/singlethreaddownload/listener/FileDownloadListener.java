package com.github.singlethreaddownload.listener;

import java.io.File;

public interface FileDownloadListener {
    void onConnect(long totalSizeByte);
    void onSpeed(float speedByteBySecond);
    void onProgress(long progressByte, long totalSizeByte);
    void onSuccess(File file);
    void onPause();
    void onDelete();
    void onError();
}
