package com.test.singlethreaddownloadproject;


import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatSeekBar;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.singlethreaddownload.DownloadConfig;
import com.github.singlethreaddownload.DownloadInfo;
import com.github.singlethreaddownload.FileDownloadManager;
import com.github.singlethreaddownload.helper.DownloadHelper;
import com.github.singlethreaddownload.listener.FileDownloadListener;

import java.io.File;

public class ApiTestActivity extends AppCompatActivity implements View.OnClickListener {


    private EditText etUrl;
    private Button btClear;
    private Button btPaste;
    private Button bt;
    private ProgressBar pbProgress;
    private TextView tvProgress;
    private TextView tvSpeed;
    private Button btPause;
    private Button btDelete;
    private Button btCopyHW;
    private Button btCopyMZ;
    private Button btCopyRE;
    private Button btCopyNBY;
    private AppCompatCheckBox cbAgainDownload;
    private AppCompatCheckBox cbReDownload;
    private AppCompatCheckBox cbUseUrlSourceName;
    private AppCompatCheckBox cbUseSpeed;
    private TextView tvThreadNum;
    private AppCompatSeekBar sbThreadNum;
    private TextView tvResult;
    private TextView tvFileSize;


    private DownloadInfo downloadInfo;

    private long downloadTime;

    public static final String nbyUrl = "https://b4fc69b7b91b11258cf93c80ebe77d53.dd.cdntips.com/imtt.dd.qq.com/16891/apk/FBAF111EE8D5AE9810A79EFA794901AA.apk?mkey=5f0db2308ccf356e&f=9870&fsname=cn.nubia.nubiashop_1.6.3.1021_77.apk&csr=1bbd&cip=140.207.19.155&proto=https";
    public static final String hwUrl = "https://imtt.dd.qq.com/16891/apk/0F9A4978BE0E05EFBBBAEF535150EEA9.apk?fsname=com.vmall.client_1.9.3.310_10903310.apk&csr=1bbd";
    public static final String mzUrl = "https://imtt.dd.qq.com/16891/apk/25B4FBFEFA567C10993D8528A939E3B3.apk?fsname=com.flyme.meizu.store_4.1.10_4110.apk&csr=1bbd";
    public static final String reUrl = "https://imtt.dd.qq.com/16891/apk/C03FDA4370630805DBE1FFB785A57E6E.apk?fsname=com.speedsoftware.rootexplorer_4.9.6_999496.apk&csr=1bbd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_api_test);

        String hwUrl = "https://imtt.dd.qq.com/16891/apk/0F9A4978BE0E05EFBBBAEF535150EEA9.apk?fsname=com.vmall.client_1.9.3.310_10903310.apk&csr=1bbd";
        long time=System.currentTimeMillis();
        String s = hwUrl.hashCode() + "";
        long time2=System.currentTimeMillis();
        Log.i("=====","====time2="+(time2-time)/1000f);

        FileDownloadManager.init(getApplication());


        initView();
        initData();


