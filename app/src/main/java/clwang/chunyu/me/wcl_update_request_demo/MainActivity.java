package clwang.chunyu.me.wcl_update_request_demo;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.File;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DEBUG-WCL: " + MainActivity.class.getSimpleName();
    private static final String APP_NAME = "Ped_android";
    private static final String VERSION = "1.5.0";
    private static final String DOWNLOAD_APK_ID = "main_activity.download_apk_id";

    private long mDownloadApkId;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                long downloadApkId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                installApk(downloadApkId);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UpdateService updateService =
                ServiceFactory.createServiceFrom(UpdateService.class, UpdateService.ENDPOINT);

        updateService.getUpdateInfo(APP_NAME, VERSION)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showInfo);
    }

    @Override protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    // 显示信息
    private void showInfo(UpdateInfo updateInfo) {
        Log.e(TAG, "下载信息 - " + updateInfo.toString());
        if (isDownloadManagerAvailable()) {
            downloadApk("春雨计步器", updateInfo.data.description, updateInfo.data.appURL);
        }
    }

    // 最小版本号大于9
    private boolean isDownloadManagerAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    // 下载Apk
    private void downloadApk(String title, String description, String apkUrl) {
        apkUrl = apkUrl.trim(); // 去掉首尾空格

        if (!apkUrl.startsWith("http")) {
            apkUrl = "http://" + apkUrl; // 添加Http信息
        }

        DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(Uri.parse(apkUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        request.setTitle(title);
        request.setDescription(description);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "chunyu_pedometer");
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        mDownloadApkId = manager.enqueue(request);
    }

    // 安装Apk
    private void installApk(long downloadApkId) {
        if (downloadApkId == mDownloadApkId) {
            DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Intent install = new Intent(Intent.ACTION_VIEW);
            Uri downloadFileUri = dManager.getUriForDownloadedFile(downloadApkId);
            install.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        }
    }
}
