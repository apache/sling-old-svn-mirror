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

import org.apache.commons.httpclient.methods.PostMethod;

/** Test the SelectorServlet */
public class SelectorServletTest extends ResolutionTestBase {
  
  public void testSelectorOne() throws Exception {
    assertServlet(
        getContent(testNodeNORT.nodeUrl + ".TEST_SEL_1.txt", CONTENT_TYPE_PLAIN),
        SEL_SERVLET_SUFFIX);
  }
  
  public void testSelectorTwo() throws Exception {
    assertServlet(
        getContent(testNodeNORT.nodeUrl + ".TEST_SEL_2.txt", CONTENT_TYPE_PLAIN),
        SEL_SERVLET_SUFFIX);
  }
  
  public void testSelectorOther() throws Exception {
    assertNotTestServlet(
        getContent(testNodeNORT.nodeUrl + ".TEST_SEL_3.txt", CONTENT_TYPE_PLAIN));
  }
  
  public void testPostWithSelector() throws Exception {
      final PostMethod post = new PostMethod(testNodeRT.nodeUrl + ".TEST_SEL_2.txt");
      final int status = httpClient.executeMethod(post);
      assertEquals("POST to testNodeRT should return 200", 200, status);
      final String content = post.getResponseBodyAsString();
      assertServlet(content, SEL_SERVLET_SUFFIX);
  }
}
