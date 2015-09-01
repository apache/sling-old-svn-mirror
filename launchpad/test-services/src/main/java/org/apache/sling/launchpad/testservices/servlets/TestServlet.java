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
package org.apache.sling.launchpad.testservices.servlets;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

/** Base class for test servlets - GET displays easy to parse information 
 *  about the servlet and resource
 */
@SuppressWarnings("serial")
class TestServlet extends SlingAllMethodsServlet {

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
  throws ServletException, IOException {
    Logger.getLogger(TestServlet.class.getName()).log(Level.SEVERE, "test JUL message");
    dumpRequestAsProperties(request, response);
  }
  
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
  throws ServletException, IOException {
    dumpRequestAsProperties(request, response);
  }
  
  protected void dumpRequestAsProperties(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
    final Properties props = new Properties();
    response.setContentType("text/plain");
    props.put("servlet.class.name", getClass().getName());
    
    final Resource r = request.getResource();
    props.put("sling.resource.path", r == null ? "" : r.getPath());
    props.put("sling.resource.type", r == null ? "" : r.getResourceType());
    props.put("http.request.method", request.getMethod());
    props.put("http.request.pathInfo", request.getPathInfo());
    props.put("http.request.requestURI", request.getRequestURI());
    props.put("http.request.requestURL", request.getRequestURL().toString());
    
    setCustomProperties(props);
    props.store(response.getOutputStream(), "Data created by " + getClass().getName() + " at " + new Date());
    response.getOutputStream().flush();
  }
  
  /** Hook for additional custom properties */
  void setCustomProperties(Properties props) {
  }
}
