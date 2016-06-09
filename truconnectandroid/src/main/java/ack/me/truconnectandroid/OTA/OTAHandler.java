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

package ack.me.truconnectandroid.OTA;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.CRC32;
import ack.me.truconnectandroid.BLE.BLEHandler;

public class OTAHandler implements Serializable
{
    public static final String TAG = "OTAHandler";

    private enum Mode
    {
        INITIALISING,
        PREPARING_DOWNLOAD,
        DOWNLOADING,
        VERIFYING,
        IDLE,
        ABORTING
    }

    private static final int OTA_FILE_VERSION_OFFSET = 4;
    private static final int RESPONSE_MIN_LENGTH = 1;
    private static final int MAX_FILE_SIZE = 30*1024;

    private OTACallbacks mCallbacks;
    private BLEHandler mBLEHandler;

    private String mDeviceName;

    private Mode mCurrentMode = Mode.IDLE;

    private FirmwareVersion mCurrentVersion;

    private byte[] mFileData;
    private int mHeaderSize, mBytesSent, mBytesRemaining;
    private boolean mAbort = false;

    private static final String URL_ACKME_TRUCONNECT_SERVER = "http://resources.ack.me/truconnect/";
    private static final String TRUCONNECT_LATEST_FILENAME = "truconnect.bin";

    public boolean init(String deviceName, BLEHandler bleHandler, OTACallbacks callbacks)
    {
        boolean result = false;

        mBLEHandler = bleHandler;
        mCallbacks = callbacks;
        mDeviceName = deviceName;

        if (deviceName != null && mBLEHandler != null && callbacks != null)
        {
            //set notify on control char
            if (mBLEHandler.setOTAControlNotify(deviceName, true))
            {
                //read app info char for firmware version
                mCurrentMode = Mode.INITIALISING;
                result = mBLEHandler.readFirmwareRev(deviceName);
            }
        }

        return result;
    }

    //returns true if initialised with the given callbacks
    public boolean isInitialised(OTACallbacks callbacks)
    {
        return (mCallbacks == callbacks && mBLEHandler != null);
    }

    public boolean readFirmwareVersion()
    {
        if (mDeviceName != null)
        {
            return mBLEHandler.readFirmwareRev(mDeviceName);
        }
        else
        {
            return false;
        }
    }

