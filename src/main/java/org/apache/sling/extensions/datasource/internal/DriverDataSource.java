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


import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolUtilities;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

/**
 * DataSource implementation which only implements the Connection creation part. Tomcat
 * JDBC currently does not support specifying the Drive instance directly. While running
 * in OSGi env DriverRegistry maintains a list of seen driver instances.
 *
 * DriverDataSource make use of the DriverRegistry to lookup right Driver instance. This avoid
 * the requirement of having the Driver OSGi bundle attaches as fragments to our bundle
 */
class DriverDataSource implements DataSource {
    private final PoolConfiguration poolProperties;
    private final DriverRegistry driverRegistry;
    private final BundleContext bundleContext;
    private final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());
    private org.apache.tomcat.jdbc.pool.jmx.ConnectionPool jmxPool;
    private Driver driver;

    public DriverDataSource(PoolConfiguration poolProperties, DriverRegistry driverRegistry,
                            BundleContext bundleContext) {
        this.poolProperties = poolProperties;
        this.driverRegistry = driverRegistry;
        this.bundleContext = bundleContext;
    }

    public Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    public Connection getConnection(String usr, String pwd) throws SQLException {
        Properties properties = PoolUtilities.clone(poolProperties.getDbProperties());
        if(usr == null){
            usr = poolProperties.getUsername();
        }
        if(pwd == null){
            pwd= poolProperties.getPassword();
        }

        if (usr != null) properties.setProperty(PoolUtilities.PROP_USER, usr);
        if (pwd != null) properties.setProperty(PoolUtilities.PROP_PASSWORD, pwd);

        String driverURL = poolProperties.getUrl();
        Connection connection;
        try {
            connection = getDriver().connect(driverURL, properties);
        } catch (Exception x) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to connect to database.", x);
            }
            //Based on logic in org.apache.tomcat.jdbc.pool.PooledConnection.connectUsingDriver()
            if (jmxPool!=null) {
                jmxPool.notify(org.apache.tomcat.jdbc.pool.jmx.ConnectionPool.NOTIFY_CONNECT,
                        ConnectionPool.getStackTrace(x));
            }
            if (x instanceof SQLException) {
                throw (SQLException)x;
            } else {
                SQLException ex = new SQLException(x.getMessage());
                ex.initCause(x);
                throw ex;
            }
        }
        if (connection==null) {
            throw new SQLException("Driver:"+driver+" returned null for URL:"+driverURL);
        }

        return connection;
    }

    public void setJmxPool(org.apache.tomcat.jdbc.pool.jmx.ConnectionPool jmxPool) {
        this.jmxPool = jmxPool;
    }

    //~-------------------------------------< DataSource >

    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    public void setLoginTimeout(int seconds) throws SQLException {

    }

    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    private Driver getDriver() throws SQLException {
        if (driver != null) {
            return driver;
        }
        final String url = poolProperties.getUrl();

        Collection<Driver> drivers = driverRegistry.getDrivers();
        if(!drivers.isEmpty()) {
            log.debug("Looking for driver for [{}] against registered drivers", url);
            driver = findMatchingDriver(drivers);
        }

        if(driver == null){
            log.debug("Looking for driver for [{}] via provided className [{}]",
                    url, poolProperties.getDriverClassName());
            driver = loadDriverClass();
        }

        if(driver == null){
            //This one is redundant as DriverManager would filter out drivers
            //whose classes are not visible from our bundle classloader which
            //means that this list would be empty in most cases
            log.debug("Looking for driver from DriverManager");
            driver = findMatchingDriver(Collections.list(DriverManager.getDrivers()));
        }

        if(driver == null){
            String msg = String.format("Not able to find any matching driver for url [%s] " +
                    "and driverClassName [%s]",url,poolProperties.getDriverClassName());
            throw new SQLException(msg);
        }

        return driver;
    }

    private Driver loadDriverClass() throws SQLException {
        try {
              log.debug("Instantiating driver using class: {} [url={}]",
                      poolProperties.getDriverClassName(),poolProperties.getUrl());
                return (Driver) bundleContext.getBundle()
                        .loadClass(poolProperties.getDriverClassName()).newInstance();
        } catch (java.lang.Exception cn) {
            log.debug("Unable to instantiate JDBC driver.", cn);
            SQLException ex = new SQLException(cn.getMessage());
            ex.initCause(cn);
            throw ex;
        }
    }

    private Driver findMatchingDriver(Collection<Driver> drivers) {
        final String url = poolProperties.getUrl();
        for (Driver driver : drivers) {
            try {
                if (driver.acceptsURL(url)) {
                    return driver;
                }
            } catch (SQLException e) {
                log.debug("Error occurred while matching driver against url {}", url, e);
            }
        }
        return null;
    }
}
