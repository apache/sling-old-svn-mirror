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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;
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

    @Property(value = PROP_DATASOURCE_NAME)
    static final String PROP_DS_SVC_PROP_NAME = "datasource.svc.prop.name";

    @Property
    static final String PROP_DRIVERCLASSNAME = "driverClassName";

    @Property
    static final String PROP_URL = "url";

    @Property
    static final String PROP_USERNAME = "username";

    @Property(passwordValue = "")
    static final String PROP_PASSWORD = "password";

    /**
     * Value indicating default value should be used. if the value is set to
     * this value then that value would be treated as null
     */
    static final String DEFAULT_VAL = "default";

    @Property(value = DEFAULT_VAL, options = {
            @PropertyOption(name = "Default", value = DEFAULT_VAL),
            @PropertyOption(name = "true", value = "true"),
            @PropertyOption(name = "false", value = "false")})
    static final String PROP_DEFAULTAUTOCOMMIT = "defaultAutoCommit";

    @Property(value = DEFAULT_VAL, options = {
            @PropertyOption(name = "Default", value = DEFAULT_VAL),
            @PropertyOption(name = "true", value = "true"),
            @PropertyOption(name = "false", value = "false")})
    static final String PROP_DEFAULTREADONLY = "defaultReadOnly";

    @Property(value = DEFAULT_VAL, options = {
            @PropertyOption(name = "Default", value = DEFAULT_VAL),
            @PropertyOption(name = "NONE", value = "NONE"),
            @PropertyOption(name = "READ_COMMITTED", value = "READ_COMMITTED"),
            @PropertyOption(name = "READ_UNCOMMITTED", value = "READ_UNCOMMITTED"),
            @PropertyOption(name = "REPEATABLE_READ", value = "REPEATABLE_READ"),
            @PropertyOption(name = "SERIALIZABLE", value = "SERIALIZABLE")})
    static final String PROP_DEFAULTTRANSACTIONISOLATION = "defaultTransactionIsolation";

    @Property
    static final String PROP_DEFAULTCATALOG = "defaultCatalog";

    @Property(intValue = PoolProperties.DEFAULT_MAX_ACTIVE)
    static final String PROP_MAXACTIVE = "maxActive";

    @Property(intValue = PoolProperties.DEFAULT_MAX_ACTIVE)
    static final String PROP_MAXIDLE = "maxIdle"; //defaults to maxActive

    @Property(intValue = 10)
    static final String PROP_MINIDLE = "minIdle"; //defaults to initialSize

    @Property(intValue = 10)
    static final String PROP_INITIALSIZE = "initialSize";

    @Property(intValue = 30000)
    static final String PROP_MAXWAIT = "maxWait";

    @Property(intValue = 0)
    static final String PROP_MAXAGE = "maxAge";

    @Property(boolValue = false)
    static final String PROP_TESTONBORROW = "testOnBorrow";

    @Property(boolValue = false)
    static final String PROP_TESTONRETURN = "testOnReturn";

    @Property(boolValue = false)
    static final String PROP_TESTWHILEIDLE = "testWhileIdle";

    @Property
    static final String PROP_VALIDATIONQUERY = "validationQuery";

    @Property(intValue = -1)
    static final String PROP_VALIDATIONQUERY_TIMEOUT = "validationQueryTimeout";

    @Property(intValue = 5000)
    protected static final String PROP_TIMEBETWEENEVICTIONRUNSMILLIS = "timeBetweenEvictionRunsMillis";

    @Property(intValue = 60000)
    protected static final String PROP_MINEVICTABLEIDLETIMEMILLIS = "minEvictableIdleTimeMillis";

    @Property
    protected static final String PROP_CONNECTIONPROPERTIES = "connectionProperties";

    @Property
    protected static final String PROP_INITSQL = "initSQL";

    @Property(value = "StatementCache;SlowQueryReport(threshold=10000);ConnectionState")
    protected static final String PROP_INTERCEPTORS = "jdbcInterceptors";

    @Property(intValue = 30000)
    protected static final String PROP_VALIDATIONINTERVAL = "validationInterval";

    @Property(boolValue = true)
    protected static final String PROP_LOGVALIDATIONERRORS = "logValidationErrors";

    @Property(value = {}, cardinality = 1024)
    static final String PROP_DATASOURCE_SVC_PROPS = "datasource.svc.properties";

    /**
     * Property names where we need to treat value 'default' as null
     */
    private static final Set<String> PROPS_WITH_DFEAULT =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    PROP_DEFAULTAUTOCOMMIT,
                    PROP_DEFAULTREADONLY,
                    PROP_DEFAULTTRANSACTIONISOLATION
            )));

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private DriverRegistry driverRegistry;

    private String name;

    private String svcPropName;

    private ObjectName jmxName;

    private ServiceRegistration dsRegistration;

    private DataSource dataSource;

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext, Map<String, ?> config) throws Exception {
        this.bundleContext = bundleContext;
        name = getDataSourceName(config);

        checkArgument(name != null, "DataSource name must be specified via [%s] property", PROP_DATASOURCE_NAME);
        dataSource = new LazyJmxRegisteringDataSource(createPoolConfig(config));

        svcPropName = getSvcPropName(config);
        registerDataSource(svcPropName);

        log.info("Created DataSource [{}] with properties {}", name, dataSource.getPoolProperties().toString());
    }

    @Modified
    protected void modified(Map<String, ?> config) throws Exception {
        String name = getDataSourceName(config);
        String svcPropName = getSvcPropName(config);

        if (!this.name.equals(name) || !this.svcPropName.equals(svcPropName)) {
            log.info("Change in datasource name/service property name detected. DataSource would be recreated");
            deactivate();
            activate(bundleContext, config);
            return;
        }

        //Other modifications can be applied at runtime itself
        //Tomcat Connection Pool is decoupled from DataSource so can be closed and reset
        dataSource.setPoolProperties(createPoolConfig(config));
        closeConnectionPool();
        dataSource.createPool();
        log.info("Updated DataSource [{}] with properties {}", name, dataSource.getPoolProperties().toString());
    }

    @Deactivate
    protected void deactivate() {
        if (dsRegistration != null) {
            dsRegistration.unregister();
            dsRegistration = null;
        }

        closeConnectionPool();
        dataSource = null;
    }

    private void closeConnectionPool() {
        unregisterJmx();
        dataSource.close();
    }

    private PoolConfiguration createPoolConfig(Map<String, ?> config) {
        Properties props = new Properties();

        //Copy the other properties first
        Map<String, String> otherProps = PropertiesUtil.toMap(config.get(PROP_DATASOURCE_SVC_PROPS), new String[0]);
        for (Map.Entry<String, String> e : otherProps.entrySet()) {
            set(e.getKey(), e.getValue(), props);
        }

        props.setProperty(org.apache.tomcat.jdbc.pool.DataSourceFactory.OBJECT_NAME, name);

        for (String propName : DummyDataSourceFactory.getPropertyNames()) {
            String value = PropertiesUtil.toString(config.get(propName), null);
            set(propName, value, props);
        }

        //Specify the DataSource such that connection creation logic is handled
        //by us where we take care of OSGi env
        PoolConfiguration poolProperties = org.apache.tomcat.jdbc.pool.DataSourceFactory.parsePoolProperties(props);
        poolProperties.setDataSource(createDriverDataSource(poolProperties));

        return poolProperties;
    }

    private DriverDataSource createDriverDataSource(PoolConfiguration poolProperties) {
        return new DriverDataSource(poolProperties, driverRegistry, bundleContext, this);
    }

    private void registerDataSource(String svcPropName) {
        Dictionary<String, Object> svcProps = new Hashtable<String, Object>();
        svcProps.put(svcPropName, name);
        svcProps.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        svcProps.put(Constants.SERVICE_DESCRIPTION, "DataSource service based on Tomcat JDBC");
        dsRegistration = bundleContext.registerService(javax.sql.DataSource.class, dataSource, svcProps);
    }

    private void registerJmx(ConnectionPool pool) throws SQLException {
        org.apache.tomcat.jdbc.pool.jmx.ConnectionPool jmxPool = pool.getJmxPool();
        if (jmxPool == null) {
            //jmx not enabled
            return;
        }
        Hashtable<String, String> table = new Hashtable<String, String>();
        table.put("type", "ConnectionPool");
        table.put("class", javax.sql.DataSource.class.getName());
        table.put("name", ObjectName.quote(name));

        try {
            jmxName = new ObjectName("org.apache.sling", table);
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(jmxPool, jmxName);
        } catch (Exception e) {
            log.warn("Error occurred while registering the JMX Bean for " +
                    "connection pool with name {}", jmxName, e);
        }
    }

    ConnectionPool getPool() {
        return dataSource.getPool();
    }

    private void unregisterJmx() {
        try {
            if (jmxName != null) {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                mbs.unregisterMBean(jmxName);
            }
        } catch (InstanceNotFoundException ignore) {
            // NOOP
        } catch (Exception e) {
            log.error("Unable to unregister JDBC pool with JMX", e);
        }
    }

    //~----------------------------------------< Config Handling >

    private static String getDataSourceName(Map<String, ?> config) {
        return PropertiesUtil.toString(config.get(PROP_DATASOURCE_NAME), null);
    }

    private static String getSvcPropName(Map<String, ?> config) {
        return PropertiesUtil.toString(config.get(PROP_DS_SVC_PROP_NAME), PROP_DATASOURCE_NAME);
    }

    private static void set(String name, String value, Properties props) {
        if (PROPS_WITH_DFEAULT.contains(name) && DEFAULT_VAL.equals(value)) {
            value = null;
        }

        if (value != null) {
            value = value.trim();
        }

        if (value != null && !value.isEmpty()) {
            props.setProperty(name, value);
        }
    }

    private static void checkArgument(boolean expression,
                                      String errorMessageTemplate,
                                      Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(
                    String.format(errorMessageTemplate, errorMessageArgs));
        }
    }

    private class LazyJmxRegisteringDataSource extends org.apache.tomcat.jdbc.pool.DataSource {
        private volatile boolean initialized;

        public LazyJmxRegisteringDataSource(PoolConfiguration poolProperties) {
            super(poolProperties);
        }

        @Override
        public ConnectionPool createPool() throws SQLException {
            ConnectionPool pool = super.createPool();
            registerJmxLazily(pool);
            return pool;
        }

        @Override
        public void close() {
            initialized = false;
            super.close();
        }

        private void registerJmxLazily(ConnectionPool pool) throws SQLException {
            if (!initialized) {
                synchronized (this) {
                    if (initialized) {
                        return;
                    }
                    DataSourceFactory.this.registerJmx(pool);
                    initialized = true;
                }
            }
        }
    }

    /**
     * Dummy impl to enable access to protected fields
     */
    private static class DummyDataSourceFactory extends org.apache.tomcat.jdbc.pool.DataSourceFactory {
        static String[] getPropertyNames() {
            return ALL_PROPERTIES;
        }
    }
}
