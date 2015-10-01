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
package org.apache.sling.resourceresolver.impl.providers;

import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.ServiceReference;

/**
 * Information about a registered resource provider
 */
public class ResourceProviderInfo implements Comparable<ResourceProviderInfo> {

    private final ServiceReference ref;

    private final String path;

    private final String name;

    private final boolean useResourceAccessSecurity;

    private final AuthType authType;

    private final boolean modifiable;

    private final boolean adaptable;

    private final boolean refreshable;

    private final boolean attributable;

    private final boolean supportsJcrQuery;

    private final boolean supportsNativeQuery;

    public ResourceProviderInfo(final ServiceReference ref) {
        this.ref = ref;
        this.path = PropertiesUtil.toString(ref.getProperty(ResourceProvider.PROPERTY_ROOT), "");
        this.name = PropertiesUtil.toString(ref.getProperty(ResourceProvider.PROPERTY_NAME), null);
        this.useResourceAccessSecurity = PropertiesUtil.toBoolean(ref.getProperty(ResourceProvider.PROPERTY_USE_RESOURCE_ACCESS_SECURITY), false);
        final String authType = PropertiesUtil.toString(ref.getProperty(ResourceProvider.PROPERTY_AUTHENTICATE), AuthType.no.name());
        AuthType aType = null;
        try {
            aType = AuthType.valueOf(authType);
        } catch ( final IllegalArgumentException iae) {
            // ignore
        }
        this.authType = aType;
        this.modifiable = PropertiesUtil.toBoolean(ref.getProperty(ResourceProvider.PROPERTY_MODIFIABLE), false);
        this.adaptable = PropertiesUtil.toBoolean(ref.getProperty(ResourceProvider.PROPERTY_ADAPTABLE), false);
        this.refreshable = PropertiesUtil.toBoolean(ref.getProperty(ResourceProvider.PROPERTY_REFRESHABLE), false);
        this.attributable = PropertiesUtil.toBoolean(ref.getProperty(ResourceProvider.PROPERTY_ATTRIBUTABLE), false);
        this.supportsJcrQuery = PropertiesUtil.toBoolean(ref.getProperty(ResourceProvider.PROPERTY_SUPPORTS_JCR_QUERY), false);
        this.supportsNativeQuery = PropertiesUtil.toBoolean(ref.getProperty(ResourceProvider.PROPERTY_SUPPORTS_NATIVE_QUERY), false);
    }

    public boolean isValid() {
        // TODO - do real path check
        if ( !path.startsWith("/") ) {
            return false;
        }
        if ( this.authType == null ) {
            return false;
        }
        return true;
    }

    public ServiceReference getServiceReference() {
        return this.ref;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public int compareTo(final ResourceProviderInfo o) {
        int result = path.compareTo(o.path);
        if ( result == 0 ) {
            result = o.ref.compareTo(ref);
        }
        return result;
    }

    @Override
    public String toString() {
        return "ResourceProviderInfo [ref=" + ref + ", path=" + path + ", useResourceAccessSecurity="
                + useResourceAccessSecurity + ", authType=" + authType + ", modifiable=" + modifiable + "]";
    }

    public AuthType getAuthType() {
        return this.authType;
    }

    public boolean getModifiable() {
        return this.modifiable;
    }

    public boolean isAdaptable() {
        return adaptable;
    }

    public boolean isRefreshable() {
        return refreshable;
    }

    public boolean isAttributable() {
        return attributable;
    }

    public boolean isSupportsJcrQuery() {
        return supportsJcrQuery;
    }

    public boolean isSupportsNativeQuery() {
        return supportsNativeQuery;
    }

    public String getName() {
        return this.name;
    }

    public boolean getUseResourceAccessSecurity() {
        return this.useResourceAccessSecurity;
    }
}
