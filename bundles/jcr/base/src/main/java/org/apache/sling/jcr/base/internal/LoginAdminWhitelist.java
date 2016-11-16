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
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Whitelist that defines which bundles can use the
 * {@link SlingRepository#loginAdministrative} method.
 *
 * The default configuration lets a few trusted Sling bundles
 * use the loginAdministrative method.
 */
@Component(
        service = LoginAdminWhitelist.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Apache Sling Login Admin Whitelist",
                Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
        }
)
@Designate(
        ocd = LoginAdminWhitelistConfiguration.class
)
public class LoginAdminWhitelist {

    private static final Logger LOG = LoggerFactory.getLogger(LoginAdminWhitelist.class);

    private volatile ConfigurationState config;

    @Activate @Modified
    void configure(LoginAdminWhitelistConfiguration configuration) {
        this.config = new ConfigurationState(configuration);
    }

    public boolean allowLoginAdministrative(Bundle b) {
        if (config == null) {
            throw new IllegalStateException("LoginAdminWhitelist has no configuration.");
        }
        // create local copy of ConfigurationState to avoid reading mixed configurations during an configure
        final ConfigurationState localConfig = this.config;
        if(localConfig.bypassWhitelist) {
            LOG.debug("Whitelist is bypassed, all bundles allowed to use loginAdministrative");
            return true;
        }

        final String bsn = b.getSymbolicName();
        if(localConfig.whitelistRegexp != null && localConfig.whitelistRegexp.matcher(bsn).matches()) {
            LOG.debug("{} is whitelisted to use loginAdministrative, by regexp", bsn);
            return true;
        } else if(localConfig.whitelistedBsn.contains(bsn)) {
            LOG.debug("{} is whitelisted to use loginAdministrative, by explicit whitelist", bsn);
            return true;
        }
        LOG.debug("{} is not whitelisted to use loginAdministrative", bsn);
        return false;
    }

    // encapsulate configuration state for atomic configuration updates
    private static class ConfigurationState {
        private final TreeSet<String> whitelistedBsn;
        private final Pattern whitelistRegexp;
        private final boolean bypassWhitelist;

        private ConfigurationState(final LoginAdminWhitelistConfiguration config) {
            whitelistedBsn = new TreeSet<String>();
            if (config.whitelist_bundles_default() != null) { // null check due to FELIX-5404
                whitelistedBsn.addAll(Arrays.asList(config.whitelist_bundles_default()));
            }
            if (config.whitelist_bundles_additional() != null) {
                whitelistedBsn.addAll(Arrays.asList(config.whitelist_bundles_additional()));
            }

            final String regexp = config.whitelist_bundles_regexp();
            if(regexp.trim().length() > 0) {
                whitelistRegexp = Pattern.compile(regexp);
                LOG.warn("A whitelist.bundles.regexp is configured, this is NOT RECOMMENDED for production: {}", whitelistRegexp);
            } else {
                whitelistRegexp = null;
            }

            bypassWhitelist = config.whitelist_bypass();
            if(bypassWhitelist) {
                LOG.info("bypassWhitelist=true, whitelisted BSNs=<ALL>");
                LOG.warn("All bundles are allowed to use loginAdministrative due to the 'bypass whitelist' " +
                        "configuration of this service. This is NOT RECOMMENDED, for security reasons."
                );
            } else {
                LOG.info("bypassWhitelist=false, whitelisted BSNs({})={}", whitelistedBsn.size(), whitelistedBsn);
            }
        }
    }
}
