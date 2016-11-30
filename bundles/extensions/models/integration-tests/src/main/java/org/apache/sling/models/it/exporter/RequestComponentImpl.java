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
package org.apache.sling.models.it.exporter;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.inject.Inject;

@Model(adaptables = { SlingHttpServletRequest.class }, adapters = Component.class, resourceType = "sling/exp-request/interface")
@Exporter(name = "jackson", extensions = "json")
public class RequestComponentImpl implements Component {

    @Inject @SlingObject
    private Resource resource;

    @Inject @Via("resource")
    private String sampleValue;

    private final SlingHttpServletRequest request;

    public RequestComponentImpl(SlingHttpServletRequest request) {
        this.request = request;
    }

    public String getId() {
        return this.resource.getPath();
    }

    public String getSampleValue() {
        return sampleValue;
    }
}
