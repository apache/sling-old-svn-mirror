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
package org.apache.sling.resourcebuilder.it;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.Comparator;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.resourcebuilder.test.ResourceAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Server-side integration test for the 
 *  ResourceBuilder, acquired via the ResourceBuilderProvider
 */
public class ResourceBuilderIT {
    
    @Rule
    public final TeleporterRule teleporter = 
        TeleporterRule
        .forClass(getClass(), "RBIT_Teleporter")
        .withResources("/files/");
    
    private TestEnvironment E;
    private ResourceAssertions A;

    @Before
    public void setup() throws LoginException, PersistenceException {
        E = new TestEnvironment(teleporter);
        A = new ResourceAssertions(E.testRootPath, E.resolver);
    }
    
    @After
    public void cleanup() throws PersistenceException {
        E.cleanup();
    }
    
    
    @Test
    public void simpleResource() {
        E.builder
            .resource("foo", "title", E.testRootPath)
            .commit();
        A.assertProperties("foo", "title", E.testRootPath);
    }
    
    @Test
    public void smallTreeWithFile() throws IOException {
        E.builder
            .resource("somefolder")
            .file("the-model.js", getClass().getResourceAsStream("/files/models.js"), "foo", 42L)
            .commit();
        
        A.assertFile("somefolder/the-model.js", "foo", "yes, it worked", 42L);
    }
    
    @Test
    public void fileAutoValues() throws IOException {
        final long startTime = System.currentTimeMillis();
        E.builder
            .resource("a/b/c")
            .file("model2.js", getClass().getResourceAsStream("/files/models.js"))
            .commit();
        
        final Comparator<Long> moreThanStartTime = new Comparator<Long>() {
            @Override
            public int compare(Long expected, Long fromResource) {
                if(fromResource >= startTime) {
                    return 0;
                }
                fail("last-modified is not >= than current time:" + fromResource + " < " + startTime);
                return -1;
            }
        };
        
        A.assertFile("a/b/c/model2.js", "application/javascript", "yes, it worked", startTime, moreThanStartTime);
    }
    
    @Test
    public void usingResolver() throws IOException {
        E.builderService.forResolver(E.resolver).resource("foo/a/b").commit();
        E.builderService.forResolver(E.resolver).resource("foo/c/d").commit();
        A.assertResource("/foo/a/b");
        A.assertResource("/foo/c/d");
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void restartFailsA() throws IOException {
        E.builder.forParent(E.resolver.getResource("/"));
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void restartFailsB() throws IOException {
        E.builder.forResolver(E.resolver);
    }
    
    @Test(expected=IllegalStateException.class)
    public void notStartedFailsA() throws IOException {
        E.builderService.resource("foo");
    }
    
    @Test(expected=IllegalStateException.class)
    public void notStartedFailsB() throws IOException {
        E.builderService.file("foo", null);
    }
}