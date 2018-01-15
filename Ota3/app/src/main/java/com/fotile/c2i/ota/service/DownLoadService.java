package com.fotile.c2i.ota.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;

import com.dl7.downloaderlib.DownloadConfig;
import com.dl7.downloaderlib.DownloadListener;
import com.dl7.downloaderlib.FileDownloader;
import com.dl7.downloaderlib.entity.FileInfo;
import com.dl7.downloaderlib.model.DownloadStatus;
import com.fotile.c2i.ota.util.OtaConstant;
import com.fotile.c2i.ota.bean.OtaFileInfo;
import com.fotile.c2i.ota.util.OtaLog;
import com.fotile.c2i.ota.util.OtaTool;
import com.fotile.c2i.ota.util.OtaUpgradeUtil;
import com.fotile.c2i.ota.view.OtaTopSnackBar;

import java.io.File;
import java.math.BigDecimal;

/**
 * 文件名称：DownLoadService
 * 创建时间：2017/12/25 14:47
 * 文件作者：yaohx
 * 功能描述：下载服务
 */
public class DownLoadService extends Service {
    /**
     * 固件包的保存目录
     */
    private final static String fileFolder = OtaConstant.FILE_FOLDER;
    /**
     * 固件包的保存完整名称
     */
    private final static String fileName = OtaConstant.FILE_NAME;
    /**
     * 文件下载地址
     */
    private String url;
    private String md5;

    private static int state = DownloadStatus.NORMAL;

    /**
     * 标志位，控制顶部提示只显示一次
     */
    private boolean show_downing_tip = false;
    private boolean show_complete_tip = false;
    private boolean show_error_tip = false;
    /**
     * 网络断开的时候不会立马检测到，所以要延迟
     */
    public static final int ERROR_DELAY_CODE = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static int getDownLoadState() {
        return state;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initData();
        url = intent.getExtras().getString("url");
        md5 = intent.getExtras().getString("md5");
        startDownload();
        return super.onStartCommand(intent, flags, startId);
    }

    private void initData() {
        //创建固件包下载目录
        FileDownloader.init(this);
        DownloadConfig config = new DownloadConfig.Builder().setDownloadDir(fileFolder).build();
        FileDownloader.setConfig(config);

        File tmpFile = new File(fileFolder);
        if (!tmpFile.exists()) {
            tmpFile.mkdir();
        }
    }

