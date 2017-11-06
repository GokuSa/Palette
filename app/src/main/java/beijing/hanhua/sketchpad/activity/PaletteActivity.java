package beijing.hanhua.sketchpad.activity;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketException;

import beijing.hanhua.sketchpad.PaletteToolsFragment;
import beijing.hanhua.sketchpad.R;
import beijing.hanhua.sketchpad.databinding.ActivityPaletteBinding;
import beijing.hanhua.sketchpad.util.Common;

/**
 * todo 使用tabs+ViewPager + Fragment 实现画板控制栏
 * 解决Fragment 之间通信
 * 多信源 多用户 的画板绘制操作 有绘制直线 椭圆 自由绘制  文字 擦除 清空 基本操作
 * 每个信源对多个用户的操作
 * 所有信源共用两个画板，计划使用两个 SurfaceView 实现，
 * 一个单独绘制自己本地的操作，实时的，一个完整操作（action down 到 action up）结束需要发送到服务器给其他用户同步
 * 一个绘制其他所有用户的绘制数据，非实时，一个个完整的操作，只接受数据 绘制
 */
public class PaletteActivity extends AppCompatActivity {
    private static final String TAG = "PaletteActivity";
    public static final int DRAW_PATH = 0;
    public static final int DRAW_LINE = 5;
    public static final int DRAW_OVAL = 6;
    public static final int DRAW_TEXT = 7;
    public static final int SEND = 1;
    public static final int ERASE = 2;
    public static final int RESET = 3;
    public static final int CLEAR = 8;
    public static final int MSG_CHANGE_VIDEO_SORUCE = 9;
    public static final int MSG_UPDATE_OPERATION = 10;
    private ActivityPaletteBinding mBinding;
    private volatile boolean start = true;
    private DatagramSocket mDatagramSocket;
    private String host = "172.168.54.25";
    private int port = 10332;
    private int mUserid = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_palette);

        if (savedInstanceState == null) {
            PaletteToolsFragment paletteToolsFragment = PaletteToolsFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.palette_tools_container, paletteToolsFragment, "palette_tools")
                    .commit();
        }
//        切换信源时，两个画板成对切换
        mBinding.palette.changeVideoSource("one");
        mBinding.palettePublic.changeVideoSource("one");
        mBinding.palette.setActivityWeakReference(this);
        if (Common.GetIpAddress().equals(host)) {
            host = "172.168.1.156";
            mUserid = 2;
            mBinding.palette.setPaintColor(Color.YELLOW);
        }
        mBinding.palette.setUserId(mUserid);
        Log.d(TAG, host + " __ " + mUserid);
        new Thread() {
            @Override
            public void run() {
                startCommunication();
            }
        }.start();

    }


    private void startCommunication() {
        try {
            mDatagramSocket = new DatagramSocket(port);
            while (start) {
                byte[] buffer = new byte[1024 * 100];
                DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
                mDatagramSocket.receive(packet);
                String receiver = new String(buffer).trim();
                Log.d(TAG, "get operation " + receiver);
                mBinding.palettePublic.updateDrawingOperation(receiver);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }

    @WorkerThread
    public void send(String operation) {
        try {
            byte[] bytes = operation.getBytes();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, Inet4Address.getByName(host), port);
            mDatagramSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        start = false;
        if (mDatagramSocket != null) {
            mDatagramSocket.close();
        }
        mBinding.palette.exit();
        mBinding.palettePublic.exit();
    }


    //绘画类型选项的可见性切换
    public void switchPaintType(View view) {
//        mBinding.setIsPaintTypeVisible(!mBinding.getIsPaintTypeVisible());
    }

    /**
     * 设置画笔类型  自由绘制  画直线 画圆
     *
     * @param view
     */
    public void setDrawPaintType(View view) {
        view.requestFocusFromTouch();
        switch (view.getId()) {
            case R.id.tv_drawPath:
                mBinding.palette.setDrawPathMode();
                break;
            case R.id.tv_drawLine:
                mBinding.palette.setDrawLineMode();
                break;
            case R.id.tv_drawOval:
                mBinding.palette.setDrawOvalMode();
                break;
        }
    }

    public void operatePalette(View view) {
        view.requestFocusFromTouch();
        switch (view.getId()) {
            case R.id.tv_clearPalette:
                mBinding.palette.clearPaletteByHand();
                break;
            case R.id.tv_erase:
                mBinding.palette.setEraserMode();
                break;
            case R.id.tv_openPalette:
                finish();
//                mBinding.palettePublic.testLayer2();
                break;
        }
    }

    public void setDrawText(View view) {
        switch (view.getId()) {
            case R.id.tv_text_one:
                mBinding.palette.setDrawTextMode("神州视翰");
                break;
            case R.id.tv_text_two:
                mBinding.palette.setDrawTextMode("shine");
                break;
        }

    }

    public void changeVideoSouce(View view) {
        switch (view.getId()) {
            case R.id.btn_one:
                mBinding.palette.changeVideoSource("one");
                mBinding.palettePublic.changeVideoSource("one");
                mBinding.palette.setUserId(mUserid);
                break;
            case R.id.btn_two:
                mBinding.palette.changeVideoSource("two");
                mBinding.palette.setUserId(mUserid);
                mBinding.palettePublic.changeVideoSource("two");

                break;
            case R.id.btn_three:
                mBinding.palette.changeVideoSource("three");
                mBinding.palette.setUserId(mUserid);
                mBinding.palettePublic.changeVideoSource("three");

                break;
            case R.id.btn_four:
                mBinding.palette.changeVideoSource("four");
                mBinding.palette.setUserId(mUserid);
                mBinding.palettePublic.changeVideoSource("four");

                break;
        }
    }
}
