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

package org.apache.sling.servlets.post.impl.operations;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.impl.helper.MockSlingHttpServlet3Request;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class StreamingUploadOperationTest {

    private static final Logger LOG = LoggerFactory.getLogger(StreamingUploadOperationTest.class);
    private StreamedUploadOperation streamedUplodOperation;

    @Before
    public void before() {
        streamedUplodOperation = new StreamedUploadOperation();

    }

    @After
    public void after() {

    }

    @Test
    public void test() throws RepositoryException, UnsupportedEncodingException {
        List<Modification> changes = new ArrayList<Modification>();
        PostResponse response = new AbstractPostResponse() {
            @Override
            protected void doSend(HttpServletResponse response) throws IOException {

            }

            @Override
            public void onChange(String type, String... arguments) {

            }

            @Override
            public String getPath() {
                return "/test/upload/location";
            }
        };

        List<Part> partsList = new ArrayList<Part>();
        partsList.add(new MockPart("formfield1", null, null, 0, new ByteArrayInputStream("testformfield1".getBytes("UTF-8")), Collections.EMPTY_MAP));
        partsList.add(new MockPart("formfield2", null, null, 0, new ByteArrayInputStream("testformfield2".getBytes("UTF-8")), Collections.EMPTY_MAP));
        partsList.add(new MockPart("test1.txt", "text/plain", "test1bad.txt", 4, new ByteArrayInputStream("test".getBytes("UTF-8")), Collections.EMPTY_MAP));
        partsList.add(new MockPart("*", "text/plain2", "test2.txt", 8, new ByteArrayInputStream("test1234".getBytes("UTF-8")), Collections.EMPTY_MAP));
        partsList.add(new MockPart("badformfield2", null, null, 0, new ByteArrayInputStream("testbadformfield2".getBytes("UTF-8")), Collections.EMPTY_MAP));
        final Iterator<Part> partsIterator = partsList.iterator();
        final Map<String, Resource> repository = new HashMap<String, Resource>();
        final ResourceResolver resourceResolver = new MockResourceResolver() {
            @Override
            public Resource getResource(String path) {

                Resource resource = repository.get(path);

                if ( resource == null ) {
                    if ( "/test/upload/location".equals(path)) {
                        resource =  new MockRealResource(this, path, "sling:Folder");
                        repository.put(path,resource);
                        LOG.debug("Created {} ", path);

                    }
                }
                LOG.debug("Resource {} is {} {}", path, resource, ResourceUtil.isSyntheticResource(resource));
                return resource;
            }




            @Override
            public Iterable<Resource> getChildren(Resource resource) {
                return null;
            }

            @Override
            public void delete(Resource resource) throws PersistenceException {

            }

            @Override
            public Resource create(Resource resource, String s, Map<String, Object> map) throws PersistenceException {
                Resource childResource = resource.getChild(s);
                if ( childResource != null) {
                    throw new IllegalArgumentException("Child "+s+" already exists ");
                }
                Resource newResource = new MockRealResource(this, resource.getPath()+"/"+s, (String)map.get("sling:resourceType"), map);
                repository.put(newResource.getPath(), newResource);
                return newResource;
            }

            @Override
            public void revert() {

            }

            @Override
            public void commit() throws PersistenceException {
                LOG.debug("Committing");
                for(Map.Entry<String, Resource> e : repository.entrySet()) {
                    LOG.debug("Committing {} ", e.getKey());
                    Resource r = e.getValue();
                    ModifiableValueMap vm = r.adaptTo(ModifiableValueMap.class);
                    for (Map.Entry<String, Object> me : vm.entrySet()) {
                        if (me.getValue() instanceof InputStream) {
                            try {
                                String value = IOUtils.toString((InputStream) me.getValue());
                                LOG.debug("Converted {} {}  ", me.getKey(), value);
                                vm.put(me.getKey(), value);

                            } catch (IOException e1) {
                                throw new PersistenceException("Failed to commit input stream", e1);
                            }
                        }
                    }
                    LOG.debug("Converted {} ", vm);
                }
                LOG.debug("Committted {} ", repository);


            }

            @Override
            public boolean hasChanges() {
                return false;
            }
        };

        SlingHttpServletRequest request = new MockSlingHttpServlet3Request(null, null, null, null, null) {
            @Override
            public Object getAttribute(String name) {
                if ( "request-parts-iterator".equals(name)) {
                    return partsIterator;
                }
                return super.getAttribute(name);
            }

            @Override
            public ResourceResolver getResourceResolver() {
                return resourceResolver;
            }
        };
        streamedUplodOperation.doRun(request, response, changes);


        {
            Resource r = repository.get("/test/upload/location/test1.txt");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);


            Assert.assertEquals("nt:file", m.get("jcr:primaryType"));

        }
        {
            Resource r = repository.get("/test/upload/location/test1.txt/jcr:content");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);

            Assert.assertEquals("nt:resource", m.get("jcr:primaryType"));
            Assert.assertTrue(m.get("jcr:lastModified") instanceof Calendar);
            Assert.assertEquals("text/plain", m.get("jcr:mimeType"));
            Assert.assertEquals("test", m.get("jcr:data"));

        }
        {
            Resource r = repository.get("/test/upload/location/test2.txt");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);


            Assert.assertEquals("nt:file", m.get("jcr:primaryType"));

        }
        {
            Resource r = repository.get("/test/upload/location/test2.txt/jcr:content");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);


            Assert.assertEquals("nt:resource", m.get("jcr:primaryType"));
            Assert.assertTrue(m.get("jcr:lastModified") instanceof Calendar);
            Assert.assertEquals("text/plain2", m.get("jcr:mimeType"));
            Assert.assertEquals("test1234", m.get("jcr:data"));
        }


    }

    @Test
    public void testParts() throws RepositoryException, UnsupportedEncodingException {
        List<Modification> changes = new ArrayList<Modification>();
        PostResponse response = new AbstractPostResponse() {
            @Override
            protected void doSend(HttpServletResponse response) throws IOException {

            }

            @Override
            public void onChange(String type, String... arguments) {

            }

            @Override
            public String getPath() {
                return "/test/upload/location";
            }
        };

        List<Part> partsList = new ArrayList<Part>();
        partsList.add(new MockPart("test1.txt@Length", null, null, 0, new ByteArrayInputStream("8".getBytes("UTF-8")), Collections.EMPTY_MAP));
        partsList.add(new MockPart("test1.txt@Offset", null, null, 0, new ByteArrayInputStream("0".getBytes("UTF-8")), Collections.EMPTY_MAP));
        partsList.add(new MockPart(
                "test1.txt",
                "text/plain",
                "test1bad.txt",
                4,
                new ByteArrayInputStream("test".getBytes("UTF-8")),
                mapOf("Content-Length", "4")));
        partsList.add(new MockPart("test1.txt@Offset", null, null, 0, new ByteArrayInputStream("4".getBytes("UTF-8")), Collections.EMPTY_MAP));
        partsList.add(new MockPart(
                "test1.txt",
                "text/plain",
                "test1bad.txt",
                4,
                new ByteArrayInputStream("part".getBytes("UTF-8")),
                mapOf("Content-Length", "4")));
        partsList.add(new MockPart("*", "text/plain2", "test2.txt", 8, new ByteArrayInputStream("test1234".getBytes("UTF-8")), Collections.EMPTY_MAP));
        partsList.add(new MockPart("badformfield2", null, null, 0, new ByteArrayInputStream("testbadformfield2".getBytes("UTF-8")), Collections.EMPTY_MAP));
        final Iterator<Part> partsIterator = partsList.iterator();
        final Map<String, Resource> repository = new HashMap<String, Resource>();
        final ResourceResolver resourceResolver = new MockResourceResolver() {
            @Override
            public Resource getResource(String path) {

                Resource resource = repository.get(path);

                if ( resource == null ) {
                    if ( "/test/upload/location".equals(path)) {
                        resource =  new MockRealResource(this, path, "sling:Folder");
                        repository.put(path,resource);
                        LOG.debug("Created {} ", path);

                    }
                }
                LOG.debug("Resource {} is {} {}", path, resource, ResourceUtil.isSyntheticResource(resource));
                return resource;
            }




            @Override
            public Iterable<Resource> getChildren(Resource resource) {

                List<Resource> children = new ArrayList<Resource>();
                for(Map.Entry<String, Resource> e : repository.entrySet()) {
                    if (isChild(resource.getPath(), e.getKey())) {
                        children.add(e.getValue());
                    }
                }
                return children;
            }

            private boolean isChild(String path, String key) {
                if ( key.length() > path.length() && key.startsWith(path)) {
                    return !key.substring(path.length()+1).contains("/");
                }
                return false;
            }

            @Override
            public Iterator<Resource> listChildren(Resource parent) {
                return getChildren(parent).iterator();
            }

            @Override
            public void delete(Resource resource) throws PersistenceException {

            }

            @Override
            public Resource create(Resource resource, String s, Map<String, Object> map) throws PersistenceException {
                Resource childResource = resource.getChild(s);
                if ( childResource != null) {
                    throw new IllegalArgumentException("Child "+s+" already exists ");
                }
                String resourceType = (String)map.get("sling:resourceType");
                if ( resourceType == null) {
                    resourceType = (String)map.get("jcr:primaryType");
                }
                if ( resourceType == null) {
                    LOG.warn("Resource type null for {} {} ", resource,  resource.getPath()+"/"+s);
                }
                Resource newResource = new MockRealResource(this, resource.getPath()+"/"+s, resourceType, map);
                repository.put(newResource.getPath(), newResource);
                LOG.debug("Created Resource {} ", newResource.getPath());
                return newResource;
            }

            @Override
            public void revert() {

            }

            @Override
            public void commit() throws PersistenceException {
                LOG.debug("Committing");
                for(Map.Entry<String, Resource> e : repository.entrySet()) {
                    LOG.debug("Committing {} ", e.getKey());
                    Resource r = e.getValue();
                    ModifiableValueMap vm = r.adaptTo(ModifiableValueMap.class);
                    for (Map.Entry<String, Object> me : vm.entrySet()) {
                        if (me.getValue() instanceof InputStream) {
                            try {
                                String value = IOUtils.toString((InputStream) me.getValue());
                                LOG.debug("Converted {} {}  ", me.getKey(), value);
                                vm.put(me.getKey(), value);

                            } catch (IOException e1) {
                                throw new PersistenceException("Failed to commit input stream", e1);
                            }
                        }
                    }
                    LOG.debug("Converted {} ", vm);
                }
                LOG.debug("Comittted {} ", repository);


            }

            @Override
            public boolean hasChanges() {
                return false;
            }
        };

        SlingHttpServletRequest request = new MockSlingHttpServlet3Request(null, null, null, null, null) {
            @Override
            public Object getAttribute(String name) {
                if ( "request-parts-iterator".equals(name)) {
                    return partsIterator;
                }
                return super.getAttribute(name);
            }

            @Override
            public ResourceResolver getResourceResolver() {
                return resourceResolver;
            }
        };
        streamedUplodOperation.doRun(request, response, changes);


        {
            Resource r = repository.get("/test/upload/location/test1.txt");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);


            Assert.assertEquals("nt:file", m.get("jcr:primaryType"));

        }
        {
            Resource r = repository.get("/test/upload/location/test1.txt/jcr:content");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);

            Assert.assertEquals("nt:resource", m.get("jcr:primaryType"));
            Assert.assertTrue(m.get("jcr:lastModified") instanceof Calendar);
            Assert.assertEquals("text/plain", m.get("jcr:mimeType"));
            Assert.assertEquals("testpart", m.get("jcr:data"));

        }
        {
            Resource r = repository.get("/test/upload/location/test2.txt");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);


            Assert.assertEquals("nt:file", m.get("jcr:primaryType"));

        }
        {
            Resource r = repository.get("/test/upload/location/test2.txt/jcr:content");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);


            Assert.assertEquals("nt:resource", m.get("jcr:primaryType"));
            Assert.assertTrue(m.get("jcr:lastModified") instanceof Calendar);
            Assert.assertEquals("text/plain2", m.get("jcr:mimeType"));
            Assert.assertEquals("test1234", m.get("jcr:data"));
        }


    }

    @Test
    public void testPartsContentRange() throws RepositoryException, UnsupportedEncodingException {
        List<Modification> changes = new ArrayList<Modification>();
        PostResponse response = new AbstractPostResponse() {
            @Override
            protected void doSend(HttpServletResponse response) throws IOException {

            }

            @Override
            public void onChange(String type, String... arguments) {

            }

            @Override
            public String getPath() {
                return "/test/upload/location";
            }
        };

        List<Part> partsList = new ArrayList<Part>();
        partsList.add(new MockPart("formfield1", null, null, 0, new ByteArrayInputStream("testformfield1".getBytes("UTF-8")), Collections.EMPTY_MAP));
        partsList.add(new MockPart("formfield2", null, null, 0, new ByteArrayInputStream("testformfield2".getBytes("UTF-8")), Collections.EMPTY_MAP));
        partsList.add(new MockPart(
                "test1.txt",
                "text/plain",
                "test1bad.txt",
                4,
                new ByteArrayInputStream("test".getBytes("UTF-8")),
                mapOf("Content-Range","bytes 0-3/8", "Content-Length", "4")));
        partsList.add(new MockPart(
                "test1.txt",
                "text/plain",
                "test1bad.txt",
                4,
                new ByteArrayInputStream("part".getBytes("UTF-8")),
                mapOf("Content-Range","bytes 4-7/8", "Content-Length", "4")));
        partsList.add(new MockPart("*", "text/plain2", "test2.txt", 8, new ByteArrayInputStream("test1234".getBytes("UTF-8")), Collections.EMPTY_MAP));
        partsList.add(new MockPart("badformfield2", null, null, 0, new ByteArrayInputStream("testbadformfield2".getBytes("UTF-8")), Collections.EMPTY_MAP));
        final Iterator<Part> partsIterator = partsList.iterator();
        final Map<String, Resource> repository = new HashMap<String, Resource>();
        final ResourceResolver resourceResolver = new MockResourceResolver() {
            @Override
            public Resource getResource(String path) {

                Resource resource = repository.get(path);

                if ( resource == null ) {
                    if ( "/test/upload/location".equals(path)) {
                        resource =  new MockRealResource(this, path, "sling:Folder");
                        repository.put(path,resource);
                        LOG.debug("Created {} ", path);

                    }
                }
                LOG.debug("Resource {} is {} {}", path, resource, ResourceUtil.isSyntheticResource(resource));
                return resource;
            }




            @Override
            public Iterable<Resource> getChildren(Resource resource) {

                List<Resource> children = new ArrayList<Resource>();
                for(Map.Entry<String, Resource> e : repository.entrySet()) {
                    if (isChild(resource.getPath(), e.getKey())) {
                        children.add(e.getValue());
                    }
                }
                return children;
            }

            private boolean isChild(String path, String key) {
                if ( key.length() > path.length() && key.startsWith(path)) {
                    return !key.substring(path.length()+1).contains("/");
                }
                return false;
            }

            @Override
            public Iterator<Resource> listChildren(Resource parent) {
                return getChildren(parent).iterator();
            }

            @Override
            public void delete(Resource resource) throws PersistenceException {

            }

            @Override
            public Resource create(Resource resource, String s, Map<String, Object> map) throws PersistenceException {
                Resource childResource = resource.getChild(s);
                if ( childResource != null) {
                    throw new IllegalArgumentException("Child "+s+" already exists ");
                }
                String resourceType = (String)map.get("sling:resourceType");
                if ( resourceType == null) {
                    resourceType = (String)map.get("jcr:primaryType");
                }
                if ( resourceType == null) {
                    LOG.warn("Resource type null for {} {} ", resource,  resource.getPath()+"/"+s);
                }
                Resource newResource = new MockRealResource(this, resource.getPath()+"/"+s, resourceType, map);
                repository.put(newResource.getPath(), newResource);
                LOG.debug("Created Resource {} ", newResource.getPath());
                return newResource;
            }

            @Override
            public void revert() {

            }

            @Override
            public void commit() throws PersistenceException {
                LOG.debug("Committing");
                for(Map.Entry<String, Resource> e : repository.entrySet()) {
                    LOG.debug("Committing {} ", e.getKey());
                    Resource r = e.getValue();
                    ModifiableValueMap vm = r.adaptTo(ModifiableValueMap.class);
                    for (Map.Entry<String, Object> me : vm.entrySet()) {
                        if (me.getValue() instanceof InputStream) {
                            try {
                                String value = IOUtils.toString((InputStream) me.getValue());
                                LOG.debug("Converted {} {}  ", me.getKey(), value);
                                vm.put(me.getKey(), value);

                            } catch (IOException e1) {
                                throw new PersistenceException("Failed to commit input stream", e1);
                            }
                        }
                    }
                    LOG.debug("Converted {} ", vm);
                }
                LOG.debug("Comittted {} ", repository);


            }

            @Override
            public boolean hasChanges() {
                return false;
            }
        };

        SlingHttpServletRequest request = new MockSlingHttpServlet3Request(null, null, null, null, null) {
            @Override
            public Object getAttribute(String name) {
                if ( "request-parts-iterator".equals(name)) {
                    return partsIterator;
                }
                return super.getAttribute(name);
            }

            @Override
            public ResourceResolver getResourceResolver() {
                return resourceResolver;
            }
        };
        streamedUplodOperation.doRun(request, response, changes);


        {
            Resource r = repository.get("/test/upload/location/test1.txt");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);


            Assert.assertEquals("nt:file", m.get("jcr:primaryType"));

        }
        {
            Resource r = repository.get("/test/upload/location/test1.txt/jcr:content");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);

            Assert.assertEquals("nt:resource", m.get("jcr:primaryType"));
            Assert.assertTrue(m.get("jcr:lastModified") instanceof Calendar);
            Assert.assertEquals("text/plain", m.get("jcr:mimeType"));
            Assert.assertEquals("testpart", m.get("jcr:data"));

        }
        {
            Resource r = repository.get("/test/upload/location/test2.txt");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);


            Assert.assertEquals("nt:file", m.get("jcr:primaryType"));

        }
        {
            Resource r = repository.get("/test/upload/location/test2.txt/jcr:content");
            Assert.assertNotNull(r);
            ValueMap m = r.adaptTo(ValueMap.class);
            Assert.assertNotNull(m);


            Assert.assertEquals("nt:resource", m.get("jcr:primaryType"));
            Assert.assertTrue(m.get("jcr:lastModified") instanceof Calendar);
            Assert.assertEquals("text/plain2", m.get("jcr:mimeType"));
            Assert.assertEquals("test1234", m.get("jcr:data"));
        }


    }



    private Map<String,Object> mapOf(String ... s) {
        Map<String, Object> m = new HashMap<String, Object>();
        for (int i = 0; i < s.length; i+=2) {
            m.put(s[i],s[i+1]);
        }
        return m;
    }
}
