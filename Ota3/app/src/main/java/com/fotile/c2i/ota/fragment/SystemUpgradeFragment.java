package com.fotile.c2i.ota.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dl7.downloaderlib.entity.FileInfo;
import com.dl7.downloaderlib.model.DownloadStatus;
import com.fotile.c2i.ota.R;
import com.fotile.c2i.ota.bean.OtaFileInfo;
import com.fotile.c2i.ota.bean.UpgradeInfo;
import com.fotile.c2i.ota.service.DownLoadService;
import com.fotile.c2i.ota.service.DownloadAction;
import com.fotile.c2i.ota.util.OtaConstant;
import com.fotile.c2i.ota.util.OtaListener;
import com.fotile.c2i.ota.util.OtaLog;
import com.fotile.c2i.ota.util.OtaTool;
import com.fotile.c2i.ota.util.OtaUpgradeUtil;
import com.fotile.c2i.ota.view.HorizontalProgressBarWithNumber;
import com.fotile.c2i.ota.view.OtaLoadingView;
import com.fotile.c2i.ota.view.OtaTopSnackBar;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 文件名称：SystemUpgradeFragment
 * 创建时间：2017/915
 * 文件作者：fuzya
 * 功能描述：系统升级
 */

public class SystemUpgradeFragment extends Fragment {
    /**********************************更新view相关控件*******************************/
    /**
     * 更新按钮
     */
    Button btn_upgrade;

    /**
     * 更新进度条view
     */
    LinearLayout lay_upgrade;
    /**
     * 版本信息view
     */
    RelativeLayout lay_version;

    /**
     * view中间的版本信息文本
     */
    TextView txt_version_info;
    /**
     * 底部老版本提示
     */
    TextView tv_old_version_tip;

    /**
     * 下载固件包提示
     */
    TextView tv_tips;
    /**
     * 下载进度条
     */
    HorizontalProgressBarWithNumber pbar_download;
    /**
     * 加载提示
     */
    RelativeLayout lay_laoding;
    OtaLoadingView img_loading;
    /**********************************更新view相关控件*******************************/

    /**
     * 升级view界面
     */
    RelativeLayout layout_main_upgrade;
    /**
     * 下载完成view界面
     */
    RelativeLayout layout_main_completed;

    /**********************************下载完成view相关控件↓*******************************/
    /**
     * 更新标题
     */
    TextView txt_update_version;
    /**
     * 更新内容
     */
    TextView tv_update_comment;
    /**
     * 立即更新
     */
    Button btn_upgrade_now;
    /**
     * 稍后更新
     */
    Button btn_upgrade_later;
    /**********************************下载完成view相关控件↑*******************************/


    public static final int NO_INVALID_PACKAGE = 0;//无新的升级包
    public static final int NEW_INVALID_PACKAGE = 1;//有新的升级包
    public static final int ERROR_INVALID_PACKAGE = 2;//获取数据异常
    /**
     * 下载工具类
     */
    private OtaUpgradeUtil otaUpgradeUtil;

    public UpgradeInfo mInfo;
    /**
     * 校验包名
     */
    private String check_package_name;
    /**
     * 校验本地版本号
     */
    private String check_version_code;
    /**
     * 校验本地mac
     */
    private String check_mac_address;

    protected View view;

    private DownloadAction.ActionListener action;

    private OtaListener otaListener;

    /**
     * view状态-获取信息中
     */
    private int VIEW_STATE_LOADING = -1;
    /**
     * view状态-获取信息失败，或者无网络
     */
    private int VIEW_STATE_NO_DATA = 1;
    /**
     * view状态-没有可更新
     */
    private int VIEW_STATE_NO_PACKAGE = 2;
    /**
     * view状态-有可更新
     */
    private int VIEW_STATE_NEW_PACKAGE = 3;

    /**
     * 下载中
     */
    private int VIEW_DOWN_DOWNING = 4;
    /**
     * 下载错误
     */
    private int VIEW_DOWN_ERROR = 5;
    /**
     * 下载完成
     */
    private int VIEW_DOWN_COMPLETE = 6;
    /**
     * 是否正在获取固件信息
     */
    private boolean is_loading_version_data;

