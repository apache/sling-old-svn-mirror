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
package org.apache.sling.replication.serialization.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.impl.jobs.config.ConfigurationConstants;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.event.ReplicationEventType;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuilderProvider;
import org.apache.sling.replication.serialization.ReplicationPackageImporter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link org.apache.sling.replication.serialization.ReplicationPackageImporter}
 */
@Component
@Service(value = ReplicationPackageImporter.class)
public class DefaultReplicationPackageImporter implements ReplicationPackageImporter {

    private static final String QUEUE_NAME = "replication-package-import";
    private static final String QUEUE_TOPIC = "org/apache/sling/replication/import";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReplicationPackageBuilderProvider replicationPackageBuilderProvider;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    @Reference
    private JobManager jobManager;

    @Reference
    private ConfigurationAdmin configAdmin;

    private ServiceRegistration jobReg;


    @Activate
    protected void activate(BundleContext context) throws Exception {
        try {
            if (jobManager.getQueue(QUEUE_NAME) == null) {
                Configuration config = configAdmin.createFactoryConfiguration(
                        QueueConfiguration.class.getName(), null);
                Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put(ConfigurationConstants.PROP_NAME, QUEUE_NAME);
                props.put(ConfigurationConstants.PROP_TYPE, QueueConfiguration.Type.ORDERED.name());
                props.put(ConfigurationConstants.PROP_TOPICS, new String[]{QUEUE_TOPIC, QUEUE_TOPIC + "/*"});
                props.put(ConfigurationConstants.PROP_RETRIES, -1);
                props.put(ConfigurationConstants.PROP_RETRY_DELAY, 2000L);
                props.put(ConfigurationConstants.PROP_KEEP_JOBS, true);
                props.put(ConfigurationConstants.PROP_PRIORITY, "MAX");
                config.update(props);
            }
        } catch (IOException e) {
            throw new ReplicationQueueException("could not create an import queue", e);
        }

        Dictionary<String, Object> jobProps = new Hashtable<String, Object>();
        jobProps.put(JobConsumer.PROPERTY_TOPICS, new String[]{QUEUE_TOPIC});
        jobReg = context.registerService(JobConsumer.class.getName(), new ReplicationPackageImporterJobConsumer(), jobProps);
    }

    @Deactivate
    public void deactivate() {
        if (jobReg != null) {
            jobReg.unregister();
        }
    }

    public boolean importStream(InputStream stream, String type) {
        boolean success = false;
        try {
            ReplicationPackage replicationPackage = null;
            if (type != null) {
                ReplicationPackageBuilder replicationPackageBuilder = replicationPackageBuilderProvider.getReplicationPackageBuilder(type);
                if (replicationPackageBuilder != null) {
                    replicationPackage = replicationPackageBuilder.readPackage(stream, true);
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("cannot read streams of type {}", type);
                    }
                }
            } else {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(stream); // needed to allow for multiple reads
                for (ReplicationPackageBuilder replicationPackageBuilder : replicationPackageBuilderProvider.getAvailableReplicationPackageBuilders()) {
                    try {
                        replicationPackage = replicationPackageBuilder.readPackage(bufferedInputStream, true);
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn("received stream cannot be read with {}", replicationPackageBuilder);
                        }
                    }
                }
            }

            if (replicationPackage != null) {
                if (log.isInfoEnabled()) {
                    log.info("replication package read and installed for path(s) {}",
                            Arrays.toString(replicationPackage.getPaths()));
                }

                Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
                dictionary.put("replication.action", replicationPackage.getAction());
                dictionary.put("replication.path", replicationPackage.getPaths());
                replicationEventFactory.generateEvent(ReplicationEventType.PACKAGE_INSTALLED, dictionary);
                success = true;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("could not read a replication package");
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("cannot import a package from the given stream of type {}", type);
            }
        }
        return success;
    }

    public void scheduleImport(InputStream stream, String type) throws Exception {
        try {
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("bin", IOUtils.toString(stream)); // TODO : compress / encode the stream
            properties.put("type", type);
            Job job = jobManager.createJob(QUEUE_TOPIC).properties(properties).add();
            if (log.isInfoEnabled()) {
                log.info("job added {}", job);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("could not add an item to the queue");
            }
            throw e;
        }
    }

    private class ReplicationPackageImporterJobConsumer implements JobConsumer {

        public JobResult process(Job job) {
            try {
                InputStream stream = IOUtils.toInputStream(String.valueOf(job.getProperty("bin"))); // TODO : decompress / decode the stream string
                boolean result = importStream(stream, String.valueOf(job.getProperty("type")));
                return result ? JobResult.OK : JobResult.FAILED;
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("could not process import job correctly", e);
                }
                return JobResult.FAILED;
            }
        }
    }


}
