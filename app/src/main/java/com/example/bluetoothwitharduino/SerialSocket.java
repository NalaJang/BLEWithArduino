package com.example.bluetoothwitharduino;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.security.InvalidParameterException;

public class SerialSocket extends BluetoothGattCallback {
    private final String TAG = "SerialSocket";
    private final Context context;
    private SerialSocket socket;
    private SerialListener listener;
    private BluetoothDevice device;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic readCharacteristic, writeCharacteristic;

    private boolean connected;
    private boolean canceled;
    private int connectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    SerialSocket(Context context, BluetoothDevice device) {
        if (context instanceof Activity)
            throw new InvalidParameterException("expected non UI context");

        this.context = context;
        this.device = device;
    }

    // BluetoothService 에서 사용
    void connect() throws IOException {
        if (connected || gatt != null)
            throw new IOException("already connected");

        canceled = false;

        if( device == null )
            Log.d(TAG, "Device not found. Unable to connect.");

        if( checkPermission() ) {
            gatt = device.connectGatt(context, false, this);
            connectionState = STATE_CONNECTED;

        } else
            Log.d(TAG, "permission denied");

        if( gatt == null )
            throw new IOException("connectGatt failed");
    }



    /**
     * SerialListener
     */
    private void onSerialConnect() {
        if( listener != null )
            listener.onSerialConnect();
    }

    private void onSerialConnectError(Exception e) {
        canceled = true;
        if( listener != null )
            listener.onSerialConnectError(e);
    }

    private void onSerialRead(byte[] data) {
        if( listener != null )
            listener.onSerialRead(data);
    }

    private void onSerialIOError(Exception e) {
        canceled = true;
        if( listener != null )
            listener.onSerialIOError(e);
    }

    private boolean checkPermission() {

        if( ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ) {

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.location_permission_title);
            builder.setMessage(R.string.location_permission_message);

            builder.show();
            return false;
        } else
            return true;
    }
}
