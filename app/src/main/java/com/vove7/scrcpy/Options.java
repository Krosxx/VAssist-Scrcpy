package com.vove7.scrcpy;


public class Options {
    private Ln.Level logLevel;

    private ConnectType connectType;

    private int socketPort = 9999;

    private String pkg;

    private boolean exitOnClose = false;

    public boolean isExitOnClose() {
        return exitOnClose;
    }

    public void setExitOnClose(boolean exitOnClose) {
        this.exitOnClose = exitOnClose;
    }

    public Ln.Level getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(Ln.Level logLevel) {
        this.logLevel = logLevel;
    }

    public void setConnectType(ConnectType connectType) {
        this.connectType = connectType;
    }

    public ConnectType getConnectType() {
        return connectType;
    }

    public int getSocketPort() {
        return socketPort;
    }

    public void setSocketPort(int socketPort) {
        this.socketPort = socketPort;
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }
}
