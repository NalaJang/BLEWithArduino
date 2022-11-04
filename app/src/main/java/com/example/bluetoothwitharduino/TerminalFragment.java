package com.example.bluetoothwitharduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class TerminalFragment extends Fragment implements SerialListener{

    private final String TAG = "TerminalFragment";
    private enum Connected {False, Pending, True}

    private String deviceAddress;
    private BluetoothService bluetoothService;

    private Connected connected = Connected.False;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( getArguments() != null)
            deviceAddress = getArguments().getString("device");
        else
            deviceAddress = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent gattServiceIntent = new Intent(getActivity(), BluetoothService.class);
        requireActivity().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        // null exception 이 발생
        if( bluetoothService != null)
            requireActivity().runOnUiThread(this::connect);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if( connected != Connected.False )
            disconnect();
        requireActivity().stopService(new Intent(getActivity(), BluetoothService.class));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_terminal, container, false);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            bluetoothService = ((BluetoothService.LocalBinder) service).getService();
            if( !bluetoothService.initialize() ) {
                requireActivity().finish();
            }

//            bluetoothService.connect(deviceAddress);
            connect();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
        }
    };

    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

            SerialSocket socket = new SerialSocket(requireActivity().getApplicationContext(), device);
            bluetoothService.connect(socket);

            Log.d(TAG, "connecting...");
            connected = Connected.Pending;

        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        bluetoothService.disconnect();
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {

    }

    @Override
    public void onSerialConnectError(Exception e) {
        Log.d(TAG, "connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {

    }

    @Override
    public void onSerialIOError(Exception e) {

    }
}