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

package ack.me.truconnectandroid.truconnect;

import android.util.Log;

import java.io.Serializable;

import ack.me.truconnectandroid.BLE.BLEHandler;
import ack.me.truconnectandroid.BLE.BLEHandlerCallbacks;
import ack.me.truconnectandroid.OTA.FirmwareVersion;

public class BLECallbackHandler implements Serializable
{
    public static final String TAG = "BLECallbackHandler";
    private static final String LINE_END = "\r\n";

    private BLEHandler mBLEHandler;
    private BLEHandlerCallbacks mBLECallbacks;
    private TruconnectHandler mTruconnectHandler;
    private TruconnectCallbacks mTruconnectCallbacks;

    BLECallbackHandler(BLEHandler BLEHandler, TruconnectHandler truconnectHandler)
    {
        mBLEHandler = BLEHandler;
        mTruconnectHandler = truconnectHandler;
        mTruconnectCallbacks = truconnectHandler.getCallbacks();

        mBLECallbacks = new BLEHandlerCallbacks(BLEHandler)
        {
            @Override
            public void onScanResult(String deviceName)
            {
                mTruconnectCallbacks.onScanResult(deviceName);
            }

            @Override
            public void onConnect(String deviceName, int services)
            {
                mTruconnectHandler.setConnectedStatus(true);
                mTruconnectCallbacks.onConnected(deviceName, services);
            }

            @Override
            public void onConnectFailed(String deviceName, BLEHandlerCallbacks.Result result)
            {
                TruconnectErrorCode errorCode;

                if (result == BLEHandlerCallbacks.Result.SERVICE_DISC_ERROR)
                {
                    errorCode = TruconnectErrorCode.SERVICE_DISCOVERY_ERROR;
                }
                else
                {
                    errorCode = TruconnectErrorCode.CONNECT_FAILED;
                }

                mTruconnectHandler.onTruconnectError(errorCode);
                mTruconnectCallbacks.onError(errorCode);
            }

            @Override
            public void onDisconnect(String deviceName)
            {
                mTruconnectHandler.setConnectedStatus(false);
                mTruconnectCallbacks.onDisconnected();
            }

            @Override
            public void onDisconnectFailed(String deviceName)
            {
                mTruconnectHandler.onTruconnectError(TruconnectErrorCode.DISCONNECT_FAILED);
                mTruconnectCallbacks.onError(TruconnectErrorCode.DISCONNECT_FAILED);
            }

            @Override
            public void onStringDataRead(String deviceName, String data)
            {
                Log.d(TAG, "onStringDataRead: " + deviceName + " , " + data);
                //append data to read buffer
                mTruconnectHandler.onStringDataRead(data);
                mTruconnectCallbacks.onStringDataRead(data);
            }

            @Override
            public void onBinaryDataRead(String deviceName, byte[] data)
            {
                Log.d(TAG, "onBinaryDataRead: " + deviceName);
                //no parsing required, just send to callback
                mTruconnectCallbacks.onBinaryDataRead(data);
            }

            @Override
            public void onStringDataWrite(String deviceName, String data)
            {
                Log.d(TAG, "onStringDataWrite: " + deviceName + " , " + data);
                mTruconnectCallbacks.onStringDataWritten(data);
            }

            @Override
            public void onBinaryDataWrite(String deviceName, byte[] data)
            {
                Log.d(TAG, "onBinaryDataWrite: " + deviceName);
                mTruconnectCallbacks.onBinaryDataWritten(data);
            }

            @Override
            public void onModeChanged(String deviceName, int mode)
            {
                mTruconnectCallbacks.onModeWritten(mode);
            }

            @Override
            public void onModeRead(String deviceName, int mode)
            {
                mTruconnectCallbacks.onModeRead(mode);
            }

            @Override
            public void onFirmwareVersionRead(String deviceName, FirmwareVersion version)
            {
                mTruconnectCallbacks.onFirmwareVersionRead(deviceName, version);
            }

            @Override
            public void onError(String deviceName, BLEHandlerCallbacks.Error error, String data)
            {
                TruconnectErrorCode truconnectErrorCode = TruconnectErrorCode.INTERNAL_ERROR;

                switch (error)
                {
                    case DISCONNECT_WITHOUT_REQUEST:
                        truconnectErrorCode = TruconnectErrorCode.CONNECTION_LOST;
                        break;

                    case CONNECT_WITHOUT_REQUEST:
                        truconnectErrorCode = TruconnectErrorCode.CONNECTION_RECONNECT;
                        break;

                    case INVALID_MODE:
                        truconnectErrorCode = TruconnectErrorCode.INTERNAL_ERROR;
                        break;

                    case NO_TX_CHARACTERISTIC:
                    case NO_RX_CHARACTERISTIC:
                    case NO_MODE_CHARACTERISTIC:
                        truconnectErrorCode = TruconnectErrorCode.DEVICE_ERROR;
                        break;

                    case NO_CONNECTION_FOUND:
                        truconnectErrorCode = TruconnectErrorCode.NO_CONNECTION_FOUND;
                        break;

                    case NULL_GATT_ON_CALLBACK:
                    case NULL_CHAR_ON_CALLBACK:
                        truconnectErrorCode = TruconnectErrorCode.SYSTEM_ERROR;
                        break;

                    case DATA_WRITE_FAILED:
                        truconnectErrorCode = TruconnectErrorCode.WRITE_FAILED;
                        //need to send newline to clear current command
                        if (data.endsWith(LINE_END))
                        {
                            mTruconnectHandler.onError(TruconnectHandler.Error.WRITE_FAILED_DISCARD_RESPONSE);
                        }
                        else
                        {
                            mTruconnectHandler.onError(TruconnectHandler.Error.WRITE_FAILED_SEND_NEWLINE);
                        }
                        break;

                    case DATA_READ_FAILED:
                        truconnectErrorCode = TruconnectErrorCode.READ_FAILED;
                        mTruconnectHandler.clearReadBuffer();
                        break;

                    case SET_TX_NOTIFY_FAILED:
                        truconnectErrorCode = TruconnectErrorCode.DEVICE_ERROR;
                        break;
                }

                mTruconnectHandler.onTruconnectError(truconnectErrorCode);
                mTruconnectCallbacks.onError(truconnectErrorCode);
            }
        };
    }

    public BLEHandlerCallbacks getBLECallbacks()
    {
        return mBLECallbacks;
    }
}
