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

import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.EnvironmentComponent;
import org.apache.sling.event.impl.jobs.DefaultJobManager;
import org.apache.sling.event.impl.jobs.JobEvent;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistence handler for the jobs
 *
 */
@Component(label="%job.persistence.name",
        description="%job.persistence.description",
        metatype=true,immediate=true)
@Services({
    @Service(value=PersistenceHandler.class),
    @Service(value=EventHandler.class),
    @Service(value=Runnable.class)
})
@Properties({
    @Property(name="event.topics",propertyPrivate=true,
            value={"org/osgi/framework/BundleEvent/UPDATED",
                   "org/osgi/framework/BundleEvent/STARTED",
                   JobUtil.TOPIC_JOB}),
     @Property(name="scheduler.period", longValue=300,
               label="%persscheduler.period.name",
               description="%persscheduler.period.description"),
     @Property(name="scheduler.concurrent", boolValue=false, propertyPrivate=true)
})
public class PersistenceHandler implements EventListener, Runnable, EventHandler {

    /** Default repository path. */
    private static final String DEFAULT_REPOSITORY_PATH = "/var/eventing/jobs";

    /** The path where all jobs are stored. */
    @Property(value=DEFAULT_REPOSITORY_PATH, propertyPrivate=true)
    private static final String CONFIG_PROPERTY_REPOSITORY_PATH = "repository.path";

    /** Default clean up time is 5 minutes. */
    private static final int DEFAULT_CLEANUP_PERIOD = 5;

    @Property(intValue=DEFAULT_CLEANUP_PERIOD,
              label="%jobcleanup.period.name",
              description="%jobcleanup.period.description")
    private static final String CONFIG_PROPERTY_CLEANUP_PERIOD = "cleanup.period";

    /** Default maximum load jobs. */
    private static final long DEFAULT_MAXIMUM_LOAD_JOBS = 1000;

    /** Number of jobs to load from the repository on startup in one go. */
    @Property(longValue=DEFAULT_MAXIMUM_LOAD_JOBS)
    private static final String CONFIG_PROPERTY_MAX_LOAD_JOBS = "max.load.jobs";

    /** Default load threshold. */
    private static final long DEFAULT_LOAD_THRESHOLD = 400;

    /** Threshold - if the queue is lower than this threshold the repository is checked for events. */
    @Property(longValue=DEFAULT_LOAD_THRESHOLD)
    private static final String CONFIG_PROPERTY_LOAD_THREASHOLD = "load.threshold";

    /** Default background load delay. */
    private static final long DEFAULT_BACKGROUND_LOAD_DELAY = 30;

    /** The background loader waits this time of seconds after startup before loading events from the repository. (in secs) */
    @Property(longValue=DEFAULT_BACKGROUND_LOAD_DELAY)
    private static final String CONFIG_PROPERTY_BACKGROUND_LOAD_DELAY = "load.delay";

    /** Default background check delay. */
    private static final long DEFAULT_BACKGROUND_CHECK_DELAY = 240;

    /** The background loader waits this time of seconds between loads from the repository. (in secs) */
    @Property(longValue=DEFAULT_BACKGROUND_CHECK_DELAY)
    private static final String CONFIG_PROPERTY_BACKGROUND_CHECK_DELAY = "load.checkdelay";

    /** We remove everything which is older than 5 min by default. */
    private int cleanupPeriod;

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The repository path. */
    private String repositoryPath;

    /** Is the background task still running? */
    private volatile boolean running;

    /** Unloaded jobs. */
    private Set<String>unloadedJobs = new HashSet<String>();

    /** A local queue for writing received events into the repository. */
    private final BlockingQueue<Event> writeQueue = new LinkedBlockingQueue<Event>();

    /** Lock for the background session. */
    private final Object backgroundLock = new Object();

    /** Background session for all reading, locking etc. */
    private Session backgroundSession;

    @Reference
    private EnvironmentComponent environment;

    @Reference
    private JobManager jobManager;

    @Reference
    private LockManager lockManager;

    /** Counter for cleanups */
    private long cleanUpCounter;

    /**
     * Activate this component.
     * @param context The component context.
     */
    @Activate
    protected void activate(final ComponentContext context) throws RepositoryException {
        @SuppressWarnings("unchecked")
        final Dictionary<String, Object> props = context.getProperties();
        this.cleanupPeriod = OsgiUtil.toInteger(props.get(CONFIG_PROPERTY_CLEANUP_PERIOD), DEFAULT_CLEANUP_PERIOD);
        if ( this.cleanupPeriod < 1 ) {
            this.cleanupPeriod = DEFAULT_CLEANUP_PERIOD;
        }
        this.repositoryPath = OsgiUtil.toString(props.get(CONFIG_PROPERTY_REPOSITORY_PATH), DEFAULT_REPOSITORY_PATH);
        this.running = true;

        // start writer background thread
        final Thread writerThread = new Thread(new Runnable() {
            public void run() {
                persistJobs();
            }
        }, "Apache Sling Job Writer");
        writerThread.setDaemon(true);
        writerThread.start();

        // start background thread which loads jobs from the repository
        final long loadThreshold = OsgiUtil.toLong(props.get(CONFIG_PROPERTY_LOAD_THREASHOLD), DEFAULT_LOAD_THRESHOLD);
        final long backgroundLoadDelay = OsgiUtil.toLong(props.get(CONFIG_PROPERTY_BACKGROUND_LOAD_DELAY), DEFAULT_BACKGROUND_LOAD_DELAY);
        final long backgroundCheckDelay = OsgiUtil.toLong(props.get(CONFIG_PROPERTY_BACKGROUND_CHECK_DELAY), DEFAULT_BACKGROUND_CHECK_DELAY);
        final long maxLoadJobs = OsgiUtil.toLong(props.get(CONFIG_PROPERTY_MAX_LOAD_JOBS), DEFAULT_MAXIMUM_LOAD_JOBS);
        final Thread loaderThread = new Thread(new Runnable() {
            public void run() {
                loadJobsInTheBackground(backgroundLoadDelay, backgroundCheckDelay, loadThreshold, maxLoadJobs);
            }
        }, "Apache Sling Job Background Loader");
        loaderThread.setDaemon(true);
        loaderThread.start();

        // open background session for all job related tasks (lock, unlock etc.)
        // create the background session and register a listener
        this.backgroundSession = this.environment.createAdminSession();
        this.backgroundSession.getWorkspace().getObservationManager().addEventListener(this,
                javax.jcr.observation.Event.PROPERTY_REMOVED
                |javax.jcr.observation.Event.PROPERTY_ADDED
                |javax.jcr.observation.Event.NODE_REMOVED,
                this.repositoryPath,
                true,
                null,
                null,
                true);
    }

