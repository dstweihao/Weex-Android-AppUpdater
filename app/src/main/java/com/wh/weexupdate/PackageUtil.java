package com.wh.weexupdate;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/*
 * 创建者 韦豪
 * 创建时间 2018/7/31 9:31
 */
public class PackageUtil {

    public static String getVersionName(Context context){

        PackageManager pm = context.getPackageManager();
        /**
         * flags:标志位，0标志包的基本信息
         */
        try {
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            String versionName = info.versionName;
            return versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "未知版本名";
    }

    public static int getVersionCode(Context context){

        PackageManager pm = context.getPackageManager();
        /**
         * flags:标志位，0标志包的基本信息
         */
        try {
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            int versionCode = info.versionCode;

            return versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 1;
    }
}
