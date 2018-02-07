package com.fotile.c2i.ota.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
    public final static String FILE_STATE_DIR =   Environment.getExternalStorageDirectory().getPath() +"/fotile/";
    public final static String FILE_STATE_NAME =  "info.txt";
    public final static String DEPART = "######";
    /**当前ota状态**/
    public final static String OTA_STATE = "ota_state";
    public final static String RECIPES_URL = "recipes_url";
    public static boolean NEW_STATE = false;
    public static boolean isNewState() {
        return NEW_STATE;
    }

    public static void setNewState(boolean newState) {
        NEW_STATE = newState;
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
        return Boolean.valueOf(map.get(OTA_STATE));
//        SharedPreferences sharedPreferences = context.getSharedPreferences(NEW_STATE_SHARED,MODE_PRIVATE);
//        return sharedPreferences.getBoolean(NEW_STATE_NAME,false);
    }
    /**获取菜谱flag**/
    public static String getRecipesUrl(){
        Map<String,String> map =HttpUtil.getStateFromFile();
        return map.get(RECIPES_URL);
//        SharedPreferences sharedPreferences = context.getSharedPreferences(NEW_STATE_SHARED,MODE_PRIVATE);
//        return sharedPreferences.getBoolean(NEW_STATE_NAME,false);
    }

    public static boolean setStateToFile(boolean ota_flag,String recipes_url){
        File file = new File(FILE_STATE_DIR);
        if(!file.exists()){
            OtaLog.LOGOta("===当前文件状态","创建文件夹");
            file.mkdirs();
        }
        File file1 = new File(FILE_STATE_DIR+FILE_STATE_NAME);
        if(!file1.exists()){
            OtaLog.LOGOta("===当前文件状态","创建文件");
            try {
                file1.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //文件输出流
            FileOutputStream fos = new FileOutputStream(file1);
            //写数据
            fos.write((String.valueOf(ota_flag) + DEPART + recipes_url).getBytes());

            //关闭文件流
            fos.close();
            return true;
        } catch (Exception e) {
            OtaLog.LOGOta("===当前文件状态","失败1111");
            e.printStackTrace();
            return false;
        }
    }
    /**
     * @param null
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
        }
        File file1 = new File(FILE_STATE_DIR+FILE_STATE_NAME);
        if(!file1.exists()){
            try {
                OtaLog.LOGOta("===当前文件状态","创建文件2222");
                file1.createNewFile();
            } catch (IOException e) {
                OtaLog.LOGOta("===当前文件状态","创建文件2222失败");
                e.printStackTrace();
            }
        }
        try {
            //输入流
            FileInputStream fis = new FileInputStream(file1);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            //读取文件中的内容
            String result = br.readLine();
            //拆分成String[]
            String[] results = result.split(DEPART);
            //将数据存到map集合中
            Map<String, String> userMap = new HashMap<String, String>();
            if(results!=null && results.length == 2){
                userMap.put(OTA_STATE, results[0]);
                userMap.put(RECIPES_URL, results[1]);
                OtaLog.LOGOta("===当前文件状态","当前ota状态"+results[0]+",当前recipes_url"+results[1]);
            }else if(results.length == 1){
                userMap.put(OTA_STATE, results[0]);
                userMap.put(RECIPES_URL, "");
                OtaLog.LOGOta("===当前文件状态","当前ota状态"+results[0]+",当前recipes_url"+"");
            }
            return userMap;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            OtaLog.LOGOta("===当前文件状态","当前ota状态 false ");
            Map<String, String> userMap = new HashMap<String, String>();
            userMap.put(OTA_STATE, "false");
            userMap.put(RECIPES_URL, "");
            return userMap;
        }
    }

}