    /**
     * Deactivate this component.
     * @param context The component context.
     */
    @Deactivate
    protected void deactivate(final ComponentContext context) {
        this.running = false;
        // stop write queue
        try {
            this.writeQueue.put(new Event("some", (Dictionary<String, Object>)null));
        } catch (InterruptedException e) {
            this.ignoreException(e);
        }
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
        logger.debug("Apache Sling Job Persistence Handler stopped on instance {}", Environment.APPLICATION_ID);
    }

    @Modified
    protected void update(final ComponentContext context) {
        // we don't need to do anything as the config values are only used for initial loading!
    }

    /**
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     */
    public void onEvent(EventIterator iter) {
        // we create an own session here - this is done lazy
        Session s = null;
        try {
            while ( iter.hasNext() ) {
                final javax.jcr.observation.Event event = iter.nextEvent();

                try {
                    final String path = event.getPath();
                    String loadNodePath = null;

                    if ( event.getType() == javax.jcr.observation.Event.NODE_ADDED) {
                        loadNodePath = path;
                    } else if ( event.getType() == javax.jcr.observation.Event.PROPERTY_REMOVED) {
                        final int pos = path.lastIndexOf('/');
                        final String propertyName = path.substring(pos+1);

                        // we are only interested in unlocks
                        if ( JCRHelper.NODE_PROPERTY_LOCK_OWNER.equals(propertyName) ) {
                            loadNodePath = path.substring(0, pos);
                        }
                    } else if ( event.getType() == javax.jcr.observation.Event.PROPERTY_ADDED ) {
                        final int pos = path.lastIndexOf('/');
                        final String propertyName = path.substring(pos+1);

                        // we are only interested in locks
                        if ( JCRHelper.NODE_PROPERTY_LOCK_OWNER.equals(propertyName) ) {
                            ((DefaultJobManager)this.jobManager).notifyActiveJob(path.substring(this.repositoryPath.length() + 1, pos));
                        }

                    } else if ( event.getType() == javax.jcr.observation.Event.NODE_REMOVED) {
                        synchronized (unloadedJobs) {
                            this.unloadedJobs.remove(path);
                        }
                        ((DefaultJobManager)this.jobManager).notifyRemoveJob(path.substring(this.repositoryPath.length() + 1));
                    }
                    if ( loadNodePath != null ) {
                        if ( s == null ) {
                            s = this.environment.createAdminSession();
                        }
                        // we do a sanity check if the node exists first
                        if ( s.itemExists(loadNodePath) ) {
                            final Node eventNode = (Node) s.getItem(loadNodePath);
                            if ( eventNode.isNodeType(JCRHelper.JOB_NODE_TYPE) ) {
                                if ( event.getType() == javax.jcr.observation.Event.NODE_ADDED ) {
                                    logger.debug("New job has been added. Trying to load from {}", loadNodePath);
                                } else {
                                    logger.debug("Job execution failed by someone else. Trying to load from {}", loadNodePath);
                                }
                                tryToLoadJob(eventNode, this.unloadedJobs);
                            }
                        }
                    }
                } catch (RepositoryException re) {
                    this.logger.error("Exception during jcr event processing.", re);
                }

            }
        } finally {
            if ( s != null ) {
                s.logout();
            }
        }
    }

    /**
     * Return the query for the clean up.
     */
    private Query getCleanUpQuery(final Session s)
    throws RepositoryException {
        final String selectorName = "nodetype";
        final Calendar deleteBefore = Calendar.getInstance();
        deleteBefore.add(Calendar.MINUTE, -this.cleanupPeriod);

        final QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

        final Query q = qomf.createQuery(
                qomf.selector(JCRHelper.JOB_NODE_TYPE, selectorName),
                qomf.and(qomf.descendantNode(selectorName, this.repositoryPath),
                         qomf.comparison(qomf.propertyValue(selectorName, JCRHelper.NODE_PROPERTY_FINISHED),
                                       QueryObjectModelFactory.JCR_OPERATOR_LESS_THAN,
                                       qomf.literal(s.getValueFactory().createValue(deleteBefore)))),
                null,
                null
        );
        return q;
    }

