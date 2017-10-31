package beijing.hanhua.sketchpad.customview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import beijing.hanhua.sketchpad.entity.DrawingOperation;

/**
 * Created by Administrator on 2016/7/11.
 * 画板视图，
 * 因为SurfaceView可以在子线程更新视图，所以使用SurfaceView执行画画功能，
 * 具体有自由绘画，画直线，椭圆，擦除，清空功能
 * <p>
 * 为了节约内存 使用一个Bitamap绘制图形，所以在不同信源切换需要保存操作
 * 对于每个信源 ，有一个独一无二的id，在这个信源中
 * 从actionDown到actionUP定义为一个Operation，使用集合存储在内存中，
 * 这样在切换信源时先清空画板，再根据id对于的数据重绘
 */
public class PaletteView extends SurfaceView implements SurfaceHolder.Callback, Handler.Callback {
    private static final String TAG = PaletteView.class.getSimpleName();
    public static final int DRAW_PATH = 0;
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
    private volatile boolean mIsDrawing;
    private Paint mBitmapPaint;
    private Gson mGson = new Gson();

    //当前视频源
    private String mCurrentVideoSource;
    //   当前视频源绘制操作
    private List<DrawingOperation> mCurrentDrawingOperation;
    //画板集合，每个源对应一个画板,使用源地址作为键
    private ArrayMap<String, List<DrawingOperation>> mPaletteMap = new ArrayMap<>();
    private Paint mPaintErase;
    //需要传输的绘画信息
    private int mWidth;
    private int mHeight;
    private DrawingOperation mDrawingOperation = null;

    //默认绘制模式为自由画笔
    private int mDrawMode = DRAW_PATH;
    private int mDrawColor;
    private int mStrokeWidth;

    private String mText = "";

    public PaletteView(Context context) {
        super(context);
        init();
    }

