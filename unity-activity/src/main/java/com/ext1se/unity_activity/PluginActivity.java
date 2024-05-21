package com.ext1se.unity_activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.ext1se.unity_activity.core.Consts;
import com.ext1se.unity_activity.core.events.DeviceAttachedEvent;
import com.ext1se.unity_activity.core.events.DeviceDetachedEvent;
import com.ext1se.unity_activity.core.events.LogMessageEvent;
import com.ext1se.unity_activity.core.events.PrepareDevicesListEvent;
import com.ext1se.unity_activity.core.events.SelectDeviceEvent;
import com.ext1se.unity_activity.core.events.ShowDevicesListEvent;
import com.ext1se.unity_activity.core.events.USBDataReceiveEvent;
import com.ext1se.unity_activity.core.services.USBHIDService;
import com.unity3d.player.UnityPlayer;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.EventBusException;

public class PluginActivity extends UnityPlayerActivity {

    public static String TAG = "UnityServices";

    private static Activity currentActivity1;
    private Intent usbService;
    private EventBus eventBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentActivity1 = UnityPlayer.currentActivity;
        
        Toast.makeText(currentActivity1, "Called from PluginActivity", Toast.LENGTH_LONG).show();
    }

    public void initAndroidServices(String message) {
        Log.d(TAG, "Init Services: " + message);

        try {
            Log.d(TAG, "Init Services: 1");
            eventBus = EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).installDefaultEventBus();
        } catch (EventBusException e) {
            Log.d(TAG, "Init Services: 2");
            eventBus = EventBus.getDefault();
        }

        if (eventBus != null) {
            Log.d(TAG, "Event Bus is not NULL");
            eventBus.register(this);
        } else {
            Log.d(TAG, "Event Bus is NULL");
        }

        startUsbService();
    }

    private void startUsbService() {
        Log.d(TAG, "startUsbService 1");
        usbService = new Intent(currentActivity1, USBHIDService.class);
        Log.d(TAG, "startUsbService 2");
        currentActivity1.startService(usbService);
        Log.d(TAG, "startUsbService 3");
    }

    private void showListOfDevices(CharSequence nameDevices[]) {
        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity1);

        if (nameDevices.length == 0) {
            builder.setTitle(Consts.MESSAGE_CONNECT_YOUR_USB_HID_DEVICE);
        } else {
            builder.setTitle(Consts.MESSAGE_SELECT_YOUR_USB_HID_DEVICE);
        }

        builder.setItems(nameDevices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (eventBus != null) {
                    ShowMessage("Item is selected");
                    eventBus.post(new SelectDeviceEvent(which));
                }
            }
        });

        builder.setCancelable(true);
        builder.show();
    }

    public void SendDataToUnity(USBDataReceiveEvent event){
        UnityPlayer.UnitySendMessage("UnityActivity", "GetDataFromNative", event.getData());
    }

    private void SaveDataToIntent(USBDataReceiveEvent event) {
        currentActivity1.getIntent().putExtra("usb_data", event.getData());
        currentActivity1.getIntent().putExtra("usb_raw_data", event.getRawData());
    }

    public void PrepareDeviceList() {
        Log.d(TAG, "PrepareDeviceList 1");
        if (eventBus != null) {
            Log.d(TAG, "PrepareDeviceList 2");
            eventBus.post(new PrepareDevicesListEvent());
        }
    }

    public void ShowMessage(String msg) {
        Toast.makeText(currentActivity1, msg, Toast.LENGTH_LONG).show();
    }

    public void ShowMessageWithTag(String msg) {
        String appName = currentActivity1.getResources().getString(R.string.app_name);
        String fullMessage = appName + ": " + msg;
        Toast.makeText(currentActivity1, fullMessage, Toast.LENGTH_LONG).show();
    }

    public int Add(int a, int b) {
        return a + b;
    }

    ///

    public void onEvent(USBDataReceiveEvent event) {
        ShowMessage("Received data");

        Log.d(TAG, "Received USBHIDTerminal: " + event.getBytesCount());
        SendDataToUnity(event);
        SaveDataToIntent(event);
    }

    public void onEvent(LogMessageEvent event) {
    }

    public void onEvent(ShowDevicesListEvent event) {
        Log.d(TAG, "Received ShowDevicesListEvent");
        showListOfDevices(event.getCharSequenceArray());
    }

    public void onEvent(DeviceAttachedEvent event) {
        ShowMessage("Device is attached");
    }

    public void onEvent(DeviceDetachedEvent event) {
        ShowMessage("Device is detached");
    }
}
