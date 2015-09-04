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
package org.apache.sling.models.testmodels.classes;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Required;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ResourceModelWithRequiredFieldOptionalStrategy {

    // although this is marked with optional=false, this is still optional, because injectionStrategy has a higher priority (even if it is only the default)
    // compare also with https://issues.apache.org/jira/browse/SLING-4155
    @ValueMapValue(optional=false)
    private String optional5;
    
    @SuppressWarnings("unused")
    @Inject
    private String optional1;

    @Inject
    @Required
    private String required1;
    
    @ValueMapValue
    private String optional2;
    
    @ValueMapValue(optional=true)
    private String optional3;
    
    @ValueMapValue(injectionStrategy=InjectionStrategy.OPTIONAL)
    private String optional4;
    
    @ValueMapValue(injectionStrategy=InjectionStrategy.REQUIRED)
    private String required2;

    public String getRequired1() {
        return required1;
    }

    public String getRequired2() {
        return required2;
    }
    

}
