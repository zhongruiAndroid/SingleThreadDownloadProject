package com.github.singlethreaddownload;

import android.app.Application;
import android.content.Context;

import com.github.singlethreaddownload.listener.FileDownloadListener;


public class FileDownloadManager {
    private static Context context;
    private static int connectTimeout=10000;
    private static int readTimeout=10000;

    public static void setConnectTimeout(int connectTimeout) {
        FileDownloadManager.connectTimeout = connectTimeout;
    }

    public static void setReadTimeout(int readTimeout) {
        FileDownloadManager.readTimeout = readTimeout;
    }

    public static int getConnectTimeout() {
        if(connectTimeout<1000){
            connectTimeout=10000;
        }
        return connectTimeout;
    }

    public static int getReadTimeout() {
        if(readTimeout<1000){
            readTimeout=10000;
        }
        return readTimeout;
    }

    public static Context getContext() {
        if (context == null) {
            throw new IllegalStateException("please call FileDownloadManager.init(context)");
        }
        return context;
    }

    public static void init(Application application) {
        if(context!=null){
            return;
        }
        context = application.getApplicationContext();
        AppStateUtils.register(application);
    }

    public static DownloadInfo download(DownloadConfig config, FileDownloadListener listener) {
        DownloadInfo downloadInfo= new DownloadInfo(config, listener);
        downloadInfo.download();
        return downloadInfo;
    }
    public static DownloadInfo download(String url, FileDownloadListener listener) {
        DownloadConfig config=new DownloadConfig.Builder().setFileDownloadUrl(url).build();
        return download(config,listener);
    }
    public static void pauseDownload(DownloadInfo downloadInfo){
        if(downloadInfo==null){
            return;
        }
        downloadInfo.pauseDownload();
    }
    public static void deleteDownload(DownloadInfo downloadInfo){
        deleteDownload(downloadInfo,false);
    }
    public static void deleteDownload(DownloadInfo downloadInfo,boolean deleteTaskAndFile){
        if(downloadInfo==null){
            return;
        }
        downloadInfo.deleteDownload(deleteTaskAndFile);
    }
}
