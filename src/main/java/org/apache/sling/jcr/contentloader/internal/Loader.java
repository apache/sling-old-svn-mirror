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

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>Loader</code> loads initial content from the bundle.
 */
public class Loader {

    public static final String EXT_XML = ".xml";

    public static final String EXT_JCR_XML = ".jcr.xml";

    public static final String EXT_JSON = ".json";

    public static final String ROOT_DESCRIPTOR = "/ROOT";

    // default content type for createFile()
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(Loader.class);

    private ContentLoaderService jcrContentHelper;

    private Map<String, ImportProvider> importProviders;

    private Map<String, List<String>> delayedReferences;

    // bundles whose registration failed and should be retried
    private List<Bundle> delayedBundles;

    public Loader(ContentLoaderService jcrContentHelper) {
        this.jcrContentHelper = jcrContentHelper;
        this.delayedReferences = new HashMap<String, List<String>>();
        this.delayedBundles = new LinkedList<Bundle>();

        importProviders = new LinkedHashMap<String, ImportProvider>();
        importProviders.put(EXT_JCR_XML, null);
        importProviders.put(EXT_JSON, JsonReader.PROVIDER);
        importProviders.put(EXT_XML, XmlReader.PROVIDER);
    }

    public void dispose() {
        delayedReferences = null;
        if (delayedBundles != null) {
            delayedBundles.clear();
            delayedBundles = null;
        }
        jcrContentHelper = null;
        importProviders.clear();
    }

