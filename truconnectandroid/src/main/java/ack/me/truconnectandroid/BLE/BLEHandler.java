/* TruConnect Android Library & Example Applications
*
* Copyright (C) 2015, Sensors.com,  Inc. All Rights Reserved.
*
* The TruConnect Android Library and TruConnect example applications are provided free of charge by
* Sensors.com. The combined source code, and all derivatives, are licensed by Sensors.com SOLELY
* for use with devices manufactured by ACKme Networks, or devices approved by Sensors.com.
*
* Use of this software on any other devices or hardware platforms is strictly prohibited.
*
* THIS SOFTWARE IS PROVIDED BY THE AUTHOR AS IS AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
* BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
* PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
* LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package ack.me.truconnectandroid.BLE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.UUID;

import ack.me.truconnectandroid.OTA.OTAHandler;
import ack.me.truconnectandroid.truconnect.TruconnectReceiveMode;

public class BLEHandler implements Serializable
{
    private static final String TAG = "BLEHandler";

    private BluetoothAdapter.LeScanCallback mScanCallback;//for android SDK
    private BluetoothCallbackHandler mBluetoothCallbacks;//for android SDK
    private BLEHandlerCallbacks mCallbacks;//callbacks for user

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private Context mContext;

    private BLEDeviceList mScannedList;
    private SearchableList<BLEConnection> mConnectionList;

    private boolean mScanning = false;

    private BLECommandQueue mQueue;
    private boolean mCommandInProgress = false;

    private TruconnectReceiveMode mReceiveMode = TruconnectReceiveMode.STRING;

    public BLEHandler (BLEDeviceList scanList, SearchableList<BLEConnection> connectedList, BLECommandQueue queue)
    {
        mScannedList = scanList;
        mConnectionList = connectedList;
        mQueue = queue;
    }

    public boolean init(Context context, BLEHandlerCallbacks callbacks, OTAHandler otaHandler)
    {
        boolean result = false;

        mScanning = false;
        mContext = context;
        mCallbacks = callbacks;
        mBluetoothCallbacks = new BluetoothCallbackHandler(this, callbacks, otaHandler);

        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        if (mBluetoothManager != null)
        {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter != null)
                result = true;
        }

        return result;
    }

    public boolean isInitialised()
    {
        return (mBluetoothAdapter != null);
    }

    public boolean isConnected(String deviceName)
    {
        BLEConnection connection = getConnection(deviceName);

        if (connection != null)
        {
            return (connection.getMode() == BLEConnection.Mode.CONNECTED);
        }
        else
        {
            return false;
        }
    }

    public boolean isBLEEnabled()
    {
        return (isInitialised() && mBluetoothAdapter.isEnabled());
    }

    public boolean isScanning()
    {
        return mScanning;
    }

    public boolean startBLEScan()
    {
        boolean result = false;
        final UUID TC_UUIDS[] = {BluetoothCallbackHandler.TRUCONNECT_SERVICE_UUID};

        if (isBLEEnabled() && !mScanning)
        {
            mScannedList.clear();
            result = mBluetoothAdapter.startLeScan(mBluetoothCallbacks.getScanCallback());
            mScanning = true;
        }
        return result;
    }

    public boolean stopBLEScan()
    {
        boolean result = false;

        if (isInitialised() && mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.stopLeScan(mBluetoothCallbacks.getScanCallback());
            mScanning = false;
            result = true;
        }

        return result;
    }

    /* Returns true if connection request was successful. The connection is not established until
     * the onConnect callback is called. */
    public boolean connect(String deviceName, boolean autoReconnect)
    {
        boolean result = false;
        BLEDevice device = mScannedList.get(deviceName);

        if (device != null)
        {
            BLEConnection newConnection = new BLEConnection();
            newConnection.setDevice(device.getDevice());
            newConnection.setMode(BLEConnection.Mode.CONNECTING);

            if (mConnectionList.add(newConnection))
            {
                BLEGatt gatt = device.connectGatt(mContext, autoReconnect,
                        mBluetoothCallbacks.getGattCallbacks());

                if (gatt != null)
                {
                    result = true;
                }
                else
                {
                    mConnectionList.remove(newConnection);
                }
            }
        }

        return result;
    }

    public boolean disconnect(String deviceName, boolean disableTxNotification)
    {
        boolean successful = false;

        if (deviceName != null)
        {
            BLEConnection connection = mConnectionList.get(deviceName);
            if (connection != null)
            {
                mQueue.clear();
                mCommandInProgress = false;
                connection.setMode(BLEConnection.Mode.DISCONNECTING);

                if (disableTxNotification)//some devices need this done, but dont want to on error
                {
                    mQueue.add(connection, BLECommandType.SET_TX_NOTIFY_DISABLE);
                }

                mQueue.add(connection, BLECommandType.DISCONNECT);
                successful = true;
                triggerNextCommand();
            }
        }

        return successful;
    }

    public void deinit()
    {
        for (int i=0; i < mConnectionList.size(); i++)
        {
            mConnectionList.get(i).close();
        }

        mConnectionList.clear();

        if (mScanning)
        {
            mBluetoothAdapter.stopLeScan(mScanCallback);
        }

        mBluetoothAdapter = null;
    }

    public boolean readStringData(String deviceName)
    {
        return addCommandAndTrigger(deviceName, BLECommandType.READ_DATA_STR);
    }

    public boolean writeData(String deviceName, String data)
    {
        boolean result = false;

        if (deviceName != null)
        {
            BLEConnection connection = getConnection(deviceName);

            if (connection != null && data != null)
            {
                result = mQueue.add(connection, BLECommandType.WRITE_DATA_STR, data);
                triggerNextCommand();
            }
        }

        return result;
    }

    public boolean readBinaryData(String deviceName)
    {
        return addCommandAndTrigger(deviceName, BLECommandType.READ_DATA_BIN);
    }

    public boolean writeData(String deviceName, byte[] data)
    {
        boolean result = false;

        if (deviceName != null)
        {
            BLEConnection connection = getConnection(deviceName);

            if (connection != null && data != null)
            {
                result = mQueue.add(connection, BLECommandType.WRITE_DATA_BIN, data);
                triggerNextCommand();
            }
        }

        return result;
    }

    public boolean writeMode(String deviceName, int mode)
    {
        boolean result = false;

        if (deviceName != null)
        {
            BLEConnection connection = getConnection(deviceName);

            if (connection != null && BluetoothCallbackHandler.isModeValid(mode))
            {
                result = mQueue.add(connection, BLECommandType.WRITE_MODE, mode);
                triggerNextCommand();
            }
        }

        return result;
    }

    public boolean readMode(String deviceName)
    {
        return addCommandAndTrigger(deviceName, BLECommandType.READ_MODE);
    }

    public boolean setTxNotify(String deviceName, boolean enable)
    {
        BLECommandType type;

        if(enable)
        {
            type = BLECommandType.SET_TX_NOTIFY_ENABLE;
        }
        else
        {
            type = BLECommandType.SET_TX_NOTIFY_DISABLE;
        }

        return addCommandAndTrigger(deviceName, type);
    }

    public boolean setOTAControlNotify(String deviceName, boolean enable)
    {
        BLECommandType type;

        if(enable)
        {
            type = BLECommandType.SET_OTA_CONTROL_NOTIFY_ENABLE;
        }
        else
        {
            type = BLECommandType.SET_OTA_CONTROL_NOTIFY_DISABLE;
        }

        return addCommandAndTrigger(deviceName, type);
    }

    public boolean readFirmwareRev(String deviceName)
    {
        return addCommandAndTrigger(deviceName, BLECommandType.READ_VERSION);
    }

    public boolean writeOTAControl(String deviceName, byte[] data)
    {
        boolean result = false;

        if (deviceName != null)
        {
            BLEConnection connection = getConnection(deviceName);

            if (connection != null && data != null)
            {
                result = mQueue.add(connection, BLECommandType.WRITE_OTA_CONTROL, data);
                triggerNextCommand();
            }
        }

        return result;
    }

    public boolean writeOTAData(String deviceName, byte[] data, int offset)
    {
        boolean result = false;

        if (deviceName != null)
        {
            BLEConnection connection = getConnection(deviceName);

            if (connection != null && data != null)
            {
                result = mQueue.add(connection, BLECommandType.WRITE_OTA_DATA, data, offset);
                triggerNextCommand();
            }
        }

        return result;
    }

    public void abortBinaryWrite()
    {
        mBluetoothCallbacks.abortBinaryWrite();
    }

    public TruconnectReceiveMode getReceiveMode()
    {
        return mReceiveMode;
    }

    public boolean setReceiveMode(TruconnectReceiveMode mode)
    {
        boolean result = false;

        if (!mCommandInProgress)
        {
            mReceiveMode = mode;
            result = true;
        }

        return result;
    }

    //Starts processing next command if there is none being processed
    synchronized private void triggerNextCommand()
    {
        if (!mCommandInProgress)
        {
            Log.d(TAG, "Triggering next BLE command");
            mCommandInProgress = true;
            mBluetoothCallbacks.processNextCommand();
        }
        else
        {
            Log.d(TAG, "Command in progress, not triggering");
        }
    }

    protected BLEDeviceList getScanned()
    {
        return mScannedList;
    }

    protected BLEConnection getConnection(String deviceName)
    {
        BLEConnection connection = null;

        if (deviceName != null && mConnectionList != null)
        {
            connection = mConnectionList.get(deviceName);
        }

        return connection;
    }

    protected boolean removeConnection(String deviceName)
    {
        boolean result = false;

        if (deviceName != null && mConnectionList != null)
        {
            BLEConnection connection = mConnectionList.get(deviceName);
            if (connection != null)
            {
                connection.close();
                result = mConnectionList.remove(connection);
            }
        }

        return result;
    }

    protected BLEConnection.Mode getConnectionMode(String deviceName)
    {
        BLEConnection connection = mConnectionList.get(deviceName);
        BLEConnection.Mode mode = null;

        if(connection != null)
        {
            mode = connection.getMode();
        }

        return mode;
    }

    protected BLECommand getNextCommand()
    {
        try
        {
            return mQueue.next();
        }
        catch (NoSuchElementException e)
        {
            return null;
        }
    }

    protected boolean addCommand(BLECommand command)
    {
        return mQueue.add(command);
    }

    protected boolean addCommand(BLEConnection connection, BLECommandType type)
    {
        return mQueue.add(new BLECommand(connection, type));
    }

    protected void setCommandInProgress(boolean inProgress)
    {
        mCommandInProgress = inProgress;
    }

    private boolean addCommandAndTrigger(String deviceName, BLECommandType cmdType)
    {
        boolean result = false;

        if (deviceName != null)
        {
            BLEConnection connection = getConnection(deviceName);

            if(connection != null)
            {
                result = mQueue.add(connection, cmdType);
                triggerNextCommand();
            }
        }

        return result;
    }
}
