package com.example.ilya.smartcap_v31b;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.Serializable;

import ack.me.truconnectandroid.truconnect.TruconnectCallbacks;
import ack.me.truconnectandroid.truconnect.TruconnectCommand;
import ack.me.truconnectandroid.truconnect.TruconnectErrorCode;
import ack.me.truconnectandroid.truconnect.TruconnectManager;
import ack.me.truconnectandroid.truconnect.TruconnectResult;

public class TruconnectService extends Service implements Serializable
{
    public static final String ACTION_SCAN_RESULT = "ACTION_SCAN_RESULT";
    public static final String ACTION_CONNECTED = "ACTION_CONNECTED";
    public static final String ACTION_DISCONNECTED = "ACTION_DISCONNECTED";
    public static final String ACTION_MODE_WRITE = "ACTION_MODE_WRITE";
    public static final String ACTION_MODE_READ = "ACTION_MODE_READ";
    public static final String ACTION_STRING_DATA_WRITE = "ACTION_STRING_DATA_WRITE";
    public static final String ACTION_BINARY_DATA_WRITE = "ACTION_BINARY_DATA_WRITE";
    public static final String ACTION_STRING_DATA_READ = "ACTION_STRING_DATA_READ";
    public static final String ACTION_BINARY_DATA_READ = "ACTION_BINARY_DATA_READ";
    public static final String ACTION_COMMAND_SENT = "ACTION_COMMAND_SENT";
    public static final String ACTION_COMMAND_RESULT = "ACTION_COMMAND_RESULT";
    public static final String ACTION_VERSION_READ = "ACTION_VERSION_READ";
    public static final String ACTION_ERROR = "ACTION_ERROR";

    public static final String ACTION_OTA_INIT = "ACTION_OTA_INIT";
    public static final String ACTION_OTA_CHECK = "ACTION_OTA_CHECK";
    public static final String ACTION_OTA_ABORT = "ACTION_OTA_ABORT";
    public static final String ACTION_OTA_START = "ACTION_OTA_START";
    public static final String ACTION_OTA_DATA_SENT = "ACTION_OTA_DATA_SENT";
    public static final String ACTION_OTA_DONE = "ACTION_OTA_DONE";
    public static final String ACTION_OTA_ERROR = "ACTION_OTA_ERROR";

    public static final String EXTRA_MODE = "EXTRA_MODE";
    public static final String EXTRA_DATA = "EXTRA_DATA";
    public static final String EXTRA_ID = "EXTRA_ID";
    public static final String EXTRA_COMMAND = "EXTRA_COMMAND";
    public static final String EXTRA_RESPONSE_CODE = "EXTRA_RESPONSE_CODE";
    public static final String EXTRA_ERROR = "EXTRA_ERROR";

    public static final String EXTRA_VERSION = "EXTRA_VERSION";
    public static final String EXTRA_NAME = "EXTRA_NAME";
    public static final String EXTRA_IS_UP_TO_DATE = "EXTRA_IS_UP_TO_DATE";

    public static final int SERVICES_NONE = 0;
    public static final int SERVICES_TRUCONNECT_ONLY = 1;
    public static final int SERVICES_OTA_ONLY = 2;
    public static final int SERVICES_BOTH = 3;

    private static final boolean DISABLE_TX_NOTIFY = true;

    private final String TAG = "TruconnectService";

    private final boolean TX_NOTIFY_DISABLE = true;

    private final int mStartMode = START_NOT_STICKY;
    private final IBinder mBinder = new LocalBinder();
    boolean mAllowRebind = true;
    private TruconnectManager mTruconnectManager;

    private TruconnectCallbacks mCallbacks;
    private LocalBroadcastManager mBroadcastManager;

    public class LocalBinder extends Binder
    {
        TruconnectService getService()
        {
            // Return this instance of LocalService so clients can call public methods
            return TruconnectService.this;
        }
    }

    @Override
    public void onCreate()
    {
        // The service is being created
        Log.d(TAG, "Creating service");

        mTruconnectManager = new TruconnectManager();
        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        initCallbacks();
        initTruconnectManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // The service is starting, due to a call to startService()
        return mStartMode;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // A client is binding to the service with bindService()
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        // All clients have unbound with unbindService()
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent)
    {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }

    @Override
    public void onDestroy()
    {
        // The service is no longer used and is being destroyed
        Log.d(TAG, "Destroying service");

        if (mTruconnectManager != null)
        {
            mTruconnectManager.stopScan();
            mTruconnectManager.disconnect(DISABLE_TX_NOTIFY);//ensure all connections are terminated
            mTruconnectManager.deinit();
        }
    }

    public TruconnectManager getManager()
    {
        return mTruconnectManager;
    }

    public boolean initTruconnectManager()
    {
        return mTruconnectManager.init(TruconnectService.this, mCallbacks, null);
    }

