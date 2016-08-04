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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.OsgiMetadata;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.Reference;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.ReferenceCardinality;
import org.junit.Test;
import org.osgi.framework.Constants;

public class OsgiMetadataUtilTest {

    @Test
    public void testMetadata() {
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(ServiceWithMetadata.class);

        Set<String> serviceInterfaces = metadata.getServiceInterfaces();
        assertEquals(3, serviceInterfaces.size());
        assertTrue(serviceInterfaces.contains("org.apache.sling.models.spi.Injector"));
        assertTrue(serviceInterfaces
                .contains("org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory"));
        assertTrue(serviceInterfaces.contains("java.lang.Comparable"));

        Map<String, Object> props = metadata.getProperties();
        assertEquals(4, props.size());
        assertEquals(5000, props.get("service.ranking"));
        assertEquals("The Apache Software Foundation", props.get(Constants.SERVICE_VENDOR));
        assertEquals("org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest$ServiceWithMetadata", props.get(Constants.SERVICE_PID));
        assertArrayEquals(new String[] { "org.apache.sling.api.resource.Resource", "org.apache.sling.api.resource.ResourceResolver" },
                (String[])props.get("adaptables"));
    }

    @Test
    public void testNoMetadata() {
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(ServiceWithoutMetadata.class);
        assertNull(metadata);
    }

    @Test
    public void testReferences() {
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(OsgiServiceUtilTest.Service3.class);
        List<Reference> references = metadata.getReferences();
        assertEquals(4, references.size());

        Reference ref1 = references.get(2);
        assertEquals("reference2", ref1.getName());
        assertEquals("org.apache.sling.testing.mock.osgi.OsgiServiceUtilTest$ServiceInterface2", ref1.getInterfaceType());
        assertEquals(ReferenceCardinality.MANDATORY_MULTIPLE, ref1.getCardinality());
        assertEquals("bindReference2", ref1.getBind());
        assertEquals("unbindReference2", ref1.getUnbind());
    }

    @Test
    public void testActivateMethodName() {
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(OsgiServiceUtilTest.Service3.class);
        assertEquals("activate", metadata.getActivateMethodName());
    }

    static class ServiceWithMetadata {
        // empty class
    }

    static class ServiceWithoutMetadata {
        // empty class
    }

}
