package beijing.hanhua.sketchpad.netty;

/**
 * Created by Administrator on 2016/6/20.
 */
public interface INettyClient {
    void connect();//1. 建立连接
    void sendMessage(int mt, String msg, long delayed);//2. 发送消息
    void addDataReceiveListener(OnDataReceiveListener listener);//3. 为不同的请求添加监听器
    //添加数据监听器，指定请求码
    void addDataReceiveListener(int requestCode, OnDataReceiveListener listener);
    //移除数据监听器
    void removeDataReceiveListener(int requestCode);
    interface OnDataReceiveListener {
        void onDataReceive(int mt, String json);//接收到数据时触发
    }

    interface OnConnectStatusListener {
        void onDisconnected();//连接异常时触发
    }
}
