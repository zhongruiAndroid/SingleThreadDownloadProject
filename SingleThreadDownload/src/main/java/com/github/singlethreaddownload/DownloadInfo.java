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


    public static final int STATUS_ERROR = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_PAUSE = 3;
    public static final int STATUS_DELETE = 4;
    public static final int STATUS_PROGRESS = 5;
    public static final int STATUS_CONNECT = 6;
    public static final int STATUS_REQUEST = 7;


    private int status;
    private AppStateUtils.AppStateChangeListener appStateChangeListener = new AppStateUtils.AppStateChangeListener() {
        @Override
        public void onStateChange(boolean intoFront) {
            notifySaveRecord();
        }
    };

    private void setAppStateChangeListener() {
        AppStateUtils.get().addAppStateChangeListener(this, appStateChangeListener);
    }

    private void removeAppStateChangeListener() {
        AppStateUtils.get().removeAppStateChangeListener(this);
    }

    public DownloadInfo(DownloadConfig config, FileDownloadListener listener) {
        this.downloadConfig = config;
        this.downloadListener = listener;
        status = 0;
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
        this.status = status;
        switch (status) {
            case STATUS_ERROR:
            case STATUS_SUCCESS:
            case STATUS_PAUSE:
            case STATUS_DELETE:
                removeAppStateChangeListener();
                break;
        }
    }

    public int getStatus() {
        return status;
    }

    public void pauseDownload() {
        int status = getStatus();
        if (status == STATUS_PAUSE || status == STATUS_SUCCESS) {
            return;
        }
        setStatus(STATUS_PAUSE);
        /*手动暂停时把内存的缓存信息保存至本地*/
        saveDownloadCacheInfo(downloadRecord);
        /*此时如果执行回调的代码，可能还在执行下载的操作，把状态给覆盖掉*/
    }

    public void deleteDownload() {
        deleteDownload(false);
    }

    public void deleteDownload(boolean deleteTaskAndFile) {
        int status = getStatus();
        if (status == STATUS_DELETE) {
            return;
        }
        setStatus(STATUS_DELETE);
        if (deleteTaskAndFile) {
            DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
            DownloadHelper.deleteFile(getDownloadConfig().getSaveFile());
        } else {
            /*如果不删除源文件，继续保留下载记录*/
            saveDownloadCacheInfo(downloadRecord);
        }

    }


    private void error() {
        if (downloadConfig != null) {
            DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
            DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
        }
        setStatus(STATUS_ERROR);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onError();
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
    }

    private void progress(final long downloadSize) {
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
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return -1;
        }
    }

    public void download() {
        if (downloadConfig == null) {
            getDownloadListener().onError();
            return;
        }
        String fileUrl = downloadConfig.getFileDownloadUrl();
        if (TextUtils.isEmpty(fileUrl)) {
            getDownloadListener().onError();
            return;
        }
        if (getStatus() == STATUS_CONNECT || getStatus() == STATUS_PROGRESS || getStatus() == STATUS_REQUEST) {
            return;
        }
        DownloadHelper.get().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                downloadByChildThread();
            }
        });
    }

    private void downloadByChildThread() {
        setAppStateChangeListener();

        reset();
        /*下载完成后需要保存的文件*/
        File saveFile = downloadConfig.getSaveFile();
        if (!saveFile.getParentFile().exists()) {
            saveFile.getParentFile().mkdirs();
        }
        /*如果存在已下载完成的文件*/
        if (saveFile != null && saveFile.exists() && saveFile.isFile()) {
            if (downloadConfig.isIfExistAgainDownload()) {
                DownloadHelper.deleteFile(saveFile);
//                downloadConfig.setSaveFile(DownloadHelper.reDownloadAndRename(saveFile, 1));
            } else {
                /*如果本地已存在下载的文件，直接返回*/
                long length = saveFile.length();
                connect(length);
                progress(length);
                success(saveFile);
                return;
            }
        }

        /*先判断内存是否存在数据，不存在再读取本地缓存配置,用于[下载-暂停-再下载]流程*/
        if (DownloadRecord.isEmpty(downloadRecord)) {
            downloadRecord = DownloadHelper.get().getRecord(downloadConfig.getDownloadSPName(), getDownloadConfig().getUnionId());
        }
        /*如果没有下载记录，那么需要删除之前已经下载的临时文件*/
        long startPoint = 0;
        /*或者如果需要重新下载，忽略之前的下载进度*/
        if (DownloadRecord.isEmpty(downloadRecord) || (downloadConfig != null && downloadConfig.isReDownload())) {
            DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
            DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
            downloadRecord = null;

            startPoint = 0;
        } else {
            downloadRecord.setDownloadLength(downloadRecord.getDownloadLength() - 1);
            /*因为断点下载的起始位置减一，相应的已经下载的长度也要减一*/
            startPoint = downloadRecord.getDownloadLength();
            if (startPoint < 0) {
                startPoint = 0;
            }
        }
        /*如果本地有下载记录，但是下载一部分的本地文件已经不存在了*/
        if (startPoint > 0) {
            if (downloadConfig.getTempSaveFile() != null && !downloadConfig.getTempSaveFile().exists()) {
                DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
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
            setStatus(STATUS_REQUEST);
            URL url = new URL(downloadConfig.getFileDownloadUrl());
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestProperty("Range", "bytes=" + startPoint + "-");
            /*使用If-Range遇到每次都会返回200的情况*/
            /*使用If-Match遇到经常返回412的情况*/
            /*鉴于以上两种情况，采用手动比较etag或者last-modified来维护是否需要重新下载文件的方案*/
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            String eTag = httpURLConnection.getHeaderField("ETag");
            String lastModified = httpURLConnection.getHeaderField("Last-Modified");
            Log.i("=====", responseCode + "====lastModified=" + lastModified + "====eTag=" + eTag);
            long contentLengthLong = getContentLength(httpURLConnection);
            if (contentLengthLong < 0) {
                /*有可能状态码=200，但是内容长度为null*/
                error();
                return;
            }
            long contentLength = contentLengthLong + startPoint;
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
            /*上次请求的eTag和lastModified,如果和这次请求返回的不一样，则从头开始下载*/
            String preETag=downloadRecord.geteTag();
            String preLastModified=downloadRecord.getLastModified();
            if(!TextUtils.isEmpty(eTag)&&!TextUtils.isEmpty(preETag)&&!TextUtils.equals(eTag,preETag)){
                /*文件被修改*/
                removeAppStateChangeListener();
                DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
                DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
                downloadRecord=null;

                /*因为需要自己调用自己，所以这里提前手动关闭连接*/
                DownloadHelper.close(randomAccessFile);
                DownloadHelper.close(bis);
                DownloadHelper.close(inputStream);
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

                downloadByChildThread();
                return;
            }else if(!TextUtils.isEmpty(lastModified)&&!TextUtils.isEmpty(preLastModified)&&!TextUtils.equals(lastModified,preLastModified)){
                /*文件被修改*/
                removeAppStateChangeListener();
                DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
                DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
                downloadRecord=null;

                /*因为需要自己调用自己，所以这里提前手动关闭连接*/
                DownloadHelper.close(randomAccessFile);
                DownloadHelper.close(bis);
                DownloadHelper.close(inputStream);
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

                downloadByChildThread();
                return;
            }
            if (!TextUtils.isEmpty(eTag)) {
                downloadRecord.seteTag(eTag);
            } else if (!TextUtils.isEmpty(lastModified)) {
                downloadRecord.setLastModified(lastModified);
            }else{
                downloadRecord.seteTag("");
                downloadRecord.setLastModified("");
            }
            /*etag和lastmodified被修改之后，保存至本地*/
            saveDownloadCacheInfo(downloadRecord);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                /*不支持范围下载*/
                /*从头下载*/
                startPoint=0;
                downloadRecord.setDownloadLength(0);
                /*如果本地存在之前下载一部分的文件，先删除*/
                DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
                DownloadHelper.deleteFile(getDownloadConfig().getSaveFile());
            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                /*支持范围下载*/
            } else if (responseCode == HttpURLConnection.HTTP_PRECON_FAILED) {
                /*断点下载文件发生了变化*/
                removeAppStateChangeListener();
                DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
                DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());

                /*因为需要自己调用自己，所以这里提前手动关闭连接*/
                DownloadHelper.close(randomAccessFile);
                DownloadHelper.close(bis);
                DownloadHelper.close(inputStream);
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

                downloadByChildThread();
                return;
            } else {
                error();
                return;
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
                    /*手动暂停时把内存的缓存信息保存至本地，防止暂停时保存信息之后，在return之前又写入了数据*/
                    saveDownloadCacheInfo(downloadRecord);
                    DownloadHelper.get().getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getDownloadListener().onPause();
                        }
                    });
                    return;
                }
                if (getStatus() == STATUS_DELETE) {
                    saveDownloadCacheInfo(downloadRecord);
                    DownloadHelper.get().getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getDownloadListener().onDelete();
                        }
                    });
                    return;
                }
            }
            saveDownloadCacheInfo(downloadRecord);
            /*下载完成重命名文件*/
            getDownloadConfig().getTempSaveFile().renameTo(downloadConfig.getSaveFile());
            success(downloadConfig.getSaveFile());
        } catch (Exception e) {
            saveDownloadCacheInfo(downloadRecord);
            e.printStackTrace();
            error();
            return;
        } finally {
            DownloadHelper.close(randomAccessFile);
            DownloadHelper.close(bis);
            DownloadHelper.close(inputStream);
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    /*保存当前下载进度*/
    private void saveDownloadCacheInfo(DownloadRecord downloadRecord) {
        if (downloadRecord == null) {
            return;
        }
        DownloadHelper.get().saveRecord(downloadConfig.getDownloadSPName(), downloadRecord);
    }

    public void notifySaveRecord() {
        saveDownloadCacheInfo(downloadRecord);
    }

    public DownloadConfig getDownloadConfig() {
        return downloadConfig;
    }
}
