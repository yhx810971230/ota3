package com.fotile.c2i.ota.util;

/**
 * @author ： panyw .
 * @date ：2018/2/5 18:05
 * @COMPANY ： Fotile智能厨电研究院
 * @description ：
 */

public class HttpUtil {

    public static boolean NEW_STATE = false;
    public static boolean isNewState() {
        return NEW_STATE;
    }

    public static void setNewState(boolean newState) {
        NEW_STATE = newState;
    }

}
