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
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

class ContentHelper {
    public static final String NT_FOLDER = "nt:folder";
    public static final String NT_FILE = "nt:file";
    public static final String NT_RESOURCE = "nt:resource";
    public static final String JCR_CONTENT = "jcr:content";
    public static final String JCR_LASTMODIFIED = "jcr:lastModified";
    public static final String JCR_MIMETYPE = "jcr:mimeType";
    public static final String JCR_ENCODING = "jcr:encoding";
    public static final String JCR_DATA = "jcr:data";
    
    final String [] WATCHED_FOLDERS = {
        "libs/foo/bar/install",
        "libs/foo/wii/install",
        "apps/install"
    };

    final String [] IGNORED_FOLDERS = {
        "libs/foo/bar/installed",
        "apps/noninstall"
    };
    
    private final Session session;
    
    ContentHelper(Session s) {
    	session = s;
    }

    void cleanupContent() throws Exception {
    	final String [] paths = { "libs", "apps" }; 
    	for(String path : paths) {
            if(session.getRootNode().hasNode(path)) {
                session.getRootNode().getNode(path).remove();
            }
    	}
    }
    
    void setupContent() throws Exception {
    	cleanupContent();
        for(String folder : WATCHED_FOLDERS) {
            createFolder(folder);
        }
        for(String folder : IGNORED_FOLDERS) {
            createFolder(folder);
        }
    }
    
    void createFolder(String path) throws Exception {
        final String [] parts = path.split("/");
        Node n = session.getRootNode();
        for(String part : parts) {
            if(n.hasNode(part)) {
                n = n.getNode(part);
            } else {
                n = n.addNode(part);
            }
        }
        session.save();
    }
    
    void createOrUpdateFile(String path, InputStream data, long lastModified) throws RepositoryException {
        final String relPath = path.substring(1);
        Node f = null;
        Node res = null;
        if(session.getRootNode().hasNode(relPath)) {
            f = session.getRootNode().getNode(relPath);
            res = f.getNode(JCR_CONTENT);
        } else {
            f = session.getRootNode().addNode(relPath);
            res = f.addNode(JCR_CONTENT);
        }
        
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(lastModified);
        res.setProperty(JCR_LASTMODIFIED, c);
        res.setProperty(JCR_DATA, data);
        
        f.getParent().save();
    }
}