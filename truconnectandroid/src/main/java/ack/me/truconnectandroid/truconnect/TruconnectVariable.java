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

public enum TruconnectVariable
{
    BLUETOOTH_ADDRESS("bl a"),
    BLUETOOTH_CON_COUNT("bl c c"),
    BLUETOOTH_SERVICE_UUID("bl s u"),
    BLUETOOTH_TX_POWER_ADV("bl t a"),
    BLUETOOTH_TX_POWER_CON("bl t c"),
    BLUETOOTH_ADV_MODE("bl v m"),
    BLUETOOTH_ADV_HIGH_DUR("bl v h d"),
    BLUETOOTH_ADV_HIGH_INT("bl v h i"),
    BLUETOOTH_ADV_LOW_DUR("bl v l d"),
    BLUETOOTH_ADV_LOW_INT("bl v l i"),
    BUS_INIT_MODE("bu i"),
    BUS_SERIAL_CONTROL("bu s c"),
    CENTRAL_CON_COUNT("ce c c"),
    CENTRAL_CON_MODE("ce c m"),
    CENTRAL_SCAN_HIGH_DUR("ce s h d"),
    CENTRAL_SCAN_HIGH_INT("ce s h i"),
    CENTRAL_SCAN_LOW_DUR("ce s l d"),
    CENTRAL_SCAN_LOW_INT("ce s l i"),
    CENTRAL_SCAN_MODE("ce s m"),
    GPIO_USAGE("gp u"),
    SYSTEM_ACTIVITY_TIMEOUT("sy a t"),
    SYSTEM_BOARD_NAME("sy b n"),
    SYSTEM_COMMAND_ECHO("sy c e"),
    SYSTEM_COMMAND_HEADER("sy c h"),
    SYSTEM_COMMAND_PROMPT("sy c p"),
    SYSTEM_COMMAND_MODE("sy c m"),
    SYSTEM_DEVICE_NAME("sy d n"),
    SYSTEM_INDICATOR_STATUS("sy i s"),
    SYSTEM_OTA_ENABLE("sy o e"),
    SYSTEM_PRINT_LEVEL("sy p"),
    SYSTEM_REMOTE_COMMAND_ENABLE("sy r e"),
    SYSTEM_GO_TO_SLEEP_TIMEOUT("sy s t"),
    SYSTEM_UUID("sy u"),
    SYSTEM_VERSION("sy v"),
    SYSTEM_WAKE_UP_TIMEOUT("sy w t"),
    UART_BAUD_RATE("ua b"),
    UART_FLOW_CONTROL("ua f"),
    USER_VARIABLE("us v");

    String varString;

    TruconnectVariable(String var)
    {
        varString = var;
    }

    @Override
    public String toString()
    {
        return varString;
    }
}

