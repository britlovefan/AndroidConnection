package com.example.ilya.smartcap_v31b;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;

import android.bluetooth.BluetoothAdapter;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import ack.me.truconnectandroid.truconnect.TruconnectCommand;
import ack.me.truconnectandroid.truconnect.TruconnectCommandMode;
import ack.me.truconnectandroid.truconnect.TruconnectErrorCode;
import ack.me.truconnectandroid.truconnect.TruconnectGPIODirection;
import ack.me.truconnectandroid.truconnect.TruconnectGPIOFunction;
import ack.me.truconnectandroid.truconnect.TruconnectManager;
import ack.me.truconnectandroid.truconnect.TruconnectReceiveMode;
import ack.me.truconnectandroid.truconnect.TruconnectResult;

public class MainActivity extends Activity implements ServiceCallbacks
{
    private static final long SCAN_PERIOD = 5000;
    private static final long CONNECT_TIMEOUT_MS = 20000;
    private static final String TAG = "SmartCap";
    private final int BLE_ENABLE_REQ_CODE = 1;

    private final boolean NO_TX_NOTIFY_DISABLE = false;

    private ProgressDialog mConnectProgressDialog;
    private DeviceList mDeviceList;
    private Button mScanButton;

    private Handler mHandler;
    private Runnable mStopScanTask;
    private Runnable mConnectTimeoutTask;

    private TruconnectManager mTruconnectManager;
    private boolean mConnecting = false;
    private boolean mConnected = false;
    private boolean mErrorDialogShowing = false;

    private String mCurrentDeviceName;

    private ServiceConnection mConnection;
    private TruconnectService mService;
    private boolean mBound = false;

    private MyService myService;
    private boolean bound = false;

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mReceiverIntentFilter;

    // Connected stuff
    private ToggleButton mModeButton;
    private int mCurrentMode;
    private Button mSendTextButton;
    private Button AutoPhoto;
    private EditText mTextToSendBox;
    private TextView mReceivedDataTextBox;
    private ScrollView mScrollView;
    private Button mClearTextButton;
    private ToggleButton mToggleIm;
    private Button mShowIm;
    private ImageView imView;

    private ProgressDialog mDisconnectDialog;
    private boolean mDisconnecting = false;
    private Runnable mDisconnectTimeoutTask;

    private boolean x = false;

    private boolean mRecording = false;
    private String mFileNameLog;
    private byte[] imBytesSplit;
    private byte[] imBytes;
    private int count_bytes;
    private int len_image;
    private int val;
    private boolean header_done = false;

    Calendar timeNow;

    private Set<String> ValidDevice = new HashSet<>();    //GL: use a set to store the valid device name
    private HashMap<String, Long> TimeTable = new HashMap<>();  //time of latest data requirement

    int RSSI;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ValidDevice.add("SCv31b1");
        initScanButton();
        initDeviceList();
        initBroadcastManager();
        initServiceConnection();
        initBroadcastReceiver();
        initReceiverIntentFilter();

        startService(new Intent(this, TruconnectService.class));

        mHandler = new Handler();

        mStopScanTask = new Runnable()
        {
            @Override
            public void run()
            {
                stopScan();
            }
        };

