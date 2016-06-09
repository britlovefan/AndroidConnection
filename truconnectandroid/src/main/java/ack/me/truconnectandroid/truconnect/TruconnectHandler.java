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

import android.content.Context;
import android.util.Log;

import java.io.Serializable;

import ack.me.truconnectandroid.BLE.BLEHandler;
import ack.me.truconnectandroid.OTA.FirmwareVersion;
import ack.me.truconnectandroid.OTA.OTACallbacks;
import ack.me.truconnectandroid.OTA.OTAHandler;
import ack.me.truconnectandroid.OTA.OTAStatus;

public class TruconnectHandler implements Serializable
{
    public static final int COMMAND_STRING_LEN_MAX = 127;
    public static final int MODE_STREAM = 1;
    public static final int MODE_COMMAND_LOCAL = 2;
    public static final int MODE_COMMAND_REMOTE = 3;

    private static final String TAG = "TruconnectHandler";

    private BLECallbackHandler mBLECallbacks;
    private BLEHandler mBLEHandler;

    private boolean mConnected = false;
    private String mDeviceName;

    private TruconnectCallbacks mCallbacks;

    private String mLastWritePacket = "";

    private TruconnectCommandQueue mCommandQueue;
    private boolean mCommandInProgress = false;
    private TruconnectCommand mCurrentCommand;
    private int mCurrentCommandID;

    private boolean mIsInitialised = false;

    private ResponseParser mResponseParser;

    private OTAHandler mOTAHandler;//for android SDK
    private Context mContext;
    private byte[] mOTAFileData;
    private String mOTAFilename;
    private OTACallbacks mUserOTACallbacks;
    private boolean mCheckingForUpdates = false;
    private boolean mOTAInProgress = false;
    private OTACallbacks mOTACallbacks = new OTACallbacks()
    {
        @Override
        public void onUpdateInitSuccess(String deviceName, FirmwareVersion currentVersion)
        {
            if (mUserOTACallbacks != null)
            {
                //check for updates
                if (mOTAHandler != null && mContext != null)
                {
                    if (mCheckingForUpdates) //if checking for latest truconnect image
                    {
                        checkForUpdates();
                    }
                    else //updating from file
                    {
                        updateFromFile();
                    }
                }

                mUserOTACallbacks.onUpdateInitSuccess(deviceName, currentVersion);
            }
            else
            {
                Log.d(TAG, "ERROR - onUpdateInitSuccess: Null user OTA callbacks!");
                mOTAInProgress = false;
            }
        }

        @Override
        public void onUpdateCheckComplete(String deviceName, boolean isUpToDate, FirmwareVersion version, byte[] file)
        {
            if (mUserOTACallbacks != null)
                mUserOTACallbacks.onUpdateCheckComplete(deviceName, isUpToDate, version, file);
        }

        @Override
        public void onUpdateAbort(String deviceName)
        {
            mOTAInProgress = false;

            if (mUserOTACallbacks != null)
                mUserOTACallbacks.onUpdateAbort(deviceName);
        }

        @Override
        public void onUpdateStart(String deviceName)
        {
            if (mUserOTACallbacks != null)
                mUserOTACallbacks.onUpdateStart(deviceName);
        }

        @Override
        public void onUpdateComplete(String deviceName)
        {
            mOTAInProgress = false;

            if (mUserOTACallbacks != null)
                mUserOTACallbacks.onUpdateComplete(deviceName);
        }

        @Override
        public void onUpdateDataSent(String deviceName, int bytesSent, int bytesRemaining)
        {
            if (mUserOTACallbacks != null)
                mUserOTACallbacks.onUpdateDataSent(deviceName, bytesSent, bytesRemaining);
        }

        @Override
        public void onUpdateError(String deviceName, OTAStatus status)
        {
            mOTAInProgress = false;

            if (mUserOTACallbacks != null)
                mUserOTACallbacks.onUpdateError(deviceName, status);
        }
    };

    public TruconnectHandler()
    {
        mResponseParser = new ResponseParser();
    }

    //error handling on BLE command fail
    public enum Error
    {
        WRITE_FAILED_DISCARD_RESPONSE,
        WRITE_FAILED_SEND_NEWLINE
    }