    /**
     * This method is invoked periodically.
     * @see java.lang.Runnable#run()
     */
    public void cleanup() {
        // remove obsolete jobs from the repository
        if ( this.running && this.jobManager.isJobProcessingEnabled() ) {
            this.logger.debug("Cleaning up repository: removing all finished jobs older than {} minutes.", this.cleanupPeriod);

            // we create an own session to avoid concurrency issues
            Session s = null;
            try {
                s = this.environment.createAdminSession();
                final Query q = this.getCleanUpQuery(s);
                if ( logger.isDebugEnabled() ) {
                    logger.debug("Executing query {}", q.getStatement());
                }
                final NodeIterator iter = q.execute().getNodes();
                int count = 0;
                while ( iter.hasNext() ) {
                    final Node eventNode = iter.nextNode();
                    eventNode.remove();
                    count++;
                }
                s.save();
                logger.debug("Removed {} entries from the repository.", count);

            } catch (RepositoryException e) {
                // in the case of an error, we just log this as a warning
                this.logger.warn("Exception during repository cleanup.", e);
            } finally {
                if ( s != null ) {
                    s.logout();
                }
            }

            cleanUpCounter++;
            // we do a full cleanup every 12th run
            if ( cleanUpCounter % 12 == 0 ) {
                this.fullEmptyFolderCleanup();
            } else {
                this.simpleEmptyFolderCleanup();
            }
        }
    }

    /**
     * Simple empty folder removes empty folders for the last five minutes
     * from an hour ago!
     * If folder for minute 59 is removed, we check the hour folder as well.
     */
    private void simpleEmptyFolderCleanup() {
        this.logger.debug("Cleaning up repository: looking for empty folders");
        // we create an own session to avoid concurrency issues
        Session s = null;
        try {
            s = this.environment.createAdminSession();
            final Calendar cleanUpDate = Calendar.getInstance();
            // go back ten minutes
            cleanUpDate.add(Calendar.HOUR, -1);
            for(int i = 0; i < 5; i++) {
                final StringBuilder sb = Utility.getAnonPath(cleanUpDate);

                final String path = this.repositoryPath + '/' + sb.toString();

                if ( s.nodeExists(path) ) {
                    final Node dir = s.getNode(path);
                    if ( !dir.hasNodes() ) {
                        dir.remove();
                        s.save();
                    }
                }
                // check hour folder
                if ( path.endsWith("59") ) {
                    final String hourPath = path.substring(0, path.length() - 3);
                    if ( s.nodeExists(hourPath) ) {
                        final Node hourNode = s.getNode(hourPath);
                        if ( !hourNode.hasNodes() ) {
                            hourNode.remove();
                            s.save();
                        }
                    }
                }
                cleanUpDate.add(Calendar.MINUTE, -1);
            }

        } catch (RepositoryException e) {
            // in the case of an error, we just log this as a warning
            this.logger.warn("Exception during repository cleanup.", e);
        } finally {
            if ( s != null ) {
                s.logout();
            }
        }
    }

    /**
     * Full cleanup - this scans all directories!
     */
    private void fullEmptyFolderCleanup() {
        this.logger.debug("Cleaning up repository: removing ALL empty folders");
        Session s = null;
        try {
            s = this.environment.createAdminSession();

            final String startPath = this.repositoryPath + "/anon";
            final Node startNode = (s.nodeExists(startPath) ? s.getNode(startPath) : null);
            if ( startNode != null ) {
                final Calendar now = Calendar.getInstance();

                // we iterate over the application id nodes
                final NodeIterator idIter = startNode.getNodes();
                while ( idIter.hasNext() ) {
                    final Node idNode = idIter.nextNode();
                    // now years
                    final NodeIterator yearIter = idNode.getNodes();
                    while ( yearIter.hasNext() ) {
                        final Node yearNode = yearIter.nextNode();
                        final int year = Integer.valueOf(yearNode.getName());
                        final boolean oldYear = year < now.get(Calendar.YEAR);

                        // months
                        final NodeIterator monthIter = yearNode.getNodes();
                        while ( monthIter.hasNext() ) {
                            final Node monthNode = monthIter.nextNode();
                            final int month = Integer.valueOf(monthNode.getName());
                            final boolean oldMonth = oldYear || month < (now.get(Calendar.MONTH) + 1);

                            // days
                            final NodeIterator dayIter = monthNode.getNodes();
                            while ( dayIter.hasNext() ) {
                                final Node dayNode = dayIter.nextNode();
                                final int day = Integer.valueOf(dayNode.getName());
                                final boolean oldDay = oldMonth || day < now.get(Calendar.DAY_OF_MONTH);

                                // hours
                                final NodeIterator hourIter = dayNode.getNodes();
                                while ( hourIter.hasNext() ) {
                                    final Node hourNode = hourIter.nextNode();
                                    final int hour = Integer.valueOf(hourNode.getName());
                                    final boolean oldHour = (oldDay && (oldMonth || now.get(Calendar.HOUR_OF_DAY) > 0)) || hour < (now.get(Calendar.HOUR_OF_DAY) -1);

                                    // we only remove minutes if the hour is old
                                    if ( oldHour ) {
                                        final NodeIterator minuteIter = hourNode.getNodes();
                                        while ( minuteIter.hasNext() ) {
                                            final Node minuteNode = minuteIter.nextNode();

                                            // check if we can delete the minute
                                            if ( !minuteNode.hasNodes()) {
                                                minuteNode.remove();
                                                s.save();
                                            }
                                        }
                                    }

                                    // check if we can delete the hour
                                    if ( oldHour && !hourNode.hasNodes()) {
                                        hourNode.remove();
                                        s.save();
                                    }
                                }
                                // check if we can delete the day
                                if ( oldDay && !dayNode.hasNodes()) {
                                    dayNode.remove();
                                    s.save();
                                }
                            }

                            // check if we can delete the month
                            if ( oldMonth && !monthNode.hasNodes() ) {
                                monthNode.remove();
                                s.save();
                            }
                        }

                        // check if we can delete the year
                        if ( oldYear && !yearNode.hasNodes() ) {
                            yearNode.remove();
                            s.save();
                        }
                    }
                }
            }

        } catch (RepositoryException e) {
            // in the case of an error, we just log this as a warning
            this.logger.warn("Exception during repository cleanup.", e);
        } finally {
            if ( s != null ) {
                s.logout();
            }
        }
    }

