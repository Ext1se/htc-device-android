package com.ext1se.service_utils;

import android.app.Activity;
import android.widget.Toast;

public class PluginInstance {

    private static Activity unityActivity;

    private static void receiveUnityActivity(Activity activity){
        unityActivity = activity;
    }

    public void Toast(String msg){
        Toast.makeText(unityActivity, msg, Toast.LENGTH_LONG).show();
    }
}
