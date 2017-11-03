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
import android.support.annotation.WorkerThread;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import beijing.hanhua.sketchpad.entity.DrawingOperation;

import static beijing.hanhua.sketchpad.activity.PaletteActivity.CLEAR;
import static beijing.hanhua.sketchpad.activity.PaletteActivity.DRAW_LINE;
import static beijing.hanhua.sketchpad.activity.PaletteActivity.DRAW_OVAL;
import static beijing.hanhua.sketchpad.activity.PaletteActivity.DRAW_PATH;
import static beijing.hanhua.sketchpad.activity.PaletteActivity.DRAW_TEXT;
import static beijing.hanhua.sketchpad.activity.PaletteActivity.ERASE;
import static beijing.hanhua.sketchpad.activity.PaletteActivity.MSG_CHANGE_VIDEO_SORUCE;
import static beijing.hanhua.sketchpad.activity.PaletteActivity.MSG_UPDATE_OPERATION;

/**
 * Created by Administrator on 2016/7/11.
 * 公共画板视图，专门绘制除当前用户外其他所有用户的操作
 * 因为SurfaceView可以在子线程更新视图，所以使用SurfaceView执行画画功能，
 * 具体有自由绘画，画直线，椭圆，擦除，清空功能
 * <p>
 * 为了节约内存 使用一个Bitamap绘制图形，所以在不同信源切换需要保存操作
 * 对于每个信源 ，有一个独一无二的字符串，在这个信源中
 * 从actionDown到actionUP定义为一个Operation，使用集合缓存在内存中，
 * 这样在切换信源时先清空画板，再根据string对于的数据重绘所有用户操作
 * <p>
 * 数据更新和绘制更新分离
 * 因为是多个信源多个用户的操作，如果收到的数据更新不是当前信源，则只更新缓存而不绘制
 * 当绘制数据是当前信源的才绘制
 * <p>
 * 绘制多用户擦除操作  使用layer离屏单独操作 再合并到屏幕上
 */
public class PublicPalette extends SurfaceView implements SurfaceHolder.Callback, Handler.Callback {
    private static final String TAG = PublicPalette.class.getSimpleName();
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Handler mHandler;
    private Paint mBitmapPaint;
    private Gson mGson = new Gson();

    //公共画板操作集合，使用源地址作为键，每个键值对应多个用户的画板操作
    private ArrayMap<String, SparseArray<List<DrawingOperation>>> mPublicPaletteMap = new ArrayMap<>();
    private Paint mPaintErase;
    //需要传输的绘画信息
    private int mWidth;
    private int mHeight;
    private String mCurrenVideoUrl = "";
    /**
     * 当前信源的所有用户操作
     */
    private SparseArray<List<DrawingOperation>> mUsersOperation = null;

    public PublicPalette(Context context) {
        super(context);
        init();
    }

