package com.test.singlethreaddownloadproject;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.github.singlethreaddownload.DownloadConfig;
import com.github.singlethreaddownload.DownloadInfo;
import com.github.singlethreaddownload.FileDownloadManager;
import com.github.singlethreaddownload.helper.DownloadHelper;
import com.github.singlethreaddownload.helper.DownloadRecord;
import com.github.singlethreaddownload.listener.FileDownloadListener;

import java.io.File;

public class TestActivity extends AppCompatActivity implements View.OnClickListener {

    TextView tvFileSize;
    TextView tvResult;
    TextView tvProgress;
    TextView tvSpeed;

    Button btStart;
    Button btPause;
    Button btDelete;
    private DownloadInfo download;

    private ProgressBar pbProgress;

    private AppCompatCheckBox cb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //只需要初始化一次，建议放在application中初始化
        FileDownloadManager.init(getApplication());


        setContentView(R.layout.activity_test);

        cb = findViewById(R.id.cb);
        pbProgress = findViewById(R.id.pbProgress);
        tvFileSize = findViewById(R.id.tvFileSize);
        tvResult = findViewById(R.id.tvResult);
        tvProgress = findViewById(R.id.tvProgress);
        tvSpeed = findViewById(R.id.tvSpeed);

        btStart = findViewById(R.id.btStart);
        btPause = findViewById(R.id.btPause);
        btDelete = findViewById(R.id.btDelete);

        btStart.setOnClickListener(this);
        btPause.setOnClickListener(this);
        btDelete.setOnClickListener(this);

        DownloadRecord record = DownloadHelper.get().getRecord(nbyUrl.hashCode() + "");
        if (!DownloadRecord.isEmpty(record)) {
            pbProgress.setMax((int) record.getFileSize());
            pbProgress.setProgress((int) record.getDownloadLength());

            tvProgress.setText(record.getDownloadLength() + "/" + record.getFileSize());
        }
    }

//    public static final String nbyUrl = "https://raw.githubusercontent.com/zhongruiAndroid/TestRxBus/master/aaaaa.apk";
    public static final String nbyUrl = "https://b4fc69b7b91b11258cf93c80ebe77d53.dd.cdntips.com/imtt.dd.qq.com/16891/apk/FBAF111EE8D5AE9810A79EFA794901AA.apk?mkey=5f0db2308ccf356e&f=9870&fsname=cn.nubia.nubiashop_1.6.3.1021_77.apk&csr=1bbd&cip=140.207.19.155&proto=https";

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btStart:
                startDownload();
                break;
            case R.id.btPause:
                FileDownloadManager.pauseDownload(download);
                break;
            case R.id.btDelete:
                FileDownloadManager.deleteDownload(download);
                break;
        }
    }

    private long startTime;

    private void startDownload() {
        if (cb.isChecked() && download != null) {
            DownloadHelper.deleteFile(download.getDownloadConfig().getSaveFile());
        }

        DownloadConfig.Builder config = new DownloadConfig.Builder();
        config.setFileDownloadUrl(nbyUrl).setIfExistAgainDownload(cb.isChecked()).setNeedSpeed(true);
        /*如果不需要显示下载速度，FileDownloadManager.download直接传入下载地址即可*/
        startTime = System.currentTimeMillis();
        if(download!=null){
            download.download();
            return;
        }
        final DownloadConfig build = config.build();
        final DownloadConfig downloadConfig = DownloadHelper.checkHasDownloadRecord(build);
        if(downloadConfig!=null){
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setMessage("已经存在相同的下载任务,是否继续下载?");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    startDownload(downloadConfig);
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setNeutralButton("继续下载", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startDownload(build);
                }
            });
            builder.show();
        }else{
            startDownload(build);
        }
    }

    private void startDownload(DownloadConfig downloadConfig) {
        download = FileDownloadManager.download(downloadConfig, new FileDownloadListener() {
            @Override
            public void onConnect(long totalSize ) {
                tvResult.setText("连接中");
                tvFileSize.setText("文件大小:" + (totalSize * 1f / 1014 / 1014) + "mb");
                pbProgress.setMax((int) totalSize);
//                pbProgress.setProgress(0);
            }

            @Override
            public void onSpeed(float speedKbBySecond) {
                tvSpeed.setText("下载速度:" + String.format("%.1f", speedKbBySecond / 1024f) + "kb/s");

            }

            @Override
            public void onProgress(long progress, long totalSize) {
                tvResult.setText("下载中");
                tvProgress.setText(progress + "/" + totalSize);
                pbProgress.setProgress((int) progress);
            }

            @Override
            public void onSuccess(File file) {
                long timeInterval = System.currentTimeMillis() - startTime;
                tvResult.setText("下载完成耗时：" + timeInterval * 1f / 1000 + "s" + "\n文件路径:" + file.getAbsolutePath());
            }

            @Override
            public void onPause() {
                tvResult.setText("暂停下载");
            }

            @Override
            public void onDelete() {
                tvResult.setText("删除文件");
                tvProgress.setText("0/0");
                pbProgress.setProgress(0);
            }

            @Override
            public void onError() {
                tvResult.setText("下载错误");
            }
        });
    }
}