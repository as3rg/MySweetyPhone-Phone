package com.mysweetyphone.phone;

import java.util.Date;

public class Message {
    private String textMessage;
    private String device;
    private long timeMessage;

    public Message(String textMessage, String device) {
        this.textMessage = textMessage;
        this.device = device;

        timeMessage = new Date().getTime();
    }

    public Message() {
    }

    public String getTextMessage() {
        return textMessage;
    }

    public void setTextMessage(String textMessage) {
        this.textMessage = textMessage;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public long getTimeMessage() {
        return timeMessage;
    }

    public void setTimeMessage(long timeMessage) {
        this.timeMessage = timeMessage;
    }
}
