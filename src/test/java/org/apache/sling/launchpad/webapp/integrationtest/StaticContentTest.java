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

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Some tests about static content */
public class StaticContentTest extends HttpTestBase {
    
    public void testWebInfForbidden() throws IOException {
        // if DefaultSlingServlet handled this we'd get an SC_FORBIDDEN, but it looks
        // like the servlet container blocks it already
        assertHttpStatus(HTTP_BASE_URL + "/WEB-INF/web.xml", HttpServletResponse.SC_NOT_FOUND);
    }
    
    public void testMetaInfForbidden() throws IOException {
        // if DefaultSlingServlet handled this we'd get an SC_FORBIDDEN, but it looks
        // like the servlet container blocks it already
        assertHttpStatus(HTTP_BASE_URL + "/META-INF/somefile.txt", HttpServletResponse.SC_NOT_FOUND);
    }
}
