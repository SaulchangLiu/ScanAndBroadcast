package com.example.administrator.scanandbroadcast;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.ParcelUuid;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    long time = Long.MAX_VALUE;
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    AdvertiseData mAdvertiseData;
    AdvertiseData.Builder mAdvertiseDataBuilder;
    ParcelUuid mServiceDataUUID;
    AdvertiseSettings mAdvertiseSettings;
    AdvertiseSettings.Builder mAdvertiseSettingBuilder = new AdvertiseSettings.Builder();
    Button BroadcastButton;
    Button StopBroadcastButton;
    ParcelUuid mServiceUUID;

    BluetoothLeScanner btScanner;
    Button startScanningButton;
    Button stopScanningButton;
    TextView peripheralTextView;
    ScanFilter mScanFilter;
    ScanFilter.Builder mScanFilterBuilder = new ScanFilter.Builder();
    ScanSettings mScanSettings;
    ScanSettings.Builder mScanSettingBuilder = new ScanSettings.Builder();
    List<ScanFilter> FilterList = new ArrayList<>();
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /** Broadcast activity */
        BLESetUpAdvertiser();
        PrepareSettings();
        PrepareData("LOMO");
        BroadcastButton = (Button) findViewById(R.id.BroadcastButton);
        BroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAdvertise();
            }
        });

        /** Scanning activity */
        btScanner = mBluetoothAdapter.getBluetoothLeScanner();
        PrepareScanFilter();
        BLESetUpScanner();
        PrepareButtons();
        PrepareScanFilter();
        PrepareScanSetting();

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }

        private ScanCallback leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                String name=result.getDevice().getName();
                time = Math.min(time, result.getTimestampNanos());
                mServiceDataUUID = ParcelUuid.fromString("00009208-0000-1000-8000-00805F9B34FB");
                String data= new String(result.getScanRecord().getServiceData(mServiceDataUUID));
                peripheralTextView.setText(
                        "Device Name = " + name +
                                "\nrssi = " + result.getRssi() +
                                "\nAddress = " + result.getDevice().getAddress() +
                                "\nTime Stamp = " + result.getTimestampNanos() +
                                "\nTime Elapsed  = "+ (result.getTimestampNanos()-time)/1000000000 +
                                "\nServiceID = " + result.getScanRecord().getServiceUuids().toString().substring(5,9) +
                                "\nIs playing game = " + (result.getScanRecord().getServiceUuids().toString().substring(5,9).equals("1830") ? "Yes": "No") +
                                "\nService Data = " + data );
            }
        };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }




        /** Button control */
        StopBroadcastButton = (Button) findViewById(R.id.StopBraodcastButton);
        StopBroadcastButton.setVisibility(View.INVISIBLE);
        StopBroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAdvertise();
                }
            });
        }


    /** Scanner callback */
    private AdvertiseCallback mCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

        }
    };


    /** Broadcast functions*/
    public void PrepareSettings(){
        mAdvertiseSettingBuilder.setAdvertiseMode(1);
        mAdvertiseSettingBuilder.setTimeout(0);
        mAdvertiseSettingBuilder.setTxPowerLevel(2);
        mAdvertiseSettingBuilder.setConnectable(true);
        mAdvertiseSettings = mAdvertiseSettingBuilder.build();
    }
    public void BLESetUpAdvertiser(){
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    }
    public void PrepareData(String str){
        mAdvertiseDataBuilder = new AdvertiseData.Builder();
        mServiceDataUUID = ParcelUuid.fromString("00009208-0000-1000-8000-00805F9B34FB");
        mAdvertiseDataBuilder.addServiceData(mServiceDataUUID,str.getBytes());
        mServiceUUID = ParcelUuid.fromString("00001830-0000-1000-8000-00805F9B34FB");
        mAdvertiseDataBuilder.setIncludeDeviceName(true);
        mAdvertiseDataBuilder.setIncludeTxPowerLevel(true);
        mAdvertiseDataBuilder.addServiceUuid(mServiceUUID);
        mAdvertiseData= mAdvertiseDataBuilder.build();
    }
    public void startAdvertise(){
        mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings,mAdvertiseData,mCallback);
        BroadcastButton.setVisibility(View.INVISIBLE);
        StopBroadcastButton.setVisibility(View.VISIBLE);
    }
    public void stopAdvertise(){
        mBluetoothLeAdvertiser.stopAdvertising(mCallback);
        BroadcastButton.setVisibility(View.VISIBLE);
        StopBroadcastButton.setVisibility(View.INVISIBLE);
    }

    /** Scanning functions*/
    public void PrepareScanFilter(){
        mServiceUUID = ParcelUuid.fromString("00001830-0000-1000-8000-00805F9B34FB");
        mScanFilterBuilder.setServiceUuid(mServiceUUID);
        mScanFilter = mScanFilterBuilder.build();
        FilterList.add(mScanFilter);
    }

    public void BLESetUpScanner(){
        mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        btScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }
    public void PrepareButtons() {
        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());

        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);
    }
    public void PrepareScanSetting(){
        mScanSettingBuilder.setScanMode(1);
        mScanSettings = mScanSettingBuilder.build();
    }

    public void startScanning() {
        System.out.println("start scanning");
        peripheralTextView.setText("");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(FilterList, mScanSettings, leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        peripheralTextView.append("Stopped Scanning\n");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

}
