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
package org.apache.sling.hc.jmx.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/** A {@link DynamicMBean} used to execute a {@link HealthCheck} service */
public class HealthCheckMBean implements DynamicMBean {

    /** A HC result is cached for this time (ms) */
    private static final long RESULT_TTL = 1500;

    public static final String HC_OK_ATTRIBUTE_NAME = "ok";
    public static final String HC_STATUS_ATTRIBUTE_NAME = "status";
    public static final String HC_LOG_ATTRIBUTE_NAME = "log";

    private static CompositeType LOG_ROW_TYPE;
    private static TabularType LOG_TABLE_TYPE;

    public static final String INDEX_COLUMN = "index";
    public static final String LEVEL_COLUMN = "level";
    public static final String MESSAGE_COLUMN = "message";

    public static final String JMX_TYPE_NAME = "HealthCheck";
    public static final String JMX_DOMAIN = "org.apache.sling.healthcheck";

    /** The health check service to call. */
    private final HealthCheck healthCheck;

    /** The mbean info. */
    private final MBeanInfo mbeanInfo;

    /** The default attributes. */
    private final Map<String, Object> defaultAttributes;

    private long healthCheckInvocationTime;

    private Result healthCheckResult;

    static {
        try {
            // Define the log row and table types
            LOG_ROW_TYPE = new CompositeType(
                    "LogLine",
                    "A line in the result log",
                    new String [] { INDEX_COLUMN, LEVEL_COLUMN, MESSAGE_COLUMN },
                    new String [] { "log line index", "log level", "log message"},
                    new OpenType[] { SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING }
                    );
            final String [] indexes = { INDEX_COLUMN };
            LOG_TABLE_TYPE = new TabularType("LogTable", "Result log messages", LOG_ROW_TYPE, indexes);
        } catch(Exception ignore) {
            // row or table type will be null if this happens
        }
    }

    public HealthCheckMBean(final ServiceReference ref, final HealthCheck hc) {
        this.healthCheck = hc;
        this.mbeanInfo = this.createMBeanInfo(ref);
        this.defaultAttributes = this.createDefaultAttributes(ref);
    }

    @Override
    public Object getAttribute(final String attribute)
    throws AttributeNotFoundException, MBeanException, ReflectionException {
        // we should call getAttributes - and not vice versa to have the result
        // of a single check call - and not do a check call for each attribute
        final AttributeList result = this.getAttributes(new String[] {attribute});
        if ( result.size() == 0 ) {
            throw new AttributeNotFoundException(attribute);
        }
        final Attribute attr = (Attribute) result.get(0);
        return attr.getValue();
    }

    private TabularData logData(final Result er) throws OpenDataException {
        final TabularDataSupport result = new TabularDataSupport(LOG_TABLE_TYPE);
        int i = 1;
        for(final ResultLog.Entry e : er) {
            final Map<String, Object> data = new HashMap<String, Object>();
            data.put(INDEX_COLUMN, i++);
            data.put(LEVEL_COLUMN, e.getStatus().toString());
            data.put(MESSAGE_COLUMN, e.getMessage());

            result.put(new CompositeDataSupport(LOG_ROW_TYPE, data));
        }
        return result;
    }

