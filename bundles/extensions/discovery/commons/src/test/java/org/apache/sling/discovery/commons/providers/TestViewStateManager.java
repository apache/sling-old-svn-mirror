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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.ViewStateManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestViewStateManager {

    private class Listener implements TopologyEventListener {

        private List<TopologyEvent> events = new LinkedList<TopologyEvent>();
        private TopologyEvent lastEvent;
        
        public synchronized void handleTopologyEvent(TopologyEvent event) {
            events.add(event);
            lastEvent = event;
        }
        
        public synchronized List<TopologyEvent> getEvents() {
            return Collections.unmodifiableList(events);
        }

        public synchronized int countEvents() {
            return events.size();
        }
        
        public synchronized TopologyEvent getLastEvent() {
            return lastEvent;
        }

        public synchronized void clearEvents() {
            events.clear();
        }

        public BaseTopologyView getLastView() {
            if (lastEvent==null) {
                return null;
            } else {
                switch(lastEvent.getType()) {
                case TOPOLOGY_INIT:
                case PROPERTIES_CHANGED:
                case TOPOLOGY_CHANGED: {
                    return (BaseTopologyView) lastEvent.getNewView();
                }
                case TOPOLOGY_CHANGING:{
                    return (BaseTopologyView) lastEvent.getOldView();
                }
                default: {
                    fail("no other types supported yet");
                }
                }
            }
            return null;
        }
        
    }
    
    private class View extends BaseTopologyView {
        
        private final BaseTopologyView clonedView;

        public View() {
            clonedView = null;
        }
        
        public View(BaseTopologyView clonedView) {
            this.clonedView = clonedView;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof View)) {
                return false;
            }
            final View other = (View) obj;
            if (clonedView!=null) {
                if (obj==clonedView) {
                    return true;
                }
            } else if (other.clonedView==this) {
                return true;
            }
            return super.equals(obj);
        }
        
        @Override
        public int hashCode() {
            if (clonedView!=null) {
                return clonedView.hashCode();
            }
            return super.hashCode();
        }

        public View addInstance() {
            return this;
        }

        public InstanceDescription getLocalInstance() {
            throw new IllegalStateException("not yet implemented");
        }

        public Set<InstanceDescription> getInstances() {
            throw new IllegalStateException("not yet implemented");
        }

        public Set<InstanceDescription> findInstances(InstanceFilter filter) {
            throw new IllegalStateException("not yet implemented");
        }

        public Set<ClusterView> getClusterViews() {
            throw new IllegalStateException("not yet implemented");
        }
    }
    
    private ViewStateManager mgr;
    
    private Random defaultRandom;

    @Before
    public void setup() throws Exception {
        mgr = new ViewStateManager();
        defaultRandom = new Random(1234123412); // I want randomness yes, but deterministic, for some methods at least
    }
    
    @After
    public void teardown() throws Exception {
        mgr = null;
        defaultRandom= null;
    }
    
    private void assertNoEvents(Listener listener) {
        assertEquals(0, listener.countEvents());
    }
    
    private void assertEvents(Listener listener, TopologyEvent... events) {
        assertEquals(events.length, listener.countEvents());
        for (int i = 0; i < events.length; i++) {
            TopologyEvent e = events[i];
            assertEquals(e.getType(), listener.getEvents().get(i).getType());
            switch(e.getType()) {
            case TOPOLOGY_INIT: {
                assertNull(listener.getEvents().get(i).getOldView());
                assertEquals(e.getNewView(), listener.getEvents().get(i).getNewView());
                break;
            }
            case TOPOLOGY_CHANGING: {
                assertEquals(e.getOldView(), listener.getEvents().get(i).getOldView());
                assertNull(listener.getEvents().get(i).getNewView());
                break;
            }
            case PROPERTIES_CHANGED:
            case TOPOLOGY_CHANGED: {
                assertEquals(e.getOldView(), listener.getEvents().get(i).getOldView());
                assertEquals(e.getNewView(), listener.getEvents().get(i).getNewView());
                break;
            }
            default: {
                fail("no other type supported yet");
            }
            }
        }
        listener.clearEvents();
    }

    /** does couple loops randomly calling handleChanging() (or not) and then handleNewView().
     * Note: random is passed to allow customizing and not hardcoding this method to a particular random **/
    private void randomEventLoop(final Random random, Listener... listeners) {
        for(int i=0; i<100; i++) {
            final boolean shouldCallChanging = random.nextBoolean();
            if (shouldCallChanging) {
                // dont always do a changing
                mgr.handleChanging();
                for(int j=0; j<listeners.length; j++) {
                    assertEvents(listeners[j], ViewStateManager.newChangingEvent(listeners[j].getLastView()));
                }
            } else {
                for(int j=0; j<listeners.length; j++) {
                    assertNoEvents(listeners[j]);
                }
            }
            final BaseTopologyView view = new View().addInstance();
            BaseTopologyView[] lastViews = new BaseTopologyView[listeners.length];
            for(int j=0; j<listeners.length; j++) {
                lastViews[j] = listeners[j].getLastView();
            }
            mgr.handleNewView(view);
            if (!shouldCallChanging) {
                // in that case I should still get a CHANGING - by contract
                for(int j=0; j<listeners.length; j++) {
                    assertEvents(listeners[j], ViewStateManager.newChangingEvent(lastViews[j]), ViewStateManager.newChangedEvent(lastViews[j], view));
                }
            } else {
                for(int j=0; j<listeners.length; j++) {
                    assertEvents(listeners[j], ViewStateManager.newChangedEvent(lastViews[j], view));
                }
            }
        }
    }
    
    @Test
    public void testDuplicateListeners() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        mgr.bind(listener); // we should be generous and allow duplicate registration
        assertTrue(mgr.unbind(listener));
        assertFalse(mgr.unbind(listener));
        
        mgr.handleActivated();
        assertFalse(mgr.unbind(listener));
        mgr.bind(listener);
        mgr.bind(listener); // we should be generous and allow duplicate registration
        assertTrue(mgr.unbind(listener));
        assertFalse(mgr.unbind(listener));
    }
    
    @Test
    public void testBindActivateChangingChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        assertNoEvents(listener);
        mgr.handleActivated();
        assertNoEvents(listener);
        mgr.handleChanging();
        assertNoEvents(listener);
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, ViewStateManager.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangingActivateChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        assertNoEvents(listener);
        mgr.handleChanging();
        assertNoEvents(listener);
        mgr.handleActivated();
        assertNoEvents(listener);
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, ViewStateManager.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangingChangedActivate() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        assertNoEvents(listener);
        mgr.handleChanging();
        assertNoEvents(listener);
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertNoEvents(listener);
        mgr.handleActivated();
        assertEvents(listener, ViewStateManager.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangingChangedChangingActivate() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        assertNoEvents(listener);
        mgr.handleChanging();
        assertNoEvents(listener);
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertNoEvents(listener);
        mgr.handleChanging();
        assertNoEvents(listener);
        mgr.handleActivated();
        assertNoEvents(listener);
        final BaseTopologyView view2 = new View().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, ViewStateManager.newInitEvent(view2));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindChangedChangingActivate() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        assertNoEvents(listener);
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertNoEvents(listener);
        mgr.handleChanging();
        assertNoEvents(listener);
        mgr.handleActivated();
        assertNoEvents(listener);
        final BaseTopologyView view2 = new View().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, ViewStateManager.newInitEvent(view2));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testActivateBindChangingChanged() throws Exception {
        final Listener listener = new Listener();
        // first activate
        mgr.handleActivated();
        assertNoEvents(listener); // paranoia
        // then bind
        mgr.bind(listener);
        assertNoEvents(listener); // there was no changing or changed yet
        mgr.handleChanging();
        assertNoEvents(listener);
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, ViewStateManager.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }

    @Test
    public void testActivateChangingBindChanged() throws Exception {
        final Listener listener = new Listener();
        // first activate
        mgr.handleActivated();
        assertNoEvents(listener); // paranoia
        mgr.handleChanging();
        assertNoEvents(listener); // no listener yet
        // then bind
        mgr.bind(listener);
        assertNoEvents(listener); // no changed event yet
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, ViewStateManager.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }

    @Test
    public void testActivateChangingChangedBind() throws Exception {
        final Listener listener = new Listener();
        // first activate
        mgr.handleActivated();
        assertNoEvents(listener); // paranoia
        mgr.handleChanging();
        assertNoEvents(listener); // no listener yet
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertNoEvents(listener); // no listener yet
        // then bind
        mgr.bind(listener);
        assertEvents(listener, ViewStateManager.newInitEvent(view));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindActivateBindChangingChanged() throws Exception {
        final Listener listener1 = new Listener();
        final Listener listener2 = new Listener();
        
        mgr.bind(listener1);
        assertNoEvents(listener1);
        mgr.handleActivated();
        assertNoEvents(listener1);
        mgr.bind(listener2);
        assertNoEvents(listener1);
        assertNoEvents(listener2);
        mgr.handleChanging();
        assertNoEvents(listener1);
        assertNoEvents(listener2);
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener1, ViewStateManager.newInitEvent(view));
        assertEvents(listener2, ViewStateManager.newInitEvent(view));
        
        randomEventLoop(defaultRandom, listener1, listener2);
    }

    @Test
    public void testBindActivateChangingBindChanged() throws Exception {
        final Listener listener1 = new Listener();
        final Listener listener2 = new Listener();
        
        mgr.bind(listener1);
        assertNoEvents(listener1);
        mgr.handleActivated();
        assertNoEvents(listener1);
        mgr.handleChanging();
        assertNoEvents(listener1);
        mgr.bind(listener2);
        assertNoEvents(listener1);
        assertNoEvents(listener2);
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener1, ViewStateManager.newInitEvent(view));
        assertEvents(listener2, ViewStateManager.newInitEvent(view));

        randomEventLoop(defaultRandom, listener1, listener2);
    }
    
    @Test
    public void testActivateBindChangingDuplicateHandleNewView() throws Exception {
        final Listener listener = new Listener();
        mgr.handleActivated();
        mgr.bind(listener);
        mgr.handleChanging();
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener, ViewStateManager.newInitEvent(view));
        mgr.handleNewView(clone(view));
        assertNoEvents(listener);
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testActivateBindChangingChangedBindDuplicateHandleNewView() throws Exception {
        final Listener listener1 = new Listener();
        mgr.handleActivated();
        mgr.bind(listener1);
        mgr.handleChanging();
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertEvents(listener1, ViewStateManager.newInitEvent(view));
        
        final Listener listener2 = new Listener();
        mgr.bind(listener2);
        mgr.handleNewView(clone(view));
        assertNoEvents(listener1);
        assertEvents(listener2, ViewStateManager.newInitEvent(view));
        randomEventLoop(defaultRandom, listener1, listener2);
    }
    
    @Test
    public void testActivateChangedBindDuplicateHandleNewView() throws Exception {
        final Listener listener = new Listener();
        mgr.handleActivated();
        assertNoEvents(listener);
        final BaseTopologyView view = new View().addInstance();
        mgr.handleNewView(view);
        assertNoEvents(listener);
        mgr.bind(listener);
        assertEvents(listener, ViewStateManager.newInitEvent(view));
        mgr.handleNewView(clone(view));
        assertNoEvents(listener);
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindActivateChangedChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.handleActivated();
        assertNoEvents(listener);
        mgr.bind(listener);
        assertNoEvents(listener);
        mgr.handleChanging();
        assertNoEvents(listener);
        final BaseTopologyView view1 = new View().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, ViewStateManager.newInitEvent(view1));
        final BaseTopologyView view2 = new View().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, ViewStateManager.newChangingEvent(view1), ViewStateManager.newChangedEvent(view1, view2));
        randomEventLoop(defaultRandom, listener);
    }
    
    @Test
    public void testBindActivateChangedDeactivateChangingActivateChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        assertNoEvents(listener);
        mgr.handleActivated();
        assertNoEvents(listener);
        final BaseTopologyView view1 = new View().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, ViewStateManager.newInitEvent(view1));
        mgr.handleDeactivated();
        assertNoEvents(listener);
        mgr.handleChanging();
        assertNoEvents(listener);
        mgr.bind(listener); // need to bind again after deactivate
        mgr.handleActivated();
        assertNoEvents(listener);
        final BaseTopologyView view2 = new View().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, ViewStateManager.newInitEvent(view2));
    }

    @Test
    public void testBindActivateChangedDeactivateChangedActivateChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        assertNoEvents(listener);
        mgr.handleActivated();
        assertNoEvents(listener);
        final BaseTopologyView view1 = new View().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, ViewStateManager.newInitEvent(view1));
        mgr.handleDeactivated();
        assertNoEvents(listener);
        final BaseTopologyView view2 = new View().addInstance();
        mgr.handleNewView(view2);
        assertNoEvents(listener);
        mgr.bind(listener); // need to bind again after deactivate
        mgr.handleActivated();
        assertEvents(listener, ViewStateManager.newInitEvent(view2));
        final BaseTopologyView view3 = new View().addInstance();
        mgr.handleNewView(view3);
        assertEvents(listener, ViewStateManager.newChangingEvent(view2), ViewStateManager.newChangedEvent(view2, view3));
    }

    @Test
    public void testBindActivateChangedChangingDeactivateActivateChangingChanged() throws Exception {
        final Listener listener = new Listener();
        mgr.bind(listener);
        assertNoEvents(listener);
        mgr.handleActivated();
        assertNoEvents(listener);
        final BaseTopologyView view1 = new View().addInstance();
        mgr.handleNewView(view1);
        assertEvents(listener, ViewStateManager.newInitEvent(view1));
        mgr.handleChanging();
        assertEvents(listener, ViewStateManager.newChangingEvent(view1));
        mgr.handleDeactivated();
        assertNoEvents(listener);
        mgr.bind(listener); // need to bind again after deactivate
        mgr.handleActivated();
        assertNoEvents(listener);
        mgr.handleChanging();
        assertNoEvents(listener);
        final BaseTopologyView view2 = new View().addInstance();
        mgr.handleNewView(view2);
        assertEvents(listener, ViewStateManager.newInitEvent(view2));
    }

    private BaseTopologyView clone(final BaseTopologyView view) {
        return new View(view);
    }
}
