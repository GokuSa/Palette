package beijing.hanhua.sketchpad.entity;

import android.graphics.Point;

import java.util.List;

/**
 * Created by Administrator on 2016/7/11.
 * 一次绘制的数据，从action down 开始到 action up结束
 * 记录所在绘制操作action：划线 画圆 自由绘制等
 * 记录绘制路径 颜色 文字
 *
 */
public class DrawingOperation {
    //所在群组，群内成员可收到其他成员的画板信息，开启画板就加入群组
    private int groupId;

    /**
     * 绘制模式
     * DRAW_LINE = 5 DRAW_OVAL = 6  DRAW_TEXT = 7
     */
    private int mDrawMode;
    //操作识别代码
    private String action;

    //画板颜色
    private int mDrawColor;
    private int mStrokeWidth;
    //画板宽度
    private int width;
    //绘制文字
    private String mText;
    //手指落点
    private Point mPointDown;
//    手指实时位置点
    private Point mPointCurrent;
    private List<Point> mPoints;
    public DrawingOperation() {

    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int roomId) {
        this.groupId = roomId;
    }


    public int getDrawMode() {
        return mDrawMode;
    }

    public void setDrawMode(int drawMode) {
        this.mDrawMode = drawMode;
    }

    public int getStrokeWidth() {
        return mStrokeWidth;
    }

    public void setStrokeWidth(int strokeWidth) {
        mStrokeWidth = strokeWidth;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getDrawColor() {
        return mDrawColor;
    }

    public void setDrawColor(int drawColor) {
        this.mDrawColor = drawColor;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
    }

    public Point getPointCurrent() {
        return mPointCurrent;
    }

    public void setPointCurrent(Point pointCurrent) {
        mPointCurrent = pointCurrent;
    }

    public List<Point> getPoints() {
        return mPoints;
    }

    public void setPoints(List<Point> points) {
        mPoints = points;
    }

    public Point getPointDown() {
        return mPointDown;
    }

    public void setPointDown(Point pointDown) {
        mPointDown = pointDown;
    }

    @Override
    public String toString() {
        return "DrawingOperation{" +
                "groupId=" + groupId +
                ", action='" + action + '\'' +
                ", mDrawColor=" + mDrawColor +
                ", width=" + width +
                ", mText='" + mText + '\'' +
                ", mPoints=" + mPoints +
                '}';
    }
}
