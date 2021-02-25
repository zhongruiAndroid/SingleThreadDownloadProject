package com.test.singlethreaddownloadproject;

import android.app.Application;

import com.github.singlethreaddownload.FileDownloadManager;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FileDownloadManager.init(this);
    }
}