    public boolean init(Context context, BLEHandler handler, TruconnectCallbacks callbacks, OTACallbacks otaCallbacks)
    {
        boolean result = false;

        if (context != null && handler != null && callbacks != null)
        {
            mContext = context;
            mBLEHandler = handler;
            mCallbacks = callbacks;
            mCommandQueue = new TruconnectCommandQueue();
            mOTAHandler = new OTAHandler();
            mOTAInProgress = false;
            mUserOTACallbacks = otaCallbacks;

            initCallbacks();
            if (handler.init(context, mBLECallbacks.getBLECallbacks(), mOTAHandler) && handler.isBLEEnabled())
            {
                result = true;
                mConnected = false;
                mIsInitialised = true;
            }
        }

        return result;
    }

    public void deinit()
    {
        if (mBLEHandler != null)
        {
            mBLEHandler.deinit();
            mIsInitialised = false;
        }
    }

    public boolean startScan()
    {
        boolean result = false;

        if (mBLEHandler != null)
        {
            result = mBLEHandler.startBLEScan();
        }

        return result;
    }

    public boolean stopScan()
    {
        boolean result = false;

        if (mBLEHandler != null)
        {
            result = mBLEHandler.stopBLEScan();
        }

        return result;
    }

    public boolean connect(String deviceName)
    {
        return connect(deviceName, false);
    }

    public boolean connect(String deviceName, boolean autoReconnect)
    {
        boolean result = false;

        if (mBLEHandler != null && !mConnected)
        {
            result = mBLEHandler.connect(deviceName, autoReconnect);
            mConnected = result;
            mDeviceName = deviceName;
        }

        return result;
    }

    public boolean disconnect(boolean disableTxNotifcation)
    {
        boolean result = false;

        if(mBLEHandler != null && mConnected)
        {
            setDisconnectedState();
            result = mBLEHandler.disconnect(mDeviceName, disableTxNotifcation);
        }

        return result;
    }

    public boolean isConnected()
    {
        return mBLEHandler.isConnected(mDeviceName);
    }

    public boolean isInitialised()
    {
        return mIsInitialised;
    }

    /**
     * Sends a Truconnect command to a connected device.
     * @param command Command to send
     * @param args Arguments for command
     * @return Command ID to identify this request
     */
    public int sendCommand(TruconnectCommand command, String args)
    {
        int command_id = TruconnectCommandQueue.ID_INVALID;

        if (command != null && args != null && !mOTAInProgress)
        {
            final String commandString = String.format("%s %s\r\n", command, args);
            if (commandString.length() <= COMMAND_STRING_LEN_MAX)
            {
                command_id = mCommandQueue.add(command, commandString);
                runNextCommand();//run next command if not currently running one
            }
        }
        else
            Log.d(TAG, "Command ignored");

        return command_id;
    }

    public TruconnectReceiveMode getReceiveMode()
    {
        return mBLEHandler.getReceiveMode();
    }

    public boolean setReceiveMode(TruconnectReceiveMode mode)
    {
        return mBLEHandler.setReceiveMode(mode);
    }

    public boolean setMode(int mode)
    {
        if (!mOTAInProgress)
        {
            return mBLEHandler.writeMode(mDeviceName, mode);
        }
        else
        {
            return false;
        }
    }

    public boolean getMode()
    {
        return mBLEHandler.readMode(mDeviceName);
    }

    public boolean writeData(String data)
    {
        if (!mOTAInProgress)
        {
            return mBLEHandler.writeData(mDeviceName, data);
        }
        else
        {
            return false;
        }
    }

    public boolean writeData(byte[] data)
    {
        if (!mOTAInProgress)
        {
            return mBLEHandler.writeData(mDeviceName, data);
        }
        else
        {
            return false;
        }
    }

    public boolean readFirmwareVersion()
    {
        if (!mOTAInProgress)
        {
            return mBLEHandler.readFirmwareRev(mDeviceName);
        }
        else
        {
            return false;
        }
    }

    public boolean otaCheckForUpdates(String filename)
    {
        boolean result = false;

        if (!mCommandInProgress && filename != null && mOTAHandler != null && !mOTAInProgress)
        {
            mCheckingForUpdates = true;
            mOTAFilename = filename;

           result = otaInit();
        }

        return result;
    }

    //Update firmware using given binary file data
    public boolean otaUpdateFromFile(byte[] fileData)
    {
        boolean result = false;

        if (!mCommandInProgress && mOTAHandler != null && !mOTAInProgress)
        {
            mOTAInProgress = true;
            mOTAFileData = fileData;
            mCheckingForUpdates = false;

            if (!mOTAHandler.isInitialised(mOTACallbacks))
            {
                result = otaInit();
            }
            else
            {
                result = updateFromFile();
            }
        }

        return result;
    }

