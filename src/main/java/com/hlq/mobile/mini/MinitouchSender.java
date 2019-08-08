package com.hlq.mobile.mini;

import com.android.ddmlib.IDevice;
import com.hlq.mobile.adb.AndroidBridge;
import com.hlq.mobile.utils.Log;
import com.hlq.mobile.utils.Tools;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;

public class MinitouchSender implements Runnable {
    protected IDevice mDevice;
    private static final String TAG = "MinitouchSender";
    private Process mProcess;
    protected int mPort;
    protected SocketChannel mChannel;


    public void start(IDevice device, int port) {
        mPort = port;
        mDevice = device;
        Tools.sExecutor.submit(this);
    }

    public void stop() {
        Log.d(TAG, "stop");
        if (mProcess != null) {
            mProcess.destroy();
        }
        if (mDevice.isOnline()) {
            Tools.executeShellCommand(mDevice, "rm /data/local/tmp/minitouch");
        }
    }

    public boolean prepare(final Semaphore semaphore) {
        String abi = mDevice.getProperty("ro.product.cpu.abi");
        String minitouchPath = Tools.getMinitouchPath() + abi + File.separator + "minitouch";
        File file = new File(minitouchPath);
        if (!file.exists()) {
            Tools.unzipMinitouchLib();
        }
        if (file.exists()) {
            try {
                mDevice.pushFile(minitouchPath, "/data/local/tmp/minitouch");
                Tools.executeShellCommand(mDevice, "chmod 777 /data/local/tmp/minitouch");
                mDevice.createForward(mPort, "minitouch", IDevice.DeviceUnixSocketNamespace.ABSTRACT);
                mProcess = Runtime.getRuntime().exec(AndroidBridge.sAdbPath + " -s " + mDevice.getSerialNumber() + " shell /data/local/tmp/minitouch");
                Tools.sExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new SequenceInputStream(mProcess.getInputStream(), mProcess.getErrorStream())));
                        try {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                Log.d(TAG, "line = " + line);
                                if (line.contains("Type")) {
                                    semaphore.release();
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public void run() {
        Semaphore semaphore = new Semaphore(1);
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (prepare(semaphore)) {
        }
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        createSocket();
    }

    protected void createSocket() {
        try {
            mChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", mPort));
            mChannel.socket().setKeepAlive(true);
            mChannel.socket().setTcpNoDelay(true);
            if (mChannel.finishConnect()) {
                ByteBuffer buffer = ByteBuffer.allocate(40);
                int len = mChannel.read(buffer);
                Log.d(TAG, "read len = " + len);
                buffer.flip();
                Log.d(TAG, "received : \n" + new String(buffer.array(), 0, len, "utf-8"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void touchDown(int x, int y) {
        sendTouchEvent("d 0 " + x + " " + y + " 50\nc\n");
    }

    public void touchUp() {
        sendTouchEvent("u 0\nc\n");
    }

    public void touchMove(int x, int y) {
        sendTouchEvent("m 0 " + x + " " + y + " 50\nc\n");
    }

    private void sendTouchEvent(String event) {
        if (mChannel != null) {
            try {
                mChannel.write(ByteBuffer.wrap(event.getBytes("utf-8")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void keycode(int keycode){
        if (mDevice != null && mDevice.isOnline()) {
            Tools.executeShellCommand(mDevice, "input keyevent " + keycode);
        }
    }
}
