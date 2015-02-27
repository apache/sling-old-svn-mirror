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
package org.apache.sling.serviceusermapping.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.apache.sling.serviceusermapping.ServiceUserValidator;
import org.apache.sling.serviceusermapping.ServiceUserMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        metatype = true,
        label = "Apache Sling Service User Mapper Service",
        description = "Configuration for the service mapping service names to names of users.")
@Service(value=ServiceUserMapper.class)
@References( {
    @Reference(name="amendment",
            referenceInterface=MappingConfigAmendment.class,
            cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy=ReferencePolicy.DYNAMIC,
            updated="updateAmendment"),
    @Reference(name = "serviceUserValidator ", referenceInterface = ServiceUserValidator.class,
    bind = "bindServiceUserValidator", unbind = "unbindServiceUserValidator",
    cardinality= ReferenceCardinality.OPTIONAL_MULTIPLE, policy= ReferencePolicy.DYNAMIC)

})

public class ServiceUserMapperImpl implements ServiceUserMapper {

    @Property(
            label = "Service Mappings",
            description = "Provides mappings from service name to user names. "
                + "Each entry is of the form 'bundleId [ \":\" subServiceName ] \"=\" userName' "
                + "where bundleId and subServiceName identify the service and userName "
                + "defines the name of the user to provide to the service. Invalid entries are logged and ignored.",
            unbounded = PropertyUnbounded.ARRAY)
    private static final String PROP_SERVICE2USER_MAPPING = "user.mapping";

    private static final String[] PROP_SERVICE2USER_MAPPING_DEFAULT = {};

    private static final String PROP_DEFAULT_USER = "user.default";

    @Property(
            name = PROP_DEFAULT_USER,
            label = "Default User",
            description = "The name of the user to use as the default if no service mapping"
                + "applies. If this property is missing or empty no default user is defined.")
    private static final String PROP_DEFAULT_USER_DEFAULT = null;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Mapping[] globalServiceUserMappings = new Mapping[0];

    private String defaultUser;

    private Map<Long, MappingConfigAmendment> amendments = new HashMap<Long, MappingConfigAmendment>();

    private Mapping[] activeMappings = new Mapping[0];

    private Vector <ServiceUserValidator> validators = new Vector<ServiceUserValidator>();

    private SortedMap<Mapping, ServiceRegistration> activeMappingRegistrations = new TreeMap<Mapping, ServiceRegistration>();

    private BundleContext bundleContext;

    @Activate
    @Modified
    void configure(BundleContext bundleContext, final Map<String, Object> config) {
        final String[] props = PropertiesUtil.toStringArray(config.get(PROP_SERVICE2USER_MAPPING),
            PROP_SERVICE2USER_MAPPING_DEFAULT);

        final ArrayList<Mapping> mappings = new ArrayList<Mapping>(props.length);
        for (final String prop : props) {
            if (prop != null && prop.trim().length() > 0 ) {
                try {
                    final Mapping mapping = new Mapping(prop.trim());
                    mappings.add(mapping);
                } catch (final IllegalArgumentException iae) {
                    log.error("configure: Ignoring '{}': {}", prop, iae.getMessage());
                }
            }
        }

        this.globalServiceUserMappings = mappings.toArray(new Mapping[mappings.size()]);
        this.defaultUser = PropertiesUtil.toString(config.get(PROP_DEFAULT_USER), PROP_DEFAULT_USER_DEFAULT);
        synchronized ( this.amendments ) {
            this.bundleContext = bundleContext;
            this.updateMappings();
        }
    }

    @Deactivate
    void deactivate() {
        synchronized ( this.amendments) {
            updateServiceMappings(new ArrayList<Mapping>());
            bundleContext = null;
        }
    }
    
    /**
     * bind the serviceUserValidator
     * @param serviceUserValidator
     * @param properties
     */
    protected void bindServiceUserValidator(final ServiceUserValidator serviceUserValidator, final Map<String, Object> properties){
        validators.add(serviceUserValidator);
    }
    
    /**
     * unbind the serviceUserValidator
     * @param serviceUserValidator
     * @param properties
     */
    protected void unbindServiceUserValidator(final ServiceUserValidator serviceUserValidator, final Map<String, Object> properties){
        validators.remove(serviceUserValidator);
    }

