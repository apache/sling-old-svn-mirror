package org.apache.sling.launchpad.testservices.servlets;

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

/** Example/test Sling Servlet registered with two selectors
 * 
 * @scr.component immediate="true" metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * 
 * @scr.property name="service.description" value="Default Query Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * 
 * Register this servlet for the default resource type and two selectors:
 * @scr.property name="sling.servlet.resourceTypes"
 *               value="sling/servlet/default"
 *               
 * @scr.property name="sling.servlet.selectors"
 *               values.1 = "TEST_SEL_1"
 *               values.2 = "TEST_SEL_2"
 *                
 * @scr.property name="sling.servlet.extensions"
 *               value = "txt"
*/

@SuppressWarnings("serial")
public class SelectorServlet extends TestServlet {  
}