    /**
     * Load all active jobs from the repository.
     */
    private void loadJobsInTheBackground(final long backgroundLoadDelay,
            final long backgroundCheckDelay,
            final long loadThreshold,
            final long maxLoadJobs) {
        final long startTime = System.currentTimeMillis();
        // give the system some time to start
        try {
            Thread.sleep(1000 * backgroundLoadDelay); // default is 30 seconds
        } catch (InterruptedException e) {
            this.ignoreException(e);
        }
        // are we still running?
        if ( this.running ) {
            logger.debug("Starting background loading.");
            long loadSince = -1;
            do {
                loadSince = this.loadJobs(loadSince, startTime, maxLoadJobs);
                if ( this.running && loadSince > -1 ) {
                    do {
                        try {
                            Thread.sleep(1000 * backgroundCheckDelay); // default is 240 seconds
                        } catch (InterruptedException e) {
                            this.ignoreException(e);
                        }
                    } while ( this.running && this.jobManager.getStatistics().getNumberOfJobs() > loadThreshold );
                }
            } while (this.running && loadSince > -1);
            logger.debug("Finished background loading.");
        }
    }

    /**
     * Load a batch of active jobs from the repository.
     */
    private long loadJobs(final long since, final long startTime, final long maxLoadJobs) {
        long eventCreated = since;
        final long maxLoad = (since == -1 ? maxLoadJobs : maxLoadJobs - this.jobManager.getStatistics().getNumberOfJobs());
        // sanity check
        if ( maxLoad > 0 ) {
            logger.debug("Loading from repository since {} and max {}", since, maxLoad);
            Session session = null;
            try {
                session = this.environment.createAdminSession();
                final QueryManager qManager = session.getWorkspace().getQueryManager();
                final ValueFactory vf = session.getValueFactory();
                final String selectorName = "nodetype";
                final Calendar startDate = Calendar.getInstance();
                startDate.setTimeInMillis(startTime);

                final QueryObjectModelFactory qomf = qManager.getQOMFactory();

                Constraint constraint = qomf.and(
                        qomf.descendantNode(selectorName, this.repositoryPath),
                        qomf.not(qomf.propertyExistence(selectorName, JCRHelper.NODE_PROPERTY_FINISHED)));
                constraint = qomf.and(constraint,
                        qomf.comparison(qomf.propertyValue(selectorName, JCRHelper.NODE_PROPERTY_CREATED),
                                QueryObjectModelFactory.JCR_OPERATOR_LESS_THAN,
                                qomf.literal(vf.createValue(startDate))));
                if ( since != -1 ) {
                    final Calendar beforeDate = Calendar.getInstance();
                    beforeDate.setTimeInMillis(since);
                    constraint = qomf.and(constraint,
                            qomf.comparison(qomf.propertyValue(selectorName, JCRHelper.NODE_PROPERTY_CREATED),
                                    QueryObjectModelFactory.JCR_OPERATOR_GREATER_THAN,
                                    qomf.literal(vf.createValue(beforeDate))));
                }
                final Query q = qomf.createQuery(
                        qomf.selector(JCRHelper.JOB_NODE_TYPE, selectorName),
                        constraint,
                        new Ordering[] {qomf.ascending(qomf.propertyValue(selectorName, JCRHelper.NODE_PROPERTY_CREATED))},
                        null
                );
                final NodeIterator result = q.execute().getNodes();
                long count = 0;
                while ( result.hasNext() && count < maxLoad ) {
                    final Node eventNode = result.nextNode();
                    final String propPath = eventNode.getPath() + '/' + JCRHelper.NODE_PROPERTY_CREATED;
                    if ( session.itemExists(propPath) ) {
                        eventCreated = eventNode.getProperty(JCRHelper.NODE_PROPERTY_CREATED).getLong();
                        if ( tryToLoadJob(eventNode, this.unloadedJobs) ) {
                            count++;
                        }
                    }
                }
                // now we have to add all jobs with the same created time!
                boolean done = false;
                while ( result.hasNext() && !done ) {
                    final Node eventNode = result.nextNode();
                    final String propPath = eventNode.getPath() + '/' + JCRHelper.NODE_PROPERTY_CREATED;
                    if ( session.itemExists(propPath) ) {
                        final long created = eventNode.getProperty(JCRHelper.NODE_PROPERTY_CREATED).getLong();
                        if ( created == eventCreated ) {
                            if ( tryToLoadJob(eventNode, this.unloadedJobs) ) {
                                count++;
                            }
                        } else {
                            done = true;
                        }
                    }
                }
                // have we processed all jobs?
                if ( !done && !result.hasNext() ) {
                    eventCreated = -1;
                }
                logger.debug("Loaded {} jobs and new since {}", count, eventCreated);
            } catch (RepositoryException re) {
                this.logger.error("Exception during initial loading of stored jobs.", re);
            } finally {
                if ( session != null ) {
                    session.logout();
                }
            }
        }
        return eventCreated;
    }

