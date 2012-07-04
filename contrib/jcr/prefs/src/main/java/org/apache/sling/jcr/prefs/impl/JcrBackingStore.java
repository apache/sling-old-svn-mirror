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
package org.apache.sling.jcr.prefs.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.prefs.BackingStore;
import org.apache.felix.prefs.BackingStoreManager;
import org.apache.felix.prefs.PreferencesDescription;
import org.apache.felix.prefs.PreferencesImpl;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.prefs.BackingStoreException;

@Component
@Service(value=BackingStore.class)
@Properties({
    @org.apache.felix.scr.annotations.Property(name="sling.preferences.jcr.path", value="/preferences"),
    @org.apache.felix.scr.annotations.Property(name="sling.preferences.jcr.namespace",value="http://osgi.org/service/prefs/Preferences/1.1"),
    @org.apache.felix.scr.annotations.Property(name="sling.preferences.jcr.prefix", value="osgipref")
})
public class JcrBackingStore implements BackingStore {

    protected static final String NS_URI = "http://osgi.org/service/prefs/Preferences/1.1";

    protected static final String PATH_PROPERTY = "sling.preferences.jcr.path";
    protected static final String NAMESPACE_PROPERTY = "sling.preferences.jcr.namespace";
    protected static final String NAMESPACE_PREFIX_PROPERTY = "sling.preferences.jcr.prefix";

    @Reference
    protected SlingRepository repository;

    protected boolean initialized = false;

    protected String rootNodePath = "/preferences";

    protected String namespacePrefix = "osgipref";

    protected String namespacePrefixSep = this.namespacePrefix + ':';

    protected String namespace = NS_URI;

    protected Session getSession() throws RepositoryException {
        return this.repository.loginAdministrative(null);
    }

    /**
     * Activate this component and get the property configuration.
     * @param componentContext
     */
    protected void activate(ComponentContext componentContext) {
        Object value = componentContext.getProperties().get(PATH_PROPERTY);
        if ( value != null ) {
            this.rootNodePath = value.toString();
        }
        value = componentContext.getProperties().get(NAMESPACE_PROPERTY);
        if ( value != null ) {
            this.namespace = value.toString();
        }
        value = componentContext.getProperties().get(NAMESPACE_PREFIX_PROPERTY);
        if ( value != null ) {
            this.namespacePrefix = value.toString();
            this.namespacePrefixSep = this.namespacePrefix + ':';
        }
    }

    /**
     * Check if the repository is already initialized and return a fresh user session
     * This method should be called by every service method before anything else.
     */
    protected Session checkInitialized() {
        if ( !this.initialized ) {
            this.initRepository();
        }
        try {
            return this.repository.loginAdministrative(null);
        } catch (RepositoryException e) {
            throw new RuntimeException("Unable to login and create a new session.", e);
        }
    }

    protected void createNodePath(Node parent, String relPath) throws RepositoryException{
        int pos = relPath.indexOf('/');
        String nodeName = (pos == -1 ? relPath : relPath.substring(0, pos));
        if ( !parent.hasNode(nodeName) ) {
            parent.addNode(nodeName, "nt:unstructured");
        }
        if ( pos != -1 ) {
            this.createNodePath(parent.getNode(nodeName), relPath.substring(pos+1));
        }
    }

    /**
     * Initialize the repository:
     * - register this component as an event listener
     * - create the root node
     */
    protected synchronized void initRepository() {
        if ( !this.initialized ) {
            Session session = null;
            try {
                session = this.repository.loginAdministrative(null);
                if ( !session.itemExists(this.rootNodePath) ) {
                    this.createNodePath(session.getRootNode(), this.rootNodePath.substring(1));
                    session.save();
                }
                try {
                    String prefix = session.getWorkspace().getNamespaceRegistry().getPrefix(this.namespace);
                    this.namespacePrefix = prefix;
                    this.namespacePrefixSep = prefix + ':';
                } catch (NamespaceException ne) {
                    // mapping does not exist
                    session.getWorkspace().getNamespaceRegistry().registerNamespace(this.namespacePrefix, this.namespace);
                }
            } catch (RepositoryException e) {
                throw new RuntimeException("Unable to get root node or to create new root node.", e);
            } finally {
                if ( session != null ) {
                    session.logout();
                }
            }
            this.initialized = true;
        }
    }

