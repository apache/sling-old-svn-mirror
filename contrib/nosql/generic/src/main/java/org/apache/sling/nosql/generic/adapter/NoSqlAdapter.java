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
package org.apache.sling.nosql.generic.adapter;

import java.util.Iterator;

import org.apache.sling.api.resource.LoginException;

import aQute.bnd.annotation.ConsumerType;

/**
 * Adapter for NoSQL databases to be hooked into the Generic NoSQL resource provider.
 * All implementors should should extend {@link AbstractNoSqlAdapter} to be compatible for future extensions.
 */
@ConsumerType
public interface NoSqlAdapter {
    
    /**
     * True if the given path is valid and supported by the NoSQL database.
     * @param path Path
     * @return true if valid, false if invalid
     */
    boolean validPath(String path);

    /**
     * Get data for a single resource from NoSQL database.
     * @param path Path
     * @return Data or null if non exists
     */
    NoSqlData get(String path);

    /**
     * Get data for all children of a resource from NoSQL database.
     * @param parentPath Parent path
     * @return List if child data or empty iterator
     */
    Iterator<NoSqlData> getChildren(String parentPath);
    
    /**
     * Store data with the given path in NoSQL database.
     * It is guaranteed that the map of the NoSqlData object does only contain primitive
     * value types String, Integer, Long, Double, Boolean or arrays of them.
     * @param data Data with path
     * @return true if a new entry was created, false if an existing was overridden.
     */
    boolean store(NoSqlData data);
    
    /**
     * Remove data including all path-related children from NoSQL database.
     * @param path Path to remove
     * @return true if anything was removed
     */
    boolean deleteRecursive(String path);

    /**
     * Query for data.
     * @param query Query
     * @param language Query language
     * @return Query result or null if query not supported
     */
    Iterator<NoSqlData> query(String query, String language);

    /**
     * Checks whether the connection to the NoSQL database is possible
     *
     * @throws LoginException in case of any errors
     */
    void checkConnection() throws LoginException;

}
