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
package org.apache.sling.testing.resourceresolver;

import org.osgi.service.event.EventAdmin;

/**
 * Options for the factory
 */
public class MockResourceResolverFactoryOptions {

    private EventAdmin eventAdmin;

    private String[] searchPaths = new String[] {"/apps/", "/libs/"};
    
    private boolean mangleNamespacePrefixes;

    public EventAdmin getEventAdmin() {
        return eventAdmin;
    }

    public MockResourceResolverFactoryOptions setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
        return this;
    }

    public String[] getSearchPaths() {
        return searchPaths;
    }

    public MockResourceResolverFactoryOptions setSearchPaths(String[] searchPaths) {
        if ( searchPaths == null ) {
            searchPaths = new String[] {};
        }
        this.searchPaths = searchPaths;
        return this;
    }

    public boolean isMangleNamespacePrefixes() {
        return mangleNamespacePrefixes;
    }

    public void setMangleNamespacePrefixes(boolean mangleNamespacePrefixes) {
        this.mangleNamespacePrefixes = mangleNamespacePrefixes;
    }
    
}
