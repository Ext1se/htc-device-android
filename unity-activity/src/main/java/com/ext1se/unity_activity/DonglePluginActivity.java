package com.ext1se.unity_activity;

import static android.content.Context.RECEIVER_EXPORTED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import com.ext1se.unity_activity.core.Consts;

import java.io.IOException;
import java.util.Map;

public class DonglePluginActivity {
    public static final int VID_VIVE = 0x0bb4;
    public static final int PID_DONGLE = 0x0350;
    Activity unityActivity;
    UsbDevice device = null;
    UsbManager manager = null;
    UsbDeviceConnection connection = null;
    PendingIntent permissionIntent;
    public static int TIMEOUT = 1000000;
    private boolean forceClaim = true;

    private USBThread usbThread;

    private static final String ACTION_USB_PERMISSION = "android.hardware.usb.host";
    //"com.google.android.HID.action.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice _device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (_device != null) {
                            // call method to set up device communication
                            connection = manager.openDevice(_device);
                            if (connection != null) {
                                device = _device;
                                for (int i = 0; i < device.getInterfaceCount(); i++) {
                                    UsbInterface intf = device.getInterface(i);
                                    Log.d("TAG", "ACTION_USB_DEVICE_ATTACHED Found interface: " + intf.getName());
                                    connection.claimInterface(intf, forceClaim);
                                }
                                Log.d("TAG", "ACTION_USB_PERMISSION Device opened success!");
                            } else Log.e("TAG", "ACTION_USB_PERMISSION Can't open device");
                        } else
                            Log.e("TAG", "ACTION_USB_PERMISSION VIVE Dongle device is not exist!");
                    } else {
                        Log.d("TAG", "ACTION_USB_PERMISSION permission denied for device " + _device);
                    }
                }
            } else if (ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice _device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (_device != null) {
                            // call method to set up device communication
                            int VID = _device.getVendorId();
                            int PID = _device.getProductId();
                            if (VID == VID_VIVE && PID == PID_DONGLE) {
                                connection = manager.openDevice(_device);
                                if (connection != null) {
                                    device = _device;
                                    for (int i = 0; i < device.getInterfaceCount(); i++) {
                                        UsbInterface intf = device.getInterface(i);
                                        Log.d("TAG", "ACTION_USB_DEVICE_ATTACHED Found interface: " + intf.getName());
                                        connection.claimInterface(intf, forceClaim);
                                    }

                                    //TODO start usbThread + check thread
                                    StartUsbThread();

                                    Log.d("TAG", "ACTION_USB_DEVICE_ATTACHED Device connection is open");
                                } else Log.e("TAG", "ACTION_USB_DEVICE_ATTACHED Can't open device");
                            }
                        } else
                            Log.e("TAG", "ACTION_USB_DEVICE_ATTACHED VIVE Dongle device is not exist!");
                    } else {
                        Log.d("TAG", "ACTION_USB_DEVICE_ATTACHED permission denied for device " + _device);
                        if (_device != null) {
                            int VID = _device.getVendorId();
                            int PID = _device.getProductId();
                            if (VID == VID_VIVE && PID == PID_DONGLE) {
                                if (!manager.hasPermission(_device)) {
                                    Log.d("TAG", "ACTION_USB_DEVICE_ATTACHED requestPermission permission");
                                    manager.requestPermission(_device, permissionIntent);
                                    if (connection != null)
                                        connection.close();
                                    connection = manager.openDevice(_device);
                                    if (connection != null) {
                                        device = _device;
                                        for (int i = 0; i < device.getInterfaceCount(); i++) {
                                            UsbInterface intf = device.getInterface(i);
                                            Log.d("TAG", "ACTION_USB_DEVICE_ATTACHED Found interface: " + intf);
                                            connection.claimInterface(intf, forceClaim);

                                        }

                                        //TODO start usbThread
                                        StartUsbThread();

                                        Log.d("TAG", "ACTION_USB_DEVICE_ATTACHED Device connection is open");
                                    } else
                                        Log.e("TAG", "ACTION_USB_DEVICE_ATTACHED Can't open device");
                                } else {
                                    Log.d("TAG", "ACTION_USB_DEVICE_ATTACHED device has permission");
                                    connection = manager.openDevice(_device);
                                    if (connection != null) {
                                        device = _device;
                                        for (int i = 0; i < device.getInterfaceCount(); i++) {
                                            UsbInterface intf = device.getInterface(i);
                                            Log.d("TAG", "ACTION_USB_DEVICE_ATTACHED Found interface: " + intf);
                                            connection.claimInterface(intf, forceClaim);
                                        }

                                        //TODO start usbThread
                                        StartUsbThread();


                                        Log.d("TAG", "ACTION_USB_DEVICE_ATTACHED Device connection is open");
                                    } else
                                        Log.e("TAG", "ACTION_USB_DEVICE_ATTACHED Can't open device");
                                }
                            } else
                                Log.e("TAG", "ACTION_USB_DEVICE_ATTACHED VID and PID device is not equal VIVE");
                        } else Log.e("TAG", "ACTION_USB_DEVICE_ATTACHED device is NULL");
                    }
                }
            } else if (ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice _device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (_device != null) {
                            int VID = _device.getVendorId();
                            int PID = _device.getProductId();
                            if (VID == VID_VIVE && PID == PID_DONGLE) {

                                StopThread();

                                if (connection != null) {
                                    connection.close();
                                    connection = null;

                                    Log.d("TAG", "ACTION_USB_DEVICE_DETACHED Device connection is closed");
                                }
                            }
                        } else
                            Log.e("TAG", "ACTION_USB_DEVICE_DETACHED VIVE Dongle device is not exist!");
                    } else {
                        Log.d("TAG", "ACTION_USB_DEVICE_DETACHED permission denied for device " + _device);
                        if (_device != null) {
                            int VID = _device.getVendorId();
                            int PID = _device.getProductId();
                            if (VID == VID_VIVE && PID == PID_DONGLE) {

                                StopThread();

                                if (connection != null) {
                                    connection.close();
                                    connection = null;
                                    device = null;
                                    Log.d("TAG", "ACTION_USB_DEVICE_DETACHED Device connection is closed");
                                }
                            }
                        }
                    }
                }
            }
        }
    };

    //TODO: how stop thread
    private void StopThread() {
        usbThread.stopThis();
        //usbThread.stop();
    }

    private void StartUsbThread() {
        usbThread.setDevice(device);
        usbThread.setConnection(connection);

        if (!usbThread.isAlive()) {
            Log.d(Consts.APP_TAG, "Thread is not alive");
            usbThread.start();
        }
        {
            Log.d(Consts.APP_TAG, "Thread is alive");
        }
    }

    public void Init(Activity activity) {
        unityActivity = activity;
        // TODO
        Log.d("TAG", "INIT");

        usbThread = new USBThread();

        //usbThread.setConnection();
        //usbThread.setInterfacesList();
        //usbThread.start();

        //byte[] data = usbThread.getData();

        ///
        ContextWrapper cw = new ContextWrapper(unityActivity);//

        permissionIntent = PendingIntent.getBroadcast(cw, 0,
                new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            unityActivity.registerReceiver(usbReceiver, filter, RECEIVER_EXPORTED);
        else unityActivity.registerReceiver(usbReceiver, filter);

        manager = (UsbManager) cw.getSystemService(Context.USB_SERVICE);
        /*if (manager != null)
        {
            java.util.HashMap<String, UsbDevice> devices = manager.getDeviceList();
            if (devices != null)
            {
                for (Map.Entry<String, UsbDevice> item:devices.entrySet())
                {
                    UsbDevice val = item.getValue();
                    int VID = val.getVendorId();
                    int PID = val.getProductId();
                    Log.d("TAG", String.format("%s VID:0x%s PID:0x%s", item.getKey(), Integer.toHexString(VID), Integer.toHexString(PID)));
                    if(VID == VID_VIVE && PID == PID_DONGLE)
                    {
                        device = val;
                    }
                }
            }
            else Log.e("TAG", "Device list is empty");
        }*/
    }

    public void Close() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        unityActivity.unregisterReceiver(usbReceiver);
    }

    public byte[] Read() {
        if (usbThread != null && connection != null && device != null) {
            return usbThread.getData();
        } else {
            Log.w("TAG", "connection is null!!!");
        }

        return new byte[0];
    }

    public byte[] Read_Deprecated() {
        if (connection != null) {

            //TODO
            byte[] data = usbThread.getData();


            //UsbInterface intf = device.getInterface(0);
            //UsbEndpoint endpoint = intf.getEndpoint(0);
            //connection.claimInterface(intf, forceClaim);
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface intf = device.getInterface(i);
                //connection.claimInterface(intf, forceClaim);
                for (int i1 = 0; i1 < intf.getEndpointCount(); i1++) {
                    UsbEndpoint endPointRead = intf.getEndpoint(i1);
                    if (UsbConstants.USB_DIR_IN == endPointRead.getDirection()) {
                        byte[] bytes = new byte[endPointRead.getMaxPacketSize()];
                        int count = connection.bulkTransfer(endPointRead, bytes, bytes.length, TIMEOUT); //do in another thread
                        Log.d("TAG", "READ bytes length:" + count);
                        if (count > 0)
                            return bytes;
                        else return new byte[0];
                    }
                }
            }
        } else Log.w("TAG", "connection is null!!!");
        return new byte[0];
    }

    public void Write(byte[] data) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface intf = device.getInterface(i);
            for (int i1 = 0; i1 < intf.getEndpointCount(); i1++) {
                UsbEndpoint endPointRead = intf.getEndpoint(i1);
                if (UsbConstants.USB_DIR_OUT == endPointRead.getDirection()) {
                    int count = connection.bulkTransfer(endPointRead, data, data.length, TIMEOUT); //do in another thread
                    Log.d("TAG", "WRITE bytes length:" + count);
                }
            }
        }
    }

    /*public int write(final byte[] data, final int length) throws IOException {
        int offset = 0;

        while (offset < length) {
            int size = Math.min(length - offset, mInEndpoint.getMaxPacketSize());
            int bytesWritten = mConnection.bulkTransfer(mOutEndpoint,
                    Arrays.copyOfRange(data, offset, offset + size), size, getWriteTimeout());

            if (bytesWritten <= 0) throw new IOException("None written");
            offset += bytesWritten;
        }
        return offset;
    }*/
}
