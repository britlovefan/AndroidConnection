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

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseParser implements Serializable
{
    public static final int ERROR_INCOMPLETE_RESPONSE = 1;
    public final String LINE_END = "\r\n";

    private ResponseHeader mResponseHeader = new ResponseHeader(-1, -1);
    private String mReadBuffer = "";

    private class ResponseHeader
    {
        int status;
        int length;

        ResponseHeader(int status, int length)
        {
            this.status = status;
            this.length = length;
        }

        public int getStatus()
        {
            return status;
        }

        public void setStatus(int status)
        {
            this.status = status;
        }

        public int getLength()
        {
            return length;
        }

        public void setLength(int length)
        {
            this.length = length;
        }
    }

    public void clearBuffer()
    {
        mReadBuffer = "";
    }

    public void addToBuffer(String data)
    {
        String filteredData = stripControlChars(data);
        mReadBuffer = mReadBuffer.concat(filteredData);
    }

    //finds response header and retuns response data if found
    //returns null if response not found
    public TruconnectResult parseResponse()
    {
        TruconnectResult result = null;
        do
        {
            String nextLine = getNextReadBufferLine();
            if (nextLine != null)
            {
                if (responseHeaderFound())
                {
                    result = new TruconnectResult(mResponseHeader.getStatus(), nextLine);

                    if (!isResponseLengthCorrect(nextLine))
                    {
                        result.setResponseCode(TruconnectResult.INCOMPLETE_RESPONSE);
                    }

                    resetParser();
                }
                else
                {
                    mResponseHeader = parseHeader(nextLine);
                }
                removeCurrentReadBufferLine();
            }
        } while (readBufferHasLine());

        return result;
    }

    private String stripControlChars(String str)
    {
        return str.replaceAll("[^\\r\\n\\x20-\\x7f]", "");
    }

    /* Returns response header size, -1 if no header found */
    private ResponseHeader parseHeader(String line)
    {
        ResponseHeader header = new ResponseHeader(-1, -1);

        Pattern header_pattern = Pattern.compile("R[0-9]{6}");
        Matcher matcher = header_pattern.matcher(line);
        if (matcher.find())
        {
            String headerStr = matcher.group();
            header.status = Integer.parseInt(headerStr.substring(1, 2));
            header.length = Integer.parseInt(headerStr.substring(2));
        }

        return header;
    }

    private String getNextReadBufferLine()
    {
        String nextLine = null;
        int index = mReadBuffer.indexOf(LINE_END);

        if (index != -1)
        {
            nextLine = mReadBuffer.substring(0, index);

        }

        return nextLine;
    }

    private boolean readBufferHasLine()
    {
        return mReadBuffer.contains(LINE_END);
    }

    private boolean responseHeaderFound()
    {
        return mResponseHeader.getLength() > -1;
    }

    private void resetParser()
    {
        clearBuffer();
        mResponseHeader.setLength(-1);
    }

    private void removeCurrentReadBufferLine()
    {
        mReadBuffer = mReadBuffer.replaceFirst(".*\\r\\n","");
    }

    private boolean isResponseLengthCorrect(String response)
    {
        return (response.length() == (mResponseHeader.getLength() - LINE_END.length()));
    }
}
