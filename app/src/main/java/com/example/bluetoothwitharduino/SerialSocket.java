package com.example.bluetoothwitharduino;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.UUID;

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

    public static final UUID UART_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");

    //  TX, RX 두 채널을 통해 데이터를 주고 받는다.
    //  TX 를 통해 데이터를 보내고 RX 를 통해 데이터를 받는다.
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

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


    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Log.d(TAG, "onConnectionStateChange newState: " + newState + ", status: " + status);
        if( newState == BluetoothProfile.STATE_CONNECTED ) {
            Log.d(TAG, "connect status " + status + ", discoverServices");

            if( checkPermission() )
                if( !gatt.discoverServices() )
                    onSerialConnectError(new IOException("discoverServices failed"));

        } else if( newState == BluetoothProfile.STATE_DISCONNECTED ) {
            if( connected )
                onSerialIOError(new IOException("gatt status " + status));
            else
                onSerialConnectError(new IOException("gatt status " + status));

        } else {
            Log.d(TAG, "unknown connect state " + newState + " " + status);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, "servicesDiscovered, status: " + status);
        if( canceled )
            return;

        if( status == BluetoothGatt.GATT_SUCCESS ) {
            List<BluetoothGattService> services = gatt.getServices();
            for( BluetoothGattService service : services ) {
                if( service.getUuid().equals(UART_SERVICE_UUID) ) {
                    readCharacteristic = service.getCharacteristic(RX_CHAR_UUID);
                    writeCharacteristic = service.getCharacteristic(TX_CHAR_UUID);
                    Log.d(TAG, "readCharacteristic: " + readCharacteristic);
                    Log.d(TAG, "writeCharacteristic: " + writeCharacteristic);
                } else
                    Log.d(TAG, "not equal");
            }
        }

        if( readCharacteristic == null || writeCharacteristic == null ) {
            for( BluetoothGattService service : gatt.getServices() ) {
                Log.d(TAG, "service " + service.getUuid());
                for( BluetoothGattCharacteristic characteristic : service.getCharacteristics() )
                    Log.d(TAG, "characteristic " + characteristic.getUuid());
            }
            onSerialConnectError(new IOException("no serial profile found"));
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if( canceled )
            return;

        if( characteristic == readCharacteristic ) {
            byte[] data = readCharacteristic.getValue();
            onSerialRead(data);
            Log.d(TAG, "read, length = " + data.length);

        } else {
            byte[] data = readCharacteristic.getValue();
            onSerialRead(data);
            Log.d(TAG, "read, length = " + data.length);

        }
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
