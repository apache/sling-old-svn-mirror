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
package org.apache.sling.discovery.impl.standalone;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.discovery.PropertyProvider;
import org.osgi.framework.Constants;

/**
 * Internal class caching some provider infos like service id and ranking.
 */
public class ProviderInfo implements Comparable<ProviderInfo> {

    public final PropertyProvider provider;
    public final int ranking;
    public final long serviceId;
    public final Map<String, String> properties = new HashMap<String, String>();

    public ProviderInfo(final PropertyProvider provider, final Map<String, Object> serviceProps) {
        this.provider = provider;
        final Object sr = serviceProps.get(Constants.SERVICE_RANKING);
        if ( sr == null || !(sr instanceof Integer)) {
            this.ranking = 0;
        } else {
            this.ranking = (Integer)sr;
        }
        this.serviceId = (Long)serviceProps.get(Constants.SERVICE_ID);
        final Object namesObj = serviceProps.get(PropertyProvider.PROPERTY_PROPERTIES);
        if ( namesObj instanceof String ) {
            final String val = provider.getProperty((String)namesObj);
            if ( val != null ) {
                this.properties.put((String)namesObj, val);
            }
        } else if ( namesObj instanceof String[] ) {
            for(final String name : (String[])namesObj ) {
                final String val = provider.getProperty(name);
                if ( val != null ) {
                    this.properties.put(name, val);
                }
            }
        }
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final ProviderInfo o) {
        // Sort by rank in ascending order.
        if ( this.ranking < o.ranking ) {
            return -1; // lower rank
        } else if (this.ranking > o.ranking ) {
            return 1; // higher rank
        }
        // If ranks are equal, then sort by service id in descending order.
        return (this.serviceId < o.serviceId) ? 1 : -1;
    }

    @Override
    public boolean equals(final Object obj) {
        if ( obj instanceof ProviderInfo ) {
            return ((ProviderInfo)obj).serviceId == this.serviceId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return provider.hashCode();
    }
}
