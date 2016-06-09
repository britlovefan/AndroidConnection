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

import java.io.Serializable;
import java.util.UUID;

import ack.me.truconnectandroid.BLE.BLECommandQueue;
import ack.me.truconnectandroid.BLE.BLEConnection;
import ack.me.truconnectandroid.BLE.BLEDevice;
import ack.me.truconnectandroid.BLE.BLEDeviceList;
import ack.me.truconnectandroid.BLE.BLEHandler;
import ack.me.truconnectandroid.BLE.SearchableList;
import ack.me.truconnectandroid.OTA.OTACallbacks;

public class TruconnectManager implements Serializable

{
    public static final int GPIO_MIN = 0;
    public static final int GPIO_MAX = 14;
    public static final int GPIO_VAL_MIN = 0;
    public static final int GPIO_VAL_MAX = 1;

    public static final int MODE_STREAM = TruconnectHandler.MODE_STREAM;
    public static final int MODE_COMMAND_LOCAL = TruconnectHandler.MODE_COMMAND_LOCAL;
    public static final int MODE_COMMAND_REMOTE = TruconnectHandler.MODE_COMMAND_REMOTE;

    public static final int ID_INVALID = TruconnectCommandQueue.ID_INVALID;

    private TruconnectHandler mTruconnectHandler;

    public TruconnectManager()
    {
        mTruconnectHandler = new TruconnectHandler();
    }

    public boolean init(Context context, TruconnectCallbacks callbacks, OTACallbacks otaCallbacks)
    {
        SearchableList<BLEConnection> connectionList = new SearchableList<BLEConnection>();
        BLEDeviceList deviceList = new BLEDeviceList(new SearchableList<BLEDevice>());
        BLECommandQueue queue = new BLECommandQueue();
        BLEHandler bleHandler = new BLEHandler(deviceList, connectionList, queue);

        return mTruconnectHandler.init(context, bleHandler, callbacks, otaCallbacks);
    }

    public void deinit()
    {
        if (mTruconnectHandler != null)
        {
            mTruconnectHandler.deinit();
            mTruconnectHandler = null;
        }
    }

    public boolean startScan()
    {
        return mTruconnectHandler.startScan();
    }

    public boolean stopScan()
    {
        return mTruconnectHandler.stopScan();
    }

    public boolean connect(String deviceName)
    {
        return mTruconnectHandler.connect(deviceName);
    }

    public boolean connect(String deviceName, boolean autoReconnect)
    {
        return mTruconnectHandler.connect(deviceName, autoReconnect);
    }

    public boolean disconnect(boolean disableTxNotification)
    {
        return mTruconnectHandler.disconnect(disableTxNotification);
    }

    public boolean isConnected()
    {
        return mTruconnectHandler.isConnected();
    }

    public boolean isInitialised()
    {
        return mTruconnectHandler.isInitialised();
    }

    public boolean setMode(int mode)
    {
        return mTruconnectHandler.setMode(mode);
    }

    public boolean getMode()
    {
        return mTruconnectHandler.getMode();
    }

    public int adc(int gpio)
    {
        if (isGPIOValid(gpio))
        {
            return mTruconnectHandler.sendCommand(TruconnectCommand.ADC, gpioToString(gpio));
        }
        else
        {
            return ID_INVALID;
        }
    }

    //Cant use these when connected, no point implementing them
    //adv
    //con
    //dct
    //rbmode
    //scan (not very useful)

    public int beep(int duration)
    {
        return mTruconnectHandler.sendCommand(TruconnectCommand.BEEP, String.valueOf(duration));
    }

    public int factoryReset(String BLEAddress)
    {
        return mTruconnectHandler.sendCommand(TruconnectCommand.FACTORY_RESET, BLEAddress);
    }

    public int GPIOFunctionSet(int gpio, TruconnectGPIOFunction func)
    {
        int command_id = ID_INVALID;

        if (isGPIOValid(gpio))
        {
            String args = makeGPIOArgString(gpio, func.toString());
            command_id = mTruconnectHandler.sendCommand(TruconnectCommand.GPIO_FUNCTION, args);
        }

        return command_id;
    }

