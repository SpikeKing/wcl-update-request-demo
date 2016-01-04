package clwang.chunyu.me.wcl_update_request_demo;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DEBUG-WCL: " + MainActivity.class.getSimpleName();
    private static final String APP_NAME = "Ped_android";
    private static final String VERSION = "1.9.0";
    private static final String INFO_NAME = "春雨计步器";

    @Bind(R.id.main_b_install_apk) Button mBInstallApk;

    private long mDownloadApkId;
    private UpdateCallback mUpdateCallback;

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
        ButterKnife.bind(this);

        mUpdateCallback = new UpdateCallback() {
            @Override public void onSuccess(UpdateInfo updateInfo) {
                Toast.makeText(MainActivity.this, "有更新", Toast.LENGTH_SHORT).show();
                downloadApk(updateInfo);
            }

            @Override public void onError() {
                Toast.makeText(MainActivity.this, "无更新", Toast.LENGTH_SHORT).show();
            }
        };

        mBInstallApk.setOnClickListener(this::checkUpdate);
    }

    @Override protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }


    // 检测更新
    public void checkUpdate(View view) {
        UpdateService updateService =
                ServiceFactory.createServiceFrom(UpdateService.class, UpdateService.ENDPOINT);

        String version = VERSION;
        updateService.getUpdateInfo(APP_NAME, version)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onNext, this::onError);
    }

    // 显示信息
    private void onNext(UpdateInfo updateInfo) {
        if (updateInfo.error_code != 0 || updateInfo.data == null ||
                updateInfo.data.appURL == null) {
            mUpdateCallback.onError(); // 失败
        } else {
            mUpdateCallback.onSuccess(updateInfo);
        }
    }

    // 错误信息
    private void onError(Throwable throwable) {
        mUpdateCallback.onError();
    }

    // 最小版本号大于9
    private boolean isDownloadManagerAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    // 下载Apk
    private void downloadApk(UpdateInfo updateInfo) {
        if (!isDownloadManagerAvailable()) {
            return;
        }

        String title = INFO_NAME;
        String description = updateInfo.data.description;
        String appUrl = updateInfo.data.appURL;

        appUrl = appUrl.trim(); // 去掉首尾空格

        if (!appUrl.startsWith("http")) {
            appUrl = "http://" + appUrl; // 添加Http信息
        }
        Log.e(TAG, "appUrl: " + appUrl);

        DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(Uri.parse(appUrl));
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

    // 错误回调
    public interface UpdateCallback {
        void onSuccess(UpdateInfo updateInfo);

        void onError();
    }
}
