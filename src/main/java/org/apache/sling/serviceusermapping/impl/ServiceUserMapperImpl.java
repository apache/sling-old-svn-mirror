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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.apache.sling.serviceusermapping.ServiceUserValidator;
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
@Service(value={ServiceUserMapper.class, ServiceUserMapperImpl.class})
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

    private final List<ServiceUserValidator> validators = new CopyOnWriteArrayList<ServiceUserValidator>();

    private SortedMap<Mapping, Registration> activeRegistrations = new TreeMap<Mapping, Registration>();

    private BundleContext bundleContext;

    private ExecutorService executorService;

    public boolean registerAsync = true;

    @Activate
    @Modified
    synchronized void configure(BundleContext bundleContext, final Map<String, Object> config) {
        if (registerAsync && executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }

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

        RegistrationSet registrationSet = null;
        this.bundleContext = bundleContext;
        registrationSet = this.updateMappings();

        this.executeServiceRegistrationsAsync(registrationSet);
    }

    @Deactivate
    synchronized void deactivate() {
        RegistrationSet registrationSet = null;
        updateServiceRegistrations(new Mapping[0]);
        bundleContext = null;
        this.executeServiceRegistrationsAsync(registrationSet);
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    private void restartAllActiveServiceUserMappedServices() {
        RegistrationSet registrationSet = new RegistrationSet();
        registrationSet.removed = activeRegistrations.values();
        registrationSet.added = activeRegistrations.values();
        executeServiceRegistrationsAsync(registrationSet);
    }

    /**
     * bind the serviceUserValidator
     * @param serviceUserValidator
     */
    protected synchronized void bindServiceUserValidator(final ServiceUserValidator serviceUserValidator) {
        validators.add(serviceUserValidator);
        restartAllActiveServiceUserMappedServices();
    }

    /**
     * unbind the serviceUserValidator
     * @param serviceUserValidator
     */
    protected synchronized void unbindServiceUserValidator(final ServiceUserValidator serviceUserValidator) {
        validators.remove(serviceUserValidator);
        restartAllActiveServiceUserMappedServices();
    }

    /**
     * @see org.apache.sling.serviceusermapping.ServiceUserMapper#getServiceUserID(org.osgi.framework.Bundle, java.lang.String)
     */
    @Override
    public String getServiceUserID(final Bundle bundle, final String subServiceName) {
        final String serviceName = getServiceName(bundle);
        final String userId = internalGetUserId(serviceName, subServiceName);
        final boolean valid = isValidUser(userId, serviceName, subServiceName);
        final String result = valid ? userId : null;
        log.debug(
                "getServiceUserID(bundle {}, subServiceName {}) returns [{}] (raw userId={}, valid={})",
                new Object[] { bundle, subServiceName, result, userId, valid });
        return result;
    }

    protected synchronized void bindAmendment(final MappingConfigAmendment amendment, final Map<String, Object> props) {
        final Long key = (Long) props.get(Constants.SERVICE_ID);
        RegistrationSet registrationSet = null;
        amendments.put(key, amendment);
        registrationSet = this.updateMappings();
        executeServiceRegistrationsAsync(registrationSet);
    }

    protected synchronized void unbindAmendment(final MappingConfigAmendment amendment, final Map<String, Object> props) {
        final Long key = (Long) props.get(Constants.SERVICE_ID);
        RegistrationSet registrationSet = null;
        if ( amendments.remove(key) != null ) {
             registrationSet = this.updateMappings();
        }
        executeServiceRegistrationsAsync(registrationSet);
    }

    protected void updateAmendment(final MappingConfigAmendment amendment, final Map<String, Object> props) {
        this.bindAmendment(amendment, props);
    }

    protected RegistrationSet updateMappings() {
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

        RegistrationSet registrationSet = updateServiceRegistrations(activeMappings);

        return registrationSet;

    }


    RegistrationSet updateServiceRegistrations(final Mapping[] newMappings) {

        RegistrationSet result = new RegistrationSet();
        // do not do anything if not activated
        if (bundleContext == null) {
            return result;
        }

        final SortedSet<Mapping> orderedNewMappings = new TreeSet<Mapping>(Arrays.asList(newMappings));
        final SortedMap<Mapping, Registration> newRegistrations = new TreeMap<Mapping, Registration>();

        // keep those that are still mapped
        for (Map.Entry<Mapping, Registration> registrationEntry: activeRegistrations.entrySet()) {
            boolean keepEntry = true;

            if (!orderedNewMappings.contains(registrationEntry.getKey())) {
                Registration registration = registrationEntry.getValue();

                result.removed.add(registration);
                keepEntry = false;
            }

            if (keepEntry) {
                newRegistrations.put(registrationEntry.getKey(), registrationEntry.getValue());
            }
        }

        // add those that are new
        for (final Mapping mapping: orderedNewMappings) {
            if (!newRegistrations.containsKey(mapping)) {
                Registration registration = new Registration(mapping);
                newRegistrations.put(mapping, registration);
                result.added.add(registration);
            }
        }

        activeRegistrations = newRegistrations;

        return result;
    }

    private void executeServiceRegistrationsAsync(final RegistrationSet registrationSet) {

        if (executorService == null) {
            executeServiceRegistrations(registrationSet);
        } else {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    executeServiceRegistrations(registrationSet);
                }
            });
        }
    }


    private void executeServiceRegistrations(RegistrationSet registrationSet) {

        if (registrationSet == null) {
            return;
        }

        for (Registration registration : registrationSet.removed) {


            ServiceRegistration serviceRegistration = registration.setService(null);

            if (serviceRegistration != null) {
                try {
                    serviceRegistration.unregister();
                    log.debug("Unregistered ServiceUserMapped {}", registration.mapping);
                } catch (IllegalStateException e) {
                    log.error("cannot unregister ServiceUserMapped {}", registration.mapping,  e);
                }
            }
        }

        BundleContext savedBundleContext = bundleContext;

        if (savedBundleContext == null) {
            return;
        }

        for (Registration registration : registrationSet.added) {
            Mapping mapping = registration.mapping;
            final Dictionary<String, Object> properties = new Hashtable<String, Object>();
            if (mapping.getSubServiceName() != null) {
                properties.put(ServiceUserMapped.SUBSERVICENAME, mapping.getSubServiceName());
            }

            properties.put(Mapping.SERVICENAME, mapping.getServiceName());
            final ServiceRegistration serviceRegistration = savedBundleContext.registerService(ServiceUserMappedImpl.SERVICEUSERMAPPED,
                    new ServiceUserMappedImpl(), properties);

            ServiceRegistration oldServiceRegistration = registration.setService(serviceRegistration);
            log.debug("Activated ServiceUserMapped {}", registration.mapping);

            if (oldServiceRegistration != null) {
                try {
                    oldServiceRegistration.unregister();
                } catch (IllegalStateException e) {
                    log.error("cannot unregister ServiceUserMapped {}", registration.mapping,  e);
                }
            }
        }

    }

    private String internalGetUserId(final String serviceName, final String subServiceName) {
        // try with serviceInfo first
        for (final Mapping mapping : this.activeMappings) {
            final String userId = mapping.map(serviceName, subServiceName);
            if (userId != null) {
                log.debug("Got userId [{}] from {}/{}", new Object[] { userId, serviceName, subServiceName });
                return userId;
            }
        }

        // second round without serviceInfo
        for (Mapping mapping : this.activeMappings) {
            final String userId = mapping.map(serviceName, null);
            if (userId != null) {
                log.debug("Got userId [{}] from {}/<no subServiceName>", userId, serviceName);
                return userId;
            }
        }

        // finally, fall back to default user
        return this.defaultUser;
    }

    private boolean isValidUser(final String userId, final String serviceName, final String subServiceName) {
        if (userId == null) {
            log.debug("isValidUser: userId is null -> invalid");
            return false;
        }
        if ( !validators.isEmpty() ) {
            for (final ServiceUserValidator validator : validators) {
                if ( validator.isValid(userId, serviceName, subServiceName) ) {
                    log.debug("isValidUser: Validator {} accepts userId [{}] -> valid", validator, userId);
                    return true;
                }
            }
            log.debug("isValidUser: No validator accepte userId [{}] -> invalid", userId);
            return false;
        } else {
            log.debug("isValidUser: No active validators for userId [{}] -> valid", userId);
            return true;
        }
    }

    static String getServiceName(final Bundle bundle) {
        return bundle.getSymbolicName();
    }

    List<Mapping> getActiveMappings() {
        return Collections.unmodifiableList(Arrays.asList(activeMappings));
    }

    class Registration {
        private Mapping mapping;
        private ServiceRegistration serviceRegistration;


        Registration(Mapping mapping) {
            this.mapping = mapping;
            this.serviceRegistration = null;
        }

        synchronized ServiceRegistration setService(ServiceRegistration serviceRegistration) {
            ServiceRegistration oldServiceRegistration = this.serviceRegistration;
            this.serviceRegistration = serviceRegistration;
            return oldServiceRegistration;
        }
    }

    class RegistrationSet {
        Collection<Registration> added = new ArrayList<Registration>();
        Collection<Registration> removed = new ArrayList<Registration>();
    }
}

