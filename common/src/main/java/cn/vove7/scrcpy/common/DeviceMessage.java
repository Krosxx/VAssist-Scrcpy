package cn.vove7.scrcpy.common;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class DeviceMessage {

    public static final int TYPE_CLIPBOARD = 0;
    public static final int TYPE_ERROR = 486;
    public static final int TYPE_GESTURE_FINISHED = 455;

    private int type;
    private String text;

    private DeviceMessage() {
    }

    public static DeviceMessage createClipboard(String text) {
        DeviceMessage event = new DeviceMessage();
        event.type = TYPE_CLIPBOARD;
        event.text = text;
        return event;
    }

    public static DeviceMessage buildErr(Throwable e) {
        DeviceMessage event = new DeviceMessage();
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        event.type = TYPE_ERROR;
        event.text = sw.toString();
        return event;
    }

    public static DeviceMessage buildGestureEnd(long useTime) {
        DeviceMessage event = new DeviceMessage();
        event.type = TYPE_GESTURE_FINISHED;
        event.text = useTime + "ms";
        return event;
    }

    public int getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "DeviceMessage{" +
                "type=" + type +
                ", text='" + text + '\'' +
                '}';
    }

}