    /**
     * Try to load a job from an event node in the repository.
     * @param eventNode       The node to read the event from
     * @param unloadedJobSet  The set of unloaded jobs - if loading fails, the node path is added here
     * @return <code>true</code> If the job can be loaded.
     */
    private boolean tryToLoadJob(final Node eventNode, final Set<String> unloadedJobSet) {
        try {
            final String nodePath = eventNode.getPath();
            // first check: job should not be finished
            if ( !eventNode.hasProperty(JCRHelper.NODE_PROPERTY_FINISHED)) {
                boolean shouldProcess = true;
                // second check: is this a job that should only run on the instance that it was created on?
                if ( eventNode.hasProperty(JobUtil.PROPERTY_JOB_RUN_LOCAL) &&
                     !eventNode.getProperty(JCRHelper.NODE_PROPERTY_APPLICATION).getString().equals(Environment.APPLICATION_ID)) {
                    shouldProcess = false;
                    if ( logger.isDebugEnabled() ) {
                         logger.debug("Discarding job at {} : local job for a different application node.", nodePath);
                    }
                }
                Event event = null;
                try {
                    event = this.readEvent(eventNode, false);
                } catch (ClassNotFoundException cnfe) {
                    // store path for lazy loading
                    synchronized ( unloadedJobSet ) {
                        unloadedJobSet.add(nodePath);
                    }
                    this.ignoreException(cnfe);
                } catch (RepositoryException re) {
                    this.logger.error("Unable to load stored job from " + nodePath, re);
                }

                if ( event == null ) {
                    try {
                        event = this.readEvent(eventNode, true);
                        shouldProcess = false;
                    } catch (ClassNotFoundException cnfe) {
                        // this can't occur
                    } catch (RepositoryException re) {
                        this.logger.error("Unable to load stored job from " + nodePath, re);
                    }
                }
                if ( event != null ) {
                    ((DefaultJobManager)this.jobManager).notifyAddJob(new JCRJobEvent(event, this));
                    if ( shouldProcess ) {
                        this.process(event);
                    }
                }
                return shouldProcess && event != null;
            }
            // if the node is finished, this is usually an unlock event
            ((DefaultJobManager)this.jobManager).notifyRemoveJob(nodePath.substring(this.repositoryPath.length() + 1));
        } catch (RepositoryException re) {
            this.logger.error("Unable to load stored job from " + eventNode, re);
        }
        return false;
    }

    /**
     * Background thread for writing jobs to the repository
     */
    private void persistJobs() {
        logger.debug("Apache Sling Job Persistence Handler started on instance {}", Environment.APPLICATION_ID);
        Session writerSession = null;
        Node rootNode = null;

        try {
            writerSession = this.environment.createAdminSession();
            // we only listen for all node added events not coming from this session(!)
            writerSession.getWorkspace().getObservationManager().addEventListener(
                    new EventListener() {

                        public void onEvent(final EventIterator events) {
                            PersistenceHandler.this.onEvent(events);
                        }
                    },
                         javax.jcr.observation.Event.NODE_ADDED,
                         this.repositoryPath,
                         true,
                         null,
                         null,
                         true);
            rootNode = this.createPath(writerSession.getRootNode(),
                    this.repositoryPath.substring(1),
                    JCRHelper.NODETYPE_ORDERED_FOLDER);
            writerSession.save();

            try {
                this.processWriteQueue(rootNode);
             } catch (Throwable t) { //NOSONAR
                 logger.error("Writer thread stopped with exception: " + t.getMessage(), t);
                 running = false;
             }
        } catch (RepositoryException e) {
            // there is nothing we can do except log!
            logger.error("Error during session starting.", e);
            running = false;
        } finally {
            if ( writerSession != null ) {
                try {
                    writerSession.getWorkspace().getObservationManager().removeEventListener(this);
                } catch (RepositoryException e) {
                    // we just ignore it
                    this.logger.warn("Unable to remove event listener.", e);
                }
                writerSession.logout();
            }
        }
    }

    /**
     * The writer queue. One job is written on each run.
     */
    private void processWriteQueue(final Node rootNode) {
        while ( this.running ) {
            // so let's wait/get the next job from the queue
            Event event = null;
            try {
                event = this.writeQueue.take();
            } catch (InterruptedException e) {
                // we ignore this
                this.ignoreException(e);
            }
            if ( event != null && this.running ) {
                if ( logger.isDebugEnabled() ) {
                    logger.debug("Persisting job {}", EventUtil.toString(event));
                }
                final String jobId = (String)event.getProperty(JobUtil.PROPERTY_JOB_NAME);
                final String jobTopic = (String)event.getProperty(JobUtil.PROPERTY_JOB_TOPIC);
                final String nodePath = Utility.getUniquePath(jobTopic, jobId);

                Node readAndProcess = null;

                // if the job has no job id, we can just write the job to the repo and don't
                // need locking
                if ( jobId == null ) {
                    try {
                        readAndProcess = this.writeEvent(rootNode, event, nodePath);
                    } catch (final RepositoryException re ) {
                        // something went wrong, so let's log it
                        this.logger.error("Exception during writing new job '" + EventUtil.toString(event) + "' to repository at " + nodePath, re);
                    }
                } else {
                    try {
                        // let's first search for an existing node with the same id
                        Node foundNode = null;
                        if ( rootNode.hasNode(nodePath) ) {
                            foundNode = rootNode.getNode(nodePath);
                        }
                        if ( foundNode == null ) {
                            // We now write the event into the repository
                            try {
                                readAndProcess = this.writeEvent(rootNode, event, nodePath);
                            } catch (ItemExistsException iee) {
                                // someone else did already write this node in the meantime
                                // nothing to do for us
                            }
                        }
                    } catch (final RepositoryException re ) {
                        // something went wrong, so let's log it
                        this.logger.error("Exception during writing new job '" + EventUtil.toString(event) + "' to repository at " + nodePath, re);
                    }
                }

                if ( readAndProcess != null ) {
                    tryToLoadJob(readAndProcess, this.unloadedJobs);
                }
            }
        }
    }

