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

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.sling.api.SlingConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.mongodb.DB;

public class MongoDBContext {

    /** The roots. */
    private final String root;

    /** The roots ended by a slash. */
    private final String rootWithSlash;

    /** Don't show these collections. */
    private final Set<String> filterCollectionNames = new HashSet<String>();

    /** The database to be used. */
    private final DB database;

    private final EventAdmin eventAdmin;

    public MongoDBContext(final DB database,
            final String configuredRoot,
            final String[] configuredFilterCollectionNames,
            final EventAdmin eventAdmin) {
        this.database = database;
        if ( configuredRoot != null ) {
            final String value = configuredRoot.trim();
            if ( value.length() > 0 ) {
                if ( value.endsWith("/") ) {
                    this.rootWithSlash = configuredRoot;
                    this.root = configuredRoot.substring(0, configuredRoot.length() - 1);
                } else {
                    this.rootWithSlash = configuredRoot + "/";
                    this.root = configuredRoot;
                }
            } else {
                this.root = "";
                this.rootWithSlash = "/";
            }
        } else {
            this.root = "";
            this.rootWithSlash = "/";
        }
        if ( configuredFilterCollectionNames != null ) {
            for(final String name : configuredFilterCollectionNames) {
                this.filterCollectionNames.add(name);
            }
        }
        this.eventAdmin = eventAdmin;
    }

    public String getRoot() {
        return root;
    }

    public String getRootWithSlash() {
        return this.rootWithSlash;
    }

    public boolean isFilterCollectionName(final String name) {
        return this.filterCollectionNames.contains(name);
    }

    public Set<String> getFilterCollectionNames() {
        return this.filterCollectionNames;
    }

    public DB getDatabase() {
        return this.database;
    }

    public void notifyRemoved(final String[] info) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(SlingConstants.PROPERTY_PATH, this.rootWithSlash + info[0] + '/' + info[1]);
        props.put("event.distribute", "");
        final Event event = new Event(SlingConstants.TOPIC_RESOURCE_REMOVED, props);
        this.eventAdmin.postEvent(event);
    }

    public void notifyAddeed(final String[] info) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(SlingConstants.PROPERTY_PATH, this.rootWithSlash + info[0] + '/' + info[1]);
        props.put("event.distribute", "");
        final Event event = new Event(SlingConstants.TOPIC_RESOURCE_ADDED, props);
        this.eventAdmin.postEvent(event);
    }

    public void notifyUpdated(final String[] info) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(SlingConstants.PROPERTY_PATH, this.rootWithSlash + info[0] + '/' + info[1]);
        props.put("event.distribute", "");
        final Event event = new Event(SlingConstants.TOPIC_RESOURCE_CHANGED, props);
        this.eventAdmin.postEvent(event);
    }    
}
