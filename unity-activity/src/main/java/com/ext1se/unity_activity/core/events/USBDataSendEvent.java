package com.ext1se.unity_activity.core.events;

public class USBDataSendEvent {
    //private final String data;
    private final byte[] bdata;

    //public USBDataSendEvent(String data) {
    //    this.data = data;
    //}
    public USBDataSendEvent(byte[] data) {
        this.bdata = data;
    }

    //public String getData() { return data; }
    public byte[] getByteData() {
        return bdata;
    }

}