    /**
     * Get the node path for the preferences tree.
     * @param desc
     * @return
     */
    protected String getNodePath(PreferencesImpl prefs) {
        final PreferencesDescription desc = prefs.getDescription();
        final StringBuilder buffer = new StringBuilder(this.rootNodePath);
        buffer.append('/');
        buffer.append(desc.getBundleId());
        buffer.append('/');
        if ( desc.getIdentifier() != null ) {
            buffer.append("users");
            buffer.append('/');
            buffer.append(desc.getIdentifier());
        } else {
            buffer.append("system");
        }
        buffer.append('/');
        buffer.append("preferences");
        final String absPath = prefs.absolutePath();
        if ( absPath.length() > 1 ) {
            buffer.append(absPath);
        }
        return buffer.toString();
    }

    /**
     * Get the node path for the preferences tree.
     * @param desc
     * @return
     */
    protected String getNodePath(PreferencesDescription desc) {
        final StringBuilder buffer = new StringBuilder(this.rootNodePath);
        buffer.append('/');
        buffer.append(desc.getBundleId());
        buffer.append('/');
        if ( desc.getIdentifier() != null ) {
            buffer.append("users");
            buffer.append('/');
            buffer.append(desc.getIdentifier());
        } else {
            buffer.append("system");
        }
        buffer.append('/');
        buffer.append("preferences");
        return buffer.toString();
    }

    /**
     * Load this preferences from a node.
     */
    @SuppressWarnings("unchecked")
    protected void readPreferences(PreferencesImpl prefs, Session session, Node node) throws RepositoryException {
        final PropertyIterator iterator = node.getProperties();
        while ( iterator.hasNext() ) {
            final Property prop = iterator.nextProperty();
            if ( prop.getName().startsWith(this.namespacePrefixSep) ) {
                prefs.getProperties().put(prop.getName(), prop.getString());
            }
        }
    }

    /**
     * Save this preferences to a node.
     */
    @SuppressWarnings("unchecked")
    protected void writePreferences(PreferencesImpl prefs, Session session) throws RepositoryException {
        final String path = this.getNodePath(prefs);
        if ( !session.itemExists(path) ) {
            this.createNodePath(session.getRootNode(), path.substring(1));
        }

        final Node node = (Node)session.getItem(path);
        // process the change set
        final Map properties = prefs.getProperties();
        Iterator<String> i;

        // remove properties
        i = prefs.getChangeSet().getRemovedProperties().iterator();
        while ( i.hasNext() ) {
            final String name = i.next();
            if ( node.hasProperty(this.namespacePrefixSep + name) ) {
                node.getProperty(this.namespacePrefixSep + name).remove();
            }
        }
        // update properties
        i = prefs.getChangeSet().getChangedProperties().iterator();
        while ( i.hasNext() ) {
            final String name = i.next();
            node.setProperty(this.namespacePrefixSep + name, (String)properties.get(name));
        }
        // remove children
        i = prefs.getChangeSet().getRemovedChildren().iterator();
        while ( i.hasNext() ) {
            final String name = i.next();
            if ( node.hasNode(name) ) {
                node.getNode(name).remove();
            }
        }
        // adding children is done by writing the child preferences!

        session.save();
    }

