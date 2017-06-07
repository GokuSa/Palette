package beijing.hanhua.sketchpad.app;

import android.app.Application;
import android.content.Context;

/**
 * Created by Administrator on 2016/7/12.
 */
public class MyApp extends Application{
    private static Context sContext;
    @Override
    public void onCreate() {
        super.onCreate();
        sContext=getApplicationContext();
    }

    public static Context getContext() {
        return sContext;
    }
}
