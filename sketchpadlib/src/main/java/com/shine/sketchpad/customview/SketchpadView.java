package com.shine.sketchpad.customview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.gson.Gson;
import com.shine.sketchpad.entity.Data;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Administrator on 2016/7/11.
 * 画板视图，
 * 因为SurfaceView可以在子线程更新视图，所以使用SurfaceView执行画画功能，
 * 具体有自由绘画，画直线，椭圆，擦除，清空功能
 */
public class SketchpadView extends SurfaceView implements SurfaceHolder.Callback, Handler.Callback {
    private static final String TAG = "SketchpadView";
    public static final int DRAW = 0;
    public static final int SEND = 1;
    public static final int ERASE = 2;
    private static final int RESET = 3;
    private static final int ACTION_DOWN = 4;
    private static final int DRAW_LINE = 5;
    private static final int DRAW_OVAL = 6;
    private static final int DRAW_TEXT = 7;
    private static final int CLEAR = 8;
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private int mOldX;
    private int mOldY;
    private Handler mHandler;
    private boolean mIsDrawing;
    private Paint mBitmapPaint;
    private Gson mGson = new Gson();
    //擦除区域，由手指落点和起点确定
    private Rect mRect = new Rect();
    //椭圆区域，由手指落点和起点确定
    private RectF mRectF = new RectF();
    //路径集合
    private List<Integer> mPointX = new ArrayList<>();
    private List<Integer> mPointY = new ArrayList<>();
    private Paint mPaintErase;
    //需要传输的绘画信息
    private Data mData = new Data();
    private int mWidth;
    private int mHeight;

    public SketchpadView(Context context) {
        super(context);
        init();
    }

