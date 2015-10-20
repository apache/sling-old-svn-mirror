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
package org.apache.sling.discovery.commons.providers;

import static org.junit.Assert.fail;

import org.junit.Test;

public class EventFactoryTest {

    BaseTopologyView newView() {
        return new DummyTopologyView();
    }
    
    @Test
    public void testInitEvent() throws Exception {
        try{
            EventFactory.newInitEvent(null);
            fail("should complain");
        } catch(Exception e) {
            // ok
        }
        EventFactory.newInitEvent(newView());
    }
    
    @Test
    public void testChangingEvent() throws Exception {
        try{
            EventFactory.newChangingEvent(null);
            fail("should complain");
        } catch(Exception e) {
            // ok
        }
        try{
            EventFactory.newChangingEvent(newView());
            fail("should complain");
        } catch(Exception e) {
            // ok
        }
        BaseTopologyView view = newView();
        view.setNotCurrent();
        EventFactory.newChangingEvent(view);
    }

    @Test
    public void testChangedEvent() throws Exception {
        try{
            EventFactory.newChangedEvent(null, null);
            fail("should complain");
        } catch(Exception e) {
            // ok
        }
        try{
            EventFactory.newChangedEvent(newView(), null);
            fail("should complain");
        } catch(Exception e) {
            // ok
        }
        try{
            EventFactory.newChangedEvent(null, newView());
            fail("should complain");
        } catch(Exception e) {
            // ok
        }
        try{
            EventFactory.newChangedEvent(newView(), newView());
            fail("should complain");
        } catch(Exception e) {
            // ok
        }
        BaseTopologyView oldView = newView();
        oldView.setNotCurrent();
        BaseTopologyView newView = newView();
        EventFactory.newChangedEvent(oldView, newView);
    }
    
    @Test
    public void testPropertiesEvent() throws Exception {
        try{
            EventFactory.newPropertiesChangedEvent(null, null);
            fail("should complain");
        } catch(Exception e) {
            // ok
        }
        try{
            EventFactory.newPropertiesChangedEvent(newView(), null);
            fail("should complain");
        } catch(Exception e) {
            // ok
        }
        try{
            EventFactory.newPropertiesChangedEvent(null, newView());
            fail("should complain");
        } catch(Exception e) {
            // ok
        }
        try{
            EventFactory.newPropertiesChangedEvent(newView(), newView());
            fail("should complain");
        } catch(Exception e) {
            // ok
        }
        BaseTopologyView oldView = newView();
        oldView.setNotCurrent();
        BaseTopologyView newView = newView();
        EventFactory.newPropertiesChangedEvent(oldView, newView);
    }
}
