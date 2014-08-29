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
package org.apache.sling.models.it.models;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

@Model(adaptables=Resource.class)
public class ConstructorInjectionTestModel {

    private final String testProperty;
    private final List<Filter> filters;
    private final Resource resource;
    
    @Inject
    public ConstructorInjectionTestModel(
            @Named("testProperty") String testProperty,
            @Named("filters") List<Filter> filters,
            @Self Resource resource) {
        this.testProperty = testProperty;
        this.filters = filters;
        this.resource = resource;
    }
    
    public String getTestProperty() {
        return testProperty;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public Resource getResource() {
        return resource;
    }

}
