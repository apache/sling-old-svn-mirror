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
package org.apache.sling.testing.teleporter.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class ClassResourceVisitorTest implements ClassResourceVisitor.Processor {

    private Map<String, String> resources;
    
    @Override
    public void process(String resourceName, InputStream resourceStream) throws IOException {
        final StringWriter w = new StringWriter();
        IOUtils.copy(resourceStream, w);
        resources.put(resourceName, w.toString());
    }

    @Before
    public void setup() {
        resources = new HashMap<String, String>();
    }
    
    private void assertResource(String name, String expected) {
        final String content = resources.get(name);
        assertNotNull("Expecting resource " + name + " (keys=" + resources.keySet() + ")", content);
        assertTrue("Expecting " + name + " to contain " + expected, content.contains(expected));
    }
    
    @Test
    public void testSingleFile() throws IOException {
        new ClassResourceVisitor(getClass(), "/somepath/two.txt").visit(this);
        assertResource("/somepath/two.txt", "two");
        assertEquals(1, resources.size());
    }
    
    @Test
    public void testSomepathFiles() throws IOException {
        new ClassResourceVisitor(getClass(), "/somepath").visit(this);
        assertResource("/somepath/two.txt", "two");
        assertEquals(3, resources.size());
    }
    
    @Test
    public void testSubFiles() throws IOException {
        new ClassResourceVisitor(getClass(), "/somepath/sub").visit(this);
        assertResource("/somepath/sub/trois.txt", "three");
        assertEquals(1, resources.size());
    }
    
    @Test
    public void testNotFound() throws Exception {
        new ClassResourceVisitor(IOUtils.class, "/NOT_FOUND").visit(this);
        assertEquals(0, resources.size());
    }
    
    @Test
    public void testJarResourcesFolder() throws Exception {
        // Get resources from the IOUtils jar to test the jar protocol
        new ClassResourceVisitor(IOUtils.class, "/META-INF/maven/commons-io/").visit(this);
        assertResource("/META-INF/maven/commons-io/commons-io/pom.properties", "artifactId=commons-io");
        assertResource("/META-INF/maven/commons-io/commons-io/pom.xml", "Licensed to the Apache Software Foundation");
        assertEquals(2, resources.size());
    }
    
    @Test
    public void testSinglJarResource() throws Exception {
        new ClassResourceVisitor(IOUtils.class, "/META-INF/maven/commons-io/commons-io/pom.properties").visit(this);
        assertResource("/META-INF/maven/commons-io/commons-io/pom.properties", "artifactId=commons-io");
        assertEquals(1, resources.size());
    }
}