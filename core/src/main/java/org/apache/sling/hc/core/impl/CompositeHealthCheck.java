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
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.Result.Status;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.hc.util.HealthCheckFilter;
import org.osgi.framework.BundleContext;
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
    private BundleContext bundleContext;

    @Property(unbounded=PropertyUnbounded.ARRAY,
              label="Filter Tags",
              description="Tags used to select which health checks the composite health check executes.")
    private static final String PROP_FILTER_TAGS = "filter.tags";
    private String [] filterTags;

    private final ThreadLocal<Boolean> recursionLock = new ThreadLocal<Boolean>();

    @Activate
    protected void activate(final ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
        filterTags = PropertiesUtil.toStringArray(ctx.getProperties().get(PROP_FILTER_TAGS), new String[] {});
        log.debug("Activated, will select HealthCheck having tags {}", Arrays.asList(filterTags));
    }

    @Deactivate
    protected void deactivate() {
        this.bundleContext = null;
    }

    @Override
    public Result execute() {
        if ( recursionLock.get() != null ) {
            // recursion
            return new Result(Status.CRITICAL,
                  "Recursive invocation of composite health checks with filter tags : " + Arrays.asList(filterTags));
        }
        final FormattingResultLog resultLog = new FormattingResultLog();
        final HealthCheckFilter filter = new HealthCheckFilter(bundleContext);
        this.recursionLock.set(Boolean.TRUE);
        try {
            final List<HealthCheck> checks = filter.getTaggedHealthChecks(filterTags);
            if (checks.size() == 0) {
                resultLog.warn("HealthCheckFilter returns no HealthCheck for tags {}", Arrays.asList(filterTags));
                return new Result(resultLog);
            }

            int executed = 0;
            resultLog.debug("Executing {} HealthCheck selected by the {} tags", checks.size(), Arrays.asList(filterTags));
            int failures = 0;
            for (final HealthCheck hc : checks) {
                if(hc == this) {
                    resultLog.info("Cowardly forfeiting execution of this HealthCheck in an infinite loop, ignoring it");
                    continue;
                }
                resultLog.debug("Executing {}", hc);
                executed++;
                final Result sub = hc.execute();
                if(!sub.isOk()) {
                    failures++;
                }
                for(final ResultLog.Entry e : sub) {
                    resultLog.add(e);
                }
            }

            if (failures == 0) {
                resultLog.debug("{} HealthCheck executed, all ok", executed);
            } else {
                resultLog.warn("{} HealthCheck executed, {} failures", executed, failures);
            }
        } finally {
            filter.dispose();
            this.recursionLock.remove();
        }
        return new Result(resultLog);
    }
}
