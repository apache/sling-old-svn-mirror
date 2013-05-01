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

import org.apache.sling.testing.tools.sling.SlingTestBase;
import org.junit.Test;

/** Simple HTTP test example, checks the validity of some
 *  OSGi webconsole URLs by GETting them and checking
 *  status code 200.
 */
public class OsgiConsoleTest extends SlingTestBase {
    
    @Test
    public void testSomeConsolePaths() throws Exception {
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
            getRequestExecutor().execute(
                    getRequestBuilder().buildGetRequest(path)
                    .withCredentials(getServerUsername(), getServerPassword())
            ).assertStatus(200);
        }
    }
}
