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
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

import java.io.Serializable;

public class BLEDevice implements Serializable
{
    private BluetoothDevice mDevice;
    private String mName;

    public BLEDevice(BluetoothDevice device)
    {
        mDevice = device;
        mName = getNameFromDevice(device);
    }

    public String toString()
    {
        return getName();
    }

    public String getName()
    {
        return mName;
    }

    public BluetoothDevice getDevice()
    {
        return mDevice;
    }

    public BLEGatt connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback)
    {
        return new BLEGatt(mDevice.connectGatt(context, autoConnect, callback));
    }

    public static String getNameFromDevice(BluetoothDevice device)
    {
        String name = "";

        if (device != null)
        {
            String deviceName = device.getName();//try to use device name first

            if (deviceName != null)
            {
                name = deviceName;
            }
            else
            {
                String address = device.getAddress();//try to use address as backup
                if (address != null)
                {
                    name = address;
                }
            }
        }

        return name;
    }
}
