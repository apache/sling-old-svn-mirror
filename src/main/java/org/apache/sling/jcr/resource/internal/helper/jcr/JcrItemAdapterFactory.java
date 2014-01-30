/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.PersistableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AdapterFactory which adapts JCR Nodes and Properties into Resources.
 */
public class JcrItemAdapterFactory implements AdapterFactory {

    private final Logger logger = LoggerFactory.getLogger(JcrItemAdapterFactory.class);

    private final JcrResourceResolverFactoryImpl resourceResolverFactory;

    private ServiceRegistration serviceRegsitration;

    public JcrItemAdapterFactory(BundleContext ctx, JcrResourceResolverFactoryImpl resourceResolverFactory) {
        this.resourceResolverFactory = resourceResolverFactory;
        Dictionary<Object, Object> properties = new Hashtable<Object, Object>();
        properties.put(ADAPTABLE_CLASSES, new String[] { Node.class.getName(), Property.class.getName() });
        properties.put(ADAPTER_CLASSES,
                new String[] { Resource.class.getName(), Map.class.getName(), ValueMap.class.getName(),
                        PersistableValueMap.class.getName() });
        properties.put(Constants.SERVICE_DESCRIPTION, "JCR Item Adapter Factory");
        properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.serviceRegsitration = ctx.registerService(AdapterFactory.class.getName(), this, properties);
    }

    public void dispose() {
        if (this.serviceRegsitration != null) {
            this.serviceRegsitration.unregister();
            this.serviceRegsitration = null;
        }
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        if (type == Resource.class) {
            try {
                if (adaptable instanceof Node) {
                    Node node = (Node) adaptable;
                    return (AdapterType) new JcrNodeResource(resourceResolverFactory.getResourceResolver(node
                            .getSession()), node, resourceResolverFactory.getDynamicClassLoader());
                } else if (adaptable instanceof Property) {
                    Property property = (Property) adaptable;
                    return (AdapterType) new JcrPropertyResource(resourceResolverFactory.getResourceResolver(property
                            .getSession()), property);
                }
            } catch (RepositoryException e) {
                logger.error("Unable to adapt JCR Item to a Resource", e);
            }
            return null;
        }
        return getAdapter(adaptable, Resource.class).adaptTo(type);
    }

}
