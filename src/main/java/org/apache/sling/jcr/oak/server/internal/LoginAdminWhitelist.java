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
package org.apache.sling.jcr.oak.server.internal;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
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
@org.osgi.service.component.annotations.Component(
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

    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean bypassWhitelist;

    private Pattern whitelistRegexp;

    private Set<String> whitelistedBsn;

    @Activate
    void activate(LoginAdminWhitelistConfiguration config) {
        whitelistedBsn = new TreeSet<String>();

        if (config.whitelist_bundles_default() != null) {
            whitelistedBsn.addAll(Arrays.asList(config.whitelist_bundles_default()));
        }
        if (config.whitelist_bundles_additional() != null) { // null check due to FELIX-5404
            whitelistedBsn.addAll(Arrays.asList(config.whitelist_bundles_additional()));
        }

        final String regexp = config.whitelist_bundles_regexp();
        if(regexp.trim().length() > 0) {
            whitelistRegexp = Pattern.compile(regexp);
            log.warn("A whitelist.bundles.regexp is configured, this is NOT RECOMMENDED for production: {}", whitelistRegexp);
        } else {
            whitelistRegexp = null;
        }

        bypassWhitelist = config.whitelist_bypass();
        if(bypassWhitelist) {
            log.info("bypassWhitelist=true, whitelisted BSNs=<ALL>");
            log.warn(
                "All bundles are allowed to use loginAdministrative due to the 'bypass whitelist' configuration"
                + " of this service. This is NOT RECOMMENDED, for security reasons."
            );
        } else {
            log.info("bypassWhitelist=false, whitelisted BSNs({})={}", whitelistedBsn.size(), whitelistedBsn);
        }
    }

    boolean allowLoginAdministrative(Bundle b) {
        if(bypassWhitelist) {
            log.debug("Whitelist is bypassed, all bundles allowed to use loginAdministrative");
            return true;
        }

        final String bsn = b.getSymbolicName();
        if(whitelistRegexp != null && whitelistRegexp.matcher(bsn).matches()) {
            log.debug("{} is whitelisted to use loginAdministrative, by regexp", bsn);
            return true;
        } else if(whitelistedBsn.contains(bsn)) {
            log.debug("{} is whitelisted to use loginAdministrative, by explicit whitelist", bsn);
            return true;
        }
        log.debug("{} is not whitelisted to use loginAdministrative", bsn);
        return false;
    }
}
