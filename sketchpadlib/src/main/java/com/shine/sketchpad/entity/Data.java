package com.shine.sketchpad.entity;

import java.util.List;

/**
 * Created by Administrator on 2016/7/11.
 * 数据传输结构
 */
public class Data {
    //所在群组，群内成员可收到其他成员的画板信息，开启画板就加入群组
    private int groupId;
    //具体终端
    private String mac;
    //操作识别代码
    private String action;
    //画板颜色
    private int color;
    //画板宽度
    private int width;
    //绘制文字
    private String text;
    //路径x,y点集合
    private List<Integer> xList;
    private List<Integer> yList;

    public Data() {
//        mac= SharePreferenceUtil.getInstance().getMac(MyApp.getContext());
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int roomId) {
        this.groupId = roomId;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Integer> getxList() {
        return xList;
    }

    public void setxList(List<Integer> xList) {
        this.xList = xList;
    }

    public List<Integer> getyList() {
        return yList;
    }

    public void setyList(List<Integer> yList) {
        this.yList = yList;
    }



    @Override
    public String toString() {
        return "Data{" +
                "groupId=" + groupId +
                ", mac='" + mac + '\'' +
                ", action='" + action + '\'' +
                ", color=" + color +
                ", width=" + width +
                ", text='" + text + '\'' +
                ", xList=" + xList +
                ", yList=" + yList +
                '}';
    }
}
