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
package org.apache.sling.event.impl.jobs;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.impl.EnvironmentComponent;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration of the job handling
 *
 */
@Component(immediate=true, metatype=true,
           label="Apache Sling Job Manager",
           description="This is the central service of the job handling.",
           name="org.apache.sling.event.impl.jobs.jcr.PersistenceHandler")
@Service(value={JobManagerConfiguration.class})
@Properties({
    @Property(name=JobManagerConfiguration.PROPERTY_DISABLE_DISTRIBUTION,
              boolValue=JobManagerConfiguration.DEFAULT_DISABLE_DISTRIBUTION,
              label="Disable Distribution",
              description="If the distribution is disabled, all jobs will be processed on the leader only! "
                        + "Please use this switch with care."),
    @Property(name=JobManagerConfiguration.PROPERTY_REPOSITORY_PATH,
              value=JobManagerConfiguration.DEFAULT_REPOSITORY_PATH, propertyPrivate=true),
    @Property(name=JobManagerConfiguration.PROPERTY_SCHEDULED_JOBS_PATH,
              value=JobManagerConfiguration.DEFAULT_SCHEDULED_JOBS_PATH, propertyPrivate=true),
    @Property(name=JobManagerConfiguration.PROPERTY_BACKGROUND_LOAD_DELAY,
              longValue=JobManagerConfiguration.DEFAULT_BACKGROUND_LOAD_DELAY, propertyPrivate=true),
})
public class JobManagerConfiguration {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Default resource path for jobs. */
    public static final String DEFAULT_REPOSITORY_PATH = "/var/eventing/jobs";

    /** Default background load delay. */
    public static final long DEFAULT_BACKGROUND_LOAD_DELAY = 30;

    /** Default for disabling the distribution. */
    public static final boolean DEFAULT_DISABLE_DISTRIBUTION = false;

    /** Default resource path for scheduled jobs. */
    public static final String DEFAULT_SCHEDULED_JOBS_PATH = "/var/eventing/scheduled-jobs";

    /** The path where all jobs are stored. */
    public static final String PROPERTY_REPOSITORY_PATH = "repository.path";

    /** The background loader waits this time of seconds after startup before loading events from the repository. (in secs) */
    public static final String PROPERTY_BACKGROUND_LOAD_DELAY = "load.delay";

    /** Configuration switch for distributing the jobs. */
    public static final String PROPERTY_DISABLE_DISTRIBUTION = "job.consumermanager.disableDistribution";

    /** Configuration property for the scheduled jobs path. */
    public static final String PROPERTY_SCHEDULED_JOBS_PATH = "job.scheduled.jobs.path";

    /** Default value for background loading. */
    public static final boolean DEFAULT_BACKGROUND_LOAD_SEARCH = true;

    /** The jobs base path with a slash. */
    private String jobsBasePathWithSlash;

    /** The base path for assigned jobs. */
    private String assignedJobsPath;

    /** The base path for unassigned jobs. */
    private String unassignedJobsPath;

    /** The base path for assigned jobs to the current instance. */
    private String localJobsPath;

    /** The base path for assigned jobs to the current instance - ending with a slash. */
    private String localJobsPathWithSlash;

    /** The base path for locks. */
    private String locksPath;

    private String previousVersionAnonPath;

    private String previousVersionIdentifiedPath;

    /** The base path for locks - ending with a slash. */
    private String locksPathWithSlash;

    private long backgroundLoadDelay;

    private boolean disabledDistribution;

    private String storedCancelledJobsPath;

    private String storedSuccessfulJobsPath;

    /** The resource path where scheduled jobs are stored. */
    private String scheduledJobsPath;

    /** The resource path where scheduled jobs are stored - ending with a slash. */
    private String scheduledJobsPathWithSlash;

    /** The environment component. */
    @Reference
    private EnvironmentComponent environment;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /**
     * Activate this component.
     * @param props Configuration properties
     * @throws RuntimeException If the default paths can't be created
     */
    @Activate
    protected void activate(final Map<String, Object> props) {
        this.update(props);
        this.jobsBasePathWithSlash = PropertiesUtil.toString(props.get(PROPERTY_REPOSITORY_PATH),
                DEFAULT_REPOSITORY_PATH) + '/';

        // create initial resources
        this.assignedJobsPath = this.jobsBasePathWithSlash + "assigned";
        this.unassignedJobsPath = this.jobsBasePathWithSlash + "unassigned";
        this.locksPath = this.jobsBasePathWithSlash + "locks";
        this.locksPathWithSlash = this.locksPath.concat("/");

        this.localJobsPath = this.assignedJobsPath.concat("/").concat(Environment.APPLICATION_ID);
        this.localJobsPathWithSlash = this.localJobsPath.concat("/");

        this.previousVersionAnonPath = this.jobsBasePathWithSlash + "anon";
        this.previousVersionIdentifiedPath = this.jobsBasePathWithSlash + "identified";

        this.storedCancelledJobsPath = this.jobsBasePathWithSlash + "cancelled";
        this.storedSuccessfulJobsPath = this.jobsBasePathWithSlash + "finished";

        this.scheduledJobsPath = PropertiesUtil.toString(props.get(PROPERTY_SCHEDULED_JOBS_PATH),
            DEFAULT_SCHEDULED_JOBS_PATH);
        this.scheduledJobsPathWithSlash = this.scheduledJobsPath + "/";

        // create initial resources
        final ResourceResolver resolver = this.createResourceResolver();
        try {
            ResourceHelper.getOrCreateBasePath(resolver, this.getLocalJobsPath());
            ResourceHelper.getOrCreateBasePath(resolver, this.getUnassignedJobsPath());
            ResourceHelper.getOrCreateBasePath(resolver, this.getLocksPath());
        } catch ( final PersistenceException pe ) {
            logger.error("Unable to create default paths: " + pe.getMessage(), pe);
            throw new RuntimeException(pe);
        } finally {
            resolver.close();
        }
    }

