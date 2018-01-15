package com.fotile.c2i.ota.util;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import com.fotile.c2i.ota.bean.UpgradeInfo;
import com.fotile.c2i.ota.service.DownLoadService;
import com.fotile.c2i.ota.service.DownloadAction;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.fotile.c2i.ota.fragment.SystemUpgradeFragment.NO_INVALID_PACKAGE;

/**
 * Created by yaohx on 2017/12/25.
 */

public class OtaTool {

    private static Timer timer_download;
    /**
     * 文鼎细黑
     */
    private static Typeface thinBoldFace;

    private static long last_time = 0;

    /**
     * 限制按钮的快速点击情况
     *
     * @return true 执行click事件
     */
    public static boolean fastclick() {
        long time = System.currentTimeMillis();
        if (time - last_time > 800) {
            return true;
        }
        return false;
    }

    public static void setTypeFace(Typeface t) {
        if (null == thinBoldFace) {
            thinBoldFace = t;

        }
    }

    public static Typeface getTypeface() {
        return thinBoldFace;
    }

    //获取amc地址
    public static String getLocalMacAddress() {
        String macSerial = null;
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
            if (!TextUtils.isEmpty(macSerial)) {
                macSerial = macSerial.replace(":", "");
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();
        }
        return macSerial;
    }

    /**
     * 判断网络是否可用
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context
                .CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isAvailable()) {
            return true;
        }
        return false;
    }

    /**
     * 获得当前activity的名字
     *
     * @param context
     * @return
     */
    public static String getCurrentActivityName(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
        ComponentName componentInfo = taskInfo.get(0).topActivity;
        String name = componentInfo.getClassName();
//        if (name.contains(".")) {
//            name = name.substring(name.lastIndexOf(".") + 1);
//        }
        return name;
    }

    /**
     * 反射机制获取android.os.SystemProperties
     *
     * @param key          属性的路径
     * @param defaultValue
     * @return
     */
    public static String getProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            value = (String) (get.invoke(c, key, defaultValue));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return value;
        }
    }

    /**
     * 开启后台夜间下载
     *
     * @param check_package_name 设备校验包名
     * @param context
     */
    public static void startDownloadBackGround(final String check_package_name, final Context context) {
        /***************************处理随机事件逻辑**********************/
        //获取当天日期对应的时间戳
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        long current_time = System.currentTimeMillis();
        Date date = new Date(current_time);
        //获取yyyy-MM-dd HH:mm:ss 对应的时间戳-在这之间发起请求指令
        String time_str_start = df.format(date) + " 02:00:00";
        String time_str_end = df.format(date) + " 02:10:00";
        try {
            df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //起点和结束点对应的时间戳
            long time_start = df.parse(time_str_start).getTime();
            long time_end = df.parse(time_str_end).getTime();

            OtaLog.LOGOta("后台下载时间校验起点", time_str_start + "--" + time_start);
            OtaLog.LOGOta("后台下载时间校验终点", time_str_end + "--" + time_end);
            OtaLog.LOGOta("后台下载时间当前时间点", current_time);

            //如果当前时间点在这两点之间
            if (current_time > time_start && current_time < time_end) {
                //如果不存在定时器
                if (null == timer_download) {
                    OtaLog.LOGOta("开启后台下载线程", "开启后台下载线程");
                    //获取一个随机数，随机数之后开启线程
                    long random_count = (long) (Math.random() * 3600 * 1000 * 2);

                    timer_download = new Timer();
                    timer_download.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            //十分钟后开始下载
                            startDownload(check_package_name, context);
                            timer_download = null;
                        }
                    }, random_count);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        /***************************处理随机事件逻辑**********************/

    }

    private static void startDownload(final String check_package_name, final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                /*******************************获取下载包信息*******************************/
                OtaUpgradeUtil otaUpgradeUtil = new OtaUpgradeUtil();
                //校验版本号
                String check_version_code = getProperty("ro.cvte.customer.version", "100");
                //校验mac地址
                String check_mac_address = getLocalMacAddress();
                UpgradeInfo mInfo = null;

                String reqUrl = otaUpgradeUtil.buildUrl(check_package_name, check_version_code, check_mac_address !=
                        null ? check_mac_address.replace(":", "") : "");
                if (OtaConstant.TEST) {
                    reqUrl = OtaConstant.TEST_URL;
                }
                try {
                    String content = otaUpgradeUtil.httpGet(reqUrl);
                    if (!TextUtils.isEmpty(content)) {
                        JSONObject jo = new JSONObject(content);
                        String mingwen = otaUpgradeUtil.Decrypt(jo.getString("message"), OtaConstant.PASSWORD);
                        OtaLog.LOGOta("请求Ota包信息返回数据", mingwen);
                        Gson parser = new Gson();
                        mInfo = parser.fromJson(mingwen, UpgradeInfo.class);
                    }
                } catch (Exception e) {
                    //异常不处理
                    e.printStackTrace();
                }
                /*******************************获取下载包信息*******************************/

                if (null != mInfo) {
                    //判断本地文件md5和网络接口获取的是否一样，不一样才执行下载
                    boolean md5_like = false;
                    //判断下载完成，文件已经保存了一份
                    File file = new File(OtaConstant.FILE_NAME);
                    if (file.exists()) {
                        String filemd5 = otaUpgradeUtil.md5sum(file.getPath());
                        if (filemd5.equals(mInfo.md5)) {
                            md5_like = true;
                            return;
                        }
                    }

                    if (!md5_like) {
                        //不通知界面
                        DownloadAction.getInstance().removeAction();
                        //开启下载服务
                        Intent intent = new Intent(context, DownLoadService.class);
                        intent.putExtra("url", mInfo.url);
                        intent.putExtra("md5", mInfo.md5);
                        context.startService(intent);
                        OtaLog.LOGOta("后台下载md5校验", "本地文件md5不同，执行下载");
                    }
                }
            }
        }).start();
    }
}
