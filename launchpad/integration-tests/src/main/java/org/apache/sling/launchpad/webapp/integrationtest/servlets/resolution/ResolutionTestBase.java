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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Base class for servlet resolution tests */
class ResolutionTestBase extends HttpTestBase {
  public static final String CLASS_PROP = "servlet.class.name";
  public static final String TEST_SERVLET_MARKER = "created by org.apache.sling.launchpad.testservices.servlets";
  public static final String TEST_RESOURCE_TYPE = "LAUNCHPAD_TEST_ResourceType";
  public static final String TEST_PATH = "/servlet-resolution-tests/" + System.currentTimeMillis();
  public static final String NONEXISTING_RESOURCE_URL = HTTP_BASE_URL + TEST_PATH + "/NonExistingResource";

  public static final String EXT_SERVLET_SUFFIX = "testservices.servlets.ExtensionServlet";
  public static final String SEL_SERVLET_SUFFIX = "testservices.servlets.SelectorServlet";
  public static final String WAR_SEL_SERVLET_SUFFIX = "testservices.war.servlets.SelectorServlet";
  public static final String PREFIX_0_SERVLET_SUFFIX = "testservices.servlets.PrefixServletZero";
  public static final String PREFIX_M1_SERVLET_SUFFIX = "testservices.servlets.PrefixServletMinusOne";
  public static final String PUT_SERVLET_SUFFIX = "testservices.servlets.PutMethodServlet";
  public static final String HTML_DEFAULT_SERVLET_SUFFIX = "testservices.servlets.HtmlDefaultServlet";
  public static final String REQUEST_URI_OPTING_SERVLET_SUFFIX = "testservices.servlets.RequestUriOptingServlet";
  public static final String PATHS_SERVLET_SUFFIX = "testservices.servlets.PathsServlet";

  protected TestNode testNodeNORT;
  protected TestNode testNodeRT;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    testNodeNORT = new TestNode(HTTP_BASE_URL + TEST_PATH, null);
    final Map<String, String> properties = new HashMap<String, String>();
    properties.put("sling:resourceType", TEST_RESOURCE_TYPE);
    testNodeRT = new TestNode(HTTP_BASE_URL + TEST_PATH, properties);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    testNodeNORT.delete();
    testNodeRT.delete();
  }

  /** Asserts that the given content is in Properties format and
   *  contains a property named CLASS_PROP that ends with
   *  expected suffix
   */
  protected void assertServlet(String content, String expectedSuffix) throws IOException {
    final Properties props = getTestServletProperties(content);
    assertTrue(
        "Content represents a non-empty Properties object (" + content + ")",
        props.size() > 0);
    final String clazz = props.getProperty(CLASS_PROP);
    assertNotNull(
        "Content contains " + CLASS_PROP + " property (" + content + ")",
        clazz);
    assertTrue(
        CLASS_PROP + " property value (" + clazz + ") ends with " + expectedSuffix,
        clazz.endsWith(expectedSuffix));
  }
  
  protected Properties getTestServletProperties(String content) throws IOException {
      final Properties props = new Properties();
      final InputStream is = new ByteArrayInputStream(content.getBytes());
      props.load(is);
      return props;
  }

  /** Assert that content does not contain TEST_SERVLET_MARKER
   */
  protected void assertNotTestServlet(String content) {
    if(content.contains(TEST_SERVLET_MARKER)) {
      fail("Content should not contain " + TEST_SERVLET_MARKER + " marker (" + content + ")");
    }
  }
}
