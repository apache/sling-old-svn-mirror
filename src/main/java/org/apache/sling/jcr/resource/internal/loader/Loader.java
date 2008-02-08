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
package org.apache.sling.jcr.resource.internal.loader;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;

import java.awt.image.ImagingOpException;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.resource.internal.ContentLoaderService;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>Loader</code> TODO
 */
public class Loader {

    public static final String CONTENT_HEADER = "Sling-Initial-Content";

    public static final String EXT_XML = ".xml";

    public static final String EXT_JCR_XML = ".jcr.xml";

    public static final String EXT_JSON = ".json";

    public static final String EXT_XJSON = ".xjson";

    // default content type for createFile()
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(Loader.class);

    private ContentLoaderService jcrContentHelper;

    private XmlReader xmlReader;

    private JsonReader jsonReader;

    private XJsonReader xjsonReader;

    private Map<String, List<String>> delayedReferences;

    // bundles whose registration failed and should be retried
    private List<Bundle> delayedBundles;

    public Loader(ContentLoaderService jcrContentHelper) {
        this.jcrContentHelper = jcrContentHelper;
        this.delayedReferences = new HashMap<String, List<String>>();
        this.delayedBundles = new LinkedList<Bundle>();
    }

    public void dispose() {
        this.xmlReader = null;
        this.jsonReader = null;
        this.xjsonReader = null;
        this.delayedReferences = null;
        if (this.delayedBundles != null) {
            this.delayedBundles.clear();
            this.delayedBundles = null;
        }
        this.jcrContentHelper = null;
    }

    public void registerBundle(Session session, Bundle bundle) {
        log.debug("Registering bundle {} for content loading.",
            bundle.getSymbolicName());
        if (this.registerBundleInternal(session, bundle, false)) {
            // handle delayed bundles, might help now
            int currentSize = -1;
            for (int i = this.delayedBundles.size(); i > 0
                && currentSize != this.delayedBundles.size()
                && !this.delayedBundles.isEmpty(); i--) {
                for (Iterator<Bundle> di = this.delayedBundles.iterator(); di.hasNext();) {
                    Bundle delayed = di.next();
                    if (this.registerBundleInternal(session, delayed, true)) {
                        di.remove();
                    }
                }
                currentSize = this.delayedBundles.size();
            }
        } else {
            // add to delayed bundles
            this.delayedBundles.add(bundle);
        }
    }