    public PaletteView(Context context, AttributeSet attrs) {
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
        mPaint.setStrokeWidth(2);
        mPaint.setTextSize(30);

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
        setDefaultSetting();

        //使用Looper+handler无限循环处理绘画事件
        HandlerThread handlerThread = new HandlerThread("work");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(), this);
    }


    //设置绘图的默认设置
    private void setDefaultSetting() {
       /* mDrawMode = DRAW_TEXT;
        mDrawColor = Color.BLUE;
        mStrokeWidth=6;
        mPaint.setTextSize(20);*/
    }

    //设置视频源及对应的画板操作
    public void changeVideoSource(String url) {
        mCurrentVideoSource = url;
        clearPalette();
        if (mPaletteMap.containsKey(url)) {
            mCurrentDrawingOperation = mPaletteMap.get(url);
            executeDrawingOperation(mCurrentDrawingOperation);
        } else {
            mCurrentDrawingOperation = new ArrayList<>();
            mPaletteMap.put(url, mCurrentDrawingOperation);
        }
    }

    //根据保存的数据 重新绘制视频源的绘画操作
    private void executeDrawingOperation(List<DrawingOperation> drawingOperations) {
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();
            if (canvas != null) {
                for (DrawingOperation drawingOperation : drawingOperations) {
                    switch (drawingOperation.getDrawMode()) {
                        case DRAW_TEXT:
                            mCanvas.drawText(drawingOperation.getText(), drawingOperation.getPointDown().x, drawingOperation.getPointDown().y, mPaint);
                            break;
                        case DRAW_LINE: {
                            Point pointDown = drawingOperation.getPointDown();
                            Point pointCurrent = drawingOperation.getPointCurrent();
                            mCanvas.drawLine(pointDown.x, pointDown.y, pointCurrent.x, pointCurrent.y, mPaint);
                        }
                        break;
                        case DRAW_OVAL: {
                            Point pointDown = drawingOperation.getPointDown();
                            Point pointCurrent = drawingOperation.getPointCurrent();
                            RectF rectF = new RectF(pointDown.x, pointDown.y, pointCurrent.x, pointCurrent.y);
                            mCanvas.drawOval(rectF, mPaint);
                        }
                        break;
                        case DRAW_PATH: {
                            List<Point> points = drawingOperation.getPoints();
                            int size = points.size();
                            if (size > 2) {
                                mPath.reset();
                                mPath.moveTo(drawingOperation.getPointDown().x, drawingOperation.getPointDown().y);
                                for (int i = 0; i < size - 1; i++) {
                                    mPath.quadTo(points.get(i).x, points.get(i).y, points.get(i + 1).x, points.get(i + 1).y);
                                }
                                mCanvas.drawPath(mPath, mPaint);
                            }
                        }
                        break;
                        case ERASE: {
                            Point pointDown = drawingOperation.getPointDown();
                            Point pointCurrent = drawingOperation.getPointCurrent();
                            RectF rectF = new RectF(pointDown.x, pointDown.y, pointCurrent.x, pointCurrent.y);
                            mCanvas.drawRect(rectF, mPaintErase);
                        }
                        break;
                    }
                }
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            }
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

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
        Log.d(TAG, "surfaceCreated: ");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: ");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed: ");
    }

    public void exit() {
        mHandler.getLooper().quit();
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
        mSurfaceHolder.removeCallback(this);
    }

    @Override
    public boolean handleMessage(Message msg) {
        MotionEvent event = (MotionEvent) msg.obj;
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (msg.what) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                handleActionUp(x, y);
                break;
        }
        mOldX = x;
        mOldY = y;
        return true;
    }

    //在View中操作的触摸事件 统一交给HandlerThread处理
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mHandler.obtainMessage(event.getAction(), event).sendToTarget();
       /* int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //开始绘制标记
                mIsDrawing = true;
                mPath.reset();
                mPath.moveTo(x, y);
                mDrawingOperation = new DrawingOperation();
                mDrawingOperation.setPointDown(new Point(x, y));
                mDrawingOperation.setDrawMode(mDrawMode);
                mDrawingOperation.setDrawColor(mDrawColor);
                mDrawingOperation.setStrokeWidth(mStrokeWidth);
                mDrawingOperation.setText(mText);
                mCurrentDrawingOperation.add(mDrawingOperation);
                if (mDrawMode == DRAW_TEXT) {
                    handleDrawingOperation(mDrawingOperation);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mDrawMode == DRAW_LINE) {
                    Point pointCurrent = mDrawingOperation.getPointCurrent();
                    if (pointCurrent != null) {
                        pointCurrent.set(x, y);
                    } else {
                        pointCurrent = new Point(x, y);
                        mDrawingOperation.setPointCurrent(pointCurrent);
                    }
                    //绘制直线，先清空以前的绘制命令，再发送当前的，保证当前最新的优先执行，提高相应速度
                    mHandler.removeCallbacks(mDrawLine);
                    mHandler.post(mDrawLine);
                } else if (mDrawMode == DRAW_OVAL) {
                    Point pointCurrent = mDrawingOperation.getPointCurrent();
                    if (pointCurrent != null) {
                        pointCurrent.set(x, y);
                    } else {
                        pointCurrent = new Point(x, y);
                        mDrawingOperation.setPointCurrent(pointCurrent);
                    }
                    //绘制椭圆，先清空以前的绘制命令，再发送当前的，保证当前最新的优先执行，提高相应速度
                    mHandler.removeCallbacks(mDrawOval);
                    mHandler.post(mDrawOval);

                } else if (mDrawMode == DRAW_PATH) {
                    //设置绘制限制，移动超过2个像素才发送绘制命令和记录路径
                    if (Math.abs(x - mOldX) > 2 || Math.abs(y - mOldY) > 2) {
                        List<Point> points = mDrawingOperation.getPoints();
                        if (null == points) {
                            points = new ArrayList<>();
                            mDrawingOperation.setPoints(points);
                        }
                        mPath.quadTo(mOldX, mOldY, x, y);
                        points.add(new Point(x, y));
                        mHandler.post(mDrawPath);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "event:" + event);
                //结束绘制标记
                mIsDrawing = false;
                mPath.reset();
                //边界检查，确保x和y在画板大小范围内
                x = Math.max(0, Math.min(x, mWidth));
                y = Math.max(0, Math.min(y, mHeight));
                if (mDrawMode == ERASE) {
                    Log.d(TAG, "ease up");
                    mDrawingOperation.setPointCurrent(new Point(x,y));
                    mHandler.post(mEraser);
                } else if (mDrawMode == DRAW_LINE) {
                    Log.d(TAG, "line up");
                    mDrawingOperation.getPointCurrent().set(x, y);
                    mHandler.removeCallbacks(mDrawLine);
                    mHandler.post(mDrawLine);
                } else if (mDrawMode == DRAW_OVAL) {
                    Log.d(TAG, "oval up");
                    mDrawingOperation.getPointCurrent().set(x, y);
                    mHandler.removeCallbacks(mDrawOval);
                    mHandler.post(mDrawOval);
                }else if(mDrawMode == DRAW_PATH) {
                    //当手指离开界面，停止绘制时，清空消息队列中没执行的draw消息，否则会阻塞后续消息
                    mHandler.removeCallbacks(mDrawPath);
                }
//                mHandler.sendEmptyMessage(SEND);
                break;
        }
        mOldX = x;
        mOldY = y;*/
        return true;
    }

    /*
   *处理手指按下的一系列初始化
    *  1.重置绘画路径，并移动到手指触摸点
    *  2.清空之前记录的路径数据，添加最新的触摸点，这是发送给另一画板处理的关键数据
    *  3.如果当前模式是绘制文字，直接操作
    *  落点一定在画板范围内，否则不会接收到触摸事件
   */
    private void handleActionDown(int x, int y) {
        //开始绘制标记
        mIsDrawing = true;
        mPath.reset();
        mPath.moveTo(x, y);
        mDrawingOperation = new DrawingOperation();
        mDrawingOperation.setPointDown(new Point(x, y));
        mDrawingOperation.setDrawMode(mDrawMode);
        mDrawingOperation.setDrawColor(mDrawColor);
        mDrawingOperation.setStrokeWidth(mStrokeWidth);
        mDrawingOperation.setText(mText);
        mCurrentDrawingOperation.add(mDrawingOperation);
        if (mDrawMode == DRAW_TEXT) {
            drawText(mDrawingOperation);
        }
    }

    //    处理手指移动事件
    private void handleActionMove(int x, int y) {
        if (mDrawMode == DRAW_LINE) {
            Point pointCurrent = mDrawingOperation.getPointCurrent();
            if (pointCurrent != null) {
                pointCurrent.set(x, y);
            } else {
                pointCurrent = new Point(x, y);
                mDrawingOperation.setPointCurrent(pointCurrent);
            }
            //绘制直线，先清空以前的绘制命令，再发送当前的，保证当前最新的优先执行，提高相应速度
            drawLine(mDrawingOperation);
        } else if (mDrawMode == DRAW_OVAL) {
            Point pointCurrent = mDrawingOperation.getPointCurrent();
            if (pointCurrent != null) {
                pointCurrent.set(x, y);
            } else {
                pointCurrent = new Point(x, y);
                mDrawingOperation.setPointCurrent(pointCurrent);
            }
            //绘制椭圆，
            drawOval(mDrawingOperation);

        } else if (mDrawMode == DRAW_PATH) {
            //设置绘制限制，移动超过2个像素才发送绘制命令和记录路径
            if (Math.abs(x - mOldX) > 2 || Math.abs(y - mOldY) > 2) {
                List<Point> points = mDrawingOperation.getPoints();
                if (null == points) {
                    points = new ArrayList<>();
                    mDrawingOperation.setPoints(points);
                }
                mPath.quadTo(mOldX, mOldY, x, y);
                points.add(new Point(x, y));
                drawPath();
            }
        }
    }

    //    处理手指离开屏幕的事件
    private void handleActionUp(int x, int y) {
        //结束绘制标记
        mIsDrawing = false;
        mPath.reset();
        //边界检查，确保x和y在画板大小范围内
        x = Math.max(0, Math.min(x, mWidth));
        y = Math.max(0, Math.min(y, mHeight));
        if (mDrawMode == ERASE) {
            Log.d(TAG, "ease up");
            mDrawingOperation.setPointCurrent(new Point(x, y));
            erase(mDrawingOperation);
        } else if (mDrawMode == DRAW_LINE) {
            Log.d(TAG, "line up");
            mDrawingOperation.getPointCurrent().set(x, y);
            drawLine(mDrawingOperation);
        } else if (mDrawMode == DRAW_OVAL) {
            Log.d(TAG, "oval up");
            mDrawingOperation.getPointCurrent().set(x, y);
            drawOval(mDrawingOperation);
        }
//                mHandler.sendEmptyMessage(SEND);
    }

    //绘制文字
    private void drawText(DrawingOperation drawingOperation) {
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();
            mCanvas.drawText(drawingOperation.getText(), drawingOperation.getPointDown().x, drawingOperation.getPointDown().y, mPaint);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    //    动态绘制直线
    private void drawLine(DrawingOperation drawingOperation) {
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            Point pointDown = drawingOperation.getPointDown();
            Point pointCurrent = drawingOperation.getPointCurrent();
//            手指离开屏幕的最终直线绘制
            if (!mIsDrawing) {
                mCanvas.drawLine(pointDown.x, pointDown.y, pointCurrent.x, pointCurrent.y, mPaint);
            }
//            绘制已保存的图像，如果不绘制，以前的图像都被清空
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            //手指在移动过程,这是实现动态画线的主要操作
            if (mIsDrawing) {
                canvas.drawLine(pointDown.x, pointDown.y, pointCurrent.x, pointCurrent.y, mPaint);
            }
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    //    绘制椭圆
    private void drawOval(DrawingOperation drawingOperation) {
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            Point pointDown = drawingOperation.getPointDown();
            Point pointCurrent = drawingOperation.getPointCurrent();
            RectF rectF = new RectF(pointDown.x, pointDown.y, pointCurrent.x, pointCurrent.y);
//            mRectF.set(mPointX.get(0), mPointY.get(0), msg.arg1, msg.arg2);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            //这是最终一绘
            if (!mIsDrawing) {
                mCanvas.drawOval(rectF, mPaint);
            }
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            //同直线绘制
            if (mIsDrawing) {
                canvas.drawOval(rectF, mPaint);
            }
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    //    即时的绘制路径
    private void drawPath() {
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();
            mCanvas.drawPath(mPath, mPaint);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    //擦除操作，手指起点和落点之间的矩形区域会被擦除 todo 显示矩形区域
    private void erase(DrawingOperation drawingOperation) {
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();
            Point pointDown = drawingOperation.getPointDown();
            Point pointCurrent = drawingOperation.getPointCurrent();
            RectF rectF = new RectF(pointDown.x, pointDown.y, pointCurrent.x, pointCurrent.y);
            mCanvas.drawRect(rectF, mPaintErase);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }


    private Runnable mDrawLine = new Runnable() {
        @Override
        public void run() {
            drawLine(mDrawingOperation);
        }
    };
    private Runnable mDrawOval = new Runnable() {
        @Override
        public void run() {
            drawOval(mDrawingOperation);
        }
    };
    private Runnable mDrawPath = new Runnable() {
        @Override
        public void run() {
            drawPath();
        }
    };
    private Runnable mEraser = new Runnable() {
        @Override
        public void run() {
            erase(mDrawingOperation);
        }
    };

    //清空画板
    private void clearPalette() {
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    //    手动清空 需要先清空缓存的数据
    public void clearPaletteByHand() {
        mCurrentDrawingOperation.clear();
        clearPalette();
    }


    private void send() {
//        String message = mGson.toJson(mData);
//        Log.d(TAG, message);
//        NettyClient.getInstance().sendMessage(5, message, 0);
    }


    public void setEraserMode() {
        mDrawMode = ERASE;
    }

    public void setDrawPathMode() {
        mDrawMode = DRAW_PATH;
    }

    public void setPaintColor(int color) {
        mPaint.setColor(color);
    }

    public void setPaintWidth(int width) {
        mPaint.setStrokeWidth(width);
    }

    public void setDrawTextMode(String text) {
        mDrawMode = DRAW_TEXT;
        mText = text;
    }

    public void setDrawLineMode() {
        mDrawMode = DRAW_LINE;
    }

    public void setDrawOvalMode() {
        mDrawMode = DRAW_OVAL;
    }


}
