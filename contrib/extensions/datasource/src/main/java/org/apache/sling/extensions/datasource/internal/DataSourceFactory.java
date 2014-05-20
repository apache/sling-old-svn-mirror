/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.extensions.datasource.internal;

import java.lang.management.ManagementFactory;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.sql.DataSource;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        name = DataSourceFactory.NAME,
        label = "%datasource.component.name",
        description = "%datasource.component.description",
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE
)
public class DataSourceFactory {
    public static final String NAME = "org.apache.sling.extensions.datasource.DataSourceFactory";

    @Property
    static final String PROP_DATASOURCE_NAME = "datasource.name";

    @Property
    static final String PROP_DRIVERCLASSNAME = "driverClassName";

    @Property
    static final String PROP_URL = "url";

    @Property
    static final String PROP_USERNAME = "username";

    @Property(passwordValue = "")
    static final String PROP_PASSWORD = "password";

    @Property(intValue = PoolProperties.DEFAULT_MAX_ACTIVE)
    static final String PROP_MAXACTIVE = "maxActive";

    @Property(value = {}, cardinality = 1024)
    static final String PROP_DATASOURCE_SVC_PROPS = "datasource.svc.properties";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private DriverRegistry driverRegistry;

    private String name;

    private ObjectName jmxName;

    private ServiceRegistration dsRegistration;

    private DataSource dataSource;

    @Activate
    protected void activate(BundleContext bundleContext, Map<String,?> config) throws Exception {
        Properties props = new Properties();
        name = PropertiesUtil.toString(config.get(PROP_DATASOURCE_NAME), null);

        checkArgument(name != null, "DataSource name must be specified via [%s] property", PROP_DATASOURCE_NAME);

        //Copy the other properties first
        Map<String,String> otherProps = PropertiesUtil.toMap(config.get(PROP_DATASOURCE_SVC_PROPS), new String[0]);
        for(Map.Entry<String, String> e : otherProps.entrySet()){
            props.setProperty(e.getKey(), e.getValue());
        }

        props.setProperty(org.apache.tomcat.jdbc.pool.DataSourceFactory.OBJECT_NAME, name);

        for(String propName : DummyDataSourceFactory.getPropertyNames()){
            String value = PropertiesUtil.toString(config.get(propName), null);
            if(value != null){
                props.setProperty(propName, value);
            }
        }

        dataSource = createDataSource(props, bundleContext);

        registerDataSource(bundleContext);
        registerJmx();

        log.info("Created DataSource [{}] with properties {}", name, getDataSourceDetails());
    }

    @Deactivate
    protected void deactivate(){
        if(dsRegistration != null){
            dsRegistration.unregister();
        }

        unregisterJmx();

        if(dataSource instanceof DataSourceProxy){
            ((DataSourceProxy) dataSource).close();
        }

    }

    private DataSource createDataSource(Properties props, BundleContext bundleContext) throws Exception {
        PoolConfiguration poolProperties = org.apache.tomcat.jdbc.pool.DataSourceFactory.parsePoolProperties(props);

        DriverDataSource driverDataSource = new DriverDataSource(poolProperties, driverRegistry, bundleContext);

        //Specify the DataSource such that connection creation logic is handled
        //by us where we take care of OSGi env
        poolProperties.setDataSource(driverDataSource);

        org.apache.tomcat.jdbc.pool.DataSource dataSource =
                new org.apache.tomcat.jdbc.pool.DataSource(poolProperties);
        //initialise the pool itself
        ConnectionPool pool = dataSource.createPool();
        driverDataSource.setJmxPool(pool.getJmxPool());

        // Return the configured DataSource instance
        return dataSource;
    }

    private void registerDataSource(BundleContext bundleContext) {
        Dictionary<String,Object> svcProps = new Hashtable<String, Object>();
        svcProps.put(PROP_DATASOURCE_NAME, name);
        svcProps.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        svcProps.put(Constants.SERVICE_DESCRIPTION, "DataSource service based on Tomcat JDBC");
        dsRegistration = bundleContext.registerService(DataSource.class, dataSource, svcProps);
    }

    private void registerJmx() throws MalformedObjectNameException {
        if(dataSource instanceof DataSourceProxy){
            org.apache.tomcat.jdbc.pool.jmx.ConnectionPool pool =
                    ((DataSourceProxy) dataSource).getPool().getJmxPool();

            if(pool == null){
                //jmx not enabled
                return;
            }
            Hashtable<String, String> table = new Hashtable<String, String>();
            table.put("type", "ConnectionPool");
            table.put("class", DataSource.class.getName());
            table.put("name", ObjectName.quote(name));
            jmxName = new ObjectName("org.apache.sling", table);

            try {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                mbs.registerMBean(pool, jmxName);
            }catch(Exception e){
                log.warn("Error occurred while registering the JMX Bean for " +
                        "connection pool with name {}",jmxName, e);
            }
        }
    }

    private void unregisterJmx(){
        try {
            if(jmxName != null) {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                mbs.unregisterMBean(jmxName);
            }
        } catch (InstanceNotFoundException ignore) {
            // NOOP
        } catch (Exception e) {
            log.error("Unable to unregister JDBC pool with JMX",e);
        }
    }

    private String getDataSourceDetails() {
        if(dataSource instanceof DataSourceProxy){
            return ((DataSourceProxy) dataSource).getPoolProperties().toString();
        }
        return "<UNKNOWN>";
    }



    public static void checkArgument(boolean expression,
                                     String errorMessageTemplate,
                                     Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(
                    String.format(errorMessageTemplate, errorMessageArgs));
        }
    }

    /**
     * Dummy impl to enable access to protected fields
     */
    private static class DummyDataSourceFactory extends org.apache.tomcat.jdbc.pool.DataSourceFactory {
        static String[] getPropertyNames(){
            return ALL_PROPERTIES;
        }
    }
}
