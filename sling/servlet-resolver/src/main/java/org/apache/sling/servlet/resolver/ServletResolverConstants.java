/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.servlet.resolver;

public class ServletResolverConstants {

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the resource type(s) supported by the servlet (value
     * is "sling.core.resourceTypes"). The type of this property is a String or
     * String[] (array of strings) denoting the resource types. If this property
     * is missing or empty the Servlet is ignored.
     */
    public static final String SLING_RESOURCE_TYPES = "sling.core.resourceTypes";

    /**
     * The name of the registered servlet used as the default servlet if no
     * other servlet or script could be selected (value is
     * "sling.core.servlet.default"). If no servlet is registered under this
     * name, the {@link org.apache.sling.core.servlets.DefaultServlet} is used.
     */
    public static final String DEFAULT_SERVLET_NAME = "sling.core.servlet.default";

}
