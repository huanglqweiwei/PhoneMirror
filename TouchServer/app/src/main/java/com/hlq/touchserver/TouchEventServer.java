package com.hlq.touchserver;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;
import android.view.MotionEvent;
import com.hlq.touchserver.wrappers.ServiceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class TouchEventServer {
    private static final String TAG = "TouchEventServer";
    private static final String HOST = "singleTouch";
    private static final byte TYPE_MOTION = 0;
    private static final byte TYPE_KEYCODE = 1;
    private final ServiceManager mServiceManager;
    private final byte[] lenBytes = new byte[4];
    private final byte[] contentBytes = new byte[10];
    private final InputStream mInputStream;
    private EventInjector mEventInjector;

    public static void main(String[] args){
        try {
            new TouchEventServer().loop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TouchEventServer() throws IOException{
        LocalServerSocket server = null;
        try {
            server = new LocalServerSocket(HOST);
            LogUtil.d("OK !");
            LocalSocket client = server.accept();
            LogUtil.d("client bind SUCCESS !");
            mServiceManager = new ServiceManager();
            int[] info = mServiceManager.getDisplayManager().getDisplayInfo();
            OutputStream output = client.getOutputStream();
            output.write((
                    "width : " + info[0] + '\n'
                    +"height : " + info[1] + '\n'
                    +"rotation : " + info[2]
            ).getBytes());
            mInputStream = client.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "TouchEventServer: ",e );
            throw e;
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private ByteBuffer readByte(byte[] bytes,int length) {
        try {
            int len;
            int offset = 0;
            while ((len = mInputStream.read(bytes,offset,length - offset)) > -1 && offset != length){
                offset += len;
            }
            if (offset == length) {
                return ByteBuffer.wrap(bytes,0,offset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void loop(){
        while (true){
            ByteBuffer lenBuffer = readByte(lenBytes,lenBytes.length);
            if (lenBuffer != null) {
                int len = lenBuffer.getInt();
                if (len > 0) {
                    ByteBuffer buffer = readByte(contentBytes, len);
                    if (buffer != null) {
                        try {
                            handleEvent(buffer);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void handleEvent(ByteBuffer buffer) {
        if (mEventInjector == null) {
            mEventInjector = new EventInjector(mServiceManager.getInputManager());
        }
        byte type = buffer.get();
        if (type == TYPE_MOTION) {
            byte action = buffer.get();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    mEventInjector.injectInputEvent(action,buffer.getInt(),buffer.getInt());
                    break;
                case MotionEvent.ACTION_UP:
                    mEventInjector.injectInputEvent(action,-1,-1);
                    break;
            }
        } else if (type == TYPE_KEYCODE){
            mEventInjector.injectKeycode(buffer.getInt());
        }
    }

}
