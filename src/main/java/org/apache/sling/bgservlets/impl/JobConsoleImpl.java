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
package org.apache.sling.bgservlets.impl;

import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.bgservlets.ExecutionEngine;
import org.apache.sling.bgservlets.JobConsole;
import org.apache.sling.bgservlets.JobStatus;
import org.apache.sling.bgservlets.impl.storage.JobStorageException;
import org.apache.sling.bgservlets.impl.storage.NodeJobStatusFactory;
import org.apache.sling.bgservlets.impl.webconsole.JobConsolePlugin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JobConsole implementation */
@Component(
        metatype=true,
        label="%JobConsoleImpl.label",
        description="%JobConsoleImpl.description")
@Service
public class JobConsoleImpl implements JobConsole {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String JOB_QUERY = "select * from sling:bgJobData order by jcr:created desc";

    @Reference
    private ExecutionEngine executionEngine;
    
    @Reference
    private NodeJobStatusFactory jobStatusFactory;
    
    @Property(boolValue=true)
    private final static String PROP_CONSOLE_PLUGIN_ACTIVE = "console.plugin.active";
    
    public Iterator<JobStatus> getJobStatus(Session session, boolean activeOnly) {
        if(activeOnly) {
            log.debug("activeOnly is set, getting jobs from ExecutionEngine");
            return getEngineJobs();
        } else {
            log.debug("activeOnly is set, getting jobs from repository query");
            try {
                return getStoredJobs(session);
            } catch(RepositoryException re) {
                throw new JobStorageException("RepositoryException in getJobStatus(query)", re);
            }
        }
    }
    
    @Activate
    protected void activate(ComponentContext ctx) {
        final Object obj = ctx.getProperties().get(PROP_CONSOLE_PLUGIN_ACTIVE);
        final boolean pluginActive = (obj instanceof Boolean ? (Boolean)obj : true);
        if(pluginActive) {
            JobConsolePlugin.initPlugin(ctx.getBundleContext(), this);
        } else {
            log.info("{} is false, not activating JobConsolePlugin", PROP_CONSOLE_PLUGIN_ACTIVE);
        }
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        JobConsolePlugin.destroyPlugin();
    }
    
    public JobStatus getJobStatus(Session session, String path) {
        // Try ExecutionEngine first, persistent storage if not found
        JobStatus result = executionEngine.getJobStatus(path);
        if(result == null) {
            try {
                if(session.itemExists(path)) {
                    final Item i = session.getItem(path);
                    if(i.isNode()) {
                        result = jobStatusFactory.getJobStatus((Node)i);
                    }
                }
            } catch(RepositoryException re) {
                throw new JobStorageException("RepositoryException in getJobStatus(path)", re);
            }
        }
        return result;
    }

    private Iterator<JobStatus> getEngineJobs() {
        return executionEngine.getMatchingJobStatus(null);
    }
    
    private Iterator<JobStatus> getStoredJobs(Session s) throws RepositoryException {
        final Query q = s.getWorkspace().getQueryManager().createQuery(JOB_QUERY, Query.SQL);
        final NodeIterator it = q.execute().getNodes();
        return new Iterator<JobStatus>() {

            public boolean hasNext() {
                return it.hasNext();
            }

            public JobStatus next() {
                try {
                    return jobStatusFactory.getJobStatus(it.nextNode());
                } catch(RepositoryException re) {
                    throw new JobStorageException("RepositoryException in next()", re);
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
            
        };
    }
    
    public String getJobStatusPagePath(HttpServletRequest request, JobStatus jobStatus, String extension) {
        if(!extension.startsWith(".")) {
            extension = "." + extension;
        }
        return request.getContextPath() + jobStatus.getPath() + extension;
    }
    
    public String getJobStreamPath(HttpServletRequest request, JobStatus jobStatus) {
        return request.getContextPath() + jobStatus.getStreamPath();
    }
}
