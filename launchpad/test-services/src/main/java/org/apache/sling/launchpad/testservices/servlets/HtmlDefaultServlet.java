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

/**
 * Default servlet for the html extension, see SLING-1069.
 * <p>
 * This servlet collides with the Default GET Servlet generating proper HTML not
 * expected by HtmlDefaultServletTest. For this reason this component is
 * disabled by default and must be enabled for testing in the
 * HtmlDefaultServletTest class.
 *
 * @scr.component enabled="false"
 *                name="org.apache.sling.launchpad.testservices.servlets.HtmlDefaultServlet"
 *                metatype="no" immediate="true" label="%sling.name"
 *                description="%sling.description"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.extensions" value="html"
 * @scr.property name="sling.servlet.resourceTypes"
 *               value="sling/servlet/default"
 * @scr.property name="sling.servlet.methods" value="GET"
 */
@SuppressWarnings("serial")
public class HtmlDefaultServlet extends TestServlet {
}