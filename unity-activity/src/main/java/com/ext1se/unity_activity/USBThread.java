package com.ext1se.unity_activity;

import static com.ext1se.unity_activity.DonglePluginActivity.TIMEOUT;
import static com.ext1se.unity_activity.core.services.AbstractUSBHIDService.REPORT_TYPE_OUTPUT;
import static com.ext1se.unity_activity.core.services.AbstractUSBHIDService.REQUEST_GET_REPORT;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.health.connect.datatypes.Device;
import android.os.Handler;
import android.util.Log;

import com.ext1se.unity_activity.core.Consts;

import java.util.List;

public class USBThread extends Thread {

    private volatile boolean isStopped;
    private UsbDeviceConnection connection;
    private List<UsbInterface> interfacesList;
    private UsbDevice device;
    private Handler uiHandler = new Handler();

    private byte[] data = new byte[0];


    public USBThread() {
    }

    public void setConnection(UsbDeviceConnection connection) {
        this.connection = connection;
    }

    public void setInterfacesList(List<UsbInterface> interfacesList) {
        this.interfacesList = interfacesList;
    }

    public void setUiHandler(Handler uiHandler) {
        this.uiHandler = uiHandler;
    }

    public void setDevice(UsbDevice device) {
        this.device = device;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void run() {
        try {
            if (connection == null) {
                Log.d(Consts.APP_TAG, "Connection = null");
            }

            if (interfacesList == null) {
                Log.d(Consts.APP_TAG, "InterfacesList = null");
            }

            if (uiHandler == null) {
                Log.d(Consts.APP_TAG, "uiHandler = null");
            }

            if (connection != null) {
                while (!isStopped) {
                    //for (UsbInterface intf : interfacesList) {
                    for (int i = 0; i < device.getInterfaceCount(); i++) {
                        UsbInterface intf = device.getInterface(i);

                        for (int j = 0; j < intf.getEndpointCount(); j++) {
                            UsbEndpoint endPointRead = intf.getEndpoint(j);
                            if (UsbConstants.USB_DIR_IN == endPointRead.getDirection()) {
                                final byte[] buffer = new byte[endPointRead.getMaxPacketSize()];
                                final int status = connection.bulkTransfer(endPointRead, buffer, buffer.length, TIMEOUT);

                                Log.d(Consts.APP_TAG, "READ bytes length:" + buffer.length);


                                if (status > 0) {
                                    data = buffer;

                                    if (uiHandler != null) {
                                        uiHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                //onUSBDataReceive(buffer);
                                            }
                                        });
                                    }
                                } else {
                                    //TODO: learn it
                                    data = new byte[0];


                                /*    int transfer = connection.controlTransfer(0xA0, REQUEST_GET_REPORT, REPORT_TYPE_OUTPUT, 0x00, buffer, buffer.length, 100);
                                    if (transfer > 0) {
                                        data = buffer;

                                        if (uiHandler != null) {
                                            uiHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    //onUSBDataReceive(buffer);
                                                }
                                            });
                                        }
                                    }*/

                                    Log.d(Consts.APP_TAG, "READ bytes length: 0");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Consts.APP_TAG, "Error in receive thread", e);
        }
    }

    public void stopThis() {
        isStopped = true;
    }
}
