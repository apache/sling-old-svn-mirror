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

package org.apache.sling.offloaddatastore;

import org.apache.jackrabbit.oak.api.conversion.URIProvider;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@Component(immediate = true, service = AdapterFactory.class,
                property={
                        AdapterFactory.ADAPTER_CLASSES + "=java.net.URI",
                        AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.resource.Resource",
                        AdapterFactory.ADAPTABLE_CLASSES + "=javax.jcr.Value"
        },
        reference = {
        @Reference(
                name="providers",
                service = URIProvider.class,
                cardinality = ReferenceCardinality.MULTIPLE,
                policy = ReferencePolicy.DYNAMIC,
                policyOption = ReferencePolicyOption.GREEDY,
                bind = "bindProvider",
                unbind = "unbindProvider")
})
public class URIAdapaterFactory implements AdapterFactory {

    private Map<URIProvider, URIProvider> providers = new ConcurrentHashMap<URIProvider, URIProvider>();


    @Override
    public <AdapterType> AdapterType getAdapter(Object o, Class<AdapterType> aClass) {
        if (URI.class.equals(aClass)) {
            try {
                if (o instanceof Resource) {
                    Node n = ((Resource) o).adaptTo(Node.class);
                    if (n.hasProperty(Property.JCR_DATA)) {
                        return (AdapterType) adapt((Value) n.getProperty(Property.JCR_DATA).getValue());
                    }
                    if (n.hasNode(Node.JCR_CONTENT)) {
                        Node content = n.getNode(Node.JCR_CONTENT);
                        if (content.hasProperty(Property.JCR_DATA)) {
                            return (AdapterType) adapt((Value) content.getProperty(Property.JCR_DATA).getValue());
                        }
                    }
                } else if (o instanceof Value) {
                    return (AdapterType) adapt((Value) o);
                }
            } catch (Exception e) {

            }

        }
        return null;
    }

    private URI adapt(Value o) {
        for (Map.Entry<URIProvider, URIProvider> uriProviderEntry : providers.entrySet()) {
            URI u = uriProviderEntry.getValue().toURI(o);
            if ( u != null) {
                return u;
            }
        }
        return null;
    }

    private void bindProvider(URIProvider uriProvider) {
        providers.put(uriProvider, uriProvider);
    }
    private void unbindProvider(URIProvider uriProvider) {
        providers.remove(uriProvider);
    }

}
