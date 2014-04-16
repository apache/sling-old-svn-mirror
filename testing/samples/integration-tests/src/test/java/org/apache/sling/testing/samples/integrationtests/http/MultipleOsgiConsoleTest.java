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

package org.apache.sling.testing.samples.integrationtests.http;

import org.apache.sling.testing.tools.sling.SlingInstance;
import org.apache.sling.testing.tools.sling.SlingInstanceManager;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Example testing several Sling instances */
public class MultipleOsgiConsoleTest {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final SlingInstanceManager manager = new SlingInstanceManager("instance1", "instance2");

    @Test
    public void testSomeConsolePaths() throws Exception {
        for (SlingInstance slingInstance : manager.getInstances()) {
            testSomeConsolePaths(slingInstance);
        }
    }

    public void testSomeConsolePaths(SlingInstance slingInstance) throws Exception {
        log.info("Running {} on {}", getClass().getSimpleName(), slingInstance.getServerBaseUrl());

        final String [] subpaths = {
                "bundles",
                "components",
                "configMgr",
                "config",
                "licenses",
                "logs",
                "memoryusage",
                "services"
        };

        for(String subpath : subpaths) {
            final String path = "/system/console/" + subpath;
            slingInstance.getRequestExecutor().execute(
                    slingInstance.getRequestBuilder().buildGetRequest(path)
                            .withCredentials(slingInstance.getServerUsername(), slingInstance.getServerPassword())
            ).assertStatus(200);
        }
    }
}