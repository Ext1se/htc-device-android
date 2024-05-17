package com.ext1se.service_utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.ext1se.service_utils.core.Consts;
import com.ext1se.service_utils.core.events.DeviceAttachedEvent;
import com.ext1se.service_utils.core.events.DeviceDetachedEvent;
import com.ext1se.service_utils.core.events.LogMessageEvent;
import com.ext1se.service_utils.core.events.PrepareDevicesListEvent;
import com.ext1se.service_utils.core.events.SelectDeviceEvent;
import com.ext1se.service_utils.core.events.ShowDevicesListEvent;
import com.ext1se.service_utils.core.events.USBDataReceiveEvent;
import com.ext1se.service_utils.core.services.USBHIDService;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.EventBusException;

public class PluginInstance {
    public static String TAG = "UnityServices";

    private static Activity unityActivity;

    private Intent usbService;
    //private EventBus eventBus;

    public PluginInstance() {
    }

    private static void receiveUnityActivity(Activity activity) {
        unityActivity = activity;
    }

    public void initAndroidServices(String message) {
        Log.d(TAG, "Init Services: " + message);

        try {
            Log.d(TAG, "Init Services: 1");

            //eventBus = EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).installDefaultEventBus();
        } catch (EventBusException e) {
            Log.d(TAG, "Init Services: 2");

            //eventBus = EventBus.getDefault();
        }

        /*
        if (eventBus != null) {
            Log.d(TAG, "Event Bus is not NULL");
            //eventBus.register(this);
        } else {
            Log.d(TAG, "Event Bus is NULL");
        }
         */

        startUsbService();
    }

    private void startUsbService() {
        Log.d(TAG, "startUsbService 1");
        usbService = new Intent(unityActivity, USBHIDService.class);
        Log.d(TAG, "startUsbService 2");
        unityActivity.startService(usbService);
        Log.d(TAG, "startUsbService 3");
    }

    private void sendToUSBService(String action) {
        usbService.setAction(action);
        unityActivity.startService(usbService);
    }

    private void sendToUSBService(String action, boolean data) {
        usbService.putExtra(action, data);
        sendToUSBService(action);
    }

    public void PrepareDeviceList() {
        //if (eventBus != null) {
         //   eventBus.post(new PrepareDevicesListEvent());
        //}
    }

    void showListOfDevices(CharSequence nameDevices[]) {
        AlertDialog.Builder builder = new AlertDialog.Builder(unityActivity);

        if (nameDevices.length == 0) {
            builder.setTitle(Consts.MESSAGE_CONNECT_YOUR_USB_HID_DEVICE);
        } else {
            builder.setTitle(Consts.MESSAGE_SELECT_YOUR_USB_HID_DEVICE);
        }

        builder.setItems(nameDevices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              //  if (eventBus != null) {
              //      eventBus.post(new SelectDeviceEvent(which));
              //  }
            }
        });

        builder.setCancelable(true);
        builder.show();
    }

    public int Add(int a, int b) {
        return a + b;
    }

    public void Toast(String msg) {
        Toast.makeText(unityActivity, msg, Toast.LENGTH_LONG).show();
    }

    ///

    public void onEvent(USBDataReceiveEvent event) {
        Log.d(TAG, "Received USBHIDTerminal: " + event.getBytesCount());
    }

    public void onEvent(LogMessageEvent event) {
    }

    public void onEvent(ShowDevicesListEvent event) {
        Log.d(TAG, "Received ShowDevicesListEvent");
        showListOfDevices(event.getCharSequenceArray());
    }

    public void onEvent(DeviceAttachedEvent event) {
    }

    public void onEvent(DeviceDetachedEvent event) {
    }
}
