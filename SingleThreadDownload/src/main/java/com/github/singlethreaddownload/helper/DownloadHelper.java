package com.github.singlethreaddownload.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import com.github.singlethreaddownload.FileDownloadManager;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadHelper {
    /**********************************************************/
    private static DownloadHelper singleObj;
    private SharedPreferences sp;

    private DownloadHelper() {
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }

    public static DownloadHelper get() {
        if (singleObj == null) {
            synchronized (DownloadHelper.class) {
                if (singleObj == null) {
                    singleObj = new DownloadHelper();
                }
            }
        }
        return singleObj;
    }

    /**********************************************************/

    private Handler handler;
    private ExecutorService executorService;

    public Handler getHandler() {
        return handler;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFile(File file) {
        if (file == null) {
            return;
        }
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    int length = files.length;
                    for (int i = 0; i < length; i++) {
                        deleteFile(files[i]);
                    }
                }
            }
            file.delete();
        }
    }

    private final String sp_file_name = "zr_single_download_sp";

    public DownloadRecord getRecord(String unionId) {
        return getRecord(sp_file_name, unionId);
    }

    public DownloadRecord getRecord(String spName, String unionId) {
        if (TextUtils.isEmpty(spName)) {
            spName = sp_file_name;
        }
        SharedPreferences sp = FileDownloadManager.getContext().getSharedPreferences(spName, Context.MODE_PRIVATE);
        String downloadRecord = sp.getString(unionId, null);
        return DownloadRecord.fromJson(downloadRecord);
    }
    public Map<String,DownloadRecord> getAllRecord( ) {
        return getAllRecord(sp_file_name);
    }
    public Map<String,DownloadRecord> getAllRecord(String spName) {
        if (TextUtils.isEmpty(spName)) {
            spName = sp_file_name;
        }
        SharedPreferences sp = FileDownloadManager.getContext().getSharedPreferences(spName, Context.MODE_PRIVATE);
        Map<String, String> all = (Map<String, String>) sp.getAll();
        Map<String,DownloadRecord> map=new HashMap();
        if(all!=null){
            for(Map.Entry<String, String> item:all.entrySet()){
                String key = item.getKey();
                DownloadRecord downloadRecord = DownloadRecord.fromJson(item.getValue());
                map.put(key,downloadRecord);
            }
        }
        return map;
    }

    public void saveRecord(DownloadRecord downloadRecord) {
        saveRecord(sp_file_name, downloadRecord);
    }

    public void saveRecord(String spName, DownloadRecord downloadRecord) {
        if (TextUtils.isEmpty(spName)) {
            spName = sp_file_name;
        }
        if (downloadRecord == null || TextUtils.isEmpty(downloadRecord.getSaveFilePath())) {
            return;
        }
        String json = downloadRecord.toJson();
        if (sp == null) {
            sp = FileDownloadManager.getContext().getSharedPreferences(spName, Context.MODE_PRIVATE);
        }
        sp.edit().putString(downloadRecord.getUniqueId(), json).commit();
    }

    public void clearRecordByUnionId(String unionId) {
        clearRecordByUnionId(sp_file_name,unionId);
    }

    public void clearRecordByUnionId(String spName,String unionId) {
        if (TextUtils.isEmpty(spName)) {
            spName = sp_file_name;
        }
        if (TextUtils.isEmpty(unionId)) {
            return;
        }
        if (sp == null) {
            sp = FileDownloadManager.getContext().getSharedPreferences(spName, Context.MODE_PRIVATE);
        }
        sp.edit().remove(unionId).commit();
    }

    public static boolean hasFreeSpace(Context context, long downloadSize) {
        if (context == null || downloadSize <= 0) {
            return true;
        }
        File externalCacheDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            externalCacheDir = context.getExternalCacheDir();
        }
        if (externalCacheDir != null) {
            return externalCacheDir.getFreeSpace() > downloadSize;
        }
        externalCacheDir = context.getFilesDir();
        return externalCacheDir.getFreeSpace() > downloadSize;
    }

    public Pair<Long, Long> getProgressByUnionId(String unionId) {
        DownloadRecord record = getRecord(unionId);
        if (record == null || record.getFileSize() <= 0) {
            return new Pair(new Long(0), new Long(0));
        }
        return new Pair(new Long(record.getDownloadLength()), new Long(record.getFileSize()));
    }

    /*重新下载时重命名*/
    public static File reDownloadAndRename(File saveFile, int reNum) {
        String parent = saveFile.getParent();
        String name = saveFile.getName();
        String newName = name.replace(".", "(" + reNum + ").");
        File newFile = new File(parent, newName);
        if (!newFile.exists()) {
            return newFile;
        } else {
            return reDownloadAndRename(saveFile, reNum + 1);
        }
    }
}
