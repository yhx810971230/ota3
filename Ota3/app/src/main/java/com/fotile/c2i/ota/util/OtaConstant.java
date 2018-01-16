package com.fotile.c2i.ota.util;

import android.os.Environment;

import java.io.File;

/**
 * Created by yaohx on 2017/12/14.
 */

public class OtaConstant {
    /**
     * 打包改为false
     */
    public static boolean TEST = true;

    public static String TEST_URL = OtaUpgradeUtil.ServerURL + "package=com.fotile.c2i.sterilizer&version=C2SL-SA111&mac=00259219e046";
    /**
     * OTA升级包文件名称
     */
    public final static String OTANAME = "update.zip";

    public final static String FILE_FOLDER_TEST = Environment.getExternalStorageState() + "/ota/";
    /**
     * 固件包的下载目录
     */
    public final static String FILE_FOLDER = TEST ? FILE_FOLDER_TEST : Environment.getDataDirectory() + File
            .separator + "media" + File.separator;

    /**
     * 固件包的完整名称
     */
    public final static String FILE_NAME = FILE_FOLDER + OTANAME;
    /**
     * OTA解密密码
     */
    public static final String PASSWORD =
            "9588028820109132570743325311898426347857298773549468758875018579537757772163084478873699447306034466200616411960574122434059469100235892702736860872901247123456";
}
