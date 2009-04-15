package org.apache.sling.launchpad.testservices.servlets;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

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

/** Example/test Sling Servlet registered for the PUT method
 *  on a specific resource type
 * 
 * @scr.component immediate="true" metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * 
 * @scr.property name="service.description" value="Default Query Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * 
 * @scr.property name="sling.servlet.resourceTypes"
 *               value="TEST_HeadMethodServlet"
 *               
 * @scr.property name="sling.servlet.methods"
 *               value = "PUT"
*/

@SuppressWarnings("serial")
public class PutMethodServlet extends TestServlet {

  @Override
  protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response) 
  throws ServletException, IOException {
    dumpRequestAsProperties(request, response);
  }
}