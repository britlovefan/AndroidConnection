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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.io.Serializable;
import java.util.UUID;

public class BLEConnection implements Serializable
{
    public static final UUID CLIENT_CHAR_CONFIG_UUID =
                                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothGatt mGatt;
    private BluetoothDevice mDevice;

    private BluetoothGattCharacteristic mTxCharacteristic;
    private BluetoothGattCharacteristic mRxCharacteristic;
    private BluetoothGattCharacteristic mModeCharacteristic;

    private BluetoothGattCharacteristic mOTAControlCharacteristic;
    private BluetoothGattCharacteristic mOTADataCharacteristic;
    private BluetoothGattCharacteristic mFirmwareRevCharacteristic;

    private final int MODE_OFFSET = 0;

    public enum Mode
    {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    };

    private Mode mMode;

    public BLEConnection()
    {
        mMode = Mode.DISCONNECTED;
    }

    public String toString()
    {
        return mDevice.getName();
    }

    public void close()
    {
        if(mGatt != null)
        {
            mGatt.close();
            mGatt = null;
        }
    }

    public void disconnect()
    {
        if(mGatt != null)
        {
            mGatt.disconnect();
        }
    }

    public boolean hasTruconnectCharacteristics()
    {
        return (mModeCharacteristic != null && mTxCharacteristic != null && mRxCharacteristic != null);
    }

    public boolean hasOTACharacteristics()
    {
        return (mOTAControlCharacteristic != null && mOTADataCharacteristic != null);
    }

    public boolean hasFirmwareRevCharacteristics()
    {
        return (mFirmwareRevCharacteristic != null);
    }

    public BluetoothGatt getGatt()
    {
        return mGatt;
    }

    public void setGatt(BluetoothGatt mGatt)
    {
        this.mGatt = mGatt;
    }

    public BluetoothDevice getDevice()
    {
        return mDevice;
    }

    public void setDevice(BluetoothDevice mDevice)
    {
        this.mDevice = mDevice;
    }

    public Mode getMode()
    {
        return mMode;
    }

    public void setMode(Mode mode)
    {
        mMode = mode;
    }

    protected void setTxCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        mTxCharacteristic = characteristic;
    }

    protected void setRxCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        mRxCharacteristic = characteristic;
    }

    protected void setModeCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        mModeCharacteristic = characteristic;
    }

    public boolean readModeCharacteristic()
    {
        return readCharacteristic(mModeCharacteristic);
    }

    public boolean writeModeCharacteristic(int mode)
    {
        boolean result = false;

        if (mGatt != null && mModeCharacteristic != null)
        {
            mModeCharacteristic.setValue(mode, BluetoothGattCharacteristic.FORMAT_UINT8, MODE_OFFSET);
            result = mGatt.writeCharacteristic(mModeCharacteristic);
        }

        return result;
    }

    public boolean readTxCharacteristic()
    {
        return readCharacteristic(mTxCharacteristic);
    }

    public boolean writeRxCharacteristic(String data)
    {
        return writeCharacteristic(mRxCharacteristic, data);
    }

    public boolean writeRxCharacteristic(byte[] data)
    {
        return writeCharacteristic(mRxCharacteristic, data);
    }

    public boolean setNotifyOnDataReady(boolean enable)
    {
        return setNotifyOnCharUpdate(mTxCharacteristic, enable);
    }

    public boolean setNotifyOnOTAControl(boolean enable)
    {
        return setNotifyOnCharUpdate(mOTAControlCharacteristic, enable);
    }

    public boolean writeOTAControlCharacteristic(byte[] data)
    {
        boolean result = false;

        if (mGatt != null && mOTAControlCharacteristic != null)
        {
            mOTAControlCharacteristic.setValue(data);
            result = mGatt.writeCharacteristic(mOTAControlCharacteristic);
        }

        return result;
    }

    public boolean writeOTADataCharacteristic(byte[] data)
    {
        boolean result = false;

        if (mGatt != null && mOTADataCharacteristic != null)
        {
            mOTADataCharacteristic.setValue(data);
            result = mGatt.writeCharacteristic(mOTADataCharacteristic);
        }

        return result;
    }

    public boolean readFirmwareRevCharacteristic()
    {
        return readCharacteristic(mFirmwareRevCharacteristic);
    }

    public void setOTAControlCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        mOTAControlCharacteristic = characteristic;
    }

    public void setOTADataCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        mOTADataCharacteristic = characteristic;
    }

    public void setFirmwareRevCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        mFirmwareRevCharacteristic = characteristic;
    }

    private boolean setNotifyOnCharUpdate(BluetoothGattCharacteristic characteristic, boolean enable)
    {
        boolean result = false;

        if (mGatt != null && characteristic != null)
        {
            mGatt.setCharacteristicNotification(characteristic, enable);
            BluetoothGattDescriptor charNotifyDescriptor =
                    new BluetoothGattDescriptor(CLIENT_CHAR_CONFIG_UUID,
                                                BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED);
            characteristic.addDescriptor(charNotifyDescriptor);

            if(enable)
            {
                charNotifyDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }
            else
            {
                charNotifyDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }

            result = mGatt.writeDescriptor(charNotifyDescriptor);
        }

        return result;
    }

    public boolean discoverServices()
    {
        boolean result = false;

        if (mGatt != null)
        {
            result = mGatt.discoverServices();
        }

        return result;
    }

    private boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data)
    {
        boolean result = false;

        if (mGatt != null && characteristic != null)
        {
            characteristic.setValue(data);
            result = mGatt.writeCharacteristic(characteristic);
        }

        return result;
    }

    private boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String data)
    {
        boolean result = false;

        if (mGatt != null && characteristic != null)
        {
            characteristic.setValue(data);
            result = mGatt.writeCharacteristic(characteristic);
        }

        return result;
    }

    private boolean readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        boolean result = false;

        if (mGatt != null && characteristic != null)
        {
            result = mGatt.readCharacteristic(characteristic);
        }

        return result;
    }
}
