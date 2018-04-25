package com.fotile.c2i.ota.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fotile.c2i.ota.util.OtaConstant;
import com.fotile.c2i.ota.util.OtaLog;
import com.fotile.c2i.ota.util.OtaTool;
import com.fotile.c2i.ota.view.OtaTopSnackBar;

/** 系统升级回调
 * Created by pyw on 2018/04/25.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        String oldVersion=  OtaTool.getNowVersion(context);
        String nowVersion = OtaTool.getProperty("ro.cvte.customer.version", "unknow");
        if(oldVersion!=null && oldVersion.equals(nowVersion)&& !oldVersion.equals("unknow")){//升级版本失败
            OtaLog.LOGOta("=== 系统升级失败","系统升级失败----------------------------------");
            OtaTopSnackBar.make(context, "系统升级失败", OtaTopSnackBar.LENGTH_LONG).show();
        }else {
            OtaTool.delectFile();
            OtaLog.LOGOta("=== 系统升级成功","系统升级成功----------------------------------");
        }
        OtaTool.checkDownloadTips(context);

    }
}