    private void initCallbacks()
    {
        mCallbacks = new TruconnectCallbacks()
        {
            @Override
            public void onScanResult(String deviceName)
            {
                Log.d(TAG, "onScanResult");

                Intent intent = new Intent(ACTION_SCAN_RESULT);
                intent.putExtra(EXTRA_DATA, deviceName);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onConnected(String deviceName, int services)
            {
                Log.d(TAG, "onConnected");

                Intent intent = new Intent(ACTION_CONNECTED);

                intent.putExtra(EXTRA_NAME, deviceName);
                intent.putExtra(EXTRA_DATA, services);

                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onDisconnected()
            {
                Log.d(TAG, "onDisconnected");

                Intent intent = new Intent(ACTION_DISCONNECTED);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onModeWritten(int mode)
            {
                Log.d(TAG, "onModeWritten");

                Intent intent = new Intent(ACTION_MODE_WRITE);
                intent.putExtra(EXTRA_MODE, mode);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onModeRead(int mode)
            {
                Log.d(TAG, "onModeRead");

                Intent intent = new Intent(ACTION_MODE_READ);
                intent.putExtra(EXTRA_MODE, mode);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onStringDataWritten(String data)
            {
                Log.d(TAG, "onStringDataWritten - " + data);

                Intent intent = new Intent(ACTION_STRING_DATA_WRITE);
                intent.putExtra(EXTRA_DATA, data);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onBinaryDataWritten(byte[] data)
            {
                Log.d(TAG, "onBinaryDataWritten - Wrote " + data.length + " bytes");

                Intent intent = new Intent(ACTION_BINARY_DATA_WRITE);
                intent.putExtra(EXTRA_DATA, data);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onStringDataRead(String data)
            {
                Log.d(TAG, "onDataRead - " + data);

                Intent intent = new Intent(ACTION_STRING_DATA_READ);
                intent.putExtra(EXTRA_DATA, data);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onBinaryDataRead(byte[] data)
            {
                Log.d(TAG, "onBinaryDataRead");

                Intent intent = new Intent(ACTION_BINARY_DATA_READ);
                intent.putExtra(EXTRA_DATA, data);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onCommandSent(int ID, TruconnectCommand command)
            {
                Log.d(TAG, "onCommandSent");

                Intent intent = new Intent(ACTION_COMMAND_SENT);
                intent.putExtra(EXTRA_ID, ID);
                intent.putExtra(EXTRA_COMMAND, command);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onCommandResult(int ID, TruconnectCommand command, TruconnectResult result)
            {
                Log.d(TAG, "onCommandResult");

                Intent intent = new Intent(ACTION_COMMAND_RESULT);
                intent.putExtra(EXTRA_COMMAND, command);

                if (result != null)
                {
                    intent.putExtra(EXTRA_ID, ID);
                    intent.putExtra(EXTRA_RESPONSE_CODE, result.getResponseCode());
                    intent.putExtra(EXTRA_DATA, result.getData());
                }

                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onError(TruconnectErrorCode error)
            {
                Intent intent = new Intent(ACTION_ERROR);
                intent.putExtra(EXTRA_ERROR, error);
                mBroadcastManager.sendBroadcast(intent);

                Log.d(TAG, "onError - " + error);

            }
        };
    }

    public static int getMode(Intent intent)
    {
        return intent.getIntExtra(EXTRA_MODE, 0);
    }

    public static String getData(Intent intent)
    {
        return intent.getStringExtra(EXTRA_DATA);
    }

    public static byte[] getBinaryData(Intent intent)
    {
        return intent.getByteArrayExtra(EXTRA_DATA);
    }

    public static int getID(Intent intent)
    {
        return intent.getIntExtra(EXTRA_ID, TruconnectManager.ID_INVALID);
    }

    public static TruconnectCommand getCommand(Intent intent)
    {
        return (TruconnectCommand)intent.getSerializableExtra(EXTRA_COMMAND);
    }

    public static int getResponseCode(Intent intent)
    {
        return intent.getIntExtra(EXTRA_RESPONSE_CODE, -1);
    }

    public static TruconnectErrorCode getErrorCode(Intent intent)
    {
        return (TruconnectErrorCode)intent.getSerializableExtra(EXTRA_ERROR);
    }

    public static String getVersion(Intent intent)
    {
        return intent.getStringExtra(EXTRA_VERSION);
    }

    public static String getDeviceName(Intent intent)
    {
        return intent.getStringExtra(EXTRA_NAME);
    }

    public static int getIntData(Intent intent)
    {
        return intent.getIntExtra(EXTRA_DATA, -1);
    }

    public static byte getByteData(Intent intent)
    {
        return intent.getByteExtra(EXTRA_DATA, (byte) 0);
    }
}