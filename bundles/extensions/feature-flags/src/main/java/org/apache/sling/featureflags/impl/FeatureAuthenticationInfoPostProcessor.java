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
package org.apache.sling.featureflags.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.AuthenticationInfoPostProcessor;
import org.apache.sling.featureflags.FeatureConstants;

/**
 * This authentication info post processor enables the feature flag support
 * in the resource resolver for GET and HEAD requests.
 */
@Component
@Service(value=AuthenticationInfoPostProcessor.class)
public class FeatureAuthenticationInfoPostProcessor implements AuthenticationInfoPostProcessor {

    /**
     * @see org.apache.sling.auth.core.spi.AuthenticationInfoPostProcessor#postProcess(org.apache.sling.auth.core.spi.AuthenticationInfo, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void postProcess(final AuthenticationInfo info,
            final HttpServletRequest request,
            final HttpServletResponse response)
    throws LoginException {
        if ( "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()) ) {
            info.put(FeatureConstants.RESOLVER_ATTR_FEATURES_ENABLED, Boolean.TRUE);
        }
    }

}
