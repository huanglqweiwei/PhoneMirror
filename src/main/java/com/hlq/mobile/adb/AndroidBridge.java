package com.hlq.mobile.adb;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.hlq.mobile.utils.Log;

public class AndroidBridge implements AndroidDebugBridge.IDeviceChangeListener {
    private static final String TAG = "AndroidBridge";
    public static String sAdbPath;
    private DeviceChangeListener mChangeListener;

    private AndroidBridge(String adbPath){
        AndroidDebugBridge.init(false);
        AndroidDebugBridge.addDeviceChangeListener(this);
        AndroidDebugBridge.createBridge(adbPath, true);
    }


    public  static AndroidBridge init(String adbPath){
        sAdbPath = adbPath;
        return new AndroidBridge(adbPath);
    }

    public static void terminate() {
        AndroidDebugBridge.disconnectBridge();
        AndroidDebugBridge.terminate();

    }

    @Override
    public void deviceConnected(IDevice iDevice) {


    }

    @Override
    public void deviceDisconnected(IDevice iDevice) {
        if (mChangeListener != null) {
            mChangeListener.onDeviceDisconnect(iDevice);
        }
    }

    @Override
    public void deviceChanged(IDevice iDevice, int mark) {
        Log.d(TAG,"deviceChanged : " + iDevice);
        if (mark == IDevice.CHANGE_BUILD_INFO) {
            if (mChangeListener != null) {
                mChangeListener.onDeviceConnect(iDevice);
            }
        }
    }

    public void setChangeListener(DeviceChangeListener changeListener) {
        mChangeListener = changeListener;
    }
}
