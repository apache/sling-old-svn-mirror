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

package org.apache.sling.distribution.util.impl;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;


public class DistributionUtils {
    private static final Logger log = LoggerFactory.getLogger(DistributionUtils.class);


    public static ResourceResolver loginService(ResourceResolverFactory resolverFactory, String serviceName) throws LoginException {
        Map<String, Object> authInfo = new HashMap<String, Object>();

        authInfo.put(ResourceResolverFactory.SUBSERVICE, serviceName);

        return resolverFactory.getServiceResourceResolver(authInfo);
    }

    public static void safelyLogout(ResourceResolver resourceResolver) {
        try {
            if (resourceResolver != null) {
                Session session = resourceResolver.adaptTo(Session.class);
                resourceResolver.close();
                if (session != null && session.isLive()) {
                    session.logout();
                }
            }
        } catch (Throwable t) {
            log.error("cannot safely close resource resolver {}", resourceResolver);
        }
    }
}
