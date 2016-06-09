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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import ack.me.truconnectandroid.OTA.FirmwareVersion;
import ack.me.truconnectandroid.OTA.OTAHandler;
import ack.me.truconnectandroid.OTA.OTAStatus;
import ack.me.truconnectandroid.truconnect.TruconnectReceiveMode;

public class BluetoothCallbackHandler implements Serializable
{
    public static final String TAG = "BluetoothCallbkHandler";

    public static final UUID TRUCONNECT_SERVICE_UUID =
                                            UUID.fromString("175f8f23-a570-49bd-9627-815a6a27de2a");
    public static final UUID TX_UUID = UUID.fromString("cacc07ff-ffff-4c48-8fae-a9ef71b75e26");
    public static final UUID RX_UUID = UUID.fromString("1cce1ea8-bd34-4813-a00a-c76e028fadcb");
    public static final UUID MODE_UUID = UUID.fromString("20b9794f-da1a-4d14-8014-a0fb9cefb2f7");

    public static final UUID OTA_SERVICE_UPGRADE_UUID =
                                            UUID.fromString("b2e7d564-c077-404e-9d29-b547f4512dce");

    public static final UUID OTA_CHAR_UPGRADE_CONTROL_POINT_UUID =
                                            UUID.fromString("48cbe15e-642d-4555-ac66-576209c50c1e");

    public static final UUID OTA_CHAR_DATA_UUID =
                                            UUID.fromString("db96492d-cf53-4a43-b896-14cbbf3bf4f3");

    public static final UUID CHAR_FIRMWARE_REV_UUID = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");

    public static final UUID SERVICE_DEVICE_INFORMATION_UUID = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");

    public static final int SERVICE_STATUS_NONE = 0;
    public static final int SERVICE_STATUS_TRUCONNECT_ONLY = 1;
    public static final int SERVICE_STATUS_OTA_ONLY = 2;
    public static final int SERVICE_STATUS_BOTH = 3;

    private final BLEHandlerCallbacks.Result ERROR_DISCOVERY =
                                                      BLEHandlerCallbacks.Result.SERVICE_DISC_ERROR;

    private final BLEHandlerCallbacks.Result ERROR_CONNECTION =
                                                         BLEHandlerCallbacks.Result.CONNECT_FAILURE;

    private final BLEHandlerCallbacks.Error ERROR_CON_NO_REQ =
                                                  BLEHandlerCallbacks.Error.CONNECT_WITHOUT_REQUEST;

    private final BLEHandlerCallbacks.Error ERROR_DISCON_NO_REQ =
                                               BLEHandlerCallbacks.Error.DISCONNECT_WITHOUT_REQUEST;

    private final BLEHandlerCallbacks.Error ERROR_INVALID_MODE =
                                                             BLEHandlerCallbacks.Error.INVALID_MODE;

    private final BLEHandlerCallbacks.Error ERROR_NO_TX_CHAR =
                                                     BLEHandlerCallbacks.Error.NO_TX_CHARACTERISTIC;

    private final BLEHandlerCallbacks.Error ERROR_NO_RX_CHAR =
                                                     BLEHandlerCallbacks.Error.NO_RX_CHARACTERISTIC;

    private final BLEHandlerCallbacks.Error ERROR_NO_MODE_CHAR =
                                                   BLEHandlerCallbacks.Error.NO_MODE_CHARACTERISTIC;

    private final BLEHandlerCallbacks.Error ERROR_NO_CONNECTION =
                                                      BLEHandlerCallbacks.Error.NO_CONNECTION_FOUND;

    private final BLEHandlerCallbacks.Error ERROR_NULL_GATT =
                                                    BLEHandlerCallbacks.Error.NULL_GATT_ON_CALLBACK;

    private final BLEHandlerCallbacks.Error ERROR_NULL_CHAR =
                                                    BLEHandlerCallbacks.Error.NULL_CHAR_ON_CALLBACK;
    private BluetoothAdapter.LeScanCallback mScanCallback;
    private BluetoothGattCallback mGattCallbacks;
    private BLEHandlerCallbacks mBLECallbacks;