    /**
     * Return the repository path.
     */
    public String getRepositoryPath() {
        return this.repositoryPath;
    }

    /**
     * Write an event to the repository.
     * @param rootNode The root node for all jobs
     * @param e The event
     * @param suggestedName A suggested name/path for the node.
     * @throws RepositoryException
     */
    private Node writeEvent(final Node rootNode, final Event e, final String path)
    throws RepositoryException {
        // create new node with name of topic
        final Node eventNode = this.createPath(rootNode,
                path,
                JCRHelper.JOB_NODE_TYPE);
        JCRHelper.writeEventProperties(eventNode, e);

        eventNode.setProperty(JCRHelper.NODE_PROPERTY_CREATED, Calendar.getInstance());
        eventNode.setProperty(JCRHelper.NODE_PROPERTY_APPLICATION, Environment.APPLICATION_ID);

        // job topic
        eventNode.setProperty(JCRHelper.NODE_PROPERTY_TOPIC, (String)e.getProperty(JobUtil.PROPERTY_JOB_TOPIC));
        // job id
        final String jobId = (String)e.getProperty(JobUtil.PROPERTY_JOB_NAME);
        if ( jobId != null ) {
            eventNode.setProperty(JCRHelper.NODE_PROPERTY_JOBID, jobId);
        }
        boolean refresh = true;
        try {
            rootNode.getSession().save();
            refresh = false;
        } finally {
            if ( refresh ) {
                try {
                    rootNode.getSession().refresh(false);
                } catch (final RepositoryException ignore) {
                    this.ignoreException(ignore);
                }
            }
        }
        return eventNode;
    }

    /**
     * Read an event from the repository.
     * This method is similar as {@link #readEvent(Node, boolean)} with the exception
     * that it even loads the event if classes are missing
     * @throws RepositoryException
     */
    public Event forceReadEvent(Node eventNode)
    throws RepositoryException {
        try {
            return this.readEvent(eventNode, false);
        } catch (ClassNotFoundException cnfe) {
            this.ignoreException(cnfe);
        }
        // we try it again and set the force load flag
        try {
            return this.readEvent(eventNode, true);
        } catch (ClassNotFoundException cnfe) {
            // this can never happen but we catch it anyway and rethrow
            this.ignoreException(cnfe);
            throw new RepositoryException(cnfe);
        }
    }

    /**
     * Read an event from the repository.
     * @throws RepositoryException
     * @throws ClassNotFoundException
     */
    private Event readEvent(Node eventNode, final boolean forceLoad)
    throws RepositoryException, ClassNotFoundException {
        final String topic = eventNode.getProperty(JCRHelper.NODE_PROPERTY_TOPIC).getString();
        final ClassLoader cl = this.environment.getDynamicClassLoader();
        final Dictionary<String, Object> eventProps = JCRHelper.readEventProperties(eventNode, cl, forceLoad);

        eventProps.put(JobUtil.JOB_ID, eventNode.getPath().substring(this.repositoryPath.length() + 1));
        // convert to integers (jcr only supports long)
        if ( eventProps.get(JobUtil.PROPERTY_JOB_RETRIES) != null ) {
            eventProps.put(JobUtil.PROPERTY_JOB_RETRIES, Integer.valueOf(eventProps.get(JobUtil.PROPERTY_JOB_RETRIES).toString()));
        }
        if ( eventProps.get(JobUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
            eventProps.put(JobUtil.PROPERTY_JOB_RETRY_COUNT, Integer.valueOf(eventProps.get(JobUtil.PROPERTY_JOB_RETRY_COUNT).toString()));
        } else {
            eventProps.put(JobUtil.PROPERTY_JOB_RETRY_COUNT, new Integer(0));
        }
        // add application id
        eventProps.put(EventUtil.PROPERTY_APPLICATION, eventNode.getProperty(JCRHelper.NODE_PROPERTY_APPLICATION).getString());
        // and created
        eventProps.put(JCRHelper.NODE_PROPERTY_CREATED, eventNode.getProperty(JCRHelper.NODE_PROPERTY_CREATED).getDate());
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
     * Helper method which just logs the exception in debug mode.
     * @param e
     */
    private void ignoreException(final Exception e) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Ignored exception " + e.getMessage(), e);
        }
    }

    /**
     * Check the job topic of an event
     */
    private boolean checkJobTopic(final Event job) {
        final String jobTopic = (String)job.getProperty(JobUtil.PROPERTY_JOB_TOPIC);
        boolean topicIsCorrect = false;
        if ( jobTopic != null ) {
            try {
                @SuppressWarnings("unused")
                final Event testEvent = new Event(jobTopic, (Dictionary<String, Object>)null);
                topicIsCorrect = true;
            } catch (IllegalArgumentException iae) {
                // we just have to catch it
            }
            if ( !topicIsCorrect ) {
                logger.warn("Discarding job {} : job has an illegal job topic {}", EventUtil.toString(job), jobTopic);
            }
        } else {
            logger.warn("Discarding job {} : job topic is missing", EventUtil.toString(job));
        }
        return topicIsCorrect;
    }

