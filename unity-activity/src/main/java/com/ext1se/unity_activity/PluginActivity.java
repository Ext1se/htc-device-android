package com.ext1se.unity_activity;

import static com.ext1se.unity_activity.core.Consts.APP_TAG;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.ext1se.unity_activity.core.events.USBDataSendEvent;
import com.ext1se.unity_activity.core.services.USBHIDService;
import com.ext1se.unity_activity.unity.UnityPlayerActivity;
import com.unity3d.player.UnityPlayer;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.EventBusException;

import java.util.Base64;

public class PluginActivity extends UnityPlayerActivity {

    /// Fields ///

    //TODO: static fields. Problem with get fields from Unity

    private static Activity currentUnityActivity;

    private Intent usbService;
    private EventBus eventBus;
    private SharedPreferences sharedPreferences;

    private static String receiveDataFormat;
    private static CharSequence[] devices;
    private static long receiveEventCount = 0;


    /// Fields ///

    /// Android Events ///

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUnityActivity = UnityPlayer.currentActivity;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(currentUnityActivity);
        receiveDataFormat = sharedPreferences.getString(Consts.RECEIVE_DATA_FORMAT, Consts.TEXT);

        showMessage("Called from Native Android");
    }

    /// Android Events ///

    /// private ///

    private void startUsbService() {
        usbService = new Intent(currentUnityActivity, USBHIDService.class);
        usbService.setAction(Consts.RECEIVE_DATA_FORMAT);
        usbService.putExtra(Consts.RECEIVE_DATA_FORMAT, receiveDataFormat);
        currentUnityActivity.startService(usbService);
    }

    private void saveListOfDevices(CharSequence nameDevices[]) {
        devices = nameDevices;
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

    public void sendDataToUnity(USBDataReceiveEvent event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            UnityPlayer.UnitySendMessage("UnityActivity", "GetDataFromNative", Base64.getEncoder().encodeToString(event.getRawData())); //event.getRawData());
        }
    }

    public void saveDataToIntent(USBDataReceiveEvent event) {
        currentUnityActivity.getIntent().putExtra("usb_data", event.getData());
        currentUnityActivity.getIntent().putExtra("usb_raw_data", event.getRawData());
        currentUnityActivity.getIntent().putExtra("usb_data_time", System.currentTimeMillis());

        receiveEventCount++;
        currentUnityActivity.getIntent().putExtra("usb_data_events_count", receiveEventCount);
    }

    public void showListOfDevices() {

        if (devices == null) {
            showMessage("Lit of devices = null");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(currentUnityActivity);

        if (devices.length == 0) {
            builder.setTitle(Consts.MESSAGE_CONNECT_YOUR_USB_HID_DEVICE);
        } else {
            builder.setTitle(Consts.MESSAGE_SELECT_YOUR_USB_HID_DEVICE);
        }

        builder.setItems(devices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (eventBus != null) {
                    showMessage("Item is selected: " + which);
                    eventBus.post(new SelectDeviceEvent(which));
                }
            }
        });

        builder.setCancelable(true);
        builder.show();
    }

    public void setReceiveFormat(int typeFormat) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        switch (typeFormat) {
            case (0):
                editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.BINARY).apply();
                break;
            case (1):
                editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.INTEGER).apply();
                break;
            case (2):
                editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.HEXADECIMAL).apply();
                break;
            case (3):
                editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.TEXT).apply();
                break;
            default:
                editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.BINARY).apply();
                break;

        }

        editor.apply();

        receiveDataFormat = sharedPreferences.getString(Consts.RECEIVE_DATA_FORMAT, Consts.TEXT);

        usbService.putExtra(Consts.RECEIVE_DATA_FORMAT, receiveDataFormat);
        usbService.setAction(Consts.RECEIVE_DATA_FORMAT);
    }

    public void prepareDeviceList() {
        Log.d(APP_TAG, "PrepareDeviceList");
        if (eventBus != null) {
            eventBus.post(new PrepareDevicesListEvent());
        }
    }

    //TODO: refactor select by PID and VID
    public void selectHtcDevice() {
        if (devices == null) {
            showMessage("Lit of devices = null");
            return;
        }

        for (int i = 0; i < devices.length; i++) {
            CharSequence deviceName = devices[i];
            if (deviceName.toString().contains("VID:0xbb4")
                    && deviceName.toString().contains("PID:0x350")) {
                showMessage("Item is selected: " + i + "; " + deviceName);
                eventBus.post(new SelectDeviceEvent(i));
            }
        }
    }

/*
    public void sendStringDataToDevice(String data){
        eventBus.post(new USBDataSendEvent(data));
    }
*/

    public void sendDataToDevice(byte[] data) {
        eventBus.post(new USBDataSendEvent(data));
    }

    public void showMessage(String msg) {
        Toast.makeText(currentUnityActivity, msg, Toast.LENGTH_SHORT).show();
    }

    public void showMessageWithTag(String msg) {
        String appName = currentUnityActivity.getResources().getString(R.string.app_name);
        String fullMessage = appName + ": " + msg;
        Toast.makeText(currentUnityActivity, fullMessage, Toast.LENGTH_SHORT).show();
    }

    public int add(int a, int b) {
        return a + b;
    }

    /// public ///

    /// Events from bus ///

    public void onEvent(USBDataReceiveEvent event) {
        Log.d(APP_TAG, "OnEvent: USBHIDTerminal: " + event.getBytesCount());
        sendDataToUnity(event);
        saveDataToIntent(event);
    }

    public void onEvent(USBDataSendEvent event) {
        Log.d(APP_TAG, "OnEvent: USBDataSendEvent");
    }

    public void onEvent(LogMessageEvent event) {
        Log.d(APP_TAG, "OnEvent: LogMessageEvent");
    }

    public void onEvent(ShowDevicesListEvent event) {
        Log.d(APP_TAG, "OnEvent: ShowDevicesListEvent");
        saveListOfDevices(event.getCharSequenceArray());
    }

    public void onEvent(DeviceAttachedEvent event) {
        Log.d(APP_TAG, "OnEvent: Device is attached");
        showMessage("OnEvent: Device is attached");
    }

    public void onEvent(DeviceDetachedEvent event) {
        Log.d(APP_TAG, "OnEvent: Device is detached");
        showMessage("OnEvent: Device is detached");
    }

    public void onEvent(ServiceStartEvent event) {
        Log.d(APP_TAG, "OnEvent: ServiceStartEvent");
        showMessage("OnEvent: ServiceStartEvent");
    }

    public void onEvent(ServiceDestroyEvent event) {
        Log.d(APP_TAG, "OnEvent: ServiceDestroyEvent");
        showMessage("OnEvent: ServiceDestroyEvent");
    }

    /// Events from bus ///
}
