# wcl-update-request-demo
更新请求的示例, 下载Apk和自动安装. 使用RxJava+Retrofit.

> 欢迎Follow我的[GitHub](https://github.com/SpikeKing).

在应用中, 为了提高用户体验, 会提供更新版本的功能. 那么如何实现呢? 我写了一个简单的Demo, 说明一下, 需要注意几个细节. 使用了Retrofit和Rx处理网络请求.

![更新](http://img.blog.csdn.net/20160107135752597)

# 1. 逻辑
访问服务器, 根据是否包含新版本, 判断是否需要更新.
下载Apk, 下载完成后, 自动安装, 高版本会覆盖低版本.

逻辑:
```
public class MainActivity extends AppCompatActivity {

    private static final String APP_NAME = "Ped_android";
    private static final String VERSION = "1.0.0";
    private static final String INFO_NAME = "计步器";
    private static final String STORE_APK = "chunyu_apk";

    @Bind(R.id.main_b_install_apk) Button mBInstallApk;

    private UpdateAppUtils.UpdateCallback mUpdateCallback; // 更新回调

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mUpdateCallback = new UpdateAppUtils.UpdateCallback() {
            @Override public void onSuccess(UpdateInfo updateInfo) {
                Toast.makeText(MainActivity.this, "有更新", Toast.LENGTH_SHORT).show();
                UpdateAppUtils.downloadApk(MainActivity.this, updateInfo, INFO_NAME, STORE_APK);
            }

            @Override public void onError() {
                Toast.makeText(MainActivity.this, "无更新", Toast.LENGTH_SHORT).show();
            }
        };

        mBInstallApk.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                UpdateAppUtils.checkUpdate(APP_NAME, VERSION, mUpdateCallback);
            }
        });
    }
}
```

``UpdateAppUtils``是核心下载类. 输入App的代号, 版本号, 异步回调, 发送到服务器, 判断是否需要更新.  如果存在新版本, 则下载Apk, 并自动安装更新. 

# 2. 网络请求
更新请求, 参数是App代号和当前版本号.
```
/**
 * 更新服务
 * <p>
 * Created by wangchenlong on 16/1/4.
 */
public interface UpdateService {
    String ENDPOINT = "http://www.chunyuyisheng.com";

    // 获取个人信息
    @GET("/cmsapi/app/update")
    Observable<UpdateInfo> getUpdateInfo(
            @Query("appName") String appName,
            @Query("version") String version);
}
```
创建服务的工厂类.
```
/**
 * 创建Retrofit服务
 * <p>
 * Created by wangchenlong on 16/1/4.
 */
public class ServiceFactory {
    public static <T> T createServiceFrom(final Class<T> serviceClass, String endpoint) {
        Retrofit adapter = new Retrofit.Builder()
                .baseUrl(endpoint)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create()) // 添加Rx适配器
                .addConverterFactory(GsonConverterFactory.create()) // 添加Gson转换器
                .build();
        return adapter.create(serviceClass);
    }
}
```
更新信息的Json类.
```
/**
 * 更新信息(JSON)
 * <p>
 * Created by wangchenlong on 16/1/4.
 */
public class UpdateInfo {
    public Data data; // 信息
    public Integer error_code; // 错误代码
    public String error_msg; // 错误信息

    public static class Data {
        public String curVersion; // 当前版本
        public String appURL; // 下载地址
        public String description; // 描述
        public String minVersion; // 最低版本
        public String appName; // 应用名称
    }

    @Override public String toString() {
        return "当前版本: " + data.curVersion + ", 下载地址: " + data.appURL + ", 描述信息: " + data.description
                + ", 最低版本: " + data.minVersion + ", 应用代称: " + data.appName
                + ", 错误代码: " + error_code + ", 错误信息: " + error_msg;
    }
}
```

# 3. 请求和下载
更新库的主类, 包含``检查更新(checkUpdate)``和``下载Apk(downloadApk)``两个重要方法.
```
/**
 * 更新管理器
 * <p>
 * Created by wangchenlong on 16/1/6.
 */
@SuppressWarnings("unused")
public class UpdateAppUtils {

    @SuppressWarnings("unused")
    private static final String TAG = "DEBUG-WCL: " + UpdateAppUtils.class.getSimpleName();

    /**
     * 检查更新
     */
    @SuppressWarnings("unused")
    public static void checkUpdate(String appCode, String curVersion, UpdateCallback updateCallback) {
        UpdateService updateService =
                ServiceFactory.createServiceFrom(UpdateService.class, UpdateService.ENDPOINT);

        updateService.getUpdateInfo(appCode, curVersion)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(updateInfo -> onNext(updateInfo, updateCallback),
                        throwable -> onError(throwable, updateCallback));
    }

    // 显示信息
    private static void onNext(UpdateInfo updateInfo, UpdateCallback updateCallback) {
        Log.e(TAG, "返回数据: " + updateInfo.toString());
        if (updateInfo.error_code != 0 || updateInfo.data == null ||
                updateInfo.data.appURL == null) {
            updateCallback.onError(); // 失败
        } else {
            updateCallback.onSuccess(updateInfo);
        }
    }

    // 错误信息
    private static void onError(Throwable throwable, UpdateCallback updateCallback) {
        updateCallback.onError();
    }

    /**
     * 下载Apk, 并设置Apk地址,
     * 默认位置: /storage/sdcard0/Download
     *
     * @param context    上下文
     * @param updateInfo 更新信息
     * @param infoName   通知名称
     * @param storeApk   存储的Apk
     */
    @SuppressWarnings("unused")
    public static void downloadApk(
            Context context, UpdateInfo updateInfo,
            String infoName, String storeApk
    ) {
        if (!isDownloadManagerAvailable()) {
            return;
        }

        String description = updateInfo.data.description;
        String appUrl = updateInfo.data.appURL;

        if (appUrl == null || appUrl.isEmpty()) {
            Log.e(TAG, "请填写\"App下载地址\"");
            return;
        }

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
        request.setTitle(infoName);
        request.setDescription(description);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, storeApk);

        Context appContext = context.getApplicationContext();
        DownloadManager manager = (DownloadManager)
                appContext.getSystemService(Context.DOWNLOAD_SERVICE);

        // 存储下载Key
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(appContext);
        sp.edit().putLong(PrefsConsts.DOWNLOAD_APK_ID_PREFS, manager.enqueue(request)).apply();
    }

    // 最小版本号大于9
    private static boolean isDownloadManagerAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    // 错误回调
    public interface UpdateCallback {
        void onSuccess(UpdateInfo updateInfo);

        void onError();
    }
}
```
检查更新: 创建服务, 在新线程中发送请求, 在主线程中接收数据, 判断成功和失败.
```
    /**
     * 检查更新
     */
    @SuppressWarnings("unused")
    public static void checkUpdate(String appCode, String curVersion, UpdateCallback updateCallback) {
        UpdateService updateService =
                ServiceFactory.createServiceFrom(UpdateService.class, UpdateService.ENDPOINT);

        updateService.getUpdateInfo(appCode, curVersion)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(updateInfo -> onNext(updateInfo, updateCallback),
                        throwable -> onError(throwable, updateCallback));
    }
```

下载Apk: 转换和解析Url, 设置通知信息和存储位置, 存储下载Id, 自动安装更新.
```
    /**
     * 下载Apk, 并设置Apk地址,
     * 默认位置: /storage/sdcard0/Download
     *
     * @param context    上下文
     * @param updateInfo 更新信息
     * @param infoName   通知名称
     * @param storeApk   存储的Apk
     */
    @SuppressWarnings("unused")
    public static void downloadApk(
            Context context, UpdateInfo updateInfo,
            String infoName, String storeApk
    ) {
        if (!isDownloadManagerAvailable()) {
            return;
        }

        String description = updateInfo.data.description;
        String appUrl = updateInfo.data.appURL;

        if (appUrl == null || appUrl.isEmpty()) {
            Log.e(TAG, "请填写\"App下载地址\"");
            return;
        }

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
        request.setTitle(infoName);
        request.setDescription(description);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, storeApk);

        Context appContext = context.getApplicationContext();
        DownloadManager manager = (DownloadManager)
                appContext.getSystemService(Context.DOWNLOAD_SERVICE);

        // 存储下载Key
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(appContext);
        sp.edit().putLong(PrefsConsts.DOWNLOAD_APK_ID_PREFS, manager.enqueue(request)).apply();
    }
```

> 使用``DownloadManager``下载文件是Android的推荐方式.
> 存储``下载Id(manager.enqueue(request))``是为了在安装应用时, 找到Apk.
> 默认存储地址``/storage/sdcard0/Download``.

# 4.自动安装
注册广播接收器, 接收消息``ACTION_DOWNLOAD_COMPLETE``, 下载完成会发送广播. 获取下载文件的Uri, 进行匹配, 发送安装消息, 自动安装.
```
/**
 * 安装下载接收器
 * <p>
 * Created by wangchenlong on 16/1/5.
 */
public class InstallReceiver extends BroadcastReceiver {

    private static final String TAG =
            "DEBUG-WCL: " + InstallReceiver.class.getSimpleName();

    // 安装下载接收器
    @Override public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            long downloadApkId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            installApk(context, downloadApkId);
        }
    }

    // 安装Apk
    private void installApk(Context context, long downloadApkId) {
        // 获取存储ID
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        long id = sp.getLong(PrefsConsts.DOWNLOAD_APK_ID_PREFS, -1L);

        if (downloadApkId == id) {
            DownloadManager dManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Intent install = new Intent(Intent.ACTION_VIEW);
            Uri downloadFileUri = dManager.getUriForDownloadedFile(downloadApkId);
            if (downloadFileUri != null) {
                install.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(install);
            } else {
                Log.e(TAG, "下载失败");
            }
        }
    }
}
```

> 安装本应用下载的Apk, 不安装其他Apk, 存储下载Id, 与广播Id进行匹配.
> 下载失败, 也会发送``下载完成(ACTION_DOWNLOAD_COMPLETE)``广播,  Uri可能为空, 需要判断, 否则发生崩溃.

OK, that's all! Enjoy It!