    /**
     * Store an event in the repository by putting it in the write queue.
     */
    private void store(final Event event) {
        if ( logger.isDebugEnabled() ) {
            logger.debug("Handling local job {}", EventUtil.toString(event));
        }
        // check job topic
        if ( this.checkJobTopic(event) ) {
            try {
                this.writeQueue.put(event);
            } catch (InterruptedException e) {
                // this should never happen
                this.ignoreException(e);
            }
        }
    }

    /**
     * Try to reload unloaded jobs - this method is invoked if bundles have been added etc.
     */
    private void tryToReloadUnloadedJobs() {
        // bundle event started or updated
        boolean doIt = false;
        synchronized ( this.unloadedJobs ) {
            if ( this.unloadedJobs.size() > 0 ) {
                doIt = true;
            }
        }
        if ( doIt ) {
            final Runnable t = new Runnable() {

                public void run() {
                    synchronized (unloadedJobs) {
                        Session s = null;
                        final Set<String> newUnloadedJobs = new HashSet<String>();
                        newUnloadedJobs.addAll(unloadedJobs);
                        try {
                            s = environment.createAdminSession();
                            for(String path : unloadedJobs ) {
                                newUnloadedJobs.remove(path);
                                try {
                                    if ( s.itemExists(path) ) {
                                        final Node eventNode = (Node) s.getItem(path);
                                        tryToLoadJob(eventNode, newUnloadedJobs);
                                    }
                                } catch (RepositoryException re) {
                                    // we ignore this and readd
                                    newUnloadedJobs.add(path);
                                    ignoreException(re);
                                }
                            }
                        } catch (RepositoryException re) {
                            // unable to create session, so we try it again next time
                            ignoreException(re);
                        } finally {
                            if ( s != null ) {
                                s.logout();
                            }
                            unloadedJobs.clear();
                            unloadedJobs.addAll(newUnloadedJobs);
                        }
                    }
                }

            };
            Environment.THREAD_POOL.execute(t);
        }
    }

    /**
     * Process the event and pass it on to the queue manager.
     * Check topic and local flag first!
     */
    private void process(final Event event) {
        if ( !checkJobTopic(event) ) {
            return;
        }
        if ( logger.isDebugEnabled() ) {
            logger.debug("Received new job {}", EventUtil.toString(event));
        }
        // check for local only jobs and remove them from the queue if they're meant
        // for another application node
        final String appId = (String)event.getProperty(EventUtil.PROPERTY_APPLICATION);
        if ( event.getProperty(JobUtil.PROPERTY_JOB_RUN_LOCAL) != null
            && appId != null && !Environment.APPLICATION_ID.equals(appId) ) {
            if ( logger.isDebugEnabled() ) {
                 logger.debug("Discarding job {} : local job for a different application node.", EventUtil.toString(event));
            }
        } else {
            final JobEvent info = new JCRJobEvent(event, this);
            ((DefaultJobManager)this.jobManager).process(info);
        }
    }

    /**
     * Try to lock the node in the repository.
     * Locking might fail if:
     * - the node has been removed
     * - the job has alreay been processed
     * - someone else locked it already
     *
     * @param info The job event
     * @return <code>true</code> if the node could be locked
     */
    public boolean lock(final JobEvent info) {
        final String path = this.getNodePath(info.uniqueId);
        synchronized ( this.backgroundLock ) {
            if ( !this.running ) {
                return false;
            }
            try {
                // check if the node still exists
                if ( this.backgroundSession.itemExists(path)
                     && !this.backgroundSession.itemExists(path + '/' + JCRHelper.NODE_PROPERTY_FINISHED)) {

                    final Node eventNode = (Node) this.backgroundSession.getItem(path);
                    if ( !eventNode.isLocked() ) {
                        // lock node
                        try {
                            this.lockManager.lock(this.backgroundSession, path);
                        } catch (RepositoryException re) {
                            // lock failed which means that the node is locked by someone else, so we don't have to requeue
                            return false;
                        }
                        ((DefaultJobManager)this.jobManager).notifyActiveJob(info.uniqueId);
                        return true;
                    }
                }
            } catch (RepositoryException re) {
                this.ignoreException(re);
            }
        }
        return false;
    }

    /**
     * Try to restart the job
     */
    public void restart(final JobEvent info) {
        final String path = this.getNodePath(info.uniqueId);
        synchronized ( this.backgroundLock ) {
            if ( this.running ) {
                try {
                    if ( this.backgroundSession.itemExists(path) ) {
                        final Node eventNode = (Node) this.backgroundSession.getItem(path);
                        this.tryToLoadJob(eventNode, this.unloadedJobs);
                    }
                } catch (RepositoryException re) {
                    this.ignoreException(re);
                }
            }
        }
    }

    /**
     * Unlock the node for the event
     */
    public void unlock(final JobEvent info) {
        final String path = this.getNodePath(info.uniqueId);
        synchronized ( this.backgroundLock ) {
            if ( !this.running ) {
                return;
            }
            try {
                this.lockManager.unlock(this.backgroundSession, path);
            } catch (RepositoryException re) {
                // there is nothing we can do
                this.ignoreException(re);
            }
        }
    }

