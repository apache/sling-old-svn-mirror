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
 * Whitelist that defines which bundles can use the
 * {@link SlingRepository#loginAdministrative} method.
 *
 * The default configuration lets a few trusted Sling bundles
 * use the loginAdministrative method.
 */
@Service(value=LoginAdminWhitelist.class)
@Component(
        label="Apache Sling Login Admin Whitelist",
        description="Defines which bundles can use SlingRepository.loginAdministrative()",
        metatype=true)
public class LoginAdminWhitelistImpl implements LoginAdminWhitelist {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Need to allow for bypassing the whitelist, for backwards
     *  compatibility with previous Sling versions which didn't
     *  implement it. Setting this to true is not recommended
     *  and logged as a warning.
     */
    @Property(
            label="Bypass the whitelist",
            description=
                "Allow all bundles to use loginAdministrative(). "
                + "Should ONLY be used for backwards compatibility reasons and "
                + "if you are aware of the related security risks.",
            boolValue=false)
    public static final String PROP_BYPASS_WHITELIST = "whitelist.bypass";
    public static final boolean DEFAULT_BYPASS = false;
    private boolean bypassWhitelist = DEFAULT_BYPASS;

    @Property(
            label="Whitelist regexp",
            description="Regular expression for bundle symbolic names for which loginAdministrative() is allowed. "
                + " NOT recommended for production use, but useful for testing with generated bundles.",
            value = "")
    public static final String PROP_WHITELIST_REGEXP = "whitelist.bundles.regexp";
    private Pattern whitelistRegexp;

    @Property(
            label="Default whitelisted BSNs",
            description="Default list of bundle symbolic names for which loginAdministrative() is allowed",
            value = {})
    public static final String PROP_DEFAULT_WHITELISTED_BSN = "whitelist.bundles.default";

    @Property(
            label="Additional whitelisted BSNs",
            description="Additional list of bundle symbolic names for which loginAdministrative() is allowed",
            value = {})
    public static final String PROP_ADDITIONAL_WHITELISTED_BSN = "whitelist.bundles.additional";

    private Set<String> whitelistedBsn;

    public void activate(Map<String, Object> config) {
        bypassWhitelist = PropertiesUtil.toBoolean(config.get(PROP_BYPASS_WHITELIST), DEFAULT_BYPASS);
        whitelistedBsn = new TreeSet<String>();

        final Object defBsns = config.get(PROP_DEFAULT_WHITELISTED_BSN);
        if(defBsns == null) {
            whitelistedBsn.addAll(Arrays.asList(DefaultWhitelist.WHITELISTED_BSN));
        } else {
            whitelistedBsn.addAll(Arrays.asList(PropertiesUtil.toStringArray(defBsns)));
        }

        final Object addBsns = config.get(PROP_ADDITIONAL_WHITELISTED_BSN);
        if(addBsns != null) {
            whitelistedBsn.addAll(Arrays.asList(PropertiesUtil.toStringArray(addBsns)));
        }

        final String regexp = PropertiesUtil.toString(config.get(PROP_WHITELIST_REGEXP), "");
        if(regexp.trim().length() > 0) {
            whitelistRegexp = Pattern.compile(regexp);
            log.warn("A {} is configured, this is NOT RECOMMENDED for production: {}", PROP_WHITELIST_REGEXP, whitelistRegexp);
        } else {
            whitelistRegexp = null;
        }

        if(bypassWhitelist) {
            log.info("bypassWhitelist={}, whitelisted BSNs=<ALL>", bypassWhitelist);
            log.warn(
                "All bundles are allowed to use loginAdministrative due to the 'bypass whitelist' configuration"
                + " of this service. This is NOT RECOMMENDED, for security reasons."
            );
        } else {
            log.info("bypassWhitelist={}, whitelisted BSNs({})={}",
                    new Object[] { bypassWhitelist, whitelistedBsn.size(), whitelistedBsn });
        }
    }

    @Override
    public boolean allowLoginAdministrative(Bundle b) {
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
