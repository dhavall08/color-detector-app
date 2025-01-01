package com.dr.colordetector;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class ColorDisplay extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    private static Handler mHandler; // Our main handler that will receive callback notifications as async task
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data

    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    private static final String MAC_ADD = "00:18:E4:00:43:97";
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    TextToSpeech mTTS;

    TextView redText;
    TextView greenText;
    TextView blueText;
    TextView colorName;
    View colorArea;

    Colors colors;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
        }
        try {
            unregisterReceiver(mPairingRequestReceiver);
            mTTS.shutdown();
        } catch (Exception e) {//already unregistered
            e.printStackTrace();
        }
        ColorDisplay.this.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

   /* @Override
    protected void onPause() {
        super.onPause();
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
        }
        try {
            unregisterReceiver(mPairingRequestReceiver);
            mTTS.shutdown();
        } catch (Exception e) {//already unregistered
            e.printStackTrace();
        }
        finish();
    }*/

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.color_val);

        redText = findViewById(R.id.red);
        greenText = findViewById(R.id.green);
        blueText = findViewById(R.id.blue);
        colorArea = findViewById(R.id.color_area);
        colorName = findViewById(R.id.color_name);


        colors = new Colors(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(mPairingRequestReceiver, filter);


        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
                        Log.d("Error", "Language not supported");
                    } else {
                        mTTS.speak("Color detection is started.", TextToSpeech.QUEUE_ADD, null, null);
                    }
                }
            }
        });

        mHandler = new Handler() {

            public void handleMessage(android.os.Message msg) {
                if (msg.what == MESSAGE_READ) {
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    Log.d("====", "Reading Message");
                    String str[];
                    assert readMessage != null;
                    str = readMessage.split(" ", 4);

                    if (str.length > 2) {
                        redText.setText(str[0]);
                        greenText.setText(str[1]);
                        blueText.setText(str[2]);

                        Log.d("-------Split Readings", str[0] + " " + str[1] + " " + str[2]);
//                        Toast.makeText(ColorDisplay.this, "R: " + str[0] + " G: " + str[1] + " B: " + str[2], Toast.LENGTH_SHORT).show();
                        int color;
                        try {
                            color = Color.rgb(Integer.parseInt(str[0]), Integer.parseInt(str[1]), Integer.parseInt(str[2]));

                            colorArea.setBackgroundColor(color);

//                        noramlizeColor(color,Integer.parseInt(str[0]),Integer.parseInt(str[1]),Integer.parseInt(str[2]));
                            String name = MatchColor.getColorName(colors, Integer.parseInt(str[0]), Integer.parseInt(str[1]), Integer.parseInt(str[2]));
                            colorName.setText(name);
                            mTTS.speak(name, TextToSpeech.QUEUE_ADD, null, null);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            restartActivity();
                        }
                    }
                    //mReadBuffer.setText(readMessage);
                }

                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1) {
                        Toast.makeText(ColorDisplay.this, "Connected to " + msg.obj, Toast.LENGTH_SHORT).show();

                        //mBluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                    } else {
                        Toast.makeText(ColorDisplay.this, "Connection Failed", Toast.LENGTH_SHORT).show();
                        //mBluetoothStatus.setText("Connection Failed");
                        Log.d("====", "Failed Connection");
                        mTTS.speak("Connection falied. Click button to restart.", TextToSpeech.QUEUE_FLUSH, null, null);
                        ColorDisplay.this.finish();
                    }
                }
            }
        };
        startThread();
    }

    private void restartActivity() {
        Intent mIntent = getIntent();
        finish();
        startActivity(mIntent);
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
        //creates secure outgoing connection with BT device using UUID
    }


    void startThread() {
        // Spawn a new thread to avoid blocking the GUI one
        new Thread() {
            public void run() {
                boolean fail = false;

                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(MAC_ADD);

                device.setPin("1234".getBytes());

                try {
                    mBTSocket = createBluetoothSocket(device);
                } catch (IOException e) {
                    fail = true;
                    Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    mTTS.speak("Connection falied. Click button to restart.", TextToSpeech.QUEUE_ADD, null, null);
                    ColorDisplay.this.finish();
                }
                // Establish the Bluetooth socket connection.
                try {
                    mBTSocket.connect();
                } catch (IOException e) {
                    try {
                        fail = true;
                        mBTSocket.close();
                        mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget();
                        mTTS.speak("Connection falied. Click button to restart.", TextToSpeech.QUEUE_ADD, null, null);
                        ColorDisplay.this.finish();
                    } catch (IOException e2) {
                        //insert code to deal with this
                        mTTS.speak("Connection falied. Click button to restart.", TextToSpeech.QUEUE_ADD, null, null);
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        ColorDisplay.this.finish();
                    }
                }
                if (!fail) {
                    mConnectedThread = new ColorDisplay.ConnectedThread(mBTSocket);
//                    mConnectedThread.write("OK");
                    mConnectedThread.writeInt(1);
                    mConnectedThread.start();
                    Log.d("-----", "Ready, Not failed.");
                    mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, device.getName())
                            .sendToTarget();

                }
            }

        }.start();

    }

    class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Log.d("-----", "Ready to read");
            } catch (IOException e) {
                Log.d("-----", "Exception Occur at ConnectedThread");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] buffer;  // buffer store for the stream
                    int bytes; // bytes returned from read()

                    // Keep listening to the InputStream until an exception occurs
                    while (true) {
                        try {
                            buffer = new byte[1024];
                            // Read from the InputStream
                            bytes = mmInStream.available();
                            if (bytes != 0) {
                                SystemClock.sleep(500); //pause and wait for rest of data. Adjust this depending on your sending speed.
                                bytes = mmInStream.available(); // how many bytes are ready to be read?
                                bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                        /* obtainMessage(int what,int arg1,int arg2,Object obj) */
                                        .sendToTarget(); // Send the obtained bytes to the UI activity
                            }
//                            mmOutStream.write(1);
                        } catch (IOException e) {
                            e.printStackTrace();
                            mTTS.speak("Connection falied. Click button to restart.", TextToSpeech.QUEUE_ADD, null, null);
                            ColorDisplay.this.finish();
                            break;
                        }
                    }

                }
            } catch (Exception consumed) {
                consumed.printStackTrace();
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.d("-----", "Exception Occur at ConnectedThread write");
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void writeInt(int input) {
            try {
                mmOutStream.write(input);
            } catch (IOException e) {
                Log.d("-----", "Exception Occur at ConnectedThread write");
            }
        }

        /* Call this from the main activity to shutdown the connection */
        void cancel() {
            try {
                writeInt(0);
                interrupt();
                mmInStream.close();
                mmOutStream.close();
                mmSocket.close();
            } catch (IOException e) {
                Log.d("-----", "Exception Occur at ConnectedThread cancel");
            }
        }
    }

    private final BroadcastReceiver mPairingRequestReceiver = new BroadcastReceiver() {
        String TAG = "Pairing----------";

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234);
                    //the pin in case you need to accept for an specific pin
                    Log.d(TAG, "Start Auto Pairing. PIN = " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234));
                    byte[] pinBytes;
                    pinBytes = ("" + pin).getBytes("UTF-8");
                    device.setPin(pinBytes);
                    //setPairing confirmation if neeeded
                    //device.setPairingConfirmation(true);
                } catch (Exception e) {
                    Log.e(TAG, "Error occurs when trying to auto pair");
                    mTTS.speak("Bluetooth pairing falied. Click button to restart.", TextToSpeech.QUEUE_ADD, null, null);
                    ColorDisplay.this.finish();
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
            }
            mTTS.shutdown();
            unregisterReceiver(mPairingRequestReceiver);
        } catch (Exception e) {//already unregistered
        }
    }

}