    public SketchpadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mSurfaceHolder = getHolder();
        setZOrderOnTop(true);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        mSurfaceHolder.addCallback(this);
        //绘画画笔
        mPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLUE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(6);
        //擦除画笔
        mPaintErase = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        mPaintErase.setColor(Color.TRANSPARENT);
        mPaintErase.setStyle(Paint.Style.FILL);
        mPaintErase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        //路径
        mPath = new Path();
        //mBitmap是记录路径的载体，用此画笔绘制
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        //初始化默认绘画信息
        mData.setAction("drawing");
        mData.setxList(mPointX);
        mData.setyList(mPointY);
        mData.setColor(Color.BLUE);
        mData.setWidth(6);

    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        //在测量此画板大小时，创建一个与其同大的Bitmap，它是所有绘制图形的载体
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        //以mbitmap为内容的画布,用来记录以前的绘制图形，
        mCanvas = new Canvas(mBitmap);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //使用Looper+handler无限循环处理绘画事件
        HandlerThread handlerThread = new HandlerThread("work");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(), this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder.removeCallback(this);
        mHandler.getLooper().quit();
    }

    //在View中操作的触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //开始绘制标记
                mIsDrawing = true;
                //使用message queue依次处理数据，防止并非修改
                mHandler.obtainMessage(ACTION_DOWN, x, y).sendToTarget();
                break;
            case MotionEvent.ACTION_MOVE:
                //绘制路径
                if (mode == DRAW) {
                    //设置绘制限制，移动超过2个像素才发送绘制命令和记录路径
                    if (Math.abs(x - mOldX) > 2 || Math.abs(y - mOldY) > 2) {
                        mPath.quadTo(mOldX, mOldY, x, y);
                        mPointX.add(x);
                        mPointY.add(y);
                        mHandler.sendEmptyMessage(DRAW);
                    }
                } else if (mode == DRAW_LINE) {
                    //绘制直线，先清空以前的绘制命令，再发送当前的，保证当前最新的优先执行，提高相应速度
                    mHandler.removeMessages(DRAW_LINE);
                    mHandler.obtainMessage(DRAW_LINE, x, y).sendToTarget();
                } else if (mode == DRAW_OVAL) {
                    //绘制椭圆，先清空以前的绘制命令，再发送当前的，保证当前最新的优先执行，提高相应速度
                    mHandler.removeMessages(DRAW_OVAL);
                    mHandler.obtainMessage(DRAW_OVAL, x, y).sendToTarget();
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "event:" + event);
                //结束绘制标记
                mIsDrawing = false;
                mPath.reset();
                //当手指离开界面，停止绘制时，清空消息队列中没执行的draw消息，否则会阻塞后续消息
                if (mode == DRAW) {
                    mHandler.removeMessages(DRAW);
                }
                //边界检查，确保x和y在画板大小范围内
                x = Math.max(0, Math.min(x, mWidth));
                y = Math.max(0, Math.min(y, mHeight));
                if (mode == ERASE) {
                    Log.d(TAG, "ease up");
                    mHandler.obtainMessage(ERASE, x, y).sendToTarget();
                } else if (mode == DRAW_LINE) {
                    Log.d(TAG, "line up");
                    mHandler.obtainMessage(DRAW_LINE, x, y).sendToTarget();
                } else if (mode == DRAW_OVAL) {
                    Log.d(TAG, "oval up");
                    mHandler.obtainMessage(DRAW_OVAL, x, y).sendToTarget();
                }
                mHandler.sendEmptyMessage(SEND);
                break;
        }
        mOldX = x;
        mOldY = y;
        return true;
    }

    //此回调是专门处理手势操作的，使用HandlerThread开启的子线程循环不断处理Handler 发来的消息
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case ACTION_DOWN:
                onActionDown(msg);
                break;
            case ERASE:
                executeDraw(ERASE, msg);
                break;
            case SEND:
                send();
                break;
            case DRAW:
                executeDraw(DRAW, null);
                break;
            case DRAW_LINE:
                executeDraw(DRAW_LINE, msg);
                break;
            case DRAW_OVAL:
                executeDraw(DRAW_OVAL, msg);
                break;
            case RESET:
                setDrawMode();
                break;
        }
        return true;
    }

    /*
    *处理手指按下的一系列初始化
     *  1.重置绘画路径，并移动到手指触摸点
     *  2.清空之前记录的路径数据，添加最新的触摸点，这是发送给另一画板处理的关键数据
     *  3.如果当前模式是绘制文字，直接操作
     *  落点一定在画板范围内，否则不会接收到触摸事件
    */
    private void onActionDown(Message msg) {
        mPath.reset();
        mPath.moveTo(msg.arg1, msg.arg2);
        mPointX.clear();
        mPointY.clear();
        mPointX.add(msg.arg1);
        mPointY.add(msg.arg2);
        if (mode == DRAW_TEXT) {
            executeDraw(DRAW_TEXT, msg);
        }
    }

    /*
    * 绘制操作的中间层，主要用来封装canvas异常处理*/
    public void executeDraw(int action, Message message) {
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();
            switch (action) {
                case DRAW:
                    drawPath(canvas);
                    break;
                case DRAW_LINE:
                    drawLine(canvas, message);
                    break;
                case DRAW_TEXT:
                    drawText(canvas, message.arg1, message.arg2);
                    break;
                case DRAW_OVAL:
                    drawOval(canvas,message);
                    break;
                case ERASE:
                    erase(canvas,message);
                    break;
                case CLEAR:
                    clear(canvas);
                    break;
            }
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    //绘制直线
    private void drawLine(Canvas canvas, Message msg) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        //手指离开界面，确定最终执行路径,有时候会执行两次？
        if (!mIsDrawing) {
            mCanvas.drawLine(mPointX.get(0), mPointY.get(0), msg.arg1, msg.arg2, mPaint);
            mPointX.add(msg.arg1);
            mPointY.add(msg.arg2);
        }
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        //手指在移动过程,这是实现动态画线的主要操作
        if (mIsDrawing) {
            canvas.drawLine(mPointX.get(0), mPointY.get(0), msg.arg1, msg.arg2, mPaint);
        }
    }

    //绘制椭圆
    private void drawOval(Canvas canvas, Message msg) {
        //先确定椭圆的坐标
        mRectF.set(mPointX.get(0), mPointY.get(0), msg.arg1, msg.arg2);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        //同绘制直线，这是最终一绘
        if (!mIsDrawing) {
            Log.d(TAG, "add point");
            mCanvas.drawOval(mRectF, mPaint);
            mPointX.add(msg.arg1);
            mPointY.add(msg.arg2);
        }
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        //同直线绘制
        if (mIsDrawing) {
            canvas.drawOval(mRectF, mPaint);
        }
    }


    //绘制路径
    private void drawPath(Canvas canvas) {
        mCanvas.drawPath(mPath, mPaint);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }

    //绘制文字
    private void drawText(Canvas canvas, int x, int y) {
        mCanvas.drawText(mData.getText(), x, y, mPaint);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }

    //清空选择区域
    private void erase(Canvas canvas, Message msg) {
        //记录起手点
        mPointX.add(msg.arg1);
        mPointY.add(msg.arg2);
        mRect.set(mPointX.get(0), mPointY.get(0), msg.arg1, msg.arg2);
        mCanvas.drawRect(mRect, mPaintErase);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }


    public void clear(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mData.setAction("clear");
        mHandler.sendEmptyMessage(SEND);
        mHandler.sendEmptyMessage(RESET);
    }

    private void send() {
        String message = mGson.toJson(mData);
        Log.d(TAG, message);
        NettyClient.getInstance().sendMessage(5, message, 0);
    }

    int mode = DRAW;

    public void setEraseMode() {
        mode = ERASE;
        mData.setAction("erase");
    }

    public void setDrawMode() {
        mode = DRAW;
        mPaint.setColor(mData.getColor());
        mPaint.setStrokeWidth(mData.getWidth());
        mData.setAction("drawing");

    }

    public void setPaintColor(int color) {
        mPaint.setColor(color);
        mData.setColor(color);
    }

    public void setPaintWidth(int width) {
        mPaint.setStrokeWidth(width);
        mData.setWidth(width);
    }

    public void setDrawTextMode(String text) {
        mode = DRAW_TEXT;
        mData.setText(text);
        mPaint.setTextSize(20);
        mData.setAction("drawtext");
    }

    public void setDrawLineMode() {
        mode = DRAW_LINE;
        mData.setAction("drawline");
    }

    public void setDrawOvalMode() {
        mode = DRAW_OVAL;
        mData.setAction("drawoval");
    }

}
