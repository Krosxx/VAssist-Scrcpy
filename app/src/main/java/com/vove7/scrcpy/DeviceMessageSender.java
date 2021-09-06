package com.vove7.scrcpy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import cn.vove7.scrcpy.common.DeviceMessage;

public final class DeviceMessageSender extends Thread {

    private final DesktopConnection connection;

    private final BlockingQueue<DeviceMessage> queue = new LinkedBlockingDeque<>();
    private final DeviceMessageWriter writer = new DeviceMessageWriter();

    public DeviceMessageSender(DesktopConnection connection) {
        this.connection = connection;
    }

    public synchronized void send(DeviceMessage msg) {
        Ln.i("send device msg: " + msg);
        queue.add(msg);
    }

    public void run() {
        try {
            while (!isInterrupted() && connection.isAlive()) {
                DeviceMessage event = queue.take();
                Ln.i("push device msg: " + event);
                if (event != null && connection.isAlive()) {
                    writer.writeTo(event, connection.getControlOutputStream());
                } else {
                    break;
                }
            }
        } catch (Throwable ignored) {
        }
        Ln.i("DeviceMessageSender stopped..");
    }
}
