package com.ext1se.service_utils.core.events;

public class SelectDeviceEvent {
    private int device;

    public SelectDeviceEvent(int device) {
        this.device = device;
    }

    public int getDevice() {
        return device;
    }
}