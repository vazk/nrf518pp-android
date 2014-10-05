package nrf518pp.test_ble_led_control;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;

import java.util.UUID;


public class LedControlActivity extends Activity {

    static BluetoothGattCharacteristic mCharacteristic;
    static BluetoothGatt mGatt;

    public static BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //Connection established
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                //Handle a disconnect event
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mGatt = gatt;
            //Now we can start reading/writing characteristics
            BluetoothGattService service = gatt.getService(UUID.fromString("00001523-1212-efde-1523-785feabcd123"));
            mCharacteristic = service.getCharacteristic(UUID.fromString("00001524-1212-efde-1523-785feabcd123"));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCharacteristic = null;
        mGatt = null;

        setContentView(R.layout.activity_led_control);

        final ToggleButton button = (ToggleButton) findViewById(R.id.LedToggleButton);

        button.setOnClickListener(
                new ToggleButton.OnClickListener() {
                    public void onClick(View v) {
                        mCharacteristic.setValue(new byte[]{(byte) (button.isChecked() ? 0x1 : 0x0)});
                        //Execute the write
                        mGatt.writeCharacteristic(mCharacteristic);
                    }
                }
        );
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
