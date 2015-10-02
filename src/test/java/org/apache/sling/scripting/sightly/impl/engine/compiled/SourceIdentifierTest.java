/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine.compiled;

import org.apache.sling.api.resource.Resource;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SourceIdentifierTest {

    private static SourceIdentifier sourceIdentifier;

    @BeforeClass
    public static void setUp() {
        final Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn("/apps/blah/static/foo/foo.html");
        sourceIdentifier = new SourceIdentifier(null, null, null, resource, "SightlyJava_");
    }

    @Test
    public void testGetClassName() throws Exception {
        assertEquals("SightlyJava_foo", sourceIdentifier.getClassName());
    }

    @Test
    public void testGetPackageName() throws Exception {
        assertEquals("apps.blah._static.foo", sourceIdentifier.getPackageName());
    }

    @Test
    public void testGetFullyQualifiedName() throws Exception {
        assertEquals("apps.blah._static.foo.SightlyJava_foo", sourceIdentifier.getFullyQualifiedName());
    }
}