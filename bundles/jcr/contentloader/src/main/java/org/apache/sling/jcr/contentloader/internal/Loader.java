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
package org.apache.sling.jcr.contentloader.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;

/**
 * The <code>Loader</code> loads initial content from the bundle.
 */
public class Loader extends BaseImportLoader {

    public static final String PARENT_DESCRIPTOR = "ROOT";

    private final Logger log = LoggerFactory.getLogger(Loader.class);

    private ContentLoaderService contentLoaderService;

    // bundles whose registration failed and should be retried
    private List<Bundle> delayedBundles;

    public Loader(ContentLoaderService contentLoaderService) {
        super();
        this.contentLoaderService = contentLoaderService;
        this.delayedBundles = new LinkedList<Bundle>();
    }

    public void dispose() {
        if (delayedBundles != null) {
            delayedBundles.clear();
            delayedBundles = null;
        }
        contentLoaderService = null;
        super.dispose();
    }

    /**
     * Register a bundle and install its content.
     *
     * @param metadataSession
     * @param bundle
     * @throws RepositoryException
     */
    public void registerBundle(final Session metadataSession, final Bundle bundle, final boolean isUpdate) throws RepositoryException {

        // if this is an update, we have to uninstall the old content first
        if (isUpdate) {
            this.unregisterBundle(metadataSession, bundle);
        }

        log.debug("Registering bundle {} for content loading.", bundle.getSymbolicName());

        if (registerBundleInternal(metadataSession, bundle, false, isUpdate)) {
            // handle delayed bundles, might help now
            int currentSize = -1;
            for (int i = delayedBundles.size(); i > 0 && currentSize != delayedBundles.size() && !delayedBundles.isEmpty(); i--) {
                for (Iterator<Bundle> di = delayedBundles.iterator(); di.hasNext(); ) {
                    Bundle delayed = di.next();
                    if (registerBundleInternal(metadataSession, delayed, true, false)) {
                        di.remove();
                    }
                }
                currentSize = delayedBundles.size();
            }
        } else if (!isUpdate) {
            // add to delayed bundles - if this is not an update!
            delayedBundles.add(bundle);
        }
    }

    private boolean registerBundleInternal(final Session metadataSession, final Bundle bundle, final boolean isRetry, final boolean isUpdate) {

        // check if bundle has initial content
        final Iterator<PathEntry> pathIter = PathEntry.getContentPaths(bundle);
        if (pathIter == null) {
            log.debug("Bundle {} has no initial content", bundle.getSymbolicName());
            return true;
        }

        try {
            contentLoaderService.createRepositoryPath(metadataSession, ContentLoaderService.BUNDLE_CONTENT_NODE);

            // check if the content has already been loaded
            final Map<String, Object> bundleContentInfo = contentLoaderService.getBundleContentInfo(metadataSession, bundle, true);

            // if we don't get an info, someone else is currently loading
            if (bundleContentInfo == null) {
                return false;
            }

            boolean success = false;
            List<String> createdNodes = null;
            try {
                final boolean contentAlreadyLoaded = ((Boolean) bundleContentInfo.get(ContentLoaderService.PROPERTY_CONTENT_LOADED)).booleanValue();
                boolean isBundleUpdated = false;
                Calendar lastLoadedAt = (Calendar) bundleContentInfo.get(ContentLoaderService.PROPERTY_CONTENT_LOADED_AT);
                if (lastLoadedAt != null) {
                    // this assumes that the bundle has been installed or updated after the content has been loaded
                    if (lastLoadedAt.getTimeInMillis() < bundle.getLastModified()) {
                        isBundleUpdated = true;
                    }
                }
                if (!isUpdate && !isBundleUpdated && contentAlreadyLoaded) {
                    log.info("Content of bundle already loaded {}.", bundle.getSymbolicName());
                } else {
                    createdNodes = installContent(metadataSession, bundle, pathIter, contentAlreadyLoaded);
                    if (isRetry) {
                        // log success of retry
                        log.info("Retrying to load initial content for bundle {} succeeded.", bundle.getSymbolicName());
                    }
                }

                success = true;
                return true;
            } finally {
                contentLoaderService.unlockBundleContentInfo(metadataSession, bundle, success, createdNodes);
            }

        } catch (RepositoryException re) {
            // if we are retrying we already logged this message once, so we
            // won't log it again
            if (!isRetry) {
                log.error("Cannot load initial content for bundle " + bundle.getSymbolicName() + " : " + re.getMessage(), re);
            }
        }
        return false;
    }

