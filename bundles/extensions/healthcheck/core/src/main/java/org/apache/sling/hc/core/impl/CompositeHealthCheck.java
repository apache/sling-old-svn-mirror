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
package org.apache.sling.hc.core.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.Result.Status;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.hc.util.HealthCheckFilter;
import org.apache.sling.hc.util.HealthCheckMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that executes a number of other HealthChecks,
 *  selected by their tags, and merges their Results.
 */

@Component(
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE,
        metatype=true,
        label="Apache Sling Composite Health Check",
        description="Executes a set of health checks, selected by tags.")
@Properties({
    @Property(name=HealthCheck.NAME,
              label="Name",
              description="Name of this health check."),
    @Property(name=HealthCheck.TAGS, unbounded=PropertyUnbounded.ARRAY,
              label="Tags",
              description="List of tags for this health check, used to select " +
                          "subsets of health checks for execution e.g. by a composite health check."),
    @Property(name=HealthCheck.MBEAN_NAME,
              label="MBean Name",
              description="Name of the MBean to create for this health check. If empty, no MBean is registered.")
})
@Service(value=HealthCheck.class)
public class CompositeHealthCheck implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(unbounded=PropertyUnbounded.ARRAY,
              label="Filter Tags",
              description="Tags used to select which health checks the composite health check executes.")
    static final String PROP_FILTER_TAGS = "filter.tags";
    private String [] filterTags;


    @Reference
    private HealthCheckExecutor healthCheckExecutor;

    private BundleContext bundleContext;
    private ServiceReference referenceToThis;
    private HealthCheckFilter healthCheckFilter;
    
    @Activate
    protected void activate(final ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
        healthCheckFilter = new HealthCheckFilter(bundleContext);
        referenceToThis = getReferenceByPid(PropertiesUtil.toString(ctx.getProperties().get(Constants.SERVICE_PID), "-1"));

        filterTags = PropertiesUtil.toStringArray(ctx.getProperties().get(PROP_FILTER_TAGS), new String[] {});
        log.debug("Activated, will select HealthCheck having tags {}", Arrays.asList(filterTags));
    }

    @Deactivate
    protected void deactivate() {
        bundleContext = null;
        healthCheckFilter = null;
        referenceToThis = null;
    }

    @Override
    public Result execute() {

        Result result = checkForRecursion(referenceToThis, new HashSet<String>());
        if(result != null) {
            // return recursion error
            return result;
        }

        FormattingResultLog resultLog = new FormattingResultLog();
        List<HealthCheckExecutionResult> executionResults = healthCheckExecutor.execute(filterTags);
        resultLog.debug("Executing {} HealthChecks selected by tags {}", executionResults.size(), Arrays.asList(filterTags));
        result = new CompositeResult(resultLog, executionResults);

        return result;
    }


    Result checkForRecursion(ServiceReference hcReference, Set<String> alreadyBannedTags) {

        HealthCheckMetadata thisCheckMetadata = new HealthCheckMetadata(hcReference);

        Set<String> bannedTagsForThisCompositeCheck = new HashSet<String>();
        bannedTagsForThisCompositeCheck.addAll(alreadyBannedTags);
        bannedTagsForThisCompositeCheck.addAll(thisCheckMetadata.getTags());

        String[] tagsForIncludedChecksArr = PropertiesUtil.toStringArray(hcReference.getProperty(PROP_FILTER_TAGS), new String[0]);
        Set<String> tagsForIncludedChecks = new HashSet<String>(Arrays.asList(tagsForIncludedChecksArr));
        
        
        log.debug("HC {} has banned tags {}", thisCheckMetadata.getName(), bannedTagsForThisCompositeCheck);
        log.debug("tagsForIncludedChecks {}", tagsForIncludedChecks);

        // is this HC ok?
        Set<String> intersection = new HashSet<String>();
        intersection.addAll(bannedTagsForThisCompositeCheck);
        intersection.retainAll(tagsForIncludedChecks);
        
        if (!intersection.isEmpty()) {
            return new Result(Status.HEALTH_CHECK_ERROR,
                    "INVALID CONFIGURATION: Cycle detected in composite health check hierarchy. Health check '" + thisCheckMetadata.getName()
                            + "' (" + hcReference.getProperty(Constants.SERVICE_PID) + ") must not have tag(s) " + intersection
                            + " as a composite check in the hierarchy is itself already tagged alike (tags assigned to composite checks: "
                            + bannedTagsForThisCompositeCheck + ")");
        }
        
        // check each sub composite check
        ServiceReference[] hcRefsOfCompositeCheck = healthCheckFilter.getTaggedHealthCheckServiceReferences(tagsForIncludedChecksArr);
        for (ServiceReference hcRefOfCompositeCheck : hcRefsOfCompositeCheck) {
            if (CompositeHealthCheck.class.getName().equals(hcRefOfCompositeCheck.getProperty(ComponentConstants.COMPONENT_NAME))) {
                log.debug("Checking sub composite HC {}, {}", hcRefOfCompositeCheck, hcRefOfCompositeCheck.getProperty(ComponentConstants.COMPONENT_NAME));
                Result result = checkForRecursion(hcRefOfCompositeCheck, bannedTagsForThisCompositeCheck);
                if (result != null) {
                    // found recursion
                    return result;
                }
            }

        }

        // no recursion detected
        return null;

    }

    private ServiceReference getReferenceByPid(String servicePid) {

        if (servicePid == null) {
            return null;
        }

        String filterString = "(" + Constants.SERVICE_PID + "=" + servicePid + ")";
        ServiceReference[] refs = null;
        try {
            refs = bundleContext.getServiceReferences(HealthCheck.class.getName(), filterString);
        } catch (InvalidSyntaxException e) {
            log.error("Invalid filter " + filterString, e);
        }
        if (refs == null || refs.length == 0) {
            return null;
        } else if (refs.length == 1) {
            return refs[0];
        } else {
            throw new IllegalStateException("OSGi Framework returned more than one service reference for unique service pid =" + servicePid);
        }

    }

    void setHealthCheckFilter(HealthCheckFilter healthCheckFilter) {
        this.healthCheckFilter = healthCheckFilter;
    }

    void setFilterTags(String[] filterTags) {
        this.filterTags = filterTags;
    }

    void setHealthCheckExecutor(HealthCheckExecutor healthCheckExecutor) {
        this.healthCheckExecutor = healthCheckExecutor;
    }

    void setReferenceToThis(ServiceReference referenceToThis) {
        this.referenceToThis = referenceToThis;
    }

}
