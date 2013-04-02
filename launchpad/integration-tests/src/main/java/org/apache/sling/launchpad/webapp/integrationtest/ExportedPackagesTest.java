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

import org.apache.sling.commons.testing.integration.HttpTestBase;

public class ExportedPackagesTest extends HttpTestBase {

    private void assertExported(String pack) throws Exception {
        final String content = getContent(HTTP_BASE_URL + "/.EXPORTED_PACKAGES.txt?package=" + pack, CONTENT_TYPE_PLAIN);
        final String marker = "PACKAGE FOUND:";
        assertTrue("Expecting '" + marker + "' in content '" + content + "'", content.contains(marker));
        assertTrue("Expecting '" + pack + "' in content '" + content + "'", content.contains(pack));
    }
    
    public void testSlingApiPackage() throws Exception {
        assertExported("org.apache.sling.api");
    }
    
    public void testPackageFromTestServices() throws Exception {
        assertExported("org.apache.sling.launchpad.testservices.exported");
    }
    
    /** TODO fails due to SLING-2808 */
    public void DISABLED_testPackageFromFragment() throws Exception {
        assertExported("org.apache.sling.launchpad.testservices.fragment.testpackage");
    }
}
