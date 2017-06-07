package beijing.hanhua.sketchpad.netty;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.net.InetSocketAddress;

import beijing.hanhua.sketchpad.app.MyApp;
import beijing.hanhua.sketchpad.util.Common;
import beijing.hanhua.sketchpad.util.SharePreferenceUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.LineEncoder;
import io.netty.handler.codec.string.LineSeparator;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * Created by Administrator on 2016/6/20.
 */
public class NettyClient implements INettyClient{
    private final String TAG = "NettyClient";
    private static NettyClient mInstance;
    private Bootstrap bootstrap;
    private Channel channel;
    private String host;
    private Handler mWorkHandler = null;
    private NettyClientHandler nettyClientHandler;
    private static final int MESSAGE_INIT = 0x1;
    private static final int MESSAGE_CONNECT = 0x2;
    private static final int MESSAGE_SEND = 0x3;
    public static final int MESSAGE_BEAT = 0x4;
    //应用是否活跃状态
//    private boolean isAlive=false;
    private Handler.Callback mWorkHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_INIT: {
                   initNetty();
                    break;
                }
                case MESSAGE_CONNECT: {
                   connectServer();
                    break;
                }
                case MESSAGE_SEND: {
                    sendRequest(msg);
                    break;
                }
            }
            return true;
        }
    };

    private NettyClient() {
        initMessageThread();
    }

    public synchronized static NettyClient getInstance() {
        if (mInstance == null)
            mInstance = new NettyClient();
        return mInstance;
    }

    //初始化接受消息的线程和处理消息的Handler
    private void initMessageThread() {
        IniReaderNoSection inir = new IniReaderNoSection("/data/work/show/system/network.ini");
        host=inir.getIniKey("commuip");
        HandlerThread workThread = new HandlerThread(NettyClient.class.getName());
        workThread.start();
        mWorkHandler = new Handler(workThread.getLooper(), mWorkHandlerCallback);
        nettyClientHandler = new NettyClientHandler();
        mWorkHandler.sendEmptyMessage(MESSAGE_INIT);
    }
    //初始化Netty相关对象
    private void initNetty() {
        NioEventLoopGroup group = new NioEventLoopGroup();
            bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    // Decoders
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024 * 1024 * 1024));
                    pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
                    //encoders
                    pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));
                    pipeline.addLast("lineEncoder", new LineEncoder(LineSeparator.UNIX, CharsetUtil.UTF_8));
                    pipeline.addLast(nettyClientHandler);

                }
            });
        }


    @Override
    public void connect() {
        mWorkHandler.sendEmptyMessage(MESSAGE_CONNECT);
    }

    //连接到服务器
    private void connectServer() {
        String mac = Common.getMacAddress();
        if (TextUtils.isEmpty(mac) ) {
            Toast.makeText(MyApp.getContext(), "没有获取到本机Mac地址", Toast.LENGTH_SHORT).show();
            return;
        }
        //存储mac
        SharePreferenceUtil.getInstance().saveMac(MyApp.getContext(),mac);
        try {
            if (TextUtils.isEmpty(host) ) {
                throw new Exception("Netty host is invalid");
            }
            channel = bootstrap.connect(new InetSocketAddress(host, 5022)).sync().channel();

        } catch (Exception e) {
            Log.e(TAG, "connect failed  " + e.getMessage() + "  reconnect delay: " +5000);
            sendReconnectMessage();
        }
    }

    //发送请求到服务器
    private void sendRequest(Message msg) {
        String sendMsg= (String) msg.obj;
        int msgType=msg.arg1;
        try {
            if (channel != null && channel.isOpen()) {
                channel.writeAndFlush(sendMsg).sync();
                Log.d(TAG, "send succeed " + sendMsg);
            } else {
                throw new Exception("channel is null | closed");
            }
        } catch (Exception e) {
            Log.d(TAG, "handleMessage: "+ e.getMessage());
//            Toast.makeText(MyApp.getContext(), "网络异常，正在尝试连接", Toast.LENGTH_SHORT).show();
            sendReconnectMessage();
        } finally {
            if (MESSAGE_BEAT == msgType){
                sendMessage(MESSAGE_BEAT, sendMsg, 5000);
            }
        }
    }

    public void removeConnectMessage() {
        mWorkHandler.removeMessages(MESSAGE_CONNECT);
    }
    @Override
    public void addDataReceiveListener(OnDataReceiveListener listener) {
        if (nettyClientHandler != null)
            nettyClientHandler.addDataReceiveListener(listener);
    }

    @Override
    public void addDataReceiveListener(int requestCode, OnDataReceiveListener listener) {
        if (nettyClientHandler != null)
            nettyClientHandler.putDataListener(requestCode,listener);
    }

    /*所有添加监听的地方在销毁前都要注销监听以防内存泄漏*/
    @Override
    public void removeDataReceiveListener(int requestCode) {
        if (nettyClientHandler != null) {
            nettyClientHandler.removeListener(requestCode);
        }
    }

    private void sendReconnectMessage() {
        Log.d(TAG, "reconnect");
        mWorkHandler.sendEmptyMessageDelayed(MESSAGE_CONNECT,10*1000);
    }

    @Override
    public void sendMessage(int mt, String msg, long delayed) {
        if (TextUtils.isEmpty(msg))
            return;
        Message message = mWorkHandler.obtainMessage();
        message.what = MESSAGE_SEND;
        message.obj=msg;
        message.arg1=mt;
        mWorkHandler.sendMessageDelayed(message, delayed);
    }

   /* public void setAlive(boolean flag) {
        isAlive=flag;
    }*/
    /*如何退出应用时停止发送请求，启动又重新发送*/
    public void exit() {
        if (mInstance != null) {
            mInstance=null;
        }
        if (mWorkHandler != null) {
            mWorkHandler.removeCallbacksAndMessages(null);
            mWorkHandler.getLooper().quit();
        }
        if (channel != null) {
            channel.close();
        }
    }

}
