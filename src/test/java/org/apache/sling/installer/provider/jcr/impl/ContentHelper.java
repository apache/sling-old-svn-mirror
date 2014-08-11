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
package org.apache.sling.installer.provider.jcr.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.commons.testing.jcr.RepositoryUtil;

/** Utility class used to create test content */
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
        "/libs/foo/bar/install",
        "/libs/foo/wii/install",
        "/apps/install"
    };

    final String [] IGNORED_FOLDERS = {
        "/libs/foo/bar/installed",
        "/apps/noninstall"
    };

    final String [] FAKE_RESOURCES = {
        "/libs/foo/bar/install/bundle1.jar",
        "/libs/foo/bar/install/cfg3.cfg",
        "/libs/foo/wii/install/bundle2.jar",
        "/libs/foo/wii/install/cfg1.properties",
        "/libs/foo/wii/install/cfg2.properties",
    };

    final String [] FAKE_CONFIGS = {
        "/libs/foo/bar/install/cfgA",
        "/libs/foo/wii/install/cfgB",
        "/libs/foo/wii/install/cfgC"
    };

    private final Session session;

    ContentHelper(Session s) throws RepositoryException, IOException {
    	session = s;

        final NamespaceRegistry r = session.getWorkspace().getNamespaceRegistry();
        try {
            r.registerNamespace("sling", "http://sling.apache.org/jcr/sling/1.0");
        } catch(RepositoryException ignore) {
            // don't fail if already registered
        }

        RepositoryUtil.registerNodeType(session,
                this.getClass().getResourceAsStream("/SLING-INF/nodetypes/osgiconfig.cnd"));
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
    	setupFolders();
        for(String path : FAKE_RESOURCES) {
            createOrUpdateFile(path);
        }
        for(String path : FAKE_CONFIGS) {
            createConfig(path, null);
        }
    }

    void setupFolders() throws Exception {
        for(String folder : WATCHED_FOLDERS) {
            createFolder(folder);
        }
        for(String folder : IGNORED_FOLDERS) {
            createFolder(folder);
        }
    }

    Node createFolder(String path) throws Exception {
        final String [] parts = relPath(path).split("/");
        Node n = session.getRootNode();
        for(String part : parts) {
            if(n.hasNode(part)) {
                n = n.getNode(part);
            } else {
                n = n.addNode(part);
            }
        }
        session.save();
        return n;
    }

    void delete(String path) throws RepositoryException {
        session.getItem(path).remove();
        session.save();
    }

    void deleteQuietly(String path) {
        try {
            delete(path);
        } catch (RepositoryException e) {}
    }

    void createOrUpdateFile(String path) throws RepositoryException {
        createOrUpdateFile(path, null, System.currentTimeMillis());
    }

    void createOrUpdateFile(String path, MockInstallableResource d) throws RepositoryException {
    	createOrUpdateFile(path, d.getInputStream(), System.currentTimeMillis());
    }

    void createOrUpdateFile(String path, InputStream data, long lastModified) throws RepositoryException {
    	if(data == null) {
            final String content = "Fake data for " + path;
            data = new ByteArrayInputStream(content.getBytes());
    	}

        final String relPath = relPath(path);
        Node f = null;
        Node res = null;
        if(session.getRootNode().hasNode(relPath)) {
            f = session.getRootNode().getNode(relPath);
            res = f.getNode(JCR_CONTENT);
        } else {
            f = session.getRootNode().addNode(relPath,NT_FILE);
            res = f.addNode(JCR_CONTENT,NT_RESOURCE);
        }

        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(lastModified);
        res.setProperty(JCR_LASTMODIFIED, c);
        res.setProperty(JCR_DATA, data);
        res.setProperty(JCR_MIMETYPE, "");

        session.save();
    }

    String relPath(String path) {
        if(path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

    void createConfig(String path, Map<String, String> data) throws RepositoryException {
        path = relPath(path);
        if( !session.getRootNode().hasNode(path)) {
            session.getRootNode().addNode(path, "sling:OsgiConfig");
            session.save();
        }
   }
}