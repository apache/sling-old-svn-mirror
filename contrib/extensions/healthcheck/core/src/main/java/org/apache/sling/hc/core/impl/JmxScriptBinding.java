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

import org.apache.sling.hc.util.FormattingResultLog;

/** The JmxBinding is meant to be bound as "jmx" global variables
 *  in scripted rules, to allow for writing scripted expressions
 *  like jmx.attribute("java.lang:type=ClassLoading", "LoadedClassCount") > 100
 *  
 *  TODO this should really be a {@link BindingsValuesProvider} service,
 *  but for this we need to modify the default Sling script handling
 *  so that BindingsValuesProvider which have a specific scope service
 *  property are ignored.
 */
public class JmxScriptBinding {
    private MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
    private final FormattingResultLog resultLog;
    
    public JmxScriptBinding(FormattingResultLog resultLog) {
        this.resultLog = resultLog;
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