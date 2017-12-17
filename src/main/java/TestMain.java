
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ice4j.channel.DataChannel;
import org.ice4j.channel.EventListener;
import org.ice4j.channel.Transporter;

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
/**
 *
 * @author ruhul
 */
public class TestMain {

    public TestMain() {
    }

    public static byte[] getRandomBytes(int client) {
        Random rand = new Random(System.currentTimeMillis());
        String str = client + " <> "+rand.nextInt(1000) + ":abcdefgh";
        return str.getBytes();
    }

    static String readSDP() throws Throwable {
        System.out.println("Paste remote SDP here. Enter an empty "
                + "line to proceed:");
        System.out.println("(we don't mind the [java] prefix in SDP intput)");
        BufferedReader reader
                = new BufferedReader(new InputStreamReader(System.in));

        StringBuffer buff = new StringBuffer();
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.replace("[java]", "");
            line = line.trim();
            if (line.length() == 0) {
                break;
            }

            buff.append(line);
            buff.append("\r\n");
        }

        return buff.toString();
    }

    public static void main(String[] args) throws Throwable {        
        Random rand = new Random(System.currentTimeMillis());
        final int client = rand.nextInt();
        System.out.println("This client-> "+client);
                
        Transporter t = new Transporter(Arrays.asList("audio"), null, new EventListener() {
            boolean state = true;

            @Override
            public void onSuccess(final DataChannel channel) {
                System.out.println("##############################On success########################");
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (state) {
                            try {
                                
                                channel.send(getRandomBytes(client));
                                byte[] bytes = channel.receive();
//                                System.out.println("Received---> " + new String(bytes));
                                Thread.sleep(1000);
                            } catch (IOException ex) {
                                Logger.getLogger(Transporter.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Transporter.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });
                t.start();
            }

            @Override
            public void onFail() {
                state = false;
                System.out.println("Failed:");
                //      System.exit(0);
            }
        });

        String localSDP = t.getSDP();
        System.out.println("LocalSDP:\n" + localSDP);
        t.setRemoteSDP(readSDP());
        t.startConnectivity();
    }

}
