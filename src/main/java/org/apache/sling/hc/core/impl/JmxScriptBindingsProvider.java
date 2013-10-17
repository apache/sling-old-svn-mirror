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

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.script.Bindings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The JmxBinding is meant to be bound as "jmx" global variables
 *  in scripted rules, to allow for writing scripted expressions
 *  like jmx.attribute("java.lang:type=ClassLoading", "LoadedClassCount") > 100
 */
@Component
@Service
@Property(name="context", value="healthcheck")
public class JmxScriptBindingsProvider implements BindingsValuesProvider {
    private MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
    private final Logger log = LoggerFactory.getLogger(getClass());
    public static final String JMX_BINDING_NAME = "jmx";

    public static class AttributeBinding {
        private final MBeanServer jmxServer;
        private final FormattingResultLog resultLog;

        AttributeBinding(MBeanServer s, FormattingResultLog r) {
            jmxServer = s;
            resultLog = r;
        }

        public Object attribute(String objectNameString, String attributeName)
                throws MalformedObjectNameException, AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException {
            final ObjectName name = new ObjectName(objectNameString);
            if(jmxServer.queryNames(name, null).size() == 0) {
                final String msg = "JMX object name not found: [" + objectNameString + "]";
                resultLog.warn(msg);
                throw new IllegalStateException(msg);
            }
            resultLog.debug("Got JMX Object [{}]", name);
            final Object value = jmxServer.getAttribute(name, attributeName);
            resultLog.debug("JMX Object [{}] Attribute [{}] = [{}]", name, attributeName, value);
            return value;
        }
    }

    @Override
    public void addBindings(Bindings b) {
        final String logBindingName = FormattingResultLog.class.getName();
        final Object resultLog = b.get(logBindingName);
        if (resultLog == null) {
            log.debug("No {} found in Bindings, cannot activate {} binding", logBindingName, JMX_BINDING_NAME);
            return;
        }
        try {
            b.put("jmx", new AttributeBinding(jmxServer, (FormattingResultLog)resultLog));
        } catch(Exception e) {
            log.error("Exception while activating " + JMX_BINDING_NAME, e);
        }
    }
}