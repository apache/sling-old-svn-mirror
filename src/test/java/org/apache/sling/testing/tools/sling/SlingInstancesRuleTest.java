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
package org.apache.sling.testing.tools.sling;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class SlingInstancesRuleTest {
    private static final List<SlingInstance> instances = new ArrayList<SlingInstance>();
    
    private static void addInstance(final String url) {
        instances.add(new SlingInstance() {
            public RequestBuilder getRequestBuilder() {
                return null;
            }

            public String getServerBaseUrl() {
                return url;
            }

            public String getServerUsername() {
                return null;
            }

            public String getServerPassword() {
                return null;
            }

            public RequestExecutor getRequestExecutor() {
                return null;
            }
                
        });
    }
    
    @Rule
    public final SlingInstancesRule rule = new SlingInstancesRule(instances);
    
    private static String result = "";
    
    @BeforeClass
    public static void setup() {
        addInstance("it does ");
        addInstance("work, cool!");
    }
    
    @AfterClass
    public static void verifyResult() {
        assertEquals("it does work, cool!", result);
    }
    
    @Test
    public void testInstanceName() {
        result = result + rule.getSlingInstance().getServerBaseUrl();
    }
}
