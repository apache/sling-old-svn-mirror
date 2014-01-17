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
package org.apache.sling.replication.transport.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.communication.ReplicationEndpoint;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilder;
import org.apache.sling.replication.transport.ReplicationTransportException;
import org.apache.sling.replication.transport.TransportHandler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

@Component(metatype = true,
        label = "Replication Http Transport Handler Factory",
        description = "OSGi configuration based HttpTransportHandler service factory",
        name = HttpTransportHandlerFactory.SERVICE_PID,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
public class HttpTransportHandlerFactory {
    static final String SERVICE_PID = "org.apache.sling.replication.transport.impl.HttpTransportHandlerFactory";


    @Property(boolValue = true)
    private static final String ENABLED = "enabled";

    @Property
    private static final String NAME = ReplicationAgentConfiguration.NAME;

    @Property(boolValue = false)
    private static final String USE_CUSTOM_HEADERS = "useCustomHeaders";

    @Property(cardinality = 50)
    private static final String CUSTOM_HEADERS = "customHeaders";

    @Property(boolValue = false)
    private static final String USE_CUSTOM_BODY = "useCustomBody";

    @Property
    private static final String CUSTOM_BODY = "customBody";

    private ServiceRegistration serviceRegistration;

    @Activate
    public void activate(BundleContext context, Map<String, ?> config) throws Exception {

        // inject configuration
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        boolean enabled = PropertiesUtil.toBoolean(config.get(ENABLED), true);
        if (enabled) {
            String name = PropertiesUtil
                    .toString(config.get(NAME), String.valueOf(new Random().nextInt(1000)));
            props.put(NAME, name);

            boolean useCustomHeaders = PropertiesUtil.toBoolean(config.get(USE_CUSTOM_HEADERS), false);
            props.put(USE_CUSTOM_HEADERS, useCustomHeaders);

            String[] customHeaders = PropertiesUtil.toStringArray(config.get(CUSTOM_HEADERS), new String[0]);
            props.put(CUSTOM_HEADERS, customHeaders);

            boolean useCustomBody = PropertiesUtil.toBoolean(config.get(USE_CUSTOM_BODY), false);
            props.put(USE_CUSTOM_BODY, useCustomBody);

            String customBody = PropertiesUtil.toString(config.get(CUSTOM_BODY), "");
            props.put(CUSTOM_BODY, customBody);

            // register transport handler
            TransportHandler transportHandler = new HttpTransportHandler(useCustomHeaders, customHeaders,  useCustomBody, customBody);

            serviceRegistration = context.registerService(TransportHandler.class.getName(), transportHandler , props);
        }
    }

    @Deactivate
    private void deactivate() {
        if(serviceRegistration != null){
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