    /**
     * Update with a new configuration
     */
    @Modified
    public void update(final Map<String, Object> props) {
        this.disabledDistribution = PropertiesUtil.toBoolean(props.get(PROPERTY_DISABLE_DISTRIBUTION), DEFAULT_DISABLE_DISTRIBUTION);
        this.backgroundLoadDelay = PropertiesUtil.toLong(props.get(PROPERTY_BACKGROUND_LOAD_DELAY), DEFAULT_BACKGROUND_LOAD_DELAY);
    }

    /**
     * Create a new resource resolver for reading and writing the resource tree.
     * The resolver needs to be closed by the client.
     * @return A resource resolver
     * @throw RuntimeException if the resolver can't be created.
     */
    public ResourceResolver createResourceResolver() {
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
        } catch ( final LoginException le) {
            logger.error("Unable to create new resource resolver: " + le.getMessage(), le);
            throw new RuntimeException(le);
        }
        return resolver;
    }

    /**
     * Get the resource path for all assigned jobs.
     * @return The path - does not end with a slash.
     */
    public String getAssginedJobsPath() {
        return this.assignedJobsPath;
    }

    /**
     * Get the resource path for all unassigned jobs.
     * @return The path - does not end with a slash.
     */
    public String getUnassignedJobsPath() {
        return this.unassignedJobsPath;
    }

    /**
     * Get the resource path for all jobs assigned to the current instance
     * @return The path - does not end with a slash
     */
    public String getLocalJobsPath() {
        return this.localJobsPath;
    }

    /**
     * Get the resource path for all locks
     * @return The path - does not end with a slash
     */
    public String getLocksPath() {
        return this.locksPath;
    }

    public long getBackgroundLoadDelay() {
        return backgroundLoadDelay;
    }

    /** Counter for jobs without an id. */
    private final AtomicLong jobCounter = new AtomicLong(0);

    /**
     * Create a unique job path (folder and name) for the job.
     */
    public String getUniquePath(final String targetId,
            final String topic,
            final String jobId,
            final Map<String, Object> jobProperties) {
        final String topicName = topic.replace('/', '.');
        final StringBuilder sb = new StringBuilder();
        if ( targetId != null ) {
            sb.append(this.assignedJobsPath);
            sb.append('/');
            sb.append(targetId);
        } else {
            sb.append(this.unassignedJobsPath);
        }
        sb.append('/');
        sb.append(topicName);
        sb.append('/');
        sb.append(jobId);

        return sb.toString();
    }

    /**
     * Get the unique job id
     */
    public String getUniqueId(final String jobTopic) {
        final String convTopic = jobTopic.replace('/', '.');

        final Calendar now = Calendar.getInstance();
        final StringBuilder sb = new StringBuilder();
        sb.append(now.get(Calendar.YEAR));
        sb.append('/');
        sb.append(now.get(Calendar.MONTH) + 1);
        sb.append('/');
        sb.append(now.get(Calendar.DAY_OF_MONTH));
        sb.append('/');
        sb.append(now.get(Calendar.HOUR_OF_DAY));
        sb.append('/');
        sb.append(now.get(Calendar.MINUTE));
        sb.append('/');
        sb.append(convTopic);
        sb.append('_');
        sb.append(Environment.APPLICATION_ID);
        sb.append('_');
        sb.append(jobCounter.getAndIncrement());

        return sb.toString();
    }

    public boolean isLocalJob(final String jobPath) {
        return jobPath != null && jobPath.startsWith(this.localJobsPathWithSlash);
    }

    public boolean isJob(final String jobPath) {
        return jobPath.startsWith(this.jobsBasePathWithSlash);
    }

    public boolean isLock(final String lockPath) {
        return lockPath.startsWith(this.locksPathWithSlash);
    }

    public String getPreviousVersionAnonPath() {
        return this.previousVersionAnonPath;
    }

    public String getPreviousVersionIdentifiedPath() {
        return this.previousVersionIdentifiedPath;
    }

    public boolean disableDistribution() {
        return this.disabledDistribution;
    }

    public String getStoredCancelledJobsPath() {
        return this.storedCancelledJobsPath;
    }

    public String getStoredSuccessfulJobsPath() {
        return this.storedSuccessfulJobsPath;
    }

    /**
     * Get the storage path for finished jobs.
     * @param finishedJob The finished job
     * @param isSuccess Whether processing was successful or not
     * @return The complete storage path
     */
    public String getStoragePath(final JobImpl finishedJob, final boolean isSuccess) {
        final String topicName = finishedJob.getTopic().replace('/', '.');
        final StringBuilder sb = new StringBuilder();
        if ( isSuccess ) {
            sb.append(this.storedSuccessfulJobsPath);
        } else {
            sb.append(this.storedCancelledJobsPath);
        }
        sb.append('/');
        sb.append(topicName);
        sb.append('/');
        sb.append(finishedJob.getId());

        return sb.toString();

    }

    /**
     * Check whether this is a storage path.
     */
    public boolean isStoragePath(final String path) {
        return path.startsWith(this.storedCancelledJobsPath) || path.startsWith(this.storedSuccessfulJobsPath);
    }

    public String getScheduledJobsPath() {
        return this.scheduledJobsPath;
    }

    public String getScheduledJobsPathWithSlash() {
        return this.scheduledJobsPathWithSlash;
    }
}
