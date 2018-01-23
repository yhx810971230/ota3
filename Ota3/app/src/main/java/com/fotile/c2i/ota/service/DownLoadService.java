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
import com.fotile.c2i.ota.bean.OtaFileInfo;
import com.fotile.c2i.ota.util.OtaConstant;
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
    private String fileFolder = OtaConstant.FILE_FOLDER;
    /**
     * 固件包的保存完整名称
     */
    private final String file_name_ota = OtaConstant.FILE_NAME_OTA;
    /**
     * mcu包的完整名称
     */
    private final String file_name_mcu = OtaConstant.FILE_NAME_MCU;
    /**
     * 文件下载地址
     */
    private String url;
    private String md5;
    /**
     * mcu url
     */
    private String ex_url;
    /**
     * mcu md5
     */
    private String ex_md5;
    /**
     * 是否只有固件包
     */
    private boolean packageOnly = false;

    private static int state = DownloadStatus.NORMAL;

    /**
     * 标志位，控制顶部提示只显示一次
     */
    private boolean show_downing_tip = false;
    private boolean show_complete_tip = false;
    private boolean show_error_tip = false;


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
        ex_url = intent.getExtras().getString("ex_url");
        ex_md5 = intent.getExtras().getString("ex_md5");

        //如果mcu url为空，判断为只下载ota包
        if (TextUtils.isEmpty(ex_url)) {
            packageOnly = true;
            startDownload();
            OtaLog.LOGOta("只下载ota包", "只下载ota包");
        } else {
            packageOnly = false;
            startMcuDownload();
            OtaLog.LOGOta("下载mcu包", "下载mcu包");
        }

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

    /**
     * 开始下载固件包
     */
    private void startDownload() {
        if (!TextUtils.isEmpty(url)) {
            state = DownloadStatus.NORMAL;
            OtaLog.LOGOta("下载Ota包url", url);
            OtaLog.LOGOta("下载Ota包保存的本地路径", file_name_ota);
            //开始下载
            FileDownloader.start(url, OtaConstant.OTANAME, new ListenerWrapper());
        }
    }

    /**
     * 开启下载mcu
     */
    private void startMcuDownload() {
        if (!TextUtils.isEmpty(ex_url)) {
            state = DownloadStatus.NORMAL;
            OtaLog.LOGOta("下载mcu包url", ex_url);
            OtaLog.LOGOta("下载mcu包保存的本地路径", file_name_mcu);
            //开始下载
            FileDownloader.start(ex_url, OtaConstant.OTANAME_MCU, new ListenerWrapper());
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        FileDownloader.cancel(url);
        FileDownloader.cancel(ex_url);
        state = DownloadStatus.NORMAL;
    }

    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            OtaFileInfo otaFileInfo = (OtaFileInfo) msg.obj;
            if (null != otaFileInfo && null != otaFileInfo.fileInfo) {
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

                        //固件包下载完成--可能下载了mcu
                        if (packageOnly) {
                            //校验成功
                            if(!checkOtafile()){ //这里是刚刚下载完mcu 然后进入这边  文件还不存在，会导致进入校验失败逻辑
                                return;
                            }
                            if (checkOtamd5()) {
                                //如果页面离开设置界面
                                OtaTool.setLastUpdateVersion(getApplicationContext(),"yes");
                                if (!current_act_name.contains("SettingActivity") && !show_complete_tip) {
                                    show_complete_tip = true;
                                    OtaTopSnackBar.make(DownLoadService.this, "升级包下载完成，可进行系统升级", OtaTopSnackBar
                                            .LENGTH_LONG).show();
                                }
                                DownloadAction.getInstance().reciverData(otaFileInfo);
                            }
                            //校验失败
                            else {
                                OtaLog.LOGOta("下载完成", "=========== 固件包md5校验失败");
                                if (!show_complete_tip) {
                                    show_complete_tip = true;
                                    OtaTopSnackBar.make(DownLoadService.this, "固件包文件MD5校验错误，请清除缓存重新下载", OtaTopSnackBar
                                            .LENGTH_LONG).show();
                                }
                            }

                        }
                        //mcu下载完成，开始下载固件包
                        else {
                            //md5校验成功才会去执行下载固件包
                            if (checkMcumd5()) {
                                packageOnly = true;
                                startDownload();
                            }
                            //校验失败
                            else {
                                OtaLog.LOGOta("下载完成", "========= mcu包md5校验失败");
                                if (!show_complete_tip) {
                                    show_complete_tip = true;
                                    OtaTopSnackBar.make(DownLoadService.this, "mcu文件MD5校验错误，请清除缓存重新下载", OtaTopSnackBar
                                            .LENGTH_LONG).show();
                                }
                            }
                        }
                        break;
                    //报错
                    case DownloadStatus.ERROR:
                        show_downing_tip = false;
                        show_complete_tip = false;
                        // show_error_tip = false;

                        DownloadAction.getInstance().reciverData(otaFileInfo);
                        if (!show_error_tip) {
                            show_error_tip = true;
                            //如果是网络造成的下载失败
                            if (!OtaTool.isNetworkAvailable(DownLoadService.this)) {
                                OtaTopSnackBar.make(DownLoadService.this, "网络断开，下载暂停，请恢复网络", OtaTopSnackBar
                                        .LENGTH_LONG).show();
                            } else {
                                OtaTopSnackBar.make(DownLoadService.this, "下载未成功，请重新下载", OtaTopSnackBar.LENGTH_LONG)
                                        .show();
                            }
                        }
                        break;

                }
            }

        }
    };

    /**
     * 校验ota的md5
     *
     * @return
     */
    private boolean checkOtamd5() {
        //ota md5校验，防止断点下载出错

        boolean check_md5_ota = false;
        File file_ota = new File(file_name_ota);
        OtaLog.LOGOta("=====","===========当前的"+md5 + "文件的是否存在:"+file_ota.exists());
        if (file_ota.exists()) {
            String str_md5_ota = new OtaUpgradeUtil().md5sum(file_ota.getPath());
            OtaLog.LOGOta("=====","===========当前的"+md5 + "文件的md5:"+str_md5_ota);
            if (!TextUtils.isEmpty(str_md5_ota) && str_md5_ota.equals(md5)) {
                check_md5_ota = true;
                OtaLog.LOGOta("固件包md5校验成功", "true");
            }
        }
        OtaLog.LOGOta("=====","===========当前的返回结果"+check_md5_ota);
        return check_md5_ota;
    }
    //判断ota文件是否存在
    private boolean checkOtafile() {
        //ota md5校验，防止断点下载出错

        boolean file_exists = false;
        File file_ota = new File(file_name_ota);
        return  file_ota.exists();
    }

    /**
     * 校验mcu的md5
     *
     * @return
     */
    private boolean checkMcumd5() {
        //mcu md5校验，防止断点下载出错
        boolean check_md5_mcu = false;
        File file_mcu = new File(file_name_mcu);
        if (file_mcu.exists()) {
            String str_md5_mcu = new OtaUpgradeUtil().md5sum(file_mcu.getPath());
            if (!TextUtils.isEmpty(str_md5_mcu) && str_md5_mcu.equals(ex_md5)) {
                check_md5_mcu = true;
                OtaLog.LOGOta("mcu包md5校验成功", "true");
            }
        }
        return check_md5_mcu;
    }

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
            //这里需要延时1秒
            /**
             * 网络断开的时候不会立马检测到，所以要延迟
             */
            uiHandler.sendMessageDelayed(msg,1000);
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
