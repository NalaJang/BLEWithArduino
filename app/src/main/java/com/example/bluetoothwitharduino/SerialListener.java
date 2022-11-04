package com.example.bluetoothwitharduino;

public interface SerialListener {
    void onSerialConnect        ();
    void onSerialConnectError   (Exception e);
    void onSerialRead           (byte[] data);
    void onSerialIOError        (Exception e);
}
