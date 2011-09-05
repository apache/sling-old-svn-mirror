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
package org.apache.sling.event.impl.jobs.jcr;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.impl.EnvironmentComponent;
import org.apache.sling.event.impl.support.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype=true,label="%lm.name",description="%lm.description")
@Service(value={Runnable.class,LockManager.class})
@Properties({
    @Property(name="scheduler.period", longValue=60, propertyPrivate=true),
    @Property(name="scheduler.concurrent", boolValue=false, propertyPrivate=true)
})
/**
 * The lock manager handles locking and unlocking nodes.
 * It can be configured to handle locks in different ways:
 */
public class LockManager implements Runnable, EventListener {

    /** Default repository path. */
    private static final String DEFAULT_REPOSITORY_PATH = "/var/eventing/cluster";

    /** Modes */
    private static final String MODE_SESSION = "session";
    private static final String MODE_OPEN = "open";
    private static final String MODE_NONE = "none";

    /** Default lock mode. */
    private static final String DEFAULT_MODE = MODE_SESSION;

    /** Property to be updated by the heartbeat. */
    private static final String LAST_MODIFIED_PROP = "lastModified";

    /** Nodetype for heartbeat nodes. */
    private static final String NODE_TYPE = "nt:unstructured";

    /** Lock info prefix. */
    private static final String OWNER_PREFIX = "SlingVersioningManager:";

    /** The path where all beats are stored. */
    @Property(value=DEFAULT_REPOSITORY_PATH, propertyPrivate=true)
    private static final String CONFIG_PROPERTY_REPOSITORY_PATH = "repository.path";

    /** Lock mode. */
    @Property(value=DEFAULT_MODE,
            options={@PropertyOption(name=MODE_SESSION,value="Session Scoped"),
                     @PropertyOption(name=MODE_OPEN,value="Open Scoped"),
                     @PropertyOption(name=MODE_NONE,value="None")})
    private static final String CONFIG_PROPERTY_MODE = "lm.mode";

    private static enum LockMode {
        session,
        open,
        none
    };

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Last modified map. */
    private final Map<String, Long> lastModifiedMap = new HashMap<String, Long>();

    /** Flag, indicating that this service is running. */
    private boolean running;

    /** Lock for the background session. */
    private final Object backgroundLock = new Object();

    /** Background session. */
    private Session backgroundSession;

    @Reference
    private EnvironmentComponent environment;

    /** The repository path. */
    private String repositoryPath;

    /** The id node path. */
    private String idNodePath;

    /** Lock mode .*/
    private LockMode mode;

    /**
     * Activate this component.
     * @param props The configuration properties.
     */
    @Activate
    protected void activate(final Map<String, Object> props) throws RepositoryException {
        this.repositoryPath = PropertiesUtil.toString(props.get(CONFIG_PROPERTY_REPOSITORY_PATH), DEFAULT_REPOSITORY_PATH);
        this.idNodePath = repositoryPath + '/' + Environment.APPLICATION_ID;

        // create the background session and register a listener
        this.backgroundSession = this.environment.createAdminSession();
        this.updateLastModified();
        this.backgroundSession.getWorkspace().getObservationManager().addEventListener(this,
                javax.jcr.observation.Event.PROPERTY_CHANGED
                |javax.jcr.observation.Event.NODE_ADDED,
                this.repositoryPath,
                true,
                null,
                null,
                true);
        logger.info("Apache Sling Versioning Manager started on instance {}", Environment.APPLICATION_ID);
        synchronized ( this.backgroundSession ) {
            this.unlock(Environment.APPLICATION_ID);
        }
        this.scanExistingNodes();

        this.update(props);
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        this.running = false;
        if ( this.backgroundSession != null ) {
            synchronized ( this.backgroundLock ) {
                this.logger.debug("Shutting down background session.");
                try {
                    this.backgroundSession.getWorkspace().getObservationManager().removeEventListener(this);
                } catch (RepositoryException e) {
                    // we just ignore it
                    this.logger.warn("Unable to remove event listener.", e);
                }
                this.backgroundSession.logout();
                this.backgroundSession = null;
            }
        }
        logger.info("Apache Sling Versioning Manager stopped on instance {}", Environment.APPLICATION_ID);
    }

    @Modified
    protected void update(final Map<String, Object> props) {
        final LockMode oldMode = this.mode;
        final String modeString = PropertiesUtil.toString(props.get(CONFIG_PROPERTY_MODE), DEFAULT_MODE);
        this.mode = LockMode.valueOf(modeString);
        if ( oldMode != this.mode ) {
            this.running = this.mode == LockMode.open;
        }
    }

