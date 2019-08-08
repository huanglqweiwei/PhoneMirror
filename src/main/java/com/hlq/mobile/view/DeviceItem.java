package com.hlq.mobile.view;

import com.android.ddmlib.IDevice;
import com.hlq.mobile.utils.Log;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class DeviceItem {
    private static final String TAG = "DeviceItem";
    private IDevice mIDevice;
    private JFrame mJFrame;
    private JButton mJButton;
    private Mirror mMirror;
    private static int COUNT = 0;
    private int index;

    public DeviceItem(IDevice iDevice, JFrame jFrame) {
        mIDevice = iDevice;
        mJFrame = jFrame;
        COUNT++;
        index = COUNT;
    }


    public void setDevice(IDevice iDevice) {
        mIDevice = iDevice;
    }

    public JButton getJButton() {
        if (mJButton == null) {
            mJButton = new JButton(mIDevice.getName());
            mJButton.setSize(288,40);
            mJButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Log.d(TAG,"actionPerformed");
                    if (mMirror == null) {
                        mMirror = new Mirror(mIDevice,mJFrame,index);
                    }
                    mMirror.show(mIDevice);
                }
            });
        }
        return mJButton;
    }
    public void stop(){
        if (mMirror != null) {
            mMirror.stop();
            mMirror = null;
        }
    }
}
