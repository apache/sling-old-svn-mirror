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
package org.apache.sling.jcr.resourcesecurity.impl;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;


@Component(configurationFactory=true, policy=ConfigurationPolicy.REQUIRE, metatype=true,
           label="Apache Sling JCR Resource Access Gate",
           description="This access gate can be used to handle the access to resources" +
                       " not backed by a JCR repository by providing ACLs in the " +
                       "reposiory")
@Service(value=ResourceAccessGate.class)
@Properties({
    @Property(name=ResourceAccessGate.PATH, label="Path",
              description="The path is a regular expression for which resources the service should be called"),
    @Property(name=ResourceAccessGateFactory.PROP_JCR_PATH,
              label="JCR Node",
              description="This node is checked for permissions to the resources."),
    @Property(name=ResourceAccessGate.OPERATIONS, value="read", propertyPrivate=true)
})
public class ResourceAccessGateFactory
    extends AllowingResourceAccessGate
    implements ResourceAccessGate {

    static final String PROP_JCR_PATH = "jcrPath";

    private String jcrPath;

    @Activate
    protected void activate(final Map<String, Object> props) {
        this.jcrPath = PropertiesUtil.toString(props.get(PROP_JCR_PATH), null);
    }

    @Override
    public boolean hasReadRestrictions(ResourceResolver resourceResolver) {
        return true;
    }

    @Override
    public GateResult canRead(final Resource resource) {
        final Session session = resource.getResourceResolver().adaptTo(Session.class);
        boolean canRead = false;
        if ( session != null ) {
            try {
                canRead = session.nodeExists(this.jcrPath);
            } catch (final RepositoryException re) {
                // ignore
            }
        }
        return canRead ? GateResult.GRANTED : GateResult.DENIED;
    }
}
