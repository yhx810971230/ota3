package com.fotile.c2i.ota.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

/**
 * @author ： panyw .
 * @date ：2018/2/5 18:05
 * @COMPANY ： Fotile智能厨电研究院
 * @description ：
 */

public class HttpUtil {
    private static String NEW_STATE_SHARED = "new_state_shared";
    private static String NEW_STATE_NAME = "now_state";
    public final static String FILE_STATE_DIR =   Environment.getExternalStorageDirectory().getPath() +"/ota";
    public final static String FILE_STATE_NAME =  "/info.txt";
    public final static String DEPART = "######";
    /**当前ota状态**/
    public final static String OTA_STATE = "ota_state";
    public final static String RECIPES_URL = "recipes_url";
    public static boolean NEW_STATE = false;
    /** 当前版本号*/
    public final static String VERSION = "version";
    public static int version_code = -1;
    // 默认状态
    public final static String DEFLUT_STATE = "false";

    public static int getVersion_code() {
        return version_code;
    }

    public static void setVersion_code(int version_code) {
        version_code = version_code;
    }

    public static boolean isNewState() {
        return NEW_STATE;
    }

    public static void setNewState(boolean newState) {
        NEW_STATE = newState;
    }

    /** 设置版本号*/
    public static void SetVersion(final Context context,int version){
        SharedPreferences sharedPreferences = context.getSharedPreferences(NEW_STATE_SHARED,MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(VERSION,version);
        editor.apply();
        editor.commit();
    }

    /**
     * 获取版本号
     * @param context
     * @return
     */
    public static int GetVersion(final Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(NEW_STATE_SHARED,MODE_PRIVATE);
        return sharedPreferences.getInt(NEW_STATE_NAME,-1);
    }

    /**设置flag**/
    public static void setNewState(final Context context, boolean newState){
        SharedPreferences sharedPreferences = context.getSharedPreferences(NEW_STATE_SHARED,MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(NEW_STATE_NAME,newState);
        editor.apply();
        editor.commit();
    }
    /**获取otaflag**/
    public static boolean isNewState(final Context context){
        Map<String,String> map =HttpUtil.getStateFromFile();
        OtaLog.LOGOta("===当前文件状态","当前ota状态"+map.get(OTA_STATE)+",当前recipes_url"+map.get(RECIPES_URL));
        return Boolean.valueOf(map.get(OTA_STATE));
//        SharedPreferences sharedPreferences = context.getSharedPreferences(NEW_STATE_SHARED,MODE_PRIVATE);
//        return sharedPreferences.getBoolean(NEW_STATE_NAME,false);
    }
    /**获取菜谱flag**/
    public static String getRecipesUrl(){
        Map<String,String> map =HttpUtil.getStateFromFile();
        OtaLog.LOGOta("===当前文件状态","当前ota状态"+map.get(OTA_STATE)+",当前recipes_url"+map.get(RECIPES_URL));
        return map.get(RECIPES_URL);
//        SharedPreferences sharedPreferences = context.getSharedPreferences(NEW_STATE_SHARED,MODE_PRIVATE);
//        return sharedPreferences.getBoolean(NEW_STATE_NAME,false);
    }

    public static boolean setStateToFile(boolean ota_flag,String recipes_url){
        File file = new File(FILE_STATE_DIR);
        if(!file.exists()){
            OtaLog.LOGOta("===当前文件状态","创建文件夹");
            file.mkdirs();
        }else {
            OtaLog.LOGOta("===当前文件状态","文件夹已经存在");
        }
        File file1 = new File(FILE_STATE_DIR+FILE_STATE_NAME);
        if(!file1.exists()){

            try {
                OtaLog.LOGOta("===当前文件状态","创建文件"+file1);
                file1.createNewFile();
            } catch (IOException e) {
                OtaLog.LOGOta("===当前文件状态","创建文件失败"+file1);
                e.printStackTrace();
            }
        } else {
            OtaLog.LOGOta("===当前文件状态","文件已经存在"+file1);
        }
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        try {
            //文件输出流
            fos = new FileOutputStream(file1);
            osw = new OutputStreamWriter(fos,"utf-8");

            //写数据
            osw.write((String.valueOf(ota_flag) + DEPART + recipes_url));
            osw.flush();
            fos.flush();
            //关闭文件流
            osw.close();
            fos.close();
            OtaLog.LOGOta("===当前文件状态","写入成功======");
            getStateFromFile();
            return true;
        } catch (Exception e) {
            OtaLog.LOGOta("===当前文件状态","失败1111");
            e.printStackTrace();
            OtaLog.LOGOta("===当前文件状态","失败1111222");
            return false;
        }finally {
            if(fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(osw != null){
                try {
                    osw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * @return  Map<String, String>
     * 返回的前面一个是ota状态，后面是网址，
     * 默认 ota false，网址“”；
     *
     * **/
    public static Map<String, String> getStateFromFile(){
        File file = new File(FILE_STATE_DIR);
        if(!file.exists()){
            OtaLog.LOGOta("===当前文件状态","创建文件夹2222");
            file.mkdirs();
        }else {
            OtaLog.LOGOta("===当前文件状态","文件夹2222存在");
        }
        File file1 = new File(FILE_STATE_DIR+FILE_STATE_NAME);
        if(!file1.exists()){
            try {
                boolean fk = file1.createNewFile();
                OtaLog.LOGOta("===当前文件状态","创建文件2222"+fk);

            } catch (IOException e) {
                OtaLog.LOGOta("===当前文件状态","创建文件2222失败");
                e.printStackTrace();
            }
        }
        FileInputStream fis = null;
        InputStreamReader isr = null;
        try {
            //输入流
            fis = new FileInputStream(file1);
            isr = new InputStreamReader(fis,"utf-8");
            Map<String, String> userMap = new HashMap<String, String>();
            char input[] = new char[fis.available()];
            isr.read(input);
            //读取文件中的内容
            String result = new String(input);
            isr.close();
            fis.close();
            //拆分成String[]
            if(result == null ||  result.length() ==0){
                OtaLog.LOGOta("===当前文件状态","当前文件读取出来的数据为空");
                userMap.put(OTA_STATE, DEFLUT_STATE);
                userMap.put(RECIPES_URL,"");
                return userMap;
            }
            int i = result.indexOf(DEPART);
            if(i == -1){    // 新文件夹
                userMap.put(OTA_STATE, DEFLUT_STATE);
                userMap.put(RECIPES_URL, "");
                OtaLog.LOGOta("===当前文件状态","当前为新文件夹，返回默认情况");
                return userMap;
            }
            String[] results = result.split(DEPART);
            //将数据存到map集合中

            if(results!=null && results.length == 2){
                userMap.put(OTA_STATE, results[0]);
                userMap.put(RECIPES_URL, results[1]);
                OtaLog.LOGOta("===当前文件状态","当前ota状态"+results[0]+",当前recipes_url"+results[1]);
            }else if(results.length == 1){
                userMap.put(OTA_STATE, results[0]);
                userMap.put(RECIPES_URL, "");
                OtaLog.LOGOta("===当前文件状态","当前ota状态 没有菜谱url"+results[0]+",当前recipes_url"+"");
            }


            return userMap;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            OtaLog.LOGOta("===当前文件状态","进入异常读取");
            OtaLog.LOGOta("===当前文件状态","当前ota状态 false  出现读取异常");
            File file12 = new File(FILE_STATE_DIR+FILE_STATE_NAME);
            if(!file12.exists()){
                try {
                    boolean result = file12.createNewFile();
                    OtaLog.LOGOta("===当前文件状态","创建文件3333"+result);

                } catch (IOException e1) {
                    e1.printStackTrace();
                    Map<String, String> userMap = new HashMap<String, String>();
                    userMap.put(OTA_STATE, "false");
                    userMap.put(RECIPES_URL, "");
                    return userMap;
                }
            }
            Map<String, String> userMap = new HashMap<String, String>();
            userMap.put(OTA_STATE, "false");
            userMap.put(RECIPES_URL, "");
            return userMap;
        }finally {
            if( fis != null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(isr != null){
                try {
                    isr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static void RemoveInfo(){
        File file1 = new File(FILE_STATE_DIR+FILE_STATE_NAME);
        if(!file1.exists()){
            final boolean delete = file1.delete();
        }
    }


}
