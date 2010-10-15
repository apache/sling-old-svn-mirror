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

import java.util.Calendar;
import java.util.Dictionary;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventListener;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.event.impl.jobs.jcr.JCRHelper;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all event handlers in this package.
 *
 */
@Component(componentAbstract=true)
@Service(value=EventHandler.class)
public abstract class AbstractRepositoryEventHandler
    implements EventHandler, EventListener {

    /** Default log. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Default path for the {@link #CONFIG_PROPERTY_REPO_PATH} */
    private static final String DEFAULT_PROPERTY_REPO_PATH = "/sling/events";

    @Property(value=DEFAULT_PROPERTY_REPO_PATH)
    protected static final String CONFIG_PROPERTY_REPO_PATH = "repository.path";

    /** The repository session to write into the repository. */
    protected Session writerSession;

    /** Sync lock */
    protected final Object writeLock = new Object();

    /** The path in the repository. */
    protected String repositoryPath;

    /** Is the background task still running? */
    protected volatile boolean running;

    /** A local queue for serialising the event processing. */
    protected final BlockingQueue<EventInfo> queue = new LinkedBlockingQueue<EventInfo>();

    /** A local queue for writing received events into the repository. */
    protected final BlockingQueue<Event> writeQueue = new LinkedBlockingQueue<Event>();

    /**
     * Environment component.
     */
    @Reference
    protected EnvironmentComponent environment;

    /** The root node for writing. */
    private Node writeRootNode;

    /**
     * Activate this component.
     * @param context
     */
    protected void activate(final ComponentContext context) {
        this.repositoryPath = OsgiUtil.toString(context.getProperties().get(
            CONFIG_PROPERTY_REPO_PATH), DEFAULT_PROPERTY_REPO_PATH);

        this.running = true;
        // start writer thread
        final Thread writerThread = new Thread(new Runnable() {
            public void run() {
                try {
                    synchronized ( writeLock ) {
                        startWriterSession();
                    }
                } catch (RepositoryException e) {
                    // there is nothing we can do except log!
                    logger.error("Error during session starting.", e);
                    running = false;
                }
                try {
                    processWriteQueue();
                } catch (Throwable t) { //NOSONAR
                    logger.error("Writer thread stopped with exception: " + t.getMessage(), t);
                    running = false;
                }
                synchronized ( writeLock ) {
                    stopWriterSession();
                }
            }
        });
        writerThread.start();
        final Thread backgroundThread = new Thread(new Runnable() {
            public void run() {
                try {
                    runInBackground();
                } catch (Throwable t) { //NOSONAR
                    logger.error("Background thread stopped with exception: " + t.getMessage(), t);
                    running = false;
                }
            }
        });
        backgroundThread.start();
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
            this.writeQueue.put(new Event("some", (Dictionary<String, Object>)null));
        } catch (InterruptedException e) {
            this.ignoreException(e);
        }
        try {
            this.queue.put(new EventInfo());
        } catch (InterruptedException e) {
            this.ignoreException(e);
        }
        this.writeRootNode = null;
    }

    /**
     * Start the repository session and add this handler as an observer
     * for new events created on other nodes.
     * @throws RepositoryException
     */
    protected void startWriterSession() throws RepositoryException {
        this.writerSession = this.environment.createAdminSession();
        this.writeRootNode = this.createPath(this.writerSession.getRootNode(),
                this.repositoryPath.substring(1),
                JCRHelper.NODETYPE_ORDERED_FOLDER);
        this.writerSession.save();
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
     * Return the node type for the event.
     */
    protected String getEventNodeType() {
        return JCRHelper.EVENT_NODE_TYPE;
    }

    /**
     * Get the root node from the writer session.
     */
    protected Node getWriterRootNode() {
        return this.writeRootNode;
    }

    /**
     * Write an event to the repository.
     * @param e The event
     * @param suggestName A suggest name/path for the node.
     * @throws RepositoryException
     */
    protected Node writeEvent(Event e, String suggestedName)
    throws RepositoryException {
        // create new node with name of topic
        final Node rootNode = this.getWriterRootNode();

        final String nodeType = this.getEventNodeType();
        final String nodeName;
        if ( suggestedName != null ) {
            nodeName = suggestedName;
        } else {
            final Calendar now = Calendar.getInstance();
            final int sepPos = nodeType.indexOf(':');
            nodeName = nodeType.substring(sepPos+1) + "-" + Environment.APPLICATION_ID + "-" + now.getTime().getTime();
        }
        final Node eventNode = this.createPath(rootNode,
                nodeName,
                nodeType);

        eventNode.setProperty(JCRHelper.NODE_PROPERTY_CREATED, Calendar.getInstance());
        eventNode.setProperty(JCRHelper.NODE_PROPERTY_TOPIC, e.getTopic());
        eventNode.setProperty(JCRHelper.NODE_PROPERTY_APPLICATION, Environment.APPLICATION_ID);

        JCRHelper.writeEventProperties(eventNode, e);
        this.addNodeProperties(eventNode, e);
        writerSession.save();

        return eventNode;
    }

    /**
     * Read an event from the repository.
     * @return
     * @throws RepositoryException
     * @throws ClassNotFoundException
     */
    protected Event readEvent(Node eventNode)
    throws RepositoryException, ClassNotFoundException {
        final String topic = eventNode.getProperty(JCRHelper.NODE_PROPERTY_TOPIC).getString();
        final ClassLoader cl = this.environment.getDynamicClassLoader();
        final Dictionary<String, Object> eventProps = JCRHelper.readEventProperties(eventNode, cl, false);

        eventProps.put(JobUtil.JOB_ID, eventNode.getPath());
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
            this.logger.debug("Ignored exception " + e.getMessage(), e);
        }
    }

    /**
     * Creates or gets the {@link javax.jcr.Node Node} at the given Path.
     * In case it has to create the Node all non-existent intermediate path-elements
     * will be create with the given intermediate node type and the returned node
     * will be created with the given nodeType
     *
     * @param parentNode starting node
     * @param relativePath to create
     * @param intermediateNodeType to use for creation of intermediate nodes (or null)
     * @param nodeType to use for creation of the final node (or null)
     * @param autoSave Should save be called when a new node is created?
     * @return the Node at path
     * @throws RepositoryException in case of exception accessing the Repository
     */
    private Node createPath(Node   parentNode,
                            String relativePath,
                            String nodeType)
    throws RepositoryException {
        if (!parentNode.hasNode(relativePath)) {
            Node node = parentNode;
            int pos = relativePath.lastIndexOf('/');
            if ( pos != -1 ) {
                final StringTokenizer st = new StringTokenizer(relativePath.substring(0, pos), "/");
                while ( st.hasMoreTokens() ) {
                    final String token = st.nextToken();
                    if ( !node.hasNode(token) ) {
                        try {
                            node.addNode(token, JCRHelper.NODETYPE_FOLDER);
                            node.getSession().save();
                        } catch (RepositoryException re) {
                            // we ignore this as this folder might be created from a different task
                            node.refresh(false);
                        }
                    }
                    node = node.getNode(token);
                }
                relativePath = relativePath.substring(pos + 1);
            }
            if ( !node.hasNode(relativePath) ) {
                node.addNode(relativePath, nodeType);
            }
            return node.getNode(relativePath);
        }
        return parentNode.getNode(relativePath);
    }

    public static final class EventInfo {
        public String nodePath;
        public Event event;
    }

}
