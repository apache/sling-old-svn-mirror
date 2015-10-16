/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.osgi.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.ide.osgi.OsgiClient;
import org.apache.sling.ide.osgi.OsgiClientException;
import org.apache.sling.ide.transport.CommandExecutionProperties;
import org.osgi.framework.Version;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * The <tt>TracingOsgiClient</tt> decorates another OsgiClient instance by adding tracing operations
 * 
 */
public class TracingOsgiClient implements OsgiClient {

    private final OsgiClient osgiClient;
    private final EventAdmin eventAdmin;
    
    public TracingOsgiClient(OsgiClient osgiClient, EventAdmin eventAdmin) {
        this.osgiClient = osgiClient;
        this.eventAdmin = eventAdmin;
    }

    @Override
    public Version getBundleVersion(String bundleSymbolicName) throws OsgiClientException {
        return osgiClient.getBundleVersion(bundleSymbolicName);
    }

    @Override
    public void installBundle(InputStream in, String fileName) throws OsgiClientException {
        osgiClient.installBundle(in, fileName);
    }

    @Override
    public void installLocalBundle(String explodedBundleLocation) throws OsgiClientException {

        logInstallLocalBundle(null, explodedBundleLocation);
    }

    private void logInstallLocalBundle(InputStream input, String explodedBundleLocation) throws OsgiClientException {

        Map<String, Object> props = new HashMap<>();
        long start = System.currentTimeMillis();
        if (input != null) {
            props.put(CommandExecutionProperties.ACTION_TYPE, "InstallJarredBundle");
        } else {
            props.put(CommandExecutionProperties.ACTION_TYPE, "InstallLocalBundle");
        }
        props.put(CommandExecutionProperties.ACTION_TARGET, explodedBundleLocation);
        props.put(CommandExecutionProperties.TIMESTAMP_START, start);
        try {
            if (input != null) {
                osgiClient.installLocalBundle(input, explodedBundleLocation);
            } else {
                osgiClient.installLocalBundle(explodedBundleLocation);
            }
            props.put(CommandExecutionProperties.RESULT_TEXT, "OK");
            props.put(CommandExecutionProperties.RESULT_STATUS, Boolean.TRUE);
        } catch (Throwable t) {
            props.put(CommandExecutionProperties.RESULT_TEXT, "FAILED");
            props.put(CommandExecutionProperties.RESULT_STATUS, Boolean.FALSE);
            props.put(CommandExecutionProperties.RESULT_THROWABLE, t);
            if (t instanceof OsgiClientException) {
                throw (OsgiClientException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                // should never happen
                throw new Error(t);
            }
        } finally {
            props.put(CommandExecutionProperties.TIMESTAMP_END, System.currentTimeMillis());
            Event event = new Event(CommandExecutionProperties.REPOSITORY_TOPIC, props);
            eventAdmin.postEvent(event);
        }
    }

    @Override
    public void installLocalBundle(InputStream jarredBundle, String sourceLocation) throws OsgiClientException {

        logInstallLocalBundle(jarredBundle, sourceLocation);
    }

}
