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

package org.apache.sling.datasource.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.datasource.internal.DataSourceFactory.checkArgument;

@Component(
        name = JNDIDataSourceFactory.NAME,
        label = "Apache Sling JNDI DataSource",
        description = "Registers a DataSource instance with OSGi ServiceRegistry which is looked up " +
                "from the JNDI",
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE
)
public class JNDIDataSourceFactory {
    public static final String NAME = "org.apache.sling.datasource.JNDIDataSourceFactory";

    @Property
    static final String PROP_DATASOURCE_NAME = "datasource.name";

    @Property(value = PROP_DATASOURCE_NAME)
    static final String PROP_DS_SVC_PROP_NAME = "datasource.svc.prop.name";

    @Property(
            label = "JNDI Name (*)",
            description = "JNDI location name used to perform DataSource instance lookup"
    )
    static final String PROP_DS_JNDI_NAME = "datasource.jndi.name";

    @Property(
            label = "JNDI Properties",
            description = "Set the environment for the JNDI InitialContext i.e. properties passed on to InitialContext " +
                    "for performing the JNDI instance lookup. Each row form a map entry where each row format be propertyName=property " +
                    "e.g. java.naming.factory.initial=exampleFactory",
            value = {},
            cardinality = 1024)
    static final String PROP_JNDI_PROPS = "jndi.properties";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ServiceRegistration dsRegistration;


    @Activate
    protected void activate(BundleContext bundleContext, Map<String, ?> config) throws Exception {
        String name = DataSourceFactory.getDataSourceName(config);
        String jndiName = PropertiesUtil.toString(config.get(PROP_DS_JNDI_NAME), null);

        checkArgument(name != null, "DataSource name must be specified via [%s] property", PROP_DATASOURCE_NAME);
        checkArgument(jndiName != null, "DataSource JNDI name must be specified via [%s] property", PROP_DS_JNDI_NAME);

        DataSource dataSource = lookupDataSource(jndiName, config);
        String svcPropName = DataSourceFactory.getSvcPropName(config);

        Dictionary<String, Object> svcProps = new Hashtable<String, Object>();
        svcProps.put(svcPropName, name);
        svcProps.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        svcProps.put(Constants.SERVICE_DESCRIPTION, "DataSource service looked up from " + jndiName);
        dsRegistration = bundleContext.registerService(javax.sql.DataSource.class, dataSource, svcProps);

        log.info("Registered DataSource [{}] looked up from JNDI at [{}]", name, jndiName);
    }

    @Deactivate
    protected void deactivate() {
        if (dsRegistration != null) {
            dsRegistration.unregister();
            dsRegistration = null;
        }
    }

    private DataSource lookupDataSource(String jndiName, Map<String, ?> config) throws NamingException {
        Properties jndiProps = createJndiEnv(config);
        Context context = null;
        try {
            log.debug("Looking up DataSource [{}] with InitialContext env [{}]", jndiName, jndiProps);

            context = new InitialContext(jndiProps);
            Object lookup = context.lookup(jndiName);
            if (lookup == null) {
                throw new NameNotFoundException("JNDI object with [" + jndiName + "] not found");
            }

            if (!DataSource.class.isInstance(lookup)) {
                throw new IllegalStateException("JNDI object of type " + lookup.getClass() +
                        "is not an instance of javax.sql.DataSource");
            }

            return (DataSource) lookup;
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    private Properties createJndiEnv(Map<String, ?> config) {
        Properties props = new Properties();

        //Copy the other properties first
        Map<String, String> otherProps = PropertiesUtil.toMap(config.get(PROP_JNDI_PROPS), new String[0]);
        for (Map.Entry<String, String> e : otherProps.entrySet()) {
            set(e.getKey(), e.getValue(), props);
        }

        return props;
    }

    //~----------------------------------------< Config Handling >

    private static void set(String name, String value, Properties props) {
        if (value != null) {
            value = value.trim();
        }

        if (value != null && !value.isEmpty()) {
            props.setProperty(name, value);
        }
    }
}
