package com.github.singlethreaddownload;

import android.text.TextUtils;

import com.github.singlethreaddownload.helper.DownloadHelper;
import com.github.singlethreaddownload.helper.DownloadRecord;
import com.github.singlethreaddownload.listener.FileDownloadListener;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadInfo {
    private FileDownloadListener downloadListener;
    private DownloadConfig downloadConfig;
    private volatile DownloadRecord downloadRecord;
    /*下载文件的总大小*/
    private long totalSize;
    /*已下载的缓存大小，这个大小是从缓存记录读取，不是从缓存文件*/
    private long localCacheSize;


    public static final int STATUS_ERROR = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_PAUSE = 3;
    public static final int STATUS_DELETE = 4;
    public static final int STATUS_PROGRESS = 5;
    public static final int STATUS_CONNECT = 6;

    private List<Object> taskInfoList = new ArrayList<>();

    private AtomicInteger status;
    /*下载进度*/
    private AtomicLong downloadProgress;

    public DownloadInfo(DownloadConfig config, FileDownloadListener listener) {
        this.downloadConfig = config;
        this.downloadListener = listener;
        status = new AtomicInteger(0);
        downloadProgress = new AtomicLong(0);
    }

    public FileDownloadListener getDownloadListener() {
        if (downloadListener == null) {
            downloadListener = new FileDownloadListener() {
                @Override
                public void onConnect(long totalSize) {

                }

                @Override
                public void onSpeed(float speedBySecond) {

                }

                @Override
                public void onProgress(long progress, long totalSize) {

                }

                @Override
                public void onSuccess(File file) {

                }

                @Override
                public void onPause() {

                }

                @Override
                public void onDelete() {

                }

                @Override
                public void onError() {

                }
            };
        }
        return downloadListener;
    }

    private void setStatus(int status) {
        this.status.set(status);
    }

    public int getStatus() {
        return status.get();
    }

    public void pauseDownload() {
        changeStatus(STATUS_PAUSE);
    }

    public void deleteDownload( ) {
        changeStatus(STATUS_DELETE);
    }

    private void changeStatus(int changeStatus) {
        if (taskInfoList == null) {
            return;
        }
        if (getStatus() == STATUS_SUCCESS) {
            // TODO:
            return;
        }
        for (TaskInfo info : taskInfoList) {
            info.changeStatus(changeStatus);
        }
        switch (changeStatus) {
            case STATUS_PAUSE:
                pause();
                break;
        }

    }

    private void error() {
        int status = getStatus();
        if (status == STATUS_ERROR) {
            return;
        }
        if (downloadConfig != null) {
            DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
            DownloadHelper.get().clearRecord(downloadConfig.getFileDownloadUrl());
        }
        setStatus(STATUS_ERROR);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onError();
            }
        });
    }

    private void pause() {
        int status = getStatus();
        if (status == STATUS_PAUSE) {
            return;
        }
        setStatus(STATUS_PAUSE);
        /*手动暂停时把内存的缓存信息保存至本地*/
        if(downloadRecord!=null){
            saveDownloadCacheInfo(downloadRecord);
        }
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onPause();
            }
        });
    }

    private void delete() {
        int status = getStatus();
        if (status == STATUS_DELETE) {
            return;
        }
        setStatus(STATUS_DELETE);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onDelete();
            }
        });
    }

    private void success(final File file) {
        int status = getStatus();
        if (status == STATUS_SUCCESS) {
            return;
        }
        setStatus(STATUS_SUCCESS);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onSuccess(file);
            }
        });
    }

    private void connect(final long totalSize) {
        int status = getStatus();
        if (status == STATUS_CONNECT) {
            return;
        }
        this.totalSize = totalSize;
        downloadProgress.set(0);
        setStatus(STATUS_CONNECT);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onConnect(totalSize);
            }
        });
    }

    private long preTime;
    private long tempDownloadSize;
    private long tempTimeInterval = 200;

    private void reset() {
        preTime = 0;
        tempDownloadSize = 0;
        localCacheSize = 0;
    }

    private  void progress(final long downloadSize) {
        saveDownloadCacheInfo(downloadRecord);
        final long progress = downloadProgress.addAndGet(downloadSize);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                //计算网速
                if (downloadConfig.isNeedSpeed()) {
                    long nowTime = System.currentTimeMillis();
                    if (preTime <= 0) {
                        tempDownloadSize = 0;
                        preTime = nowTime;
                    }
                    long timeInterval = nowTime - preTime;
                    if (timeInterval >= tempTimeInterval) {
                        tempTimeInterval = 1000;
                        float speedBySecond = tempDownloadSize * 1000f / timeInterval / 1024;
                        preTime = nowTime;
                        tempDownloadSize = 0;
                        getDownloadListener().onSpeed(Float.parseFloat(String.format("%.1f", speedBySecond)));
                    } else {
                        tempDownloadSize += downloadSize;
                    }
                }
                getDownloadListener().onProgress(progress + localCacheSize, totalSize);
            }
        });
    }

    private long getContentLength(HttpURLConnection httpURLConnection) {
        String value = httpURLConnection.getHeaderField("content-length");
        return Long.parseLong(value);
    }
    public void download(){
        if(downloadConfig==null){
            getDownloadListener().onError();
        }
        if(getStatus()==STATUS_PROGRESS||getStatus()==STATUS_CONNECT){
            return;
        }
        String fileUrl=downloadConfig.getFileDownloadUrl();
        if (TextUtils.isEmpty(fileUrl)) {
            getDownloadListener().onError();
        }
        /*检查下载条件*/
        // TODO: 2021/2/8
        /*需要检查本地是否已经存在未下载完成的任务*/
        DownloadHelper.get().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                downloadByChildThread();
            }
        });
    }
    private void downloadByChildThread() {
        reset();
        /*下载完成后需要保存的文件*/
        File saveFile = downloadConfig.getSaveFile();
        /*如果存在已下载完成的文件*/
        if (saveFile != null && saveFile.exists() && saveFile.isFile()) {
            if (downloadConfig.isIfExistAgainDownload()) {
                // TODO: 2021/2/8
                /*下载新的文件*/
//                downloadConfig.setSaveFile(reDownloadAndRename(1));

//                DownloadHelper.deleteFile(saveFile);
            } else {
                /*如果本地已存在下载的文件，直接返回*/
                long length = saveFile.length();
                connect(length);
                progress(length);
                success(saveFile);
                return;
            }
        }
        String fileUrl=downloadConfig.getFileDownloadUrl();
        /*获取本地是否存在下载记录*/

        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(fileUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestProperty("Range", "bytes=" + 0 + "-");
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            long contentLength = 0;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                /*不支持范围下载*/
                contentLength = getContentLength(httpURLConnection);
                if (contentLength <= 0) {
                    error();
                    return;
                }
                if (!DownloadHelper.hasFreeSpace(FileDownloadManager.getContext(), contentLength)) {
                    //储存空间不足
                    error();
                    return;
                }
                connect(contentLength);
                /*单线程下载*/
                downloadRecord = new DownloadRecord(contentLength, 1);

            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                /*支持范围下载*/
                contentLength = getContentLength(httpURLConnection);
                if (!DownloadHelper.hasFreeSpace(FileDownloadManager.getContext(), contentLength)) {
                    error();
                    return;
                }
                connect(contentLength);
                /*如果文件小于30kb，就用单线程下载*/
                if (contentLength < 30 * 1024) {
                    /*单线程下载*/
                    downloadRecord = new DownloadRecord(contentLength, 1);
                } else {
                    int threadNum = downloadConfig.getThreadNum();
                    /*先判断内存是否存在数据，不存在再读取本地缓存配置*/
                    if(downloadRecord==null){
                        downloadRecord = DownloadHelper.get().getRecord(fileUrl);
                    }
                    /*如果重新下载，忽略之前的下载进度*/
                    if (downloadConfig.isReDownload()) {
                        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
                        DownloadHelper.get().clearRecord(downloadConfig.getFileDownloadUrl());
                        downloadRecord.getFileRecordList().clear();
                    }
//                    Log.i("=====", "=====toJson=" + downloadRecord.toJson());
                    // TODO: 2020/8/28
                    /*如果本地缓存配置有数据，但是下载的文件不存在，则删除本地配置*/
                    if (downloadConfig.getTempSaveFile() != null && !downloadConfig.getTempSaveFile().exists()) {
                        DownloadHelper.get().clearRecord(fileUrl);
                        downloadRecord = null;
                    }
                    if (downloadRecord == null || downloadRecord.getFileSize() <= 0 || downloadRecord.getFileSize() != contentLength) {
                        /*如果用户手动删除了配置缓存文件，则重新下载*/
                        /*如果下载的文件大小和缓存的配置大小不一致，重新下载,删除需要下载的文件缓存*/
                        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
                        downloadRecord = new DownloadRecord(contentLength, threadNum);
                    }
                }
            }
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            /*开始准备下载*/
            prepareDownload();
        } catch (Exception e) {
            e.printStackTrace();
            error();
        }

    }

    /*开始准备下载*/
    private void prepareDownload() {
        List<DownloadRecord.FileRecord> fileRecordList = downloadRecord.getFileRecordList();

        int threadNum = downloadConfig.getThreadNum();
        setStatus(STATUS_PROGRESS);
        for (int i = 0; i < threadNum; i++) {
            final DownloadRecord.FileRecord record = fileRecordList.get(i);
            long downloadLength = record.getDownloadLength();

            if (downloadLength > 0) {
                record.setDownloadLength(record.getDownloadLength() - 1);
                downloadLength = record.getDownloadLength();
            }
            /*记录之前缓存的下载的进度*/
            localCacheSize += downloadLength;
            TaskInfo taskInfo = new TaskInfo(downloadConfig.getFileDownloadUrl(), record.getStartPoint() + downloadLength, record.getEndPoint(), downloadConfig.getTempSaveFile(), new TaskInfo.ReadStreamListener() {
                @Override
                public void readLength(long readLength) {
                    long currentProgress = record.getDownloadLength() + readLength;
                    record.setDownloadLength(currentProgress);
                    int status = getStatus();
                    if(status==STATUS_PAUSE||status==STATUS_ERROR||status==STATUS_DELETE){
                        return;
                    }
                    progress(readLength);
                }

                @Override
                public void readComplete() {
                    /*每个taskinfo下载完之后检查其他的taskinfo是否也下载完成*/
                    checkOtherTaskInfoIsComplete();
                }

                @Override
                public void fail() {
                    /*如果有taskinfo出现错误，则通知其他taskinfo也改变状态为error*/
                    checkOtherTaskInfoIsError();
                }

                @Override
                public void needDelete() {
                    /*taskinfo告诉外部可以删除的时候，检查每个taskinfo是否都是可删除状态*/
                    checkOtherTaskInfoIsDelete();
                }
            });
            taskInfoList.add(taskInfo);
            DownloadHelper.get().getExecutorService().execute(taskInfo);
        }
    }

    private void checkOtherTaskInfoIsComplete() {
        if (taskInfoList == null) {
            return;
        }
        for (TaskInfo info : taskInfoList) {
            int taskInfoStatus = info.getCurrentStatus();
            if (taskInfoStatus != DownloadInfo.STATUS_SUCCESS) {
                return;
            }
        }
        /*所有taskinfo下载完才是真的下载完*/
        downloadConfig.getTempSaveFile().renameTo(downloadConfig.getSaveFile());
        DownloadHelper.get().clearRecord(downloadConfig.getFileDownloadUrl());
        success(downloadConfig.getSaveFile());
    }

    /*如果外部通知下载任务需要删除，检查下载任务是否停止下载*/
    private synchronized void checkOtherTaskInfoIsDelete() {
        if (taskInfoList == null) {
            return;
        }
        for (TaskInfo info : taskInfoList) {
            int taskInfoStatus = info.getCurrentStatus();
            if (taskInfoStatus != DownloadInfo.STATUS_DELETE) {
                return;
            }
        }
        /*所有taskinfo都可删除才是真的可以执行删除操作*/
        // TODO: 2020/8/31
        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
        DownloadHelper.deleteFile(downloadConfig.getSaveFile());
        DownloadHelper.get().clearRecord(downloadConfig.getFileDownloadUrl());
        delete();
    }

    /*如果下载任务中某个任务错误，则将其他任务改成error状态*/
    private void checkOtherTaskInfoIsError() {
        if (taskInfoList == null) {
            return;
        }
        for (TaskInfo info : taskInfoList) {
            info.changeStatus(STATUS_ERROR);
        }
        error();
    }

    private long preSaveDownloadRecordTime;
    /*边下载边保存当前下载进度*/
    private void saveDownloadCacheInfo(DownloadRecord downloadRecord) {
        if (downloadRecord == null) {
            return;
        }
        int status = getStatus();
        if (status == STATUS_ERROR) {
            return;
        }
        long nowTime=System.currentTimeMillis();
        if(nowTime-preSaveDownloadRecordTime<1200){
            return;
        }
        preSaveDownloadRecordTime=nowTime;
        DownloadHelper.get().saveRecord(downloadRecord,downloadConfig.getFileDownloadUrl());
    }
    public String getFileDownloadUrl(){
        if(downloadConfig==null){
            return "";
        }
        return downloadConfig.getFileDownloadUrl();
    }

    public DownloadConfig getDownloadConfig() {
        return downloadConfig;
    }
}