    private void startDownload() {
        if (!TextUtils.isEmpty(url)) {
            state = DownloadStatus.NORMAL;
            OtaLog.LOGOta("下载Ota包url", url);
            OtaLog.LOGOta("下载Ota包保存的本地路径", fileName);
            //开始下载
            FileDownloader.start(url, OtaConstant.OTANAME, new ListenerWrapper());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FileDownloader.cancel(url);
        state = DownloadStatus.NORMAL;
    }

    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            OtaFileInfo otaFileInfo = (OtaFileInfo) msg.obj;
            String current_act_name = OtaTool.getCurrentActivityName(DownLoadService.this);
            state = otaFileInfo.fileInfo.getStatus();
            switch (state) {
                //准备中
                case DownloadStatus.START:
                    show_downing_tip = false;
                    show_complete_tip = false;
                    show_error_tip = false;

                    DownloadAction.getInstance().reciverData(otaFileInfo);
                    break;
                //下载中
                case DownloadStatus.DOWNLOADING:
                    //show_downing_tip = false;
                    show_complete_tip = false;
                    show_error_tip = false;

                    DownloadAction.getInstance().reciverData(otaFileInfo);
                    //如果页面离开设置界面
                    if (!current_act_name.contains("SettingActivity") && !show_downing_tip) {
                        show_downing_tip = true;
                        OtaTopSnackBar.make(DownLoadService.this, "后台持续下载升级包", OtaTopSnackBar.LENGTH_LONG).show();
                    }
                    break;
                //完成
                case DownloadStatus.COMPLETE:
                    show_downing_tip = false;
                    //show_complete_tip = false;
                    show_error_tip = false;

                    //md5校验，防止断点下载出错
                    File file = new File(fileName);
                    if (file.exists()) {
                        OtaUpgradeUtil otaUpgradeUtil = new OtaUpgradeUtil();
                        //本地文件MD5
                        String filemd5 = otaUpgradeUtil.md5sum(file.getPath());
                        if (!md5.equals(filemd5)) {
                            OtaLog.LOGOta("下载完成", "MD5校验失败");
                            if (!show_complete_tip) {
                                show_complete_tip = true;
                                OtaTopSnackBar.make(DownLoadService.this, "文件MD5校验错误，请清除缓存重新下载", OtaTopSnackBar
                                        .LENGTH_LONG).show();
                            }
                        }
                        //md5校验正确
                        else {
                            //如果页面离开设置界面
                            if (!current_act_name.contains("SettingActivity") && !show_complete_tip) {
                                show_complete_tip = true;
                                OtaTopSnackBar.make(DownLoadService.this, "升级包下载完成，可进行系统升级", OtaTopSnackBar
                                        .LENGTH_LONG).show();
                            }
                            DownloadAction.getInstance().reciverData(otaFileInfo);
                        }
                    }
                    break;
                //报错
                case DownloadStatus.ERROR:
                    show_downing_tip = false;
                    show_complete_tip = false;
                    // show_error_tip = false;

                    uiHandler.sendEmptyMessageDelayed(1, 1000);
                    break;
                //报错
                case ERROR_DELAY_CODE:
                    DownloadAction.getInstance().reciverData(otaFileInfo);
                    if (!show_error_tip) {
                        show_error_tip = true;
                        //如果是网络造成的下载失败
                        if (!OtaTool.isNetworkAvailable(DownLoadService.this)) {
                            OtaTopSnackBar.make(DownLoadService.this, "网络断开，下载暂停，请恢复网络", OtaTopSnackBar.LENGTH_LONG)
                                    .show();
                        } else {
                            OtaTopSnackBar.make(DownLoadService.this, "下载未成功，请重新下载", OtaTopSnackBar.LENGTH_LONG).show();
                        }
                    }
                    break;
            }
        }
    };

    /**
     * 监听器封装类
     */
    class ListenerWrapper implements DownloadListener {

        @Override
        public void onStart(FileInfo fileInfo) {
            OtaLog.LOGOta("InstallAc", "ListenerWrapper = 准备中--->" + fileInfo.getUrl());
            Message msg = uiHandler.obtainMessage();
            msg.obj = new OtaFileInfo(fileInfo, "");
            uiHandler.sendMessage(msg);
        }

        @Override
        public void onUpdate(FileInfo fileInfo) {
            float progress = getProgress(fileInfo.getLoadBytes(), fileInfo.getTotalBytes());
            OtaLog.LOGOta("InstallAc", "ListenerWrapper = 下载中--->" + progress);
            Message msg = uiHandler.obtainMessage();
            msg.obj = new OtaFileInfo(fileInfo, "");
            uiHandler.sendMessage(msg);
        }

        @Override
        public void onStop(FileInfo fileInfo) {
            OtaLog.LOGOta("InstallAc", "ListenerWrapper = 停止了--->" + fileInfo.getPath());
            Message msg = uiHandler.obtainMessage();
            msg.obj = new OtaFileInfo(fileInfo, "");
            uiHandler.sendMessage(msg);
        }

        @Override
        public void onComplete(FileInfo fileInfo) {
            Message msg = uiHandler.obtainMessage();
            msg.obj = new OtaFileInfo(fileInfo, "");
            uiHandler.sendMessage(msg);
        }

        @Override
        public void onCancel(FileInfo fileInfo) {
            Message msg = uiHandler.obtainMessage();
            msg.obj = new OtaFileInfo(fileInfo, "");
            uiHandler.sendMessage(msg);
        }

        @Override
        public void onError(FileInfo fileInfo, String s) {
            OtaLog.LOGOta("InstallAc", "ListenerWrapper = 失败了--->" + fileInfo.getStatus() + ":" + s);
            Message msg = uiHandler.obtainMessage();
            msg.obj = new OtaFileInfo(fileInfo, "");
            uiHandler.sendMessage(msg);
        }
    }

    private float getProgress(int progress, int max) {
        BigDecimal bigDecimal1 = new BigDecimal(progress);
        BigDecimal bigDecimal2 = new BigDecimal(max);
        return bigDecimal1.divide(bigDecimal2, 4, BigDecimal.ROUND_DOWN).floatValue();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
