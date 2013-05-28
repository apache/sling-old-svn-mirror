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
package org.apache.sling.discovery.impl.common.resource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.impl.common.DefaultClusterViewImpl;
import org.apache.sling.discovery.impl.common.DefaultInstanceDescriptionImpl;

/**
 * An InstanceDescription which reads the properties from the according location
 * in the repository
 */
public class EstablishedInstanceDescription extends
        DefaultInstanceDescriptionImpl {

    public EstablishedInstanceDescription(final DefaultClusterViewImpl clusterView,
            final Resource res, final String slingId, final boolean isLeader, final boolean isOwn) {
        super(clusterView, isLeader, isOwn, slingId, null);

        readProperties(res);
    }

    /**
     * Read the properties from the repository
     */
    private void readProperties(final Resource res) {
        final Map<String, String> props = new HashMap<String, String>();
        if (res != null) {
            final Resource propertiesChild = res.getChild("properties");
            if (propertiesChild != null) {
                final ValueMap properties = propertiesChild.adaptTo(ValueMap.class);
                if (properties != null) {
                    for (Iterator<String> it = properties.keySet().iterator(); it
                            .hasNext();) {
                        String key = it.next();
                        if (!key.equals("jcr:primaryType")) {
                            props.put(key, properties.get(key, String.class));
                        }
                    }
                }
            }
        }
        setProperties(props);
    }
}
