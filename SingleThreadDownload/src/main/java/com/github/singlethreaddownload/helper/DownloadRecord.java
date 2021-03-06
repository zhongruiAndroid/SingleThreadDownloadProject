package com.github.singlethreaddownload.helper;

import android.text.TextUtils;

import com.github.singlethreaddownload.DownloadConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class DownloadRecord implements Serializable {
    /*需要下载的文件长度*/
    private long fileSize;
    /*该文件已经下载的长度*/
    private volatile long downloadLength;
    /*下载地址*/
    private String downloadUrl;
    /*保存路径*/
    private String saveFilePath;
    /*下载任务的唯一标识*/
    private String uniqueId;
    private String lastModified;
    private String eTag;

    public static String createUIDByFilePath(String saveFilePath){
        if(TextUtils.isEmpty(saveFilePath)){
            return "";
        }
        return saveFilePath.hashCode()+"";
    }
    /*第一次初始化下载*/
    /*从缓存获取数据*/
    public DownloadRecord(long fileSize, String uniqueId) {
        this.fileSize = fileSize;
        this.uniqueId = uniqueId;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
        if(TextUtils.isEmpty(uniqueId)&&!TextUtils.isEmpty(getDownloadUrl())){
            this.uniqueId=getDownloadUrl().hashCode()+"";
        }
    }

    public String getUniqueId() {
        if (TextUtils.isEmpty(uniqueId)) {
            uniqueId = getDownloadUrl().hashCode()+"";
        }
        return uniqueId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        if(TextUtils.isEmpty(uniqueId)&&!TextUtils.isEmpty(downloadUrl)){
            uniqueId=downloadUrl.hashCode()+"";
        }
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String geteTag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public long getDownloadLength() {
        return downloadLength;
    }

    public void setDownloadLength(long downloadLength) {
        this.downloadLength = downloadLength;
    }
    public void addDownloadLength(long downloadLength) {
        this.downloadLength = this.downloadLength+downloadLength;
    }

    public String getSaveFilePath() {
        return saveFilePath;
    }

    public void setSaveFilePath(String saveFilePath) {
        this.saveFilePath = saveFilePath;
    }
    public boolean isCompleteDownload(){
        return this.fileSize==this.downloadLength&&this.fileSize>0;
    }

    public static DownloadRecord fromJson(String json) {
        DownloadRecord downloadRecord;
        if (TextUtils.isEmpty(json)) {
            downloadRecord = new DownloadRecord(0,"");
            return downloadRecord;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            long fileSize = jsonObject.optLong("fileSize");
            long downloadLength = jsonObject.optLong("downloadLength");
            String downloadUrl = jsonObject.optString("downloadUrl");
            String lastModified = jsonObject.optString("lastModified");
            String eTag = jsonObject.optString("eTag");
            String saveFilePath = jsonObject.optString("saveFilePath");
            String uniqueId = jsonObject.optString("uniqueId");

            downloadRecord = new DownloadRecord(fileSize, uniqueId);
            downloadRecord.fileSize=fileSize;
            if(downloadLength>0){
                /*因为断点下载的起始位置减一，相应的已经下载的长度也要减一*/
                downloadLength=downloadLength-1;
            }
            downloadRecord.downloadLength=downloadLength;
            downloadRecord.downloadUrl=downloadUrl;
            downloadRecord.lastModified=lastModified;
            downloadRecord.eTag=eTag;
            downloadRecord.saveFilePath=saveFilePath;
            downloadRecord.uniqueId=uniqueId;

        } catch (JSONException e) {
            e.printStackTrace();
            downloadRecord = new DownloadRecord(0, "");
        }
        return downloadRecord;
    }
    public static boolean isEmpty(DownloadRecord downloadRecord){
        return downloadRecord==null||downloadRecord.getFileSize()<=0;
    }


    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fileSize", getFileSize());
            jsonObject.put("downloadLength", getDownloadLength());
            jsonObject.put("downloadUrl", getDownloadUrl());
            jsonObject.put("lastModified", getLastModified());
            jsonObject.put("eTag", geteTag());
            jsonObject.put("saveFilePath", getSaveFilePath());
            jsonObject.put("uniqueId", getUniqueId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
