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
package org.apache.sling.discovery.impl.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;

/**
 * This service provides the standard instance properties (if available)
 */
@Component(immediate=true)
@Reference(referenceInterface=HttpService.class,
           cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
           policy=ReferencePolicy.DYNAMIC)
public class StandardPropertyProvider {

    /** Endpoint service registration property from RFC 189 */
    private static final String REG_PROPERTY_ENDPOINTS = "osgi.http.service.endpoints";

    private volatile long changeCount;

    private ServiceRegistration propagationService;

    private final Map<Long, String[]> endpoints = new HashMap<Long, String[]>();

    private String endpointString;

    @Reference SlingSettingsService settings;

    private Dictionary<String, Object> getRegistrationProperties() {
        final List<String> names = new ArrayList<String>();
        names.add(InstanceDescription.PROPERTY_NAME);
        names.add(InstanceDescription.PROPERTY_DESCRIPTION);
        names.add(InstanceDescription.PROPERTY_ENDPOINTS);

        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        synchronized ( this.endpoints ) {
            for(final String[] points : endpoints.values()) {
                for(final String point : points) {
                    if ( first ) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    sb.append(point);
                }
            }
        }
        this.endpointString = sb.toString();

        final Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
        serviceProps.put(PropertyProvider.PROPERTY_PROPERTIES, names.toArray(new String[names.size()]));
        // we add a changing property to the service registration
        // to make sure a modification event is really sent
        synchronized ( this ) {
            serviceProps.put("changeCount", this.changeCount++);
        }
        return serviceProps;
    }

    @Activate
    protected void activate(final ComponentContext cc) {
        this.modified(cc);
    }

    @Modified
    protected void modified(final ComponentContext cc) {
        this.propagationService = cc.getBundleContext().registerService(PropertyProvider.class.getName(),
                new PropertyProvider() {

                    public String getProperty(final String name) {
                        if ( InstanceDescription.PROPERTY_DESCRIPTION.equals(name) ) {
                            return settings.getSlingDescription();
                        }
                        if ( InstanceDescription.PROPERTY_NAME.equals(name) ) {
                            return settings.getSlingName();
                        }
                        if ( InstanceDescription.PROPERTY_ENDPOINTS.equals(name) ) {
                            return endpointString;
                        }
                        return null;
                    }
                }, this.getRegistrationProperties());
    }

    @Deactivate
    protected void deactivate() {
        if ( this.propagationService != null ) {
            this.propagationService.unregister();
            this.propagationService = null;
        }
    }

    /**
     * Bind a http service
     */
    protected void bindHttpService(final ServiceReference reference) {
        final String[] endpointUrls = toStringArray(reference.getProperty(REG_PROPERTY_ENDPOINTS));
        if ( endpointUrls != null ) {
            synchronized ( this.endpoints ) {
                this.endpoints.put((Long)reference.getProperty(Constants.SERVICE_ID), endpointUrls);
            }
            if ( this.propagationService != null ) {
                this.propagationService.setProperties(this.getRegistrationProperties());
            }
        }
    }

    /**
     * Unbind a http service
     */
    protected void unbindHttpService(final ServiceReference reference) {
        boolean changed = false;
        synchronized ( this.endpoints ) {
            if ( this.endpoints.remove(reference.getProperty(Constants.SERVICE_ID)) != null ) {
                changed = true;
            }
        }
        if ( changed && this.propagationService != null ) {
            this.propagationService.setProperties(this.getRegistrationProperties());
        }
    }

    private String[] toStringArray(final Object propValue) {
        if (propValue == null) {
            // no value at all
            return null;

        } else if (propValue instanceof String) {
            // single string
            return new String[] { (String) propValue };

        } else if (propValue instanceof String[]) {
            // String[]
            return (String[]) propValue;

        } else if (propValue.getClass().isArray()) {
            // other array
            Object[] valueArray = (Object[]) propValue;
            List<String> values = new ArrayList<String>(valueArray.length);
            for (Object value : valueArray) {
                if (value != null) {
                    values.add(value.toString());
                }
            }
            return values.toArray(new String[values.size()]);

        } else if (propValue instanceof Collection<?>) {
            // collection
            Collection<?> valueCollection = (Collection<?>) propValue;
            List<String> valueList = new ArrayList<String>(valueCollection.size());
            for (Object value : valueCollection) {
                if (value != null) {
                    valueList.add(value.toString());
                }
            }
            return valueList.toArray(new String[valueList.size()]);
        }

        return null;
    }
}
