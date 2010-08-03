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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.bgservlets.BackgroundServletConstants;
import org.apache.sling.bgservlets.JobData;
import org.apache.sling.bgservlets.JobStatus;
import org.apache.sling.bgservlets.impl.nodestream.NodeInputStream;
import org.apache.sling.bgservlets.impl.nodestream.NodeOutputStream;

class JobDataImpl implements JobData {

	private final Node node;
	private final String path;
	private final Calendar creationTime;
	
	public static final String STREAM_PATH = JobStatus.STREAM_PATH_SUFFIX.substring(1);
	
    public static final String RT_PROP = SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_TYPE;
    
	/** Build a JobDataImpl on supplied node, which must exists */
	JobDataImpl(Node n) throws RepositoryException {
		node = n;
		path = node.getPath();
		if(node.hasProperty(BackgroundServletConstants.CREATION_TIME_PROPERTY)) {
		    creationTime = node.getProperty(BackgroundServletConstants.CREATION_TIME_PROPERTY).getDate();
		} else {
		    // Set fake date if not found
		    creationTime = Calendar.getInstance();
		    creationTime.set(1900, 1, 1);
		}
	}
	
	public InputStream getInputStream() {
        try {
    		if(node.hasNode(STREAM_PATH)) {
    		    return new NodeInputStream(node.getNode(STREAM_PATH));
    		}
	    } catch(Exception e) {
	        throw new JobStorageException("Exception in getInputStream()", e);
	    }
		return null;
	}

	public OutputStream getOutputStream() {
        try {
            if(node.hasNode(STREAM_PATH)) {
                throw new IllegalArgumentException("Stream node already exists: " 
                        + node.getPath() + "/" + STREAM_PATH);
            }
            node.setProperty(RT_PROP, BackgroundServletConstants.JOB_RESOURCE_TYPE);
            final Node stream = node.addNode(STREAM_PATH);
            stream.setProperty(RT_PROP, BackgroundServletConstants.STREAM_RESOURCE_TYPE);
            node.save();
            return new NodeOutputStream(stream);
        } catch(Exception e) {
            throw new JobStorageException("Exception in getOutputStream()", e);
        }
	}

	public String getPath() {
		return path;
	}

    public String getProperty(String name) {
        String result = null;
        try {
            if(node.hasProperty(name)) {
                result = node.getProperty(name).getValue().getString();
            }
        } catch(RepositoryException re) {
            throw new JobStorageException("RepositoryException in getProperty", re);
        }
        return result;
    }

    public void setProperty(String name, String value) {
        try {
            node.setProperty(name, value);
            node.save();
        } catch(RepositoryException re) {
            throw new JobStorageException("RepositoryException in setProperty", re);
        }
    }

    public Date getCreationTime() {
        return creationTime.getTime();
    }
}