    /**
     * Creates or gets the {@link javax.jcr.Node Node} at the given Path.
     * In case it has to create the Node all non-existent intermediate path-elements
     * will be create with the given intermediate node type and the returned node
     * will be created with the given nodeType
     *
     * @param relativePath to create
     * @return the Node at path
     * @throws RepositoryException in case of exception accessing the Repository
     */
    private Node createPath(String relativePath)
    throws RepositoryException {
        final Node parentNode = this.backgroundSession.getRootNode();
        if (!parentNode.hasNode(relativePath)) {
            Node node = parentNode;
            int pos = relativePath.lastIndexOf('/');
            if ( pos != -1 ) {
                final StringTokenizer st = new StringTokenizer(relativePath.substring(0, pos), "/");
                while ( st.hasMoreTokens() ) {
                    final String token = st.nextToken();
                    if ( !node.hasNode(token) ) {
                        try {
                            node.addNode(token, NODE_TYPE);
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
                node.addNode(relativePath, NODE_TYPE);
            }
            return node.getNode(relativePath);
        }
        return parentNode.getNode(relativePath);
    }

    /**
     * Update the last modified of this node
     */
    private void updateLastModified() {
        synchronized ( this.backgroundLock ) {
            try {
                final Node slingNode = this.createPath(this.idNodePath.substring(1));
                slingNode.setProperty(LAST_MODIFIED_PROP, System.currentTimeMillis());
                this.backgroundSession.save();
                logger.debug("Heartbeat at {}", Environment.APPLICATION_ID);
            } catch (final RepositoryException re) {
                this.ignoreException(re);
                try {
                    this.backgroundSession.refresh(false);
                } catch (final RepositoryException ignore) {
                    this.ignoreException(ignore);
                }
            }
        }
    }

    /** Scan for existing ids */
    private void scanExistingNodes() {
        synchronized ( this.backgroundLock ) {
            try {
                final Node rootNode = this.backgroundSession.getNode(this.repositoryPath);
                final NodeIterator nI = rootNode.getNodes();
                while ( nI.hasNext() ) {
                    final Node node = nI.nextNode();
                    final String id = node.getName();
                    if ( !Environment.APPLICATION_ID.equals(id) && node.hasProperty(LAST_MODIFIED_PROP) ) {
                        final javax.jcr.Property prop = node.getProperty(LAST_MODIFIED_PROP);
                        logger.debug("Updated heartbeat from {}", id);
                        this.lastModifiedMap.put(id, prop.getLong());
                    }
                }
            } catch (final RepositoryException re) {
                this.ignoreException(re);
            }
        }
    }

    /**
     * Helper method which just logs the exception in debug mode.
     * @param e Exception to ignore
     */
    private void ignoreException(final Exception e) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Ignored exception " + e.getMessage(), e);
        }
    }

    /**
     * Cron job
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if ( this.running ) {
            // we update last modified
            this.updateLastModified();
            final long teeMinusTwo = System.currentTimeMillis() - 120000;
            synchronized ( this.backgroundLock ) {
                for(final Map.Entry<String, Long> entry : this.lastModifiedMap.entrySet() ) {
                    if ( entry.getValue() != -1 ) {
                        logger.debug("Checking cluster node {}", entry.getKey());
                        if ( entry.getValue() <= teeMinusTwo ) {
                            this.unlock(entry.getKey());
                            entry.setValue(-1L);
                        }
                    }
                }
            }
        }

    }

    /**
     * Search all locked nodes with this id
     * @param id The sling id
     */
    private void unlock(final String id) {
        logger.info("Trying to unlock {}", id);
        try {
            final String searchString = OWNER_PREFIX + id;

            final QueryManager qm = this.backgroundSession.getWorkspace().getQueryManager();
            final Query q = qm.createQuery("select * from nt:base where " + JCRHelper.NODE_PROPERTY_LOCK_OWNER + " = '" + searchString + "'",
                    Query.SQL);
            final QueryResult qr = q.execute();
            final NodeIterator nI = qr.getNodes();
            while ( nI.hasNext() ) {
                final Node node = nI.nextNode();
                try {
                    if ( node.hasProperty(JCRHelper.NODE_PROPERTY_LOCK_OWNER) ) {
                        if ( node.isLocked()
                             && node.getProperty(JCRHelper.NODE_PROPERTY_LOCK_OWNER).getString().endsWith(searchString) ) {
                            logger.debug("Trying to unlock node {} from {}", node.getPath(), id);
                            this.backgroundSession.getWorkspace().getLockManager().unlock(node.getPath());
                        }
                    }
                } catch (final RepositoryException re) {
                    this.ignoreException(re);
                }
            }
        } catch (final RepositoryException re) {
            this.ignoreException(re);
        }
    }

    /**
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     */
    public void onEvent(final EventIterator events) {
        synchronized ( this.backgroundLock ) {
            while ( events.hasNext() ) {
                final Event event = events.nextEvent();
                if ( this.running ) {
                    try {
                        final String path = event.getType() == javax.jcr.observation.Event.NODE_ADDED
                                       ? event.getPath() + '/' + LAST_MODIFIED_PROP : event.getPath();
                        if ( this.backgroundSession.propertyExists(path) ) {
                            final javax.jcr.Property prop = this.backgroundSession.getProperty(path);
                            final String id = prop.getParent().getName();
                            logger.debug("Updated heartbeat from {}", id);
                            this.lastModifiedMap.put(id, prop.getLong());
                        }
                    } catch (final RepositoryException re) {
                        this.ignoreException(re);
                    }
                }
            }
        }
    }

    /**
     * Lock the node at the given path
     * @param session The session to create the lock with
     * @param path The path to the node to lock
     * @throws RepositoryException If anything goes wrong
     */
    public void lock(final Session session, final String path) throws RepositoryException {
        if ( this.mode != LockMode.none ) {
            session.getWorkspace().getLockManager().lock(path, false,
                    this.mode == LockMode.session, Long.MAX_VALUE,
                    OWNER_PREFIX + Environment.APPLICATION_ID);
        }
    }

    /**
     * Unlock the node at the given path.
     * @param session The session for unlocking
     * @param path The path to the node to unlock
     * @throws RepositoryException If anything goes wrong
     */
    public void unlock(final Session session, final String path)
    throws RepositoryException {
        if ( this.mode != LockMode.none ) {
            session.getWorkspace().getLockManager().unlock(path);
        }
    }
}
