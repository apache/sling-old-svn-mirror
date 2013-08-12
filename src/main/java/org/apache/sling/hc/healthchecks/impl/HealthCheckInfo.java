/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.healthchecks.impl;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.Constants;

// TODO move to services bundle

/** Utility that copies useful HealthCheck service
 *  properties to a Map that can be used as the Result's
 *  info(). Copies all service properties that have names
 *  that start with the {#Constants.HC_PROP_PREFIX} */
public class HealthCheckInfo extends HashMap<String, String> {
    private static final long serialVersionUID = 8661195387931574705L;

    public HealthCheckInfo(Dictionary<?, ?> serviceProperties) {
        put(Constants.HC_NAME, PropertiesUtil.toString(serviceProperties.get(Constants.HC_NAME), ""));
        put(Constants.HC_MBEAN_NAME, PropertiesUtil.toString(serviceProperties.get(Constants.HC_MBEAN_NAME), ""));
        put(Constants.HC_TAGS, 
                Arrays.asList(PropertiesUtil.toStringArray(serviceProperties.get(Constants.HC_TAGS), new String[] {})).toString());
    }
}