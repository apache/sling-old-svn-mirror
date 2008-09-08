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
package org.apache.sling.jcr.jcrinstall;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.jcr.api.SlingRepository;
import static org.apache.sling.jcr.jcrinstall.JcrBundlesConstants.BUNDLES_NODENAME;
import static org.apache.sling.jcr.jcrinstall.JcrBundlesConstants.STATUS_BASE_PATH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a folder that contains bundles and configurations: listens to
 * changes in the folder, and rescan it when needed, passing nodes to
 * NodeProcessors that want to process them.
 */
class BundlesFolder implements EventListener {
    protected static final Logger log = LoggerFactory.getLogger(BundlesFolder.class);

    private final String path;
    private final Session session;
    private long nextScan;

    /**
     * After receiving JCR events, we wait for this many msec before
     * re-scanning the folder, as events often come in bursts.
     */
    public static final long SCAN_DELAY_MSEC = 1000;

    /**
     * List of processors for our bundles and configs
     */
    private final List<NodeProcessor> processors;

    /**
     * Create a BundlesFolder on the given Repository, at the
     * given path
     */
    BundlesFolder(SlingRepository r, String path, List<NodeProcessor> processors) throws RepositoryException {
        this.path = path;
        this.processors = processors;
        session = r.loginAdministrative(r.getDefaultWorkspace());

        // observe any changes in our folder, recursively
        final int eventTypes = Event.NODE_ADDED | Event.NODE_REMOVED
                | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
        final boolean isDeep = true;
        final boolean noLocal = true;
        session.getWorkspace().getObservationManager().addEventListener(this, eventTypes, path,
                isDeep, null, null, noLocal);

        // trigger the initial scan without waiting
        setScanTimer(0);

        log.info("{} created for path {}", getClass().getSimpleName(), path);
    }

    /**
     * MUST be called when done using this object
     */
    void cleanup() throws RepositoryException {
        session.getWorkspace().getObservationManager().removeEventListener(this);
        session.logout();
    }

    /**
     * Any event causes us to rescan our folder, once there are no more
     * events during SCAN_DELAY_MSEC
     */
    public void onEvent(EventIterator it) {
        setScanTimer(SCAN_DELAY_MSEC);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BundlesFolder)) {
            return false;
        }

        final BundlesFolder other = (BundlesFolder) obj;
        return path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    /**
     * Find all bundles folders under rootPath
     */
    static Set<BundlesFolder> findBundlesFolders(SlingRepository r, String rootPath,
                                                 List<NodeProcessor> processors)
            throws RepositoryException {
        final Set<BundlesFolder> result = new HashSet<BundlesFolder>();
        Session s = null;

        try {
            s = r.loginAdministrative(r.getDefaultWorkspace());
            if (!s.getRootNode().hasNode(relPath(rootPath))) {
                log.info("Bundles root node {} not found, ignored", rootPath);
            } else {
                log.debug("Bundles root node {} found, looking for bundle folders inside it", rootPath);
                final Node n = s.getRootNode().getNode(relPath(rootPath));
                findBundlesFolders(r, n, result, processors);
            }
        } finally {
            if (s != null) {
                s.logout();
            }
        }

        return result;
    }

    /**
     * Add n to setToUpdate if it is a bundle folder, and recurse into its children
     * to do the same.
     */
    static void findBundlesFolders(SlingRepository r, Node n,
                                   Set<BundlesFolder> setToUpdate, List<NodeProcessor> processors)
            throws RepositoryException {
        if (n.getName().equals(BUNDLES_NODENAME)) {
            setToUpdate.add(new BundlesFolder(r, n.getPath(), processors));
        }
        final NodeIterator it = n.getNodes();
        while (it.hasNext()) {
            findBundlesFolders(r, it.nextNode(), setToUpdate, processors);
        }

    }

    /**
     * Set or reset our scanning timer: scanIfNeeded() will do nothing
     * unless this timer has expired
     */
    protected void setScanTimer(long delayMsec) {
        nextScan = System.currentTimeMillis() + delayMsec;
    }

    /**
     * If our timer allows it, recursively call processNode
     * on our Node and its children
     */
    void scanIfNeeded(Map<String, Boolean> flags) throws Exception {
        if (nextScan != -1 && System.currentTimeMillis() > nextScan) {
            nextScan = -1;
            log.debug("Timer expired, scanning {}", path);
            checkDeletions(flags);
            checkUpdatesAndDeletes(flags);
        }
    }

    /**
     * Let our processors handle all nodes under the main tree
     */
    void checkUpdatesAndDeletes(Map<String, Boolean> flags) throws Exception {
        if (session.getRootNode().hasNode(relPath(path))) {
            final Node n = session.getRootNode().getNode(relPath(path));
            processNode(n, flags, false);
        } else {
            log.info("Bundles folder {} does not exist anymore", path);
        }
    }

    /**
     * Check for nodes in the status tree that have disappeared from their main
     * locations, and let the processors handle these deletes.
     */
    void checkDeletions(Map<String, Boolean> flags) throws Exception {
        final String statusPath = STATUS_BASE_PATH + path;
        if (session.getRootNode().hasNode(relPath(statusPath))) {
            final Node n = session.getRootNode().getNode(relPath(statusPath));
            processNode(n, flags, true);
        } else {
            log.info("Status folder {} does not exist, checkDeletions does nothing", statusPath);
        }
    }

    /**
     * Let the first NodeProcessor that accepts n process it (for normal processing
     * or deletions), and recurse into n's children to do the same
     */
    protected void processNode(Node n, Map<String, Boolean> flags, boolean checkDeletions) throws Exception {

        boolean accepted = false;
        final String path = n.getPath();
        final Session s = n.getSession();

        for (NodeProcessor p : processors) {
            if (p.accepts(n)) {
                accepted = true;
                if (checkDeletions) {
                    p.checkDeletions(n, flags);
                } else {
                    p.process(n, flags);
                }
                break;
            }
        }

        if (!accepted) {
            log.debug("No NodeProcessor found for node {}, ignored", n.getPath());
        }

        // n might have been deleted above, if it's a status done
        if (s.itemExists(path)) {
            final NodeIterator it = n.getNodes();
            while (it.hasNext()) {
                processNode(it.nextNode(), flags, checkDeletions);
            }
        }
    }

    /**
     * Return the relative path for supplied path
     */
    static String relPath(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

}