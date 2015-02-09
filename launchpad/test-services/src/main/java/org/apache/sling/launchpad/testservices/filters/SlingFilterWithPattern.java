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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

/** Example/test Filter with sling.filter.pattern see also SLING-4294 */
@Component(immediate=true, metatype=false)
@Service(value=javax.servlet.Filter.class)
@Properties({
    @Property(name="service.description", value="SlingFilter Test Filter"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="filter.scope", value="request"),
    @Property(name="sling.filter.scope", value="request"),
    @Property(name="sling.filter.pattern", value="/system.*")
})
public class SlingFilterWithPattern extends TestFilter {

    @Override
    protected String getHeaderName() {
        return "FILTER_COUNTER_SLING_WITH_PATTERN";
    }

}
