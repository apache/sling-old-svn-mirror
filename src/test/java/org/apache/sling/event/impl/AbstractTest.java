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
package org.apache.sling.event.impl;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junitx.util.PrivateAccessor;

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.threads.ModifiableThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.settings.SlingSettingsService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.runner.RunWith;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@RunWith(JMock.class)
public abstract class AbstractTest {

    protected static final String REPO_PATH = "/test/events";
    protected static final String SLING_ID = "4711";

    protected static Session session;

    protected abstract Mockery getMockery();

    protected EnvironmentComponent environment;

    protected Hashtable<String, Object> getComponentConfig() {
        final Hashtable<String, Object> config = new Hashtable<String, Object>();
        config.put(AbstractRepositoryEventHandler.CONFIG_PROPERTY_REPO_PATH, REPO_PATH);

        return config;
    }

    @org.junit.BeforeClass public static void setupRepository() throws Exception {
        RepositoryTestUtil.startRepository();
        final SlingRepository repository = RepositoryTestUtil.getSlingRepository();
        session = repository.loginAdministrative(repository.getDefaultWorkspace());
        assertTrue(RepositoryTestUtil.registerNodeType(session, DistributingEventHandler.class.getResourceAsStream("/SLING-INF/nodetypes/event.cnd")));
        assertTrue(RepositoryTestUtil.registerNodeType(session, DistributingEventHandler.class.getResourceAsStream("/SLING-INF/nodetypes/folder.cnd")));
    }

    @org.junit.AfterClass public static void shutdownRepository() throws Exception {
        if ( session != null ) {
            session.logout();
            session = null;
        }
        RepositoryTestUtil.stopRepository();
    }

    @org.junit.Before public void setup() throws Throwable {
        // remove content from another test
        if ( session.itemExists(REPO_PATH) ) {
            session.getItem(REPO_PATH).remove();
            session.save();
        }
        // activate
        this.activate(null);
    }

    protected int activateCount = 1;

    protected void activate(final EventAdmin ea) throws Throwable {
        this.environment = new EnvironmentComponent();
        PrivateAccessor.setField(this.environment, "repository", RepositoryTestUtil.getSlingRepository());
        PrivateAccessor.setField(this.environment, "classLoaderManager", new DynamicClassLoaderManager() {

            public ClassLoader getDynamicClassLoader() {
                return this.getClass().getClassLoader();
            }
        });

        // the event admin
        if ( ea != null ) {
            PrivateAccessor.setField(this.environment, "eventAdmin", ea);
        } else {
            final EventAdmin eventAdmin = this.getMockery().mock(EventAdmin.class, "eventAdmin" + activateCount);
            PrivateAccessor.setField(this.environment, "eventAdmin", eventAdmin);
            this.getMockery().checking(new Expectations() {{
                allowing(eventAdmin).postEvent(with(any(Event.class)));
                allowing(eventAdmin).sendEvent(with(any(Event.class)));
            }});
        }
        // sling settings service
        PrivateAccessor.setField(this.environment, "settingsService", new SlingSettingsService() {
            public String getSlingId() {
                return SLING_ID;
            }

            public URL getSlingHome() {
                return null;
            }

            public String getSlingHomePath() {
                return null;
            }

            public Set<String> getRunModes() {
                return Collections.<String> emptySet();
            }

            public String getAbsolutePathWithinSlingHome(String relativePath) {
                return null;
            }
        });

        // we need a thread pool
        PrivateAccessor.setField(this.environment, "threadPool", new ThreadPoolImpl());
        this.environment.activate();
    }

    protected void deactivate() throws Throwable {
        this.environment.deactivate();
        this.environment = null;
        activateCount++;
    }

    protected void setEventAdmin(final EventAdmin ea) throws Exception {
        PrivateAccessor.setField(this.environment, "eventAdmin", ea);
    }

    @org.junit.After public void shutdown() throws Throwable {
        this.deactivate();
        try {
            // delete all child nodes to get a clean repository again
            final Node rootNode = (Node) session.getItem(REPO_PATH);
            final NodeIterator iter = rootNode.getNodes();
            while ( iter.hasNext() ) {
                final Node child = iter.nextNode();
                child.remove();
            }
            session.save();
        } catch ( RepositoryException re) {
            // we ignore this for the test
        }
    }

    @org.junit.Test public void testPathCreation() throws RepositoryException {
        assertTrue(session.itemExists(REPO_PATH));
    }

    final class ThreadPoolImpl implements ThreadPool {

        public void execute(Runnable runnable) {
            final Thread t = new Thread(runnable);
            t.start();
        }

        public String getName() {
            return "default";
        }

        public ThreadPoolConfig getConfiguration() {
            return new ModifiableThreadPoolConfig();
        }

    }

    public static void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException ie) {
            // ignore
        }
    }
}
