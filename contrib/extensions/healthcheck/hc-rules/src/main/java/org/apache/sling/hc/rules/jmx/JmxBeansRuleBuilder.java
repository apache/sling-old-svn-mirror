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
package org.apache.sling.hc.rules.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.api.SystemAttribute;
import org.slf4j.Logger;

/** Rules that give access to JMX beans */
public class JmxBeansRuleBuilder implements RuleBuilder {

    public static final String NAMESPACE = "jmxbeans";
    private MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
    
    private class JmxBeanAttribute implements SystemAttribute {
        private final String attributeName;
        private final String beanName;
        
        JmxBeanAttribute(String beanName, String attributeName) {
            // TODO we have a problem with : in bean names as it's used
            // as a separator in the healthcheck simple text rules format
            // For now, convert to a comma so that bean names can be
            // specified as java.lang#type=ClassLoading
            this.beanName = beanName.replaceAll("#", ":");
            this.attributeName = attributeName;
        }
        
        @Override
        public String toString() {
            return beanName + ":" + attributeName;
        }
        
        @Override
        public Object getValue(Logger logger) {
            try {
                final ObjectName objectName = new ObjectName(beanName);
                if(jmxServer.queryNames(objectName, null).size() == 0) {
                    logger.error("MBean not found: {}", objectName);
                }
                final Object value = jmxServer.getAttribute(objectName, attributeName);
                logger.debug("{} {} returns {}", new Object[] { beanName, attributeName, value });
                return value;
            } catch(Exception e) {
                logger.error("MBean exception", e);
            }
            return null;
        }
    }
    
    @Override
    public Rule buildRule(String namespace, String ruleName, String qualifier, String expression) {
        if(!NAMESPACE.equals(namespace)) {
            return null;
        }
        return new Rule(new JmxBeanAttribute(ruleName, qualifier), expression);
    }
}