    /**
     * Finish the job
     */
    public void finished(final JobEvent info) {
        final String jobId = (String)info.event.getProperty(JobUtil.PROPERTY_JOB_NAME);
        final String path = this.getNodePath(info.uniqueId);
        synchronized ( this.backgroundLock ) {
            if ( !this.running ) {
                return;
            }
            try {
                ((DefaultJobManager)this.jobManager).notifyRemoveJob(info.uniqueId);
                if ( this.backgroundSession.itemExists(path) ) {
                    final Node eventNode = (Node)this.backgroundSession.getItem(path);
                    if ( jobId == null ) {
                        // simply remove the node
                        eventNode.remove();
                    } else {
                        eventNode.setProperty(JCRHelper.NODE_PROPERTY_FINISHED, Calendar.getInstance());
                        eventNode.setProperty(JCRHelper.NODE_PROPERTY_PROCESSOR, Environment.APPLICATION_ID);
                    }
                    this.backgroundSession.save();
                    // and unlock
                    if ( jobId != null && eventNode.isLocked() ) {
                        this.lockManager.unlock(this.backgroundSession, path);
                    }
                }
            } catch (final RepositoryException re) {
                // there is nothing we can do
                this.ignoreException(re);
                try {
                    this.backgroundSession.refresh(false);
                } catch (final RepositoryException ignore) {
                    this.ignoreException(ignore);
                }
            }
        }
    }

    private String getNodePath(final String jobId) {
        return this.repositoryPath + '/' + jobId;
    }

    /**
     * Remove the job - if not currently in processing.
     */
    public boolean remove(final String jobId) {
        if ( this.backgroundSession != null && jobId != null ) {
            final String path = this.getNodePath(jobId);
            synchronized ( this.backgroundLock ) {
                try {
                    if ( this.backgroundSession.itemExists(path) ) {
                        final Node eventNode = (Node) this.backgroundSession.getItem(path);
                        if ( eventNode.isLocked() ) {
                            this.logger.debug("Attempted to cancel a running job at {}", path);
                            return false;
                        }
                        // try to load job to send notification
                        try {
                            final Event job = this.forceReadEvent(eventNode);
                            Utility.sendNotification(this.environment, JobUtil.TOPIC_JOB_CANCELLED, job, null);
                        } catch (RepositoryException ignore) {
                            this.ignoreException(ignore);
                        }
                        eventNode.remove();
                        this.backgroundSession.save();
                        ((DefaultJobManager)this.jobManager).notifyRemoveJob(jobId);
                    }
                } catch (RepositoryException e) {
                    this.logger.error("Error during cancelling job at " + path, e);
                    try {
                        this.backgroundSession.refresh(false);
                    } catch (final RepositoryException ignore) {
                        this.ignoreException(ignore);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Reschedule the job
     */
    public boolean reschedule(final JobEvent info) {
        final String path = this.getNodePath(info.uniqueId);
        synchronized ( this.backgroundLock ) {
            try {
                if ( this.backgroundSession.itemExists(path) ) {
                    final Node eventNode = (Node)this.backgroundSession.getItem(path);
                    if ( info.event.getProperty(JobUtil.PROPERTY_JOB_RETRIES) != null ) {
                        eventNode.setProperty(JobUtil.PROPERTY_JOB_RETRIES, (Integer)info.event.getProperty(JobUtil.PROPERTY_JOB_RETRIES));
                    }
                    if ( info.event.getProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
                        eventNode.setProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT, (Integer)info.event.getProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT));
                    }
                    eventNode.setProperty(JCRHelper.NODE_PROPERTY_PROCESSOR, Environment.APPLICATION_ID);
                    this.backgroundSession.save();

                    // and unlock
                    this.lockManager.unlock(this.backgroundSession, path);
                    ((DefaultJobManager)this.jobManager).notifyRescheduleJob(info.uniqueId);
                    return true;
                }
            } catch (RepositoryException re) {
                // there is nothing we can do
                this.ignoreException(re);
                try {
                    this.backgroundSession.refresh(false);
                } catch (final RepositoryException ignore) {
                    this.ignoreException(ignore);
                }
            }
        }
        ((DefaultJobManager)this.jobManager).notifyRemoveJob(info.uniqueId);
        return false;
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
                            node.getSession().refresh(false);
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

    /**
     * This method is invoked periodically by the scheduler.
     * @see java.lang.Runnable#run()
     */
    public void run() {
        this.cleanup();
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(final Event event) {
        if ( logger.isDebugEnabled() ) {
            logger.debug("Receiving event {}", EventUtil.toString(event));
        }
        // we ignore remote job events
        if ( EventUtil.isLocal(event) ) {
            // check for bundle event
            if ( event.getTopic().equals(JobUtil.TOPIC_JOB)) {
                this.store(event);
            } else {
                // bundle event started or updated
                this.tryToReloadUnloadedJobs();
            }
        }
    }

    /**
     * Check if the job is still alive = unfinished node in repository
     */
    public boolean isAlive(final JCRJobEvent info) {
        final String path = this.getNodePath(info.uniqueId);
        Session s = null;
        try {
            s = this.environment.createAdminSession();
            if ( s.itemExists(path) ) {
                final String finishedPath = path + '/' + JCRHelper.NODE_PROPERTY_FINISHED;
                if ( !s.itemExists(finishedPath) ) {
                    return true;
                }
            }
        } catch (final RepositoryException re) {
            // there is nothing we can do
            this.ignoreException(re);
        } finally {
            if ( s != null ) {
                s.logout();
            }
        }
        return false;
    }
}
