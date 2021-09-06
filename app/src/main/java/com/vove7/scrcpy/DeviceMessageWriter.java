package com.vove7.scrcpy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.IOException;

import cn.vove7.scrcpy.common.DeviceMessage;

public class DeviceMessageWriter {

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    public void writeTo(DeviceMessage msg, BufferedWriter output) throws IOException {
        output.write(gson.toJson(msg) + "\n");
        output.flush();
    }
}
