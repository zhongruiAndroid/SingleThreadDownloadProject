package com.github.singlethreaddownload;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.Serializable;

public class DownloadConfig implements Serializable {
    private String unionId;
    /*下载成功的文件*/
    private File saveFile;
    /*下载中的文件*/
    private File tempSaveFile;
    /*重新下载，忽略之前下载的进度*/
    private boolean reDownload;
    /*如果要下载的文件存在，是否删除之前的重新下载*/
    private boolean ifExistAgainDownload;
    /*下载地址*/
    private String fileDownloadUrl;
    /*是否使用url上面的文件名*/
    private boolean useSourceName;
    /*是否需要用到下载速度*/
    private boolean needSpeed;
    /*下载缓冲大小*/
    private int downloadBufferSize;



    protected DownloadConfig(Builder builder) {
        if (builder.saveFile == null) {
            if (TextUtils.isEmpty(builder.downloadFileSavePath)) {
                throw new IllegalStateException("please call setSaveFile() set filePath and fileName");
            } else {
                String fileDownloadUrl = builder.fileDownloadUrl;
                if (TextUtils.isEmpty(fileDownloadUrl)) {
                    throw new IllegalStateException("please call setFileDownloadUrl() set downloadUrl");
                }
                fileDownloadUrl = fileDownloadUrl.split("\\?")[0];
                int index = fileDownloadUrl.lastIndexOf(".");
                /*通过url获取文件后缀*/
                String suffix = fileDownloadUrl.substring(index);
                String fileName = MD5Coder.encode(fileDownloadUrl).substring(8, 24) + suffix;
                if (builder.useSourceName) {
                    index = fileDownloadUrl.lastIndexOf("/");
                    fileName = fileDownloadUrl.substring(index);
                }
                builder.setSaveFile(new File(builder.downloadFileSavePath, fileName));
            }
        } else {
            String fileDownloadUrl = builder.fileDownloadUrl;
            fileDownloadUrl = fileDownloadUrl.split("\\?")[0];
            if (builder.useSourceName && !TextUtils.isEmpty(fileDownloadUrl)) {
                int index = fileDownloadUrl.lastIndexOf("/");
                String fileName = fileDownloadUrl.substring(index);
                if(builder.saveFile.isFile()){
                    builder.setSaveFile(new File(builder.saveFile.getParent(), fileName));
                }else{
                    builder.setSaveFile(new File(builder.saveFile, fileName));
                }
            }
        }

        this.saveFile = builder.saveFile;
        this.tempSaveFile = builder.tempSaveFile;
        this.reDownload = builder.reDownload;
        this.ifExistAgainDownload = builder.ifExistAgainDownload;
        this.fileDownloadUrl = builder.fileDownloadUrl;
        this.useSourceName = builder.useSourceName;

        this.unionId=builder.unionId;
        this.needSpeed = builder.needSpeed;
        this.downloadBufferSize = builder.downloadBufferSize;
    }

    public static class Builder {
        private String unionId;
        private Context context;
        private String downloadFileSavePath;


        /*下载成功的文件*/
        private File saveFile;
        /*下载中的文件*/
        private File tempSaveFile;
        /*重新下载，忽略之前下载的进度*/
        private boolean reDownload;
        /*如果要下载的文件存在，是否删除之前的重新下载*/
        private boolean ifExistAgainDownload;
        /*下载地址*/
        private String fileDownloadUrl;
        /*是否使用url上面的文件名*/
        private boolean useSourceName;
        /*是否需要用到下载速度*/
        private boolean needSpeed;

        /*下载缓冲大小*/
        private int downloadBufferSize;


        public Builder() {
            context = FileDownloadManager.getContext();
            boolean sdCardCanReadAndWrite = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            File useDownloadFile = context.getExternalFilesDir("download");
            if (sdCardCanReadAndWrite && useDownloadFile != null) {
                downloadFileSavePath = useDownloadFile.getAbsolutePath();
            } else {
                downloadFileSavePath = context.getFilesDir() + File.separator + "download";
            }
        }

        public Builder setSaveFile(File saveFile) {
            if (saveFile != null && !saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            this.saveFile = saveFile;
            this.tempSaveFile=createTempSaveFileBySaveFile(saveFile);
            return this;
        }

        public Builder setSaveFile(String filePath, String fileName) {
            setSaveFile(new File(filePath, fileName));
            return this;
        }

        public Builder setIfExistAgainDownload(boolean ifExistAgainDownload) {
            this.ifExistAgainDownload = ifExistAgainDownload;
            return this;
        }

        public Builder setFileDownloadUrl(String fileDownloadUrl) {
            this.fileDownloadUrl = fileDownloadUrl;
            return this;
        }

        public Builder setUseSourceName(boolean useSourceName) {
            this.useSourceName = useSourceName;
            return this;
        }



        public Builder setNeedSpeed(boolean needSpeed) {
            this.needSpeed = needSpeed;
            return this;
        }

        public Builder setReDownload(boolean reDownload) {
            this.reDownload = reDownload;
            return this;
        }
        public void setUnionId(String unionId) {
            this.unionId = unionId;
        }

        public DownloadConfig build() {
            DownloadConfig downloadConfig = new DownloadConfig(this);
            return downloadConfig;
        }

        public void setDownloadBufferSize(int downloadBufferSize) {
            this.downloadBufferSize = downloadBufferSize;
        }
    }

    public File getSaveFile() {
        return saveFile;
    }

    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
        this.tempSaveFile=createTempSaveFileBySaveFile(saveFile);
        if(TextUtils.isEmpty(unionId)){
            unionId=getSaveFile().getAbsolutePath().hashCode()+"";
        }
    }
    public static File createTempSaveFileBySaveFile(File saveFile) {
        String name = saveFile.getName();
        String substring = name.substring(0, name.lastIndexOf("."));
        File tempSaveFile = new File(saveFile.getParent(), substring + ".temp");
        return tempSaveFile;
    }
    public File getTempSaveFile() {
        return tempSaveFile;
    }

    public boolean isReDownload() {
        return reDownload;
    }

    public boolean isIfExistAgainDownload() {
        return ifExistAgainDownload;
    }

    public String getFileDownloadUrl() {
        return fileDownloadUrl;
    }

    public boolean isUseSourceName() {
        return useSourceName;
    }

    public boolean isNeedSpeed() {
        return needSpeed;
    }



    public String getUnionId() {
        if(TextUtils.isEmpty(unionId)){
            unionId=getSaveFile().getAbsolutePath().hashCode()+"";
        }
        return unionId;
    }

    public int getDownloadBufferSize() {
        if(downloadBufferSize<20480){
            downloadBufferSize=20480;
        }
        return downloadBufferSize;
    }
}
