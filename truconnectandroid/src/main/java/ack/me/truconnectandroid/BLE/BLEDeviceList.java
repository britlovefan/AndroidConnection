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

public class BLEDeviceList implements Serializable
{
    private SearchableList<BLEDevice> mList;

    public BLEDeviceList(SearchableList<BLEDevice> list)
    {
        mList = list;
    }

    public boolean add(BLEDevice device)
    {
        boolean result = false;

        if (device != null && (mList.get(device.getName()) == null))
        {
            result = mList.add(device);
        }

        return result;
    }

    public BLEDevice get(int index)
    {
        if (index < 0 || mList.size() <= 0)
            return null;

        return mList.get(index);
    }

    public BLEDevice get(String name)
    {
        if (name == "")
            return null;

        return mList.get(name);
    }

    public void clear()
    {
        mList.clear();
    }

    public int size()
    {
        return mList.size();
    }
}
