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
package org.apache.sling.distribution.transport.impl;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.transport.DistributionTransportSecret;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = true,
        label = "Apache Sling Distribution Transport Credentials - User Credentials based DistributionTransportSecretProvider",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = DistributionTransportSecretProvider.class)
@Property(name="webconsole.configurationFactory.nameHint", value="Secret provider name: {name}")
public class UserCredentialsDistributionTransportSecretProvider implements
        DistributionTransportSecretProvider {

    /**
     * name of this component.
     */
    @Property(label = "Name")
    public static final String NAME = DistributionComponentConstants.PN_NAME;

    @Property(label = "User Name", description = "The name of the user used to perform remote actions.")
    private final static String USERNAME = "username";

    @Property(label = "Password", description = "The clear text password to perform authentication. Warning: storing clear text passwords is not safe.")
    private final static String PASSWORD = "password";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String username;
    private String password;

    @Activate
    protected void activate(Map<String, Object> config) {
        username = PropertiesUtil.toString(config.get(USERNAME), "").trim();
        password = PropertiesUtil.toString(config.get(PASSWORD), "").trim();
    }

    public DistributionTransportSecret getSecret(URI uri) {
        return new DistributionTransportSecret() {
            public String asToken() {
                return null;
            }

            public Map<String, String> asCredentialsMap() {
                Map<String, String> map = new HashMap<String, String>();
                map.put(USERNAME, username);
                map.put(PASSWORD, password);
                return Collections.unmodifiableMap(map);
            }
        };
    }
}