    private boolean registerBundleInternal(Session session, Bundle bundle,
            boolean isRetry) {
        try {
            this.installContent(session, bundle);
            if (isRetry) {
                // log success of retry
                log.info(
                    "Retrytring to load initial content for bundle {} succeeded.",
                    bundle.getSymbolicName());
            }
            return true;
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

    public void unregisterBundle(Bundle bundle) {
        if (this.delayedBundles.contains(bundle)) {
            this.delayedBundles.remove(bundle);
        } else {
            this.uninstallContent(bundle);
        }
    }

    // ---------- internal -----------------------------------------------------

    private void installContent(Session session, Bundle bundle)
            throws RepositoryException {
        String root = (String) bundle.getHeaders().get(CONTENT_HEADER);
        if (root == null) {
            log.debug("Bundle {} has no initial content",
                bundle.getSymbolicName());
            return;
        }

        try {
            log.debug("Installing initial content from bundle {}",
                bundle.getSymbolicName());
            StringTokenizer tokener = new StringTokenizer(root, ",");
            while (tokener.hasMoreTokens()) {
                String path = tokener.nextToken().trim();
                this.install(bundle, path, session.getRootNode());
            }

            // persist modifications now
            session.save();
            log.debug("Done installing initial content from bundle {}",
                bundle.getSymbolicName());
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

    }

    private void install(Bundle bundle, String path, javax.jcr.Node parent)
            throws RepositoryException {
        @SuppressWarnings("unchecked")
        Enumeration<String> entries = bundle.getEntryPaths(path);
        if (entries == null) {
            log.info("install: No initial content entries at {}", path);
            return;
        }

        Set<URL> ignoreEntry = new HashSet<URL>();
        while (entries.hasMoreElements()) {
            final String entry = entries.nextElement();
            log.debug("Processing initial content entry {}", entry);
            if (entry.endsWith("/")) {
                // dir, check for node descriptor , else create dir
                String base = entry.substring(0, entry.length() - 1);
                String name = this.getName(base);

                Node node = null;
                URL nodeDescriptor = bundle.getEntry(base + EXT_XML);
                if (nodeDescriptor == null) {
                    nodeDescriptor = bundle.getEntry(base + EXT_JSON);
                }
                if (nodeDescriptor == null) {
                    nodeDescriptor = bundle.getEntry(base + EXT_XJSON);
                }

                // if we have a descriptor, which has not been processed yet,
                // otherwise call crateFolder, which creates an nt:folder or
                // returns an existing node (created by a descriptor)
                if (nodeDescriptor != null
                    && !ignoreEntry.contains(nodeDescriptor)) {
                    node = this.createNode(parent, name, nodeDescriptor);
                    ignoreEntry.add(nodeDescriptor);
                } else {
                    node = this.createFolder(parent, name);
                }

                // walk down the line
                if (node != null) {
                    this.install(bundle, entry, node);
                }

            } else {
                // file => create file
                URL file = bundle.getEntry(entry);
                if (ignoreEntry.contains(file)) {
                    // this is a consumed node descriptor
                    continue;
                }

                // install if it is a descriptor
                if (entry.endsWith(EXT_XML) || entry.endsWith(EXT_JSON)
                    || entry.endsWith(EXT_XJSON)) {
                    if (this.createNode(parent, this.getName(entry), file) != null) {
                        ignoreEntry.add(file);
                        continue;
                    }
                }

                // otherwise just place as file
                try {
                    this.createFile(parent, file);
                } catch (IOException ioe) {
                    log.warn("Cannot create file node for {}", file, ioe);
                }
            }
        }
    }

    private javax.jcr.Node createFolder(javax.jcr.Node parent, String name)
            throws RepositoryException {
        if (parent.hasNode(name)) {
            return parent.getNode(name);
        }

        return parent.addNode(name, "nt:folder");
    }

    private javax.jcr.Node createNode(Node parent, String name, URL nodeXML)
            throws RepositoryException {

        InputStream ins = null;
        try {
            NodeReader nodeReader;
            if (nodeXML.getPath().toLowerCase().endsWith(EXT_XML)) {
                // return immediately if system/document view import succeeds
                Node childNode = importSystemView(parent, name, nodeXML);
                if (childNode != null) {
                    return childNode;
                }
                
                nodeReader = this.getXmlReader();
            } else if (nodeXML.getPath().toLowerCase().endsWith(EXT_JSON)) {
                nodeReader = this.getJsonReader();
            } else if (nodeXML.getPath().toLowerCase().endsWith(EXT_XJSON)) {
                nodeReader = this.getXJsonReader();
            } else {
                // cannot find out the type
                return null;
            }

            ins = nodeXML.openStream();
            org.apache.sling.jcr.resource.internal.loader.Node clNode = nodeReader.parse(ins);

            // nothing has been parsed
            if (clNode == null) {
                return null;
            }

            if (clNode.getName() == null) {
                // set the name without the [last] extension (xml or json)
                clNode.setName(toPlainName(name));
            }

            return this.createNode(parent, clNode);
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

    private Node createNode(Node parentNode,
            org.apache.sling.jcr.resource.internal.loader.Node clNode)
            throws RepositoryException {
        Node node;
        if (parentNode.hasNode(clNode.getName())) {
            node = parentNode.getNode(clNode.getName());
        } else {
            node = parentNode.addNode(clNode.getName(),
                clNode.getPrimaryNodeType());

            if (clNode.getMixinNodeTypes() != null) {
                for (String mixin : clNode.getMixinNodeTypes()) {
                    node.addMixin(mixin);
                }
            }
        }

        if (clNode.getProperties() != null) {
            for (Property prop : clNode.getProperties()) {
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
                    String uuid = this.getUUID(node.getSession(), propPath,
                        prop.getValue());
                    if (uuid != null) {
                        node.setProperty(prop.getName(), uuid, type);
                    }
                } else {
                    node.setProperty(prop.getName(), prop.getValue(), type);
                }
            }
        }

        if (clNode.getChildren() != null) {
            for (org.apache.sling.jcr.resource.internal.loader.Node child : clNode.getChildren()) {
                this.createNode(node, child);
            }
        }

        this.resolveReferences(node);

        return node;
    }

    private void createFile(Node parent, URL source) throws IOException,
            RepositoryException {
        String name = this.getName(source.getPath());
        if (parent.hasNode(name)) {
            return;
        }

        URLConnection conn = source.openConnection();
        long lastModified = conn.getLastModified();
        String type = conn.getContentType();
        InputStream data = conn.getInputStream();

        // ensure content type
        if (type == null) {
            type = this.jcrContentHelper.getMimeType(name);
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
            List<String> current = this.delayedReferences.get(referencePath);
            if (current == null) {
                current = new ArrayList<String>();
                this.delayedReferences.put(referencePath, current);
            }
            current.add(propPath);
        }

        // no UUID found
        return null;
    }

    private void resolveReferences(Node node) throws RepositoryException {
        List<String> props = this.delayedReferences.remove(node.getPath());
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
            String name = this.getName(property);
            Node parentNode = this.getParentNode(session, property);
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

    private void uninstallContent(Bundle bundle) {
        String root = (String) bundle.getHeaders().get(CONTENT_HEADER);
        if (root == null) {
            log.debug("Bundle {} has no initial content",
                bundle.getSymbolicName());
            return;
        }

        log.info(
            "Content deinstallation not implemented yet. Keeping content of bundle {}",
            bundle.getSymbolicName());
    }

    private XmlReader getXmlReader() throws IOException {
        if (this.xmlReader == null) {
            try {
                this.xmlReader = new XmlReader();
            } catch (Throwable t) {
                throw (IOException) new IOException(t.getMessage()).initCause(t);
            }
        }

        return this.xmlReader;
    }

    private JsonReader getJsonReader() {
        if (this.jsonReader == null) {
            this.jsonReader = new JsonReader();
        }
        return this.jsonReader;
    }

    private XJsonReader getXJsonReader() {
        if (this.xjsonReader == null) {
            this.xjsonReader = new XJsonReader();
        }
        return this.xjsonReader;
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

        // only consider ".jcr.xml" files here
        if (!nodeXML.getPath().toLowerCase().endsWith(EXT_JCR_XML)) {
            return null;
        }

        InputStream ins = null;
        try {

            ins = nodeXML.openStream();
            Session session = parent.getSession();
            session.importXML(parent.getPath(), ins, IMPORT_UUID_CREATE_NEW);

            // additionally check whether the expected child node exists
            name = toPlainName(name);
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
    
    private String toPlainName(String name) {
        int diff;
        if (name.endsWith(EXT_JCR_XML)) {
            diff = EXT_JCR_XML.length();
        } else if (name.endsWith(EXT_XML)) {
            diff = EXT_XML.length();
        } else if (name.endsWith(EXT_JSON)) {
            diff = EXT_JSON.length();
        } else if (name.endsWith(EXT_XJSON)) {
            diff = EXT_XJSON.length();
        } else {
            return name;
        }
        
        return name.substring(0, name.length() - diff);
    }
}