    public int GPIODirectionSet(int gpio, TruconnectGPIODirection dir)
    {
        int command_id = ID_INVALID;

        if (isGPIOValid(gpio))
        {
            String args = makeGPIOArgString(gpio, dir.toString());
            command_id = mTruconnectHandler.sendCommand(TruconnectCommand.GPIO_DIRECTION, args);
        }

        return command_id;
    }

    public int GPIOGet(int gpio)
    {
        int command_id = ID_INVALID;

        if (isGPIOValid(gpio))
        {
            String args = gpioToString(gpio);
            command_id = mTruconnectHandler.sendCommand(TruconnectCommand.GPIO_GET, args);
        }

        return command_id;
    }

    public int GPIOSet(int gpio, int value)
    {
        int command_id = ID_INVALID;

        if (isGPIOValid(gpio) && isGPIOValueValid(value))
        {
            String args = makeGPIOArgString(gpio, String.valueOf(value));
            command_id = mTruconnectHandler.sendCommand(TruconnectCommand.GPIO_SET, args);
        }

        return command_id;
    }

    public int getBluetoothAddress()
    {
        String variable = TruconnectVariable.BLUETOOTH_ADDRESS.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getBluetoothConnectionCount()
    {
        String variable = TruconnectVariable.BLUETOOTH_CON_COUNT.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getBluetoothServiceUUID()
    {
        String variable = TruconnectVariable.BLUETOOTH_SERVICE_UUID.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getBluetoothTxPowerAdv()
    {
        String variable = TruconnectVariable.BLUETOOTH_TX_POWER_ADV.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getBluetoothTxPowerCon()
    {
        String variable = TruconnectVariable.BLUETOOTH_TX_POWER_CON.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getBluetoothAdvMode()
    {
        String variable = TruconnectVariable.BLUETOOTH_ADV_MODE.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getBluetoothAdvHighDur()
    {
        String variable = TruconnectVariable.BLUETOOTH_ADV_HIGH_DUR.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getBluetoothAdvHighInt()
    {
        String variable = TruconnectVariable.BLUETOOTH_ADV_HIGH_INT.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getBluetoothAdvLowDur()
    {
        String variable = TruconnectVariable.BLUETOOTH_ADV_LOW_DUR.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getBluetoothAdvLowInt()
    {
        String variable = TruconnectVariable.BLUETOOTH_ADV_LOW_INT.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getBusInitMode()
    {
        String variable = TruconnectVariable.BUS_INIT_MODE.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getBusSerialControl()
    {
        String variable = TruconnectVariable.BUS_SERIAL_CONTROL.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getCentralConCount()
    {
        String variable = TruconnectVariable.CENTRAL_CON_COUNT.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getCentralConMode()
    {
        String variable = TruconnectVariable.CENTRAL_CON_MODE.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getCentralScanHighDur()
    {
        String variable = TruconnectVariable.CENTRAL_SCAN_HIGH_DUR.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getCentralScanHighInt()
    {
        String variable = TruconnectVariable.CENTRAL_SCAN_HIGH_INT.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getCentralScanLowDur()
    {
        String variable = TruconnectVariable.CENTRAL_SCAN_LOW_DUR.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getCentralScanLowInt()
    {
        String variable = TruconnectVariable.CENTRAL_SCAN_LOW_INT.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getCentralScanMode()
    {
        String variable = TruconnectVariable.CENTRAL_SCAN_MODE.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemActivityTimeout()
    {
        String variable = TruconnectVariable.SYSTEM_ACTIVITY_TIMEOUT.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemBoardName()
    {
        String variable = TruconnectVariable.SYSTEM_BOARD_NAME.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemCommandEcho()
    {
        String variable = TruconnectVariable.SYSTEM_COMMAND_ECHO.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemCommandHeader()
    {
        String variable = TruconnectVariable.SYSTEM_COMMAND_HEADER.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemCommandPrompt()
    {
        String variable = TruconnectVariable.SYSTEM_COMMAND_PROMPT.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemDeviceName()
    {
        String variable = TruconnectVariable.SYSTEM_DEVICE_NAME.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemIndicatorStatus()
    {
        String variable = TruconnectVariable.SYSTEM_INDICATOR_STATUS.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemOTAEnable()
    {
        String variable = TruconnectVariable.SYSTEM_OTA_ENABLE.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemPrintLevel()
    {
        String variable = TruconnectVariable.SYSTEM_PRINT_LEVEL.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemRemoteEnable()
    {
        String variable = TruconnectVariable.SYSTEM_REMOTE_COMMAND_ENABLE.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemGoToSleepTimeout()
    {
        String variable = TruconnectVariable.SYSTEM_GO_TO_SLEEP_TIMEOUT.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getSystemUUID()
    {
        String variable = TruconnectVariable.SYSTEM_UUID.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getVersion()
    {
        return mTruconnectHandler.sendCommand(TruconnectCommand.VERSION, "");
    }

    public int getSystemWakeUpTimeout()
    {
        String variable = TruconnectVariable.SYSTEM_WAKE_UP_TIMEOUT.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getUARTBaudRate()
    {
        String variable = TruconnectVariable.UART_BAUD_RATE.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getUARTFlowControl()
    {
        String variable = TruconnectVariable.UART_FLOW_CONTROL.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int getUserVariable()
    {
        String variable = TruconnectVariable.USER_VARIABLE.toString();
        return mTruconnectHandler.sendCommand(TruconnectCommand.GET, variable);
    }

    public int pwmStart(int gpio, float dutyCycle, int frequency)
    {
        int command_id = ID_INVALID;

        int highCount = calcPWMHighCount(dutyCycle, frequency);
        int lowCount = calcPWMCount(dutyCycle, frequency);

        return startPWM(gpio, highCount, lowCount);
    }

    public int pwmStop(int gpio)
    {
        int command_id = ID_INVALID;

        if (isGPIOValid(gpio))
        {
            String args = String.format("%d stop", gpio);
            command_id = mTruconnectHandler.sendCommand(TruconnectCommand.PWM, args);
        }

        return command_id;
    }

    public int reboot()
    {
        return mTruconnectHandler.sendCommand(TruconnectCommand.REBOOT, "");
    }

    public int save()
    {
        return mTruconnectHandler.sendCommand(TruconnectCommand.SAVE, "");
    }

    public int setBluetoothServiceUUID(UUID uuid)
    {
        return setBluetoothServiceUUID(uuid.toString());
    }

    public int setBluetoothServiceUUID(String uuid)
    {
        String argString = String.format("%s %s", TruconnectVariable.BLUETOOTH_SERVICE_UUID, uuid);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setBluetoothTxPowerAdv(int power)
    {
        String argString = String.format("%s %d", TruconnectVariable.BLUETOOTH_TX_POWER_ADV, power);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setBluetoothTxPowerCon(int power)
    {
        String argString = String.format("%s %d", TruconnectVariable.BLUETOOTH_TX_POWER_CON, power);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setBluetoothAdvHighDur(int dur)
    {
        String argString = String.format("%s %d", TruconnectVariable.BLUETOOTH_ADV_HIGH_DUR, dur);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setBluetoothAdvHighInt(int interval)
    {
        String argString = String.format("%s %d",
                                         TruconnectVariable.BLUETOOTH_ADV_HIGH_INT, interval);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setBluetoothAdvLowDur(int dur)
    {
        String argString = String.format("%s %d", TruconnectVariable.BLUETOOTH_ADV_LOW_DUR, dur);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setBluetoothAdvLowInt(int interval)
    {
        String argString = String.format("%s %d",
                                         TruconnectVariable.BLUETOOTH_ADV_LOW_INT, interval);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setBusInitMode(TruconnectBusInitMode mode)
    {
        String argString = String.format("%s %s", TruconnectVariable.BUS_INIT_MODE, mode);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setBusSerialControl(TruconnectSerialControl control)
    {
        String argString = String.format("%s %s", TruconnectVariable.BUS_INIT_MODE, control);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setCentralScanHighDur(int dur)
    {
        String argString = String.format("%s %d", TruconnectVariable.CENTRAL_SCAN_HIGH_DUR, dur);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setCentralScanHighInt(int interval)
    {
        String argString = String.format("%s %d",
                                         TruconnectVariable.CENTRAL_SCAN_HIGH_INT, interval);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setCentralScanLowDur(int dur)
    {
        String argString = String.format("%s %d", TruconnectVariable.CENTRAL_SCAN_LOW_DUR, dur);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setCentralScanLowInt(int interval)
    {
        String argString = String.format("%s %d", TruconnectVariable.CENTRAL_SCAN_LOW_INT, interval);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setActivityTimeout(int timeout)
    {
        String argString = String.format("%s %d",
                                         TruconnectVariable.SYSTEM_ACTIVITY_TIMEOUT, timeout);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setSystemBoardName(String name)
    {
        String argString = String.format("%s %s", TruconnectVariable.SYSTEM_BOARD_NAME, name);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setSystemCommandEcho(boolean enabled)
    {
        String enabled_arg = boolToStrArg(enabled);
        String argString = String.format("%s %s",
                                         TruconnectVariable.SYSTEM_COMMAND_ECHO, enabled_arg);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setSystemCommandHeader(boolean enabled)
    {
        String enabled_arg = boolToStrArg(enabled);
        String argString = String.format("%s %s",
                                         TruconnectVariable.SYSTEM_COMMAND_HEADER, enabled_arg);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setSystemCommandMode(TruconnectCommandMode mode)
    {
        String argString = String.format("%s %s", TruconnectVariable.SYSTEM_COMMAND_MODE, mode);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setSystemCommandPrompt(boolean enabled)
    {
        String enabled_arg = boolToStrArg(enabled);
        String argString = String.format("%s %s",
                                         TruconnectVariable.SYSTEM_COMMAND_PROMPT, enabled_arg);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setSystemDeviceName(String name)
    {
        String argString = String.format("%s %s", TruconnectVariable.SYSTEM_DEVICE_NAME, name);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    /* Set the blink rate and duty cycle for the status LED when disconnected and connected. */
    public int setSystemIndicatorBlinkRate(float dutyDiscon, float periodDiscon, float dutyCon,
                                               float periodCon)
    {
        int conHighCount = calcStatusLedHighCount(dutyCon, periodCon);
        int conLowCount = calcStatusLedLowCount(dutyCon, periodCon);
        int disconHighCount = calcStatusLedHighCount(dutyDiscon, periodDiscon);
        int disconLowCount = calcStatusLedLowCount(dutyDiscon, periodDiscon);

        String argString = String.format("%s %02X%02X%02X%02X",
                                         TruconnectVariable.SYSTEM_INDICATOR_STATUS,
                                         disconLowCount, disconHighCount,
                                         conLowCount, conHighCount);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setSystemOTAEnable(boolean enabled)
    {
        String enabled_arg = boolToStrArg(enabled);
        String argString = String.format("%s %s", TruconnectVariable.SYSTEM_OTA_ENABLE, enabled_arg);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setSystemPrintLevel(TruconnectPrintLevel level)
    {
        String argString = String.format("%s %s", TruconnectVariable.SYSTEM_PRINT_LEVEL, level);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setSystemRemoteCommandEnable(boolean enabled)
    {
        String enabled_arg = boolToStrArg(enabled);
        String argString = String.format("%s %s",
                                         TruconnectVariable.SYSTEM_REMOTE_COMMAND_ENABLE,
                                         enabled_arg);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setSystemGoToSleepTimeout(int timeout)
    {
        String argString = String.format("%s %d",
                                         TruconnectVariable.SYSTEM_GO_TO_SLEEP_TIMEOUT, timeout);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setSystemGoWakeUpTimeout(int timeout)
    {
        String argString = String.format("%s %d",
                                         TruconnectVariable.SYSTEM_WAKE_UP_TIMEOUT, timeout);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setUARTBaudRate(TruconnectBaudRate baud)
    {
        String argString = String.format("%s %s", TruconnectVariable.SYSTEM_PRINT_LEVEL, baud);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setUARTFlowControl(boolean enabled)
    {
        String enabled_arg = boolToStrArg(enabled);
        String argString = String.format("%s %s", TruconnectVariable.UART_FLOW_CONTROL, enabled_arg);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int setUserVariable(String value)
    {
        String argString = String.format("%s %s", TruconnectVariable.USER_VARIABLE, value);
        return mTruconnectHandler.sendCommand(TruconnectCommand.SET, argString);
    }

    public int sleep()
    {
        return mTruconnectHandler.sendCommand(TruconnectCommand.SLEEP, "");
    }

    public int streamMode()
    {
        return mTruconnectHandler.sendCommand(TruconnectCommand.STREAM_MODE, "");
    }

    public TruconnectReceiveMode getReceiveMode()
    {
        return mTruconnectHandler.getReceiveMode();
    }

    public boolean setReceiveMode(TruconnectReceiveMode mode)
    {
        return mTruconnectHandler.setReceiveMode(mode);
    }

    public boolean writeData(String data)
    {
        return mTruconnectHandler.writeData(data);
    }

    public boolean writeData(byte[] data)
    {
        return mTruconnectHandler.writeData(data);
    }

    public boolean readFirmwareVersion()
    {
        return mTruconnectHandler.readFirmwareVersion();
    }

    //Input callbacks to use and filename on server ("" if default truconnect)
    public boolean otaCheckForUpdates(String filename)
    {
        return mTruconnectHandler.otaCheckForUpdates(filename);
    }

    //Input callbacks to use and OTA image data (can be obtained from onUpdateCheckComplete callback)
    public boolean otaUpdateFromFile(byte[] fileData)
    {
        return mTruconnectHandler.otaUpdateFromFile(fileData);
    }

    public boolean otaUpdateAbort()
    {
        return mTruconnectHandler.otaUpdateAbort();
    }

    private String gpioToString(int gpio)
    {
        return String.valueOf(gpio);
    }

    private boolean isGPIOValueValid(int value)
    {
        return (value >= GPIO_VAL_MIN && value <= GPIO_VAL_MAX);
    }

    private boolean isGPIOValid(int gpio)
    {
        return (gpio >= GPIO_MIN && gpio <= GPIO_MAX);
    }

    private String makeGPIOArgString(int gpio, String arg)
    {
        return String.format("%d %s", gpio, arg);
    }

    private String boolToStrArg(boolean arg)
    {
        String strArg;

        if (arg)
        {
            strArg = "1";
        }
        else
        {
            strArg = "0";
        }

        return strArg;
    }

    private int startPWM(int gpio, int highCount, int lowCount)
    {
        int command_id = ID_INVALID;

        if (isGPIOValid(gpio))
        {
            String args = String.format("%d %d %d", gpio, highCount, lowCount);
            command_id = mTruconnectHandler.sendCommand(TruconnectCommand.PWM, args);
        }

        return command_id;
    }

    private int calcPWMHighCount(float dutyCycle, int frequency)
    {
        return calcPWMCount(dutyCycle, frequency);
    }

    private int calcPWMLowCount(float dutyCycle, int frequency)
    {
        float inverseDuty = 1.0f - dutyCycle;
        return calcPWMCount(inverseDuty, frequency);
    }

    private int calcPWMCount(float dutyCycle, int frequency)
    {
        final float PWM_CONSTANT = 131072.0f;

        return (int)((PWM_CONSTANT * dutyCycle) / (float)frequency);
    }

    private int calcStatusLedLowCount(float dutyCycle, float period)
    {
        float inverseDuty = 1.0f - dutyCycle;
        return calcStatusLedCount(inverseDuty, period);
    }

    private int calcStatusLedHighCount(float dutyCycle, float period)
    {
        return calcStatusLedCount(dutyCycle, period);
    }

    private int calcStatusLedCount(float dutyCycle, float period)
    {
        final float PWM_CONSTANT = 8.0f;

        return (int)(PWM_CONSTANT * dutyCycle * period);
    }
}
