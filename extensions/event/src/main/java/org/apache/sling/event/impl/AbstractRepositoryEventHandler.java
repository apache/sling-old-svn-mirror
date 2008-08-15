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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.engine.SlingSettingsService;
import org.apache.sling.event.EventUtil;
import org.apache.sling.jcr.api.SlingRepository;
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

    /**
     * Activate this component.
     * @param context
     * @throws RepositoryException
     */
    protected void activate(final ComponentContext context)
    throws Exception {
        this.applicationId = this.settingsService.getSlingId();
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
            this.createRepositoryPath();
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
     * Create the repository path in the repository.
     */
    protected void createRepositoryPath()
    throws RepositoryException {
        if ( !this.writerSession.itemExists(this.repositoryPath) ) {
            Node node = this.writerSession.getRootNode();
            String path = this.repositoryPath.substring(1);
            int pos = path.lastIndexOf('/');
            if ( pos != -1 ) {
                final StringTokenizer st = new StringTokenizer(path.substring(0, pos), "/");
                while ( st.hasMoreTokens() ) {
                    final String token = st.nextToken();
                    if ( !node.hasNode(token) ) {
                        try {
                            node.addNode(token, "nt:folder");
                            node.save();
                        } catch (RepositoryException re) {
                            // we ignore this as this folder might be created from a different task
                            node.refresh(false);
                        }
                    }
                    node = node.getNode(token);
                }
                path = path.substring(pos + 1);
            }
            if ( !node.hasNode(path) ) {
                node.addNode(path, this.getContainerNodeType());
                node.save();
            }
        }
    }

    protected String getContainerNodeType() {
        return EventHelper.EVENTS_NODE_TYPE;
    }

    protected String getEventNodeType() {
        return EventHelper.EVENT_NODE_TYPE;
    }

    /**
     * Write an event to the repository.
     * @param e
     * @throws RepositoryException
     * @throws IOException
     */
    protected Node writeEvent(Event e, String suggestedName)
    throws RepositoryException {
        // create new node with name of topic
        final Node rootNode = (Node) this.writerSession.getItem(this.repositoryPath);

        final String nodeType = this.getEventNodeType();
        final String nodeName;
        if ( suggestedName != null ) {
            nodeName = suggestedName;
        } else {
            final Calendar now = Calendar.getInstance();
            final int sepPos = nodeType.indexOf(':');
            nodeName = nodeType.substring(sepPos+1) + "-" + this.applicationId + "-" + now.getTime().getTime();
        }
        final Node eventNode = rootNode.addNode(nodeName, nodeType);

        eventNode.setProperty(EventHelper.NODE_PROPERTY_CREATED, Calendar.getInstance());
        eventNode.setProperty(EventHelper.NODE_PROPERTY_TOPIC, e.getTopic());
        eventNode.setProperty(EventHelper.NODE_PROPERTY_APPLICATION, this.applicationId);

        final String[] names = e.getPropertyNames();

        // if the application property is available, we will override it
        // if it is not available we will add it
        eventNode.setProperty(EventUtil.PROPERTY_APPLICATION, this.applicationId);

        if ( names != null ) {
            // check which props we can write directly and
            // which we need to write as a binary blob
            final List<String> propsAsBlob = new ArrayList<String>();

            for(final String propName : names) {
                // ignore application, distribute and context property
                if ( !propName.equals(EventUtil.PROPERTY_DISTRIBUTE)
                    && !propName.equals(EventUtil.PROPERTY_APPLICATION)
                    && !propName.equals(EventUtil.JobStatusNotifier.CONTEXT_PROPERTY_NAME) ) {
                    final Object value = e.getProperty(propName);
                    // sanity check
                    if ( value != null ) {
                        if ( !this.setProperty(propName, value, eventNode) ) {
                            propsAsBlob.add(propName);
                        }
                    }
                }
            }
            // write the remaining properties as a blob
            if ( propsAsBlob.size() > 0 ) {
                try {
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeInt(propsAsBlob.size());
                    for(final String propName : propsAsBlob) {
                        oos.writeObject(propName);
                        oos.writeObject(e.getProperty(propName));
                    }
                    oos.close();
                    eventNode.setProperty(EventHelper.NODE_PROPERTY_PROPERTIES, new ByteArrayInputStream(baos.toByteArray()));
                } catch (IOException ioe) {
                    throw new RepositoryException("Unable to serialize event properties.", ioe);
                }
            }
        }
        this.addNodeProperties(eventNode, e);
        rootNode.save();

        return eventNode;
    }

    /**
     * Return the converted repository property name
     * @param name The OSGi event property name
     * @return The converted name or null if not possible.
     */
    protected String getNodePropertyName(final String name) {
        // if name contains a colon, we can't set it as a property
        if ( name.indexOf(':') != -1 ) {
            return null;
        }
        return ISO9075.encode(name);
    }

    /**
     * Return the converted repository property value
     * @param valueFactory The value factory
     * @param eventValue The event value
     * @return The converted value or null if not possible
     */
    protected Value getNodePropertyValue(final ValueFactory valueFactory, final Object eventValue) {
        final Value val;
        if (eventValue.getClass().isAssignableFrom(Calendar.class)) {
            val = valueFactory.createValue((Calendar)eventValue);
        } else if (eventValue.getClass().isAssignableFrom(Long.class)) {
            val = valueFactory.createValue((Long)eventValue);
        } else if (eventValue.getClass().isAssignableFrom(Double.class)) {
            val = valueFactory.createValue(((Double)eventValue).doubleValue());
        } else if (eventValue.getClass().isAssignableFrom(Boolean.class)) {
            val = valueFactory.createValue((Boolean) eventValue);
        } else if (eventValue instanceof String) {
            val = valueFactory.createValue((String)eventValue);
        } else {
            val = null;
        }
        return val;
    }

    /**
     * Try to set the OSGi event property as a property of the node.
     * @param name
     * @param value
     * @param node
     * @return
     * @throws RepositoryException
     */
    private boolean setProperty(String name, Object value, Node node)
    throws RepositoryException {
        final String propName = this.getNodePropertyName(name);
        if ( propName == null ) {
            return false;
        }
        final ValueFactory fac = node.getSession().getValueFactory();
        final Value val = this.getNodePropertyValue(fac, value);
        if ( val != null ) {
            node.setProperty(propName, val);
            return true;
        }
        return false;
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
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        // check the properties blob
        if ( eventNode.hasProperty(EventHelper.NODE_PROPERTY_PROPERTIES) ) {
            try {
                final ObjectInputStream ois = new ObjectInputStream(eventNode.getProperty(EventHelper.NODE_PROPERTY_PROPERTIES).getStream());
                int length = ois.readInt();
                for(int i=0;i<length;i++) {
                    final String key = (String)ois.readObject();
                    final Object value = ois.readObject();
                    properties.put(key, value);
                }
            } catch (IOException ioe) {
                throw new RepositoryException("Unable to deserialize event properties.", ioe);
            }
        }
        // now all properties that have been set directly
        final PropertyIterator pI = eventNode.getProperties();
        while ( pI.hasNext() ) {
            final Property p = pI.nextProperty();
            if ( !p.getName().startsWith("jcr:") && !p.getName().startsWith(EventHelper.EVENT_PREFIX) ) {
                final String name = ISO9075.decode(p.getName());
                final Value value = p.getValue();
                final Object o;
                switch (value.getType()) {
                    case PropertyType.BOOLEAN:
                        o = value.getBoolean(); break;
                    case PropertyType.DATE:
                        o = value.getDate(); break;
                    case PropertyType.DOUBLE:
                        o = value.getDouble(); break;
                    case PropertyType.LONG:
                        o = value.getLong(); break;
                    case PropertyType.STRING:
                        o = value.getString(); break;
                    default: // this should never happen - we convert to a string...
                        o = value.getString();
                }
                properties.put(name, o);
            }
        }
        this.addEventProperties(eventNode, properties);
        try {
            final Event event = new Event(topic, properties);
            return event;
        } catch (IllegalArgumentException iae) {
            // this exception occurs if the topic is not correct (it should never happen,
            // but you never know)
            throw new RepositoryException("Unable to read event: " + iae.getMessage(), iae);
        }
    }

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