        mConnectTimeoutTask = new Runnable()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showErrorDialog(R.string.error, R.string.con_timeout_message);
                        dismissConnectDialog();
                        if (mTruconnectManager != null && mTruconnectManager.isConnected()) {
                            disconnect(NO_TX_NOTIFY_DISABLE);
                        }
                    }
                });
            }
        };

        mModeButton = (ToggleButton) findViewById(R.id.toggle_str_comm);
        mModeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mModeButton.isChecked())
                {
                    mCurrentMode = TruconnectManager.MODE_STREAM;
                }
                else
                {
                    mCurrentMode = TruconnectManager.MODE_COMMAND_REMOTE;
                }
                x = mTruconnectManager.setMode(mCurrentMode);
                Log.d(TAG, "Mode set to: " + mCurrentMode);
                Log.d(TAG, "Truconnect Manager returned: " + x);
            }
        });



       /*
       LG: auto connect to the ble and require the photo data
       By pushing this button, it will auto finish connecting the device, change mode and send command
       Setting name can connect to different device
       */
        AutoPhoto = (Button) findViewById(R.id.autobutton);
        AutoPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = "";
                while (!mConnecting) {
                    stopScan();
                    if(mDeviceList.findDeviceWithName(name) != null) {
                        Log.d(TAG, "Connecting to BLE device " + name);
                        mCurrentDeviceName = name;
                        mTruconnectManager.connect(name);
                        // showConnectingDialog(view.getContext());
                        mHandler.postDelayed(mConnectTimeoutTask, CONNECT_TIMEOUT_MS);
                        mConnecting = true;
                        Log.d(TAG,"SDFADSFASDFASFASFASFASFA   111");
                    }
                }

                Log.d(TAG,"SDFADSFASDFASFASFASFASFA   connected");
                while(mTruconnectManager == null || !mTruconnectManager.isConnected()) {

                }
                if (mTruconnectManager != null && mTruconnectManager.isConnected()) {
                    Log.d(TAG, "SDFADSFASDFASFASFASFASFA   entered");
                    mCurrentMode = TruconnectManager.MODE_STREAM;
                    mTruconnectManager.setMode(mCurrentMode);
                    Log.d(TAG, "SDFADSFASDFASFASFASFASFA   mode");
                    String dataToSend = "0";
                //    dataToSend = "*0#";
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            auto_photo();
                        }
                    }, 4500);
                }
                Log.d(TAG, "SDFADSFASDFASFASFASFASFA   out");

            }
        });

        mTextToSendBox = (EditText) findViewById(R.id.editText);
        mSendTextButton = (Button) findViewById(R.id.button_send);
        mSendTextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String data = mTextToSendBox.getText().toString();
                String dataToSend = "0";
                if (mCurrentMode == TruconnectManager.MODE_STREAM && data != null && !data.isEmpty())
                {
                    if (data.equals("R")) { // READ RTC
                        dataToSend = "*R#";
                        mTruconnectManager.writeData(dataToSend);
                        Log.d(TAG, "Sent: " + dataToSend);
                    }
                    else if (data.equals("T")) { // SET RTC
                        timeNow = Calendar.getInstance();
                        int second = timeNow.get(Calendar.SECOND);
                        int minute = timeNow.get(Calendar.MINUTE);
                        int hour = timeNow.get(Calendar.HOUR_OF_DAY);
                        int dayOfWeek = timeNow.get(Calendar.DAY_OF_WEEK);
                        int dayOfMonth = timeNow.get(Calendar.DAY_OF_MONTH);
                        int month = timeNow.get(Calendar.MONTH);
                        int year = timeNow.get(Calendar.YEAR);
                        //String rtc_data = Integer.toString(second) + Integer.toString(minute) + Integer.toString(hour) + Integer.toString(dayOfWeek) + Integer.toString(dayOfMonth) + Integer.toString(month) + Integer.toString(year);
                        String rtc_data = String.format("%02d%02d%02d%d%02d%02d%04d",second,minute,hour,dayOfWeek,dayOfMonth,month+1,year);
                        dataToSend = "*T" + rtc_data + "#";
                        mTruconnectManager.writeData(dataToSend);
                        Log.d(TAG, "Sent: " + dataToSend);
                    }
                    else if (data.equals("C")) { // TAKE IMAGE
                        dataToSend = "*C#";
                        mTruconnectManager.writeData(dataToSend);
                        Log.d(TAG, "Sent: " + dataToSend);
                    }
                    else if (data.startsWith("*")) { // Own command
                        dataToSend = data;
                        mTruconnectManager.writeData(dataToSend);
                        Log.d(TAG, "Sent: " + dataToSend);
                    }
                    else {
                        dataToSend = data;
                        mTruconnectManager.writeData(dataToSend);
                        Log.d(TAG, "Sent: " + dataToSend);
                    }
                }

                if (mCurrentMode == TruconnectManager.MODE_COMMAND_REMOTE) {
                    if (data.isEmpty()) {
                        //mTruconnectManager.GPIOGetUsage();
                    }
                    else if (Integer.parseInt(data) == 1) {
                        mTruconnectManager.GPIOFunctionSet(10, TruconnectGPIOFunction.CONN_GPIO);
                        mTruconnectManager.GPIOFunctionSet(11, TruconnectGPIOFunction.STDIO);
                        mTruconnectManager.GPIODirectionSet(11, TruconnectGPIODirection.HIGH_IMPEDANCE);
                    }
                    else if (Integer.parseInt(data) == 0) {
                        mTruconnectManager.save();
                        mTruconnectManager.reboot();
                    }
                }

                mTextToSendBox.setText("");//clear input after send
            }
        });

        mClearTextButton = (Button) findViewById(R.id.clear_button);
        mClearTextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
              //  StartBack();
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        startb();                               //LG: start background running
                    }
                }, 500);
                clearReceivedTextBox();
            }
        });

        mToggleIm = (ToggleButton) findViewById(R.id.toggle_im);
        mToggleIm.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mToggleIm.isChecked())
                {
                    // TODO start logging
                    doStartRecording();
                    startRecording();
                    count_bytes = 0;
                    header_done = false;
                }
                else
                {
                    // TODO stop logging
                    doStopRecording();
                    stopRecording();
                }
                Log.d(TAG, "Image recording: " + mRecording);
            }
        });

        mShowIm = (Button) findViewById(R.id.show_im);
        mShowIm.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                /*imView = (ImageView) findViewById(R.id.imageView);
                Bitmap bmp = BitmapFactory.decodeFile(mFileNameLog);
                imView.setImageBitmap(bmp);*/
                imView = (ImageView) findViewById(R.id.imageView);
                Bitmap bmp = BitmapFactory.decodeByteArray(imBytes, 0, len_image);
                imView.setImageBitmap(bmp);
                Log.d(TAG, "Showing Image");
                Log.d(TAG, "File path: " + mFileNameLog);
            }
        });

        /*mReceivedDataTextBox = (TextView) findViewById(R.id.receivedDataBox);
        mReceivedDataTextBox.setMovementMethod(new ScrollingMovementMethod());*/
        mReceivedDataTextBox = (TextView) findViewById(R.id.receivedDataBox);
        mScrollView = (ScrollView) findViewById(R.id.scroll_view);

        GUISetCommandMode();//set up gui for command mode initially


        mDisconnectTimeoutTask = new Runnable()
        {
            @Override
            public void run()
            {
                dismissProgressDialog();
                showErrorDialog(R.string.error, R.string.discon_timeout_message);
            }
        };
    }


    private void startb(){
        startc();
    }
    /*
    LG: this will start the MyService service which is aimed to run background
     */
    private void startc(){
        Intent intent = new Intent(this, MyService.class);
        startService(intent);
      //  try{ Thread.sleep(10000); }catch(InterruptedException e){ }
    }

    /*
    LG: send command to require transferring the photo
     */

    public void auto_photo(){
        Log.d(TAG, "SDFADSFASDFASFASFASFASFA   auto_photo");
        String dataToSend = "*0#";
        mTruconnectManager.writeData(dataToSend);
        Log.d(TAG, "Sent: " + dataToSend);
       // try{ Thread.sleep(10000); }catch(InterruptedException e){ }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.action_about:
                openAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        mDeviceList.clear();
        mConnected = false;
        mConnecting = false;
        Intent intent1 = new Intent(this, MyService.class);
        bindService(intent1, serviceConnection, Context.BIND_AUTO_CREATE);

        Intent intent = new Intent(this, TruconnectService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mReceiverIntentFilter);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        mHandler.removeCallbacks(mStopScanTask);
        cancelConnectTimeout();
        dismissConnectDialog();
        if (bound) {
            myService.setCallbacks(null); // unregister
            unbindService(serviceConnection);
            bound = false;
        }
        if (mBound)
        {
            mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
            unbindService(mConnection);
            mBound = false;
        }

        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        stopService(new Intent(this, TruconnectService.class));
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // cast the IBinder and get MyService instance
            MyService.LocalBinder binder = (MyService.LocalBinder) service;
            myService = binder.getService();
            bound = true;
            myService.setCallbacks(MainActivity.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    /*
    LG:  Background service run this algorithm
     */
    @Override
    public void doSomething() {
       // try{ Thread.sleep(3000); }catch(InterruptedException e){ }
        Timer mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        ChooseAndConnect(); // do your work right here
                    }
                });
            }
        }, 20000, 20000);                           //scan time
    }

    //LG: iterate the bluetooth device list and choose the valid device name
    private void ChooseAndConnect() {
        startScan();
        int Len = mDeviceList.count();
        for(int i = 0; i < Len; i++) {
            String name = mDeviceList.get(i);
            if (ValidDevice.contains(name)) {    //if it has been 2000 since last connected, connected.
                if(TimeTable.containsKey(name)) {
                    long time = System.currentTimeMillis();
                    long lasttime = TimeTable.get(name);
                    if (lasttime - time > 2000) {
                        TimeTable.put(name, time);
                        auto(name);
                    }
                }
                else {
                    TimeTable.put(name, System.currentTimeMillis());
                    auto(name);
                }
            }
        }
    }

    //LG: connect to the specific device (chose by name)
    private void auto(String name){
       // while (!mConnecting) {
            stopScan();
            if(mDeviceList.findDeviceWithName(name) != null) {
                Log.d(TAG, "Connecting to BLE device " + name);
                mCurrentDeviceName = name;
                mTruconnectManager.connect(mCurrentDeviceName);
                // showConnectingDialog(view.getContext());
                mHandler.postDelayed(mConnectTimeoutTask, CONNECT_TIMEOUT_MS);
                mConnecting = true;
                Log.d(TAG,"SDFADSFASDFASFASFASFASFA   111");
            }
    //    }

        Log.d(TAG,"SDFADSFASDFASFASFASFASFA   connected");
        while(mTruconnectManager == null || !mTruconnectManager.isConnected()) {

        }
        if (mTruconnectManager != null && mTruconnectManager.isConnected()) {
            Log.d(TAG, "SDFADSFASDFASFASFASFASFA   entered");
            mCurrentMode = TruconnectManager.MODE_STREAM;
            mTruconnectManager.setMode(mCurrentMode);
            Log.d(TAG, "SDFADSFASDFASFASFASFASFA   mode");
            String dataToSend = "0";

          new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    auto_photo();
                }
            }, 4500);
            //try{ Thread.sleep(3000); }catch(InterruptedException e){ }
            //auto_photo();

        }
        Log.d(TAG, "SDFADSFASDFASFASFASFASFA   out");
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == BLE_ENABLE_REQ_CODE)
        {
            mService.initTruconnectManager();//try again
            if (mTruconnectManager.isInitialised())
            {
                startScan();
            }
            else
            {
                showUnrecoverableErrorDialog(R.string.init_fail_title, R.string.init_fail_msg);
            }
        }
    }

    private void initScanButton()
    {
        mScanButton = (Button) findViewById(R.id.button_scan);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeviceList.clear();
                startScan();
            }
        });
    }

    private void initDeviceList()
    {
        ListView deviceListView = (ListView) findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.listitem, R.id.textView);

        initialiseListviewListener(deviceListView);
        mDeviceList = new DeviceList(adapter, deviceListView);
    }

    private void initServiceConnection()
    {
        mConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service)
            {
                TruconnectService.LocalBinder binder = (TruconnectService.LocalBinder) service;
                mService = binder.getService();
                mBound = true;

                mTruconnectManager = mService.getManager();
                if(!mTruconnectManager.isInitialised())
                {
                    startBLEEnableIntent();
                }
                else
                {
                    startScan();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0)
            {
                mBound = false;
            }
        };
    }

    private void initBroadcastReceiver()
    {
        mBroadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                // Get extra data included in the Intent
                String action = intent.getAction();

                Log.d(TAG, "Received intent " + intent);

                switch (action)
                {
                    case TruconnectService.ACTION_SCAN_RESULT:
                        addDeviceToList(TruconnectService.getData(intent));
                        break;

                    case TruconnectService.ACTION_CONNECTED:
                        String deviceName = TruconnectService.getDeviceName(intent);
                        int services = TruconnectService.getIntData(intent);

                        cancelConnectTimeout();
                        dismissConnectDialog();

                        //RSSI = intent.getShortExtra(mBroadcastReceiver.EXTRA_RSSI,Short.MIN_VALUE);

                        //if no truconnect services
                        if (services == TruconnectService.SERVICES_NONE ||
                                services == TruconnectService.SERVICES_OTA_ONLY)
                        {
                            showErrorDialog(R.string.error, R.string.error_service_disc);
                            disconnect(NO_TX_NOTIFY_DISABLE);
                        }
                        else if (!mConnected)
                        {
                            mConnected = true;
                            showToast("Connected to " + deviceName, Toast.LENGTH_SHORT);
                            Log.d(TAG, "Connected to " + deviceName);

                            startDeviceInfoActivity();
                        }
                        break;

                    case TruconnectService.ACTION_DISCONNECTED:
                        setDisconnectedState();
                        dismissConnectDialog();
                        cancelConnectTimeout();
                        break;

                    case TruconnectService.ACTION_ERROR:
                        TruconnectErrorCode errorCode = TruconnectService.getErrorCode(intent);
                        //handle errors
                        switch (errorCode)
                        {
                            case CONNECT_FAILED:
                                setDisconnectedState();
                                dismissConnectDialog();
                                cancelConnectTimeout();
                                showErrorDialog(R.string.error, R.string.con_err_message);
                                break;

                            case DEVICE_ERROR:
                                cancelConnectTimeout();
                                dismissConnectDialog();
                                if (mTruconnectManager != null && mTruconnectManager.isConnected())
                                {
                                    disconnect(NO_TX_NOTIFY_DISABLE);
                                }
                                break;

                            case SET_NOTIFY_FAILED:
                                cancelConnectTimeout();
                                dismissConnectDialog();
                                disconnect(NO_TX_NOTIFY_DISABLE);
                                showErrorDialog(R.string.error, R.string.error_configuration);
                                break;

                            case SERVICE_DISCOVERY_ERROR:
                                //need to disconnect
                                cancelConnectTimeout();
                                dismissConnectDialog();
                                disconnect(NO_TX_NOTIFY_DISABLE);
                                showErrorDialog(R.string.error, R.string.error_service_disc);
                                break;

                            case DISCONNECT_FAILED:
                                if (!isFinishing())
                                {
                                    showUnrecoverableErrorDialog(R.string.error, R.string.discon_err_message);
                                }
                                break;
                        }
                        break;

                    case TruconnectService.ACTION_COMMAND_SENT:
                        String command = TruconnectService.getCommand(intent).toString();
                        Log.d(TAG, "Command " + command + " sent");
                        break;

                    case TruconnectService.ACTION_COMMAND_RESULT:
                        handleCommandResponse(intent);
                        break;

                    case TruconnectService.ACTION_MODE_WRITE:
                        int mode = TruconnectService.getMode(intent);
                        if (mode == TruconnectManager.MODE_STREAM)
                        {
                            //disable buttons while in stream mode (must be in rem command to work)
                            GUISetStreamMode();
                        }
                        else
                        {
                            GUISetCommandMode();
                        }
                        break;

                    case TruconnectService.ACTION_STRING_DATA_READ:
                        //if (mCurrentMode == TruconnectManager.MODE_STREAM)
                        //{
                        String text = TruconnectService.getData(intent);
                        updateReceivedTextBox(text);
                        if (text.equals("I")) {
                            mTruconnectManager.setReceiveMode(TruconnectReceiveMode.BINARY);
                            count_bytes = 0;
                            header_done = false;
                            mTruconnectManager.writeData("0");
                            break;
                        }

                        if(mRecording) {
                            writeLog(text);
                            if (count_bytes == 0) {
                                val = Integer.parseInt(text);
                                len_image = val;
                                count_bytes++;
                            }
                            else if (count_bytes == 1) {
                                val = Integer.parseInt(text);
                                len_image += val*256;
                                imBytesSplit = new byte[2*len_image];
                                count_bytes++;
                                header_done = true;
                            }
                            else {
                                byte[] block = text.getBytes(Charset.forName("UTF-8"));
                                //byte[] block = TruconnectService.getByteData(intent);
                                System.arraycopy(block,0,imBytesSplit,count_bytes-2,block.length);
                                    /*for (int ii=0;ii<block.length;ii++) {
                                        imBytes[count_bytes-2+ii] = block[ii];
                                    }*/
                                count_bytes += block.length;
                                //imBytes[count_bytes-2] = (byte) val;
                            }

                            if (count_bytes < len_image*2) mTruconnectManager.writeData("0");

                            //count_bytes++;

                            if (count_bytes>=(2*len_image) && header_done) {
                                imBytes = new byte[len_image];
                                for (int ii=0;ii<len_image;ii++) {
                                    imBytes[ii] = (byte) (imBytesSplit[2*ii] + (imBytesSplit[(2*ii)+1]*16));
                                }
                                saveImage(imBytes);
                                mToggleIm.setChecked(false);
                                doStopRecording();
                                stopRecording();
                            }
                        }
                        Log.d(TAG, "Bytes: " + count_bytes);
                        //}
                        break;

                    case TruconnectService.ACTION_BINARY_DATA_READ:
                        byte[] block = TruconnectService.getBinaryData(intent);
                        if(mRecording) {
                            writeLog(block.toString());
                        }
                        if (!header_done) {
                            if (block.length == 1 && count_bytes == 0) {
                                len_image = block[0];
                                count_bytes++;
                            }
                            else if (block.length == 1 && count_bytes == 1) {
                                len_image += block[0]*256;
                                count_bytes--;
                                header_done = true;
                                imBytes = new byte[len_image];
                            }
                            else if (block.length > 1) {
                                len_image = (block[0] & 0x00FF) | ((block[1] << 8) & 0xFF00);
                                header_done = true;
                                imBytes = new byte[len_image];

                                if (block.length > 2) {
                                    System.arraycopy(block, 2, imBytes, count_bytes, block.length - 2);
                                    count_bytes += block.length - 2;
                                }
                            }
                        }
                        else {
                            if (count_bytes + block.length > len_image) {
                                System.arraycopy(block, 0, imBytes, count_bytes, len_image-count_bytes);
                                count_bytes += (len_image-count_bytes);
                            }
                            else {
                                System.arraycopy(block, 0, imBytes, count_bytes, block.length);
                                count_bytes += block.length;
                            }
                        }

                        //if (count_bytes < len_image) mTruconnectManager.writeData("0");
                        //mTruconnectManager.writeData("0");

                        if (count_bytes>2 && imBytes[count_bytes-2]==-1 && imBytes[count_bytes-1]==-39) {
                            //if (count_bytes>=(len_image) && header_done) {
                            saveImage(imBytes);
                            mToggleIm.setChecked(false);
                            doStopRecording();
                            stopRecording();
                            clearReceivedTextBox();
                            Log.d(TAG, "SDFADSFASDFASFASFASFASFA   photo show");
                            imView = (ImageView) findViewById(R.id.imageView);
                            Bitmap bmp = BitmapFactory.decodeByteArray(imBytes, 0, len_image);
                            imView.setImageBitmap(bmp);
                            mTruconnectManager.setReceiveMode(TruconnectReceiveMode.STRING);
                            //mTruconnectManager.writeData("*000#");
                            //mTruconnectManager.writeData("*R#");
                            //mTruconnectManager.writeData("*W#");
                        }
                        Log.d(TAG, "Bytes: " + count_bytes + "Val: " + block[0]);

                        break;
                }
            }
        };
    }

    public void initBroadcastManager()
    {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
    }

    public void initReceiverIntentFilter()
    {
        mReceiverIntentFilter = new IntentFilter();
        mReceiverIntentFilter.addAction(TruconnectService.ACTION_SCAN_RESULT);
        mReceiverIntentFilter.addAction(TruconnectService.ACTION_CONNECTED);
        mReceiverIntentFilter.addAction(TruconnectService.ACTION_DISCONNECTED);
        mReceiverIntentFilter.addAction(TruconnectService.ACTION_ERROR);
        mReceiverIntentFilter.addAction(TruconnectService.ACTION_COMMAND_RESULT);
        mReceiverIntentFilter.addAction(TruconnectService.ACTION_STRING_DATA_READ);
        mReceiverIntentFilter.addAction(TruconnectService.ACTION_BINARY_DATA_READ);
        mReceiverIntentFilter.addAction(TruconnectService.ACTION_MODE_WRITE);
    }

    private void startBLEEnableIntent()
    {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, BLE_ENABLE_REQ_CODE);
    }

    private void initialiseListviewListener(ListView listView)
    {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                mCurrentDeviceName = mDeviceList.get(position);

                if (!mConnecting) {
                    mConnecting = true;

                    stopScan();
                    Log.d(TAG, "Connecting to BLE device " + mCurrentDeviceName);
                    mTruconnectManager.connect(mCurrentDeviceName);
                    Log.d(TAG,"dfasfasdfasdfasdfasdfadsfdsafa" + mCurrentDeviceName);

                    showConnectingDialog(view.getContext());

                    mHandler.postDelayed(mConnectTimeoutTask, CONNECT_TIMEOUT_MS);
                }
            }
        });
    }

    private void startScan()
    {
        if (mTruconnectManager != null)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTruconnectManager.startScan();
                }
            });
            //startProgressBar();
            disableScanButton();
            mHandler.postDelayed(mStopScanTask, SCAN_PERIOD);
        }
    }

    private void stopScan()
    {
        if (mTruconnectManager != null && mTruconnectManager.stopScan())
        {
            //stopProgressBar();
            enableScanButton();
        }
    }

    private void startDeviceInfoActivity()
    {
        //GUISetCommandMode();
        mTruconnectManager.setMode(TruconnectManager.MODE_COMMAND_REMOTE);
        mTruconnectManager.setSystemCommandMode(TruconnectCommandMode.MACHINE);
        mTruconnectManager.getVersion();
        //startActivity(new Intent(getApplicationContext(), DeviceInfoActivity.class));
        mDeviceList.clear();
        mDeviceList.add(mCurrentDeviceName);
    }

    private void enableScanButton()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mScanButton.setEnabled(true);
            }
        });
    }

    private void disableScanButton()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mScanButton.setEnabled(false);
            }
        });
    }

    private void showToast(final String msg, final int duration)
    {
        if (!isFinishing())
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(getApplicationContext(), msg, duration).show();
                }
            });
        }
    }

    private void showErrorDialog(final int titleID, final int msgID)
    {
        if (!mErrorDialogShowing && !isFinishing())
        {
            mErrorDialogShowing = true;

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(titleID)
                            .setMessage(msgID)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                    mErrorDialogShowing = false;
                                }
                            })
                            .create()
                            .show();
                }
            });
        }
    }

    private void showUnrecoverableErrorDialog(final int titleID, final int msgID)
    {
        if (!isFinishing())
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    builder.setTitle(titleID)
                            .setMessage(msgID)
                            .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                            .create()
                            .show();
                }
            });
        }
    }

    private void dismissConnectDialog()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (mConnectProgressDialog != null)
                {
                    mConnectProgressDialog.dismiss();
                    mConnectProgressDialog = null;
                }
            }
        });
    }

    //Only adds to the list if not already in it
    private void addDeviceToList(final String name)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceList.add(name);
            }
        });
    }

    private void showConnectingDialog(final Context context)
    {
        if (!isFinishing())
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
                    String title = getString(R.string.progress_title);
                    String msg = getString(R.string.progress_message);
                    dialog.setIndeterminate(true);//Dont know how long connection could take.....
                    dialog.setCancelable(true);

                    mConnectProgressDialog = dialog.show(context, title, msg);
                    mConnectProgressDialog.setCancelable(true);
                    mConnectProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                    {
                        @Override
                        public void onCancel(DialogInterface dialogInterface)
                        {
                            dialogInterface.dismiss();
                        }
                    });
                }
            });
        }
    }

    private void setDisconnectedState()
    {
        mConnected = false;
        mConnecting = false;
    }

    private void cancelConnectTimeout()
    {
        mHandler.removeCallbacks(mConnectTimeoutTask);
    }

    private void disconnect(boolean disableTxNotify)
    {
        setDisconnectedState();
        mTruconnectManager.disconnect(disableTxNotify);
    }

    private void openAboutDialog()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialogs.makeAboutDialog(MainActivity.this).show();
            }
        });
    }

    private void dismissProgressDialog()
    {
        if (mDisconnectDialog != null)
        {
            mDisconnectDialog.dismiss();
        }
    }

    private void handleCommandResponse(Intent intent)
    {
        TruconnectCommand command = TruconnectService.getCommand(intent);
        int ID = TruconnectService.getID(intent);
        int code = TruconnectService.getResponseCode(intent);
        String result = TruconnectService.getData(intent);
        String message = "";

        Log.d(TAG, "Command " + command + " result");

        if (code == TruconnectResult.SUCCESS)
        {
            switch (command)
            {
                case ADC:
                    /*if (ID == mCurrentADCCommandID)
                    {
                        message = String.format("ADC: %s", result);
                        mADCTextView.setText(message);
                    }
                    else
                    {
                        Log.d(TAG, "Invalid ADC command ID!");
                    }*/
                    break;

                case GPIO_GET:
                    /*if (ID == mCurrentGPIOCommandID)
                    {
                        message = String.format("GPIO: %s", result);
                        mGPIOTextView.setText(message);
                    }
                    else
                    {
                        Log.d(TAG, "Invalid GPIO command ID!");
                    }*/
                    break;

                case GPIO_SET:
                    break;
            }
        }
        else
        {
            message = String.format("ERROR %d - %s", code, result);
            showToast(message, Toast.LENGTH_SHORT);
        }
    }




    //set up gui elements for command mode operation
    private void GUISetCommandMode()
    {
        //mSendTextButton.setEnabled(false);
        //mTextToSendBox.setVisibility(View.INVISIBLE);
    }

    //set up gui elements for command mode operation
    private void GUISetStreamMode()
    {
        mSendTextButton.setEnabled(true);
        mTextToSendBox.setVisibility(View.VISIBLE);
    }

    private void updateReceivedTextBox(String newData)
    {
        mReceivedDataTextBox.append(newData);
    }

    private void clearReceivedTextBox()
    {
        mReceivedDataTextBox.setText("");
    }

    private void doStartRecording() {
        File sdCard = Environment.getExternalStorageDirectory();

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateTimeString = format.format(new Date());
        String fileName = sdCard.getAbsolutePath() + "/ams001_" + currentDateTimeString + ".log";

        this.setFileNameLog(fileName);
        this.startRecording();

        showToast("Logging Started", Toast.LENGTH_SHORT);
    }

    private void doStopRecording() {
        this.stopRecording();
        showToast("Logging Stopped", Toast.LENGTH_SHORT);
    }

    public void setFileNameLog( String fileNameLog ) {
        mFileNameLog = fileNameLog;
    }

    public void startRecording() {
        mRecording = true;
    }

    public void stopRecording() {
        mRecording = false;
    }

    private boolean writeLog(String buffer) {
        String state = Environment.getExternalStorageState();
        File logFile = new File ( mFileNameLog );

        if (Environment.MEDIA_MOUNTED.equals(state)) {

            try {
                FileOutputStream f = new FileOutputStream( logFile, true );

                PrintWriter pw = new PrintWriter(f);
                pw.print( buffer );
                pw.flush();
                pw.close();

                f.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            this.stopRecording();
            return false;
        } else {
            this.stopRecording();
            return false;
        }

        return true;
    }

    private void saveImage(byte[] data) {
        File sdCard = Environment.getExternalStorageDirectory();
        String fileName = sdCard.getAbsolutePath() + "/ams001_image.jpg";
        File testimage = new File(fileName);

        if (testimage.exists()) {
            testimage.delete();
        }

        try {
            FileOutputStream fos = new FileOutputStream(testimage.getPath());
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}