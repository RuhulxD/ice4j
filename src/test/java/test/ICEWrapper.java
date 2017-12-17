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

package test;

import java.util.List;
import org.ice4j.ice.Agent;
import org.ice4j.ice.harvest.CandidateHarvester;

/**
 *
 * @author ruhul
 */
public class ICEWrapper extends Ice{
    Agent agent;
    int State;
    public ICEWrapper(List<CandidateHarvester> harvesters, EventListener listener) throws Throwable {
        agent = createAgent(9090, false, harvesters);
        
        
    }
    
    public void startCall(){
        
    }
    
    
    public static void main(String[] args) {   
        
    }
    private final static class STATES{
        final int NOT_INITIALIZED=0;
        final int ICE_STARTED=1;
        final int ICE_WAITING=2;
        final int ICE_SUCCESS=3;
        final int ICE_FAILED=4;
    }       
}