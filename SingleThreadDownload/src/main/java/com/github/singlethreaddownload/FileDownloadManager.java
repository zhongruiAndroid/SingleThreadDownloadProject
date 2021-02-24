package com.github.singlethreaddownload;

import android.content.Context;
import android.support.v4.util.LruCache;

import com.github.singlethreaddownload.listener.FileDownloadListener;


public class FileDownloadManager {
    private static Context context;

    public static Context getContext() {
        if (context == null) {
            throw new IllegalStateException("please call FileDownloadManager.init(context)");
        }
        return context;
    }

    public static void init(Context ctx) {
        context = ctx.getApplicationContext();
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
        if(downloadInfo==null){
            return;
        }
        downloadInfo.deleteDownload();
    }
}
