package cn.vove7.scrcpy.common;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Union of all supported event types, identified by their {@code type}.
 */
public final class ControlMessage {

    public static final int TYPE_INJECT_KEYCODE = 0;
    public static final int TYPE_INJECT_TEXT = 1;
    public static final int TYPE_INJECT_TOUCH_EVENT = 2;
    public static final int TYPE_INJECT_SCROLL_EVENT = 3;
    public static final int TYPE_BACK_OR_SCREEN_ON = 4;
    public static final int TYPE_EXPAND_NOTIFICATION_PANEL = 5;
    public static final int TYPE_EXPAND_SETTINGS_PANEL = 6;
    public static final int TYPE_COLLAPSE_PANELS = 7;
    public static final int TYPE_GET_CLIPBOARD = 8;
    public static final int TYPE_SET_CLIPBOARD = 9;
    public static final int TYPE_SET_DISPLAY_ID = 10;
    public static final int TYPE_ROTATE_DEVICE = 11;
    public static final int TYPE_SET_POWER_SAVE_MODE = 12;
    public static final int TYPE_SIMPLE_GESTURE = 13;
    public static final int TYPE_PERFORM_ACS_ACTION = 14;
    public static final int TYPE_EXIT = 15;

    private int type;
    private String text;
    private int metaState; // KeyEvent.META_*
    private int action; // KeyEvent.ACTION_* or MotionEvent.ACTION_* or POWER_MODE_*
    private int keycode; // KeyEvent.KEYCODE_*
    private int buttons; // MotionEvent.BUTTON_*
    private long pointerId;
    private float pressure;
    private Position position;
    private int hScroll;
    private int vScroll;
    private boolean paste;
    private int repeat;
    private List<List<Point>> gesturePaths;

    private ControlMessage() {
    }

    public static ControlMessage createInjectKeycode(int action, int keycode, int repeat, int metaState) {
        ControlMessage msg = new ControlMessage();
        msg.type = TYPE_INJECT_KEYCODE;
        msg.action = action;
        msg.keycode = keycode;
        msg.repeat = repeat;
        msg.metaState = metaState;
        return msg;
    }

    public static ControlMessage createInjectText(String text) {
        ControlMessage msg = new ControlMessage();
        msg.type = TYPE_INJECT_TEXT;
        msg.text = text;
        return msg;
    }

    public static ControlMessage createSimpleGesture(List<List<Point>> gesturePaths, int duration) {
        ControlMessage msg = new ControlMessage();
        msg.type = TYPE_SIMPLE_GESTURE;
        msg.gesturePaths = gesturePaths;
        msg.action = duration;
        return msg;
    }

    public static ControlMessage createInjectTouchEvent(int action, long pointerId, Position position, float pressure, int buttons) {
        ControlMessage msg = new ControlMessage();
        msg.type = TYPE_INJECT_TOUCH_EVENT;
        msg.action = action;
        msg.pointerId = pointerId;
        msg.pressure = pressure;
        msg.position = position;
        msg.buttons = buttons;
        return msg;
    }

    public static ControlMessage exit() {
        ControlMessage msg = new ControlMessage();
        msg.type = TYPE_EXIT;
        return msg;
    }

    public static ControlMessage createInjectScrollEvent(Position position, int hScroll, int vScroll) {
        ControlMessage msg = new ControlMessage();
        msg.type = TYPE_INJECT_SCROLL_EVENT;
        msg.position = position;
        msg.hScroll = hScroll;
        msg.vScroll = vScroll;
        return msg;
    }

    public static ControlMessage createBackOrScreenOn(int action) {
        ControlMessage msg = new ControlMessage();
        msg.type = TYPE_BACK_OR_SCREEN_ON;
        msg.action = action;
        return msg;
    }

    public static ControlMessage createDisplayId(int action) {
        ControlMessage msg = new ControlMessage();
        msg.type = TYPE_SET_DISPLAY_ID;
        msg.action = action;
        return msg;
    }

    public static ControlMessage createSetClipboard(String text) {
        ControlMessage msg = new ControlMessage();
        msg.type = TYPE_SET_CLIPBOARD;
        msg.text = text;
        return msg;
    }

    public static ControlMessage createPowerSaveModeEnable(boolean enabled) {
        ControlMessage msg = new ControlMessage();
        msg.type = TYPE_SET_POWER_SAVE_MODE;
        msg.text = String.valueOf(enabled);
        return msg;
    }
    
    public static ControlMessage performAcsAction(int action) {
        ControlMessage msg = new ControlMessage();
        msg.type = TYPE_PERFORM_ACS_ACTION;
        msg.action = action;
        return msg;
    }

    public static ControlMessage createEmpty(int type) {
        ControlMessage msg = new ControlMessage();
        msg.type = type;
        return msg;
    }

    public int getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public int getMetaState() {
        return metaState;
    }

    public int getAction() {
        return action;
    }

    public List<List<Point>> getGesturePaths() {
        return gesturePaths;
    }

    public int getKeycode() {
        return keycode;
    }

    public int getButtons() {
        return buttons;
    }

    public long getPointerId() {
        return pointerId;
    }

    public float getPressure() {
        return pressure;
    }

    public Position getPosition() {
        return position;
    }

    public int getHScroll() {
        return hScroll;
    }

    public int getVScroll() {
        return vScroll;
    }

    public boolean getPaste() {
        return paste;
    }

    public int getRepeat() {
        return repeat;
    }

    @Override
    public String toString() {
        return "ControlMessage{" +
                "type=" + type +
                ", text='" + text + '\'' +
                ", metaState=" + metaState +
                ", action=" + action +
                ", keycode=" + keycode +
                ", buttons=" + buttons +
                ", pointerId=" + pointerId +
                ", pressure=" + pressure +
                ", position=" + position +
                ", hScroll=" + hScroll +
                ", vScroll=" + vScroll +
                ", paste=" + paste +
                ", repeat=" + repeat +
                '}';
    }

    public void post(BufferedWriter out) throws IOException {
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .create();
        String cd = gson.toJson(this);
        out.write(cd + "\n");
        out.flush();
    }

}
