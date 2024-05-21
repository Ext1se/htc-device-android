package com.ext1se.unity_activity.core.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.ext1se.unity_activity.R;
import com.ext1se.unity_activity.USBHIDTerminal;
import com.ext1se.unity_activity.core.Consts;
import com.ext1se.unity_activity.core.USBUtils;
import com.ext1se.unity_activity.core.events.DeviceAttachedEvent;
import com.ext1se.unity_activity.core.events.DeviceDetachedEvent;
import com.ext1se.unity_activity.core.events.LogMessageEvent;
import com.ext1se.unity_activity.core.events.ShowDevicesListEvent;
import com.ext1se.unity_activity.core.events.USBDataReceiveEvent;

public class USBHIDService extends AbstractUSBHIDService {

    private String delimiter;
    private String receiveDataFormat;

    @Override
    public void onCreate() {
        super.onCreate();
        setupNotifications();
    }

    @Override
    public void onCommand(Intent intent, String action, int flags, int startId) {
        if (Consts.RECEIVE_DATA_FORMAT.equals(action)) {
            receiveDataFormat = intent.getStringExtra(Consts.RECEIVE_DATA_FORMAT);
            delimiter = intent.getStringExtra(Consts.DELIMITER);
        }
        super.onCommand(intent, action, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDeviceConnected(UsbDevice device) {
        mLog("device VID:0x" + Integer.toHexString(device.getVendorId()) + " PID:0x" + Integer.toHexString(device.getProductId()) + " " + device.getDeviceName() + " connected");
    }

    @Override
    public void onDeviceDisconnected(UsbDevice device) {
        mLog("device disconnected");
        eventBus.post(new DeviceDetachedEvent());
    }

    @Override
    public void onDeviceSelected(UsbDevice device) {
        mLog("Selected device VID:0x" + Integer.toHexString(device.getVendorId()) + " PID:0x" + Integer.toHexString(device.getProductId()));
        mLog("id " + showDecHex(device.getDeviceId()));
        mLog("name " + device.getDeviceName());
        mLog("manufacturer name " + device.getManufacturerName());
        mLog("serial number " + device.getSerialNumber());
        mLog("class " + showDecHex(device.getDeviceClass()));
        mLog("subclass " + showDecHex(device.getDeviceSubclass()));
        mLog("protocol " + showDecHex(device.getDeviceProtocol()));
        mLog("");
        mLog("interfaces count " + device.getInterfaceCount());
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            mLog("");
            mLog("interface " + i);
            UsbInterface dInterface = device.getInterface(i);
            mLog(" name " + dInterface.getName());
            mLog(" id " + showDecHex(dInterface.getId()));
            mLog(" class " + showDecHex(dInterface.getInterfaceClass()));
            mLog(" subclass " + showDecHex(dInterface.getInterfaceSubclass()));
            mLog(" protocol " + showDecHex(dInterface.getInterfaceProtocol()));
            mLog("");
            mLog(" endpoint count " + dInterface.getEndpointCount());
            for (int ien = 0; ien < dInterface.getEndpointCount(); ien++) {
                UsbEndpoint endpoint = dInterface.getEndpoint(ien);
                mLog("");
                mLog("  endpoint " + ien);
                mLog("  endpoint number " + endpoint.getEndpointNumber());
                mLog("  address " + showDecHex(endpoint.getAddress()));
                mLog("  type " + showDecHex(endpoint.getType()));
                mLog("  direction " + directionInfo(endpoint.getDirection()));
                mLog("  max packet size " + endpoint.getMaxPacketSize());
                mLog("  interval " + endpoint.getInterval());
                mLog("  attributes " + showDecHex(endpoint.getAttributes()));
            }
        }
        mLog("");
        mLog("configuration count " + device.getConfigurationCount());
        for (int i = 0; i < device.getConfigurationCount(); i++) {
            UsbConfiguration configuration = device.getConfiguration(i);
            mLog("");
            mLog("configuration " + i);
            mLog(" name " + configuration.getName());
            mLog(" id " + showDecHex(configuration.getId()));
            mLog(" max power " + configuration.getMaxPower());
            mLog(" is self powered " + configuration.isSelfPowered());
            mLog("");
            mLog("configuration interfaces count " + configuration.getInterfaceCount());
            for (int ic = 0; i < configuration.getInterfaceCount(); i++) {
                mLog("");
                mLog("configuration interface " + ic);
                UsbInterface cInterface = configuration.getInterface(i);
                mLog(" name " + cInterface.getName());
                mLog(" id " + showDecHex(cInterface.getId()));
                mLog(" class " + showDecHex(cInterface.getInterfaceClass()));
                mLog(" subclass " + showDecHex(cInterface.getInterfaceSubclass()));
                mLog(" protocol " + showDecHex(cInterface.getInterfaceProtocol()));
                mLog("");
                mLog(" configuration endpoint count " + cInterface.getEndpointCount());
                for (int ien = 0; ien < cInterface.getEndpointCount(); ien++) {
                    UsbEndpoint endpoint = cInterface.getEndpoint(ien);
                    mLog("");
                    mLog("  endpoint " + ien);
                    mLog("  endpoint number " + endpoint.getEndpointNumber());
                    mLog("  address " + showDecHex(endpoint.getAddress()));
                    mLog("  type " + showDecHex(endpoint.getType()));
                    mLog("  direction " + directionInfo(endpoint.getDirection()));
                    mLog("  max packet size " + endpoint.getMaxPacketSize());
                    mLog("  interval " + endpoint.getInterval());
                    mLog("  attributes " + showDecHex(endpoint.getAttributes()));
                }
            }
        }

    }

    @Override
    public void onDeviceAttached(UsbDevice device) {
        eventBus.post(new DeviceAttachedEvent());
    }

    @Override
    public void onShowDevicesList(CharSequence[] deviceName) {
        eventBus.post(new ShowDevicesListEvent(deviceName));
    }

    private String directionInfo(int data) {
        if (UsbConstants.USB_DIR_IN == data) {
            return "IN " + showDecHex(data);
        }
        if (UsbConstants.USB_DIR_OUT == data) {
            return "OUT " + showDecHex(data);
        }
        return "NA " + showDecHex(data);
    }

    private String showDecHex(int data) {
        return data + " 0x" + Integer.toHexString(data);
    }

    @Override
    public CharSequence onBuildingDevicesList(UsbDevice usbDevice) {
        return "VID:0x" + Integer.toHexString(usbDevice.getVendorId()) + " PID:0x" + Integer.toHexString(usbDevice.getProductId()) + " " + usbDevice.getDeviceName() + " devID:" + usbDevice.getDeviceId();
    }

    @Override
    public void onUSBDataSending(String data) {
        mLog("Sending: " + data);
    }

    @Override
    public void onUSBDataSended(int status, byte[] out) {
        if (status <= 0) {
            mLog("Unable to send");
        } else {
            mLog("Sended " + status + " bytes");
            for (int i = 0; i < out.length/* && out[i] != 0*/; i++) {
                mLog(Consts.SPACE + USBUtils.toInt(out[i]));
            }
        }
    }

    @Override
    public void onSendingError(Exception e) {
        mLog("Please check your bytes, sent as text");
    }

    @Override
    public void onUSBDataReceive(byte[] buffer) {
        Log.d("USB_DataReceive", "buffer.length: " + buffer.length); //!!!!!!!!!!!!!!!!!!!!!!!!!

        if (receiveDataFormat == null){
            Log.d("USB_DataReceive", "receiveDataFormat = null" );
            receiveDataFormat = Consts.TEXT;
        }

        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        if (receiveDataFormat.equals(Consts.INTEGER)) {
            for (; i < buffer.length/* && buffer[i] != 0*/; i++) {
                stringBuilder.append(delimiter).append(String.valueOf(USBUtils.toInt(buffer[i])));
            }
        } else if (receiveDataFormat.equals(Consts.HEXADECIMAL)) {
            for (; i < buffer.length/* && buffer[i] != 0*/; i++) {
                stringBuilder.append(delimiter).append(Integer.toHexString(buffer[i]));
            }
        } else if (receiveDataFormat.equals(Consts.TEXT)) {
            for (; i < buffer.length/* && buffer[i] != 0*/; i++) {
                stringBuilder.append(String.valueOf((char) buffer[i]));
            }
        } else if (receiveDataFormat.equals(Consts.BINARY)) {
            for (; i < buffer.length/* && buffer[i] != 0*/; i++) {
                stringBuilder.append(delimiter).append("0b").append(Integer.toBinaryString(Integer.valueOf(buffer[i])));
            }
        }
        //eventBus.post(new USBDataReceiveEvent(stringBuilder.toString(), i));
        eventBus.post(new USBDataReceiveEvent(
                stringBuilder.toString(),
                buffer,
                i));
    }

    private void mLog(String log) {
        eventBus.post(new LogMessageEvent(log));
    }

    private void setupNotifications() { //called in onCreate()

        Log.d("USBHIDService", "sadsadasdasdasdsadsssssssssssssssssssssssssssssssssssssssssssssssss");

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(this);

        NotificationChannel mChannel = null;

        String idChannel = "usb_channel";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel(idChannel, "USB Service", NotificationManager.IMPORTANCE_LOW);
            mChannel.setDescription("Description");
            mChannel.enableVibration(true);

            mNotificationManager.createNotificationChannel(mChannel);
        }

        int pendingFlags = PendingIntent.FLAG_MUTABLE;
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            //pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            pendingFlags = PendingIntent.FLAG_MUTABLE;
        } else {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, USBHIDTerminal.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                pendingFlags);
        PendingIntent pendingCloseIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, USBHIDTerminal.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .setAction(Consts.USB_HID_TERMINAL_CLOSE_ACTION),
                pendingFlags);
        mNotificationBuilder
                .setSmallIcon(R.drawable.ic_launcher)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(getText(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_exit), pendingCloseIntent)
                .setOngoing(true);
        mNotificationBuilder
                .setTicker(getText(R.string.app_name))
                .setContentText(getText(R.string.app_name));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mNotificationBuilder.setChannelId(idChannel);
        }

        if (mNotificationManager != null) {
            mNotificationManager.notify(Consts.USB_HID_TERMINAL_NOTIFICATION, mNotificationBuilder.build());
        }

        Log.d("USBHIDService", "2 ssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss");
    }

}