    /**
     * @see org.apache.sling.serviceusermapping.ServiceUserMapper#getServiceUserID(org.osgi.framework.Bundle, java.lang.String)
     */
    public String getServiceUserID(final Bundle bundle, final String subServiceName) {
        final String serviceName = bundle.getSymbolicName();
        final String userId = internalGetUserId(serviceName, subServiceName);
        return isValidUser(userId, serviceName, subServiceName) ? userId : null;
    }

    protected void bindAmendment(final MappingConfigAmendment amendment, final Map<String, Object> props) {
        final Long key = (Long) props.get(Constants.SERVICE_ID);
        synchronized ( this.amendments ) {
            amendments.put(key, amendment);
            this.updateMappings();
        }
    }

    protected void unbindAmendment(final MappingConfigAmendment amendment, final Map<String, Object> props) {
        final Long key = (Long) props.get(Constants.SERVICE_ID);
        synchronized ( this.amendments ) {
            if ( amendments.remove(key) != null ) {
                this.updateMappings();
            }
        }

    }

    protected void updateAmendment(final MappingConfigAmendment amendment, final Map<String, Object> props) {
        this.bindAmendment(amendment, props);
    }

    protected void updateMappings() {
        final List<MappingConfigAmendment> sortedMappings = new ArrayList<MappingConfigAmendment>();
        for(final MappingConfigAmendment amendment : this.amendments.values() ) {
            sortedMappings.add(amendment);
        }
        Collections.sort(sortedMappings);

        final List<Mapping> mappings = new ArrayList<Mapping>();
        for(final Mapping m : this.globalServiceUserMappings) {
            mappings.add(m);
        }
        for(final MappingConfigAmendment mca : sortedMappings) {
            for(final Mapping m : mca.getServiceUserMappings()) {
                mappings.add(m);
            }
        }


        activeMappings = mappings.toArray(new Mapping[mappings.size()]);

        updateServiceMappings(mappings);

    }


    void updateServiceMappings(List<Mapping> newMappings) {
        
        // do not do anything if not activated
        if (bundleContext == null) {
            return;
        }

        SortedSet<Mapping> orderedActiveMappings = new TreeSet<Mapping>(newMappings);


        Iterator<Map.Entry<Mapping, ServiceRegistration>> it = activeMappingRegistrations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Mapping, ServiceRegistration> registrationEntry = it.next();

            if (!orderedActiveMappings.contains(registrationEntry.getKey())) {
                registrationEntry.getValue().unregister();
                it.remove();
            }
        }


        for (Mapping mapping: orderedActiveMappings) {
            if (!activeMappingRegistrations.containsKey(mapping)) {
                Dictionary<String, Object> properties = new Hashtable<String, Object>();
                if (mapping.getSubServiceName() != null) {
                    properties.put(ServiceUserMapping.SUBSERVICENAME, mapping.getSubServiceName());
                }

                properties.put(Mapping.SERVICENAME, mapping.getServiceName());
                ServiceRegistration registration = bundleContext.registerService(ServiceUserMapping.class.getName(), mapping, properties);
                activeMappingRegistrations.put(mapping, registration);
            }
        }
    }

    private String internalGetUserId(String serviceName, String subServiceName) {
        // try with serviceInfo first
        for (Mapping mapping : this.activeMappings) {
            final String userId = mapping.map(serviceName, subServiceName);
            if (userId != null) {
                return userId;
            }
        }

        // second round without serviceInfo
        for (Mapping mapping : this.activeMappings) {
            final String userId = mapping.map(serviceName, null);
            if (userId != null) {
                return userId;
            }
        }

        // finally, fall back to default user
        return this.defaultUser;
    }

    private boolean isValidUser(String userId, String serviceName, String subServiceName) {
        if (userId == null) {
            return false;
        }
        if (validators != null && validators.size() > 0) {
            for (ServiceUserValidator validator : validators) {
                boolean valid = validator.isValid(userId, serviceName, subServiceName);
                if (!valid) {
                    return false;
                }
            }
        }
        return true;
    }
}