    /**
     * Register a bundle and install its content.
     * 
     * @param session
     * @param bundle
     */
    public void registerBundle(final Session session, final Bundle bundle,
            final boolean isUpdate) {

        log.debug("Registering bundle {} for content loading.",
            bundle.getSymbolicName());

        if (registerBundleInternal(session, bundle, false, isUpdate)) {

            // handle delayed bundles, might help now
            int currentSize = -1;
            for (int i = delayedBundles.size(); i > 0
                && currentSize != delayedBundles.size()
                && !delayedBundles.isEmpty(); i--) {

                for (Iterator<Bundle> di = delayedBundles.iterator(); di.hasNext();) {

                    Bundle delayed = di.next();
                    if (registerBundleInternal(session, delayed, true, false)) {
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

    private boolean registerBundleInternal(final Session session,
            final Bundle bundle, final boolean isRetry, final boolean isUpdate) {

        // check if bundle has initial content
        final Iterator<PathEntry> pathIter = PathEntry.getContentPaths(bundle);
        if (pathIter == null) {
            log.debug("Bundle {} has no initial content",
                bundle.getSymbolicName());
            return true;
        }

        try {
            
            // check if the content has already been loaded
            final Map<String, Object> bundleContentInfo = jcrContentHelper.getBundleContentInfo(
                session, bundle);
            
            // if we don't get an info, someone else is currently loading
            if (bundleContentInfo == null) {
                return false;
            }

            boolean success = false;
            try {
                
                final boolean contentAlreadyLoaded = ((Boolean) bundleContentInfo.get(ContentLoaderService.PROPERTY_CONTENT_LOADED)).booleanValue();
                
                if (!isUpdate && contentAlreadyLoaded) {
                    
                    log.info("Content of bundle already loaded {}.",
                        bundle.getSymbolicName());
                    
                } else {
                    
                    installContent(session, bundle, pathIter,
                        contentAlreadyLoaded);
                    
                    if (isRetry) {
                        // log success of retry
                        log.info(
                            "Retrytring to load initial content for bundle {} succeeded.",
                            bundle.getSymbolicName());
                    }
                    
                }
                
                success = true;
                return true;
                
            } finally {
                jcrContentHelper.unlockBundleContentInfo(session, bundle,
                    success);
            }
            
        } catch (RepositoryException re) {
            // if we are retrying we already logged this message once, so we
            // won't log it again
            if (!isRetry) {
                log.error("Cannot load initial content for bundle "
                    + bundle.getSymbolicName() + " : " + re.getMessage(), re);
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
        
        // check if bundle has initial content
        final Iterator<PathEntry> pathIter = PathEntry.getContentPaths(bundle);
        if (delayedBundles.contains(bundle)) {
            
            delayedBundles.remove(bundle);
            
        } else {
            
            if (pathIter != null) {
                uninstallContent(session, bundle, pathIter);
                jcrContentHelper.contentIsUninstalled(session, bundle);
            }
            
        }
    }

    // ---------- internal -----------------------------------------------------

    private void installContent(final Session session, final Bundle bundle,
            final Iterator<PathEntry> pathIter,
            final boolean contentAlreadyLoaded) throws RepositoryException {
        log.debug("Installing initial content from bundle {}",
            bundle.getSymbolicName());
        try {

            // the nodes marked to be checked-in after import
            List<Node> versionables = new ArrayList<Node>();

            while (pathIter.hasNext()) {
                final PathEntry entry = pathIter.next();
                if (!contentAlreadyLoaded || entry.isOverwrite()) {
                    
                    Node targetNode = getTargetNode(session, entry.getTarget());

                    if (targetNode != null) {
                        installFromPath(bundle, entry.getPath(),
                            entry.isOverwrite(), versionables,
                            entry.isCheckin(), targetNode);
                    }
                }
            }

            // persist modifications now
            session.save();

            // finally checkin versionable nodes
            for (Node versionable : versionables) {
                versionable.checkin();
            }

        } finally {
            try {
                if (session.hasPendingChanges()) {
                    session.refresh(false);
                }
            } catch (RepositoryException re) {
                log.warn(
                    "Failure to rollback partial initial content for bundle {}",
                    bundle.getSymbolicName(), re);
            }
        }
        log.debug("Done installing initial content from bundle {}",
            bundle.getSymbolicName());

    }

    /**
     * Handle content installation for a single path.
     * 
     * @param bundle The bundle containing the content.
     * @param path The path
     * @param overwrite Should the content be overwritten.
     * @param parent The parent node.
     * @throws RepositoryException
     */
    private void installFromPath(final Bundle bundle, final String path,
            final boolean overwrite, List<Node> versionables,
            final boolean checkin, final Node parent)
            throws RepositoryException {
        
        @SuppressWarnings("unchecked")
        Enumeration<String> entries = bundle.getEntryPaths(path);
        if (entries == null) {
            log.info("install: No initial content entries at {}", path);
            return;
        }

        Map<URL, Node> processedEntries = new HashMap<URL, Node>();

        // potential root node import/extension
        URL rootNodeDescriptor = importRootNode(parent.getSession(), bundle,
            path, versionables, checkin);
        if (rootNodeDescriptor != null) {
            processedEntries.put(rootNodeDescriptor,
                parent.getSession().getRootNode());
        }

        while (entries.hasMoreElements()) {
            final String entry = entries.nextElement();
            log.debug("Processing initial content entry {}", entry);
            if (entry.endsWith("/")) {
                
                // dir, check for node descriptor , else create dir
                String base = entry.substring(0, entry.length() - 1);
                String name = getName(base);

                URL nodeDescriptor = null;
                for (String ext : importProviders.keySet()) {
                    nodeDescriptor = bundle.getEntry(base + ext);
                    if (nodeDescriptor != null) {
                        break;
                    }
                }

                // if we have a descriptor, which has not been processed yet,
                // otherwise call createFolder, which creates an nt:folder or
                // returns an existing node (created by a descriptor)
                Node node = null;
                if (nodeDescriptor != null) {
                    node = processedEntries.get(nodeDescriptor);
                    if (node == null) {
                        node = createNode(parent, name, nodeDescriptor,
                            overwrite, versionables, checkin);
                        processedEntries.put(nodeDescriptor, node);
                    }
                } else {
                    node = createFolder(parent, name, overwrite);
                }

                // walk down the line
                if (node != null) {
                    installFromPath(bundle, entry, overwrite, versionables,
                        checkin, node);
                }

            } else {
                
                // file => create file
                URL file = bundle.getEntry(entry);
                if (processedEntries.containsKey(file)) {
                    // this is a consumed node descriptor
                    continue;
                }

                // install if it is a descriptor
                boolean foundProvider = false;
                final Iterator<String> ipIter = importProviders.keySet().iterator();
                while (!foundProvider && ipIter.hasNext()) {
                    final String ext = ipIter.next();
                    if (entry.endsWith(ext)) {
                        foundProvider = true;
                    }
                }
                if (foundProvider) {
                    Node node = null;
                    if ((node = createNode(parent, getName(entry), file, overwrite,
                        versionables, checkin)) != null) {
                        processedEntries.put(file, node);
                        continue;
                    }
                }

                // otherwise just place as file
                try {
                    createFile(parent, file);
                } catch (IOException ioe) {
                    log.warn("Cannot create file node for {}", file, ioe);
                }
            }
        }
    }

    private Node createNode(Node parent, String name, URL nodeXML,
            boolean overwrite, List<Node> versionables, boolean checkin)
            throws RepositoryException {

        InputStream ins = null;
        try {
            // special treatment for system view imports
            if (nodeXML.getPath().toLowerCase().endsWith(EXT_JCR_XML)) {
                return importSystemView(parent, name, nodeXML);
            }

            NodeReader nodeReader = null;
            for (Map.Entry<String, ImportProvider> e : importProviders.entrySet()) {
                if (nodeXML.getPath().toLowerCase().endsWith(e.getKey())) {
                    nodeReader = e.getValue().getReader();
                    break;
                }
            }

            // cannot find out the type
            if (nodeReader == null) {
                return null;
            }

            ins = nodeXML.openStream();
            NodeDescription clNode = nodeReader.parse(ins);

            // nothing has been parsed
            if (clNode == null) {
                return null;
            }

            if (clNode.getName() == null) {
                // set the name without the [last] extension (xml or json)
                clNode.setName(toPlainName(name));
            }

            return createNode(parent, clNode, overwrite, versionables, checkin);
        } catch (RepositoryException re) {
            throw re;
        } catch (Throwable t) {
            throw new RepositoryException(t.getMessage(), t);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * Delete the node from the initial content.
     * 
     * @param parent
     * @param name
     * @param nodeXML
     * @throws RepositoryException
     */
    private void deleteNode(Node parent, String name)
            throws RepositoryException {
        if (parent.hasNode(name)) {
            parent.getNode(name).remove();
        }
    }

    private Node createNode(Node parentNode, NodeDescription clNode,
            final boolean overwrite, List<Node> versionables, boolean checkin)
            throws RepositoryException {
        
        // if node already exists but should be overwritten, delete it
        if (overwrite && parentNode.hasNode(clNode.getName())) {
            parentNode.getNode(clNode.getName()).remove();
        }

        // ensure repository node
        Node node;
        if (parentNode.hasNode(clNode.getName())) {

            // use existing node
            node = parentNode.getNode(clNode.getName());

        } else if (clNode.getPrimaryNodeType() == null) {

            // node explicit node type, use repository default
            node = parentNode.addNode(clNode.getName());

        } else {

            // explicit primary node type
            node = parentNode.addNode(clNode.getName(),
                clNode.getPrimaryNodeType());
        }

        return setupNode(node, clNode, versionables, checkin);
    }

    private Node setupNode(Node node, NodeDescription clNode,
            List<Node> versionables, boolean checkin)
            throws RepositoryException {

        // ammend mixin node types
        if (clNode.getMixinNodeTypes() != null) {
            for (String mixin : clNode.getMixinNodeTypes()) {
                if (!node.isNodeType(mixin)) {
                    node.addMixin(mixin);
                }
            }
        }

        // check if node is versionable
        boolean addToVersionables = checkin
            && node.isNodeType("mix:versionable");

        if (clNode.getProperties() != null) {
            for (PropertyDescription prop : clNode.getProperties()) {
                if (node.hasProperty(prop.getName())
                    && !node.getProperty(prop.getName()).isNew()) {
                    continue;
                }

                int type = PropertyType.valueFromName(prop.getType());
                if (prop.isMultiValue()) {

                    String[] values = prop.getValues().toArray(
                        new String[prop.getValues().size()]);
                    node.setProperty(prop.getName(), values, type);

                } else if (type == PropertyType.REFERENCE) {

                    // need to resolve the reference
                    String propPath = node.getPath() + "/" + prop.getName();
                    String uuid = getUUID(node.getSession(), propPath,
                        prop.getValue());
                    if (uuid != null) {
                        node.setProperty(prop.getName(), uuid, type);
                    }

                } else if ("jcr:isCheckedOut".equals(prop.getName())) {

                    // don't try to write the property but record its state
                    // for later checkin if set to false
                    boolean checkedout = Boolean.valueOf(prop.getValue());
                    if (!checkedout) {
                        addToVersionables = true;
                    }

                } else {

                    node.setProperty(prop.getName(), prop.getValue(), type);

                }
            }
        }

        // add the current node to the list of versionables to be checked
        // in at the end. This is done if checkin is true and the node is
        // versionable or if the jcr:isCheckedOut property is false
        if (addToVersionables) {
            versionables.add(node);
        }

        // create child nodes from the descriptor
        if (clNode.getChildren() != null) {
            for (NodeDescription child : clNode.getChildren()) {
                createNode(node, child, false, versionables, checkin);
            }
        }

        // resolve REFERENCE property values pointing to this node
        resolveReferences(node);

        return node;
    }

    /**
     * Create a folder
     * 
     * @param parent The parent node.
     * @param name The name of the folder
     * @param overwrite If set to true, an existing folder is removed first.
     * @return The node pointing to the folder.
     * @throws RepositoryException
     */
    private Node createFolder(Node parent, String name, final boolean overwrite)
            throws RepositoryException {
        if (parent.hasNode(name)) {
            if (overwrite) {
                parent.getNode(name).remove();
            } else {
                return parent.getNode(name);
            }
        }

        return parent.addNode(name, "nt:folder");
    }

    /**
     * Create a file from the given url.
     * 
     * @param parent
     * @param source
     * @throws IOException
     * @throws RepositoryException
     */
    private void createFile(Node parent, URL source) throws IOException,
            RepositoryException {
        String name = getName(source.getPath());
        if (parent.hasNode(name)) {
            return;
        }

        URLConnection conn = source.openConnection();
        long lastModified = conn.getLastModified();
        String type = conn.getContentType();
        InputStream data = conn.getInputStream();

        // ensure content type
        if (type == null) {
            type = jcrContentHelper.getMimeType(name);
            if (type == null) {
                log.info(
                    "createFile: Cannot find content type for {}, using {}",
                    source.getPath(), DEFAULT_CONTENT_TYPE);
                type = DEFAULT_CONTENT_TYPE;
            }
        }

        // ensure sensible last modification date
        if (lastModified <= 0) {
            lastModified = System.currentTimeMillis();
        }

        Node file = parent.addNode(name, "nt:file");
        Node content = file.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:mimeType", type);
        content.setProperty("jcr:lastModified", lastModified);
        content.setProperty("jcr:data", data);
    }

    /**
     * Delete the file from the given url.
     * 
     * @param parent
     * @param source
     * @throws IOException
     * @throws RepositoryException
     */
    private void deleteFile(Node parent, URL source) throws IOException,
            RepositoryException {
        String name = getName(source.getPath());
        if (parent.hasNode(name)) {
            parent.getNode(name).remove();
        }
    }

    private String getUUID(Session session, String propPath,
            String referencePath) throws RepositoryException {
        if (session.itemExists(referencePath)) {
            Item item = session.getItem(referencePath);
            if (item.isNode()) {
                Node refNode = (Node) item;
                if (refNode.isNodeType("mix:referenceable")) {
                    return refNode.getUUID();
                }
            }
        } else {
            // not existing yet, keep for delayed setting
            List<String> current = delayedReferences.get(referencePath);
            if (current == null) {
                current = new ArrayList<String>();
                delayedReferences.put(referencePath, current);
            }
            current.add(propPath);
        }

        // no UUID found
        return null;
    }

    private void resolveReferences(Node node) throws RepositoryException {
        List<String> props = delayedReferences.remove(node.getPath());
        if (props == null || props.size() == 0) {
            return;
        }

        // check whether we can set at all
        if (!node.isNodeType("mix:referenceable")) {
            return;
        }

        Session session = node.getSession();
        String uuid = node.getUUID();

        for (String property : props) {
            String name = getName(property);
            Node parentNode = getParentNode(session, property);
            if (parentNode != null) {
                parentNode.setProperty(name, uuid, PropertyType.REFERENCE);
            }
        }
    }

    /**
     * Gets and decods the name part of the <code>path</code>. The name is
     * the part of the path after the last slash (or the complete path if no
     * slash is contained). To support names containing unsupported characters
     * such as colon (<code>:</code>), names may be URL encoded (see
     * <code>java.net.URLEncoder</code>) using the <i>UTF-8</i> character
     * encoding. In this case, this method decodes the name using the
     * <code>java.netURLDecoder</code> class with the <i>UTF-8</i> character
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
                log.error("Cannot decode "
                    + name
                    + " beause the platform has no support for UTF-8, using undecoded");
            } catch (Exception e) {
                // IllegalArgumentException or failure to decode
                log.error("Cannot decode " + name + ", using undecoded", e);
            }
        }

        // not encoded or problems decoding, return the name unmodified
        return name;
    }

    private Node getParentNode(Session session, String path)
            throws RepositoryException {
        int lastSlash = path.lastIndexOf('/');

        // not an absolute path, cannot find parent
        if (lastSlash < 0) {
            return null;
        }

        // node below root
        if (lastSlash == 0) {
            return session.getRootNode();
        }

        // item in the hierarchy
        path = path.substring(0, lastSlash);
        if (!session.itemExists(path)) {
            return null;
        }

        Item item = session.getItem(path);
        return (item.isNode()) ? (Node) item : null;
    }

    private Node getTargetNode(Session session, String path)
            throws RepositoryException {

        // not specyfied path directive
        if (path == null) return session.getRootNode();

        int firstSlash = path.indexOf("/");

        // it´s a relative path
        if (firstSlash != 0) path = "/" + path;

        Item item = session.getItem(path);
        return (item.isNode()) ? (Node) item : null;
    }

    private void uninstallContent(final Session session, final Bundle bundle,
            final Iterator<PathEntry> pathIter) {
        try {
            log.debug("Uninstalling initial content from bundle {}",
                bundle.getSymbolicName());
            while (pathIter.hasNext()) {
                final PathEntry entry = pathIter.next();
                if (entry.isUninstall()) {
                    Node targetNode = getTargetNode(session, entry.getTarget());
                    if (targetNode != null)
                        uninstallFromPath(bundle, entry.getPath(), targetNode);
                } else {
                    log.debug(
                        "Ignoring to uninstall content at {}, uninstall directive is not set.",
                        entry.getPath());
                }
            }

            // persist modifications now
            session.save();
            log.debug("Done uninstalling initial content from bundle {}",
                bundle.getSymbolicName());
        } catch (RepositoryException re) {
            log.error("Unable to uninstall initial content from bundle "
                + bundle.getSymbolicName(), re);
        } finally {
            try {
                if (session.hasPendingChanges()) {
                    session.refresh(false);
                }
            } catch (RepositoryException re) {
                log.warn(
                    "Failure to rollback uninstaling initial content for bundle {}",
                    bundle.getSymbolicName(), re);
            }
        }
    }

    /**
     * Handle content uninstallation for a single path.
     * 
     * @param bundle The bundle containing the content.
     * @param path The path
     * @param parent The parent node.
     * @throws RepositoryException
     */
    private void uninstallFromPath(final Bundle bundle, final String path,
            final Node parent) throws RepositoryException {
        @SuppressWarnings("unchecked")
        Enumeration<String> entries = bundle.getEntryPaths(path);
        if (entries == null) {
            return;
        }

        Set<URL> ignoreEntry = new HashSet<URL>();

        // potential root node import/extension
        Descriptor rootNodeDescriptor = getRootNodeDescriptor(bundle, path);
        if (rootNodeDescriptor != null) {
            ignoreEntry.add(rootNodeDescriptor.rootNodeDescriptor);
        }

        while (entries.hasMoreElements()) {
            final String entry = entries.nextElement();
            log.debug("Processing initial content entry {}", entry);
            if (entry.endsWith("/")) {
                // dir, check for node descriptor , else create dir
                String base = entry.substring(0, entry.length() - 1);
                String name = getName(base);

                URL nodeDescriptor = null;
                for (String ext : importProviders.keySet()) {
                    nodeDescriptor = bundle.getEntry(base + ext);
                    if (nodeDescriptor != null) {
                        break;
                    }
                }

                final Node node;
                boolean delete = false;
                if (nodeDescriptor != null
                    && !ignoreEntry.contains(nodeDescriptor)) {
                    node = (parent.hasNode(toPlainName(name))
                            ? parent.getNode(toPlainName(name))
                            : null);
                    delete = true;
                } else {
                    node = (parent.hasNode(name) ? parent.getNode(name) : null);
                }

                if (node != null) {
                    // walk down the line
                    uninstallFromPath(bundle, entry, node);
                }

                if (delete) {
                    deleteNode(parent, toPlainName(name));
                    ignoreEntry.add(nodeDescriptor);
                }

            } else {
                // file => create file
                URL file = bundle.getEntry(entry);
                if (ignoreEntry.contains(file)) {
                    // this is a consumed node descriptor
                    continue;
                }

                // uninstall if it is a descriptor
                boolean foundProvider = false;
                final Iterator<String> ipIter = importProviders.keySet().iterator();
                while (!foundProvider && ipIter.hasNext()) {
                    final String ext = ipIter.next();
                    if (entry.endsWith(ext)) {
                        foundProvider = true;
                    }
                }
                if (foundProvider) {
                    deleteNode(parent, toPlainName(getName(entry)));
                    ignoreEntry.add(file);
                    continue;
                }

                // otherwise just delete the file
                try {
                    deleteFile(parent, file);
                } catch (IOException ioe) {
                    log.warn("Cannot delete file node for {}", file, ioe);
                }
            }
        }
    }

    /**
     * Import the XML file as JCR system or document view import. If the XML
     * file is not a valid system or document view export/import file,
     * <code>false</code> is returned.
     * 
     * @param parent The parent node below which to import
     * @param nodeXML The URL to the XML file to import
     * @return <code>true</code> if the import succeeds, <code>false</code>
     *         if the import fails due to XML format errors.
     * @throws IOException If an IO error occurrs reading the XML file.
     */
    private Node importSystemView(Node parent, String name, URL nodeXML)
            throws IOException {

        InputStream ins = null;
        try {

            // check whether we have the content already, nothing to do then
            name = toPlainName(name);
            if (parent.hasNode(name)) {
                log.debug(
                    "importSystemView: Node {} for XML {} already exists, nothing to to",
                    name, nodeXML);
                return parent.getNode(name);
            }

            ins = nodeXML.openStream();
            Session session = parent.getSession();
            session.importXML(parent.getPath(), ins, IMPORT_UUID_CREATE_NEW);

            // additionally check whether the expected child node exists
            return (parent.hasNode(name)) ? parent.getNode(name) : null;

        } catch (InvalidSerializedDataException isde) {

            // the xml might not be System or Document View export, fall back
            // to old-style XML reading
            log.info(
                "importSystemView: XML {} does not seem to be system view export, trying old style",
                nodeXML);
            return null;

        } catch (RepositoryException re) {

            // any other repository related issue...
            log.info(
                "importSystemView: Repository issue loading XML {}, trying old style",
                nodeXML);
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
        public URL rootNodeDescriptor;

        public NodeReader nodeReader;
    }

    /**
     * Return the root node descriptor.
     */
    private Descriptor getRootNodeDescriptor(final Bundle bundle,
            final String path) {
        URL rootNodeDescriptor = null;

        for (Map.Entry<String, ImportProvider> e : importProviders.entrySet()) {
            if (e.getValue() != null) {
                rootNodeDescriptor = bundle.getEntry(path + ROOT_DESCRIPTOR
                    + e.getKey());
                if (rootNodeDescriptor != null) {
                    try {
                        final Descriptor d = new Descriptor();
                        d.rootNodeDescriptor = rootNodeDescriptor;
                        d.nodeReader = e.getValue().getReader();
                        return d;
                    } catch (IOException ioe) {
                        log.error("Unable to setup node reader for "
                            + e.getKey(), ioe);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Imports mixin nodes and properties (and optionally child nodes) of the
     * root node.
     */
    private URL importRootNode(Session session, Bundle bundle, String path,
            List<Node> versionables, boolean checkin)
            throws RepositoryException {
        final Descriptor descriptor = getRootNodeDescriptor(bundle, path);
        // no root descriptor found
        if (descriptor == null) {
            return null;
        }

        InputStream ins = null;
        try {

            ins = descriptor.rootNodeDescriptor.openStream();
            NodeDescription clNode = descriptor.nodeReader.parse(ins);

            setupNode(session.getRootNode(), clNode, versionables, checkin);

            return descriptor.rootNodeDescriptor;
        } catch (RepositoryException re) {
            throw re;
        } catch (Throwable t) {
            throw new RepositoryException(t.getMessage(), t);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
        }

    }

    private String toPlainName(String name) {
        String providerExt = null;
        final Iterator<String> ipIter = importProviders.keySet().iterator();
        while (providerExt == null && ipIter.hasNext()) {
            final String ext = ipIter.next();
            if (name.endsWith(ext)) {
                providerExt = ext;
            }
        }
        if (providerExt != null) {
            return name.substring(0, name.length() - providerExt.length());
        }
        return name;

    }
}
