/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.discovery.impl.cluster.helpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssertingTopologyEventListener implements TopologyEventListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<TopologyEventAsserter> expectedEvents = new LinkedList<TopologyEventAsserter>();

    private String debugInfo = null;
    
    private String errorMsg = null;
    
    public AssertingTopologyEventListener() {
    }

    public AssertingTopologyEventListener(String debugInfo) {
        this.debugInfo = debugInfo;
    }
    
    @Override
    public String toString() {
        return super.toString()+"-[debugInfo="+debugInfo+"]";
    }
    
    private List<TopologyEvent> events_ = new LinkedList<TopologyEvent>();

    private List<TopologyEvent> unexpectedEvents_ = new LinkedList<TopologyEvent>();

    public void handleTopologyEvent(TopologyEvent event) {
        final String logPrefix = "handleTopologyEvent["+(debugInfo!=null ? debugInfo : "this="+this) +"] ";
        logger.info(logPrefix + "got event=" + event);
        TopologyEventAsserter asserter = null;
        synchronized (expectedEvents) {
            if (expectedEvents.size() == 0) {
                unexpectedEvents_.add(event);
                throw new IllegalStateException(
                        "no expected events anymore. But got: " + event);
            }
            asserter = expectedEvents.remove(0);
        }
        if (asserter == null) {
            throw new IllegalStateException("this should not occur");
        }
        try{
            asserter.assertOk(event);
            logger.info(logPrefix + "event matched expectations (" + event+")");
        } catch(RuntimeException re) {
            synchronized(expectedEvents) {
                unexpectedEvents_.add(event);
            }
            throw re;
        } catch(Error er) {
            synchronized(expectedEvents) {
                unexpectedEvents_.add(event);
            }
            throw er;
        }
        try{
        switch(event.getType()) {
        case PROPERTIES_CHANGED: {
            assertNotNull(event.getOldView());
            assertNotNull(event.getNewView());
            assertTrue(event.getNewView().isCurrent());
            assertFalse(event.getOldView().isCurrent());
            break;
        }
        case TOPOLOGY_CHANGED: {
            assertNotNull(event.getOldView());
            assertNotNull(event.getNewView());
            assertTrue(event.getNewView().isCurrent());
            assertFalse(event.getOldView().isCurrent());
            break;
        }
        case TOPOLOGY_CHANGING: {
            assertNotNull(event.getOldView());
            assertNull(event.getNewView());
            assertFalse(event.getOldView().isCurrent());
            break;
        }
        case TOPOLOGY_INIT: {
            assertNull(event.getOldView());
            assertNotNull(event.getNewView());
            // cannot make any assertions on event.getNewView().isCurrent()
            // as that can be true or false
            break;
        }
        }
        } catch(RuntimeException re) {
            logger.error("RuntimeException: "+re, re);
            throw re;
        } catch(AssertionError e) {
            logger.error("AssertionError: "+e, e);
            throw e;
        }
        events_.add(event);
    }

    public List<TopologyEvent> getEvents() {
        return events_;
    }

    public void addExpected(Type expectedType) {
        addExpected(new AcceptsParticularTopologyEvent(expectedType));
    }

    public void addExpected(TopologyEventAsserter topologyEventAsserter) {
        expectedEvents.add(topologyEventAsserter);
    }

    public int getRemainingExpectedCount() {
        return expectedEvents.size();
    }
    
    public int getUnexpectedCount() {
        return unexpectedEvents_.size();
    }

    public void dump() {
        StringBuffer ue = new StringBuffer();
        if (unexpectedEvents_.size()>0) {
            for (Iterator<TopologyEvent> it = unexpectedEvents_.iterator(); it.hasNext();) {
                TopologyEvent topologyEvent = it.next();
                ue.append(topologyEvent+", ");
            }
            unexpectedEvents_.iterator();
        }
        logger.info("dump: got "+events_.size()+" events, "+unexpectedEvents_.size()+" (details: "+ue+") thereof unexpected. My list of expected events contains "+expectedEvents.size());
    }
}