package com.ext1se.unity_activity.core.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.ext1se.unity_activity.core.Consts;
import com.ext1se.unity_activity.core.USBUtils;
import com.ext1se.unity_activity.core.events.PrepareDevicesListEvent;
import com.ext1se.unity_activity.core.events.SelectDeviceEvent;
import com.ext1se.unity_activity.core.events.USBDataSendEvent;
import com.unity3d.player.UnityPlayer;

import java.util.LinkedList;
import java.util.List;

import de.greenrobot.event.EventBus;

public abstract class AbstractUSBHIDService extends Service {

    protected static final String TAG = AbstractUSBHIDService.class.getCanonicalName();

    public static final int REQUEST_GET_REPORT = 0x01;
    public static final int REQUEST_SET_REPORT = 0x09;
    public static final int REPORT_TYPE_INPUT = 0x0100;
    public static final int REPORT_TYPE_OUTPUT = 0x0200;
    public static final int REPORT_TYPE_FEATURE = 0x0300;

    private USBThreadDataReceiver usbThreadDataReceiver;

    private final Handler uiHandler = new Handler();

    private List<UsbInterface> interfacesList = null;

    private UsbManager mUsbManager;
    private UsbDeviceConnection connection;
    private UsbDevice device;

    private IntentFilter filter;
    private PendingIntent mPermissionIntent;

    //private boolean sendedDataType;
    private boolean sendedDataType = true;

    protected EventBus eventBus = EventBus.getDefault();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate: start");

