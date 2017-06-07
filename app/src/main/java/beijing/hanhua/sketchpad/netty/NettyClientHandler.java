package beijing.hanhua.sketchpad.netty;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import beijing.hanhua.sketchpad.app.MyApp;
import beijing.hanhua.sketchpad.entity.Data;
import beijing.hanhua.sketchpad.util.SharePreferenceUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by Administrator on 2016/6/20.
 */
@ChannelHandler.Sharable
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    private final String TAG = "NettyClientHandler";
    private List<INettyClient.OnDataReceiveListener> listeners = new ArrayList<>();
    //服务器返回数据监听集合，使用请求码作为键值
    private SparseArray<INettyClient.OnDataReceiveListener> mListenerSparseArray = new SparseArray<>();
    private Handler mHandler;
    private boolean isSaved = false;
    //是否曾经链接过，服务器重启会导致重连，而以前的队列依然在发心跳，所以只要连接成功能过就不再发心跳信号
    private boolean isConnected=false;
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.d(TAG, "channel active");
        //清楚其余连接请求
        NettyClient.getInstance().removeConnectMessage();
        if (!isConnected) {
            mHandler = new Handler(Looper.getMainLooper());
            Data data=new Data();
            data.setMac( SharePreferenceUtil.getInstance().getMac(MyApp.getContext()));
            data.setAction("fresh");
            Log.d("NettyClientHandler", "data:" + data);
            NettyClient.getInstance().sendMessage(NettyClient.MESSAGE_BEAT, new Gson().toJson(data), 0);
            isConnected=true;
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String result = (String) msg;
        Log.d(TAG, "verify : " + result);
        JSONObject jsonObject = new JSONObject(result);
        String action = jsonObject.optString("action");
        switch (action) {
            case "fresh":

                break;
            default:
                callListeners(0,result);
                break;

        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        Log.d(TAG, "Unexpected exception from downstream : " + cause.getMessage());

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
    }

    //回调 void onDataReceive(int mt, String json);方法
    private void callListeners(final int msgType, final String json) {
        final INettyClient.OnDataReceiveListener listener = mListenerSparseArray.get(msgType);
        if (listener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onDataReceive(msgType, json);
                }
            });
        }

    }


    //绑定OnDataReceiveListener
    @Deprecated
    public void addDataReceiveListener(INettyClient.OnDataReceiveListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    //添加数据返回监听器
    public void putDataListener(int requestCode, INettyClient.OnDataReceiveListener listener) {
        mListenerSparseArray.put(requestCode, listener);
    }

    //移除数据返回监听器
    public void removeListener(int requestCode) {
        mListenerSparseArray.remove(requestCode);
    }


}
