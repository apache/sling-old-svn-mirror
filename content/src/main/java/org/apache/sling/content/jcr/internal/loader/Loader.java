/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.content.jcr.internal.loader;

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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.sling.content.jcr.NodeTypeLoader;
import org.apache.sling.content.jcr.internal.JcrContentHelper;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>Loader</code> TODO
 */
public class Loader {

    public static final String CONTENT_HEADER = "Sling-Initial-Content";
    public static final String NODETYPES_BUNDLE_HEADER = "Sling-Nodetypes";

    public static final String EXT_XML = ".xml";
    public static final String EXT_JSON = ".json";

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(Loader.class);

    private JcrContentHelper jcrContentHelper;
    private XmlReader xmlReader;
    private JsonReader jsonReader;
    private Map<String, List<String>> delayedReferences;

    // bundles whose registration failed and should be retried
    private List<Bundle> delayedBundles;

    public Loader(JcrContentHelper jcrContentHelper) {
        this.jcrContentHelper = jcrContentHelper;
        this.delayedReferences = new HashMap<String, List<String>>();
        this.delayedBundles = new LinkedList<Bundle>();
    }

    public void dispose() {
        xmlReader = null;
        jsonReader = null;
        delayedReferences = null;
        if (delayedBundles != null) {
            delayedBundles.clear();
            delayedBundles = null;
        }
        jcrContentHelper = null;
    }

    public void registerBundle(Bundle bundle) {
        if (registerBundleInternal(bundle)) {
            // handle delayed bundles, might help now
            int currentSize = -1;
            for (int i=delayedBundles.size(); i > 0 && currentSize != delayedBundles.size() && !delayedBundles.isEmpty(); i--) {
                for (Iterator<Bundle> di=delayedBundles.iterator(); di.hasNext(); ) {
                    Bundle delayed = di.next();
                    if (registerBundleInternal(delayed)) {
                        di.remove();
                    }
                }
                currentSize = delayedBundles.size();
            }
        } else {
            // add to delayed bundles
            delayedBundles.add(bundle);
        }
    }

    private boolean registerBundleInternal (Bundle bundle) {
        try {
            if (registerNodeTypes(bundle)) {
                installContent(bundle);
                return true;
            }
        } catch (RepositoryException re) {
            log.error("Cannot load initial content for bundle {}: {}",
                bundle.getSymbolicName(), re);
        }

        return false;
    }

    public void unregisterBundle(Bundle bundle) {
        uninstallContent(bundle);
    }

    public void checkNodeType(ClassDescriptor classDescriptor) throws RepositoryException {
        Session session = getSession();
        try {
            String nodeType = classDescriptor.getJcrNodeType();
            if (nodeType == null || nodeType.length() == 0) {
                return;
            }

            try {
                session.getWorkspace().getNodeTypeManager().getNodeType(nodeType);
                return;
            } catch (NoSuchNodeTypeException nsnte) {
                // have to register
                log.debug("Node Type {} is not registered yet", nodeType);
            }

            // TODO: create and later register it
            log.error("Nodetype {} is missing, but Session cannot be used to register", nodeType);
            return;

        } finally {
            ungetSession(session);
        }
    }

    public void registerNodeTypes(List<?> nodeTypes) throws RepositoryException {
        // TODO
    }

    //---------- internal -----------------------------------------------------