        int pendingFlags = PendingIntent.FLAG_MUTABLE;
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            //pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            pendingFlags = PendingIntent.FLAG_MUTABLE;
        } else {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        mPermissionIntent = PendingIntent.getBroadcast(this,
                0,
                new Intent(Consts.ACTION_USB_PERMISSION),
                pendingFlags);

        filter = new IntentFilter(Consts.ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(Consts.ACTION_USB_SHOW_DEVICES_LIST);
        filter.addAction(Consts.ACTION_USB_DATA_TYPE);

        registerReceiver(mUsbReceiver, filter);
        eventBus.register(this);

        Log.d(TAG, "Service onCreate: complete");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + startId);

        String action = intent.getAction();
        if (Consts.ACTION_USB_DATA_TYPE.equals(action)) {
            //sendedDataType = intent.getBooleanExtra(Consts.ACTION_USB_DATA_TYPE, false);

            //TODO
            sendedDataType = intent.getBooleanExtra(Consts.ACTION_USB_DATA_TYPE, true);
        }
        onCommand(intent, action, flags, startId);

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");

        eventBus.unregister(this);
        super.onDestroy();
        if (usbThreadDataReceiver != null) {
            usbThreadDataReceiver.stopThis();
        }
        unregisterReceiver(mUsbReceiver);
    }

    private class USBThreadDataReceiver extends Thread {

        private volatile boolean isStopped;

        public USBThreadDataReceiver() {
        }

        @Override
        public void run() {
            try {
                if (connection != null) {
                    while (!isStopped) {
                        for (UsbInterface intf : interfacesList) {
                            for (int i = 0; i < intf.getEndpointCount(); i++) {
                                UsbEndpoint endPointRead = intf.getEndpoint(i);
                                if (UsbConstants.USB_DIR_IN == endPointRead.getDirection()) {
                                    final byte[] buffer = new byte[endPointRead.getMaxPacketSize()];
                                    final int status = connection.bulkTransfer(endPointRead, buffer, buffer.length, 100);
                                    if (status > 0) {
                                        uiHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                onUSBDataReceive(buffer);
                                            }
                                        });
                                    } else {
                                        int transfer = connection.controlTransfer(0xA0, REQUEST_GET_REPORT, REPORT_TYPE_OUTPUT, 0x00, buffer, buffer.length, 100);
                                        if (transfer > 0) {
                                            uiHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    onUSBDataReceive(buffer);
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in receive thread", e);
            }
        }

        public void stopThis() {
            isStopped = true;
        }
    }

    public void onEventMainThread(USBDataSendEvent event) {
        sendData(event.getByteData());
    }

    public void onEvent(SelectDeviceEvent event) {
        Log.d(TAG, "SelectDeviceEvent 1");

        device = (UsbDevice) mUsbManager.getDeviceList().values().toArray()[event.getDevice()];
        mUsbManager.requestPermission(device, mPermissionIntent);

        Log.d(TAG, "SelectDeviceEvent 2");
    }

    public void onEventMainThread(PrepareDevicesListEvent event) {
        Log.d(TAG, "PrepareDeviceList 3");

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<CharSequence> list = new LinkedList<CharSequence>();
        for (UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
            list.add(onBuildingDevicesList(usbDevice));
        }
        final CharSequence devicesName[] = new CharSequence[mUsbManager.getDeviceList().size()];
        list.toArray(devicesName);

        Log.d(TAG, "PrepareDeviceList 3 size = " + devicesName.length);

        onShowDevicesList(devicesName);
    }

    private void sendData(byte[] data)
    {
        Log.d(TAG, "sendData " + data);

        if (device != null && mUsbManager.hasPermission(device))
        {
            Log.d(TAG, "sendData; device is not null!");

            // mLog(connection +"\n"+ device +"\n"+ request +"\n"+
            // packetSize);
            for (UsbInterface usbInterface : interfacesList)
            {
                for (int i = 0; i < usbInterface.getEndpointCount(); i++)
                {
                    UsbEndpoint endPointWrite = usbInterface.getEndpoint(i);
                    if (UsbConstants.USB_DIR_OUT == endPointWrite.getDirection())
                    {
                        // Charset.forName("UTF-16")
                        onUSBDataSending(data);
                        int status = connection.bulkTransfer(endPointWrite, data, data.length, 250);
                        onUSBDataSended(status, data);
                        status = connection.controlTransfer(0x21, REQUEST_SET_REPORT, REPORT_TYPE_OUTPUT, 0x02, data, data.length, 250);
                        onUSBDataSended(status, data);
                    }
                }
            }
        }
    }

    /**
     * receives the permission request to connect usb devices
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "BroadcastReceiver");

            String action = intent.getAction();
            if (Consts.ACTION_USB_PERMISSION.equals(action)) {
                Log.d(TAG, "BroadcastReceiver setDevice 1");

                setDevice(intent);
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(TAG, "BroadcastReceiver attach 2");

                setDevice(intent);
                if (device != null) {
                    Log.d(TAG, "BroadcastReceiver attach 3");

                    onDeviceConnected(device);

                    Log.d(TAG, "BroadcastReceiver attach 4");

                }
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                Log.d(TAG, "BroadcastReceiver detach 4");


                if (device != null) {
                    Log.d(TAG, "BroadcastReceiver detach 5");

                    device = null;
                    if (usbThreadDataReceiver != null) {
                        usbThreadDataReceiver.stopThis();
                    }
                    onDeviceDisconnected(device);
                }

                Log.d(TAG, "BroadcastReceiver detach 6");

            }
        }

        private void setDevice(Intent intent) {
            Log.d(TAG, "BroadcastReceiver setDevice 10");
            device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, true)) {
                Log.d(TAG, "BroadcastReceiver setDevice 11");


                onDeviceSelected(device);
                connection = mUsbManager.openDevice(device);
                if (connection == null) {
                    Log.d(TAG, "BroadcastReceiver setDevice 12 null");

                    return;
                }
                interfacesList = new LinkedList();
                for (int i = 0; i < device.getInterfaceCount(); i++) {
                    UsbInterface intf = device.getInterface(i);
                    connection.claimInterface(intf, true);
                    interfacesList.add(intf);
                }
                usbThreadDataReceiver = new USBThreadDataReceiver();
                usbThreadDataReceiver.start();
                onDeviceAttached(device);
                Log.d(TAG, "BroadcastReceiver setDevice 14");

            } else {
                Log.d(TAG, "BroadcastReceiver setDevice NULL " + device);
            }

            Log.d(TAG, "BroadcastReceiver setDevice 20");
        }
    };

    public void onCommand(Intent intent, String action, int flags, int startId) {
    }

    public void onUSBDataReceive(byte[] buffer) {
    }

    public void onDeviceConnected(UsbDevice device) {
    }

    public void onDeviceDisconnected(UsbDevice device) {
    }

    public void onDeviceSelected(UsbDevice device) {
    }

    public void onDeviceAttached(UsbDevice device) {
    }

    public void onShowDevicesList(CharSequence[] deviceName) {
    }

    public CharSequence onBuildingDevicesList(UsbDevice usbDevice) {
        return null;
    }

    public void onUSBDataSending(byte[] data) {
    }

    public void onUSBDataSended(int status, byte[] out) {
    }

    public void onSendingError(Exception e) {
    }

}