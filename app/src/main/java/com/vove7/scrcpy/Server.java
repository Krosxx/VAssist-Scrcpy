package com.vove7.scrcpy;

import android.os.Build;
import android.os.Looper;

import java.io.IOException;
import java.util.Locale;

import cn.vove7.scrcpy.common.DeviceMessage;

public final class Server {

    public static boolean RUNNING = false;

    private Server() {
        // not instantiable
    }

    private static void scrcpy(Options options) throws IOException {
        RUNNING = true;
        Ln.i("Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")" + "\n" +
                "SysContext: " + Workarounds.SysContext + "\n" +
                "App:" + Workarounds.APP + "\n" +
                "pkg: " + options.getPkg());
        final Device device = new Device();

        boolean loop = !options.isExitOnClose() && options.getConnectType().name().contains("Server");

        //noinspection LoopConditionNotUpdatedInsideLoop
        do {
            DesktopConnection connection = DesktopConnection.open(options);
            Ln.i("connected: " + connection);
            final Controller controller = new Controller(device, connection);
            try {
                controller.startController(connection.toString())
                        .join();
            } catch (InterruptedException e) {
                controller.send(DeviceMessage.buildErr(e));
                e.printStackTrace();
            }
            connection.close();
        } while (loop);
    }

    private static Options createOptions(String... args) {

        final int expectedParameters = 2;
        if (args.length < expectedParameters) {
            throw new IllegalArgumentException("Expecting " + expectedParameters + " parameters");
        }

        Options options = new Options();

        Ln.Level level = Ln.Level.valueOf(args[0].toUpperCase(Locale.ENGLISH));
        options.setLogLevel(level);

        ConnectType connectType = ConnectType.valueOf(args[1]);
        options.setConnectType(connectType);

        if (args.length > 2) {
            int port = Integer.parseInt(args[2]);
            options.setSocketPort(port);
        }
        if (args.length > 3) {
            options.setPkg(args[3]);
        }
        if (args.length > 4) {
            boolean eoc = Boolean.parseBoolean(args[4]);
            options.setExitOnClose(eoc);
        }

        return options;
    }

    public static void main(String... args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Ln.e("Exception on thread " + t, e));

        Options options = createOptions(args);

        Ln.initLogLevel(options.getLogLevel());
        Workarounds.prepareMainLooper();
        Workarounds.fillAppInfo(options.getPkg());

        scrcpy(options);
    }
}
