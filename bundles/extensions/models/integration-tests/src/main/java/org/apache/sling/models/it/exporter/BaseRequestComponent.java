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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.ExporterOption;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Model(adaptables = { SlingHttpServletRequest.class }, resourceType = "sling/exp-request/base")
@Exporter(name = "jackson", extensions = "json", options = {
    @ExporterOption(name = "MapperFeature.SORT_PROPERTIES_ALPHABETICALLY", value = "true")
})
public class BaseRequestComponent {

    @Inject @SlingObject
    private Resource resource;

    @Inject @Via("resource")
    private String sampleValue;

    @Inject
    private Map<String, Object> testBindingsObject;

    @Inject
    private Map<String, Object> testBindingsObject2;

    private final SlingHttpServletRequest request;

    public BaseRequestComponent(SlingHttpServletRequest request) {
        this.request = request;
    }

    public String getId() {
        return this.resource.getPath();
    }

    public String getSampleValue() {
        return sampleValue;
    }

    @JsonProperty(value="UPPER")
    public String getSampleValueToUpperCase() {
        return sampleValue.toUpperCase();
    }

    public Resource getResource() {
        return resource;
    }

    public Map<String, Object> getTestBindingsObject() {
        return testBindingsObject;
    }

    public Map<String, Object> getTestBindingsObject2() {
        return testBindingsObject2;
    }

    public SlingHttpServletRequest getSlingHttpServletRequest() {
        return request;
    }

    public HttpServletRequest getHttpServletRequest() {
        return request;
    }

    public ServletRequest getServletRequest() {
        return request;
    }

}
