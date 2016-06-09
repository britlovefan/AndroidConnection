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

import java.io.Serializable;

public class OTAStatus implements Serializable
{
    // event definitions for the OTA FW update
    public static final byte OK                  = (byte)0;
    public static final byte UNSUPPORTED_COMMAND = (byte)1;
    public static final byte ILLEGAL_STATE       = (byte)2;
    public static final byte VERIFICATION_FAILED = (byte)3;
    public static final byte INVALID_IMAGE       = (byte)4;
    public static final byte INVALID_IMAGE_SIZE  = (byte)5;
    public static final byte MORE_DATA           = (byte)6;
    public static final byte INVALID_APPID       = (byte)7;
    public static final byte INVALID_VERSION     = (byte)8;
    public static final byte COMMUNICATION_ERROR = (byte)100;
    public static final byte FAILED_TO_GET_IMAGE = (byte)101;
    public static final byte NO_INTERNET         = (byte)102;
    public static final byte INIT_FAILED         = (byte)103;
    public static final byte ABORT_FAILED        = (byte)104;

    private byte code;

    public OTAStatus(byte code)
    {
        this.code = code;
    }

    public byte getCode()
    {
        return this.code;
    }

    @Override
    public String toString()
    {
        String str = "invalid";

        switch (code)
        {
            case OK:
                str = "OK";
                break;

            case UNSUPPORTED_COMMAND:
                str = "Unsupported command";
                break;

            case ILLEGAL_STATE:
                str = "Illegal state";
                break;

            case VERIFICATION_FAILED:
                str = "Verification failed";
                break;

            case INVALID_IMAGE:
                str = "Invalid image";
                break;

            case INVALID_IMAGE_SIZE:
                str = "Invalid image size";
                break;

            case MORE_DATA:
                str = "More data than expected";
                break;

            case INVALID_APPID:
                str = "Invalid app ID";
                break;

            case INVALID_VERSION:
                str = "Invalid version";
                break;

            case COMMUNICATION_ERROR:
                str = "Communication error";
                break;

            case FAILED_TO_GET_IMAGE:
                str = "Failed to get latest firmware image";
                break;

            case NO_INTERNET:
                str = "No internet connection";
                break;

            case INIT_FAILED:
                str = "Failed to initialise OTA update";
                break;

            case ABORT_FAILED:
                str = "Failed to send abort command";
                break;
        }

        return str;
    }
}
