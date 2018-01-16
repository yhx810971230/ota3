package com.fotile.c2i.ota.util;

/**
 * Created by yaohx on 2017/12/25.
 */

public interface OtaListener {
    /**
     * 返回设备工作状态
     *
     * @return
     */
    abstract boolean isWorking();

    abstract void onDownloadCompleted(String newVersion);

//    abstract void onInstallNow();

    abstract void onInstallLater();
}
