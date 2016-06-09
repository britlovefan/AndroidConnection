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
import java.util.regex.Pattern;

public class FirmwareVersion implements Serializable
{
    private static final int MIN_VERSION_STR_LEN = 4;

    private int major;
    private int minor;
    private int patch;
    private int rc;
    private String versionString;

    public FirmwareVersion(int major, int minor, int patch, int rc)
    {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.rc = rc;
    }

    public FirmwareVersion(String version)
    {
        versionString = filterString(version);

        parseString();
    }

    //return substring of original string up until the first invalid character ie not 0-9 or .
    private static String filterString(String str)
    {
        int end = str.length() + 1;

        for (int i=0; i<str.length(); i++)
        {
            if (!Pattern.matches("[0-9.]", str.substring(i, i+1)) &&
                !Pattern.matches("[#0-9a-fA-F]", str.substring(i, i+1)))//could be a git hash
            {
                end = i;
                break;
            }
        }

        return str.substring(0, end);
    }

    private boolean parseString()
    {
        boolean result = false;

        String[] tokens = versionString.split("[.]");
        if (tokens.length >= MIN_VERSION_STR_LEN)
        {
            major = Integer.parseInt(tokens[0]);
            minor = Integer.parseInt(tokens[1]);
            patch = Integer.parseInt(tokens[2]);
            rc = Integer.parseInt(tokens[3]);

            result = true;
        }

        return result;
    }

    public boolean isEqualTo(FirmwareVersion version)
    {
        if (version.major == this.major &&
            version.minor == this.minor &&
            version.patch == this.patch &&
            version.rc == this.rc)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public String toString()
    {
        if (versionString != null)
            return versionString;
        else
            return String.format("%d.%d.%d.%d", major, minor, patch, rc);
    }
}
