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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;

@Model(adaptables = SlingHttpServletRequest.class)
public class InjectorSpecificAnnotationModel {

    @ValueMapValue(optional = true)
    private String first;

    @ValueMapValue(name = "second", optional = true)
    private String secondWithOtherName;

    @ValueMapValue(optional = true)
    private Logger log;

    @ScriptVariable(optional = true, name = "sling")
    private SlingScriptHelper helper;

    @RequestAttribute(optional = true, name = "attribute")
    private Object requestAttribute;

    @OSGiService(optional = true)
    private Logger service;

    @ChildResource(optional = true, name = "child1")
    private Resource childResource;

    public String getFirst() {
        return first;
    }

    public String getSecond() {
        return secondWithOtherName;
    }

    public Logger getLog() {
        return log;
    }

    public Logger getService() {
        return service;
    }

    public SlingScriptHelper getHelper() {
        return helper;
    }

    public Object getRequestAttribute() {
        return requestAttribute;
    }

    public Resource getChildResource() {
        return childResource;
    }

}
