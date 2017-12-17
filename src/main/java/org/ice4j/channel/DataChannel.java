/*
 * Copyright 2017 ruhul.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ice4j.channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.logging.Logger;
import org.ice4j.socket.IceSocketWrapper;

/**
 *
 * @author ruhul
 */
public class DataChannel {

    private static final Logger logger
            = Logger.getLogger(DataChannel.class.getName());
    private DatagramPacket snd;
    private DatagramPacket rcv;
    private IceSocketWrapper wrapper;
    public final int MAX_BYTE_LENGTH = 1000;

    public DataChannel(final IceSocketWrapper wrapper, final InetAddress hostname, final int port) {
        this.wrapper = wrapper;
        snd = new DatagramPacket(new byte[MAX_BYTE_LENGTH], MAX_BYTE_LENGTH);
        rcv = new DatagramPacket(new byte[MAX_BYTE_LENGTH], MAX_BYTE_LENGTH);
        snd.setAddress(hostname);
        snd.setPort(port);
    }

    public void send(byte[] data) throws IOException {
//        logger.log(Level.INFO, "Sending DATA->:{0}", snd.getSocketAddress().toString());
        System.out.println("sent:"+new String(data));
        snd.setData(data);
        wrapper.send(snd);
    }

    public byte[] receive() throws IOException {
//        logger.log(Level.INFO, "waiting for data:");
        wrapper.receive(rcv);
//        logger.log(Level.INFO, "DATA received->:{0}", rcv.getSocketAddress());

        System.out.println("recv:"+new String(rcv.getData()));
        return rcv.getData();
    }

    void close() {
        wrapper.close();
        snd = null;
        rcv = null;
    }
}
