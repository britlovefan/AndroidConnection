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

public enum TruconnectBaudRate
{
    BAUD_9600("9600"),
    BAUD_14400("14400"),
    BAUD_19200("19200"),
    BAUD_28800("28800"),
    BAUD_38400("38400"),
    BAUD_56000("56000"),
    BAUD_57600("57600"),
    BAUD_115200("115200"),
    BAUD_128000("128000"),
    BAUD_153600("153600"),
    BAUD_230400("230400"),
    BAUD_256000("256000"),
    BAUD_460800("460800"),
    BAUD_921600("921600"),
    BAUD_1000000("1000000"),
    BAUD_1500000("1500000");

    String cmdArg;

    TruconnectBaudRate(String arg)
    {
        cmdArg = arg;
    }

    @Override
    public String toString()
    {
        return cmdArg;
    }
}
