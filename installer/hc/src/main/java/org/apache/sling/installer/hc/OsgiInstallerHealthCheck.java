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
package org.apache.sling.installer.hc;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.annotations.SlingHealthCheck;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.info.InfoProvider;
import org.apache.sling.installer.api.info.InstallationState;
import org.apache.sling.installer.api.info.Resource;
import org.apache.sling.installer.api.info.ResourceGroup;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SlingHealthCheck(name = OsgiInstallerHealthCheck.HC_NAME, description = "Checks that all OSGi configurations/bundles are successfully installed by the OSGi Installer (and are not skipped for some reason).", tags = "osgi")
public class OsgiInstallerHealthCheck implements HealthCheck {
    protected static final String HC_NAME = "OSGi Installer Health Check";

    @Reference
    private InfoProvider infoProvider;

    private static final Logger LOG = LoggerFactory.getLogger(OsgiInstallerHealthCheck.class);

    private static final String DEFAULT_URL_PREFIX = "jcrinstall:/apps/";

    @Property(label = "URL Prefixes to consider", description = "Only those OSGi configurations/bundles whose location are starting with one of the given URL prefixes are checked (whether they are installed correctly). Open /system/console/osgi-installer for a list of valid prefixes. The bundles/configs with those prefixes are asserted to be successfully installed under all circumstances!", cardinality = 1, value = DEFAULT_URL_PREFIX)
    static final String PROP_URL_PREFIXES = "urlPrefixes";

    @Property(label = "Check Bundles", description = "If enabled bundles are checked (restricted to the ones matching one of the prefixes)", boolValue = true)
    static final String PROP_CHECK_BUNDLES = "checkBundles";

    @Property(label = "Check Configurations", description = "If enabled configurations are checked (restricted to the ones matching one of the prefixes)", boolValue = true)
    static final String PROP_CHECK_CONFIGURATIONS = "checkConfigurations";

    private String[] urlPrefixes;
    private boolean checkBundles;
    private boolean checkConfigurations;
    
    private final static String DOCUMENTATION_URL = "https://sling.apache.org/documentation/bundles/osgi-installer.html#health-check";

    @Reference
    private ConfigurationAdmin configurationAdmin;

    @Activate
    public void activate(Map<String, ?> properties) {
        urlPrefixes = PropertiesUtil.toStringArray(properties.get(PROP_URL_PREFIXES),
                new String[] { DEFAULT_URL_PREFIX });
        checkBundles = PropertiesUtil.toBoolean(properties.get(PROP_CHECK_BUNDLES), true);
        checkConfigurations = PropertiesUtil.toBoolean(properties.get(PROP_CHECK_CONFIGURATIONS), true);
    }

    @Override
    public Result execute() {
        InstallationState installationState = infoProvider.getInstallationState();
        FormattingResultLog hcLog = new FormattingResultLog();

        int numCheckedConfigurations = 0;
        int numCheckedBundles = 0;
        // go through all resource groups of the OSGi Installer
        for (final ResourceGroup group : installationState.getInstalledResources()) {
            String type = evaluateGroup(group, hcLog);
            switch (type) {
            case InstallableResource.TYPE_CONFIG:
                numCheckedConfigurations++;
                break;
            case InstallableResource.TYPE_BUNDLE:
                numCheckedBundles++;
                break;
            }
        }
        hcLog.info("Checked {} OSGi bundles and {} configurations.", numCheckedBundles, numCheckedConfigurations);
        if (hcLog.getAggregateStatus().ordinal() >= Result.Status.WARN.ordinal()) {
            hcLog.info("Refer to the OSGI installer's documentation page at {} for further details on how to fix those issues.", DOCUMENTATION_URL);
        }
        return new Result(hcLog);
    }

    /**
     * @param group
     *            the resource group to evaluate
     * @param hcLog
     *            the log to fill during the health check
     * @return the type of resources in this group ("bundle" or "config") or empty string, if the group was not
     *         considered by this health check
     */
    private String evaluateGroup(ResourceGroup group, FormattingResultLog hcLog) {
        Resource invalidResource = null;
        String resourceType = "";
        // go through all resources within the given group
        for (Resource resource : group.getResources()) {
            // check for the correct type
            resourceType = resource.getType();
            switch (resourceType) {
            case InstallableResource.TYPE_CONFIG:
                if (!checkConfigurations) {
                    LOG.debug("Skip resource '{}' as it is a configuration", resource.getEntityId());
                    return "";
                }
                break;
            case InstallableResource.TYPE_BUNDLE:
                if (!checkBundles) {
                    LOG.debug("Skip resource '{}' as it is a bundle", resource.getEntityId());
                    return "";
                }
                break;
            default:
                LOG.debug("Skip resource '{}' as it is neither a bundle nor a configuration but a {}",
                        resource.getEntityId(), resourceType);
                return "";
            }
            if (StringUtils.startsWithAny(resource.getURL(), urlPrefixes)) {
                switch (resource.getState()) {
                case IGNORED: // means a considered resource was found and it is invalid
                    // still the other resources need to be evaluated
                case INSTALL:
                    if (invalidResource == null) {
                        invalidResource = resource;
                    }
                    break;
                default:
                    // means a considered resource was found and it is valid
                    // no need to evaluate other resources from this group
                    return resourceType;
                }
            } else {
                LOG.debug("Skipping resource '{}' as its URL is not starting with any of these prefixes'{}'", resource,
                        StringUtils.join(urlPrefixes, ","));
            }
        }
        if (invalidResource != null) {
            if (resourceType.equals(InstallableResource.TYPE_CONFIG)) {
                hcLog.critical(
                        "The installer state of the OSGi configuration resource '{}' is {}, probably because a later version of that bundle is already installed!",
                        invalidResource, invalidResource.getState());
            } else {
                hcLog.critical(
                        "The installer state of the OSGi bundle resource '{}' is {}, config might have been manually overwritten!",
                        invalidResource, invalidResource.getState());
            }
        }
        return resourceType;
    }

}
