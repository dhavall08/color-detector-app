package com.dr.colordetector;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    Button button;
    BluetoothAdapter bluetoothAdapter;
    ProgressDialog pd;
    private static final String MAC_ADD = "00:18:E4:00:43:97";

    final private int REQUEST_COARSE_LOCATION = 0;
    ArrayList<BluetoothDevice> mDeviceList;

    TextToSpeech mTTS;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu); //your file name
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        bluetoothAdapter.cancelDiscovery();
        mTTS.shutdown();
    }

  /*  @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        switch (item.getItemId()) {
            case R.id.settings:
                //your code
                // EX : call intent if you want to swich to other activity
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        if (mTTS != null) {
            mTTS.speak("Press button to connect the module.", TextToSpeech.QUEUE_ADD, null, null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDeviceList = new ArrayList<BluetoothDevice>();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        button = findViewById(R.id.button);
        pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("Scanning...Connecting...");
        pd.setCanceledOnTouchOutside(false);


        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
                        Log.d("Error", "Language not supported");
                    } else {
                        mTTS.speak("Press button to connect the module.", TextToSpeech.QUEUE_ADD, null, null);
                    }
                }
            }
        });

        if (!bluetoothAdapter.isEnabled()) {
//            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(turnOn, 0);
            bluetoothAdapter.enable(); // not working sometime
            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth already on", Toast.LENGTH_LONG).show();
        }

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pd.show();
                registerReceiver(mReceiver, filter);
                if (!bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable(); // not working sometime
                }
                checkLocationPermission();
            }
        });
    }

    boolean deviceFound = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            assert action != null;
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    //discovery starts, we can show progress dialog or perform other tasks
                    Log.i("--------", "Discovery Started");
                    pd.show();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.i("--------", "Discovery Finished ");

                    if (!deviceFound) {
                        Toast.makeText(getApplicationContext(), "No Color Detection Module Found.", Toast.LENGTH_SHORT).show();
                        mTTS.speak("No Color Detection Module is Found.", TextToSpeech.QUEUE_ADD, null, null);
                    }
                    pd.dismiss();
                    //mDeviceList.get(0).createBond();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    //bluetooth device found
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.i("--------", "New Device found");
                    if (device.getAddress().equals(MAC_ADD)) {
                        deviceFound = true;
                        Log.i("--------", "Color Detection Module Found");
                        Toast.makeText(getApplicationContext(), "Color Detection Module Found.", Toast.LENGTH_SHORT).show();
                        mTTS.speak("Color Detection Module is Found.", TextToSpeech.QUEUE_FLUSH, null, null);
                        afterDiscovered();
                    }
                    break;
            }
        }
    };


    private void afterDiscovered() {
        bluetoothAdapter.cancelDiscovery();
        Intent myIntent = new Intent(MainActivity.this, ColorDisplay.class);
        startActivity(myIntent);
        pd.dismiss(); // dismiss after opening other activity
    }

    // both below functions are used to check if permission for location is granted or not
    protected void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("--------", "Not having Permission");

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Please grant permission")
                        .setMessage("Location permission is required for bluetooth.")
                        .setPositiveButton("Yes. I agree.", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                        REQUEST_COARSE_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_COARSE_LOCATION);
            }
        } else {
            bluetoothAdapter.startDiscovery();
            mTTS.speak("Please wait while connecting to bluetooth.", TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void infoMessage(View view) {
        Toast.makeText(view.getContext(), "Change voice from : Settings > Language & Input > Text-to-Speech output", Toast.LENGTH_LONG).show();
        mTTS.speak("You can change my voice from your settings app.", TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.startDiscovery(); // --->
                } else {
                    //re-request
                    Toast.makeText(getApplicationContext(), "Permission Not Granted!", Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "Bluetooth OFF", Toast.LENGTH_LONG).show();
        bluetoothAdapter.disable();
        // Don't forget to unregister the ACTION_FOUND receiver.
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {//already unregistered
        }
    }


    public void aboutMessage(View view) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("About Developers")
                .setMessage("Dhaval Laiya and Rishi Kansara are the developers of this application. They did their graduation in Computer Engineering from Government Engineering College, Rajkot. This application has been developed as a part of their last year project. Application can only work with this project with other modules.")
                .create()
                .show();
    }
}