    private Timer timer_net;

    public SystemUpgradeFragment(String packageName, Typeface typeface, OtaListener otaListener) {
        check_package_name = packageName;
        this.otaListener = otaListener;
        OtaTool.setTypeFace(typeface);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_system_upgrade, container, false);
        initView();
        createAction();
        return view;
    }


    public void createAction() {
        action = new DownloadAction.ActionListener() {
            @Override
            public void onAction(OtaFileInfo otaFileInfo) {
                FileInfo fileInfo = otaFileInfo.fileInfo;
                switch (fileInfo.getStatus()) {
                    //开始下载
                    case DownloadStatus.START:

                        break;
                    //正在下载
                    case DownloadStatus.DOWNLOADING:
                        //等待固件信息获取完毕，才去刷新
                        if (!is_loading_version_data) {
                            updateDownloadValue(VIEW_DOWN_DOWNING);
                            pbar_download.setMax(fileInfo.getTotalBytes());
                            pbar_download.setProgress(fileInfo.getLoadBytes());
                        }
                        break;
                    //下载失败
                    case DownloadStatus.ERROR:
                        //等待固件信息获取完毕，才去刷新
                        if (!is_loading_version_data) {
                            updateDownloadValue(VIEW_DOWN_ERROR);
                        }
                        break;
                    //下载完成
                    case DownloadStatus.COMPLETE:
                        //等待固件信息获取完毕，才去刷新
                        if (!is_loading_version_data) {
                            updateDownloadValue(VIEW_DOWN_COMPLETE);
                        }
                        break;
                }
            }
        };
        DownloadAction.getInstance().addAction(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (null != action) {
            DownloadAction.getInstance().removeAction();
        }
        cancelNetTimer();
    }

    /**
     * 判断ota文件是否存在
     *
     * @return
     */
    public boolean checkDownloadFileExists() {
        File file = new File(OtaConstant.FILE_NAME);
        return file.exists();
    }

    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            //针对清空缓存逻辑
            if (!is_loading_version_data) {
                int state = DownLoadService.getDownLoadState();
                //判断后台下载完成，文件已经保存了一份
                if (!checkDownloadFileExists() && state == DownloadStatus.COMPLETE) {
                    showView(1);
                    startInitLogic();
                }
            }
        }
    }

    /**
     * 1--显示升级界面
     * 2--显示完成界面
     *
     * @param type
     */
    private void showView(int type) {
        if (type == 1) {
            layout_main_upgrade.setVisibility(View.VISIBLE);
            layout_main_completed.setVisibility(View.GONE);
        } else {
            layout_main_upgrade.setVisibility(View.GONE);
            layout_main_completed.setVisibility(View.VISIBLE);
            if (null != mInfo) {
                //下载完成显示更新包信息
                txt_update_version.setText("方太智慧厨房 " + mInfo.name);
                tv_update_comment.setText(mInfo.comment);
            }
        }
    }

    private void initView() {
        layout_main_upgrade = (RelativeLayout) view.findViewById(R.id.layout_main_upgrade);
        layout_main_completed = (RelativeLayout) view.findViewById(R.id.layout_main_completed);
        //默认显示升级界面
        showView(1);

        btn_upgrade = (Button) view.findViewById(R.id.btn_upgrade);
        txt_version_info = (TextView) view.findViewById(R.id.txt_version_info);
        tv_old_version_tip = (TextView) view.findViewById(R.id.tv_old_version_tip);
        tv_tips = (TextView) view.findViewById(R.id.tv_tips);
        pbar_download = (HorizontalProgressBarWithNumber) view.findViewById(R.id.pbar_download);
        img_loading = (OtaLoadingView) view.findViewById(R.id.img_loading);

        lay_upgrade = (LinearLayout) view.findViewById(R.id.lay_upgrade);
        lay_version = (RelativeLayout) view.findViewById(R.id.lay_version);
        lay_laoding = (RelativeLayout) view.findViewById(R.id.lay_laoding);
        //----------------------------------------------------------------------//
        txt_update_version = (TextView) view.findViewById(R.id.txt_update_version);
        tv_update_comment = (TextView) view.findViewById(R.id.tv_update_comment);
        btn_upgrade_now = (Button) view.findViewById(R.id.btn_upgrade_now);
        btn_upgrade_later = (Button) view.findViewById(R.id.btn_upgrade_later);

        //-----------------------------------listener-----------------------------------//
        //点击下载或者重新下载
        btn_upgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (OtaTool.fastclick()) {
                    if (OtaTool.isNetworkAvailable(getActivity()) && null != mInfo) {
                        if (null != otaListener && !otaListener.isWorking()) {
                            startDownLoadService();
                            lay_upgrade.setVisibility(View.VISIBLE);
                            lay_version.setVisibility(View.GONE);
                        }
                    } else {
                        OtaTopSnackBar.make(getActivity(), "请检查网络连接！", OtaTopSnackBar.LENGTH_SHORT).show();
                    }
                }
            }
        });
        //立即安装
        btn_upgrade_now.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //如果设备没有工作中，才执行ota
                if (null != otaListener && !otaListener.isWorking()) {
                    upgrade();
                }
            }
        });
        //稍后安装
        btn_upgrade_later.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != otaListener) {
                    otaListener.onInstallLater();
                }
            }
        });
        //-----------------------------------listener-----------------------------------//

        otaUpgradeUtil = new OtaUpgradeUtil();
        startInitLogic();
    }

    private void startInitLogic() {
        //显示加载动画--如果网络畅通，获取升级包信息
        if (OtaTool.isNetworkAvailable(getActivity())) {
            is_loading_version_data = true;
            updateVersionValue(VIEW_STATE_LOADING);
            getParams();
        } else {
            updateVersionValue(VIEW_STATE_NO_DATA);
            OtaTopSnackBar.make(getActivity(), "请检查网络连接！", com.fotile.c2i.ota.view.OtaTopSnackBar.LENGTH_SHORT).show();
        }
    }

    /**
     * 升级
     */
    private void upgrade() {
        Intent otaIntent = new Intent("com.cvte.androidsystemtoolbox.action.SYSTEM_UPGRADE");
        otaIntent.putExtra("path", OtaConstant.FILE_NAME);//data/media/update.zip
        getActivity().sendBroadcast(otaIntent);
    }

    /**
     * 根据获取到的版本信息，来显示版本相关view
     *
     * @param type -1-固件包数据获取中，loading现实中
     *             0-网络异常，或者没有获取到数据
     *             1-没有可更新的固件包
     *             2-有可更新的固件包
     */
    private void updateVersionValue(int type) {
        tv_old_version_tip.setText("方太智慧厨房 " + OtaTool.getProperty("ro.cvte.customer.version", "unknow"));
        //信息获取中
        if (type == VIEW_STATE_LOADING) {
            //当前系统版本
            String tip = "方太智慧厨房 " + OtaTool.getProperty("ro.cvte.customer.version", "unknow");
            txt_version_info.setText(tip);
            txt_version_info.setVisibility(View.GONE);
            btn_upgrade.setVisibility(View.GONE);
            btn_upgrade.setText("系统升级");
            tv_old_version_tip.setVisibility(View.GONE);
            img_loading.startRotationAnimation();

            lay_laoding.setVisibility(View.VISIBLE);
            lay_upgrade.setVisibility(View.GONE);
            lay_version.setVisibility(View.GONE);
        }
        //获取数据失败，没有网络
        if (type == VIEW_STATE_NO_DATA) {
            //当前系统版本
            String tip = "方太智慧厨房 " + OtaTool.getProperty("ro.cvte.customer.version", "unknow");
            txt_version_info.setText(tip);
            txt_version_info.setVisibility(View.VISIBLE);
            btn_upgrade.setVisibility(View.GONE);
            btn_upgrade.setText("系统升级");
            tv_old_version_tip.setVisibility(View.GONE);
            img_loading.stopRotationAnimation();

            lay_laoding.setVisibility(View.GONE);
            lay_upgrade.setVisibility(View.GONE);
            lay_version.setVisibility(View.VISIBLE);
        }
        //没有可更新的固件包
        if (type == VIEW_STATE_NO_PACKAGE) {
            String tip = "方太智慧厨房 " + OtaTool.getProperty("ro.cvte.customer.version", "unknow") + "\n当前为最新版本。";
            txt_version_info.setText(tip);
            txt_version_info.setVisibility(View.VISIBLE);
            btn_upgrade.setVisibility(View.GONE);
            btn_upgrade.setText("系统升级");
            tv_old_version_tip.setVisibility(View.GONE);
            img_loading.stopRotationAnimation();

            lay_laoding.setVisibility(View.GONE);
            lay_upgrade.setVisibility(View.GONE);
            lay_version.setVisibility(View.VISIBLE);
        }
        //有可更新的固件包
        if (type == VIEW_STATE_NEW_PACKAGE) {
            String tip = "检测到有新系统版本方太智慧 " + mInfo.name;
            txt_version_info.setText(tip);
            txt_version_info.setVisibility(View.VISIBLE);
            btn_upgrade.setVisibility(View.VISIBLE);
            btn_upgrade.setText("系统升级");
            tv_old_version_tip.setVisibility(View.VISIBLE);
            img_loading.stopRotationAnimation();

            lay_laoding.setVisibility(View.GONE);
            lay_upgrade.setVisibility(View.GONE);
            lay_version.setVisibility(View.VISIBLE);
        }
    }


    private synchronized void cancelNetTimer() {
        if (null != timer_net) {
            timer_net.cancel();
            timer_net.purge();
            timer_net = null;
        }
    }

    /**
     * 根据下载状态来显示相应的view
     *
     * @param type
     */
    private void updateDownloadValue(int type) {
        //下载完成
        if (type == VIEW_DOWN_COMPLETE) {
            tv_tips.setText("升级包下载完成");
            //下载完成界面跳转
            if (null != otaListener && null != mInfo) {
                otaListener.onDownloadCompleted(mInfo.name);
            }

            lay_laoding.setVisibility(View.GONE);
            lay_upgrade.setVisibility(View.VISIBLE);
            lay_version.setVisibility(View.GONE);

            showView(2);
        }
        //下载失败
        if (type == VIEW_DOWN_ERROR) {
            //网络造成的下载失败
            if (!OtaTool.isNetworkAvailable(getActivity())) {
                lay_laoding.setVisibility(View.GONE);
                lay_upgrade.setVisibility(View.VISIBLE);
                lay_version.setVisibility(View.GONE);
                tv_tips.setText("暂停下载升级包");

                if (null == timer_net) {
                    timer_net = new Timer();
                }
                timer_net.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //网络连接时，自动下载
                        if (OtaTool.isNetworkAvailable(getActivity())) {
                            startDownLoadService();
                            cancelNetTimer();
                        }
                    }
                }, 1000, 1000);
            } else {
                String tip = "";
                txt_version_info.setText(tip);
                txt_version_info.setVisibility(View.GONE);
                btn_upgrade.setVisibility(View.VISIBLE);
                btn_upgrade.setText("重新下载");
                tv_old_version_tip.setVisibility(View.VISIBLE);

                lay_laoding.setVisibility(View.GONE);
                lay_upgrade.setVisibility(View.GONE);
                lay_version.setVisibility(View.VISIBLE);
            }
        }
        //下载中
        if (type == VIEW_DOWN_DOWNING) {
            lay_laoding.setVisibility(View.GONE);
            lay_upgrade.setVisibility(View.VISIBLE);
            lay_version.setVisibility(View.GONE);
        }

    }

    /**
     * 开启下载服务
     */
    public void startDownLoadService() {
        if (null != mInfo) {
            Intent intent = new Intent(getActivity(), DownLoadService.class);
            intent.putExtra("url", mInfo.url);
            intent.putExtra("md5", mInfo.md5);
            getActivity().startService(intent);
        }
    }


    /**
     * 固件包升级信息回调
     * 进入该回调，mInfo有值了
     */
    Handler checkhandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                //获取数据异常
                case ERROR_INVALID_PACKAGE:
                    updateVersionValue(VIEW_STATE_NO_DATA);
                    break;
                //没有可更新的固件包
                case NO_INVALID_PACKAGE:
                    updateVersionValue(VIEW_STATE_NO_PACKAGE);
                    break;
                //有可更新的固件包
                case NEW_INVALID_PACKAGE:
                    updateVersionValue(VIEW_STATE_NEW_PACKAGE);

                    int state = DownLoadService.getDownLoadState();
                    //如果当前后台正在下载
                    if (state == DownloadStatus.DOWNLOADING) {
                        updateDownloadValue(VIEW_DOWN_DOWNING);
                    }
                    //如果当前后台下载错误
                    if (state == DownloadStatus.ERROR) {
                        updateDownloadValue(VIEW_DOWN_ERROR);
                    }
                    //判断后台下载完成，文件已经保存了一份
                    if (checkDownloadCompleted() && null != otaListener) {
                        otaListener.onDownloadCompleted(mInfo.name);
                        showView(2);
                    }
                    break;
                default:
                    break;
            }

            is_loading_version_data = false;
        }
    };


    /**
     * 获取升级包信息
     */
    private void getParams() {
        //用于匹配OTA服务器的包名
        //check_package_name = OtaConstant.PACKAGE_NAME;
        //校验匹配版本号
        check_version_code = OtaTool.getProperty("ro.cvte.customer.version", "100");
        //校验mac地址
        check_mac_address = OtaTool.getLocalMacAddress();

        new Thread(new Runnable() {
            @Override
            public void run() {
                getUpgradeInfo();
            }
        }).start();
    }

    /**
     * 校验文件是否下载完成或者已经下载过了
     *
     * @return
     */
    public boolean checkDownloadCompleted() {
        File file = new File(OtaConstant.FILE_NAME);
        if (file.exists()) {
            //本地文件md5
            String filemd5 = otaUpgradeUtil.md5sum(file.getPath());
            if (null != mInfo && filemd5.equals(mInfo.md5)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取OTA服务器上的固件的信息
     */

    private void getUpgradeInfo() {
        String reqUrl = otaUpgradeUtil.buildUrl(check_package_name, check_version_code, check_mac_address != null ?
                check_mac_address.replace(":", "") : "");
        if (OtaConstant.TEST) {
            reqUrl = OtaConstant.TEST_URL;
        }
        OtaLog.LOGOta("请求Ota包信息url", reqUrl);
        String content = "";
        String miwen = "";
        String mingwen = "";
        try {
            content = otaUpgradeUtil.httpGet(reqUrl);
            if (content == null || content.equals("{}")) {
                checkhandler.sendEmptyMessage(NO_INVALID_PACKAGE);
                return;
            }
            JSONObject jo = new JSONObject(content);
            miwen = jo.getString("message");
            mingwen = otaUpgradeUtil.Decrypt(miwen, OtaConstant.PASSWORD);
            OtaLog.LOGOta("请求Ota包信息返回数据", mingwen);
        } catch (IOException e) {
            e.printStackTrace();
            checkhandler.sendEmptyMessage(ERROR_INVALID_PACKAGE);
            return;
        } catch (JSONException e) {
            e.printStackTrace();
            checkhandler.sendEmptyMessage(ERROR_INVALID_PACKAGE);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            checkhandler.sendEmptyMessage(ERROR_INVALID_PACKAGE);
            return;
        }
        Gson parser = new Gson();
        mInfo = parser.fromJson(mingwen, UpgradeInfo.class);
        checkhandler.sendEmptyMessage(NEW_INVALID_PACKAGE);
    }


}