    private BLEHandler mHandler;
    private OTAHandler mOtaHandler;

    private static final int MODES_EQUAL = 0;

    public static final int MODE_STREAM = 1;
    public static final int MODE_LOCAL_COMMAND = 2;
    public static final int MODE_REMOTE_COMMAND = 3;

    private enum DataType { DATA_BINARY, DATA_STRING }

    private DataType dataType;

    private final int FORMAT_MODE = BluetoothGattCharacteristic.FORMAT_UINT8;
    private final int OFFSET_MODE = 0;
    private final int OFFSET_RX = 0;
    private final int OFFSET_TX = 0;
    private final int OFFSET_FW_REV = 0;
    private final String DATA_NONE = "";

    private static final boolean IS_OTA_DATA = true;

    private final int PACKET_SIZE_MAX = 20;
    private String mWriteBuffer = "";//store strings too large for single RX char write
    private String mLastStringPacket = "";//store strings too large for single RX char write

    private byte[] mBinaryBuffer;
    private int mBinaryOffset = 0;
    private int mBinaryBufferPointer = 0;
    private boolean mAbortBinaryWrite = false;
    private Object mAbortLock = new Object();

    private final int RETRY_COUNT_MAX = 3;//max # of retries permitted per packet
    private int mRetryCount = 0;

    private BLEConnection mCurrentConnection;

    private byte[] mLastBinaryPacket;

    private static final int WRITE_FAIL = -1;
    private static final int WRITE_SUCCESS = 0;
    private static final int WRITE_NO_MORE_DATA = 1;


