package com.red.redbluetooth.bean;

/**
 * Created by Red on 2017/5/8.
 */

public class DataSource {
    private int ID;   // 每条消息的唯一id
    private int GET_OR_SEND;  // 1:send 0:get
    private String content;
    private int length;
    private int group;


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getGET_OR_SEND() {
        return GET_OR_SEND;
    }

    public void setGET_OR_SEND(int GET_OR_SEND) {
        this.GET_OR_SEND = GET_OR_SEND;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
