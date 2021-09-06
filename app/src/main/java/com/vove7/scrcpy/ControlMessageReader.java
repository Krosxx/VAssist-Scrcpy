package com.vove7.scrcpy;

import java.io.BufferedReader;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import androidx.annotation.NonNull;


public class ControlMessageReader implements Runnable {

    BufferedReader reader;
    Runnable onClose;

    private final BlockingDeque<String> queue = new LinkedBlockingDeque<>();

    public ControlMessageReader(BufferedReader reader, @NonNull Runnable onClose) {
        this.reader = reader;
        this.onClose = onClose;
    }

    Thread readThread;

    public void startRecvMsg() {
        readThread = new Thread(this);
        readThread.start();
    }

    @Override
    public void run() {
        Thread t = Thread.currentThread();
        try {
            while (!t.isInterrupted()) {
                String l = reader.readLine();
                if (l != null) {
                    queue.put(l);
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        onClose.run();
    }

    public String get() throws InterruptedException {
        return queue.take();
    }
}
