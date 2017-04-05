#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
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
package ${package}.${slingModelSubPackage};

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class SampleRequestModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleRequestModel.class);

    @SlingObject
    private ResourceResolver resourceResolver;

    public SampleRequestModel() {
        LOGGER.trace("Model Instance created");
    }

    /** @return User Name of the Current User **/
    public String getCurrentUser() {
        String answer = "No User";

        // Adapt to a session and get the current User ID.
        Session session = resourceResolver.adaptTo(Session.class);
        LOGGER.trace("Found Session from Resolver: '{}'", session);
        if(session != null) {
            answer = session.getUserID();
        }
        return answer;
    }
}