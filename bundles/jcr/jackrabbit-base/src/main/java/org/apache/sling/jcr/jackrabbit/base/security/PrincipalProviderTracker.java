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

package org.apache.sling.jcr.jackrabbit.base.security;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrincipalProviderTracker extends ServiceTracker implements PrincipalProviderRegistry{
    /**
     * Property-Key if the <code>PrincipalProvider</code> configured with
     * {@link LoginModuleConfig#PARAM_PRINCIPAL_PROVIDER_CLASS} be registered using the
     * specified name instead of the class name which is used by default if the
     * name parameter is missing.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2629">JCR-2629</a>
     */
    private static final String COMPAT_PRINCIPAL_PROVIDER_NAME = "principal_provider.name";
    public static final PrincipalProvider[] EMPTY_ARRAY = new PrincipalProvider[0];

    private Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, PrincipalProvider> providers = new ConcurrentHashMap<String, PrincipalProvider>();
    private final Map<ServiceReference, String> refToNameMapping = new ConcurrentHashMap<ServiceReference, String>();
    private PrincipalProvider[] providerArray = EMPTY_ARRAY;

    public PrincipalProviderTracker(BundleContext context) {
        super(context, PrincipalProvider.class.getName(), null);
    }

    //~-------------------------------------< ServiceTracker >

    @Override
    public Object addingService(ServiceReference reference) {
        PrincipalProvider provider = (PrincipalProvider) super.addingService(reference);
        addProvider(provider,reference);
        reloadProviders();
        return provider;
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        //Check if the name has changed then re-register the provider
        String newName = getProviderName((PrincipalProvider) service,reference);
        String oldName = refToNameMapping.get(reference);
        if(!equals(newName,oldName)){
            if(oldName != null){
                providers.remove(oldName);
            }
            addProvider((PrincipalProvider) service, reference);
            reloadProviders();
        }
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        String name = refToNameMapping.remove(reference);
        if(name != null){
            providers.remove(name);
        }
        reloadProviders();
    }

    @Override
    public void close() {
        super.close();
        providers.clear();
        refToNameMapping.clear();
        providerArray = EMPTY_ARRAY;
    }

    //~-------------------------------------< PrincipalProviderRegistry >

    public PrincipalProvider registerProvider(Properties configuration) throws RepositoryException {
        throw new UnsupportedOperationException("The PrincipalProvider are only registered as OSGi services");
    }

    public PrincipalProvider getDefault() {
        throw new UnsupportedOperationException("Default provider is handled via WorkspaceBasedPrincipalProviderRegistry");
    }

    public PrincipalProvider getProvider(String className) {
        return providers.get(className);
    }

    public PrincipalProvider[] getProviders() {
        return providerArray;
    }

    private void addProvider(PrincipalProvider provider,ServiceReference reference){
        String providerName = getProviderName(provider,reference);
        if(providers.containsKey(providerName)){
            log.warn("Provider with name {} is already registered. PrincipalProvider {} " +
                    "would not be registered",providerName,reference);
            return;
        }
        providers.put(providerName,provider);
        refToNameMapping.put(reference,providerName);
    }

    private void reloadProviders() {
        PrincipalProvider[] providerArray = providers.values().toArray(new PrincipalProvider[providers.size()]);
        synchronized (this){
            this.providerArray = providerArray;
        }
    }

    private static String getProviderName(PrincipalProvider provider,ServiceReference ref){
        String providerName = (String) ref.getProperty(COMPAT_PRINCIPAL_PROVIDER_NAME);
        if(providerName == null){
            providerName = provider.getClass().getName();
        }
        return providerName;
    }

    private static boolean equals(Object object1, Object object2) {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        return object1.equals(object2);
    }

}
