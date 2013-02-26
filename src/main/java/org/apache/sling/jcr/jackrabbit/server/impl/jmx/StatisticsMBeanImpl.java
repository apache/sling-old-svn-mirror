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
package org.apache.sling.jcr.jackrabbit.server.impl.jmx;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.ReflectionException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.stats.RepositoryStatistics;
import org.apache.jackrabbit.api.stats.TimeSeries;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.stats.RepositoryStatisticsImpl;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractSlingRepository;
import org.apache.sling.jcr.jackrabbit.server.jmx.RepositoryStatisticsMBean;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MBean to expose Repository Statistics and make the repository statistics
 * available to other components read only.
 */
@Component(immediate = true)
@Service(value = { RepositoryStatisticsMBean.class, RepositoryStatistics.class })
@Properties(@Property(name = "jmx.objectname", value = "org.apache.sling.Resository:type=Statistics"))
public class StatisticsMBeanImpl implements DynamicMBean,
        RepositoryStatisticsMBean, RepositoryStatistics {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(StatisticsMBeanImpl.class);
    @Reference
    public SlingRepository repository;
    private RepositoryStatisticsImpl statistics;

    public StatisticsMBeanImpl() throws NotCompliantMBeanException {
    }

    @Activate
    public void activate(ComponentContext context) {
        try {
            Method getRepositoryMethod = AbstractSlingRepository.class
                    .getDeclaredMethod("getRepository", (Class<?>[]) null);
            if (!getRepositoryMethod.isAccessible()) {
                getRepositoryMethod.setAccessible(true);
            }
            RepositoryImpl repositoryImpl = (RepositoryImpl) getRepositoryMethod
                    .invoke(repository, (Object[]) null);
            Field contextField = RepositoryImpl.class
                    .getDeclaredField("context");
            if (!contextField.isAccessible()) {
                contextField.setAccessible(true);
            }
            RepositoryContext repositoryContext = (RepositoryContext) contextField
                    .get(repositoryImpl);
            statistics = repositoryContext.getRepositoryStatistics();

        } catch (Exception e) {
            LOGGER.error("Unable to retrive repository statistics ", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.management.DynamicMBean#getAttribute(java.lang.String)
     */
    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        if ( statistics == null ) {
            throw new AttributeNotFoundException("No Statistics Available");
        }
        try {
            if (attribute.startsWith("PerSecond_")) {
                return getTimeSeries(attribute.substring("PerSecond_".length()))
                        .getValuePerSecond();
            } else if (attribute.startsWith("PerMinute_")) {
                return getTimeSeries(attribute.substring("PerMinute_".length()))
                        .getValuePerMinute();
            } else if (attribute.startsWith("PerHour_")) {
                return getTimeSeries(attribute.substring("PerHour_".length()))
                        .getValuePerHour();
            } else if (attribute.startsWith("PerWeek_")) {
                return getTimeSeries(attribute.substring("PerWeek_".length()))
                        .getValuePerWeek();
            } else if (attribute.startsWith("LastMinutePerSecond_")) {
                return getCurrentValue(getTimeSeries(
                        attribute.substring("LastMinutePerSecond_".length()))
                        .getValuePerSecond());
            } else if (attribute.startsWith("LastHourPerMinute_")) {
                return getCurrentValue(getTimeSeries(
                        attribute.substring("LastHourPerMinute_".length()))
                        .getValuePerMinute());
            } else if (attribute.startsWith("LastWeekPerHour_")) {
                return getCurrentValue(getTimeSeries(
                        attribute.substring("LastWeekPerHour_".length()))
                        .getValuePerHour());
            } else if (attribute.startsWith("LastYearPerWeek_")) {
                return getCurrentValue(getTimeSeries(
                        attribute.substring("LastYearPerWeek_".length()))
                        .getValuePerWeek());
            }
        } catch (AttributeNotFoundException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return getCounter(attribute);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
     */
    public AttributeList getAttributes(String[] attributes) {
        if ( statistics == null ) {
            return new AttributeList();
        }
        AttributeList al = new AttributeList();
        Iterator<Entry<Type, TimeSeries>> statIter = statistics.iterator();
        while (statIter.hasNext()) {
            Entry<Type, TimeSeries> entry = statIter.next();
            long[] valuePerSecond = entry.getValue().getValuePerSecond();
            al.add(new Attribute("PerSecond_" + entry.getKey().name(),
                    valuePerSecond[valuePerSecond.length - 1]));
            al.add(new Attribute(
                    "LastMinutePerSecond_" + entry.getKey().name(),
                    valuePerSecond));
            long[] valuePerMinute = entry.getValue().getValuePerMinute();
            al.add(new Attribute("PerMinute" + entry.getKey().name(),
                    valuePerMinute[valuePerMinute.length - 1]));
            al.add(new Attribute("LastHourPerMinute_" + entry.getKey().name(),
                    valuePerMinute));
            long[] valuePerHour = entry.getValue().getValuePerHour();
            al.add(new Attribute("PerHour_" + entry.getKey().name(),
                    valuePerHour[valuePerHour.length - 1]));
            al.add(new Attribute("LastWeekPerHour_" + entry.getKey().name(),
                    valuePerHour));
            long[] valuePerWeek = entry.getValue().getValuePerWeek();
            al.add(new Attribute("PerWeek_" + entry.getKey().name(),
                    valuePerWeek[valuePerWeek.length - 1]));
            al.add(new Attribute("LastYearPerWeek_" + entry.getKey().name(),
                    valuePerWeek));
        }
        return al;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.management.DynamicMBean#getMBeanInfo()
     */
    public MBeanInfo getMBeanInfo() {
        if ( statistics == null ) {
            return new MBeanInfo(this.getClass().getName(),
                "Repository Statistics Unavailable", null, null, null, null);
        }
        List<MBeanAttributeInfo> attributesList = new ArrayList<MBeanAttributeInfo>();
        Set<Type> types = new HashSet<Type>();
        Iterator<Entry<Type, TimeSeries>> statIter = statistics.iterator();
        while (statIter.hasNext()) {
            Entry<Type, TimeSeries> entry = statIter.next();
            attributesList.add(new MBeanAttributeInfo("PerSecond_"
                    + entry.getKey().name(), long.class.getName(),
                    "Current per second value of " + entry.getKey().name(),
                    true, false, false));
            attributesList.add(new MBeanAttributeInfo("LastMinutePerSecond_"
                    + entry.getKey().name(), long[].class.getName(),
                    "Last minute of per seconds values of "
                            + entry.getKey().name(), true, false, false));
            attributesList.add(new MBeanAttributeInfo("PerMinute"
                    + entry.getKey().name(), long.class.getName(),
                    "Current per minute value of " + entry.getKey().name(),
                    true, false, false));
            attributesList.add(new MBeanAttributeInfo("LastHourPerMinute_"
                    + entry.getKey().name(), long[].class.getName(),
                    "Last hour of per minute values of "
                            + entry.getKey().name(), true, false, false));
            attributesList.add(new MBeanAttributeInfo("PerHour_"
                    + entry.getKey().name(), long.class.getName(),
                    "Current per hour value of " + entry.getKey().name(), true,
                    false, false));
            attributesList.add(new MBeanAttributeInfo("LastWeekPerHour_"
                    + entry.getKey().name(), long[].class.getName(),
                    "Last week of per hour values of " + entry.getKey().name(),
                    true, false, false));
            attributesList.add(new MBeanAttributeInfo("PerWeek_"
                    + entry.getKey().name(), long.class.getName(),
                    "Current per week value of " + entry.getKey().name(), true,
                    false, false));
            attributesList.add(new MBeanAttributeInfo("LastYearPerWeek_"
                    + entry.getKey().name(), long[].class.getName(),
                    "Last year of per week values of " + entry.getKey().name(),
                    true, false, false));
            types.add(entry.getKey());
        }
        for (Type t : Type.values()) {
            if (!types.contains(t)) {
                attributesList.add(new MBeanAttributeInfo(t.name(), long.class
                        .getName(), "Current counter value of " + t.name(),
                        true, false, false));
                types.add(t);
            }
        }
        MBeanAttributeInfo[] attributes = attributesList
                .toArray(new MBeanAttributeInfo[attributesList.size()]);
        return new MBeanInfo(this.getClass().getName(),
                "Repository Statistics", attributes, null, null, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.management.DynamicMBean#invoke(java.lang.String,
     * java.lang.Object[], java.lang.String[])
     */
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        return new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
     */
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        throw new UnsupportedOperationException("read only");

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.management.DynamicMBean#setAttributes(javax.management.AttributeList
     * )
     */
    public AttributeList setAttributes(AttributeList attributes) {
        throw new UnsupportedOperationException("read only");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.jackrabbit.api.stats.RepositoryStatistics#getTimeSeries(org
     * .apache.jackrabbit.api.stats.RepositoryStatistics.Type)
     */
    public TimeSeries getTimeSeries(Type type) {
        if ( statistics == null ) {
            throw new IllegalStateException("Repository statistics are not available");
        }
        return statistics.getTimeSeries(type);
    }

    /* Private Utility methods */
    /**
     * @param name
     *            name of the attribute.
     * @return the value of the counter
     * @throws AttributeNotFoundException
     *             if the attribute doesnt exist.
     */
    private long getCounter(String name) throws AttributeNotFoundException {
        try {
            AtomicLong ts = statistics.getCounter(Type.valueOf(name));
            if (ts == null) {
                throw new AttributeNotFoundException("Attribute " + name
                        + " doesnt exist");
            }
            return ts.get();
        } catch (Exception e) {
            throw new AttributeNotFoundException("Attribute " + name
                    + " doesnt exist");
        }
    }

    /**
     * @param values
     *            array of values
     * @return current value ( the last in the array)
     */
    private long getCurrentValue(long[] values) {
        return values[values.length - 1];
    }

    /**
     * @param name
     * @return a time series based on name
     * @throws AttributeNotFoundException
     *             if the time series isnt available.
     */
    private TimeSeries getTimeSeries(String name)
            throws AttributeNotFoundException {
        try {
            TimeSeries ts = statistics.getTimeSeries(Type.valueOf(name));
            if (ts == null) {
                throw new AttributeNotFoundException("Attribute " + name
                        + " doesnt exist");
            }
            return ts;
        } catch (Exception e) {
            throw new AttributeNotFoundException("Attribute " + name
                    + " doesnt exist");
        }
    }

}
