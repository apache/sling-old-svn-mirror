/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.testing.models;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

@Model(adaptables = SlingHttpServletRequest.class)
public class RequestModel {

    @Inject @Via("resource") @Named("jcr:title")
    private String title;

    @Inject @Named("argument")
    // get it from request attributes
    private String requestArgument;

    @ScriptVariable
    private ValueMap properties;
    private String jcrType;

    public String getTitle() {
        return title != null ? title : "FAILED";
    }

    public String getRequestArgument() {
        return requestArgument != null ? requestArgument : "FAILED";
    }

    public String getJCRType() {
        return PropertiesUtil.toString(properties.get("jcr:primaryType"), "FAILED");
    }

}
