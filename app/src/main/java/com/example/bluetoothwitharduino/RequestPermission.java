package com.example.bluetoothwitharduino;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class RequestPermission {

    private Fragment fragment;
    private final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
    };

    RequestPermission(Fragment fragment) {
        this.fragment = fragment;
    }

    // 권한 요청 프로세스
    public void requestPermission() {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {

            if( ContextCompat.checkSelfPermission(
                    fragment.requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    fragment.requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    fragment.requireActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
            ) {

                performAction();

            }
            // 이전에 권한 요청을 거부했었을 경우, 다시 권한의 필요성을 안내
            else if( fragment.shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                performAction();
            }
            else {
                requestPermissionResult.launch(PERMISSIONS);
            }
        }
    }

    // 권한 요청 다이얼로그
    private void performAction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.requireActivity());
        builder.setTitle(R.string.location_permission_title);
        builder.setMessage(R.string.location_permission_message);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {

            requestPermissionResult.launch(PERMISSIONS);

        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
        });

        builder.show();
    }

    // 권한 요청에 대한 응답 처리
    private final ActivityResultLauncher<String[]> requestPermissionResult = fragment.registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {

                Boolean fineLocationEnabled = result.get(Manifest.permission.ACCESS_FINE_LOCATION);

                if( fineLocationEnabled != null && fineLocationEnabled ) {
                    System.out.println("위치 접근 권한 승인");
                }
            }
    );

}
