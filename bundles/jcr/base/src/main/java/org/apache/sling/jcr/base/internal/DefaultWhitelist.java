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
package org.apache.sling.jcr.base.internal;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.LoginAdminWhitelist;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define the default whitelist in its own class to better
 * keep track of it. The goal is to reduce it to the bare
 * minimum over time.
 */
class DefaultWhitelist {
    static final String [] WHITELISTED_BSN = {
            "org.apache.sling.discovery.commons",
            "org.apache.sling.discovery.base",
            "org.apache.sling.discovery.oak",
            "org.apache.sling.extensions.webconsolesecurityprovider",
            "org.apache.sling.i18n",
            "org.apache.sling.installer.provider.jcr",
            "org.apache.sling.jcr.base",
            "org.apache.sling.jcr.contentloader",
            "org.apache.sling.jcr.davex",
            "org.apache.sling.jcr.jackrabbit.usermanager",
            "org.apache.sling.jcr.oak.server",
            "org.apache.sling.jcr.resource",
            "org.apache.sling.jcr.webconsole",
            "org.apache.sling.jcr.webdav",
            "org.apache.sling.junit.core",
            "org.apache.sling.resourceresolver",
            "org.apache.sling.scripting.core",
            "org.apache.sling.scripting.sightly",
            "org.apache.sling.servlets.post",
            "org.apache.sling.servlets.resolver",
            "org.apache.sling.xss"
    };
}