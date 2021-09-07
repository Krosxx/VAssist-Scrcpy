package com.vove7.scrcpy;

import android.graphics.Path;
import android.os.Build;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.Closeable;
import java.util.List;

import cn.vove7.scrcpy.common.ControlMessage;
import cn.vove7.scrcpy.common.DeviceMessage;
import cn.vove7.scrcpy.common.GestureDescription;
import cn.vove7.scrcpy.common.Point;
import cn.vove7.scrcpy.common.Position;

import static java.lang.Thread.sleep;

public class Controller implements Closeable {

    private static final int DEFAULT_DEVICE_ID = 0;

    private final DeviceOp device;
    private final DesktopConnection connection;
    private final DeviceMessageSender sender;

    private final KeyCharacterMap charMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);

    private long lastTouchDown;
    private final PointersState pointersState = new PointersState();
    private final MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[PointersState.MAX_POINTERS];
    private final MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[PointersState.MAX_POINTERS];


    public Controller(DeviceOp device, DesktopConnection connection) {
        this.device = device;
        this.connection = connection;
        initPointers();
        connection.addCloseable(this);
        sender = new DeviceMessageSender(connection);
    }

    private void initPointers() {
        for (int i = 0; i < PointersState.MAX_POINTERS; ++i) {
            MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
            props.toolType = MotionEvent.TOOL_TYPE_FINGER;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.orientation = 0;
            coords.size = 0;

            pointerProperties[i] = props;
            pointerCoords[i] = coords;
        }
    }

    private void control() {
        // on start, power on the device
        while (connection.isAlive()) {
            try {
                handleEvent();
            } catch (Exception e) {
                send(DeviceMessage.buildErr(e));
                Ln.e("control err: " + e.toString());
                break;
            }
        }
    }

    private void handleEvent() throws InterruptedException {
        ControlMessage msg = connection.receiveControlMessage();
        if (msg == null) return;
        switch (msg.getType()) {
            case ControlMessage.TYPE_INJECT_KEYCODE:
                if (device.supportsInputEvents()) {
                    injectKeycode(msg.getAction(), msg.getKeycode(), msg.getRepeat(), msg.getMetaState());
                }
                break;
            case ControlMessage.TYPE_INJECT_TEXT:
                if (device.supportsInputEvents()) {
                    injectText(msg.getText());
                }
                break;
            case ControlMessage.TYPE_INJECT_TOUCH_EVENT:
                if (device.supportsInputEvents()) {
                    injectTouch(msg.getAction(), msg.getPointerId(), msg.getPosition(), msg.getPressure(), msg.getButtons());
                }
                break;
            case ControlMessage.TYPE_INJECT_SCROLL_EVENT:
                if (device.supportsInputEvents()) {
                    injectScroll(msg.getPosition(), msg.getHScroll(), msg.getVScroll());
                }
                break;
            case ControlMessage.TYPE_BACK_OR_SCREEN_ON:
                if (device.supportsInputEvents()) {
                    pressBackOrTurnScreenOn(msg.getAction());
                }
                break;
            case ControlMessage.TYPE_EXPAND_NOTIFICATION_PANEL:
                Device.expandNotificationPanel();
                break;
            case ControlMessage.TYPE_EXPAND_SETTINGS_PANEL:
                Device.expandSettingsPanel();
                break;
            case ControlMessage.TYPE_COLLAPSE_PANELS:
                Device.collapsePanels();
                break;
            case ControlMessage.TYPE_GET_CLIPBOARD:
                String clipboardText = Device.getClipboardText();
                sender.send(DeviceMessage.createClipboard(clipboardText));
                break;
            case ControlMessage.TYPE_SET_CLIPBOARD:
                Device.setClipboardText(msg.getText());
                break;
            case ControlMessage.TYPE_SET_DISPLAY_ID:
                device.setDisplayId(msg.getAction());
                break;
            case ControlMessage.TYPE_ROTATE_DEVICE:
                Device.rotateDevice();
                break;
            case ControlMessage.TYPE_SET_POWER_SAVE_MODE:
                Device.setPowerSaveModeEnabled(Boolean.parseBoolean(msg.getText()));
                break;
            case ControlMessage.TYPE_SIMPLE_GESTURE:
                playSimpleGesture(msg.getGesturePaths(), msg.getAction());
                break;
            case ControlMessage.TYPE_PERFORM_ACS_ACTION:
                boolean ret = Device.performAcsAction(msg.getAction());
                Ln.d("performAcsAction " + msg.getAction() + " ret: " + ret);
                break;
            default:
                Ln.e("unknown msg type: " + msg.getType() + "\n" + msg.toString());
                // do nothing
        }
    }

    public void send(DeviceMessage msg) {
        sender.send(msg);
    }

    private boolean injectKeycode(int action, int keycode, int repeat, int metaState) {
        return device.injectKeyEvent(action, keycode, repeat, metaState);
    }

    private boolean injectChar(char c) {
        String decomposed = KeyComposition.decompose(c);
        char[] chars = decomposed != null ? decomposed.toCharArray() : new char[]{c};
        KeyEvent[] events = charMap.getEvents(chars);
        if (events == null) {
            return false;
        }
        for (KeyEvent event : events) {
            if (!device.injectEvent(event)) {
                return false;
            }
        }
        return true;
    }

    private int injectText(String text) {
        int successCount = 0;
        for (char c : text.toCharArray()) {
            if (!injectChar(c)) {
                Ln.w("Could not inject char u+" + String.format("%04x", (int) c));
                continue;
            }
            successCount++;
        }
        return successCount;
    }

    private boolean injectTouch(int action, long pointerId, Position position, float pressure, int buttons) {
        long now = SystemClock.uptimeMillis();

        Point point = position.getPoint();
        if (point == null) {
            Ln.w("Ignore touch event, it was generated for a different device size");
            return false;
        }

        int pointerIndex = pointersState.getPointerIndex(pointerId);
        if (pointerIndex == -1) {
            Ln.w("Too many pointers for touch event");
            return false;
        }
        Pointer pointer = pointersState.get(pointerIndex);
        pointer.setPoint(point);
        pointer.setPressure(pressure);
        pointer.setUp(action == MotionEvent.ACTION_UP);

        int pointerCount = pointersState.update(pointerProperties, pointerCoords);

        if (pointerCount == 1) {
            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchDown = now;
            }
        } else {
            // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
            if (action == MotionEvent.ACTION_UP) {
                action = MotionEvent.ACTION_POINTER_UP | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            } else if (action == MotionEvent.ACTION_DOWN) {
                action = MotionEvent.ACTION_POINTER_DOWN | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            }
        }

        // Right-click and middle-click only work if the source is a mouse
        boolean nonPrimaryButtonPressed = (buttons & ~MotionEvent.BUTTON_PRIMARY) != 0;
        int source = nonPrimaryButtonPressed ? InputDevice.SOURCE_MOUSE : InputDevice.SOURCE_TOUCHSCREEN;
        if (source != InputDevice.SOURCE_MOUSE) {
            // Buttons must not be set for touch events
            buttons = 0;
        }

        MotionEvent event = MotionEvent
                .obtain(lastTouchDown, now, action, pointerCount, pointerProperties, pointerCoords, 0, buttons, 1f, 1f, DEFAULT_DEVICE_ID, 0, source,
                        0);
        return device.injectEvent(event);
    }

    private boolean injectScroll(Position position, int hScroll, int vScroll) {
        long now = SystemClock.uptimeMillis();
        Point point = position.getPoint();
        if (point == null) {
            // ignore event
            return false;
        }

        MotionEvent.PointerProperties props = pointerProperties[0];
        props.id = 0;

        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.x = point.getX();
        coords.y = point.getY();
        coords.setAxisValue(MotionEvent.AXIS_HSCROLL, hScroll);
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);

        MotionEvent event = MotionEvent
                .obtain(lastTouchDown, now, MotionEvent.ACTION_SCROLL, 1, pointerProperties, pointerCoords, 0, 0, 1f, 1f, DEFAULT_DEVICE_ID, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);
        return device.injectEvent(event);
    }


    private boolean pressBackOrTurnScreenOn(int action) {
        if (Device.isScreenOn()) {
            return device.injectKeyEvent(action, KeyEvent.KEYCODE_BACK, 0, 0);
        }

        // Screen is off
        // Only press POWER on ACTION_DOWN
        if (action != KeyEvent.ACTION_DOWN) {
            // do nothing,
            return true;
        }
        return device.pressReleaseKeycode(KeyEvent.KEYCODE_POWER);
    }

    private boolean parseText(String text) {

        // On Android >= 7, also press the PASTE key if requested
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && device.supportsInputEvents()) {
            device.pressReleaseKeycode(KeyEvent.KEYCODE_PASTE);
            return true;
        }
        return false;
    }

    private Thread loopThread;

    public Thread startController(String name) {
        if (loopThread != null) {
            throw new IllegalStateException("loopThread != null");
        }
        sender.start();
        loopThread = new Thread(() -> {
            control();
            Ln.d("Controller stopped " + name);
        });
        loopThread.start();
        return loopThread;
    }


    private Thread gestureThread = null;

    public void playSimpleGesture(List<List<Point>> gesturePaths, int duration) {
        GestureDescription desc = buildGesDesc(gesturePaths, duration);
        //async
        if (gestureThread != null) {
            gestureThread.interrupt();
        }
        gestureThread = new Thread(() -> playGesture(desc));
        gestureThread.start();
    }

    private void playGesture(GestureDescription gesDesc) {
        List<GestureDescription.GestureStep> steps = GestureDescription.MotionEventGenerator
                .getGestureStepsFromGestureDescription(gesDesc, 16);
        int i = 0;
        long bb = System.currentTimeMillis();
        for (GestureDescription.GestureStep step : steps) {
            Ln.v("step: " + step);
            long b = System.currentTimeMillis();

            int ti = 0;

            for (GestureDescription.TouchPoint point : step.touchPoints) {

                if (point.mIsStartOfPath) {
                    injectTouch(MotionEvent.ACTION_DOWN, ti,
                            new Position((int) point.mX, (int) point.mY), 1f, 0);
                }
                injectTouch(MotionEvent.ACTION_MOVE, ti,
                        new Position((int) point.mX, (int) point.mY),
                        1f, 0
                );
                if (point.mIsEndOfPath) {
                    injectTouch(MotionEvent.ACTION_UP, ti,
                            new Position((int) point.mX, (int) point.mY),
                            1f, 0);
                }
                ti++;
            }

            long e = System.currentTimeMillis();
            long sendUseTime = e - b;
            if (i + 1 < steps.size()) {//wait
                long wait = steps.get(i + 1).timeSinceGestureStart - step.timeSinceGestureStart - sendUseTime;
                Ln.i("wait: " + wait);
                if (wait > 0) {
                    try {
                        sleep(wait);
                    } catch (InterruptedException interruptedException) {
                        break;
                    }
                }
            }
            i++;
        }
        long useTime = System.currentTimeMillis() - bb;
        Ln.v("playGesture use time: " + (useTime) + "ms");
        send(DeviceMessage.buildGestureEnd(useTime));
    }

    private static GestureDescription buildGesDesc(List<List<Point>> gesturePaths, int duration) {
        GestureDescription.Builder builder = new GestureDescription.Builder();

        for (List<Point> pathPoints : gesturePaths) {
            Path path = new Path();
            int i = 0;
            for (Point p : pathPoints) {
                if (i == 0) path.moveTo(p.getX(), p.getY());
                else path.lineTo(p.getX(), p.getY());
                i++;
            }
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
        }
        return builder.build();
    }


    @Override
    public void close() {
        if (loopThread != null) {
            loopThread.interrupt();
        }
        sender.interrupt();
    }
}
