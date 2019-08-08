package com.hlq.mobile.mini;

import com.android.ddmlib.IDevice;
import com.hlq.mobile.adb.AndroidBridge;
import com.hlq.mobile.utils.Log;
import com.hlq.mobile.utils.Tools;
import com.hlq.mobile.view.Mirror;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinicapReceiver  implements Runnable{
    private static final String TAG = "MinicapReceiver";

    private boolean stoped  = false;
    private IDevice mDevice;
    private Callback mCallback;
    private int mPort;
    private CountDownLatch mLatch;
    private SocketChannel mChannel;
    private int mPid;
    private int mWidth;

    public MinicapReceiver(IDevice iDevice ,Callback callback ,int port){
        mDevice = iDevice;
        mCallback = callback;
        mPort = port;
    }

    @Override
    public void run() {
        Process process = prepareMinicap();
        if (process != null) {
            minicapRecord(mCallback);
            process.destroy();
            destroy();
        }
    }

    public void destroy() {
        if (mDevice != null && mDevice.isOnline()) {
            if (mPid != 0) {
                String result = Tools.executeShellCommand(mDevice, "kill -9 " + mPid);
                Log.d(TAG,"kill result = " + result);
            }
            Tools.executeShellCommand(mDevice, "rm /data/local/tmp/minicap");

        }
        if (mLatch != null) {
            mLatch.countDown();
        }
    }

    public void stop(){
        if (!stoped) {
            stoped = true;
            if (mChannel != null) {
                try {
                    mChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mChannel = null;
            }
            mLatch = new CountDownLatch(1);
            boolean await = false;
            try {
                await =  mLatch.await(2000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mLatch = null;

            Log.d(TAG,"stop minicap await = " + await);
        }
    }

    public Process prepareMinicap() {
        String sdk = mDevice.getProperty("ro.build.version.sdk");
        String abi = mDevice.getProperty("ro.product.cpu.abi");
        Log.d(TAG,"mDevice.getProperty : sdk = " + sdk + " abi = " + abi);
        String minicapLibPath = Tools.getMinicapLibPath();
        String minicapLib = minicapLibPath + "aosp" + File.separator + "libs" + File.separator + "android-" + sdk + File.separator + abi + File.separator + "minicap.so";
        String minicap = minicapLibPath + "libs" + File.separator + abi + File.separator + "minicap";
        if (!new File(minicap).exists()) {
            Tools.unzipMinicapLib();
        }
        if (!new File(minicapLib).exists()) {
            Tools.unzipMinicapLib();
        }
        try {
            mDevice.pushFile(minicapLib,"/data/local/tmp/minicap.so");
            mDevice.pushFile(minicap,"/data/local/tmp/minicap");
            Tools.executeShellCommand(mDevice,"chmod 777 /data/local/tmp/minicap");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mDevice.createForward(mPort,"minicap", IDevice.DeviceUnixSocketNamespace.ABSTRACT);
            Process process = Runtime.getRuntime().exec(AndroidBridge.sAdbPath + " -s " + mDevice.getSerialNumber() + " shell LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/minicap -P "+getDisplaySize(mDevice)+"@"+ Mirror.WIDTH +"x"+Mirror.HEIGHT +"/0");
            BufferedReader reader = new BufferedReader(new InputStreamReader(new SequenceInputStream(process.getInputStream(), process.getErrorStream())));
            try {
                String line;
                while ((line = reader.readLine()) != null){
                    Log.d(TAG+"_reader","line = " + line);
                    if (line.contains("JPG encoder")) {
                        break;
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return process;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private String getDisplaySize(IDevice iDevice) {
        String result =  Tools.executeShellCommand(iDevice,"wm size");
        Log.d(TAG,"getDisplaySize : " + result);
        if (result != null) {
            try {
                Pattern pattern = Pattern.compile("(\\d+)x(\\d+)");
                Matcher matcher = pattern.matcher(result);
                if (matcher.find()) {
                    String width = matcher.group(1);
                    String height = matcher.group(2);
                    Log.d(TAG, "getDisplaySize : width = " + width + ", height = " + height);
                    mWidth = Integer.valueOf(width);
                    return width + "x" + height;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return "1080x1920";
    }

    public int getDisplayWidth() {
        return mWidth;
    }

    public void minicapRecord(Callback callback) {
        try {
            mChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",mPort));
            mChannel.socket().setTcpNoDelay(true);
            mChannel.socket().setKeepAlive(true);
            if (mChannel.finishConnect()) {
                Log.d(TAG,"finish connect port = " + mPort);
                ByteBuffer header = ByteBuffer.allocate(24);
                fillBuffer(header, mChannel);
                header.flip();
                log("version = " + header.get());
                int headLen = header.get();
                log("headLen = " + headLen);
                if (headLen == 24) {
                    mPid = getInt(header);
                    log("pid = " + mPid);
                    log("realWidth = " + getInt(header));
                    log("realHeight = " + getInt(header));
                    log("virWidth = " + getInt(header));
                    log("virHeight = " +getInt(header));
                    log("orientation = " + header.get());
                    log("Quirk = " + header.get());
                }
                ByteBuffer frameLen = ByteBuffer.allocate(4);
                ByteBuffer frame;
                int len;
                while (!stoped) {
                    fillBuffer(frameLen, mChannel);
                    if (!frameLen.hasRemaining()) {
                        frameLen.flip();
                        len =  getInt(frameLen);
                        if (len > 1) {
                            frame = ByteBuffer.allocate(len);
                            fillBuffer(frame, mChannel);
                            frame.flip();
                            if (callback != null) {
                                callback.updateFrame(frame.array());
                            }
                        }
                    } else {
                        log("len has remain = " + frameLen.remaining());
                    }
                    frameLen.clear();
                }
            }
            log("record end...");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mChannel != null) {
                try {
                    mChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private  int getInt(ByteBuffer buffer) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (buffer.get() & 0xff )<< (i * 8);
        }
        return result;
    }
    private void fillBuffer(ByteBuffer buffer,SocketChannel channel) throws  IOException{
        int len;
        do {
            len = channel.read(buffer);
        }while (buffer.hasRemaining() && len != -1);
    }
    private void log(String msg){
        System.out.println("ReceiveJpg : " + msg);
    }


    public interface Callback {
        void updateFrame(byte[] data);
    }
}
