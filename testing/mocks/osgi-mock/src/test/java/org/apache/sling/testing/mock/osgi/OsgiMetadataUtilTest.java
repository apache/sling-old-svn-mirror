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
package org.apache.sling.testing.mock.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.Reference;
import org.junit.Test;
import org.w3c.dom.Document;

public class OsgiMetadataUtilTest {

    @Test
    public void testMetadata() {
        Document doc = OsgiMetadataUtil.getMetadata(ServiceWithMetadata.class);

        Set<String> serviceInterfaces = OsgiMetadataUtil.getServiceInterfaces(ServiceWithMetadata.class, doc);
        assertEquals(3, serviceInterfaces.size());
        assertTrue(serviceInterfaces.contains("org.apache.sling.models.spi.Injector"));
        assertTrue(serviceInterfaces
                .contains("org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory"));
        assertTrue(serviceInterfaces.contains("java.lang.Comparable"));

        Map<String, Object> props = OsgiMetadataUtil.getProperties(ServiceWithMetadata.class, doc);
        assertEquals(3, props.size());
        assertEquals(5000, props.get("service.ranking"));
        assertEquals("The Apache Software Foundation", props.get("service.vendor"));
        assertEquals("org.apache.sling.models.impl.injectors.OSGiServiceInjector", props.get("service.pid"));
    }

    @Test
    public void testNoMetadata() {
        Document doc = OsgiMetadataUtil.getMetadata(ServiceWithoutMetadata.class);

        Set<String> serviceInterfaces = OsgiMetadataUtil.getServiceInterfaces(ServiceWithoutMetadata.class, doc);
        assertEquals(0, serviceInterfaces.size());

        Map<String, Object> props = OsgiMetadataUtil.getProperties(ServiceWithoutMetadata.class, doc);
        assertEquals(0, props.size());
    }

    @Test
    public void testReferences() {
        Document doc = OsgiMetadataUtil.getMetadata(ReflectionServiceUtilTest.Service3.class);
        List<Reference> references = OsgiMetadataUtil.getReferences(ReflectionServiceUtilTest.Service3.class, doc);
        assertEquals(3, references.size());

        Reference ref1 = references.get(0);
        assertEquals("reference2", ref1.getName());
        assertEquals("org.apache.sling.testing.mock.osgi.ReflectionServiceUtilTest$ServiceInterface2", ref1.getInterfaceType());
        assertEquals(ReferenceCardinality.MANDATORY_MULTIPLE, ref1.getCardinality());
        assertEquals("bindReference2", ref1.getBind());
        assertEquals("unbindReference2", ref1.getUnbind());
    }

    @Test
    public void testActivateMethodName() {
        Document doc = OsgiMetadataUtil.getMetadata(ReflectionServiceUtilTest.Service3.class);
        String methodName = OsgiMetadataUtil.getActivateMethodName(ReflectionServiceUtilTest.Service3.class, doc);
        assertEquals("activate", methodName);
    }

    static class ServiceWithMetadata {
        // empty class
    }

    static class ServiceWithoutMetadata {
        // empty class
    }

}
