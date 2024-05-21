package com.ext1se.unity_activity.core.events;

public class LogMessageEvent {

    private final String data;

    public LogMessageEvent(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}