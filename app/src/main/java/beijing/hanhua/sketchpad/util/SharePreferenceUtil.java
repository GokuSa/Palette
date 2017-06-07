package beijing.hanhua.sketchpad.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Administrator on 2016/6/27.
 */
public class SharePreferenceUtil {
    private  final String NAME="sketchpad";
    private SharePreferenceUtil(){}
    private static SharePreferenceUtil sSharePreferenceUtil;

    public static SharePreferenceUtil getInstance() {
        if (sSharePreferenceUtil == null) {
            sSharePreferenceUtil=new SharePreferenceUtil();

        }
        return sSharePreferenceUtil;
    }

    public void saveMac(Context context,String mac) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
       sharedPreferences.edit().putString("mac", mac).apply();
    }

    public String getMac(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("mac", "");
    }



}
