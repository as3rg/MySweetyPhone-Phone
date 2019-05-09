package com.mysweetyphone.phone;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Message {
    /**
     *  ,------,----,------,-----,--------------,
     *  | head | id | next | len |     body     |
     *  |  1b  | 4b |  4b  |  4b |    65494b    |
     *  '------'----'------'-----'--------------'
     */
    static int currentId;
    public static final int idSize = 4;
    public int maxSize;
    public int bodySize;
    public static final int sendPort = 2020;
    private byte[] arr;
    private int id, next, len;
    private boolean isHeadValue;
    public static int MAXIMUM = 65507;
    public static int BODYMAXIMUM = 65494;

    static {
        currentId = 0;
    }

    private Message(int length, byte[] body, int size){
        if(size > BODYMAXIMUM) {
            throw new RuntimeException("Размер превышает максимальный");
        }
        if(size != -1) {
            bodySize = size;
            maxSize = bodySize + idSize*3 + 1;
        }else{
            maxSize = 65507;
            bodySize = maxSize-idSize*3;
        }
        if(body.length > bodySize)
            throw new RuntimeException("");
        arr = new byte[maxSize];
        id = currentId;
        this.next = -1;
        len = length;
        isHeadValue = false;

        byte[] byteId = ByteBuffer.allocate(idSize).putInt(currentId).array();
        byte[] byteNext = ByteBuffer.allocate(idSize).putInt(next).array();
        byte[] byteLen = ByteBuffer.allocate(idSize).putInt(length).array();
        arr[0] = 0;
        int i = 1;
        for(; i < idSize+1; i++){
            arr[i] = byteId[i-1];
        }
        for(; i < idSize*2+1; i++){
            arr[i] = byteNext[i-idSize-1];
        }
        for(; i < idSize*3+1; i++){
            arr[i] = byteLen[i-idSize*2-1];
        }
        for(; i < maxSize; i++){
            arr[i] = body[i-3*idSize-1];
        }
        currentId++;
        if(currentId < 0) currentId = 0;
    }

    public Message(byte[] array){
        arr = array;
        isHeadValue = arr[0] != 0 ? true : false;
        maxSize = array.length;
        bodySize = array.length - 3*idSize - 1;
        int i = 1;
        byte[] value = new byte[idSize];
        for(; i < idSize+1; i++){
            value[i-1] = arr[i];
        }
        id = ByteBuffer.wrap(value).getInt();
        value = new byte[idSize];
        for(; i < idSize*2+1; i++){
            value[i-idSize-1] = arr[i];
        }
        next = ByteBuffer.wrap(value).getInt();
        value = new byte[idSize];
        for(; i < idSize*3+1; i++){
            value[i-idSize*2-1] = arr[i];
        }
        len = ByteBuffer.wrap(value).getInt();
    }

    public byte[] getArr() {
        return arr;
    }

    public int getId() {
        return id;
    }

    public int getNext() {
        return next;
    }

    public int getLen() {
        return len;
    }

    public boolean isHead() {
        return isHeadValue;
    }

    public void setHead(boolean a){
        arr[0] = (byte)(a ? 1 : 0);
        isHeadValue = a;
    }

    public byte[] getBody(){
        byte[] b = new byte[bodySize];
        for(int i = idSize*3+1; i < maxSize; i++ )
            b[i-idSize*3-1] = arr[i];
        return b;
    }

    public static Message[] getMessages(byte[] body, int bodySize) {
        ArrayList<Message> result = new ArrayList<>();
        byte[] b = new byte[bodySize];
        for (int i = 0; i < body.length;) {
            int nextZero = i+bodySize;
            int j = i%bodySize;
            for(;i < nextZero && i < body.length;i++,j++) {
                b[j] = body[i];
            }
            result.add(new Message(j, b, bodySize));
        }
        Message[] out = result.toArray(new Message[result.size()]);
        out[0].setHead(true);
        for(int i = 0; i < out.length - 1; i++)
            out[i].next = out[i+1].id;
        return out;
    }

    public static int getMessageSize(int i){
        return i+idSize*3+1;
    }
}