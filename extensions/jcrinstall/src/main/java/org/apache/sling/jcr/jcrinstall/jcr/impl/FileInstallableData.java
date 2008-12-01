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
package org.apache.sling.jcr.jcrinstall.jcr.impl;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.sling.jcr.jcrinstall.osgi.InstallableData;

/** Provides data (InputStream, last modified data) about
 * 	JCR nodes which are files.
 */
class FileInstallableData implements InstallableData {
    /**
     * The relative path of the data and last modified date of an nt:file node
     */
    public static final String JCR_CONTENT = "jcr:content";
    public static final String JCR_CONTENT_DATA = JCR_CONTENT + "/jcr:data";
    public static final String JCR_LAST_MODIFIED = "jcr:lastModified";
    public static final String JCR_CONTENT_LAST_MODIFIED = JCR_CONTENT + "/" + JCR_LAST_MODIFIED;
    
    private final Property dataProperty;
    private final String digest;
    private final String path;
    
    @Override
    public String toString() {
    	return getClass().getSimpleName() + ": " + path;
    }
    
	FileInstallableData(Node n) throws RepositoryException {
		this.path = n.getPath();
        if (n.hasProperty(JCR_CONTENT_LAST_MODIFIED)) {
        	digest = String.valueOf(n.getProperty(JCR_CONTENT_LAST_MODIFIED).getDate().getTimeInMillis());
        } else {
        	digest = null;
	    }
	    
        if(n.hasProperty(JCR_CONTENT_DATA)) {
        	dataProperty = n.getProperty(JCR_CONTENT_DATA);
        } else {
        	dataProperty = null;
        }
	}
	
    @SuppressWarnings("unchecked")
	public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
		if(type.equals(InputStream.class)) {
			if (dataProperty != null) {
			    try {
			        return (AdapterType) dataProperty.getStream();
			    } catch (RepositoryException re) {
			        // might log
			    }
			}
		}
		return null;
	}

	public String getDigest() {
		return digest;
	}
}