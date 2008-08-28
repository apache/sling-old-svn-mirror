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

import java.io.IOException;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventListener;

import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.engine.SlingSettingsService;
import org.apache.sling.event.EventPropertiesMap;
import org.apache.sling.event.EventUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all event handlers in this package.
 *
 * @scr.component abstract="true" metatype="no"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 */
public abstract class AbstractRepositoryEventHandler
    implements EventHandler, EventListener {

    /** Default log. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** @scr.property valueRef="DEFAULT_PROPERTY_REPO_PATH" */
    protected static final String CONFIG_PROPERTY_REPO_PATH = "repository.path";

    /** Default path for the {@link #CONFIG_PROPERTY_REPO_PATH} */
    private static final String DEFAULT_PROPERTY_REPO_PATH = "/sling/events";

    /** @scr.reference */
    protected SlingRepository repository;

    /** @scr.reference */
    protected EventAdmin eventAdmin;

    /** Our application id. */
    protected String applicationId;

    /** The repository session to write into the repository. */
    protected Session writerSession;

    /** The path in the repository. */
    protected String repositoryPath;

    /** Is the background task still running? */
    protected boolean running;

    /** A local queue for serialising the event processing. */
    protected final BlockingQueue<EventInfo> queue = new LinkedBlockingQueue<EventInfo>();

    /** A local queue for writing received events into the repository. */
    protected final BlockingQueue<Event> writeQueue = new LinkedBlockingQueue<Event>();

    /** @scr.reference */
    protected ThreadPoolManager threadPoolManager;

    /** Our thread pool. */
    protected ThreadPool threadPool;

    /** @scr.reference
     *  Sling settings service. */
    protected SlingSettingsService settingsService;

    public static String APPLICATION_ID;

    /** List of ignored properties to write to the repository. */
    private static final String[] IGNORE_PROPERTIES = new String[] {
        EventUtil.PROPERTY_DISTRIBUTE,
        EventUtil.PROPERTY_APPLICATION,
        EventHelper.NODE_PROPERTY_NODE_PATH,
        EventUtil.JobStatusNotifier.CONTEXT_PROPERTY_NAME
    };

    /** List of ignored prefixes to read from the repository. */
    private static final String[] IGNORE_PREFIXES = new String[] {
        EventHelper.EVENT_PREFIX
    };

    /**
     * Activate this component.
     * @param context
     * @throws RepositoryException
     */
    protected void activate(final ComponentContext context)
    throws Exception {
        this.applicationId = this.settingsService.getSlingId();
        APPLICATION_ID = this.applicationId;
        this.repositoryPath = OsgiUtil.toString(context.getProperties().get(
            CONFIG_PROPERTY_REPO_PATH), DEFAULT_PROPERTY_REPO_PATH);

        // start background threads
        if ( this.threadPoolManager == null ) {
            throw new Exception("No ThreadPoolManager found.");
        }
        final ThreadPoolConfig config = new ThreadPoolConfig();
        config.setMinPoolSize(10);
        config.setMaxPoolSize(30);
        config.setQueueSize(-1);
        config.setShutdownGraceful(true);
        threadPoolManager.create(EventHelper.THREAD_POOL_NAME, config);

        this.threadPool = threadPoolManager.get(EventHelper.THREAD_POOL_NAME);
        if ( this.threadPool == null ) {
            throw new Exception("No thread pool found.");
        }
        this.running = true;
        // start writer thread
        this.threadPool.execute(new Runnable() {
            public void run() {
                try {
                    startWriterSession();
                } catch (RepositoryException e) {
                    // there is nothing we can do except log!
                    logger.error("Error during session starting.", e);
                    running = false;
                }
                try {
                    processWriteQueue();
                } catch (Throwable t) {
                    logger.error("Writer thread stopped with exception: " + t.getMessage(), t);
                    running = false;
                }
                stopWriterSession();
            }
        });
        this.threadPool.execute(new Runnable() {
            public void run() {
                try {
                    runInBackground();
                } catch (Throwable t) {
                    logger.error("Background thread stopped with exception: " + t.getMessage(), t);
                    running = false;
                }
            }
        });
    }

    protected abstract void runInBackground() throws RepositoryException;

    protected abstract void processWriteQueue();

    /**
     * Deactivate this component.
     * @param context
     */
    protected void deactivate(final ComponentContext context) {
        // stop background threads by putting empty objects into the queue
        this.running = false;
        try {
            this.writeQueue.put(new Event("some", null));
        } catch (InterruptedException e) {
            this.ignoreException(e);
        }
        try {
            this.queue.put(new EventInfo());
        } catch (InterruptedException e) {
            this.ignoreException(e);
        }
        this.threadPool = null;
    }

    /**
     * Create a new session.
     * @return
     * @throws RepositoryException
     */
    protected Session createSession()
    throws RepositoryException {
        final SlingRepository repo = this.repository;
        if ( repo == null ) {
            throw new RepositoryException("Repository is currently not available.");
        }
        return repo.loginAdministrative(null);
    }

    /**
     * Start the repository session and add this handler as an observer
     * for new events created on other nodes.
     * @throws RepositoryException
     */
    protected void startWriterSession() throws RepositoryException {
        this.writerSession = this.createSession();
        if ( this.repositoryPath != null ) {
            this.ensureRepositoryPath();
        }
    }

    /**
     * Stop the session.
     */
    protected void stopWriterSession() {
        if ( this.writerSession != null ) {
            try {
                this.writerSession.getWorkspace().getObservationManager().removeEventListener(this);
            } catch (RepositoryException e) {
                // we just ignore it
                this.logger.warn("Unable to remove event listener.", e);
            }
            this.writerSession.logout();
            this.writerSession = null;
        }
    }

    /**
     * Check if the repository path already exists. If not, create it.
     */
    protected Node ensureRepositoryPath()
    throws RepositoryException {
        final Node node = JcrResourceUtil.createPath(this.repositoryPath,
                                   EventHelper.NODETYPE_FOLDER,
                                   EventHelper.NODETYPE_FOLDER,
                                   this.writerSession, true);

        return node;
    }

    /**
     * Return the node type for the event.
     */
    protected String getEventNodeType() {
        return EventHelper.EVENT_NODE_TYPE;
    }

    /**
     * Write an event to the repository.
     * @param e The event
     * @param suggestName A suggest name/path for the node.
     * @throws RepositoryException
     * @throws IOException
     */
    protected Node writeEvent(Event e, String suggestedName)
    throws RepositoryException {
        // create new node with name of topic
        final Node rootNode = this.ensureRepositoryPath();

        final String nodeType = this.getEventNodeType();
        final String nodeName;
        if ( suggestedName != null ) {
            nodeName = suggestedName;
        } else {
            final Calendar now = Calendar.getInstance();
            final int sepPos = nodeType.indexOf(':');
            nodeName = nodeType.substring(sepPos+1) + "-" + this.applicationId + "-" + now.getTime().getTime();
        }
        final Node eventNode = JcrResourceUtil.createPath(rootNode,
                nodeName,
                EventHelper.NODETYPE_FOLDER,
                nodeType, false);

        eventNode.setProperty(EventHelper.NODE_PROPERTY_CREATED, Calendar.getInstance());
        eventNode.setProperty(EventHelper.NODE_PROPERTY_TOPIC, e.getTopic());
        eventNode.setProperty(EventHelper.NODE_PROPERTY_APPLICATION, this.applicationId);

        EventUtil.addProperties(eventNode,
                                new EventPropertiesMap(e),
                                IGNORE_PROPERTIES,
                                EventHelper.NODE_PROPERTY_PROPERTIES);
        this.addNodeProperties(eventNode, e);
        rootNode.save();

        return eventNode;
    }

    /**
     * Read an event from the repository.
     * @return
     * @throws RepositoryException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected Event readEvent(Node eventNode)
    throws RepositoryException, ClassNotFoundException {
        final String topic = eventNode.getProperty(EventHelper.NODE_PROPERTY_TOPIC).getString();
        final EventPropertiesMap eventProps = EventUtil.readProperties(eventNode,
                EventHelper.NODE_PROPERTY_PROPERTIES,
                IGNORE_PREFIXES);

        eventProps.put(EventHelper.NODE_PROPERTY_NODE_PATH, eventNode.getPath());
        this.addEventProperties(eventNode, eventProps);
        try {
            final Event event = new Event(topic, eventProps);
            return event;
        } catch (IllegalArgumentException iae) {
            // this exception occurs if the topic is not correct (it should never happen,
            // but you never know)
            throw new RepositoryException("Unable to read event: " + iae.getMessage(), iae);
        }
    }

    /**
     * Add properties from the node to the event properties.
     * @param eventNode The repository node.
     * @param properties The event properties.
     * @throws RepositoryException
     */
    protected void addEventProperties(Node eventNode, Dictionary<String, Object> properties)
    throws RepositoryException {
        // nothing to do
    }

    /**
     * Add properties when storing event in repository.
     * This method can be enhanced by sub classes.
     * @param eventNode
     * @param event
     * @throws RepositoryException
     */
    protected void addNodeProperties(Node eventNode, Event event)
    throws RepositoryException {
        // nothing to do here
    }

    /**
     * Helper method which just logs the exception in debug mode.
     * @param e
     */
    protected void ignoreException(Exception e) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Ignore exception " + e.getMessage(), e);
        }
    }

    protected static final class EventInfo {
        public String nodePath;
        public Event event;
    }

}
