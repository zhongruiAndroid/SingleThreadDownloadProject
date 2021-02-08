package com.github.singlethreaddownload.helper;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DownloadRecord implements Serializable {
    private long fileSize;
    private long downloadLength;
    private String uniqueId;
    private String downloadUrl;

    /*从缓存获取数据*/
    private DownloadRecord(long fileSize, String uniqueId) {
        this.fileSize = fileSize;
        this.uniqueId = uniqueId;
    }

    /*第一次初始化下载*/
    public DownloadRecord(long fileSize) {
        this.fileSize = fileSize;
    }


    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getUniqueId() {
        if (TextUtils.isEmpty(uniqueId)) {
            uniqueId = "";
        }
        return uniqueId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public static DownloadRecord fromJson(String json) {
        DownloadRecord downloadRecord;
        if (TextUtils.isEmpty(json)) {
            downloadRecord = new DownloadRecord(0);
            return downloadRecord;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            long fileSize = jsonObject.optLong("fileSize");
            String uniqueId = jsonObject.optString("uniqueId");
            JSONArray fileRecordList = jsonObject.optJSONArray("fileRecordList");
            downloadRecord = new DownloadRecord(fileSize, uniqueId);
            downloadRecord.fileSize = fileSize;
            if (fileRecordList != null && fileRecordList.length() > 0) {
                for (int i = 0; i < fileRecordList.length(); i++) {
                    JSONObject itemObj = fileRecordList.getJSONObject(i);
                    FileRecord record = new FileRecord();
                    record.setStartPoint(itemObj.optLong("startPoint"));
                    record.setEndPoint(itemObj.optLong("endPoint"));
                    record.setDownloadLength(itemObj.optLong("downloadLength"));
                    downloadRecord.addFileRecordList(record);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            downloadRecord = new DownloadRecord(0, "");
        }
        return downloadRecord;
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fileSize", getFileSize());
            jsonObject.put("uniqueId", getUniqueId());
            JSONArray jsonArray = new JSONArray();
            for (FileRecord fileRecord : getFileRecordList()) {
                JSONObject itemJson = new JSONObject();
                itemJson.put("startPoint", fileRecord.getStartPoint());
                itemJson.put("endPoint", fileRecord.getEndPoint());
                itemJson.put("downloadLength", fileRecord.getDownloadLength());
                jsonArray.put(itemJson);
            }
            jsonObject.put("fileRecordList", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
