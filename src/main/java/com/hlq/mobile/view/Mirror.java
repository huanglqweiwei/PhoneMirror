package com.hlq.mobile.view;

import com.android.ddmlib.IDevice;
import com.hlq.mobile.mini.MinicapReceiver;
import com.hlq.mobile.mini.SingleTouch;
import com.hlq.mobile.utils.Tools;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class Mirror implements MinicapReceiver.Callback, WindowListener {
    private static final String TAG = "Mirror";
    private final int mTouchPort;
    private  MinicapReceiver mTask;
    private final ImageJPanel mJLabel;
    private final JFrame mJFrame;
    public static final int WIDTH = 360;
    public static final int HEIGHT = 640;
    private final int mPort;
    private SingleTouch mSingleTouch;
    private float mScale;


    public Mirror(IDevice device, JFrame jFrame ,int index){

        mJFrame = createJFrame(device);
        mJFrame.setLocationRelativeTo(jFrame);
        int location = 40 * index;
        mPort = 1700 + index;
        mTouchPort = 1300 + index;
        mJFrame.setLocation(location,location);
        mJLabel = new ImageJPanel();
        mJLabel.setSize(WIDTH,HEIGHT);
        mJLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int [] xy = convertXY(x, y);
                if (xy != null && mSingleTouch != null) {
                    mSingleTouch.touchDown(xy[0],xy[1]);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (mSingleTouch != null) {
                    mSingleTouch.touchUp();
                }
            }

        });

        mJLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int [] xy = convertXY(x, y);
                if (xy != null && mSingleTouch != null) {
                    mSingleTouch.touchMove(xy[0],xy[1]);
                }

            }
        });

        mJLabel.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int wheelRotation = e.getWheelRotation();
                if (wheelRotation != 0) {
                    int [] xy = convertXY(e.getX(), e.getY());
                    if (xy != null && mSingleTouch != null) {

                        mSingleTouch.touchScroll(xy[0],xy[1], (e.getModifiers() & InputEvent.SHIFT_MASK) != 0,wheelRotation);
                    }
                }
            }
        });

        mJFrame.add(mJLabel,BorderLayout.CENTER);
        addBottomButton();
    }

    private void addBottomButton() {
        JPanel panel = new JPanel(new GridLayout(1, 4));
        mJFrame.add(panel,BorderLayout.SOUTH);
        panel.add(getJButton("POWER", 26));
        panel.add(getJButton("MENU",82));
        panel.add(getJButton("HOME",3));
        panel.add(getJButton("BACK",4));
    }

    private JButton getJButton(String title, final int keyCode) {
        JButton power = new JButton(title);
        power.setFocusPainted(false);
        power.addActionListener(new ActionListener() {
            long preExe;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (keyCode == 26) {
                    long now = System.currentTimeMillis();
                    if (now - preExe > 500) {
                        preExe = now;
                        if (mSingleTouch != null) {
                            mSingleTouch.keycode(keyCode);
                        }
                    }
                } else {
                    if (mSingleTouch != null) {
                        mSingleTouch.keycode(keyCode);
                    }
                }

            }
        });
        return power;
    }

    private int[] convertXY(int x, int y) {
        if (mScale > 0) {
            x = (int) (x * mScale);
            y = (int) (y * mScale);
            return new int[]{x,y};
        }
        return null;
    }

    public void show(IDevice device){
        if (!mJFrame.isShowing()) {
            if (mTask != null) {
                mTask.stop();
            }
            mTask = new MinicapReceiver(device, this,mPort);
            Tools.sExecutor.submit(mTask);
            mJFrame.setVisible(true);
            if (mSingleTouch != null) {
                mSingleTouch.stop();
            }
            mSingleTouch = new SingleTouch();
            mSingleTouch.start(device,mTouchPort);
        }
    }


    private JFrame createJFrame(IDevice device) {
        JFrame jFrame = new JFrame(device.getName());

        jFrame.setSize(WIDTH,HEIGHT +  60);
        jFrame.addWindowListener(this);
        jFrame.setLayout(new BorderLayout());
        jFrame.setResizable(false);
        return jFrame;
    }

    @Override
    public void updateFrame(byte[] data) {
        try {
            Image image = ImageIO.read(new ByteArrayInputStream(data));
            mJLabel.setImage(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onDisplaySize(int width, int height) {
        mScale = width * 1.0f / WIDTH;
        height = (int) (height /mScale);
        mJFrame.setSize(WIDTH , height + 60);
        mJLabel.setSize(WIDTH,height);
        return height;
    }

    public void stop(){
        mJLabel.setImage(null);
        mTask.stop();
        if (mSingleTouch != null) {
            mSingleTouch.stop();
        }
        mJFrame.dispose();
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        stop();
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
}
