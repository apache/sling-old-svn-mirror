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

import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.hc.util.SimpleConstraintChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that checks a single JMX attribute */
@Component(
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE,
        metatype=true,
        label="Apache Sling JMX Attribute Health Check",
        description="Checks the value of a single JMX attribute.")
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
public class JmxAttributeHealthCheck implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private String mbeanName;
    private String attributeName;
    private String constraint;

    @Property(label="Check MBean Name",
              description="The name of the MBean to check by this health check.")
    public static final String PROP_OBJECT_NAME = "mbean.name";

    @Property(label="Check Attribute Name",
            description="The name of the MBean attribute to check by this health check.")
    public static final String PROP_ATTRIBUTE_NAME = "attribute.name";

    @Property(label="Check Attribute Constraint",
            description="Constraint on the MBean attribute value.")
    public static final String PROP_CONSTRAINT = "attribute.value.constraint";


    @Activate
    protected void activate(final Map<String, Object> properties) {
        mbeanName = PropertiesUtil.toString(properties.get(PROP_OBJECT_NAME), "");
        attributeName = PropertiesUtil.toString(properties.get(PROP_ATTRIBUTE_NAME), "");
        constraint = PropertiesUtil.toString(properties.get(PROP_CONSTRAINT), "");

        log.debug("Activated with HealthCheck name={}, objectName={}, attribute={}, constraint={}",
                new Object[] { properties.get(HealthCheck.NAME), mbeanName, attributeName, constraint });
    }

    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        resultLog.debug("Checking {} / {} with constraint {}", mbeanName, attributeName, constraint);
        try {
            final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
            final ObjectName objectName = new ObjectName(mbeanName);
            if(jmxServer.queryNames(objectName, null).size() == 0) {
                resultLog.warn("MBean not found: {}", objectName);
            } else {
                final Object value = jmxServer.getAttribute(objectName, attributeName);
                resultLog.debug("{} {} returns {}", mbeanName, attributeName, value);
                new SimpleConstraintChecker().check(value, constraint, resultLog);
            }
        } catch(final Exception e) {
            log.warn("JMX attribute {}/{} check failed: {}", new Object []{ mbeanName, attributeName, e});
            resultLog.healthCheckError("JMX attribute check failed: {}", e);
        }
        return new Result(resultLog);
    }
}
