package com.vove7.scrcpy;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.vove7.scrcpy.common.ControlMessage;

public final class DesktopConnection implements Closeable {

    private static final String SOCKET_NAME = "scrcpy";

    private final List<Closeable> closeables = new ArrayList<>();
    private final BufferedWriter controlOutputStream;
    private final Gson gson = new Gson();
    private final ControlMessageReader reader;

    private boolean isAlive = true;

    private DesktopConnection(InputStream inputStream, OutputStream outputStream, Closeable... closeables) throws IOException {
        this.closeables.addAll(Arrays.asList(closeables));
        BufferedReader controlInputStream = new BufferedReader(new InputStreamReader(inputStream));
        reader = new ControlMessageReader(controlInputStream, this::close);
        controlOutputStream = new BufferedWriter(new OutputStreamWriter(outputStream));
        reader.startRecvMsg();
    }

    public BufferedWriter getControlOutputStream() {
        return controlOutputStream;
    }

    private static LocalSocket connect(String abstractName) throws IOException {
        LocalSocket localSocket = new LocalSocket();
        localSocket.connect(new LocalSocketAddress(abstractName));
        return localSocket;
    }

    public static DesktopConnection open(Options options) throws IOException {
        LocalSocket controlSocket;
        ConnectType connectType = options.getConnectType();
        Ln.i("open: " + connectType);
        if (connectType == ConnectType.LocalSocketServer) {
            Ln.i("localServerSocket: " + SOCKET_NAME);
            try (LocalServerSocket localServerSocket = new LocalServerSocket(SOCKET_NAME)) {
                try {
                    controlSocket = localServerSocket.accept();
                    return new DesktopConnection(controlSocket.getInputStream(), controlSocket.getOutputStream(),
                            controlSocket, localServerSocket);
                } catch (IOException | RuntimeException e) {
                    throw e;
                }
            }
        } else if (connectType == ConnectType.LocalSocketClient) {
            try {
                controlSocket = connect(SOCKET_NAME);
                return new DesktopConnection(controlSocket.getInputStream(), controlSocket.getOutputStream(), controlSocket);
            } catch (IOException | RuntimeException e) {
                throw e;
            }
        } else if (connectType == ConnectType.SocketServer) {
            Ln.i("listen sock on port: " + options.getSocketPort());
            ServerSocket ssock = new ServerSocket(options.getSocketPort(), 0, Inet4Address.getByName("localhost"));
            Ln.i("waiting client connect...");
            Socket sock = ssock.accept();
            Ln.i("client connect: " + sock.toString());
            return new DesktopConnection(sock.getInputStream(), sock.getOutputStream(), sock, ssock);
        } else if (connectType == ConnectType.SocketClient) {
            Socket sock = new Socket();
            Ln.i("socket connect to localhost:" + options.getSocketPort());
            sock.connect(new InetSocketAddress("localhost", options.getSocketPort()));
            return new DesktopConnection(sock.getInputStream(), sock.getOutputStream(), sock);
        }
        throw new RuntimeException("unsupported con type: " + connectType);
    }

    public void close() {
        Ln.i("con closed: " + toString());
        isAlive = false;

        for (Closeable c : closeables) {
            try {
                if (c instanceof LocalSocket) {
                    ((LocalSocket) c).shutdownInput();
                    ((LocalSocket) c).shutdownOutput();
                }
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ControlMessage receiveControlMessage() throws InterruptedException {
        try {
            String msg = reader.get();
            Ln.i("on recv msg: " + msg);
            return gson.fromJson(msg, ControlMessage.class);
        } catch (Throwable e) {
            throw new InterruptedException();
        }
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void addCloseable(Closeable closeable) {
        closeables.add(closeable);
    }
}
