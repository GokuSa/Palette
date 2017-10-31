package beijing.hanhua.sketchpad.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.google.gson.Gson;

import beijing.hanhua.sketchpad.R;
import beijing.hanhua.sketchpad.customview.BoardView;
import beijing.hanhua.sketchpad.customview.SketchpadView;
import beijing.hanhua.sketchpad.entity.Data;
import beijing.hanhua.sketchpad.netty.INettyClient;
import beijing.hanhua.sketchpad.netty.NettyClient;

/*
* 画板控制界面，适用一对一场景，如果是多用户，需动态添加多个BoardView
* 使用2个画板，一个接受端，一个发送端
* 1.本画板的绘制，加入服务器，发送本画板数据到服务器
* 2.接受服务器数据，同步发送端画板
* 3.退出，注销画板
*
* 橡皮擦、清除只能针对自己的画板
* 创建者的清空直接清空所有画板包括参与者
* 视频源关联绘画内容，也就是切换视频源需要同时切换画板内容，打算采用sqlite存储不同绘画数据，切换时即时绘制
*
*/
public class SketchpadActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "SketchpadActivity";
    public static final int[] COLORS = new int[]{Color.BLUE, Color.BLACK, Color.rgb(0, 255, 255), Color.RED, Color.YELLOW};
    public static final int[] WIDTH = new int[]{4, 6, 8, 10, 12};
    private SketchpadView mSketchpadView;
    private Gson mGson = new Gson();
    private BoardView mBoardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sketchpad);
        initView();
        NettyClient.getInstance().connect();
        //监听服务端返回的数据
        NettyClient.getInstance().addDataReceiveListener(0, mOnDataReceiveListener);
    }


    private void initView() {
        mSketchpadView = (SketchpadView) findViewById(R.id.sketchpad);
        mBoardView = (BoardView) findViewById(R.id.board);
        Spinner spinner = (Spinner) findViewById(R.id.color);
        spinner.setOnItemSelectedListener(this);
        Spinner line = (Spinner) findViewById(R.id.line);
        line.setOnItemSelectedListener(this);
        Spinner action = (Spinner) findViewById(R.id.action);
        action.setOnItemSelectedListener(this);
        Spinner word = (Spinner) findViewById(R.id.word);
        word.setOnItemSelectedListener(this);
        findViewById(R.id.btn_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String itme = (String) parent.getItemAtPosition(position);

        switch (parent.getId()) {
            case R.id.color:
                mSketchpadView.setPaintColor(COLORS[position]);
                break;
            case R.id.line:
                mSketchpadView.setPaintWidth(WIDTH[position]);
                break;
            case R.id.word:
                if (position != 0) {
                    mSketchpadView.setDrawTextMode(itme);
                }
                break;
            case R.id.action:
                switch (position) {
                    case 1:
                        Log.d(TAG, "set drawmode");
                        mSketchpadView.setDrawMode();
                        break;
                    case 2:
                        mSketchpadView.setEraseMode();
                        break;
                    case 3:
                        Log.d(TAG, "clear");
                        mSketchpadView.executeDraw(8,null);
                        parent.setSelection(0);
                        break;
                    case 4:
                        mSketchpadView.setDrawLineMode();
                        break;
                    case 5:
                        mSketchpadView.setDrawOvalMode();
                        break;
                }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }


    private INettyClient.OnDataReceiveListener mOnDataReceiveListener = new INettyClient.OnDataReceiveListener() {
        @Override
        public void onDataReceive(int mt, String json) {
            Data data = mGson.fromJson(json, Data.class);
            mBoardView.getHandler().obtainMessage(0,data).sendToTarget();
        }
    };

    @Override
    protected void onDestroy() {
        NettyClient.getInstance().removeDataReceiveListener(0);
        NettyClient.getInstance().exit();
        super.onDestroy();
    }
}
