package com.github.singlethreaddownload;

import android.text.TextUtils;
import android.util.Log;

import com.github.singlethreaddownload.helper.DownloadHelper;
import com.github.singlethreaddownload.helper.DownloadRecord;
import com.github.singlethreaddownload.listener.FileDownloadListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

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


    private int status;

    public DownloadInfo(DownloadConfig config, FileDownloadListener listener) {
        this.downloadConfig = config;
        this.downloadListener = listener;
        status = 0;
    }

    public FileDownloadListener getDownloadListener() {
        if (downloadListener == null) {
            downloadListener = new FileDownloadListener() {
                @Override
                public boolean onRepeatDownload() {
                    return false;
                }
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
        this.status=status;
    }

    public int getStatus() {
        return status;
    }

    public void pauseDownload() {
        changeStatus(STATUS_PAUSE);
    }

    public void deleteDownload() {
        changeStatus(STATUS_DELETE);
    }

    private void changeStatus(int changeStatus) {
        if (getStatus() == STATUS_SUCCESS) {
            return;
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
            DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(),downloadConfig.getUnionId());
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
        if (downloadRecord != null) {
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

    private void progress(final long downloadSize) {
        // TODO: 2021/2/24 按照规则将内存数据保存到本地
//        saveDownloadCacheInfo(downloadRecord);
        //计算网速
        if (downloadConfig.isNeedSpeed()) {
            long nowTime = System.currentTimeMillis();
            if (preTime <= 0) {
                tempDownloadSize = downloadSize;
                preTime = nowTime;
            }
            long timeInterval = nowTime - preTime;
            if (timeInterval >= tempTimeInterval) {
                tempTimeInterval = 1000;
                final float speedBySecond = (downloadSize - tempDownloadSize) * 1000f / timeInterval;
                preTime = nowTime;
                tempDownloadSize = downloadSize;
                DownloadHelper.get().getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        getDownloadListener().onSpeed(Float.parseFloat(String.format("%.1f", speedBySecond)));
                    }
                });
            }
        }
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onProgress(downloadSize, totalSize);
            }
        });
    }

    private long getContentLength(HttpURLConnection httpURLConnection) {
        String value = httpURLConnection.getHeaderField("content-length");
        return Long.parseLong(value);
    }

    public void download() {
        if (downloadConfig == null) {
            getDownloadListener().onError();
        }
        if (getStatus() == STATUS_CONNECT||getStatus() == STATUS_PROGRESS) {
            return;
        }
        DownloadHelper.get().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                downloadByChildThread();
            }
        });
    }
    /*如果存在相同的下载任务，需要询问是否重复下载，而且需要变更文件下载路径和unionId*/
    private void repeatDownloadAndReUnionId(DownloadConfig downloadConfig,int num ){
        DownloadRecord record = DownloadHelper.get().getRecord(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
        if(record!=null&&record.getFileSize()>0){
            if(num==1){
                /*只需要询问一次是否重复下载*/
                boolean flag = downloadListener.onRepeatDownload();
                if(flag){
                    /*如果存在相同的下载任务，则重复下载*/
                    downloadConfig.setSaveFile(DownloadHelper.reDownloadAndRename(downloadConfig.getSaveFile(), num));
                    downloadConfig.setUnionId(downloadConfig.getSaveFile().getAbsoluteFile().hashCode()+"");
                    repeatDownloadAndReUnionId(downloadConfig,num+1);
                }else{
                    return;
                }
            }else{
                downloadConfig.setSaveFile(DownloadHelper.reDownloadAndRename(downloadConfig.getSaveFile(), num));
                downloadConfig.setUnionId(downloadConfig.getSaveFile().getAbsoluteFile().hashCode()+"");
                repeatDownloadAndReUnionId(downloadConfig,num+1);
            }
        }else{
            return;
        }
    }
    private void downloadByChildThread() {
        if(getStatus()==0){
            /*开始下载时，需要判断是否存在其他相同的下载任务*/
            repeatDownloadAndReUnionId(downloadConfig,1);
        }

        String fileUrl = downloadConfig.getFileDownloadUrl();
        if (TextUtils.isEmpty(fileUrl)) {
            DownloadHelper.get().getHandler().post(new Runnable() {
                @Override
                public void run() {
                    getDownloadListener().onError();
                }
            });
            return;
        }
        reset();
        /*下载完成后需要保存的文件*/
        File saveFile = downloadConfig.getSaveFile();
        /*如果存在已下载完成的文件*/
        if (saveFile != null && saveFile.exists() && saveFile.isFile()) {
            if (downloadConfig.isIfExistAgainDownload()) {
                downloadConfig.setSaveFile(DownloadHelper.reDownloadAndRename(saveFile, 1));
            } else {
                /*如果本地已存在下载的文件，直接返回*/
                long length = saveFile.length();
                connect(length);
                progress(length);
                success(saveFile);
                return;
            }
        }

        /*先判断内存是否存在数据，不存在再读取本地缓存配置*/
        if (DownloadRecord.isEmpty(downloadRecord)) {
            downloadRecord = DownloadHelper.get().getRecord(downloadConfig.getDownloadSPName(),getDownloadConfig().getUnionId());
        }
        /*如果没有下载记录，那么需要删除之前已经下载的临时文件*/
        long startPoint = 0;
        /*或者如果需要重新下载，忽略之前的下载进度*/
        if (DownloadRecord.isEmpty(downloadRecord) || (downloadConfig != null && downloadConfig.isReDownload())) {
            DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
            DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(),downloadConfig.getUnionId());
            downloadRecord = null;

            startPoint = 0;
        } else {
            startPoint = downloadRecord.getDownloadLength() - 1;
            if (startPoint < 0) {
                startPoint = 0;
            }
        }
        /*如果本地有下载记录，但是下载一部分的本地文件已经不存在了*/
        if (startPoint > 0) {
            if (downloadConfig.getTempSaveFile() != null && !downloadConfig.getTempSaveFile().exists()) {
                DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(),downloadConfig.getUnionId());
                downloadRecord = null;
                startPoint = 0;
            }
        }

        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        // 随机访问文件，可以指定断点续传的起始位置
        BufferedInputStream bis = null;
        RandomAccessFile randomAccessFile = null;
        try {
            URL url = new URL(fileUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestProperty("Range", "bytes=" + startPoint + "-");
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            long contentLength = getContentLength(httpURLConnection) + startPoint;
            Log.i("=====", "====contentLength=" + contentLength);
            connect(contentLength);
            if (contentLength <= 0) {
                error();
                return;
            }
            if (!DownloadHelper.hasFreeSpace(FileDownloadManager.getContext(), contentLength)) {
                //储存空间不足
                error();
                return;
            }
            /*如果首次下载*/
            if (downloadRecord == null || downloadRecord.getFileSize() <= 0) {
                downloadRecord = new DownloadRecord(contentLength, downloadConfig.getUnionId());
                downloadRecord.setFileSize(contentLength);
                downloadRecord.setDownloadLength(0);
                downloadRecord.setDownloadUrl(downloadConfig.getFileDownloadUrl());
                downloadRecord.setSaveFilePath(downloadConfig.getSaveFile().getAbsolutePath());
                downloadRecord.setUniqueId(downloadConfig.getUnionId());
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                /*不支持范围下载*/
                /*从头下载*/
                /*如果本地存在之前下载一部分的文件，先删除*/
                DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
                DownloadHelper.deleteFile(getDownloadConfig().getSaveFile());
            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                /*支持范围下载*/
                // TODO: 2021/2/24
                /*需要验证之前下载的文件，继续断点下载时，文件是否变更*/
            }
            inputStream = httpURLConnection.getInputStream();
            byte[] buff = new byte[getDownloadConfig().getDownloadBufferSize()];
            int len = 0;
            bis = new BufferedInputStream(inputStream);
            randomAccessFile = new RandomAccessFile(downloadConfig.getTempSaveFile(), "rw");
            randomAccessFile.seek(startPoint);
            while ((len = bis.read(buff)) != -1) {
                downloadRecord.addDownloadLength(len);
                randomAccessFile.write(buff, 0, len);
                progress(downloadRecord.getDownloadLength());
                Log.i("=====", "====progress=" + downloadRecord.getDownloadLength());
                if (getStatus() == STATUS_PAUSE) {
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            error();
        } finally {
            DownloadHelper.close(randomAccessFile);
            DownloadHelper.close(bis);
            DownloadHelper.close(inputStream);
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        /*下载完成重命名文件*/
        getDownloadConfig().getTempSaveFile().renameTo(downloadConfig.getSaveFile());
        success(downloadConfig.getSaveFile());
    }

    private long preSaveDownloadRecordTime;

    /*边下载边保存当前下载进度*/
    private void saveDownloadCacheInfo(DownloadRecord downloadRecord) {
        if (downloadRecord == null) {
            return;
        }
        DownloadHelper.get().saveRecord(downloadConfig.getDownloadSPName(),downloadRecord);
    }

    public String getFileDownloadUrl() {
        if (downloadConfig == null) {
            return "";
        }
        return downloadConfig.getFileDownloadUrl();
    }

    public DownloadConfig getDownloadConfig() {
        return downloadConfig;
    }
}
