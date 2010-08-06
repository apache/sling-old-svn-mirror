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
package org.apache.sling.bgservlets.impl.storage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.bgservlets.BackgroundServletConstants;
import org.apache.sling.bgservlets.JobData;
import org.apache.sling.bgservlets.JobStorage;
import org.apache.sling.bgservlets.impl.DeepNodeCreator;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default JobStorage implementation */
@Component(
        metatype=true, 
        label="%JobStorage.label", 
        description="%JobStorage.description")
@Service
public class JobStorageImpl implements JobStorage {

    private Logger log = LoggerFactory.getLogger(getClass());
    
    /** Configurable base path for storing job data */
    @Property(value="/var/bg/jobs")
    public static final String PROP_JOB_STORAGE_PATH = "job.storage.path";
    
    public static final String PATH_FORMAT = "/yyyy/MM/dd/HH/mm";
    public static final String JOB_NODETYPE = "nt:unstructured";
    
    private String jobStoragePath;
	private int counter;
	private static final DateFormat pathFormat = new SimpleDateFormat(PATH_FORMAT);
	
    protected void activate(ComponentContext ctx) {
        jobStoragePath = (String)ctx.getProperties().get(PROP_JOB_STORAGE_PATH);
        if(jobStoragePath == null || jobStoragePath.length() == 0) {
            throw new IllegalStateException("Missing " + PROP_JOB_STORAGE_PATH + " in ComponentContext");
        }
        if(!jobStoragePath.startsWith("/")) {
            jobStoragePath = "/" + jobStoragePath;
        }
        if(jobStoragePath.endsWith("/")) {
            jobStoragePath = jobStoragePath.substring(0, jobStoragePath.length() - 1);
        }
        log.info("Jobs will be stored under {}", jobStoragePath);
    }
    
	public JobData createJobData(Session s) {
        try {
            return getJobData(createNewJobNode(s));
        } catch(Exception e) {
            throw new JobStorageException("Unable to create new JobDataImpl", e);
        }
	}

	public JobData getJobData(Node n) {
        try {
            return new JobDataImpl(n);
        } catch(Exception e) {
            throw new JobStorageException("Unable to create JobDataImpl", e);
        }
	}

	Node createNewJobNode(Session s) throws RepositoryException {
	    String path = null;
	    synchronized (this) {
	        counter++;
	        path = jobStoragePath + pathFormat.format(new Date()) + "/" + counter;
        }
	    final Node result = new DeepNodeCreator().deepCreateNode(path, s, JOB_NODETYPE);
	    result.addMixin(JobData.JOB_DATA_MIXIN);
	    result.setProperty(BackgroundServletConstants.CREATION_TIME_PROPERTY, Calendar.getInstance());
	    result.save();
	    log.debug("Job node {} created", result.getPath());
	    return result;
	}
}
