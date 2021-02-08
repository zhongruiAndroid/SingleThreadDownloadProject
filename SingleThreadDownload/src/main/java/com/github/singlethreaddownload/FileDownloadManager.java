package com.github.singlethreaddownload;

import android.content.Context;
import android.support.v4.util.LruCache;

import com.github.singlethreaddownload.listener.FileDownloadListener;


public class FileDownloadManager {
    private static Context context;

    public static Context getContext() {
        if (context == null) {
            throw new IllegalStateException("please call DownloadManager.init(context)");
        }
        return context;
    }

    public static void init(Context ctx) {
        context = ctx.getApplicationContext();
    }
    private static LruCache<String,DownloadInfo> downloadMap =new LruCache<String,DownloadInfo>(6);

    public static DownloadInfo download(DownloadConfig config, FileDownloadListener listener) {
        DownloadInfo downloadInfo= downloadMap.get(config.getFileDownloadUrl());
        if(downloadInfo==null||config.isIfExistAgainDownload()||config.isReDownload()){
            downloadInfo = new DownloadInfo(config, listener);
            downloadMap.put(config.getFileDownloadUrl(),downloadInfo);
        }
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
        if(downloadMap!=null){
            downloadMap.remove(downloadInfo.getFileDownloadUrl());
        }
    }
}
