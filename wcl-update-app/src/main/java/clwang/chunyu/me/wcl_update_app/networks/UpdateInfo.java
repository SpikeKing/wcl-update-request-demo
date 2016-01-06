package clwang.chunyu.me.wcl_update_app.networks;

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
