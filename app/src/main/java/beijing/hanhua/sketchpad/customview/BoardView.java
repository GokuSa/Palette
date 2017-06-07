package beijing.hanhua.sketchpad.customview;

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
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

import beijing.hanhua.sketchpad.entity.Data;

/**
 * Created by Administrator on 2016/7/11.
 * 绘画显示视图，
 * 绘制对象传来的图形数据，不具备自己绘画功能
 * 建立Queue存储绘图数据
 */
public class BoardView extends SurfaceView implements SurfaceHolder.Callback, Handler.Callback {
    private static final String TAG = "BoardView";
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    //擦除区域
    private Rect mRect=new Rect();
    private Paint mPaintErase;
    private Handler mHandler;
    //绘制椭圆的参数
    private RectF mRectF=new RectF();

    public BoardView(Context context) {
        super(context);
        init();
    }

    public BoardView(Context context, AttributeSet attrs) {
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
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setTextSize(28);
        //擦除画笔
        mPaintErase = new Paint(Paint.DITHER_FLAG| Paint.ANTI_ALIAS_FLAG);
        mPaintErase.setColor(Color.TRANSPARENT);
        mPaintErase.setStyle(Paint.Style.FILL);
        mPaintErase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        //路径
        mPath = new Path();
        //mBitmap是记录路径的载体，用此画笔绘制
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    public Handler getHandler() {
        return mHandler;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //在测量此画板大小时，创建一个与其同大的Bitmap，它是所有绘制图形的载体
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        //以mbitmap为内容的画布,用来记录以前的绘制图形，
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        HandlerThread handlerThread = new HandlerThread("boardview");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(),this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder.removeCallback(this);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.getLooper().quit();

    }

    @Override
    public boolean handleMessage(Message msg) {
        handleAction((Data) msg.obj);
        return true;
    }

    public void handleAction(Data data) {
        mPath.reset();
        switch (data.getAction()) {
            case "clear":
                clear();
                break;
            case "erase":
                erase(data);
                break;
            case "drawing":
                draw(data);
                break;
            case "drawtext":
                drawText(data);
                break;
            case "drawline":
                drawLine(data);
                break;
            case "drawoval":
                drawOval(data);
                break;
        }
    }

    private void draw(Data data) {
        mPaint.setColor(data.getColor());
        mPaint.setStrokeWidth(data.getWidth());
        List<Integer> mPointX = data.getxList();
        List<Integer> mPointY = data.getyList();
        mPath.moveTo(mPointX.get(0), mPointY.get(0));
        for (int i = 1; i < mPointX.size() - 1; i++) {
            int x1 = mPointX.get(i);
            int y1 = mPointY.get(i);
            int x2 = mPointX.get(i + 1);
            int y2 = mPointY.get(i + 1);
            mPath.quadTo(x1, y1, x2, y2);
        }
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas != null) {
            mCanvas.drawPath(mPath, mPaint);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawText(Data data) {
        mPaint.setColor(data.getColor());
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas != null) {
            mCanvas.drawText(data.getText(), data.getxList().get(0),data.getyList().get(0), mPaint);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
    }
    private void drawLine(Data data) {
        mPaint.setColor(data.getColor());
        mPaint.setStrokeWidth(data.getWidth());
        List<Integer> xPoints = data.getxList();
        List<Integer> yPoints = data.getyList();
        if (xPoints.size() >= 2) {
            Canvas canvas = mSurfaceHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                mCanvas.drawLine(xPoints.get(0), yPoints.get(0),xPoints.get(1), yPoints.get(1), mPaint);
                canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void drawOval(Data data) {
        mPaint.setColor(data.getColor());
        mPaint.setStrokeWidth(data.getWidth());
        List<Integer> xPoints = data.getxList();
        List<Integer> yPoints = data.getyList();
        if (xPoints.size() >= 2) {
            Canvas canvas = mSurfaceHolder.lockCanvas();
            if (canvas != null) {
                //先确定椭圆的坐标
                mRectF.set(xPoints.get(0),yPoints.get(0),xPoints.get(1),yPoints.get(1));
                mCanvas.drawOval(mRectF, mPaint);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void erase(Data data) {
        List<Integer> xPoints = data.getxList();
        List<Integer> yPoints = data.getyList();
        if (xPoints.size() >=2) {
            mRect.set(xPoints.get(0),yPoints.get(0),xPoints.get(1),yPoints.get(1));
            mCanvas.drawRect(mRect, mPaintErase);
            Canvas canvas = mSurfaceHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    public void clear() {
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        mSurfaceHolder.unlockCanvasAndPost(canvas);
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }
}