    private boolean registerNodeTypes(Bundle bundle) throws RepositoryException {
        // TODO: define header referring to mapper files
        String typesHeader = (String) bundle.getHeaders().get(NODETYPES_BUNDLE_HEADER);
        if (typesHeader == null) {
            // no components in the bundle, return with success
            log.debug("registerNodeTypes: Bundle {} has no nodetypes",
                bundle.getSymbolicName());
            return true;
        }

        boolean success = true;
        Session session = getSession();
        try {
            StringTokenizer tokener = new StringTokenizer(typesHeader, ",");
            while (tokener.hasMoreTokens()) {
                String nodeTypeFile = tokener.nextToken().trim();

                URL mappingURL = bundle.getEntry(nodeTypeFile);
                if (mappingURL == null) {
                    log.warn("Mapping {} not found in bundle {}", nodeTypeFile, bundle.getSymbolicName());
                    continue;
                }

                InputStream ins = null;
                try {
                    // laod the descriptors
                    ins = mappingURL.openStream();
                    NodeTypeLoader.registerNodeType(session, ins);
                } catch (IOException ioe) {
                    success = false;
//                    log.error("Cannot read node types " + nodeTypeFile
//                        + " from bundle " + bundle.getSymbolicName(), ioe);
                    log.warn("Cannot read node types {} from bundle {}: {}",
                        new Object[]{ nodeTypeFile, bundle.getSymbolicName(), ioe });
                } catch (Exception e) {
                    success = false;
//                    log.error("Error loading node types " + nodeTypeFile
//                        + " from bundle " + bundle.getSymbolicName(), e);
                    log.error("Error loading node types {} from bundle {}: {}",
                        new Object[]{ nodeTypeFile, bundle.getSymbolicName(), e });
                } finally {
                    if (ins != null) {
                        try {
                            ins.close();
                        } catch (IOException ioe) {
                            // ignore
                        }
                    }
                }
            }
        } finally {
            ungetSession(session);
        }

        return success;
    }

    private void installContent(Bundle bundle) throws RepositoryException {
        String root = (String) bundle.getHeaders().get(CONTENT_HEADER);
        if (root == null) {
            log.debug("Bundle {} has no initial content", bundle.getSymbolicName());
            return;
        }

        Session session = getSession();

        try {
            log.debug("Installing Initial Content of Bundle {}", bundle.getSymbolicName());
            StringTokenizer tokener = new StringTokenizer(root, ",");
            while (tokener.hasMoreTokens()) {
                String path = tokener.nextToken().trim();
                install(bundle, path, session.getRootNode());
            }

            // persist modifications now
            session.save();
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

            // logout the session for now
            ungetSession(session);
        }

    }

