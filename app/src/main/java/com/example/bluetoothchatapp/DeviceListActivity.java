package com.example.bluetoothchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private ListView listpaireddevices,listavailabledevices;
    private Context context;
    private ArrayAdapter<String> adapterPairedDevice,adapterAvailableDevice;
    private BluetoothAdapter bluetoothAdapter;
    private ProgressBar progressScanDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        context=this;
        init();
    }
    public void init(){
        listpaireddevices=findViewById(R.id.lv_paired_devices);
        listavailabledevices=findViewById(R.id.lv_available_devices);

        adapterPairedDevice=new ArrayAdapter<String>(context,R.layout.device_list_item);
        adapterAvailableDevice=new ArrayAdapter<String>(context,R.layout.device_list_item);

        listpaireddevices.setAdapter(adapterPairedDevice);
        listavailabledevices.setAdapter(adapterAvailableDevice);

        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices=bluetoothAdapter.getBondedDevices();

        progressScanDevices=findViewById(R.id.progress_scan_devices);

        listavailabledevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info =((TextView) view).getText().toString();
                String address=info.substring(info.length()-17);

                Intent intent =new Intent();
                intent.putExtra("DeviceAddress",address);
                setResult(RESULT_OK,intent);
                finish();
            }
        });


        if(pairedDevices!=null && pairedDevices.size()>0){
            for(BluetoothDevice device:pairedDevices){
                adapterPairedDevice.add(device.getName()+"\n"+device.getAddress());
            }
        }

        IntentFilter intentFilter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothDeviceListener,intentFilter);
        IntentFilter intentFilter1=new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothDeviceListener,intentFilter1);

    }


    private BroadcastReceiver bluetoothDeviceListener=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.getBondState()  != BluetoothDevice.BOND_BONDED){
                    adapterAvailableDevice.add(device.getName()+"\n"+device.getAddress());
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                progressScanDevices.setVisibility(View.GONE);
                if(adapterAvailableDevice.getCount()==0){
                    Toast.makeText(context,"NO NEW DEVICE FOUND",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(context,"CLICK ON THE DEVICE TO STRT A CHAT",Toast.LENGTH_SHORT).show();
                }
            }

        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_scan_devices:
                Toast.makeText(context,"Scan Devices Clicked",Toast.LENGTH_SHORT).show();
                scanDevices();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void scanDevices(){
        progressScanDevices.setVisibility(View.VISIBLE);
        adapterAvailableDevice.clear();

        Toast.makeText(context,"Scanning Devices",Toast.LENGTH_SHORT).show();

        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }
}






