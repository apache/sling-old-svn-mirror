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
package org.apache.sling.launchpad.webapp.integrationtest.apt;

import org.apache.sling.commons.testing.integration.HttpTestBase;

public class SimpleAptRenderingTest extends HttpTestBase {
    
     private final static String EOL = System.getProperty( "line.separator" );

     public void testAptDocument() throws Exception {
        testClient.mkdirs(WEBDAV_BASE_URL, "/apt-test");
        final String toDelete = uploadTestScript("/apt-test", "apt/apt-test.apt", "apt-test.apt");
        try {
            // .apt returns plain text
            getContent(HTTP_BASE_URL + "/apt-test/apt-test.apt", CONTENT_TYPE_PLAIN);
            
            // .apt.aptml converts APT to html
            final String content = getContent(HTTP_BASE_URL + "/apt-test/apt-test.apt.aptml", CONTENT_TYPE_HTML);
            assertTrue("HTML opening tag present (" + content + ")", content.startsWith("<html>"));
            assertTrue("HTML closing tag present (" + content + ")", content.endsWith("</html>" + EOL));
            assertTrue("title parsed as expected (" + content + ")", content.contains("<title>Simple APT file test</title>"));
            assertTrue("body opening tag is present (" + content + ")", content.contains("</head>" + EOL + "<body>"));
            assertTrue("body closing tag is present (" + content + ")", content.endsWith("</body>" + EOL + "</html>" + EOL));
            assertTrue("h1 parsed as expected (" + content + ")", content.contains("<h1>h1 heading"));
            assertTrue("h2 parsed as expected (" + content + ")", content.contains("<h2>h2 heading"));
            assertTrue("h3 parsed as expected (" + content + ")", content.contains("<h3>h3 heading"));
        } finally {
            testClient.delete(toDelete);
        }
    }
}
