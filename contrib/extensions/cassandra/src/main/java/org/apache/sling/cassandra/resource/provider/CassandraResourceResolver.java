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

package org.apache.sling.cassandra.resource.provider;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Map;

public class CassandraResourceResolver implements ResourceResolver {

    public Resource resolve(HttpServletRequest httpServletRequest, String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Resource resolve(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Resource resolve(HttpServletRequest httpServletRequest) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String map(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String map(HttpServletRequest httpServletRequest, String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Resource getResource(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Resource getResource(Resource resource, String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String[] getSearchPath() {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Iterator<Resource> listChildren(Resource resource) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Iterable<Resource> getChildren(Resource resource) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Iterator<Resource> findResources(String s, String s1) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Iterator<Map<String, Object>> queryResources(String s, String s1) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ResourceResolver clone(Map<String, Object> stringObjectMap) throws LoginException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isLive() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getUserID() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Iterator<String> getAttributeNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getAttribute(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void delete(Resource resource) throws PersistenceException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Resource create(Resource resource, String s, Map<String, Object> stringObjectMap) throws PersistenceException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void revert() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void commit() throws PersistenceException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean hasChanges() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getParentResourceType(Resource resource) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getParentResourceType(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isResourceType(Resource resource, String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void refresh() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> adapterTypeClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