    public PublicPalette(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mSurfaceHolder = getHolder();
//        setZOrderOnTop(true);
        setZOrderMediaOverlay(true);
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
//        mPaintErase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mPaintErase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
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

    //切换视频源及获取对应的多用户画板操作,然后绘制
    public void changeVideoSource(String url) {
       /* Message message = mHandler.obtainMessage();
        message.what = MSG_CHANGE_VIDEO_SORUCE;
        message.obj = url;
        message.sendToTarget();*/
        if (TextUtils.isEmpty(url)) {
            return;
        }
        mHandler.obtainMessage(MSG_CHANGE_VIDEO_SORUCE, url).sendToTarget();
    }

    /**
     * 根据保存的数据 重新绘制视频源的绘画操作
     * 不同用户需要使用不同层layer 否则擦除会清除其他用户数据
     * 每个用户的数据在自己层绘制后合并到屏幕
     *
     * @param allDrawingOperations 当前信源的所有用户操作
     */
    @WorkerThread
    private void handleAllUsersOperation(SparseArray<List<DrawingOperation>> allDrawingOperations) {
        //获取用户个数
        int userCount = allDrawingOperations.size();
//                遍历每个用户
        for (int index = 0; index < userCount; index++) {
//                    获取这个用户的操作
            List<DrawingOperation> drawingOperations = allDrawingOperations.valueAt(index);
            Log.d(TAG, "handleAllUsersOperation:" + drawingOperations);
            handleSingleUser(drawingOperations);
        }

    }

    /**
     * 处理单个用户的操作
     * 在单独的层中绘制此用户数据
     *
     * @param drawingOperations
     */
    @WorkerThread
    private void handleSingleUser(List<DrawingOperation> drawingOperations) {
        Log.d(TAG, "handleSingleUser: begin");
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();
            int saveCount = mCanvas.saveLayer(0, 0, mWidth, mHeight, null, Canvas.ALL_SAVE_FLAG);
            for (DrawingOperation drawingOperation : drawingOperations) {
                mPaint.setColor(drawingOperation.getDrawColor());
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

            mCanvas.restoreToCount(saveCount);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
        Log.d(TAG, "handleSingleUser: finish");
    }


    /**
     * 更新多用户的画板操作
     * 使用 handler 依次处理 防止并发修改
     *
     * @param
     * @param receive 服务器发来的操作
     */

    public void updateDrawingOperation(String receive) {
        try {
            DrawingOperation drawingOperation = mGson.fromJson(receive, DrawingOperation.class);
            Message message = mHandler.obtainMessage();
            message.what = MSG_UPDATE_OPERATION;
            message.obj = drawingOperation;
            message.sendToTarget();
//            mHandler.obtainMessage(MSG_UPDATE_OPERATION, drawingOperation).sendToTarget();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
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
//        mCanvas.drawColor(Color.WHITE);

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
        switch (msg.what) {
            case MSG_CHANGE_VIDEO_SORUCE:
                handleVideoSourceChange((String) msg.obj);
                break;
            case MSG_UPDATE_OPERATION:
                DrawingOperation drawingOperation = (DrawingOperation) msg.obj;
                handleUpdateOperation(drawingOperation);
                break;
        }
        return true;
    }

    /**
     * 切换视频源
     * 信源上多用户的绘制操作
     * 每个操作包含信源和用户信息
     * 把用户操作分开 是为了处理擦除（需要分层操作） 否则在同一画布绘制不需要分开
     *
     * @param url
     */
    @WorkerThread
    private void handleVideoSourceChange(String url) {
        mCurrenVideoUrl = url;
//        首先清空当前画板
        clearPalette();
        if (mPublicPaletteMap.containsKey(url)) {
            mUsersOperation = mPublicPaletteMap.get(url);
            handleAllUsersOperation(mUsersOperation);
        } else {
            mUsersOperation = new SparseArray<>();
            mPublicPaletteMap.put(url, mUsersOperation);
        }
    }

    @WorkerThread
    private void handleUpdateOperation(DrawingOperation drawingOperation) {
        if (null == drawingOperation) return;
        String url = drawingOperation.getUrl();
        if (TextUtils.isEmpty(url)) return;
        //这个信源的所有用户操作
//        SparseArray<List<DrawingOperation>> allUsersOperations = mPublicPaletteMap.get(url);
        SparseArray<List<DrawingOperation>> allUsersOperations = mUsersOperation;
        /*if (allUsersOperations == null) {
            allUsersOperations = new SparseArray<>();
//            缓存此信源的所有用户数据
            mPublicPaletteMap.put(url, allUsersOperations);
        }*/
//        获取这个用户的操作
        List<DrawingOperation> drawingOperations = allUsersOperations.get(drawingOperation.getUserId());
        if (drawingOperations == null) {
            drawingOperations = new ArrayList<>();
//            缓存此用户的所有操作
            allUsersOperations.put(drawingOperation.getUserId(), drawingOperations);
        }
//        处理单个操作
        if (drawingOperation.getDrawMode() == CLEAR) {
            Log.d(TAG, "handleUpdateOperation: clear");
            //当操作不为空才执行 防止无效重复复制
            if (drawingOperations.size() > 0) {
                drawingOperations.clear();
                if (mCurrenVideoUrl.equals(url)) {
                    clearPalette();
                    // 正在操作当前信源 立刻重绘
//                    handleAllUsersOperation(mPublicPaletteMap.get(url));
                    handleAllUsersOperation(allUsersOperations);
                }
            }
        } else if (drawingOperation.getDrawMode() == ERASE) {
            Log.d(TAG, "handleUpdateOperation: erease");
            //当操作不为空的时候才执行擦除 否则无意义 并且导致重绘
            if (drawingOperations.size() > 0) {
                drawingOperations.add(drawingOperation);
                if (mCurrenVideoUrl.equals(url)) {
                    clearPalette();
                    // 正在操作当前信源 原因立刻重绘
                    handleAllUsersOperation(allUsersOperations);
                }
            }
        } else {
            drawingOperations.add(drawingOperation);
            if (mCurrenVideoUrl.equals(url)) {
                handleSingleOperation(drawingOperation);
            }
        }
    }

    @Deprecated
    private void handleUpdateOperation2(DrawingOperation drawingOperation) {
        if (null == drawingOperation) return;
        String url = drawingOperation.getUrl();
        if (TextUtils.isEmpty(url)) return;
//            获取这个信源所有用户的操作
        SparseArray<List<DrawingOperation>> allUsersOperations = mUsersOperation;
//            根据用户id，获取这个操作所属的集合
        List<DrawingOperation> drawingOperations = allUsersOperations.get(drawingOperation.getUserId());
        if (drawingOperations == null) {
            drawingOperations = new ArrayList<>();
            allUsersOperations.put(drawingOperation.getUserId(), drawingOperations);
        }
        if (mCurrenVideoUrl.equals(drawingOperation.getUrl())) {
            if (drawingOperation.getDrawMode() == CLEAR) {
                Log.d(TAG, "handleUpdateOperation: clear");
                //当操作不为空才执行 防止无效重复复制
                if (drawingOperations.size() > 0) {
//                    清空操作
                    drawingOperations.clear();
                    clearPalette();
                    // 正在操作当前信源 立刻重绘
                    handleAllUsersOperation(allUsersOperations);
                }
            } else if (drawingOperation.getDrawMode() == ERASE) {
                Log.d(TAG, "handleUpdateOperation: erease");
                //当操作不为空的时候才执行擦除 否则无意义 并且导致重绘
                if (drawingOperations.size() > 0) {
                    drawingOperations.add(drawingOperation);
                    clearPalette();
                    handleAllUsersOperation(allUsersOperations);
                }
            } else {
                drawingOperations.add(drawingOperation);
                handleSingleOperation(drawingOperation);
            }
        } else {
            drawingOperations.add(drawingOperation);
        }

    }

    /**
     * 处理除擦除外的更新操作 可直接在画布上操作
     *
     * @param drawingOperation
     */
    @WorkerThread
    private void handleSingleOperation(DrawingOperation drawingOperation) {
        Log.d(TAG, "handleSingleOperation: begin");
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();
            mPaint.setColor(drawingOperation.getDrawColor());
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
                    if (points == null || points.size() < 2) return;
                    int size = points.size();
                    mPath.reset();
                    mPath.moveTo(drawingOperation.getPointDown().x, drawingOperation.getPointDown().y);
                    for (int i = 0; i < size - 1; i++) {
                        mPath.quadTo(points.get(i).x, points.get(i).y, points.get(i + 1).x, points.get(i + 1).y);
                    }
                    mCanvas.drawPath(mPath, mPaint);
                }
                break;
            }
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
        Log.d(TAG, "handleSingleOperation: finish");
    }

    public void testLayer() {
        Log.d(TAG, "testLayer: ");
        Canvas canvas = null;
        try {
            canvas = mSurfaceHolder.lockCanvas();
            mCanvas.drawText("12345", mWidth / 2, mHeight / 2, mPaint);
            int saveLayer = mCanvas.saveLayer(new RectF(0, 0, mWidth, mHeight), null, Canvas.ALL_SAVE_FLAG);
            mCanvas.drawLine(0, 0, mWidth, mHeight, mPaint);
            RectF rectF = new RectF(mWidth / 2 - 50, mHeight / 2 - 50, mWidth / 2 + 50, mHeight / 2 + 50);
            mCanvas.drawRect(rectF, mPaintErase);
            mCanvas.restoreToCount(saveLayer);

            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        } finally {
            if (canvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }


    //清空画板
    @WorkerThread
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


}
