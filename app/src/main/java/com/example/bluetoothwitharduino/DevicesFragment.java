package com.example.bluetoothwitharduino;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;

// ListActivity -> ListFragment : ListActivity was deprecated in API level 30.
public class DevicesFragment extends ListFragment {

    private final String TAG = "DevicesFragment";

    private enum ScanState {NONE, LE_SCAN, DISCOVERY, DISCOVERY_FINISHED}
    private ScanState scanState = ScanState.NONE;

    private Menu menu;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ArrayAdapter<BluetoothDevice> deviceListAdapter;
    private final ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private static final long SCAN_PERIOD = 10000;  // 10초
    private String getDeviceName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Log.d(TAG, "onCreate");

        // BluetoothAdapter 가져오기
        if (requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Bluetooth 지원 기기인지 확인
        if (bluetoothAdapter == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(R.string.ble_not_supported);
            builder.setMessage(R.string.ble_not_supported);
            builder.setPositiveButton(R.string.close_app, ((dialog, which) -> requireActivity().finish()));

        } else {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        deviceListAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, deviceList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
                Log.d(TAG, "getView");

                BluetoothDevice device = deviceList.get(position);

                if( view == null )
                    view = requireActivity().getLayoutInflater().inflate(R.layout.device_list_item, parent, false);

                TextView device_name = view.findViewById(R.id.deviceList_name);
                TextView device_address = view.findViewById(R.id.deviceList_address);

                if( checkPermission() ) {
                    getDeviceName = device.getName();
                    Log.d(TAG, "getDeviceName = " + getDeviceName);
                }

                if( getDeviceName == null || getDeviceName.isEmpty() )
                    device_name.setText(R.string.unknown_device);
                else
                    device_name.setText(getDeviceName);

                device_address.setText(device.getAddress());

                return view;
            }
        };
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        setListAdapter(null);
        View header = requireActivity().getLayoutInflater().inflate(R.layout.device_list_header, null, false);
        getListView().addHeaderView(header, null, false);
        // list 가 비어있을 때 보여 줄 텍스트
        setEmptyText("initializing...");
        ((TextView) getListView().getEmptyView()).setTextSize(18);
        setListAdapter(deviceListAdapter);
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "onCreateOptionsMenu");
        this.menu = menu;
        inflater.inflate(R.menu.menu_device_scan, menu);

        if( bluetoothAdapter == null ) {
            menu.findItem(R.id.ble_start_scan).setEnabled(false);
            menu.findItem(R.id.ble_setting).setEnabled(false);

        } else if( !bluetoothAdapter.isEnabled() )
            menu.findItem(R.id.ble_start_scan).setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if( id == R.id.ble_start_scan ) {
            startScan();
            return true;

        } else if( id == R.id.ble_stop_scan ) {
            stopScan();
            return true;

        } else if( id == R.id.ble_setting ) {
            // 블루투스 설정창으로 이동
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            return true;

        } else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        // bluetooth adapter 확인
        if( bluetoothAdapter == null ) {
            setEmptyText("<bluetooth LE not supported>");

        }
        // bluetooth 활성화 확인
        else if( !bluetoothAdapter.isEnabled() ) {
            setEmptyText("<bluetooth is disabled>");

            if( menu != null ) {
                deviceList.clear();
                deviceListAdapter.notifyDataSetChanged();
                menu.findItem(R.id.ble_start_scan).setEnabled(false);
            }

            // bluetooth 활성화 1회 요구
            if( !result_canceled ) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                requestEnableResult.launch(enableIntent);
            }

        }
        // bluetooth 사용 가능
        else {
            setEmptyText("<use SCAN to refresh devices>");
            if( menu != null )
                menu.findItem(R.id.ble_start_scan).setEnabled(true);
        }
    }

    // 스캔을 시작하면 권한 요청
    public boolean checkPermission() {
        Log.d(TAG, "checkPermission");
//        if( scanState != ScanState.NONE )
//            return false;
//
//        scanState = ScanState.LE_SCAN;

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if( ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ) {

                scanState = ScanState.NONE;

                AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                builder.setTitle(R.string.location_permission_title);
                builder.setMessage(R.string.location_permission_message);
                builder.setNegativeButton(android.R.string.cancel, ((dialog, which) -> {
                    stopScan();
                }));

                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
//                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                    scanState = ScanState.LE_SCAN;
                    requestPermissionResult.launch(new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    });
                });
                builder.show();
            }

            boolean locationEnabled = false;
            LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

            try {
                locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ignored) {}

            try {
                locationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ignored) {}

            if( !locationEnabled)
                scanState = ScanState.DISCOVERY;

            return true;

        } else {
            Log.d(TAG, "낮은 버전");
            return false;
        }
    }

    private boolean result_canceled;
    private final ActivityResultLauncher<Intent> requestEnableResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->  {
                if( result.getResultCode() == Activity.RESULT_CANCELED ) {
                    Toast.makeText(getActivity(), R.string.bluetooth_request_canceled, Toast.LENGTH_SHORT).show();
                    result_canceled = true;
                }
            }
    );


    private final ActivityResultLauncher<String[]> requestPermissionResult = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocationEnabled = result.get(Manifest.permission.ACCESS_FINE_LOCATION);

                if( fineLocationEnabled != null && fineLocationEnabled ) {
                    Log.d(TAG, "location 승인");
                }

            }
    );


    private void startScan() {
        Log.d(TAG, "startScan");
        if( scanState != ScanState.NONE )
            return;

        scanState = ScanState.LE_SCAN;

        if( checkPermission() ) {

            deviceList.clear();
            deviceListAdapter.notifyDataSetChanged();

            setEmptyText("<scanning...>");
            menu.findItem(R.id.ble_start_scan).setVisible(false);
            menu.findItem(R.id.ble_stop_scan).setVisible(true);

            // scan 중
            if( scanState == ScanState.LE_SCAN ) {

                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    if( checkPermission() )
                        bluetoothLeScanner.stopScan(scanCallback);
                Log.d(TAG, "postDelayed");
                }, SCAN_PERIOD);

                Log.d(TAG, "postDelayed22");
                bluetoothLeScanner.startScan(scanCallback);

            } else {
                Log.d(TAG, "startDiscovery");
                bluetoothAdapter.startDiscovery();
            }
        }
    }

    private void stopScan() {
        // 이미 스캔 중이 아님
        if( scanState == ScanState.NONE )
            return;

        setEmptyText("<no bluetooth devices found>");

        if( menu != null ) {
            menu.findItem(R.id.ble_start_scan).setVisible(true);
            menu.findItem(R.id.ble_stop_scan).setVisible(false);
        }

        switch (scanState) {
            case LE_SCAN:
                if( checkPermission() )
                    bluetoothLeScanner.stopScan(scanCallback);
                break;

            case DISCOVERY:
                bluetoothAdapter.cancelDiscovery();
                break;

            default:
                // already canceled
        }

        scanState = ScanState.NONE;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult -> " + result);
            processResult(result);
        }
    };

    private void processResult(final ScanResult result) {
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "processResult: " + result.getDevice());
            // listView 에 주변 device 를 추가
            deviceListAdapter.add(result.getDevice());
            deviceListAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Log.d(TAG, "onListItemClick");

        stopScan();

        BluetoothDevice device = deviceList.get(position);
        Bundle bundle = new Bundle();
        bundle.putString("device", device.getAddress());

        Fragment fragment = new TerminalFragment();
        fragment.setArguments(bundle);
        requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).addToBackStack(null).commit();
    }
}