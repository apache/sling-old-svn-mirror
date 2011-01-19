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
package org.apache.sling.launchpad.testservices.filters;

/** Example/test Sling Servlet registered with two extensions
 *
 * @scr.component immediate="true" metatype="no"
 * @scr.service interface="javax.servlet.Filter"
 * @scr.property name="sling.filter.scope" value="request"
 *
 * @scr.property name="service.description" value="Test Filter"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 *
 * Register this filter with the Sling-specific registration property
 * @scr.property name="filter.scope" value="request"
 */
public class SlingFilter extends TestFilter {

    @Override
    protected String getHeaderName() {
        return "FILTER_COUNTER_SLING";
    }

}