    private void install(Bundle bundle, String path, javax.jcr.Node parent) throws RepositoryException {
        Set<URL> ignoreEntry = new HashSet<URL>();

        Enumeration<?> entries = bundle.getEntryPaths(path);
        if (entries == null) {
            log.info("install: No entries at {}", path);
            return;
        }

        while (entries.hasMoreElements()) {
            String entry = (String) entries.nextElement();
            if (entry.endsWith("/")) {
                // dir, check for node descriptor , else create dir
                String base = entry.substring(0, entry.length()-1);
                String name = getName(base);

                Node node = null;
                URL nodeDescriptor = bundle.getEntry(base + EXT_XML);
                if (nodeDescriptor == null) {
                    nodeDescriptor = bundle.getEntry(base + EXT_JSON);
                }

                // if we have a descriptor, which has not been processed yet,
                // otherwise call crateFolder, which creates an nt:folder or
                // returns an existing node (created by a descriptor)
                if (nodeDescriptor != null && !ignoreEntry.contains(nodeDescriptor)) {
                    node = createNode(parent, name, nodeDescriptor);
                    ignoreEntry.add(nodeDescriptor);
                } else {
                    node = createFolder(parent, name);
                }

                // walk down the line
                if (node != null) {
                    install(bundle, entry, node);
                }

            } else {
                // file => create file
                URL file = bundle.getEntry(entry);
                if (ignoreEntry.contains(file)) {
                    // this is a consumed node descriptor
                    continue;
                }

                // install if it is a descriptor
                if (entry.endsWith(EXT_XML) || entry.endsWith(EXT_JSON)) {
                    if (createNode(parent, getName(entry), file) != null) {
                        ignoreEntry.add(file);
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

    private javax.jcr.Node createFolder(javax.jcr.Node parent, String name) throws RepositoryException {
        if (parent.hasNode(name)) {
            return parent.getNode(name);
        }

        return parent.addNode(name, "nt:folder");
    }

    private javax.jcr.Node createNode(Node parent, String name, URL nodeXML) throws RepositoryException {

        InputStream ins = null;
        try {
            NodeReader nodeReader;
            if (nodeXML.getPath().toLowerCase().endsWith(".xml")) {
                nodeReader = getXmlReader();
            } else if (nodeXML.getPath().toLowerCase().endsWith(".json")) {
                nodeReader = getJsonReader();
            } else {
                // cannot find out the type
                return null;
            }

            ins = nodeXML.openStream();
            org.apache.sling.content.jcr.internal.loader.Node clNode = nodeReader.parse(ins);

            // nothing has been parsed
            if (clNode == null) {
                return null;
            }

            if (clNode.getName() == null) {
                // set the name without the [last] extension (xml or json)
                clNode.setName(name.substring(0, name.lastIndexOf('.')));
            }

            return createNode(parent, clNode);
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

    private Node createNode(Node parentNode, org.apache.sling.content.jcr.internal.loader.Node clNode) throws RepositoryException {
        Node node;
        if (parentNode.hasNode(clNode.getName())) {
            node = parentNode.getNode(clNode.getName());
        } else {
            node = parentNode.addNode(clNode.getName(), clNode.getPrimaryNodeType());

            if (clNode.getMixinNodeTypes() != null) {
                for (String mixin : clNode.getMixinNodeTypes()) {
                    node.addMixin(mixin);
                }
            }
        }

        if (clNode.getProperties() != null) {
            for (Property prop : clNode.getProperties()) {
                if (node.hasProperty(prop.getName()) && !node.getProperty(prop.getName()).isNew()) {
                    continue;
                }

                int type = PropertyType.valueFromName(prop.getType());
                if (prop.isMultiValue()) {
                    String[] values = prop.getValues().toArray(new String[prop.getValues().size()]);
                    node.setProperty(prop.getName(), values, type);
                } else if (type == PropertyType.REFERENCE) {
                    // need to resolve the reference
                    String propPath = node.getPath() + "/" + prop.getName();
                    String uuid = getUUID(node.getSession(), propPath, prop.getValue());
                    if (uuid != null) {
                        node.setProperty(prop.getName(), uuid, type);
                    }
                } else {
                    node.setProperty(prop.getName(), prop.getValue(), type);
                }
            }
        }

        if (clNode.getChildren() != null) {
            for (org.apache.sling.content.jcr.internal.loader.Node child : clNode.getChildren()) {
                createNode(node, child);
            }
        }

        resolveReferences(node);

        return node;
    }

    private void createFile(Node parent, URL source) throws IOException, RepositoryException {
        String name = getName(source.getPath());
        if (parent.hasNode(name)) {
            return;
        }

        URLConnection conn = source.openConnection();
        long lastModified = conn.getLastModified();
        String type = conn.getContentType();
        InputStream data = conn.getInputStream();

        if (type == null) {
            type = jcrContentHelper.getMimeType(name);
            if (type == null) {
                type = "application/octet-stream";
            }
        }

        Node file = parent.addNode(name, "nt:file");
        Node content = file.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:mimeType", type);
        content.setProperty("jcr:lastModified", lastModified);
        content.setProperty("jcr:data", data);
    }

    private String getUUID(Session session, String propPath, String referencePath) throws RepositoryException {
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
     * Gets and decods the name part of the <code>path</code>. The name is the
     * part of the path after the last slash (or the complete path if no slash
     * is contained). To support names containing unsupported characters such
     * as colon (<code>:</code>), names may be URL encoded (see <code>java.net.URLEncoder</code>)
     * using the <i>UTF-8</i> character encoding. In this case, this method
     * decodes the name using the <code>java.netURLDecoder</code> class with
     * the <i>UTF-8</i> character encoding.
     *
     * @param path The path from which to extract the name part.
     *
     * @return The URL decoded name part.
     */
    private String getName(String path){
        int lastSlash = path.lastIndexOf('/');
        String name = (lastSlash < 0) ? path : path.substring(lastSlash+1);

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

    private Node getParentNode(Session session, String path) throws RepositoryException {
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
            log.debug("Bundle {} has no initial content", bundle.getSymbolicName());
            return;
        }

        log.info("Content deinstallation not implemented yet. Keeping content of bundle {}",
            bundle.getSymbolicName());
    }

    private XmlReader getXmlReader() throws IOException {
        if (xmlReader == null) {
            try {
                xmlReader = new XmlReader();
            } catch (Throwable t) {
                throw (IOException) new IOException(t.getMessage()).initCause(t);
            }
        }

        return xmlReader;
    }

    private JsonReader getJsonReader() {
        if (jsonReader == null) {
            jsonReader = new JsonReader();
        }
        return jsonReader;
    }

    private Session getSession() throws RepositoryException {
        return jcrContentHelper.getRepository().loginAdministrative(null);
    }

    private void ungetSession(Session session) {
        if (session != null) {
            session.logout();
        }
    }
}
