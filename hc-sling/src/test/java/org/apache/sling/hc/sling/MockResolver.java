/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.sling;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class MockResolver implements ResourceResolver {

    private final List<MockResource> resources = new ArrayList<MockResource>();
    
    @Mock
    private Session session;

    void addResource(MockResource r) {
        resources.add(r);
    }
    
    MockResolver() {
        MockitoAnnotations.initMocks(this);
    }
    
    void setUnauthorized() throws RepositoryException {
        Mockito.doThrow(RepositoryException.class).when(session).checkPermission(Matchers.anyString(), Matchers.anyString());
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> target) {
        if(target == Session.class) {
            return (AdapterType)session;
        }
        return null;
    }

    @Override
    public Iterator<Resource> findResources(String arg0, String arg1) {
        return null;
    }

    @Override
    public Resource getResource(Resource arg0, String arg1) {
        return null;
    }

    @Override
    public Resource getResource(String arg0) {
        return null;
    }

    @Override
    public String[] getSearchPath() {
        return null;
    }

    @Override
    public Iterator<Resource> listChildren(Resource r) {
        final List<Resource> kids = new ArrayList<Resource>();
        for(Resource kid : resources) {
            if(kid.getPath().startsWith(r.getPath()) && kid.getPath().length() > r.getPath().length()) {
                kids.add(kid);
            }
        }
        return kids.iterator();
    }

    @Override
    public String map(HttpServletRequest arg0, String arg1) {
        return null;
    }

    @Override
    public String map(String arg0) {
        return null;
    }

    @Override
    public Iterator<Map<String, Object>> queryResources(String arg0, String arg1) {
        return null;
    }

    @Override
    public Resource resolve(HttpServletRequest arg0, String arg1) {
        return null;
    }

    @Override
    @Deprecated
    public Resource resolve(HttpServletRequest arg0) {
        return null;
    }

    @Override
    public Resource resolve(String arg0) {
        return null;
    }

    @Override
    public ResourceResolver clone(Map<String, Object> arg0)
            throws LoginException {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public Object getAttribute(String arg0) {
        return null;
    }

    @Override
    public Iterator<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getUserID() {
        return null;
    }

    @Override
    public boolean isLive() {
        return false;
    }
}