    //checks latest firmware on ACKme server
    public boolean checkForUpdates(Context context, String filename)
    {
        boolean result = false;

        if(context != null && filename != null)
        {
            URL url = getURL(filename);

            result = true;

            ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connMgr != null)
            {
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected())
                {
                    DownloadFileTask downloadFileTask = new DownloadFileTask();
                    downloadFileTask.execute(url);//calls onUpdateComplete if success
                }
                else
                {
                    mCallbacks.onUpdateError(mDeviceName, new OTAStatus(OTAStatus.NO_INTERNET));
                }
            }
        }

        return result;
    }

    //Input version to check.
    //Returns true if given version is the same as the device firmware version
    public boolean isUpToDate(FirmwareVersion version)
    {
        boolean result = false;

        if (mCurrentVersion != null && version.isEqualTo(mCurrentVersion))
        {
            result = true;
        }

        return result;
    }

    public boolean updateStart(byte[] fileData)
    {
        byte[] command = {OTACommand.PREPARE_DOWNLOAD.getCode()};

        mHeaderSize = getHeaderSize(fileData);

        mBytesSent = 0;
        mBytesRemaining = getFileSize(fileData, mHeaderSize);

        mCurrentMode = Mode.PREPARING_DOWNLOAD;
        mFileData = fileData;

        return mBLEHandler.writeOTAControl(mDeviceName, command);
    }

    public boolean updateAbort()
    {
        boolean result = false;

        switch (mCurrentMode)
        {
            case INITIALISING:
            case PREPARING_DOWNLOAD:
                mAbort = true;//send abort code after response received
                break;

            //need to abort binary write at BLE level and send abort control code
            case DOWNLOADING:
                mCurrentMode = Mode.ABORTING;
                mBLEHandler.abortBinaryWrite();
                result = sendAbortCode();
                break;

            //cant abort in these states
            case VERIFYING:
            case ABORTING:
            case IDLE:
                break;

        }

        return result;
    }

    //called by BLE module when all OTA data has been sent
    public void onOTADataSendFinished()
    {
        if (mCurrentMode == Mode.DOWNLOADING)
        {
            byte[] crc32 = calcCRC32(mFileData, mHeaderSize);
            byte[] command = {OTACommand.VERIFY.getCode(), crc32[0], crc32[1], crc32[2], crc32[3]};

            mCurrentMode = Mode.VERIFYING;
            mBLEHandler.writeOTAControl(mDeviceName, command);
        }
    }

    //called by BLE module
    public void onFirmwareRevReceive(FirmwareVersion version)
    {
        Log.d(TAG, String.format("Got firmware version - %s", version));
        if (mCurrentMode == Mode.INITIALISING)
        {
            mCurrentVersion = version;
            mCallbacks.onUpdateInitSuccess(mDeviceName, mCurrentVersion);
        }
    }

    public void onOTADataSent(byte[] data)
    {
        mBytesSent += data.length;
        mBytesRemaining -= data.length;

        mCallbacks.onUpdateDataSent(mDeviceName, mBytesSent, mBytesRemaining);
    }

    public void onOTAControlReceive(byte[] data)
    {
        switch(mCurrentMode)
        {
            case PREPARING_DOWNLOAD:
                if (data.length >= RESPONSE_MIN_LENGTH)
                {
                    if (data[0] == OTAStatus.OK)
                    {
                        if (mAbort)
                        {
                            //abort OTA
                            mAbort = false;
                            mCurrentMode = Mode.ABORTING;
                            if (!sendAbortCode())
                            {
                                OTAStatus status = new OTAStatus(OTAStatus.ABORT_FAILED);
                                mCallbacks.onUpdateError(mDeviceName, status);
                            }
                        }
                        else
                        {
                            Log.d(TAG, "Response OK, starting download");
                            mCurrentMode = Mode.DOWNLOADING;
                            byte[] size = convertToBytes(getFileSize(mFileData, mHeaderSize), 2);
                            byte[] command = {OTACommand.DOWNLOAD.getCode(), size[0], size[1]};
                            mCallbacks.onUpdateStart(mDeviceName);
                            mBLEHandler.writeOTAControl(mDeviceName, command);
                        }
                    }
                    else
                    {
                        OTAStatus status = new OTAStatus(data[0]);
                        Log.d(TAG, "ERROR preparing download - " + status);
                        mCallbacks.onUpdateError(mDeviceName, status);
                    }
                }
                break;

            case DOWNLOADING:
                if (data.length >= RESPONSE_MIN_LENGTH)
                {
                    if(data[0] == OTAStatus.OK)
                    {
                        //start writing OTA data from file
                        Log.d(TAG, "Response OK, writing data");
                        mBLEHandler.writeOTAData(mDeviceName, mFileData, mHeaderSize);
                    }
                    else
                    {
                        OTAStatus status = new OTAStatus(data[0]);
                        Log.d(TAG, "ERROR downloading - " + status);
                        mCallbacks.onUpdateError(mDeviceName, status);
                    }
                }
                break;

            case VERIFYING:
                if(data.length >= RESPONSE_MIN_LENGTH)
                {
                    mCurrentMode = Mode.IDLE;

                    if(data[0] == OTAStatus.OK)
                    {
                        //data verified successfully
                        Log.d(TAG, "Response OK, Update complete!");
                        mCallbacks.onUpdateComplete(mDeviceName);
                    }
                    else
                    {
                        //verify failed!
                        OTAStatus status = new OTAStatus(data[0]);
                        Log.d(TAG, "ERROR verifying - " + status);
                        mCallbacks.onUpdateError(mDeviceName, status);
                    }
                }
                break;

            case ABORTING:
                if(data.length >= RESPONSE_MIN_LENGTH)
                {
                    mCurrentMode = Mode.IDLE;

                    if(data[0] == OTAStatus.OK)
                    {
                        //data verified successfully
                        Log.d(TAG, "Response OK, Update aborted!");
                        mCallbacks.onUpdateAbort(mDeviceName);
                    }
                    else
                    {
                        //verify failed!
                        OTAStatus status = new OTAStatus(data[0]);
                        Log.d(TAG, "ERROR aborting - " + status);
                        mCallbacks.onUpdateError(mDeviceName, status);
                    }
                }
                break;
        }
    }

    public void onError(OTAStatus status)
    {
        mCallbacks.onUpdateError(mDeviceName, status);
    }

    //get version from OTA binary file data
    public FirmwareVersion getVersion(byte[] fileData)
    {
        int index = OTA_FILE_VERSION_OFFSET;

        return new FirmwareVersion(fileData[index], fileData[index+1], fileData[index+2], fileData[index+3]);
    }

    public int getFileSize(byte[] fileData, int offset)
    {
        int length = fileData.length - offset;
        if (length > MAX_FILE_SIZE)
        {
            length = -1;
        }

        return length;
    }

    public byte[] convertToBytes(int value, int byteCount)
    {
        byte[] bytes = new byte[byteCount];
        int byteMask = 0x000000FF;

        for (int i=0; i<byteCount; i++)
        {
            int shiftAmount = 8*i;
            bytes[i] = (byte)((value & (byteMask << shiftAmount)) >> shiftAmount);
        }

        return bytes;
    }

    public int getHeaderSize(byte[] fileData)
    {
        return fileData[2] + ((int)fileData[3] >> 8);
    }

    public byte[] calcCRC32(byte[] data, int offset)
    {
        CRC32 crcHandler = new CRC32();

        crcHandler.update(data, offset, data.length - offset);

        return convertToBytes((int)crcHandler.getValue(), 4);
    }

    //Input filename of firmware image (NULL for latest truconnect)
    //Returns the complete URL (ACKme server), null if MalformedURLException occurs
    private URL getURL(String filename)
    {
        URL url = null;
        String urlStr = URL_ACKME_TRUCONNECT_SERVER;

        if (filename.compareTo("") == 0)
        {
            urlStr = urlStr.concat(TRUCONNECT_LATEST_FILENAME);
        }
        else
        {
            urlStr = urlStr.concat(filename);
        }

        try
        {
            url = new URL(urlStr);
        }
        catch (MalformedURLException e) {}

        return url;
    }

    private class DownloadFileTask extends AsyncTask<URL, Integer, byte[]>
    {
        private int getRemoteFileSize(URL url)
        {
            int fileSize = -1;
            HttpURLConnection urlConnection = null;

            try
            {
                urlConnection = (HttpURLConnection) url.openConnection();

                //request headers only to check content-length before allocating memory
                urlConnection.setRequestMethod("HEAD");
                InputStream inputStream = urlConnection.getInputStream();//need to get stream to read response headers

                String contentLength = urlConnection.getHeaderField("Content-Length");
                if (contentLength != null)
                {
                    fileSize = Integer.parseInt(contentLength);
                }
                else
                    throw new IOException("Null content length header!");
            }
            catch (IOException e)
            {
                Log.d(TAG, "ERROR - Failed to get remote file size: " + e);
                return -1;
            }
            finally
            {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }

            return fileSize;
        }

        private byte[] getRemoteFile(URL url, int size)
        {
            byte[] file = null;

            if (size > 0 && size <= MAX_FILE_SIZE)
            {
                file = new byte[size];
                HttpURLConnection urlConnection = null;

                try
                {
                    urlConnection = (HttpURLConnection) url.openConnection();

                    BufferedInputStream inStream = new BufferedInputStream(urlConnection.getInputStream());

                    int totalBytesRead = 0;

                    while (totalBytesRead < size)
                    {
                        int bytesRead = inStream.read(file, totalBytesRead, (size-totalBytesRead));
                        if (bytesRead == -1)
                            break;//stream closed
                        else
                            totalBytesRead += bytesRead;
                    }
                    inStream.close();

                    if (totalBytesRead != size)//transfer error
                    {
                        String excStr = String.format("File size incorrect, expected %d bytes, got %d bytes", size, totalBytesRead);
                        throw new IOException(excStr);
                    }
                }
                catch (IOException e)
                {
                    Log.d(TAG, "ERROR - Failed to get remote file: " + e);
                    return null;
                }
                finally
                {
                    if (urlConnection != null)
                        urlConnection.disconnect();
                }
            }

            return file;
        }

        protected byte[] doInBackground(URL... urls)
        {
            return getRemoteFile(urls[0], getRemoteFileSize(urls[0]));
        }

        protected void onPostExecute(byte[] result)
        {
            if (result != null)
            {
                FirmwareVersion version = getVersion(result);
                boolean isUpToDate = isUpToDate(version);

                mCallbacks.onUpdateCheckComplete(mDeviceName, isUpToDate, version, result);
            }
            else
                mCallbacks.onUpdateError(mDeviceName, new OTAStatus(OTAStatus.FAILED_TO_GET_IMAGE));
        }
    }

    private boolean sendAbortCode()
    {
        byte[] command = {OTACommand.ABORT.getCode()};
        return mBLEHandler.writeOTAControl(mDeviceName, command);
    }
}

