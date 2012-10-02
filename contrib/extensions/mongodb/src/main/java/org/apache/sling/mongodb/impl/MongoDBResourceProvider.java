/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.mongodb.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

/**
 * The MongoDB resource provider creates resources based on MongoDB entries.
 * The resources contain all properties stored in the MongoDB except those starting with a "_".
 */
public class MongoDBResourceProvider implements ResourceProvider, ModifyingResourceProvider {

    /** The special path property containing the (relative) path of the resource in the tree. */
    public static final String PROP_PATH = "_path";

    /** The id property. */
    public static final String PROP_ID = "_id";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The global context .*/
    private final MongoDBContext context;

    private final Map<String, MongoDBResource> changedResources = new HashMap<String, MongoDBResource>();

    private final Set<String> deletedResources = new HashSet<String>();

    public MongoDBResourceProvider(final MongoDBContext context) {
        this.context = context;
    }

    public static String propNameToKey(final String name) {
        if ( name.startsWith("_") ) {
            return "_" + name;
        }
        return name;
    }

    public static String keyToPropName(final String key) {
        if ( key.startsWith("__") ) {
            return key.substring(1);
        } else if ( key.startsWith("_") ) {
            return null;
        }
        return key;
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#create(org.apache.sling.api.resource.ResourceResolver, java.lang.String, java.util.Map)
     */
    public Resource create(final ResourceResolver resolver, final String path, final Map<String, Object> properties)
            throws PersistenceException {
        final String[] info = this.extractResourceInfo(path);
        if ( info != null && info.length == 2) {
            final boolean deleted = this.deletedResources.remove(path);
            final MongoDBResource oldResource = (MongoDBResource)this.getResource(resolver, path, info);
            if ( !deleted && oldResource != null ) {
                throw new PersistenceException("Resource already exists at " + path, null, path, null);
            }
            final DBObject dbObj = new BasicDBObject();
            dbObj.put(PROP_PATH, info[1]);
            if ( properties != null ) {
                for(Map.Entry<String, Object> entry : properties.entrySet()) {
                    final String key = propNameToKey(entry.getKey());
                    dbObj.put(key, entry.getValue());
                }
            }
            if ( deleted && oldResource != null ) {
                dbObj.put(PROP_ID, oldResource.getProperties().get(PROP_ID));
            }
            final MongoDBResource rsrc = new MongoDBResource(resolver, path, info[0], dbObj, this);
            this.changedResources.put(path, rsrc);

            return rsrc;
        }
        throw new PersistenceException("Illegal path - unable to create resource at " + path, null, path, null);
    }

    /**
     * TODO - we should handle delete different and not put all child resources into the
     * deleted set.
     * Instead when getting resources, the parents of the resource should be checked
     * first.
     * This minimizes concurrency issues.
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#delete(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    public void delete(final ResourceResolver resolver, final String path)
            throws PersistenceException {
        final String[] info = this.extractResourceInfo(path);
        if ( info != null ) {
            boolean deletedResource = false;
            if ( !deletedResources.contains(path) ) {
                final Resource rsrc = this.getResource(resolver, path, info);
                if ( rsrc instanceof MongoDBResource ) {
                    this.deletedResources.add(path);
                    this.changedResources.remove(path);

                    final DBCollection col = this.getCollection(info[0]);
                    final String pattern = "^" + Pattern.quote(info[1]) + "/";

                    final DBObject query = QueryBuilder.start(PROP_PATH).regex(Pattern.compile(pattern)).get();
                    final DBCursor cur = col.find(query);
                    while ( cur.hasNext() ) {
                        final DBObject dbObj = cur.next();
                        final String childPath = info[0] + '/' + dbObj.get(PROP_PATH);
                        this.deletedResources.add(childPath);
                        this.changedResources.remove(childPath);
                    }
                    deletedResource = true;
                }
            } else {
                deletedResource = true;
            }
            if ( deletedResource ) {
                final String prefix = path + "/";
                final Iterator<Map.Entry<String, MongoDBResource>> i = this.changedResources.entrySet().iterator();
                while ( i.hasNext() ) {
                    final Map.Entry<String, MongoDBResource> entry = i.next();
                    if ( entry.getKey().startsWith(prefix) ) {
                        i.remove();
                    }
                }
                return;
            }

        }
        throw new PersistenceException("Unable to delete resource at {}" + path, null, path, null);
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#revert(ResourceResolver)
     */
    public void revert(final ResourceResolver resolver) {
        this.changedResources.clear();
        this.deletedResources.clear();
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#commit(ResourceResolver)
     */
    public void commit(final ResourceResolver resolver) throws PersistenceException {
        try {
            for(final String deleted : this.deletedResources) {
                final String[] info = this.extractResourceInfo(deleted);

                // check if the collection still exists
                final DBCollection col = this.getCollection(info[0]);
                if ( col != null ) {
                    if ( col.findAndRemove(QueryBuilder.start(PROP_PATH).is(info[1]).get()) != null ) {
                        this.context.notifyRemoved(info);
                    }
                }
            }
            for(final MongoDBResource changed : this.changedResources.values()) {

                final DBCollection col = this.context.getDatabase().getCollection(changed.getCollection());
                if ( col != null ) {
                    final String[] info = new String[] {changed.getCollection(),
                            changed.getProperties().get(PROP_PATH).toString()};
                    // create or update?
                    if ( changed.getProperties().get(PROP_ID) != null ) {
                        col.update(QueryBuilder.start(PROP_PATH).is(changed.getProperties().get(PROP_PATH)).get(),
                                changed.getProperties());
                        this.context.notifyUpdated(info);
                    } else {
                        // create
                        col.save(changed.getProperties());
                        this.context.notifyUpdated(info);
                    }
                } else {
                    throw new PersistenceException("Unable to create collection " + changed.getCollection(), null, changed.getPath(), null);
                }
            }
        } finally {
            this.revert(resolver);
        }
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#hasChanges(ResourceResolver)
     */
    public boolean hasChanges(final ResourceResolver resolver) {
        return this.changedResources.size() > 0 || this.deletedResources.size() > 0;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    public Resource getResource(final ResourceResolver resourceResolver, final String path) {
        if ( this.deletedResources.contains(path) ) {
            return null;
        }
        if ( this.changedResources.containsKey(path) ) {
            return this.changedResources.get(path);
        }
        final String[] info = this.extractResourceInfo(path);
        if ( info != null ) {
            return this.getResource(resourceResolver, path, info);
        }
        return null;
    }

    /**
     * Inform about changes of a resource.
     */
    public void changed(final MongoDBResource resource) {
        this.deletedResources.remove(resource.getPath());
        this.changedResources.put(resource.getPath(), resource);
    }

    /**
     * TODO - we have to check for deleted and added resources
     * @see org.apache.sling.api.resource.ResourceProvider#listChildren(org.apache.sling.api.resource.Resource)
     */
    public Iterator<Resource> listChildren(final Resource parent) {
        final String[] info = this.extractResourceInfo(parent.getPath());
        if ( info != null ) {
            if ( info.length == 0 ) {
                // all collections
                final Set<String> names = new HashSet<String>(context.getDatabase().getCollectionNames());
                names.removeAll(this.context.getFilterCollectionNames());
                final Iterator<String> i = names.iterator();
                return new Iterator<Resource>() {

                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    public Resource next() {
                        final String name = i.next();
                        return new MongoDBCollectionResource(parent.getResourceResolver(), parent.getPath() + '/' + name);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }

                };
            }
            final DBCollection col = this.getCollection(info[0]);
            if ( col != null ) {
                final String pattern;
                if ( info.length == 1 ) {
                    pattern = "^([^/])*$";
                } else {
                    pattern = "^" + Pattern.quote(info[1]) + "/([^/])*$";
                }

                final DBObject query = QueryBuilder.start(PROP_PATH).regex(Pattern.compile(pattern)).get();
                final DBCursor cur = col.find(query).
                        sort(BasicDBObjectBuilder.start(PROP_PATH, 1).get());
                return new Iterator<Resource>() {

                    public boolean hasNext() {
                        return cur.hasNext();
                    }

                    public Resource next() {
                        final DBObject obj = cur.next();
                        final String objPath = obj.get(PROP_PATH).toString();
                        final int lastSlash = objPath.lastIndexOf('/');
                        final String name;
                        if (lastSlash == -1) {
                            name = objPath;
                        } else {
                            name = objPath.substring(lastSlash + 1);
                        }
                        return new MongoDBResource(parent.getResourceResolver(),
                                parent.getPath() + '/' + name,
                                info[0],
                                obj,
                                MongoDBResourceProvider.this);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }

                };
            }
        }
        return null;
    }

    /**
     * @see org.apache.sling.api.resource.ResourceProvider#getResource(org.apache.sling.api.resource.ResourceResolver, javax.servlet.http.HttpServletRequest, java.lang.String)
     */
    @SuppressWarnings("javadoc")
    public Resource getResource(final ResourceResolver resourceResolver,
            final HttpServletRequest request,
            final String path) {
        return this.getResource(resourceResolver, path);
    }

    /**
     * Extract info about collection and path
     */
    private String[] extractResourceInfo(final String path) {
        if ( path.startsWith(this.context.getRootWithSlash()) ) {
            if ( path.length() == this.context.getRootWithSlash().length() ) {
                // special resource - show all collections
                return new String[0];
            }
            final String info = path.substring(this.context.getRootWithSlash().length());
            final int slashPos = info.indexOf('/');
            if ( slashPos != -1 ) {
                return new String[] {info.substring(0, slashPos), info.substring(slashPos + 1)};
            }
            // special resource - collection
            return new String[] {info};
        }

        if ( path.equals(this.context.getRoot()) ) {
            // special resource - show all collections
            return new String[0];
        }

        return null;
    }

    /**
     * Check if a collection with a given name exists
     */
    private boolean hasCollection(final String name) {
        final Set<String> names = this.context.getDatabase().getCollectionNames();
        return names.contains(name) && !this.context.isFilterCollectionName(name);
    }

    /**
     * Check if a collection with a given name exists and return it
     */
    private DBCollection getCollection(final String name) {
        if ( this.hasCollection(name) ) {
            return this.context.getDatabase().getCollection(name);
        }
        return null;
    }

    /**
     * Get a resource
     */
    private Resource getResource(final ResourceResolver resourceResolver, final String path, final String[] info) {
        if ( info.length == 0 ) {
            // special resource : all collections
            return new MongoDBCollectionResource(resourceResolver, path);
        } else if ( info.length == 1 ) {
            // special resource : collection
            if ( this.hasCollection(info[0]) ) {
                return new MongoDBCollectionResource(resourceResolver, path);
            }
            return null;
        }
        logger.debug("Searching {} in {}", info[1], info[0]);
        final DBCollection col = this.getCollection(info[0]);
        if ( col != null ) {
            final DBObject obj = col.findOne(QueryBuilder.start(PROP_PATH).is(info[1]).get());
            logger.debug("Found {}", obj);
            if ( obj != null ) {
                return new MongoDBResource(resourceResolver,
                        path,
                        info[0],
                        obj,
                        this);
            }
        }

        return null;
    }

    /**
     * Check if there is a newer db object for that path.
     */
    public DBObject getUpdatedDBObject(final String path, final DBObject dbObj) {
        final MongoDBResource stored = this.changedResources.get(path);
        if ( stored != null ) {
            return stored.getProperties();
        }
        return dbObj;
    }
}
