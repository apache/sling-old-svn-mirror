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

/** Servlet that registers itself for specific paths
 * 
 *  @scr.component immediate="true" metatype="no"
 *  @scr.service interface="javax.servlet.Servlet"
 * 
 *  @scr.property name="service.description" value="Paths Test Servlet"
 *  @scr.property name="service.vendor" value="The Apache Software Foundation"
 * 
 *  @scr.property name="sling.servlet.paths" 
 *                  values.0="/testing/PathsServlet/foo" 
 *                  values.1="/testing/PathsServlet/bar/more/foo.html" 
 * 
 */
public class PathsServlet extends TestServlet {
}
