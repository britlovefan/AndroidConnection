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
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

public class TruconnectCommandQueue implements Serializable
{
    private Queue<TruconnectCommandRequest> mQueue;
    private int mNextID = 0;
    public static final int ID_MAX = Integer.MAX_VALUE;
    public static final int ID_INVALID = -1;

    public TruconnectCommandQueue()
    {
        mQueue = new LinkedList<TruconnectCommandRequest>();
    }

    public int add(TruconnectCommand command, String command_string)
    {
        int ID = getNextID();

        TruconnectCommandRequest request = new TruconnectCommandRequest(ID, command, command_string);

        //return valid ID if add successful
        if (mQueue.add(request))
        {
            return ID;
        }
        else
        {
            return ID_INVALID;
        }
    }

    public TruconnectCommandRequest getNext()
    {
        try
        {
            return mQueue.remove();
        }
        catch (NoSuchElementException e)
        {
            return null;
        }
    }

    public void clear()
    {
        mQueue.clear();
    }

    private int getNextID()
    {
        if (mNextID < ID_MAX)
        {
            mNextID++;
        }
        else
        {
            mNextID = 0;
        }

        return mNextID;
    }
}
