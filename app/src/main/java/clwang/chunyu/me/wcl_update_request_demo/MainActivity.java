package clwang.chunyu.me.wcl_update_request_demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import clwang.chunyu.me.wcl_update_app.UpdateAppUtils;
import clwang.chunyu.me.wcl_update_app.networks.UpdateInfo;

public class MainActivity extends AppCompatActivity {

    private static final String APP_NAME = "Ped_android";
    private static final String VERSION = "1.9.0";
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
