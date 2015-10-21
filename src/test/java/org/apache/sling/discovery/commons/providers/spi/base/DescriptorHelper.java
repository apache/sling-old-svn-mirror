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
package org.apache.sling.discovery.commons.providers.spi.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.SimpleValueFactory;
import org.apache.jackrabbit.oak.util.GenericDescriptors;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteDescriptor;

public class DescriptorHelper {

    public static void setDiscoveryLiteDescriptor(ResourceResolverFactory factory, DiscoveryLiteDescriptorBuilder builder) throws Exception {
        setDescriptor(factory, DiscoveryLiteDescriptor.OAK_DISCOVERYLITE_CLUSTERVIEW, builder.asJson());
    }
    
    public static void setDescriptor(ResourceResolverFactory factory, String key,
            String value) throws Exception {
        ResourceResolver resourceResolver = factory.getAdministrativeResourceResolver(null);
        try{
            Session session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                return;
            }
            Repository repo = session.getRepository();
            
            //<hack>
//            Method setDescriptorMethod = repo.getClass().
//                    getDeclaredMethod("setDescriptor", String.class, String.class);
//            if (setDescriptorMethod!=null) {
//                setDescriptorMethod.setAccessible(true);
//                setDescriptorMethod.invoke(repo, key, value);
//            } else {
//                fail("could not get 'setDescriptor' method");
//            }
            Method getDescriptorsMethod = repo.getClass().getDeclaredMethod("getDescriptors");
            if (getDescriptorsMethod==null) {
                fail("could not get 'getDescriptors' method");
            } else {
                getDescriptorsMethod.setAccessible(true);
                GenericDescriptors descriptors = (GenericDescriptors) getDescriptorsMethod.invoke(repo);
                SimpleValueFactory valueFactory = new SimpleValueFactory();
                descriptors.put(key, valueFactory.createValue(value), true, true);
            }
            //</hack>
            
            //<verify-hack>
            assertEquals(value, repo.getDescriptor(key));
            //</verify-hack>
        } finally {
            if (resourceResolver!=null) {
                resourceResolver.close();
            }
        }
    }
}
