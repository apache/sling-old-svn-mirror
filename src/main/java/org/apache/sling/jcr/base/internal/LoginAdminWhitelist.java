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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
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

    // for backwards compatibility only
    private volatile WhitelistFragment defaultFragment;

    // for backwards compatibility only
    private volatile WhitelistFragment additionalFragment;

    private final List<WhitelistFragment> whitelistFragments = new CopyOnWriteArrayList<>();

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    ) @SuppressWarnings("unused")
    void bindWhitelistFragment(WhitelistFragment fragment) {
        whitelistFragments.add(fragment);
        LOG.info("WhitelistFragment added '{}'", fragment);
    }

    @SuppressWarnings("unused")
    void unbindWhitelistFragment(WhitelistFragment fragment) {
        whitelistFragments.remove(fragment);
        LOG.info("WhitelistFragment removed '{}'", fragment);
    }

    @Activate @Modified @SuppressWarnings("unused")
    void configure(LoginAdminWhitelistConfiguration configuration) {
        this.config = new ConfigurationState(configuration);
        backwardsCompatibility(configuration);
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
        }

        for (final WhitelistFragment fragment : whitelistFragments) {
            if (fragment.allows(bsn)) {
                LOG.debug("{} is whitelisted to use loginAdministrative, by whitelist fragment '{}'",
                        bsn, fragment);
                return true;
            }
        }

        LOG.debug("{} is not whitelisted to use loginAdministrative", bsn);
        return false;
    }

    // encapsulate configuration state for atomic configuration updates
    private static class ConfigurationState {

        private final boolean bypassWhitelist;

        private final Pattern whitelistRegexp;

        private ConfigurationState(final LoginAdminWhitelistConfiguration config) {
            final String regexp = config.whitelist_bundles_regexp();
            if(regexp.trim().length() > 0) {
                whitelistRegexp = Pattern.compile(regexp);
                LOG.warn("A whitelist.bundles.regexp is configured, this is NOT RECOMMENDED for production: {}",
                        whitelistRegexp);
            } else {
                whitelistRegexp = null;
            }

            bypassWhitelist = config.whitelist_bypass();
            if(bypassWhitelist) {
                LOG.info("bypassWhitelist=true, whitelisted BSNs=<ALL>");
                LOG.warn("All bundles are allowed to use loginAdministrative due to the 'bypass whitelist' " +
                        "configuration of this service. This is NOT RECOMMENDED, for security reasons."
                );
            }
        }
    }

    @SuppressWarnings("deprecated")
    private void backwardsCompatibility(final LoginAdminWhitelistConfiguration configuration) {
        if (defaultFragment != null) {
            unbindWhitelistFragment(defaultFragment);
        }
        if (additionalFragment != null) {
            unbindWhitelistFragment(additionalFragment);
        }
        final String[] defaultBsns = configuration.whitelist_bundles_default();
        if (defaultBsns != null && defaultBsns.length != 0) {
            LOG.warn("Using deprecated configuration property 'whitelist.bundles.default'");
            defaultFragment = new WhitelistFragment("deprecated-whitelist.bundles.default", defaultBsns);
            bindWhitelistFragment(defaultFragment);
        }

        final String[] additionalBsns = configuration.whitelist_bundles_additional();
        if (additionalBsns != null && additionalBsns.length != 0) {
            LOG.warn("Using deprecated configuration property 'whitelist.bundles.additional'");
            additionalFragment = new WhitelistFragment("deprecated-whitelist.bundles.additional", additionalBsns);
            bindWhitelistFragment(additionalFragment);
        }
    }
}
