package clwang.chunyu.me.wcl_update_app.networks;

import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

/**
 * 春雨更新服务
 * 更新: http://www.chunyuyisheng.com/cmsapi/app/update?appName=Ped_android&version=0.0.0
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
