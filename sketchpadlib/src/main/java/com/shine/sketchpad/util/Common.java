package com.shine.sketchpad.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


/**
 * Created by Administrator on 2016/6/17.
 * 常用工具
 */
public class Common {
    private static final String TAG = "Common";

    /**
     * 获得屏幕高度
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    /**
     * 获得屏幕宽度
     *
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    public static int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }



    /**
     * 获取MAC地址
     *
     * @return
     */
    public static String getMacAddress() {
        String strMacAddr = "";
        try {
            NetworkInterface NIC = NetworkInterface.getByName("eth0");
            //6个字节，48位
            byte[] bytes = NIC.getHardwareAddress();
            StringBuilder buffer = new StringBuilder();
            for (byte b : bytes) {
                String str = Integer.toHexString(b & 0xff);
                buffer.append(str.length() == 1 ? 0 + str : str);
            }
            strMacAddr = buffer.toString().toUpperCase();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Mac Address : " + strMacAddr);
        return strMacAddr;
    }

    /**
     * 获取ip地址
     * @return
     */
    public static String GetIpAddress()  {
        String ip = "";
        try {
            Enumeration<NetworkInterface>  netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface intf = netInterfaces.nextElement();
                if (intf.getName().toLowerCase().equals("eth0") || intf.getName().toLowerCase().equals("wlan0")) {
                    Enumeration<InetAddress> inetAddresses = intf.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        //不是环回地址,不是ip6地址
                        if (!inetAddress.isLoopbackAddress()&&!inetAddress.getHostAddress().contains("::")) {
                            ip = inetAddress.getHostAddress();
                            Log.d(TAG, ip);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ip;
    }


    public static boolean isNetworkAvailable(Context context) {
        boolean result=false;
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        result=activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        if (!result) {
            Toast.makeText(context, "网络不可连接", Toast.LENGTH_SHORT).show();
        }
        return result;
    }
}