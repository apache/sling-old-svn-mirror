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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.bgservlets.JobData;
import org.apache.sling.bgservlets.impl.nodestream.NodeInputStream;
import org.apache.sling.bgservlets.impl.nodestream.NodeOutputStream;

class JobDataImpl implements JobData {

	private final Node node;
	private final String path;
	
	public static final String STREAM_PATH = "outputStream";
	
	/** Build a JobDataImpl on supplied node, which must exists */
	JobDataImpl(Node n) throws RepositoryException {
		node = n;
		path = n.getPath();
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
            final Node stream = node.addNode(STREAM_PATH);
            node.save();
            return new NodeOutputStream(stream);
        } catch(Exception e) {
            throw new JobStorageException("Exception in getOutputStream()", e);
        }
	}

	public String getPath() {
		return path;
	}
}
