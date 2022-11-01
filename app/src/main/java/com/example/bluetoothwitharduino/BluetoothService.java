package com.example.bluetoothwitharduino;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class BluetoothService extends Service {
    private final String TAG = "BluetoothService";
    private boolean connected;
    private BluetoothGatt bluetoothGatt;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private DevicesFragment devicesFragment = new DevicesFragment();

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
        if( bluetoothAdapter == null)
            return false;

        return true;
    }

    public boolean connect(final String address) {
        if( bluetoothAdapter == null || address == null)
            return false;

        return true;
    }


    public void disconnect() {
        connected = false;

        if( devicesFragment.checkPermission() )
            bluetoothGatt.disconnect();
    }

    public void close() {
        if (bluetoothGatt == null)
            return;

        if( devicesFragment.checkPermission() ) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}
