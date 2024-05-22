package com.ext1se.unity_activity;

import static com.ext1se.unity_activity.core.Consts.APP_TAG;

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
import com.ext1se.unity_activity.core.events.ServiceDestroyEvent;
import com.ext1se.unity_activity.core.events.ServiceStartEvent;
import com.ext1se.unity_activity.core.events.ShowDevicesListEvent;
import com.ext1se.unity_activity.core.events.USBDataReceiveEvent;
import com.ext1se.unity_activity.core.services.USBHIDService;
import com.ext1se.unity_activity.unity.UnityPlayerActivity;
import com.unity3d.player.UnityPlayer;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.EventBusException;

public class PluginActivity extends UnityPlayerActivity {

    /// Fields ///

    private static Activity currentUnityActivity;

    private Intent usbService;
    private EventBus eventBus;

    /// Fields ///

    /// Android Events ///

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUnityActivity = UnityPlayer.currentActivity;

        ShowMessage("Called from Native Android");
    }

    /// Android Events ///

    /// private ///

    private void startUsbService() {
        usbService = new Intent(currentUnityActivity, USBHIDService.class);
        currentUnityActivity.startService(usbService);
    }

    private void showListOfDevices(CharSequence nameDevices[]) {
        AlertDialog.Builder builder = new AlertDialog.Builder(currentUnityActivity);

        if (nameDevices.length == 0) {
            builder.setTitle(Consts.MESSAGE_CONNECT_YOUR_USB_HID_DEVICE);
        } else {
            builder.setTitle(Consts.MESSAGE_SELECT_YOUR_USB_HID_DEVICE);
        }

        builder.setItems(nameDevices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (eventBus != null) {
                    ShowMessage("Item is selected: " + which);
                    eventBus.post(new SelectDeviceEvent(which));
                }
            }
        });

        builder.setCancelable(true);
        builder.show();
    }

    /// private ///


    /// public ///

    public void initAndroidServices(String message) {
        Log.d(APP_TAG, "Init Android Services: " + message);

        try {
            eventBus = EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).installDefaultEventBus();
        } catch (EventBusException e) {
            eventBus = EventBus.getDefault();
        }

        eventBus.register(this);
        startUsbService();
    }

    public void SendDataToUnity(USBDataReceiveEvent event) {
        UnityPlayer.UnitySendMessage("UnityActivity", "GetDataFromNative", event.getData());
    }

    public void SaveDataToIntent(USBDataReceiveEvent event) {
        currentUnityActivity.getIntent().putExtra("usb_data", event.getData());
        currentUnityActivity.getIntent().putExtra("usb_raw_data", event.getRawData());
    }

    public void PrepareDeviceList() {
        Log.d(APP_TAG, "PrepareDeviceList");
        if (eventBus != null) {
            eventBus.post(new PrepareDevicesListEvent());
        }
    }

    public void ShowMessage(String msg) {
        Toast.makeText(currentUnityActivity, msg, Toast.LENGTH_LONG).show();
    }

    public void ShowMessageWithTag(String msg) {
        String appName = currentUnityActivity.getResources().getString(R.string.app_name);
        String fullMessage = appName + ": " + msg;
        Toast.makeText(currentUnityActivity, fullMessage, Toast.LENGTH_LONG).show();
    }

    public int Add(int a, int b) {
        return a + b;
    }

    /// public ///

    /// Events from bus ///

    public void onEvent(USBDataReceiveEvent event) {
        Log.d(APP_TAG, "OnEvent: USBHIDTerminal: " + event.getBytesCount());
        SendDataToUnity(event);
        SaveDataToIntent(event);
    }

    public void onEvent(LogMessageEvent event) {
        Log.d(APP_TAG, "OnEvent: LogMessageEvent");
    }

    public void onEvent(ShowDevicesListEvent event) {
        Log.d(APP_TAG, "OnEvent: ShowDevicesListEvent");
        showListOfDevices(event.getCharSequenceArray());
    }

    public void onEvent(DeviceAttachedEvent event) {
        Log.d(APP_TAG, "OnEvent: Device is attached");
        ShowMessage("OnEvent: Device is attached");
    }

    public void onEvent(DeviceDetachedEvent event) {
        Log.d(APP_TAG, "OnEvent: Device is detached");
        ShowMessage("OnEvent: Device is detached");
    }

    public void onEvent(ServiceStartEvent event) {
        Log.d(APP_TAG, "OnEvent: ServiceStartEvent");
        ShowMessage("OnEvent: ServiceStartEvent");
    }

    public void onEvent(ServiceDestroyEvent event) {
        Log.d(APP_TAG, "OnEvent: ServiceDestroyEvent");
        ShowMessage("OnEvent: ServiceDestroyEvent");
    }

    /// Events from bus ///
}
