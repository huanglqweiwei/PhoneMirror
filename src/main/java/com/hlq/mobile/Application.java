package com.hlq.mobile;


import com.android.ddmlib.IDevice;
import com.hlq.mobile.adb.AndroidBridge;
import com.hlq.mobile.adb.DeviceChangeListener;
import com.hlq.mobile.utils.Log;
import com.hlq.mobile.utils.Tools;
import com.hlq.mobile.view.DeviceItem;
import com.hlq.mobile.view.VFlowLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Map;

public class Application implements WindowListener, DeviceChangeListener {
    private static final String TAG = "Application";
    private AndroidBridge mAndroidBridge;
    private JFrame mJFrame;
    private Map<String, DeviceItem> mDeviceItems;
    private JPanel mJPanel;

    public void start() {

        mJFrame = new JFrame("PhoneMirror");

        mJFrame.setSize(290, 512);
        mJFrame.setLocationRelativeTo(null);
        mJFrame.addWindowListener(this);
        mJFrame.setBackground(Color.WHITE);

        mJPanel = new JPanel();
        mJPanel.setLayout(new VFlowLayout());

        mJFrame.add(new JScrollPane(mJPanel), BorderLayout.CENTER);

        mJFrame.setVisible(true);
        String adbPath = Tools.getAdbPath();
        Log.d(TAG,"adb Path = " + adbPath);
        mAndroidBridge = AndroidBridge.init(adbPath);
        mAndroidBridge.setChangeListener(this);

    }


    @Override
    public void windowOpened(WindowEvent e) {
        Log.d(TAG, "windowOpened");
    }

    @Override
    public void windowClosing(WindowEvent e) {
        Log.d(TAG, "windowClosing");
        if (mDeviceItems != null) {
            for (DeviceItem item:mDeviceItems.values()) {
                item.stop();
            }
            mDeviceItems.clear();
        }
        System.exit(0);
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void onDeviceConnect(IDevice iDevice) {
        Log.d(TAG, "deviceConnected : " + iDevice);
        if (mDeviceItems == null) {
            mDeviceItems = new HashMap<String, DeviceItem>();
        }
        DeviceItem deviceItem = mDeviceItems.get(iDevice.getSerialNumber());
        if (deviceItem == null) {
            deviceItem = new DeviceItem(iDevice,mJFrame);
            mDeviceItems.put(iDevice.getSerialNumber(), deviceItem);
        } else {
            deviceItem.setDevice(iDevice);
        }

        addItem(deviceItem.getJButton());
    }

    private void addItem(JButton jButton) {
        Component[] components = mJPanel.getComponents();
        boolean contain = false;
        if (components != null) {
            for (Component component : components) {
                if (component.equals(jButton)) {
                    contain = true;
                    break;
                }
            }
        }
        if (!contain) {
            try {
                Log.d(TAG, "add device");
                mJPanel.add(jButton);
                SwingUtilities.updateComponentTreeUI(mJPanel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onDeviceDisconnect(IDevice iDevice) {
        Log.d(TAG, "deviceDisconnected : " + iDevice);
        if (mDeviceItems != null) {
            DeviceItem deviceItem = mDeviceItems.get(iDevice.getSerialNumber());
            if (deviceItem != null) {
                deviceItem.stop();
                mJPanel.remove(deviceItem.getJButton());
                SwingUtilities.updateComponentTreeUI(mJPanel);
            }
        }

    }
}
