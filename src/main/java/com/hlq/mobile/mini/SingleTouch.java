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

public class SingleTouch  implements Runnable{
    private static final String TAG = "SingleTouch";
    private Process mProcess;
    private ByteBuffer mBuffer = ByteBuffer.allocate(14);
    private static final byte TYPE_MOTION = 0;
    private static final byte TYPE_KEYCODE = 1;
    private static final byte ACTION_DOWN = 0;
    private static final byte ACTION_UP = 1;
    private static final byte ACTION_MOVE = 2;
    private boolean mStopped = false;
    private int mPort;
    private SocketChannel mChannel;
    private IDevice mDevice;

    public void start(IDevice device, int port) {
        mPort = port;
        mDevice = device;
        Tools.sExecutor.submit(this);
        mStopped = false;
    }

    public void stop() {
        if (!mStopped) {
            mStopped = true;
            Log.d(TAG,"stop");
            if (mProcess != null) {
                mProcess.destroy();
                mProcess = null;
            }
            if (mDevice.isOnline()) {
                Tools.executeShellCommand(mDevice, "rm /data/local/tmp/" + Tools.SINGLETOUCH_APK);
            }
        }
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

    public boolean prepare(Semaphore semaphore) {
        BufferedReader reader = null;
        try {
            File apkFile = new File(Tools.getLocalPath() + File.separator + Tools.LIBS_PATH_NAME, Tools.SINGLETOUCH_APK);
            if (!apkFile.exists()) {
                Tools.copySingleTouchApk(apkFile);
            }
            String remotePath = "/data/local/tmp/" + Tools.SINGLETOUCH_APK;
            mDevice.pushFile(apkFile.getAbsolutePath(),remotePath);
            mDevice.createForward(mPort, "singleTouch", IDevice.DeviceUnixSocketNamespace.ABSTRACT);
            mProcess = Runtime.getRuntime().exec(AndroidBridge.sAdbPath + " -s " + mDevice.getSerialNumber() + " shell");
            OutputStream output = mProcess.getOutputStream();
            output.write(("export CLASSPATH=" + remotePath + "\n").getBytes());
            output.write(("app_process /system/bin com.hlq.touchserver.TouchEventServer\n").getBytes());
            output.flush();
            reader = new BufferedReader(new InputStreamReader(new SequenceInputStream(mProcess.getInputStream(), mProcess.getErrorStream())));
            String line;
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "line = " + line);
                if (line.contains("OK")) {
                    break;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            semaphore.release();
        }
        return false;
    }


    public void touchDown(int x, int y) {
        sendTouch(x, y, ACTION_DOWN);

    }

    public void touchMove(int x, int y) {
        sendTouch(x, y, ACTION_MOVE);
    }

    private void sendTouch(int x, int y, byte action) {
        mBuffer.clear();
        mBuffer.putInt(10);
        mBuffer.put(TYPE_MOTION);
        mBuffer.put(action);
        mBuffer.putInt(x);
        mBuffer.putInt(y);
        mBuffer.flip();
        writeBuffer();
    }

    private void writeBuffer() {
        if (mChannel != null) {
            try {
                mChannel.write(mBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void touchUp() {
        mBuffer.clear();
        mBuffer.putInt(2);
        mBuffer.put(TYPE_MOTION);
        mBuffer.put(ACTION_UP);
        mBuffer.flip();
        writeBuffer();
    }

    public void keycode(int keycode){
        mBuffer.clear();
        mBuffer.putInt(5);
        mBuffer.put(TYPE_KEYCODE);
        mBuffer.putInt(keycode);
        mBuffer.flip();
        writeBuffer();
    }
}
