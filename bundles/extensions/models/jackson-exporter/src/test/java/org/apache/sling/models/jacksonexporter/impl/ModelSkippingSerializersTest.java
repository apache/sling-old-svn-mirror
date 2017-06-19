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
package org.apache.sling.models.jacksonexporter.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class ModelSkippingSerializersTest {

    private ModelSkippingSerializers serializers = new ModelSkippingSerializers();
    private JavaType nonAnnotatedType = TypeFactory.defaultInstance().constructType(NonAnnotated.class);
    private JavaType annotatedType = TypeFactory.defaultInstance().constructType(Annotated.class);

    @Before
    public void setup() {
        serializers.addSerializer(Resource.class, new ResourceSerializer(-1));
    }

    @Test
    public void testDefaultSerializerAccess() {
        assertTrue(serializers.findSerializer(null, nonAnnotatedType, null) instanceof ResourceSerializer);
    }

    @Test
    public void testAnnotatedNullLookup() {
        assertNull(serializers.findSerializer(null, annotatedType, null));
    }

    @Model(adaptables = SlingHttpServletRequest.class)
    private class Annotated extends AbstractResource {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public Resource getParent() {
            return null;
        }

        @Override
        public Resource getChild(String relPath) {
            return null;
        }

        @Override
        public Iterator<Resource> listChildren() {
            return null;
        }

        @Override
        public Iterable<Resource> getChildren() {
            return null;
        }

        @Override
        public boolean isResourceType(String resourceType) {
            return false;
        }

        @Override
        public String getResourceType() {
            return null;
        }

        @Override
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return null;
        }

        @Override
        public ResourceMetadata getResourceMetadata() {
            return null;
        }

        @Override
        public ResourceResolver getResourceResolver() {
            return null;
        }

        @Override
        public String getResourceSuperType() {
            return null;
        }
    }

    private class NonAnnotated extends AbstractResource {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public Resource getParent() {
            return null;
        }

        @Override
        public Resource getChild(String relPath) {
            return null;
        }

        @Override
        public Iterator<Resource> listChildren() {
            return null;
        }

        @Override
        public Iterable<Resource> getChildren() {
            return null;
        }

        @Override
        public boolean isResourceType(String resourceType) {
            return false;
        }

        @Override
        public String getResourceType() {
            return null;
        }

        @Override
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return null;
        }

        @Override
        public ResourceMetadata getResourceMetadata() {
            return null;
        }

        @Override
        public ResourceResolver getResourceResolver() {
            return null;
        }

        @Override
        public String getResourceSuperType() {
            return null;
        }
    }
}
