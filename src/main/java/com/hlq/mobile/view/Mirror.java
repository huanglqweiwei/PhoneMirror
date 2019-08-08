package com.hlq.mobile.view;

import com.android.ddmlib.IDevice;
import com.hlq.mobile.mini.MinicapReceiver;
import com.hlq.mobile.mini.MinitouchSender;
import com.hlq.mobile.mini.SingleTouch;
import com.hlq.mobile.utils.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Mirror implements MinicapReceiver.Callback, WindowListener {
    private static final String TAG = "Mirror";
    private final int mTouchPort;
    private  MinicapReceiver mTask;
    private final JLabel mJLabel;
    private final JFrame mJFrame;
    public static final int WIDTH = 360;
    public static final int HEIGHT = 640;
    private final int mPort;
    private ImageIcon mNextIcon;
    private MinitouchSender mMinitouchSender;
    private float mScale;


    public Mirror(IDevice device, JFrame jFrame ,int index){

        mJFrame = createJFrame(device);
        mJFrame.setLocationRelativeTo(jFrame);
        int location = 40 * index;
        mPort = 1700 + index;
        mTouchPort = 1300 + index;
        mJFrame.setLocation(location,location);
        JPanel jPanel = new JPanel();
        jPanel.setSize(WIDTH,HEIGHT);
        jPanel.setLayout(null);
        mJLabel = new JLabel();
        mJLabel.setSize(WIDTH,HEIGHT);
        mJLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int [] xy = convertXY(x, y);
                if (xy != null && mMinitouchSender != null) {
                    mMinitouchSender.touchDown(xy[0],xy[1]);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (mMinitouchSender != null) {
                    mMinitouchSender.touchUp();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {

            }
        });

        mJLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int [] xy = convertXY(x, y);
                if (xy != null && mMinitouchSender != null) {
                    mMinitouchSender.touchMove(xy[0],xy[1]);
                }

            }
        });

        jPanel.add(mJLabel);
        mJFrame.add(jPanel,BorderLayout.CENTER);
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
        power.addActionListener(new ActionListener() {
            long preExe;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (keyCode == 26) {
                    long now = System.currentTimeMillis();
                    if (now - preExe > 500) {
                        preExe = now;
                        if (mMinitouchSender != null) {
                            mMinitouchSender.keycode(keyCode);
                        }
                    }
                } else {
                    if (mMinitouchSender != null) {
                        mMinitouchSender.keycode(keyCode);
                    }
                }

            }
        });
        return power;
    }

    private int[] convertXY(int x, int y) {
        if (mTask != null) {
            if (mScale == 0) {
                mScale = mTask.getDisplayWidth() * 1.0f / WIDTH;
            }
            if (mScale > 0) {
                x = (int) (x * mScale);
                y = (int) (y * mScale);
                return new int[]{x,y};
            }
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
            if (mMinitouchSender != null) {
                mMinitouchSender.stop();
            }
            mMinitouchSender = new SingleTouch();
            mMinitouchSender.start(device,mTouchPort);
        }
    }


    private JFrame createJFrame(IDevice device) {
        JFrame jFrame = new JFrame(device.getName());

        jFrame.setSize(WIDTH + 2,HEIGHT +  60);
        jFrame.addWindowListener(this);
        jFrame.setBackground(Color.WHITE);
        jFrame.setLayout(new BorderLayout());
        jFrame.setResizable(false);
        return jFrame;
    }

    @Override
    public void updateFrame(byte[] data) {
        Image image = Toolkit.getDefaultToolkit().createImage(data);
        Icon icon = mJLabel.getIcon();
        if (icon == null) {
            icon = new ImageIcon(image);
        } else {
            if (mNextIcon == null) {
                mNextIcon = (ImageIcon) icon;
                icon = new ImageIcon(image);
            } else {
                mNextIcon.setImage(image);
                ImageIcon temp = (ImageIcon) icon;
                icon = mNextIcon;
                mNextIcon = temp;
            }
        }
        mJLabel.setIcon(icon);
    }
    public void stop(){
        mJLabel.setIcon(null);
        mTask.stop();
        if (mMinitouchSender != null) {
            mMinitouchSender.stop();
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
