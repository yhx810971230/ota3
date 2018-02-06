package com.fotile.c2i.ota.util;

import android.content.Context;
import android.content.SharedPreferences;

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
    /**获取flag**/
    public static boolean isNewState(final Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(NEW_STATE_SHARED,MODE_PRIVATE);
        return sharedPreferences.getBoolean(NEW_STATE_NAME,false);
    }
}
