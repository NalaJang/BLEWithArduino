package com.example.bluetoothwitharduino;

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

import androidx.fragment.app.Fragment;

public class TerminalFragment extends Fragment {

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

            bluetoothService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
        }
    };

    private void disconnect() {
        connected = Connected.False;
        bluetoothService.disconnect();
    }
}