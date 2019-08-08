package com.hlq.touchserver;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.hlq.touchserver.wrappers.InputManager;

public class EventInjector {
    private InputManager mInputManager;
    private long mLastTime;
    private final MotionEvent.PointerProperties[] pointerProperties = {new MotionEvent.PointerProperties()};
    private final MotionEvent.PointerCoords[] pointerCoords = {new MotionEvent.PointerCoords()};

    public EventInjector(InputManager inputManager) {
        mInputManager = inputManager;

        MotionEvent.PointerProperties props = pointerProperties[0];
        props.id = 0;
        props.toolType = MotionEvent.TOOL_TYPE_FINGER;

        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.orientation = 0;
        coords.pressure = 1;
        coords.size = 1;
    }

    private void setPointerCoords(int x, int y) {
        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.x = x;
        coords.y = y;
    }

    void injectInputEvent(int action, int x, int y) {
        long now = SystemClock.uptimeMillis();
        if (action == MotionEvent.ACTION_DOWN) {
            mLastTime = now;
        }
        if (x > 0) {
            setPointerCoords(x, y);
        }
        MotionEvent event = MotionEvent.obtain(mLastTime, now, action, 1, pointerProperties, pointerCoords, 0, MotionEvent.BUTTON_PRIMARY, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        mInputManager.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    boolean injectKeycode(int keyCode) {
        return injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode)
                && injectKeyEvent(KeyEvent.ACTION_UP, keyCode);
    }

    private boolean injectKeyEvent(int action, int keyCode) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, action, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        return mInputManager.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    private void setScroll(int hScroll, int vScroll) {
        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.setAxisValue(MotionEvent.AXIS_HSCROLL, hScroll);
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);
    }

    private boolean injectScroll(int x, int y, int hScroll, int vScroll) {
        long now = SystemClock.uptimeMillis();

        setPointerCoords(x,y);
        setScroll(hScroll, vScroll);
        MotionEvent event = MotionEvent.obtain(mLastTime, now, MotionEvent.ACTION_SCROLL, 1, pointerProperties, pointerCoords, 0, 0, 1f, 1f, 0,
                0, InputDevice.SOURCE_MOUSE, 0);
        return mInputManager.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

}
