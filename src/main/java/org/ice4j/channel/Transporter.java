/*
 * ice4j, the OpenSource Java Solution for NAT and Firewall Traversal.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ice4j.channel;

import java.beans.*;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.ice.*;
import org.ice4j.ice.harvest.*;
import org.ice4j.security.*;
import org.ice4j.socket.IceSocketWrapper;

/**
 * Simple ice4j testing scenario. The sample application would create and start
 * both agents and make one of them run checks against the other.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class Transporter {

    /**
     * The <tt>Logger</tt> used by the <tt>Ice</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
            = Logger.getLogger(Transporter.class.getName());

    /**
     * Start time for debugging purposes.
     */
    static long startTime;

    private final Agent agent;

    private EventListener event;
    Random rand = new Random(System.currentTimeMillis());


    /**
     * Runs the test
     *
     * @param channelNames
     * @param harvesters
     * @param evt
     *
     * @throws Throwable if bad stuff happens.
     */
    public Transporter(List<String> channelNames, List<CandidateHarvester> harvesters, EventListener evt) throws Throwable {
        agent = createAgent(9090+rand.nextInt(100), false, harvesters, channelNames);
        this.event = evt;
        agent.setControlling(false);
        agent.addStateChangeListener(new IceProcessingListener());
    }

    public String getSDP() throws Throwable {
        return SdpUtils.createSDPDescription(agent);
    }

    public void setRemoteSDP(String remoteSDP) throws Throwable {
        SdpUtils.parseSDP(agent, remoteSDP);
    }

    public void startConnectivity() {
        agent.startConnectivityEstablishment();
    }

    

    /**
     * The listener that would end example execution once we enter the completed
     * state.
     */
    public final class IceProcessingListener
            implements PropertyChangeListener {

        /**
         * System.exit()s as soon as ICE processing enters a final state.
         *
         * @param evt the {@link PropertyChangeEvent} containing the old and new
         * states of ICE processing.
         */
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            long processingEndTime = System.currentTimeMillis();

            Object iceProcessingState = evt.getNewValue();

            logger.info("Agent entered the " + iceProcessingState + " state.");
            if (iceProcessingState == IceProcessingState.COMPLETED) {
                logger.info("Total ICE processing time: "
                        + (processingEndTime - startTime) + "ms");
                Agent agent = (Agent) evt.getSource();
                List<IceMediaStream> streams = agent.getStreams();

                for (IceMediaStream stream : streams) {
                    String streamName = stream.getName();
                    logger.info(
                            "Pairs selected for stream: " + streamName);
                    List<Component> components = stream.getComponents();

                    for (Component cmp : components) {
                        String cmpName = cmp.getName();
                        logger.info(cmpName + ": "
                                + cmp.getSelectedPair());
                    }
                }

                logger.info("Printing the completed check lists:");
                for (IceMediaStream stream : streams) {
                    String streamName = stream.getName();
                    logger.info("Check list for  stream: " + streamName);
                    //uncomment for a more verbose output
                    logger.info(stream.getCheckList().toString());
                }

                logger.info("Total ICE processing time to completion: "
                        + (System.currentTimeMillis() - startTime));
            } else if (iceProcessingState == IceProcessingState.TERMINATED) {
                Agent agent = (Agent) evt.getSource();
                for (IceMediaStream stream : agent.getStreams()) {
                    Component rtpComponent = stream.getComponent(org.ice4j.ice.Component.RTP);
                    // We use IceSocketWrapper, but you can just use the UDP socket
                    // The advantage is that you can change the protocol from UDP to TCP easily
                    // Currently only UDP exists so you might not need to use the wrapper.
                    IceSocketWrapper wrapper = rtpComponent.getSocketWrapper();
                    // Get information about remote address for packet settings
                    TransportAddress ta = rtpComponent.getRemoteCandidates().get(0).getTransportAddress();
                    InetAddress hostname = ta.getAddress();
                    int port = ta.getPort();
                    DataChannel channel = new DataChannel(wrapper, hostname, port);
                    event.onSuccess(channel);
                }
            } else if (iceProcessingState == IceProcessingState.FAILED) {
                /*
                 * Though the process will be instructed to die, demonstrate
                 * that Agent instances are to be explicitly prepared for
                 * garbage collection.
                 */
                ((Agent) evt.getSource()).free();
                logger.info("Total ICE processing time: "
                        + (System.currentTimeMillis() - startTime));
                event.onFail();
//                System.exit(0);
            }
        }
    }

    /**
     * Creates a vanilla ICE <tt>Agent</tt> and adds to it an audio and a video
     * stream with RTP and RTCP components.
     *
     * @param rtpPort the port that we should try to bind the RTP component on
     * (the RTCP one would automatically go to rtpPort + 1)
     * @return an ICE <tt>Agent</tt> with an audio stream with RTP and RTCP
     * components.
     *
     * @throws Throwable if anything goes wrong.
     */
    protected static Agent createAgent(int rtpPort)
            throws Throwable {
        return createAgent(rtpPort, false);
    }

    /**
     * Creates an ICE <tt>Agent</tt> (vanilla or trickle, depending on the value
     * of <tt>isTrickling</tt>) and adds to it an audio and a video stream with
     * RTP and RTCP components.
     *
     * @param rtpPort the port that we should try to bind the RTP component on
     * (the RTCP one would automatically go to rtpPort + 1)
     * @return an ICE <tt>Agent</tt> with an audio stream with RTP and RTCP
     * components.
     * @param isTrickling indicates whether the newly created agent should be
     * performing trickle ICE.
     *
     * @throws Throwable if anything goes wrong.
     */
    protected static Agent createAgent(int rtpPort, boolean isTrickling)
            throws Throwable {
        return createAgent(rtpPort, isTrickling, null, Arrays.asList("audio"));
    }

    /**
     * Creates an ICE <tt>Agent</tt> (vanilla or trickle, depending on the value
     * of <tt>isTrickling</tt>) and adds to it an audio and a video stream with
     * RTP and RTCP components.
     *
     * @param rtpPort the port that we should try to bind the RTP component on
     * (the RTCP one would automatically go to rtpPort + 1)
     * @param streams
     * @return an ICE <tt>Agent</tt> with an audio stream with RTP and RTCP
     * components.
     * @param isTrickling indicates whether the newly created agent should be
     * performing trickle ICE.
     * @param harvesters the list of {@link CandidateHarvester}s that the new
     * agent should use or <tt>null</tt> if it should include the default ones.
     *
     * @throws Throwable if anything goes wrong.
     */
    protected static Agent createAgent(int rtpPort,
            boolean isTrickling,
            List<CandidateHarvester> harvesters, List<String> streams)
            throws Throwable {
        long startTime = System.currentTimeMillis();
        Agent agent = new Agent();
        agent.setTrickling(isTrickling);

        if (harvesters == null) {
            // STUN
//            StunCandidateHarvester stunHarv = new StunCandidateHarvester(
//                    new TransportAddress("35.193.188.252", 3478, Transport.UDP));
//            StunCandidateHarvester stun6Harv = new StunCandidateHarvester(
//                    new TransportAddress("35.193.188.252", 3478, Transport.UDP));
//
//            agent.addCandidateHarvester(stunHarv);
//            agent.addCandidateHarvester(stun6Harv);

            // TURN
            String[] hostnames = new String[]{
                "35.193.188.252"
//                    ,
//                                    "stun6.jitsi.net"
            };
            int port = 3478;
            LongTermCredential longTermCredential
                    = new LongTermCredential("asdf", "123456");

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
        } else {
            for (CandidateHarvester harvester : harvesters) {
                agent.addCandidateHarvester(harvester);
            }
        }

        //STREAMS
        for (String stream : streams) {
            createStream(rtpPort, stream, agent);
        }
        long endTime = System.currentTimeMillis();
        long total = endTime - startTime;

        logger.info("Total harvesting time: " + total + "ms.");

        return agent;
    }

    /**
     * Creates an <tt>IceMediaStream</tt> and adds to it an RTP and and RTCP
     * component.
     *
     * @param rtpPort the port that we should try to bind the RTP component on
     * (the RTCP one would automatically go to rtpPort + 1)
     * @param streamName the name of the stream to create
     * @param agent the <tt>Agent</tt> that should create the stream.
     *
     * @return the newly created <tt>IceMediaStream</tt>.
     * @throws Throwable if anything goes wrong.
     */
    private static IceMediaStream createStream(int rtpPort,
            String streamName,
            Agent agent)
            throws Throwable {
        IceMediaStream stream = agent.createMediaStream(streamName);

        long startTime = System.currentTimeMillis();

        //TODO: component creation should probably be part of the library. it
        //should also be started after we've defined all components to be
        //created so that we could run the harvesting for everyone of them
        //simultaneously with the others.
        //rtp
        agent.createComponent(
                stream, Transport.UDP, rtpPort, rtpPort, rtpPort + 100);

        long endTime = System.currentTimeMillis();
        logger.info("RTP Component created in "
                + (endTime - startTime) + " ms");
        startTime = endTime;
        //rtcpComp
        agent.createComponent(
                stream, Transport.UDP, rtpPort + 1, rtpPort + 1, rtpPort + 101);

        endTime = System.currentTimeMillis();
        logger.info("RTCP Component created in "
                + (endTime - startTime) + " ms");

        return stream;
    }
}
