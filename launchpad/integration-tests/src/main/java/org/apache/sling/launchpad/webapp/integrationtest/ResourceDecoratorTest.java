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
package org.apache.sling.launchpad.webapp.integrationtest;

import org.apache.sling.commons.testing.integration.HttpTestBase;

public class ResourceDecoratorTest extends HttpTestBase {
    
    public void testDecoratedResource() throws Exception {
        final String path = "/testing/TestResourceDecorator/resource" + System.currentTimeMillis();
        final TestNode tn = new TestNode(HTTP_BASE_URL + path, null);
        final String content = getContent(tn.nodeUrl + ".txt", CONTENT_TYPE_PLAIN);
        final String expect = "TEST_RESOURCE_DECORATOR_RESOURCE_TYPE";
        assertTrue("Expecting content to contain " + expect + " (" + content + ")", content.contains(expect));
    }
}
