package com.example.bluetoothwitharduino;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        if( savedInstanceState == null )
            getSupportFragmentManager().beginTransaction().add(R.id.container, new DevicesFragment()).commit();
        else
            onBackStackChanged();
    }


    //  전면에 나와있는 프래그먼트
    //  최초 add 한 것은 backStack 을 가지지 않으므로 카운트 = 0
    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount()>0);
    }
}