        String preDownloadUrl = getStringData("download_url");
        String threadNum = getStringData("threadNum");
        if (!TextUtils.isEmpty(preDownloadUrl)) {

            copy(this, preDownloadUrl);
            etUrl.setText(preDownloadUrl);
            //获取之前下载任务的下载进度以及文件大小
            Pair<Long, Long> progressByUrl = DownloadHelper.get().getProgressByUnionId(preDownloadUrl.hashCode()+"");
            Long second = progressByUrl.second;
            pbProgress.setMax(Integer.valueOf(second + ""));
            pbProgress.setProgress(Integer.valueOf(progressByUrl.first + ""));

            tvFileSize.setText("文件大小:"+(second*1f/1014/1014)+"mb");

            tvProgress.setText(progressByUrl.first + "/" + second);

            bt.setText("继续上次下载");

        } else {
            copy(this, nbyUrl);
            etUrl.setText(nbyUrl);
        }
        if (!TextUtils.isEmpty(threadNum)) {
            int i = Integer.parseInt(threadNum);
            sbThreadNum.setProgress(i);
            tvThreadNum.setText("线程数量：" + i);
        }
    }

    private void initData() {

    }

    private void initView() {


        tvFileSize = findViewById(R.id.tvFileSize);
        etUrl = findViewById(R.id.etUrl);
        btClear = findViewById(R.id.btClear);
        btPaste = findViewById(R.id.btPaste);
        bt = findViewById(R.id.bt);
        pbProgress = findViewById(R.id.pbProgress);
        tvProgress = findViewById(R.id.tvProgress);
        tvSpeed = findViewById(R.id.tvSpeed);
        btPause = findViewById(R.id.btPause);
        btDelete = findViewById(R.id.btDelete);
        btCopyHW = findViewById(R.id.btCopyHW);
        btCopyMZ = findViewById(R.id.btCopyMZ);
        btCopyRE = findViewById(R.id.btCopyRE);
        btCopyNBY = findViewById(R.id.btCopyNBY);
        cbAgainDownload = findViewById(R.id.cbAgainDownload);
        cbReDownload = findViewById(R.id.cbReDownload);
        cbUseUrlSourceName = findViewById(R.id.cbUseUrlSourceName);
        cbUseSpeed = findViewById(R.id.cbUseSpeed);
        tvThreadNum = findViewById(R.id.tvThreadNum);
        sbThreadNum = findViewById(R.id.sbThreadNum);

        tvResult = findViewById(R.id.tvResult);


        bt.setOnClickListener(this);
        btPause.setOnClickListener(this);
        btDelete.setOnClickListener(this);
        btClear.setOnClickListener(this);
        btCopyHW.setOnClickListener(this);
        btCopyMZ.setOnClickListener(this);
        btCopyRE.setOnClickListener(this);
        btCopyNBY.setOnClickListener(this);

        sbThreadNum.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvThreadNum.setText("线程数量："+progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btPause:
                if (downloadInfo != null) {
                    tvResult.setText("暂停下载");
                    downloadInfo.pauseDownload();
                }
                break;
            case R.id.btDelete:
                if (downloadInfo != null) {
                    tvResult.setText("删除下载文件");
                    downloadInfo.deleteDownload(true);
                }
                break;
            case R.id.bt:
                start();
                break;
            case R.id.btClear:
                etUrl.setText("");
                break;
            case R.id.btCopyHW:
                copyUrl(hwUrl);
                break;
            case R.id.btCopyMZ:
                copyUrl(mzUrl);
                break;
            case R.id.btCopyRE:
                copyUrl(reUrl);
                break;
            case R.id.btCopyNBY:
                copyUrl(nbyUrl);
                break;
        }
    }
    private void copyUrl(String url) {
        if (etUrl == null) {
            return;
        }
        etUrl.setText(url);
        copy(this, url);
    }

    private void start() {
        bt.setText("开始下载(下载之前在底下填写下载地址)");
        if (TextUtils.isEmpty(etUrl.getText())) {
            showToast("请填写下载地址");
            return;
        }
        String downloadUrl = etUrl.getText().toString();
        setStringData("download_url", downloadUrl);

        int threadNum = sbThreadNum.getProgress();
        if (threadNum <= 0) {
            sbThreadNum.setProgress(1);
            threadNum = 1;
        }
        setStringData("threadNum", threadNum + "");

        tvResult.setText("开始下载");

        DownloadConfig.Builder config = new DownloadConfig.Builder();

        config.setFileDownloadUrl(downloadUrl);

        config.setNeedSpeed(cbUseSpeed.isChecked());
        config.setIfExistAgainDownload(cbAgainDownload.isChecked());
        config.setReDownload(cbReDownload.isChecked());
        config.setUseSourceName(cbUseUrlSourceName.isChecked());
        if (downloadInfo != null && downloadInfo.getStatus() == DownloadInfo.STATUS_PROGRESS) {
            return;
        }
        downloadInfo = FileDownloadManager.download(config.build(), new FileDownloadListener() {
            @Override
            public boolean onRepeatDownload() {
                return false;
            }

            @Override
            public void onConnect(long totalSize) {
                tvFileSize.setText("文件大小:"+(totalSize*1f/1014/1014)+"mb");
                pbProgress.setMax((int) totalSize);
                tvResult.setText("连接中");
                downloadTime=System.currentTimeMillis();
            }

            @Override
            public void onSpeed(float speedBySecond) {
                tvSpeed.setText("下载速度:" + speedBySecond + "kb/s");
            }

            @Override
            public void onProgress(long progress, long totalSize) {
                tvProgress.setText(progress + "/" + totalSize);
                pbProgress.setProgress((int) progress);
                tvResult.setText("下载中");
            }

            @Override
            public void onSuccess(File file) {
                long timeInterval = System.currentTimeMillis() - downloadTime;
                tvResult.setText("下载完成：(耗时"+timeInterval*1f/1000+"s)" + file.getAbsolutePath());
            }

            @Override
            public void onPause() {
                tvResult.setText("暂停下载");
            }

            @Override
            public void onDelete() {
                tvResult.setText("删除下载文件");
                setStringData("download_url", "");
            }

            @Override
            public void onError() {
                tvResult.setText("下载error");
                setStringData("download_url", "");
            }
        });
    }


    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String getStringData(String key) {
        SharedPreferences preferences = getSharedPreferences("zhongrui_download", Context.MODE_PRIVATE);
        return preferences.getString(key, "");
    }

    private void setStringData(String key, String url) {
        SharedPreferences preferences = getSharedPreferences("zhongrui_download", Context.MODE_PRIVATE);
        preferences.edit().putString(key, url).commit();
    }

    public static void copy(Context ctx, String txt) {
        if (ctx == null || TextUtils.isEmpty(txt)) {
            return;
        }
        ClipboardManager clipboardManager = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("text", txt);
        clipboardManager.setPrimaryClip(clipData);
    }

    public static String paste(Context context) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager.hasPrimaryClip() && clipboardManager.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
            CharSequence text = item.getText();
            if (text == null) {
                return "";
            }
            return text.toString();
        }
        return "";
    }
}