    BluetoothCallbackHandler(BLEHandler handler, final BLEHandlerCallbacks BLECallbacks,
                             OTAHandler otaHandler)
    {
        mHandler = handler;
        mBLECallbacks = BLECallbacks;
        mOtaHandler = otaHandler;

        mScanCallback = new BluetoothAdapter.LeScanCallback()
        {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
            {
                BLEDeviceList list = mHandler.getScanned();

                list.add(new BLEDevice(device));
                mBLECallbacks.onScanResult(device.getName());
            }
        };

        mGattCallbacks = new BluetoothGattCallback()
        {

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status)
            {
                Log.d(TAG, "onServicesDiscovered");
                String deviceName = gatt.getDevice().getName();

                if (status != BluetoothGatt.GATT_SUCCESS)
                {
                    mBLECallbacks.onConnectFailed(deviceName, ERROR_DISCOVERY);
                }
                else
                {
                    getServices(gatt);
                }

                processNextCommand();
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                Log.d(TAG, "onConnectionStateChanged");
                if (verifyGatt(gatt))
                {
                    String deviceName = gatt.getDevice().getName();
                    BLEConnection.Mode currentMode = mHandler.getConnectionMode(deviceName);

                    if (currentMode == null)
                    {
                        mBLECallbacks.onError(deviceName, ERROR_INVALID_MODE, DATA_NONE);
                        return;
                    }

                    switch(currentMode)
                    {
                        case CONNECTED:
                            if (newState == BluetoothGatt.STATE_DISCONNECTED)
                            {
                                mHandler.removeConnection(deviceName);
                                mBLECallbacks.onError(deviceName, ERROR_DISCON_NO_REQ, DATA_NONE);
                                resetBuffers();//abort any writes taking place
                            }
                            break;

                        case DISCONNECTED:
                            if (newState == BluetoothGatt.STATE_CONNECTED)
                                mBLECallbacks.onError(deviceName, ERROR_CON_NO_REQ, DATA_NONE);
                            break;

                        case CONNECTING:
                            if(newState == BluetoothGatt.STATE_CONNECTED)
                            {
                                resetBuffers();

                                if(status == BluetoothGatt.GATT_SUCCESS)
                                {
                                    BLEConnection connection = mHandler.getConnection(deviceName);
                                    setConnectionGatt(connection, gatt);
                                    connection.setMode(BLEConnection.Mode.CONNECTED);

                                    BLECommand serv_disc_cmd = new BLECommand(connection,
                                            BLECommandType.DISCOVER_SERVICES);
                                    if(!mHandler.addCommand(serv_disc_cmd))
                                    {
                                        mBLECallbacks.onConnectFailed(deviceName, ERROR_DISCOVERY);
                                    }
                                }
                                else
                                    mBLECallbacks.onConnectFailed(deviceName, ERROR_CONNECTION);
                            }
                            else
                            {
                                if (status == BluetoothGatt.GATT_SUCCESS)
                                    mBLECallbacks.onError(deviceName, ERROR_CON_NO_REQ, DATA_NONE);
                                else
                                    mBLECallbacks.onConnectFailed(deviceName, ERROR_CONNECTION);
                            }
                            break;

                        case DISCONNECTING:
                            if (newState == BluetoothGatt.STATE_DISCONNECTED)
                            {
                                mHandler.removeConnection(deviceName);
                                resetBuffers();

                                if (status == BluetoothGatt.GATT_SUCCESS)
                                {
                                    mBLECallbacks.onDisconnect(deviceName);
                                }
                                else
                                    mBLECallbacks.onDisconnectFailed(deviceName);
                            }
                            else
                            {
                                if (status == BluetoothGatt.GATT_SUCCESS)
                                    mBLECallbacks.onError(deviceName, ERROR_CON_NO_REQ, DATA_NONE);
                                else
                                    mBLECallbacks.onDisconnectFailed(deviceName);
                            }
                            break;

                        default:
                            resetBuffers();
                            mBLECallbacks.onError(deviceName, ERROR_INVALID_MODE, DATA_NONE);
                            break;
                    }
                }

                processNextCommand();
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                Log.d(TAG, "onCharacteristicRead");
                if (verifyGatt(gatt) && verifyCharacteristic(characteristic))
                {
                    String deviceName = gatt.getDevice().getName();
                    UUID uuid = characteristic.getUuid();

                    if (uuid.compareTo(MODE_UUID) == MODES_EQUAL)
                    {
                        if (status == BluetoothGatt.GATT_SUCCESS)
                        {
                            int mode = characteristic.getIntValue(FORMAT_MODE, OFFSET_MODE);
                            mBLECallbacks.onModeRead(deviceName, mode);
                        }
                        else
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.MODE_READ_FAILED, DATA_NONE);
                        }
                    }
                    else if (uuid.compareTo(TX_UUID) == MODES_EQUAL)
                    {
                        if (status == BluetoothGatt.GATT_SUCCESS)
                        {
                            if (dataType == DataType.DATA_STRING)
                            {
                                String data = characteristic.getStringValue(OFFSET_RX);
                                mBLECallbacks.onStringDataRead(deviceName, data);
                            }
                            else
                            {
                                byte[] data = characteristic.getValue();
                                mBLECallbacks.onBinaryDataRead(deviceName, data);
                            }
                        }
                        else
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.DATA_READ_FAILED, DATA_NONE);
                        }
                    }
                    else if (uuid.compareTo(CHAR_FIRMWARE_REV_UUID) == MODES_EQUAL)
                    {
                        String versionString = characteristic.getStringValue(OFFSET_FW_REV);
                        FirmwareVersion version = new FirmwareVersion(versionString);
                        mBLECallbacks.onFirmwareVersionRead(deviceName, version);
                        mOtaHandler.onFirmwareRevReceive(version);
                    }
                }

                processNextCommand();
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                boolean processNextCommand = true;

                Log.d(TAG, "onCharacteristicWrite");
                if (verifyGatt(gatt) && verifyCharacteristic(characteristic))
                {
                    String deviceName = gatt.getDevice().getName();
                    UUID uuid = characteristic.getUuid();

                    if(uuid.compareTo(MODE_UUID) == MODES_EQUAL)
                    {
                        if (status == BluetoothGatt.GATT_SUCCESS)
                        {
                            int mode = characteristic.getIntValue(FORMAT_MODE, OFFSET_MODE);
                            mBLECallbacks.onModeChanged(deviceName, mode);
                        }
                        else
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.MODE_WRITE_FAILED, DATA_NONE);
                        }
                    }
                    else if (uuid.compareTo(RX_UUID) == MODES_EQUAL)
                    {
                        if (dataType == DataType.DATA_STRING)
                        {
                            if (status == BluetoothGatt.GATT_SUCCESS)
                            {
                                mRetryCount = 0;
                                String data = characteristic.getStringValue(OFFSET_TX);
                                if (!mLastStringPacket.contentEquals(data))
                                {
                                    clearWriteBuffer();
                                    mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.DATA_WRITE_FAILED, data);
                                }
                                else
                                {
                                    mBLECallbacks.onStringDataWrite(deviceName, data);
                                    if(!writeNextPacket(mCurrentConnection))
                                    {
                                        mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.DATA_WRITE_FAILED, DATA_NONE);
                                    }
                                }
                            }
                            else if (!retryLastStringWrite(mCurrentConnection))
                            {
                                mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.DATA_WRITE_FAILED, DATA_NONE);
                            }
                        }
                        else//binary data
                        {
                            if (status == BluetoothGatt.GATT_SUCCESS)
                            {
                                mRetryCount = 0;
                                byte[] data = characteristic.getValue();
                                mBLECallbacks.onBinaryDataWrite(deviceName, data);

                                int writeResult = writeNextBinaryPacket(mCurrentConnection, !IS_OTA_DATA);
                                if (writeResult == WRITE_FAIL)
                                {
                                    mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.DATA_WRITE_FAILED, DATA_NONE);
                                }
                                else if (writeResult == WRITE_SUCCESS)
                                {
                                    processNextCommand = false;//still more data to send
                                }
                            }
                            else if (!retryLastBinaryWrite(mCurrentConnection, false))
                            {
                                mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.DATA_WRITE_FAILED, DATA_NONE);
                            }
                        }
                    }
                    else if (uuid.compareTo(OTA_CHAR_DATA_UUID) == MODES_EQUAL)
                    {
                        if (status == BluetoothGatt.GATT_SUCCESS)
                        {
                            mRetryCount = 0;
                            mOtaHandler.onOTADataSent(characteristic.getValue());
                            int writeResult = writeNextBinaryPacket(mCurrentConnection, IS_OTA_DATA);
                            if (writeResult == WRITE_FAIL)
                            {
                                mOtaHandler.onError(new OTAStatus(OTAStatus.COMMUNICATION_ERROR));
                            }
                            else if (writeResult == WRITE_SUCCESS)
                            {
                                processNextCommand = false;//still more data to send
                            }
                        }
                        else
                        {
                            //if retry failed or exhausted all attempts
                            if (!retryLastBinaryWrite(mCurrentConnection, true))
                            {
                                mOtaHandler.onError(new OTAStatus(OTAStatus.COMMUNICATION_ERROR));
                            }
                        }
                    }
                }

                if (processNextCommand)
                {
                    processNextCommand();
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
            {
                Log.d(TAG, "onCharacteristicChanged");
                if (verifyGatt(gatt) && verifyCharacteristic(characteristic))
                {
                    UUID uuid = characteristic.getUuid();
                    if (uuid.compareTo(TX_UUID) == MODES_EQUAL)
                    {
                        TruconnectReceiveMode currentMode = mHandler.getReceiveMode();
                        if (currentMode == TruconnectReceiveMode.STRING)
                        {
                            String data = characteristic.getStringValue(OFFSET_TX);
                            mBLECallbacks.onStringDataRead(gatt.getDevice().getName(), data);
                        }
                        else if (currentMode == TruconnectReceiveMode.BINARY)
                        {
                            byte[] data = characteristic.getValue();
                            mBLECallbacks.onBinaryDataRead(gatt.getDevice().getName(), data);
                        }
                    }
                    else if (uuid.compareTo(OTA_CHAR_UPGRADE_CONTROL_POINT_UUID) == MODES_EQUAL)
                    {
                        mOtaHandler.onOTAControlReceive(characteristic.getValue());
                    }
                }
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
            {
                Log.d(TAG, "onDescriptorRead");
                processNextCommand();
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
            {
                Log.d(TAG, "onDescriptorWrite");
                processNextCommand();
            }
        };
    }

    public BluetoothAdapter.LeScanCallback getScanCallback()
    {
        return mScanCallback;
    }

    public BluetoothGattCallback getGattCallbacks()
    {
        return mGattCallbacks;
    }

    public static boolean isModeValid(int mode)
    {
        return (mode >= MODE_STREAM && mode <= MODE_REMOTE_COMMAND);
    }

    private void resetBuffers()
    {
        mAbortBinaryWrite = false;
        resetBinaryBuffer();
        mRetryCount = 0;
        mLastBinaryPacket = null;
        mWriteBuffer = "";
        mLastStringPacket = "";
    }

    public void abortBinaryWrite()
    {
        synchronized (mAbortLock)
        {
            mAbortBinaryWrite = true;
        }
    }

    private boolean setConnectionGatt(BLEConnection connection, BluetoothGatt gatt)
    {
        boolean result = false;

        if (connection != null)
        {
            connection.setGatt(gatt);
            result = true;
        }

        return result;
    }

    private void getServices(BluetoothGatt gatt)
    {
        String deviceName = BLEDevice.getNameFromDevice(gatt.getDevice());
        BLEConnection connection = mHandler.getConnection(deviceName);

        if (connection == null)
        {
            mBLECallbacks.onError(deviceName, ERROR_NO_CONNECTION, DATA_NONE);
        }
        else
        {
            boolean error = true;
            List<BluetoothGattService> services = gatt.getServices();

            if (services != null)
            {
                Iterator<BluetoothGattService> iterator = services.iterator();
                if (iterator != null)
                {
                    error = false;

                    while (iterator.hasNext())
                    {
                        BluetoothGattService service = iterator.next();

                        if (service != null)
                        {
                            setConnectionCharacterisitics(connection, service);
                        }
                    }

                    boolean hasTruconnectChars = connection.hasTruconnectCharacteristics();
                    boolean hasOTAChars = connection.hasOTACharacteristics();

                    //if no truconnect and no OTA characteristics, cant talk to device
                    if (!hasTruconnectChars && !hasOTAChars)
                    {
                        mBLECallbacks.onConnectFailed(deviceName, ERROR_DISCOVERY);
                    }
                    else
                    {
                        int serviceStatus = getStatus(hasTruconnectChars, hasOTAChars);
                        mBLECallbacks.onConnect(deviceName, serviceStatus);
                    }
                }
            }

            if (error)
            {
                mBLECallbacks.onConnectFailed(deviceName, ERROR_DISCOVERY);
            }
        }
    }

    private int getStatus(boolean hasTruconnectChars, boolean hasOTAChars)
    {
        int status = SERVICE_STATUS_NONE;

        if (hasTruconnectChars)
        {
            if (hasOTAChars)
            {
                status = SERVICE_STATUS_BOTH;
            }
            else
            {
                status = SERVICE_STATUS_TRUCONNECT_ONLY;
            }
        }
        else
        {
            if (hasOTAChars)
            {
                status = SERVICE_STATUS_OTA_ONLY;
            }
        }

        return status;
    }

    private void setConnectionCharacterisitics(BLEConnection connection, BluetoothGattService service)
    {
        //connection and service are null checked before this function
        UUID uuid = service.getUuid();

        if (uuid != null)
        {
            if (service.getUuid().compareTo(OTA_SERVICE_UPGRADE_UUID) == 0)
            {
                setOTACharacteristics(connection, service);
            }
            else if (service.getUuid().compareTo(SERVICE_DEVICE_INFORMATION_UUID) == 0)
            {
                setFirmwareRevCharacteristics(connection, service);
            }
            else if (hasTruconnectCharacteristics(service))
            {
                setTruconnectCharacteristics(connection, service);
            }
        }
    }

    private boolean hasTruconnectCharacteristics(BluetoothGattService service)
    {
        return ((service.getCharacteristic(TX_UUID) != null) &&
                (service.getCharacteristic(RX_UUID) != null) &&
                (service.getCharacteristic(MODE_UUID) != null));
    }

    private void setTruconnectCharacteristics(BLEConnection connection, BluetoothGattService service)
    {
        String name = connection.getDevice().getName();

        BluetoothGattCharacteristic txChar = service.getCharacteristic(TX_UUID);
        BluetoothGattCharacteristic rxChar = service.getCharacteristic(RX_UUID);
        BluetoothGattCharacteristic modeChar = service.getCharacteristic(MODE_UUID);

        if (txChar == null)
        {
            mBLECallbacks.onError(name, ERROR_NO_TX_CHAR, DATA_NONE);
        }
        else
        {
            connection.setTxCharacteristic(txChar);
            mHandler.addCommand(connection, BLECommandType.SET_TX_NOTIFY_ENABLE);
        }

        if (rxChar == null)
        {
            mBLECallbacks.onError(name, ERROR_NO_RX_CHAR, DATA_NONE);
        }
        else
        {
            connection.setRxCharacteristic(rxChar);
        }

        if (modeChar == null)
        {
            mBLECallbacks.onError(name, ERROR_NO_MODE_CHAR, DATA_NONE);
        }
        else
        {
            connection.setModeCharacteristic(modeChar);
        }
    }

    private void setOTACharacteristics(BLEConnection connection, BluetoothGattService service)
    {
        BluetoothGattCharacteristic controlChar = service.getCharacteristic(OTA_CHAR_UPGRADE_CONTROL_POINT_UUID);
        BluetoothGattCharacteristic dataChar = service.getCharacteristic(OTA_CHAR_DATA_UUID);

        if (controlChar != null)
        {
            connection.setOTAControlCharacteristic(controlChar);
        }

        if (dataChar != null)
        {
            connection.setOTADataCharacteristic(dataChar);
        }


    }

    private void setFirmwareRevCharacteristics(BLEConnection connection, BluetoothGattService service)
    {
        BluetoothGattCharacteristic firmwareRevChar = service.getCharacteristic(CHAR_FIRMWARE_REV_UUID);

        if (firmwareRevChar != null)
        {
            connection.setFirmwareRevCharacteristic(firmwareRevChar);
        }
    }

    //calls BLEHandlerCallbacks.onError() and returns false if Gatt invalid
    private boolean verifyGatt(BluetoothGatt gatt)
    {
        boolean valid = true;

        if (gatt == null)
        {
            mBLECallbacks.onError("", ERROR_NULL_GATT, DATA_NONE);
            valid = false;
        }

        return valid;
    }

    //calls BLEHandlerCallbacks.onError() and returns false if characteristic invalid
    private boolean verifyCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        boolean valid = true;

        if (characteristic == null)
        {
            mBLECallbacks.onError("", ERROR_NULL_CHAR, DATA_NONE);
            valid = false;
        }

        return valid;
    }

    private boolean writeStringData(BLEConnection connection, String data)
    {
        addToWriteBuffer(data);

        mCurrentConnection = connection;
        return writeNextPacket(connection);
    }

    private int writeBinaryData(BLEConnection connection, byte[] data, boolean isOtaData)
    {
        if (mBinaryBuffer != null)
            return WRITE_FAIL;
        else
        {
            mBinaryBuffer = data;
            mBinaryBufferPointer = 0;
            mCurrentConnection = connection;
            return writeNextBinaryPacket(connection, isOtaData);
        }
    }

    private synchronized void addToWriteBuffer(String data)
    {
        mWriteBuffer = mWriteBuffer.concat(data);
    }

    private synchronized void clearWriteBuffer()
    {
        mWriteBuffer = "";
    }

    private String getNextDataPacket()
    {
        String packet;

        if (mWriteBuffer.length() <= PACKET_SIZE_MAX)
        {
            packet = mWriteBuffer;
            clearWriteBuffer();
        }
        else
        {
            packet = mWriteBuffer.substring(0, PACKET_SIZE_MAX);
            mWriteBuffer = mWriteBuffer.substring(PACKET_SIZE_MAX);
        }

        return packet;
    }

    private byte[] getNextBinaryDataPacket()
    {
        int length = PACKET_SIZE_MAX;

        if (mBinaryBufferPointer == 0)
        {
            mBinaryBufferPointer = this.mBinaryOffset;
        }

        int remainingBytes = getRemainingBytes();

        if (remainingBytes < PACKET_SIZE_MAX)
        {
            if (remainingBytes == 0)
                return null;
            else
                length = remainingBytes;
        }

        byte[] packet = new byte[length];

        for (int i=0; i < length; i++)
        {
            packet[i] = mBinaryBuffer[i+mBinaryBufferPointer];
        }

        mBinaryBufferPointer += length;

        return packet;
    }

    //returns number of binary data bytes left to write
    private int getRemainingBytes()
    {
        return mBinaryBuffer.length - mBinaryBufferPointer;
    }

    private boolean writeNextPacket(BLEConnection connection)
    {
        boolean result = true;
        String packet = getNextDataPacket();

        mLastStringPacket = packet;
        if (!packet.isEmpty())
        {
            result = writeStringPacket(connection, packet);
        }

        return result;
    }

    private boolean writeStringPacket(BLEConnection connection, String packet)
    {
        boolean result = false;

        if (connection != null)
        {
            result = connection.writeRxCharacteristic(packet);
        }

        return result;
    }

    private boolean retryLastStringWrite(BLEConnection connection)
    {
        boolean result = false;

        if (mRetryCount < RETRY_COUNT_MAX)
        {
            mRetryCount++;
            if (mLastStringPacket != null)
            {
                result = writeStringPacket(connection, mLastStringPacket);
            }
        }

        return result;
    }

    private int writeNextBinaryPacket(BLEConnection connection, boolean isOTAData)
    {
        int result = WRITE_SUCCESS;
        byte[] packet = getNextBinaryDataPacket();

        synchronized (mAbortLock)
        {
            if (mAbortBinaryWrite)
            {
                mAbortBinaryWrite = false;//reset flag

                mBinaryBuffer = null;
                mBinaryBufferPointer = 0;

                return WRITE_NO_MORE_DATA;
            }
        }

        if (packet != null)
        {
            mLastBinaryPacket = packet;

            if (!writeBinaryPacket(connection, isOTAData, packet))
            {
                result = WRITE_FAIL;
            }
        }
        else
        {
            result = WRITE_NO_MORE_DATA;

            resetBinaryBuffer();
            if (isOTAData)
                mOtaHandler.onOTADataSendFinished();
        }

        return result;
    }

    private void resetBinaryBuffer()
    {
        mBinaryBuffer = null;
        mBinaryBufferPointer = 0;
    }

    private boolean retryLastBinaryWrite(BLEConnection connection, boolean isOTAData)
    {
        boolean result = false;

        if (mRetryCount < RETRY_COUNT_MAX)
        {
            mRetryCount++;
            if (mLastBinaryPacket != null)
            {
                result = writeBinaryPacket(connection, isOTAData, mLastBinaryPacket);
            }
        }

        return result;
    }

    private boolean writeBinaryPacket(BLEConnection connection, boolean isOTAData, byte[] packet)
    {
        boolean result = false;

        if (connection != null)
        {
            if (isOTAData)
                result = connection.writeOTADataCharacteristic(packet);
            else
                result = connection.writeRxCharacteristic(packet);
        }

        return result;
    }

    synchronized protected void processNextCommand()
    {
        BLECommand nextCommand = mHandler.getNextCommand();

        if (nextCommand != null)
        {
            Log.d(TAG, "Running next BLE command - " + nextCommand.getType());
            BLEConnection connection = nextCommand.getConnection();

            if (connection == null)
            {
                mBLECallbacks.onError("", ERROR_NO_CONNECTION, DATA_NONE);
            }
            else
            {
                String deviceName = connection.getDevice().getName();
                BLECommandType type = nextCommand.getType();

                switch (type)
                {
                    case READ_MODE:
                        if (!connection.readModeCharacteristic())
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.MODE_READ_FAILED, DATA_NONE);
                        }
                        break;

                    case WRITE_MODE:
                        if (!connection.writeModeCharacteristic(nextCommand.getMode()))
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.MODE_WRITE_FAILED, DATA_NONE);
                        }
                        break;

                    case READ_DATA_BIN:
                        dataType = DataType.DATA_BINARY;
                        if (!connection.readTxCharacteristic())
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.DATA_READ_FAILED, DATA_NONE);
                        }
                        break;

                    case READ_DATA_STR:
                        dataType = DataType.DATA_STRING;
                        if (!connection.readTxCharacteristic())
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.DATA_READ_FAILED, DATA_NONE);
                        }
                        break;

                    case WRITE_DATA_STR:
                        mRetryCount = 0;
                        dataType = DataType.DATA_STRING;
                        if (!writeStringData(connection, nextCommand.getStringData()))
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.DATA_WRITE_FAILED, DATA_NONE);
                        }
                        break;

                    case WRITE_DATA_BIN:
                        mRetryCount = 0;
                        dataType = DataType.DATA_BINARY;
                        mBinaryOffset = nextCommand.getBinaryDataOffset();
                        if (writeBinaryData(connection, nextCommand.getBinaryData(), false) == WRITE_FAIL)
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.DATA_WRITE_FAILED, DATA_NONE);
                        }
                        break;

                    case WRITE_OTA_CONTROL:
                        mRetryCount = 0;
                        dataType = DataType.DATA_BINARY;
                        if (!connection.writeOTAControlCharacteristic(nextCommand.getBinaryData()))
                        {
                            mOtaHandler.onError(new OTAStatus(OTAStatus.COMMUNICATION_ERROR));
                            Log.d(TAG, "Failed to write OTA control");
                        }
                        break;

                    case WRITE_OTA_DATA:
                        mRetryCount = 0;
                        dataType = DataType.DATA_BINARY;
                        mBinaryOffset = nextCommand.getBinaryDataOffset();
                        if (writeBinaryData(connection, nextCommand.getBinaryData(), true) == WRITE_FAIL)
                        {
                            mOtaHandler.onError(new OTAStatus(OTAStatus.COMMUNICATION_ERROR));
                            Log.d(TAG, "Failed to write OTA data");
                        }
                        break;

                    case SET_TX_NOTIFY_ENABLE:
                        if (!connection.setNotifyOnDataReady(true))
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.SET_TX_NOTIFY_FAILED, DATA_NONE);
                        }
                        break;

                    case SET_TX_NOTIFY_DISABLE:
                        if (!connection.setNotifyOnDataReady(false))
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.SET_TX_NOTIFY_FAILED, DATA_NONE);
                        }
                        break;

                    case SET_OTA_CONTROL_NOTIFY_ENABLE:
                        if (!connection.setNotifyOnOTAControl(true))
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.SET_OTA_CONTROL_NOTIFY_FAILED, DATA_NONE);
                        }
                        break;

                    case SET_OTA_CONTROL_NOTIFY_DISABLE:
                        if (!connection.setNotifyOnOTAControl(false))
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.SET_OTA_CONTROL_NOTIFY_FAILED, DATA_NONE);
                        }
                        break;

                    case READ_VERSION:
                        if (!connection.readFirmwareRevCharacteristic())
                        {
                            mOtaHandler.onError(new OTAStatus(OTAStatus.COMMUNICATION_ERROR));
                            Log.d(TAG, "Failed to read firmware revision characteristic");
                        }
                        break;

                    case DISCOVER_SERVICES:
                        if (!connection.discoverServices())
                        {
                            mBLECallbacks.onError(deviceName, BLEHandlerCallbacks.Error.SERVICE_DISCOVERY_FAILED, DATA_NONE);
                        }
                        break;

                    case DISCONNECT:
                        connection.disconnect();
                        break;
                }
            }
        }
        else
        {
            mHandler.setCommandInProgress(false);
            Log.d(TAG, "No commands to run");
        }
    }
}