    public boolean otaUpdateAbort()
    {
        boolean result = false;

        if  (mOTAHandler != null && mOTAInProgress)
        {
            result = mOTAHandler.updateAbort();
        }

        return result;
    }

    private void initCallbacks()
    {
        mBLECallbacks = new BLECallbackHandler(mBLEHandler, this);
    }

    private synchronized void runNextCommand()
    {
        if (!mCommandInProgress)
        {
            TruconnectCommandRequest req = mCommandQueue.getNext();
            if (req != null)
            {
                mCommandInProgress = true;

                mCurrentCommandID = req.getID();
                mCurrentCommand = req.getCommand();
                mLastWritePacket = req.getCommandString();

                Log.d(TAG, "Running next command " + req.getCommandString());
                mBLEHandler.writeData(mDeviceName, req.getCommandString());
            }
        }
    }

    protected TruconnectCallbacks getCallbacks()
    {
        return mCallbacks;
    }

    protected void setConnectedStatus(boolean status)
    {
        mConnected = status;
    }

    protected void onStringDataRead(String data)
    {
        mResponseParser.addToBuffer(data);
        parseResponse();
    }

    //called from BLE code
    protected void onTruconnectError(TruconnectErrorCode code)
    {
        switch (code)
        {
            case CONNECT_FAILED:
                setDisconnectedState();
                break;

            case CONNECTION_LOST:
                setDisconnectedState();
                break;

            case CONNECTION_RECONNECT:
                mConnected = true;
                break;
        }
    }

    protected void onError(Error error)
    {
        mCurrentCommand = null;//throw away response, if we get one

        if (error == Error.WRITE_FAILED_DISCARD_RESPONSE)
        {
            Log.d(TAG, "onError, discarding response after read");
        }
        else if (error == Error.WRITE_FAILED_SEND_NEWLINE)
        {
            Log.d(TAG, "onError, sending newline to clear device command buffer");
            mBLEHandler.writeData(mDeviceName, "\r\n");//clear current line and get a response
        }
    }

    protected String getLastWritePacket()
    {
        return mLastWritePacket;
    }

    protected void clearCommandQueue()
    {
        mCommandQueue.clear();
    }

    protected void clearReadBuffer()
    {
        mResponseParser.clearBuffer();
    }

    private void setDisconnectedState()
    {
        clearCommandQueue();
        mCurrentCommand = null;
        mCommandInProgress = false;
        mConnected = false;
        mOTAInProgress = false;
    }

    private void parseResponse()
    {
        TruconnectResult result = mResponseParser.parseResponse();

        if (result != null)
        {
            if (result.getResponseCode() == TruconnectResult.INCOMPLETE_RESPONSE)
            {
                mCallbacks.onError(TruconnectErrorCode.INCOMPLETE_RESPONSE);
                Log.d(TAG, "Error, incomplete response");
            }
            else if (mCurrentCommand != null && mCommandInProgress)
            {
                mCallbacks.onCommandResult(mCurrentCommandID, mCurrentCommand, result);
                Log.d(TAG, "Command complete");
            }
            else
            {
                Log.d(TAG, "Command result discarded");
            }
            mCommandInProgress = false;
            mCurrentCommandID = TruconnectCommandQueue.ID_INVALID;
            mCurrentCommand = null;
            runNextCommand();
        }
    }

    private boolean otaInit()
    {
        boolean result = false;

        if (mOTAHandler.init(mDeviceName, mBLEHandler, mOTACallbacks))
        {
            result = true;
        }
        else if (mUserOTACallbacks != null)
        {
            OTAStatus status = new OTAStatus(OTAStatus.INIT_FAILED);
            mUserOTACallbacks.onUpdateError(mDeviceName, status);
        }

        return result;
    }

    private boolean checkForUpdates()
    {
        boolean result = false;

        if (mOTAHandler.checkForUpdates(mContext, mOTAFilename))
        {
            result = true;
        }
        else if (mUserOTACallbacks != null)
        {
            OTAStatus status = new OTAStatus(OTAStatus.FAILED_TO_GET_IMAGE);
            mUserOTACallbacks.onUpdateError(mDeviceName, status);
        }

        return result;
    }

    private boolean updateFromFile()
    {
        boolean result = false;

        if (mOTAHandler.updateStart(mOTAFileData))
        {
            result = true;
        }
        else if (mUserOTACallbacks != null)
        {
            OTAStatus status = new OTAStatus(OTAStatus.COMMUNICATION_ERROR);
            mUserOTACallbacks.onUpdateError(mDeviceName, status);
            Log.d(TAG, "Failed to start update");
        }

        return result;
    }
}
