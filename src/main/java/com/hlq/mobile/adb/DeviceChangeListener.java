package com.hlq.mobile.adb;

import com.android.ddmlib.IDevice;

public interface DeviceChangeListener {
    void onDeviceConnect(IDevice iDevice);
    void onDeviceDisconnect(IDevice iDevice);
}
