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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mongodb.DB;

public class MongoDBContext {

    /** The roots. */
    private final String[] roots;

    /** The roots ended by a slash. */
    private final String[] rootsWithSlash;

    /** Don't show these collections. */
    private final Set<String> filterCollectionNames = new HashSet<String>();

    /** The database to be used. */
    private final DB database;

    public MongoDBContext(final DB database,
                    final String[] configuredRoots,
                    final String[] configuredFilterCollectionNames) {
        this.database = database;
        if ( configuredRoots != null ) {
            final List<String> rootsList = new ArrayList<String>();
            final List<String> rootsWithSlashList = new ArrayList<String>();
            for(final String r : configuredRoots) {
                if ( r != null ) {
                    final String value = r.trim();
                    if ( value.length() > 0 ) {
                        if ( value.endsWith("/") ) {
                            rootsWithSlashList.add(value);
                            rootsList.add(value.substring(0, value.length() - 1));
                        } else {
                            rootsWithSlashList.add(value + "/");
                            rootsList.add(value);
                        }
                    }
                }
            }
            this.roots = rootsList.toArray(new String[rootsList.size()]);
            this.rootsWithSlash = rootsWithSlashList.toArray(new String[rootsWithSlashList.size()]);
        } else {
            this.roots = new String[0];
            this.rootsWithSlash = new String[0];
        }
        if ( configuredFilterCollectionNames != null ) {
            for(final String name : configuredFilterCollectionNames) {
                this.filterCollectionNames.add(name);
            }
        }
    }

    public String[] getRoots() {
        return roots;
    }

    public String[] getRootsWithSlash() {
        return this.rootsWithSlash;
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
}
