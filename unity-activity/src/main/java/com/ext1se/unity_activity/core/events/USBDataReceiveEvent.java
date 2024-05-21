package com.ext1se.unity_activity.core.events;

public class USBDataReceiveEvent {
    private final String data;
    private byte[] rawData;
    private final int bytesCount;


    public USBDataReceiveEvent(String data, byte[] rawData, int bytesCount) {
        this.data = data;
        this.rawData = rawData;
        this.bytesCount = bytesCount;
    }

    public USBDataReceiveEvent(String data, int bytesCount) {
        this.data = data;
        this.bytesCount = bytesCount;
    }

    public byte[] getRawData() {
        return rawData;
    }

    public String getData() {
        return data;
    }

    public int getBytesCount() {
        return bytesCount;
    }

}