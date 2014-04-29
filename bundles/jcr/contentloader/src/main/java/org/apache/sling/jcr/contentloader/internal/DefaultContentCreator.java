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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.contentloader.ContentImportListener;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ContentLoader</code> creates the nodes and properties.
 *
 * @since 2.0.4
 */
public class DefaultContentCreator implements ContentCreator {

    final Logger log = LoggerFactory.getLogger(DefaultContentCreator.class);

    private ImportOptions configuration;

    private final Pattern jsonDatePattern = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}[-+]{1}[0-9]{2}[:]{0,1}[0-9]{2}$");

    private final SimpleDateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final Stack<Node> parentNodeStack = new Stack<Node>();

    /**
     * The list of versionables.
     */
    private final List<Node> versionables = new ArrayList<Node>();

    /**
     * Delayed references during content loading for the reference property.
     */
    private final Map<String, List<String>> delayedReferences = new HashMap<String, List<String>>();

    private final Map<String, String[]> delayedMultipleReferences = new HashMap<String, String[]>();

    private String defaultName;

    private Node createdRootNode;

    private boolean isParentNodeImport;

    private boolean ignoreOverwriteFlag = false;

    // default content type for createFile()
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /**
     * Helper class to get the mime type of a file.
     */
    private final JcrContentHelper jcrContentHelper;

    /**
     * List of active import providers mapped by extension.
     */
    private Map<String, ImportProvider> importProviders;

    /**
     * Optional list of created nodes (for uninstall)
     */
    private List<String> createdNodes;

    /**
     * Optional listener to get notified about changes
     */
    private ContentImportListener importListener;

    /**
     * A one time use seed to randomize the user location.
     */
    private static final long INSTANCE_SEED = System.currentTimeMillis();

    /**
     * The number of levels folder used to store a user, could be a configuration option.
     */
    private static final int STORAGE_LEVELS = 3;

    /**
     * Constructor.
     *
     * @param jcrContentHelper Helper class to get the mime type of a file
     */
    public DefaultContentCreator(JcrContentHelper jcrContentHelper) {
        this.jcrContentHelper = jcrContentHelper;
    }

    /**
     * Initialize this component.
     *
     * @param pathEntry              The configuration for this import.
     * @param defaultImportProviders List of all import providers.
     * @param createdNodes           Optional list to store new nodes (for uninstall)
     */
    public void init(final ImportOptions pathEntry, final Map<String, ImportProvider> defaultImportProviders, final List<String> createdNodes, final ContentImportListener importListener) {
        this.configuration = pathEntry;
        // create list of allowed import providers
        this.importProviders = new HashMap<String, ImportProvider>();
        final Iterator<Map.Entry<String, ImportProvider>> entryIter = defaultImportProviders.entrySet().iterator();
        while (entryIter.hasNext()) {
            final Map.Entry<String, ImportProvider> current = entryIter.next();
            if (!configuration.isIgnoredImportProvider(current.getKey())) {
                importProviders.put(current.getKey(), current.getValue());
            }
        }
        this.createdNodes = createdNodes;
        this.importListener = importListener;
    }

    /**
     * If the defaultName is null, we are in PARENT_NODE import mode.
     *
     * @param parentNode
     * @param defaultName
     */
    public void prepareParsing(final Node parentNode, final String defaultName) {
        this.parentNodeStack.clear();
        this.parentNodeStack.push(parentNode);
        this.defaultName = defaultName;
        isParentNodeImport = defaultName == null;
        this.createdRootNode = null;
    }

    /**
     * Get the list of versionable nodes.
     */
    public List<Node> getVersionables() {
        return this.versionables;
    }

    /**
     * Clear the content loader.
     */
    public void clear() {
        this.versionables.clear();
    }

    /**
     * Set the ignore overwrite flag.
     *
     * @param flag
     */
    public void setIgnoreOverwriteFlag(boolean flag) {
        this.ignoreOverwriteFlag = flag;
    }

    /**
     * Get the created root node.
     */
    public Node getCreatedRootNode() {
        return this.createdRootNode;
    }

    /**
     * Get all active import providers.
     *
     * @return A map of providers
     */
    public Map<String, ImportProvider> getImportProviders() {
        return this.importProviders;
    }

    /**
     * Return the import provider for the name
     *
     * @param name The file name.
     * @return The provider or <code>null</code>
     */
    public ImportProvider getImportProvider(String name) {
        ImportProvider provider = null;
        final Iterator<String> ipIter = importProviders.keySet().iterator();
        while (provider == null && ipIter.hasNext()) {
            final String ext = ipIter.next();
            if (name.endsWith(ext)) {
                provider = importProviders.get(ext);
            }
        }
        return provider;
    }

    /**
     * Get the extension of the file name.
     *
     * @param name The file name.
     * @return The extension a provider is registered for - or <code>null</code>
     */
    public String getImportProviderExtension(String name) {
        String providerExt = null;
        final Iterator<String> ipIter = importProviders.keySet().iterator();
        while (providerExt == null && ipIter.hasNext()) {
            final String ext = ipIter.next();
            if (name.endsWith(ext)) {
                providerExt = ext;
            }
        }
        return providerExt;
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createNode(java.lang.String, java.lang.String, java.lang.String[])
     */
    public void createNode(String name, String primaryNodeType, String[] mixinNodeTypes) throws RepositoryException {
        final Node parentNode = this.parentNodeStack.peek();
        boolean isParentImport = (name == null && isParentNodeImport);
        if (name == null) {
            if (this.parentNodeStack.size() > 1) {
                throw new RepositoryException("Node needs to have a name.");
            }
            name = this.defaultName;
        }

        // if we are in parent node import mode, we don't create the root top level node!
        if (!isParentImport || this.parentNodeStack.size() > 1) {
            // if node already exists but should be overwritten, delete it
            if (!this.ignoreOverwriteFlag && this.configuration.isOverwrite() && parentNode.hasNode(name)) {
                checkoutIfNecessary(parentNode);
                parentNode.getNode(name).remove();
            }

            // ensure repository node
            Node node;
            if (parentNode.hasNode(name)) {
                // use existing node
                node = parentNode.getNode(name);
            } else if (primaryNodeType == null) {
                // no explicit node type, use repository default
                checkoutIfNecessary(parentNode);
                node = parentNode.addNode(name);
                addNodeToCreatedList(node);
                if (this.importListener != null) {
                    this.importListener.onCreate(node.getPath());
                }
            } else {
                // explicit primary node type
                checkoutIfNecessary(parentNode);
                node = parentNode.addNode(name, primaryNodeType);
                addNodeToCreatedList(node);
                if (this.importListener != null) {
                    this.importListener.onCreate(node.getPath());
                }
            }

            // amend mixin node types
            if (mixinNodeTypes != null) {
                for (final String mixin : mixinNodeTypes) {
                    if (!node.isNodeType(mixin)) {
                        node.addMixin(mixin);
                    }
                }
            }

            // check if node is versionable
            final boolean addToVersionables = this.configuration.isCheckin() && node.isNodeType("mix:versionable");
            if (addToVersionables) {
                this.versionables.add(node);
            }

            this.parentNodeStack.push(node);
            if (this.createdRootNode == null) {
                this.createdRootNode = node;
            }
        }
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createProperty(java.lang.String, int, java.lang.String)
     */
    public void createProperty(String name, int propertyType, String value) throws RepositoryException {
        final Node node = this.parentNodeStack.peek();
        // check if the property already exists and isPropertyOverwrite() is false, don't overwrite it in this case
        if (node.hasProperty(name) && !this.configuration.isPropertyOverwrite() && !node.getProperty(name).isNew()) {
            return;
        }

        if (propertyType == PropertyType.REFERENCE) {
            // need to resolve the reference
            String propPath = node.getPath() + "/" + name;
            String uuid = getUUID(node.getSession(), propPath, getAbsPath(node, value));
            if (uuid != null) {
                checkoutIfNecessary(node);
                node.setProperty(name, uuid, propertyType);
                if (this.importListener != null) {
                    this.importListener.onCreate(node.getProperty(name).getPath());
                }
            }
        } else if ("jcr:isCheckedOut".equals(name)) {
            // don't try to write the property but record its state
            // for later checkin if set to false
            final boolean checkedout = Boolean.valueOf(value);
            if (!checkedout) {
                if (!this.versionables.contains(node)) {
                    this.versionables.add(node);
                }
            }
        } else if (propertyType == PropertyType.DATE) {
            checkoutIfNecessary(node);
            try {
                node.setProperty(name, parseDateString(value));
            } catch (ParseException e) {
                // Fall back to default behaviour if this fails
                node.setProperty(name, value, propertyType);
            }
            if (this.importListener != null) {
                this.importListener.onCreate(node.getProperty(name).getPath());
            }
        } else {
            checkoutIfNecessary(node);
            if (propertyType == PropertyType.UNDEFINED) {
                node.setProperty(name, value);
            } else {
                node.setProperty(name, value, propertyType);
            }
            if (this.importListener != null) {
                this.importListener.onCreate(node.getProperty(name).getPath());
            }
        }
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createProperty(java.lang.String, int, java.lang.String[])
     */
    public void createProperty(String name, int propertyType, String[] values) throws RepositoryException {
        final Node node = this.parentNodeStack.peek();
        // check if the property already exists and isPropertyOverwrite() is false, don't overwrite it in this case
        if (node.hasProperty(name) && !this.configuration.isPropertyOverwrite() && !node.getProperty(name).isNew()) {
            return;
        }
        if (propertyType == PropertyType.REFERENCE) {
            String propPath = node.getPath() + "/" + name;
            boolean hasAll = true;
            String[] uuids = new String[values.length];
            String[] uuidOrPaths = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                uuids[i] = getUUID(node.getSession(), propPath, getAbsPath(node, values[i]));
                uuidOrPaths[i] = uuids[i] != null ? uuids[i] : getAbsPath(node, values[i]);
                if (uuids[i] == null) hasAll = false;
            }
            checkoutIfNecessary(node);
            node.setProperty(name, uuids, propertyType);
            if (this.importListener != null) {
                this.importListener.onCreate(node.getProperty(name).getPath());
            }
            if (!hasAll) {
                delayedMultipleReferences.put(propPath, uuidOrPaths);
            }
        } else if (propertyType == PropertyType.DATE) {
            checkoutIfNecessary(node);
            try {
                // This modification is to remove the colon in the JSON Timezone
                ValueFactory valueFactory = node.getSession().getValueFactory();
                Value[] jcrValues = new Value[values.length];

                for (int i = 0; i < values.length; i++) {
                    jcrValues[i] = valueFactory.createValue(parseDateString(values[i]));
                }

                node.setProperty(name, jcrValues, propertyType);
            } catch (ParseException e) {
                // If this failes, fallback to the default
                log.warn("Could not create dates for property, fallingback to defaults", e);
                node.setProperty(name, values, propertyType);
            }
            if (this.importListener != null) {
                this.importListener.onCreate(node.getProperty(name).getPath());
            }
        } else {
            checkoutIfNecessary(node);
            if (propertyType == PropertyType.UNDEFINED) {
                node.setProperty(name, values);
            } else {
                node.setProperty(name, values, propertyType);
            }
            if (this.importListener != null) {
                this.importListener.onCreate(node.getProperty(name).getPath());
            }
        }
    }

    protected Value createValue(final ValueFactory factory, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return factory.createValue((Long) value);
        } else if (value instanceof Date) {
            final Calendar c = Calendar.getInstance();
            c.setTime((Date) value);
            return factory.createValue(c);
        } else if (value instanceof Calendar) {
            return factory.createValue((Calendar) value);
        } else if (value instanceof Double) {
            return factory.createValue((Double) value);
        } else if (value instanceof Boolean) {
            return factory.createValue((Boolean) value);
        } else if (value instanceof InputStream) {
            return factory.createValue((InputStream) value);
        } else {
            return factory.createValue(value.toString());
        }
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createProperty(java.lang.String, java.lang.Object)
     */
    public void createProperty(String name, Object value) throws RepositoryException {
        createProperty(name, value, false);
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createProperty(java.lang.String, java.lang.Object[])
     */
    public void createProperty(String name, Object[] values) throws RepositoryException {
        createProperty(name, values, false);
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#finishNode()
     */
    public void finishNode() throws RepositoryException {
        final Node node = this.parentNodeStack.pop();
        // resolve REFERENCE property values pointing to this node
        resolveReferences(node);
    }

    private void addNodeToCreatedList(Node node) throws RepositoryException {
        if (this.createdNodes != null) {
            this.createdNodes.add(node.getSession().getWorkspace().getName() + ":" + node.getPath());
        }
    }

    private String getAbsPath(Node node, String path) throws RepositoryException {
        if (path.startsWith("/")) {
            return path;
        }

        while (path.startsWith("../")) {
            path = path.substring(3);
            node = node.getParent();
        }

        while (path.startsWith("./")) {
            path = path.substring(2);
        }

        return node.getPath() + "/" + path;
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
                checkoutIfNecessary(parentNode);
                if (parentNode.hasProperty(name) && parentNode.getProperty(name).getDefinition().isMultiple()) {
                    boolean hasAll = true;
                    String[] uuidOrPaths = delayedMultipleReferences.get(property);
                    String[] uuids = new String[uuidOrPaths.length];
                    for (int i = 0; i < uuidOrPaths.length; i++) {
                        // is the reference still a path
                        if (uuidOrPaths[i].startsWith("/")) {
                            if (uuidOrPaths[i].equals(node.getPath())) {
                                uuidOrPaths[i] = uuid;
                                uuids[i] = uuid;
                            } else {
                                uuids[i] = null;
                                hasAll = false;
                            }
                        } else {
                            uuids[i] = uuidOrPaths[i];
                        }
                    }
                    parentNode.setProperty(name, uuids, PropertyType.REFERENCE);
                    if (this.importListener != null) {
                        this.importListener.onCreate(parentNode.getProperty(name).getPath());
                    }
                    if (hasAll) {
                        delayedMultipleReferences.remove(property);
                    }
                } else {
                    parentNode.setProperty(name, uuid, PropertyType.REFERENCE);
                    if (this.importListener != null) {
                        this.importListener.onCreate(parentNode.getProperty(name).getPath());
                    }
                }
            }
        }
    }

    /**
     * Gets the name part of the <code>path</code>. The name is
     * the part of the path after the last slash (or the complete path if no
     * slash is contained).
     *
     * @param path The path from which to extract the name part.
     * @return The name part.
     */
    private String getName(String path) {
        int lastSlash = path.lastIndexOf('/');
        String name = (lastSlash < 0) ? path : path.substring(lastSlash + 1);
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

    private Calendar parseDateString(String value) throws ParseException {
        if (jsonDatePattern.matcher(value).matches()) {
            String modifiedJsonDate = value;

            // This modification is to remove the colon in the JSON Timezone
            // to match the Java Version
            if (value.lastIndexOf(":") == 26) {
                modifiedJsonDate = value.substring(0, 26) + value.substring(27);
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(jsonDateFormat.parse(modifiedJsonDate));

            return cal;
        }

        return null;
    }

    private void createProperty(String name, Object value, boolean overwriteExisting) throws RepositoryException {
        final Node node = this.parentNodeStack.peek();
        // check if the property already exists, don't overwrite it in this case
        if (node.hasProperty(name) && !node.getProperty(name).isNew() && !overwriteExisting) {
            return;
        }
        if (value == null) {
            if (node.hasProperty(name)) {
                checkoutIfNecessary(node);
                String propPath = node.getProperty(name).getPath();
                node.getProperty(name).remove();
                if (this.importListener != null) {
                    this.importListener.onDelete(propPath);
                }
            }
        } else {
            checkoutIfNecessary(node);
            final Value jcrValue = this.createValue(node.getSession().getValueFactory(), value);
            node.setProperty(name, jcrValue);
            if (this.importListener != null) {
                this.importListener.onModify(node.getProperty(name).getPath());
            }
        }
    }

    private void createProperty(String name, Object[] values, boolean overwriteExisting) throws RepositoryException {
        final Node node = this.parentNodeStack.peek();
        // check if the property already exists, don't overwrite it in this case
        if (node.hasProperty(name) && !node.getProperty(name).isNew() && !overwriteExisting) {
            return;
        }
        if (values == null || values.length == 0) {
            if (node.hasProperty(name)) {
                checkoutIfNecessary(node);
                String propPath = node.getProperty(name).getPath();
                node.getProperty(name).remove();
                if (this.importListener != null) {
                    this.importListener.onDelete(propPath);
                }
            }
        } else {
            checkoutIfNecessary(node);
            final Value[] jcrValues = new Value[values.length];
            for (int i = 0; i < values.length; i++) {
                jcrValues[i] = this.createValue(node.getSession().getValueFactory(), values[i]);
            }
            node.setProperty(name, jcrValues);
            if (this.importListener != null) {
                this.importListener.onModify(node.getProperty(name).getPath());
            }
        }
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createFileAndResourceNode(java.lang.String, java.io.InputStream, java.lang.String, long)
     */
    public void createFileAndResourceNode(String name, InputStream data, String mimeType, long lastModified) throws RepositoryException {
        int lastSlash = name.lastIndexOf('/');
        name = (lastSlash < 0) ? name : name.substring(lastSlash + 1);
        final Node parentNode = this.parentNodeStack.peek();

        // if node already exists but should be overwritten, delete it
        if (parentNode.hasNode(name)) {
            this.parentNodeStack.push(parentNode.getNode(name));
            Node contentNode = parentNode.getNode(name).getNode("jcr:content");
            this.parentNodeStack.push(contentNode);
            long nodeLastModified = 0L;
            if (contentNode.hasProperty("jcr:lastModified")) {
                nodeLastModified = contentNode.getProperty("jcr:lastModified").getDate().getTimeInMillis();
            }
            if (!this.configuration.isOverwrite() && nodeLastModified >= lastModified) {
                return;
            }
            log.info("Updating {} lastModified:{} New Content LastModified:{}", new Object[]{parentNode.getNode(name).getPath(), new Date(nodeLastModified), new Date(lastModified)});
        } else {
            this.createNode(name, "nt:file", null);
            this.createNode("jcr:content", "nt:resource", null);
        }

        // ensure content type
        if (mimeType == null) {
            mimeType = jcrContentHelper.getMimeType(name);
            if (mimeType == null) {
                log.info("createFile: Cannot find content type for {}, using {}", name, DEFAULT_CONTENT_TYPE);
                mimeType = DEFAULT_CONTENT_TYPE;
            }
        }

        // ensure sensible last modification date
        if (lastModified <= 0) {
            lastModified = System.currentTimeMillis();
        }
        this.createProperty("jcr:mimeType", mimeType, true);
        this.createProperty("jcr:lastModified", lastModified, true);
        this.createProperty("jcr:data", data, true);
    }

    /**
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#switchCurrentNode(java.lang.String, java.lang.String)
     */
    public boolean switchCurrentNode(String subPath, String newNodeType) throws RepositoryException {
        if (subPath.startsWith("/")) {
            subPath = subPath.substring(1);
        }
        final StringTokenizer st = new StringTokenizer(subPath, "/");
        Node node = this.parentNodeStack.peek();
        while (st.hasMoreTokens()) {
            final String token = st.nextToken();
            if (!node.hasNode(token)) {
                if (newNodeType == null) {
                    return false;
                }
                checkoutIfNecessary(node);
                final Node n = node.addNode(token, newNodeType);
                addNodeToCreatedList(n);
                if (this.importListener != null) {
                    this.importListener.onCreate(node.getPath());
                }
            }
            node = node.getNode(token);
        }
        this.parentNodeStack.push(node);
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createGroup(java.lang.String, java.lang.String[], java.util.Map)
     */
    public void createGroup(final String name, String[] members, Map<String, Object> extraProperties) throws RepositoryException {

        final Node parentNode = this.parentNodeStack.peek();
        Session session = parentNode.getSession();

        UserManager userManager = AccessControlUtil.getUserManager(session);
        Authorizable authorizable = userManager.getAuthorizable(name);
        if (authorizable == null) {
            //principal does not exist yet, so create it
            Group group = userManager.createGroup(new Principal() {
                                                      public String getName() {
                                                          return name;
                                                      }
                                                  },
                hashPath(name)
            );
            authorizable = group;
        } else {
            //principal already exists, check to make sure it is the expected type
            if (!authorizable.isGroup()) {
                throw new RepositoryException("A user already exists with the requested name: " + name);
            }
            //group already exists so just update it below
        }
        //update the group members
        if (members != null) {
            Group group = (Group) authorizable;
            for (String member : members) {
                Authorizable memberAuthorizable = userManager.getAuthorizable(member);
                if (memberAuthorizable != null) {
                    group.addMember(memberAuthorizable);
                }
            }
        }
        if (extraProperties != null) {
            ValueFactory valueFactory = session.getValueFactory();
            Set<Entry<String, Object>> entrySet = extraProperties.entrySet();
            for (Entry<String, Object> entry : entrySet) {
                Value value = createValue(valueFactory, entry.getValue());
                authorizable.setProperty(entry.getKey(), value);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createUser(java.lang.String, java.lang.String, java.util.Map)
     */
    public void createUser(final String name, String password, Map<String, Object> extraProperties) throws RepositoryException {
        final Node parentNode = this.parentNodeStack.peek();
        Session session = parentNode.getSession();

        UserManager userManager = AccessControlUtil.getUserManager(session);
        Authorizable authorizable = userManager.getAuthorizable(name);
        if (authorizable == null) {
            //principal does not exist yet, so create it
            User user = userManager.createUser(name,
                password,
                new Principal() {
                    public String getName() {
                        return name;
                    }
                },
                hashPath(name)
            );
            authorizable = user;
        } else {
            //principal already exists, check to make sure it is the expected type
            if (authorizable.isGroup()) {
                throw new RepositoryException("A group already exists with the requested name: " + name);
            }
            //user already exists so just update it below
        }
        if (extraProperties != null) {
            ValueFactory valueFactory = session.getValueFactory();
            Set<Entry<String, Object>> entrySet = extraProperties.entrySet();
            for (Entry<String, Object> entry : entrySet) {
                Value value = createValue(valueFactory, entry.getValue());
                authorizable.setProperty(entry.getKey(), value);
            }
        }
    }

    /**
     * @param item
     * @return a parent path fragment for the item.
     */
    protected String hashPath(String item) throws RepositoryException {
        try {
            String hash = digest("sha1", (INSTANCE_SEED + item).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < STORAGE_LEVELS; i++) {
                sb.append(hash, i * 2, (i * 2) + 2).append("/");
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException("Unable to hash the path.", e);
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException("Unable to hash the path.", e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.sling.jcr.contentloader.internal.ContentCreator#createAce(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[])
	 */
    public void createAce(String principalId, String[] grantedPrivilegeNames, String[] deniedPrivilegeNames, String order) throws RepositoryException {
        final Node parentNode = this.parentNodeStack.peek();
        Session session = parentNode.getSession();
        PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
        Principal principal = principalManager.getPrincipal(principalId);
        if (principal == null) {
            throw new RepositoryException("No principal found for id: " + principalId);
        }
        String resourcePath = parentNode.getPath();

        if ((grantedPrivilegeNames != null) || (deniedPrivilegeNames != null)) {
            AccessControlUtil.replaceAccessControlEntry(session, resourcePath, principal, grantedPrivilegeNames, deniedPrivilegeNames, null, order);
        }
    }

    /**
     * used for the md5
     */
    private static final char[] hexTable = "0123456789abcdef".toCharArray();

    /**
     * Digest the plain string using the given algorithm.
     *
     * @param algorithm The alogrithm for the digest. This algorithm must be
     *                  supported by the MessageDigest class.
     * @param data      the data to digest with the given algorithm
     * @return The digested plain text String represented as Hex digits.
     * @throws java.security.NoSuchAlgorithmException if the desired algorithm is not supported by
     *                                                the MessageDigest class.
     */
    public static String digest(String algorithm, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] digest = md.digest(data);
        StringBuffer res = new StringBuffer(digest.length * 2);
        for (int i = 0; i < digest.length; i++) {
            byte b = digest[i];
            res.append(hexTable[(b >> 4) & 15]);
            res.append(hexTable[b & 15]);
        }
        return res.toString();
    }

    /**
     * Find an ancestor that is versionable
     */
    protected Node findVersionableAncestor(Node node) throws RepositoryException {
        if (node == null) {
            return null;
        } else if (isVersionable(node)) {
            return node;
        } else {
            try {
                node = node.getParent();
                return findVersionableAncestor(node);
            } catch (ItemNotFoundException e) {
                // top-level
                return null;
            }
        }
    }

    protected boolean isVersionable(Node node) throws RepositoryException {
        return node.isNodeType("mix:versionable");
    }

    /**
     * Checkout the node if needed
     */
    protected void checkoutIfNecessary(Node node) throws RepositoryException {
        if (this.configuration.isAutoCheckout()) {
            Node versionableNode = findVersionableAncestor(node);
            if (versionableNode != null) {
                if (!versionableNode.isCheckedOut()) {
                    versionableNode.checkout();
                    if (this.importListener != null) {
                        this.importListener.onCheckout(versionableNode.getPath());
                    }
                }
            }
        }
    }

}
