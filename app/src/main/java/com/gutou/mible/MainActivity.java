package com.gutou.mible;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    //private static final String MAC_ADDR = "0C:95:41:DB:AE:42";
    private static final UUID SCALE_SERVICE = UUID.fromString("0000181b-0000-1000-8000-00805f9b34fb");
    private static final UUID SCALE_DATA = UUID.fromString("00002a9c-0000-1000-8000-00805f9b34fb");
    private static final UUID DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final String TARGET_DEVICE = "MIBFS";



    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    //private List<BluetoothGattService> mServiceList;
    private ScanCallback mScanCallback;
    private BluetoothGatt mBluetoothGatt;
    private ArrayAdapter<String> listAdapter;

    private Button start;
    private ListView mList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = findViewById(R.id.start);
        mList = findViewById(R.id.devices);

        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mList.setAdapter(listAdapter);

        //获取蓝牙设备
        mScanCallback = new LeScanCallback();

        initBLE();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanLeDevices();
            }
        });
    }

    private void initBLE() {
        //get Bluetooth service
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //get Bluetooth Adapter
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {//platform not support bluetooth
            System.out.println("BLE not supported");
        } else {
            int status = mBluetoothAdapter.getState();
            //bluetooth is disabled
            if (status == BluetoothAdapter.STATE_OFF) {
                // enable bluetooth
                mBluetoothAdapter.enable();
            }
        }
    }

    private void startScanLeDevices() {
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(mScanCallback);
    }

    private class LeScanCallback extends ScanCallback {
        /**
         * 扫描结果的回调，每次扫描到一个设备，就调用一次。
         *
         * @param callbackType
         * @param result
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //Log.d(Tag, "onScanResult");
            if (result != null) {
                System.out.println("扫面到设备：" + result.getDevice().getName() + "  " + result.getDevice().getAddress());

                if (result.getDevice().getName() != null && TARGET_DEVICE.equals(result.getDevice().getName())) {
                    //扫描到我们想要的设备后，立即停止扫描
                    start.setText("Found");
                    start.setEnabled(false);
                    BluetoothDevice device = result.getDevice();
                    mBluetoothGatt = device.connectGatt(MainActivity.this, false, mGattCallback);
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
            }
        }
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {


        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote GATT server
         *
         * @param gatt 返回连接建立的gatt对象
         * @param status 返回的是此次gatt操作的结果，成功了返回0
         * @param newState 每次client连接或断开连接状态变化，STATE_CONNECTED 0，STATE_CONNECTING 1,STATE_DISCONNECTED 2,STATE_DISCONNECTING 3
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            System.out.println("onConnectionStateChange status:" + status + "  newState:" + newState);
            if (status == 0) {
                gatt.discoverServices();
            }
        }

        /**
         * Callback invoked when the list of remote services, characteristics and descriptors for the remote device have been updated, ie new services have been discovered.
         *
         * @param gatt 返回的是本次连接的gatt对象
         * @param status
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            System.out.println("onServicesDiscovered status" + status);

            if(status == BluetoothGatt.GATT_SUCCESS){
                BluetoothGattService gattService = mBluetoothGatt.getService(SCALE_SERVICE);
                if(gattService != null){
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(SCALE_DATA);
                    if(gattCharacteristic!=null){
                        System.out.println("Reading characterisitc...");
                        mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                        BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(DESCRIPTOR);
                        if(descriptor!=null){
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                                boolean descriptorRes = mBluetoothGatt.writeDescriptor(descriptor);
                        }
                        mBluetoothGatt.readCharacteristic(gattCharacteristic);
                    }else{
                        System.out.println("couldn't read character");
                    }
                }else{
                    System.out.println("No such service");
                }
            }else{
                System.out.println("status failed");
            }
        }

        /**
         * Callback triggered as a result of a remote characteristic notification.
         *
         * @param gatt
         * @param characteristic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    byte[] val = characteristic.getValue();
                    StringBuilder sb = new StringBuilder();

                    for (byte b : val) {
                        sb.append(String.format("%02x", b));
                    }

                    //Map<String, String> res = new HashMap<>();
                    JSONObject main = new JSONObject();

                    String time="";
                    String year = sb.substring(6,8) + sb.substring(4,6);
                    time += Integer.toString(Integer.parseInt(year,16));
                    time+=":";
                    String month = sb.substring(8,10);
                    time += Integer.toString(Integer.parseInt(month,16));
                    time+=":";
                    String date = sb.substring(10, 12);
                    time += Integer.toString(Integer.parseInt(date,16));
                    time+=":";
                    String hour = sb.substring(12, 14);
                    time += Integer.toString(Integer.parseInt(hour,16));
                    time+=":";
                    String min = sb.substring(14,16);
                    time+=Integer.toString(Integer.parseInt(min,16));
                    time+=":";
                    String sec = sb.substring(16,18);
                    time+=Integer.toString(Integer.parseInt(sec,16));

                    JSONObject res = new JSONObject();

                    if(sb.charAt(1) == '2'){
                        res.put("Unit", "0"); // kg
                    }else{
                        res.put("Unit", "1"); // lb
                    }
                    if(sb.substring(2,4).equals("04")){
                        res.put("Object", "1"); // object on the scale
                    }else{
                        res.put("Object", "0"); //removed
                    }

                    String weight = sb.substring(24,26) + sb.substring(22,24);
                    res.put("Weight", Double.toString(Integer.parseInt(weight,16)/100.));

                    main.put(time, res);

                    listAdapter.add(main.toString());
                    listAdapter.notifyDataSetChanged();
                }
            });
        }

        /**
         * Callback indicating the result of a characteristic write operation.
         *
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("onCharacteristicWrite");
        }

        /**
         *Callback reporting the result of a characteristic read operation.
         *
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("onCharacteristicRead");
        }

        /**
         * Callback indicating the result of a descriptor write operation.
         *
         * @param gatt
         * @param descriptor
         * @param status
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            System.out.println("onDescriptorWrite");
        }

    };
}