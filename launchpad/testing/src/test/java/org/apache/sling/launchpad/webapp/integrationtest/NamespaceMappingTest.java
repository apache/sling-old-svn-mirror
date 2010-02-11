/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/**
 * Test that both Sling-Namespaces and {@link NamespaceMapper} work.
 */
public class NamespaceMappingTest extends HttpTestBase {

    /**
     * Verify that Sling-Namespaces works.
     */
    public void testNamespaceFromManifest() throws IOException {
        final String expected = "test1=http://sling.apache.org/test/one";
        final String content = getContent(HTTP_BASE_URL + "/testing/NamespaceTestServlet/output",
                CONTENT_TYPE_PLAIN);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

    /**
     * Verify that {@link NamespaceMapper} works.
     */
    public void testNamespaceFromNamespaceMapper() throws IOException {
        final String expected = "test2=http://sling.apache.org/test/two";
        final String content = getContent(HTTP_BASE_URL + "/testing/NamespaceTestServlet/output",
                CONTENT_TYPE_PLAIN);
        assertTrue("Content contains " + expected + " (" + content + ")", content.contains(expected));
    }

}
