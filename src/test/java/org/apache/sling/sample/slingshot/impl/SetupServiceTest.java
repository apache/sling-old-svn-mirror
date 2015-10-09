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
package org.apache.sling.sample.slingshot.impl;

import static org.apache.sling.hamcrest.ResourceMatchers.hasChildren;
import static org.apache.sling.hamcrest.ResourceMatchers.resourceOfType;
import static org.apache.sling.sample.slingshot.SlingshotConstants.RESOURCETYPE_USER;
import static org.apache.sling.sample.slingshot.impl.InternalConstants.RESOURCETYPE_HOME;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.sample.slingshot.SlingshotConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class SetupServiceTest {
    
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    @Test
    public void setup() throws Exception{
        
        // create expected content structure
        context.load().json("/slingshot.json", SlingshotConstants.APP_ROOT_PATH);
        
        // create a dummy config admin to prevent registration of service user amendments
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);
        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] { null });
        context.registerService(ConfigurationAdmin.class, configAdmin);
        
        // run the activation code
        context.registerInjectActivateService(new SetupService());
        
        // validate that the expected users are created
        Session adminSession = context.resourceResolver().adaptTo(Session.class);
        UserManager userManager = AccessControlUtil.getUserManager(adminSession);
        for ( String user : new String[] { "slingshot1", "slingshot2", InternalConstants.SERVICE_USER_NAME } ) {
            assertThat(userManager.getAuthorizable(user), notNullValue());    
        }
        
        // validate content structure
        Resource resource = context.resourceResolver().getResource(SlingshotConstants.APP_ROOT_PATH);
        
        assertThat(resource, resourceOfType(RESOURCETYPE_HOME));
        assertThat(resource.getChild("users"), notNullValue());
        assertThat(resource.getChild("users/slingshot1"), resourceOfType(RESOURCETYPE_USER));
        assertThat(resource.getChild("users/slingshot1"), hasChildren("info", "profile", "ugc"));
        
        // validate access control entries
        
        Session user = adminSession.impersonate(new SimpleCredentials("slingshot1", "slingshot1".toCharArray()));
        
        assertThat(user.hasPermission(SlingshotConstants.APP_ROOT_PATH+"/users/slingshot1/info", "read,add_node,set_property"), equalTo(true));
    }

}
