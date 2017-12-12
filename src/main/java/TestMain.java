
import java.io.IOException;
import java.net.InetAddress;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.ice.harvest.UPNPHarvester;
import org.ice4j.security.LongTermCredential;

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

    public static void main(String[] args) throws IllegalArgumentException, IOException {
        Agent agent = new Agent(); // A simple ICE Agent
        String[] hostnames = new String[]{
            "35.193.188.252"
        };
        int port = 3478;
        LongTermCredential longTermCredential
                = new LongTermCredential("ruhul1", "123456");

        for (String hostname : hostnames) {
            agent.addCandidateHarvester(
                    new TurnCandidateHarvester(
                            new TransportAddress(
                                    hostname, port, Transport.UDP),
                            longTermCredential));
        }

        //UPnP: adding an UPnP harvester because they are generally slow
        //which makes it more convenient to test things like trickle.
        agent.addCandidateHarvester(new UPNPHarvester());
        
        
        IceMediaStream stream = agent.createMediaStream("audio");
        port = 5000; // Choose any port
        agent.createComponent(stream, Transport.UDP, port, port, port+100);
        
        

    }
}
