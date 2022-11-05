package com.example.bluetoothwitharduino;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

public class BluetoothService extends Service {
    private final String TAG = "BluetoothService";
    private boolean connected;
    private BluetoothGatt bluetoothGatt;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private SerialSocket socket;


    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder binder = new LocalBinder();

    public boolean initialize() {
        if( bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (bluetoothManager == null)
                return false;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
//        if( bluetoothAdapter == null)
//            return false;

        return true;
    }

    public boolean connect(final String address) {
        if( bluetoothAdapter == null || address == null)
            return false;

        return true;
    }

    // TerminalFragment 에서 사용
    public void connect(SerialSocket socket) throws IOException {
        this.socket = socket;
        socket.connect();
        connected = true;
    }


    public void disconnect() {
        connected = false;

        if( checkPermission() )
            bluetoothGatt.disconnect();
    }

    public void close() {
        if (bluetoothGatt == null)
            return;

        if( checkPermission() ) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    private boolean checkPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false;
        } else
            return true;
    }
}
