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
package org.apache.sling.samples.fling.page;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class UserPage extends Page {

    private User user;

    private Map<String, String> userProperties;

    public UserPage() {
    }

    @PostConstruct
    public void init() throws Exception {
        final Session session = resource.getResourceResolver().adaptTo(Session.class);
        final UserManager userManager = AccessControlUtil.getUserManager(session);
        user = (User) userManager.getAuthorizable(session.getUserID());
        userProperties = mapUserProperties(user);
    }

    public User getUser() {
        return user;
    }

    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    private static Map<String, String> mapUserProperties(final User user) throws RepositoryException {
        final Map<String, String> userProperties = new HashMap<>();
        final Iterator<String> keys = user.getPropertyNames();
        while (keys.hasNext()) {
            final String key = keys.next();
            final Value[] values = user.getProperty(key);
            if (values != null && values.length > 0) {
                if (values.length == 1) {
                    userProperties.put(key, values[0].getString());
                } else {
                    final String[] strings = new String[values.length];
                    for (int i = 0; i < values.length; i++) {
                        strings[i] = values[i].getString();
                    }
                    userProperties.put(key, Arrays.toString(strings));
                }
            }
        }
        return userProperties;
    }

}
