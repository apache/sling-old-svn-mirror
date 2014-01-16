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
package org.apache.sling.replication.queue.impl.jobhandling;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobHandlingUtils {

    private static final Logger log = LoggerFactory.getLogger(JobHandlingUtils.class.getName());

    private static final String PATHS = "replication.package.paths";

    private static final String ID = "replication.package.id";

    private static final String LENGTH = "replication.package.length";

    private static final String BIN = "replication.package.stream";

    private static final String TYPE = "replication.package.type";

    protected static final String ACTION = "replication.package.action";

    public static ReplicationPackage getPackage(ScheduledJobInfo info, JobManager jobManager,
                    String topic) {
        Job job = getJob(info, jobManager, topic);
        return JobHandlingUtils.getPackage(job);
    }

    public static Job getJob(ScheduledJobInfo info, JobManager jobManager, String topic) {
        String id = String.valueOf(info.getJobProperties().get(ID));
        Map<String, Object> jobProps = JobHandlingUtils.createIdPropertiesFromId(id);
        return jobManager.getJob(topic, jobProps);
    }

    public static ReplicationPackage getPackage(ReplicationPackageBuilder packageBuilder,
                    final Job job) {
        ReplicationPackage pkg = null;
        String id = String.valueOf(job.getProperty(ID));
        try {
            pkg = packageBuilder.getPackage(id);
            if (pkg != null) {
                if (log.isInfoEnabled()) {
                    log.info("successfully retrieved a package with id {}", id);
                }
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("failed retrieving a package with id {}", id);
            }
        }
        if (pkg == null) {
            try {
                pkg = getPackage(job);
                if (pkg != null) {
                    if (log.isInfoEnabled()) {
                        log.info("successfully deserialized a package from job {}", job);
                    }
                }
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("failed deserializing package from job {}", job, e);
                }
            }
        }
        if (pkg == null) {
            if (log.isErrorEnabled()) {
                log.error("could not find a package from job {}", job);
            }
        }
        return pkg;
    }

    public static ReplicationPackage getPackage(final Job job) {
        return new ReplicationPackage() {

            private static final long serialVersionUID = 1L;

            public String[] getPaths() {
                return (String[]) job.getProperty(PATHS);
            }

            public long getLength() {
                return (Long) job.getProperty(LENGTH);
            }

            public InputStream createInputStream() throws IOException {
                return IOUtils.toInputStream(String.valueOf(job.getProperty(BIN)));

                // workaround to make void package work while we get SLING-3140 to be released
//                return IOUtils.toInputStream(String.valueOf(job.getProperty(ID)));
            }

            public String getId() {
                return String.valueOf(job.getProperty(ID));
            }

            public String getType() {
                return String.valueOf(job.getProperty(TYPE));
            }

            public String getAction() {
                return String.valueOf(job.getProperty(ACTION));
            }
        };

    }

    public static Map<String, Object> createFullPropertiesFromPackage(
                    ReplicationPackage replicationPackage) throws IOException {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ID, replicationPackage.getId());
        properties.put(PATHS, replicationPackage.getPaths());
        properties.put(LENGTH, replicationPackage.getLength());
        properties.put(ACTION, replicationPackage.getAction());
        properties.put(BIN, IOUtils.toString(replicationPackage.createInputStream()));
        properties.put(TYPE, replicationPackage.getType());
        return properties;
    }

    public static Map<String, Object> createIdPropertiesFromPackage(
                    ReplicationPackage replicationPackage) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ID, replicationPackage.getId());
        return properties;
    }

    public static Map<String, Object> createIdPropertiesFromId(String id) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(ID, id);
        return properties;
    }

}
