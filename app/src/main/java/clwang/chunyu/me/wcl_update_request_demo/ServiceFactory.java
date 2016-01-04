package clwang.chunyu.me.wcl_update_request_demo;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * 创建服务
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