    /**
     * Read the preferences from the repository.
     */
    protected void readTree(PreferencesImpl prefs, Session session, Node node)
    throws RepositoryException {
        this.readPreferences(prefs, session, node);
        final NodeIterator iterator = node.getNodes();
        while ( iterator.hasNext() ) {
            final Node current = iterator.nextNode();
            final PreferencesImpl impl = (PreferencesImpl)prefs.node(current.getName());
            this.readTree(impl, session, current);
        }
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#availableBundles()
     */
    public Long[] availableBundles() {
        final Session session = this.checkInitialized();
        Long[] result = new Long[0];
        try {
            try {
                final List<Long> bundleIds = new ArrayList<Long>();
                final NodeIterator iterator = session.getRootNode().getNodes(this.rootNodePath);
                while ( iterator.hasNext() ) {
                    final Node current = iterator.nextNode();
                    try {
                        final Long id = Long.valueOf(current.getName());
                        bundleIds.add(id);
                    } catch (NumberFormatException nfe) {
                        // we ignore this as this just indicates a wrong node in the tree
                    }
                }
                result = bundleIds.toArray(new Long[bundleIds.size()]);
            } catch (RepositoryException e) {
                // we ignore this for now
            }
        } finally {
            session.logout();
        }
        return result;
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#load(org.apache.felix.prefs.BackingStoreManager, org.apache.felix.prefs.PreferencesDescription)
     */
    public PreferencesImpl load(BackingStoreManager manager, PreferencesDescription desc) throws BackingStoreException {
        final Session session = this.checkInitialized();
        try {
            final String path = this.getNodePath(desc);
            if ( session.itemExists(path) ) {
                final PreferencesImpl root = new PreferencesImpl(desc, manager);
                this.readTree(root, session, (Node)session.getItem(path));

                return root;
            }
        } catch (RepositoryException re) {
            throw new BackingStoreException("Unable to load preferences.", re);
        } finally {
            session.logout();
        }
        return null;
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#loadAll(org.apache.felix.prefs.BackingStoreManager, java.lang.Long)
     */
    public PreferencesImpl[] loadAll(BackingStoreManager manager, Long bundleId) throws BackingStoreException {
        final Session session = this.checkInitialized();
        try {
            final List<PreferencesImpl> list = new ArrayList<PreferencesImpl>();
            // check for system preferences
            final PreferencesDescription systemDesc = new PreferencesDescription(bundleId, null);
            final String systemPath = this.getNodePath(systemDesc);
            if ( session.itemExists(systemPath) ) {
                final Node rootNode = (Node)session.getItem(systemPath);
                final PreferencesImpl root = new PreferencesImpl(systemDesc, manager);
                this.readTree(root, session, rootNode);
            }
            // user preferences
            final String userPath = this.rootNodePath + '/' + bundleId + '/' + "users";
            if ( session.itemExists(userPath) ) {
                final NodeIterator iterator = ((Node)session.getItem(userPath)).getNodes();
                while ( iterator.hasNext() ) {
                    final Node current = iterator.nextNode();
                    final PreferencesDescription desc = new PreferencesDescription(bundleId, current.getName());
                    final PreferencesImpl root = new PreferencesImpl(desc, manager);

                    this.readTree(root, session, current);

                    list.add(root);
                }
            }
            return list.toArray(new PreferencesImpl[list.size()]);
        } catch (RepositoryException re) {
            throw new BackingStoreException("Unable to load preferences.", re);
        } finally {
            session.logout();
        }
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#remove(java.lang.Long)
     */
    public void remove(Long bundleId) throws BackingStoreException {
        final Session session = this.checkInitialized();
        try {
            final String nodePath = this.rootNodePath + '/' + bundleId;
            if ( session.itemExists(nodePath) ) {
                final Node node = ((Node)session).getNode(nodePath);
                final Node parent = node.getParent();
                node.remove();
                parent.save();
            }
        } catch (RepositoryException re) {
            throw new BackingStoreException("Unable to remove preferences.", re);
        } finally {
            session.logout();
        }
    }

    /**
     * @see org.apache.felix.prefs.BackingStore#store(org.apache.felix.prefs.PreferencesImpl)
     */
    public void store(PreferencesImpl prefs) throws BackingStoreException {
        final Session session = this.checkInitialized();
        try {
            this.store(prefs, session);
        } catch (RepositoryException re) {
            throw new BackingStoreException("Unable to store preferences.", re);
        } finally {
            session.logout();
        }
    }

    protected void store(PreferencesImpl prefs, Session session)
    throws BackingStoreException, RepositoryException {
        // do we need to store at all?
        if ( prefs.getChangeSet().hasChanges() ) {
            // load existing data
            final PreferencesImpl savedData = this.load(prefs.getBackingStoreManager(), prefs.getDescription());
            if ( savedData != null ) {
                // merge with saved version
                final PreferencesImpl n = savedData.getOrCreateNode(prefs.absolutePath());
                n.applyChanges(prefs);
                prefs = n;
            }
            this.writePreferences(prefs, session);
        }
        // now process children
        @SuppressWarnings("unchecked")
        final Iterator<PreferencesImpl> i = prefs.getChildren().iterator();
        while ( i.hasNext() ) {
            final PreferencesImpl p = i.next();
            this.store(p, session);
        }

    }

    /**
     * @see org.apache.felix.prefs.BackingStore#update(org.apache.felix.prefs.PreferencesImpl)
     */
    public void update(PreferencesImpl prefs) throws BackingStoreException {
        final PreferencesImpl root = this.load(prefs.getBackingStoreManager(), prefs.getDescription());
        if ( root != null ) {
            // and now update
            if ( root.nodeExists(prefs.absolutePath()) ) {
                final PreferencesImpl updated = (PreferencesImpl)root.node(prefs.absolutePath());
                prefs.update(updated);
            }
        }
    }
}
