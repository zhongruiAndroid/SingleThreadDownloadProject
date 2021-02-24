package com.github.singlethreaddownload.helper;

import android.text.TextUtils;

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
        if(TextUtils.isEmpty(uniqueId)&&!TextUtils.isEmpty(saveFilePath)){
            this.uniqueId=saveFilePath.hashCode()+"";
        }
    }

    public String getUniqueId() {
        if (TextUtils.isEmpty(uniqueId)) {
            uniqueId = getSaveFilePath().hashCode()+"";
        }
        return uniqueId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
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
        if(TextUtils.isEmpty(uniqueId)&&!TextUtils.isEmpty(saveFilePath)){
            uniqueId=saveFilePath.hashCode()+"";
        }
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
            String saveFilePath = jsonObject.optString("saveFilePath");
            String uniqueId = jsonObject.optString("uniqueId");

            downloadRecord = new DownloadRecord(fileSize, uniqueId);
            downloadRecord.fileSize=fileSize;
            downloadRecord.downloadLength=downloadLength;
            downloadRecord.downloadUrl=downloadUrl;
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
            jsonObject.put("saveFilePath", getSaveFilePath());
            jsonObject.put("uniqueId", getUniqueId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