    @Override
    public AttributeList getAttributes(final String[] attributes) {
        final AttributeList result = new AttributeList();
        if ( attributes != null ) {
            Result hcResult = null;
            for(final String key : attributes) {
                final Object defaultValue = this.defaultAttributes.get(key);
                if ( defaultValue != null ) {
                    result.add(new Attribute(key, defaultValue));
                } else {
                    // we assume that a valid attribute name is used
                    // which is requesting a hc result
                    if ( hcResult == null ) {
                        hcResult = this.getHealthCheckResult();
                    }

                    if ( HC_OK_ATTRIBUTE_NAME.equals(key) ) {
                        result.add(new Attribute(key, hcResult.isOk()));
                    } else if ( HC_LOG_ATTRIBUTE_NAME.equals(key) ) {
                        try {
                            result.add(new Attribute(key, logData(hcResult)));
                        } catch ( final OpenDataException ignore ) {
                            // we ignore this and simply don't add the attribute
                        }
                    } else if ( HC_STATUS_ATTRIBUTE_NAME.equals(key) ) {
                        result.add(new Attribute(key, hcResult.getStatus().toString()));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Create the mbean info
     */
    private MBeanInfo createMBeanInfo(final ServiceReference serviceReference) {
        final List<MBeanAttributeInfo> attrs = new ArrayList<MBeanAttributeInfo>();

        // add relevant service properties
        if ( serviceReference.getProperty(HealthCheck.NAME) != null ) {
            attrs.add(new MBeanAttributeInfo(HealthCheck.NAME, String.class.getName(), "The name of the health check service.", true, false, false));
        }
        if ( serviceReference.getProperty(HealthCheck.TAGS) != null ) {
            attrs.add(new MBeanAttributeInfo(HealthCheck.TAGS, String.class.getName(), "The tags of the health check service.", true, false, false));
        }
        if ( serviceReference.getProperty(Constants.SERVICE_PID) != null ) {
            attrs.add(new MBeanAttributeInfo(Constants.SERVICE_PID, String.class.getName(), "The persistence identifier of the service.", true, false, false));
        }

        // add standard attributes
        attrs.add(new MBeanAttributeInfo(HC_OK_ATTRIBUTE_NAME, Boolean.class.getName(), "The health check result", true, false, false));
        attrs.add(new MBeanAttributeInfo(HC_STATUS_ATTRIBUTE_NAME, String.class.getName(), "The health check status", true, false, false));

        attrs.add(new OpenMBeanAttributeInfoSupport(HC_LOG_ATTRIBUTE_NAME, "The health check result log", LOG_TABLE_TYPE, true, false, false));

        final String description;
        if ( serviceReference.getProperty(Constants.SERVICE_DESCRIPTION) != null ) {
            description = serviceReference.getProperty(Constants.SERVICE_DESCRIPTION).toString();
        } else {
            description = "Health check";
        }
        return new MBeanInfo(this.getClass().getName(),
                   description,
                   attrs.toArray(new MBeanAttributeInfo[attrs.size()]), null, null, null);
    }

    /**
     * Create the default attributes.
     */
    private Map<String, Object> createDefaultAttributes(final ServiceReference serviceReference) {
        final Map<String, Object> list = new HashMap<String, Object>();
        if ( serviceReference.getProperty(HealthCheck.NAME) != null ) {
            list.put(HealthCheck.NAME, serviceReference.getProperty(HealthCheck.NAME).toString());
        }
        if ( serviceReference.getProperty(HealthCheck.TAGS) != null ) {
            final Object value = serviceReference.getProperty(HealthCheck.TAGS);
            if ( value instanceof String[] ) {
                list.put(HealthCheck.TAGS, Arrays.toString((String[])value));
            } else {
                list.put(HealthCheck.TAGS, value.toString());
            }
        }
        if ( serviceReference.getProperty(Constants.SERVICE_PID) != null ) {
            list.put(Constants.SERVICE_PID, serviceReference.getProperty(Constants.SERVICE_PID).toString());
        }
        return list;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return this.mbeanInfo;
    }

    @Override
    public Object invoke(final String actionName, final Object[] params, final String[] signature)
            throws MBeanException, ReflectionException {
        throw new MBeanException(new UnsupportedOperationException(getClass().getSimpleName() + " does not support operations."));
    }

    @Override
    public void setAttribute(final Attribute attribute)
    throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        throw new MBeanException(new UnsupportedOperationException(getClass().getSimpleName() + " does not support setting attributes."));
    }

    @Override
    public AttributeList setAttributes(final AttributeList attributes) {
        return new AttributeList();
    }

    @Override
    public String toString() {
        return "HealthCheckMBean [healthCheck=" + healthCheck + "]";
    }

    private Result getHealthCheckResult() {
        synchronized ( this ) {
            if ( this.healthCheckResult == null || this.healthCheckInvocationTime < System.currentTimeMillis() ) {
                this.healthCheckResult = this.healthCheck.execute();
                this.healthCheckInvocationTime = System.currentTimeMillis() + RESULT_TTL;
            }
            return this.healthCheckResult;
        }
    }
}