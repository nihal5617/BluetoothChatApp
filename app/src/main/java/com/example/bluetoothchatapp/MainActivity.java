package com.example.bluetoothchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private ListView listmainchat;
    private EditText edCreateMessage;
    private Button btnSendMessage;
    private ArrayAdapter<String> adaptermainchat;


    private ChatUtils chatUtils;

    private BluetoothAdapter bluetoothAdapter;
    private final int LOCATION_PERMISSION_REQUEST=101;
    private final int SELECT_DEVICE=102;

    public static final int MESSAGE_STATE_CHANGED=0;
    public static final int MESSAGE_READ=1;
    public static final int MESSAGE_WRITE=2;
    public static final int MESSAGE_DEVICE_NAME=3;
    public static final int MESSAGE_TOAST=4;

    public static final String DEVICE_NAME="deviceName";
    public static final String TOAST ="toast";
    private String connectedDevice;

    private Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage( Message msg) {
            switch (msg.what){
                case MESSAGE_STATE_CHANGED:
                    switch (msg.arg1){
                        case ChatUtils.STATE_NONE:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_LISTEN:
                            setState("Again Not Connected");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState("Connected: "+ connectedDevice);
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] buffer=(byte[]) msg.obj;
                    String inputBuffer=new String(buffer,0,msg.arg1);
                    adaptermainchat.add(connectedDevice+": "+ inputBuffer);
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer1=(byte[]) msg.obj;
                    String outputBuffer=new String(buffer1);
                    adaptermainchat.add("Me: "+ outputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice=msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(MainActivity.this, connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void setState(CharSequence subTitle){
        getSupportActionBar().setSubtitle(subTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatUtils=new ChatUtils(MainActivity.this,handler);

        init();
        initBluetooth();
    }

    private void init(){
        listmainchat=findViewById(R.id.list_coversation);
        edCreateMessage=findViewById(R.id.ed_enter_text);
        btnSendMessage=findViewById(R.id.btn_send_text);

        adaptermainchat=new ArrayAdapter<String>(MainActivity.this,R.layout.message_layout);
        listmainchat.setAdapter(adaptermainchat);

        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message=edCreateMessage.getText().toString();
                if(!message.isEmpty()){
                    edCreateMessage.setText("");
                    chatUtils.write(message.getBytes());
                }
            }
        });
    }

    private void initBluetooth(){
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){
            Toast.makeText(MainActivity.this,"No Bluetooth Found",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_search_devices:
                checkPermissions();
                return true;
            case R.id.menu_enable_bluetooh:
                enableBluetooth();
                return true;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkPermissions(){
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }else{
            Intent intent=new Intent(MainActivity.this,DeviceListActivity.class);
            startActivityForResult(intent,SELECT_DEVICE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==SELECT_DEVICE&& resultCode==RESULT_OK){
            String address=data.getStringExtra("DeviceAddress");
            Toast.makeText(MainActivity.this, "Address: "+address, Toast.LENGTH_SHORT).show();
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==LOCATION_PERMISSION_REQUEST){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Intent intent=new Intent(MainActivity.this,DeviceListActivity.class);
                startActivityForResult(intent,SELECT_DEVICE);
            }else{
                new AlertDialog.Builder(MainActivity.this)
                        .setCancelable(false)
                        .setMessage("Location Permission is required\n Please Grant")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkPermissions();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MainActivity.this.finish();
                            }
                        }).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void enableBluetooth(){
        if(bluetoothAdapter.isEnabled()){
            Toast.makeText(MainActivity.this,"Bluetooth Already Enabled", Toast.LENGTH_SHORT).show();
        }
        else{
            bluetoothAdapter.enable();
            Toast.makeText(MainActivity.this,"Bluetooth Enabled", Toast.LENGTH_SHORT).show();
        }

        if(bluetoothAdapter.getScanMode()!=BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoveryIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(discoveryIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(chatUtils!=null){
            chatUtils.stop();
        }
    }
}
