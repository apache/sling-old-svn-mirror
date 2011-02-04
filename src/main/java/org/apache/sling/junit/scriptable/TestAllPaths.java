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
package org.apache.sling.junit.scriptable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** The single test class that runs all our path-based tests */
@RunWith(Parameterized.class)
public class TestAllPaths {

    private String path;
    private static boolean executing = false;
    public static final String TEST_URL_SUFFIX = ".test.txt";
    public static final String PASSED = "TEST_PASSED";
    
    // TODO can we do better than this to inject our environment here?
    static List<String> testPaths;
    static SlingRequestProcessor requestProcessor;
    static ResourceResolver resolver;

    public TestAllPaths(String path) {
        this.path = path;
    }
    
    // Due to our use of static context, only one instance of this test can
    // execute at any given time.
    @BeforeClass
    public static void checkConcurrency() {
        if(executing) {
            fail("Concurrent execution detected, not supported by this class");
        }
        executing = true;
    }

    @AfterClass
    public static void cleanup() {
        executing = false;
    }

    /** Let JUnit run this all on our paths */
    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new ArrayList<Object[]>();
        for(String path : testPaths) {
            data.add(new Object[] { path + TEST_URL_SUFFIX });
        }
        return data;
    }

    @Test
    public void verifyContent() throws Exception {
        
        // Get content via internal Sling request
        final HttpRequest req = new HttpRequest(path);
        final HttpResponse resp = new HttpResponse();
        requestProcessor.processRequest(req, resp, resolver);
        final String content = resp.getContent();
        assertEquals("Expecting HTTP status 200 for path " + path, 200, resp.getStatus());
        
        // Expect a single line of content with TEST_PASSED, ignoring
        // empty lines and lines that start with #
        final BufferedReader br = new BufferedReader(new StringReader(content));
        String line = null;
        int passedCount = 0;
        while( (line = br.readLine()) != null) {
            if(line.trim().length() == 0) {
                // ignore
            } else if(line.startsWith("#")) {
                // ignore
            } else if(line.trim().equals(PASSED) && passedCount == 0) {
                passedCount++;
            } else {
                fail("Unexpected content at path " + path 
                        + ", should be just " + PASSED + " (lines starting with # and empty lines are ignored)"
                        + "\ncontent was:\n" + content + "\n");
            }
        }
    }
}

