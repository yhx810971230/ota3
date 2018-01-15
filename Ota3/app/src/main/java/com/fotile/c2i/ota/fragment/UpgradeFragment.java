package com.fotile.c2i.ota.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.fotile.c2i.ota.R;
import com.fotile.c2i.ota.util.OtaConstant;
import com.fotile.c2i.ota.util.OtaListener;

/**
 * 文件名称：SystemUpgradeFragment
 * 创建时间：2017/11/27 14:40
 * 文件作者：fuzya
 * 功能描述：系统下载完成后升级页面
 */

public class UpgradeFragment extends Fragment implements View.OnClickListener {

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
    protected View view;
    /**
     * 新版本
     */
    private String newVersion;
    /**
     * 版本描述
     */
    private String comments;
    private OtaListener otaListener;


    public UpgradeFragment() {

    }

    public UpgradeFragment(String newVersion, String comments, OtaListener otaListener) {
        this.newVersion = newVersion;
        this.comments = comments;
        this.otaListener = otaListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_upgrade, container, false);
        initView();
        return view;
    }

    private void initView() {
        txt_update_version = (TextView) view.findViewById(R.id.txt_update_version);
        tv_update_comment = (TextView) view.findViewById(R.id.tv_update_comment);
        btn_upgrade_now = (Button) view.findViewById(R.id.btn_upgrade_now);
        btn_upgrade_later = (Button) view.findViewById(R.id.btn_upgrade_later);

        txt_update_version.setText("方太智慧厨房 " + newVersion);
        tv_update_comment.setText(comments);

        if (null == otaListener) {
            return;
        }
        btn_upgrade_now.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //如果设备没有工作中，才执行ota
                if (null != otaListener) {
                    if (!otaListener.isWorking()) {
                        upgrade();
                    }
                }
            }
        });
        btn_upgrade_later.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                otaListener.onInstallLater();
            }
        });
    }


    /**
     * 升级
     */
    private void upgrade() {
        Intent otaIntent = new Intent("com.cvte.androidsystemtoolbox.action.SYSTEM_UPGRADE");
        otaIntent.putExtra("path", OtaConstant.FILE_NAME);//data/media/update.zip
        getActivity().sendBroadcast(otaIntent);
    }

    @Override
    public void onClick(View v) {
//        switch (v.getId()) {
//          //立即更新
//            case R.id.btn_upgrade_now:
//                if (JniCallBackManager.is_working) {
//                    OtaTopSnackBar.make(getActivity(), "消毒柜工作中，无法进行系统升级", OtaTopSnackBar.LENGTH_SHORT).show();
//                } else {
//                    upgrade();
//                }
//                break;
//            //稍后更新
//            case R.id.btn_upgrade_later:
//                break;
//    }
    }
}
