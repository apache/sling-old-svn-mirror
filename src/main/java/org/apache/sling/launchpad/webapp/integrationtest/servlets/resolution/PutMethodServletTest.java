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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.resolution;

import org.apache.commons.httpclient.methods.PutMethod;

/** Test the PutMethodServlet resolution */
public class PutMethodServletTest extends ResolutionTestBase {
  
  public void testPutMethodServletSpecificRT() throws Exception {
    final PutMethod put = new PutMethod(testNodeRT.nodeUrl);
    final int status = httpClient.executeMethod(put);
    assertEquals("PUT to testNodeRT should return 200", 200, status);
    final String content = put.getResponseBodyAsString();
    assertServlet(content, PUT_SERVLET_SUFFIX);
  }
  
  public void testPutMethodServletDefaultRT() throws Exception {
    final PutMethod put = new PutMethod(testNodeNORT.nodeUrl);
    final int status = httpClient.executeMethod(put);
    assertFalse("PUT to testNodeRT should not return 200", 200 == status);
  }
}
