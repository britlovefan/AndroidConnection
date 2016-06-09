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

import java.io.Serializable;

public class BLECommand implements Serializable
{
    private BLECommandType type;
    private BLEConnection connection;
    private int mode;
    private String data_str;
    private byte[] data_bytes;
    private int offset;//send bytes after offset, used for OTA only

    BLECommand(BLEConnection connection, BLECommandType type)
    {
        this.connection = connection;
        this.type = type;
    }

    BLECommand(BLEConnection connection, BLECommandType type, int mode)
    {
        this.connection = connection;
        this.type = type;
        this.mode = mode;
    }

    BLECommand(BLEConnection connection, BLECommandType type, String data)
    {
        this.connection = connection;
        this.type = type;
        this.data_str = data;
        this.data_bytes = null;
    }

    BLECommand(BLEConnection connection, BLECommandType type, byte[] data)
    {
        this.connection = connection;
        this.type = type;
        this.data_bytes = data;
        this.data_str = null;
        this.offset = 0;
    }

    BLECommand(BLEConnection connection, BLECommandType type, byte[] data, int offset)
    {
        this.connection = connection;
        this.type = type;
        this.data_bytes = data;
        this.data_str = null;
        this.offset = offset;
    }

    public BLEConnection getConnection()
    {
        return connection;
    }

    public BLECommandType getType()
    {
        return type;
    }

    public int getMode()
    {
        return mode;
    }

    public String getStringData()
    {
        return this.data_str;
    }

    public byte[] getBinaryData()
    {
        return this.data_bytes;
    }

    public int getBinaryDataOffset()
    {
        return this.offset;
    }
}
