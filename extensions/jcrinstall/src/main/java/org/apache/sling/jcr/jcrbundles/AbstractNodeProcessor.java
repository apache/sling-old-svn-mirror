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
package org.apache.sling.jcr.jcrbundles;

import java.io.InputStream;
import java.util.Calendar;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.apache.sling.jcr.jcrbundles.JcrBundlesConstants.STATUS_BASE_PATH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NodeProcessor that accepts nodes based on a regexp on
 * their name, but does nothing with them
 */
abstract class AbstractNodeProcessor implements NodeProcessor {

    /**
     * The relative path of the data and last modified date of an nt:file node
     */
    public static final String JCR_CONTENT = "jcr:content";
    public static final String JCR_CONTENT_DATA = JCR_CONTENT + "/jcr:data";
    public static final String JCR_LAST_MODIFIED = "jcr:lastModified";
    public static final String JCR_CONTENT_LAST_MODIFIED = JCR_CONTENT + "/" + JCR_LAST_MODIFIED;

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Pattern pattern;

    AbstractNodeProcessor(String regexp) {
        pattern = Pattern.compile(regexp);
    }

    public boolean accepts(Node n) throws RepositoryException {
        boolean result = pattern.matcher(n.getName()).matches();
        if (result) {
            log.debug("Node {} accepted by {}", n.getPath(), getClass().getName());
        }
        return result;
    }

    static InputStream getInputStream(Node fileNode) throws RepositoryException {
        Property prop = fileNode.getProperty(JCR_CONTENT_DATA);
        return prop.getStream();
    }

    /**
     * Return the Node to use to store status of given fileNode.
     * Session is not saved if status node is created
     */
    protected Node getStatusNode(Node fileNode, boolean createIfNotFound) throws RepositoryException {
        final String path = STATUS_BASE_PATH + fileNode.getPath();
        final Session s = fileNode.getSession();
        Node result = null;

        if (s.itemExists(path)) {
            result = (Node) s.getItem(path);
        } else if (createIfNotFound) {
            result = deepCreate(s, path);
        }

        if (result != null) {
            log.debug("Status node for {} is at {}", fileNode.getPath(), result.getPath());
        } else {
            log.debug("No status node found for {}", fileNode.getPath());
        }

        return result;
    }

    /**
     * Find the path of the node in the main tree that corresponds to the given status node path
     */
    protected String getMainNodePath(String statusNodePath) {
        return statusNodePath.substring(STATUS_BASE_PATH.length());
    }

    /**
     * Find the node in the main tree that corresponds to the given status node
     */
    protected Node getMainNode(Node statusNode) throws RepositoryException {
        final String path = getMainNodePath(statusNode.getPath());
        final Session s = statusNode.getSession();
        Node result = null;

        if (s.itemExists(path)) {
            result = (Node) s.getItem(path);
        }

        return result;
    }

    protected Calendar getLastModified(Node fileNode) throws RepositoryException {
        if (fileNode.hasProperty(JCR_CONTENT_LAST_MODIFIED)) {
            return fileNode.getProperty(JCR_CONTENT_LAST_MODIFIED).getDate();
        }
        return null;
    }

    protected Node deepCreate(Session s, String path) throws RepositoryException {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        final String[] names = path.split("/");
        String currentPath = "";

        for (int i = 0; i < names.length; i++) {
            currentPath += "/" + names[i];
            if (!s.itemExists(currentPath)) {
                s.getRootNode().addNode(currentPath.substring(1));
			}
		}

		return (Node)s.getItem("/" + path);
	}
}