    /**
     * Unregister a bundle. Remove installed content.
     *
     * @param bundle The bundle.
     */
    public void unregisterBundle(final Session session, final Bundle bundle) {

        if (delayedBundles.contains(bundle)) {
            delayedBundles.remove(bundle);
        } else {
            try {
                contentLoaderService.createRepositoryPath(session, ContentLoaderService.BUNDLE_CONTENT_NODE);

                final Map<String, Object> bundleContentInfo = contentLoaderService.getBundleContentInfo(session, bundle, false);

                // if we don't get an info, someone else is currently loading or unloading
                // or the bundle is already uninstalled
                if (bundleContentInfo == null) {
                    return;
                }

                try {
                    uninstallContent(session, bundle, (String[]) bundleContentInfo.get(ContentLoaderService.PROPERTY_UNINSTALL_PATHS));
                    contentLoaderService.contentIsUninstalled(session, bundle);
                } finally {
                    contentLoaderService.unlockBundleContentInfo(session, bundle, false, null);
                }
            } catch (RepositoryException re) {
                log.error("Cannot remove initial content for bundle " + bundle.getSymbolicName() + " : " + re.getMessage(), re);
            }
        }
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Install the content from the bundle.
     *
     * @return If the content should be removed on uninstall, a list of top nodes
     */
    private List<String> installContent(final Session defaultSession, final Bundle bundle, final Iterator<PathEntry> pathIter, final boolean contentAlreadyLoaded) throws RepositoryException {

        final List<String> createdNodes = new ArrayList<String>();
        final Map<String, Session> createdSessions = new HashMap<String, Session>();

        log.debug("Installing initial content from bundle {}", bundle.getSymbolicName());
        final DefaultContentCreator contentCreator = new DefaultContentCreator(this.contentLoaderService);
        try {
            while (pathIter.hasNext()) {
                final PathEntry pathEntry = pathIter.next();
                if (!contentAlreadyLoaded || pathEntry.isOverwrite()) {
                    String workspace = pathEntry.getWorkspace();
                    final Session targetSession;
                    if (workspace != null) {
                        if (createdSessions.containsKey(workspace)) {
                            targetSession = createdSessions.get(workspace);
                        } else {
                            targetSession = createSession(workspace);
                            createdSessions.put(workspace, targetSession);
                        }
                    } else {
                        targetSession = defaultSession;
                    }

                    final Node targetNode = getTargetNode(targetSession, pathEntry.getTarget());

                    if (targetNode != null) {
                        installFromPath(bundle, pathEntry.getPath(), pathEntry, targetNode, pathEntry.isUninstall() ? createdNodes : null, contentCreator);
                    }
                }
            }

            // now optimize created nodes list
            Collections.sort(createdNodes);
            if (createdNodes.size() > 1) {
                final Iterator<String> i = createdNodes.iterator();
                String previous = i.next() + '/';
                while (i.hasNext()) {
                    final String current = i.next();
                    if (current.startsWith(previous)) {
                        i.remove();
                    } else {
                        previous = current + '/';
                    }
                }
            }

            // persist modifications now
            defaultSession.refresh(true);
            defaultSession.save();

            for (Session session : createdSessions.values()) {
                session.refresh(true);
                session.save();
            }

            // finally check in versionable nodes
            for (final Node versionable : contentCreator.getVersionables()) {
                versionable.checkin();
            }
        } finally {
            try {
                if (defaultSession.hasPendingChanges()) {
                    defaultSession.refresh(false);
                }
                for (Session session : createdSessions.values()) {
                    if (session.hasPendingChanges()) {
                        session.refresh(false);
                    }
                }
            } catch (RepositoryException re) {
                log.warn("Failure to rollback partial initial content for bundle {}", bundle.getSymbolicName(), re);
            }
            contentCreator.clear();
            for (Session session : createdSessions.values()) {
                session.logout();
            }
        }
        log.debug("Done installing initial content from bundle {}", bundle.getSymbolicName());

        return createdNodes;
    }

    /**
     * Handle content installation for a single path.
     *
     * @param bundle        The bundle containing the content.
     * @param path          The path
     * @param configuration
     * @param parent        The parent node.
     * @param createdNodes  An optional list to store all new nodes. This list is used for an uninstall
     * @throws RepositoryException
     */
    private void installFromPath(final Bundle bundle, final String path, final PathEntry configuration, final Node parent, final List<String> createdNodes, final DefaultContentCreator contentCreator) throws RepositoryException {

        //  init content creator
        contentCreator.init(configuration, this.defaultImportProviders, createdNodes, null);

        final Map<String, Node> processedEntries = new HashMap<String, Node>();

        @SuppressWarnings("unchecked")
        Enumeration<String> entries = bundle.getEntryPaths(path);
        if (entries == null) {
            // check for single content
            final URL u = bundle.getEntry(path);
            if (u == null) {
                log.info("install: No initial content entries at {} in bundle {}", path, bundle.getSymbolicName());
                return;
            }
            // we have a single file content, let's check if this has an import provider extension
            for (String ext : contentCreator.getImportProviders().keySet()) {
                if (path.endsWith(ext)) {

                }
            }
            handleFile(path, bundle, processedEntries, configuration, parent, createdNodes, contentCreator);
            return;
        }

        // potential parent node import/extension
        URL parentNodeDescriptor = importParentNode(bundle, path, parent, contentCreator);
        if (parentNodeDescriptor != null) {
            processedEntries.put(parentNodeDescriptor.toString(), parent);
        }

        while (entries.hasMoreElements()) {
            final String entry = entries.nextElement();
            log.debug("Processing initial content entry {} in bundle {}", entry, bundle.getSymbolicName());
            if (entry.endsWith("/")) {

                // dir, check for node descriptor, else create dir
                final String base = entry.substring(0, entry.length() - 1);

                URL nodeDescriptor = null;
                for (String ext : contentCreator.getImportProviders().keySet()) {
                    nodeDescriptor = bundle.getEntry(base + ext);
                    if (nodeDescriptor != null) {
                        break;
                    }
                }

                // if we have a descriptor, which has not been processed yet,
                // otherwise call createFolder, which creates an nt:folder or
                // returns an existing node (created by a descriptor)
                final String name = getName(base);
                Node node = null;
                if (nodeDescriptor != null) {
                    node = processedEntries.get(nodeDescriptor.toString());
                    if (node == null) {
                        node = createNode(parent, name, nodeDescriptor, contentCreator);
                        processedEntries.put(nodeDescriptor.toString(), node);
                    }
                } else {
                    node = createFolder(parent, name, configuration.isOverwrite());
                }

                // walk down the line
                if (node != null) {
                    installFromPath(bundle, entry, configuration, node, createdNodes, contentCreator);
                }

            } else {
                // file => create file
                handleFile(entry, bundle, processedEntries, configuration, parent, createdNodes, contentCreator);
            }
        }
    }

    /**
     * Handle a file entry.
     *
     * @param entry
     * @param bundle
     * @param processedEntries
     * @param configuration
     * @param parent
     * @param createdNodes
     * @throws RepositoryException
     */
    private void handleFile(final String entry, final Bundle bundle, final Map<String, Node> processedEntries, final PathEntry configuration, final Node parent, final List<String> createdNodes, final DefaultContentCreator contentCreator) throws RepositoryException {

        final URL file = bundle.getEntry(entry);
        final String name = getName(entry);
        try {
            if (processedEntries.containsKey(file.toString())) {
                // this is a consumed node descriptor
                return;
            }

            // check for node descriptor
            URL nodeDescriptor = null;
            for (String ext : contentCreator.getImportProviders().keySet()) {
                nodeDescriptor = bundle.getEntry(entry + ext);
                if (nodeDescriptor != null) {
                    break;
                }
            }

            // install if it is a descriptor
            boolean foundProvider = contentCreator.getImportProvider(entry) != null;

            Node node = null;
            if (foundProvider) {
                node = createNode(parent, name, file, contentCreator);
                if (node != null) {
                    log.debug("Created node as {} {}", node.getPath(), name);
                    processedEntries.put(file.toString(), node);
                } else {
                    log.warn("No node created for file {} {}", file, name);
                }
            } else {
                log.debug("Can't find provider for entry {} at {}", entry, name);
            }

            // otherwise just place as file
            if (node == null) {
                try {
                    createFile(configuration, parent, file, createdNodes, contentCreator);
                    node = parent.getNode(name);
                } catch (IOException ioe) {
                    log.warn("Cannot create file node for {}", file, ioe);
                }
            }
            // if we have a descriptor, which has not been processed yet,
            // process it
            if (nodeDescriptor != null && processedEntries.containsKey(nodeDescriptor.toString())) {
                try {
                    contentCreator.setIgnoreOverwriteFlag(true);
                    node = createNode(parent, name, nodeDescriptor, contentCreator);
                    processedEntries.put(nodeDescriptor.toString(), node);
                } finally {
                    contentCreator.setIgnoreOverwriteFlag(false);
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to process file {} from {}", file, name);
            throw e;
        }
    }

    /**
     * Create a new node from a content resource found in the bundle.
     *
     * @param parent         The parent node
     * @param name           The name of the new content node
     * @param resourceUrl    The resource url.
     * @param contentCreator
     * @return
     * @throws RepositoryException
     */
    private Node createNode(Node parent, String name, URL resourceUrl, final DefaultContentCreator contentCreator) throws RepositoryException {

        final String resourcePath = resourceUrl.getPath().toLowerCase();
        try {
            // special treatment for system view imports
            if (resourcePath.endsWith(EXT_JCR_XML)) {
                return importSystemView(parent, name, resourceUrl);
            }

            // get the node reader for this resource
            final ImportProvider ip = contentCreator.getImportProvider(resourcePath);
            if (ip == null) {
                return null;
            }
            final ContentReader nodeReader = ip.getReader();

            // cannot find out the type
            if (nodeReader == null) {
                return null;
            }

            contentCreator.prepareParsing(parent, toPlainName(name, contentCreator));
            nodeReader.parse(resourceUrl, contentCreator);

            return contentCreator.getCreatedRootNode();
        } catch (RepositoryException re) {
            throw re;
        } catch (Throwable t) {
            throw new RepositoryException(t.getMessage(), t);
        }
    }

    /**
     * Create a folder
     *
     * @param parent    The parent node.
     * @param name      The name of the folder
     * @param overwrite If set to true, an existing folder is removed first.
     * @return The node pointing to the folder.
     * @throws RepositoryException
     */
    private Node createFolder(Node parent, String name, final boolean overwrite) throws RepositoryException {

        if (parent.hasNode(name)) {
            if (overwrite) {
                parent.getNode(name).remove();
            } else {
                return parent.getNode(name);
            }
        }

        return parent.addNode(name, "sling:Folder");
    }

    /**
     * Create a file from the given url.
     *
     * @param configuration
     * @param parent
     * @param source
     * @param createdNodes
     * @param contentCreator
     * @throws IOException
     * @throws RepositoryException
     */
    private void createFile(PathEntry configuration, Node parent, URL source, List<String> createdNodes, final DefaultContentCreator contentCreator) throws IOException, RepositoryException {

        final String srcPath = source.getPath();
        int pos = srcPath.lastIndexOf("/");
        final String name = getName(source.getPath());
        final String path;
        if (pos == -1) {
            path = name;
        } else {
            path = srcPath.substring(0, pos + 1) + name;
        }

        contentCreator.init(configuration, defaultImportProviders, createdNodes, null);
        contentCreator.prepareParsing(parent, name);
        final URLConnection conn = source.openConnection();
        final long lastModified = Math.min(conn.getLastModified(), configuration.getLastModified());
        final String type = conn.getContentType();
        final InputStream data = conn.getInputStream();
        contentCreator.createFileAndResourceNode(path, data, type, lastModified);
        contentCreator.finishNode();
        contentCreator.finishNode();
    }

    /**
     * Gets and decodes the name part of the <code>path</code>. The name is
     * the part of the path after the last slash (or the complete path if no
     * slash is contained). To support names containing unsupported characters
     * such as colon (<code>:</code>), names may be URL encoded (see
     * <code>java.net.URLEncoder</code>) using the <i>UTF-8</i> character
     * encoding. In this case, this method decodes the name using the
     * <code>java.net.URLDecoder</code> class with the <i>UTF-8</i> character
     * encoding.
     *
     * @param path The path from which to extract the name part.
     * @return The URL decoded name part.
     */
    private String getName(String path) {

        int lastSlash = path.lastIndexOf('/');
        String name = (lastSlash < 0) ? path : path.substring(lastSlash + 1);

        // check for encoded characters (%xx)
        // has encoded characters, need to decode
        if (name.indexOf('%') >= 0) {
            try {
                return URLDecoder.decode(name, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                // actually unexpected because UTF-8 is required by the spec
                log.error("Cannot decode " + name + " because the platform has no support for UTF-8, using undecoded");
            } catch (Exception e) {
                // IllegalArgumentException or failure to decode
                log.error("Cannot decode " + name + ", using undecoded", e);
            }
        }

        // not encoded or problems decoding, return the name unmodified
        return name;
    }

    private Node getTargetNode(Session session, String path) throws RepositoryException {

        // not specified path directive
        if (path == null) {
            return session.getRootNode();
        }

        if (!path.startsWith("/")) {
            // make relative path absolute
            path = "/" + path;
        }

        if (!session.itemExists(path)) {
            Node currentNode = session.getRootNode();
            final StringTokenizer st = new StringTokenizer(path.substring(1), "/");
            while (st.hasMoreTokens()) {
                final String name = st.nextToken();
                if (!currentNode.hasNode(name)) {
                    currentNode.addNode(name, "sling:Folder");
                }
                currentNode = currentNode.getNode(name);
            }
            return currentNode;
        }
        Item item = session.getItem(path);
        return (item.isNode()) ? (Node) item : null;
    }

    private void uninstallContent(final Session defaultSession, final Bundle bundle, final String[] uninstallPaths) {

        final Map<String, Session> createdSessions = new HashMap<String, Session>();

        try {
            log.debug("Uninstalling initial content from bundle {}", bundle.getSymbolicName());
            if (uninstallPaths != null && uninstallPaths.length > 0) {
                for (String path : uninstallPaths) {
                    final Session targetSession;

                    final int wsSepPos = path.indexOf(":/");
                    if (wsSepPos != -1) {
                        final String workspaceName = path.substring(0, wsSepPos);
                        path = path.substring(wsSepPos + 1);
                        if (workspaceName.equals(defaultSession.getWorkspace().getName())) {
                            targetSession = defaultSession;
                        } else if (createdSessions.containsKey(workspaceName)) {
                            targetSession = createdSessions.get(workspaceName);
                        } else {
                            targetSession = createSession(workspaceName);
                            createdSessions.put(workspaceName, targetSession);
                        }
                    } else {
                        targetSession = defaultSession;
                    }

                    if (targetSession.itemExists(path)) {
                        targetSession.getItem(path).remove();
                    }
                }

                // persist modifications now
                defaultSession.save();

                for (Session session : createdSessions.values()) {
                    session.save();
                }
            }
            log.debug("Done uninstalling initial content from bundle {}", bundle.getSymbolicName());
        } catch (RepositoryException re) {
            log.error("Unable to uninstall initial content from bundle " + bundle.getSymbolicName(), re);
        } finally {
            try {
                if (defaultSession.hasPendingChanges()) {
                    defaultSession.refresh(false);
                }
                for (Session session : createdSessions.values()) {
                    if (session.hasPendingChanges()) {
                        session.refresh(false);
                    }
                }
            } catch (RepositoryException re) {
                log.warn("Failure to rollback uninstalling initial content for bundle {}", bundle.getSymbolicName(), re);
            }

            for (Session session : createdSessions.values()) {
                session.logout();
            }
        }
    }

    /**
     * Import the XML file as JCR system or document view import. If the XML
     * file is not a valid system or document view export/import file,
     * <code>false</code> is returned.
     *
     * @param parent  The parent node below which to import
     * @param nodeXML The URL to the XML file to import
     * @return <code>true</code> if the import succeeds, <code>false</code>
     *         if the import fails due to XML format errors.
     * @throws IOException If an IO error occurs reading the XML file.
     */
    private Node importSystemView(Node parent, String name, URL nodeXML) throws IOException {

        InputStream ins = null;
        try {
            // check whether we have the content already, nothing to do then
            if (name.endsWith(EXT_JCR_XML)) {
                name = name.substring(0, name.length() - EXT_JCR_XML.length());
            }
            if (parent.hasNode(name)) {
                log.debug("importSystemView: Node {} for XML {} already exists, nothing to do", name, nodeXML);
                return parent.getNode(name);
            }

            ins = nodeXML.openStream();
            Session session = parent.getSession();
            session.importXML(parent.getPath(), ins, IMPORT_UUID_CREATE_NEW);

            // additionally check whether the expected child node exists
            return (parent.hasNode(name)) ? parent.getNode(name) : null;
        } catch (InvalidSerializedDataException isde) {
            // the XML might not be system or document view export, fall back
            // to old-style XML reading
            log.info("importSystemView: XML {} does not seem to be system view export, trying old style; cause: {}", nodeXML, isde.toString());
            return null;
        } catch (RepositoryException re) {
            // any other repository related issue...
            log.info("importSystemView: Repository issue loading XML {}, trying old style; cause: {}", nodeXML, re.toString());
            return null;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    protected static final class Descriptor {

        public URL url;

        public ContentReader contentReader;

    }

    /**
     * Return the parent node descriptor (ROOT).
     */
    private Descriptor getParentNodeDescriptor(final Bundle bundle, final String path, final DefaultContentCreator contentCreator) {

        for (Map.Entry<String, ImportProvider> entry : contentCreator.getImportProviders().entrySet()) {
            if (entry.getValue() != null) {
                final StringBuilder filePath = new StringBuilder(path);
                if (!path.endsWith("/")) {
                    filePath.append("/");
                }
                filePath.append(PARENT_DESCRIPTOR);
                // add file extension, e.g. .jcr.xml, .xml, .zip (see BaseImportLoader)
                filePath.append(entry.getKey());
                URL url = bundle.getEntry(filePath.toString());
                if (url != null) {
                    try {
                        final Descriptor descriptor = new Descriptor();
                        descriptor.url = url;
                        descriptor.contentReader = entry.getValue().getReader();
                        return descriptor;
                    } catch (IOException ioe) {
                        log.error("Unable to setup node reader for " + entry.getKey(), ioe);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Imports mixin nodes and properties (and optionally child nodes) of the
     * parent node.
     */
    private URL importParentNode(Bundle bundle, String path, Node parent, final DefaultContentCreator contentCreator) throws RepositoryException {

        final Descriptor descriptor = getParentNodeDescriptor(bundle, path, contentCreator);
        // no parent descriptor (ROOT) found
        if (descriptor == null) {
            return null;
        }

        try {
            contentCreator.prepareParsing(parent, null);
            descriptor.contentReader.parse(descriptor.url, contentCreator);
            return descriptor.url;
        } catch (RepositoryException re) {
            throw re;
        } catch (Throwable t) {
            throw new RepositoryException(t.getMessage(), t);
        }
    }

    private String toPlainName(final String name, final DefaultContentCreator contentCreator) {

        final String providerExt = contentCreator.getImportProviderExtension(name);
        if (providerExt != null) {
            return name.substring(0, name.length() - providerExt.length());
        }
        return name;
    }

    private Session createSession(String workspace) throws RepositoryException {
        try {
            return contentLoaderService.getRepository().loginAdministrative(workspace);
        } catch (NoSuchWorkspaceException e) {
            Session temp = contentLoaderService.getRepository().loginAdministrative(null);
            temp.getWorkspace().createWorkspace(workspace);
            temp.logout();
            return contentLoaderService.getRepository().loginAdministrative(workspace);
        }
    }

}
