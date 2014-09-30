package nrf518pp.test_ble_advdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class BLEAdvDataActivity extends ListActivity {
    ArrayList<String> mListItems=new ArrayList<String>();
    ArrayAdapter<String> adapter;

    private static final String TAG=BLEAdvDataActivity.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private HashMap<String,Integer> mBleDevices = new HashMap<String,Integer>();
    private int mScanCount;
    private static final int REQUEST_ENABLE_BT=123456;

    public static class AdRecord {
        int mLength;
        int mType;
        byte[] mData;
        public enum RecordTypes
        {
            //RT_DEVNAME,
            //RT_UUID ,
            //RT_ADVDATA =
            //RT_APPEARANCE = 0x19, // len 3
            //RT_SERVICEDATA = 0x16
        }
        public AdRecord(int length, int type, byte[] data) {
            mLength = length;
            mType = type;
            mData = data;
        }

        public String toString() {
            return "[len: " + mLength + ", type: " + mType + ", data: " + Arrays.toString(mData) + "] ";
        }

        public static List<AdRecord> parseScanRecord(byte[] scanRecord) {
            List<AdRecord> records = new ArrayList<AdRecord>();

            int index = 0;
            while (index < scanRecord.length) {
                int length = scanRecord[index++];
                //Done once we run out of records
                if (length == 0) break;

                int type = scanRecord[index];
                //Done if our record isn't a valid type
                if (type == 0) break;

                byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);

                records.add(new AdRecord(length, type, data));
                //Advance
                index += length;
            }

            return records;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleadv_data);
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, mListItems);
        setListAdapter(adapter);

        checkBLE();
        init();
        boolean ret = enableBLE();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bleadv_data, menu);
        return true;
    }

    private void init(){
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }
    private void startScan(boolean success){
        if(mBluetoothAdapter == null){
            init();
        }
        if(success){
            mScanning=true;
            scanLeDevice(mScanning);
            return;
        }
        if(enableBLE()){
            mScanning=true;
            scanLeDevice(mScanning);
        }else{
            Log.d(TAG,getCtx()+" startScan Waiting for on onActivityResult success:"+success);
        }
    }
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Log.d(TAG,getCtx()+" scanLeDevice startLeScan:"+enable);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            Log.d(TAG,getCtx()+ " scanLeDevice stopLeScan:"+enable);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }
    private static String getCtx(){
        Date dt = new Date();
        return dt+ " thread:"+Thread.currentThread().getName();
    }
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi,
                                     final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mScanning) {
                                mScanCount++;
                                List<AdRecord> records = AdRecord.parseScanRecord(scanRecord);
                                String rdata = new String();
                                for (int i = 0; i < records.size(); i++) {
                                    rdata += records.get(i).toString() + "\n";
                                }
                                updateItems(device.getName(), rdata);
                            }
                        }
                    });
                }
            };
    private void updateItems(String deviceName, String msg) {
        synchronized(mListItems) {
            String record = deviceName + " (scan#" + mScanCount + ")\n" + msg;
            if(mBleDevices.containsKey(deviceName)) {
                mListItems.set(mBleDevices.get(deviceName), record);
            } else {
                Integer id = mListItems.size();
                mBleDevices.put(deviceName, id);
                mListItems.add(record);
            }
            adapter.notifyDataSetChanged();
        }
    }
    public void startScan(View v) {
        if(mBluetoothAdapter == null){
            init();
        }
        if(enableBLE()){
            mScanning = true;
            mScanCount = 0;
            scanLeDevice(mScanning);
            mListItems.clear();
            mBleDevices.clear();
            adapter.notifyDataSetChanged();
        }
    }
    public void stopScan(View v) {
        mScanning=false;
        scanLeDevice(mScanning);
    }

    private  void checkBLE(){
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private boolean enableBLE(){
        boolean ret=true;
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.d(TAG,getCtx()+" enableBLE either mBluetoothAdapter == null or disabled:"+mBluetoothAdapter);
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
            ret=false;
        }
        return ret;
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,getCtx()+" onActivityResult requestCode="+requestCode+
                        ", resultCode="+resultCode+", Intent:"+data
        );
        if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
            startScan(true);
        }
    }
}